package nc.view.tb.adjbill;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import nc.bs.framework.common.NCLocator;
import nc.desktop.ui.WorkbenchEnvironment;
import nc.funcnode.ui.AbstractFunclet;
import nc.itf.mdm.dim.IDimManager;
import nc.ms.mdm.dim.DimServiceGetter;
import nc.ms.tb.adjbill.model.NtbEntityModel;
import nc.ms.tb.task.model.NtbTaskDefModel;
import nc.pubitf.rbac.IPermissionDomainService;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.bd.ref.RefInitializeCondition;
import nc.ui.bd.ref.RefUIConfig;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.UITextField;
import nc.ui.pub.beans.constenum.IConstEnum;
import nc.ui.queryarea.quick.QuickQueryArea;
import nc.ui.querytemplate.CriteriaChangedEvent;
import nc.ui.querytemplate.ICriteriaEditor;
import nc.ui.querytemplate.IQueryConditionDLG;
import nc.ui.querytemplate.IQueryTemplateTotalVOProcessor;
import nc.ui.querytemplate.QueryConditionDLG;
import nc.ui.querytemplate.QueryConditionEditor;
import nc.ui.querytemplate.QueryConditionEditorContext;
import nc.ui.querytemplate.component.UIRefPaneForEnum;
import nc.ui.querytemplate.filter.IFilter;
import nc.ui.querytemplate.filtereditor.DefaultFilterEditor;
import nc.ui.querytemplate.filtereditor.IFilterEditor;
import nc.ui.querytemplate.meta.FilterMeta;
import nc.ui.querytemplate.meta.IFilterMeta;
import nc.ui.querytemplate.simpleeditor.SimpleEditor;
import nc.ui.querytemplate.value.IFieldValue;
import nc.ui.querytemplate.value.IFieldValueElement;
import nc.ui.querytemplate.value.RefValueObject;
import nc.ui.querytemplate.valueeditor.DefaultFieldValueEditor;
import nc.ui.querytemplate.valueeditor.DefaultFieldValueElementEditor;
import nc.ui.querytemplate.valueeditor.IFieldValueElementEditor;
import nc.ui.querytemplate.valueeditor.IFieldValueElementEditorFactory;
import nc.ui.querytemplate.valueeditor.RefElementEditor;
import nc.ui.querytemplate.valueeditor.UIRefpaneCreator;
import nc.ui.querytemplate.valueeditor.ref.CompositeRefElementEditor;
import nc.ui.querytemplate.valueeditor.ref.CompositeRefInfo;
import nc.ui.querytemplate.valueeditor.ref.CompositeRefPanel;
import nc.ui.uif2.actions.DefaultQueryDelegator;
import nc.view.tb.adjbill.action.AdjustBillUITool;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.mdm.dim.DimDef;
import nc.vo.mdm.dim.DimHierarchy;
import nc.vo.mdm.dim.EntHierType;
import nc.vo.mdm.pub.NtbLogger;
import nc.vo.org.GroupVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.query.QueryConditionVO;
import nc.vo.pub.query.QueryTempletTotalVO;
import nc.vo.querytemplate.md.MDFilterMeta;
import nc.vo.querytemplate.sysfunc.SFType;
import nc.vo.sm.UserVO;
import nc.vo.uif2.LoginContext;
import nc.vo.util.innercode.NamedParamUtil;

public class AdjustBillQueryDelegator extends DefaultQueryDelegator {
	private static String PK_DATAENT = "body.pk_dataent";

	private static String PK_TASKDEF = "body.pk_taskdef";

	private static String SYSCODE = "TBB_SysCode";

	public AdjustBillQueryDelegator() {
		this.processor = new AdjustBillQueryTemplateTotalVOProcessor();
		this.elementeditor = new AdjustBillFieldValueElementEditorFactory();
		this.criterialeditor = new AdjustBillCriteriaEditorChangedListener();
	}

	private AdjustBillQueryTemplateTotalVOProcessor processor;

	private AdjustBillFieldValueElementEditorFactory elementeditor;

	private AdjustBillCriteriaEditorChangedListener criterialeditor;

	public static final String SUBJECT_NOTOWNED_MANYORG_COND =
			"select subjectorg.subjectid from   org_orgs org inner join sm_subject_org subjectorg on org.pk_org = subjectorg.pk_org  where subjectorg.pk_group = '{pk_group}' and org.pk_org in {orgs}  group by subjectorg.subjectid having count(subjectorg.subjectid) <= {orgscount}";
	public static final String USERS_OWNED_MANYORG_COND =
			" cuserid in  (select cuserid from sm_user_role where enabledate<='{nowtime}' and (isnull(cast(disabledate as char),'~')='~' or disabledate>'{nowtime}')  and pk_role in (select subjectid from sm_subject_org where subjectid in (select subjectorg.subjectid from   org_orgs org inner join sm_subject_org subjectorg on org.pk_org = subjectorg.pk_org  where subjectorg.pk_group = '{pk_group}' and org.pk_org in {orgs}  group by subjectorg.subjectid having count(subjectorg.subjectid) <= {orgscount}) and pk_group ='{pk_group}'))";

	protected IQueryConditionDLG createQueryDlg_New() {
		IQueryConditionDLG iQueryDlg = super.createQueryDlg_New();
		if (iQueryDlg != null) {
			iQueryDlg.registerQueryTemplateTotalVOProceeor(this.processor);
			iQueryDlg.registerFieldValueEelementEditorFactory(this.elementeditor);
			iQueryDlg.registerCriteriaEditorListener(this.criterialeditor);
		}
		return iQueryDlg;
	}

	class AdjustBillQueryTemplateTotalVOProcessor implements IQueryTemplateTotalVOProcessor {
		AdjustBillQueryTemplateTotalVOProcessor() {
		}

		public void processQueryTempletTotalVO(QueryTempletTotalVO totalVO) {
			if (totalVO != null) {
				QueryConditionVO[] qryconds = totalVO.getConditionVOs();
				for (QueryConditionVO qcvo : qryconds)
					if (AdjustBillQueryDelegator.PK_DATAENT.equals(qcvo.getFieldCode())) {
						String defaultOrgPk = AdjustBillQueryDelegator.this.getDefaultOrgPk();
						if (!StringUtil.isEmptyWithTrim(defaultOrgPk))
							qcvo.setValue(defaultOrgPk);
					} else if ("vbillno".equals(qcvo.getFieldCode())) {
						// 20170515 tsy 可以使用“单据编号”进行查询
						// qcvo.setIfUsed(UFBoolean.valueOf(false));
						// 20170515 end
					} else if ("createddate".equals(qcvo.getFieldCode())) {
						qcvo.setValue("#day(0)#,#day(0)#");
					}
			}
		}
	}

	class AdjustBillFieldValueElementEditorFactory implements IFieldValueElementEditorFactory {
		AdjustBillFieldValueElementEditorFactory() {
		}

		public IFieldValueElementEditor createFieldValueElementEditor(FilterMeta meta) {
			if (AdjustBillQueryDelegator.PK_DATAENT.equals(meta.getFieldCode())) {
				QueryConditionEditorContext context =
						AdjustBillQueryDelegator.this.queryDlg == null ? AdjustBillQueryDelegator.this.iQueryDlg.getQryCondEditor().getQueryContext() : AdjustBillQueryDelegator.this.queryDlg.getQueryContext();

				UIRefPane refPane = new AdjustBillQueryDelegator.NtbEntityRefPane();

				if (meta.isRequired()) {
					refPane.getUITextField().setShowMustInputHint(true);
				}

				String sysCode =
						((AbstractFunclet) AdjustBillQueryDelegator.this.getContext().getEntranceUI()).getParameter(AdjustBillQueryDelegator.SYSCODE);

				refPane.setMultiCorpRef(true);
				NtbEntityModel model = new NtbEntityModel(sysCode);
				model.setNodeCode(AdjustBillQueryDelegator.this.getContext().getNodeCode());

				refPane.setRefModel(model);
				refPane.getRefUIConfig().setModel(model);

				return new CompositeRefElementEditor(refPane, meta.getReturnType(), AdjustBillQueryDelegator.this.createCompositeRefInfo(meta));
			}
			if ("vtranstype".equals(meta.getFieldCode())) {
				QueryConditionEditorContext context =
						AdjustBillQueryDelegator.this.queryDlg == null ? AdjustBillQueryDelegator.this.iQueryDlg.getQryCondEditor().getQueryContext() : AdjustBillQueryDelegator.this.queryDlg.getQueryContext();

				UIRefPane refPane = new UIRefpaneCreator(context).createUIRefPane(meta);
				String pk_group = WorkbenchEnvironment.getInstance().getGroupVO().getPrimaryKey();
				String strWhere = "systemcode = 'tbb' and parentbilltype in('TBWT', 'TBWW') and pk_group = '" + pk_group + "'";
				refPane.setWhereString(strWhere);
				return new RefElementEditor(refPane, meta.getReturnType());
			}
			if (AdjustBillQueryDelegator.PK_TASKDEF.equals(meta.getFieldCode())) {
				QueryConditionEditorContext context =
						AdjustBillQueryDelegator.this.queryDlg == null ? AdjustBillQueryDelegator.this.iQueryDlg.getQryCondEditor().getQueryContext() : AdjustBillQueryDelegator.this.queryDlg.getQueryContext();

				UIRefPane refPane = new UIRefpaneCreator(context).createUIRefPane(meta);

				String pk_group = WorkbenchEnvironment.getInstance().getGroupVO().getPrimaryKey();

				String groupWhere = "(pk_group = '" + pk_group + "' or pk_org = '" + "GLOBLE00000000000000" + "')";

				String sysCode =
						((AbstractFunclet) AdjustBillQueryDelegator.this.getContext().getEntranceUI()).getParameter(AdjustBillQueryDelegator.SYSCODE);

				String sysWhere = "(avabusisystem like '%" + sysCode + "%' or avabusisystem = '~')";

				String strWhere = groupWhere + " and " + sysWhere;
				refPane.setWhereString(strWhere);

				refPane.setWhereString(null);

				AbstractRefModel model = refPane.getRefModel();
				if ((model instanceof NtbTaskDefModel)) {
					((NtbTaskDefModel) model).setDataPerm(true);
				}
				return new RefElementEditor(refPane, meta.getReturnType());
			}
			return null;
		}
	}

	class AdjustBillCriteriaEditorChangedListener implements nc.ui.querytemplate.ICriteriaChangedListener {
		AdjustBillCriteriaEditorChangedListener() {
		}

		public void criteriaChanged(CriteriaChangedEvent event) {
			try {
				if (event.getEventtype() == 4) {
					IFilterEditor filterEditor = event.getFiltereditor();
					if (!(filterEditor instanceof DefaultFilterEditor))
						return;
					DefaultFilterEditor filter = (DefaultFilterEditor) filterEditor;
					String fieldCode = filter.getFilter().getFilterMeta().getFieldCode();
					if (AdjustBillQueryDelegator.PK_DATAENT.equals(fieldCode)) {
						DefaultFieldValueEditor editor = (DefaultFieldValueEditor) filter.getFieldValueEditor();
						IFieldValueElementEditor leftValueElemEditor = editor.getFieldValueElemEditor();
						UIRefPane refPane = null;
						if ((leftValueElemEditor instanceof CompositeRefElementEditor)) {
							CompositeRefElementEditor compositeRefElementEditor = (CompositeRefElementEditor) leftValueElemEditor;
							CompositeRefPanel compositeRefPanel =
									(CompositeRefPanel) compositeRefElementEditor.getFieldValueElemEditorComponent();
							refPane = compositeRefPanel.getStdRefPane();
						}

						if (refPane != null) {
							String defaultOrgPk = AdjustBillQueryDelegator.this.getDefaultOrgPk();
							if (!StringUtil.isEmptyWithTrim(defaultOrgPk)) {
								refPane.setPK(defaultOrgPk);

								if (filter.getFilter() != null)
									filter.getFilter().setFieldValue(editor.getValue());
							}
						}
					} else if (("createdby".equals(fieldCode)) || ("approvedby".equals(fieldCode))) {
						DefaultFieldValueEditor editor = (DefaultFieldValueEditor) filter.getFieldValueEditor();
						IFieldValueElementEditor leftValueElemEditor = editor.getFieldValueElemEditor();
						UIRefPane refPane = null;
						if ((leftValueElemEditor instanceof CompositeRefElementEditor)) {
							CompositeRefElementEditor compositeRefElementEditor = (CompositeRefElementEditor) leftValueElemEditor;
							CompositeRefPanel compositeRefPanel =
									(CompositeRefPanel) compositeRefElementEditor.getFieldValueElemEditorComponent();
							refPane = compositeRefPanel.getStdRefPane();
						}

						String sqlWhere = "1 != 1";
						if (refPane != null) {
							refPane.getRefModel().setSelectedData(null);
							refPane.setWhereString(sqlWhere);
						}
					} else if ("vbilltype".equals(fieldCode)) {
						DefaultFieldValueEditor editor = (DefaultFieldValueEditor) filter.getFieldValueEditor();
						IFieldValueElementEditor elemEditor = editor.getFieldValueElemEditor();
						if ((elemEditor instanceof DefaultFieldValueElementEditor)) {
							JComponent component = elemEditor.getFieldValueElemEditorComponent();
							if ((component instanceof UIRefPaneForEnum)) {
								String sysCode =
										((AbstractFunclet) AdjustBillQueryDelegator.this.getContext().getEntranceUI()).getParameter(AdjustBillQueryDelegator.SYSCODE);
								if (StringUtil.isEmptyWithTrim(sysCode))
									sysCode = "TB";
								IConstEnum[] enums = AdjustBillUITool.getEnumBillType(sysCode);
								if (enums != null) {
									DefaultListModel model = new DefaultListModel();
									for (IConstEnum enu : enums)
										model.addElement(enu);
									((UIRefPaneForEnum) component).setCheckListModel(model);
								}
							}
						}
					}
				} else if ((event.getEventtype() == 1) && (AdjustBillQueryDelegator.PK_DATAENT.equals(event.getFieldCode()))) {
					ICriteriaEditor criteriaEditor = event.getCriteriaEditor();
					List<IFilterEditor> listFilterEditor = null;
					if ((criteriaEditor instanceof SimpleEditor)) {
						SimpleEditor simpleEditor = (SimpleEditor) criteriaEditor;
						listFilterEditor = simpleEditor.getFilterEditors();
					} else if ((criteriaEditor instanceof QuickQueryArea)) {
						QuickQueryArea queryArea = (QuickQueryArea) criteriaEditor;
						listFilterEditor = queryArea.getFilterEditors();
					}
					if (listFilterEditor == null) {
						return;
					}
					String sqlWhere = "1 != 1";

					List<IFieldValueElement> listFieldValue = event.getFilter().getFieldValue().getFieldValues();
					if ((listFieldValue != null) && (listFieldValue.size() > 0)) {
						StringBuffer strWhere = new StringBuffer();
						for (IFieldValueElement fieldValue : listFieldValue) {
							Object valueObj = fieldValue.getValueObject();
							String strValue = null;
							if ((valueObj instanceof RefValueObject))
								strValue = ((RefValueObject) valueObj).getPk();
							if (!StringUtil.isEmptyWithTrim(strValue)) {
								if (strWhere.length() > 0)
									strWhere.append(",");
								strWhere.append("'").append(strValue).append("'");
							}
						}
						if (strWhere.length() > 0) {
							StringBuffer sbWhere = new StringBuffer();
							sbWhere.append("(").append(strWhere.toString()).append(")");
							String pk_group = WorkbenchEnvironment.getInstance().getGroupVO().getPrimaryKey();
							NamedParamUtil npu = new NamedParamUtil();
							npu.addNamedParam("pk_group", pk_group);
							npu.addNamedParam("orgs", sbWhere.toString());
							npu.addNamedParam("orgscount", Integer.valueOf(listFieldValue.size()));
							npu.addNamedParam("nowtime", new UFDateTime().toString());
							sqlWhere =
									npu.format(" cuserid in  (select cuserid from sm_user_role where enabledate<='{nowtime}' and (isnull(cast(disabledate as char),'~')='~' or disabledate>'{nowtime}')  and pk_role in (select subjectid from sm_subject_org where subjectid in (select subjectorg.subjectid from   org_orgs org inner join sm_subject_org subjectorg on org.pk_org = subjectorg.pk_org  where subjectorg.pk_group = '{pk_group}' and org.pk_org in {orgs}  group by subjectorg.subjectid having count(subjectorg.subjectid) <= {orgscount}) and pk_group ='{pk_group}'))");
						}
					}
					for (IFilterEditor filterEditor : listFilterEditor)
						if ((filterEditor instanceof DefaultFilterEditor)) {
							DefaultFilterEditor filter = (DefaultFilterEditor) filterEditor;
							String fieldCode = filter.getFilter().getFilterMeta().getFieldCode();

							if (("createdby".equals(fieldCode)) || ("approvedby".equals(fieldCode))) {
								DefaultFieldValueEditor editor = (DefaultFieldValueEditor) filter.getFieldValueEditor();
								IFieldValueElementEditor leftValueElemEditor = editor.getFieldValueElemEditor();
								UIRefPane refPane = null;
								if ((leftValueElemEditor instanceof CompositeRefElementEditor)) {
									CompositeRefElementEditor compositeRefElementEditor = (CompositeRefElementEditor) leftValueElemEditor;
									CompositeRefPanel compositeRefPanel =
											(CompositeRefPanel) compositeRefElementEditor.getFieldValueElemEditorComponent();
									refPane = compositeRefPanel.getStdRefPane();
								}
								if (refPane != null)
									refPane.setWhereString(sqlWhere);
							}
						}
				}
			} catch (Exception e) {
				String sqlWhere;
				NtbLogger.error("AdjustBill CriteriaEditorChangedListener Intialize Error --->" + e.getMessage());
			}
		}
	}

	private String[] getFuncletOrgPks() {
		String[] permOrgPks = null;
		try {
			String pk_user = WorkbenchEnvironment.getInstance().getLoginUser().getPrimaryKey();

			IPermissionDomainService service =
					(IPermissionDomainService) NCLocator.getInstance().lookup(IPermissionDomainService.class.getName());
			permOrgPks = service.getAllPermissionPkorgs(pk_user, getContext().getNodeCode());
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return permOrgPks;
	}

	private CompositeRefInfo createCompositeRefInfo(FilterMeta meta) {
		CompositeRefInfo info = new CompositeRefInfo();
		info.setSysFuncRefUsed(meta.isSysFuncRefUsed());
		if ((meta instanceof MDFilterMeta)) {
			meta = (MDFilterMeta) meta;
			info.setSysFunctionType(SFType.get(((MDFilterMeta) meta).getAttribute()));
		}
		return info;
	}

	private RefInitializeCondition getRefIntializeCondition(String sysCode) {
		RefInitializeCondition sourceRefInitCon = new RefInitializeCondition();
		String pk_orgStru = getDefaultDimHierarchyPk(sysCode);
		if (!StringUtil.isEmptyWithTrim(pk_orgStru)) {

			sourceRefInitCon.setDefaultPk(null);
		}
		return sourceRefInitCon;
	}

	private String getDefaultDimHierarchyPk(String sysCode) {
		DimDef ddEntity = DimServiceGetter.getDimManager().getDimDefByPK("TB_DIMDEF_ENTITY_000");

		List<DimHierarchy> listDhs = ddEntity.getHierarchies();

		ArrayList<DimHierarchy> listBudgetStru = new ArrayList();

		ArrayList<DimHierarchy> listPurPlanStru = new ArrayList();
		for (DimHierarchy dh : listDhs) {
			if ((dh != null) && (dh.getEntHierType() != null)) {
				EntHierType hierType = dh.getEntHierType();
				if (EntHierType.BUDGET_SCHM.equals(hierType)) {
					listBudgetStru.add(dh);
				} else if (EntHierType.PURCHASE_SCHM.equals(hierType))
					listPurPlanStru.add(dh);
			}
		}
		DimHierarchy defaultDh = null;
		if ("MPP".equals(sysCode)) {
			defaultDh = listPurPlanStru.size() > 0 ? (DimHierarchy) listPurPlanStru.get(0) : null;
		} else {
			defaultDh = listBudgetStru.size() > 0 ? (DimHierarchy) listBudgetStru.get(0) : null;
		}
		return defaultDh == null ? null : defaultDh.getPrimaryKey();
	}

	class NtbEntityRefPane extends UIRefPane {
		NtbEntityRefPane() {
		}

		public void setRefValue() {
			if ((getRefModel() instanceof NtbEntityModel)) {
				NtbEntityModel model = (NtbEntityModel) getRefModel();
				getRefUIConfig().getRefFilterInitconds()[0].setDefaultPk(model.getPk_OrgStru());
			}
			super.setRefValue();
		}
	}

	protected String getDefaultOrgPk() {
		return null;
	}
}