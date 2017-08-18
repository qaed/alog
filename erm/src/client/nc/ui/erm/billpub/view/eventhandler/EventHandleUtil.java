package nc.ui.erm.billpub.view.eventhandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;
import nc.bd.accperiod.InvalidAccperiodExcetion;
import nc.bs.erm.util.ErAccperiodUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Log;
import nc.itf.fi.pub.Currency;
import nc.itf.resa.costcenter.ICostCenterQueryOpt;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.bd.ref.model.AccPeriodDefaultRefModel;
import nc.ui.bd.ref.model.CashAccountRefModel;
import nc.ui.er.util.BXUiUtil;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.remote.BXDeptRelCostCenterCall;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.billpub.view.ErmBillBillFormHelper;
import nc.ui.erm.costshare.common.ErmForCShareUiUtil;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.UITable;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillData;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillModel;
import nc.ui.pub.bill.BillTabbedPane;
import nc.ui.uif2.ShowStatusBarMsgUtil;
import nc.ui.uif2.model.AbstractAppModel;
import nc.ui.vorg.ref.DeptVersionDefaultRefModel;
import nc.vo.arap.bx.util.BXUtil;
import nc.vo.bd.period2.AccperiodmonthVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.er.djlx.DjLXVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.er.util.UFDoubleTool;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.BusinessRuntimeException;
import nc.vo.pub.bill.BillTabVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.resa.costcenter.CostCenterVO;

public class EventHandleUtil {
	private ErmBillBillForm editor = null;
	private ErmBillBillFormHelper helper = null;

	public EventHandleUtil(ErmBillBillForm editor) {
		this.editor = editor;
		this.helper = editor.getHelper();
	}

	public String getBillHeadFinanceOrg(String orgVField, String vid, BillCardPanel editor) {
		if (editor.getHeadItem(orgVField) == null) {
			throw new BusinessRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011ermpub0316_0", "02011ermpub0316-0003"));
		}

		UIRefPane refPane = (UIRefPane) editor.getHeadItem(orgVField).getComponent();

		AbstractRefModel versionModel = refPane.getRefModel();
		return MultiVersionUtil.getBillFinanceOrg(versionModel, vid);
	}

	protected void setCostCenter(String pk_fydept, String pk_fydwbm) {
		boolean isResInstalled = BXUtil.isProductInstalled(BXUiUtil.getPK_group(), "3820");

		if (!isResInstalled) {
			return;
		}
		if (StringUtil.isEmpty(pk_fydept)) {
			return;
		}

		Map<String, CostCenterVO> map = (Map) this.helper.getCacheValue(BXDeptRelCostCenterCall.DEPT_REL_COSTCENTER);

		String key = pk_fydept;
		String pk_costcenter = null;
		if ((map == null) || (map.get(key) == null)) {
			CostCenterVO[] vos = null;
			try {
				vos =
						((ICostCenterQueryOpt) NCLocator.getInstance().lookup(ICostCenterQueryOpt.class)).queryCostCenterVOByDept(new String[] { pk_fydept });
			} catch (BusinessException e) {
				Log.getInstance(getClass()).error(e.getMessage());
				return;
			}
			if (vos != null) {
				for (CostCenterVO vo : vos) {
					if (pk_fydwbm.equals(vo.getPk_financeorg())) {
						pk_costcenter = vo.getPk_costcenter();
						break;
					}
				}
			}
			setHeadValue(JKBXHeaderVO.PK_RESACOSTCENTER, pk_costcenter);
		} else {
			CostCenterVO vo = (CostCenterVO) map.get(key);
			if (pk_fydwbm.equals(vo.getPk_financeorg())) {
				pk_costcenter = vo.getPk_costcenter();
			}
			setHeadValue(JKBXHeaderVO.PK_RESACOSTCENTER, pk_costcenter);
		}
	}

	public void filterCashAccount(String pk_currtype) {
		UIRefPane refPane = (UIRefPane) getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_CASHACCOUNT).getComponent();

		CashAccountRefModel model = (CashAccountRefModel) refPane.getRefModel();

		String prefix = "pk_moneytype=";
		String pk_org = (String) getBillCardPanel().getHeadItem("pk_org").getValueObject();

		if (StringUtil.isEmpty(pk_currtype)) {
			model.setWherePart(null);
			model.setPk_org(pk_org);
			return;
		}
		model.setPk_org(pk_org);
		model.setWherePart("pk_moneytype='" + pk_currtype + "'");
		List<String> pkValueList = new ArrayList();
		Vector vct = model.reloadData();
		Iterator<Vector> it = vct.iterator();
		int index = model.getFieldIndex("pk_cashaccount");
		while (it.hasNext()) {
			Vector next = (Vector) it.next();
			pkValueList.add((String) next.get(index));
		}
		String refPK = refPane.getRefPK();
		if (!pkValueList.contains(refPK)) {
			refPane.setPK(null);
		}
	}

	protected void setHeadValue(String key, Object value) {
		if (getBillCardPanel().getHeadItem(key) != null) {
			getBillCardPanel().getHeadItem(key).setValue(value);
		}
	}

	private BillCardPanel getBillCardPanel() {
		return this.editor.getBillCardPanel();
	}

	public String getBillHeadDept(String orgHeadItemKey, String vid) {
		if (getBillCardPanel().getHeadItem(orgHeadItemKey) == null) {
			throw new BusinessRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011ermpub0316_0", "02011ermpub0316-0002"));
		}

		UIRefPane refPane = (UIRefPane) getBillCardPanel().getHeadItem(orgHeadItemKey).getComponent();

		AbstractRefModel versionModel = refPane.getRefModel();
		if ((versionModel instanceof DeptVersionDefaultRefModel)) {
			DeptVersionDefaultRefModel model = (DeptVersionDefaultRefModel) versionModel;
			Object value = model.getValue("pk_dept");
			return (String) value;
		}
		return null;
	}

	public void setHeadNotNullValue(String itemKey, Object value) {
		if (isHeadItemExist(itemKey)) {
			getBillCardPanel().getHeadItem(itemKey).setValue(value);
		}
	}

	private boolean isHeadItemValueNull(String itemKey) {
		return (getBodyAmountValue(itemKey) == null) || (getBodyAmountValue(itemKey).toString().trim().length() == 0);
	}

	private boolean isHeadItemExist(String itemKey) {
		return getBillCardPanel().getHeadItem(itemKey) != null;
	}

	private boolean isCostShareChecked() {
		Object isCost = getBillCardPanel().getHeadItem("iscostshare").getValueObject();
		if ((isCost != null) && (isCost.toString().equals("true"))) {
			return true;
		}
		return false;
	}

	public void afterEditIsExpamt() {
		if (isExpamtChecked()) {
			String pk_org = getBillCardPanel().getHeadItem("fydwbm").getValueObject().toString();

			try {
				AccperiodmonthVO accperiodmonthVO =
						ErAccperiodUtil.getAccperiodmonthByUFDate(pk_org, (UFDate) getBillCardPanel().getHeadItem("djrq").getValueObject());
				getBillCardPanel().getHeadItem("total_period").setEnabled(true);
				getBillCardPanel().getHeadItem("start_period").setEnabled(true);
				JComponent component = getBillCardPanel().getHeadItem("total_period").getComponent();
				component.repaint();
				((AccPeriodDefaultRefModel) ((UIRefPane) getBillCardPanel().getHeadItem("start_period").getComponent()).getRefModel()).setDefaultpk_accperiodscheme(accperiodmonthVO.getPk_accperiodscheme());

				getBillCardPanel().getHeadItem("start_period").setValue(accperiodmonthVO.getPk_accperiodmonth());
			} catch (InvalidAccperiodExcetion e) {
				ExceptionHandler.handleExceptionRuntime(e);
			}
		} else if (isConfirm()) {
			getBillCardPanel().setHeadItem("start_period", null);
			getBillCardPanel().setHeadItem("total_period", null);
			getBillCardPanel().getHeadItem("total_period").setEnabled(false);
			getBillCardPanel().getHeadItem("start_period").setEnabled(false);
		} else {
			getBillCardPanel().getHeadItem("isexpamt").setValue(Boolean.valueOf(true));
		}
	}

	private boolean isConfirm() {
		if (MessageDialog.showYesNoDlg(this.editor, NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0038"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2006030102", "UPP2006030102-000386")) == 4) {

			return true;
		}
		ShowStatusBarMsgUtil.showStatusBarMsg(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006030102", "UPP2006030102-000385"), this.editor.getModel().getContext());

		return false;
	}

	private boolean isExpamtChecked() {
		Object isExpamt = getBillCardPanel().getHeadItem("isexpamt").getValueObject();
		if ((isExpamt != null) && (isExpamt.toString().equals("true"))) {
			return true;
		}
		return false;
	}

	public void afterEditIsCostShare() {
		if (isCostShareChecked()) {
			UFDouble totalJe = new UFDouble(getBillCardPanel().getHeadItem("ybje").getValueObject().toString());

			/*
			 * 20170510 tsy 去除"小于0不能进行分摊"校验 if
			 * (totalJe.compareTo(UFDouble.ZERO_DBL) < 0) {
			 * ShowStatusBarMsgUtil.
			 * showErrorMsg(NCLangRes4VoTransl.getNCLangRes(
			 * ).getStrByID("201107_0", "0201107-0150"),
			 * NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0",
			 * "0201107-0007"), this.editor.getModel().getContext());
			 * 
			 * getBillCardPanel().getHeadItem("iscostshare").setValue(UFBoolean.
			 * FALSE); return; }
			 */
			ErmForCShareUiUtil.setCostPageShow(getBillCardPanel(), true);

			BillItem[] items = getBillCardPanel().getBillData().getBillModel("er_cshare_detail").getBodyItems();
			if (items != null) {
				for (BillItem item : items) {
					if ((item.isShow()) && ((item.getComponent() instanceof UIRefPane))) {
						UIRefPane refPane = (UIRefPane) item.getComponent();
						refPane.setMultiSelectedEnabled(true);
					}
				}
			}

			getBillCardPanel().getHeadItem(JKBXHeaderVO.FYDWBM_V).getComponent().setEnabled(false);

			setTabbedPaneSelected("er_cshare_detail");
		} else {
			int count = getBillCardPanel().getBillTable("er_cshare_detail").getRowCount();
			if (count > 0) {
				ShowStatusBarMsgUtil.showErrorMsg(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0149"), NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0008"), this.editor.getModel().getContext());

				getBillCardPanel().getHeadItem("iscostshare").setValue(UFBoolean.TRUE);
				return;
			}

			ErmForCShareUiUtil.setCostPageShow(getBillCardPanel(), false);
			getBillCardPanel().getHeadItem(JKBXHeaderVO.FYDWBM_V).getComponent().setEnabled(true);
		}
	}

	private void setTabbedPaneSelected(String tableCode) {
		BillTabbedPane tabPane = getBillCardPanel().getBodyTabbedPane();
		int index = tabPane.indexOfComponent(getBillCardPanel().getBodyPanel(tableCode));

		if (index >= 0) {
			getBillCardPanel().getBodyTabbedPane().setSelectedIndex(index);
		}
	}

	public void setHeadYFB() throws BusinessException {
		UFDouble total = new UFDouble(0);
		if (getBillCardPanel().getHeadItem("total") != null) {
			total = setHeadAmountValue("total");
		} else if (getBillCardPanel().getHeadItem("ybje") != null) {
			total = setHeadAmountValue("ybje");
		}
		String bzbm = "null";
		if (getBillCardPanel().getHeadItem("bzbm").getValueObject() != null) {
			bzbm = getBillCardPanel().getHeadItem("bzbm").getValueObject().toString();
		}
		UFDouble hl = getBodyAmountValue("bbhl");
		UFDouble globalhl = getBodyAmountValue("globalbbhl");
		UFDouble grouphl = getBodyAmountValue("groupbbhl");
		String pk_group = null;
		if (getBillCardPanel().getHeadItem("pk_group").getValueObject() != null) {
			pk_group = getBillCardPanel().getHeadItem("pk_group").getValueObject().toString();
		}
		if (getPk_org() != null) {
			String org = getBillCardPanel().getHeadItem("pk_org").getValueObject().toString();
			UFDouble[] je = Currency.computeYFB(getPk_org(), 1, bzbm, total, null, null, null, hl, BXUiUtil.getSysdate());
			getBillCardPanel().setHeadItem("ybje", je[0]);
			getBillCardPanel().setHeadItem("bbje", je[2]);
			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, BXUiUtil.getSysdate(), org, pk_group, globalhl, grouphl);

			int globalRateDigit = Currency.getCurrDigit(Currency.getGlobalCurrPk(getPk_org()));
			if (globalRateDigit == 0) {
				globalRateDigit = 2;
			}

			int groupRateDigit = Currency.getCurrDigit(Currency.getGroupCurrpk(pk_group));
			if (globalRateDigit == 0) {
				globalRateDigit = 2;
			}

			getBillCardPanel().setHeadItem("groupbbje", money[0]);
			getBillCardPanel().setHeadItem("globalbbje", money[1]);
			getBillCardPanel().getHeadItem("groupbbje").setDecimalDigits(groupRateDigit);
			getBillCardPanel().getHeadItem("globalbbje").setDecimalDigits(globalRateDigit);
		}
		// 20170713 tsy 设置原币金额和本币金额为含税金额
		if (getBillCardPanel().getHeadItem("zyx12") != null) {
			total = (UFDouble) getBillCardPanel().getHeadItem("zyx12").getValueObject();
		}
		// 20170713 end

		resetCjkjeAndYe(total, bzbm, hl);

		ErmForCShareUiUtil.reComputeAllJeByRatio(getBillCardPanel());
	}

	public void resetHeadYFB() throws BusinessException {
		UFDouble total = UFDouble.ZERO_DBL;
		if (getBillCardPanel().getHeadItem("total") != null) {
			total = setHeadAmountValue("total");
		} else if (getBillCardPanel().getHeadItem("ybje") != null) {
			total = setHeadAmountValue("ybje");
		}
		String bzbm = "null";
		if (getBillCardPanel().getHeadItem("bzbm").getValueObject() != null) {
			bzbm = getBillCardPanel().getHeadItem("bzbm").getValueObject().toString();
		}
		UFDouble hl = getBodyAmountValue("bbhl");
		String pk_group = null;
		if (getBillCardPanel().getHeadItem("pk_group").getValueObject() != null) {
			pk_group = getBillCardPanel().getHeadItem("pk_group").getValueObject().toString();
		}
		if (getPk_org() != null) {
			UFDouble ybje = UFDouble.ZERO_DBL;
			UFDouble bbje = UFDouble.ZERO_DBL;
			UFDouble groupbbje = UFDouble.ZERO_DBL;
			UFDouble groupzfbbje = UFDouble.ZERO_DBL;
			UFDouble globalbbje = UFDouble.ZERO_DBL;
			UFDouble globalzfbbje = UFDouble.ZERO_DBL;

			BillTabVO[] billTabVOs = getBillCardPanel().getBillData().getBillTabVOs(1);

			for (BillTabVO billTabVO : billTabVOs) {
				String metadatapath = billTabVO.getMetadatapath();
				if (("er_busitem".equals(metadatapath)) || ("jk_busitem".equals(metadatapath)) || (metadatapath == null)) {
					String tabcode = billTabVO.getTabcode();
					int ybjeCol = getBillCardPanel().getBodyColByKey(tabcode, "ybje");
					int bbjeCol = getBillCardPanel().getBodyColByKey(tabcode, "bbje");
					int groupbbjeCol = getBillCardPanel().getBodyColByKey(tabcode, "groupbbje");
					int groupzfbbjeCol = getBillCardPanel().getBodyColByKey(tabcode, "groupzfbbje");
					int globalbbjeCol = getBillCardPanel().getBodyColByKey(tabcode, "globalbbje");
					int globalzfbbjeCol = getBillCardPanel().getBodyColByKey(tabcode, "globalzfbbje");

					UFDouble ybjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, ybjeCol);
					UFDouble bbjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, bbjeCol);
					UFDouble groupbbjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, groupbbjeCol);
					UFDouble groupzfbbjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, groupzfbbjeCol);
					UFDouble globalbbjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, globalbbjeCol);
					UFDouble globalzfbbjeValue =
							(UFDouble) getBillCardPanel().getBillData().getBillModel(tabcode).getTotalTableModel().getValueAt(0, globalzfbbjeCol);

					if (ybjeValue != null) {
						ybje = ybje.add(ybjeValue);
					}
					if (bbjeValue != null) {
						bbje = bbje.add(bbjeValue);
					}
					if (groupbbjeValue != null) {
						groupbbje = groupbbje.add(groupbbjeValue);
					}
					if (globalbbjeValue != null) {
						globalbbje = globalbbje.add(globalbbjeValue);
					}
					if (groupzfbbjeValue != null) {
						groupzfbbje = groupzfbbje.add(groupzfbbjeValue);
					}
					if (globalzfbbjeValue != null) {
						globalzfbbje = globalzfbbje.add(globalzfbbjeValue);
					}
				}
			}

			int globalRateDigit = Currency.getCurrDigit(Currency.getGlobalCurrPk(getPk_org()));
			if (globalRateDigit == 0) {
				globalRateDigit = 2;
			}

			int groupRateDigit = Currency.getCurrDigit(Currency.getGroupCurrpk(pk_group));
			if (globalRateDigit == 0) {
				globalRateDigit = 2;
			}

			getBillCardPanel().setHeadItem("ybje", ybje);
			getBillCardPanel().setHeadItem("bbje", bbje);
			getBillCardPanel().setHeadItem("groupbbje", groupbbje);
			getBillCardPanel().setHeadItem("groupzfbbje", groupzfbbje);
			getBillCardPanel().setHeadItem("globalbbje", globalbbje);
			getBillCardPanel().setHeadItem("globalzfbbje", globalzfbbje);
			getBillCardPanel().getHeadItem("groupbbje").setDecimalDigits(groupRateDigit);
			getBillCardPanel().getHeadItem("groupzfbbje").setDecimalDigits(groupRateDigit);
			getBillCardPanel().getHeadItem("globalbbje").setDecimalDigits(globalRateDigit);
			getBillCardPanel().getHeadItem("globalzfbbje").setDecimalDigits(globalRateDigit);
		}
		resetCjkjeAndYe(total, bzbm, hl);

		ErmForCShareUiUtil.reComputeAllJeByRatio(getBillCardPanel());
	}

	private UFDouble setHeadAmountValue(String total2) {
		Object valueObject = getBillCardPanel().getHeadItem(total2).getValueObject();
		UFDouble total = valueObject == null ? UFDouble.ZERO_DBL : (UFDouble) valueObject;
		return total;
	}

	private UFDouble getBodyAmountValue(String bbhl) {
		Object valueObject = getBillCardPanel().getHeadItem(bbhl).getValueObject();
		return (UFDouble) (valueObject == null ? null : valueObject);
	}

	public void resetCjkjeAndYe(UFDouble total, String bzbm, UFDouble hl) throws BusinessException {
		UFDouble[] je = null;

		UFDouble cjkybje = UFDouble.ZERO_DBL;
		BillItem item = getBillCardPanel().getHeadItem("cjkybje");
		if ((item != null) && (item.getValueObject() != null)) {
			cjkybje = new UFDouble(item.getValueObject().toString());
		}
		BillModel billModel = getBillCardPanel().getBillModel("er_bxcontrast");
		BxcontrastVO[] bxcontrastVO =
				billModel == null ? null : (BxcontrastVO[]) billModel.getBodyValueChangeVOs(BxcontrastVO.class.getName());
		if ((bxcontrastVO != null) && (bxcontrastVO.length > 0)) {
			je = Currency.computeYFB(getPk_org(), 1, bzbm, cjkybje, null, null, null, hl, BXUiUtil.getSysdate());

			getBillCardPanel().setHeadItem("cjkybje", je[0]);
			getBillCardPanel().setHeadItem("cjkbbje", je[2]);
			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, BXUiUtil.getSysdate(), getBillCardPanel().getHeadItem("pk_org").getValueObject().toString(), getBillCardPanel().getHeadItem("pk_group").getValueObject().toString(), null, null);

			getBillCardPanel().setHeadItem("groupcjkbbje", money[0]);
			getBillCardPanel().setHeadItem("globalcjkbbje", money[1]);
		}
		boolean isJK = getBillCardPanel().getBillData().getHeadItem("djdl").getValueObject().equals("jk");
		if (!isJK) {
			JKBXHeaderVO jkHead = ((JKBXVO) this.editor.getValue()).getParentVO();
			if (UFDoubleTool.isZero(cjkybje)) {
				if (jkHead.getYbje().doubleValue() > 0.0D) {
					setJe(jkHead, total, new String[] { "zfybje", "zfbbje", "groupzfbbje", "globalzfbbje" });

					setHeadValues(new String[] { "hkybje", "hkbbje" }, new Object[] { null, null });
				} else {
					setJe(jkHead, total, new String[] { "hkybje", "hkbbje", "grouphkbbje", "globalhkbbje" });

					setHeadValues(new String[] { "zfybje", "zfbbje" }, new Object[] { null, null });
				}
			} else if (cjkybje.doubleValue() >= total.doubleValue()) {
				setJe(jkHead, cjkybje.sub(total), new String[] { "hkybje", "hkbbje", "grouphkbbje", "globalhkbbje" });

				setHeadValues(new String[] { "zfybje", "zfbbje" }, new Object[] { null, null });
			} else if (cjkybje.doubleValue() < total.doubleValue()) {
				setJe(jkHead, total.sub(cjkybje), new String[] { "zfybje", "zfbbje", "groupzfbbje", "globalzfbbje" });

				setHeadValues(new String[] { "hkybje", "hkbbje" }, new Object[] { null, null });
			}
		} else {
			Object ybje = getHeadValue("ybje");
			Object bbje = getHeadValue("bbje");
			Object groupje = getHeadValue("groupbbje");
			Object globalje = getHeadValue("globalbbje");
			setHeadValues(new String[] { "zfybje", "zfbbje", "groupzfbbje", "globalzfbbje" }, new Object[] { ybje, bbje, groupje, globalje });
		}

		getBillCardPanel().setHeadItem("ybye", getBillCardPanel().getHeadItem("ybje").getValueObject());
		getBillCardPanel().setHeadItem("bbye", getBillCardPanel().getHeadItem("bbje").getValueObject());
	}

	public void setJe(JKBXHeaderVO jkHead, UFDouble cjkybje, String[] yfbKeys) throws BusinessException {
		try {
			UFDouble newCjkybje = null;
			//20170713 tsy
			if (cjkybje == null ) {
				return;
			}
			if (cjkybje.doubleValue() < 0.0D) {
				newCjkybje = cjkybje.abs();
			} else {
				newCjkybje = cjkybje;
			}
			//20170713 end
			setHeadValue(yfbKeys[0], newCjkybje);
			if ((getPk_org() != null) && (jkHead.getBzbm() != null)) {
				UFDouble[] yfbs =
						Currency.computeYFB(getPk_org(), 2, jkHead.getBzbm(), newCjkybje, null, null, null, jkHead.getBbhl(), jkHead.getDjrq());

				UFDouble[] money =
						Currency.computeGroupGlobalAmount(newCjkybje, yfbs[2], jkHead.getBzbm(), BXUiUtil.getSysdate(), getPk_org(), getBillCardPanel().getHeadItem("pk_group").getValueObject().toString(), null, null);

				setHeadValue(yfbKeys[1], yfbs[2]);
				setHeadValue(yfbKeys[2], money[0]);
				setHeadValue(yfbKeys[3], money[1]);
			}
		} catch (BusinessException e) {
			Log.getInstance(getClass()).error(e.getMessage(), e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000009"));
		}
	}

	public void setHeadYfbByHead() {
		Object valueObject = getBillCardPanel().getHeadItem("ybje").getValueObject();

		if ((valueObject == null) || (valueObject.toString().trim().length() == 0)) {
			return;
		}
		UFDouble newYbje = new UFDouble(valueObject.toString());
		try {
			String bzbm = "null";
			if (getHeadValue("bzbm") != null) {
				bzbm = getHeadValue("bzbm").toString();
			}

			UFDouble hl = null;

			UFDouble globalhl =
					getBillCardPanel().getHeadItem("globalbbhl").getValueObject() != null ? new UFDouble(getBillCardPanel().getHeadItem("globalbbhl").getValueObject().toString()) : null;

			UFDouble grouphl =
					getBillCardPanel().getHeadItem("groupbbhl").getValueObject() != null ? new UFDouble(getBillCardPanel().getHeadItem("groupbbhl").getValueObject().toString()) : null;

			if (getBillCardPanel().getHeadItem("bbhl").getValueObject() != null) {
				hl = new UFDouble(getBillCardPanel().getHeadItem("bbhl").getValueObject().toString());
			}

			UFDouble[] je = Currency.computeYFB(getPk_org(), 1, bzbm, newYbje, null, null, null, hl, BXUiUtil.getSysdate());

			getBillCardPanel().setHeadItem("ybje", je[0]);
			getBillCardPanel().setHeadItem("bbje", je[2]);

			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, BXUiUtil.getSysdate(), getBillCardPanel().getHeadItem("pk_org").getValueObject().toString(), getBillCardPanel().getHeadItem("pk_group").getValueObject().toString(), globalhl, grouphl);

			DjLXVO currentDjlx = ((ErmBillBillManageModel) this.editor.getModel()).getCurrentDjLXVO();
			if ("jk".equals(currentDjlx.getDjdl())) {
				getBillCardPanel().setHeadItem("total", je[0]);
			}
			getBillCardPanel().setHeadItem("groupbbje", money[0]);
			getBillCardPanel().setHeadItem("globalbbje", money[1]);
			getBillCardPanel().setHeadItem("groupbbhl", money[2]);
			getBillCardPanel().setHeadItem("globalbbhl", money[3]);

			resetCjkjeAndYe(je[0], bzbm, hl);

			ErmForCShareUiUtil.reComputeAllJeByRatio(getBillCardPanel());
		} catch (BusinessException e) {
			ExceptionHandler.consume(e);
		}
	}

	protected void setHeadValues(String[] key, Object[] value) {
		for (int i = 0; i < value.length; i++) {
			getBillCardPanel().getHeadItem(key[i]).setValue(value[i]);
		}
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

	public String getPk_org() {
		if (!this.editor.isShowing())
			return null;
		if (this.editor.isShowing()) {
			return (String) getBillCardPanel().getHeadItem("pk_org").getValueObject();
		}

		return BXUiUtil.getBXDefaultOrgUnit();
	}
}