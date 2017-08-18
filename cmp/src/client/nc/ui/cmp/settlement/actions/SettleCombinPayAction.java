package nc.ui.cmp.settlement.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.cmp.settlement.validate.SettleValidate;
import nc.cmp.utils.CmpUtils;
import nc.cmp.utils.SettleUtils;
import nc.itf.cmp.pub.CmpSelfDefButtonNameConst;
import nc.itf.cmp.settlement.ISettlementQueryService;
import nc.ui.cmp.netpayment.PaymentProc;
import nc.ui.cmp.settlement.view.SettlementCard;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.tmpub.security.DefCommonClientSign;
import nc.ui.uif2.ShowStatusBarMsgUtil;
import nc.ui.uif2.model.AbstractAppModel;
import nc.vo.cmp.BusiStatus;
import nc.vo.cmp.SettleStatus;
import nc.vo.cmp.settlement.SettleEnumCollection;
import nc.vo.cmp.settlement.SettleEnumCollection.Direction;
import nc.vo.cmp.settlement.SettlementAggVO;
import nc.vo.cmp.settlement.SettlementBodyVO;
import nc.vo.cmp.settlement.SettlementHeadVO;
import nc.vo.cmp.settlement.international.util.SettleInternationalValidate;
import nc.vo.cmp.validate.CMPValidate;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;

public class SettleCombinPayAction extends SettleDefaultAction {
	private static final long serialVersionUID = -4257102182028309094L;

	public SettleCombinPayAction() {
		setBtnName(CmpSelfDefButtonNameConst.getInstance().getConstBtnnameCombinpayName());
		putValue("ShortDescription", getBtnName());
		putValue("Code", "ºÏ²¢Ö§¸¶");
	}

	public void doAction(ActionEvent e) throws Exception {
		SettlementAggVO[] aggvos = null;
		try {
			Object obj = getValue();
			if (obj.getClass().isArray()) {
				aggvos = (SettlementAggVO[]) obj;
			} else {
				aggvos = new SettlementAggVO[] { (SettlementAggVO) obj };
			}
			List<SettlementAggVO> aggLst = SettleUtils.filterSettleInfo4NetSettleFlagUnSettle(aggvos);
			if (aggLst.size() == 0) {
				ShowStatusBarMsgUtil.showErrorMsg(NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set1_0", "03607set1-0078"), NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set1_0", "03607set1-0079"), getModel().getContext());
				return;
			}
			aggvos = (SettlementAggVO[]) aggLst.toArray(new SettlementAggVO[0]);
			validate(aggvos);
//			getEdit().getClientSigner().checkIfCASign();
			int i =
					MessageDialog.showYesNoDlg(getEdit(), NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set_0", "03607set-0029"), NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set_0", "03607set-0050"));
			if (i != 4) {
				return;
			}
			Integer doZhifu = new PaymentProc().doZhifu(getEdit(), aggvos);
			if (doZhifu.intValue() == -1) {
				return;
			}
			List<String> pkLst = CmpUtils.makeList();
			for (SettlementAggVO aggvo : aggvos) {
				pkLst.add(aggvo.getParentVO().getPrimaryKey());
			}
			aggvos =
					((ISettlementQueryService) NCLocator.getInstance().lookup(ISettlementQueryService.class)).querySettlementAggVOsByPks((String[]) pkLst.toArray(new String[0]));
			if (!isListSelected()) {
				getEdit().setLoadBean(aggvos[0]);
			}
			getModel().directlyUpdate(aggvos);
			getEdit().setValue(aggvos[0]);
			ShowStatusBarMsgUtil.showStatusBarMsg(NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set_0", "03607set-0051"), getModel().getContext());
		} catch (BusinessException be) {
			Logger.error(be.getMessage(), be);
			throw new BusinessException(be.getMessage());
		}
	}

	private void validate(SettlementAggVO[] aggvos) throws BusinessException {
		validateSalaryBill(aggvos);
		SettleValidate.validatePayData(aggvos);
		SettleValidate.validateOBM(aggvos);
		SettleValidate.validateNetPayNegativeRow(aggvos);
		SettleValidate.validateNetPayState(aggvos);
		SettleValidate.validateSupplier(aggvos);
		SettleInternationalValidate.checkInternationalNotSupportedOperate(aggvos);
		CMPValidate.validate(aggvos);
	}

	private void validateSalaryBill(SettlementAggVO[] aggvos) throws BusinessException {
		StringBuffer sbmsg = new StringBuffer();
		for (SettlementAggVO settlementAggVO : aggvos) {
			SettlementHeadVO headvo = (SettlementHeadVO) settlementAggVO.getParentVO();
			String pk_tradetype = headvo.getPk_tradetype();
			if ("DS".equals(pk_tradetype)) {
				sbmsg.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("3607set1_0", "03607set1-0221", null, new String[] { headvo.getBillcode() }));
			}
		}
		if (sbmsg.length() > 0) {
			throw new BusinessException(sbmsg.toString());
		}
	}

	protected boolean isActionEnable() {
		SettlementAggVO[] aggvos = getSelectedAggVOs();
		if ((aggvos == null) || (aggvos.length == 0) || (aggvos[0] == null)) {
			return false;
		}
		for (SettlementAggVO settlementAggVO : aggvos) {
			SettlementAggVO currentAggvo = settlementAggVO;
			SettlementHeadVO head = (SettlementHeadVO) currentAggvo.getParentVO();
			if (SettleEnumCollection.Direction.REC.VALUE.equals(head.getDirection())) {
				return false;
			}
			if ((head.getBusistatus() == null) || (!head.getBusistatus().equals(Integer.valueOf(BusiStatus.Sign.getBillStatusKind())))) {
				return false;
			}
			if ("DS".equals(head.getPk_tradetype())) {
				return false;
			}
			if (head.getPk_ftsbill() != null) {
				return false;
			}
			if (SettleUtils.isExistInnerAccount(currentAggvo)) {
				return false;
			}
			for (SettlementBodyVO body : (SettlementBodyVO[]) settlementAggVO.getChildrenVO()) {
				if ((body.getSettlestatus() != null) && (body.getSettlestatus().equals(Integer.valueOf(SettleStatus.SETTLERESET.getStatus())))) {
					return false;
				}
			}
		}
		return super.isActionEnable();
	}
}