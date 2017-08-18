package nc.bs.obm.ebankconfirmpay.ace.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import nc.bs.framework.common.NCLocator;
import nc.impl.pubapp.pattern.rule.IRule;
import nc.impl.tmpub.pattern.template.BaseAdapterVO;
import nc.itf.obm.ebanklog.IEbankLogQueryService;
import nc.itf.obm.payroll.IEbankdfgzQuery;
import nc.pubitf.org.IOrgUnitPubService;
import nc.ui.obm.proxy.OBMClientServiceProxy;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.obm.ebankconfirmpay.AggConfirmPayHVO;
import nc.vo.obm.ebankconfirmpay.ConfirmPayBVO;
import nc.vo.obm.ebankconfirmpay.ConfirmPayHVO;
import nc.vo.obm.ebankpaylog.EBankPayLogAggVO;
import nc.vo.obm.ebankpaylog.EBankPayLogHVO;
import nc.vo.obm.ebankpaylog.EBankPayLogVO;
import nc.vo.obm.ebankpaylog.FuncEnum;
import nc.vo.obm.ebankpaylog.PaystaeEnum;
import nc.vo.obm.log.ObmLog;
import nc.vo.obm.ml.MLObm;
import nc.vo.obm.obmvo.PmtconfirmAddVO;
import nc.vo.obm.payroll.DfgzBVO;
import nc.vo.obm.payroll.DfgzBillVO;
import nc.vo.obm.payroll.DfgzHVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.BusinessRuntimeException;
import nc.vo.pub.ISuperVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pubapp.AppContext;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.tmpub.util.ArrayUtil;

public class Convert2ConfirmPayVORule implements IRule<BaseAdapterVO> {
	private String dfgzNode = "36C9";

	public Convert2ConfirmPayVORule() {
	}

	public void process(BaseAdapterVO[] vos) {
		if (ArrayUtil.isNull(vos))
			return;
		PmtconfirmAddVO[] addVOs = (PmtconfirmAddVO[]) vos[0].getParamVO();
		String strNodeCode = addVOs[0].getSrcbilltype();

		if (this.dfgzNode.equalsIgnoreCase(strNodeCode)) {
			dfgz2ConfirmVO(vos);
		} else {
			payLog2ConfirmVO(vos);
		}
	}

	public void dfgz2ConfirmVO(BaseAdapterVO[] vos) {
		IEbankdfgzQuery query = (IEbankdfgzQuery) NCLocator.getInstance().lookup(IEbankdfgzQuery.class);

		PmtconfirmAddVO[] addvos = (PmtconfirmAddVO[]) vos[0].getParamVO();
		PmtconfirmAddVO addvo = addvos[0];
		if (ArrayUtil.isNull(addvos))
			return;
		String yurref = addvos[0].getYurref();
		DfgzBillVO[] bills = null;
		try {
			bills = query.queryDfgzLogVoByYuref(yurref);
		} catch (BusinessException e) {
			ExceptionUtils.wrappBusinessException(e.getMessage());
		}
		if ((bills == null) || (bills.length == 0)) {
			ExceptionUtils.wrappBusinessException(MLObm.getStr00864());
		}
		if (bills.length > 1) {
			ExceptionUtils.wrappBusinessException(MLObm.getStr00131());
		}
		DfgzBillVO bill = bills[0];
		AggConfirmPayHVO resultBill = new AggConfirmPayHVO();
		ConfirmPayHVO parent = new ConfirmPayHVO();
		DfgzHVO dfgzhvo = (DfgzHVO) bill.getParent();

		if (StringUtil.isEmptyWithTrim(addvo.getPk_group())) {
			parent.setPk_group(AppContext.getInstance().getPkGroup());
		} else {
			parent.setPk_group(addvo.getPk_group());
		}
		parent.setPk_org(addvo.getPk_org());
		setPk_Org_V(parent);

		parent.setYurref(addvo.getYurref());
		parent.setReconciliationcode(dfgzhvo.getReconciliationcode());

		parent.setDbtrbank(dfgzhvo.getBbknbr());
		parent.setPk_banktype(dfgzhvo.getPk_banktype());
		parent.setDbtracc(dfgzhvo.getDbtacc());
		parent.setDbtraccname(dfgzhvo.getDbtaccname());
		parent.setPk_bankaccbas(dfgzhvo.getPk_bankaccsub());
		parent.setPk_bankaccsub(dfgzhvo.getPk_bankaccsub());
		parent.setSourcesys(addvo.getSrcsystem());
		parent.setSourcefuncode(addvo.getSrcnodecode());
		parent.setSourcebilltype(addvo.getSrcbilltype());
		parent.setSourcebillpk(addvo.getSrcpkid());
		parent.setSourcebillno(addvo.getSrcbillcode());
		parent.setBillstate(Integer.valueOf(-1));

		parent.setBilldate(AppContext.getInstance().getBusiDate());

		parent.setBillmaker(AppContext.getInstance().getPkUser());

		String paylogpk = dfgzhvo.getPk_dfgz();

		parent.setPaylogpk(paylogpk);

		DfgzBVO[] dfgzChildren = (DfgzBVO[]) bill.getChildren(DfgzBVO.class);
		List<ConfirmPayBVO> children = new ArrayList();
		for (DfgzBVO bvo : dfgzChildren) {
			Integer payState = bvo.getPaystate();
			if (payState.equals(Integer.valueOf(PaystaeEnum.PAY_UNKNOWN.toIntValue()))) {

				ConfirmPayBVO payBVO = new ConfirmPayBVO();
				payBVO.setSourcebillpk(bvo.getPk_dfgz_b());
				payBVO.setCdtracc(bvo.getCrtacc());
				payBVO.setCdtraccname(bvo.getCrtaccname());
				payBVO.setCdtrbranchname(bvo.getCrtbankname());
				UFDate paydate = bvo.getPaydate() == null ? null : UFDate.getDate(bvo.getPaydate());

				payBVO.setPaydate(paydate);
				payBVO.setAcceptstate(bvo.getPaystate());
				payBVO.setPaystate(bvo.getPaystate());
				payBVO.setErrmsg(bvo.getErrmsg());
				payBVO.setTransmnt(bvo.getAmount());
				payBVO.setCurrtype(bvo.getPk_currtype());
				payBVO.setYurref(parent.getYurref());
				children.add(payBVO);
			}
		}
		if (children.size() == 0)
			ExceptionUtils.wrappBusinessException(MLObm.getStr00864());
		resultBill.setParent(parent);
		resultBill.setChildren(ConfirmPayBVO.class, (ISuperVO[]) children.toArray(new ConfirmPayBVO[0]));

		vos[0].setResultVO(resultBill);
	}

	public void payLog2ConfirmVO(BaseAdapterVO[] vos) {
		PmtconfirmAddVO[] addVOs = (PmtconfirmAddVO[]) vos[0].getParamVO();
		PmtconfirmAddVO addvo = addVOs[0];
		EBankPayLogAggVO paylogaggvos = null;
		try {
			IEbankLogQueryService queryService = OBMClientServiceProxy.getEbankLogQueryService();

			paylogaggvos = queryService.queryPaylogAggVOsByYurref(addvo.getYurref());
		} catch (BusinessException e) {
			ExceptionUtils.wrappBusinessException(e.getMessage());
		}

		if (paylogaggvos == null) {
			ExceptionUtils.wrappBusinessException(MLObm.getStr00866(new String[] { addvo.getYurref() }));
		}

		AggConfirmPayHVO bill = new AggConfirmPayHVO();

		ArrayList<ConfirmPayBVO> children = new ArrayList();

		EBankPayLogHVO parentVO = (EBankPayLogHVO) paylogaggvos.getParentVO();
		EBankPayLogVO[] paylogs = (EBankPayLogVO[]) paylogaggvos.getChildren(EBankPayLogVO.class);

		if (FuncEnum.PROVIDE_DEDUCT.toStringValue().equals(paylogs[0].getFunc())) {
			
			int tempi = 0;
			for (EBankPayLogVO payLog : paylogs) {
				if ((payLog.getPaystate() != null) && (Integer.valueOf(PaystaeEnum.PAY_UNKNOWN.toIntValue()).equals(payLog.getPaystate()))) {

					ConfirmPayBVO payBVO = new ConfirmPayBVO();
					payBVO.setSourcebillpk(payLog.getPk_ebank_paylog());
					payBVO.setCdtracc(payLog.getCrtacc());
					payBVO.setCdtraccname(payLog.getCrtaccname());
					payBVO.setCdtrbranchname(payLog.getCrtbranchname());
					payBVO.setPaydate(payLog.getSenddate());
					payBVO.setAcceptstate(payLog.getPaystate());
					payBVO.setPaystate(payLog.getPaystate());
					payBVO.setErrmsg(payLog.getPayerrmsg());
					payBVO.setTransmnt(payLog.getTrsamt());
					payBVO.setYurref(payLog.getYurref());
					payBVO.setCurrtype(payLog.getC_ccynbr());
					// 20170503 tsy Ôö¼ÓÐÐºÅ
					payBVO.setRowno(++tempi+"");
					// 20170503 end
					children.add(payBVO);
				}
			}

			// for (int i = 0; i < paylogs.length; i++) {
			// if ((paylogs[i].getPaystate() != null) &&
			// (Integer.valueOf(PaystaeEnum.PAY_UNKNOWN.toIntValue()).equals(paylogs[i].getPaystate())))
			// {
			//
			// ConfirmPayBVO payBVO = new ConfirmPayBVO();
			// payBVO.setSourcebillpk(paylogs[i].getPk_ebank_paylog());
			// payBVO.setCdtracc(paylogs[i].getCrtacc());
			// payBVO.setCdtraccname(paylogs[i].getCrtaccname());
			// payBVO.setCdtrbranchname(paylogs[i].getCrtbranchname());
			// payBVO.setPaydate(paylogs[i].getSenddate());
			// payBVO.setAcceptstate(paylogs[i].getPaystate());
			// payBVO.setPaystate(paylogs[i].getPaystate());
			// payBVO.setErrmsg(paylogs[i].getPayerrmsg());
			// payBVO.setTransmnt(paylogs[i].getTrsamt());
			// payBVO.setYurref(paylogs[i].getYurref());
			// payBVO.setCurrtype(paylogs[i].getC_ccynbr());
			// //2017
			// payBVO.setRowno(i + 1 + "");
			// children.add(payBVO);
			// }
			// }
			ConfirmPayHVO payHVO = new ConfirmPayHVO();

			if (StringUtil.isEmptyWithTrim(parentVO.getPk_group())) {
				payHVO.setPk_group(AppContext.getInstance().getPkGroup());
			} else {
				payHVO.setPk_group(parentVO.getPk_group());
			}

			payHVO.setPk_org(parentVO.getPk_org());
			setPk_Org_V(payHVO);
			payHVO.setYurref(paylogs[0].getYurref());
			payHVO.setReconciliationcode(paylogs[0].getReconciliationcode());
			payHVO.setDbtrbank(paylogs[0].getDbtbranchname());
			payHVO.setPk_banktype(paylogs[0].getBanktype());
			payHVO.setDbtracc(paylogs[0].getDbtacc());
			payHVO.setDbtraccname(paylogs[0].getDbtaccname());
			payHVO.setPk_bankaccbas(paylogs[0].getPk_bankaccbas());
			payHVO.setPk_bankaccsub(paylogs[0].getPk_bankaccsub());

			payHVO.setPaylogpk(parentVO.getPk_ebank_paylog_h());
			if (!StringUtil.isEmptyWithTrim(addvo.getSrcsystem())) {
				payHVO.setSourcesys(addvo.getSrcsystem());
			} else {
				payHVO.setSourcesys(parentVO.getSrcsystem());
			}
			payHVO.setSourcefuncode(addvo.getSrcnodecode());
			if (!StringUtil.isEmptyWithTrim(addvo.getSrcbilltype())) {
				payHVO.setSourcebilltype(addvo.getSrcbilltype());
			} else {
				payHVO.setSourcebilltype(parentVO.getSrcbilltype());
			}

			if ("OBM".equals(payHVO.getSourcesys())) {
				payHVO.setSourcebillpk(parentVO.getPk_ebank_paylog_h());
				payHVO.setSourcebillno(parentVO.getHeadpackageid());
			} else {
				payHVO.setSourcebillpk(parentVO.getSrcpkid());
				payHVO.setSourcebillno(parentVO.getSrcbillcode());
			}
			payHVO.setSourcebillno(parentVO.getSrcbillcode());
			payHVO.setBillstate(Integer.valueOf(-1));

			payHVO.setBilldate(AppContext.getInstance().getBusiDate());

			payHVO.setBillmaker(AppContext.getInstance().getPkUser());

			if (!StringUtil.isEmptyWithTrim(addvo.getPk_ebank_paylog())) {
				payHVO.setAttributeValue("Pk_ebank_paylog", addvo.getPk_ebank_paylog());
			}

			bill.setParent(payHVO);
		} else {
			EBankPayLogVO logVO = new EBankPayLogVO();
			for (EBankPayLogVO payLog : paylogs) {
				if (addvo.getYurref().equals(payLog.getYurref())) {

					if ((payLog.getPaystate() != null) && (Integer.valueOf(PaystaeEnum.PAY_UNKNOWN.toIntValue()).equals(payLog.getPaystate()))) {

						logVO = payLog;
						ConfirmPayBVO payBVO = new ConfirmPayBVO();
						payBVO.setSourcebillpk(payLog.getPk_ebank_paylog());
						payBVO.setCdtracc(payLog.getCrtacc());
						payBVO.setCdtraccname(payLog.getCrtaccname());
						payBVO.setCdtrbranchname(payLog.getCrtbranchname());
						payBVO.setPaydate(payLog.getSenddate());
						payBVO.setAcceptstate(payLog.getPaystate());
						payBVO.setPaystate(payLog.getPaystate());
						payBVO.setErrmsg(payLog.getPayerrmsg());
						payBVO.setTransmnt(payLog.getTrsamt());
						payBVO.setYurref(payLog.getYurref());
						payBVO.setCurrtype(payLog.getC_ccynbr());
						children.add(payBVO);
						break;
					}
				}
			}
			ConfirmPayHVO payHVO = new ConfirmPayHVO();
			if (StringUtil.isEmptyWithTrim(parentVO.getPk_group())) {
				payHVO.setPk_group(AppContext.getInstance().getPkGroup());
			} else {
				payHVO.setPk_group(parentVO.getPk_group());
			}

			payHVO.setPk_org(parentVO.getPk_org());
			setPk_Org_V(payHVO);
			payHVO.setYurref(logVO.getYurref());
			payHVO.setReconciliationcode(logVO.getReconciliationcode());
			payHVO.setDbtrbank(logVO.getDbtbranchname());
			payHVO.setPk_banktype(logVO.getBanktype());
			payHVO.setDbtracc(logVO.getDbtacc());
			payHVO.setDbtraccname(logVO.getDbtaccname());
			payHVO.setPk_bankaccbas(logVO.getPk_bankaccbas());
			payHVO.setPk_bankaccsub(logVO.getPk_bankaccsub());

			payHVO.setPaylogpk(parentVO.getPk_ebank_paylog_h());
			if (!StringUtil.isEmptyWithTrim(addvo.getSrcsystem())) {
				payHVO.setSourcesys(addvo.getSrcsystem());
			} else {
				payHVO.setSourcesys(parentVO.getSrcsystem());
			}
			payHVO.setSourcefuncode(addvo.getSrcnodecode());
			if (!StringUtil.isEmptyWithTrim(addvo.getSrcbilltype())) {
				payHVO.setSourcebilltype(addvo.getSrcbilltype());
			} else {
				payHVO.setSourcebilltype(parentVO.getSrcbilltype());
			}

			if ("OBM".equals(payHVO.getSourcesys())) {
				payHVO.setSourcebillpk(parentVO.getPk_ebank_paylog_h());
				payHVO.setSourcebillno(parentVO.getHeadpackageid());
			} else {
				payHVO.setSourcebillpk(parentVO.getSrcpkid());
				payHVO.setSourcebillno(parentVO.getSrcbillcode());
			}
			payHVO.setBillstate(Integer.valueOf(-1));

			payHVO.setBilldate(AppContext.getInstance().getBusiDate());

			payHVO.setBillmaker(AppContext.getInstance().getPkUser());

			if (!StringUtil.isEmptyWithTrim(addvo.getPk_ebank_paylog())) {
				payHVO.setAttributeValue("Pk_ebank_paylog", addvo.getPk_ebank_paylog());
			}

			bill.setParent(payHVO);
		}

		if (children.size() == 0)
			ExceptionUtils.wrappBusinessException(MLObm.getStr00868());
		bill.setChildren(ConfirmPayBVO.class, (ISuperVO[]) children.toArray(new ConfirmPayBVO[0]));

		vos[0].setResultVO(bill);
	}

	private void setPk_Org_V(ConfirmPayHVO vo) throws BusinessRuntimeException {
		try {
			String pk_org = vo.getPk_org();
			UFDate bizDate = AppContext.getInstance().getBusiDate();
			IOrgUnitPubService orgUnitSrv = (IOrgUnitPubService) NCLocator.getInstance().lookup(IOrgUnitPubService.class);

			Map<String, String> orgVersionMap = orgUnitSrv.getNewVIDSByOrgIDSAndDate(new String[] { pk_org }, bizDate);

			String pk_org_v = (String) orgVersionMap.get(pk_org);
			if (pk_org_v == null) {
				ExceptionUtils.wrappBusinessException(MLObm.getStr00403());
			}

			vo.setPk_org_v(pk_org_v);
		} catch (BusinessException ex) {
			ObmLog.error(ex.getMessage(), ex, getClass(), "setPk_Org_V");

			throw new BusinessRuntimeException(ex.getMessage());
		}
	}
}