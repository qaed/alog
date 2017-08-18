package nc.bs.er.exp.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import nc.bs.er.exp.cjk.ctrl.CjkMainViewCtrl;
import nc.bs.er.exp.util.ExpCommonUtil;
import nc.bs.er.exp.util.ExpDatasets2AggVOSerializer;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.er.exp.util.YerMultiVersionUtil;
import nc.bs.er.util.YerUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.pub.formulaparse.FormulaParse;
import nc.itf.arap.prv.IBXBillPrivate;
import nc.itf.erm.prv.IArapCommonPrivate;
import nc.itf.fi.pub.Currency;
import nc.uap.lfw.core.LfwRuntimeEnvironment;
import nc.uap.lfw.core.WebContext;
import nc.uap.lfw.core.base.ExtAttribute;
import nc.uap.lfw.core.cmd.base.UifCommand;
import nc.uap.lfw.core.ctx.AppLifeCycleContext;
import nc.uap.lfw.core.ctx.ViewContext;
import nc.uap.lfw.core.ctx.WindowContext;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.DatasetRelation;
import nc.uap.lfw.core.data.DatasetRelations;
import nc.uap.lfw.core.data.Field;
import nc.uap.lfw.core.data.FieldSet;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.data.RowData;
import nc.uap.lfw.core.event.DatasetCellEvent;
import nc.uap.lfw.core.event.DatasetEvent;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.formular.LfwFormulaParser;
import nc.uap.lfw.core.log.LfwLogger;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.page.LfwWindow;
import nc.uap.lfw.core.page.ViewModels;
import nc.uap.lfw.core.serializer.impl.Dataset2SuperVOSerializer;
import nc.uap.lfw.core.serializer.impl.Datasets2AggVOSerializer;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.arap.bx.util.BodyEditVO;
import nc.vo.arap.bx.util.BxUIControlUtil;
import nc.vo.ep.bx.BXVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.er.expensetype.ExpenseTypeVO;
import nc.vo.er.reimrule.ReimRuleVO;
import nc.vo.er.reimtype.ReimTypeHeaderVO;
import nc.vo.erm.util.VOUtils;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.ValidationException;
import nc.vo.pub.formulaset.VarryVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class ExpBodyValueChangeCmd extends UifCommand {
	private String masterDsID;
	private DatasetEvent datasetEvent;

	public ExpBodyValueChangeCmd(String masterDsID, DatasetEvent datasetCellEvent) {
		this.masterDsID = masterDsID;
		this.datasetEvent = datasetCellEvent;
	}

	public void execute() {
		String hasBusitemGrid = (String) AppUtil.getAppAttr("ExpHasBusitemGrid");
		if ("N".equals(hasBusitemGrid)) {
			return;
		}
		DatasetCellEvent datasetCellEvent = (DatasetCellEvent) this.datasetEvent;
		if ((datasetCellEvent.getNewValue() == null) && (datasetCellEvent.getOldValue() == null)) {
			return;
		}
		if ((datasetCellEvent.getNewValue() != null) && (datasetCellEvent.getNewValue().equals(datasetCellEvent.getOldValue()))) {
			return;
		}
		Dataset busitemDs = (Dataset) datasetCellEvent.getSource();
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = jkbxWindow.getView("main");
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		String pkItem = (String) masterDs.getSelectedRow().getValue(masterDs.nameToIndex("pk_item"));
		if ((("busitem".equals(busitemDs.getId())) || ("jk_busitem".equals(busitemDs.getId()))) && (pkItem != null) && (!"".equals(pkItem))) {
			Map<String, String> formularMap =
					(HashMap) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute("yer_formularMap");
			Set<String> keySet = formularMap.keySet();
			for (String key : keySet) {
				int keyIndex = busitemDs.nameToIndex(key);
				if (datasetCellEvent.getColIndex() == keyIndex) {
					YerUtil.modifyField(busitemDs, "EditFormular", key, (String) formularMap.get(key));
				}
			}
			Field currentField = busitemDs.getFieldSet().getField(datasetCellEvent.getColIndex());
			if ((currentField.getEditFormular() != null) && (currentField.getEditFormular().indexOf("amount->") != -1)) {
				processEditorFormular(busitemDs);
			}
		}
		int amountIndex = busitemDs.nameToIndex("amount");
		if (datasetCellEvent.getColIndex() == amountIndex) {
			setHeadTotalValue();
			int rowIndex = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(rowIndex);
			UFDouble amount = (UFDouble) row.getValue(amountIndex);
			row.setValue(busitemDs.nameToIndex("ybje"), amount);
			modifyFinValues(busitemDs.nameToIndex("ybje"), rowIndex, busitemDs, row);
			try {
				doContract(busitemDs, row);
			} catch (BusinessException e1) {
				Logger.error(e1.getMessage(), e1);
				throw new LfwRuntimeException(e1);
			}
		}
		// 20170712 tsy 自定义25做含税金额，值改变是传到表头
		// 税率改变时：带不含税金额到到表头
		int defitem25Index = busitemDs.nameToIndex("defitem25");
		if (datasetCellEvent.getColIndex() == defitem25Index) {
			setHeadTotalValue();
		}
		// 20170712 end
		if (datasetCellEvent.getColIndex() == busitemDs.nameToIndex("ybje")) {
			setHeadYbjeValue();
			int rowIndex = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(rowIndex);
			modifyFinValues(busitemDs.nameToIndex("ybje"), rowIndex, busitemDs, row);
			try {
				doContract(busitemDs, row);
			} catch (BusinessException e1) {
				Logger.error(e1.getMessage(), e1);
				throw new LfwRuntimeException(e1);
			}
		}
		int reimtypeIndex = busitemDs.nameToIndex("pk_reimtype");
		if (datasetCellEvent.getColIndex() == reimtypeIndex) {
			int index = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(index);
			if ("bxzb".equals(this.masterDsID)) {
				processReimRule(busitemDs, row);
			}
		}
		int jobid = busitemDs.nameToIndex("jobid");
		if (datasetCellEvent.getColIndex() == jobid) {
			int index = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(index);
			row.setValue(busitemDs.nameToIndex("projecttask"), null);
		}
		if (datasetCellEvent.getColIndex() == busitemDs.nameToIndex("pk_pcorg_v")) {
			int index = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(index);
			String pk_pcorg_v = (String) datasetCellEvent.getNewValue();
			String pk_pcorg = YerMultiVersionUtil.getBillHeadFinanceOrg("pk_pcorg_v", pk_pcorg_v, widget, this.masterDsID);
			ExpUtil.setRowValue(row, busitemDs, "pk_pcorg", pk_pcorg);
			ExpUtil.setRowValue(row, busitemDs, "pk_checkele", null);
		}
		if (datasetCellEvent.getColIndex() == busitemDs.nameToIndex("pk_pcorg")) {
			int index = datasetCellEvent.getRowIndex();
			Row row = busitemDs.getCurrentRowData().getRow(index);
			String pk_pcorg = (String) datasetCellEvent.getNewValue();
			UFDate date = (UFDate) masterDs.getSelectedRow().getValue(masterDs.nameToIndex("djrq"));
			if ((date == null) || (StringUtil.isEmpty(date.toString()))) {
				date = ExpUtil.getBusiDate();
			}
			String pk_org = (String) masterDs.getSelectedRow().getValue(masterDs.nameToIndex("pk_org"));
			String pk_pcorg_v_value =
					YerMultiVersionUtil.getBillHeadFinanceOrgVersion(JKBXHeaderVO.PK_PCORG_V, pk_pcorg, date, masterDs, pk_org, widget);
			ExpUtil.setRowValue(row, busitemDs, "pk_pcorg_v", pk_pcorg_v_value);
		}
	}

	public void doContract(Dataset busitemDs, Row busitemRow) throws ValidationException, BusinessException {
		UFDouble ybje = busitemRow.getUFDobule(busitemDs.nameToIndex("ybje"));
		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = pageMeta.getWidget("main");
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		Row bxzbRow = masterDs.getSelectedRow();
		if (bxzbRow == null) {
			return;
		}
		String djdl = (String) bxzbRow.getValue(masterDs.nameToIndex("djdl"));
		if ((ybje != null) && (!ybje.equals(UFDouble.ZERO_DBL)) && ("bx".equals(djdl))) {
			Dataset contractDs = widget.getViewModels().getDataset("contrast");
			Dataset2SuperVOSerializer ser = new Dataset2SuperVOSerializer();
			SuperVO[] pvos = ser.serialize(contractDs);
			if ((pvos != null) && (pvos.length > 0)) {
				List<BxcontrastVO> contrastsList = new ArrayList();
				for (int i = 0; i < pvos.length; i++) {
					contrastsList.add((BxcontrastVO) pvos[i]);
				}
				new CjkMainViewCtrl().doContrast(masterDs, bxzbRow, contrastsList);
			}
		}
	}

	public void modifyFinValues(int jeIndex, int rowIndex, Dataset busitemDs, Row row) {
		UFDouble ybje =
				row.getValue(busitemDs.nameToIndex("ybje")) == null ? new UFDouble(0) : (UFDouble) row.getValue(busitemDs.nameToIndex("ybje"));
		UFDouble cjkybje =
				row.getValue(busitemDs.nameToIndex("cjkybje")) == null ? new UFDouble(0) : (UFDouble) row.getValue(busitemDs.nameToIndex("cjkybje"));
		UFDouble zfybje =
				row.getValue(busitemDs.nameToIndex("zfybje")) == null ? new UFDouble(0) : (UFDouble) row.getValue(busitemDs.nameToIndex("zfybje"));
		UFDouble hkybje =
				row.getValue(busitemDs.nameToIndex("hkybje")) == null ? new UFDouble(0) : (UFDouble) row.getValue(busitemDs.nameToIndex("hkybje"));
		if ((busitemDs.nameToIndex("ybje") == jeIndex) || (busitemDs.nameToIndex("cjkybje") == jeIndex)) {
			if (ybje.getDouble() > cjkybje.getDouble()) {
				ExpUtil.setRowValue(row, busitemDs, "zfybje", ybje.sub(cjkybje));
				ExpUtil.setRowValue(row, busitemDs, "hkybje", new UFDouble(0));
				ExpUtil.setRowValue(row, busitemDs, "cjkybje", cjkybje);
			} else {
				ExpUtil.setRowValue(row, busitemDs, "hkybje", cjkybje.sub(ybje));
				ExpUtil.setRowValue(row, busitemDs, "zfybje", new UFDouble(0));
				ExpUtil.setRowValue(row, busitemDs, "cjkybje", cjkybje);
			}
		}
		ExpUtil.setRowValue(row, busitemDs, "ybye", ybje);
		transFinYbjeToBbje(rowIndex, busitemDs, row);
	}

	protected void transFinYbjeToBbje(int rowIndex, Dataset busitemDs, Row row) {
		UFDouble ybje = (UFDouble) row.getValue(busitemDs.nameToIndex("ybje"));
		UFDouble cjkybje = (UFDouble) row.getValue(busitemDs.nameToIndex("cjkybje"));
		UFDouble hkybje = (UFDouble) row.getValue(busitemDs.nameToIndex("hkybje"));
		UFDouble zfybje = (UFDouble) row.getValue(busitemDs.nameToIndex("zfybje"));
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = jkbxWindow.getView("main");
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		Row rowHead = masterDs.getCurrentRowData().getSelectedRow();
		String bzbm = (String) rowHead.getValue(masterDs.nameToIndex("bzbm"));
		if (("".equals(bzbm)) || (bzbm == null)) {
			bzbm = "null";
		}
		UFDouble hl = (UFDouble) rowHead.getValue(masterDs.nameToIndex("bbhl"));
		UFDouble grouphl = (UFDouble) rowHead.getValue(masterDs.nameToIndex("groupbbhl"));
		UFDouble globalhl = (UFDouble) rowHead.getValue(masterDs.nameToIndex("globalbbhl"));
		String pk_org = (String) rowHead.getValue(masterDs.nameToIndex("pk_org"));
		String pk_group = ExpUtil.getPKGroup();
		try {
			UFDouble[] bbje = Currency.computeYFB(pk_org, 1, bzbm, ybje, null, null, null, hl, ExpUtil.getSysdate());
			ExpUtil.setRowValue(row, busitemDs, "bbje", bbje[2]);
			ExpUtil.setRowValue(row, busitemDs, "bbye", bbje[2]);
			bbje = Currency.computeYFB(pk_org, 1, bzbm, cjkybje, null, null, null, hl, ExpUtil.getSysdate());
			ExpUtil.setRowValue(row, busitemDs, "cjkbbje", bbje[2]);
			bbje = Currency.computeYFB(pk_org, 1, bzbm, hkybje, null, null, null, hl, ExpUtil.getSysdate());
			ExpUtil.setRowValue(row, busitemDs, "hkbbje", bbje[2]);
			bbje = Currency.computeYFB(pk_org, 1, bzbm, zfybje, null, null, null, hl, ExpUtil.getSysdate());
			ExpUtil.setRowValue(row, busitemDs, "zfbbje", bbje[2]);
			UFDouble[] je = Currency.computeYFB(pk_org, 1, bzbm, ybje, null, null, null, hl, ExpUtil.getSysdate());
			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, ExpUtil.getSysdate(), pk_org, pk_group, globalhl, grouphl);
			ExpUtil.setRowValue(row, busitemDs, "groupbbje", money[0]);
			ExpUtil.setRowValue(row, busitemDs, "globalbbje", money[1]);
		} catch (BusinessException e) {
			ExceptionHandler.consume(e);
		}
	}

	public void setHeadTotalValue() {
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = jkbxWindow.getView("main");
		Dataset[] allDs = widget.getViewModels().getDatasets();
		List<Dataset> datasetList = new ArrayList();
		// 20170711 tsy 表体自定义22作为不含税金额，计算合计到表头自定义11
		String totalItem2 = "defitem25";
		UFDouble totalAll2 = new UFDouble(0);
		// 20170711 end
		for (int i = 0; i < allDs.length; i++) {
			String dsID = allDs[i].getId();
			if ((!dsID.startsWith("$refds")) && (!dsID.equals("bxzb")) && (!dsID.equals("jkzb")) && (!dsID.equals("contrast")) && (!dsID.equals("finitem")) && (!dsID.equals("jkcontrast")) && (!dsID.equals("jkfinitem")) && (!dsID.equals("bx_cshare_detail"))) {
				datasetList.add(allDs[i]);
			}
		}
		Row row = null;
		UFDouble totalAll = new UFDouble(0);
		for (int i = 0; i < datasetList.size(); i++) {
			Dataset thisDs = (Dataset) datasetList.get(i);
			RowData rowData = thisDs.getCurrentRowData();
			if (rowData != null) {
				Row[] rowArr = rowData.getRows();
				for (int j = 0; j < rowArr.length; j++) {
					row = rowArr[j];
					int index = thisDs.nameToIndex("amount");
					UFDouble amount = (UFDouble) row.getValue(index);
					if (amount != null) {
						totalAll = totalAll.add(amount);
					}
					// 20170711 tsy 表体自定义25作为不含税金额，计算合计到表头自定义12
					Object defitem25 = row.getValue(thisDs.nameToIndex(totalItem2));
					if (null != defitem25) {
						UFDouble amount2 = new UFDouble(defitem25.toString());
						if (amount2 != null) {
							totalAll2 = totalAll2.add(amount2);
						}
					}
					// 20170711 end
				}
			}
		}
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		Row row_bxzb = masterDs.getCurrentRowData().getSelectedRow();
		ExpUtil.setRowValue(row_bxzb, masterDs, "total", totalAll);
		ExpUtil.setRowValue(row_bxzb, masterDs, "ybje", totalAll);
		// 20170711 tsy 表体自定义25作为不含税金额，计算合计到表头自定义12
		ExpUtil.setRowValue(row_bxzb, masterDs, "zyx12", totalAll2);
	}

	public void setHeadYbjeValue() {
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = jkbxWindow.getView("main");
		Dataset[] allDs = widget.getViewModels().getDatasets();
		List<Dataset> datasetList = new ArrayList();
		for (int i = 0; i < allDs.length; i++) {
			String dsID = allDs[i].getId();
			if ((!dsID.startsWith("$refds")) && (!dsID.equals("bxzb")) && (!dsID.equals("jkzb")) && (!dsID.equals("contrast")) && (!dsID.equals("finitem")) && (!dsID.equals("jkcontrast")) && (!dsID.equals("jkfinitem")) && (!dsID.equals("bx_cshare_detail"))) {
				datasetList.add(allDs[i]);
			}
		}
		Row row = null;
		UFDouble totalAll = new UFDouble(0);
		for (int i = 0; i < datasetList.size(); i++) {
			Dataset thisDs = (Dataset) datasetList.get(i);
			RowData rowData = thisDs.getCurrentRowData();
			if (rowData != null) {
				Row[] rowArr = rowData.getRows();
				for (int j = 0; j < rowArr.length; j++) {
					row = rowArr[j];
					int index = thisDs.nameToIndex("ybje");
					UFDouble amount = (UFDouble) row.getValue(index);
					if (amount != null) {
						totalAll = totalAll.add(amount);
					}
				}
			}
		}
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		Row row_bxzb = masterDs.getCurrentRowData().getSelectedRow();
		ExpUtil.setRowValue(row_bxzb, masterDs, "ybje", totalAll);
	}

	private void processReimRule(Dataset ds, Row row) {
		LfwView widget = getLifeCycleContext().getWindowContext().getViewContext("main").getView();
		Dataset masterDs = widget.getViewModels().getDataset(this.masterDsID);
		ArrayList<Dataset> allDetailDs = new ArrayList();
		DatasetRelations dsRels = widget.getViewModels().getDsrelations();
		if (dsRels != null) {
			DatasetRelation[] rels = dsRels.getDsRelations(this.masterDsID);
			if (rels != null) {
				for (int i = 0; i < rels.length; i++) {
					String detailDsId = rels[i].getDetailDataset();
					Dataset detailDs = widget.getViewModels().getDataset(detailDsId);
					if (detailDs != null) {
						allDetailDs.add(detailDs);
					}
				}
			}
		}
		Datasets2AggVOSerializer serializer = new ExpDatasets2AggVOSerializer();
		Dataset[] detailDss = (Dataset[]) allDetailDs.toArray(new Dataset[0]);
		AggregatedValueObject aggVo = serializer.serialize(masterDs, detailDss, BXVO.class.getName());
		BXVO bxvo = (BXVO) aggVo;
		try {
			List<ReimRuleVO> vos = new ArrayList();
			vos =
					((IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class)).queryReimRule(null, (String) masterDs.getSelectedRow().getValue(masterDs.nameToIndex(ExpCommonUtil.getReimRuleOrg())));
			Map<String, List<SuperVO>> reimRuleDataMap = VOUtils.changeCollectionToMapList(vos, "pk_billtype");
			String pkGroup = ExpUtil.getPKGroup();
			Collection<SuperVO> expenseType =
					((IArapCommonPrivate) NCLocator.getInstance().lookup(IArapCommonPrivate.class)).getVOs(ExpenseTypeVO.class, "pk_group='" + pkGroup + "'", false);
			Collection<SuperVO> reimType =
					((IArapCommonPrivate) NCLocator.getInstance().lookup(IArapCommonPrivate.class)).getVOs(ReimTypeHeaderVO.class, "pk_group='" + pkGroup + "'", false);
			Map<String, SuperVO> expenseMap = VOUtils.changeCollectionToMap(expenseType);
			Map<String, SuperVO> reimtypeMap = VOUtils.changeCollectionToMap(reimType);
			Object tabcode = ds.getExtendAttributeValue("$TAB_CODE");
			String tabcodeStr = tabcode == null ? "table_code" : (String) tabcode;
			HashMap<String, String> hashMap = new HashMap();
			Field[] fields = ds.getFieldSet().getFields();
			Field field;
			String userdefine1;
			String expenseName;
			for (int i = 0; i < fields.length; i++) {
				field = fields[i];
				Object attr = field.getExtendMap().get("$bill_template_field_def1");
				if (attr != null) {
					ExtAttribute extAtrr = (ExtAttribute) attr;
					userdefine1 = (String) extAtrr.getValue();
					if ((userdefine1 != null) && (userdefine1.startsWith("getReimvalue"))) {
						expenseName = userdefine1.substring(userdefine1.indexOf("(") + 1, userdefine1.indexOf(")"));
						Collection<SuperVO> values = expenseMap.values();
						for (SuperVO vo : values) {
							if (("\"" + vo.getAttributeValue("code") + "\"").equals(expenseName)) {
								userdefine1 = vo.getPrimaryKey();
								hashMap.put(tabcodeStr + "@" + field.getId(), userdefine1);
							}
						}
					}
				}
			}
			List<BodyEditVO> result = BxUIControlUtil.doBodyReimAction(bxvo, reimRuleDataMap, hashMap);
			for (BodyEditVO vo : result) {
				int index = ds.getRowIndex(row);
				if (vo.getRow() == index) {
					setRowValue(row, ds, vo.getItemkey(), vo.getValue());
				}
			}
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
			throw new LfwRuntimeException(e.getMessage());
		}
	}

	private void setRowValue(Row row, Dataset ds, String name, Object value) {
		if (ds.nameToIndex(name) != -1) {
			row.setValue(ds.nameToIndex(name), value);
		}
	}

	private void processEditorFormular(Dataset ds) {
		RowData rd = ds.getCurrentRowData();
		if (rd == null)
			return;
		Row selectedRow = ds.getSelectedRow();
		if (selectedRow == null)
			return;
		List<String> executedFpList = new ArrayList();
		int fieldCount = ds.getFieldSet().getFieldCount();
		FormulaParse fp = LfwFormulaParser.getInstance();
		for (int i = 0; i < fieldCount; i++) {
			try {
				Field field = ds.getFieldSet().getField(i);
				String formular = field.getEditFormular();
				if (formular != null) {
					if (!executedFpList.contains(formular)) {
						executedFpList.add(formular);
						String[] expArr = formular.split(";");
						fp.setExpressArray(expArr);
						VarryVO[] varryVOs = fp.getVarryArray();
						if ((varryVOs != null) && (varryVOs.length > 0)) {
							String[] formularNames = new String[varryVOs.length];
							Map<String, Integer> indexMap = getIndexMap(ds);
							for (int j = 0; j < varryVOs.length; j++) {
								String[] keys = varryVOs[j].getVarry();
								if (keys != null) {
									for (String key : keys) {
										List<Object> valueList = new ArrayList();
										if (indexMap.get(key) != null) {
											Object value = selectedRow.getValue(((Integer) indexMap.get(key)).intValue());
											if (field.getExtendAttribute(field.getId()) != null) {
												String refKey = ((Field) field.getExtendAttributeValue(field.getId())).getId();
												value = selectedRow.getValue(((Integer) indexMap.get(refKey)).intValue());
											}
											Field f = ds.getFieldSet().getField(key);
											if ((f != null) && (value != null)) {
												if (("UFDouble".equals(f.getDataType())) || ("Double".equals(f.getDataType())) || ("Decimal".equals(f.getDataType())) || (("SelfDefine".equals(f.getDataType())) && (f.getPrecision() != null) && (!f.getPrecision().equals("")))) {
													if (!(value instanceof UFDouble)) {
														value = new UFDouble(value.toString());
													}
												} else if (("Integer".equals(f.getDataType())) && (!(value instanceof Integer))) {
													value = new Integer((String) value);
												}
											}
											valueList.add(value);
											fp.addVariable(key, valueList);
										}
									}
									formularNames[j] = varryVOs[j].getFormulaName();
								}
							}
							Object[][] result = fp.getValueOArray();
							if (result != null) {
								for (int l = 0; l < formularNames.length; l++) {
									int index = ds.nameToIndex(formularNames[l]);
									if (index == -1) {
										LfwLogger.error("can not find column:" + formularNames[l] + ", ds id:" + ds.getId());
									} else
										selectedRow.setValue(index, result[l][0]);
								}
							}
						} else {
							fp.getValueOArray();
						}
					}
				}
			} catch (Throwable e) {
				if ((e instanceof LfwRuntimeException))
					throw ((LfwRuntimeException) e);
				Logger.error(e.getMessage(), e);
			}
		}
	}

	private Map<String, Integer> getIndexMap(Dataset ds) {
		Map<String, Integer> indexMap = new HashMap();
		int count = ds.getFieldSet().getFieldCount();
		for (int i = 0; i < count; i++) {
			Field field = ds.getFieldSet().getField(i);
			String key = field.getId();
			indexMap.put(key, Integer.valueOf(i));
		}
		return indexMap;
	}
}