package nc.bs.er.exp.ctrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nc.bs.er.exp.cmd.ExpBodyValueChangeCmd;
import nc.bs.er.exp.cmd.ExpHeadValueChangeCmd;
import nc.bs.er.exp.cmd.ExpLineAddCmd;
import nc.bs.er.exp.cmd.ExpLineDelCmd;
import nc.bs.er.exp.cmd.ExpLineInsertCmd;
import nc.bs.er.exp.cmd.ExpNcWfmCmd;
import nc.bs.er.exp.cmd.ExpUifAddCmd;
import nc.bs.er.exp.cmd.ExpUifCancelCmd;
import nc.bs.er.exp.cmd.ExpUifCommitCmd;
import nc.bs.er.exp.cmd.ExpUifDatasetAfterSelectCmd;
import nc.bs.er.exp.cmd.ExpUifDatasetLoadCmd;
import nc.bs.er.exp.cmd.ExpUifEditCmd;
import nc.bs.er.exp.cmd.ExpUifTempSaveCmd;
import nc.bs.er.exp.util.ExpDatasets2AggVOSerializer;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.er.exp.util.YerCShareUtil;
import nc.bs.er.util.YerUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.fi.pub.Currency;
import nc.uap.ctrl.tpl.qry.FromWhereSQLImpl;
import nc.uap.lfw.core.AppInteractionUtil;
import nc.uap.lfw.core.LfwRuntimeEnvironment;
import nc.uap.lfw.core.cmd.CmdInvoker;
import nc.uap.lfw.core.cmd.UifDatasetLoadCmd;
import nc.uap.lfw.core.cmd.UifLineCopyCmd;
import nc.uap.lfw.core.cmd.UifLinePasteCmd;
import nc.uap.lfw.core.cmd.base.CommandStatus;
import nc.uap.lfw.core.cmd.base.UifCommand;
import nc.uap.lfw.core.comp.GridColumn;
import nc.uap.lfw.core.comp.GridComp;
import nc.uap.lfw.core.ctrl.IController;
import nc.uap.lfw.core.ctx.AppLifeCycleContext;
import nc.uap.lfw.core.ctx.ViewContext;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.DatasetRelation;
import nc.uap.lfw.core.data.Field;
import nc.uap.lfw.core.data.IRefDataset;
import nc.uap.lfw.core.data.Parameter;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.datamodel.IDatasetProvider;
import nc.uap.lfw.core.event.CellEvent;
import nc.uap.lfw.core.event.DataLoadEvent;
import nc.uap.lfw.core.event.DatasetCellEvent;
import nc.uap.lfw.core.event.DatasetEvent;
import nc.uap.lfw.core.event.DialogEvent;
import nc.uap.lfw.core.event.GridEvent;
import nc.uap.lfw.core.event.MouseEvent;
import nc.uap.lfw.core.event.RowInsertEvent;
import nc.uap.lfw.core.event.ScriptEvent;
import nc.uap.lfw.core.exception.LfwBusinessException;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.exception.LfwValidateException;
import nc.uap.lfw.core.log.LfwLogger;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.page.LfwWindow;
import nc.uap.lfw.core.serializer.impl.Datasets2AggVOSerializer;
import nc.uap.lfw.core.uif.delegator.DefaultDataValidator;
import nc.uap.lfw.core.uif.delegator.IDataValidator;
import nc.uap.lfw.core.uif.listener.IBodyInfo;
import nc.uap.lfw.core.uif.listener.SingleBodyInfo;
import nc.uap.lfw.jsp.uimeta.UIFlowvPanel;
import nc.uap.wfm.constant.WfmConstants;
import nc.uap.wfm.ncworkflow.cmd.NcWfmCmd;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.BXHeaderVO;
import nc.vo.ep.bx.BXVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.exp.IExpConst;
import nc.vo.erm.costshare.CShareDetailVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;

/**
 * @author kongxl 2012-02-29
 */
public class ExpViewCtrl implements IController {
	private static final long serialVersionUID = 1L;
	


	public void beforeShow(DialogEvent e) {
		
		
		UIFlowvPanel cSharevpanel = ExpUtil.getUIFlowvPanel(IExpConst.COST_SHARE_VPANEL); 
		if (cSharevpanel != null ) {
			cSharevpanel.setVisible(false);
		}
		LfwView widget = AppLifeCycleContext.current().getViewContext().getView();
		//�õ���Dataset��Id
		String masterDsId = getMasterDataset(widget);
		Dataset masterDs = widget.getViewModels().getDataset(getMasterDataset(widget));
		Row row = masterDs.getSelectedRow();
		if (row != null) {
			
			UFBoolean isCostShare = (UFBoolean)row.getValue(masterDs.nameToIndex(JKBXHeaderVO.ISCOSTSHARE));
			if (UFBoolean.TRUE.equals(isCostShare)){
				cSharevpanel.setVisible(true);
			}
		}
		
		
		
	}


	public void onDataLoad(  DataLoadEvent e) {

		
		Dataset ds = e.getSource();
		//�������ô�Bill ID
		if(IExpConst.BXZB_DS_ID.equals(ds.getId()) || IExpConst.JKZB_DS_ID.equals(ds.getId())){
			String billId = LfwRuntimeEnvironment.getWebContext().getOriginalParameter(UifDatasetLoadCmd.OPEN_BILL_ID);
			LfwRuntimeEnvironment.getWebContext().getParameter(UifDatasetLoadCmd.OPEN_BILL_ID);

			LfwRuntimeEnvironment.getWebContext().getAppSession().getOriginalParameter(UifDatasetLoadCmd.OPEN_BILL_ID);
			ds.getReqParameters().addParameter(new Parameter(UifDatasetLoadCmd.OPEN_BILL_ID, billId));

		}
		CmdInvoker.invoke(new ExpUifDatasetLoadCmd(e.getSource().getId()));

	}

	public void onAfterRowSelect(DatasetEvent e){
		CmdInvoker.invoke(new ExpUifDatasetAfterSelectCmd(e.getSource().getId()));
	}

	
	public void onAfterBusiRowSelect(DatasetEvent e){

		//ѡ�б�����ʱ��չ�ʽ������ı��һ�н�ѡ�еڶ��У����棬�ڶ��н�����
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget =  jkbxWindow.getView("main");
		Dataset masterDs = widget.getViewModels().getDataset(getMasterDataset(widget));
		Dataset busitemDs = e.getSource();
		
		String pkItem = (String)masterDs.getSelectedRow().getValue(masterDs.nameToIndex(JKBXHeaderVO.PK_ITEM));
		
		if (pkItem!=null && !"".equals(pkItem)) {
			Map<String,String> formularMap = (HashMap<String,String>)LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute("yer_formularMap");
			Set<String> keySet = formularMap.keySet();
			for (String key : keySet) {
				YerUtil.modifyField(busitemDs, "EditFormular", key, null);
			}
		}
	}

	/**
	 * ����ҳ�� �ύ
	 * @param map
	 */
	public void pluginplugin_exetask(Map map){
		
		
		LfwView widget = AppLifeCycleContext.current().getViewContext().getView();
		//�õ���Dataset��Id
		String masterDsId = getMasterDataset(widget);
		Dataset masterDs = widget.getViewModels().getDataset(getMasterDataset(widget));
		Dataset[] slaveDss = getSlaveDatasets(widget, masterDsId);
		Datasets2AggVOSerializer ser = new ExpDatasets2AggVOSerializer();
		String aggVoClassName = getAggvo(masterDs.getVoMeta());
		AggregatedValueObject aggVo  = ser.serialize(masterDs, slaveDss, aggVoClassName);

		

		AppUtil.addAppAttr(WfmConstants.WfmAppAttr_FormInFoCtx, aggVo);
		
		
		//��У��֧��Ϊ�����һЩ���������� �ᱨ����ʾ��ʵ�������У��û�б�Ҫ��
		//ʵ�ʵ�vo�Ѿ���AppUtil.addAppAttr(WfmConstants.WfmAppAttr_FormInFoCtx, aggVo);�������
		doValidate(masterDs,slaveDss);
		
		NcWfmCmd ncWfmCmd = new ExpNcWfmCmd();
		ncWfmCmd.execute();


		ser.update(aggVo, masterDs, slaveDss);
		//�رմ���
		if(CommandStatus.SUCCESS.equals(CommandStatus.getCommandStatus())){
//			AppLifeCycleContext.current().getApplicationContext().closeWinDialog();
			AppLifeCycleContext.current().getApplicationContext().closeWinDialog("wfl");
		}
	}
	
	/**
	 * �������������б�
	 * @param map
	 */
	public void pluginplugin_fysqlist(Map map){
		FromWhereSQLImpl whereSql = (FromWhereSQLImpl) map.get("whereSql");
//		String wheresql = whereSql.getWhere();
		
		AppUtil.addAppAttr("fysq_whereSql", whereSql);
		 AppLifeCycleContext.current().getWindowContext()
		.popView("fysqlist", "850", "578", nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("yer","0E110YER-V63-001")/*@res "�������뵥�б�"*/,false);  //false �رնԻ���ʱ ������ȷ����
  
	}
	
	
	public void pluginpluginQuickShare(Map keys) {
		
		
		//��ʾ��̯������QuickShareViewController�н�����ʾ�����
		UIFlowvPanel cSharevpanel = ExpUtil.getUIFlowvPanel(IExpConst.COST_SHARE_VPANEL); //��̯�����panel������̶�
		if (cSharevpanel !=null) {
			cSharevpanel.setVisible(true);
		}
	}
	
	/**
	 * ���Է������뵥��ѯȷ��
	 * */
	public void pluginsimpleQuery_plugin(Map keys){
		
	}
	
	/**
	 * ���Է������뵥��ѯȷ��
	 * */
	public void conditionQueryPlugin(Map keys){
		
	}


	/**
	 * BusitemDs �仯
	 *
	 * @param datasetCellEvent
	 */
	public void onAfterBusitemDsChange(DatasetCellEvent datasetCellEvent) {
		UifCommand expBodyValueChangeCmd = new ExpBodyValueChangeCmd(getMasterDsId(),datasetCellEvent);
		expBodyValueChangeCmd.execute();

	}

	/**
	 * BusitemDs ɾ��
	 *
	 * @param datasetEvent
	 */
	public void onAfterBusitemDsRowDelete(DatasetEvent datasetEvent) {
		String dsID = datasetEvent.getSource().getId();
		if (IExpConst.BX_CSHARE_DS_ID.equals(dsID)) {
			return;
		}
		
		ExpBodyValueChangeCmd expBodyValueChangeCmd = new ExpBodyValueChangeCmd(getMasterDsId(),null);
		expBodyValueChangeCmd.setHeadTotalValue();
		
		/**
		 * ȷ��ɾ���е�ʱ�� ����ȷ�����ó��������Ϣ
		 */
		Dataset busitemDs = datasetEvent.getSource();
		Row row = busitemDs.getEmptyRow();
		row.setValue(busitemDs.nameToIndex(BXBusItemVO.YBJE), new UFDouble(1));
		try {  
			expBodyValueChangeCmd.doContract(busitemDs, row);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		}
	}

	/**
	 * BusitemDs ������
	 *
	 * @param rowInsertEvent
	 */
	public void onAfterBusitemDsRowInsert(RowInsertEvent rowInsertEvent) {
		int index = rowInsertEvent.getInsertedIndex();
		Dataset busitemDs = rowInsertEvent.getSource();
		Row row = busitemDs.getCurrentRowData().getRow(index);
		UFDouble amount = (UFDouble)row.getValue(busitemDs.nameToIndex(BXBusItemVO.AMOUNT));
		
		if (amount == null || UFDouble.ZERO_DBL.equals(amount)) {//���򻹿���� �ύ �ᱨ��
			return;
		}
		
		for (String je : BXBusItemVO.getBodyGlobalBbjeField()) {
			ExpUtil.setRowValue(row, busitemDs, je, null);
		}
		for (String je : BXBusItemVO.getBodyGroupBbjeField()) {
			ExpUtil.setRowValue(row, busitemDs, je, null);
		}
		for (String je : BXBusItemVO.getBodyOrgBbjeField()) {
			ExpUtil.setRowValue(row, busitemDs, je, null);
		}
		for (String je : BXBusItemVO.getYbjeField()) {
			ExpUtil.setRowValue(row, busitemDs, je, null);
		}
		
		row.setValue(busitemDs.nameToIndex(BXBusItemVO.AMOUNT), amount);
		row.setValue(busitemDs.nameToIndex(BXBusItemVO.YBJE), amount);
		
		
		ExpBodyValueChangeCmd expBodyValueChangeCmd = new ExpBodyValueChangeCmd(getMasterDsId(),null);
		expBodyValueChangeCmd.setHeadTotalValue();
		
		/**
		 * ȷ��ճ���е�ʱ�� ����ȷ�����ò��񼰳��������Ϣ
		 */
		expBodyValueChangeCmd.modifyFinValues(busitemDs.nameToIndex(BXBusItemVO.YBJE), index, busitemDs, row);
		try {  
			expBodyValueChangeCmd.doContract(busitemDs, row);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		}
		
		
	}
	
	
	/**
	 * 
	 * ���ͱ����Ļ���仯���������� 100�� 99.99 ���໥�仯������cShareGridAfterEditʵ��
	 * CshareDs �仯
	 *
	 * @param datasetCellEvent
	 */
	public void onAfterCshareDsChange(DatasetCellEvent datasetCellEvent) {
		
		Dataset cShareDs = datasetCellEvent.getSource();
		
		int rowIndex = datasetCellEvent.getRowIndex();
		if (cShareDs.getCurrentRowData().getRowCount() <= rowIndex) {//��ֹ���з�̯���ݿ���ɾ��ʱ����
			return;
		}
		Row row = cShareDs.getCurrentRowData().getRow(rowIndex);
		
		
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbxHeadDs =  jkbxWindow.getView("main").getViewModels().getDataset(getMasterDsId());
		Row headRow = jkbxHeadDs.getSelectedRow();
		UFDouble totalAmount = (UFDouble)headRow.getValue(jkbxHeadDs.nameToIndex(JKBXHeaderVO.YBJE));
		
		
		/**�е���λ�仯**/
		if (datasetCellEvent.getColIndex() == cShareDs.nameToIndex(CShareDetailVO.ASSUME_ORG)) {
			
			String [] changeField = new String[] {CShareDetailVO.PK_IOBSCLASS,CShareDetailVO.JOBID,CShareDetailVO.PROJECTTASK,CShareDetailVO.PK_RESACOSTCENTER,
					CShareDetailVO.ASSUME_DEPT,CShareDetailVO.CUSTOMER,CShareDetailVO.HBBM};
			
			for (String field:changeField) {
				ExpUtil.setRowValue(row, cShareDs, field, null);
			}
			
			YerCShareUtil.setRateAndAmount(row, cShareDs, jkbxHeadDs);
			
		}
		
		/**��Ŀ�仯**/
		if (datasetCellEvent.getColIndex() == cShareDs.nameToIndex(CShareDetailVO.JOBID)) {
			//�����Ŀ����
			ExpUtil.setRowValue(row, cShareDs, CShareDetailVO.PROJECTTASK, null);
			
		}
		
		/**��������**/
		if (datasetCellEvent.getColIndex() == cShareDs.nameToIndex(CShareDetailVO.PK_PCORG)) {
			
			//��պ���Ҫ��
			ExpUtil.setRowValue(row, cShareDs, CShareDetailVO.PK_CHECKELE, null);
			
		}
		
		
		//��̯���仯
		if (datasetCellEvent.getColIndex() == cShareDs.nameToIndex(CShareDetailVO.ASSUME_AMOUNT)) {
			
			YerCShareUtil.setRateAndAmount(row, cShareDs, jkbxHeadDs);
			
		}
		
		
			
		

	}

	/**
	 * CshareDs ɾ��
	 *
	 * @param datasetEvent
	 */
	public void onAfterCshareDsRowDelete(DatasetEvent datasetEvent) {
//		if (IExpConst.BX_CSHARE_DS_ID.equals(DsID)) {
//			
//		}
		
		Dataset cShareDs = datasetEvent.getSource();
		int count = cShareDs.getCurrentRowData().getRows().length;
		
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbxHeadDs =  jkbxWindow.getView("main").getViewModels().getDataset(getMasterDsId());
		Row headRow = jkbxHeadDs.getSelectedRow();
		UFDouble totalAmount = (UFDouble)headRow.getValue(jkbxHeadDs.nameToIndex(JKBXHeaderVO.YBJE));
		String pk_currentype = (String) headRow.getValue(jkbxHeadDs.nameToIndex(JKBXHeaderVO.BZBM));
		int currentDigit = Currency.getCurrDigit(pk_currentype);//��ǰ���־���
		
		if(count == 0){
			
			if (null == AppInteractionUtil.getConfirmDialogResult()) {
//				AppUtil.addAppAttr(IExpConst.IS_CSHARE_AVG,null);
				AppInteractionUtil.showConfirmDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("yer","0E110YER-V63-008")/*@res "ȷ��ȡ����̯"*/, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0","0201107-0006")/*@res "�Ƿ�ȡ����̯"*/);
			} else {
				// �Ƿ�ɾ��
				if (AppInteractionUtil.getConfirmDialogResult().equals(Boolean.TRUE)) {
					
					ExpUtil.setRowValue(headRow, jkbxHeadDs, BXHeaderVO.ISCOSTSHARE, UFBoolean.FALSE);
				} 
			}
			
			
			
//            int result = MessageDialog.showOkCancelDlg(getCardpanel(), null, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0","0201107-0006")/*@res "�Ƿ�ȡ����̯"*/);
//            App
//            
//            if(result==MessageDialog.ID_OK){
//                if(getBillCardPanel().getHeadItem(BXHeaderVO.ISCOSTSHARE)!=null){
//                    getBillCardPanel().getHeadItem(BXHeaderVO.ISCOSTSHARE).setValue(UFBoolean.FALSE);
//                }
//                ErmForCShareUiUtil.setCostPageShow(this.getBillCardPanel(), false);
//                this.getBillCardPanel().getHeadItem(BXHeaderVO.FYDWBM_V).getComponent().setEnabled(true);
//            }
//            ((ErmBillBillForm)getCardpanel()).setCShareChanged(false);
        }else{
//            if(!((ErmBillBillForm)getCardpanel()).isCShareChanged()){
//                ErmForCShareUiUtil.reComputeAllJeByAvg(getBillCardPanel());
//            }
//        	String isAVg = (String)AppUtil.getAppAttr(IExpConst.IS_CSHARE_AVG);
//        	String isNeedAvg = (String) AppUtil.getAppAttr(IExpConst.IS_CSHARE_AVG);
////        	Dataset oldcShareDs = (Dataset)AppUtil.getAppAttr("bx_cs_old_ds");
////        	boolean isNeedAvg = YerCShareUtil.isNeedBalanceJe(oldcShareDs,totalAmount,currentDigit);
//        	if ("Y".equals(isNeedAvg)){
//        		YerCShareUtil.reComputeAllJeByAvg(cShareDs, totalAmount,jkbxHeadDs,currentDigit);
//        	}
        }
		
	}

	/**
	 * CshareDs ������
	 *
	 * @param rowInsertEvent
	 */
	public void onAfterCshareDsRowInsert(RowInsertEvent rowInsertEvent) {
		
		
		
	}
	
	
	

	protected String getMasterDsId() {
		return IExpConst.BXZB_DS_ID;
	}

	/**
	 * �������Ϊ��
	 *
	 * @return
	 */
	private boolean getBodyNotNull() {
		return false;
	}

	private String[] getDetailDsIds() {
		String[] detailDsIds = null;

		LfwView widget = AppLifeCycleContext.current()
				.getApplicationContext().getCurrentWindowContext()
				.getViewContext("main").getView();

		if (widget.getViewModels().getDsrelations() != null) {
			DatasetRelation[] rels = widget.getViewModels().getDsrelations()
					.getDsRelations(getMasterDsId());
			if (rels != null) {
				detailDsIds = new String[rels.length];
				for (int i = 0; i < rels.length; i++) {
					detailDsIds[i] = rels[i].getDetailDataset();
				}
			}
		}
		return detailDsIds;
	}



	/**
	 * �������ݱ仯��Ĵ���
	 *
	 * @param datasetCellEvent
	 */
	public void onAfterZBDataChange(DatasetCellEvent datasetCellEvent) {
		
		UifCommand expBxzbDataChangeCmd = new ExpHeadValueChangeCmd(
				datasetCellEvent);
		expBxzbDataChangeCmd.execute();
	}

	public void pluginmain_in(Map keys) {

	}

	public void pluginDataPlugIn(Map keys) {
		String menuName = (String) keys.get(IExpConst.MENU_Key);

		if (IExpConst.MENU_ADD.equals(menuName)) { // ����
			add(null);
		} else if (IExpConst.MENU_SAVE.equals(menuName)) { // ����
			tempSave(null);
		} else if (IExpConst.MENU_EDIT.equals(menuName)) { // ����
			edit(null);
		} else if (IExpConst.MENU_COMMIT.equals(menuName)) { // �ύ
			commit(null);
		} else if (IExpConst.MENU_DELETE.equals(menuName)) { // ɾ��
		} else if (IExpConst.MENU_CJK.equals(menuName)) { // ����
			cjk(null);
		}
	}


	/**
	 * ����
	 *
	 * @param mouseEvent
	 */
	public void add(MouseEvent mouseEvent) {
		// LfwWidget widgetMain =
		// AppLifeCycleContext.current().getApplicationContext().getCurrentWindowContext().getViewContext("main").getView();
		// Dataset bxzbDS = widgetMain.getViewModels().getDataset("bxzb");
		// bxzbDS.clear();
		// Row emptyRow = bxzbDS.getEmptyRow();
		// bxzbDS.addRow(emptyRow);
		// bxzbDS.setSelectedIndex(bxzbDS.getRowIndex(emptyRow));
		// bxzbDS.setEnabled(true);

		UifCommand addCmd = new ExpUifAddCmd(getMasterDsId());
		addCmd.execute();
	}

	/**
	 * �޸�
	 *
	 * @param mouseEvent
	 */
	public void edit(MouseEvent mouseEvent) {
		UifCommand editCmd = new ExpUifEditCmd(getMasterDsId());
		editCmd.execute();
	}

	/**
	 * ɾ��
	 *
	 * @param mouseEvent
	 */
	public void delete(MouseEvent mouseEvent) {
		// UifCommand deleteCmd = new ExpUifDeleteCmd(getMasterDsId());
		// deleteCmd.execute();
	}

	/**
	 * �ύ
	 *
	 * @param mouseEvent
	 */
	public void commit(MouseEvent mouseEvent) {
		UifCommand commitCmd = new ExpUifCommitCmd(getMasterDsId(),
				getDetailDsIds(), BXVO.class.getName(), getBodyNotNull());
		commitCmd.execute();
	}

	/**
	 * �ݴ�
	 *
	 * @param mouseEvent
	 */
	public void tempSave(MouseEvent mouseEvent) {
		UifCommand tempSaveCmd = new ExpUifTempSaveCmd(getMasterDsId(),
				getDetailDsIds(), BXVO.class.getName(), getBodyNotNull());
		tempSaveCmd.execute();
	}

	/**
	 * ȡ��
	 *
	 * @param mouseEvent
	 */
	public void cancel(MouseEvent mouseEvent) {
		UifCommand cancelCmd = new ExpUifCancelCmd(getMasterDsId());
		cancelCmd.execute();
	}

	/**
	 * ����
	 *
	 * @param mouseEvent
	 */
	public void cjk(MouseEvent mouseEvent) {
		// ApplicationContext appCtx =
		// AppLifeCycleContext.current().getApplicationContext();
		// appCtx.navgateTo("cjk", "����", "850", "450", null);

		AppLifeCycleContext.current().getWindowContext()
				.popView("cjk", "850", "450", nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("per_codes","0per_codes0014")
/*@res "����"*/);
	}



	/**
	 * ��������
	 * @param scriptEvent
	 */
	public void onGridAddClick(MouseEvent scriptEvent) {
		GridComp gridComp = (GridComp)scriptEvent.getSource();
		IBodyInfo bodyInfo = new SingleBodyInfo(gridComp.getDataset());

		ExpLineAddCmd lineAddCmd = new ExpLineAddCmd(bodyInfo, getMasterDsId());
		lineAddCmd.execute();
	}


	/**
	 * ����ɾ��
	 * @param scriptEvent
	 */
	public void onGridDeleteClick(MouseEvent scriptEvent) {
		GridComp gridComp = (GridComp)scriptEvent.getSource();

//		IBodyInfo bodyInfo = new SingleBodyInfo(gridComp.getDataset());
//		UifLineDelCmd lineDelCmd = new UifLineDelCmd(bodyInfo);
		ExpLineDelCmd lineDelCmd = new ExpLineDelCmd(gridComp.getDataset());
		lineDelCmd.execute();
	}


	/**
	 * ���帴��
	 * @param scriptEvent
	 */
	public void onGridCopyClick(MouseEvent scriptEvent) {
		GridComp gridComp = (GridComp)scriptEvent.getSource();

		IBodyInfo bodyInfo = new SingleBodyInfo(gridComp.getDataset());
		UifLineCopyCmd lineCopyCmd = new UifLineCopyCmd(bodyInfo);
		lineCopyCmd.execute();
	}

	/**
	 * �������
	 * @param scriptEvent
	 */
	public void onGridInsertClick(MouseEvent scriptEvent) {
		GridComp gridComp = (GridComp)scriptEvent.getSource();
		String  dsId = gridComp.getDataset();
		LfwView widget = AppLifeCycleContext.current().getApplicationContext().getCurrentWindowContext().getViewContext("main").getView();
		Dataset ds = widget.getViewModels().getDataset(dsId);
		//������������ʱ�����ܲ�����
		if (ds.getCurrentRowCount() <=0) {
			return;
		}
		IBodyInfo bodyInfo = new SingleBodyInfo(dsId);
		ExpLineInsertCmd lineInsertCmd = new ExpLineInsertCmd(bodyInfo,getMasterDsId());
		lineInsertCmd.execute();
	}

	/**
	 * ����ճ��
	 * @param scriptEvent
	 */
	public void onGridPasteClick(MouseEvent scriptEvent) {
		GridComp gridComp = (GridComp)scriptEvent.getSource();

		IBodyInfo bodyInfo = new SingleBodyInfo(gridComp.getDataset());
		UifLinePasteCmd linePasteCmd = new UifLinePasteCmd(bodyInfo);
		linePasteCmd.execute();
	}


	/**
	 * �༭��
	 * @param scriptEvent
	 */
	public void onGridEditClick(ScriptEvent scriptEvent) {
	}
	
	public void busiGridBeforeEdit(  CellEvent gridCellEvent){
	}
	
	
	/**
	 * ���������һ��Ԫ��enter
	 * @param scriptEvent
	 */
	public void lastCellEnter(GridEvent gridEvent) {
		
		ViewContext widgetctx = AppLifeCycleContext.current().getViewContext();
		Dataset headDs = widgetctx.getView().getViewModels().getDataset(getMasterDsId());
		
		if (IExpConst.BXZB_DS_ID.equals(getMasterDsId()) || IExpConst.JKZB_DS_ID.equals(getMasterDsId())) {
			if (headDs!=null && headDs.getSelectedRow()!=null) {
				String pk_item = (String)headDs.getSelectedRow().getValue(headDs.nameToIndex(JKBXHeaderVO.PK_ITEM));
				if (pk_item != null && !"".equals(pk_item)) { //������lastCellEnter �������á�
					return;
				}
			}
		}
		
		GridComp gridComp = gridEvent.getSource();
		IBodyInfo bodyInfo = new SingleBodyInfo(gridComp.getDataset());

		ExpLineAddCmd lineAddCmd = new ExpLineAddCmd(bodyInfo, getMasterDsId());
		lineAddCmd.execute();
		
		//ƽ̨�����ϸ������һ�к󣬵�һ����Ԫ���ý��� 2012-10-25
		String widgetId = gridComp.getWidget().getId();
		  AppLifeCycleContext.current().getApplicationContext().addExecScript("pageUI.getWidget('" + widgetId + "').getComponent('" + gridComp.getId() + "').setGridInEdit(); \n");

	}


	private String getMasterDataset(LfwView widget) {
		if (null != widget.getViewModels().getDsrelations()) {
			DatasetRelation[] relationList = widget.getViewModels().getDsrelations().getDsRelations();
			if (relationList.length > 0) {
				DatasetRelation relation = relationList[0];
				String masterDataset = relation.getMasterDataset();
				return masterDataset;
			}
		}
		Dataset[] dss = widget.getViewModels().getDatasets();
		for (int i = 0; i < dss.length; i++) {
			if(dss[i] instanceof IRefDataset)
				continue;
			return dss[i].getId();
		}
		return "";
	}


	/**
	 * �����ӵ�����Ds
	 * @param widget
	 * @param masterDsId
	 * @return
	 */
	private Dataset[] getSlaveDatasets(LfwView widget, String masterDsId) {
		List slaveDsList = new ArrayList<Dataset>();
		DatasetRelation[] masterRels = widget.getViewModels().getDsrelations().getDsRelations(masterDsId);
		for (int i = 0; i < masterRels.length; i++) {
			//��ȡ�Ӷ�Ӧ�����ֵ�������õ�VO������
			DatasetRelation dr = masterRels[i];
			Dataset detailDs = widget.getViewModels().getDataset(dr.getDetailDataset());
			slaveDsList.add(detailDs);
		}
		return (Dataset[]) slaveDsList.toArray(new Dataset[0]);

	}


	private String getAggvo(String fullClassName){
		IDatasetProvider dataProvider = NCLocator.getInstance().lookup(IDatasetProvider.class);
		try {
			return dataProvider.getAggVo(fullClassName);
		} catch (LfwBusinessException e) {
			// TODO Auto-generated catch block
			LfwLogger.error(e.getMessage(), e);
		}
		return null;
	}
	
	
	
	
	




//	/**
//	 * ������
//	 *
//	 * @param mouseEvent
//	 */
//	public void busitemAdd(MouseEvent mouseEvent) {
//		MenuItem item = (MenuItem)mouseEvent.getSource();
//		String dsID = (String)item.getExtendAttributeValue(IExpConst.EXT_ATTR_BUSITEM_DS_ID);
//		IBodyInfo bodyInfo = new SingleBodyInfo(dsID);
//
//		UifLineAddCmd lineAddCmd = new ExpLineAddCmd(bodyInfo, getMasterDsId());
//		lineAddCmd.execute();
//	}
//
//	/**
//	 * �в���
//	 *
//	 * @param mouseEvent
//	 */
//	public void busitemInsert(MouseEvent mouseEvent) {
//		// TODO
//		//IBodyInfo bodyInfo = new SingleBodyInfo("busitem");
//		MenuItem item = (MenuItem)mouseEvent.getSource();
//		String dsID = (String)item.getExtendAttributeValue(IExpConst.EXT_ATTR_BUSITEM_DS_ID);
//		IBodyInfo bodyInfo = new SingleBodyInfo(dsID);
//		UifLineInsertCmd lineInsertCmd = new UifLineInsertCmd(bodyInfo);
//		lineInsertCmd.execute();
//	}
//
//	/**
//	 * ��ɾ��
//	 *
//	 * @param mouseEvent
//	 */
//	public void busitemDelete(MouseEvent mouseEvent) {
//		//IBodyInfo bodyInfo = new SingleBodyInfo("busitem");
//		MenuItem item = (MenuItem)mouseEvent.getSource();
//		String dsID = (String)item.getExtendAttributeValue(IExpConst.EXT_ATTR_BUSITEM_DS_ID);
//		IBodyInfo bodyInfo = new SingleBodyInfo(dsID);
//		UifLineDelCmd lineDelCmd = new UifLineDelCmd(bodyInfo);
//		lineDelCmd.execute();
//	}
//
//	public void busitemCopy(MouseEvent mouseEvent) {
//
//	}
//
//	public void busitemPaste(MouseEvent mouseEvent) {
//
//	}
	
	protected void doValidate(Dataset masterDs,Dataset[] detailDs) throws LfwValidateException {
		IDataValidator validator = getValidator();
		validator.validate(masterDs, new LfwView());
		if (detailDs != null) {
			int size = detailDs.length;
			if (size > 0) {
				for (int i = 0; i < size; i++) {
					Dataset ds = detailDs[i];
					validator.validate(ds, new LfwView());
//					if (notNullBodyList != null && notNullBodyList.contains(ds.getId())) {
//						doSingleValidateBodyNotNull(ds);
//					}
				}
//				if (bodyNotNull) {
//					doValidateBodyNotNull(detailDs);
//				}
			}
		}
	}
	
	protected IDataValidator getValidator() {
		return new DefaultDataValidator();
	}
	
	
	
	/**
	 * һ��ֵ�ı仯������һ��ֵ�ı仯 �����ٵ��õ��÷�����

	 * @param gridCellEvent
	 */
	public void cShareGridAfterEdit(  CellEvent gridCellEvent){
		

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbxHeadDs =  jkbxWindow.getView("main").getViewModels().getDataset(getMasterDsId());
		
		Dataset cShareDs = jkbxWindow.getView("main").getViewModels().getDataset(IExpConst.BX_CSHARE_DS_ID);
		
		int rowIndex = gridCellEvent.getRowIndex();
		if (cShareDs.getCurrentRowData().getRowCount() <= rowIndex) {//��ֹ���з�̯���ݿ���ɾ��ʱ����
			return;
		}
		Row row = cShareDs.getCurrentRowData().getRow(rowIndex);
		
		
		Row headRow = jkbxHeadDs.getSelectedRow();
		UFDouble totalAmount = (UFDouble)headRow.getValue(jkbxHeadDs.nameToIndex(JKBXHeaderVO.YBJE));
		
		
		int gidColIndex = gridCellEvent.getColIndex();
		
		GridColumn  GridColumn= (GridColumn)gridCellEvent.getSource().getColumn(gidColIndex);
			
//		row.setValue(cShareDs.nameToIndex(GridColumn.getField()), gridCellEvent.getNewValue());
		
		
		
		
		/**�е���λ�仯**/
		if (CShareDetailVO.ASSUME_ORG.equals(GridColumn.getField())) {
			
			ExpUtil.setRowValue(row, cShareDs, CShareDetailVO.ASSUME_DEPT, null);
			ExpUtil.setRowValue(row, cShareDs, CShareDetailVO.PK_IOBSCLASS, null);
			
		}
		
		//��̯���仯
		if (CShareDetailVO.ASSUME_AMOUNT.equals(GridColumn.getField())) {
			
			//grid AfterEdit row�����ݲ�����³��޸ĺ�ģ����Դ˴��ֶ������޸ĵ�ֵ
//			ExpUtil.setRowValue(row, cShareDs, GridColumn.getField(),new UFDouble((String)gridCellEvent.getNewValue()));
//			
//			YerCShareUtil.resetRatioByJe(rowIndex, cShareDs, row, totalAmount, true);
//			
//			if (!gridCellEvent.getNewValue().equals(gridCellEvent.getOldValue()) && !"".equals(gridCellEvent.getOldValue()) ) {//��һ�ε����Ԫ��û�޸Ļᴥ���¼�(oldΪ"" newΪ��Ԫ���ϵ�ֵ)����ʱ�����ñ�־
//				
//				AppUtil.addAppAttr(IExpConst.IS_CSHARE_AVG,"N"); //��̯���ͱ����ֶ��޸ĺ����к�ɾ�в����Զ�����ƽ��ֵ
//			}
			
		}
		
		
		//��̯�����仯
		if (CShareDetailVO.SHARE_RATIO.equals(GridColumn.getField())) {
//			ExpUtil.setRowValue(row, cShareDs, GridColumn.getField(),new UFDouble((String)gridCellEvent.getNewValue()));
//			
//			YerCShareUtil.resetJeByRatio(rowIndex, cShareDs, row, totalAmount, false);
//			if (!gridCellEvent.getNewValue().equals(gridCellEvent.getOldValue()) && !"".equals(gridCellEvent.getOldValue()) ) {
//				AppUtil.addAppAttr(IExpConst.IS_CSHARE_AVG,"N");//��̯���ͱ����ֶ��޸ĺ����к�ɾ�в����Զ�����ƽ��ֵ
//			}
			
		}
		
	  }
	  public void cShareGridCellEdit(  CellEvent gridCellEvent){
		  
	  }
}