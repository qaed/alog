package nc.vo.sf.allocateagree.util;

import java.util.ArrayList;
import java.util.List;
import nc.itf.sf.allocate.proxy.AllocateServiceProxy;
import nc.itf.sf.allocateagree.proxy.AllocateAgreeServiceProxy;
import nc.pubitf.uapbd.IBankaccPubQueryService;
import nc.vo.bd.bankaccount.BankAccbasVO;
import nc.vo.ebank.interfac.FuncTypeConst;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.obm.pay.OnlinePaymentVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.sf.allocate.AggAllocateVO;
import nc.vo.sf.allocate.AllocateBVO;
import nc.vo.sf.allocate.AllocateVO;
import nc.vo.sf.allocate.IAllocateConst;
import nc.vo.sf.allocate.util.AllocateAmountUtil;
import nc.vo.sf.allocate.util.AllocatePubUtil;
import nc.vo.sf.allocateagree.AggAllocateAgreeVO;
import nc.vo.sf.allocateagree.AllocateAgreeBVO;
import nc.vo.sf.allocateagree.AllocateAgreeVO;
import nc.vo.sf.allocateagree.IAllocateAgreeConst;
import nc.vo.sf.allocateapply.AggAllocateApplyVO;
import nc.vo.sf.allocateapply.AllocateApplyBVO;
import nc.vo.sf.allocateapply.AllocateApplyVO;
import nc.vo.sf.allocateapply.IAllocateApplyConst;
import nc.vo.sf.pub.util.SFOrgUtil;
import nc.vo.tmpub.util.ArrayUtil;
import nc.vo.tmpub.util.StringUtil;

public class AllocateAgreeVOUtil {
	public AllocateAgreeVOUtil() {
	}

	public static AggAllocateVO converToAllocateVO(AggAllocateAgreeVO aggAgreeVo) throws BusinessException {
		AllocateAgreeVO agreeVO = (AllocateAgreeVO) aggAgreeVo.getParentVO();
		AllocateAgreeBVO[] agreeBvos = (AllocateAgreeBVO[]) aggAgreeVo.getChildrenVO();

		AggAllocateVO aggAllocateVo = new AggAllocateVO();
		AllocateVO allocateVO = new AllocateVO();
		AllocateBVO[] allocateBvos = new AllocateBVO[agreeBvos.length];

		allocateVO.setPk_srcbill(agreeVO.getPk_allocateagree_h());
		allocateVO.setSrcbilltype(agreeVO.getPk_billtypeid());
		allocateVO.setSrcbillcode(agreeVO.getVbillno());
		allocateVO.setRecmodul("SF");
		allocateVO.setSrcbusitype(IAllocateConst.SrcBusiType_Apply);
		allocateVO.setPk_currtype(agreeVO.getPk_currtype());
		allocateVO.setStroke(agreeVO.getStroke());
		allocateVO.setBusitype(agreeVO.getBusitype());
		allocateVO.setPk_billtype("36K2");
		allocateVO.setPk_org(agreeVO.getPk_org());
		allocateVO.setPk_org_v(SFOrgUtil.getOrgVidByOrgPK(agreeVO.getPk_org()));
		allocateVO.setIsreversebustype(UFBoolean.FALSE);
		allocateVO.setIsmakevoucher(UFBoolean.FALSE);
		allocateVO.setIsreversebustype(UFBoolean.FALSE);
		allocateVO.setMemo(agreeVO.getMemo());

		allocateVO.setVuserdef1(agreeVO.getVuserdef1());
		allocateVO.setVuserdef2(agreeVO.getVuserdef2());
		allocateVO.setVuserdef3(agreeVO.getVuserdef3());
		allocateVO.setVuserdef4(agreeVO.getVuserdef4());
		// 20170417_0 tsy ±£¥Ê…Í«Îµ•∫≈
		// allocateVO.setVuserdef5(agreeVO.getVuserdef5());
		 allocateVO.setVuserdef5(agreeVO.getSrcbillcode());
		// 20170417_0 end
		allocateVO.setVuserdef6(agreeVO.getVuserdef6());
		allocateVO.setVuserdef7(agreeVO.getVuserdef7());
		allocateVO.setVuserdef8(agreeVO.getVuserdef8());
		allocateVO.setVuserdef9(agreeVO.getVuserdef9());
		allocateVO.setVuserdef10(agreeVO.getVuserdef10());
		allocateVO.setPk_org_v(agreeVO.getPk_org_v());
		allocateVO.setPk_group(agreeVO.getPk_group());
		allocateVO.setStatus(2);
		String pk_org_r_v = SFOrgUtil.getOrgVidByOrgPK(agreeBvos[0].getPk_financeorg_r());

		for (int j = 0; j < agreeBvos.length; j++) {
			if ((agreeBvos[j].getIsagree().equals(IAllocateAgreeConst.AgreeMode_Agree)) && ((agreeBvos[j].getRecordstatus().equals(IAllocateAgreeConst.RecordStatus_PARTALLOCATE)) || (agreeBvos[j].getRecordstatus().equals(IAllocateAgreeConst.RecordStatus_WaitALLOCATE)))) {

				AllocateBVO allocateBvo = new AllocateBVO();
				allocateBvo.setSrcbillcode(agreeVO.getVbillno());
				allocateBvo.setPk_srcbill(agreeBvos[j].getPk_allocateagree_b());
				allocateBvo.setSrcbilltype(agreeVO.getPk_billtypeid());
				allocateBvo.setPk_srcbillhead(agreeVO.getPk_allocateagree_h());
				allocateBvo.setRecmodul("SF");
				allocateBvo.setPk_balatype(agreeBvos[j].getPk_balatype());
				allocateBvo.setFundtype(agreeBvos[j].getFundtype());
				allocateBvo.setPk_bankacc_p(agreeBvos[j].getPk_bankacc_p());
				allocateBvo.setBankname_p(agreeBvos[j].getBankname_p());
				allocateBvo.setPk_planitem_p(agreeBvos[j].getPk_planitem_p());
				allocateBvo.setPk_org_r(agreeBvos[j].getPk_financeorg_r());
				allocateBvo.setPk_org_r_v(pk_org_r_v);
				allocateBvo.setPk_accid_r(agreeBvos[j].getPk_accid());
				allocateBvo.setPk_planitem_r(agreeBvos[j].getPk_planitem_r());
				allocateBvo.setPk_bankacc_r(agreeBvos[j].getPk_bankacc_r());
				allocateBvo.setBankacccode_r(agreeBvos[j].getBankacccode_r());
				allocateBvo.setBankaccname_r(agreeBvos[j].getBankaccname_r());
				allocateBvo.setBankname_r(agreeBvos[j].getBankname_r());
				if (agreeBvos[j].getPayamount() != null) {
					allocateBvo.setAmount(agreeBvos[j].getAgreeamount().sub(agreeBvos[j].getPayamount()));
				} else {
					allocateBvo.setAmount(agreeBvos[j].getAgreeamount());
				}
				allocateBvo.setRecordstatus(IAllocateConst.RecordStatus_Normal);
				allocateBvo.setPaystatus(IAllocateConst.PayStatus_WaitCommit);
				allocateBvo.setIsnetpay(agreeBvos[j].getIsnetpay());
				allocateBvo.setPaytype(agreeBvos[j].getPaytype());
				allocateBvo.setVuserdef1(agreeBvos[j].getVuserdef1());
				allocateBvo.setVuserdef2(agreeBvos[j].getVuserdef2());
				allocateBvo.setVuserdef3(agreeBvos[j].getVuserdef3());
				allocateBvo.setVuserdef4(agreeBvos[j].getVuserdef4());
				allocateBvo.setVuserdef5(agreeBvos[j].getVuserdef5());
				allocateBvo.setVuserdef6(agreeBvos[j].getVuserdef6());
				allocateBvo.setVuserdef7(agreeBvos[j].getVuserdef7());
				allocateBvo.setVuserdef8(agreeBvos[j].getVuserdef8());
				allocateBvo.setVuserdef9(agreeBvos[j].getVuserdef9());
				allocateBvo.setVuserdef10(agreeBvos[j].getVuserdef10());
				allocateBvo.setRemark(agreeBvos[j].getRemark());
				allocateBvo.setPk_srcbill(agreeBvos[j].getPk_allocateagree_b());
				allocateBvo.setSrcbillcode(agreeVO.getVbillno());
				allocateBvos[j] = allocateBvo;
			}
		}
		allocateVO.setSubmituser(agreeVO.getSubmituser());
		allocateVO.setSubmitdate(agreeVO.getBusidate());
		allocateVO.setSubmittime(new UFDateTime());
		allocateVO.setBillmaker("NC_USER0000000000000");
		allocateVO.setDbilldate(agreeVO.getBusidate());
		allocateVO.setDbilltime(new UFDateTime());
		allocateVO.setCreator("NC_USER0000000000000");
		allocateVO.setCreationtime(new UFDateTime());

		allocateVO.setBusidate(agreeVO.getBusidate());

		aggAllocateVo.setParent(allocateVO);
		if (ArrayUtil.isNull(allocateBvos)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0240"));
		}
		aggAllocateVo.setChildrenVO((CircularlyAccessibleValueObject[]) ArrayUtil.shrinkArray(allocateBvos));
		AllocatePubUtil.setBillDateTimeByBusiDate(aggAllocateVo, "acceptdate", null);

		AllocateAmountUtil.setAllocateRateAndAmount(aggAllocateVo);

		return aggAllocateVo;
	}

	public static AggAllocateVO converToAllocateVO(AggAllocateAgreeVO[] aggAgreeVos) throws BusinessException {
		AggAllocateVO aggAllocateVo = new AggAllocateVO();
		AllocateVO allocateVO = new AllocateVO();
		List<AllocateBVO> bvoList = new ArrayList();
		int count = 0;
		for (int i = 0; i < aggAgreeVos.length; i++) {
			AllocateAgreeVO agreeVO = (AllocateAgreeVO) aggAgreeVos[i].getParentVO();

			AllocateAgreeBVO[] agreeBvos = (AllocateAgreeBVO[]) aggAgreeVos[i].getChildrenVO();

			allocateVO.setRecmodul("SF");
			allocateVO.setSrcbusitype(IAllocateConst.SrcBusiType_Apply);
			if ((allocateVO.getPk_currtype() != null) && (!agreeVO.getPk_currtype().equals(allocateVO.getPk_currtype()))) {

				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0241"));
			}
			allocateVO.setPk_currtype(agreeVO.getPk_currtype());

			if ((allocateVO.getBusitype() != null) && (!agreeVO.getBusitype().equals(allocateVO.getBusitype()))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0242"));
			}
			allocateVO.setBusitype(agreeVO.getBusitype());
			allocateVO.setPk_billtype("36K2");
			allocateVO.setSrcbilltype("36K7Z600000000000000");
			if ((allocateVO.getPk_org() != null) && (!agreeVO.getPk_org().equals(allocateVO.getPk_org()))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0243"));
			}
			allocateVO.setPk_org(agreeVO.getPk_org());
			allocateVO.setPk_org_v(SFOrgUtil.getOrgVidByOrgPK(agreeVO.getPk_org()));

			allocateVO.setPk_group(agreeVO.getPk_group());
			allocateVO.setStatus(2);

			for (int j = 0; j < agreeBvos.length; j++) {
				if ((agreeBvos[j].getRecordstatus().intValue() == IAllocateAgreeConst.RecordStatus_PARTALLOCATE.intValue()) || (agreeBvos[j].getRecordstatus().intValue() == IAllocateAgreeConst.RecordStatus_WaitALLOCATE.intValue())) {
					AllocateBVO allocateBvo = new AllocateBVO();
					allocateBvo.setSrcbillcode(agreeVO.getVbillno());
					allocateBvo.setPk_srcbill(agreeBvos[j].getPk_allocateagree_b());

					allocateBvo.setPk_srcbillhead(agreeVO.getPk_allocateagree_h());

					allocateBvo.setSrcbilltype(agreeVO.getPk_billtypeid());
					allocateBvo.setRecmodul("SF");
					allocateBvo.setPk_balatype(agreeBvos[j].getPk_balatype());
					allocateBvo.setFundtype(agreeBvos[j].getFundtype());
					allocateBvo.setPk_bankacc_p(agreeBvos[j].getPk_bankacc_p());
					allocateBvo.setBankname_p(agreeBvos[j].getBankname_p());
					allocateBvo.setPk_org_r(agreeBvos[j].getPk_financeorg_r());
					allocateBvo.setPk_org_r_v(SFOrgUtil.getOrgVidByOrgPK(agreeBvos[j].getPk_financeorg_r()));

					allocateBvo.setPk_accid_r(agreeBvos[j].getPk_accid());
					allocateBvo.setPk_planitem_r(agreeBvos[j].getPk_planitem_r());

					allocateBvo.setPk_bankacc_r(agreeBvos[j].getPk_bankacc_r());
					allocateBvo.setBankacccode_r(agreeBvos[j].getBankacccode_r());
					allocateBvo.setBankaccname_r(agreeBvos[j].getBankaccname_r());

					allocateBvo.setBankname_r(agreeBvos[j].getBankname_r());

					if (agreeBvos[j].getPayamount() != null) {
						allocateBvo.setAmount(agreeBvos[j].getAgreeamount().sub(agreeBvos[j].getPayamount()));
					} else {
						allocateBvo.setAmount(agreeBvos[j].getAgreeamount());
					}
					allocateBvo.setIsnetpay(agreeBvos[j].getIsnetpay());
					allocateBvo.setPaytype(agreeBvos[j].getPaytype());
					allocateBvo.setRemark(agreeBvos[j].getRemark());
					allocateBvo.setPk_srcbill(agreeBvos[j].getPk_allocateagree_b());

					allocateBvo.setSrcbillcode(agreeVO.getVbillno());
					allocateBvo.setPaystatus(IAllocateConst.PayStatus_WaitCommit);

					allocateBvo.setRecordstatus(IAllocateConst.RecordStatus_Normal);

					agreeBvos[j].setRecordstatus(IAllocateAgreeConst.RecordStatus_ALLOCATEOK);

					agreeBvos[j].setPayamount(agreeBvos[j].getAgreeamount());
					bvoList.add(allocateBvo);
					count++;
				}
			}

			allocateVO.setSubmitdate(new UFDate());
			allocateVO.setSubmittime(new UFDateTime());
			allocateVO.setBillmaker("NC_USER0000000000000");
			allocateVO.setDbilldate(new UFDate());
			allocateVO.setDbilltime(new UFDateTime());
			allocateVO.setCreator("NC_USER0000000000000");
			allocateVO.setCreationtime(new UFDateTime());
			allocateVO.setBusidate(agreeVO.getBusidate());
			allocateVO.setIsreversebustype(UFBoolean.FALSE);
			allocateVO.setIsmakevoucher(UFBoolean.FALSE);
			allocateVO.setIsreversebustype(UFBoolean.FALSE);
		}

		aggAllocateVo.setParent(allocateVO);
		aggAllocateVo.getHead().setStroke(Integer.valueOf(count));
		aggAllocateVo.setChildrenVO((CircularlyAccessibleValueObject[]) bvoList.toArray(new AllocateBVO[count]));
		AllocatePubUtil.setBillDateTimeByBusiDate(aggAllocateVo, "acceptdate", null);

		AllocateAmountUtil.setAllocateRateAndAmount(aggAllocateVo);

		return aggAllocateVo;
	}

	public static AggAllocateApplyVO converToAllocateApplyVO(AggAllocateAgreeVO aggAgreeVO, int flag) {
		AllocateAgreeVO agreeVO = aggAgreeVO.getHead();
		AllocateAgreeBVO[] agreeBvos = aggAgreeVO.getItem();
		AggAllocateApplyVO aggApplyVo = new AggAllocateApplyVO();
		AllocateApplyVO applyVO = new AllocateApplyVO();
		AllocateApplyBVO[] applyBvos = new AllocateApplyBVO[agreeBvos.length];

		applyVO.setPk_currtype(agreeVO.getPk_currtype());
		applyVO.setStroke(agreeVO.getStroke());
		applyVO.setBusitype(agreeVO.getBusitype());
		applyVO.setPk_billtype("36K1");
		applyVO.setPk_allocateapply_h(agreeVO.getPk_srcbill());

		boolean agreeFlag = true;
		for (int j = 0; j < agreeBvos.length; j++) {
			AllocateApplyBVO applyBvo = new AllocateApplyBVO();
			applyBvo.setPk_balatype(agreeBvos[j].getPk_balatype());
			applyBvo.setFundtype(agreeBvos[j].getFundtype());
			applyBvo.setPk_bankacc_p(agreeBvos[j].getPk_bankacc_p());
			applyBvo.setBankname_p(agreeBvos[j].getBankname_p());
			applyBvo.setPk_financeorg_r(agreeBvos[j].getPk_financeorg_r());
			applyBvo.setPk_financeorg_r_v(agreeBvos[j].getPk_financeorg_r_v());
			applyBvo.setPk_accid(agreeBvos[j].getPk_accid());
			applyBvo.setPk_planitem_r(agreeBvos[j].getPk_planitem_r());
			applyBvo.setPk_bankacc_r(agreeBvos[j].getPk_bankacc_r());
			applyBvo.setBankname_r(agreeBvos[j].getBankname_r());

			applyBvo.setAgreeamount(agreeBvos[j].getAgreeamount());
			applyBvo.setAgreetype(agreeBvos[j].getAgreetype());
			if (!agreeBvos[j].getAgreetype().equals(IAllocateAgreeConst.AgreeType_ALL)) {
				agreeFlag = false;
			}
			if (flag == 0) {
				applyBvo.setAgreeamount(null);
				applyBvo.setAgreetype(null);
			}

			applyBvo.setRemark(agreeBvos[j].getRemark());
			applyBvo.setPk_allocateapply_b(agreeBvos[j].getPk_srcbill());
			applyBvo.setPk_allocateapply_h(agreeVO.getPk_srcbill());
			applyBvos[j] = applyBvo;
		}
		if (flag == 1) {
			applyVO.setBillstatus(IAllocateApplyConst.BILLSTATUS_AGREEALL);
		} else {
			applyVO.setBillstatus(IAllocateApplyConst.BILLSTATUS_WAITAGREE);
		}
		aggApplyVo.setParent(applyVO);
		aggApplyVo.setChildrenVO(applyBvos);

		return aggApplyVo;
	}

	public static OnlinePaymentVO[] convertToOnlinePaymentVO(AggAllocateAgreeVO billvo, boolean checkIsFull) throws BusinessException {
		AllocateAgreeVO parentvo = (AllocateAgreeVO) billvo.getParentVO();
		String pk_billtype = parentvo.getPk_billtype();
		String sourcemoudeCode = "sf";
		String pk_currtype = parentvo.getPk_currtype();
		String pk_submiter = parentvo.getSubmituser();
		AllocateAgreeBVO[] childVos = (AllocateAgreeBVO[]) billvo.getChildrenVO();

		OnlinePaymentVO[] netpayvos = new OnlinePaymentVO[childVos.length];

		for (int i = 0; i < netpayvos.length; i++) {
			if ((checkIsFull) && ((childVos[i].getIsnetbankfull() == null) || (!childVos[i].getIsnetbankfull().booleanValue()))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0193"));
			}

			netpayvos[i] = new OnlinePaymentVO();
			netpayvos[i].setCurrency(pk_currtype);

			if (StringUtil.isNull(childVos[i].getPk_bankacc_p())) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0244"));
			}
			netpayvos[i].setDbtaccPk(childVos[i].getPk_bankacc_p());

			BankAccbasVO[] bankaccs_p =
					AllocateAgreeServiceProxy.getBankaccPubQueryService().queryBankaccsByPks(new String[] { childVos[i].getPk_bankacc_p() });

			if ((bankaccs_p != null) && (bankaccs_p.length > 0)) {
				netpayvos[i].setDbtacc(bankaccs_p[0].getAccnum());
				netpayvos[i].setDbtname(bankaccs_p[0].getAccname());
			}

			if (StringUtil.isNull(childVos[i].getPk_bankacc_r())) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632allocation_0", "03632allocation-0245"));
			}
			netpayvos[i].setCrtaccPk(childVos[i].getPk_bankacc_r());
			BankAccbasVO[] bankaccs_r =
					AllocateServiceProxy.getBankaccPubQueryService().queryBankaccsByPks(new String[] { childVos[i].getPk_bankacc_r() });

			if ((bankaccs_r != null) && (bankaccs_r.length > 0)) {
				netpayvos[i].setCrtacc(bankaccs_r[0].getAccnum());
				netpayvos[i].setCrtname(bankaccs_r[0].getAccname());
				netpayvos[i].setCrtbank(bankaccs_r[0].getPk_bankdoc());
			}

			netpayvos[i].setTrsamt(childVos[i].getAgreeamount());
			netpayvos[i].setYurref(childVos[i].getPrimaryKey());

			netpayvos[i].setBilltype(pk_billtype);
			netpayvos[i].setModulecode(sourcemoudeCode);
			netpayvos[i].setFunc(FuncTypeConst.ZF);
			netpayvos[i].setPk_org(parentvo.getPk_org());
		}
		return netpayvos;
	}
}