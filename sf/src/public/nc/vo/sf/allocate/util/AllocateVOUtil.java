package nc.vo.sf.allocate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import nc.itf.bd.bankdoc.IBankdocQueryService;
import nc.itf.sf.allocate.proxy.AllocateServiceProxy;
import nc.itf.sf.allocateagree.IAllocateAgreeQueryService;
import nc.itf.tmpub.datemanage.DateManageProxy;
import nc.itf.tmpub.datemanage.IDateManage;
import nc.itf.tmpub.uapbd.IUapPubSmartService;
import nc.pubitf.obm.IObmLogManageService;
import nc.pubitf.org.IOrgUnitPubService;
import nc.sf.pub.util.SFQueryModulesUtil;
import nc.vo.bd.bankaccount.BankAccbasVO;
import nc.vo.bd.bankdoc.BankdocVO;
import nc.vo.ebank.interfac.FuncTypeConst;
import nc.vo.fts.commission.proxy.ComOtherServiceProxy;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.obm.pay.LogMgrRequestVO;
import nc.vo.obm.pay.OnlinePaymentVO;
import nc.vo.obm.pay.PaymentRetMsg;
import nc.vo.org.OrgVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.sf.allocate.AggAllocateVO;
import nc.vo.sf.allocate.AllocateBVO;
import nc.vo.sf.allocate.AllocateVO;
import nc.vo.sf.allocate.IAllocateConst;
import nc.vo.sf.allocateagree.AggAllocateAgreeVO;
import nc.vo.sf.allocateagree.AllocateAgreeBVO;
import nc.vo.sf.allocateagree.AllocateAgreeVO;
import nc.vo.sf.allocatereceipt.AggAllocateReceiptVO;
import nc.vo.sf.allocatereceipt.AllocateReceiptVO;
import nc.vo.sf.allocatereceipt.util.AllocateReceiptAmountUtil;
import nc.vo.sf.pub.batch.BatchHintVO;
import nc.vo.sf.pub.batch.BodyErrorVO;
import nc.vo.sf.pub.pay.PayRetMessageUtil;
import nc.vo.sf.pub.util.SFOrgUtil;
import nc.vo.tmpub.datemanage.DateManageQueryVO;
import nc.vo.tmpub.util.ArrayUtil;
import nc.vo.tmpub.util.DateUtil;
import nc.vo.tmpub.util.StringUtil;

public class AllocateVOUtil {
	private AllocateVOUtil() {
		throw new AssertionError();
	}

	public static void deleteEbankWrite(AggAllocateVO billVO) throws BusinessException {
		AllocateVO allocateVO = (AllocateVO) billVO.getParentVO();

		boolean isOBMEnable = SFQueryModulesUtil.isOBMEnable(allocateVO.getPk_group());
		if (isOBMEnable) {
			List<String> yurrefList = new ArrayList();
			for (AllocateBVO child : billVO.getItem()) {
				if (StringUtil.isNotNull(child.getPrimaryKey())) {
					yurrefList.add(child.getPrimaryKey());
					child.setIsnetbankfull(UFBoolean.FALSE);
				}
			}

			if (!ArrayUtil.isNull(yurrefList)) {
				LogMgrRequestVO requestVO = LogMgrRequestVO.createDelRequest((String[]) yurrefList.toArray(new String[yurrefList.size()]));
				ComOtherServiceProxy.getObmLogManageService().deleteObmLog(requestVO);
			}
		}
	}

	public static OnlinePaymentVO[] convertToOnlinePaymentVO(AggAllocateVO billvo, boolean checkIsFull) throws BusinessException {
		AllocateVO parentvo = (AllocateVO) billvo.getParentVO();
		String pk_billtype = parentvo.getPk_billtype();
		String sourcemoudeCode = parentvo.getRecmodul();
		String pk_currtype = parentvo.getPk_currtype();
		String pk_submiter = parentvo.getSubmituser();
		AllocateBVO[] childVos = (AllocateBVO[]) billvo.getChildrenVO();
		List<OnlinePaymentVO> netpayvoList = new ArrayList();
		List<String> pk_bankaccList = new ArrayList();
		for (int i = 0; i < childVos.length; i++) {
			pk_bankaccList.add(childVos[i].getPk_bankacc_p());
			pk_bankaccList.add(childVos[i].getPk_bankacc_r());
		}

		HashMap<String, BankAccbasVO> accVOMap = AllocateServiceProxy.getUapPubSmartService().getBankAccbasVOBySubAccPk(pk_bankaccList);

		for (int i = 0; i < childVos.length; i++) {
			OnlinePaymentVO netpayvo = new OnlinePaymentVO();
			if ((childVos[i].getIsnetpay() != null) && (childVos[i].getIsnetpay().booleanValue())) {

				if ((checkIsFull) && ((childVos[i].getIsnetbankfull() == null) || (!childVos[i].getIsnetbankfull().booleanValue()))) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0193"));
				}

				netpayvo = new OnlinePaymentVO();
				netpayvo.setCurrency(pk_currtype);

				if (StringUtil.isNull(childVos[i].getPk_bankacc_p())) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0194"));
				}

				netpayvo.setDbtaccsubPk(childVos[i].getPk_bankacc_p());

				BankAccbasVO bankaccs_p = (BankAccbasVO) accVOMap.get(childVos[i].getPk_bankacc_p());

				if (bankaccs_p != null) {
					netpayvo.setDbtacc(bankaccs_p.getAccnum());
					netpayvo.setDbtname(bankaccs_p.getAccname());
					netpayvo.setDbtaccPk(bankaccs_p.getPk_bankaccbas());
				}

				if (StringUtil.isNull(childVos[i].getPk_bankacc_r())) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0195"));
				}
				netpayvo.setCrtaccPk(childVos[i].getPk_bankacc_r());
				netpayvo.setCrtacc(childVos[i].getBankacccode_r());
				netpayvo.setCrtname(childVos[i].getBankaccname_r());

				BankAccbasVO bankaccs_r = (BankAccbasVO) accVOMap.get(childVos[i].getPk_bankacc_r());
				if (bankaccs_r != null) {
					BankdocVO docVO = AllocateServiceProxy.getBankdocQueryService().getBankdocVOByPk(bankaccs_r.getPk_bankdoc());

					String nameField = "name";
					netpayvo.setCrtbank(docVO != null ? (String) docVO.getAttributeValue(nameField) : null);
				}

				netpayvo.setTrsamt(childVos[i].getAmount());
				netpayvo.setYurref(childVos[i].getPrimaryKey());

				netpayvo.setBilltype(pk_billtype);
				netpayvo.setModulecode(sourcemoudeCode);
				netpayvo.setSrcsystem("SF");
				netpayvo.setBillcode(parentvo.getVbillno());
				netpayvo.setBillpk(parentvo.getPrimaryKey());
				netpayvo.setFunc(FuncTypeConst.JTGJ);
				netpayvo.setTransfer_type("1");
				netpayvo.setPk_org(parentvo.getPk_org());
				netpayvo.setPaytype(childVos[i].getPaytype());
				netpayvo.setIssamebank(childVos[i].getIssamebank());
				netpayvo.setIssamecity(childVos[i].getIssamecity());
				netpayvo.setBusnar(childVos[i].getRemark());
				netpayvo.setNusage(childVos[i].getRemark());
				netpayvo.setReconciliationcode(childVos[i].getBankrelatcode());
				netpayvo.setCryptograph(childVos[i].getEncryptkey());
				if (checkIsFull) {
					netpayvoList.add(netpayvo);
				} else if ((!StringUtil.toIntegerIgnoreNull(childVos[i].getPaystatus()).equals(IAllocateConst.PayStatus_Paying)) && (!StringUtil.toIntegerIgnoreNull(childVos[i].getPaystatus()).equals(IAllocateConst.PayStatus_Payok))) {

					netpayvoList.add(netpayvo);
				}
			}
		}
		if (netpayvoList.size() == 0) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0196"));
		}
		return (OnlinePaymentVO[]) netpayvoList.toArray(new OnlinePaymentVO[netpayvoList.size()]);
	}

	public static AllocateBVO[] getNetPayAllocateBVOs(AggAllocateVO aggAllocateVO, boolean netPayFlag) {
		List<AllocateBVO> allocateBvoList = new ArrayList();
		AllocateBVO[] childrenVos = aggAllocateVO.getItem();
		if (childrenVos != null) {
			for (AllocateBVO tempvo : childrenVos) {
				if (netPayFlag) {
					if ((StringUtil.toBoolean(tempvo.getIsnetpay())) && ((StringUtil.toIntegerIgnoreNull(tempvo.getPaystatus()).equals(IAllocateConst.PayStatus_Payfailed)) || (StringUtil.toIntegerIgnoreNull(tempvo.getPaystatus()).equals(IAllocateConst.PayStatus_WaitCommit)) || (tempvo.getPaystatus() == null)) && (!IAllocateConst.RecordStatus_Disuse.equals(tempvo.getRecordstatus()))) {

						allocateBvoList.add(tempvo);
					}
				} else if ((tempvo.getIsnetpay() == null) || (!tempvo.getIsnetpay().booleanValue())) {
					allocateBvoList.add(tempvo);
				}
			}
		}

		return (AllocateBVO[]) allocateBvoList.toArray(new AllocateBVO[allocateBvoList.size()]);
	}

	public static AllocateBVO[] getNetPayAllocateBVOs(AggAllocateVO aggAllocateVO, BatchHintVO batchHintVo) {
		List<AllocateBVO> allocateBvoList = new ArrayList();
		AllocateBVO[] childrenVos = aggAllocateVO.getItem();
		List<BodyErrorVO> errorVOList = new ArrayList();
		if (childrenVos != null) {
			for (AllocateBVO tempvo : childrenVos) {
				if ((tempvo.getIsnetpay() != null) && (tempvo.getIsnetpay().booleanValue())) {
					if ((tempvo.getIsnetbankfull() != null) && (tempvo.getIsnetbankfull().booleanValue())) {
						if ((tempvo.getPaystatus() == null) || (tempvo.getPaystatus().equals(IAllocateConst.PayStatus_WaitCommit)) || (tempvo.getPaystatus().equals(IAllocateConst.PayStatus_Payfailed))) {

							allocateBvoList.add(tempvo);
						}
					} else {
						BodyErrorVO errorVO = new BodyErrorVO();
						errorVO.setPk_body(tempvo.getPk_allocate_b());
						errorVO.setBillno(aggAllocateVO.getHead().getVbillno());
						errorVO.setInCompleteInfo(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0197"));
						errorVOList.add(errorVO);
					}
				}
			}
		}
		batchHintVo.setErrVOList(errorVOList);
		return (AllocateBVO[]) allocateBvoList.toArray(new AllocateBVO[allocateBvoList.size()]);
	}

	public static AggAllocateVO setHandPayInfo(AggAllocateVO aggAllocateVO) throws BusinessException {
		AllocateBVO[] allocateBVOs = aggAllocateVO.getItem();
		AllocateVO allocateVO = aggAllocateVO.getHead();
		DateManageQueryVO queryVO = new DateManageQueryVO();
		queryVO.setCurrentBusiDate(allocateVO.getBusidate());
		queryVO.setPk_group(allocateVO.getPk_group());
		queryVO.setPk_org(allocateVO.getPk_org());
		queryVO.setMaxBillDate(AllocatePubUtil.getMaxActionDate(aggAllocateVO));
		UFDate payDate = DateManageProxy.getDateManageService().getProcessedBusiDate(queryVO);

		for (int i = 0; i < allocateBVOs.length; i++) {
			if ((!allocateBVOs[i].getIsnetpay().booleanValue()) && (allocateBVOs[i].getPaystatus() == null)) {
				allocateBVOs[i].setPaystatus(IAllocateConst.PayStatus_Payok);

				allocateBVOs[i].setPaydate(payDate);
				allocateBVOs[i].setPaytime(DateUtil.getCurrTime());
				allocateBVOs[i].setStatus(1);
			}
		}

		return aggAllocateVO;
	}

	public static void setNetPayFailInfo(PaymentRetMsg[] refmsgs, BatchHintVO batchHintVo) throws BusinessException {
		AggAllocateVO aggVo = (AggAllocateVO) batchHintVo.getAggVO();
		AllocateBVO[] allocateBVOs = aggVo.getItem();
		AllocateVO allocateVO = aggVo.getHead();
		DateManageQueryVO queryVO = new DateManageQueryVO();
		queryVO.setCurrentBusiDate(allocateVO.getBusidate());
		queryVO.setPk_group(allocateVO.getPk_group());
		queryVO.setPk_org(allocateVO.getPk_org());
		queryVO.setMaxBillDate(AllocatePubUtil.getMaxActionDate(aggVo));
		UFDate payDate = DateManageProxy.getDateManageService().getProcessedBusiDate(queryVO);

		if ((refmsgs != null) && (refmsgs.length > 0)) {
			for (int i = 0; i < refmsgs.length; i++) {
				if (refmsgs[i].getTranflag().intValue() == 1) {
					BodyErrorVO error = new BodyErrorVO();
					error.setPk_body(refmsgs[i].getYurref());
					error.setInCompleteInfo(refmsgs[i].getErrmsg());
					error.setBillno(allocateVO.getVbillno());
					batchHintVo.getErrVOList().add(error);
				} else {
					for (AllocateBVO tmpvo : allocateBVOs) {
						if (refmsgs[i].getYurref().equals(tmpvo.getPrimaryKey())) {
							String errmsg = refmsgs[i].getErrmsg();
							if (StringUtil.isNull(errmsg)) {
								errmsg = refmsgs[i].getMsg();
							}
							tmpvo.setNetacceptinfo(PayRetMessageUtil.getPayRetMsg(errmsg));
							tmpvo.setPaystatus(IAllocateConst.PayStatus_Paying);
							tmpvo.setPaydate(payDate);
							tmpvo.setPaytime(DateUtil.getCurrTime());
							tmpvo.setStatus(1);
						}
					}
				}
			}
		}
	}

	public static AggAllocateReceiptVO[] convertToReceiptVO(AggAllocateVO aggVO) throws BusinessException {
		List<AggAllocateReceiptVO> voList = new ArrayList();
		AllocateVO allocateVO = aggVO.getHead();
		AllocateBVO[] allocateBvos = (AllocateBVO[]) aggVO.getChildrenVO();

		for (AllocateBVO allocateBvo : allocateBvos) {
			if (allocateBvo.getPaystatus().equals(IAllocateConst.PayStatus_Payok)) {
				HashMap hm = SFOrgUtil.getOrgVidByOrgPK(new String[] { allocateBvo.getPk_org_r(), allocateVO.getPk_org() });

				AllocateReceiptVO receiptVO = new AllocateReceiptVO();
				receiptVO.setPk_org(allocateBvo.getPk_org_r());
				receiptVO.setPk_org_v(StringUtil.toString(hm.get(allocateBvo.getPk_org_r())));

				try {
					OrgVO[] orgVOs =
							AllocateServiceProxy.getOrgUnitPubService().getOrgs(new String[] { allocateBvo.getPk_org_r() }, new String[] { "pk_org", "pk_group" });

					receiptVO.setPk_group(orgVOs[0].getPk_group());
				} catch (BusinessException e) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0198"));
				}

				receiptVO.setPk_org_r(allocateBvo.getPk_org_r());
				receiptVO.setPk_org_r_v(StringUtil.toString(hm.get(allocateBvo.getPk_org_r())));

				receiptVO.setPk_org_p(allocateVO.getPk_org());
				receiptVO.setPk_org_p_v(StringUtil.toString(hm.get(allocateVO.getPk_org())));

				receiptVO.setBusitype(allocateVO.getBusitype());
				receiptVO.setFundtype(allocateBvo.getFundtype());
				receiptVO.setPk_balatype(allocateBvo.getPk_balatype());
				receiptVO.setIsreverse(allocateVO.getIsreversebustype());
				receiptVO.setIstally(UFBoolean.FALSE);
				receiptVO.setIsunitevoucher(UFBoolean.FALSE);
				receiptVO.setIsvoucher(UFBoolean.FALSE);
				receiptVO.setPaytype(allocateBvo.getPaytype());
				receiptVO.setPaydate(allocateBvo.getPaydate());
				receiptVO.setAmount(allocateBvo.getAmount());
				receiptVO.setPk_currtype(allocateVO.getPk_currtype());
				receiptVO.setPk_accid_r(allocateBvo.getPk_accid_r());
				receiptVO.setPk_bankacc_p(allocateBvo.getPk_bankacc_p());
				receiptVO.setPk_bankdoc_p(allocateBvo.getBankname_p());
				receiptVO.setPk_bankacc_r(allocateBvo.getPk_bankacc_r());
				receiptVO.setBankacccode_r(allocateBvo.getBankacccode_r());
				receiptVO.setBankaccname_r(allocateBvo.getBankaccname_r());
				receiptVO.setPk_bankdoc_r(allocateBvo.getBankname_r());
				receiptVO.setPk_planitem(allocateBvo.getPk_planitem_p());
				receiptVO.setPk_planitem_r(allocateBvo.getPk_planitem_r());
				receiptVO.setRecmodule("SF");
				receiptVO.setSrcbillno(allocateVO.getVbillno());
				receiptVO.setPk_srcbill(allocateVO.getPk_allocate_h());
				receiptVO.setPk_srcbill_b(allocateBvo.getPk_allocate_b());
				receiptVO.setSrcbilltype("36K2Z600000000000000");
				receiptVO.setRemark(allocateBvo.getRemark());
				if ((allocateVO.getSrcbilltype() != null) && (allocateVO.getSrcbilltype().equals("36K7Z600000000000000"))) {

					AggAllocateAgreeVO aggAgreeVO =
							AllocateServiceProxy.getAllocateAgreeQueryService().queryAllocateAgreeByPK(allocateBvo.getPk_srcbillhead());

					receiptVO.setPk_allocateapply_h(aggAgreeVO.getHead().getPk_srcbill());
					// 20170417_0 tsy ±£¥Ê…Í«Îµ•∫≈
					receiptVO.setVueserdef5(aggAgreeVO.getHead().getSrcbillcode());
					// 20170417_0 end

					for (CircularlyAccessibleValueObject agreeBvo : aggAgreeVO.getChildrenVO()) {
						if (((AllocateAgreeBVO) agreeBvo).getPk_allocateagree_b().equals(allocateBvo.getPk_srcbill())) {

							receiptVO.setPk_allocateapply_b(((AllocateAgreeBVO) agreeBvo).getPk_srcbill());

							break;
						}
					}
				}
				receiptVO.setVueserdef1(allocateBvo.getVuserdef1());
				receiptVO.setVueserdef2(allocateBvo.getVuserdef2());
				receiptVO.setVueserdef3(allocateBvo.getVuserdef3());
				receiptVO.setVueserdef4(allocateBvo.getVuserdef4());
				// 20170417_1 tsy ±£¥Ê…Í«Îµ•∫≈
				// receiptVO.setVueserdef5(allocateBvo.getVuserdef5());
				// 20170417_1 end
				receiptVO.setVueserdef6(allocateBvo.getVuserdef6());
				receiptVO.setVueserdef7(allocateBvo.getVuserdef7());
				receiptVO.setVueserdef8(allocateBvo.getVuserdef8());
				receiptVO.setVueserdef9(allocateBvo.getVuserdef9());
				receiptVO.setVueserdef10(allocateBvo.getVuserdef10());

				receiptVO.setBankrelatcode(allocateBvo.getBankrelatcode());

				receiptVO.setCreationtime(allocateBvo.getPaytime());
				receiptVO.setCreator("NC_USER0000000000000");

				AggAllocateReceiptVO aggReceiptVO = new AggAllocateReceiptVO();
				aggReceiptVO.setHead(receiptVO);
				AllocateReceiptAmountUtil.fillAmountFiledsAndRate(aggReceiptVO);
				voList.add(aggReceiptVO);
			}
		}
		return (AggAllocateReceiptVO[]) voList.toArray(new AggAllocateReceiptVO[voList.size()]);
	}

	public static boolean isIncludeNetPayBody(AggAllocateVO[] aggvos) {
		if (ArrayUtil.isNull(aggvos)) {
			return false;
		}
		for (AggAllocateVO tempvo : aggvos) {
			if (!ArrayUtil.isNull(getNetPayAllocateBVOs(tempvo, true))) {
				return true;
			}
		}
		return false;
	}

	public static AllocateBVO[] getNetPayBodys(AggregatedValueObject[] aggvos) {
		if (ArrayUtil.isNull(aggvos)) {
			return null;
		}
		List<AllocateBVO> bodyList = new ArrayList();
		for (AggregatedValueObject tempvo : aggvos) {
			AllocateBVO[] netpaybody = getNetPayAllocateBVOs((AggAllocateVO) tempvo, true);
			for (AllocateBVO body : netpaybody) {
				bodyList.add(body);
			}
		}
		return (AllocateBVO[]) bodyList.toArray(new AllocateBVO[bodyList.size()]);
	}
}