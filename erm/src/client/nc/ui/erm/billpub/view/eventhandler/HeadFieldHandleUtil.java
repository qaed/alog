package nc.ui.erm.billpub.view.eventhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Log;
import nc.pubitf.uapbd.ISupplierPubService_C;
import nc.ui.bd.ref.AbstractRefGridTreeModel;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.bd.ref.IFilterCommonDataVec;
import nc.ui.bd.ref.model.BankaccSubDefaultRefModel;
import nc.ui.bd.ref.model.CashAccountRefModel;
import nc.ui.bd.ref.model.CustBankaccDefaultRefModel;
import nc.ui.bd.ref.model.FreeCustRefModel;
import nc.ui.bd.ref.model.PsnbankaccDefaultRefModel;
import nc.ui.dbcache.DBCacheFacade;
import nc.ui.er.util.BXUiUtil;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.util.ErUiUtil;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillItem;
import nc.ui.vorg.ref.DeptVersionDefaultRefModel;
import nc.ui.vorg.ref.FinanceOrgVersionDefaultRefTreeModel;
import nc.vo.bd.supplier.SupplierVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.djlx.DjLXVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.org.OrgQueryUtil;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;

public class HeadFieldHandleUtil {
	private ErmBillBillForm editor = null;

	public HeadFieldHandleUtil(ErmBillBillForm editor) {
		this.editor = editor;
	}

	public void initProj() {
		UIRefPane refPane = getHeadItemUIRefPane("jobid");
		String pk_org = getHeadItemStrValue("fydwbm");
		if (pk_org == null) {
			refPane.setEnabled(false);
		} else {
			refPane.setEnabled(true);
		}
		refPane.getRefModel().setPk_org(pk_org);
	}

	public void initFreeCust() {
		UIRefPane refPane = getHeadItemUIRefPane("freecust");

		String pk_supplier = getHeadItemStrValue("hbbm");

		if ((pk_supplier != null) && (pk_supplier.trim().length() > 0)) {
			try {
				SupplierVO[] supplierVO =
						((ISupplierPubService_C) NCLocator.getInstance().lookup(ISupplierPubService_C.class)).getSupplierVO(new String[] { pk_supplier }, new String[] { "isfreecust" });
				if ((supplierVO != null) && (supplierVO.length != 0) && (supplierVO[0].getIsfreecust().equals(UFBoolean.TRUE))) {
					getBillCardPanel().getHeadItem("freecust").setEnabled(true);
					((FreeCustRefModel) refPane.getRefModel()).setCustomSupplier(pk_supplier);
				} else {
					getBillCardPanel().getHeadItem("freecust").setEnabled(false);
				}
			} catch (BusinessException e) {
				ExceptionHandler.handleExceptionRuntime(e);
			}
		} else {
			getBillCardPanel().getHeadItem("freecust").setEnabled(false);
		}
	}

	public void initCashProj() {
		UIRefPane ref = getHeadItemUIRefPane("cashproj");

		String pk_org = null;
		if ("20110RB".equals(this.editor.getModel().getContext().getNodeCode())) {
			pk_org = (String) getBillCardPanel().getHeadItem("pk_org").getValueObject();
		} else {
			pk_org = (String) getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_PAYORG).getValueObject();
		}
		ref.getRefModel().setPk_org(pk_org);
		ref.getRefModel().addWherePart(" and inoutdirect = '1' ", false);
	}

	public void initPk_Checkele() {
		UIRefPane refPane = getHeadItemUIRefPane("pk_checkele");
		String pk_pcorg = getHeadItemStrValue("pk_pcorg");
		if (pk_pcorg != null) {
			refPane.setEnabled(true);
			setPkOrg2RefModel(refPane, pk_pcorg);
		} else {
			refPane.setPK(null);
			refPane.setEnabled(false);
		}
	}

	public void beforeEditPkOrg_v(String vOrgField) {
		UFDate date = (UFDate) getBillCardPanel().getHeadItem("djrq").getValueObject();
		if (this.editor.isInit()) {

			date = new UFDate("3000-01-01");

		} else if ((date == null) || (StringUtil.isEmpty(date.toString()))) {
			date = BXUiUtil.getBusiDate();
		}

		UIRefPane refPane = getHeadItemUIRefPane(vOrgField);
		FinanceOrgVersionDefaultRefTreeModel model = (FinanceOrgVersionDefaultRefTreeModel) refPane.getRefModel();
		model.setVstartdate(date);

		String refPK = refPane.getRefPK();
		String[] pk_vids = ErUiUtil.getPermissionOrgVs(this.editor.getModel().getContext(), date);
		if (pk_vids == null) {
			pk_vids = new String[0];
		}

		ErUiUtil.setRefFilterPks(refPane.getRefModel(), pk_vids);
		List<String> list = Arrays.asList(pk_vids);
		if (list.contains(refPK)) {
			refPane.setPK(refPK);
		} else {
			refPane.setPK(null);
		}
	}

	public void beforeEditDept_v(String pk_org, String vDeptField) {
		UIRefPane refPane = getHeadItemUIRefPane(vDeptField);
		DeptVersionDefaultRefModel model = (DeptVersionDefaultRefModel) refPane.getRefModel();
		UFDate date = (UFDate) getBillCardPanel().getHeadItem("djrq").getValueObject();
		if (date == null) {
			date = BXUiUtil.getBusiDate();
		}
		model.setVstartdate(date);
		model.setPk_org(pk_org);
	}

	public void initJkbxr() {
		if (!this.editor.isInit()) {
			try {
				BillItem headItem = this.editor.getBillCardPanel().getHeadItem("jkbxr");
				initSqdlr(this.editor, headItem, ((ErmBillBillManageModel) this.editor.getModel()).getCurrentBillTypeCode(), getBillCardPanel().getHeadItem("dwbm"));
			} catch (BusinessException e) {
				Log.getInstance(getClass()).error(e);
			}
		}
	}

	public static void initSqdlr(ErmBillBillForm editor, BillItem headItem, String billtype, BillItem headOrg) throws BusinessException {
		if (headItem == null)
			return;
		String refType = headItem.getRefType();
		if (refType == null)
			return;
		String pk_org = "";
		if ((headOrg != null) && (headOrg.getValueObject() != null)) {
			pk_org = headOrg.getValueObject().toString();
		}
		String wherePart = BXUiUtil.getAgentWhereString(billtype, BXUiUtil.getPk_user(), BXUiUtil.getSysdate().toString(), pk_org);

		String newWherePart = "1=1 " + wherePart;
		UIRefPane refPane = (UIRefPane) editor.getBillCardPanel().getHeadItem("jkbxr").getComponent();
		final AbstractRefGridTreeModel model = (AbstractRefGridTreeModel) refPane.getRefModel();

		model.setPk_org(pk_org);
		model.setWherePart(newWherePart);

		model.setFilterCommonDataVec(new IFilterCommonDataVec() {
			public void filterCommonDataVec(Vector vec) {
				if ((vec == null) || (vec.isEmpty())) {
					return;
				}
				String sql = model.getRefSql();
				Vector<Vector<String>> vers = (Vector<Vector<String>>) DBCacheFacade.getFromDBCache(sql);

				if ((vers == null) || (vers.isEmpty())) {
					vec.removeAllElements();
					return;
				}

				Set<String> jkbxrdata = new HashSet();
				for (Vector<String> ve : vers) {
					jkbxrdata.add(ve.get(2));
				}
				Vector removed = new Vector();
				for (Object data : vec) {
					Vector<Object> ve = (Vector) data;
					String pk_psndoc = (String) ve.get(2);
					if (!jkbxrdata.contains(pk_psndoc)) {
						removed.addElement(ve);
					}
				}
				if (removed.size() > 0) {
					vec.removeAll(removed);
				}
			}
		});
	}

	public void initResaCostCenter() {
		String pk_fydwbm = getHeadItemStrValue("fydwbm");
		UIRefPane refPane = getHeadItemUIRefPane(JKBXHeaderVO.PK_RESACOSTCENTER);
		String wherePart = "pk_financeorg='" + pk_fydwbm + "'";
		addWherePart2RefModel(refPane, pk_fydwbm, wherePart);
	}

	public void initSkyhzh() {
		String filterStr = null;
		if (isJk()) {
			filterStr = getHeadItemStrValue("jkbxr");
			getBillCardPanel().setHeadItem("skyhzh", null);
		}
		String pk_currtype = getHeadItemStrValue("bzbm");
		if ((filterStr != null) && (filterStr.trim().length() > 0)) {
			UIRefPane refPane = getHeadItemUIRefPane("skyhzh");
			String wherepart = " pk_psndoc='" + filterStr + "'";
			wherepart = wherepart + " and pk_currtype='" + pk_currtype + "'";
			setWherePart2RefModel(refPane, getHeadItemStrValue("dwbm"), wherepart);
		}
	}

	public void editReceiver() {
		String receiver = getHeadItemStrValue("receiver");
		String pk_currtype = getHeadItemStrValue("bzbm");

		UIRefPane refpane = getHeadItemUIRefPane("skyhzh");

		String wherepart = " pk_psndoc='" + receiver + "'";
		wherepart = wherepart + " and pk_currtype='" + pk_currtype + "'";
		// 20170227_01 tsy
		// setWherePart2RefModel(refpane,
		// getHeadItemStrValue("dwbm"),wherepart);
		PsnbankaccDefaultRefModel psnbankModel = (PsnbankaccDefaultRefModel) refpane.getRefModel();
		psnbankModel.setWherePart(wherepart);
		psnbankModel.setPk_psndoc(receiver);
		// 20170227_01 end
		String pk_psndoc = (String) refpane.getRefValue("pk_psndoc");
		if ((pk_psndoc != null) && (!pk_psndoc.equals(receiver))) {
			getBillCardPanel().setHeadItem("skyhzh", null);
		}
		// 20170227_02 tsy 自动带出银行帐号
		Vector<Vector> vos = psnbankModel.getRefData();
		int fieldIndex = psnbankModel.getFieldIndex("pk_bankaccsub");
		// 仅当银行帐号有且只有1个时，自动带出
		if (vos != null && vos.size() == 1 && fieldIndex > -1) {
			getBillCardPanel().setHeadItem("skyhzh", vos.get(0).get(fieldIndex));
		} else {
			getBillCardPanel().setHeadItem("skyhzh", null);
		}
		// 20170227_02 end

	}

	public void initProjTask() {
		String pk_project = getHeadItemStrValue("jobid");
		UIRefPane refPane = getHeadItemUIRefPane("projecttask");
		if (pk_project != null) {
			String wherePart = " pk_project='" + pk_project + "'";

			String pkOrg = getHeadItemUIRefPane("jobid").getRefModel().getPk_org();
			String pk_org = getHeadItemStrValue("fydwbm");
			if (BXUiUtil.getPK_group().equals(pkOrg)) {
				pk_org = BXUiUtil.getPK_group();
			}

			setWherePart2RefModel(refPane, pk_org, wherePart);
		} else {
			setWherePart2RefModel(refPane, null, "1=0");
		}
	}

	public void initCustAccount() {
		String pk_supplier = getHeadItemStrValue("hbbm");
		UIRefPane refPane = getHeadItemUIRefPane("custaccount");
		CustBankaccDefaultRefModel refModel = (CustBankaccDefaultRefModel) refPane.getRefModel();
		if (refModel != null) {
			refModel.setPk_cust(pk_supplier);
		}
		refPane.getRefModel().setWherePart("accclass='3'");

		refPane.getRefModel().addWherePart(getBankWherePart());
	}

	public void initCustomCustAccount() {
		String pk_supplier = getHeadItemStrValue("customer");
		UIRefPane refPane = getHeadItemUIRefPane("custaccount");
		CustBankaccDefaultRefModel refModel = (CustBankaccDefaultRefModel) refPane.getRefModel();
		if (refModel != null) {
			refModel.setPk_cust(pk_supplier);
		}
		refPane.getRefModel().setWherePart("accclass='1'");
		refPane.getRefModel().addWherePart(getBankWherePart());
	}

	private String getBankWherePart() {
		return getCurrencyWherePart() + getEnablestate();
	}

	private String getCurrencyWherePart() {
		StringBuffer appending = new StringBuffer();
		String pk_currtype = getHeadItemStrValue("bzbm");
		appending.append(" and ").append("pk_currtype");
		appending.append(" = '").append(pk_currtype).append("'");
		return appending.toString();
	}

	private String getEnablestate() {
		StringBuffer appending = new StringBuffer();
		appending.append(" and ").append("enablestate");
		appending.append(" =  ").append(2);
		return appending.toString();
	}

	public void initSzxm() {
		UIRefPane refPane = getHeadItemUIRefPane("szxmid");
		String pk_org = getHeadItemStrValue("fydwbm");

		refPane.setPk_org(pk_org);
	}

	public void initZy() {
		UIRefPane refPane = getHeadItemUIRefPane("zy");

		refPane.setAutoCheck(false);
	}

	public void initFkyhzh() {
		String pk_currtype = getHeadItemStrValue("bzbm");
		BillItem headItem = getBillCardPanel().getHeadItem("fkyhzh");
		if (headItem != null) {
			UIRefPane refPane = (UIRefPane) headItem.getComponent();

			String refPK = refPane.getRefPK();
			BankaccSubDefaultRefModel model = (BankaccSubDefaultRefModel) refPane.getRefModel();

			String prefix = "pk_currtype = ";
			String pk_org = null;
			if ("20110RB".equals(this.editor.getModel().getContext().getNodeCode())) {
				pk_org = (String) getBillCardPanel().getHeadItem("pk_org").getValueObject();
			} else {
				pk_org = (String) getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_PAYORG).getValueObject();
			}

			if (StringUtil.isEmpty(pk_currtype)) {
				model.setWherePart(" acctype not in ('1','2')");
				model.setPk_org(pk_org);
				return;
			}
			model.setPk_org(pk_org);
			model.setWherePart("pk_currtype = '" + pk_currtype + "' and acctype not in ('1','2')");
			model.setMatchPkWithWherePart(true);
			model.setPKMatch(true);

			// TFZQ 初始化默认单位银行帐号，默认账号维护在业务单元中 LinZB 2016-12-21
			if (refPK == null) {
				OrgVO[] orgs = OrgQueryUtil.queryOrgVOByPks(new String[] { pk_org });
				// orgV
				// =NCLocator.getInstance().lookup(IOrgVersionQryService.class).getOrgVersionVOByVID(pk_org);
				refPK = orgs == null ? null : orgs[0] == null ? null : orgs[0].getDef6();
				getBillCardPanel().getHeadItem("fkyhzh").setValue(refPK);
			}// end

			if (refPK != null) {
				Vector vec = model.matchPkData(refPK);
				if ((vec == null) || (vec.isEmpty())) {
					refPane.setPK(null);
				}
			}
		}
	}

	public void initAccount() {
		String pk_currtype = getHeadItemStrValue("bzbm");
		UIRefPane refPane = (UIRefPane) getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_CASHACCOUNT).getComponent();
		CashAccountRefModel model = (CashAccountRefModel) refPane.getRefModel();
		String prefix = "pk_moneytype=";
		String pk_org = (String) getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_PAYORG).getValueObject();
		if (StringUtil.isEmpty(pk_currtype)) {
			model.setWherePart(null);
			model.setPk_org(pk_org);
			return;
		}
		model.setPk_org(pk_org);
		model.setWherePart("pk_moneytype='" + pk_currtype + "'");

		String refPK = refPane.getRefPK();
		if (refPK != null) {
			List<String> pkValueList = new ArrayList();
			Vector vct = model.reloadData();
			Iterator<Vector> it = vct.iterator();
			int index = model.getFieldIndex("pk_cashaccount");
			while (it.hasNext()) {
				Vector next = (Vector) it.next();
				pkValueList.add((String) next.get(index));
			}

			if (!pkValueList.contains(refPK)) {
				refPane.setPK(null);
			}
		}
	}

	public static void addWherePart2RefModel(UIRefPane refPane, String pk_org, String addwherePart) {
		filterRefModelWithWherePart(refPane, pk_org, null, addwherePart);
	}

	public static void setWherePart2RefModel(UIRefPane refPane, String pk_org, String wherePart) {
		filterRefModelWithWherePart(refPane, pk_org, wherePart, null);
	}

	public static void filterRefModelWithWherePart(UIRefPane refPane, String pk_org, String wherePart, String addWherePart) {
		AbstractRefModel model = refPane.getRefModel();
		model.setPk_org(pk_org);
		model.setWherePart(wherePart);
		if (addWherePart != null) {
			model.setPk_org(pk_org);
			model.addWherePart(" and " + addWherePart);
		}
	}

	public String getHeadItemStrValue(String itemKey) {
		BillItem headItem = getBillCardPanel().getHeadItem(itemKey);
		return headItem == null ? null : (String) headItem.getValueObject();
	}

	protected boolean isJk() {
		DjLXVO currentDjlx = ((ErmBillBillManageModel) this.editor.getModel()).getCurrentDjLXVO();
		return "jk".equals(currentDjlx.getDjdl());
	}

	public UIRefPane getHeadItemUIRefPane(String key) {
		return (UIRefPane) getBillCardPanel().getHeadItem(key).getComponent();
	}

	private BillCardPanel getBillCardPanel() {
		return this.editor.getBillCardPanel();
	}

	public void setPkOrg2RefModel(UIRefPane refPane, String pk_org) {
		refPane.getRefModel().setPk_org(pk_org);
	}

}
