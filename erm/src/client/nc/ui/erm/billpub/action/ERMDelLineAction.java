package nc.ui.erm.billpub.action;

import java.awt.event.ActionEvent;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.costshare.common.ErmForCShareUiUtil;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillModel;
import nc.ui.pub.bill.BillScrollPane;
import nc.ui.uif2.actions.DelLineAction;
import nc.ui.uif2.editor.BillForm;
import nc.vo.ep.bx.BXHeaderVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.ValidationException;
import nc.vo.pub.lang.UFDouble;

public class ERMDelLineAction extends DelLineAction {
	private static final long serialVersionUID = 1L;

	public ERMDelLineAction() {
	}

	public void doAction(ActionEvent e) throws Exception {
		boolean isNeedAvg = ErmForCShareUiUtil.isNeedBalanceJe(getBillCardPanel());

		if (getBillCardPanel().getBillModel().getRowCount() != 0) {
			// 20170510 tsy 去除"金额小于0不需要分摊"
			// validateAddRow();
			// 20170510 end
			boolean isNeed = isNeedContrast();

			super.doAction(e);

			if (isNeed) {
				doContract();
			}

			((ErmBillBillForm) getCardpanel()).getbodyEventHandle().resetJeAfterModifyRow();

			if (getBillCardPanel().getCurrentBodyTableCode().equals("er_cshare_detail")) {
				if (getBillCardPanel().getBillModel().getRowCount() == 0) {
					int result =
							nc.ui.pub.beans.MessageDialog.showYesNoDlg(getCardpanel(), NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0038"), NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0006"));

					if (result == 4) {
						if (getBillCardPanel().getHeadItem("iscostshare") != null) {
							getBillCardPanel().getHeadItem("iscostshare").setValue(nc.vo.pub.lang.UFBoolean.FALSE);
						}
						ErmForCShareUiUtil.setCostPageShow(getBillCardPanel(), false);
						getBillCardPanel().getHeadItem(BXHeaderVO.FYDWBM_V).getComponent().setEnabled(true);
					}
				} else if (isNeedAvg) {
					ErmForCShareUiUtil.reComputeAllJeByAvg(getBillCardPanel());
					for (int i = 0; i < getBillCardPanel().getRowCount(); i++) {
						ErmForCShareUiUtil.setRateAndAmount(i, getBillCardPanel());
					}
				}
			}
		}
	}

	private void doContract() throws ValidationException, BusinessException {
		BxcontrastVO[] bxcontrastVO =
				(BxcontrastVO[]) getBillCardPanel().getBillModel("er_bxcontrast").getBodyValueVOs(BxcontrastVO.class.getName());

		if ((bxcontrastVO != null) && (bxcontrastVO.length > 0)) {
			ContrastAction.doContrastToUI(getBillCardPanel(), ((ErmBillBillForm) getCardpanel()).getHelper().getJKBXVO(getCardpanel()), java.util.Arrays.asList(bxcontrastVO), (ErmBillBillForm) getCardpanel());
		}
	}

	private boolean isNeedContrast() {
		String tableCode = getBillCardPanel().getCurrentBodyTableCode();
		BillScrollPane bsp = getBillCardPanel().getBodyPanel(tableCode);
		int selectedRow = bsp.getTable().getSelectedRow();

		UFDouble cjkybje = (UFDouble) getBillCardPanel().getBodyValueAt(selectedRow, "cjkybje");

		if ((!tableCode.equals("er_bxcontrast")) && (cjkybje != null) && (cjkybje.compareTo(UFDouble.ZERO_DBL) > 0)) {
			return true;
		}

		return false;
	}

	private boolean validateAddRow() throws BusinessException {
		if (getBillCardPanel().getCurrentBodyTableCode().equals("er_cshare_detail")) {
			UFDouble totalAmount = (UFDouble) getBillCardPanel().getHeadItem("ybje").getValueObject();
			if ((!nc.util.erm.costshare.ErmForCShareUtil.isUFDoubleGreaterThanZero(totalAmount)) && (!"20110CBSG".equals(getNodeCode())) && (!"20110CBS".equals(getNodeCode()))) {

				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0005"));
			}
		}

		return true;
	}

	private String getNodeCode() {
		return getCardpanel().getModel().getContext().getNodeCode();
	}

	private BillCardPanel getBillCardPanel() {
		return getCardpanel().getBillCardPanel();
	}

	protected boolean isActionEnable() {
		if ((getModel() instanceof ErmBillBillManageModel)) {
			ErmBillBillManageModel model = (ErmBillBillManageModel) getModel();
			String tradeType = model.getSelectBillTypeCode();
			if ("2647".equals(tradeType)) {
				return false;
			}
		}
		return super.isActionEnable();
	}
}