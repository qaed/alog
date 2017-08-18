package nc.bs.er.exp.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nc.bd.accperiod.InvalidAccperiodExcetion;
import nc.bs.er.exp.util.ExpCommonUtil;
import nc.bs.er.exp.util.ExpDatasets2AggVOSerializer;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.er.exp.util.YerFromFysqUtil;
import nc.bs.er.exp.util.YerMultiVersionUtil;
import nc.bs.er.util.YerUtil;
import nc.bs.erm.util.ErAccperiodUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.arap.prv.IBXBillPrivate;
import nc.itf.bd.psnbankacc.IPsnBankaccPubService;
import nc.itf.erm.prv.IArapCommonPrivate;
import nc.itf.fi.pub.Currency;
import nc.itf.fi.pub.SysInit;
import nc.itf.org.IOrgVersionQryService;
import nc.itf.resa.costcenter.ICostCenterQueryOpt;
import nc.uap.cpb.org.exception.CpbBusinessException;
import nc.uap.lfw.core.LfwRuntimeEnvironment;
import nc.uap.lfw.core.cmd.UifAddCmd;
import nc.uap.lfw.core.ctx.ViewContext;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.DatasetRelation;
import nc.uap.lfw.core.data.DatasetRelations;
import nc.uap.lfw.core.data.Field;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.serializer.impl.Datasets2AggVOSerializer;
import nc.uap.lfw.core.serializer.impl.SuperVO2DatasetSerializer;
import nc.uap.lfw.jsp.uimeta.UIFlowvPanel;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.arap.bx.util.BXParamConstant;
import nc.vo.arap.bx.util.BXUtil;
import nc.vo.arap.bx.util.BxUIControlUtil;
import nc.vo.bd.bankaccount.BankAccSubVO;
import nc.vo.bd.bankaccount.BankAccbasVO;
import nc.vo.bd.period2.AccperiodmonthVO;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.BXHeaderVO;
import nc.vo.ep.bx.BXVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.er.djlx.DjLXVO;
import nc.vo.er.expensetype.ExpenseTypeVO;
import nc.vo.er.reimrule.ReimRuleVO;
import nc.vo.er.reimtype.ReimTypeVO;
import nc.vo.erm.util.VOUtils;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.resa.costcenter.CostCenterVO;
import nc.vo.vorg.OrgVersionVO;

public class ExpUifAddCmd extends UifAddCmd {
	private String dsId;
	private DjLXVO djlxvo;
	private LfwView widget;
	private String navDatasetId;
	private String navStr;
	private ArrayList<Dataset> allDetailDs;

	public ExpUifAddCmd(String dsId) {
		super(dsId);
		this.dsId = dsId;
	}

	public void execute() {
		boolean haveRelate = ExpUtil.haveRelate();
		if (haveRelate) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("weberm_0", "0E010001-0033"));
		}

		ViewContext widgetctx = getLifeCycleContext().getWindowContext().getViewContext("main");
		boolean pageRecordUndo = false;
		boolean widgetRecordUndo = false;
		LfwView widget = widgetctx.getView();
		if (this.dsId == null) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("per_codes", "0per_codes0008"));
		}
		Dataset ds = widget.getViewModels().getDataset(this.dsId);
		if (ds == null) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("per_codes", "0per_codes0009") + this.dsId + "!");
		}
		String currKey = ds.getCurrentKey();
		if ((currKey == null) || (currKey.equals(""))) {
			if (this.navDatasetId != null)
				throw new LfwRuntimeException(this.navStr);
			ds.getRowSet("MASTER_KEY", true);
			ds.setCurrentKey("MASTER_KEY");
		}
		if (ds.isControlwidgetopeStatus()) {
			widgetRecordUndo = true;
		}
		Dataset firstBusitemDs = null;
		List<String> idList = new ArrayList();
		this.allDetailDs = new ArrayList();
		idList.add(this.dsId);
		DatasetRelations dsRels = widget.getViewModels().getDsrelations();
		if (dsRels != null) {
			DatasetRelation[] rels = dsRels.getDsRelations(this.dsId);
			if (rels != null) {
				for (int i = 0; i < rels.length; i++) {
					String detailDsId = rels[i].getDetailDataset();
					idList.add(detailDsId);
					Dataset detailDs = widget.getViewModels().getDataset(detailDsId);
					this.allDetailDs.add(detailDs);
					Map<String, String> formularMap;
					if (("busitem".equals(detailDs.getId())) || ("jk_busitem".equals(detailDs.getId()))) {
						firstBusitemDs = detailDs;
						formularMap =
								(HashMap) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute("yer_formularMap");
						Set<String> keySet = formularMap.keySet();
						for (String key : keySet) {
							YerUtil.modifyField(detailDs, "EditFormular", key, (String) formularMap.get(key));
						}
					}

					detailDs.clear();
					detailDs.setCurrentKey("MASTER_KEY");
					detailDs.setEnabled(true);
					if (detailDs.isControlwidgetopeStatus()) {
						widgetRecordUndo = true;
					}
				}
			}
		}

		String formID = "bxzb_base_info_form";
		if ("jkzb".equals(this.dsId)) {
			formID = "jkzb_base_info_form";
		}
		String gridID = "busitem_grid";

		Row oldRow = ds.getSelectedRow();
		if ((oldRow != null) && (ds.nameToIndex("pk_item") != -1)) {
			String pkItem = (String) oldRow.getValue(ds.nameToIndex("pk_item"));

			String djlxbm = (String) oldRow.getValue(ds.nameToIndex("djlxbm"));

			if ((pkItem != null) && (!"".equals(pkItem))) {
				YerFromFysqUtil.setBillPageEditable(widget, pkItem, djlxbm, ds, firstBusitemDs, formID, gridID, true);
			}
		}

		Row row = ds.getEmptyRow();
		setNavPkToRow(row, this.navDatasetId, ds);
		onBeforeRowAdd(row);
		ds.addRow(row);
		ds.setRowSelectIndex(Integer.valueOf(ds.getRowIndex(row)));
		ds.setEnabled(true);

		processReimRule(ds, this.allDetailDs, row);
		onAfterRowAdd(row);

		YerFromFysqUtil.setHeadRelateFieldEditable(widget, ds, formID, true);

		YerFromFysqUtil.setGridMenuEnable(widget, true);
		Row selectRow = ds.getSelectedRow();

		String hbbm = (String) selectRow.getValue(ds.nameToIndex("hbbm"));
		if ((hbbm == null) || ("".equals(hbbm))) {
			ExpUtil.setFormEleEditable(formID, new String[] { "freecust", "freecust_name" }, false);
		}
		if ("bxzb".equals(this.dsId)) {
			UFBoolean iscostshare = (UFBoolean) selectRow.getValue(ds.nameToIndex("iscostshare"));
			UFBoolean isexpamt = (UFBoolean) selectRow.getValue(ds.nameToIndex("isexpamt"));
			UIFlowvPanel cSharevpanel = ExpUtil.getUIFlowvPanel("csharevpanel");
			if (cSharevpanel != null) {
				if (iscostshare != null) {
					if (iscostshare == UFBoolean.FALSE) {
						cSharevpanel.setVisible(false);
					} else {
						cSharevpanel.setVisible(true);
					}
				} else {
					cSharevpanel.setVisible(false);
				}
			}
			if ((isexpamt != null) && (isexpamt == UFBoolean.TRUE)) {
				try {
					String pk_org = (String) selectRow.getValue(ds.nameToIndex("pk_org"));
					UFDate date = (UFDate) selectRow.getValue(ds.nameToIndex("djrq"));
					ExpUtil.setFormEleEditable("bxzb_base_info_form", new String[] { "start_period_yearmth", "total_period" }, true);
					AccperiodmonthVO accperiodmonthVO = ErAccperiodUtil.getAccperiodmonthByUFDate(pk_org, date);
					if (AppUtil.getAppAttr("YER_START_PERIOD") != null) {
						ExpUtil.setRowValue(selectRow, ds, "start_period", (String) AppUtil.getAppAttr("YER_START_PERIOD"));
					} else {
						ExpUtil.setRowValue(selectRow, ds, "start_period", accperiodmonthVO.getPk_accperiodmonth());
					}
				} catch (InvalidAccperiodExcetion e) {
					Logger.error(e.getMessage(), e);
				}
			}
		}

		String cur_pk_org = (String) row.getValue(ds.nameToIndex("pk_org"));
		if ((cur_pk_org == null) || ("".equals(cur_pk_org))) {
			ExpUtil.setFormEleEditable(widget, formID, new String[] { "bzbm", ExpUtil.getRefItem("bzbm", ds) }, false);
		}

		Map<String, Map<String, String>> changeInfoMap = new HashMap();
		LfwRuntimeEnvironment.getWebContext().getRequest().getSession().setAttribute("change_info_map", changeInfoMap);
	}

	protected void onBeforeRowAdd(Row row) {
		super.onBeforeRowAdd(row);

		ViewContext widgetctx = getLifeCycleContext().getWindowContext().getViewContext("main");

		String funcode = (String) getLifeCycleContext().getWindowContext().getWindow().getExtendAttributeValue("funcode");
		String pk_group = ExpUtil.getPKGroup();

		String billType = (String) AppUtil.getAppAttr("$$$$$$$$FLOWTYPEPK");
		if ((billType == null) && ("".equals(billType))) {
			billType = ExpUtil.getBillType(funcode);
		}
		this.djlxvo = ExpUtil.getDjlxvos(billType, pk_group);
		if (this.djlxvo == null) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("weberm_0", "0E010001-0035"));
		}

		UFBoolean fcbz = this.djlxvo.getFcbz();
		if ((fcbz != null) && (fcbz.booleanValue())) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000429"));
		}

		this.widget = widgetctx.getView();
		Dataset ds = this.widget.getViewModels().getDataset(this.dsId);

		setDefaultValue(row, ds);

		setRowValue(row, ds, "djlxbm", billType);
		setRowValue(row, ds, "djdl", this.djlxvo.getDjdl());

		String pk_org = (String) row.getValue(ds.nameToIndex("pk_org"));
		String oriOrg = pk_org;

		SuperVO2DatasetSerializer serializer = new SuperVO2DatasetSerializer();
		boolean includeGroup = true;
		if ((this.djlxvo.getIsloadtemplate() != null) && (this.djlxvo.getIsloadtemplate().booleanValue())) {
			List<JKBXVO> initBill = null;
			try {
				initBill = BxUIControlUtil.getInitBill(pk_org, pk_group, this.djlxvo.getDjlxbm(), includeGroup);
			} catch (BusinessException e) {
				Logger.error(e.getMessage(), e);
			}
			if ((initBill != null) && (initBill.size() > 0)) {
				JKBXVO jkbxvo = (JKBXVO) initBill.get(0);
				JKBXHeaderVO headVO = jkbxvo.getParentVO();

				UFBoolean isExpamt = headVO.getIsexpamt();
				if ((isExpamt != null) && (isExpamt.booleanValue())) {
					ExpUtil.setFormEleEditable("bxzb_base_info_form", new String[] { "start_period_yearmth", "total_period" }, true);
				}

				String[] fieldNotCopy = BXHeaderVO.getFieldNotInit();
				for (int i = 0; i < fieldNotCopy.length; i++) {
					int index = ds.nameToIndex(fieldNotCopy[i]);
					if (index != -1) {
						row.setValue(index, row.getValue(index));
					}
				}

				List<String> fieldNotCopyList = Arrays.asList(fieldNotCopy);
				Field[] fields = ds.getFieldSet().getFields();

				String[] str =
						{ JKBXHeaderVO.FYDWBM_V, JKBXHeaderVO.FYDEPTID_V, JKBXHeaderVO.PK_PAYORG, JKBXHeaderVO.PK_PAYORG_V, "fydwbm", "fydeptid", JKBXHeaderVO.DEPTID_V, "dwbm", "pk_org" };

				List<String> ruleTypeFields = new ArrayList();
				for (int i = 0; i < str.length; i++) {
					ruleTypeFields.add(str[i]);
				}
				for (Field f : fields) {
					String t = f.getId();
					if (!fieldNotCopyList.contains(t)) {

						if (!t.endsWith("_name")) {

							if ("iscostshare".equals(t)) {
								UFBoolean temp = (UFBoolean) headVO.getAttributeValue(t);
								if ("Y".equals(temp.toString())) {
									UIFlowvPanel cSharevpanel = ExpUtil.getUIFlowvPanel("csharevpanel");
									if (cSharevpanel != null) {
										cSharevpanel.setVisible(true);
									}
								}
							}
							if (ruleTypeFields.contains(t)) {
								if (headVO.getAttributeValue(t) != null) {
									row.setValue(ds.nameToIndex(t), headVO.getAttributeValue(t));
								}
							} else {
								row.setValue(ds.nameToIndex(t), headVO.getAttributeValue(t));
							}
						}
					}
				}

				String currOrg = (String) row.getValue(ds.nameToIndex("pk_org"));
				if ((currOrg == null) || (currOrg.trim().length() == 0)) {
					row.setValue(ds.nameToIndex("pk_org"), pk_org);
				}

				BXBusItemVO busItemVO = null;

				for (Dataset detailDs : this.allDetailDs) {
					Object tabcode = detailDs.getExtendAttributeValue("$TAB_CODE");

					if (tabcode != null) {

						detailDs.setCurrentKey(row.getRowId());

						CircularlyAccessibleValueObject[] vos = jkbxvo.getTableVO(tabcode == null ? "arap_bxbusitem" : tabcode.toString());

						if (vos != null) {
							for (int i = 0; i < vos.length; i++) {
								busItemVO = (BXBusItemVO) vos[i];
								busItemVO.setPk_busitem(null);
								busItemVO.setPk_jkbx(null);
								Row busRow = detailDs.getEmptyRow();
								serializer.vo2DataSet(busItemVO, detailDs, busRow);
								detailDs.addRow(busRow);
								detailDs.setSelectedIndex(detailDs.getRowIndex(busRow));
							}
						}

						detailDs.setEnabled(true);
					}
				}

				setHeadOrgMultiVersion(row, ds, new String[] { "pk_org_v", JKBXHeaderVO.FYDWBM_V, JKBXHeaderVO.DWBM_V, JKBXHeaderVO.PK_PCORG_V, JKBXHeaderVO.FYDEPTID_V }, new String[] { "pk_org", "fydwbm", "dwbm", "pk_pcorg", "fydeptid" }, this.widget);
			}

			pk_org = (String) row.getValue(ds.nameToIndex("pk_org"));
			if ((oriOrg != null) && (!oriOrg.equals(pk_org))) {
				setDefaultValueInitBill(row, ds, pk_org);
			}
			String fydwbm = (String) row.getValue(ds.nameToIndex("fydwbm"));
			String fydeptid = row.getString(ds.nameToIndex("fydeptid"));
			YerMultiVersionUtil.setRowValueDeptMultiVersion(row, ds, new String[] { JKBXHeaderVO.FYDEPTID_V }, fydwbm, fydeptid, this.widget);
		}

		setZhrq((String) row.getValue(ds.nameToIndex("pk_org")), row, ds);

		UFDate date = (UFDate) row.getValue(ds.nameToIndex("djrq"));
		String bzbm = (String) row.getValue(ds.nameToIndex("bzbm"));
		try {
			ExpCommonUtil.setCurrencyInfo(pk_org, Currency.getOrgLocalCurrPK(pk_org), bzbm, date, this.widget, ds, row);
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
		}
	}

	protected void onAfterRowAdd(Row row) {
	}

	private void setDefaultValue(Row row, Dataset ds) {
		String pk_group = ExpUtil.getPKGroup();
		String userID = YerUtil.getPk_user();
		// 20170518 tsy 设置支付方式默认值
		// 测试-银企直连：1002A11000000000CY5H
		// 正式-银企直连：1001A4100000000F5WOM
		// 测试、正式-网银：0001Z0100000000000Y2
		// 20170607 tsy 当单据类型为其他收入单时，不用设置默认支付方式
		if (!"264X-Cxx-05".equals(this.djlxvo.getDjlxbm())) {
			setRowValue(row, ds, "jsfs", "1001A4100000000F5WOM");
		}
		// 20170607 end
		// 20170518 end
		setRowValue(row, ds, "ybje", UFDouble.ZERO_DBL);
		setRowValue(row, ds, "bbje", UFDouble.ZERO_DBL);
		setRowValue(row, ds, "total", UFDouble.ZERO_DBL);

		setRowValue(row, ds, "operator", userID);
		setRowValue(row, ds, "pk_group", pk_group);

		setRowValue(row, ds, "djrq", ExpUtil.getBusiDate());

		String[] str = { null, null, null, null };
		try {
			str = ((IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class)).queryPsnidAndDeptid(userID, pk_group);
			if ((str == null) || (StringUtil.isEmpty(str[0]))) {
				return;
			}
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
			throw new LfwRuntimeException(e.getMessage());
		}

		if (!pk_group.equals(str[3])) {
			return;
		}

		String defaultOrg = str[2];

		String pk_org = null;
		try {
			if ((defaultOrg != null) && (defaultOrg.length() > 0)) {
				String[] values = ExpUtil.getPermissionOrgsPortal();
				if ((values != null) && (values.length != 0)) {

					List<String> permissionOrgList = Arrays.asList(values);
					if (permissionOrgList.contains(defaultOrg)) {
						pk_org = defaultOrg;
					}
				}
			}
		} catch (CpbBusinessException e) {
			Logger.error(e.getMessage(), e);
			pk_org = defaultOrg;
		}
		setRowValue(row, ds, "pk_org", pk_org);

		setRowValue(row, ds, new String[] { "jkbxr", "receiver" }, str[0]);
		setRowValue(row, ds, new String[] { "deptid", "fydeptid" }, str[1]);

		setRowValue(row, ds, new String[] { "dwbm", "fydwbm", "pk_pcorg", "pk_fiorg", JKBXHeaderVO.PK_PAYORG }, str[2]);

		setRowValue(row, ds, "creator", userID);

		YerMultiVersionUtil.setRowValueOrgMultiVersion(row, ds, new String[] { "pk_org_v" }, pk_org, this.widget);
		YerMultiVersionUtil.setRowValueOrgMultiVersion(row, ds, new String[] { JKBXHeaderVO.FYDWBM_V, JKBXHeaderVO.DWBM_V, BXHeaderVO.PK_PCORG_V, JKBXHeaderVO.PK_PAYORG_V }, str[2], this.widget);
		YerMultiVersionUtil.setRowValueDeptMultiVersion(row, ds, new String[] { JKBXHeaderVO.DEPTID_V, JKBXHeaderVO.FYDEPTID_V }, str[2], str[1], this.widget);
		// 20170518 tsy 设置银行帐号默认值
		// 组织是心怡总部：测试库：0001A1100000000047BL 总部：0001A410000000000UOS

		try {
			String pk_org_v = (String) row.getValue(ds.nameToIndex("pk_org_v"));
			OrgVersionVO orgV = NCLocator.getInstance().lookup(IOrgVersionQryService.class).getOrgVersionVOByVID(pk_org_v);
			setRowValue(row, ds, "fkyhzh", orgV == null ? null : orgV.getDef6());
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

		// 20170518 end

		String org = pk_org == null ? "" : pk_org;

		String defcurrency = this.djlxvo.getDefcurrency();
		if (((defcurrency == null) || (defcurrency.trim().length() == 0)) && (org != null) && (org.length() != 0)) {
			try {
				defcurrency = Currency.getOrgLocalCurrPK(org);
			} catch (BusinessException e) {
				Logger.error(e.getMessage(), e);
				throw new LfwRuntimeException(e.getMessage());
			}
		}

		setRowValue(row, ds, "bzbm", defcurrency);

		try {
			IPsnBankaccPubService pa = (IPsnBankaccPubService) NCLocator.getInstance().lookup(IPsnBankaccPubService.class.getName());
			BankAccbasVO bankAccbasVO = pa.queryDefaultBankAccByPsnDoc(ExpUtil.getOperator(YerUtil.getPk_user(), YerUtil.getPK_group()));

			if (bankAccbasVO != null) {
				BankAccSubVO[] bankAccSubVO = bankAccbasVO.getBankaccsub();

				Integer enAbleState = bankAccbasVO.getEnablestate();
				if ((bankAccSubVO != null) && (bankAccSubVO.length > 0) && (bankAccSubVO[0] != null)) {
					if (enAbleState.intValue() == 2) {
						setRowValue(row, ds, "skyhzh", bankAccSubVO[0].getPk_bankaccsub());
					}
				}
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}

		if (row.getValue(ds.nameToIndex(JKBXHeaderVO.PK_RESACOSTCENTER)) == null) {
			String pk_fydept = (String) row.getValue(ds.nameToIndex("fydeptid"));

			String pk_fydwbm = (String) row.getValue(ds.nameToIndex("fydwbm"));

			setCostCenter(row, ds, pk_fydept, pk_fydwbm);
		}

		setRowValue(row, ds, "sxbz", Integer.valueOf(0));
	}

	protected void setCostCenter(Row row, Dataset ds, String pk_fydept, String pk_fydwbm) {
		boolean isResInstalled = BXUtil.isProductInstalled(YerUtil.getPK_group(), "3820");
		if (!isResInstalled) {
			return;
		}
		if (StringUtil.isEmpty(pk_fydept)) {
			return;
		}
		String key = pk_fydept;
		String pk_costcenter = null;

		CostCenterVO[] vos = null;
		try {
			vos =
					((ICostCenterQueryOpt) NCLocator.getInstance().lookup(ICostCenterQueryOpt.class)).queryCostCenterVOByDept(new String[] { pk_fydept });
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
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
		setRowValue(row, ds, JKBXHeaderVO.PK_RESACOSTCENTER, pk_costcenter);
	}

	private void setRowValue(Row row, Dataset ds, String name, Object value) {
		if (ds.nameToIndex(name) != -1) {
			row.setValue(ds.nameToIndex(name), value);
		}
	}

	private void setRowValue(Row row, Dataset ds, String[] names, Object value) {
		if (names == null) {
			return;
		}
		for (String name : names) {
			if (ds.nameToIndex(name) != -1) {
				row.setValue(ds.nameToIndex(name), value);
			}
		}
	}

	private void setHeadOrgMultiVersion(Row row, Dataset ds, String[] fields, String[] ofields, LfwView widget) {
		for (int i = 0; i < fields.length; i++) {
			if (ds.nameToIndex(ofields[i]) != -1) {
				String value = (String) row.getValue(ds.nameToIndex(ofields[i]));
				YerMultiVersionUtil.setRowValueOrgMultiVersion(row, ds, fields[i], value, widget);
			}
		}
	}

	protected void setZhrq(String org, Row row, Dataset ds) {
		if (org == null) {
			return;
		}
		try {
			if (ds.getFieldSet().getField("zhrq") != null) {
				GregorianCalendar rq = new GregorianCalendar();
				Object valueRq = row.getValue(ds.nameToIndex("djrq"));
				String djrq = valueRq == null ? new UFDate().toString() : valueRq.toString();
				int year = new Integer(djrq.substring(0, 4)).intValue();
				int month = new Integer(djrq.substring(5, 7)).intValue();
				int date = new Integer(djrq.substring(8, 10)).intValue();
				month--;
				rq.set(year, month, date);
				int time = SysInit.getParaInt(org, BXParamConstant.PARAM_ER_RETURN_DAYS).intValue();

				rq.add(5, time);
				month = rq.get(2) + 1;
				String zhrq = rq.get(1) + "-" + month + "-" + rq.get(5);
				setRowValue(row, ds, "zhrq", new UFDate(zhrq));
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
	}

	private void setDefaultValueInitBill(Row row, Dataset ds, String org) {
		String pk_group = YerUtil.getPK_group();
		String userID = YerUtil.getPk_user();

		String[] permOrgs = new String[0];
		try {
			permOrgs = ExpUtil.getPermissionOrgsPortal();
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
		}
		List<String> permOrgsList = Arrays.asList(permOrgs);

		if (permOrgsList.contains(org)) {
			setRowValue(row, ds, "pk_org", org);
			YerMultiVersionUtil.setRowValueOrgMultiVersion(row, ds, new String[] { "pk_org_v" }, org, this.widget);
		} else {
			setRowValue(row, ds, "pk_org", null);
			setRowValue(row, ds, "pk_org_v", null);
		}
	}

	private void processReimRule(Dataset ds, ArrayList<Dataset> allDetailDs, Row row) {
		Datasets2AggVOSerializer serializer = new ExpDatasets2AggVOSerializer();
		Dataset[] detailDss = (Dataset[]) allDetailDs.toArray(new Dataset[0]);
		AggregatedValueObject aggVo = serializer.serialize(ds, detailDss, BXVO.class.getName());
		BXVO bxvo = (BXVO) aggVo;
		try {
			List<ReimRuleVO> vos = new ArrayList();
			vos =
					((IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class)).queryReimRule(null, (String) row.getValue(ds.nameToIndex("pk_org")));

			Map<String, List<SuperVO>> reimRuleDataMap = VOUtils.changeCollectionToMapList(vos, "pk_billtype");

			String pkGroup = ExpUtil.getPKGroup();

			Collection<SuperVO> expenseType =
					((IArapCommonPrivate) NCLocator.getInstance().lookup(IArapCommonPrivate.class)).getVOs(ExpenseTypeVO.class, "pk_group='" + pkGroup + "'", false);

			Collection<SuperVO> reimType =
					((IArapCommonPrivate) NCLocator.getInstance().lookup(IArapCommonPrivate.class)).getVOs(ReimTypeVO.class, "pk_group='" + pkGroup + "'", false);

			Map<String, SuperVO> expenseMap = VOUtils.changeCollectionToMap(expenseType);
			Map<String, SuperVO> reimtypeMap = VOUtils.changeCollectionToMap(reimType);

			String reimrule = BxUIControlUtil.doHeadReimAction(bxvo, reimRuleDataMap, expenseMap, reimtypeMap);

			setRowValue(row, ds, "reimrule", reimrule);
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
			throw new LfwRuntimeException(e.getMessage());
		}
	}
}