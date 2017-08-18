package nc.bs.er.exp.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.er.exp.util.ExpCommonUtil;
import nc.bs.er.exp.util.ExpDatasets2AggVOSerializer;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.er.exp.util.YerCShareUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.erm.prv.IArapCommonPrivate;
import nc.itf.fi.pub.Currency;
import nc.uap.lfw.core.base.ExtAttribute;
import nc.uap.lfw.core.cmd.UifLineInsertCmd;
import nc.uap.lfw.core.common.ExtAttrConstants;
import nc.uap.lfw.core.ctx.ViewContext;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.DatasetRelation;
import nc.uap.lfw.core.data.DatasetRelations;
import nc.uap.lfw.core.data.Field;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.serializer.impl.Datasets2AggVOSerializer;
import nc.uap.lfw.core.uif.listener.IBodyInfo;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.arap.bx.util.BodyEditVO;
import nc.vo.arap.bx.util.BxUIControlUtil;
import nc.vo.ep.bx.BXVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.exp.IExpConst;
import nc.vo.er.expensetype.ExpenseTypeVO;
import nc.vo.er.reimrule.ReimRuleVO;
import nc.vo.er.reimtype.ReimTypeHeaderVO;
import nc.vo.erm.costshare.CShareDetailVO;
import nc.vo.erm.util.VOUtils;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFDouble;

public class ExpLineInsertCmd extends UifLineInsertCmd {
	private String masterDsID;

	public ExpLineInsertCmd(IBodyInfo bodyInfo, String masterDsID) {
		super(bodyInfo);
		this.masterDsID = masterDsID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nc.uap.lfw.core.cmd.UifLineInsertCmd#execute()
	 */
	@Override
	public void execute() {
		ViewContext widgetctx = getLifeCycleContext().getViewContext();
		String dsId = getSlaveDataset(widgetctx);
		if (dsId == null)
			throw new LfwRuntimeException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("per_codes", "0per_codes0005")
			/* @res "没有获得当前增行数据集" */);
		Dataset ds = widgetctx.getView().getViewModels().getDataset(dsId);
		Dataset oldDs = (Dataset) ds.clone();
		String keyValue = getKeyValue(widgetctx, ds.getId());
		ds.setCurrentKey(keyValue);
		Row row = ds.getEmptyRow();
		processRow(ds, row);
		Dataset headDs = widgetctx.getView().getViewModels().getDataset(masterDsID);
		Row headRow = headDs.getSelectedRow();
		if (headRow == null) {
			return;
		}
		Integer index = getInsertIndex(ds);
		ds.insertRow(index, row);
		ds.setEnabled(true);
		ds.setRowSelectIndex(index);
		// 表体增行时，将表头的值写到表体
		// 收支项目，项目，项目任务，利润中心（及版本），核算要素，成本中心
		// 承担单位,承担部门, 客户,供应商 分摊页签再多加上这几项
		Map<String, String> headCShareMap = new HashMap<String, String>();
		headCShareMap.put(JKBXHeaderVO.SZXMID, CShareDetailVO.PK_IOBSCLASS);
		headCShareMap.put(JKBXHeaderVO.JOBID, CShareDetailVO.JOBID);
		headCShareMap.put(JKBXHeaderVO.PROJECTTASK, CShareDetailVO.PROJECTTASK);
		headCShareMap.put(JKBXHeaderVO.PK_PCORG, CShareDetailVO.PK_PCORG);
		headCShareMap.put(JKBXHeaderVO.PK_PCORG_V, JKBXHeaderVO.PK_PCORG_V);// 分摊页签无利润中心版本
		headCShareMap.put(JKBXHeaderVO.PK_CHECKELE, CShareDetailVO.PK_CHECKELE);
		headCShareMap.put(JKBXHeaderVO.PK_RESACOSTCENTER, CShareDetailVO.PK_RESACOSTCENTER);
		headCShareMap.put(JKBXHeaderVO.FYDWBM, CShareDetailVO.ASSUME_ORG);
		headCShareMap.put(JKBXHeaderVO.FYDEPTID, CShareDetailVO.ASSUME_DEPT);
		headCShareMap.put(JKBXHeaderVO.CUSTOMER, CShareDetailVO.CUSTOMER);
		headCShareMap.put(JKBXHeaderVO.HBBM, CShareDetailVO.HBBM);
		for (String item : headCShareMap.keySet()) {
			if (headDs.nameToIndex(item) != -1) {
				String itemValue = (String) headRow.getValue(headDs.nameToIndex(item));
				if (itemValue != null && itemValue.trim().length() > 0) {
					if (IExpConst.BX_CSHARE_DS_ID.equals(dsId)) {
						String cShareItem = headCShareMap.get(item);
						ExpUtil.setRowValue(row, ds, cShareItem, itemValue);
					} else {
						ExpUtil.setRowValue(row, ds, item, itemValue);
					}
				}
			}
		}
		if (IExpConst.BX_CSHARE_DS_ID.equals(dsId)) {// 分摊明细页签
			// 20170711 tsy 分摊明细增行时 分摊金额
			// UFDouble totalAmount = (UFDouble)
			// headRow.getValue(headDs.nameToIndex(JKBXHeaderVO.YBJE));
			UFDouble totalAmount = (UFDouble) headRow.getValue(headDs.nameToIndex(JKBXHeaderVO.TOTAL));
			// 20170711 end
			String pk_currentype = (String) headRow.getValue(headDs.nameToIndex(JKBXHeaderVO.BZBM));
			int currentDigit = Currency.getCurrDigit(pk_currentype);// 当前币种精度
			if (totalAmount.compareTo(UFDouble.ZERO_DBL) <= 0) {
				throw new LfwRuntimeException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0005")/*
																																 * @
																																 * res
																																 * "报销金额应大于0！"
																																 */);
			}
			// boolean isNeedAvg =
			// YerCShareUtil.isNeedBalanceJe(oldDs,totalAmount,currentDigit);
			// // String isAVg =
			// (String)AppUtil.getAppAttr(IExpConst.IS_CSHARE_AVG);
			// if (isNeedAvg){
			// YerCShareUtil.reComputeAllJeByAvg(ds, totalAmount,
			// headDs,currentDigit);
			// } else {
			// ExpUtil.setRowValue(row, ds, CShareDetailVO.ASSUME_AMOUNT,
			// UFDouble.ZERO_DBL);
			// ExpUtil.setRowValue(row, ds, CShareDetailVO.SHARE_RATIO,
			// UFDouble.ZERO_DBL);
			// }
			// 设置集团默认值（集团为必填项）
			ExpUtil.setRowValue(row, ds, CShareDetailVO.PK_GROUP, (String) headRow.getValue(headDs.nameToIndex(JKBXHeaderVO.PK_GROUP)));
		} else {
			// 如果不是第一行数据，并且这一列锁定的话，则默认取上一列的值
			if (index.intValue() >= 1) {
				Row row1 = ds.getCurrentRowData().getRow(index - 1);
				if (row1 != null) {
					Field[] fields = ds.getFieldSet().getFields();
					for (Field field : fields) {
						if (field.isLock()) {
							String value = (String) row1.getValue(ds.nameToIndex(field.getId()));
							if (value == null)
								continue;
							row.setValue(ds.nameToIndex(field.getId()), value);
						}
					}
				}
			}
			// kkk~
			// kkk处理表体报销标准 k61移到ExpViewCtrl-> ExpBodyReimRuleCmd处理
			if (IExpConst.BXZB_DS_ID.equals(masterDsID)) { // 报销单进行报销标准的处理
				processReimRule(ds, row);
			}
		}
	}

	/**
	 * 处理表体报销标准
	 * 
	 * @param ds
	 * @param row
	 */
	private void processReimRule(Dataset ds, Row row) {
		// 报销标准
		LfwView widget = getLifeCycleContext().getWindowContext().getViewContext(IExpConst.EXP_WIDGET_ID).getView();
		Dataset masterDs = widget.getViewModels().getDataset(masterDsID);
		ArrayList<Dataset> allDetailDs = new ArrayList<Dataset>();
		DatasetRelations dsRels = widget.getViewModels().getDsrelations();
		if (dsRels != null) {
			DatasetRelation[] rels = dsRels.getDsRelations(masterDsID);
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
		Dataset[] detailDss = allDetailDs.toArray(new Dataset[0]);
		AggregatedValueObject aggVo = serializer.serialize(masterDs, detailDss, BXVO.class.getName());
		BXVO bxvo = (BXVO) aggVo;
		try {
			List<ReimRuleVO> vos = new ArrayList<ReimRuleVO>();
			vos =
					NCLocator.getInstance().lookup(nc.itf.arap.prv.IBXBillPrivate.class).queryReimRule(null, (String) masterDs.getSelectedRow().getValue(masterDs.nameToIndex(ExpCommonUtil.getReimRuleOrg())));
			Map<String, List<SuperVO>> reimRuleDataMap = VOUtils.changeCollectionToMapList(vos, "pk_billtype");
			String pkGroup = ExpUtil.getPKGroup();
			Collection<SuperVO> expenseType =
					NCLocator.getInstance().lookup(IArapCommonPrivate.class).getVOs(ExpenseTypeVO.class, "pk_group='" + pkGroup + "'", false);
			Collection<SuperVO> reimType =
					NCLocator.getInstance().lookup(IArapCommonPrivate.class).getVOs(ReimTypeHeaderVO.class, "pk_group='" + pkGroup + "'", false);
			Map<String, SuperVO> expenseMap = VOUtils.changeCollectionToMap(expenseType);
			Map<String, SuperVO> reimtypeMap = VOUtils.changeCollectionToMap(reimType);
			Object tabcode = ds.getExtendAttributeValue(ExtAttrConstants.TAB_CODE);
			// TODO 目前框架中不是分页签的取不到tabcode， 无tabcode 如何处理，目前有报销标准的单据都能取到tabcode
			String tabcodeStr = tabcode == null ? "table_code" : (String) tabcode;
			HashMap<String, String> hashMap = new HashMap<String, String>();
			Field[] fields = ds.getFieldSet().getFields();
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				// 获取自定义1
				Object attr = field.getExtendMap().get("$bill_template_field_def1");
				if (attr == null) {
					continue;
				}
				ExtAttribute extAtrr = (ExtAttribute) attr;
				String userdefine1 = (String) extAtrr.getValue();
				if (userdefine1 != null && userdefine1.startsWith("getReimvalue")) {
					String expenseName = userdefine1.substring(userdefine1.indexOf("(") + 1, userdefine1.indexOf(")"));
					Collection<SuperVO> values = expenseMap.values();
					for (SuperVO vo : values) {
						if (("\"" + vo.getAttributeValue(ExpenseTypeVO.CODE) + "\"").equals(expenseName)) {
							userdefine1 = vo.getPrimaryKey();
							hashMap.put(tabcodeStr + ReimRuleVO.REMRULE_SPLITER + field.getId(), userdefine1);
						}
					}
				}
			}
			List<BodyEditVO> result = BxUIControlUtil.doBodyReimAction(bxvo, reimRuleDataMap, hashMap);
			for (BodyEditVO vo : result) {
				// getBillCardPanel().setBodyValueAt(vo.getValue(), vo.getRow(),
				// vo.getItemkey(),vo.getTablecode());
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
}
