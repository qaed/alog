package nc.ui.fa.deployout.view.event;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.logging.Logger;
import nc.itf.fa.service.IAssetService;
import nc.itf.fa.service.ICloseBookService;
import nc.itf.fa.service.IDepmethodService;
import nc.pub.fa.card.AssetFieldConst;
import nc.pub.fa.common.manager.CompareManager;
import nc.pub.fa.common.manager.DeptScaleManager;
import nc.pub.fa.common.manager.VOManager;
import nc.pub.fa.common.util.StringUtils;
import nc.pub.fa.dep.ReduceSimulateManager;
import nc.pub.fa.deploy.DeployManager;
import nc.pub.fa.deploy.DeployValueParamConst;
import nc.ui.am.editor.AMBillForm;
import nc.ui.am.util.BillCardPanelUtils;
import nc.ui.fa.common.view.FADefaultCardEditEventHandler;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillItemEvent;
import nc.ui.pub.bill.BillModel;
import nc.ui.uif2.UIState;
import nc.vo.am.common.MappedBaseVO;
import nc.vo.am.common.util.ArrayUtils;
import nc.vo.am.common.util.CollectionUtils;
import nc.vo.am.common.util.MapUtils;
import nc.vo.am.common.util.UFDoubleUtils;
import nc.vo.am.constant.CommonKeyConst;
import nc.vo.am.manager.AccbookManager;
import nc.vo.am.manager.AccperiodVO;
import nc.vo.am.manager.CurrencyManager;
import nc.vo.am.manager.CurrencyRateManager;
import nc.vo.am.manager.LockManager;
import nc.vo.am.manager.ParameterManager;
import nc.vo.am.proxy.AMProxy;
import nc.vo.fa.asset.AssetVO;
import nc.vo.fa.deployin.DeployInHeadVO;
import nc.vo.fa.deployout.DeployOutBodyVO;
import nc.vo.fa.deployout.DeployOutHeadVO;
import nc.vo.fa.deployout.DeployOutVO;
import nc.vo.fa.deptscale.DeptScaleVO;
import nc.vo.fa.ref.CardRefModel;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

/**
 * ����������༭�¼��Ĵ���
 * 
 * @author yanjq 2009-12-21
 * 
 */
public class DeployOutEventHandler extends FADefaultCardEditEventHandler {

	// Map<pk_card,ģ���۾ɶ�>
	private Map<String, UFDouble> simulateDepamountMap = new HashMap<String, UFDouble>();
	// Map<pk_card,�ۼ��۾ɶ�>

	private Map<String, UFDouble> simulateAccudepMap = new HashMap<String, UFDouble>();
	// �Ѽ����·�

	Map<String, Integer> simulateUsedMonthMap = new HashMap<String, Integer>();
	// ��ƬMap<pk_card, pk_accbook>������֯���˲�������

	Map<String, String> outBizAssetMap = new HashMap<String, String>();;

	@Override
	public void handleHeadAfterEditEvent(AMBillForm billForm, BillEditEvent e) {

		// ������������֯����һ��
		if (e.getKey().equals(DeployOutHeadVO.PK_ORG_IN) || e.getKey().equals(DeployOutHeadVO.PK_ORG_IN_V)) {
			checkOrg(billForm);
		}
		if (e.getKey().equals(DeployOutHeadVO.ACCOUNT_CURRENCY)) {

			Object pk_org = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
			UFDate bussiness_date = (UFDate) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.BUSINESS_DATE).getValueObject();
			UFDouble account_rate = null;
			String account_currency = ArrayUtils.getFirstElem((String[]) e.getValue());
			if (StringUtils.isEmpty(account_currency)) {
				return;
			}

			int rateDigit = 2;
			try {
				String pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(pk_org.toString());
				String pk_currency = CurrencyManager.getCurrencyPKByAccbook(pk_accbook);
				account_rate = CurrencyRateManager.getRate(pk_currency, account_currency, bussiness_date);
				// ���ʾ���

				rateDigit = CurrencyRateManager.getRateDigitByAccbook(pk_accbook, pk_currency, account_currency);
				account_rate.setScale(rateDigit, UFDouble.ROUND_HALF_UP);
			} catch (BusinessException e1) {
				Logger.error(e);
			}
			// ���þ���

			BillItem item = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE);
			item.setDecimalDigits(8);
			billForm.getBillCardPanel().setHeadItem(DeployOutHeadVO.ACCOUNT_RATE, account_rate);
			billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE).setDecimalDigits(rateDigit);
			handleHeadAccountCurrencyAfterEdit(billForm);
		}

		if (DeployOutHeadVO.BUSINESS_DATE.equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
			// ��ͷ���������ڡ��༭���¼�
			handleHeadBusinessDateAfterEdit(billForm);
		}

		if (DeployOutHeadVO.ACCOUNT_RATE.equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
			// ��ͷ��������ʡ��༭���¼�
			handleHeadAccountRateAfterEdit(billForm);
		}
	}

	private void handleHeadAccountRateAfterEdit(AMBillForm billForm) {
		Object pk_org = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
		UFDate bussiness_date = (UFDate) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.BUSINESS_DATE).getValueObject();
		UFDouble account_rate = (UFDouble) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE).getValueObject();
		String account_currency = (String) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_CURRENCY).getValueObject();
		DeployOutVO obj = (DeployOutVO) billForm.getBillCardPanel().getBillData().getBillObjectByMetaData();
		String[] card_pks = VOManager.getAttributeValueArray(obj.getChildrenVO(), DeployOutBodyVO.PK_CARD);
		if (card_pks[0] == null) {
			return;
		}
		// �����۸��ѡ��

		Integer deployValueParamSelect = null;
		String pk_currency = null;
		// ģ���۾ɵõ�������
		try {

			String pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(pk_org.toString());
			pk_currency = CurrencyManager.getCurrencyPKByAccbook(pk_accbook);
			// �����۸��ѡ��

			deployValueParamSelect =
					ParameterManager.getParaInt(billForm.getContext().getPk_group(), DeployValueParamConst.DEPLOY_PARAM_CONST);

			simulateDep(billForm.getContext().getPk_group(), billForm.getContext().getPk_org(), ((DeployOutHeadVO) obj.getParentVO()).getBusiness_date(), card_pks);

			BillModel billModel = billForm.getBillCardPanel().getBillModel("bodyvos");
			int rowCount = billModel.getRowCount();
			// �жϵ�ǰ��ui״̬ ���Ϊ�޸�̬ Ҫ�������ñ����VOΪ�޸�̬

			UIState uiState = billForm.getModel().getUiState();
			DeployOutBodyVO[] objBodys = (DeployOutBodyVO[]) obj.getChildrenVO();
			for (int i = 0; i < rowCount; i++) {
				DeployOutBodyVO body = (DeployOutBodyVO) billModel.getBodyValueRowVO(i, DeployOutBodyVO.class.getName());
				for (int j = 0; j < objBodys.length; j++) {
					DeployOutBodyVO objBody = objBodys[j];
					if (objBody.getPk_card().equals(body.getPk_card())) {
						// ����ԭֵ
						UFDouble localoriginvalue =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.LOCALORIGINVALUE);
						// �ۼ��۾�

						UFDouble accudep = simulateAccudepMap.get(body.getPk_card());
						// ��ֵ׼��

						UFDouble predevaluate =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.PREDEVALUATE);

						UFDouble deployPrice = UFDouble.ZERO_DBL;
						UFDouble deployAccudep = UFDouble.ZERO_DBL;
						UFDouble deployPredevaluate = UFDouble.ZERO_DBL;
						String card_book = outBizAssetMap.get(objBody.getPk_card());
						String card_currency = CurrencyManager.getCurrencyPKByAccbook(card_book);
						if (card_currency.equals(pk_currency)) {
							deployPrice =
									DeployManager.procDeployPrice(pk_currency, account_currency, localoriginvalue, accudep, predevaluate, deployValueParamSelect, account_rate, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(pk_currency, account_currency, accudep, deployValueParamSelect, account_rate, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(pk_currency, account_currency, predevaluate, deployValueParamSelect, account_rate, bussiness_date);
						} else {

							deployPrice =
									DeployManager.procDeployPrice(card_currency, account_currency, localoriginvalue, accudep, predevaluate, deployValueParamSelect, null, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(card_currency, account_currency, accudep, deployValueParamSelect, null, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(card_currency, account_currency, predevaluate, deployValueParamSelect, null, bussiness_date);
						}
						// ���Ϊ�޸�̬ ����

						if (UIState.EDIT == uiState) {
							billForm.getBillCardPanel().getBillModel().setRowState(i, BillModel.MODIFICATION);
						}
						// ע��ֵ

						billForm.getBillCardPanel().setBodyValueAt(deployPrice, i, DeployOutBodyVO.DEPLOY_PRICE);
						billForm.getBillCardPanel().setBodyValueAt(deployAccudep, i, DeployOutBodyVO.DEPLOY_ACCUDEP);
						billForm.getBillCardPanel().setBodyValueAt(deployPredevaluate, i, DeployOutBodyVO.DEPLOY_PDALUATE);
					}
				}

			}
		} catch (BusinessException e) {
			Logger.error(e);
			billForm.showErrorMessage(e.getMessage());
		}
	}

	private void handleHeadAccountCurrencyAfterEdit(AMBillForm billForm) {
		Object pk_org = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
		UFDate bussiness_date = (UFDate) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.BUSINESS_DATE).getValueObject();
		UFDouble account_rate = (UFDouble) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE).getValueObject();
		String account_currency = (String) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_CURRENCY).getValueObject();
		DeployOutVO obj = (DeployOutVO) billForm.getBillCardPanel().getBillData().getBillObjectByMetaData();
		String[] card_pks = VOManager.getAttributeValueArray(obj.getChildrenVO(), DeployOutBodyVO.PK_CARD);
		if (card_pks[0] == null) {
			return;
		}
		// �����۸��ѡ��

		Integer deployValueParamSelect = null;
		String pk_currency = null;
		// ģ���۾ɵõ�������
		try {

			String pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(pk_org.toString());
			pk_currency = CurrencyManager.getCurrencyPKByAccbook(pk_accbook);
			// �����۸��ѡ��

			deployValueParamSelect =
					ParameterManager.getParaInt(billForm.getContext().getPk_group(), DeployValueParamConst.DEPLOY_PARAM_CONST);

			simulateDep(billForm.getContext().getPk_group(), billForm.getContext().getPk_org(), ((DeployOutHeadVO) obj.getParentVO()).getBusiness_date(), card_pks);

			BillModel billModel = billForm.getBillCardPanel().getBillModel("bodyvos");
			int rowCount = billModel.getRowCount();
			// �жϵ�ǰ��ui״̬ ���Ϊ�޸�̬ Ҫ�������ñ����VOΪ�޸�̬

			UIState uiState = billForm.getModel().getUiState();
			DeployOutBodyVO[] objBodys = (DeployOutBodyVO[]) obj.getChildrenVO();
			for (int i = 0; i < rowCount; i++) {
				DeployOutBodyVO body = (DeployOutBodyVO) billModel.getBodyValueRowVO(i, DeployOutBodyVO.class.getName());
				for (int j = 0; j < objBodys.length; j++) {
					DeployOutBodyVO objBody = objBodys[j];
					if (objBody.getPk_card().equals(body.getPk_card())) {
						// ����ԭֵ
						UFDouble localoriginvalue =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.LOCALORIGINVALUE);
						// �ۼ��۾�

						UFDouble accudep = simulateAccudepMap.get(body.getPk_card());
						// ��ֵ׼��

						UFDouble predevaluate =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.PREDEVALUATE);

						UFDouble deployPrice = UFDouble.ZERO_DBL;
						UFDouble deployAccudep = UFDouble.ZERO_DBL;
						UFDouble deployPredevaluate = UFDouble.ZERO_DBL;
						String card_book = outBizAssetMap.get(objBody.getPk_card());
						String card_currency = CurrencyManager.getCurrencyPKByAccbook(card_book);
						if (card_currency.equals(pk_currency)) {
							deployPrice =
									DeployManager.procDeployPrice(pk_currency, account_currency, localoriginvalue, accudep, predevaluate, deployValueParamSelect, account_rate, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(pk_currency, account_currency, accudep, deployValueParamSelect, account_rate, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(pk_currency, account_currency, predevaluate, deployValueParamSelect, account_rate, bussiness_date);
						} else {

							deployPrice =
									DeployManager.procDeployPrice(card_currency, account_currency, localoriginvalue, accudep, predevaluate, deployValueParamSelect, null, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(card_currency, account_currency, accudep, deployValueParamSelect, null, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(card_currency, account_currency, predevaluate, deployValueParamSelect, null, bussiness_date);
						}
						// ���Ϊ�޸�̬ ����

						if (UIState.EDIT == uiState) {
							billForm.getBillCardPanel().getBillModel().setRowState(i, BillModel.MODIFICATION);
						}
						// ע��ֵ

						billForm.getBillCardPanel().setBodyValueAt(deployPrice, i, DeployOutBodyVO.DEPLOY_PRICE);
						billForm.getBillCardPanel().setBodyValueAt(deployAccudep, i, DeployOutBodyVO.DEPLOY_ACCUDEP);
						billForm.getBillCardPanel().setBodyValueAt(deployPredevaluate, i, DeployOutBodyVO.DEPLOY_PDALUATE);
					}
				}

			}
		} catch (BusinessException e) {
			Logger.error(e);
			billForm.showErrorMessage(e.getMessage());
		}
	}

	private void handleHeadBusinessDateAfterEdit(AMBillForm billForm) {
		Object pk_org = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
		UFDate bussiness_date = (UFDate) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.BUSINESS_DATE).getValueObject();
		UFDouble account_rate = (UFDouble) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE).getValueObject();
		String account_currency = (String) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_CURRENCY).getValueObject();
		DeployOutVO obj = (DeployOutVO) billForm.getBillCardPanel().getBillData().getBillObjectByMetaData();
		// �����۸��ѡ��

		Integer deployValueParamSelect = null;
		String pk_currency = null;
		// ģ���۾ɵõ�������
		try {

			String pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(pk_org.toString());
			// 20170517 tsy ��ӵ���ʱ��У��
			boolean ismin =
					AMProxy.lookup(ICloseBookService.class).isMinUnCloseBookPeriod(pk_org.toString(), bussiness_date.toString(), pk_accbook);
			if (!ismin) {
				AccperiodVO min = AMProxy.lookup(ICloseBookService.class).queryMinUnClosebookPeriod(pk_org.toString(), pk_accbook);
				billForm.getBillCardPanel().setHeadItem(DeployOutHeadVO.BUSINESS_DATE, min.getEnddate());
				bussiness_date = min.getEnddate();
				billForm.showErrorMessage(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0072"));/*
																															 * @
																															 * res
																															 * "�������ڱ�������Сδ�������ڣ�ϵͳĬ��Ϊ��Сδ�����µ����һ�죡"
																															 */
				//��ձ�����
				int rows = billForm.getBillCardPanel().getRowCount();
				for (int i = 0; i < rows; i++) {
					billForm.getBillCardPanel().clearRowData(i, null);
				}
				return;
			}
			// 20170517 end

			pk_currency = CurrencyManager.getCurrencyPKByAccbook(pk_accbook);
			String[] card_pks = VOManager.getAttributeValueArray(obj.getChildrenVO(), DeployOutBodyVO.PK_CARD);
			if (card_pks[0] == null) {
				return;
			}
			// �����۸��ѡ��

			deployValueParamSelect =
					ParameterManager.getParaInt(billForm.getContext().getPk_group(), DeployValueParamConst.DEPLOY_PARAM_CONST);

			simulateDep(billForm.getContext().getPk_group(), billForm.getContext().getPk_org(), ((DeployOutHeadVO) obj.getParentVO()).getBusiness_date(), card_pks);

			BillModel billModel = billForm.getBillCardPanel().getBillModel("bodyvos");
			int rowCount = billModel.getRowCount();
			// �жϵ�ǰ��ui״̬ ���Ϊ�޸�̬ Ҫ�������ñ����VOΪ�޸�̬

			UIState uiState = billForm.getModel().getUiState();
			DeployOutBodyVO[] objBodys = (DeployOutBodyVO[]) obj.getChildrenVO();
			for (int i = 0; i < rowCount; i++) {
				DeployOutBodyVO body = (DeployOutBodyVO) billModel.getBodyValueRowVO(i, DeployOutBodyVO.class.getName());
				for (int j = 0; j < objBodys.length; j++) {
					DeployOutBodyVO objBody = objBodys[j];
					if (objBody.getPk_card().equals(body.getPk_card())) {
						// ����ԭֵ
						UFDouble localoriginvalue =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.LOCALORIGINVALUE);
						// �ۼ��۾�

						UFDouble accudep = simulateAccudepMap.get(body.getPk_card());
						// ��ֵ׼��

						UFDouble predevaluate =
								(UFDouble) billForm.getBillCardPanel().getBodyValueAt(i, AssetFieldConst.CARD + AssetVO.PREDEVALUATE);

						Integer usemonth = simulateUsedMonthMap.get(body.getPk_card());
						// ģ���۾�

						UFDouble simuldateDep = simulateDepamountMap.get(body.getPk_card());

						UFDouble red_accudep = accudep;

						UFDouble deployPrice = UFDouble.ZERO_DBL;
						UFDouble deployAccudep = UFDouble.ZERO_DBL;
						UFDouble deployPredevaluate = UFDouble.ZERO_DBL;
						String card_book = outBizAssetMap.get(objBody.getPk_card());
						String card_currency = CurrencyManager.getCurrencyPKByAccbook(card_book);
						if (card_currency.equals(pk_currency)) {
							deployPrice =
									DeployManager.procDeployPrice(pk_currency, account_currency, localoriginvalue, red_accudep, predevaluate, deployValueParamSelect, account_rate, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(pk_currency, account_currency, red_accudep, deployValueParamSelect, account_rate, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(pk_currency, account_currency, predevaluate, deployValueParamSelect, account_rate, bussiness_date);
						} else {

							deployPrice =
									DeployManager.procDeployPrice(card_currency, account_currency, localoriginvalue, red_accudep, predevaluate, deployValueParamSelect, null, bussiness_date);

							deployAccudep =
									DeployManager.procDeployAccudep(card_currency, account_currency, red_accudep, deployValueParamSelect, null, bussiness_date);

							deployPredevaluate =
									DeployManager.procDeployPredevaluate(card_currency, account_currency, predevaluate, deployValueParamSelect, null, bussiness_date);
						}
						// ���Ϊ�޸�̬ ����

						if (UIState.EDIT == uiState) {
							billForm.getBillCardPanel().getBillModel().setRowState(i, BillModel.MODIFICATION);
						}
						// ��ֵ = ����ԭֵ - �ۼ��۾�

						UFDouble netValue = UFDoubleUtils.sub(localoriginvalue, accudep);
						UFDouble netrating = UFDoubleUtils.sub(netValue, predevaluate);

						// ע��ֵ

						billForm.getBillCardPanel().setBodyValueAt(deployPrice, i, DeployOutBodyVO.DEPLOY_PRICE);
						billForm.getBillCardPanel().setBodyValueAt(deployAccudep, i, DeployOutBodyVO.DEPLOY_ACCUDEP);
						billForm.getBillCardPanel().setBodyValueAt(deployPredevaluate, i, DeployOutBodyVO.DEPLOY_PDALUATE);
						billForm.getBillCardPanel().setBodyValueAt(accudep, i, DeployOutBodyVO.ACCUDEP);
						billForm.getBillCardPanel().setBodyValueAt(simuldateDep, i, DeployOutBodyVO.SIMULATEDEP);
						billForm.getBillCardPanel().setBodyValueAt(red_accudep, i, DeployOutBodyVO.RED_ACCUDEP);
						billForm.getBillCardPanel().setBodyValueAt(netValue, i, DeployOutBodyVO.NETVALUE);
						billForm.getBillCardPanel().setBodyValueAt(netrating, i, DeployOutBodyVO.NETRATING);
						billForm.getBillCardPanel().setBodyValueAt(usemonth, i, DeployOutBodyVO.USEMONTH);
					}
				}

			}
		} catch (BusinessException e) {
			Logger.error(e);
			billForm.showErrorMessage(e.getMessage());
		}
	}

	@Override
	public void handleBodyAfterEditEvent(AMBillForm billForm, BillEditEvent e) {

		// ��Ƭ�༭���¼�
		if (e.getKey().equals(DeployOutBodyVO.PK_CARD)) {
			handleAfterCard(billForm, e);
		}
	}

	private void checkOrg(AMBillForm billForm) {

		Object pk_outObj = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
		Object pk_inObj = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG_IN).getValueObject();

		if (CompareManager.equals(pk_outObj, pk_inObj)) {
			MessageDialog.showErrorDlg(billForm, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0023")/*
																																	 * @
																																	 * res
																																	 * "����"
																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0024")/*
																																																							 * @
																																																							 * res
																																																							 * "����������ҵ��Ԫ������ͬ��"
																																																							 */);

			billForm.getBillCardPanel().setHeadItem(DeployOutHeadVO.PK_ORG_IN, null);
			billForm.getBillCardPanel().setHeadItem(DeployOutHeadVO.PK_ORG_IN_V, null);
		}

	}

	/**
	 * ��Ƭ�༭���¼�
	 * 
	 * @param billForm
	 * @param e
	 * @date 2010-4-20
	 */

	private void handleAfterCard(AMBillForm billForm, BillEditEvent e) {
		Object pk_org = billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.PK_ORG).getValueObject();
		String pk_accbook = "";
		UFDate bussiness_date = (UFDate) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.BUSINESS_DATE).getValueObject();
		try {
			if (bussiness_date == null) {
				billForm.showErrorMessage(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0073")/*
																															 * @
																															 * res
																															 * "����ѡ�������ڣ�"
																															 */);

				return;
			} else {
				pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(pk_org.toString());
				//20170517 tsy ��ӵ���ʱ��У��
				boolean ismin =
						AMProxy.lookup(ICloseBookService.class).isMinUnCloseBookPeriod(pk_org.toString(), bussiness_date.toString(), pk_accbook);
				if (!ismin) {
					billForm.getBillCardPanel().setHeadItem(DeployOutHeadVO.BUSINESS_DATE, null);
					billForm.showErrorMessage(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0072"));/*
																																 * @
																																 * res
																																 * "�������ڱ�������Сδ�������ڣ�"
																																 */
					//��ձ�����
					int rows = billForm.getBillCardPanel().getRowCount();
					for (int i = 0; i < rows; i++) {
						billForm.getBillCardPanel().clearRowData(i, null);
					}
					return;
				}
				//20170517 end
			}

		} catch (BusinessException e2) {

			billForm.showErrorMessage(e2.getMessage());
		}
		UFDouble account_rate = (UFDouble) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_RATE).getValueObject();
		String account_currency = (String) billForm.getBillCardPanel().getHeadItem(DeployOutHeadVO.ACCOUNT_CURRENCY).getValueObject();
		// DeployOutVO obj = (DeployOutVO)
		// billForm.getBillCardPanel().getBillData().getBillObjectByMetaData();

		String pk_currency = null;
		BillCardPanel billCardPanel = billForm.getBillCardPanel();
		UIRefPane refPane = (UIRefPane) billCardPanel.getBodyItem(DeployOutBodyVO.PK_CARD).getComponent();
		String[] pk_cards = refPane.getRefModel().getPkValues();
		Map<String, List<DeptScaleVO>> deptMap = null;
		String oldPk_card = (String) e.getOldValue();
		// �Ծɿ�Ƭ����

		if (StringUtils.isNotBlank(oldPk_card)) {
			LockManager.releasePKLocks(new String[] { oldPk_card }, billForm.getContext().getPk_loginUser());
		}

		if (ArrayUtils.isEmpty(pk_cards)) {
			// ��ñ���PK
			String pk_deplouout_b = (String) billForm.getBillCardPanel().getBodyValueAt(e.getRow(), DeployOutBodyVO.PK_DEPLOYOUT_B);
			// ɾ��ԭ������
			// billForm.getBillCardPanel().getBillModel().delLine(new
			// int[]{e.getRow()});
			// ���ԭ������

			billForm.getBillCardPanel().clearRowData(e.getRow(), null);
			// ���ñ���PK

			billForm.getBillCardPanel().setBodyValueAt(pk_deplouout_b, e.getRow(), DeployOutBodyVO.PK_DEPLOYOUT_B);
			return;
		}

		try {

			pk_currency = CurrencyManager.getCurrencyPKByAccbook(pk_accbook);
			// ��ѡ�¿�Ƭ����

			manageNewCards(billForm, e);
		} catch (BusinessException be) {
			Logger.error(be);
			billForm.showErrorMessage(be.getMessage());
			return;
		}

		List<String> repeatedCardPks = getRepeatedCardPks();
		billForm.getBillCardPanel().getBillModel().execLoadFormula();
		if (repeatedCardPks != null && !repeatedCardPks.isEmpty()) {
			List<String> filteredNewPkCards = new ArrayList<String>();
			for (String pk : pk_cards) {
				filteredNewPkCards.add(pk);
			}

			filteredNewPkCards.removeAll(repeatedCardPks);
			pk_cards = filteredNewPkCards.toArray(new String[0]);
		}

		try {
			deptMap = AMProxy.lookup(IAssetService.class).queryUseDeptBatch(pk_cards);

			// ����PK
			String pk_group = billForm.getContext().getPk_group();
			// �����۸��ѡ��
			Integer deployValueParamSelect = ParameterManager.getParaInt(pk_group, DeployValueParamConst.DEPLOY_PARAM_CONST);

			// ģ���۾�
			UFDate business_date = (UFDate) BillCardPanelUtils.getHeadItemValue(billForm.getBillCardPanel(), DeployOutHeadVO.BUSINESS_DATE);
			simulateDep(pk_group, billForm.getContext().getPk_org(), business_date, pk_cards);

			// ȡ��ͷ�����˲�
			String main_pk_accbook_scale = AccbookManager.queryMainAccbookIDByOrg(pk_org.toString());
			// ע���˲�ֵ
			billCardPanel.setHeadItem(DeployOutHeadVO.PK_ACCBOOK_SCALE, main_pk_accbook_scale);

			MappedBaseVO[] mapBaseVOs =
					AMProxy.lookup(IAssetService.class).queryAssetFieldValues(pk_cards, main_pk_accbook_scale, new String[] { "fa_card." + AssetFieldConst.PK_CARD, AssetFieldConst.ORIGINVALUE, AssetFieldConst.LOCALORIGINVALUE, AssetFieldConst.PREDEVALUATE, AssetFieldConst.ASSET_STATE, AssetFieldConst.PK_CURRENCY, AssetFieldConst.SERVICEMONTH, AssetFieldConst.TAX_INPUT });

			// ע���ͷ����
			if (ArrayUtils.isNotEmpty(mapBaseVOs)) {
				MappedBaseVO baseVO = mapBaseVOs[0];
				// ��ñ��� �����˲�
				if (null != baseVO) {
					// ȡ��Ƭ����
					String mbPk_currency = StringUtils.toString(baseVO.getAttributeValue(AssetFieldConst.PK_CURRENCY));
					// ע��ֵ
					billCardPanel.setHeadItem(DeployOutHeadVO.PK_CURRENCY, mbPk_currency);
				}
			}
			// ��֤�������ݵľ�����ȷ
			BillItem item1 = billForm.getBillCardPanel().getBodyItem(DeployOutBodyVO.DEPLOY_PRICE);
			item1.setDecimalDigits(8);
			BillItem item2 = billForm.getBillCardPanel().getBodyItem(DeployOutBodyVO.DEPLOY_ACCUDEP);
			item2.setDecimalDigits(8);
			BillItem item3 = billForm.getBillCardPanel().getBodyItem(DeployOutBodyVO.DEPLOY_PDALUATE);
			item3.setDecimalDigits(8);

			for (int i = 0; i < pk_cards.length; i++) {
				int row = e.getRow() + i;

				if (ArrayUtils.isNotEmpty(mapBaseVOs)) {
					for (MappedBaseVO baseVO : mapBaseVOs) {
						// ��ÿ�ƬPK
						String mbPk_card = StringUtils.toString(baseVO.getAttributeValue(CommonKeyConst.pk_card));
						if (StringUtils.isNotEmpty(mbPk_card) && StringUtils.isNotEmpty(pk_cards[i]) && pk_cards[i].equals(mbPk_card)) {
							// ��ñ��� ���ֿ�Ƭ
							String mbPk_currency = StringUtils.toString(baseVO.getAttributeValue(AssetFieldConst.PK_CURRENCY));
							// ע�������֣�pk_accbook_scale ��ʱ���á����Դ洢���Ǳ���ֵ
							billCardPanel.setBodyValueAt(mbPk_currency, row, DeployOutBodyVO.PK_ACCBOOK_SCALE);
							billCardPanel.setBodyValueAt(mbPk_currency, row, DeployOutBodyVO.PK_CURRENCY);
						}
					}
				}

				// ע������˲�
				// billCardPanel.setBodyValueAt(main_pk_accbook_scale, row,
				// DeployOutBodyVO.PK_ACCBOOK_SCALE);
				// ��ǰ��Ƭ����
				String pk_card = pk_cards[i];
				// ʹ�ò���
				String usedept = DeptScaleManager.getShowName(CollectionUtils.toArray(deptMap.get(pk_card)));
				billCardPanel.setBodyValueAt(usedept, row, DeptScaleManager.USEDEPT);

				// ����ԭֵ
				UFDouble localoriginvalue = (UFDouble) billCardPanel.getBodyValueAt(row, AssetFieldConst.CARD + AssetVO.LOCALORIGINVALUE);
				// �ۼ��۾�
				UFDouble accudep = simulateAccudepMap.get(pk_card);
				// ��ֵ׼��
				UFDouble predevaluate = (UFDouble) billCardPanel.getBodyValueAt(row, AssetFieldConst.CARD + AssetVO.PREDEVALUATE);
				Integer usemonth = simulateUsedMonthMap.get(pk_card);
				// ģ���۾�
				UFDouble simuldateDep = simulateDepamountMap.get(pk_card);
				UFDouble red_accudep = accudep;
				UFDouble deployPrice = UFDouble.ZERO_DBL;
				UFDouble deployAccudep = UFDouble.ZERO_DBL;
				UFDouble deployPredevaluate = UFDouble.ZERO_DBL;
				String card_book = outBizAssetMap.get(pk_card);
				String card_currency = CurrencyManager.getCurrencyPKByAccbook(card_book);
				if (StringUtils.isEmpty(card_currency)) {
					return;
				}
				if (StringUtils.isNotEmpty(account_currency) && StringUtils.isNotEmpty(card_currency)) {
					if (card_currency.equals(pk_currency)) {
						deployPrice =
								DeployManager.procDeployPrice(pk_currency, account_currency, localoriginvalue, red_accudep, predevaluate, deployValueParamSelect, account_rate, bussiness_date);
						deployAccudep =
								DeployManager.procDeployAccudep(pk_currency, account_currency, red_accudep, deployValueParamSelect, account_rate, bussiness_date);
						deployPredevaluate =
								DeployManager.procDeployPredevaluate(pk_currency, account_currency, predevaluate, deployValueParamSelect, account_rate, bussiness_date);
					} else {
						deployPrice =
								DeployManager.procDeployPrice(card_currency, account_currency, localoriginvalue, red_accudep, predevaluate, deployValueParamSelect, null, bussiness_date);
						deployAccudep =
								DeployManager.procDeployAccudep(card_currency, account_currency, red_accudep, deployValueParamSelect, null, bussiness_date);
						deployPredevaluate =
								DeployManager.procDeployPredevaluate(card_currency, account_currency, predevaluate, deployValueParamSelect, null, bussiness_date);
					}
				}

				// ��ֵ = ����ԭֵ - �ۼ��۾�
				UFDouble netValue = UFDoubleUtils.sub(localoriginvalue, accudep);
				UFDouble netrating = UFDoubleUtils.sub(netValue, predevaluate);

				// ע��ֵ
				billCardPanel.setBodyValueAt(deployPrice, row, DeployOutBodyVO.DEPLOY_PRICE);
				billCardPanel.setBodyValueAt(deployAccudep, row, DeployOutBodyVO.DEPLOY_ACCUDEP);
				billCardPanel.setBodyValueAt(deployPredevaluate, row, DeployOutBodyVO.DEPLOY_PDALUATE);
				billCardPanel.setBodyValueAt(accudep, row, DeployOutBodyVO.ACCUDEP);
				billCardPanel.setBodyValueAt(simuldateDep, row, DeployOutBodyVO.SIMULATEDEP);
				billCardPanel.setBodyValueAt(red_accudep, row, DeployOutBodyVO.RED_ACCUDEP);
				billCardPanel.setBodyValueAt(netValue, row, DeployOutBodyVO.NETVALUE);
				billCardPanel.setBodyValueAt(netrating, row, DeployOutBodyVO.NETRATING);
				billCardPanel.setBodyValueAt(usemonth, row, DeployOutBodyVO.USEMONTH);
			}

			// �����������Ŀ�Ƭ
			handleWorkLoanCard(billForm, e.getRow(), pk_cards.length);

			// ���ع�����
			billCardPanel.getBillModel().loadLoadRelationItemValue();
			// DeployOutVO obj = (DeployOutVO)
			// billForm.getBillCardPanel().getBillData().getBillObjectByMetaData();
			// �ֶ�������
			billForm.getScaleProcessor().processScale();
			// ���⴦������״̬ʱ�ı����С�ԭ��ԭֵ����Ŀ���ȡ�
			setBodyOriginvalueScale(billForm, e.getRow(), pk_cards.length);

		} catch (BusinessException e1) {
			billForm.showErrorMessage(e1.getMessage());

		}
	}

	/**
	 * <p>
	 * ���⴦������״̬ʱ�ı����С�ԭ��ԭֵ����Ŀ���ȡ�
	 * 
	 * @param billForm
	 *            �ʲ���������
	 * @param rowNum
	 *            ���е��к�
	 */
	private void setBodyOriginvalueScale(AMBillForm billForm, int editRow, int cardCount) {

		if (editRow >= 0 && cardCount >= 0) {
			// ȡ�á�ԭ��ԭֵ����Ŀ��
			BillItem item = billForm.getBillCardPanel().getBodyItem(AssetFieldConst.CARD + AssetFieldConst.ORIGINVALUE);
			// ���д���
			for (int row = editRow; row < editRow + cardCount; ++row) {
				if (item.getDecimalListener() != null) {
					// ȡ�á�ԭ�ұ��֡���
					Object pkValue = billForm.getBillCardPanel().getBodyValueAt(row, AssetFieldConst.CARD + AssetFieldConst.PK_CURRENCY);
					if (pkValue != null) {
						// ���á�ԭ��ԭֵ����Ŀ�ľ��ȡ�
						item.setDecimalDigits(item.getDecimalListener().getDecimalFromSource(row, pkValue));
						// ȡ�á�ԭ��ԭֵ����ֵ��
						Object originvalue =
								billForm.getBillCardPanel().getBodyValueAt(row, AssetFieldConst.CARD + AssetFieldConst.ORIGINVALUE);
						// �������á�ԭ��ԭֵ����ֵ���������ܾ�ȷ���ϲ����õľ����ˡ�
						billForm.getBillCardPanel().getBillModel().setValueAt(originvalue, row, AssetFieldConst.CARD + AssetFieldConst.ORIGINVALUE);
					}
				}
			}
		}
	}

	/**
	 * �༭ǰ�¼�
	 * 
	 * @param billForm
	 * @param e
	 * @return
	 * @author yanjq
	 * @date 2010-4-20
	 * @see nc.ui.fa.common.view.FADefaultCardEditEventHandler#handleBodyBeforeEditEvent(nc.ui.am.editor.AMBillForm,
	 *      nc.ui.pub.bill.BillEditEvent)
	 */
	@Override
	public boolean handleBodyBeforeEditEvent(AMBillForm billForm, BillEditEvent e) {

		// ���ø���ķ���
		super.handleBodyBeforeEditEvent(billForm, e);

		BillCardPanel cardPanel = billForm.getBillCardPanel();
		BillItem orgItem = cardPanel.getHeadItem(DeployOutHeadVO.PK_ORG);

		String pk_org = (String) orgItem.getValueObject();
		if (pk_org == null) {
			pk_org = (String) orgItem.getValueCache();
		}
		// û¼����֯�ֶξ��޷�¼���������
		if (orgItem == null || StringUtils.isBlank(pk_org)) {
			return false;
		}

		if (e.getKey().equals(CommonKeyConst.pk_card)) {
			// ���忨Ƭ����
			BillItem cardItem = billForm.getBillCardPanel().getBodyItem(CommonKeyConst.pk_card);
			if (cardItem != null) {
				UIRefPane cardref = (UIRefPane) cardItem.getComponent();
				CardRefModel refModel = (CardRefModel) cardref.getRefModel();
				// ����pk_org�����忨Ƭ����
				refModel.setPk_org(pk_org);
			}
		}
		return true;

	}

	@Override
	public boolean handleHeadBeforeEditEvent(AMBillForm billForm, BillItemEvent e) {

		// ��pk_org���õ����в�����
		super.handleHeadBeforeEditEvent(billForm, e);

		// ������֯�Ĳ��ղ�������pk_org
		BillItem inOrgItem = billForm.getBillCardPanel().getHeadItem(DeployInHeadVO.PK_ORG_IN);
		if (inOrgItem != null) {
			UIRefPane inOrgRef = (UIRefPane) inOrgItem.getComponent();
			inOrgRef.getRefModel().setPk_org(null);
		}

		return true;
	}

	/**
	 * ģ���۾�
	 * 
	 * @param pk_group
	 * @param pk_org
	 * @param pk_cards
	 * @return
	 * @throws BusinessException
	 * @author yanjq
	 * @date 2010-6-9
	 */
	private void simulateDep(String pk_group, String pk_org, UFDate business_date, String... pk_cards) throws BusinessException {

		// Map<pk_card,ģ���۾�ֵ>
		ReduceSimulateManager depFactory = new ReduceSimulateManager();

		// ����ͬ�˲��Ŀ�ƬŪ��һ��Map<pk_accbook,List<Pk_card>>
		Map<String, List<String>> accbookToCardListMap = new HashMap<String, List<String>>();

		Map<String, String> accbookCardMap = AMProxy.lookup(IAssetService.class).queryBizAccbook(pk_cards);
		outBizAssetMap = accbookCardMap;
		for (Map.Entry<String, String> entry : accbookCardMap.entrySet()) {
			MapUtils.putListElem(accbookToCardListMap, entry.getValue(), entry.getKey());
		}

		for (Map.Entry<String, List<String>> entry : accbookToCardListMap.entrySet()) {
			String[] toSimCardPks = CollectionUtils.toArray(entry.getValue());
			String pk_accbook = entry.getKey();
			depFactory.procSimulateDep(toSimCardPks, pk_group, pk_org, pk_accbook, business_date, UFBoolean.FALSE);
			simulateDepamountMap.putAll(depFactory.getSimulateDepamoutMap());
			simulateAccudepMap.putAll(depFactory.getSimulateAccudepMap());
			simulateUsedMonthMap.putAll(depFactory.getSimulateUsedMonthMap());
		}
	}

	private void handleWorkLoanCard(AMBillForm billForm, int editRow, int cardCount) throws BusinessException {

		// ����PK
		String pk_group = billForm.getContext().getPk_group();
		// ���������۾ɷ���
		String pkDepmethodOfWorkLoad = AMProxy.lookup(IDepmethodService.class).queryDepmethodPKOfWorkLoan(pk_group);

		List<Integer> workLoanRowList = new ArrayList<Integer>();
		BillCardPanel billCardPanel = billForm.getBillCardPanel();
		for (int row = editRow; row < editRow + cardCount; ++row) {
			String pk_depmethod = (String) billCardPanel.getBodyValueAt(row, AssetVO.PK_CARD + "." + AssetVO.PK_DEPMETHOD + "_ID");

			if (StringUtils.isEquals(pkDepmethodOfWorkLoad, pk_depmethod)) {
				workLoanRowList.add(row);
			}
		}

		if (CollectionUtils.isNotEmpty(workLoanRowList)) {
			StringBuilder card_codeBuilder = new StringBuilder();
			for (int row : workLoanRowList) {
				// AssetVO.PK_CARD���ǿ�Ƭ���룬BillCardPanelUtils.getBodyValue���ܻ�ȡ��PK
				String card_code = (String) billCardPanel.getBodyValueAt(row, AssetVO.PK_CARD);
				card_codeBuilder.append("[" + card_code + "] ");
			}
			String msg = nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0025")/*
																											 * @
																											 * res
																											 * "���Ϊ{0}�Ŀ�Ƭ���۾ɷ���Ϊ������������ҪԤ��¼�뱾�¹�����������׼ȷ�ؼ���Ԥ���۾ɡ�\n��ȷ����Щ��Ƭ�Ƿ��Ѿ�¼���˱��¹������������δ¼�룬�뵽�۾���̯���ڵ�¼�뱾�¹�������Ȼ���ٽ��б��β���"
																											 */;
			msg = MessageFormat.format(msg, card_codeBuilder.toString());
			if (nc.ui.pub.beans.MessageDialog.showYesNoDlg(billForm, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0020")/*
																																						 * @
																																						 * res
																																						 * "NCϵͳ"
																																						 */, msg) == nc.ui.pub.beans.UIDialog.ID_NO) {
				List<String> pk_cardList = new ArrayList<String>();
				for (int row : workLoanRowList) {
					String pk_card = (String) BillCardPanelUtils.getBodyValue(billCardPanel, AssetVO.PK_CARD, row);
					pk_cardList.add(pk_card);
					billCardPanel.clearRowData(row, null);
				}
				// û��¼�뱾�¹��������Ŀ�Ƭ��յ�
				LockManager.releasePKLocks(CollectionUtils.toArray(pk_cardList), billForm.getContext().getPk_loginUser());

			}
		}
	}

}