package nc.bs.er.exp.cjk.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nc.bs.er.exp.util.ExpBusitemDs2SpuerVOArrSerializer;
import nc.bs.er.exp.util.ExpCommonUtil;
import nc.bs.er.exp.util.ExpDatasets2AggVOSerializer;
import nc.bs.er.exp.util.ExpUtil;
import nc.bs.framework.common.NCLocator;
import nc.bs.framework.exception.ComponentException;
import nc.bs.logging.Log;
import nc.bs.logging.Logger;
import nc.itf.arap.prv.IBXBillPrivate;
import nc.itf.arap.pub.IBxUIControl;
import nc.itf.fi.pub.Currency;
import nc.itf.fi.pub.SysInit;
import nc.uap.lfw.core.AppInteractionUtil;
import nc.uap.lfw.core.LfwRuntimeEnvironment;

import nc.uap.lfw.core.comp.GridColumn;
import nc.uap.lfw.core.comp.GridComp;
import nc.uap.lfw.core.ctrl.IController;
import nc.uap.lfw.core.ctx.AppLifeCycleContext;

import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.data.RowData;

import nc.uap.lfw.core.event.CellEvent;
import nc.uap.lfw.core.event.DataLoadEvent;
import nc.uap.lfw.core.event.DatasetCellEvent;
import nc.uap.lfw.core.event.DatasetEvent;
import nc.uap.lfw.core.event.DialogEvent;
import nc.uap.lfw.core.event.MouseEvent;
import nc.uap.lfw.core.exception.LfwRuntimeException;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.page.LfwWindow;

import nc.uap.lfw.core.serializer.impl.Dataset2SuperVOSerializer;
import nc.uap.lfw.core.serializer.impl.Datasets2AggVOSerializer;
import nc.uap.lfw.core.serializer.impl.SuperVO2DatasetSerializer;
import nc.uap.lfw.jsp.uimeta.UIFlowvPanel;
import nc.uap.lfw.jsp.uimeta.UIMeta;
import nc.uap.wfm.utils.AppUtil;
import nc.vo.arap.bx.util.BXConstans;
import nc.vo.arap.bx.util.BXParamConstant;
import nc.vo.arap.bx.util.BXStatusConst;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.BXHeaderVO;
import nc.vo.ep.bx.BXVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.ep.bx.JKBusItemVO;
import nc.vo.ep.bx.JKHeaderVO;
import nc.vo.ep.bx.JKVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.er.exp.IExpConst;
import nc.vo.er.util.StringUtils;

import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

/**
 * @author kongxl
 */
public class CjkMainViewCtrl implements IController {

	public static final String EXP_CJK_BXZB_DS = "bxzb";
	public static final String EXP_CJK_JKZB_DS = "jkzb";
	public static final String EXP_CJK_JKBUSITEM_DS = "jkbusitem";
	public static final String EXP_CJK_SELECTEDVO_MAP = "cjk_selectedVOMap";

	// cjk_jkbusitem_grid
	public static final String EXP_CJK_JKBUSITEM_GRID = "cjk_jkbusitem_grid";
	public static final String EXP_CJK_JKZB_CJKYBJE = "cjkybje";

	private static final long serialVersionUID = 1L;

	/**
	 * 需设置和清理精度的金额字段
	 */
	// private static String[] ybjeArr =
	// {"ybje","ybye","zyx2","hkybje","zfybje","yjye","zpxe","zyx29"};
	private static String[] ybjeArr = { "ybje", "ybye", EXP_CJK_JKZB_CJKYBJE, "hkybje", "zfybje", "yjye", "zpxe" };

	/**
	 * 行选中事件
	 * 
	 * @param datasetEvent
	 */
	public void onAfterHeadRowSelect(DatasetEvent datasetEvent) {

		Dataset ds = datasetEvent.getSource();

		Row headSelectedRow = ds.getSelectedRow();
		String pk_jkd = (String) headSelectedRow.getValue(ds.nameToIndex(JKBXHeaderVO.PK_JKBX));

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset parentHeadDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		Row row = parentHeadDs.getSelectedRow();
		String pk_bxd = (String) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.PK_JKBX));

		String bzbm = (String) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.BZBM));
		// 设置冲借款相关精度
		int currentDigit = Currency.getCurrDigit(bzbm);// 当前币种精度

		Integer djzt = (Integer) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.DJZT));

		Dataset contrastDs = pageMeta.getWidget("main").getViewModels().getDataset("contrast");

		List<String> pkBusitemList = new ArrayList<String>();
		if (contrastDs.getCurrentRowData() != null) {// 冲销保存
			Row[] rows = contrastDs.getCurrentRowData().getRows();
			if (rows != null && rows.length > 0) {// 非暂存
				for (int i = 0; i < rows.length; i++) {
					String pk_busitem = (String) rows[i].getValue(contrastDs.nameToIndex(BxcontrastVO.PK_BUSITEM));
					pkBusitemList.add(pk_busitem);
				}
			}
		}

		try {
			BXBusItemVO[] bodyVos = queryByHeaders(pk_jkd, pk_bxd);

			Map<String, Map<String, BXBusItemVO>> selectedVO =
					(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

			Map<String, Map<String, BXBusItemVO>> selectedVO_haveSave =
					(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute("cjk_selectedVOMap_havesave");

			for (BXBusItemVO vo : bodyVos) {

				// 一张借款报销单据只能拉一张申请单，表体的费用申请单据号放到表头显示
				// if (vo.getPk_item() != null) {
				// String pk_item_billno = ExpUtil.getColValue("er_mtapp_bill",
				// "billno", "pk_mtapp_bill", vo.getPk_item());
				// vo.setPk_item_billno(pk_item_billno);
				// }

				Map<String, BXBusItemVO> map = selectedVO.get(vo.getPk_jkbx());
				if (map != null) {
					BXBusItemVO item = map.get(vo.getPk_busitem());
					if (item != null) {

						String pk_busitem = item.getPk_busitem();

						vo.setCjkybje(item.getCjkybje());
						vo.setHkybje(item.getHkybje());
						if (contrastDs.getCurrentRowData() != null) {// 冲销保存
							Row[] rows = contrastDs.getCurrentRowData().getRows();

							if (rows != null && rows.length > 0 && djzt != null && 0 != djzt) {// 非暂存

								if (pkBusitemList.contains(pk_busitem)) { // 只处理冲销保存中对应的借款单业务行项
									vo.setYbye(vo.getYjye().add(vo.getCjkybje()));
								}

							}
						}

						vo.setSelected(UFBoolean.TRUE);
						setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new String[] { BXBusItemVO.CJKYBJE }, true);
					}
				}

				Map<String, BXBusItemVO> map_havesave = selectedVO_haveSave.get(vo.getPk_jkbx());
				if (map_havesave != null) {
					BXBusItemVO item = map_havesave.get(vo.getPk_busitem());

					if (item != null) {
						if (djzt != null && 0 != djzt) {// 非暂存
							vo.setYbye(vo.getYjye().add(item.getCjkybje()));
						}
					}
				}

			}
			Dataset jkbusitemDs = pageMeta.getView("cjk").getViewModels().getDataset(EXP_CJK_JKBUSITEM_DS);
			// 设置子表精度
			ExpCommonUtil.setDsPrecision(jkbusitemDs, BXBusItemVO.getYbjeField(), currentDigit);
			jkbusitemDs.setEnabled(true);
			SuperVO2DatasetSerializer ser = new SuperVO2DatasetSerializer();
			ser.serialize(bodyVos, jkbusitemDs, Row.STATE_NORMAL);
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
			throw new LfwRuntimeException(e.getMessage(), e);
		}

		// CmdInvoker.invoke(new UifDatasetAfterSelectCmd(ds.getId()){
		// protected String chanCurrentKey(String keyValue){
		// Random rand = new Random();
		// return keyValue+String.valueOf(rand.nextFloat());
		// }
		//
		// @Override
		// protected void modifyVos(SuperVO[] vos) {
		// //@see ContrastDialog getCachVO
		// Map<String, Map<String, BXBusItemVO>> selectedVO = (Map<String,
		// Map<String,
		// BXBusItemVO>>)LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);
		// List<BXBusItemVO> list = new ArrayList<BXBusItemVO>();
		// if (!ArrayUtils.isEmpty(vos)) {
		// for (SuperVO svo : vos) {
		// if (svo instanceof BxcontrastVO) {
		// return;
		// }
		//
		// BXBusItemVO vo = (BXBusItemVO)svo;
		// Map<String, BXBusItemVO> map = selectedVO.get(vo.getPk_jkbx());
		// if (map != null) {
		// BXBusItemVO item = map.get(vo.getPk_busitem());
		// if (item != null) {
		// vo.setCjkybje(item.getCjkybje());
		// vo.setHkybje(item.getHkybje());
		// if (!StringUtils.isNullWithTrim(item.getPk_bxcontrast())) {// 冲销保存
		// vo.setYbye(vo.getYjye().add(vo.getCjkybje()));
		// }
		// vo.setSelected(UFBoolean.TRUE);
		// }
		// }
		// }
		// }
		//
		// }
		//
		//
		//
		// });
		//
		// LfwView cjkView =
		// AppLifeCycleContext.current().getWindowContext().getWindow().getView("cjk");
		// Dataset jkbusitemDs =
		// cjkView.getViewModels().getDataset("jkbusitem");
		// if (jkbusitemDs!=null) {
		// jkbusitemDs.setEnabled(true);
		// }

	}

	public void onDataLoadQueryDs(DataLoadEvent e) {
		Dataset ds = e.getSource();

		// 新增行并选中
		Row row = ds.getEmptyRow();
		ds.addRow(row);
		ds.setRowSelectIndex(ds.getRowIndex(row));
		ds.setEnabled(true);

	}

	public void beforeShow(DialogEvent e) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset parentHeadDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");

		Row pRow = parentHeadDs.getSelectedRow();
		if (pRow == null) {
			return;
		}
		String pk_org = (String) pRow.getValue(parentHeadDs.nameToIndex(BXHeaderVO.PK_ORG));

		boolean para = true;
		try {
			para = SysInit.getParaBoolean(pk_org, BXParamConstant.PARAM_IS_CONTRAST_OTHERS).booleanValue();
		} catch (Exception e1) {
			Logger.error(e1.getMessage(), e1);
		}
		if (!para) {

			UIMeta uimeta = AppUtil.getCntViewCtx().getUIMeta();
			UIFlowvPanel pane2 = (UIFlowvPanel) uimeta.findChildById("panelv03674");
			UIFlowvPanel pane3 = (UIFlowvPanel) uimeta.findChildById("panelv03675");
			if (pane2 != null) {
				String tHeight2 = pane2.getHeight();
				pane2.setVisible(false); // 隐藏查询部分的panel

				// panelv03674隐藏时，把panelv03674的高度加在panelv03675上，保持页面格式一致
				if (pane3 != null) {
					String tHeight3 = pane3.getHeight();
					int tHeight = Integer.parseInt(tHeight3) + Integer.parseInt(tHeight2);
					pane3.setHeight(String.valueOf(tHeight));
				}
			}
		}

	}

	public void query(MouseEvent mouseEvent) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset parentHeadDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");

		// BXHeaderVO headVO = new BXHeaderVO();
		Row pRow = parentHeadDs.getSelectedRow();
		if (pRow == null) {
			return;
		}
		// headVO.setJkbxr((String)pRow.getValue(parentHeadDs.nameToIndex(BXHeaderVO.JKBXR)));
		// headVO.setBzbm((String)pRow.getValue(parentHeadDs.nameToIndex(BXHeaderVO.BZBM)));
		// headVO.setYbje(new UFDouble(0));

		Dataset queryDs = pageMeta.getWidget("cjk").getViewModels().getDataset("query_ds");

		Row row = queryDs.getCurrentRowData().getSelectedRow();
		String jkbxr = (String) row.getValue(queryDs.nameToIndex("jkbxr"));
		UFDate starttime = (UFDate) row.getValue(queryDs.nameToIndex("starttime"));
		UFDate endtime = (UFDate) row.getValue(queryDs.nameToIndex("endtime"));

		if (starttime != null && endtime != null) {
			if (starttime.compareTo(endtime) > 0) {
				AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("weberm_0", "0E010001-0023")/*
																																		 * @
																																		 * res
																																		 * "开始日期不能大于结束日期，请重新输入!"
																																		 */);
			}
		}

		if (jkbxr == null || jkbxr.trim().equals("")) {
			AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000378")/*
																																 * @
																																 * res
																																 * "请先选择借款人名称"
																																 */);
		} else {

			// 再次查询时清空明细信息
			LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
			Dataset tBusitemDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(IExpConst.YER_CJK_BUSITEM_DS);
			if (tBusitemDs != null) {
				tBusitemDs.clear();
			}

			String sql = " zb.jkbxr='" + jkbxr + "'";
			if (endtime != null) {
				sql += " and zb.djrq<='" + endtime.asEnd() + "'";
			}
			if (starttime != null) {
				sql += " and zb.djrq>='" + starttime.asBegin() + "'";
			}

			try {
				// List<JKBXHeaderVO> jkd = getIBxUIControl().getJKD(headVO,new
				// UFDate(new Date()), sql);
				// List<JKBXHeaderVO> jkd = getIBxUIControl().getJKD(new
				// BXVO(headVO),new UFDate(new Date()), sql);
				Datasets2AggVOSerializer serializer = new ExpDatasets2AggVOSerializer();

				Dataset[] detailDss = ExpUtil.getAllDetailDss(pageMeta.getView("main"), "bxzb");
				AggregatedValueObject aggVo = serializer.serialize(parentHeadDs, detailDss, BXVO.class.getName());
				BXVO bxvo = (BXVO) aggVo;
				List<JKBXHeaderVO> jkd = getIBxUIControl().getJKD(bxvo, new UFDate(new Date()), sql);
				for (JKBXHeaderVO headerVO : jkd) {
					// headerVO.setZyx30(ExpUtil.getBilltypeNameMultiLang(headerVO.getDjlxbm()));//用zyx30来存储交易类型
					headerVO.setDjlxmc(ExpUtil.getBilltypeNameMultiLang(headerVO.getDjlxbm()));// 用djlxmc来存储交易类型
				}
				Dataset dataset = pageMeta.getWidget("cjk").getViewModels().getDataset("jkzb");
				dataset.setEnabled(true); // 所有均可编辑

				new SuperVO2DatasetSerializer().serialize(jkd.toArray(new SuperVO[0]), dataset, Row.STATE_NORMAL);

				Integer djzt = (Integer) pRow.getValue(parentHeadDs.nameToIndex(BXHeaderVO.DJZT));
				Dataset contrastDs = pageMeta.getWidget("main").getViewModels().getDataset("contrast");

				SuperVO[] contrastVO = new Dataset2SuperVOSerializer().serialize(contrastDs);

				List<String> Pk_busitemList = new ArrayList<String>();
				if (contrastVO != null && contrastVO.length != 0) {
					for (SuperVO a : contrastVO) {
						BxcontrastVO b = (BxcontrastVO) a;

						Pk_busitemList.add(b.getPk_busitem());
					}
				}

				Map<String, Map<String, BXBusItemVO>> selectedVO =
						(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

				boolean a = false;
				for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO.entrySet()) {
					String jkdpk = selectedvo.getKey();
					Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

					UFDouble cjkybje = UFDouble.ZERO_DBL;
					UFDouble hkybje = UFDouble.ZERO_DBL;

					for (BXBusItemVO item : busiitems.values()) {
						if (Pk_busitemList.contains(item.getPk_busitem())) {
							a = true;
							continue;
						}

					}
				}

				// 如果取消选中了表体行，切换报销人查询的话，重新加载下冲销行
				if (contrastVO != null && contrastVO.length != 0 && a == false) {
					loadselectedVO(contrastVO);
				}

				for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO.entrySet()) {
					String jkdpk = selectedvo.getKey();
					Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

					UFDouble cjkybje = UFDouble.ZERO_DBL;
					UFDouble hkybje = UFDouble.ZERO_DBL;

					for (BXBusItemVO item : busiitems.values()) {
						UFDouble tcjkybje = item.getCjkybje();
						if (tcjkybje == null) {
							tcjkybje = UFDouble.ZERO_DBL;
						}
						cjkybje = cjkybje.add(tcjkybje);
						UFDouble thkybje = item.getHkybje();
						if (thkybje == null) {
							thkybje = UFDouble.ZERO_DBL;
						}
						hkybje = hkybje.add(thkybje);
					}

					setHeadColumnValue(dataset, JKBXHeaderVO.PK_JKBX, jkdpk, EXP_CJK_JKZB_CJKYBJE, cjkybje);
					setHeadColumnValue(dataset, JKBXHeaderVO.PK_JKBX, jkdpk, "hkybje", hkybje);

					if (contrastVO != null && contrastVO.length != 0) {// 已做过冲借款的单据打开

						if (djzt != null && 0 != djzt) {// 非暂存态

							if (dataset.getCurrentRowData() != null) {
								Row[] rows = dataset.getCurrentRowData().getRows();
								for (int i = 0; i < rows.length; i++) {
									Row thisrow = rows[i];
									UFDouble yjye = (UFDouble) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.YJYE));
									if (jkdpk.equals((String) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.PK_JKBX)))) {
										thisrow.setValue(dataset.nameToIndex(JKBXHeaderVO.YBYE), cjkybje.add(yjye));
									}
								}
							}

						}
					}

					Row[] rows = dataset.getCurrentRowData().getRows();
					for (int i = 0; i < rows.length; i++) {
						Row row1 = rows[i];
						if (jkdpk.equals((String) row1.getValue(dataset.nameToIndex(JKBXHeaderVO.PK_JKBX)))) {
							dataset.setSelectedIndex(i);
						}
					}

				}

			} catch (ComponentException e1) {
				// TODO Auto-generated catch block
				Logger.error(e1.getMessage(), e1);
				throw new LfwRuntimeException(e1.getMessage(), e1);
			} catch (BusinessException e1) {
				// TODO Auto-generated catch block
				Logger.error(e1.getMessage(), e1);
				throw new LfwRuntimeException(e1.getMessage(), e1);
			}
		}

	}

	public void cancel(MouseEvent mouseEvent) {

		AppLifeCycleContext.current().getWindowContext().closeView("cjk");

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset ds = pageMeta.getWidget("cjk").getViewModels().getDataset("jkzb");
		clearPrecisionProperties(ybjeArr, ds);

	}

	public void onDataLoad(DataLoadEvent e) {

		Map<String, Map<String, BXBusItemVO>> selectedVO = new HashMap<String, Map<String, BXBusItemVO>>();
		LfwRuntimeEnvironment.getWebContext().getRequest().getSession().setAttribute(EXP_CJK_SELECTEDVO_MAP, selectedVO);

		Map<String, Map<String, BXBusItemVO>> selectedVO_haveSave = new HashMap<String, Map<String, BXBusItemVO>>();
		LfwRuntimeEnvironment.getWebContext().getRequest().getSession().setAttribute("cjk_selectedVOMap_havesave", selectedVO_haveSave);

		// 隐藏全选框
		// String execScript =
		// "pageUI.getWidget('main').getComponent('headTab_list_tabcjk_grid').setHeaderCheckBoxVisible('cjkselected',false)";
		// getGlobalContext().addExecScript(execScript);

		// 获取父页面的PageMeta
		// String appid =
		// (String)LfwRuntimeEnvironment.getWebContext().getWebSession().getAttribute(WebContext.APP_ID);
		// Application app =
		// (Application)LfwRuntimeEnvironment.getWebContext().getWebSession().getAttribute(WebContext.APP_CONF);
		// String winId = app.getDefaultWindowId();

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset parentHeadDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");

		Dataset contrastDs = pageMeta.getWidget("main").getViewModels().getDataset("contrast");

		Datasets2AggVOSerializer serializer = new ExpDatasets2AggVOSerializer();

		Dataset[] detailDss = ExpUtil.getAllDetailDss(pageMeta.getView("main"), "bxzb");
		AggregatedValueObject aggVo = serializer.serialize(parentHeadDs, detailDss, BXVO.class.getName());
		BXVO bxvo = (BXVO) aggVo;

		SuperVO[] contrastVO = new Dataset2SuperVOSerializer().serialize(contrastDs);

		if (contrastVO != null && contrastVO.length != 0) {
			loadselectedVO(contrastVO);
		}

		Row row = parentHeadDs.getSelectedRow();
		if (row == null) {
			return;
		}

		String jkbxr = (String) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.JKBXR));
		String bzbm = (String) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.BZBM));

		Integer djzt = (Integer) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.DJZT));
		/*
		 * // String pk_jkbx =
		 * (String)row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.PK_JKBX));
		 * 
		 * BXHeaderVO headVO = new BXHeaderVO(); headVO.setJkbxr(jkbxr);
		 * headVO.setBzbm(bzbm); // headVO.setPk_jkbx(pk_jkbx);
		 * headVO.setYbje(new UFDouble(0));
		 * 
		 * String userID = YerUtil.getPk_user(); headVO.setOperator(userID);
		 */

		try {
			// 单据有冲销信息，必须加载上，否则查询不出冲借款信息
			String pk = (String) row.getValue(parentHeadDs.nameToIndex(BXHeaderVO.PK_JKBX));
			BxcontrastVO[] newContrastvos = bxvo.getContrastVO();
			BxcontrastVO[] oldContrastvos = null;
			if (pk != null /* && newContrastvos ==null */) {
				JKBXHeaderVO headerVO = bxvo.getParentVO();
				List<JKBXVO> voList = getIBXBillPrivate().queryVOsByPrimaryKeys(new String[] { headerVO.getPk_jkbx() }, headerVO.getDjdl());
				if (voList != null && voList.size() >= 1) {
					JKBXVO OldBXVO = voList.get(0);
					oldContrastvos = OldBXVO.getContrastVO();
					bxvo.setContrastVO(oldContrastvos);
				}
			}

			// List<JKBXHeaderVO> jkd = getIBxUIControl().getJKD(headVO,new
			// UFDate(new Date()), "");
			List<JKBXHeaderVO> jkd = getIBxUIControl().getJKD(bxvo, new UFDate(new Date()), null);

			for (JKBXHeaderVO headerVO : jkd) {
				// headerVO.setZyx30(ExpUtil.getBilltypeNameMultiLang(headerVO.getDjlxbm()));//用zyx30来存储交易类型
				headerVO.setDjlxmc(ExpUtil.getBilltypeNameMultiLang(headerVO.getDjlxbm()));// 用djlxmc来存储交易类型

			}

			Dataset dataset = e.getSource();
			dataset.setEnabled(true); // 所有均可编辑

			// 设置冲借款相关精度
			int currentDigit = Currency.getCurrDigit(bzbm);// 当前币种精度
			ExpCommonUtil.setDsPrecision(dataset, ybjeArr, currentDigit);

			new SuperVO2DatasetSerializer().serialize(jkd.toArray(new SuperVO[0]), dataset, Row.STATE_NORMAL);

			// if (contrastVO!=null) {
			// for (SuperVO head:contrastVO) {
			//
			// setCjkje((BxcontrastVO)head, dataset); //设置冲借款金额
			// }
			// }

			for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO.entrySet()) {
				String jkdpk = selectedvo.getKey();
				Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

				UFDouble cjkybje = UFDouble.ZERO_DBL;
				UFDouble hkybje = UFDouble.ZERO_DBL;

				for (BXBusItemVO item : busiitems.values()) {
					UFDouble tcjkybje = item.getCjkybje();
					if (tcjkybje == null) {
						tcjkybje = UFDouble.ZERO_DBL;
					}
					cjkybje = cjkybje.add(tcjkybje);
					UFDouble thkybje = item.getHkybje();
					if (thkybje == null) {
						thkybje = UFDouble.ZERO_DBL;
					}
					hkybje = hkybje.add(thkybje);
				}

				setHeadColumnValue(dataset, JKBXHeaderVO.PK_JKBX, jkdpk, EXP_CJK_JKZB_CJKYBJE, cjkybje);
				setHeadColumnValue(dataset, JKBXHeaderVO.PK_JKBX, jkdpk, "hkybje", hkybje);

				if (contrastVO != null && contrastVO.length != 0) {// 已做过冲借款的单据打开

					if (djzt != null && 0 != djzt) {// 非暂存态

						if (dataset.getCurrentRowData() != null) {
							Row[] rows = dataset.getCurrentRowData().getRows();
							for (int i = 0; i < rows.length; i++) {
								Row thisrow = rows[i];
								UFDouble yjye = (UFDouble) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.YJYE));
								if (jkdpk.equals((String) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.PK_JKBX)))) {
									thisrow.setValue(dataset.nameToIndex(JKBXHeaderVO.YBYE), cjkybje.add(yjye));
								}
							}
						}

					}
				}

				Row[] rows = dataset.getCurrentRowData().getRows();
				for (int i = 0; i < rows.length; i++) {
					Row row1 = rows[i];
					if (jkdpk.equals((String) row1.getValue(dataset.nameToIndex(JKBXHeaderVO.PK_JKBX)))) {
						dataset.setSelectedIndex(i);
					}
				}

			}

			if (pk != null && /* newContrastvos ==null && */oldContrastvos != null) { // 修改保存后的已经冲借款的报销单，勾掉冲借款，确定再打开

				for (SuperVO svo : oldContrastvos) {
					BxcontrastVO vo = (BxcontrastVO) svo;
					BXBusItemVO busitemvo = new BXBusItemVO();
					busitemvo.setSelected(UFBoolean.TRUE);
					busitemvo.setSzxmid(vo.getSzxmid());
					busitemvo.setPk_jkbx(vo.getPk_jkd());
					busitemvo.setPk_busitem(vo.getPk_busitem());
					busitemvo.setCjkybje(vo.getCjkybje());
					busitemvo.setHkybje(vo.getHkybje());
					busitemvo.setPk_bxcontrast(vo.getPk_bxcontrast());
					Map<String, BXBusItemVO> map = selectedVO_haveSave.get(vo.getPk_jkd());
					if (map == null) {
						map = new HashMap<String, BXBusItemVO>();
						selectedVO_haveSave.put(vo.getPk_jkd(), map);
					}

					// 对于冲销行和借款单业务行不一致时，处理方式
					BXBusItemVO item = map.get(vo.getPk_busitem());
					if (item != null) {
						busitemvo.setCjkybje((item.getCjkybje().add(vo.getCjkybje())));
					}

					map.put(vo.getPk_busitem(), busitemvo);
				}

				for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO_haveSave.entrySet()) {
					String jkdpk = selectedvo.getKey();
					Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

					UFDouble cjkybje = UFDouble.ZERO_DBL;

					for (BXBusItemVO item : busiitems.values()) {
						UFDouble tcjkybje = item.getCjkybje();
						if (tcjkybje == null) {
							tcjkybje = UFDouble.ZERO_DBL;
						}
						cjkybje = cjkybje.add(tcjkybje);
					}

					if (oldContrastvos != null && oldContrastvos.length != 0) {// 已做过冲借款的单据打开

						if (djzt != null && 0 != djzt) {// 非暂存态

							if (dataset.getCurrentRowData() != null) {
								Row[] rows = dataset.getCurrentRowData().getRows();
								for (int i = 0; i < rows.length; i++) {
									Row thisrow = rows[i];
									UFDouble yjye = (UFDouble) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.YJYE));
									if (jkdpk.equals((String) thisrow.getValue(dataset.nameToIndex(JKBXHeaderVO.PK_JKBX)))) {
										thisrow.setValue(dataset.nameToIndex(JKBXHeaderVO.YBYE), cjkybje.add(yjye));
									}
								}
							}

						}
					}

				}

			}

			// setHeadTotalAmount();

			// getGlobalContext().getPageMeta().getWidget("main").setOperatorState(IOperatorState.EDIT);
		} catch (ComponentException e1) {
			Logger.error(e1.getMessage(), e1);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
		}

	}

	protected IBXBillPrivate getIBXBillPrivate() throws ComponentException {
		return ((IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class.getName()));
	}

	public void onAfterRowSelect(DatasetEvent e) {
		GridComp comp =
				(GridComp) AppLifeCycleContext.current().getWindowContext().getWindow().getWidget("main").getViewComponents().getComponent("headTab_list_tabcjk_grid");
		comp.setEditable(true);
		comp.getColumnByField(EXP_CJK_JKZB_CJKYBJE);

		Dataset ds = e.getSource();
		Row[] rows = ds.getSelectedRows();

		int jkdOrgIndex = ds.nameToIndex("pk_org");

		for (int i = 0; i < rows.length; i++) {
			Row row = rows[i];

			// String id = row.getRowId();

			String jkdOrg = (String) row.getValue(jkdOrgIndex);

		}
	}

	public void onAfterDataChange(DatasetCellEvent e) {

		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();

		Dataset bxzbDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		Row bxzbRow = bxzbDs.getCurrentRowData().getSelectedRow();
		// 20170119 tsy 获取单据类型
		String djlx = (String) bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.DJLXBM));
		// 20170919 end
		Dataset jkzbDs = pageMeta.getWidget("cjk").getViewModels().getDataset("jkzb");
		Row jkzbRow = jkzbDs.getCurrentRowData().getSelectedRow();

		Dataset jkbusitemDs = e.getSource();
		Row jkbusitemRow = jkbusitemDs.getCurrentRowData().getRows()[e.getRowIndex()];
		// 20170119 tsy 源码中有，但反编译中的没有
		// Dataset cjkjkzbDs =
		// pageMeta.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKZB_DS);
		// Row[] headRowArr = cjkjkzbDs.getCurrentRowData().getRows();
		// 20170119 end
		// 20170712 tsy
		// UFDouble bxybje = (UFDouble)
		// bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.YBJE));
		UFDouble bxybje = new UFDouble(bxzbRow.getValue(bxzbDs.nameToIndex("zyx12")).toString());
		// 20170712 end
		if (e.getColIndex() == jkbusitemDs.nameToIndex("selected")) { // 选中项

			String selectValue = (String) e.getNewValue();
			if ("Y".equals(selectValue)) {

				UFDouble ybye = (UFDouble) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.YBYE));
				// 20170119 tsy 设置报销金额
				UFDouble resetBXJE = getBxje().sub(getHeadTotalCjkje());// 不产生还款金额的特殊处理
				if (ybye.doubleValue() <= resetBXJE.doubleValue() || "2647".equals(djlx)) {
					jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE), ybye);
				} else {
					jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE), resetBXJE);
				}
				// jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE),
				// ybye);
				// 20170919 end
				jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.HKYBJE), new UFDouble(0));

				// 将选择的表体VO放到一个缓存中
				setSelectedVO(jkbusitemRow);
				setHeadTotalAmount();
				// // 如果是还款单的话，要将还款原币金额设置为不可编辑
				// String djlxbm =
				// (String)bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.DJLXBM));
				// if (BXConstans.BILLTYPECODE_RETURNBILL.equals(djlxbm)) {
				// setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new
				// String[]{BXBusItemVO.CJKYBJE}, true);
				// } else {
				// setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new
				// String[]{BXBusItemVO.CJKYBJE,BXBusItemVO.HKYBJE}, true);
				// }
				setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new String[] { BXBusItemVO.CJKYBJE }, true);

				// GridComp cjkGrid =
				// (GridComp)pageMeta.getWidget("cjk").getViewComponents().getComponent("cjk_jkzb_grid");
				// GridColumn zyx2Column =
				// (GridColumn)cjkGrid.getColumnById("zyx2");
				// zyx2Column.setEditable(true);

			} else {

				String pk_jkbx = (String) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.PK_JKBX));

				String pk_busitem = (String) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.PK_BUSITEM));
				// 20170119 tsy 源码中有，但反编译中的没有
				// UFDouble resetBXJE = UFDouble.ZERO_DBL;
				// 20170119 end
				Map<String, BXBusItemVO> map = selectedVO.get(pk_jkbx);
				if (map != null && map.containsKey(pk_busitem)) {
					selectedVO.get(pk_jkbx).remove(pk_busitem);
				}
				// 20170119 tsy 源码中有，但反编译中的没有
				/*
				 * resetBXJE = getBxje(); for (Row headRow : headRowArr) {
				 * Map<String, BXBusItemVO> tmap =
				 * selectedVO.get(headRow.getValue
				 * (jkzbDs.nameToIndex(JKBXHeaderVO.PK_JKBX))); if (tmap != null
				 * && !"2647".equals(djlx)) { // 不产生还款金额的特殊处理...
				 * List<BXBusItemVO> list = new ArrayList<BXBusItemVO>(); //
				 * 先按照业务行的pk排序 for (BXBusItemVO order : tmap.values()) {
				 * list.add(order); } Collections.sort(list, new
				 * AddrComparator()); for (BXBusItemVO item : list) { UFDouble
				 * ybje = item.getYbye(); UFDouble cjkybje = item.getCjkybje();
				 * if (ybje.doubleValue() >= resetBXJE.doubleValue()) {
				 * item.setCjkybje(resetBXJE); resetBXJE = UFDouble.ZERO_DBL; }
				 * else if (resetBXJE.doubleValue() ==
				 * UFDouble.ZERO_DBL.doubleValue()) {
				 * item.setCjkybje(UFDouble.ZERO_DBL); } else {
				 * item.setCjkybje(ybje); resetBXJE = resetBXJE.sub(ybje); }
				 * setBodyColumnValue(BXBusItemVO.CJKYBJE,
				 * BXBusItemVO.PK_BUSITEM, item.getPk_busitem(),
				 * item.getCjkybje());
				 * 
				 * }
				 * 
				 * } }
				 */
				// 20170119 end
				jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE), new UFDouble(0));
				jkbusitemRow.setValue(jkbusitemDs.nameToIndex(BXBusItemVO.HKYBJE), new UFDouble(0));
				setHeadTotalAmount();
				// // 如果是还款单的话，要将还款原币金额设置为不可编辑
				// String djlxbm =
				// (String)bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.DJLXBM));
				// if (BXConstans.BILLTYPECODE_RETURNBILL.equals(djlxbm)) {
				// setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new
				// String[]{BXBusItemVO.CJKYBJE}, false);
				// } else {
				// setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new
				// String[]{BXBusItemVO.CJKYBJE,BXBusItemVO.HKYBJE}, false);
				// }

				int selCount = 0;
				Row[] rowArr = jkbusitemDs.getCurrentRowData().getRows();
				if (rowArr != null) {
					for (int i = 0; i < rowArr.length; i++) {
						UFBoolean isSel = (UFBoolean) rowArr[i].getValue(jkbusitemDs.nameToIndex("selected"));
						if (isSel != null && isSel.booleanValue()) {
							selCount++;
						}

					}

				}
				if (selCount <= 0) {
					setGridColumnEditable(EXP_CJK_JKBUSITEM_GRID, new String[] { BXBusItemVO.CJKYBJE }, false);
				}

			}
			// this.resetHkje(jkzbDs, bxybje);

		} else if (e.getColIndex() == jkbusitemDs.nameToIndex(JKBusItemVO.CJKYBJE)) { // 编辑冲借款金额
			// UFDouble ybye =
			// (UFDouble)jkzbRow.getValue(jkzbDs.nameToIndex(BXHeaderVO.YBYE));
			// UFDouble cjkybje =
			// (UFDouble)jkzbRow.getValue(jkzbDs.nameToIndex("zyx2"));
			// if (cjkybje == null || ybye == null) {
			// return;
			// }
			// if(cjkybje.compareTo(ybye)>0){
			// //InteractionUtil.showMessageDialog(getGlobalContext(),
			// nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON","UPP2011-000177")/*@res
			// "冲借款金额不能大于借款单余额!"*/);
			// AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON","UPP2011-000177")/*@res
			// "冲借款金额不能大于借款单余额!"*/);
			// jkzbRow.setValue(jkzbDs.nameToIndex("zyx2"), ybye);
			// } else if(cjkybje.compareTo(new UFDouble(0))<0){
			// //InteractionUtil.showMessageDialog(getGlobalContext(),
			// nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON","UPP2011-000178")/*@res
			// "冲借款金额必须大于零!"*/);
			// AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON","UPP2011-000178")/*@res
			// "冲借款金额必须大于零!"*/);
			// jkzbRow.setValue(jkzbDs.nameToIndex("zyx2"), ybye);
			// } else {
			// this.resetHkje(jkzbDs, bxybje);
			// }

			UFBoolean isSel = (UFBoolean) jkbusitemRow.getValue(jkbusitemDs.nameToIndex("selected"));
			if (isSel == null) {
				return;
			}
			if (!isSel.booleanValue()) {
				return;
			}

			UFDouble ybye = (UFDouble) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.YBYE));
			UFDouble cjkje = (UFDouble) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE));

			String djlxbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.DJLXBM));

			// 如果是还款单的话，要将还款金额设置为冲借款金额
			if (BXConstans.BILLTYPECODE_RETURNBILL.equals(djlxbm)) {
				ExpUtil.setRowValue(jkbusitemRow, jkbusitemDs, BXBusItemVO.HKYBJE, cjkje);
			}

			if (cjkje.compareTo(ybye) > 0) {

				AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000177")/*
																																	 * @
																																	 * res
																																	 * "冲借款金额不能大于借款单余额!"
																																	 */);
				ExpUtil.setRowValue(jkbusitemRow, jkbusitemDs, BXBusItemVO.CJKYBJE, ybye);

				// BXUiUtil.showUif2DetailMessage(ContrastDialog.this,
				// nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
				// .getStrByID("201107_0", "0201107-0022")/*
				// * @res
				// * "冲借款金额不应大于借款余额"
				// */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
				// .getStrByID("201107_0", "0201107-0022")/*
				// * @res
				// * "冲借款金额不应大于借款余额"
				// */);
			}

			if (BXConstans.BILLTYPECODE_RETURNBILL.equals(djlxbm)) {
				UFDouble totalHkje = sumHkje();
				ExpUtil.setRowValue(jkzbRow, jkzbDs, JKBXHeaderVO.HKYBJE, totalHkje);
			}

			// 修改缓存中的数据
			setSelectedVO(jkbusitemRow);

			// 修改冲借款时修改重新计算还款金额
			setHeadTotalAmount();

		}
		// else if(e.getColIndex() ==
		// jkbusitemDs.nameToIndex(JKBusItemVO.HKYBJE)){
		// //编辑还款原币金额--目前该功能已经屏蔽掉，不允许编辑还款原币金额项
		//
		// UFBoolean isSel =
		// (UFBoolean)jkbusitemRow.getValue(jkbusitemDs.nameToIndex("selected"));
		// if (isSel == null) {
		// return;
		// }
		// if (!isSel.booleanValue()) {
		// return;
		// }
		// // UFDouble hkje =
		// (UFDouble)jkzbRow.getValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE));
		// // if (hkje== null) {
		// // return;
		// // }
		// //
		// // if(hkje.compareTo(new UFDouble(0))<0){
		// //
		// AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON","UPP2011-000373")/*@res
		// "还款金额不能小于零!"*/);
		// // jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), new
		// UFDouble(0));
		// // }
		//
		//
		// UFDouble totalHkje = sumHkje();
		// UFDouble totalcjkje = sumCjkje();
		// if (totalcjkje.compareTo(bxybje) <= 0 &&
		// totalHkje.compareTo(UFDouble.ZERO_DBL) > 0) {
		// // BXUiUtil.showUif2DetailMessage(ContrastDialog.this,
		// nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
		// // .getStrByID("201107_0", "0201107-0023")/*
		// // * @res
		// // * "冲借款金额小于等于报销金额时，不可以有还款金额"
		// // */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
		// // .getStrByID("201107_0", "0201107-0023")/*
		// // * @res
		// // * "冲借款金额小于等于报销金额时，不可以有还款金额"
		// // */);
		//
		// AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
		// .getStrByID("201107_0", "0201107-0023")/*
		// * @res
		// * "冲借款金额小于等于报销金额时，不可以有还款金额"
		// */);
		//
		// ExpUtil.setRowValue(jkbusitemRow, jkbusitemDs, BXBusItemVO.HKYBJE,
		// new UFDouble(0));
		//
		// }
		//
		// if (totalHkje.compareTo(totalcjkje.sub(bxybje)) != 0) {
		// AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
		// .getStrByID("201107_0", "0201107-0024")/*
		// * @res
		// * "还款金额应等于冲借款金额-报销借款"
		// */);
		//
		// ExpUtil.setRowValue(jkbusitemRow, jkbusitemDs,
		// BXBusItemVO.HKYBJE,totalcjkje.sub(bxybje));
		// }
		//
		// ExpUtil.setRowValue(jkzbRow, jkzbDs, JKBXHeaderVO.HKYBJE, totalHkje);
		// // 修改缓存中的数据
		// setSelectedVO(jkbusitemRow);
		//
		//
		//
		//
		// }
	}

	private UFDouble getContrastTotal(Dataset jkzbDs) {
		UFDouble total = new UFDouble(0);
		Row[] rows = jkzbDs.getCurrentRowData().getRows();

		for (int i = 0; i < rows.length; i++) {
			Row jkzbRow = rows[i];
			String isSelect = (String) jkzbRow.getValue(jkzbDs.nameToIndex("cjkselected"));
			if (isSelect == null) {

				continue;
			}
			if ("false".equals(isSelect)) {
				continue;
			}

			UFDouble cjkybje = (UFDouble) jkzbRow.getValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE));

			if (cjkybje != null) {
				total = total.add(cjkybje);

			}
		}
		return total;
	}

	private void resetHkje(Dataset jkzbDs, UFDouble bxybje) {
		UFDouble contrastTotal = getContrastTotal(jkzbDs);
		UFDouble bxje = bxybje;
		UFDouble hkje = contrastTotal.compareTo(bxje) > 0 ? contrastTotal.sub(bxje) : UFDouble.ZERO_DBL;
		Row[] rows = jkzbDs.getCurrentRowData().getRows();
		for (int i = 0; i < rows.length; i++) {
			Row jkzbRow = rows[i];
			String isSelect = (String) jkzbRow.getValue(jkzbDs.nameToIndex("cjkselected"));
			if (isSelect != null && "true".equals(isSelect)) {
				UFDouble cjkje = (UFDouble) jkzbRow.getValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE));
				if (cjkje == null) {
					cjkje = UFDouble.ZERO_DBL;
				}
				if (hkje.compareTo(UFDouble.ZERO_DBL) > 0) {
					if (cjkje.compareTo(hkje) >= 0) {
						jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), hkje);
						hkje = UFDouble.ZERO_DBL;
					} else {
						jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), cjkje);
						hkje = hkje.sub(cjkje);
					}
				} else {
					jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), UFDouble.ZERO_DBL);
				}
			}
		}
	}

	/**
	 * @param pk_jkd
	 * @param cjkybje设置冲借款金额
	 */
	private void setCjkje(BxcontrastVO vo, Dataset dataset) {
		String pk_jkd = vo.getPk_jkd();

		if (pk_jkd != null) {

			setHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, "cjkselected", "true");

			// getListPanel().setHeadColumnValue(BXHeaderVO.SELECTED,BXHeaderVO.PK_JKBX,
			// pk_jkd,UFBoolean.TRUE);
			// getListPanel().setHeadColumnValue("pk_bxcontrast",BXHeaderVO.PK_JKBX,
			// pk_jkd, vo.getPk_bxcontrast());

			Object cjkje = getHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, EXP_CJK_JKZB_CJKYBJE);
			if (cjkje != null)
				setHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, EXP_CJK_JKZB_CJKYBJE, vo.getCjkybje().add((UFDouble) cjkje));
			else
				setHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, EXP_CJK_JKZB_CJKYBJE, vo.getCjkybje());

		}

		if (!StringUtils.isNullWithTrim(vo.getPrimaryKey())) {
			Object ybye = getHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, BXHeaderVO.YBYE);
			if (ybye != null)
				setHeadColumnValue(dataset, BXHeaderVO.PK_JKBX, pk_jkd, BXHeaderVO.YBYE, vo.ybje.add(new UFDouble(ybye.toString())));

		}
		//
		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();

		Dataset bxzbDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		Row bxzbRow = bxzbDs.getCurrentRowData().getSelectedRow();

		UFDouble bxybje = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.YBJE));
		resetHkje(dataset, bxybje);
	}

	private void setHeadColumnValue(Dataset dataset, String compKey, Object compValue, String key, Object value) {
		if (dataset.getCurrentRowData() == null || value == null) {
			return;
		}
		Row[] rows = dataset.getCurrentRowData().getRows();
		for (int i = 0; i < rows.length; i++) {
			Row row = rows[i];
			if (compValue.equals((String) row.getValue(dataset.nameToIndex(compKey)))) {
				row.setValue(dataset.nameToIndex(key), value);
			}
		}
	}

	private Object getHeadColumnValue(Dataset dataset, String compKey, Object compValue, String key) {
		if (dataset.getCurrentRowData() == null) {
			return null;
		}
		Row[] rows = dataset.getCurrentRowData().getRows();
		for (int i = 0; i < rows.length; i++) {
			Row row = rows[i];
			if (compValue.equals((String) row.getValue(dataset.nameToIndex(compKey)))) {
				return row.getValue(dataset.nameToIndex(key));
			}
		}
		return null;
	}

	private IBxUIControl getIBxUIControl() throws ComponentException {
		return ((IBxUIControl) NCLocator.getInstance().lookup(IBxUIControl.class.getName()));
	}

	// private UFDouble getUFDoubleJe (Row jkzbRow, int index) {
	// UFDouble cjkybje = UFDouble.ZERO_DBL;
	// Object cjkybjeObj = jkzbRow.getValue(index);
	// if (cjkybjeObj instanceof String) {
	// cjkybje = new UFDouble((String)cjkybjeObj);
	// } else {
	// cjkybje = (UFDouble)cjkybjeObj;
	// }
	// return cjkybje;
	// }

	public void cjkOK(MouseEvent mouseEvent) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset bxContrastDs = pageMeta.getWidget("main").getViewModels().getDataset("contrast");

		// 传递pk_org, 因模板中冲款对照行中无此项，单保存提交时需要此项
		// ClientSession csession =
		// getGlobalContext().getParentGlobalContext().getClientSession();
		// WebSession webSession =
		// getGlobalContext().getParentGlobalContext().getWebSession();

		// WebSession webSession =
		// LfwRuntimeEnvironment.getWebContext().getParentSession();
		// LfwRuntimeEnvironment.getWebContext().getWebSession();

		// GridComp comp =
		// (GridComp)meta.getWidget("main").getViewComponents().getComponent("headTab_list_tabcjk_grid");

		Dataset ds = pageMeta.getWidget("cjk").getViewModels().getDataset("jkzb");
		// Row[] rows = ds.getCurrentRowData().getRows();
		// 获取所有行
		Row[] rows = ds.getCurrentRowData().getRows(); // 将所有项提交过来
		// 冲借款总金额
		UFDouble totalCjk = new UFDouble(0);

		// List<BxcontrastVO> bxContrastVOlist = new ArrayList<BxcontrastVO>();
		// if (rows != null && rows.length != 0) {
		// for (int i=0; i<rows.length; i++) {
		// Row row = rows[i];
		//
		// int indexSelect = ds.nameToIndex("cjkselected");
		// String isSelect = (String)row.getValue(indexSelect);
		// if (isSelect==null || "false".equals(isSelect)) { //选中项没有被选中
		// continue;
		// }
		//
		//
		// /**
		// * TODO 2012-10-10 当修改已经冲销了的单据的币种时 再进行冲借款，会将本身冲的借款单查出来，此时做如下判断
		// */
		// String JkBzbm =
		// (String)row.getValue(ds.nameToIndex(JKBXHeaderVO.BZBM));
		// Dataset parentHeadDs =
		// pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		// Row pRow = parentHeadDs.getSelectedRow();
		// String BxBzbm =
		// (String)pRow.getValue(parentHeadDs.nameToIndex(BXHeaderVO.BZBM));
		//
		// if (!JkBzbm.equals(BxBzbm)) {
		// throw new
		// LfwRuntimeException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("per_codes","0per_codes0107")/*@res
		// "不能够修改成不同的币种进行冲借款!"*/);
		// }//~
		//
		//
		//
		// BxcontrastVO bxcontrastVO = new BxcontrastVO();
		//
		// //冲销原币金额
		// int index = ds.nameToIndex("zyx2");
		// UFDouble cjkybje= (UFDouble)row.getValue(index);
		// bxcontrastVO.setCjkybje(cjkybje);
		//
		// bxcontrastVO.setYbje(cjkybje);
		// bxcontrastVO.setCxrq(ExpUtil.getBusiDate());
		//
		// //借款部门
		// index = ds.nameToIndex("deptid");
		// bxcontrastVO.setDeptid((String)row.getValue(index));
		//
		// //
		// index = ds.nameToIndex("djlxbm");
		// bxcontrastVO.setDjlxbm((String)row.getValue(index));
		//
		// //借款人
		// index = ds.nameToIndex("jkbxr");
		// bxcontrastVO.setJkbxr((String)row.getValue(index));
		//
		// //
		// index = ds.nameToIndex("jobid");
		// bxcontrastVO.setJobid((String)row.getValue(index));
		//
		// //TODO v6.1
		// // /bxcontrastVO.setPk_bxd(parentVO.getPk_jkbx());
		//
		// index = ds.nameToIndex("pk_org");
		// String org = (String)row.getValue(index);
		// bxcontrastVO.setPk_org(org);
		//
		//
		//
		// //借款单主键
		// index = ds.nameToIndex("pk_jkbx");
		// String pkJkbx = (String)row.getValue(index);
		// bxcontrastVO.setPk_jkd(pkJkbx);
		//
		// bxcontrastVO.setSxbz(BXStatusConst.SXBZ_NO);
		// bxcontrastVO.setSxrq(null);
		//
		// //
		// index = ds.nameToIndex("szxmid");
		// bxcontrastVO.setSzxmid((String)row.getValue(index));
		//
		//
		// //注释掉，否则，提交后再修改再提交
		// 取到的冲销ts不是库中真正的ts，更新冲销信息时比较ts会报“并发异常，数据已经更新，请重新查询数据后操作”
		// //ContrastBO saveContrast
		// index = ds.nameToIndex("ts");
		// bxcontrastVO.setTs((UFDateTime)row.getValue(index));
		//
		//
		// index = ds.nameToIndex("hkybje");
		// bxcontrastVO.setHkybje((UFDouble)row.getValue(index));
		//
		// bxcontrastVO.setFyybje(bxcontrastVO.getCjkybje().sub(bxcontrastVO.getHkybje()));
		//
		//
		//
		// //bxcontrastVO.setPk_bxcontrast(getListPanel().getHeadColumnValue(BxcontrastVO.PK_BXCONTRAST,
		// row)==null?null:getListPanel().getHeadColumnValue(BxcontrastVO.PK_BXCONTRAST,
		// row).toString());
		//
		// //bxcontrastVO.setBxdjbh(parentVO.getDjbh());
		//
		//
		//
		//
		// //借款单号
		// index = ds.nameToIndex("djbh");
		// bxcontrastVO.setJkdjbh((String)row.getValue(index));
		//
		//
		//
		//
		//
		//
		// //累计 冲借款金额
		// totalCjk = totalCjk.add(cjkybje);
		//
		//
		//
		//
		// //webSession.setAttribute(pkJkbx, org);
		// bxcontrastVO.setPk_org(org);
		//
		//
		//
		//
		// bxContrastVOlist.add(bxcontrastVO); //TODO
		//
		//
		// }

		/**
		 * 下面逻辑为为报销主表中的项设值
		 */
		Dataset bxzbDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		Row bxzbRow = bxzbDs.getCurrentRowData().getSelectedRow();

		List<BxcontrastVO> bxContrastVOlist = getContrastData();

		new SuperVO2DatasetSerializer().serialize(bxContrastVOlist.toArray(new SuperVO[0]), bxContrastDs, Row.STATE_NORMAL);

		AppUtil.addAppAttr("EXP_ISContrast", "true");

		// 为冲借款原币金额设值
		// int index_bxzb = bxzbDs.nameToIndex("cjkybje");
		// bxzbRow.setValue(index_bxzb, new UFDouble(totalCjk));
		//
		// UFDouble zero = new UFDouble(0);
		//
		// try {
		// UFDouble headYbje =
		// (UFDouble)bxzbRow.getValue(bxzbDs.nameToIndex("ybje"));
		// if(totalCjk.doubleValue() > headYbje.doubleValue()){ //设置还款项
		//
		// setJe(bxzbDs, bxzbRow, totalCjk.sub(headYbje),new
		// String[]{BXHeaderVO.HKYBJE,BXHeaderVO.HKBBJE,BXHeaderVO.GROUPHKBBJE,BXHeaderVO.GLOBALHKBBJE});
		// bxzbRow.setValue(bxzbDs.nameToIndex("zfybje"), zero);
		// bxzbRow.setValue(bxzbDs.nameToIndex("zfbbje"), zero);
		// }else if(totalCjk.doubleValue() < headYbje.doubleValue()){ //
		// setJe(bxzbDs, bxzbRow, headYbje.sub(totalCjk),new
		// String[]{BXHeaderVO.ZFYBJE,BXHeaderVO.ZFBBJE,BXHeaderVO.GROUPZFBBJE,BXHeaderVO.GLOBALZFBBJE});
		//
		// bxzbRow.setValue(bxzbDs.nameToIndex("hkybje"), zero);
		// bxzbRow.setValue(bxzbDs.nameToIndex("hkbbje"), zero);
		// }
		//
		// } catch (BusinessException e1) {
		// Logger.error(e1.getMessage(), e1);
		// throw new LfwRuntimeException(e1);
		// }
		try {
			doContrast(bxzbDs, bxzbRow, bxContrastVOlist);
		} catch (BusinessException e1) {
			Logger.error(e1.getMessage(), e1);
			throw new LfwRuntimeException(e1);
		}

		// 还款单情况下，根据表头信息生成业务行
		String djlxbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.DJLXBM));
		if (BXConstans.BILLTYPECODE_RETURNBILL.equals(djlxbm)) {
			doHkBusitem(bxzbDs, bxzbRow);
		}
		// }

		// 隐藏冲借款对话框
		AppLifeCycleContext.current().getWindowContext().closeView("cjk");
		// getGlobalContext().getParentGlobalContext().hideCurrentDialog();

		clearPrecisionProperties(ybjeArr, ds);

	}

	/**
	 * @see -ContrastAction doHkBusitem
	 * @param vo1
	 */
	private void doHkBusitem(Dataset bxzbDs, Row bxzbRow) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = pageMeta.getWidget("main");
		Dataset[] BusitemDss = ExpUtil.getBusitemDss(widget, bxzbDs.getId());

		Dataset busitemDs = BusitemDss[0];
		Row busitemRow = null;

		if (bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.CJKYBJE)).equals(new UFDouble(0))) {
			busitemDs.getCurrentRowSet().clear();
			return;
		}

		RowData rd = busitemDs.getCurrentRowSet().getCurrentRowData();
		if (rd == null)
			return;
		Row[] rows = rd.getRows();
		if (rows != null && rows.length > 0) {
			busitemRow = rows[0];
		} else {
			busitemRow = busitemDs.getEmptyRow();

			int index = busitemDs.getCurrentRowCount();
			busitemDs.insertRow(index, busitemRow);
		}

		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.TABLECODE), BXConstans.BUS_PAGE);
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.YBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.YBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.BBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.BBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.HKYBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.HKYBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.HKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.HKBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.ZFYBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.ZFYBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.ZFBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.ZFBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.CJKYBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.CJKYBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.CJKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.CJKBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GROUPBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GROUPBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GROUPHKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GROUPHKBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GROUPZFBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GROUPZFBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GROUPCJKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GROUPCJKBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GLOBALBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GLOBALBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GLOBALHKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GLOBALHKBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GLOBALZFBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GLOBALZFBBJE)));
		busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.GLOBALCJKBBJE), bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.GLOBALCJKBBJE)));
		// 20170119 tsy 源码中有，但反编译中的没有
		/*
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.PAYTARGET),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.PAYTARGET)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.RECEIVER),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.RECEIVER)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.DWBM),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.DWBM)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.DEPTID),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.DEPTID)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.JKBXR),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.JKBXR)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.SKYHZH),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.SKYHZH)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.HBBM),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.HBBM)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.CUSTOMER),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.CREATOR)));
		 * busitemRow.setValue(busitemDs.nameToIndex(BXBusItemVO.CUSTACCOUNT),
		 * bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.CUSTACCOUNT)));
		 */
		// 20170119 end
		// 移到上面else里
		// int index = busitemDs.getCurrentRowCount();
		// busitemDs.insertRow(index, busitemRow);
	}

	/**
	 * @see -BxUIControlUtil doContrast
	 * 
	 * @param bxzbDs
	 * @param bxzbRow
	 * @param bxvo
	 * @param contrastsData
	 * @return
	 * @throws BusinessException
	 */
	public JKBXVO doContrast(Dataset bxzbDs, Row bxzbRow, /* JKBXVO bxvo, */List<BxcontrastVO> contrastsData) throws BusinessException {
		// JKBXHeaderVO head = bxvo.getParentVO();
		UFDouble zero = new UFDouble(0);
		if (contrastsData == null || contrastsData.size() == 0) {

			// 取消借款单的冲销
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.CJKYBJE, zero);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.CJKBBJE, zero);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKYBJE, zero);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKBBJE, zero);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFYBJE, (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.YBJE)));
			ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFBBJE, (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.BBJE)));

			LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();

			Dataset contrastDs = pageMeta.getView("main").getViewModels().getDataset("contrast");
			contrastDs.clear();

		} else {
			UFDouble cjkybje = zero;
			for (Iterator<BxcontrastVO> iter = contrastsData.iterator(); iter.hasNext();) {

				BxcontrastVO contrast = iter.next();
				cjkybje = cjkybje.add(contrast.getCjkybje());
			}

			// 计算冲借款,还款,支付本币, 取借款单汇率, 日期取借款单日期
			setJeMul(contrastsData, /* head, */new String[] { JKBXHeaderVO.CJKYBJE, JKBXHeaderVO.CJKBBJE, JKBXHeaderVO.GROUPCJKBBJE, JKBXHeaderVO.GLOBALCJKBBJE }, bxzbDs, bxzbRow);
			// 20170712 tsy
			// UFDouble headYbje = (UFDouble)
			// bxzbRow.getValue(bxzbDs.nameToIndex("ybje"));
			UFDouble headYbje = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("zyx12"));
			// 20170712 end
			if (cjkybje.doubleValue() > headYbje.doubleValue()) {
				// 冲借款金额>报销金额，则有还款
				setHeadJe(/* head, */bxzbDs, bxzbRow, cjkybje.sub(headYbje), new String[] { JKBXHeaderVO.HKYBJE, JKBXHeaderVO.HKBBJE, JKBXHeaderVO.GROUPHKBBJE, JKBXHeaderVO.GLOBALHKBBJE });

				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFYBJE, zero);
				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFBBJE, zero);
			} else if (cjkybje.doubleValue() < headYbje.doubleValue()) {
				// 冲借款金额<报销金额，则有支付
				setHeadJe(/* head, */bxzbDs, bxzbRow, headYbje.sub(cjkybje), new String[] { JKBXHeaderVO.ZFYBJE, JKBXHeaderVO.ZFBBJE, JKBXHeaderVO.GROUPZFBBJE, JKBXHeaderVO.GLOBALZFBBJE });

				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKYBJE, zero);
				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKBBJE, zero);
			} else if (cjkybje.doubleValue() == headYbje.doubleValue()) {
				// 冲借款金额==报销金额，则既无还款，又无支付
				setHeadJe(/* head, */bxzbDs, bxzbRow, headYbje.sub(cjkybje), new String[] { JKBXHeaderVO.ZFYBJE, JKBXHeaderVO.ZFBBJE, JKBXHeaderVO.GROUPZFBBJE, JKBXHeaderVO.GLOBALZFBBJE });

				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKYBJE, zero);
				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.HKBBJE, zero);

				setHeadJe(/* head, */bxzbDs, bxzbRow, cjkybje.sub(headYbje), new String[] { JKBXHeaderVO.HKYBJE, JKBXHeaderVO.HKBBJE, JKBXHeaderVO.GROUPHKBBJE, JKBXHeaderVO.GLOBALHKBBJE });
				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFYBJE, zero);
				ExpUtil.setRowValue(bxzbRow, bxzbDs, JKBXHeaderVO.ZFBBJE, zero);
			}
		}

		// 折算表体冲借款金额
		caculateBodyCjkje(/* bxvo */bxzbDs, bxzbRow);
		return null;
	}

	/**
	 * @see -BxUIControlUtil setJeMul
	 * @param contrastsData
	 * @param yfbKeys
	 * 
	 *            设置冲销金额
	 * @throws BusinessException
	 */
	private void setJeMul(List<BxcontrastVO> contrastsData,/*
															 * JKBXHeaderVO
															 * head,
															 */
			String[] yfbKeys, Dataset bxzbDs, Row bxzbRow) throws BusinessException {
		try {

			String headOrg = (String) bxzbRow.getValue(bxzbDs.nameToIndex("pk_org"));
			String headBzbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex("bzbm"));
			UFDouble headBbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("bbhl"));
			UFDate headDjrq = (UFDate) bxzbRow.getValue(bxzbDs.nameToIndex("djrq"));
			UFDouble headGlobalbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("globalbbhl"));
			UFDouble headGroupbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("groupbbhl"));

			UFDouble[] yfbs = null;
			for (Iterator<BxcontrastVO> iter = contrastsData.iterator(); iter.hasNext();) {

				BxcontrastVO vo = iter.next();
				UFDouble cjkybje = vo.getCjkybje();
				UFDouble[] values =
						Currency.computeYFB(headOrg, Currency.Change_YBJE, headBzbm, cjkybje, null, null, null, headBbhl, headDjrq);

				vo.setCjkbbje(values[2]);
				vo.setBbje(values[2]);

				UFDouble[] money =
						Currency.computeGroupGlobalAmount(cjkybje, values[2], headBzbm, headDjrq, headOrg, ExpUtil.getPKGroup(), headGlobalbbhl, headGroupbbhl);

				vo.setGroupcjkbbje(money[0]);
				vo.setGlobalcjkbbje(money[1]);
				vo.setGroupbbje(money[0]);
				vo.setGlobalbbje(money[1]);

				if (yfbs == null) {
					yfbs = values;
				} else {
					for (int i = 0; i < 3; i++) {
						// yfbs[i] = yfbs[i].add(values[0]); //TODO v6.1 有点疑问
						// i=2 时 把原币加到本币上了
						yfbs[i] = yfbs[i].add(values[i]);
					}
				}
			}
			UFDouble[] money2 =
					Currency.computeGroupGlobalAmount(yfbs[0], yfbs[2], headBzbm, headDjrq, headOrg, ExpUtil.getPKGroup(), headGlobalbbhl, headGroupbbhl);

			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[0], yfbs[0]);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[1], yfbs[2]);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[2], money2[0]);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[3], money2[1]);
			// head.setAttributeValue(yfbKeys[0], yfbs[0]);
			// head.setAttributeValue(yfbKeys[1], yfbs[2]);
			// head.setAttributeValue(yfbKeys[2], money2[0]);
			// head.setAttributeValue(yfbKeys[3], money2[1]);

		} catch (BusinessException e) {

			// 设置本币错误.
			Log.getInstance(this.getClass()).error(e.getMessage(), e);
			throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000009")/*
																														 * @
																														 * res
																														 * "设置本币错误!"
																														 */);
		}
	}

	/**
	 * @param head
	 * @param ybje
	 * @param yfbKeys
	 * 
	 *            设置冲销金额
	 * @throws BusinessException
	 */
	private void setJe(Dataset bxzbDs, Row bxzbRow, UFDouble ybje, String[] yfbKeys) throws BusinessException {
		try {

			String headOrg = (String) bxzbRow.getValue(bxzbDs.nameToIndex("pk_org"));
			String headBzbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex("bzbm"));
			UFDouble headBbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("bbhl"));
			UFDate headDjrq = (UFDate) bxzbRow.getValue(bxzbDs.nameToIndex("djrq"));
			String pk_group = ExpUtil.getPKGroup();

			UFDouble headGlobalbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("globalbbhl"));
			UFDouble headGroupbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("groupbbhl"));

			UFDouble[] yfbs;

			bxzbRow.setValue(bxzbDs.nameToIndex(yfbKeys[0]), ybje); // 设定hkybje

			yfbs = Currency.computeYFB(headOrg, Currency.Change_YBJE, headBzbm, ybje, null, null, null, headBbhl, headDjrq);
			UFDouble[] money =
					Currency.computeGroupGlobalAmount(ybje, yfbs[2], headBzbm, headDjrq, headOrg, pk_group, headGlobalbbhl, headGroupbbhl);

			bxzbRow.setValue(bxzbDs.nameToIndex(yfbKeys[1]), yfbs[2]);
			bxzbRow.setValue(bxzbDs.nameToIndex(yfbKeys[2]), money[0]);
			bxzbRow.setValue(bxzbDs.nameToIndex(yfbKeys[3]), money[1]);

		} catch (BusinessException e) {
			// 设置本币错误.
			Log.getInstance(this.getClass()).error(e.getMessage(), e);
			throw new LfwRuntimeException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000009")/*
																															 * @
																															 * res
																															 * "设置本币错误!"
																															 */);
		}
	}

	/**
	 * 折算表头金额字段
	 * 
	 * @param head
	 * @param ybje
	 * @param yfbKeys
	 * @throws BusinessException
	 */
	private void setHeadJe(/* JKBXHeaderVO head, */Dataset bxzbDs, Row bxzbRow, UFDouble ybje, String[] yfbKeys) throws BusinessException {
		try {

			String headOrg = (String) bxzbRow.getValue(bxzbDs.nameToIndex("pk_org"));
			String headBzbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex("bzbm"));
			UFDouble headBbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("bbhl"));
			UFDate headDjrq = (UFDate) bxzbRow.getValue(bxzbDs.nameToIndex("djrq"));
			String pk_group = ExpUtil.getPKGroup();

			UFDouble headGlobalbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("globalbbhl"));
			UFDouble headGroupbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("groupbbhl"));

			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[0], ybje);
			UFDouble[] yfbs = Currency.computeYFB(headOrg, Currency.Change_YBJE, headBzbm, ybje, null, null, null, headBbhl, headDjrq);

			UFDouble[] money =
					Currency.computeGroupGlobalAmount(ybje, yfbs[2], headBzbm, headDjrq, headOrg, pk_group, headGlobalbbhl, headGroupbbhl);

			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[1], yfbs[2]);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[2], money[0]);
			ExpUtil.setRowValue(bxzbRow, bxzbDs, yfbKeys[3], money[1]);

		} catch (BusinessException e) {
			// 设置本币错误.
			Log.getInstance(this.getClass()).error(e.getMessage(), e);
			throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000009")/*
																														 * @
																														 * res
																														 * "设置本币错误!"
																														 */);
		}
	}

	/**
	 * @see -BxUIControlUtil caculateBodyCjkje 折算表体冲借款金额(同时计算支付或还款金额)
	 * @param bxvo
	 */
	public void caculateBodyCjkje(Dataset bxzbDs, Row bxzbRow /* JKBXVO bxvo */) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = pageMeta.getWidget("main");

		Dataset[] BusitemDss = ExpUtil.getBusitemDss(widget, bxzbDs.getId());

		ExpBusitemDs2SpuerVOArrSerializer busitemDsSer = new ExpBusitemDs2SpuerVOArrSerializer();
		BXBusItemVO[] chilerenVO = busitemDsSer.serialize(bxzbDs, BusitemDss); // 需ExpPageModel
																				// 中
																				// modifyCjkOKButtonSubmitRule

		if (chilerenVO == null || chilerenVO.length == 0)
			return;

		// 过滤掉金额为空的行
		chilerenVO = filterJeNullRow(chilerenVO);

		// ***更新BusitemDss 时用到
		JKBXVO jkbxvo = null;
		if (IExpConst.BXZB_DS_ID.equals(bxzbDs.getId())) {
			jkbxvo = new BXVO();
		} else {
			jkbxvo = new JKVO();
		}
		jkbxvo.setChildrenVO(chilerenVO);
		// ~

		// 重设表体
		// bxvo.setChildrenVO(chilerenVO);

		// 取得表头币种编码和汇率值，根据汇率值换算本币的值，若币种与本位币相同，则忽略界面中自定的汇率
		UFDouble cjkje = bxzbRow.getUFDobule(bxzbDs.nameToIndex(JKBXHeaderVO.CJKYBJE));
		for (int i = 0; i < chilerenVO.length; i++) {
			BXBusItemVO child = chilerenVO[i];
			if (cjkje != null) {
				// 还有没分配完的冲借款金额
				//20170712 tsy 
//				UFDouble ybje = child.getYbje();
				UFDouble ybje = new UFDouble((String)child.getDefitem25());
				//20170712 end

				// 当前行是最后一行
				if (i == chilerenVO.length - 1) {
					child.setAttributeValue(BXBusItemVO.CJKYBJE, cjkje);
					modifyValues(child);
					transYbjeToBbje(/* head, */bxzbDs, bxzbRow, child);

					busitemDsSer.update(jkbxvo, BusitemDss);

					return;
				}
				if (cjkje.compareTo(ybje) > 0) {
					// 如果剩余的冲借款金额大于原币金额，则该项的冲借款金额值与原币金额相同
					child.setAttributeValue(BXBusItemVO.CJKYBJE, ybje);
					modifyValues(child);
					transYbjeToBbje(/* head, */bxzbDs, bxzbRow, child);
					cjkje = cjkje.sub(ybje);
				} else {
					// 如果剩余的冲借款金额不大于原币金额，则该项的冲借款金额值设为剩余的冲借款金额
					child.setAttributeValue(BXBusItemVO.CJKYBJE, cjkje);
					modifyValues(child);
					transYbjeToBbje(/* head, */bxzbDs, bxzbRow, child);
					cjkje = null;
				}
			} else {
				// 冲借款金额都分配完了，剩余项都用0填补
				child.setAttributeValue(BXBusItemVO.CJKYBJE, new UFDouble(0));
				modifyValues(child);
				transYbjeToBbje(/* head, */bxzbDs, bxzbRow, child);
			}
		}

		// 更新BusitemDss
		busitemDsSer.update(jkbxvo, BusitemDss);

	}

	private BXBusItemVO[] filterJeNullRow(BXBusItemVO[] chilerenVO) {
		// 过滤掉没有金额的行
		List<BXBusItemVO> voList = new ArrayList<BXBusItemVO>();
		for (BXBusItemVO vo : chilerenVO) {
			UFDouble ybje = vo.getYbje();
			if (ybje == null) {
				ybje = UFDouble.ZERO_DBL;
			}
			if (UFDouble.ZERO_DBL.equals(ybje)) {

				// 没有原币金额的行不折算
				continue;
			}
			voList.add(vo);
		}
		return voList.toArray(new BXBusItemVO[0]);
	}

	/**
	 * 根据冲借款金额的变化，修改表体中数据的其他金额数值
	 * 
	 */
	private void modifyValues(BXBusItemVO vo) {
		//20170712 tsy 
//		UFDouble ybje = vo.getYbje();
		UFDouble ybje = new UFDouble((String)vo.getDefitem25());
		//20170712 end
		UFDouble cjkybje = vo.getCjkybje();
		if (ybje.getDouble() > cjkybje.getDouble()) {// 如果原币金额大于冲借款金额
			vo.setAttributeValue(BXBusItemVO.ZFYBJE, ybje.sub(cjkybje));// 支付金额=原币金额-冲借款金额
			vo.setAttributeValue(BXBusItemVO.HKYBJE, new UFDouble(0));
		} else {
			vo.setAttributeValue(BXBusItemVO.HKYBJE, cjkybje.sub(ybje));// 还款金额=冲借款金额-原币金额
			vo.setAttributeValue(BXBusItemVO.ZFYBJE, new UFDouble(0));
		}
	}

	private void transYbjeToBbje(/* JKBXHeaderVO head */Dataset bxzbDs, Row bxzbRow, BXBusItemVO itemVO) {

		String pk_corp = (String) bxzbRow.getValue(bxzbDs.nameToIndex("pk_org"));
		String bzbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex("bzbm"));
		UFDouble hl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("bbhl"));
		UFDate headDjrq = (UFDate) bxzbRow.getValue(bxzbDs.nameToIndex("djrq"));

		// String pk_corp = head.getPk_org();
		// String bzbm = head.getBzbm();
		// UFDouble hl = head.getBbhl();
		//20170712 tsy
//		UFDouble ybje = itemVO.getYbje();
		UFDouble ybje = new UFDouble((String)itemVO.getDefitem25());
		//20170712 end

		UFDouble cjkybje = itemVO.getCjkybje();
		UFDouble hkybje = itemVO.getHkybje();
		UFDouble zfybje = itemVO.getZfybje();
		try {
			UFDouble[] bbje = Currency.computeYFB(pk_corp, Currency.Change_YBCurr, bzbm, ybje, null, null, null, hl, headDjrq);

			itemVO.setAttributeValue(JKBXHeaderVO.BBJE, bbje[2]);
			itemVO.setAttributeValue(JKBXHeaderVO.BBYE, bbje[2]);
			bbje = Currency.computeYFB(pk_corp, Currency.Change_YBCurr, bzbm, cjkybje, null, null, null, hl, headDjrq);

			itemVO.setAttributeValue(JKBXHeaderVO.CJKBBJE, bbje[2]);
			bbje = Currency.computeYFB(pk_corp, Currency.Change_YBCurr, bzbm, hkybje, null, null, null, hl, headDjrq);

			itemVO.setAttributeValue(JKBXHeaderVO.HKBBJE, bbje[2]);
			bbje = Currency.computeYFB(pk_corp, Currency.Change_YBCurr, bzbm, zfybje, null, null, null, hl, headDjrq);
			itemVO.setAttributeValue(JKBXHeaderVO.ZFBBJE, bbje[2]);

			// 折算冲借款集团、全局本币金额
			caculateGroupAndGlobalBbje(/* head, */bxzbDs, bxzbRow, itemVO, BXBusItemVO.CJKYBJE, BXBusItemVO.CJKYBJE, BXBusItemVO.GROUPCJKBBJE, BXBusItemVO.GLOBALCJKBBJE);

			// 折算支付集团、全局本币金额
			caculateGroupAndGlobalBbje(/* head, */bxzbDs, bxzbRow, itemVO, BXBusItemVO.ZFYBJE, BXBusItemVO.ZFYBJE, BXBusItemVO.GROUPZFBBJE, BXBusItemVO.GLOBALZFBBJE);

			// 折算还款集团、全局本币金额
			caculateGroupAndGlobalBbje(/* head, */bxzbDs, bxzbRow, itemVO, BXBusItemVO.HKYBJE, BXBusItemVO.HKYBJE, BXBusItemVO.GROUPHKBBJE, BXBusItemVO.GLOBALHKBBJE);

		} catch (BusinessException e) {
			ExceptionHandler.consume(e);
		}
	}

	/**
	 * 折算集团和全局本币金额
	 * 
	 * @param ybje
	 * @param bbje
	 * @param pk_currtype
	 * @param date
	 * @param pk_org
	 * @param pk_group
	 * @param globalrate
	 * @param grouprate
	 * @throws BusinessException
	 */
	private static void caculateGroupAndGlobalBbje(/* JKBXHeaderVO head, */Dataset bxzbDs, Row bxzbRow, BXBusItemVO itemVO, String ybjeField, String bbjeField, String groupbbjeField, String globalbbjeField)
			throws BusinessException {

		String headOrg = (String) bxzbRow.getValue(bxzbDs.nameToIndex("pk_org"));
		String headBzbm = (String) bxzbRow.getValue(bxzbDs.nameToIndex("bzbm"));
		UFDouble headBbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("bbhl"));
		UFDate headDjrq = (UFDate) bxzbRow.getValue(bxzbDs.nameToIndex("djrq"));
		String pk_group = ExpUtil.getPKGroup();

		UFDouble headGlobalbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("globalbbhl"));
		UFDouble headGroupbbhl = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex("groupbbhl"));

		UFDouble[] moneys =
				Currency.computeGroupGlobalAmount((UFDouble) itemVO.getAttributeValue(ybjeField), (UFDouble) itemVO.getAttributeValue(bbjeField), headBzbm, headDjrq, headOrg, pk_group, headGlobalbbhl, headGroupbbhl);

		// 集团
		itemVO.setAttributeValue(groupbbjeField, moneys[0]);

		// 全局
		itemVO.setAttributeValue(globalbbjeField, moneys[1]);
	}

	public void afterEdit(CellEvent e) {

		LfwWindow pageMeta = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset bxzbDs = pageMeta.getWidget("main").getViewModels().getDataset("bxzb");
		Row bxzbRow = bxzbDs.getCurrentRowData().getSelectedRow();

		String datasetID = e.getSource().getDataset();
		Dataset jkzbDs =
				AppLifeCycleContext.current().getWindowContext().getWindow().getWidget("cjk").getViewModels().getDataset(datasetID);
		Row jkzbRow = jkzbDs.getCurrentRowData().getRows()[e.getRowIndex()];

		UFDouble bxybje = (UFDouble) bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.YBJE));

		/*
		 * if (e.getColIndex() == 0){ //选中项 String selectValue =
		 * (String)e.getNewValue(); if ("true".equals(selectValue)) { String
		 * jkdpk_corp = (String)jkzbRow.getValue(jkzbDs.nameToIndex("pk_org"));
		 * String bxdpk_corp =
		 * (String)bxzbRow.getValue(bxzbDs.nameToIndex("pk_org")); try{ String
		 * bbpk = Currency.getOrgLocalCurrPK(jkdpk_corp); String bbpk2 =
		 * Currency.getOrgLocalCurrPK(bxdpk_corp); if(
		 * !VOUtils.simpleEquals(bbpk2, bbpk)){ throw new
		 * LfwRuntimeException(nc.
		 * vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID
		 * ("COMMON","UPP2011-000372"));@res
		 * "两张单据的所在公司本位币不同，不能进行冲借款操作，请选择其他单据"); } }catch (BusinessException e1)
		 * { throw new LfwRuntimeException(e1);
		 * //getListPanel().setHeadColumnValue
		 * (BXHeaderVO.SELECTED,e.getRow(),new Boolean(false)); }
		 * 
		 * UFDouble ybye =
		 * (UFDouble)jkzbRow.getValue(jkzbDs.nameToIndex(BXHeaderVO.YBYE));
		 * //UFDouble bxybje =
		 * (UFDouble)bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.YBJE));
		 * 
		 * 
		 * UFBoolean isXeBill
		 * =(UFBoolean)bxzbRow.getValue(bxzbDs.nameToIndex(BXHeaderVO.ISCHECK));
		 * 
		 * if(isXeBill.booleanValue()){ //是限额支票型单据
		 * //jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.YBYE), value)
		 * //getListPanel().setHeadColumnValue("zpje",e.getRow(),bxybje);
		 * //getListPanel().setHeadColumnValue(BXHeaderVO.CJKYBJE,e.getRow(),new
		 * UFDouble(0));
		 * //getListPanel().setHeadColumnValue(BxcontrastVO.HKYBJE,
		 * e.getRow(),new UFDouble(0)); }else{
		 * jkzbRow.setValue(jkzbDs.nameToIndex("zyx2"), ybye);
		 * jkzbRow.setValue(jkzbDs.nameToIndex("hkybje"), new UFDouble(0)); } }
		 * else{ jkzbRow.setValue(jkzbDs.nameToIndex("zyx2"), new UFDouble(0));
		 * jkzbRow.setValue(jkzbDs.nameToIndex("hkybje"), new UFDouble(0));
		 * //getListPanel().getHeadItem(BXHeaderVO.CJKYBJE).setEnabled(false);
		 * //getListPanel().getHeadItem(BXHeaderVO.HKYBJE).setEnabled(false);
		 * 
		 * } this.resetHkje(jkzbDs, bxybje);
		 * 
		 * } else
		 */if (e.getColIndex() == 9) { // 编辑冲借款金额

			// TODO 从新设置ds中的数据， 目前响应gridcelllistener 框架没有将最新的ds提交到后台
			jkzbRow.setValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE), new UFDouble((String) e.getNewValue()));

			UFDouble ybye = (UFDouble) jkzbRow.getValue(jkzbDs.nameToIndex(BXHeaderVO.YBYE));
			UFDouble cjkybje = (UFDouble) jkzbRow.getValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE));
			if (cjkybje.compareTo(ybye) > 0) {
				AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000177")/*
																																	 * @
																																	 * res
																																	 * "冲借款金额不能大于借款单余额!"
																																	 */);
				jkzbRow.setValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE), ybye);
			} else if (cjkybje.compareTo(new UFDouble(0)) < 0) {
				AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000178")/*
																																	 * @
																																	 * res
																																	 * "冲借款金额必须大于零!"
																																	 */);
				jkzbRow.setValue(jkzbDs.nameToIndex(EXP_CJK_JKZB_CJKYBJE), ybye);
			} else {
				this.resetHkje(jkzbDs, bxybje);
			}

		} else if (e.getColIndex() == 10) { // 编辑还款原币金额
			// TODO 从新设置ds中的数据， 目前响应gridcelllistener 框架没有将最新的ds提交到后台
			jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), new UFDouble((String) e.getNewValue()));
			UFDouble hkje = (UFDouble) jkzbRow.getValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE));

			if (hkje.compareTo(new UFDouble(0)) < 0) {
				AppInteractionUtil.showMessageDialog(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("COMMON", "UPP2011-000373")/*
																																	 * @
																																	 * res
																																	 * "还款金额不能小于零!"
																																	 */);
				jkzbRow.setValue(jkzbDs.nameToIndex(BXHeaderVO.HKYBJE), new UFDouble(0));
			}
		}

	}

	/**
	 * 清除为ds的fied上设置的精度， 否则关掉冲借款界面后，填其他金额时更新精度时会报错 “pageUI.getWidget("cjk") is
	 * undefined”，是否当成平台bug待确定?
	 * 
	 * @param fields
	 * @param ds
	 */
	private void clearPrecisionProperties(String[] fields, Dataset JkdDs) {

		for (String field : fields) {
			// TODO v63 临时修改防止做盘报错。
			// List<String> list =
			// JkdDs.getFieldSet().getField(field).getCtxChangedProperties();
			List<String> list = new ArrayList<String>();

			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					String pro = list.get(i);
					if ("precision".equals(pro)) {
						list.remove(i);
						i--;
					}
				}
			}
		}
	}

	// 设置表头中的总金额
	public void setHeadTotalAmount() {

		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

		UFDouble sumCjkje = sumCjkje();

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkzbDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKZB_DS);

		int selectedRowIndex = 0;
		Row currentHeadRow = jkzbDs.getSelectedRow();
		if (currentHeadRow != null) {

			selectedRowIndex = jkzbDs.getSelectedIndex();

			ExpUtil.setRowValue(currentHeadRow, jkzbDs, EXP_CJK_JKZB_CJKYBJE, sumCjkje);
		} else {
			selectedRowIndex = -1;
		}

		// 取到 表头合计冲借款金额
		UFDouble totalcjk = getHeadTotalCjkje();

		// 没有要冲借款的借款单时
		if (totalcjk == null || totalcjk.doubleValue() == 0) {
			if (currentHeadRow != null) {
				ExpUtil.setRowValue(currentHeadRow, jkzbDs, JKBXHeaderVO.HKYBJE, new UFDouble(0));
			}
			return;
		}

		// 计算出还款金额
		UFDouble hkje = totalcjk.sub(getBxje()).doubleValue() < 0 ? new UFDouble(0) : totalcjk.sub(getBxje());
		// 得到所有表头行的值

		// JKHeaderVO[] dataVector = (JKHeaderVO[])
		// getListPanel().getHeadBillModel().getBodyValueVOs(
		// nc.vo.ep.bx.JKHeaderVO.class.getName());
		Row[] headRowArr = jkzbDs.getCurrentRowData().getRows();

		int row = 0;

		// for (JKHeaderVO jkHeaderVO : dataVector) {
		for (Row headRow : headRowArr) {
			Map<String, BXBusItemVO> map = selectedVO.get(headRow.getValue(jkzbDs.nameToIndex(JKBXHeaderVO.PK_JKBX)));
			int jkhkje = 0;
			if (map != null) {
				List<BXBusItemVO> list = new ArrayList<BXBusItemVO>();

				// 先按照业务行的pk排序
				for (BXBusItemVO order : map.values()) {
					list.add(order);
				}
				Collections.sort(list, new AddrComparator());

				for (BXBusItemVO item : list) {
					if (item.getCjkybje().compareTo(hkje) >= 0) {
						// 如果冲借款金额大于还款金额，设置还款金额,并设置还款金额为0
						item.setHkybje(hkje);
						hkje = new UFDouble(0);
					} else {
						// 如果冲借款金额小于还款金额，设置冲借款金额 ，并减少还款金额
						// 20170712 tsy
						// item.setHkybje(item.getCjkybje());
						item.setHkybje(hkje);
						// 20170712 end
						hkje = hkje.sub(item.getCjkybje());
					}
					jkhkje += item.getHkybje().doubleValue();
					if (selectedRowIndex == row) {
						// 当前选择行的子表，界面更新
						setBodyColumnValue(BXBusItemVO.HKYBJE, BXBusItemVO.PK_BUSITEM, item.getPk_busitem(), item.getHkybje());
					}
				}
			}

			// jkHeaderVO.setHkybje(new UFDouble(jkhkje));
			// getListPanel().getHeadBillModel().setValueAt(jkHeaderVO.getHkybje(),
			// row++, JKBXHeaderVO.HKYBJE);
			ExpUtil.setRowValue(headRow, jkzbDs, JKBXHeaderVO.HKYBJE, new UFDouble(jkhkje));
			row++;
		}
	}

	// 计算表体的冲借款金额之和
	private UFDouble sumCjkje() {
		UFDouble totalcjkje = new UFDouble(0);
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbusitemDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKBUSITEM_DS);
		if (jkbusitemDs.getCurrentRowData() != null) {
			Row[] rowArr = jkbusitemDs.getCurrentRowData().getRows();

			for (int i = 0; i < rowArr.length; i++) {
				Row row = rowArr[i];
				UFBoolean selected = (UFBoolean) row.getValue(jkbusitemDs.nameToIndex("selected"));
				if (UFBoolean.FALSE.equals(selected)) {

					continue;
				}
				if (UFBoolean.TRUE.equals(selected)) {
					UFDouble cjkje = (UFDouble) row.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.CJKYBJE));
					totalcjkje = totalcjkje.add(cjkje);
				}
			}
		}
		return totalcjkje;
	}

	// 合计表体的还款金额之和
	private UFDouble sumHkje() {
		UFDouble totalhkje = new UFDouble(0);

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbusitemDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKBUSITEM_DS);
		Row[] rowArr = jkbusitemDs.getCurrentRowData().getRows();

		for (int i = 0; i < rowArr.length; i++) {
			Row row = rowArr[i];
			UFBoolean selected = (UFBoolean) row.getValue(jkbusitemDs.nameToIndex("selected"));
			if (UFBoolean.FALSE.equals(selected)) {

				continue;
			}
			if (UFBoolean.TRUE.equals(selected)) {
				UFDouble hkje = (UFDouble) row.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.HKYBJE));
				totalhkje = totalhkje.add(hkje);
			}
		}

		return totalhkje;
	}

	// 计算表头的冲借款金额之和
	private UFDouble getHeadTotalCjkje() {

		// UFDouble totalcjkje = new UFDouble(0);
		// LfwWindow jkbxWindow =
		// AppLifeCycleContext.current().getWindowContext().getWindow();
		// Dataset jkzbDs =
		// jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKZB_DS);
		// Row[] rowArr = jkzbDs.getCurrentRowData().getRows();
		//
		// for (int i = 0; i < rowArr.length; i++) {
		// Row row = rowArr[i];
		// UFDouble cjkje = (UFDouble)row.getValue(jkzbDs.nameToIndex("zyx2"))
		// == null ?
		// UFDouble.ZERO_DBL:(UFDouble)row.getValue(jkzbDs.nameToIndex("zyx2"));
		// totalcjkje = totalcjkje.add(cjkje);
		// }
		// return totalcjkje;

		UFDouble totalcjkje = new UFDouble(0);

		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

		for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO.entrySet()) {
			Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

			if (busiitems == null || busiitems.isEmpty()) {
				continue;
			}
			for (BXBusItemVO item : busiitems.values()) {
				UFDouble thisCjkybje = item.getCjkybje() == null ? new UFDouble(0) : item.getCjkybje();
				totalcjkje = totalcjkje.add(thisCjkybje);
			}

		}

		return totalcjkje;

	}

	// 多个或者单个
	private UFDouble getBxje() {
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset bxzbDs = jkbxWindow.getView("main").getViewModels().getDataset(IExpConst.EXP_DATASET_ID);
		Row row = bxzbDs.getSelectedRow();
		// return (UFDouble)
		// row.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.YBJE));
		return new UFDouble((String) row.getValue(bxzbDs.nameToIndex("zyx12")));

	}

	public void setSelectedVO(Row jkbusitemRow) {

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbusitemDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKBUSITEM_DS);

		String pk_jkbx = (String) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.PK_JKBX));
		String pk_busitem = (String) jkbusitemRow.getValue(jkbusitemDs.nameToIndex(BXBusItemVO.PK_BUSITEM));

		Dataset2SuperVOSerializer superSer = new Dataset2SuperVOSerializer<SuperVO>();
		SuperVO[] spuerVO = superSer.serialize(jkbusitemDs, jkbusitemRow);
		BXBusItemVO bxbusitemVO = (BXBusItemVO) spuerVO[0];

		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

		Map<String, BXBusItemVO> map = selectedVO.get(pk_jkbx);
		if (map == null) {
			map = new HashMap<String, BXBusItemVO>();
			selectedVO.put(pk_jkbx, map);
		}
		map.put(pk_busitem, bxbusitemVO);
	}

	private void setBodyColumnValue(String key, String compKey, Object compValue, Object value) {

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset jkbusitemDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKBUSITEM_DS);
		Row[] rowArr = jkbusitemDs.getCurrentRowData().getRows();
		for (Row row : rowArr) {

			Object v1 = row.getValue(jkbusitemDs.nameToIndex(compKey));
			if (compValue.equals(v1)) {
				ExpUtil.setRowValue(row, jkbusitemDs, key, value);
			}
		}

	}

	/**
	 * 定义个比较器
	 */
	class AddrComparator implements Comparator<BXBusItemVO> {

		@Override
		public int compare(BXBusItemVO vo1, BXBusItemVO vo2) {
			return vo1.getPk_busitem().compareTo(vo2.getPk_busitem());
		}
	}

	/**
	 * @return　
	 * 
	 *         冲借款信息选择后，返回信息，进行后续业务处理 由表头和表体的数据组成
	 */
	public List<BxcontrastVO> getContrastData() {
		List<BxcontrastVO> list = new ArrayList<BxcontrastVO>();
		// JKBXHeaderVO parentVO = bxvo.getParentVO();

		// 已经选择的借款单主表vo 用来给JKBXVO.jkHeadVOs设值
		// 20170119 tsy 源码中有，但反编译中的没有
		/*
		 * List<JKHeaderVO> selectedJKHeadVOsList = new ArrayList<JKHeaderVO>();
		 * LfwRuntimeEnvironment
		 * .getWebContext().getRequest().getSession().setAttribute
		 * (IExpConst.CJK_SELECTED_JKHEADVOS_LIST, selectedJKHeadVOsList);
		 */
		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		Dataset bxzbDs = jkbxWindow.getView("main").getViewModels().getDataset("bxzb");
		Row bxzbRow = bxzbDs.getCurrentRowData().getSelectedRow();

		String pk_jkbx = (String) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.PK_JKBX));
		String bxdjbh = (String) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.DJBH));

		Dataset jkzbDs = jkbxWindow.getView(IExpConst.YER_CJK_VIEW).getViewModels().getDataset(EXP_CJK_JKZB_DS);
		// 20170119 tsy 源码中有，但反编译中的没有
		/*
		 * Map<String, JKHeaderVO> jKHeaderVOMap = new HashMap<String,
		 * JKHeaderVO>();
		 * 
		 * SuperVO[] jkHeaderVOArr = new
		 * Dataset2SuperVOSerializer().serialize(jkzbDs); if (jkHeaderVOArr !=
		 * null && jkHeaderVOArr.length > 0) { for (int i = 0; i <
		 * jkHeaderVOArr.length; i++) { JKHeaderVO thisVo = (JKHeaderVO)
		 * jkHeaderVOArr[i]; jKHeaderVOMap.put(thisVo.getPk_jkbx(), thisVo); } }
		 */
		// 20170119 end
		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);

		try {
			String bx_pkpog = (String) bxzbRow.getValue(bxzbDs.nameToIndex(JKBXHeaderVO.PK_ORG));
			String bxCurrPK = Currency.getLocalCurrPK(bx_pkpog);

			StringBuffer msginfo = new StringBuffer();
			for (Entry<String, Map<String, BXBusItemVO>> selectedvo : selectedVO.entrySet()) {
				String jkdpk = selectedvo.getKey();
				Map<String, BXBusItemVO> busiitems = selectedvo.getValue();

				if (busiitems == null || busiitems.isEmpty()) {
					continue;
				}
				// 20170119 tsy 源码中有，但反编译中的没有
				/*
				 * selectedJKHeadVOsList.add(jKHeaderVOMap.get(jkdpk));
				 */
				// 20170119 end
				String jk_pkorg = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.PK_ORG);
				String jk_djbh = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.DJBH);

				String deptid = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.DEPTID);
				String djlxbm = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.DJLXBM);
				String jkbxr = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.JKBXR);
				String jobid = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.JOBID);
				// String pk_org =
				// (String)getHeadColumnValue(jkzbDs,JKBXHeaderVO.PK_JKBX,
				// jkdpk,JKBXHeaderVO.PK_ORG);
				// String djbh =
				// (String)getHeadColumnValue(jkzbDs,JKBXHeaderVO.PK_JKBX,
				// jkdpk,JKBXHeaderVO.DJBH);
				String pk_payorg = (String) getHeadColumnValue(jkzbDs, JKBXHeaderVO.PK_JKBX, jkdpk, JKBXHeaderVO.PK_PAYORG);

				if (jk_pkorg == null || jk_djbh == null) { // 冲两个人的借款时，有一个ds中是取不到的，此时从库中取
					List<JKBXHeaderVO> headVoList =
							((IBXBillPrivate) NCLocator.getInstance().lookup(IBXBillPrivate.class.getName())).queryHeadersByPrimaryKeys(new String[] { jkdpk }, BXConstans.JK_DJDL);
					JKBXHeaderVO jkheaderVO = headVoList.get(0);
					jk_pkorg = jkheaderVO.getPk_org();
					jk_djbh = jkheaderVO.getDjbh();

					deptid = jkheaderVO.getDeptid();
					djlxbm = jkheaderVO.getDjlxbm();
					jkbxr = jkheaderVO.getJkbxr();
					jobid = jkheaderVO.getJobid();
					// String pk_org =
					// (String)getHeadColumnValue(jkzbDs,JKBXHeaderVO.PK_JKBX,
					// jkdpk,JKBXHeaderVO.PK_ORG);
					// String djbh =
					// (String)getHeadColumnValue(jkzbDs,JKBXHeaderVO.PK_JKBX,
					// jkdpk,JKBXHeaderVO.DJBH);
					pk_payorg = jkheaderVO.getPk_payorg();
				}

				String jkCurrPK = Currency.getLocalCurrPK(jk_pkorg);
				if (!bxCurrPK.equals(jkCurrPK)) {
					msginfo.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000900") + jk_djbh + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000901"));
				}

				if (msginfo != null && msginfo.length() > 0) {// 报销单与借款单的组织本币不一致不能冲借款
					throw new LfwRuntimeException(msginfo.toString());
				}

				for (BXBusItemVO item : busiitems.values()) {
					BxcontrastVO bxcontrastVO = new BxcontrastVO();
					bxcontrastVO.setYbje(item.getCjkybje());
					bxcontrastVO.setCjkybje(item.getCjkybje());
					bxcontrastVO.setCxrq(ExpUtil.getBusiDate());
					bxcontrastVO.setDeptid(deptid);
					bxcontrastVO.setDjlxbm(djlxbm);
					bxcontrastVO.setJkbxr(jkbxr);
					bxcontrastVO.setJobid(jobid);
					bxcontrastVO.setPk_payorg(pk_payorg);
					bxcontrastVO.setPk_bxd(pk_jkbx);
					bxcontrastVO.setPk_org(jk_pkorg);
					bxcontrastVO.setPk_jkd(item.getPk_jkbx());
					bxcontrastVO.setPk_busitem(item.getPk_busitem());
					bxcontrastVO.setSxbz(BXStatusConst.SXBZ_NO);
					bxcontrastVO.setSxrq(null);
					bxcontrastVO.setSzxmid(item.getSzxmid());
					bxcontrastVO.setHkybje(item.getHkybje() == null ? UFDouble.ZERO_DBL : item.getHkybje());
					bxcontrastVO.setFyybje(bxcontrastVO.getCjkybje().sub(bxcontrastVO.getHkybje()));
					bxcontrastVO.setBxdjbh(bxdjbh);
					bxcontrastVO.setJkdjbh(jk_djbh);
					list.add(bxcontrastVO);
				}

			}

		} catch (BusinessException e) {

			Logger.error(e.getMessage(), e);
		}

		return list;
	}

	/**
	 * 加载冲销行的数据
	 */
	private void loadselectedVO(SuperVO[] contrastVO) {

		Map<String, Map<String, BXBusItemVO>> selectedVO =
				(Map<String, Map<String, BXBusItemVO>>) LfwRuntimeEnvironment.getWebContext().getRequest().getSession().getAttribute(EXP_CJK_SELECTEDVO_MAP);
		// 20170119 tsy 源码中有，但反编译中的没有
		/*
		 * ArrayList<String> temList = new ArrayList<String>(); for (SuperVO svo
		 * : contrastVO) { temList.add(((BxcontrastVO) svo).getPk_busitem()); }
		 * String[] temArr = temList.toArray(new String[0]); Map<String,
		 * UFDouble> temMap = new HashMap<String, UFDouble>(); try {
		 * BXBusItemVO[] buvo =
		 * nc.bs.erm.util.CacheUtil.getVOArrayByPkArray(BXBusItemVO.class,
		 * temArr); if (buvo != null) { for (int i = 0; i < buvo.length; i++) {
		 * temMap.put(buvo[i].getPk_busitem(), buvo[i].getYbye()); } } } catch
		 * (BusinessException e1) { Logger.error(e1.getMessage(), e1); throw new
		 * LfwRuntimeException(e1.getMessage(), e1); }
		 */
		// 20170119 end
		for (SuperVO svo : contrastVO) {
			BxcontrastVO vo = (BxcontrastVO) svo;
			BXBusItemVO busitemvo = new BXBusItemVO();
			busitemvo.setSelected(UFBoolean.TRUE);
			busitemvo.setSzxmid(vo.getSzxmid());
			busitemvo.setPk_jkbx(vo.getPk_jkd());
			busitemvo.setPk_busitem(vo.getPk_busitem());
			busitemvo.setCjkybje(vo.getCjkybje());
			busitemvo.setHkybje(vo.getHkybje());
			busitemvo.setPk_bxcontrast(vo.getPk_bxcontrast());
			Map<String, BXBusItemVO> map = selectedVO.get(vo.getPk_jkd());
			if (map == null) {
				map = new HashMap<String, BXBusItemVO>();
				selectedVO.put(vo.getPk_jkd(), map);
			}

			// 对于冲销行和借款单业务行不一致时，处理方式
			BXBusItemVO item = map.get(vo.getPk_busitem());
			if (item != null) {
				busitemvo.setCjkybje((item.getCjkybje().add(vo.getCjkybje())));
			}
			// 20170119 tsy 源码中有，但反编译中的没有
			/*
			 * if (temMap != null) { if (temMap.get(vo.getPk_busitem()) != null)
			 * { busitemvo.setYbye(temMap.get(vo.getPk_busitem())); } else {
			 * busitemvo.setYbye(UFDouble.ZERO_DBL); } }
			 */
			// 20170119 end
			map.put(vo.getPk_busitem(), busitemvo);
		}

	}

	public void setGridColumnEditable(String gridID, String[] eleNames, boolean editable) {

		LfwWindow jkbxWindow = AppLifeCycleContext.current().getWindowContext().getWindow();
		LfwView widget = jkbxWindow.getView(IExpConst.YER_CJK_VIEW);
		GridComp gridComp = (GridComp) widget.getViewComponents().getComponent(gridID);
		if (gridComp != null) {
			for (String eleName : eleNames) {
				GridColumn ele = (GridColumn) gridComp.getColumnById(eleName);
				if (ele != null) {
					ele.setEditable(editable);
				}
			}
		}

	}

	// 得到借款表体业务行
	public BXBusItemVO[] queryByHeaders(String pk_jkd, String pk_bxd) throws BusinessException {
		if (pk_jkd == null) {
			return null;
		}
		return NCLocator.getInstance().lookup(IBxUIControl.class).queryByPk(pk_jkd, pk_bxd);
	}

}
