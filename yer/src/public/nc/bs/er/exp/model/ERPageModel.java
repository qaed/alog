package nc.bs.er.exp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import nc.bs.er.exp.quickshare.listeners.ref.ShareRuleRefListener;
import nc.bs.er.exp.util.ExpRegisterUtil;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.er.util.YerUtil;
import nc.bs.erm.util.ErBudgetUtil;
import nc.bs.erm.util.ErUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.framework.exception.ComponentException;
import nc.bs.logging.Logger;
import nc.itf.arap.prv.IBXBillPrivate;
import nc.itf.er.pub.IArapBillTypePublic;
import nc.itf.erm.proxy.ErmProxy;
import nc.itf.tb.control.IAccessableBusiVO;
import nc.itf.tb.control.IBudgetControl;
import nc.itf.tb.control.ILinkQuery;
import nc.uap.lfw.core.AppSession;
import nc.uap.lfw.core.LfwRuntimeEnvironment;
import nc.uap.lfw.core.WebContext;
import nc.uap.lfw.core.comp.FormComp;
import nc.uap.lfw.core.comp.GridColumn;
import nc.uap.lfw.core.comp.GridComp;
import nc.uap.lfw.core.comp.IGridColumn;
import nc.uap.lfw.core.comp.MenuItem;
import nc.uap.lfw.core.comp.MenubarComp;
import nc.uap.lfw.core.comp.WebComponent;
import nc.uap.lfw.core.comp.WebElement;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.Field;
import nc.uap.lfw.core.data.FieldRelations;
import nc.uap.lfw.core.data.FieldSet;
import nc.uap.lfw.core.data.LfwParameter;
import nc.uap.lfw.core.data.UnmodifiableMdField;
import nc.uap.lfw.core.event.conf.DatasetRule;
import nc.uap.lfw.core.event.conf.EventConf;
import nc.uap.lfw.core.event.conf.EventSubmitRule;
import nc.uap.lfw.core.event.conf.FormRule;
import nc.uap.lfw.core.event.conf.ViewRule;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.model.PageModel;
import nc.uap.lfw.core.page.Connector;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.page.LfwWindow;
import nc.uap.lfw.core.page.PluginDesc;
import nc.uap.lfw.core.page.ViewComponents;
import nc.uap.lfw.core.page.ViewMenus;
import nc.uap.lfw.core.page.ViewModels;
import nc.uap.lfw.core.refnode.MasterFieldInfo;
import nc.uap.lfw.core.refnode.NCRefNode;
import nc.uap.lfw.core.refnode.RefNode;
import nc.uap.lfw.core.refnode.RefNodeRelation;
import nc.uap.lfw.core.refnode.RefNodeRelations;
import nc.uap.lfw.core.uimodel.WindowConfig;
import nc.uap.lfw.jsp.uimeta.UIElement;
import nc.uap.lfw.jsp.uimeta.UIFlowvLayout;
import nc.uap.lfw.jsp.uimeta.UIFlowvPanel;
import nc.uap.lfw.jsp.uimeta.UILayoutPanel;
import nc.uap.lfw.jsp.uimeta.UIMeta;
import nc.uap.lfw.jsp.uimeta.UIView;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.er.djlx.DjLXVO;
import nc.vo.er.linkntb.LinkNtbParamVO;
import nc.vo.erm.control.YsControlVO;
import nc.vo.erm.util.ErVOUtils;
import nc.vo.fibill.outer.FiBillAccessableBusiVO;
import nc.vo.fibill.outer.FiBillAccessableBusiVOProxy;
import nc.vo.fipub.rulecontrol.RuleDataCacheEx;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.tb.control.DataRuleVO;
import nc.vo.tb.obj.NtbParamVO;
import org.apache.commons.lang.StringUtils;

public abstract class ERPageModel extends PageModel {
	public ERPageModel() {
	}

	protected void initPageMetaStruct() {
		super.initPageMetaStruct();

		UIMeta uiMeta = (UIMeta) getUIMeta();

		String includejs = uiMeta.getIncludejs();
		if (!StringUtils.isEmpty(includejs)) {
			includejs = includejs + ",";
		} else {
			includejs = "";
		}

		uiMeta.setIncludejs(includejs + "../sync/yer/weberm/html/nodes/includejs/themes/webclassic/ermybill/erbill.js");

		UIFlowvPanel cSharevpanel = ExpUtil.getUIFlowvPanel("csharevpanel");
		if (cSharevpanel != null) {
		}

		String flowTypePk = LfwRuntimeEnvironment.getWebContext().getAppSession().getOriginalParameter("billType");
		String taskPk = LfwRuntimeEnvironment.getWebContext().getAppSession().getOriginalParameter("taskPk");
		AppUtil.addAppAttr("$$$$$$$$FLOWTYPEPK", flowTypePk);
		AppUtil.addAppAttr("$$$$$$$$TaskPk", taskPk);
		String billId = LfwRuntimeEnvironment.getWebContext().getOriginalParameter("openBillId");
		AppUtil.addAppAttr("billId", billId);
		AppUtil.addAppAttr("NC", "Y");

		DjLXVO djLXVO = null;
		try {
			djLXVO =
					((IArapBillTypePublic) NCLocator.getInstance().lookup(IArapBillTypePublic.class)).getDjlxvoByDjlxbm(flowTypePk, YerUtil.getPK_group());
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
		}
		if (djLXVO == null) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000171"));
		}

		UFBoolean fcbz = djLXVO.getFcbz();
		if ((fcbz != null) && (fcbz.booleanValue())) {
			throw new LfwRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000429"));
		}

		String appName = (String) LfwRuntimeEnvironment.getWebContext().getAppSession().getAttribute("appId");
		String nodecode = (String) LfwRuntimeEnvironment.getWebContext().getAppSession().getAttribute("nodecode");
		AppUtil.addAppAttr("DJURL", "app/" + appName + "?billType=" + flowTypePk + "&nodecode=" + nodecode);

		AppUtil.addAppAttr("CURRENT_MASTER_DS", getDatasetID());

		LfwView mainWidget = getPageMeta().getWidget("main");

		saveBusitemDsFormular(mainWidget);

		LfwView quickShareView = getPageMeta().getView("quickshare");

		if (quickShareView != null) {
			NCRefNode quickShareRefNode = (NCRefNode) quickShareView.getViewModels().getRefNode("refnode_quickshare_rule");
			if (quickShareRefNode != null) {
				quickShareRefNode.setDataListener(ShareRuleRefListener.class.getName());
			}
		}

		String djzt = LfwRuntimeEnvironment.getWebContext().getOriginalParameter("djzt");
		if ((djzt != null) && (!"0".equals(djzt)) && (!"1".equals(djzt))) {
			WebComponent[] components = mainWidget.getViewComponents().getComponents();
			if (components != null) {
				for (WebComponent com : components) {
					if ((com instanceof FormComp)) {
						((FormComp) com).setRenderType(6);
						((FormComp) com).setLabelMinWidth(153);
					}

					if ((com instanceof GridComp)) {
						((GridComp) com).setShowImageBtn(false);
					}

				}

			}

		} else {
			WebComponent[] components = mainWidget.getViewComponents().getComponents();
			if (components != null) {
				for (WebComponent com : components) {
					if ((com instanceof FormComp)) {
						((FormComp) com).setRenderType(5);
						((FormComp) com).setLabelMinWidth(153);
					}
				}
			}
		}

		WebComponent[] components = mainWidget.getViewComponents().getComponents();
		if (components != null) {
			for (WebComponent com : components) {
				if ((com instanceof GridComp)) {

					String gridDsId = ((GridComp) com).getDataset();
					Dataset gridDs = mainWidget.getViewModels().getDataset(gridDsId);

					for (IGridColumn column : ((GridComp) com).getColumnList()) {
						GridColumn thisColumn = (GridColumn) column;

						if ("DecimalText".equals(thisColumn.getEditorType())) {
							Field field = gridDs.getFieldSet().getField(thisColumn.getField());

							YerUtil.modifyField(gridDs, "Precision", field.getId(), "8");
						}

						if ("IntegerText".equals(thisColumn.getEditorType())) {

							Field field = gridDs.getFieldSet().getField(thisColumn.getField());
							if ((field instanceof UnmodifiableMdField)) {
								Field newField = ((UnmodifiableMdField) field).getMDField();
								newField.setDataType("Integer");
								gridDs.getFieldSet().updateField(newField.getId(), newField);
							} else {
								field.setDataType("Integer");
							}
						}

						if (("String".equals(thisColumn.getDataType())) || ("SelfDefine".equals(thisColumn.getDataType()))) {
							thisColumn.setMaxLength("101");
						}
					}

					if ("contrast_grid".equals(com.getId())) {
						((GridComp) com).setShowImageBtn(false);
					} else {
						MenubarComp menuBarComp = ((GridComp) com).getMenuBar();
						if (menuBarComp != null) {
							List<MenuItem> menuList = menuBarComp.getMenuList();
							for (int i = 0; i < menuList.size(); i++) {
								MenuItem item = (MenuItem) menuList.get(i);

								if (item.getId().endsWith("HeaderBtn_Edit")) {
									menuList.remove(i);
									i--;
								}

								if (item.getId().endsWith("HeaderBtn_Add")) {

									menuList.remove(i);
									i--;
								}

								if (item.getId().endsWith("HeaderBtn_Delete")) {

									menuList.remove(i);
									i--;
								}
							}
						}

						initExpGridMenubar((GridComp) com);
					}
				}
			}
		}

		ExpRegisterUtil.registerListenerAll(this);

		ExpRegisterUtil.registerListener(this);

		LfwWindow meta = getPageMeta();
		String dsID = getDatasetID();
		Dataset masterDs = meta.getWidget("main").getViewModels().getDataset(dsID);

		Field costShareField = masterDs.getFieldSet().getField("iscostshare");
		if ((costShareField != null) && ("UFBoolean".equals(costShareField.getDataType())) && ("N".equals(costShareField.getDefaultValue()))) {
			if ((costShareField instanceof UnmodifiableMdField)) {
				Field newField = ((UnmodifiableMdField) costShareField).getMDField();
				newField.setDefaultValue(UFBoolean.FALSE);
				masterDs.getFieldSet().updateField(newField.getId(), newField);
			} else {
				costShareField.setDefaultValue(UFBoolean.FALSE);
			}
		}

		Field expamtField = masterDs.getFieldSet().getField("isexpamt");
		if ((expamtField != null) && ("UFBoolean".equals(expamtField.getDataType())) && ("N".equals(expamtField.getDefaultValue()))) {
			if ((expamtField instanceof UnmodifiableMdField)) {
				Field newField = ((UnmodifiableMdField) expamtField).getMDField();
				newField.setDefaultValue(UFBoolean.FALSE);
				masterDs.getFieldSet().updateField(newField.getId(), newField);
			} else {
				expamtField.setDefaultValue(UFBoolean.FALSE);
			}
		}

		boolean hasBusitemGrid = ExpUtil.hasBusitemGrid(mainWidget, masterDs);
		boolean hasBusiGridInUIMeta = ExpUtil.isHasBusiGridInUIMeta();
		if ((!hasBusitemGrid) || (!hasBusiGridInUIMeta)) {
			AppUtil.addAppAttr("ExpHasBusitemGrid", "N");
		}

		Field field = masterDs.getFieldSet().getField("zy");
		if (field != null) {
			field.setExtendAttribute(Field.MAX_LENGTH, "256");
		}

		RefNode zyRef = (RefNode) meta.getWidget("main").getViewModels().getRefNode("refnode_bxzb_zy_summaryname");
		if (zyRef == null) {
			zyRef = (RefNode) meta.getWidget("main").getViewModels().getRefNode("refnode_jkzb_zy_summaryname");
		}

		if (zyRef != null) {
			zyRef.setReadFields("summaryname,summaryname");
			masterDs.getFieldRelations().removeFieldRelation("zy_mc_rel");
			masterDs.getFieldRelations().removeFieldRelation("zy_rel");

			zyRef.setAllowInput(true);
		}

		List<EventConf> eventConfList = mainWidget.getEventConfList();
		if (eventConfList != null) {
			EventConf eventConf = (EventConf) eventConfList.get(0);
			if ((eventConf != null) && ("pluginplugin_exetask".equals(eventConf.getMethodName()))) {
				eventConf.setName("approvePlugin");
				addWebElementEvent(mainWidget, "approvePlugin", "pluginplugin_exetask", null);
			}
		}

		addWebElementEvent(masterDs, "onAfterDataChange", "onAfterZBDataChange", "nc.uap.lfw.core.event.conf.DatasetListener");
		addWebElementEvent(masterDs, "onDataLoad", "onDataLoad", "nc.uap.lfw.core.event.conf.DatasetListener");
		addWebElementEvent(masterDs, "onAfterRowSelect", "onAfterRowSelect", "nc.uap.lfw.core.event.conf.DatasetListener");

		Dataset[] busitemDss = ExpUtil.getBusitemDss(meta.getWidget("main"), getDatasetID());
		if (busitemDss != null) {
			for (Dataset busitemDs : busitemDss) {
				addWebElementEvent(busitemDs, "onAfterDataChange", "onAfterBusitemDsChange", "nc.uap.lfw.core.event.conf.DatasetListener");
				addWebElementEvent(busitemDs, "onAfterRowDelete", "onAfterBusitemDsRowDelete", "nc.uap.lfw.core.event.conf.DatasetListener");
				addWebElementEvent(busitemDs, "onAfterRowInsert", "onAfterBusitemDsRowInsert", "nc.uap.lfw.core.event.conf.DatasetListener");
			}
		}

		LfwView jkbxMenuWidget = getPageMeta().getWidget("bx_menu");
		if (jkbxMenuWidget == null)
			jkbxMenuWidget = getPageMeta().getWidget("jk_menu");
		MenubarComp jkbxMenubar;
		if ("bx_menu".equals(jkbxMenuWidget.getId())) {
			jkbxMenubar = jkbxMenuWidget.getViewMenus().getMenuBar("bxzb_menu");
		} else {
			jkbxMenubar = jkbxMenuWidget.getViewMenus().getMenuBar("jkzb_menu");
		}
		addWebElementEvent(jkbxMenubar.getItem("commit"), "onclick", "commit", "nc.uap.lfw.core.event.conf.MouseListener");
		addWebElementEvent(jkbxMenubar.getItem("tempsave"), "onclick", "tempSave", "nc.uap.lfw.core.event.conf.MouseListener");
		addWebElementEvent(jkbxMenubar.getItem("print"), "onclick", "print", "nc.uap.lfw.core.event.conf.MouseListener");
		addWebElementEvent(jkbxMenubar.getItem("copy"), "onclick", "copy", "nc.uap.lfw.core.event.conf.MouseListener");

		addInlineAdvqueryWinConnector(jkbxMenuWidget);

		LfwView fysqlistView = getPageMeta().getView("fysqlist");
		addInlineAdvqueryWinConnector(fysqlistView);

		if (fysqlistView != null) {
			Dataset fysqzb = fysqlistView.getViewModels().getDataset("fysqzb");
			Field pk_tradetype_text = fysqzb.getFieldSet().getField(fysqzb.nameToIndex("pk_tradetype_text"));
			if (pk_tradetype_text != null) {
				String billtypename = ExpUtil.getCurrentLangNameColumn("billtypename");
				String formular =
						"pk_tradetype_text->getColValue2(bd_billtype," + billtypename + "," + "pk_billtypecode ,pk_tradetype,pk_group,pk_group)";
				pk_tradetype_text.setLoadFormular(formular);
			}
		}

		LfwView appRoveFysqInfoView = getPageMeta().getView("approvefysqinfo");
		if (appRoveFysqInfoView != null) {
			Dataset fysqzb = appRoveFysqInfoView.getViewModels().getDataset("fysqzb");
			Field pk_tradetype_text = fysqzb.getFieldSet().getField(fysqzb.nameToIndex("pk_tradetype_text"));
			if (pk_tradetype_text != null) {
				String billtypename = ExpUtil.getCurrentLangNameColumn("billtypename");
				String formular =
						"pk_tradetype_text->getColValue2(bd_billtype," + billtypename + "," + "pk_billtypecode ,pk_tradetype,pk_group,pk_group)";
				pk_tradetype_text.setLoadFormular(formular);
			}
		}

		String pageFlag = LfwRuntimeEnvironment.getWebContext().getParameter("sourcePage");
		UIFlowvLayout flowvLayout = (UIFlowvLayout) uiMeta.getElement();
		List<UILayoutPanel> list = flowvLayout.getPanelList();
		if ((pageFlag != null) && ("image".equals(pageFlag))) {
			List<UILayoutPanel> removePanelList = new ArrayList();
			for (UILayoutPanel panel : list) {
				UIView widget = (UIView) panel.getElement();
				if (widget != null) {

					String id = widget.getId();

					if ("pubview_exetask".equals(id)) {
						removePanelList.add(panel);
					}

					if (("bx_menu".equals(id)) || ("jk_menu".equals(id))) {
						LfwView menuWidget = getPageMeta().getWidget(id);

						MenubarComp[] menubars = menuWidget.getViewMenus().getMenuBars();
						List<MenuItem> menuList;
						for (MenubarComp menubar : menubars) {
							menuList = menubar.getMenuList();
							List<MenuItem> removeMenuList = new ArrayList();
							for (MenuItem item : menuList) {
								if (!"filemanager".equals(item.getId())) {
									removeMenuList.add(item);
								}
							}

							for (MenuItem item : removeMenuList) {
								menuList.remove(item);
							}
						}
					}
				}
			}
			for (UILayoutPanel panel : removePanelList) {
				flowvLayout.removePanel(panel);
			}

			if (components != null) {
				for (WebComponent com : components) {
					if ((com instanceof FormComp)) {
						((FormComp) com).setRenderType(6);
						((FormComp) com).setLabelMinWidth(153);
					}

					if ((com instanceof GridComp)) {
						((GridComp) com).setShowImageBtn(false);

						List<IGridColumn> columnList = ((GridComp) com).getColumnList();

						for (IGridColumn gridColumn : columnList) {
							((GridColumn) gridColumn).setEditable(false);
						}
					}
				}
			}
		}

		if ("2647".equals(djLXVO.getDjlxbm())) {
			if (list != null) {
				for (UILayoutPanel panel : list) {
					UIView widget = (UIView) panel.getElement();
					if (widget != null) {

						String id = widget.getId();
						if ("main".equals(id)) {
							UIMeta hkUiMeta = widget.getUimeta();

							UIFlowvLayout hkLayout = (UIFlowvLayout) hkUiMeta.getElement();

							List<UILayoutPanel> removePanelList = new ArrayList();
							List<UILayoutPanel> hkPanelList = hkLayout.getPanelList();

							for (UILayoutPanel hkPanel : hkPanelList) {
								if ("panelv10322".equals(hkPanel.getId())) {
									removePanelList.add(hkPanel);
								}
							}
							for (UILayoutPanel ppp : removePanelList)
								hkLayout.removePanel(ppp);
						}
					}
				}
			}
			UIFlowvLayout hkLayout;
			if (jkbxMenubar.getItem("addgroup") != null) {
				List<MenuItem> menuList = jkbxMenubar.getItem("addgroup").getChildList();
				jkbxMenubar.getItem("addgroup").getChildList().removeAll(menuList);

				MenuItem addgroup = jkbxMenubar.getItem("addgroup");
				addgroup.setId("add");
				addWebElementEvent(addgroup, "onclick", "add", "nc.uap.lfw.core.event.conf.MouseListener");
			}

			MenuItem quickshareItem = jkbxMenubar.getItem("quickshare");
			jkbxMenubar.getMenuList().remove(quickshareItem);
		}

		if ((pageFlag == null) || (!"workflow".equals(pageFlag))) {
			for (int i = 0; i < list.size(); i++) {
				UILayoutPanel panel = (UILayoutPanel) list.get(i);

				UIElement element = panel.getElement();
				if ((element instanceof UIView)) {
					UIView widget = (UIView) element;
					String id = widget.getId();
					if ("pubview_exetask".equals(id)) {
						flowvLayout.removePanel(panel);
						i--;
					}
					if ("approvefysqinfo".equals(id)) {
						flowvLayout.removePanel(panel);
						i--;
					}
				}
			}
		}

		if ("workflow".equals(pageFlag)) {
			AppUtil.addAppAttr("pageFlag", "workflow");

			LfwView menuWidget = getPageMeta().getWidget("bx_menu");
			if (menuWidget == null)
				menuWidget = getPageMeta().getWidget("jk_menu");
			MenubarComp menubar;
			if ("bx_menu".equals(menuWidget.getId())) {
				menubar = menuWidget.getViewMenus().getMenuBar("bxzb_menu");
			} else {
				menubar = menuWidget.getViewMenus().getMenuBar("jkzb_menu");
			}

			MenuItem item = menubar.getItem("commit");
			if (item != null) {
				item.setI18nName("p_bx_menu-000005");
			}

			item = menubar.getItem("tempsave");
			List<MenuItem> menuList = menubar.getMenuList();
			menuList.remove(item);

			doApproveYsinfo();
			doApproveFysqinfo();
		}

		addBusitemGridEvent(mainWidget);

		Dataset cShareDs = mainWidget.getViewModels().getDataset("bx_cshare_detail");
		if (cShareDs != null) {
			Field cShareField = cShareDs.getFieldSet().getField("pk_costshare");
			if (cShareField != null) {

				if ((cShareField instanceof UnmodifiableMdField)) {
					Field newField = ((UnmodifiableMdField) cShareField).getMDField();
					newField.setNullAble(true);
					cShareDs.getFieldSet().updateField(newField.getId(), newField);
				} else {
					cShareField.setNullAble(true);
				}
			}
		}

		EventConf eventConf = masterDs.getEventConf("onAfterDataChange", "onAfterZBDataChange");

		String[] changFields =
				{ "bbhl", "bzbm", JKBXHeaderVO.DEPTID_V, "djrq", "dwbm", JKBXHeaderVO.DWBM_V, JKBXHeaderVO.FYDEPTID_V, "fydwbm", JKBXHeaderVO.FYDWBM_V, "iscostshare", "isexpamt", "jkbxr", "pk_org", "pk_org_v", JKBXHeaderVO.PK_PAYORG_V, JKBXHeaderVO.PK_PCORG_V, "receiver", "szxmid", "total", "ybje", "jobid", "hbbm", "customer", "projecttask", "pk_pcorg", "pk_checkele", JKBXHeaderVO.PK_RESACOSTCENTER, "fydeptid", "cashproj", "freecust", "groupbbhl", "globalbbhl" };

		String changFieldStr = "";
		for (int i = 0; i < changFields.length; i++) {
			changFieldStr = changFieldStr + changFields[i];
			changFieldStr = changFieldStr + ",";
		}
		changFieldStr = changFieldStr.substring(0, changFieldStr.length() - 1);
		LfwParameter parameter = new LfwParameter("dataset_field_id", changFieldStr);
		eventConf.addExtendParam(parameter);

		String busitemDsID = "";
		if ("bxzb".equals(getDatasetID())) {
			busitemDsID = "busitem";
		} else {
			busitemDsID = "jk_busitem";
		}

		Map<String, String> formularMap =
				(HashMap) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute("yer_formularMap");
		Set<String> keySet = formularMap.keySet();
		// 20170712 tsy add defitem25
		String busitemChangFieldStr = "amount,ybje,pk_reimtype,jobid,pk_pcorg,pk_pcorg_v,defitem25";
		// 20170712 end
		for (String key : keySet) {
			busitemChangFieldStr = busitemChangFieldStr + ",";
			busitemChangFieldStr = busitemChangFieldStr + key;
		}

		Dataset busitemDs = mainWidget.getViewModels().getDataset(busitemDsID);
		EventConf busitemDsChangeEventConf = busitemDs.getEventConf("onAfterDataChange", "onAfterBusitemDsChange");
		LfwParameter busitemParameter = new LfwParameter("dataset_field_id", busitemChangFieldStr);
		busitemDsChangeEventConf.addExtendParam(busitemParameter);

		if (cShareDs != null) {
			EventConf cShareDsChangeEventConf = cShareDs.getEventConf("onAfterDataChange", "onAfterCshareDsChange");
			LfwParameter cShareDsParameter = new LfwParameter("dataset_field_id", "assume_org,assume_amount,jobid,pk_pcorg");
			if (cShareDsChangeEventConf != null) {
				cShareDsChangeEventConf.addExtendParam(cShareDsParameter);
			}
		}

		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getRuledatamap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getRulesmap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getBillruleMap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getFactorruleMap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getAccasoaruleMap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getBusinormruleMap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getRule_assmapMap().clear();
		RuleDataCacheEx.getInstance().getRule_assid_valMap().clear();
		RuleDataCacheEx.getInstance();
		RuleDataCacheEx.getItembindmap().clear();
	}

	private void modifySubmitRule() {
		LfwView menuWidget = getPageMeta().getWidget("bx_menu");
		if (menuWidget == null)
			menuWidget = getPageMeta().getWidget("jk_menu");
		MenubarComp menubar;
		if ("bx_menu".equals(menuWidget.getId())) {
			menubar = menuWidget.getViewMenus().getMenuBar("bxzb_menu");
		} else {
			menubar = menuWidget.getViewMenus().getMenuBar("jkzb_menu");
		}

		EventConf eventConfig = menubar.getItem("tempsave").getEventConf("onclick", "tempSave");

		EventSubmitRule submitRule = eventConfig.getSubmitRule();

		ViewRule wr = submitRule.getWidgetRule("main");

		FormRule fr = new FormRule();
		if ("bx_menu".equals(menuWidget.getId())) {
			fr.setId("bxzb_base_info_form");
		} else {
			fr.setId("jkzb_base_info_form");
		}
		fr.setType("all_child");
		if (wr != null) {
			wr.addFormRule(fr);
		}
	}

	private void addBodyMenuAubmitRule(MenuItem item, String methodName, String DsId) {
		EventConf eventConfig = item.getEventConf("onclick", methodName);

		EventSubmitRule submitRule = eventConfig.getSubmitRule();
		if (submitRule == null) {
			submitRule = new EventSubmitRule();
			eventConfig.setSubmitRule(submitRule);
		}

		ViewRule wr = new ViewRule();
		wr.setId("main");

		DatasetRule dsRule = new DatasetRule();
		dsRule.setId(DsId);
		dsRule.setType("ds_current_page");

		wr.addDsRule(dsRule);
		submitRule.addWidgetRule(wr);
	}

	public abstract String getDatasetID();

	public abstract String getMenuViewName();

	private void addRefNodeRelation(String masterField, String slaveFieldId, LfwWindow meta) {
		Dataset masterDs = meta.getWidget("main").getViewModels().getDataset(getDatasetID());
		if (masterDs.getFieldSet().getField(slaveFieldId) != null) {
			RefNodeRelation rnr = new RefNodeRelation();
			rnr.setId("relation_" + masterField + "_" + slaveFieldId);
			rnr.setDetailRefNode("refnode_" + getDatasetID() + "_" + slaveFieldId + "_name");
			MasterFieldInfo mfi = new MasterFieldInfo();
			mfi.setDsId(getDatasetID());
			mfi.setFieldId(masterField);
			mfi.setFilterSql("1=1");

			mfi.setNullProcess("ignore");
			rnr.addMasterFieldInfo(mfi);
			RefNodeRelations refNodeRelations = meta.getWidget("main").getViewModels().getRefNodeRelations();

			if (refNodeRelations != null) {
				refNodeRelations.addRefNodeRelation(rnr);
			} else {
				RefNodeRelations newRefNodeRelations = new RefNodeRelations();
				meta.getWidget("main").getViewModels().setRefnodeRelations(newRefNodeRelations);
				newRefNodeRelations.addRefNodeRelation(rnr);
			}
		}
	}

	private void initExpGridMenubar(GridComp gc) {
		MenubarComp menubarComp = gc.getMenuBar();
		if (menubarComp == null) {
			return;
		}
		if (menubarComp.getMenuList().size() > 4) {
			return;
		}
		String[] itemIds = { "new_row", "delete_row", "insert_row", "copy_row", "paste_row" };

		String[] eventMethodNames = { "onGridAddClick", "onGridDeleteClick", "onGridInsertClick", "onGridCopyClick", "onGridPasteClick" };
		for (int i = 0; i < itemIds.length; i++) {
			MenuItem item = new MenuItem(itemIds[i]);
			item.setI18nName(itemIds[i]);
			item.setTipI18nName(itemIds[i]);
			item.setLangDir("lfwbuttons");

			item.setShowModel(2);

			EventConf event = new EventConf();
			event.setMethodName(eventMethodNames[i]);

			event.setOnserver(true);
			event.setName("onclick");
			item.addEventConf(event);
			gc.getMenuBar().addMenuItem(item);

			addBodyMenuAubmitRule(item, eventMethodNames[i], gc.getDataset());
		}
	}

	private void modifyMasterDsSubmitRule(Dataset masterDs, String eventName, String methodName) {
		EventConf event = masterDs.getEventConf(eventName, methodName);
		ViewRule wr;
		if (event != null) {
			EventSubmitRule submitRule = event.getSubmitRule();

			if (submitRule == null) {
				submitRule = new EventSubmitRule();
				event.setSubmitRule(submitRule);
			}

			wr = submitRule.getWidgetRule("main");
			if (wr == null) {
				wr = new ViewRule();
				wr.setId("main");
				submitRule.addWidgetRule(wr);
			}

			List<String> DsID = ExpUtil.getSlaveDsIDs(getPageMeta().getWidget("main"), masterDs.getId());
			for (String dsID : DsID) {
				if ((!"contrast".equals(dsID)) && (!"jkcontrast".equals(dsID))) {

					DatasetRule dsRule = new DatasetRule();
					dsRule.setId(dsID);
					dsRule.setType("ds_current_page");
					wr.addDsRule(dsRule);
				}
			}
		}
	}

	protected void addWebElementEvent(WebElement webElement, String eventName, String methodName, String JsEventClaszz) {
		if (webElement == null) {
			return;
		}

		EventConf event = webElement.getEventConf(eventName, methodName);
		if (event == null) {
			event = new EventConf();
			event.setMethodName(methodName);

			event.setOnserver(true);
			event.setName(eventName);
			webElement.addEventConf(event);
		}

		EventSubmitRule submitRule = event.getSubmitRule();

		if (submitRule == null) {
			submitRule = new EventSubmitRule();
			event.setSubmitRule(submitRule);
		}

		ViewRule wr = submitRule.getWidgetRule("main");
		if (wr == null) {
			wr = new ViewRule();
			wr.setId("main");
			submitRule.addWidgetRule(wr);
		}

		List<String> DsID = ExpUtil.getSlaveDsIDs(getPageMeta().getWidget("main"), getDatasetID());
		for (String dsID : DsID) {
			DatasetRule dsRule = new DatasetRule();
			dsRule.setId(dsID);
			dsRule.setType("ds_current_page");
			wr.addDsRule(dsRule);
		}
	}

	/**
	 * @deprecated
	 */
	protected void addMenuItemEvent(MenuItem menuItem, String eventName, String methodName) {
		if (menuItem == null) {
			return;
		}

		EventConf event = menuItem.getEventConf(eventName, methodName);
		if (event == null) {
			event = new EventConf();
			event.setMethodName(methodName);

			event.setOnserver(true);
			event.setName(eventName);
			menuItem.addEventConf(event);
		}

		EventSubmitRule submitRule = event.getSubmitRule();

		if (submitRule == null) {
			submitRule = new EventSubmitRule();
			event.setSubmitRule(submitRule);
		}

		ViewRule wr = submitRule.getWidgetRule("main");
		if (wr == null) {
			wr = new ViewRule();
			wr.setId("main");
			submitRule.addWidgetRule(wr);
		}

		List<String> DsID = ExpUtil.getSlaveDsIDs(getPageMeta().getWidget("main"), getDatasetID());
		for (String dsID : DsID) {
			DatasetRule dsRule = new DatasetRule();
			dsRule.setId(dsID);
			dsRule.setType("ds_current_page");
			wr.addDsRule(dsRule);
		}
	}

	private void addBusitemGridEvent(LfwView widget) {
		WebComponent[] wcs = widget.getViewComponents().getComponents();
		for (WebComponent webComp : wcs) {
			if ((webComp instanceof GridComp)) {
				String dsID = ((GridComp) webComp).getDataset();
				if (dsID != null) {

					if ((!"contrast".equals(dsID)) && (!"jkcontrast".equals(dsID))) {

						LfwParameter param = new LfwParameter("gridEvent", "nc.uap.lfw.core.event.GridEvent");
						EventConf eventConfig = new EventConf("onLastCellEnter", param, null);
						eventConfig.setAsync(true);

						eventConfig.setMethodName("lastCellEnter");
						eventConfig.setOnserver(true);
						EventSubmitRule submitRule = new EventSubmitRule();
						eventConfig.setSubmitRule(submitRule);

						((GridComp) webComp).addEventConf(eventConfig);
					}
				}
			}
		}
	}

	private void addInlineAdvqueryWinConnector(LfwView targetView) {
		if (targetView == null) {
			return;
		}
		PluginDesc pluginDesc = new PluginDesc();
		pluginDesc.setId("conditionQueryPlugin");
		targetView.addPluginDescs(pluginDesc);
		pluginDesc.setMethodName("conditionQueryPlugin");

		WindowConfig winConf = new WindowConfig();
		winConf.setCaption(NCLangRes4VoTransl.getNCLangRes().getStrByID("yer", "yer_seniorQuery"));
		winConf.setId("uap.lfw.imp.query.advquery");
		targetView.addInlineWindow(winConf);

		Connector conn = new Connector();
		conn.setId("adv_simple_conn");
		conn.setConnType("6");
		conn.setPluginId("conditionQueryPlugin");
		conn.setPlugoutId("proxy_qryout");
		conn.setSource(winConf.getId());
		conn.setTarget(targetView.getId());
		targetView.addConnector(conn);
	}

	private void saveBusitemDsFormular(LfwView view) {
		String busitemDsID = "";
		if ("bxzb".equals(getDatasetID())) {
			busitemDsID = "busitem";
		} else {
			busitemDsID = "jk_busitem";
		}

		Map<String, String> formularMap = new HashMap();
		Dataset ds = view.getViewModels().getDataset(busitemDsID);
		List<Field> fieldList = ds.getFieldSet().getFieldList();
		for (Field f : fieldList) {
			String editFormular = f.getEditFormular();
			if ((editFormular != null) && (editFormular != null) && (editFormular.startsWith("amount->"))) {
				formularMap.put(f.getId(), editFormular);
			}
		}

		LfwRuntimeEnvironment.getWebContext().getRequest().getSession().setAttribute("yer_formularMap", formularMap);
	}

	public void doApproveYsinfo() {
		String djdl = "bxzb".equals(getDatasetID()) ? "bx" : "jk";
		String billId = LfwRuntimeEnvironment.getWebContext().getOriginalParameter("openBillId");

		if ((billId == null) || ("".equals(billId))) {
			return;
		}

		JKBXVO bxvo = null;
		try {
			List<JKBXVO> bxvos = getIBXBillPrivate().queryVOsByPrimaryKeys(new String[] { billId }, djdl);
			if ((bxvos == null) || (bxvos.size() == 0))
				return;
			bxvo = (JKBXVO) bxvos.get(0);
		} catch (ComponentException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		}

		if (bxvo == null) {
			return;
		}

		boolean istbbused = ErUtil.isProductTbbInstalled("1050");
		if (!istbbused) {
			return;
		}

		String actionCode = getActionCode(bxvo);
		LinkNtbParamVO[] linkNtbParamVO = null;

		JKBXHeaderVO[] items = ErVOUtils.prepareBxvoItemToHeaderClone(bxvo);

		for (JKBXHeaderVO vo : items) {
			if (vo.getShrq() == null) {
				vo.setShrq(new UFDateTime());
			}
		}

		try {
			DataRuleVO[] ruleVos =
					((IBudgetControl) NCLocator.getInstance().lookup(IBudgetControl.class)).queryControlTactics(items[0].getDjlxbm(), actionCode, false);

			if ((ruleVos == null) || (ruleVos.length == 0)) {
				return;
			}

			List<FiBillAccessableBusiVOProxy> voProxys = new ArrayList();

			YsControlVO[] controlVos = ErBudgetUtil.getCtrlVOs(items, true, ruleVos);

			if (controlVos != null) {
				for (YsControlVO vo : controlVos) {
					voProxys.add(getFiBillAccessableBusiVOProxy(vo, vo.getParentBillType()));
				}
			}

			NtbParamVO[] vos = ErmProxy.getILinkQuery().getLinkDatas((IAccessableBusiVO[]) voProxys.toArray(new IAccessableBusiVO[0]));

			if ((null == vos) || (vos.length == 0)) {
				return;
			}

			linkNtbParamVO = convert2WebbxVo(vos);

			AppUtil.addAppAttr("link_ntb_vos_Ysinfo", linkNtbParamVO);
		} catch (Exception e1) {
			Logger.error(e1.getMessage(), e1);
			if ((e1 instanceof LfwRuntimeException)) {
				throw ((LfwRuntimeException) e1);
			}
			throw new LfwRuntimeException(e1);
		}
	}

	private IBXBillPrivate getIBXBillPrivate() throws ComponentException {
		return (IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class.getName());
	}

	private String getActionCode(JKBXVO bxvo) {
		JKBXHeaderVO headVO = bxvo.getParentVO();
		int billStatus = headVO.getDjzt().intValue();
		switch (billStatus) {
			case 3:
				return "APPROVE";
			case 2:
				return "APPROVE";
		}
		return "SAVE";
	}

	private FiBillAccessableBusiVOProxy getFiBillAccessableBusiVOProxy(FiBillAccessableBusiVO vo, String parentBillType) {
		FiBillAccessableBusiVOProxy voProxy = new FiBillAccessableBusiVOProxy(vo);
		return voProxy;
	}

	private LinkNtbParamVO[] convert2WebbxVo(NtbParamVO[] vos) {
		LinkNtbParamVO[] wvos = new LinkNtbParamVO[vos.length];
		for (int i = 0; i < vos.length; i++) {
			LinkNtbParamVO lnvo = new LinkNtbParamVO();
			lnvo.setBegindate(vos[i].getBegDate());
			lnvo.setEnddate(vos[i].getEndDate());
			int currtype = vos[i].getCurr_type();
			UFDouble runvalue = vos[i].getRundata()[currtype];
			UFDouble readyvalue = vos[i].getReadydata()[currtype];
			lnvo.setRundata(runvalue.setScale(2, 0));
			lnvo.setReadydata(readyvalue.setScale(2, 0));
			lnvo.setPlanname(vos[i].getPlanname());
			lnvo.setBalance(vos[i].getBalance().setScale(2, 0));

			lnvo.setPlandata(vos[i].getPlanData().setScale(2, 0));

			String[] pkdim = vos[i].getPkDim();
			lnvo.setPkdim(pkdim);

			String[] pkdiscdim = new String[pkdim.length];
			HashMap map = vos[i].getHashDescription();
			for (int k = 0; k < pkdiscdim.length; k++) {
				pkdiscdim[k] = ((String) map.get(pkdim[k]));
			}
			lnvo.setPkdiscdim(pkdiscdim);

			lnvo.setTypedim(vos[i].getTypeDim());
			lnvo.setBusiAttrs(vos[i].getBusiAttrs());
			wvos[i] = lnvo;
		}
		return wvos;
	}

	public void doApproveFysqinfo() {
		List<String> fysqPkList = new ArrayList();
		String pkStr = "";

		String djdl = "bxzb".equals(getDatasetID()) ? "bx" : "jk";
		String billId = LfwRuntimeEnvironment.getWebContext().getOriginalParameter("openBillId");

		if ((billId == null) || ("".equals(billId))) {
			return;
		}

		JKBXVO bxvo = null;
		try {
			List<JKBXVO> bxvos = getIBXBillPrivate().queryVOsByPrimaryKeys(new String[] { billId }, djdl);
			if ((bxvos == null) || (bxvos.size() == 0))
				return;
			bxvo = (JKBXVO) bxvos.get(0);
		} catch (ComponentException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		}

		if (bxvo == null) {
			return;
		}
		BXBusItemVO[] busitemArr = bxvo.getBxBusItemVOS();
		if (busitemArr == null) {
			return;
		}
		for (int i = 0; i < busitemArr.length; i++) {
			String pkfysq = busitemArr[i].getPk_item();
			if (pkfysq == null) {
				return;
			}
			if (!fysqPkList.contains(pkfysq)) {
				fysqPkList.add(pkfysq);
				pkStr = pkStr + ",,," + pkfysq;
			}
		}

		AppUtil.addAppAttr("Yer_Weberm_ApproveFysqinfo", pkStr);
	}
}