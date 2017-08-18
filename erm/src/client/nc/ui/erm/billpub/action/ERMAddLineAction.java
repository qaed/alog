package nc.ui.erm.billpub.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.costshare.common.ErmForCShareUiUtil;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillData;
import nc.ui.pub.bill.BillItem;
import nc.ui.uif2.UIState;
import nc.ui.uif2.actions.AddLineAction;
import nc.ui.uif2.editor.BillForm;
import nc.ui.uif2.model.AbstractAppModel;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;

public class ERMAddLineAction extends AddLineAction {
	private static final long serialVersionUID = 1L;

	public ERMAddLineAction() {
	}

	public void doAction(ActionEvent e) throws Exception {
		// 20170510 tsy 去除"金额小于0不需要分摊"
		// validateAddRow();
		// 20170510 end

		Object valueObject = getBillCardPanel().getHeadItem("pk_item").getValueObject();
		String currentBodyTableCode = getBillCardPanel().getCurrentBodyTableCode();
		if ((!currentBodyTableCode.equals("er_cshare_detail")) && (valueObject != null) && (!nc.vo.er.util.StringUtils.isNullWithTrim(valueObject.toString()))) {
			return;
		}

		boolean isNeedAvg = ErmForCShareUiUtil.isNeedBalanceJe(getBillCardPanel());

		super.doAction(e);

		setItemDefaultValue(getBillCardPanel().getBillData().getBodyItemsForTable(currentBodyTableCode));

		int rownum = getBillCardPanel().getRowCount() - 1;

		if (currentBodyTableCode.equals("er_cshare_detail")) {
			ErmForCShareUiUtil.afterAddOrInsertRowCsharePage(rownum, getBillCardPanel());

			if (isNeedAvg) {
				ErmForCShareUiUtil.reComputeAllJeByAvg(getBillCardPanel());
				for (int i = 0; i < getBillCardPanel().getRowCount(); i++) {
					ErmForCShareUiUtil.setRateAndAmount(i, getBillCardPanel());
				}
			} else {
				getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "assume_amount");
				getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "share_ratio");
				ErmForCShareUiUtil.setRateAndAmount(rownum, getBillCardPanel());
			}
		} else {
			List<String> keyList = new ArrayList();
			keyList.add("szxmid");
			keyList.add("jkbxr");
			keyList.add("jobid");
			keyList.add("cashproj");
			keyList.add("projecttask");
			keyList.add("pk_pcorg");
			keyList.add(JKBXHeaderVO.PK_PCORG_V);
			keyList.add("pk_checkele");
			keyList.add(JKBXHeaderVO.PK_RESACOSTCENTER);
			doCoresp(rownum, keyList, currentBodyTableCode);

			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "ybje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "cjkybje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "zfybje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "hkybje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "bbje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "cjkbbje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "zfbbje");
			getBillCardPanel().setBodyValueAt(UFDouble.ZERO_DBL, rownum, "hkbbje");

			((ErmBillBillForm) getCardpanel()).getbodyEventHandle().getBodyEventHandleUtil().doBodyReimAction();
		}
		getBillCardPanel().getBillModel().loadLoadRelationItemValue(rownum);
	}

	protected boolean isActionEnable() {
		BillItem headItem = getCardpanel().getBillCardPanel().getHeadItem("pk_item");
		Object mtAppPk = null;
		if (headItem != null) {
			mtAppPk = headItem.getValueObject();
		}

		if ((getModel() instanceof ErmBillBillManageModel)) {
			ErmBillBillManageModel model = (ErmBillBillManageModel) getModel();
			String tradeType = model.getSelectBillTypeCode();
			if ("2647".equals(tradeType)) {
				return false;
			}
		}

		return ((getModel().getUiState() == UIState.ADD) || (getModel().getUiState() == UIState.EDIT)) && (mtAppPk == null);
	}

	private boolean validateAddRow() throws BusinessException {
		if (getBillCardPanel().getCurrentBodyTableCode().equals("er_cshare_detail")) {
			UFDouble totalAmount = (UFDouble) getBillCardPanel().getHeadItem("ybje").getValueObject();
			if ((!nc.util.erm.costshare.ErmForCShareUtil.isUFDoubleGreaterThanZero(totalAmount)) && (!"20110CBSG".equals(getNodeCode())) && (!"20110CBS".equals(getNodeCode()))) {

				throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0005"));
			}
		}

		return true;
	}

	private void doCoresp(int rownum, List<String> keyList, String tablecode) {
		for (String key : keyList) {
			String value = null;
			if ((getBillCardPanel().getHeadItem(key) != null) && (getBillCardPanel().getHeadItem(key).getValueObject() != null)) {
				value = getBillCardPanel().getHeadItem(key).getValueObject().toString();
			}

			String bodyvalue = (String) getBillCardPanel().getBodyValueAt(rownum, key);
			if (bodyvalue == null) {
				getBillCardPanel().setBodyValueAt(value, rownum, key);
			}
		}
	}

	private String getNodeCode() {
		return getCardpanel().getModel().getContext().getNodeCode();
	}

	private BillCardPanel getBillCardPanel() {
		return ((ErmBillBillForm) getCardpanel()).getBillCardPanel();
	}

	protected Object getHeadValue(String key) {
		BillItem headItem = getBillCardPanel().getHeadItem(key);
		if (headItem == null) {
			headItem = getBillCardPanel().getTailItem(key);
		}
		if (headItem == null) {
			return null;
		}
		return headItem.getValueObject();
	}

	private void setItemDefaultValue(BillItem[] items) {
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				BillItem item = items[i];
				Object value = item.getDefaultValueObject();
				if (value != null) {
					item.setValue(value);
				}
			}
		}
	}
}