package nc.vo.sf.delivery.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import nc.itf.sf.delivery.proxy.DeliveryServiceProxy;
import nc.itf.sf.deliveryapply.proxy.DeliveryApplyServiceProxy;
import nc.pubitf.org.IOrgUnitPubService;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.BusinessRuntimeException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.vo.sf.delivery.AggDeliveryVO;
import nc.vo.sf.delivery.DeliveryBVO;
import nc.vo.sf.delivery.DeliveryVO;
import nc.vo.sf.delivery.IDeliveryConst;
import nc.vo.sf.deliveryreceipt.AggDeliveryReceiptVO;
import nc.vo.sf.deliveryreceipt.DeliveryReceiptVO;
import nc.vo.sf.deliveryreceipt.IDeliveryReceiptConst;
import nc.vo.tmpub.util.ArrayUtil;
import nc.vo.tmpub.util.TMCurrencyUtil;
import nc.vo.uap.rbac.constant.INCSystemUserConst;

public class DeliveryToReceiptVOUtil {
	public DeliveryToReceiptVOUtil() {
	}

	public static AggDeliveryReceiptVO[] convertToReceiptVO(AggDeliveryVO deliveryvo) throws BusinessException {
		List<AggDeliveryReceiptVO> receiptArray = new ArrayList();

		DeliveryVO headvo = deliveryvo.getHead();
		DeliveryBVO[] bodyvos = deliveryvo.getItem();

		for (DeliveryBVO bodyvo : bodyvos) {
			if (IDeliveryConst.PayStatus_Payok.equals(bodyvo.getPaystatus())) {

				AggDeliveryReceiptVO receiptvo = new AggDeliveryReceiptVO();
				DeliveryReceiptVO receiptHeadvo = new DeliveryReceiptVO();
				receiptvo.setHead(receiptHeadvo);

				receiptHeadvo.setPk_org(bodyvo.getPk_org_p());
				setPkOrgInfo(receiptHeadvo);

				receiptHeadvo.setPk_billtype(IDeliveryReceiptConst.BillType);// ±‡“Î–ﬁ∏ƒ"36K9"
				receiptHeadvo.setPk_billtypeid(IDeliveryReceiptConst.BillTypeID);// ±‡“Î–ﬁ∏ƒ"1001Z61000000001OF5T"
				receiptHeadvo.setBusitype(headvo.getBusitype());
				receiptHeadvo.setPk_org_r(headvo.getPk_org());
				String vid =
						DeliveryApplyServiceProxy.getOrgUnitPubService().getNewVIDSByOrgIDS(new String[] { headvo.getPk_org() }).get(headvo.getPk_org());

				receiptHeadvo.setPk_org_r_v(vid);
				receiptHeadvo.setIsreverse(headvo.getIsreversebusitype());
				receiptHeadvo.setPk_currtype(headvo.getPk_currtype());
				receiptHeadvo.setPk_balatype(bodyvo.getPk_balatype());
				receiptHeadvo.setFundtype(bodyvo.getFundtype());
				receiptHeadvo.setPk_bankacc_r(bodyvo.getPk_bankacc_r());
				receiptHeadvo.setBankacccode_r(bodyvo.getBankacccode_r());
				receiptHeadvo.setBankaccname_r(bodyvo.getBankaccname_r());
				receiptHeadvo.setPk_bankdoc_r(bodyvo.getBankname_r());
				receiptHeadvo.setPk_planitem(bodyvo.getPk_planitem_r());
				receiptHeadvo.setPk_org_p(bodyvo.getPk_org_p());
				receiptHeadvo.setPk_org_p_v(receiptHeadvo.getPk_org_v());
				receiptHeadvo.setPk_bankacc_p(bodyvo.getPk_bankacc_p());
				receiptHeadvo.setPk_bankdoc_p(bodyvo.getBankname_p());
				receiptHeadvo.setPk_accid_p(bodyvo.getPk_accid());
				receiptHeadvo.setPk_planitem_p(bodyvo.getPk_planitem_p());
				receiptHeadvo.setAmount(bodyvo.getAmount());

				receiptHeadvo.setPaytype(bodyvo.getPay_type());
				receiptHeadvo.setRemark(bodyvo.getRemark());
				receiptHeadvo.setPaydate(bodyvo.getGatheringdate());
				receiptHeadvo.setIstally(UFBoolean.FALSE);
				receiptHeadvo.setIsvoucher(UFBoolean.FALSE);
				receiptHeadvo.setIsunitevoucher(UFBoolean.FALSE);
				receiptHeadvo.setRecmodule(IDeliveryReceiptConst.MoudleCode_SF);// ±‡“Î–ﬁ∏ƒ"SF"
				receiptHeadvo.setSrcbilltype(IDeliveryConst.BillTypeID_DELIVERY);// ±‡“Î–ﬁ∏ƒ"36K4Z600000000000000"
				receiptHeadvo.setSrcbillno(headvo.getVbillno());
				receiptHeadvo.setPk_srcbill(headvo.getPk_delivery_h());
				receiptHeadvo.setPk_srcbill_b(bodyvo.getPk_delivery_b());
				if (IDeliveryConst.SrcBusiType_Apply.equals(headvo.getSrcbusitype())) {
					receiptHeadvo.setPk_deliveryapply_h(headvo.getPk_srcbill());
					receiptHeadvo.setPk_deliveryapply_b(bodyvo.getPk_srcbillrow());
					// 20170417_0 tsy ±£¥Ê…Í«Îµ•∫≈
					receiptHeadvo.setVueserdef5(headvo.getSrcbillcode());
					// 20170417_0 end
				}

				receiptHeadvo.setCreator(INCSystemUserConst.NC_USER_PK);// ±‡“Î–ﬁ∏ƒ"NC_USER0000000000000"
				receiptHeadvo.setCreationtime(bodyvo.getGatheringtime());

				receiptHeadvo.setVueserdef1(bodyvo.getVuserdef1());
				receiptHeadvo.setVueserdef2(bodyvo.getVuserdef2());
				receiptHeadvo.setVueserdef3(bodyvo.getVuserdef3());
				receiptHeadvo.setVueserdef4(bodyvo.getVuserdef4());
				// 20170417_1 tsy ±£¥Ê…Í«Îµ•∫≈
				// receiptHeadvo.setVueserdef5(bodyvo.getVuserdef5());
				// 20170417_1 end
				receiptHeadvo.setVueserdef6(bodyvo.getVuserdef6());
				receiptHeadvo.setVueserdef7(bodyvo.getVuserdef7());
				receiptHeadvo.setVueserdef8(bodyvo.getVuserdef8());
				receiptHeadvo.setVueserdef9(bodyvo.getVuserdef9());
				receiptHeadvo.setVueserdef10(bodyvo.getVuserdef10());

				receiptHeadvo.setBankrelatcode(bodyvo.getBankrelatcode());

				receiptHeadvo.setApprover(headvo.getApprover());
				fillAmountFiledsAndRate(receiptHeadvo);
				receiptArray.add(receiptvo);
			}
		}
		return (AggDeliveryReceiptVO[]) receiptArray.toArray(new AggDeliveryReceiptVO[0]);
	}

	private static void setPkOrgInfo(DeliveryReceiptVO receiptHeadvo) throws BusinessException {
		try {
			OrgVO[] orgVOs =
					DeliveryServiceProxy.getOrgUnitPubService().getOrgs(new String[] { receiptHeadvo.getPk_org() }, new String[] { "pk_org", "pk_vid", "pk_group" });

			if (ArrayUtil.isNull(orgVOs)) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632delivery_0", "03632delivery-0204"));
			}
			receiptHeadvo.setPk_org_v(orgVOs[0].getPk_vid());
			receiptHeadvo.setPk_group(orgVOs[0].getPk_group());
		} catch (BusinessException e) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("3632delivery_0", "03632delivery-0205"));
		}
	}

	public static void fillAmountFiledsAndRate(DeliveryReceiptVO headVO) {
		String pkOrg = headVO.getPk_org();
		String pkGroup = headVO.getPk_group();
		String pkCurrtype = headVO.getPk_currtype();
		nc.vo.pub.lang.UFDate exchangeDate = headVO.getPaydate();

		try {
			UFDouble currOlcrate = TMCurrencyUtil.getOrgCurrRate(pkOrg, pkCurrtype, exchangeDate);

			UFDouble currGlcrate = TMCurrencyUtil.getGroupCurrRate(pkGroup, pkOrg, pkCurrtype, exchangeDate);

			UFDouble currGllcrate = TMCurrencyUtil.getGlobalCurrRate(pkOrg, pkCurrtype, exchangeDate);

			UFDouble amount = headVO.getAmount();
			if (currOlcrate != null) {
				headVO.setOlcrate(currOlcrate);
				headVO.setOlcamount(TMCurrencyUtil.getOrgLocalMoney(pkOrg, pkCurrtype, amount, currOlcrate, exchangeDate));
			}

			if (currGlcrate != null) {
				headVO.setGlcrate(currGlcrate);
				headVO.setGlcamount(TMCurrencyUtil.getGroupLocalMoney(pkGroup, pkOrg, pkCurrtype, amount, currGlcrate, currOlcrate, exchangeDate));
			}

			if (currGllcrate != null) {
				headVO.setGllcrate(currGllcrate);
				headVO.setGllcamount(TMCurrencyUtil.getGlobalLocalMoney(pkOrg, pkCurrtype, amount, currGllcrate, currOlcrate, exchangeDate));
			}
		} catch (BusinessException e1) {
			nc.bs.logging.Logger.error(e1.getMessage(), e1);
			throw new BusinessRuntimeException(e1.getMessage());
		}
	}
}