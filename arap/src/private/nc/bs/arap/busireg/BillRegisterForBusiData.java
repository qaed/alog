package nc.bs.arap.busireg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nc.bd.accperiod.InvalidAccperiodExcetion;
import nc.bs.arap.bill.ArapBillCalUtil;
import nc.bs.arap.billact.LogTime;
import nc.bs.arap.util.ArapMaterialUtils;
import nc.bs.arap.util.SqlUtils;
import nc.bs.businessevent.BdUpdateEvent;
import nc.bs.businessevent.BusinessEvent;
import nc.bs.businessevent.IBusinessEvent;
import nc.bs.businessevent.IBusinessListener;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.framework.core.util.ObjectCreator;
import nc.bs.logging.Log;
import nc.bs.logging.Logger;
import nc.bs.uap.lock.PKLock;
import nc.itf.arap.prv.IArapVerifyLogPrivate;
import nc.itf.fi.pub.Currency;
import nc.itf.fi.pub.SysInit;
import nc.itf.uap.busibean.SysinitAccessor;
import nc.login.bs.IServerEnvironmentService;
import nc.md.data.access.NCObject;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pubitf.accperiod.AccountCalendar;
import nc.pubitf.arap.pub.IArap4VerifyQryBill;
import nc.pubitf.arap.receivable.IArapReceivableBillPubQueryService;
import nc.pubitf.arap.termitem.IArapTermItemPubQueryService;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.pubitf.uapbd.CurrencyRateUtilHelper;
import nc.pubitf.uapbd.ICustomerPubService;
import nc.pubitf.uapbd.ISupplierPubService;
import nc.vo.arap.agiotage.AgiotageChildVO;
import nc.vo.arap.agiotage.ArapBusiDataVO;
import nc.vo.arap.agiotage.ArapBusiDataVOList;
import nc.vo.arap.baddebts.BaddebtsReceVO;
import nc.vo.arap.baddebts.BaddebtsRechVO;
import nc.vo.arap.basebill.BaseAggVO;
import nc.vo.arap.basebill.BaseBillVO;
import nc.vo.arap.basebill.BaseItemVO;
import nc.vo.arap.debttransfer.DebtTransferVO;
import nc.vo.arap.pay.PayBillItemVO;
import nc.vo.arap.payable.PayableBillItemVO;
import nc.vo.arap.pfflow.ArapBillMapVO;
import nc.vo.arap.pfflow.ArapBillMapVOTool;
import nc.vo.arap.pub.ArapBillTypeInfo;
import nc.vo.arap.pub.ArapConstant;
import nc.vo.arap.pub.BillEnumCollection;
import nc.vo.arap.receivable.ReceivableBillItemVO;
import nc.vo.arap.sysinit.SysinitConst;
import nc.vo.arap.termitem.TermVO;
import nc.vo.arap.utils.ArrayUtil;
import nc.vo.arap.utils.CmpQueryModulesUtil;
import nc.vo.arap.utils.StringUtil;
import nc.vo.arap.verify.AggverifyVO;
import nc.vo.arap.verify.RBVerify;
import nc.vo.arap.verify.SameMnyVerify;
import nc.vo.arap.verify.UnsameMnyVerify;
import nc.vo.arap.verify.VerifyDetailVO;
import nc.vo.arap.verify.VerifyTool;
import nc.vo.bd.accessor.IBDData;
import nc.vo.bd.cust.CustomerVO;
import nc.vo.bd.supplier.SupplierVO;
import nc.vo.fipub.exception.ExceptionHandler;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.verifynew.pub.DefaultVerifyRuleVO;
import nc.vo.verifynew.pub.VerifyCom;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class BillRegisterForBusiData implements IBusinessListener {
	public BillRegisterForBusiData() {
	}

	private static String SPECIAL_KEY = "def1";

	Map<String, ArrayList<AggverifyVO>> aggMap = new HashMap();
	Map<String, List<TermVO>> termVOMap = new HashMap();
	AggregatedValueObject[] aggvos = null;

	public AggregatedValueObject[] getAggvos() {
		return this.aggvos;
	}

	public void setAggvos(AggregatedValueObject[] aggvos) {
		this.aggvos = aggvos;
	}

	String pkorg = "";
	private String pk_user;
	private Integer syscode;

	public Integer getSyscode() {
		return this.syscode;
	}

	public void setSyscode(Integer syscode) {
		this.syscode = syscode;
	}

	public String getPkorg() {
		return this.pkorg;
	}

	public void setPkorg(String pkorg) {
		this.pkorg = pkorg;
	}

	public String getPk_user() {
		return this.pk_user;
	}

	public void setPk_user(String pkUser) {
		this.pk_user = pkUser;
	}

	protected AggregatedValueObject[] dealUserObj(Object obj) {
		if ((obj instanceof BusinessEvent.BusinessUserObj)) {
			obj = ((BusinessEvent.BusinessUserObj) obj).getUserObj();
		}
		AggregatedValueObject[] retvos = null;
		if (obj.getClass().isArray()) {
			retvos = (AggregatedValueObject[]) obj;
		} else {
			retvos = new AggregatedValueObject[] { (AggregatedValueObject) obj };
		}
		this.pk_user = ((String) retvos[0].getParentVO().getAttributeValue("approver"));
		this.syscode = ((Integer) retvos[0].getParentVO().getAttributeValue("syscode"));
		this.pkorg = ((String) retvos[0].getParentVO().getAttributeValue("pk_org"));
		return retvos;
	}

	public void doAction(IBusinessEvent event) throws BusinessException {
		long t1 = System.currentTimeMillis();
		if ("200604".equals(event.getEventType())) {
			BusinessEvent e = (BusinessEvent) event;
			if (null != e.getUserObject()) {
				AggregatedValueObject[] vos = dealUserObj(e.getUserObject());
				setAggvos(vos);
				for (AggregatedValueObject obj : vos) {
					CircularlyAccessibleValueObject parentVO = obj.getParentVO();
					UFBoolean isinit = (UFBoolean) parentVO.getAttributeValue("isinit");
					if ((isinit != null) && (isinit.booleanValue())) {
						return;
					}
				}
				Object[] transform = transform(vos);
				List<ArapBusiDataVO> insertdatas = (List) transform[0];
				List<ArapBusiDataVO> updatedatas = (List) transform[1];
				if ((insertdatas != null) && (insertdatas.size() > 0)) {
					setAreaPKForBusiData(insertdatas);
				}

				doInsertRegister(insertdatas);

				if ((updatedatas != null) && (updatedatas.size() > 0)) {
					setAreaPKForBusiData(updatedatas);
				}

				doInsertRegister(updatedatas);

				for (AggregatedValueObject obj : vos) {
					String billclass = (String) obj.getParentVO().getAttributeValue("billclass");
					if ((billclass != null) && ((billclass.equals(ArapConstant.ARAP_ZS_BILLCLASS)) || (billclass.equals(ArapConstant.ARAP_ZF_BILLCLASS)))) {
						return;
					}
				}

				List<String> pks = new ArrayList();
				List<ArapBillMapVO> scmList = new ArrayList();
				List<ArapBillMapVO> defF2List = new ArrayList();
				List<ArapBillMapVO> psList = new ArrayList();
				List<ArapBillMapVO> pcmList = new ArrayList();

				Hashtable<String, DefaultVerifyRuleVO> ruleVOMap = creatVerifyRuleVO();
				VerifyCom com = new VerifyCom();

				billRBVerify(vos, ruleVOMap, com);

				for (AggregatedValueObject vo : vos) {
					Integer src_syscode = (Integer) vo.getParentVO().getAttributeValue("src_syscode");
					BaseItemVO[] childrenVO = (BaseItemVO[]) vo.getChildrenVO();

					if (("21".equals(childrenVO[0].getTop_billtype())) || ("36D1".equals(childrenVO[0].getTop_billtype())) || ("36D7".equals(childrenVO[0].getTop_billtype()))) {

						for (BaseItemVO itemVO : childrenVO) {
							if (itemVO.getTop_termch() == null) {
								ArapBillMapVO mapVO = ArapBillMapVOTool.changeVotoBillMapNew((BaseBillVO) vo.getParentVO(), itemVO);
								mapVO.setS_itemid(itemVO.getPurchaseorder());
								scmList.add(mapVO);
							} else {
								ArapBillMapVO mapVO = new ArapBillMapVO();
								mapVO.setT_itemid(itemVO.getPrimaryKey());
								mapVO.setS_termid(itemVO.getTop_termch());
								mapVO.setS_system(src_syscode.intValue());
								mapVO.setS_billtype(itemVO.getTop_billtype());
								mapVO.setYbje(itemVO.getMoney_bal());
								psList.add(mapVO);
							}
						}
					} else if (("25".equals(childrenVO[0].getTop_billtype())) || ("45".equals(childrenVO[0].getTop_billtype())) || ("Z2".equals(childrenVO[0].getTop_billtype()))) {

						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = ArapBillMapVOTool.changeVotoBillMapNew((BaseBillVO) vo.getParentVO(), itemVO);
							scmList.add(mapVO);
						}
					} else if (("30".equals(childrenVO[0].getSrc_billtype())) || ("Z3".equals(childrenVO[0].getTop_billtype())) || ("32".equals(childrenVO[0].getTop_billtype()))) {

						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = ArapBillMapVOTool.changeVotoBillMapNewForSo((BaseBillVO) vo.getParentVO(), itemVO);
							scmList.add(mapVO);
						}
					} else if (("4D48".equals(childrenVO[0].getTop_billtype())) || ("4D50".equals(childrenVO[0].getTop_billtype())) || ("4D46".equals(childrenVO[0].getTop_billtype()))) {

						for (BaseItemVO itemVO : childrenVO) {
							String srcBillid = itemVO.getSrc_billid();
							if (StringUtils.isNotEmpty(srcBillid)) {
								if ("4D46".equals(childrenVO[0].getTop_billtype())) {
									Collection<PayableBillItemVO> itemCollections = MDPersistenceService.lookupPersistenceQueryService().queryBillOfVOByCond(ArapBillTypeInfo.getInstance("F1").getItemvoClass(), SqlUtils.getInStr("src_billid", new String[] { srcBillid }) + " and dr = 0", false);

									if (itemCollections != null) {
										for (PayableBillItemVO item : itemCollections) {
											ArapBillMapVO mapVO = new ArapBillMapVO();
											mapVO.setT_itemid(itemVO.getPrimaryKey());
											mapVO.setS_itemid(item.getPrimaryKey());
											pcmList.add(mapVO);
										}
									}
								} else {
									Collection<PayBillItemVO> itemCollections = MDPersistenceService.lookupPersistenceQueryService().queryBillOfVOByCond(ArapBillTypeInfo.getInstance("F3").getItemvoClass(), SqlUtils.getInStr("src_billid", new String[] { srcBillid }) + " and dr = 0", false);

									if (itemCollections != null) {
										for (PayBillItemVO item : itemCollections) {
											ArapBillMapVO mapVO = new ArapBillMapVO();
											mapVO.setT_itemid(itemVO.getPrimaryKey());
											mapVO.setS_itemid(item.getPrimaryKey());
											pcmList.add(mapVO);
										}
									}
								}
							}
						}
					}
					if ((src_syscode.intValue() == BillEnumCollection.FromSystem.AR.VALUE.intValue()) || (src_syscode.intValue() == BillEnumCollection.FromSystem.AP.VALUE.intValue())) {
						pks.add(vo.getParentVO().getPrimaryKey());
						if ((src_syscode.intValue() == BillEnumCollection.FromSystem.AR.VALUE.intValue()) && (vo.getParentVO().getAttributeValue("pk_billtype").equals("F2"))) {
							for (BaseItemVO itemVO : childrenVO) {
								ArapBillMapVO mapVO = ArapBillMapVOTool.changeVotoBillMapNewForSo((BaseBillVO) vo.getParentVO(), itemVO);
								defF2List.add(mapVO);
							}
						}
					}
				}
				try {
					if ((scmList != null) && (!scmList.isEmpty())) {
						String classname_so = "";
						String classname_pu = "";
						String classname_ps = "";

						if (((ArapBillMapVO) scmList.get(0)).getS_billtype() == null) {
							VerifyByContractNo(scmList, com, ruleVOMap);
							return;
						}
						if (((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("21")) {
							classname_pu = "nc.pubimpl.pu.m25.arap.f3.InvoiceQueryForVerifyImpl";
						} else if ((((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("36D7")) || (((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("36D1"))) {
							if (CmpQueryModulesUtil.isPUEnable(InvocationInfoProxy.getInstance().getGroupId())) {
								classname_ps = "nc.pubimpl.pu.m25.arap.f3.InvoiceQueryForVerifyImpl";
							}
						} else if ((((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("25")) || (((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("45"))) {
							classname_pu = "nc.pubimpl.pu.m21.arap.mf1.OrderQueryForVerifyImpl";
						} else if ((((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("30")) || (((ArapBillMapVO) scmList.get(0)).getS_billtype().equals("32"))) {
							classname_so = "nc.pubimpl.so.sobalance.arap.verify.SoBalance4VerifyQryBillImpl";
						}
						IArap4VerifyQryBill qryClass = null;

						List<ArapBillMapVO> newList = new ArrayList();
						newList.addAll(scmList);
						List<ArapBillMapVO> puList1 = new ArrayList();
						for (ArapBillMapVO vo : scmList) {
							if (!StringUtils.isEmpty(vo.getS_itemid())) {
								puList1.add(vo);
							}
						}
						if (!classname_pu.equals("")) {
							qryClass = (IArap4VerifyQryBill) ObjectCreator.newInstance("pu", classname_pu);
							com.setReserveFlag(VerifyDetailVO.reserve_pu);
						} else if (!classname_so.equals("")) {
							qryClass = (IArap4VerifyQryBill) ObjectCreator.newInstance("so", classname_so);
							com.setReserveFlag(VerifyDetailVO.reserve_so);
						} else if (!classname_ps.equals("")) {
							try {
								qryClass = (IArap4VerifyQryBill) ObjectCreator.newInstance("pu", classname_ps);
								com.setReserveFlag(VerifyDetailVO.reserve_pc);
							} catch (Throwable ex) {
								ExceptionHandler.consume(ex);
							}
						}

						Map<ArapBillMapVO, Collection<ArapBillMapVO>> arapBillMap = null;

						if ((puList1 != null) && (puList1.size() > 0) && (qryClass != null)) {
							arapBillMap = qryClass.queryArapBillmap((ArapBillMapVO[]) scmList.toArray(new ArapBillMapVO[0]));
						}

						printMapForDebug(arapBillMap);

						Collection<Collection<ArapBillMapVO>> mapCollections = null;
						if ((arapBillMap != null) && (arapBillMap.size() > 0)) {
							arapBillMap = ArapBillMapVOTool.combineSameSourceBillMap(arapBillMap);

							for (Iterator i$ = arapBillMap.keySet().iterator(); i$.hasNext();) {
								ArapBillMapVO vo = (ArapBillMapVO) i$.next();
								Collection<ArapBillMapVO> collection = (Collection) arapBillMap.get(vo);
								for (ArapBillMapVO vo1 : collection) {
									vo1.setT_billid(vo.getT_billid());
									vo1.setT_billtype(vo.getT_billtype());
									vo1.setT_itemid(vo.getT_itemid());
									vo1.setPk_currtype(vo.getPk_currtype());
								}
							}
							mapCollections = arapBillMap.values();
							for (Collection<ArapBillMapVO> collection : mapCollections) {
								doVerifyWithMap(collection, ruleVOMap, com, null);
							}
						}

						VerifyByContractNo(newList, com, ruleVOMap);
					}

					if (defF2List.size() > 0) {
						IArap4VerifyQryBill qryClass = null;
						String classname_so = "nc.pubimpl.so.sobalance.arap.verify.SoBalance4VerifyQryBillImpl";

						Map<ArapBillMapVO, Collection<ArapBillMapVO>> arapBillMap = null;

						if (CmpQueryModulesUtil.isSOEnable(InvocationInfoProxy.getInstance().getGroupId())) {
							try {
								qryClass = (IArap4VerifyQryBill) ObjectCreator.newInstance("so", classname_so);
								com.setReserveFlag(VerifyDetailVO.reserve_so);
							} catch (Exception e1) {
								Logger.error(e1.getMessage(), e1);
							}
							arapBillMap = qryClass.queryArapBillmap((ArapBillMapVO[]) defF2List.toArray(new ArapBillMapVO[0]));
						}

						Collection<Collection<ArapBillMapVO>> mapCollections = null;
						if ((arapBillMap != null) && (arapBillMap.size() > 0)) {
							arapBillMap = ArapBillMapVOTool.combineSameSourceBillMap(arapBillMap);

							for (Iterator i$ = arapBillMap.keySet().iterator(); i$.hasNext();) {
								ArapBillMapVO vo = (ArapBillMapVO) i$.next();
								Collection<ArapBillMapVO> collection = (Collection) arapBillMap.get(vo);
								for (ArapBillMapVO vo1 : collection) {
									vo1.setT_billid(vo.getT_billid());
									vo1.setT_billtype(vo.getT_billtype());
									vo1.setT_itemid(vo.getT_itemid());
									vo1.setPk_currtype(vo.getPk_currtype());
								}
							}
							mapCollections = arapBillMap.values();
							for (Collection<ArapBillMapVO> collection : mapCollections) {
								doVerifyWithMap(collection, ruleVOMap, com, null);
							}
						}
					}
					if ((pks != null) && (pks.size() > 0)) {
						com.setReserveFlag(VerifyDetailVO.reserve_arap);
						com.setExactVerify(true);
						for (DefaultVerifyRuleVO rule : ruleVOMap.values()) {
							rule.setM_creditObjKeys(new String[] { SPECIAL_KEY });
							rule.setM_debtObjKeys(new String[] { SPECIAL_KEY });
						}

						Collection<ArapBillMapVO> billMapData = new BaseDAO().retrieveByClause(ArapBillMapVO.class, SqlUtils.getInStr("t_billid", (String[]) pks.toArray(new String[0])));
						doArapVerifyWithMap(billMapData, ruleVOMap, com);
						updateBillMapYbye(billMapData, true);
					}

					if ((pcmList != null) && (pcmList.size() > 0)) {
						com.setReserveFlag(VerifyDetailVO.reserve_pcm);
						doVerifyWithMap(pcmList, ruleVOMap, com);
					}

					if ((psList != null) && (!psList.isEmpty())) {
						for (String key : ruleVOMap.keySet()) {
							DefaultVerifyRuleVO ruleVO = (DefaultVerifyRuleVO) ruleVOMap.get(key);
							ruleVO.setIsFkpc(new Boolean(true));
						}
						com.setReserveFlag(VerifyDetailVO.reserve_pc);
						doVerifyWithMap(psList, ruleVOMap, com, null);
					}
				} catch (Exception e1) {
					throw ExceptionHandler.handleException(e1);
				}

			}
		} else if ("200605".equals(event.getEventType())) {
			uneffectBill(event);

		} else if ("1002".equals(event.getEventType())) {
			BusinessEvent e = (BusinessEvent) event;
			if (null != e.getUserObject()) {
				AggregatedValueObject[] vos = dealUserObj(e.getUserObject());

				UFBoolean isinit = (UFBoolean) vos[0].getParentVO().getAttributeValue("isinit");
				if ((isinit != null) && (isinit.booleanValue())) {
					Object[] transform = transform(vos);
					List<ArapBusiDataVO> insertdatas = (List) transform[0];
					List<ArapBusiDataVO> updatedatas = (List) transform[1];
					if ((insertdatas != null) && (insertdatas.size() > 0)) {
						setAreaPKForBusiData(insertdatas);
					}
					doInsertRegister(insertdatas);
					if ((updatedatas != null) && (updatedatas.size() > 0)) {
						setAreaPKForBusiData(updatedatas);
					}
					doInsertRegister(updatedatas);
				}

			}

		} else if ("1005".equals(event.getEventType())) {
			BusinessEvent e = (BusinessEvent) event;
			if (null != e.getUserObject()) {
				AggregatedValueObject[] vos = dealUserObj(e.getUserObject());
				List<String> deldata = new ArrayList();
				for (AggregatedValueObject vo : vos) {
					deldata.add(vo.getParentVO().getPrimaryKey());
				}
				doCheckBillDeal(deldata);
				doDeleteRegisterByBillid((String[]) deldata.toArray(new String[0]));
			}
		}

		if ("1004".equals(event.getEventType())) {
			BdUpdateEvent e = (BdUpdateEvent) event;
			if (null != e.getNewObject()) {
				AggregatedValueObject[] vos = dealUserObjForInit(e.getNewObject());

				UFBoolean isinit = (UFBoolean) vos[0].getParentVO().getAttributeValue("isinit");
				String pk_bill = vos[0].getParentVO().getPrimaryKey();
				if ((isinit != null) && (isinit.booleanValue())) {
					new BaseDAO().deleteByClause(ArapBusiDataVO.class, " pk_bill ='" + pk_bill + "'");
					Object[] transform = transform(vos);
					List<ArapBusiDataVO> updatedatas = (List) transform[1];

					setAreaPKForBusiData(updatedatas);
					if (updatedatas != null) {
						doInsertRegister(updatedatas);
					}
				}
			}
		}

		LogTime.debug(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0258"), t1);
	}

	private void printMapForDebug(Map<ArapBillMapVO, Collection<ArapBillMapVO>> arapBillMap) {
		try {
			if (arapBillMap != null) {
				for (ArapBillMapVO vo : arapBillMap.keySet()) {
					Log.getInstance(LogTime.class).debug("ARAPBILLMAP");
					Log.getInstance(LogTime.class).debug(vo.getYbje());
					Log.getInstance(LogTime.class).debug(vo.getYbye());
					Log.getInstance(LogTime.class).debug(vo.getS_itemid());
					Log.getInstance(LogTime.class).debug(vo.getT_itemid());
					for (ArapBillMapVO svo : arapBillMap.get(vo)) {
						Log.getInstance(LogTime.class).debug(svo.getYbje());
						Log.getInstance(LogTime.class).debug(svo.getYbye());
						Log.getInstance(LogTime.class).debug(svo.getS_itemid());
						Log.getInstance(LogTime.class).debug(svo.getT_itemid());
					}
					Log.getInstance(LogTime.class).debug("ENDDDDD");
				}
			}
		} catch (Exception e) {
		}
	}

	private void uneffectBill(IBusinessEvent event) throws BusinessException {
		BusinessEvent e = (BusinessEvent) event;
		if (null != e.getUserObject()) {
			AggregatedValueObject[] vos = dealUserObj(e.getUserObject());
			List<String> deldata = new ArrayList();
			List<String> pks = new ArrayList();
			List<ArapBillMapVO> puList = new ArrayList();
			List<ArapBillMapVO> soList = new ArrayList();
			List<ArapBillMapVO> htList = new ArrayList();
			List<ArapBillMapVO> psList = new ArrayList();
			List<ArapBillMapVO> pcmList = new ArrayList();
			List<String> bodypks = new ArrayList();
			List<ArapBillMapVO> F2ForSoList = new ArrayList();
			String billclass = ((BaseAggVO) vos[0]).getHeadVO().getBillclass();
			boolean ifF1Uneffect = (billclass != null) && (billclass.equals("yf"));
			for (AggregatedValueObject vo : vos) {
				Integer src_syscode = (Integer) vo.getParentVO().getAttributeValue("src_syscode");
				BaseItemVO[] childrenVO = (BaseItemVO[]) vo.getChildrenVO();
				if ((src_syscode.intValue() == BillEnumCollection.FromSystem.AR.VALUE.intValue()) || (src_syscode.intValue() == BillEnumCollection.FromSystem.AP.VALUE.intValue())) {
					pks.add(vo.getParentVO().getPrimaryKey());
					if ((src_syscode.intValue() == BillEnumCollection.FromSystem.AR.VALUE.intValue()) && (vo.getParentVO().getAttributeValue("pk_billtype").equals("F2"))) {
						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = new ArapBillMapVO();
							mapVO.setT_itemid(itemVO.getPrimaryKey());
							if ("F2".equals(itemVO.getAttributeValue("top_billtype"))) {
								bodypks.add(itemVO.getPrimaryKey());
							} else {
								F2ForSoList.add(mapVO);
							}
						}
					}
					if ((src_syscode.intValue() == BillEnumCollection.FromSystem.AP.VALUE.intValue()) && (vo.getParentVO().getAttributeValue("pk_billtype").equals("F3"))) {
						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = new ArapBillMapVO();
							mapVO.setT_itemid(itemVO.getPrimaryKey());
							if ("F3".equals(itemVO.getAttributeValue("top_billtype"))) {
								bodypks.add(itemVO.getPrimaryKey());
							}
						}
					}
				} else {
					String pk_billtype = (String) vo.getParentVO().getAttributeValue("pk_billtype");
					if ((pk_billtype.equals("F0")) || (pk_billtype.equals("F2"))) {
						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = new ArapBillMapVO();
							mapVO.setT_itemid(itemVO.getPrimaryKey());

							if ("Z3".equals(itemVO.getTop_billtype())) {
								htList.add(mapVO);
							} else {
								soList.add(mapVO);
							}
						}
					} else if ((pk_billtype.equals("F1")) || (pk_billtype.equals("F3"))) {
						for (BaseItemVO itemVO : childrenVO) {
							ArapBillMapVO mapVO = new ArapBillMapVO();
							mapVO.setT_itemid(itemVO.getPrimaryKey());

							if (("36D1".equals(childrenVO[0].getTop_billtype())) || ("36D7".equals(childrenVO[0].getTop_billtype()))) {
								psList.add(mapVO);
							} else if ((itemVO.getTop_billtype() != null) && (("4D46".equals(childrenVO[0].getTop_billtype())) || ("4D50".equals(childrenVO[0].getTop_billtype())) || ("4D48".equals(childrenVO[0].getTop_billtype())))) {
								pcmList.add(mapVO);
							} else {
								puList.add(mapVO);
							}
						}
					}
				}
				deldata.add(vo.getParentVO().getPrimaryKey());
			}
			doCheckBillDeal(deldata);
			try {
				boolean isfkpc = false;
				if (ifF1Uneffect) {
					isfkpc = true;
				}
				if ((pks != null) && (pks.size() > 0)) {
					Collection<ArapBillMapVO> billMapData = new BaseDAO().retrieveByClause(ArapBillMapVO.class, SqlUtils.getInStr("t_billid", (String[]) pks.toArray(new String[0])));
					doUnVerifyWithMap(billMapData, Boolean.valueOf(isfkpc));
					updateBillMapYbye(billMapData, false);

					if (((billMapData == null) || (billMapData.size() == 0)) && (F2ForSoList != null) && (!F2ForSoList.isEmpty())) {
						doUnVerifyWithMap(F2ForSoList, Boolean.valueOf(isfkpc));
					}
				}

				if ((pcmList != null) && (!pcmList.isEmpty())) {
					doUnVerifyWithMap(pcmList, Boolean.valueOf(isfkpc));
				}

				if ((psList != null) && (!psList.isEmpty())) {
					doUnVerifyWithMap(psList, Boolean.TRUE);
				}
				if ((soList != null) && (!soList.isEmpty())) {
					doUnVerifyWithMap(soList, Boolean.valueOf(isfkpc));
				}
				if ((htList != null) && (!htList.isEmpty())) {
					doUnVerifyWithMap(htList, Boolean.valueOf(isfkpc));
				}
				if ((puList != null) && (!puList.isEmpty())) {
					doUnVerifyWithMap(puList, Boolean.valueOf(isfkpc));
				}

				if (bodypks.size() > 0) {
					Collection<ArapBillMapVO> billMapData = new HashSet();
					for (String pk : bodypks) {
						ArapBillMapVO vo = new ArapBillMapVO();
						vo.setT_itemid(pk);
						billMapData.add(vo);
					}
					doUnVerifyWithMap(billMapData, Boolean.valueOf(isfkpc));
				}

				doDeleteRegisterByBillid((String[]) deldata.toArray(new String[0]));
			} catch (Exception e1) {
				throw ExceptionHandler.handleException(e1);
			}
		}
	}

	private void billRBVerify(AggregatedValueObject[] vos, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap, VerifyCom com) throws BusinessException, MetaDataException {
		for (AggregatedValueObject vo : vos) {
			BaseBillVO parentVO = (BaseBillVO) vo.getParentVO();
			String primaryKey = parentVO.getPrimaryKey();
			BaseItemVO[] itemVOs = (BaseItemVO[]) vo.getChildrenVO();

			this.aggMap.clear();

			String topBilltype = ((BaseItemVO) vo.getChildrenVO()[0]).getTop_billtype();

			if ((topBilltype != null) && (((topBilltype.equals("32")) && (parentVO.getPk_billtype().equals("F0"))) || ((topBilltype.equals("25")) && (parentVO.getPk_billtype().equals("F1"))) || ((topBilltype.equals("30")) && (parentVO.getPk_billtype().equals("F2"))))) {

				String whereCondStr = "";

				whereCondStr = " pk_bill ='" + primaryKey + "'";

				NCObject[] objectByCond = MDPersistenceService.lookupPersistenceQueryService().queryBillOfNCObjectByCond(ArapBusiDataVO.class, whereCondStr, false);

				List<ArapBusiDataVO> jfList = new ArrayList();
				List<ArapBusiDataVO> dfList = new ArrayList();

				for (NCObject data : objectByCond) {
					ArapBusiDataVO busiDataVO = (ArapBusiDataVO) data.getContainmentObject();

					if ((busiDataVO.getPk_billtype().equals("F2")) && ("30".equals(busiDataVO.getTop_billtype()))) {
						busiDataVO.setAttributeValue("SETT_MONEY", busiDataVO.getMoney_bal().sub(busiDataVO.getOccupationmny()));
					} else {
						busiDataVO.setAttributeValue("SETT_MONEY", busiDataVO.getOccupationmny());
					}
					if (busiDataVO.getDirection().intValue() == 1) {
						jfList.add(busiDataVO);
					} else {
						dfList.add(busiDataVO);
					}
				}
				this.aggMap = onVerify(com, ruleVOMap, (ArapBusiDataVO[]) jfList.toArray(new ArapBusiDataVO[0]), (ArapBusiDataVO[]) dfList.toArray(new ArapBusiDataVO[0]));
				ArrayList<AggverifyVO> aggVOList = getAggVerifyVO(this.aggMap);

				if ((aggVOList != null) && (aggVOList.size() > 0)) {
					((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);

					reQueryBillIteminfo(vo);
				}

				this.aggMap.clear();
			}
		}
	}

	private void reQueryBillIteminfo(AggregatedValueObject vo) throws BusinessException {
		CircularlyAccessibleValueObject[] childrenVO = vo.getChildrenVO();
		List<String> pks = new ArrayList();
		for (CircularlyAccessibleValueObject s : childrenVO) {
			pks.add(s.getPrimaryKey());
		}

		if (vo.getParentVO().getAttributeValue("pk_billtype").equals("F0")) {
			IArapReceivableBillPubQueryService service = (IArapReceivableBillPubQueryService) NCLocator.getInstance().lookup(IArapReceivableBillPubQueryService.class);
			try {
				ReceivableBillItemVO[] items = service.queryRecieableBillItem((String[]) pks.toArray(new String[pks.size()]));
				vo.setChildrenVO(items);
			} catch (BusinessException e) {
				ExceptionHandler.handleException(e);
			}
		} else if (vo.getParentVO().getAttributeValue("pk_billtype").equals("F1")) {
			try {
				Collection<PayableBillItemVO> bills = MDPersistenceService.lookupPersistenceQueryService().queryBillOfVOByPKs(PayableBillItemVO.class, (String[]) pks.toArray(new String[pks.size()]), false);
				PayableBillItemVO[] items = (PayableBillItemVO[]) bills.toArray(new PayableBillItemVO[0]);
				vo.setChildrenVO(items);
			} catch (BusinessException e) {
				ExceptionHandler.handleException(e);
			}
		}
	}

	protected AggregatedValueObject[] dealUserObjForInit(Object obj) {
		AggregatedValueObject[] retvos = null;
		if (obj.getClass().isArray()) {
			retvos = (AggregatedValueObject[]) obj;
		} else {
			retvos = new AggregatedValueObject[] { (AggregatedValueObject) obj };
		}
		this.pk_user = ((String) retvos[0].getParentVO().getAttributeValue("approver"));
		this.syscode = ((Integer) retvos[0].getParentVO().getAttributeValue("syscode"));
		this.pkorg = ((String) retvos[0].getParentVO().getAttributeValue("pk_org"));
		return retvos;
	}

	private void VerifyByContractNo(AggregatedValueObject[] vos, VerifyCom com, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap) throws BusinessException {
		List<ArapBusiDataVO> dataList = new ArrayList();
		List<ArapBusiDataVO> jfdataList = new ArrayList();
		List<ArapBusiDataVO> dfdataList = new ArrayList();
		ArrayList<AggverifyVO> aggVOList = new ArrayList();
		for (AggregatedValueObject aggvo : vos) {
			BaseItemVO[] childrenVO = (BaseItemVO[]) aggvo.getChildrenVO();

			for (BaseItemVO childvo : childrenVO) {
				this.aggMap.clear();
				String pk_item = childvo.getPrimaryKey();
				Object contractno = childvo.getAttributeValue("contractno");
				String currtype = (String) childvo.getAttributeValue("pk_currtype");
				String pk_billtype = (String) childvo.getAttributeValue("pk_billtype");
				String pk_org = (String) childvo.getAttributeValue("pk_org");

				if (contractno != null) {

					String qrySql = " pk_item ='" + pk_item + "' or (pk_org ='" + pk_org + "'" + " and pausetransact ='N' " + " and pk_currtype ='" + currtype + "'" + "and pk_billtype !='" + pk_billtype + "' and contractno ='" + contractno + "' and pk_item <> '" + pk_item + "' and money_bal<> 0)";

					NCObject[] ncObjects = MDPersistenceService.lookupPersistenceQueryService().queryBillOfNCObjectByCond(ArapBusiDataVO.class, qrySql, false);
					if (!ArrayUtils.isEmpty(ncObjects)) {
						for (NCObject obj : ncObjects) {
							ArapBusiDataVO dataVO = (ArapBusiDataVO) obj.getContainmentObject();
							dataList.add(dataVO);
						}

						for (ArapBusiDataVO datavo : dataList) {
							if ((datavo.getPk_billtype().equals("F2")) && ("30".equals(datavo.getTop_billtype()))) {
								datavo.setAttributeValue("SETT_MONEY", datavo.getMoney_bal().sub(datavo.getOccupationmny()));
							} else {
								datavo.setAttributeValue("SETT_MONEY", datavo.getOccupationmny());
							}
							if (datavo.getDirection().intValue() == 1) {
								jfdataList.add(datavo);
							} else {
								dfdataList.add(datavo);
							}
						}
						Map<String, ArrayList<AggverifyVO>> aggMap = onVerify(com, ruleVOMap, (ArapBusiDataVO[]) jfdataList.toArray(new ArapBusiDataVO[0]), (ArapBusiDataVO[]) dfdataList.toArray(new ArapBusiDataVO[0]));

						aggVOList = getAggVerifyVO(aggMap);
						if ((aggVOList != null) && (aggVOList.size() > 0))
							((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);
					}
				}
			}
		}
	}

	private void VerifyByContractNo(List<ArapBillMapVO> MapVOList, VerifyCom com, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap) throws BusinessException {
		com.setReserveFlag(VerifyDetailVO.reserve_ht);
		for (ArapBillMapVO vo : MapVOList) {
			Map<String, String> busiDataTsMap = new HashMap();
			List<ArapBusiDataVO> dataList = new ArrayList();
			List<ArapBusiDataVO> jfdataList = new ArrayList();
			List<ArapBusiDataVO> dfdataList = new ArrayList();
			this.aggMap.clear();
			String currtype = vo.getPk_currtype();
			String pk_item = vo.getT_itemid();
			String contractno = vo.getContractno();
			String pk_org = vo.getPk_org();
			if (contractno != null) {

				String billType = "";
				if ("F0".equals(vo.getT_billtype())) {
					billType = "F2";
				} else if ("F1".equals(vo.getT_billtype())) {
					billType = "F3";
				} else if ("F2".equals(vo.getT_billtype())) {
					billType = "F0";
				} else if ("F3".equals(vo.getT_billtype())) {
					billType = "F1";
				}

				String qrySql = " (money_bal <> 0 and pk_item ='" + pk_item + "') or (pk_org ='" + pk_org + "'" + " and pausetransact ='N' " + " and pk_currtype ='" + currtype + "'" + "and pk_billtype ='" + billType + "' and contractno ='" + contractno + "' and pk_item <> '" + pk_item + "' and money_bal<> 0)";

				NCObject[] ncObjects = MDPersistenceService.lookupPersistenceQueryService().queryBillOfNCObjectByCond(ArapBusiDataVO.class, qrySql, false);
				if (!ArrayUtils.isEmpty(ncObjects)) {
					for (NCObject obj : ncObjects) {
						ArapBusiDataVO dataVO = (ArapBusiDataVO) obj.getContainmentObject();
						dataList.add(dataVO);

						busiDataTsMap.put(dataVO.getPk_busidata(), dataVO.getTs().toString());
						com.setBusiDataTsMap(busiDataTsMap);
					}

					for (ArapBusiDataVO datavo : dataList) {
						if ((datavo.getPk_billtype().equals("F2")) && ("30".equals(datavo.getTop_billtype()))) {
							datavo.setAttributeValue("SETT_MONEY", datavo.getMoney_bal().sub(datavo.getOccupationmny()));
						} else {
							datavo.setAttributeValue("SETT_MONEY", datavo.getOccupationmny());
						}
						if (datavo.getDirection().intValue() == 1) {
							jfdataList.add(datavo);
						} else {
							dfdataList.add(datavo);
						}
					}
					Map<String, ArrayList<AggverifyVO>> aggMap = onVerify(com, ruleVOMap, (ArapBusiDataVO[]) jfdataList.toArray(new ArapBusiDataVO[0]), (ArapBusiDataVO[]) dfdataList.toArray(new ArapBusiDataVO[0]));
					ArrayList<AggverifyVO> aggVOList = getAggVerifyVO(aggMap);

					if ((aggVOList != null) && (aggVOList.size() > 0)) {
						((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);
					}
				}
			}
		}
	}

	private void updateBillMapYbye(Collection<ArapBillMapVO> billMapData, boolean setZero) throws DAOException {
		String[] fild = { "ybye" };
		if ((billMapData != null) && (billMapData.size() > 0)) {
			if (setZero) {
				for (ArapBillMapVO vo : billMapData) {
					vo.setYbye(UFDouble.ZERO_DBL);
				}
			} else {
				for (ArapBillMapVO vo : billMapData) {
					vo.setYbye(vo.getYbje());
				}
			}

			billMapData.toArray(new SuperVO[0]);
			new BaseDAO().updateVOArray((SuperVO[]) billMapData.toArray(new SuperVO[0]), fild);
		}
	}

	public void doCheckBillDeal(List<String> pkbillList) throws BusinessException {
		String[] pkbillArr = (String[]) pkbillList.toArray(new String[0]);
		String insql = "";
		BaseDAO dao = new BaseDAO();
		insql = SqlUtils.getInStr("pk_bill", pkbillArr);
		String[] fields = { "pk_bill", "billno" };

		Collection<VerifyDetailVO> verifydata = dao.retrieveByClause(VerifyDetailVO.class, insql + " and isauto='N'", fields);
		StringBuffer billStr = new StringBuffer();
		Set<String> billSet = new HashSet();
		if ((verifydata != null) && (verifydata.size() > 0)) {
			for (VerifyDetailVO vo : verifydata) {
				billSet.add(vo.getBillno());
			}
			for (String billno : billSet) {
				billStr.append(billno);
				billStr.append(",");
			}
			int lastIndexOf = billStr.lastIndexOf(",");
			String substring = billStr.substring(0, lastIndexOf);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0251") + substring + NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0259"));
		}

		Collection<AgiotageChildVO> agiotagedata = dao.retrieveByClause(AgiotageChildVO.class, insql + "and not exists (select 1 from ARAP_VERIFYDETAIL where " + insql + " )", fields);
		if ((agiotagedata != null) && (agiotagedata.size() > 0)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0260"));
		}

		Collection<DebtTransferVO> debtdata = dao.retrieveByClause(DebtTransferVO.class, insql, fields);
		if ((debtdata != null) && (debtdata.size() > 0)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0261"));
		}

		Collection<nc.vo.arap.baddebts.BaddebtsOcchVO> badLosedata = dao.retrieveByClause(nc.vo.arap.baddebts.BaddebtsOccuVO.class, insql, fields);
		if ((badLosedata != null) && (badLosedata.size() > 0)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0262"));
		}

		Collection<BaddebtsRechVO> badBackdata = dao.retrieveByClause(BaddebtsReceVO.class, insql, fields);
		if ((badBackdata != null) && (badBackdata.size() > 0)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0263"));
		}
	}

	protected Object[] transform(AggregatedValueObject[] vos) throws BusinessException {
		List<SuperVO> insertvos = new ArrayList();
		List<SuperVO> updatevos = new ArrayList();
		List<String> delvos = new ArrayList();
		List<String> billPKList = new ArrayList();

		Object[] ret = { insertvos, updatevos, delvos };

		for (AggregatedValueObject aggvo : vos) {
			billPKList.add(aggvo.getParentVO().getPrimaryKey());
		}
		TermVO[] termVOs = ((IArapTermItemPubQueryService) NCLocator.getInstance().lookup(IArapTermItemPubQueryService.class)).queryTermVOByBillParentPK((String[]) billPKList.toArray(new String[0]));
		this.termVOMap = getTermVOByItemPK(termVOs);
		for (AggregatedValueObject vo : vos) {
			SuperVO[] items = (SuperVO[]) vo.getChildrenVO();
			for (SuperVO item : items) {
				switch (item.getStatus()) {
					case 2:
						List<ArapBusiDataVO> transform = transform(item, (SuperVO) vo.getParentVO());
						insertvos.addAll(transform);
						break;
					case 3:
						delvos.add(item.getPrimaryKey());
					default:
						List<ArapBusiDataVO> busidataList = transform(item, (SuperVO) vo.getParentVO());
						updatevos.addAll(busidataList);
				}

			}
		}

		ArapMaterialUtils.translateMaterial2NonVersion((CircularlyAccessibleValueObject[]) insertvos.toArray(new ArapBusiDataVO[0]), "material", "material_v");
		ArapMaterialUtils.translateMaterial2NonVersion((CircularlyAccessibleValueObject[]) updatevos.toArray(new ArapBusiDataVO[0]), "material", "material_v");

		return ret;
	}

	private Map<String, List<TermVO>> getTermVOByItemPK(TermVO[] termVOs) {
		Map<String, List<TermVO>> termMap = new HashMap();
		for (TermVO vo : termVOs) {
			if (termMap.containsKey(vo.getPk_item())) {
				((List) termMap.get(vo.getPk_item())).add(vo);
			} else {
				List<TermVO> temp = new ArrayList();
				temp.add(vo);
				termMap.put(vo.getPk_item(), temp);
			}
		}
		return termMap;
	}

	protected List<ArapBusiDataVO> transform(SuperVO item, SuperVO head) throws BusinessException {
		ArapBusiDataVO retvo = new ArapBusiDataVO();
		String[] attrs = retvo.getAttributeNames();
		for (String attr : attrs) {
			Object attributeValue = item.getAttributeValue(attr);
			if (attributeValue == null) {
				attributeValue = head.getAttributeValue(attr);
			}
			retvo.setAttributeValue(attr, attributeValue);

			if (attr.equals("pk_costsubj")) {
				retvo.setAttributeValue("pk_costsubj", item.getAttributeValue("pk_subjcode"));
			}

			if (attr.equals("price")) {
				retvo.setAttributeValue("price", item.getAttributeValue("taxprice"));
			}

			if (attr.equals("pk_pcorg")) {
				retvo.setAttributeValue("pk_pcorg", item.getAttributeValue("pk_pcorg"));
			}

			if ((attributeValue == null) && ((attr.equals("global_money_bal")) || (attr.equals("group_money_bal")) || (attr.equals("global_money_de")) || (attr.equals("global_money_cr")) || (attr.equals("group_money_de")) || (attr.equals("group_money_cr")))) {
				retvo.setAttributeValue(attr, UFDouble.ZERO_DBL);
			}
		}

		retvo.setPk_bill(head.getPrimaryKey());
		retvo.setPk_item(item.getPrimaryKey());

		retvo.setSum_headmoney((UFDouble) head.getAttributeValue("money"));

		String billcalss = (String) item.getAttributeValue("billclass");
		if ((billcalss.equals("ys")) || (billcalss.equals("fk"))) {
			retvo.setSum_itemmoney((UFDouble) item.getAttributeValue("money_de"));
		} else {
			retvo.setSum_itemmoney((UFDouble) item.getAttributeValue("money_cr"));
		}

		retvo.setBillstatus(Integer.valueOf(1));

		retvo.setHeadsubjcode((String) head.getAttributeValue("subjcode"));

		retvo.setPk_busitype((String) head.getAttributeValue("pk_busitype"));
		retvo.setOperator((String) head.getAttributeValue("billmaker"));

		retvo.setBillno((String) head.getAttributeValue("billno"));

		retvo.setEstflag(head.getAttributeValue("estflag") == null ? Integer.valueOf(0) : (Integer) head.getAttributeValue("estflag"));

		List<ArapBusiDataVO> busiDataList = queryTermVOByBillParentPK(retvo);

		return busiDataList;
	}

	private void setAreaPKForBusiData(List<ArapBusiDataVO> busiDataList) {
		if (((ArapBusiDataVO) busiDataList.get(0)).getObjtype().intValue() == 0) {
			String[] custPKS = getCustOrSuppPKS(busiDataList, true);
			try {
				CustomerVO[] customerVOs = ((ICustomerPubService) NCLocator.getInstance().lookup(ICustomerPubService.class)).getCustomerVO(custPKS, new String[] { "pk_areacl", "pk_customer" });
				Map custMap = new HashMap();
				if (!ArrayUtils.isEmpty(customerVOs)) {
					for (CustomerVO cust : customerVOs) {
						if (cust != null) {
							custMap.put(cust.getPk_customer(), cust);
						}
					}
				}

				for (ArapBusiDataVO data : busiDataList) {
					if (custMap.get(data.getCustomer()) != null) {
						data.setPk_areacl(((CustomerVO) custMap.get(data.getCustomer())).getPk_areacl());
					}
				}
			} catch (BusinessException e) {
				Map<String, CustomerVO> custMap;
				Logger.error(e.getMessage(), e);
			}
		} else if (((ArapBusiDataVO) busiDataList.get(0)).getObjtype().intValue() == 1) {
			String[] suppPKS = getCustOrSuppPKS(busiDataList, false);
			try {
				SupplierVO[] supplierVOs = ((ISupplierPubService) NCLocator.getInstance().lookup(ISupplierPubService.class)).getSupplierVO(suppPKS, new String[] { "pk_areacl", "pk_supplier" });
				Map suppMap = new HashMap();
				if (!ArrayUtils.isEmpty(supplierVOs)) {
					for (SupplierVO cust : supplierVOs) {
						if (cust != null) {
							suppMap.put(cust.getPk_supplier(), cust);
						}
					}
				}

				for (ArapBusiDataVO data : busiDataList) {
					if (suppMap.get(data.getSupplier()) != null) {
						data.setPk_areacl(((SupplierVO) suppMap.get(data.getSupplier())).getPk_areacl());
					}
				}
			} catch (BusinessException e) {
				Map<String, SupplierVO> suppMap;
				Logger.error(e.getMessage(), e);
			}
		}
	}

	private String[] getCustOrSuppPKS(List<ArapBusiDataVO> vos, boolean iscust) {
		Set<String> pkS = new HashSet();
		for (ArapBusiDataVO vo : vos) {
			if (iscust) {
				pkS.add(vo.getCustomer());
			} else {
				pkS.add(vo.getSupplier());
			}
		}
		String[] pkArr = (String[]) pkS.toArray(new String[0]);
		return pkArr;
	}

	private List<ArapBusiDataVO> queryTermVOByBillParentPK(ArapBusiDataVO busidataVO) throws BusinessException {
		List<TermVO> termlist = (List) this.termVOMap.get(busidataVO.getPk_item());
		List<ArapBusiDataVO> busiDataList = new ArrayList();
		if ((termlist != null) && (termlist.size() > 0)) {
			for (TermVO termVO : termlist) {
				ArapBusiDataVO copyVO = (ArapBusiDataVO) busidataVO.clone();
				copyVO.setExpiredate(termVO.getExpiredate());

				copyVO.setInnerctldeferdays(termVO.getInnerctldeferdays());
				copyVO.setInsurance(termVO.getInsurance());
				copyVO.setPk_termitem(termVO.getPk_termitem());

				copyVO.setMoney_de(termVO.getMoney_de() == null ? UFDouble.ZERO_DBL : termVO.getMoney_de());

				copyVO.setLocal_money_de(termVO.getLocal_money_de() == null ? UFDouble.ZERO_DBL : termVO.getLocal_money_de());

				copyVO.setMoney_cr(termVO.getMoney_cr() == null ? UFDouble.ZERO_DBL : termVO.getMoney_cr());

				copyVO.setLocal_money_cr(termVO.getLocal_money_cr() == null ? UFDouble.ZERO_DBL : termVO.getLocal_money_cr());

				copyVO.setMoney_bal(termVO.getMoney_bal() == null ? UFDouble.ZERO_DBL : termVO.getMoney_bal());

				copyVO.setLocal_money_bal(termVO.getLocal_money_bal() == null ? UFDouble.ZERO_DBL : termVO.getLocal_money_bal());

				copyVO.setGroup_money_de(termVO.getGroupdebit() == null ? UFDouble.ZERO_DBL : termVO.getGroupdebit());

				copyVO.setGroup_money_cr(termVO.getGroupcredit() == null ? UFDouble.ZERO_DBL : termVO.getGroupcredit());

				copyVO.setGroup_money_bal(termVO.getGroupbalance() == null ? UFDouble.ZERO_DBL : termVO.getGroupbalance());

				copyVO.setGlobal_money_de(termVO.getGlobaldebit() == null ? UFDouble.ZERO_DBL : termVO.getGlobaldebit());

				copyVO.setGlobal_money_cr(termVO.getGlobalcredit() == null ? UFDouble.ZERO_DBL : termVO.getGlobalcredit());

				copyVO.setGlobal_money_bal(termVO.getGlobalbalance() == null ? UFDouble.ZERO_DBL : termVO.getGlobalbalance());

				copyVO.setQuantity_de(termVO.getQuantity_de() == null ? UFDouble.ZERO_DBL : termVO.getQuantity_de());

				copyVO.setQuantity_cr(termVO.getQuantity_cr() == null ? UFDouble.ZERO_DBL : termVO.getQuantity_cr());

				copyVO.setQuantity_bal(termVO.getQuantity_bal() == null ? UFDouble.ZERO_DBL : termVO.getQuantity_bal());

				copyVO.setOccupationmny(termVO.getOccupationmny());
				copyVO.setIstransin(UFBoolean.valueOf(false));
				copyVO.setPrepay(Integer.valueOf((termVO.getPrepay() != null) && (termVO.getPrepay().booleanValue()) ? 1 : 0));
				busiDataList.add(copyVO);
			}
		}
		return busiDataList;
	}

	protected void doInsertRegister(List<ArapBusiDataVO> insertdatas) throws BusinessException {
		Logger.debug("插入arap_busidata 表  数量 =" + insertdatas.size());
		new BaseDAO().insertVOList(insertdatas);
	}

	protected void doUpdateRegister(List<ArapBusiDataVO> updatedatas) throws BusinessException {
		new BaseDAO().updateVOList(updatedatas);
	}

	protected void doDeleteRegisterByBillid(String[] billids) throws BusinessException {
		if (!PKLock.getInstance().addBatchDynamicLock(billids)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0257"));
		}

		new BaseDAO().deleteByClause(ArapBusiDataVO.class, SqlUtils.getInStr("pk_bill", billids));
	}

	protected void doDeleteRegisterByItemid(String[] itemids) throws BusinessException {
		new BaseDAO().deleteByClause(ArapBusiDataVO.class, SqlUtils.getInStr("pk_item", itemids));
	}

	private void doUnVerifyWithMap(Collection<ArapBillMapVO> mapvos, Boolean isFkpc) throws BusinessException {
		if ((mapvos == null) || (mapvos.size() == 0))
			return;
		Map<String, String> businoMap = new HashMap();

		List<String> pks = new ArrayList();
		for (ArapBillMapVO vo : mapvos) {
			pks.add(vo.getT_itemid());
		}

		Collection<VerifyDetailVO> detailColet = new BaseDAO().retrieveByClause(VerifyDetailVO.class, " isauto='Y' and " + SqlUtils.getInStr("pk_item", (String[]) pks.toArray(new String[0])));
		for (VerifyDetailVO detailvo : detailColet) {
			if (businoMap.containsKey(detailvo.getBusino())) {
				businoMap.put(detailvo.getBusino(), businoMap.get(detailvo.getBusino()));
			} else {
				businoMap.put(detailvo.getBusino(), detailvo.getBusino());
			}
		}
		if (businoMap.size() == 0) {
			return;
		}
		Set<String> keyset = businoMap.keySet();
		keyset.toArray(new String[keyset.size()]);
		((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).onCancelVerify((String[]) keyset.toArray(new String[keyset.size()]), isFkpc);
	}

	private void doVerifyWithMap(Collection<ArapBillMapVO> mapvos, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap, VerifyCom com, String nouse) throws BusinessException {
		if ((mapvos == null) || (mapvos.size() == 0)) {
			return;
		}

		if (VerifyDetailVO.reserve_so.equals(com.getReserveFlag())) {
			UFBoolean isNeed = SysInit.getParaBoolean(getPkorg(), SysinitConst.AR35);
			if ((isNeed != null) && (isNeed.booleanValue())) {
				for (DefaultVerifyRuleVO rule : ruleVOMap.values()) {
					rule.setM_creditObjKeys(new String[] { "prepay" });
					rule.setM_debtObjKeys(new String[] { "prepay" });
				}
			}
		} else if (VerifyDetailVO.reserve_pu.equals(com.getReserveFlag())) {
			UFBoolean isNeed = SysInit.getParaBoolean(getPkorg(), SysinitConst.AP35);
			if ((isNeed != null) && (isNeed.booleanValue())) {
				for (DefaultVerifyRuleVO rule : ruleVOMap.values()) {
					rule.setM_creditObjKeys(new String[] { "prepay" });
					rule.setM_debtObjKeys(new String[] { "prepay" });
				}
			}
		}

		this.aggMap.clear();
		List<String> pks = new ArrayList();
		List<String> termPkList = new ArrayList();
		List<String> itemPkList = new ArrayList();
		Collection<ArapBusiDataVO> busiList = null;
		ArapBillMapVO[] mapVOs2 = (ArapBillMapVO[]) mapvos.toArray(new ArapBillMapVO[0]);

		for (ArapBillMapVO vo : mapvos) {
			if (("36D1".equals(vo.getS_billtype())) || ("36D7".equals(vo.getS_billtype()))) {
				termPkList.add(vo.getS_termid());
				itemPkList.add(vo.getT_itemid());
			} else if (vo.getS_itemid() != null) {
				pks.add(vo.getS_itemid());
				pks.add(vo.getT_itemid());
			}
		}

		if ((termPkList != null) && (termPkList.size() > 0)) {
			String sql = SqlUtils.getInStr("pk_termitem", (String[]) termPkList.toArray(new String[0])) + " and pausetransact ='N' " + " or " + SqlUtils.getInStr("pk_item", (String[]) itemPkList.toArray(new String[0])) + " and pausetransact ='N' ";
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, sql);
		} else if ((pks != null) && (pks.size() > 0)) {
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, SqlUtils.getInStr("pk_item", (String[]) pks.toArray(new String[0])) + " and pk_org='" + ((DefaultVerifyRuleVO) ruleVOMap.values().iterator().next()).getPk_org() + "' and pausetransact ='N' " + " and money_bal <> 0  " + " and pk_currtype ='" + mapVOs2[0].getPk_currtype() + "'");
		}

		if ((busiList == null) || (busiList.size() == 0)) {
			return;
		}

		Map<String, String> busiDataTsMap = new HashMap();
		for (ArapBusiDataVO vo : busiList) {
			busiDataTsMap.put(vo.getPk_busidata(), vo.getTs().toString());
		}
		com.setBusiDataTsMap(busiDataTsMap);
		List<ArapBusiDataVO> jfList = new ArrayList();
		List<ArapBusiDataVO> dfList = new ArrayList();
		// 20170301 tsy 修复付款排程多行时，表体单行VO的所属借贷方混乱
		List<String> itemidList = new ArrayList();
		// 保存借方所有的pk_item到List中
		for (ArapBillMapVO vo : mapVOs2) {
			itemidList.add(vo.getT_itemid());
		}
		for (ArapBusiDataVO data : busiList) {
			// if (data.getPk_item().equals(mapVOs2[0].getT_itemid())) {
			if (itemidList.contains(data.getPk_item())) {// 是否属于借方
				jfList.add(data);
			} else {
				dfList.add(data);
			}
		}
		// 20170301 end

		Integer flag = ((DefaultVerifyRuleVO) ruleVOMap.get("SAME_VERIFY")).getM_verifySeq();
		ArapBusiDataVO[] jfVos = (ArapBusiDataVO[]) jfList.toArray(new ArapBusiDataVO[0]);// src_itemid
		ArapBusiDataVO[] dfVos = (ArapBusiDataVO[]) dfList.toArray(new ArapBusiDataVO[0]);// pk_item
		VerifyTool.sortVector(jfVos, flag.intValue());
		VerifyTool.sortVector(dfVos, flag.intValue());
		Map<String, List<ArapBusiDataVO>> dfBusidataMap = getBusidataMap(dfVos);

		UFDouble clje = UFDouble.ZERO_DBL;// 处理金额吗？
		UFDouble jf_clje = UFDouble.ZERO_DBL;
		for (ArapBillMapVO vo : mapVOs2) {
			clje = clje.add(vo.getYbje());
		}
		jf_clje = clje;
		// 36D1:付款申请 36D7:计划执行
		if ((mapVOs2[0].getS_billtype().equals("36D1")) || (mapVOs2[0].getS_billtype().equals("36D7"))) {
			for (ArapBillMapVO vo : mapvos) {
				if ((vo.getS_billtype().equals("36D1")) || (vo.getS_billtype().equals("36D7"))) {

					UFDouble clje1 = vo.getYbje();// 原币金额
					if ((dfList != null) && (dfList.size() > 0)) {// 贷方
						for (ArapBusiDataVO datavo : dfVos) {
							UFDouble occupationmny = datavo.getOccupationmny();// 预占用核销原币余额
							// 20170302 tsy 修复核销数值错误
							// if (clje1.compareTo(occupationmny) > 0) {
							if (clje1.compareTo(occupationmny) > 0 || clje1.compareTo(occupationmny) < 0) {
								// 20170302 end
								datavo.setAttributeValue("SETT_MONEY", occupationmny);// 应付金额
								clje1 = clje.sub(occupationmny);
							} else {
								datavo.setAttributeValue("SETT_MONEY", clje1);
								clje1 = UFDouble.ZERO_DBL;
							}

						}

					}
				}
			}
		} else if ((dfList != null) && (dfList.size() > 0)) {
			for (String pk_item : dfBusidataMap.keySet()) {
				List<ArapBusiDataVO> list = (List) dfBusidataMap.get(pk_item);
				UFDouble verifyMoney = UFDouble.ZERO_DBL;
				for (ArapBillMapVO mapvo : mapvos) {
					if (pk_item.equals(mapvo.getS_itemid())) {
						verifyMoney = mapvo.getYbje();
						break;
					}
				}
				for (ArapBusiDataVO datavo : list) {
					UFDouble occupationmny = datavo.getOccupationmny();
					UFDouble money_bal = datavo.getMoney_bal();
					if (datavo.getPk_billtype().equals("F2")) {
						if (verifyMoney.compareTo(money_bal.sub(occupationmny)) > 0) {
							datavo.setAttributeValue("SETT_MONEY", money_bal.sub(occupationmny));
							verifyMoney = verifyMoney.sub(money_bal.sub(occupationmny));
						} else {
							datavo.setAttributeValue("SETT_MONEY", verifyMoney);
							verifyMoney = UFDouble.ZERO_DBL;
						}
					} else if (verifyMoney.compareTo(occupationmny) > 0) {
						datavo.setAttributeValue("SETT_MONEY", occupationmny);
						verifyMoney = verifyMoney.sub(occupationmny);
					} else {
						datavo.setAttributeValue("SETT_MONEY", verifyMoney);
						verifyMoney = UFDouble.ZERO_DBL;
					}
				}
			}
		}

		UFDouble verifyMoney;
		for (ArapBusiDataVO jfvo : jfVos) {
			UFDouble occupationmny = jfvo.getOccupationmny();
			UFDouble money_bal = jfvo.getMoney_bal();
			if (jfvo.getPk_billtype().equals("F2")) {
				if (jf_clje.compareTo(money_bal.sub(occupationmny)) > 0) {
					jfvo.setAttributeValue("SETT_MONEY", money_bal.sub(occupationmny));
					jf_clje = jf_clje.sub(money_bal.sub(occupationmny));
				} else {
					jfvo.setAttributeValue("SETT_MONEY", jf_clje);
					jf_clje = UFDouble.ZERO_DBL;
				}
			} else if ((jf_clje.compareTo(occupationmny) > 0) || (jf_clje.compareTo(occupationmny) < 0)) {
				jfvo.setAttributeValue("SETT_MONEY", occupationmny);
				jf_clje = jf_clje.sub(occupationmny);
			} else {
				jfvo.setAttributeValue("SETT_MONEY", jf_clje);
				jf_clje = UFDouble.ZERO_DBL;
			}
		}

		this.aggMap = onVerify(com, ruleVOMap, jfVos, dfVos);
		ArrayList<AggverifyVO> aggVOList = getAggVerifyVO(this.aggMap);

		if ((aggVOList != null) && (aggVOList.size() > 0)) {
			((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);
		}
	}

	private Map<String, List<ArapBusiDataVO>> getBusidataMap(ArapBusiDataVO[] dfVos) {
		Map<String, List<ArapBusiDataVO>> dataMap = new HashMap();
		for (ArapBusiDataVO vo : dfVos) {
			if (dataMap.containsKey(vo.getPk_item())) {
				((List) dataMap.get(vo.getPk_item())).add(vo);
			} else {
				List<ArapBusiDataVO> tempList = new ArrayList();
				tempList.add(vo);
				dataMap.put(vo.getPk_item(), tempList);
			}
		}

		return dataMap;
	}

	public static String SAMVERIFYFLAG = "SAMVERIFYFLAG";
	public static String UNVERIFYFLAG = "UNVERIFYFLAG";
	public static String HCSAMVERIFYFLAG = "HCSAMVERIFYFLAG";
	public static String HCUNVERIFYFLAG = "HCUNVERIFYFLAG";

	private void doArapVerifyWithMap(Collection<ArapBillMapVO> mapvos, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap, VerifyCom com) throws BusinessException {
		Map<String, List<ArapBillMapVO>> billMapDataUnSam;

		if ((mapvos != null) && (mapvos.size() > 0)) {
			List<ArapBillMapVO> billMapDataArap = new ArrayList();
			List<ArapBillMapVO> billMapDataUnRb = new ArrayList();
			billMapDataUnSam = new HashMap();
			for (ArapBillMapVO vo : mapvos) {
				if ((StringUtil.isEmpty(vo.getSettlecurr())) || (vo.getSettlemoney() == null)) {
					billMapDataArap.add(vo);
				} else if (("F0".equals(vo.getS_billtype())) && ("F3".equals(vo.getT_billtype()))) {
					billMapDataArap.add(vo);
				} else if (vo.getS_billtype().equals(vo.getT_billtype())) {
					billMapDataUnRb.add(vo);
				} else if (vo.getPk_currtype().equals(vo.getSettlecurr())) {
					billMapDataArap.add(vo);
				} else {
					String key = vo.getPk_currtype() + "*" + vo.getSettlecurr();
					if (billMapDataUnSam.get(key) != null) {
						((List) billMapDataUnSam.get(key)).add(vo);
					} else {
						List<ArapBillMapVO> map = new ArrayList();
						map.add(vo);
						billMapDataUnSam.put(key, map);
					}
				}
			}

			if ((billMapDataArap != null) && (billMapDataArap.size() > 0)) {
				doVerifyWithMap(billMapDataArap, ruleVOMap, com);
			}
			if ((billMapDataUnRb != null) && (billMapDataUnRb.size() > 0)) {
				doArapVerifyWithMap(billMapDataUnRb, createRBVerifyRuleVO(), HCUNVERIFYFLAG);
			}
			for (String key : billMapDataUnSam.keySet()) {
				doArapVerifyWithMap((List) billMapDataUnSam.get(key), null, UNVERIFYFLAG);
			}
		}
	}

	private void doArapVerifyWithMap(List<ArapBillMapVO> mapvos, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap, String verifyFlag) throws BusinessException {
		if ((mapvos == null) || (mapvos.size() == 0))
			return;
		this.aggMap.clear();
		VerifyCom com = new VerifyCom();
		com.setReserveFlag(VerifyDetailVO.reserve_arap);
		com.setExactVerify(true);
		Map<String, List<ArapBusiDataVO>> busiMap = new HashMap();
		List<String> pks = new ArrayList();
		List<String> termPkList = new ArrayList();
		List<String> itemPkList = new ArrayList();
		Collection<ArapBusiDataVO> busiList = null;
		Map<String, String> item_TermMap = new HashMap();

		for (ArapBillMapVO vo : mapvos) {
			if ("36D1".equals(vo.getS_billtype())) {
				termPkList.add(vo.getS_termid());
				itemPkList.add(vo.getT_itemid());
			} else if (vo.getS_itemid() != null) {
				pks.add(vo.getS_itemid());
				pks.add(vo.getT_itemid());
			}
		}

		if ((termPkList != null) && (termPkList.size() > 0)) {
			String sql = SqlUtils.getInStr("pk_termitem", (String[]) termPkList.toArray(new String[0])) + " and pausetransact ='N' " + " or " + SqlUtils.getInStr("pk_item", (String[]) itemPkList.toArray(new String[0]));
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, sql);
		} else if ((pks != null) && (pks.size() > 0)) {
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, SqlUtils.getInStr("pk_item", (String[]) pks.toArray(new String[0])) + " and money_bal <> 0 " + " and pausetransact ='N' ");
		}

		if ((busiList == null) || (busiList.size() == 0)) {
			return;
		}

		Map<String, String> busiDataTsMap = new HashMap();
		for (ArapBusiDataVO vo : busiList) {
			busiDataTsMap.put(vo.getPk_busidata(), vo.getTs().toString());
		}
		com.setBusiDataTsMap(busiDataTsMap);

		for (ArapBusiDataVO bvo : busiList) {
			item_TermMap.put(bvo.getPk_termitem(), bvo.getPk_item());
			if (busiMap.containsKey(bvo.getPk_item())) {
				List<ArapBusiDataVO> list2 = (List) busiMap.get(bvo.getPk_item());
				list2.add(bvo);
				busiMap.put(bvo.getPk_item(), list2);
			} else {
				List<ArapBusiDataVO> slist = new ArrayList();
				slist.add(bvo);
				busiMap.put(bvo.getPk_item(), slist);
			}
		}
		Map<String, ArrayList<AggverifyVO>> aggMap = null;

		List<ArapBusiDataVO> jf_vos_list = new ArrayList();
		List<ArapBusiDataVO> df_vos_list = new ArrayList();

		UFDate busiDate = null;
		if (UNVERIFYFLAG.equals(verifyFlag)) {
			busiDate = ((ArapBusiDataVO) ((List) busiMap.get(((ArapBillMapVO) mapvos.get(0)).getT_itemid())).get(0)).getBilldate();
			ruleVOMap = createUnVerifyRuleVO((ArapBillMapVO) mapvos.get(0), busiDate);
		}
		for (ArapBillMapVO vo : mapvos) {
			ArapBusiDataVO[] jf_vos = null;
			ArapBusiDataVO[] df_vos = null;

			ArapBusiDataVO[] jf_vostemp = null;
			ArapBusiDataVO[] df_vostemp = null;
			String sItemid = vo.getS_itemid();
			String tItemid = vo.getT_itemid();
			List<ArapBusiDataVO> slist = null;
			if (!"36D1".equals(vo.getS_billtype())) {
				if (sItemid == null) {
					return;
				}
			}
			List<ArapBusiDataVO> tlist = null;

			if ("36D1".equals(vo.getS_billtype())) {
				String stermid = vo.getS_termid();
				String titemid = vo.getT_itemid();

				String sitemid = (String) item_TermMap.get(stermid);
				slist = (List) busiMap.get(sitemid);
				tlist = (List) busiMap.get(titemid);
			} else {
				slist = (List) busiMap.get(sItemid);
				tlist = (List) busiMap.get(tItemid);
			}

			if (com.isArapVerify()) {
				if (slist != null) {
					for (ArapBusiDataVO svo : slist) {
						svo.setAttributeValue(SPECIAL_KEY, svo.getPk_item());
					}
				}
				if (tlist != null) {
					for (ArapBusiDataVO tvo : tlist) {
						tvo.setAttributeValue(SPECIAL_KEY, tvo.getTop_itemid());
					}
				}
			}
			ArapBusiDataVOList svolist = new ArapBusiDataVOList(slist);
			ArapBusiDataVOList tvolist = new ArapBusiDataVOList(tlist);
			if ((tvolist.getVolist() == null) || (tvolist.getVolist().size() == 0) || (svolist.getVolist() == null) || (svolist.getVolist().size() == 0)) {
				return;
			}
			if (SAMVERIFYFLAG.equals(verifyFlag)) {
				if ((((ArapBusiDataVO) svolist.getVolist().get(0)).getPk_billtype().equals("F0")) && (((ArapBusiDataVO) tvolist.getVolist().get(0)).getPk_billtype().equals("F3"))) {
					svolist.setDealMoney(vo.getYbje().multiply(-1.0D), com.isSoVerify(), aggMap);
					tvolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
				} else {
					svolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
					tvolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
				}
			} else if (UNVERIFYFLAG.equals(verifyFlag)) {
				svolist.setUnDealMoney(vo, true, aggMap, busiDate);
				tvolist.setUnDealMoney(vo, false, aggMap, busiDate);
			} else if (HCSAMVERIFYFLAG.equals(verifyFlag)) {
				svolist.setHcDealMoney(vo, true, aggMap);
				tvolist.setHcDealMoney(vo, false, aggMap);
			} else if (HCUNVERIFYFLAG.equals(verifyFlag)) {
				svolist.setHcDealMoney(vo, true, aggMap);
				tvolist.setHcDealMoney(vo, false, aggMap);
			}

			setPkorg(vo.getPk_org());

			if (isJie(svolist.getVolist())) {
				jf_vostemp = new ArapBusiDataVO[svolist.getVolist().size()];
				svolist.getVolist().toArray(jf_vostemp);

				df_vostemp = new ArapBusiDataVO[tvolist.getVolist().size()];
				tvolist.getVolist().toArray(df_vostemp);
			} else {
				df_vostemp = new ArapBusiDataVO[svolist.getVolist().size()];
				svolist.getVolist().toArray(df_vostemp);

				jf_vostemp = new ArapBusiDataVO[tvolist.getVolist().size()];
				tvolist.getVolist().toArray(jf_vostemp);
			}

			if ((jf_vostemp[0].getPk_billtype().equals("F0")) && (df_vostemp[0].getPk_billtype().equals("F3"))) {
				jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, jf_vostemp);
				jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, df_vostemp);
				com.setRedContrast(true);
			} else {
				jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, jf_vostemp);
				df_vos = (ArapBusiDataVO[]) ArrayUtil.union(df_vos, df_vostemp);
			}

			if (jf_vos != null) {
				for (ArapBusiDataVO jfvo : jf_vos) {
					jf_vos_list.add(jfvo);
				}
			}
			if (df_vos != null) {
				for (ArapBusiDataVO dfvo : df_vos) {
					df_vos_list.add(dfvo);
				}
			}
			if ((HCSAMVERIFYFLAG.equals(verifyFlag)) || (HCUNVERIFYFLAG.equals(verifyFlag))) {
				if (isJie(svolist.getVolist())) {
					jf_vos_list.addAll(df_vos_list);
					df_vos_list.clear();
				} else {
					df_vos_list.addAll(jf_vos_list);
					jf_vos_list.clear();
				}
			}
		}

		if (com.isArapVerify()) {
			aggMap = onVerify(com, ruleVOMap, (ArapBusiDataVO[]) jf_vos_list.toArray(new ArapBusiDataVO[0]), (ArapBusiDataVO[]) df_vos_list.toArray(new ArapBusiDataVO[0]));
		}

		ArrayList<AggverifyVO> aggVOList = getAggVerifyVO(aggMap);
		dealOrgScomment(aggVOList);

		if ((aggVOList != null) && (aggVOList.size() > 0)) {
			((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);
		}
	}

	private void dealOrgScomment(ArrayList<AggverifyVO> aggVOList) {
		for (AggverifyVO aggVO : aggVOList) {
			VerifyDetailVO[] childrenVO = (VerifyDetailVO[]) aggVO.getChildrenVO();
			for (VerifyDetailVO verifyvo : childrenVO) {
				if ((!StringUtil.isEmpty(verifyvo.getSrc_org())) && (!verifyvo.getPk_org().equals(verifyvo.getSrc_org()))) {
					IGeneralAccessor accessor = nc.pubitf.bd.accessor.GeneralAccessorFactory.getAccessor("2cfe13c5-9757-4ae8-9327-f5c2d34bcb46");
					IBDData docByPk = accessor.getDocByPk(verifyvo.getSrc_org());
					String orgName = "";
					if (docByPk != null) {
						orgName = docByPk.getName().toString();
					}
					if (("F0".equals(verifyvo.getPk_billtype())) || ("F2".equals(verifyvo.getPk_billtype()))) {
						verifyvo.setScomment(verifyvo.getScomment() + " " + NCLangRes4VoTransl.getNCLangRes().getStrByID("2006pub0322_0", "02006pub0322-0033", null, new String[] { orgName }));
					} else {
						verifyvo.setScomment(verifyvo.getScomment() + " " + NCLangRes4VoTransl.getNCLangRes().getStrByID("2006pub0322_0", "02006pub0322-0034", null, new String[] { orgName }));
					}
				}
			}
		}
	}

	private void doVerifyWithMap(Collection<ArapBillMapVO> mapvos, Hashtable<String, DefaultVerifyRuleVO> ruleVOMap, VerifyCom com) throws BusinessException {
		if ((mapvos == null) || (mapvos.size() == 0))
			return;
		this.aggMap.clear();
		Map<String, List<ArapBusiDataVO>> busiMap = new HashMap();
		List<String> pks = new ArrayList();
		List<String> termPkList = new ArrayList();
		List<String> itemPkList = new ArrayList();
		Collection<ArapBusiDataVO> busiList = null;
		Map<String, String> item_TermMap = new HashMap();

		for (ArapBillMapVO vo : mapvos) {
			if ("36D1".equals(vo.getS_billtype())) {
				termPkList.add(vo.getS_termid());
				itemPkList.add(vo.getT_itemid());
			} else if (vo.getS_itemid() != null) {
				pks.add(vo.getS_itemid());
				pks.add(vo.getT_itemid());
			}
		}

		if ((termPkList != null) && (termPkList.size() > 0)) {
			String sql = SqlUtils.getInStr("pk_termitem", (String[]) termPkList.toArray(new String[0])) + " and pausetransact ='N' " + " or " + SqlUtils.getInStr("pk_item", (String[]) itemPkList.toArray(new String[0]));
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, sql);
		} else if ((pks != null) && (pks.size() > 0)) {
			busiList = new BaseDAO().retrieveByClause(ArapBusiDataVO.class, SqlUtils.getInStr("pk_item", (String[]) pks.toArray(new String[0])) + " and money_bal <> 0 " + " and pausetransact ='N' ");
		}

		if ((busiList == null) || (busiList.size() == 0)) {
			return;
		}

		Map<String, String> busiDataTsMap = new HashMap();
		for (ArapBusiDataVO vo : busiList) {
			busiDataTsMap.put(vo.getPk_busidata(), vo.getTs().toString());
		}
		com.setBusiDataTsMap(busiDataTsMap);

		for (ArapBusiDataVO bvo : busiList) {
			item_TermMap.put(bvo.getPk_termitem(), bvo.getPk_item());
			if (busiMap.containsKey(bvo.getPk_item())) {
				List<ArapBusiDataVO> list2 = (List) busiMap.get(bvo.getPk_item());
				list2.add(bvo);
				busiMap.put(bvo.getPk_item(), list2);
			} else {
				List<ArapBusiDataVO> slist = new ArrayList();
				slist.add(bvo);
				busiMap.put(bvo.getPk_item(), slist);
			}
		}
		Map<String, ArrayList<AggverifyVO>> aggMap = null;

		List<ArapBusiDataVO> jf_vos_list = new ArrayList();
		List<ArapBusiDataVO> df_vos_list = new ArrayList();

		for (ArapBillMapVO vo : mapvos) {
			ArapBusiDataVO[] jf_vos = null;
			ArapBusiDataVO[] df_vos = null;

			ArapBusiDataVO[] jf_vostemp = null;
			ArapBusiDataVO[] df_vostemp = null;
			String sItemid = vo.getS_itemid();
			String tItemid = vo.getT_itemid();
			List<ArapBusiDataVO> slist = null;
			if (("36D1".equals(vo.getS_billtype())) ||

			(sItemid != null)) {

				List<ArapBusiDataVO> tlist = null;

				if ("36D1".equals(vo.getS_billtype())) {
					String stermid = vo.getS_termid();
					String titemid = vo.getT_itemid();

					String sitemid = (String) item_TermMap.get(stermid);
					slist = (List) busiMap.get(sitemid);
					tlist = (List) busiMap.get(titemid);
				} else {
					slist = (List) busiMap.get(sItemid);
					tlist = (List) busiMap.get(tItemid);
				}

				if (com.isArapVerify()) {
					if (slist != null) {
						for (ArapBusiDataVO svo : slist) {
							svo.setAttributeValue(SPECIAL_KEY, svo.getPk_item());
						}
					}
					if (tlist != null) {
						for (ArapBusiDataVO tvo : tlist) {
							tvo.setAttributeValue(SPECIAL_KEY, tvo.getTop_itemid());
						}
					}
				}

				ArapBusiDataVOList svolist = new ArapBusiDataVOList(slist);
				ArapBusiDataVOList tvolist = new ArapBusiDataVOList(tlist);
				if ((tvolist.getVolist() != null) && (tvolist.getVolist().size() != 0) && (svolist.getVolist() != null) && (svolist.getVolist().size() != 0)) {

					if ((((ArapBusiDataVO) svolist.getVolist().get(0)).getPk_billtype().equals("F0")) && (((ArapBusiDataVO) tvolist.getVolist().get(0)).getPk_billtype().equals("F3"))) {
						svolist.setDealMoney(vo.getYbje().multiply(-1.0D), com.isSoVerify(), aggMap);
						tvolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
					} else {
						svolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
						tvolist.setDealMoney(vo.getYbje(), com.isSoVerify(), aggMap);
					}

					setPkorg(vo.getPk_org());

					if (isJie(svolist.getVolist())) {
						jf_vostemp = new ArapBusiDataVO[svolist.getVolist().size()];
						svolist.getVolist().toArray(jf_vostemp);

						df_vostemp = new ArapBusiDataVO[tvolist.getVolist().size()];
						tvolist.getVolist().toArray(df_vostemp);
					} else {
						df_vostemp = new ArapBusiDataVO[svolist.getVolist().size()];
						svolist.getVolist().toArray(df_vostemp);

						jf_vostemp = new ArapBusiDataVO[tvolist.getVolist().size()];
						tvolist.getVolist().toArray(jf_vostemp);
					}

					if ((jf_vostemp[0].getPk_billtype().equals("F0")) && (df_vostemp[0].getPk_billtype().equals("F3"))) {
						jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, jf_vostemp);
						jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, df_vostemp);
						com.setRedContrast(true);
					} else {
						jf_vos = (ArapBusiDataVO[]) ArrayUtil.union(jf_vos, jf_vostemp);
						df_vos = (ArapBusiDataVO[]) ArrayUtil.union(df_vos, df_vostemp);
					}

					if (!com.isArapVerify()) {
						aggMap = onVerify(com, ruleVOMap, jf_vos, df_vos);
					} else {
						if (jf_vos != null) {
							for (ArapBusiDataVO jfvo : jf_vos) {
								jf_vos_list.add(jfvo);
							}
						}
						if (df_vos != null) {
							for (ArapBusiDataVO dfvo : df_vos)
								df_vos_list.add(dfvo);
						}
					}
				}
			}
		}
		if (com.isArapVerify()) {
			aggMap = onVerify(com, ruleVOMap, (ArapBusiDataVO[]) jf_vos_list.toArray(new ArapBusiDataVO[0]), (ArapBusiDataVO[]) df_vos_list.toArray(new ArapBusiDataVO[0]));
		}

		ArrayList<AggverifyVO> aggVOList = getAggVerifyVO(aggMap);
		dealOrgScomment(aggVOList);

		if ((aggVOList != null) && (aggVOList.size() > 0)) {
			((IArapVerifyLogPrivate) NCLocator.getInstance().lookup(IArapVerifyLogPrivate.class)).save(aggVOList, ruleVOMap, com);
		}
	}

	private Map<String, ArrayList<AggverifyVO>> onVerify(VerifyCom com, Map<String, DefaultVerifyRuleVO> ruleVOMap, ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos) {
		com.setM_creditSelected(df_vos);
		com.setM_debitSelected(jf_vos);
		AggverifyVO aggvo = null;
		if ((!ArrayUtils.isEmpty(jf_vos)) || (!ArrayUtils.isEmpty(df_vos))) {
			for (String key : ruleVOMap.keySet()) {
				if ((ArrayUtils.isEmpty(jf_vos)) || (ArrayUtils.isEmpty(df_vos))) {
					if (!key.equals("SAME_VERIFY")) {
					}
				} else {
					String billclass = jf_vos[0].getBillclass();
					String billclass2 = df_vos[0].getBillclass();

					if (((!billclass.equals("ys")) || (!billclass2.equals("fk"))) && (((billclass.equals("fk")) && (billclass2.equals("ys")) && (

					(key.equals("SAME_VERIFY")) || (key.equals("UNSAME_VERIFY")))) || (

					((!billclass.equals("ys")) || (!billclass2.equals("sk"))) && ((!billclass.equals("sk")) || (!billclass2.equals("ys"))) && ((!billclass.equals("yf")) || (!billclass2.equals("fk"))) && ((billclass.equals("fk")) && (billclass2.equals("yf")) &&

					(key.equals("RB_VERIFY")))))) {
						continue;
					}
				}

				DefaultVerifyRuleVO rulevo = (DefaultVerifyRuleVO) ruleVOMap.get(key);
				if (key.equals("SAME_VERIFY")) {
					SameMnyVerify smverify = new SameMnyVerify(com);
					aggvo = smverify.onVerify(jf_vos, df_vos, rulevo);
					if (aggvo != null) {

						if (!ArrayUtils.isEmpty(aggvo.getChildrenVO()))
							if (this.aggMap.containsKey("SAME_VERIFY")) {
								((ArrayList) this.aggMap.get(key)).add(aggvo);
							} else {
								ArrayList<AggverifyVO> temp = new ArrayList();
								temp.add(aggvo);
								this.aggMap.put("SAME_VERIFY", temp);
							}
					}
				} else if (key.equals("RB_VERIFY")) {
					RBVerify rbverify = new RBVerify(com);
					aggvo = rbverify.onVerify(jf_vos, df_vos, rulevo);
					if (!ArrayUtils.isEmpty(aggvo.getChildrenVO())) {
						if (this.aggMap.containsKey("RB_VERIFY")) {
							((ArrayList) this.aggMap.get(key)).add(aggvo);
						} else {
							ArrayList<AggverifyVO> temp = new ArrayList();
							temp.add(aggvo);
							this.aggMap.put("RB_VERIFY", temp);
						}
					}
				} else if (key.equals("UNSAME_VERIFY")) {
					UnsameMnyVerify unsameMnyVerify = new UnsameMnyVerify(com);
					aggvo = unsameMnyVerify.onVerify(jf_vos, df_vos, rulevo);
					if (!ArrayUtils.isEmpty(aggvo.getChildrenVO())) {
						if (this.aggMap.containsKey("UNSAME_VERIFY")) {
							((ArrayList) this.aggMap.get(key)).add(aggvo);
						} else {
							ArrayList<AggverifyVO> temp = new ArrayList();
							temp.add(aggvo);
							this.aggMap.put("UNSAME_VERIFY", temp);
						}
					}
				}
			}
		}

		return this.aggMap;
	}

	private ArrayList<AggverifyVO> getAggVerifyVO(Map<String, ArrayList<AggverifyVO>> aggMap) throws BusinessException {
		List<VerifyDetailVO> detailVOList = new ArrayList();
		ArrayList resultList = new ArrayList();
		if (aggMap == null) {
			return resultList;
		}
		for (String key : aggMap.keySet()) {
			ArrayList<AggverifyVO> arrayList = (ArrayList) aggMap.get(key);
			if (arrayList != null) {
				for (AggverifyVO aggVO : arrayList) {
					VerifyDetailVO[] childrenVO = (VerifyDetailVO[]) aggVO.getChildrenVO();

					for (VerifyDetailVO verifyvo : childrenVO) {
						verifyvo.setIsauto(UFBoolean.TRUE);
						detailVOList.add(verifyvo);
					}
				}

				AggverifyVO aggVO = new AggverifyVO();
				aggVO.setChildrenVO((CircularlyAccessibleValueObject[]) detailVOList.toArray(new VerifyDetailVO[0]));

				VerifyCom.calGroupAndGloablMny(aggVO);
				ArrayList<AggverifyVO> aggvolist = (ArrayList) VerifyCom.verifySumData(aggVO);

				resultList.addAll(aggvolist);
			}
		}
		return resultList;
	}

	private Hashtable<String, DefaultVerifyRuleVO> createUnVerifyRuleVO(ArapBillMapVO vo, UFDate busiDate) throws BusinessException {
		Hashtable<String, DefaultVerifyRuleVO> ruleMap = new Hashtable();
		DefaultVerifyRuleVO rulevo = getVerifyRuleVO();
		rulevo.setM_verifyName("UNSAME_VERIFY");

		String pk_org = "";
		String pk_group = InvocationInfoProxy.getInstance().getGroupId();
		if (vo.getPk_org().equals(vo.getPk_org1())) {
			pk_org = vo.getPk_org();
		} else {
			pk_org = pk_group;
		}
		String verifyCurr = CurrencyRateUtilHelper.getInstance().getLocalCurrtypeByOrgID(pk_org);
		rulevo.setM_verifyCurr(verifyCurr);

		if (("F2".equals(vo.getS_billtype())) || ("F1".equals(vo.getS_billtype()))) {
			int rateType = ArapBillCalUtil.getRateType(vo.getT_billtype(), pk_group);
			rulevo.setM_debitCurr(vo.getPk_currtype());
			rulevo.setM_jfbz2zjbzHL(Currency.getRate(pk_org, vo.getPk_currtype(), verifyCurr, busiDate, rateType));
			rulevo.setM_debttoBBExchange_rate(Currency.getRate(pk_org, vo.getPk_currtype(), busiDate, rateType));

			int rateType1 = ArapBillCalUtil.getRateType(vo.getS_billtype(), pk_group);
			rulevo.setM_creditCurr(vo.getSettlecurr());
			rulevo.setM_dfbz2zjbzHL(Currency.getRate(pk_org, vo.getSettlecurr(), verifyCurr, busiDate, rateType1));
			rulevo.setM_creditoBBExchange_rate(Currency.getRate(pk_org, vo.getSettlecurr(), busiDate, rateType1));
		} else {
			int rateType = ArapBillCalUtil.getRateType(vo.getS_billtype(), pk_group);
			rulevo.setM_debitCurr(vo.getSettlecurr());
			rulevo.setM_jfbz2zjbzHL(Currency.getRate(pk_org, vo.getSettlecurr(), verifyCurr, busiDate, rateType));
			rulevo.setM_debttoBBExchange_rate(Currency.getRate(pk_org, vo.getSettlecurr(), busiDate, rateType));

			int rateType1 = ArapBillCalUtil.getRateType(vo.getT_billtype(), pk_group);
			rulevo.setM_creditCurr(vo.getPk_currtype());
			rulevo.setM_dfbz2zjbzHL(Currency.getRate(pk_org, vo.getPk_currtype(), verifyCurr, busiDate, rateType1));
			rulevo.setM_creditoBBExchange_rate(Currency.getRate(pk_org, vo.getPk_currtype(), busiDate, rateType1));
		}
		ruleMap.put("UNSAME_VERIFY", rulevo);
		return ruleMap;
	}

	private Hashtable<String, DefaultVerifyRuleVO> createRBVerifyRuleVO() {
		Hashtable<String, DefaultVerifyRuleVO> ruleMap = new Hashtable();
		DefaultVerifyRuleVO rulevo = getVerifyRuleVO();
		rulevo.setM_verifyName("RB_VERIFY");
		ruleMap.put("RB_VERIFY", rulevo);
		return ruleMap;
	}

	private Hashtable<String, DefaultVerifyRuleVO> createVerifyRuleVO() {
		Hashtable<String, DefaultVerifyRuleVO> ruleMap = new Hashtable();
		DefaultVerifyRuleVO rulevo = getVerifyRuleVO();
		rulevo.setM_verifyName("SAME_VERIFY");
		ruleMap.put("SAME_VERIFY", rulevo);
		return ruleMap;
	}

	private DefaultVerifyRuleVO getVerifyRuleVO() {
		DefaultVerifyRuleVO rulevo = new DefaultVerifyRuleVO();
		Integer m_hxSeq = Integer.valueOf(0);
		UFDateTime busidate = null;
		try {
			busidate = ((IServerEnvironmentService) NCLocator.getInstance().lookup(IServerEnvironmentService.class)).getServerTime();
			String hxSeq = "";
			if (getSyscode().intValue() == 0) {
				hxSeq = SysinitAccessor.getInstance().getParaString(getPkorg(), "AR1");
			} else {
				hxSeq = SysinitAccessor.getInstance().getParaString(getPkorg(), "AP1");
			}
			if (hxSeq.equals(SysinitConst.VERIFY_ZAO)) {
				m_hxSeq = Integer.valueOf(0);
			} else {
				m_hxSeq = Integer.valueOf(1);
			}
		} catch (Exception e) {
			ExceptionHandler.consume(e);
		}
		rulevo.setM_verifySeq(m_hxSeq);
		UFDate clrq = new UFDate(InvocationInfoProxy.getInstance().getBizDateTime());
		rulevo.setM_Clrq(clrq);
		AccountCalendar ac = AccountCalendar.getInstanceByPk_org(getPkorg());
		try {
			ac.setDate(busidate.getDate());
		} catch (InvalidAccperiodExcetion e) {
			ExceptionHandler.consume(e);
		}
		rulevo.setSystem(getSyscode());
		rulevo.setPk_org(getPkorg());
		rulevo.setM_Clnd(ac.getYearVO().getPeriodyear());
		rulevo.setM_Clqj(ac.getMonthVO().getAccperiodmth());
		rulevo.setIsOccuption(Boolean.valueOf(true));
		rulevo.setM_clr(getPk_user());
		return rulevo;
	}

	private Hashtable<String, DefaultVerifyRuleVO> creatVerifyRuleVO() {
		Hashtable<String, DefaultVerifyRuleVO> ruleMap = new Hashtable();
		DefaultVerifyRuleVO rulevo = getVerifyRuleVO();
		rulevo.setM_verifyName("SAME_VERIFY");
		DefaultVerifyRuleVO rbrulevo = (DefaultVerifyRuleVO) rulevo.clone();
		rbrulevo.setM_verifyName("RB_VERIFY");
		ruleMap.put("SAME_VERIFY", rulevo);
		ruleMap.put("RB_VERIFY", rbrulevo);

		return ruleMap;
	}

	private boolean isJie(List<ArapBusiDataVO> listvo) {
		Iterator i$ = listvo.iterator();
		if (i$.hasNext()) {
			ArapBusiDataVO vo = (ArapBusiDataVO) i$.next();
			if (vo == null) {
				return false;
			}
			Integer int_fx = (Integer) vo.getAttributeValue("direction");
			if (int_fx == null) {
				return false;
			}

			int fx = int_fx.intValue();

			return fx == 1;
		}

		return false;
	}
}