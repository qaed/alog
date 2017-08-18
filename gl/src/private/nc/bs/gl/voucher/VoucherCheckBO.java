package nc.bs.gl.voucher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.NamingException;

import nc.bd.accperiod.InvalidAccperiodExcetion;
import nc.bs.framework.common.NCLocator;
import nc.bs.glcom.ass.assitem.cache.AccAssItemCache;
import nc.bs.logging.Log;
import nc.bs.logging.Logger;
import nc.bs.ml.NCLangResOnserver;
import nc.bs.pub.SystemException;
import nc.gl.utils.AccasoaUtils;
import nc.itf.gl.pub.GLStartCheckUtil;
import nc.itf.gl.pub.ICommAccBookPub;
import nc.itf.gl.pub.IFreevaluePub;
import nc.itf.gl.pub.IGlPeriod;
import nc.itf.glcom.para.GLParaAccessor;
import nc.itf.glcom.para.IGlPara;
import nc.pubitf.accperiod.AccountCalendar;
import nc.pubitf.org.IAccountingBookPubService;
import nc.pubitf.rbac.IFunctionPermissionPubService;
import nc.pubitf.uapbd.IAccountAssPubService;
import nc.ui.gl.datacache.GLParaDataCache;
import nc.ui.glcom.balance.CAssSortTool;
import nc.ui.glcom.balance.GlAssDeal;
import nc.vo.bd.account.AccAssVO;
import nc.vo.bd.account.AccountVO;
import nc.vo.bd.currtype.CurrtypeVO;
import nc.vo.bd.period.AccperiodVO;
import nc.vo.bd.vouchertype.VoucherTypeVO;
import nc.vo.fipub.freevalue.GlAssVO;
import nc.vo.fipub.freevalue.Module;
import nc.vo.fipub.freevalue.account.proxy.AccAssGL;
import nc.vo.fipub.vouchertyperule.VouchertypeRuleVO;
import nc.vo.gateway60.accountbook.AccountBookUtil;
import nc.vo.gateway60.itfs.AccountUtilGL;
import nc.vo.gateway60.itfs.CalendarUtilGL;
import nc.vo.gateway60.itfs.CloseAccBookUtils;
import nc.vo.gateway60.itfs.CurrtypeGL;
import nc.vo.gateway60.itfs.VoucherTypeGL;
import nc.vo.gateway60.pub.GlBusinessException;
import nc.vo.gateway60.pub.VoucherTypeDataCache;
import nc.vo.gl.pubvoucher.DetailVO;
import nc.vo.gl.pubvoucher.GLParameterVO;
import nc.vo.gl.pubvoucher.OperationResultVO;
import nc.vo.gl.pubvoucher.VoucherVO;
import nc.vo.gl.voucher.VoucherCheckConfigVO;
import nc.vo.gl.voucher.VoucherCheckMessage;
import nc.vo.gl.vouchertools.DetailTool;
import nc.vo.glcom.ass.AssVO;
import nc.vo.glcom.balance.GlBalanceVO;
import nc.vo.glcom.balance.GlQueryVO;
import nc.vo.glcom.exception.GLBusinessException;
import nc.vo.glcom.glperiod.GlPeriodVO;
import nc.vo.glcom.inteltool.CDataSource;
import nc.vo.glcom.inteltool.CGenTool;
import nc.vo.glcom.inteltool.COutputTool;
import nc.vo.glcom.inteltool.CSumTool;
import nc.vo.glcom.intelvo.CIntelVO;
import nc.vo.glcom.para.GlDebugFlag;
import nc.vo.glcom.sorttool.ISortTool;
import nc.vo.glcom.sorttool.ISortToolProvider;
import nc.vo.glcom.tools.GLPubProxy;
import nc.vo.glcom.tools.GLSystemControlTool;
import nc.vo.glcom.wizard.VoWizard;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.org.AccountingBookVO;
import nc.vo.org.SetOfBookVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

import org.apache.commons.lang.StringUtils;

public class VoucherCheckBO implements ISortToolProvider {
	private ISortTool m_assSortTool;

	public VoucherCheckBO() {
	}

	private GlBalanceVO[] combineAss(GlBalanceVO[] vos, GlQueryVO m_qryVO) throws Exception {
		CIntelVO tt = new CIntelVO();

		int intSumLimit = 0;

		int[] intSortIndex = { 20 };

		CGenTool genTool = new CGenTool();

		genTool.setLimitSumGen(intSumLimit);
		genTool.setSortIndex(intSortIndex);

		genTool.setGetSortTool(this);

		CSumTool sumTool = new CSumTool();

		int[] sumIndex = { 17, 13, 10, 14, 46, 44, 47, 45 };

		sumTool.setSumIndex(sumIndex);

		COutputTool outputTool = new COutputTool();
		outputTool.setRequireOutputDetail(false);
		outputTool.setSummaryCol(-1);

		CDataSource datasource = new CDataSource();

		Vector vecVos = new Vector();

		Object userData = null;
		for (int i = 0; i < vos.length; i++) {

			vos[i].setUserData(null);

			vecVos.addElement(vos[i]);
		}

		datasource.setSumVector(CDataSource.sortVector(vecVos, genTool, false));
		try {
			tt.setSumTool(sumTool);
			tt.setGenTool(genTool);
			tt.setDatasource(datasource);
			tt.setOutputTool(outputTool);
		} catch (Throwable e) {
			Logger.error(e.getMessage(), e);
		}

		Vector recVector = tt.getResultVector();

		GlBalanceVO[] VOs = new GlBalanceVO[recVector.size()];

		recVector.copyInto(VOs);

		for (int i = 0; i < VOs.length; i++)
			VOs[i].setUserData(userData);
		return VOs;
	}

	private static final Log log = Log.getInstance(VoucherCheckBO.class);

	protected VoucherVO catOutSubjVoucher(VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		if (voucher == null)
			return null;
		voucher.setIsOutSubj(UFBoolean.FALSE);
		if (voucher.getNumDetails() > 0) {
			for (int i = 0; i < voucher.getNumDetails(); i++) {
				AccountVO acc = (AccountVO) tempaccsubj.get(voucher.getDetail(i).getPk_accasoa());

				if ((acc != null) && (acc.getOutflag() != null) && (acc.getOutflag().booleanValue())) {
					voucher.setIsOutSubj(UFBoolean.TRUE);
					break;
				}
			}
		}
		return voucher;
	}

	protected OperationResultVO[] checkALL(VoucherVO voucher, HashMap accsubjcache, GLParameterVO param) throws Exception {
		return null;
	}

	protected OperationResultVO[] checkAmountBalance(VoucherVO voucher, Integer controlmode) {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		if (voucher.getNumDetails() <= 0)
			return null;
		if ((voucher.getIsOutSubj() != null) && (voucher.getIsOutSubj().booleanValue())) {
			return null;
		}
		HashMap currtype_map = new HashMap();
		try {
			CurrtypeVO[] currtypes = getCurrency(voucher.getPk_org());

			if ((currtypes == null) || (currtypes.length == 0)) {
				throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000571"));
			}

			for (int i = 0; i < currtypes.length; i++) {
				currtype_map.put(currtypes[i].getPk_currtype(), currtypes[i]);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new GlBusinessException(e.getMessage());
		}
		OperationResultVO[] result = null;
		HashMap debit_map = new HashMap();
		HashMap credit_map = new HashMap();
		HashMap curr_map = new HashMap();
		UFDouble fracDebit = new UFDouble(0);
		UFDouble fracCredit = new UFDouble(0);
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			DetailVO detail = voucher.getDetail(i);
			curr_map.put(detail.getPk_currtype(), detail.getPk_currtype());
			fracDebit = (UFDouble) debit_map.get(detail.getPk_currtype());
			if (fracDebit == null)
				fracDebit = new UFDouble(0);
			fracCredit = (UFDouble) credit_map.get(detail.getPk_currtype());
			if (fracCredit == null)
				fracCredit = new UFDouble(0);
			fracDebit = fracDebit.add(detail.getDebitamount());
			fracCredit = fracCredit.add(detail.getCreditamount());
			debit_map.put(detail.getPk_currtype(), fracDebit);
			credit_map.put(detail.getPk_currtype(), fracCredit);
		}
		Iterator iterator = curr_map.keySet().iterator();
		Vector vecCurr = new Vector();
		while (iterator.hasNext()) {
			vecCurr.addElement(iterator.next());
		}
		for (int i = 0; i < vecCurr.size(); i++) {
			if (vecCurr.elementAt(i) != null) {
				CurrtypeVO curr = (CurrtypeVO) currtype_map.get(vecCurr.elementAt(i).toString());

				if (curr == null) {
					throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000572"));
				}

				fracDebit = (UFDouble) debit_map.get(vecCurr.elementAt(i).toString());

				if (fracDebit == null)
					fracDebit = new UFDouble(0);
				fracCredit = (UFDouble) credit_map.get(vecCurr.elementAt(i).toString());

				if (fracCredit == null)
					fracCredit = new UFDouble(0);
				if (!fracDebit.equals(fracCredit)) {
					switch (controlmode.intValue()) {
						case 1: {
							OperationResultVO rs = new OperationResultVO();
							rs.m_intSuccess = 1;
							rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20002) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000573", null, new String[] { curr.getName() }));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000332") + fracDebit.setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000333") + fracCredit.setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000335") + fracDebit.sub(fracCredit).abs().setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription += "\n";
							result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

							break;
						}
						case 2: {
							OperationResultVO rs = new OperationResultVO();
							rs.m_intSuccess = 2;
							rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20002) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000573", null, new String[] { curr.getName() }));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000332") + fracDebit.setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000333") + fracCredit.setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription = (rs.m_strDescription + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000335") + fracDebit.sub(fracCredit).abs().setScale(curr.getCurrdigit().intValue(), 4));

							rs.m_strDescription += "\n";
							result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

							break;
						}
						default:
							throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
					}

				}
			}
		}

		return result;
	}

	protected OperationResultVO[] checkAmountZero(VoucherVO voucher, Integer controlmode) {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		if (voucher.getNumDetails() <= 0)
			return null;
		if (voucher.getVoucherkind().intValue() == 3) {
			return null;
		}
		OperationResultVO[] result = null;
		Vector vecresult = new Vector();
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			DetailVO detail = voucher.getDetail(i);
			if ((detail.getDebitamount().equals(new UFDouble(0))) && (detail.getCreditamount().equals(new UFDouble(0)))) {

				switch (controlmode.intValue()) {
					case 1: {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 1;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20001) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						vecresult.addElement(rs);
						break;
					}
					case 2: {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20001) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						vecresult.addElement(rs);
						break;
					}
					default:
						throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
				}

			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkBalance(VoucherVO voucher) throws Exception {
		if ((voucher.getIsOutSubj() != null) && (voucher.getIsOutSubj().booleanValue())) {
			return null;
		}
		OperationResultVO[] result = null;
		if (voucher.getTotaldebit().sub(voucher.getTotalcredit()).abs().compareTo(UFDouble.ZERO_DBL) > 0) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10001);

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		if ((GLParaAccessor.isStartGroupCurr(voucher.getPk_group()).booleanValue()) && (voucher.getTotaldebitgroup().sub(voucher.getTotalcreditgroup()).abs().compareTo(UFDouble.ZERO_DBL) > 0)) {

			String para = GLParaAccessor.getGroupAmountCtrl(voucher.getPk_accountingbook());

			if ("2".equals(para)) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 1;
				rs.m_strPK = voucher.getPk_voucher();
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20019);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			} else if ("3".equals(para)) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strPK = voucher.getPk_voucher();
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20019);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		if ((GLParaAccessor.isStartGlobalCurr().booleanValue()) && (voucher.getTotaldebitglobal().sub(voucher.getTotalcreditglobal()).abs().compareTo(UFDouble.ZERO_DBL) > 0)) {

			String para = GLParaAccessor.getGlobalAmountCtrl(voucher.getPk_accountingbook());

			if ("2".equals(para)) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 1;
				rs.m_strPK = voucher.getPk_voucher();
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20020);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			} else if ("3".equals(para)) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strPK = voucher.getPk_voucher();
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20020);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		return result;
	}

	protected OperationResultVO[] checkBalanceControl(VoucherVO voucher) throws Exception {
		Integer controlmode = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getBalanceControlStyle(voucher.getPk_accountingbook());

		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		HashMap accsubjcache = getAccountMap(voucher, new HashMap());
		HashMap balancemap = new HashMap();
		HashMap groupBalancemap = new HashMap();
		HashMap globalBalancemap = new HashMap();
		HashMap tmp_subjmap = new HashMap();
		OperationResultVO[] result = null;
		Vector vecresult = new Vector();
		DetailVO[] details = voucher.getDetails();
		details = filterDetailForBalanceCtrl(details);
		DetailVO[] tmp_sumdetails = null;
		Vector vecdetails = new Vector();
		Vector vecSubjs = new Vector();

		UFDouble debitbalance = null;
		UFDouble groupDebitbalance = null;
		UFDouble globalDebitbalance = null;
		UFDouble t_debitbalance = null;

		OperationResultVO oprs = null;
		int infoLevel = 0;
		switch (controlmode.intValue()) {
			case 1:
				infoLevel = 1;
				break;

			case 2:
				infoLevel = 2;
				break;

			default:
				throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
		}

		if (details != null)
			for (int i = 0; i < details.length; i++) {
				AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

				if ((null != accvo) && (accvo.getBalanflag() != null) && (accvo.getBalanflag().booleanValue())) {

					DetailVO tmp_detail = (DetailVO) details[i].clone();
					tmp_detail.setUserData(null);
					vecdetails.addElement(tmp_detail);
				}
			}
		if (vecdetails.size() > 0) {
			DetailVO[] t_details = new DetailVO[vecdetails.size()];
			vecdetails.copyInto(t_details);
			tmp_sumdetails = DetailTool.sumDetails(t_details, new int[] { 103 });
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		if (voucher.getPk_voucher() != null) {
			DetailVO[] t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = filterDetailForBalanceCtrl(t_old_details);
			getAccountMap(t_old_details, accsubjcache);
			Vector vecolddetails = new Vector();
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo != null) {
						if ((accvo.getBalanflag() != null) && (accvo.getBalanflag().booleanValue())) {
							vecolddetails.addElement(t_old_details[i].clone());
						}
					}
				}
			}
			if (vecolddetails.size() > 0) {
				t_old_details = new DetailVO[vecolddetails.size()];
				vecolddetails.copyInto(t_old_details);
				t_old_details = DetailTool.sumDetails(t_old_details, new int[] { 103 });
			} else {
				t_old_details = null;
			}
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					debitbalance = (UFDouble) balancemap.get(t_old_details[i].getPk_accasoa());

					debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).sub(t_old_details[i].getLocaldebitamount()).add(t_old_details[i].getLocalcreditamount());

					balancemap.put(t_old_details[i].getPk_accasoa(), debitbalance);

					groupDebitbalance = (UFDouble) groupBalancemap.get(t_old_details[i].getPk_accasoa());

					groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).sub(t_old_details[i].getGroupdebitamount()).add(t_old_details[i].getGroupcreditamount());

					groupBalancemap.put(t_old_details[i].getPk_accasoa(), groupDebitbalance);

					globalDebitbalance = (UFDouble) globalBalancemap.get(t_old_details[i].getPk_accasoa());

					globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).sub(t_old_details[i].getGlobaldebitamount()).add(t_old_details[i].getGlobalcreditamount());

					globalBalancemap.put(t_old_details[i].getPk_accasoa(), globalDebitbalance);

					if (tmp_subjmap.get(t_old_details[i].getPk_accasoa()) == null) {
						vecSubjs.addElement(t_old_details[i].getPk_accasoa());
						tmp_subjmap.put(t_old_details[i].getPk_accasoa(), t_old_details[i].getPk_accasoa());
					}
				}
			}
		}

		String[] pk_accsubjs = null;
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				if (tmp_subjmap.get(tmp_sumdetails[i].getPk_accasoa()) == null) {
					vecSubjs.addElement(tmp_sumdetails[i].getPk_accasoa());
					tmp_subjmap.put(tmp_sumdetails[i].getPk_accasoa(), tmp_sumdetails[i].getPk_accasoa());
				}
			}
		}
		if (vecSubjs.size() > 0) {
			pk_accsubjs = new String[vecSubjs.size()];
			vecSubjs.copyInto(pk_accsubjs);
		}
		if ((pk_accsubjs == null) || (pk_accsubjs.length == 0))
			return null;
		GlQueryVO qryvo = new GlQueryVO();
		qryvo.setPk_account(pk_accsubjs);
		qryvo.setYear(year);
		qryvo.setPeriod(period);
		qryvo.setIncludeUnTallyed(true);
		qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

		qryvo.setGroupFields(new int[] { 4 });
		qryvo.setSubjVersion(voucher.getPrepareddate().toStdString());
		GlBalanceVO[] rs = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

		if (rs != null) {
			for (int i = 0; i < rs.length; i++) {
				debitbalance = (rs[i].getLocaldebitamount() == null ? new UFDouble(0) : rs[i].getLocaldebitamount()).sub(rs[i].getLocalcreditamount() == null ? new UFDouble(0) : rs[i].getLocalcreditamount());

				t_debitbalance = (UFDouble) balancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(debitbalance);

				balancemap.put(rs[i].getPk_accasoa(), t_debitbalance);

				groupDebitbalance = (rs[i].getDebitGroupAmount() == null ? new UFDouble(0) : rs[i].getDebitGroupAmount()).sub(rs[i].getCreditGroupAmount() == null ? new UFDouble(0) : rs[i].getCreditGroupAmount());

				t_debitbalance = (UFDouble) groupBalancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(groupDebitbalance);

				groupBalancemap.put(rs[i].getPk_accasoa(), t_debitbalance);

				globalDebitbalance = (rs[i].getDebitGlobalAmount() == null ? new UFDouble(0) : rs[i].getDebitGlobalAmount()).sub(rs[i].getCreditGlobalAmount() == null ? new UFDouble(0) : rs[i].getCreditGlobalAmount());

				t_debitbalance = (UFDouble) globalBalancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(globalDebitbalance);

				globalBalancemap.put(rs[i].getPk_accasoa(), t_debitbalance);
			}
		}
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				debitbalance = (UFDouble) balancemap.get(tmp_sumdetails[i].getPk_accasoa());

				debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).add(tmp_sumdetails[i].getLocaldebitamount()).sub(tmp_sumdetails[i].getLocalcreditamount());

				balancemap.put(tmp_sumdetails[i].getPk_accasoa(), debitbalance);

				groupDebitbalance = (UFDouble) groupBalancemap.get(tmp_sumdetails[i].getPk_accasoa());

				groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).add(tmp_sumdetails[i].getGroupdebitamount()).sub(tmp_sumdetails[i].getGroupcreditamount());

				groupBalancemap.put(tmp_sumdetails[i].getPk_accasoa(), groupDebitbalance);

				globalDebitbalance = (UFDouble) globalBalancemap.get(tmp_sumdetails[i].getPk_accasoa());

				globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).add(tmp_sumdetails[i].getGlobaldebitamount()).sub(tmp_sumdetails[i].getGlobalcreditamount());

				globalBalancemap.put(tmp_sumdetails[i].getPk_accasoa(), globalDebitbalance);
			}
		}
		for (int i = 0; i < pk_accsubjs.length; i++) {
			debitbalance = (UFDouble) balancemap.get(pk_accsubjs[i]);
			groupDebitbalance = (UFDouble) groupBalancemap.get(pk_accsubjs[i]);
			globalDebitbalance = (UFDouble) globalBalancemap.get(pk_accsubjs[i]);

			if ((debitbalance != null) || (groupDebitbalance != null) || (globalDebitbalance != null)) {

				AccountVO accvo = (AccountVO) accsubjcache.get(pk_accsubjs[i]);
				if (accvo.getBalanorient().intValue() == 0) {
					if (debitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20030) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20031) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20032) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
				} else {
					if (debitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20030) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20031) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20032) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
				}
			}
		}
		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkAssBalanceControl(VoucherVO voucher, Boolean operatedirection) throws Exception {
		Integer controlmode = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getBalanceControlStyle(voucher.getPk_accountingbook());

		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		String[] pk_accsubj = new String[voucher.getNumDetails()];
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			pk_accsubj[i] = voucher.getDetail(i).getPk_accasoa();
		}
		HashMap accsubjcache = new HashMap();
		AccountVO[] accvos = getAccsubj(pk_accsubj, voucher.getPrepareddate().toStdString());

		IAccountAssPubService accountassservice = (IAccountAssPubService) NCLocator.getInstance().lookup(IAccountAssPubService.class.getName());

		Map<String, List<AccAssVO>> asMap = accountassservice.queryAllByAccPKs(pk_accsubj, voucher.getPrepareddate().toStdString());

		AccountVO voTemp = null;
		if (accvos != null) {
			for (int i = 0; i < accvos.length; i++) {
				voTemp = accvos[i];
				voTemp.setAccass(asMap.get(voTemp.getPk_accasoa()) == null ? null : (AccAssVO[]) ((List) asMap.get(voTemp.getPk_accasoa())).toArray(new AccAssVO[0]));

				accsubjcache.put(accvos[i].getPk_accasoa(), accvos[i]);
			}
		}
		HashMap balancemap = new HashMap();
		HashMap tmp_subjmap = new HashMap();
		OperationResultVO[] result = null;
		Vector vecresult = new Vector();

		DetailVO[] details = voucher.getDetails();
		DetailVO[] tmp_sumdetails = null;
		Vector vecSubjs = new Vector();
		DetailVO[] t_old_details = null;
		Vector vecSubjAsss = new Vector();
		ArrayList V_assarray = new ArrayList();
		AssVO[] V_ass = null;
		Vector assvecdetails = new Vector();
		HashMap asstemphashmap = new HashMap();
		if (details != null) {
			for (int i = 0; i < details.length; i++) {
				AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

				if (accvo != null) {

					AccAssVO[] accass = accvo.getAccass();
					if ((accass != null) && (accass.length > 0)) {
						Boolean isasscontrol = new Boolean(false);
						for (int j = 0; j < accass.length; j++) {
							AccAssVO subjass = accass[j];
							if (subjass.getIsbalancecontrol().booleanValue()) {
								isasscontrol = new Boolean(true);
								break;
							}
						}
						if (!isasscontrol.booleanValue()) {
							break;
						}

						HashMap assvohm = new HashMap();
						ArrayList D_assarray = new ArrayList();
						DetailVO tmp_detail = (DetailVO) details[i].clone();
						tmp_detail.setUserData(null);
						if ((isasscontrol.booleanValue()) && (details[i].getAssid() != null) && ((details[i].getAss() == null) || (details[i].getAss().length == 0))) {

							details[i].setAss(((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class.getName())).queryAssvosByid(details[i].getAssid(), Module.GL));
						}

						for (int j = 0;

						(isasscontrol.booleanValue()) && (details[i].getAss() != null) && (j < details[i].getAss().length); j++) {
							assvohm.put(details[i].getAss()[j].getPk_Checktype(), details[i].getAss()[j]);

							asstemphashmap.put(details[i].getAss()[j].getPk_Checktype() + details[i].getAss()[j].getPk_Checkvalue(), details[i].getAss()[j]);
						}

						for (int j = 0; (isasscontrol.booleanValue()) && (j < accass.length); j++) {
							AccAssVO subjass = accass[j];
							if (subjass.getIsbalancecontrol().booleanValue()) {
								if (!vecSubjAsss.contains(accvo.getPk_accasoa())) {
									vecSubjAsss.addElement(accvo.getPk_accasoa());
								}

								AssVO tempass = (AssVO) assvohm.get(subjass.getPk_entity());

								tempass.setUserData(null);
								D_assarray.add(tempass);
								String hashkey = tempass.getPk_Checktype() + tempass.getPk_Checkvalue();

								if (asstemphashmap.get(hashkey) != null) {
									boolean include = false;
									for (int k = 0; k < V_assarray.size(); k++) {
										AssVO assVO = (AssVO) V_assarray.get(k);
										if (hashkey.equals(assVO.getPk_Checktype() + assVO.getPk_Checkvalue())) {

											include = true;
											break;
										}
									}
									if (!include) {
										V_assarray.add(tempass);
									}
								}
							}
						}

						if (D_assarray.size() > 0) {
							AssVO[] ass = new AssVO[D_assarray.size()];
							D_assarray.toArray(ass);
							tmp_detail.setAss(ass);
							assvecdetails.addElement(tmp_detail);
						}
					}
				}
			}
		}
		if (assvecdetails.size() > 0) {
			DetailVO[] t_details = new DetailVO[assvecdetails.size()];
			assvecdetails.copyInto(t_details);
			tmp_sumdetails = DetailTool.sumDetails(t_details, new int[] { 103, 303 });
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		if (voucher.getPk_voucher() != null) {
			t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = catAss(t_old_details);
			Vector vecolddetails = new Vector();
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

						accsubjcache.put(t_old_details[i].getPk_accasoa(), accvo);
					}

					AccAssVO subass = getAssbalanceByAcc(accvo, voucher);
					if (subass != null) {
						vecolddetails.addElement(t_old_details[i].clone());
					}
				}
			}
			if (vecolddetails.size() > 0) {
				t_old_details = new DetailVO[vecolddetails.size()];
				vecolddetails.copyInto(t_old_details);
				t_old_details = DetailTool.sumDetails(t_old_details, new int[] { 103, 303 });
			} else {
				t_old_details = null;
			}
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO D_accsubj = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					AssVO balanceass = getBalanceAssVO(t_old_details[i], D_accsubj);

					if (balanceass != null) {
						String B_hm_key = D_accsubj.getPk_accasoa() + balanceass.getPk_Checktype() + balanceass.getPk_Checkvalue();

						UFDouble debitbalance = (UFDouble) balancemap.get(B_hm_key);

						debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).sub(t_old_details[i].getLocaldebitamount()).add(t_old_details[i].getLocalcreditamount());

						balancemap.put(B_hm_key, debitbalance);
						if (tmp_subjmap.get(t_old_details[i].getPk_accasoa()) == null) {
							vecSubjs.addElement(t_old_details[i].getPk_accasoa());

							tmp_subjmap.put(t_old_details[i].getPk_accasoa(), t_old_details[i].getPk_accasoa());
						}
					}
				}
			}
		}

		String[] pk_accsubjs = null;
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				if (tmp_subjmap.get(tmp_sumdetails[i].getPk_accasoa()) == null) {
					vecSubjs.addElement(tmp_sumdetails[i].getPk_accasoa());
					tmp_subjmap.put(tmp_sumdetails[i].getPk_accasoa(), tmp_sumdetails[i].getPk_accasoa());
				}
			}
		}
		if (vecSubjs.size() > 0) {
			pk_accsubjs = new String[vecSubjs.size()];
			vecSubjs.copyInto(pk_accsubjs);
		}
		if (V_assarray.size() > 0) {
			V_ass = new AssVO[V_assarray.size()];
			V_assarray.toArray(V_ass);
		}
		if ((pk_accsubjs == null) || (pk_accsubjs.length == 0))
			return null;
		GlQueryVO qryvo = new GlQueryVO();
		qryvo.setPk_account(pk_accsubjs);
		qryvo.setAssVos(V_ass);
		qryvo.setYear(year);
		qryvo.setPeriod(period);
		qryvo.setIncludeUnTallyed(true);
		qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

		qryvo.setBaseAccountingbook(voucher.getPk_accountingbook());
		if (voucher.getVoucherkind().intValue() == 2) {
			if (voucher.getPeriod().equals("00")) {
				qryvo.setSubjVerisonPeriod(calendar.getFirstMonthOfCurrentYear().getAccperiodmth());
			} else {
				qryvo.setSubjVerisonPeriod(voucher.getPeriod());
			}
		} else {
			calendar.setDate(voucher.getPrepareddate());
			qryvo.setSubjVerisonPeriod(calendar.getMonthVO().getAccperiodmth());
		}
		qryvo.setSubjVersionYear(year);
		qryvo.setUseSubjVersion(true);

		qryvo.setGroupFields(new int[] { 4, 19 });

		GlBalanceVO[] rs = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

		if ((voucher.getPk_voucher() != null) && (t_old_details != null) && (t_old_details.length > 0)) {
			for (int i = 0; i < t_old_details.length; i++) {
				boolean ismatch = false;
				DetailVO olddetail = t_old_details[i];
				if (rs != null) {
					for (int j = 0; j < rs.length; j++) {
						ismatch = olddetail.getPk_accasoa().equals(rs[j].getPk_accasoa());

						AssVO[] sumass = olddetail.getAss();
						AssVO[] balanceass = rs[j].getAssVos();
						if ((balanceass == null) && (rs[j].getAssid() != null)) {
							balanceass = ((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class.getName())).queryAssvosByid(rs[j].getAssid(), Module.GL);
						}

						ismatch = compareBalanceAssvos(sumass, balanceass).booleanValue();

						if (ismatch) {
							UFDouble debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

							if (operatedirection.booleanValue()) {
								debitbalance = debitbalance.sub((olddetail.getLocaldebitamount() == null ? new UFDouble(0) : olddetail.getLocaldebitamount()).sub(olddetail.getLocalcreditamount() == null ? new UFDouble(0) : olddetail.getLocalcreditamount()));

							} else {

								debitbalance = debitbalance.add((olddetail.getLocaldebitamount() == null ? new UFDouble(0) : olddetail.getLocaldebitamount()).sub(olddetail.getLocalcreditamount() == null ? new UFDouble(0) : olddetail.getLocalcreditamount()));
							}

							AccountVO accvo = (AccountVO) accsubjcache.get(rs[j].getPk_accasoa());

							if (accvo == null) {

								accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

								accsubjcache.put(details[i].getPk_accasoa(), accvo);
							}

							if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
								if (debitbalance.compareTo(new UFDouble(-1.0E-8D)) >= 0)
									break;
								OperationResultVO oprs = new OperationResultVO();
								switch (controlmode.intValue()) {
									case 1:
										oprs.m_intSuccess = 1;
										break;

									case 2:
										oprs.m_intSuccess = 2;
										break;

									default:
										throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
								}

								oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

								vecresult.addElement(oprs);
								break;
							}
							if (debitbalance.compareTo(new UFDouble(1.0E-8D)) <= 0)
								break;
							OperationResultVO oprs = new OperationResultVO();
							switch (controlmode.intValue()) {
								case 1:
									oprs.m_intSuccess = 1;
									break;

								case 2:
									oprs.m_intSuccess = 2;
									break;

								default:
									throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
							}

							oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

							vecresult.addElement(oprs);
							break;
						}
					}
				}
			}
		}

		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				boolean ismatch = false;
				DetailVO sumdetail = tmp_sumdetails[i];
				if (rs != null) {
					for (int j = 0; j < rs.length; j++) {
						ismatch = sumdetail.getPk_accasoa().equals(rs[j].getPk_accasoa());

						AssVO[] sumass = sumdetail.getAss();
						AssVO[] balanceass = rs[j].getAssVos();
						ismatch = (ismatch) && (new CAssSortTool().compare(sumass, balanceass) == 0);

						if (ismatch) {
							UFDouble debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

							if (operatedirection.booleanValue()) {
								debitbalance = debitbalance.sub((sumdetail.getLocaldebitamount() == null ? new UFDouble(0) : sumdetail.getLocaldebitamount()).sub(sumdetail.getLocalcreditamount() == null ? new UFDouble(0) : sumdetail.getLocalcreditamount()));

							} else {

								debitbalance = debitbalance.add((sumdetail.getLocaldebitamount() == null ? new UFDouble(0) : sumdetail.getLocaldebitamount()).sub(sumdetail.getLocalcreditamount() == null ? new UFDouble(0) : sumdetail.getLocalcreditamount()));
							}

							AccountVO accvo = (AccountVO) accsubjcache.get(rs[j].getPk_accasoa());

							if (accvo == null) {
								accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

								accsubjcache.put(details[i].getPk_accasoa(), accvo);
							}

							if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
								if (debitbalance.compareTo(new UFDouble(-1.0E-8D)) >= 0)
									break;
								OperationResultVO oprs = new OperationResultVO();
								switch (controlmode.intValue()) {
									case 1:
										oprs.m_intSuccess = 1;
										break;

									case 2:
										oprs.m_intSuccess = 2;
										break;

									default:
										throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
								}

								oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

								vecresult.addElement(oprs);
								break;
							}
							if (debitbalance.compareTo(new UFDouble(1.0E-8D)) <= 0)
								break;
							OperationResultVO oprs = new OperationResultVO();
							switch (controlmode.intValue()) {
								case 1:
									oprs.m_intSuccess = 1;
									break;

								case 2:
									oprs.m_intSuccess = 2;
									break;

								default:
									throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
							}

							oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

							vecresult.addElement(oprs);
							break;
						}
					}
				}
			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkAssBalanceControlNew(VoucherVO voucher, Boolean operatedirection) throws Exception {
		Integer controlmode = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getBalanceControlStyle(voucher.getPk_accountingbook());

		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		HashMap accsubjcache = getAccountMap(voucher, new HashMap());
		HashMap balancemap = new HashMap();
		HashMap groupBalancemap = new HashMap();
		HashMap globalBalancemap = new HashMap();
		HashMap accsubjmap = new HashMap();
		HashMap assmap = new HashMap();
		HashMap oldbalancemap = new HashMap();
		HashMap oldGroupBalancemap = new HashMap();
		HashMap oldGlobalBalancemap = new HashMap();
		HashMap<String, AssVO> asschecktypemap = new HashMap();
		AssVO[] queryass = null;
		Vector vecresult = new Vector();
		OperationResultVO[] result = null;

		String balabcekey = null;
		UFDouble debitbalance = null;
		UFDouble groupDebitbalance = null;
		UFDouble globalDebitbalance = null;

		OperationResultVO oprs = null;
		int infoLevel = 0;
		switch (controlmode.intValue()) {
			case 1:
				infoLevel = 1;
				break;

			case 2:
				infoLevel = 2;
				break;

			default:
				throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
		}

		DetailVO[] details = voucher.getDetails();
		details = filterDetailForBalanceCtrl(details);
		DetailVO[] t_old_details = voucher.getDetails();
		details = catAss(details);

		for (int i = 0; (details != null) && (i < details.length); i++) {
			AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

			if (accvo == null) {
				accvo = new AccountVO();
				accvo.setPk_accasoa(details[i].getPk_accasoa());
				accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

				accsubjcache.put(details[i].getPk_accasoa(), accvo);

				Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

				if ((subassvec != null) && (subassvec.size() > 0)) {
					accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
				}
			}
			if (accvo != null) {

				AccAssVO[] accass = accvo.getAccass();
				if ((accass != null) && (accass.length > 0)) {
					for (int j = 0; j < accass.length; j++) {
						AssVO assvo = matchAssBalance(accass[j], details[i].getAss());

						if (assvo != null) {

							balabcekey = details[i].getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

							debitbalance = balancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) balancemap.get(balabcekey);

							debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).add((details[i].getLocaldebitamount() == null ? new UFDouble(0) : details[i].getLocaldebitamount()).sub(details[i].getLocalcreditamount() == null ? new UFDouble(0) : details[i].getLocalcreditamount()));

							balancemap.put(balabcekey, debitbalance);
							groupDebitbalance = (UFDouble) groupBalancemap.get(balabcekey);

							groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).add((details[i].getGroupdebitamount() == null ? new UFDouble(0) : details[i].getGroupdebitamount()).sub(details[i].getGroupcreditamount() == null ? new UFDouble(0) : details[i].getGroupcreditamount()));

							groupBalancemap.put(balabcekey, groupDebitbalance);
							globalDebitbalance = (UFDouble) globalBalancemap.get(balabcekey);

							globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).add((details[i].getGlobaldebitamount() == null ? new UFDouble(0) : details[i].getGlobaldebitamount()).sub(details[i].getGlobalcreditamount() == null ? new UFDouble(0) : details[i].getGlobalcreditamount()));

							globalBalancemap.put(balabcekey, globalDebitbalance);
							if (accsubjmap.get(accvo.getPk_accasoa()) == null) {
								accsubjmap.put(accvo.getPk_accasoa(), accvo.getPk_accasoa());
							}

							if (assmap.get(assvo.getPk_Checktype() + assvo.getPk_Checkvalue()) == null) {
								assmap.put(assvo.getPk_Checktype() + assvo.getPk_Checkvalue(), assvo);
							}
						}
					}
				}
			}
		}

		if (voucher.getPk_voucher() != null) {
			t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = filterDetailForBalanceCtrl(t_old_details);
			getAccountMap(t_old_details, accsubjcache);
			t_old_details = catAss(t_old_details);
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = new AccountVO();
						accvo.setPk_accasoa(t_old_details[i].getPk_accasoa());

						accvo = AccountUtilGL.findAccountVOByPrimaryKey(t_old_details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

						accsubjcache.put(t_old_details[i].getPk_accasoa(), accvo);

						Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

						if ((subassvec != null) && (subassvec.size() > 0)) {
							accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
						}
					}
					if (accvo != null) {

						AccAssVO[] accass = accvo.getAccass();
						if ((accass != null) && (accass.length > 0)) {
							for (int j = 0; j < accass.length; j++) {
								AssVO assvo = matchAssBalance(accass[j], t_old_details[i].getAss());

								if (assvo != null) {

									balabcekey = t_old_details[i].getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

									debitbalance = oldbalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldbalancemap.get(balabcekey);

									debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).add((t_old_details[i].getLocaldebitamount() == null ? new UFDouble(0) : t_old_details[i].getLocaldebitamount()).sub(t_old_details[i].getLocalcreditamount() == null ? new UFDouble(0) : t_old_details[i].getLocalcreditamount()));

									oldbalancemap.put(balabcekey, debitbalance);
									groupDebitbalance = oldGroupBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldGroupBalancemap.get(balabcekey);

									groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).add((t_old_details[i].getGroupdebitamount() == null ? new UFDouble(0) : t_old_details[i].getGroupdebitamount()).sub(t_old_details[i].getGroupcreditamount() == null ? new UFDouble(0) : t_old_details[i].getGroupcreditamount()));

									oldGroupBalancemap.put(balabcekey, groupDebitbalance);

									globalDebitbalance = oldGlobalBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldGlobalBalancemap.get(balabcekey);

									globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).add((t_old_details[i].getGlobaldebitamount() == null ? new UFDouble(0) : t_old_details[i].getGlobaldebitamount()).sub(t_old_details[i].getGlobalcreditamount() == null ? new UFDouble(0) : t_old_details[i].getGlobalcreditamount()));

									oldGlobalBalancemap.put(balabcekey, globalDebitbalance);

									if (accsubjmap.get(accvo.getPk_accasoa()) == null) {
										accsubjmap.put(accvo.getPk_accasoa(), accvo.getPk_accasoa());
									}

									if (assmap.get(assvo.getPk_Checktype() + assvo.getPk_Checkvalue()) == null) {
										assmap.put(assvo.getPk_Checktype() + assvo.getPk_Checkvalue(), assvo);
									}
								}
							}
						}
					}
				}
			}
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		String[] pk_accsubjs = null;
		if ((accsubjmap != null) && (accsubjmap.size() > 0)) {
			pk_accsubjs = new String[accsubjmap.size()];
			pk_accsubjs = (String[]) accsubjmap.values().toArray(pk_accsubjs);
		}
		AssVO[] V_ass = null;
		if ((assmap != null) && (assmap.size() > 0)) {
			V_ass = new AssVO[assmap.size()];
			V_ass = (AssVO[]) assmap.values().toArray(V_ass);
		}
		if (V_ass == null) {
			return null;
		}
		for (int i = 0; i < V_ass.length; i++) {
			if (asschecktypemap.get(V_ass[i].getPk_Checktype()) == null) {
				asschecktypemap.put(V_ass[i].getPk_Checktype(), V_ass[i]);
			} else {
				AssVO _ass = (AssVO) asschecktypemap.get(V_ass[i].getPk_Checktype());
				_ass.setPk_Checkvalue(_ass.getPk_Checkvalue() + "," + V_ass[i].getPk_Checkvalue());

				asschecktypemap.put(V_ass[i].getPk_Checktype(), _ass);
			}
		}
		queryass = new AssVO[asschecktypemap.size()];
		queryass = (AssVO[]) asschecktypemap.values().toArray(queryass);
		GlQueryVO qryvo = new GlQueryVO();
		qryvo.setPk_account(pk_accsubjs);
		qryvo.setAssVos(queryass);
		qryvo.setYear(year);
		qryvo.setPeriod(period);
		qryvo.setIncludeUnTallyed(true);
		qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

		qryvo.setBaseAccountingbook(voucher.getPk_accountingbook());

		if (voucher.getVoucherkind().intValue() == 2) {
			if (voucher.getPeriod().equals("00")) {
				qryvo.setSubjVerisonPeriod(calendar.getFirstMonthOfCurrentYear().getAccperiodmth());
			} else {
				qryvo.setSubjVerisonPeriod(voucher.getPeriod());
			}
		} else {
			calendar.setDate(voucher.getPrepareddate());
			qryvo.setSubjVerisonPeriod(calendar.getMonthVO().getAccperiodmth());
		}
		qryvo.setSubjVersionYear(year);
		qryvo.setUseSubjVersion(true);
		qryvo.setSubjVersion(voucher.getPrepareddate().toStdString());

		qryvo.setGroupFields(new int[] { 4, 19 });

		GlBalanceVO[] rs = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

		if ((rs != null) && (rs.length > 0)) {
			GlAssDeal objTemp = new GlAssDeal();

			String[] strType = null;
			AssVO[] assvos = qryvo.getAssVos();
			if ((assvos != null) && (assvos.length != 0)) {
				strType = new String[assvos.length];
				for (int i = 0; i < assvos.length; i++) {
					strType[i] = assvos[i].getPk_Checktype();
				}
			}
			objTemp.setMatchingIndex(19);
			objTemp.setAppendIndex(20);

			objTemp.dealWith(rs, strType);

			rs = combineAss(rs, qryvo);
		}
		for (int j = 0; j < rs.length; j++) {
			debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

			groupDebitbalance = (rs[j].getDebitGroupAmount() == null ? new UFDouble(0) : rs[j].getDebitGroupAmount()).sub(rs[j].getCreditGroupAmount() == null ? new UFDouble(0) : rs[j].getCreditGroupAmount());

			globalDebitbalance = (rs[j].getDebitGlobalAmount() == null ? new UFDouble(0) : rs[j].getDebitGlobalAmount()).sub(rs[j].getCreditGlobalAmount() == null ? new UFDouble(0) : rs[j].getCreditGlobalAmount());

			AccountVO accvo = (AccountVO) accsubjcache.get(rs[j].getPk_accasoa());

			if (accvo == null) {
				accvo = new AccountVO();
				accvo.setPk_accasoa(rs[j].getPk_accasoa());

				accvo = AccountUtilGL.findAccountVOByPrimaryKey(rs[j].getPk_accasoa(), voucher.getPrepareddate().toStdString());

				accsubjcache.put(rs[j].getPk_accasoa(), accvo);
			}
			AssVO assvo = matchAssBalance(accvo, rs[j].getAssVos(), voucher);
			UFDouble oldbalance = null;
			UFDouble oldGroupBalance = null;
			UFDouble oldGlobalBalance = null;
			UFDouble balance = null;
			UFDouble groupBalance = null;
			UFDouble globalBalance = null;
			if (assvo != null) {
				balabcekey = accvo.getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

				oldbalance = (UFDouble) oldbalancemap.get(balabcekey);
				balance = (UFDouble) balancemap.get(balabcekey);
				oldGroupBalance = (UFDouble) oldGroupBalancemap.get(balabcekey);
				groupBalance = (UFDouble) groupBalancemap.get(balabcekey);
				oldGlobalBalance = (UFDouble) oldGlobalBalancemap.get(balabcekey);

				globalBalance = (UFDouble) globalBalancemap.get(balabcekey);
			}
			debitbalance = debitbalance.sub(oldbalance == null ? new UFDouble(0) : oldbalance);

			debitbalance = debitbalance.add(balance == null ? new UFDouble(0) : balance);

			groupDebitbalance = groupDebitbalance.sub(oldGroupBalance == null ? new UFDouble(0) : oldGroupBalance);

			groupDebitbalance = groupDebitbalance.add(groupBalance == null ? new UFDouble(0) : groupBalance);

			globalDebitbalance = globalDebitbalance.sub(oldGlobalBalance == null ? new UFDouble(0) : oldGlobalBalance);

			globalDebitbalance = globalDebitbalance.add(globalBalance == null ? new UFDouble(0) : globalBalance);

			if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
				if (debitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
			} else {
				if (debitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	private Boolean compareBalanceAssvos(AssVO[] sumass, AssVO[] balanceass) {
		if ((sumass != null) && (sumass.length > 0) && (balanceass != null) && (balanceass.length > 0)) {
			for (int i = 0; i < balanceass.length; i++) {
				AssVO assVO = balanceass[i];
				for (int j = 0; j < sumass.length; j++) {
					if ((assVO.getPk_Checktype().equals(sumass[j].getPk_Checktype())) && (assVO.getPk_Checkvalue().equals(sumass[j].getPk_Checkvalue()))) {

						return new Boolean(true);
					}
				}
			}
		}

		return new Boolean(false);
	}

	private AssVO getBalanceAssVO(DetailVO detail, AccountVO accsubj) {
		AssVO[] ass = detail.getAss();

		AccAssVO[] accass = accsubj.getAccass();
		if ((ass != null) && (ass.length > 0) && (accass != null) && (accass.length > 0)) {
			for (int i = 0; i < ass.length; i++) {
				for (int j = 0; j < accass.length; j++) {
					AccAssVO subjass = accass[j];
					if ((subjass != null) && (subjass.getIsbalancecontrol().booleanValue()) && (ass[i].getPk_Checktype().equals(subjass.getPk_entity()))) {

						return ass[i];
					}
				}
			}
		}
		return null;
	}

	private AccAssVO getAssbalanceByAcc(AccountVO accsubj, VoucherVO vo) {
		if (accsubj == null) {
			return null;
		}
		if ((accsubj.getAccass() != null) && (accsubj.getAccass().length > 0)) {
			for (AccAssVO assvo : accsubj.getAccass()) {
				if (assvo.getIsbalancecontrol().booleanValue()) {
					return assvo;
				}
			}
		}
		return null;
	}

	private AssVO matchAssBalance(AccountVO accsubj, AssVO[] ass, VoucherVO vo) {
		String pk_checktype = null;
		if (accsubj != null) {

			if ((accsubj.getAccass() != null) && (accsubj.getAccass().length > 0)) {
				AccAssVO[] asvos = accsubj.getAccass();
				for (AccAssVO asvo : asvos) {
					if (asvo.getIsbalancecontrol().booleanValue()) {
						pk_checktype = asvo.getPk_entity();
						break;
					}
				}
				if (pk_checktype != null) {
					for (int i = 0; (ass != null) && (ass.length > 0) && (i < ass.length); i++) {
						AssVO assVO = ass[i];
						if (assVO.getPk_Checktype().equals(pk_checktype)) {
							return assVO;
						}

					}
				} else {
					return null;
				}
			}
		} else {
			return null;
		}

		return null;
	}

	protected OperationResultVO[] checkBalanceControl(VoucherVO voucher, HashMap accsubjcache, GLParameterVO param, Integer controlmode) throws Exception {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		HashMap balancemap = new HashMap();
		HashMap groupBalancemap = new HashMap();
		HashMap globalBalancemap = new HashMap();
		HashMap tmp_subjmap = new HashMap();
		OperationResultVO[] result = null;
		Vector vecresult = new Vector();
		DetailVO[] details = voucher.getDetails();
		details = filterDetailForBalanceCtrl(details);
		DetailVO[] tmp_sumdetails = null;
		Vector vecdetails = new Vector();
		Vector vecSubjs = new Vector();
		UFDouble debitbalance = null;
		UFDouble groupDebitbalance = null;
		UFDouble globalDebitbalance = null;
		int infoLevel = 0;
		OperationResultVO oprs = null;

		switch (controlmode.intValue()) {
			case 1:
				infoLevel = 1;
				break;

			case 2:
				infoLevel = 2;
				break;

			default:
				throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
		}

		if (details != null)
			for (int i = 0; i < details.length; i++) {
				AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

				if (accvo == null) {
					accvo = AccountUtilGL.findByPrimaryKey(details[i].getPk_accasoa());

					accsubjcache.put(details[i].getPk_accasoa(), accvo);
				}
				if ((accvo != null) && (accvo.getBalanflag() != null) && (accvo.getBalanflag().booleanValue())) {

					DetailVO tmp_detail = (DetailVO) details[i].clone();
					tmp_detail.setUserData(null);
					vecdetails.addElement(tmp_detail);
				}
			}
		if (vecdetails.size() > 0) {
			DetailVO[] t_details = new DetailVO[vecdetails.size()];
			vecdetails.copyInto(t_details);
			tmp_sumdetails = DetailTool.sumDetails(t_details, new int[] { 103 });
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		if (voucher.getPk_voucher() != null) {
			DetailVO[] t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = filterDetailForBalanceCtrl(t_old_details);
			getAccountMap(t_old_details, accsubjcache);
			Vector vecolddetails = new Vector();
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = AccountUtilGL.findAccountVOByPrimaryKey(t_old_details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

						accsubjcache.put(t_old_details[i].getPk_accasoa(), accvo);
					}

					if ((accvo != null) && (accvo.getBalanflag() != null) && (accvo.getBalanflag().booleanValue())) {

						vecolddetails.addElement(t_old_details[i].clone());
					}
				}
			}
			if (vecolddetails.size() > 0) {
				t_old_details = new DetailVO[vecolddetails.size()];
				vecolddetails.copyInto(t_old_details);
				t_old_details = DetailTool.sumDetails(t_old_details, new int[] { 103 });
			} else {
				t_old_details = null;
			}
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					debitbalance = (UFDouble) balancemap.get(t_old_details[i].getPk_accasoa());

					debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).sub(t_old_details[i].getLocaldebitamount()).add(t_old_details[i].getLocalcreditamount());

					balancemap.put(t_old_details[i].getPk_accasoa(), debitbalance);

					groupDebitbalance = (UFDouble) groupBalancemap.get(t_old_details[i].getPk_accasoa());

					groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).sub(t_old_details[i].getGroupdebitamount()).add(t_old_details[i].getGroupcreditamount());

					groupBalancemap.put(t_old_details[i].getPk_accasoa(), groupDebitbalance);

					globalDebitbalance = (UFDouble) globalBalancemap.get(t_old_details[i].getPk_accasoa());

					globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).sub(t_old_details[i].getGlobaldebitamount()).add(t_old_details[i].getGlobalcreditamount());

					globalBalancemap.put(t_old_details[i].getPk_accasoa(), globalDebitbalance);

					if (tmp_subjmap.get(t_old_details[i].getPk_accasoa()) == null) {
						vecSubjs.addElement(t_old_details[i].getPk_accasoa());
						tmp_subjmap.put(t_old_details[i].getPk_accasoa(), t_old_details[i].getPk_accasoa());
					}
				}
			}
		}

		String[] pk_accsubjs = null;
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				if (tmp_subjmap.get(tmp_sumdetails[i].getPk_accasoa()) == null) {
					vecSubjs.addElement(tmp_sumdetails[i].getPk_accasoa());
					tmp_subjmap.put(tmp_sumdetails[i].getPk_accasoa(), tmp_sumdetails[i].getPk_accasoa());
				}
			}
		}
		if (vecSubjs.size() > 0) {
			pk_accsubjs = new String[vecSubjs.size()];
			vecSubjs.copyInto(pk_accsubjs);
		}
		if ((pk_accsubjs == null) || (pk_accsubjs.length == 0))
			return null;
		GlQueryVO qryvo = new GlQueryVO();
		qryvo.setPk_account(pk_accsubjs);
		qryvo.setYear(year);
		qryvo.setPeriod(period);
		qryvo.setIncludeUnTallyed(true);
		qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

		qryvo.setBaseAccountingbook(voucher.getPk_accountingbook());
		qryvo.setGroupFields(new int[] { 4 });

		if (voucher.getVoucherkind().intValue() == 2) {

			String pk_orgbook = voucher.getPk_accountingbook();
			String[] str = GLPubProxy.getRemoteGlPara().getStartPeriod(pk_orgbook, "2002");

			String strPeriod = str[0].equals(year) ? str[1] : calendar.getFirstMonthOfCurrentYear().getAccperiodmth();

			qryvo.setSubjVerisonPeriod(strPeriod);
		} else {
			calendar.setDate(voucher.getPrepareddate());
			qryvo.setSubjVerisonPeriod(calendar.getMonthVO().getAccperiodmth());
		}
		qryvo.setSubjVersionYear(year);
		qryvo.setUseSubjVersion(true);
		qryvo.setSubjVersion(voucher.getPrepareddate().toStdString());

		GlBalanceVO[] rs = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

		if (rs != null) {
			UFDouble t_debitbalance = null;
			for (int i = 0; i < rs.length; i++) {
				debitbalance = (rs[i].getLocaldebitamount() == null ? new UFDouble(0) : rs[i].getLocaldebitamount()).sub(rs[i].getLocalcreditamount() == null ? new UFDouble(0) : rs[i].getLocalcreditamount());

				t_debitbalance = (UFDouble) balancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(debitbalance);

				balancemap.put(rs[i].getPk_accasoa(), t_debitbalance);

				groupDebitbalance = (rs[i].getDebitGroupAmount() == null ? new UFDouble(0) : rs[i].getDebitGroupAmount()).sub(rs[i].getCreditGroupAmount() == null ? new UFDouble(0) : rs[i].getCreditGroupAmount());

				t_debitbalance = (UFDouble) groupBalancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(groupDebitbalance);

				groupBalancemap.put(rs[i].getPk_accasoa(), t_debitbalance);

				globalDebitbalance = (rs[i].getDebitGlobalAmount() == null ? new UFDouble(0) : rs[i].getDebitGlobalAmount()).sub(rs[i].getCreditGlobalAmount() == null ? new UFDouble(0) : rs[i].getCreditGlobalAmount());

				t_debitbalance = (UFDouble) globalBalancemap.get(rs[i].getPk_accasoa());

				t_debitbalance = (t_debitbalance == null ? new UFDouble(0) : t_debitbalance).add(globalDebitbalance);

				globalBalancemap.put(rs[i].getPk_accasoa(), t_debitbalance);
			}
		}
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				debitbalance = (UFDouble) balancemap.get(tmp_sumdetails[i].getPk_accasoa());

				debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).add(tmp_sumdetails[i].getLocaldebitamount()).sub(tmp_sumdetails[i].getLocalcreditamount());

				balancemap.put(tmp_sumdetails[i].getPk_accasoa(), debitbalance);

				groupDebitbalance = (UFDouble) groupBalancemap.get(tmp_sumdetails[i].getPk_accasoa());

				groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).add(tmp_sumdetails[i].getGroupdebitamount()).sub(tmp_sumdetails[i].getGroupcreditamount());

				groupBalancemap.put(tmp_sumdetails[i].getPk_accasoa(), groupDebitbalance);

				globalDebitbalance = (UFDouble) globalBalancemap.get(tmp_sumdetails[i].getPk_accasoa());

				globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).add(tmp_sumdetails[i].getGlobaldebitamount()).sub(tmp_sumdetails[i].getGlobalcreditamount());

				globalBalancemap.put(tmp_sumdetails[i].getPk_accasoa(), globalDebitbalance);
			}
		}

		for (int i = 0; i < pk_accsubjs.length; i++) {
			debitbalance = (UFDouble) balancemap.get(pk_accsubjs[i]);
			groupDebitbalance = (UFDouble) groupBalancemap.get(pk_accsubjs[i]);
			globalDebitbalance = (UFDouble) globalBalancemap.get(pk_accsubjs[i]);

			if ((debitbalance != null) || (groupDebitbalance != null) || (globalDebitbalance != null)) {

				AccountVO accvo = (AccountVO) accsubjcache.get(pk_accsubjs[i]);
				if (accvo == null) {
					accvo = new AccountVO();
					accvo.setPk_accasoa(details[i].getPk_accasoa());
					accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

					accsubjcache.put(details[i].getPk_accasoa(), accvo);
				}
				if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
					if (debitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20030) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20031) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20032) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
				} else {
					if (debitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20030) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20031) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
					if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
						oprs = new OperationResultVO();
						oprs.m_intSuccess = infoLevel;
						oprs.m_strPK = voucher.getPk_voucher();
						oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20032) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

						vecresult.addElement(oprs);
					}
				}
			}
		}
		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkAssBalanceControl(VoucherVO voucher, HashMap accsubjcache, GLParameterVO param, Integer controlmode) throws Exception {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		HashMap balancemap = new HashMap();
		HashMap tmp_subjmap = new HashMap();

		OperationResultVO[] result = null;
		Vector vecresult = new Vector();
		DetailVO[] details = voucher.getDetails();

		DetailVO[] t_old_details = null;

		DetailVO[] tmp_sumdetails = null;

		Vector vecSubjs = new Vector();
		Vector vecSubjAsss = new Vector();
		ArrayList V_assarray = new ArrayList();
		AssVO[] V_ass = null;
		Vector assvecdetails = new Vector();
		HashMap asstemphashmap = new HashMap();

		if (details != null) {
			for (int i = 0; i < details.length; i++) {
				AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

				if (accvo == null) {

					accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

					accsubjcache.put(details[i].getPk_accasoa(), accvo);

					Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

					if ((subassvec != null) && (subassvec.size() > 0)) {
						accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
					}
				}
				if (accvo != null) {

					AccAssVO[] accass = accvo.getAccass();
					if ((accass != null) && (accass.length > 0)) {
						Boolean isasscontrol = new Boolean(false);
						for (int j = 0; j < accass.length; j++) {
							AccAssVO subjass = accass[j];
							if (subjass.getIsbalancecontrol().booleanValue()) {
								isasscontrol = new Boolean(true);
								break;
							}
						}
						if (!isasscontrol.booleanValue()) {
							break;
						}

						HashMap assvohm = new HashMap();
						ArrayList D_assarray = new ArrayList();
						DetailVO tmp_detail = (DetailVO) details[i].clone();
						tmp_detail.setUserData(null);
						if ((isasscontrol.booleanValue()) && (details[i].getAssid() != null) && ((details[i].getAss() == null) || (details[i].getAss().length == 0))) {

							details[i].setAss(((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class.getName())).queryAssvosByid(details[i].getAssid(), Module.GL));
						}

						for (int j = 0;

						(isasscontrol.booleanValue()) && (details[i].getAss() != null) && (j < details[i].getAss().length); j++) {
							assvohm.put(details[i].getAss()[j].getPk_Checktype(), details[i].getAss()[j]);

							asstemphashmap.put(details[i].getAss()[j].getPk_Checktype() + details[i].getAss()[j].getPk_Checkvalue(), details[i].getAss()[j]);
						}

						for (int j = 0; (isasscontrol.booleanValue()) && (j < accass.length); j++) {
							AccAssVO subjass = accass[j];
							if (subjass.getIsbalancecontrol().booleanValue()) {
								if (!vecSubjAsss.contains(accvo.getPk_accasoa())) {
									vecSubjAsss.addElement(accvo.getPk_accasoa());
								}

								AssVO tempass = (AssVO) assvohm.get(subjass.getPk_entity());

								tempass.setUserData(null);
								D_assarray.add(tempass);
								String hashkey = tempass.getPk_Checktype() + tempass.getPk_Checkvalue();

								if (asstemphashmap.get(hashkey) != null) {
									boolean include = false;
									for (int k = 0; k < V_assarray.size(); k++) {
										AssVO assVO = (AssVO) V_assarray.get(k);
										if (hashkey.equals(assVO.getPk_Checktype() + assVO.getPk_Checkvalue())) {

											include = true;
											break;
										}
									}
									if (!include) {
										V_assarray.add(tempass);
									}
								}
							}
						}

						if (D_assarray.size() > 0) {
							AssVO[] ass = new AssVO[D_assarray.size()];
							D_assarray.toArray(ass);
							tmp_detail.setAss(ass);
							assvecdetails.addElement(tmp_detail);
						}
					}
				}
			}
		}
		if (assvecdetails.size() > 0) {
			DetailVO[] t_details = new DetailVO[assvecdetails.size()];
			assvecdetails.copyInto(t_details);
			tmp_sumdetails = DetailTool.sumDetails(t_details, new int[] { 103, 303 });
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		if (voucher.getPk_voucher() != null) {
			t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = filterDetailForBalanceCtrl(t_old_details);
			t_old_details = catAss(t_old_details);
			Vector vecolddetails = new Vector();
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = new AccountVO();
						accvo.setPk_accasoa(details[i].getPk_accasoa());

						accvo = AccountUtilGL.queryByPks(new String[] { details[i].getPk_accasoa() })[0];

						accsubjcache.put(t_old_details[i].getPk_accasoa(), accvo);

						Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

						if ((subassvec != null) && (subassvec.size() > 0)) {
							accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
						}
					}
					AccAssVO subass = getAssbalanceByAcc(accvo, voucher);
					if (subass != null) {
						vecolddetails.addElement(t_old_details[i].clone());
					}
				}
			}
			if (vecolddetails.size() > 0) {
				t_old_details = new DetailVO[vecolddetails.size()];
				vecolddetails.copyInto(t_old_details);
				t_old_details = DetailTool.sumDetails(t_old_details, new int[] { 103, 303 });
			} else {
				t_old_details = null;
			}
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					UFDouble debitbalance = (UFDouble) balancemap.get(t_old_details[i].getPk_accasoa());

					debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).sub(t_old_details[i].getLocaldebitamount()).add(t_old_details[i].getLocalcreditamount());

					balancemap.put(t_old_details[i].getPk_accasoa(), debitbalance);

					if (tmp_subjmap.get(t_old_details[i].getPk_accasoa()) == null) {
						vecSubjs.addElement(t_old_details[i].getPk_accasoa());
						tmp_subjmap.put(t_old_details[i].getPk_accasoa(), t_old_details[i].getPk_accasoa());
					}
				}
			}
		}

		String[] pk_accsubjs = null;
		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				if (tmp_subjmap.get(tmp_sumdetails[i].getPk_accasoa()) == null) {
					vecSubjs.addElement(tmp_sumdetails[i].getPk_accasoa());
					tmp_subjmap.put(tmp_sumdetails[i].getPk_accasoa(), tmp_sumdetails[i].getPk_accasoa());
				}
			}
		}
		if (vecSubjs.size() > 0) {
			pk_accsubjs = new String[vecSubjs.size()];
			vecSubjs.copyInto(pk_accsubjs);
		}
		if (V_assarray.size() > 0) {
			V_ass = new AssVO[V_assarray.size()];
			V_assarray.toArray(V_ass);
		}
		if ((pk_accsubjs == null) || (pk_accsubjs.length == 0))
			return null;
		GlQueryVO qryvo = new GlQueryVO();
		qryvo.setPk_account(pk_accsubjs);
		qryvo.setAssVos(V_ass);
		qryvo.setYear(year);
		qryvo.setPeriod(period);
		qryvo.setIncludeUnTallyed(true);
		qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

		qryvo.setBaseAccountingbook(voucher.getPk_accountingbook());

		if (voucher.getVoucherkind().intValue() == 2) {
			if (voucher.getPeriod().equals("00")) {
				qryvo.setSubjVerisonPeriod(calendar.getFirstMonthOfCurrentYear().getAccperiodmth());
			} else {
				qryvo.setSubjVerisonPeriod(voucher.getPeriod());
			}
		} else {
			calendar.setDate(voucher.getPrepareddate());
			qryvo.setSubjVerisonPeriod(calendar.getMonthVO().getAccperiodmth());
		}
		qryvo.setSubjVersionYear(year);
		qryvo.setUseSubjVersion(true);

		qryvo.setGroupFields(new int[] { 4, 19 });

		GlBalanceVO[] rs = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

		if ((rs != null) && (rs.length > 0)) {
			GlAssDeal objTemp = new GlAssDeal();

			String[] strType = null;
			AssVO[] assvos = qryvo.getAssVos();
			if ((assvos != null) && (assvos.length != 0)) {
				strType = new String[assvos.length];
				for (int i = 0; i < assvos.length; i++) {
					strType[i] = assvos[i].getPk_Checktype();
				}
			}
			objTemp.setMatchingIndex(19);
			objTemp.setAppendIndex(20);

			objTemp.dealWith(rs, strType);

			rs = combineAss(rs, qryvo);
		}
		if ((voucher.getPk_voucher() != null) && (t_old_details != null) && (t_old_details.length > 0)) {
			for (int i = 0; i < t_old_details.length; i++) {
				boolean ismatch = false;
				DetailVO olddetail = t_old_details[i];
				if (rs != null) {
					for (int j = 0; j < rs.length; j++) {
						ismatch = olddetail.getPk_accasoa().equals(rs[j].getPk_accasoa());

						AssVO[] sumass = olddetail.getAss();
						AssVO[] balanceass = rs[j].getAssVos();
						ismatch = (ismatch) && (new CAssSortTool().compare(sumass, balanceass) == 0);

						if (ismatch) {
							UFDouble debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

							debitbalance = debitbalance.sub((olddetail.getLocaldebitamount() == null ? new UFDouble(0) : olddetail.getLocaldebitamount()).sub(olddetail.getLocalcreditamount() == null ? new UFDouble(0) : olddetail.getLocalcreditamount()));

							AccountVO accvo = (AccountVO) accsubjcache.get(pk_accsubjs[i]);

							if (accvo == null) {

								accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

								accsubjcache.put(details[i].getPk_accasoa(), accvo);
							}

							if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
								if (debitbalance.compareTo(new UFDouble(-1.0E-8D)) >= 0)
									break;
								OperationResultVO oprs = new OperationResultVO();
								switch (controlmode.intValue()) {
									case 1:
										oprs.m_intSuccess = 1;
										break;

									case 2:
										oprs.m_intSuccess = 2;
										break;

									default:
										throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
								}

								oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

								vecresult.addElement(oprs);
								break;
							}
							if (debitbalance.compareTo(new UFDouble(1.0E-8D)) <= 0)
								break;
							OperationResultVO oprs = new OperationResultVO();
							switch (controlmode.intValue()) {
								case 1:
									oprs.m_intSuccess = 1;
									break;

								case 2:
									oprs.m_intSuccess = 2;
									break;

								default:
									throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
							}

							oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

							vecresult.addElement(oprs);
							break;
						}
					}
				}
			}
		}

		if (tmp_sumdetails != null) {
			for (int i = 0; i < tmp_sumdetails.length; i++) {
				boolean ismatch = false;
				DetailVO sumdetail = tmp_sumdetails[i];
				if (rs != null) {
					for (int j = 0; j < rs.length; j++) {
						ismatch = sumdetail.getPk_accasoa().equals(rs[j].getPk_accasoa());

						AssVO[] sumass = sumdetail.getAss();
						AssVO[] balanceass = rs[j].getAssVos();
						ismatch = (ismatch) && (new CAssSortTool().compare(sumass, balanceass) == 0);

						if (ismatch) {
							UFDouble debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

							debitbalance = debitbalance.add((sumdetail.getLocaldebitamount() == null ? new UFDouble(0) : sumdetail.getLocaldebitamount()).sub(sumdetail.getLocalcreditamount() == null ? new UFDouble(0) : sumdetail.getLocalcreditamount()));

							AccountVO accvo = (AccountVO) accsubjcache.get(rs[j].getPk_accasoa());

							if (accvo == null) {

								accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

								accsubjcache.put(details[i].getPk_accasoa(), accvo);
							}

							if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
								if (debitbalance.compareTo(new UFDouble(-1.0E-8D)) >= 0)
									break;
								OperationResultVO oprs = new OperationResultVO();
								switch (controlmode.intValue()) {
									case 1:
										oprs.m_intSuccess = 1;
										break;

									case 2:
										oprs.m_intSuccess = 2;
										break;

									default:
										throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
								}

								oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

								vecresult.addElement(oprs);
								break;
							}
							if (debitbalance.compareTo(new UFDouble(1.0E-8D)) <= 0)
								break;
							OperationResultVO oprs = new OperationResultVO();
							switch (controlmode.intValue()) {
								case 1:
									oprs.m_intSuccess = 1;
									break;

								case 2:
									oprs.m_intSuccess = 2;
									break;

								default:
									throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
							}

							oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20010) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

							vecresult.addElement(oprs);
							break;
						}
					}
				}
			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	private boolean isAssEqual(AssVO assVo1, AssVO assVo2) {
		if (!assVo1.getPk_Checktype().equals(assVo2.getPk_Checktype())) {
			return false;
		}

		String pk_Checkvalue1 = assVo1.getPk_Checkvalue();
		String pk_Checkvalue2 = assVo2.getPk_Checkvalue();

		if (pk_Checkvalue1 == null)
			pk_Checkvalue1 = "~";
		if (pk_Checkvalue2 == null)
			pk_Checkvalue2 = "~";
		return pk_Checkvalue1.equals(pk_Checkvalue2);
	}

	private AssVO matchAssBalance(AccAssVO accAssVo, AssVO[] ass) {
		if ((accAssVo == null) || (ass == null)) {
			return null;
		}
		if (!accAssVo.getIsbalancecontrol().booleanValue()) {
			return null;
		}
		String pk_checktype = accAssVo.getPk_entity();
		if (pk_checktype != null) {
			for (int i = 0; i < ass.length; i++) {
				AssVO assVO = ass[i];
				if (assVO.getPk_Checktype().equals(pk_checktype)) {
					return assVO;
				}
			}
		}
		return null;
	}

	protected OperationResultVO[] checkAssBalanceControlNew(VoucherVO voucher, HashMap accsubjcache, GLParameterVO param, Integer controlmode) throws Exception {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		HashMap balancemap = new HashMap();
		HashMap groupBalancemap = new HashMap();
		HashMap globalBalancemap = new HashMap();
		HashMap oldbalancemap = new HashMap();
		HashMap oldGroupBalancemap = new HashMap();
		HashMap oldGlobalBalancemap = new HashMap();
		HashMap<String, Vector> accsubjmap = new HashMap();
		HashMap assmap = new HashMap();
		Vector vecresult = new Vector();
		OperationResultVO[] result = null;

		String balabcekey = null;
		UFDouble debitbalance = null;
		UFDouble groupDebitbalance = null;
		UFDouble globalDebitbalance = null;

		OperationResultVO oprs = null;
		int infoLevel = 0;

		switch (controlmode.intValue()) {
			case 1:
				infoLevel = 1;
				break;

			case 2:
				infoLevel = 2;
				break;

			default:
				throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
		}

		DetailVO[] details = voucher.getDetails();
		details = filterDetailForBalanceCtrl(details);
		DetailVO[] t_old_details = voucher.getDetails();
		details = catAss(details);

		for (int i = 0; (details != null) && (i < details.length); i++) {
			AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

			if (accvo == null) {
				accvo = new AccountVO();
				accvo.setPk_accasoa(details[i].getPk_accasoa());

				accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

				accsubjcache.put(details[i].getPk_accasoa(), accvo);

				Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

				if ((subassvec != null) && (subassvec.size() > 0)) {
					accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
				}
			}
			if (accvo != null) {

				AccAssVO[] accass = accvo.getAccass();
				if ((accass != null) && (accass.length > 0)) {
					if (details[i].getAss() != null) {
						details[i].setAss(((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class.getName())).queryAssvosByid(details[i].getAssid(), Module.GL));
					}

					for (int j = 0; j < accass.length; j++) {
						AssVO assvo = matchAssBalance(accass[j], details[i].getAss());

						if (assvo != null) {

							balabcekey = details[i].getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

							debitbalance = balancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) balancemap.get(balabcekey);

							debitbalance = (debitbalance == null ? new UFDouble(0) : debitbalance).add((details[i].getLocaldebitamount() == null ? new UFDouble(0) : details[i].getLocaldebitamount()).sub(details[i].getLocalcreditamount() == null ? new UFDouble(0) : details[i].getLocalcreditamount()));

							balancemap.put(balabcekey, debitbalance);
							groupDebitbalance = groupBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) groupBalancemap.get(balabcekey);

							groupDebitbalance = (groupDebitbalance == null ? new UFDouble(0) : groupDebitbalance).add((details[i].getGroupdebitamount() == null ? new UFDouble(0) : details[i].getGroupdebitamount()).sub(details[i].getGroupcreditamount() == null ? new UFDouble(0) : details[i].getGroupcreditamount()));

							groupBalancemap.put(balabcekey, groupDebitbalance);
							globalDebitbalance = globalBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) globalBalancemap.get(balabcekey);

							globalDebitbalance = (globalDebitbalance == null ? new UFDouble(0) : globalDebitbalance).add((details[i].getGlobaldebitamount() == null ? new UFDouble(0) : details[i].getGlobaldebitamount()).sub(details[i].getGlobalcreditamount() == null ? new UFDouble(0) : details[i].getGlobalcreditamount()));

							globalBalancemap.put(balabcekey, globalDebitbalance);

							if (accsubjmap.get(accvo.getPk_accasoa()) == null) {
								Vector vector = new Vector();
								vector.add(assvo);

								accsubjmap.put(accvo.getPk_accasoa(), vector);
							} else {
								Vector vector = (Vector) accsubjmap.get(accvo.getPk_accasoa());

								boolean equalFlag = false;
								for (int row = 0; row < vector.size(); row++) {
									Object object = vector.get(row);
									AssVO oldAssVo = (AssVO) object;
									if (isAssEqual(oldAssVo, assvo)) {
										equalFlag = true;
									}
								}

								if (!equalFlag)
									vector.add(assvo);
							}
							if (assmap.get(assvo.getPk_Checktype() + assvo.getPk_Checkvalue()) == null) {
								assmap.put(assvo.getPk_Checktype() + assvo.getPk_Checkvalue(), assvo);
							}
						}
					}
				}
			}
		}

		if (voucher.getPk_voucher() != null) {
			t_old_details = new DetailExtendDMO().queryByVoucherPks(new String[] { voucher.getPk_voucher() });

			t_old_details = filterDetailForBalanceCtrl(t_old_details);
			getAccountMap(t_old_details, accsubjcache);
			t_old_details = catAss(t_old_details);
			if (t_old_details != null) {
				for (int i = 0; i < t_old_details.length; i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(t_old_details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = new AccountVO();
						accvo.setPk_accasoa(t_old_details[i].getPk_accasoa());

						accvo = AccountUtilGL.findAccountVOByPrimaryKey(t_old_details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

						accsubjcache.put(t_old_details[i].getPk_accasoa(), accvo);

						Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

						if ((subassvec != null) && (subassvec.size() > 0)) {
							accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
						}
					}
					if (accvo != null) {

						AccAssVO[] accass = accvo.getAccass();
						if ((accass != null) && (accass.length > 0)) {
							for (int j = 0; j < accass.length; j++) {
								AssVO assvo = matchAssBalance(accass[j], t_old_details[i].getAss());

								if (assvo != null) {

									balabcekey = t_old_details[i].getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

									debitbalance = oldbalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldbalancemap.get(balabcekey);

									debitbalance = debitbalance.add((t_old_details[i].getLocaldebitamount() == null ? new UFDouble(0) : t_old_details[i].getLocaldebitamount()).sub(t_old_details[i].getLocalcreditamount() == null ? new UFDouble(0) : t_old_details[i].getLocalcreditamount()));

									oldbalancemap.put(balabcekey, debitbalance);
									groupDebitbalance = oldGroupBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldGroupBalancemap.get(balabcekey);

									groupDebitbalance = groupDebitbalance.add((t_old_details[i].getGroupdebitamount() == null ? new UFDouble(0) : t_old_details[i].getGroupdebitamount()).sub(t_old_details[i].getGroupcreditamount() == null ? new UFDouble(0) : t_old_details[i].getGroupcreditamount()));

									oldGroupBalancemap.put(balabcekey, groupDebitbalance);

									globalDebitbalance = oldGlobalBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) oldGlobalBalancemap.get(balabcekey);

									globalDebitbalance = globalDebitbalance.add((t_old_details[i].getGlobaldebitamount() == null ? new UFDouble(0) : t_old_details[i].getGlobaldebitamount()).sub(t_old_details[i].getGlobalcreditamount() == null ? new UFDouble(0) : t_old_details[i].getGlobalcreditamount()));

									oldGlobalBalancemap.put(balabcekey, globalDebitbalance);

									if (accsubjmap.get(accvo.getPk_accasoa()) == null) {
										Vector vector = new Vector();
										vector.add(assvo);
										accsubjmap.put(accvo.getPk_accasoa(), vector);
									} else {
										Vector vector = (Vector) accsubjmap.get(accvo.getPk_accasoa());

										boolean equalFlag = false;
										for (int row = 0; row < vector.size(); row++) {
											Object object = vector.get(row);
											AssVO oldAssVo = (AssVO) object;
											if (isAssEqual(oldAssVo, assvo)) {
												equalFlag = true;
											}
										}

										if (!equalFlag)
											vector.add(assvo);
									}
									if (assmap.get(assvo.getPk_Checktype() + assvo.getPk_Checkvalue()) == null) {
										assmap.put(assvo.getPk_Checktype() + assvo.getPk_Checkvalue(), assvo);
									}
								}
							}
						}
					}
				}
			}
		}

		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

		calendar.set(voucher.getYear());
		String year = voucher.getYear();
		String period = calendar.getLastMonthOfCurrentYear().getAccperiodmth();

		String[] pk_accsubjs = null;
		if ((accsubjmap != null) && (accsubjmap.size() > 0)) {
			pk_accsubjs = new String[accsubjmap.size()];
			pk_accsubjs = (String[]) accsubjmap.keySet().toArray(pk_accsubjs);
		}

		GlBalanceVO[] rs = null;
		Vector<GlBalanceVO> glv = new Vector();

		if ((pk_accsubjs != null) && (pk_accsubjs.length > 0)) {
			Iterator iter;
			for (int j = 0; j < pk_accsubjs.length; j++) {
				Vector<AssVO> assvo = null;

				if ((accsubjmap.get(pk_accsubjs[j]) instanceof Vector)) {
					assvo = (Vector) accsubjmap.get(pk_accsubjs[j]);
				}

				for (iter = assvo.iterator(); iter.hasNext();) {
					AssVO element = (AssVO) iter.next();
					GlQueryVO qryvo = new GlQueryVO();
					qryvo.setPk_account(new String[] { pk_accsubjs[j] });

					qryvo.setAssVos(new AssVO[] { element });

					qryvo.setYear(year);
					qryvo.setPeriod(period);
					qryvo.setIncludeUnTallyed(true);
					qryvo.setpk_accountingbook(new String[] { voucher.getPk_accountingbook() });

					qryvo.setBaseAccountingbook(voucher.getPk_accountingbook());
					if (voucher.getVoucherkind().intValue() == 2) {
						if (voucher.getPeriod().equals("00")) {
							qryvo.setSubjVerisonPeriod(calendar.getFirstMonthOfCurrentYear().getAccperiodmth());
						} else {
							qryvo.setSubjVerisonPeriod(voucher.getPeriod());
						}
					} else {
						calendar.setDate(voucher.getPrepareddate());
						qryvo.setSubjVerisonPeriod(calendar.getMonthVO().getAccperiodmth());
					}

					qryvo.setSubjVersionYear(year);
					qryvo.setUseSubjVersion(true);
					qryvo.setSubjVersion(voucher.getPrepareddate().toStdString());

					qryvo.setGroupFields(new int[] { 4, 19 });

					GlBalanceVO[] rs1 = ((ICommAccBookPub) NCLocator.getInstance().lookup(ICommAccBookPub.class)).getEndBalance(qryvo);

					if ((rs1 != null) && (rs1.length > 0)) {
						GlAssDeal objTemp = new GlAssDeal();

						String[] strType = null;
						AssVO[] assvos = qryvo.getAssVos();
						if ((assvos != null) && (assvos.length != 0)) {
							strType = new String[assvos.length];
							for (int i = 0; i < assvos.length; i++) {
								strType[i] = assvos[i].getPk_Checktype();
							}
						}
						objTemp.setMatchingIndex(19);
						objTemp.setAppendIndex(20);

						objTemp.dealWith(rs1, strType);

						rs1 = combineAss(rs1, qryvo);
						for (int i = 0; i < rs1.length; i++) {
							GlBalanceVO glb = rs1[i];
							glv.addElement(glb);
						}
					}
				}
			}
		}
		rs = new GlBalanceVO[glv.size()];
		rs = (GlBalanceVO[]) glv.toArray(rs);

		for (int j = 0; j < rs.length; j++) {
			debitbalance = (rs[j].getLocaldebitamount() == null ? new UFDouble(0) : rs[j].getLocaldebitamount()).sub(rs[j].getLocalcreditamount() == null ? new UFDouble(0) : rs[j].getLocalcreditamount());

			groupDebitbalance = (rs[j].getDebitGroupAmount() == null ? new UFDouble(0) : rs[j].getDebitGroupAmount()).sub(rs[j].getCreditGroupAmount() == null ? new UFDouble(0) : rs[j].getCreditGroupAmount());

			globalDebitbalance = (rs[j].getDebitGlobalAmount() == null ? new UFDouble(0) : rs[j].getDebitGlobalAmount()).sub(rs[j].getCreditGlobalAmount() == null ? new UFDouble(0) : rs[j].getCreditGlobalAmount());

			AccountVO accvo = (AccountVO) accsubjcache.get(rs[j].getPk_accasoa());

			if (accvo == null) {
				accvo = new AccountVO();
				accvo.setPk_accasoa(rs[j].getPk_accasoa());

				accvo = AccountUtilGL.findAccountVOByPrimaryKey(rs[j].getPk_accasoa(), voucher.getPrepareddate().toStdString());

				accsubjcache.put(rs[j].getPk_accasoa(), accvo);
			}

			AssVO assvo = null;
			if ((rs[j].getAssVos() != null) && (rs[j].getAssVos().length > 0)) {
				assvo = rs[j].getAssVos()[0];
			}
			UFDouble oldbalance = null;
			UFDouble oldGroupBalance = null;
			UFDouble oldGlobalBalance = null;
			UFDouble balance = null;
			UFDouble groupBalance = null;
			UFDouble globalBalance = null;
			if (assvo != null) {
				balabcekey = accvo.getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

				oldbalance = oldbalancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) oldbalancemap.get(balabcekey);

				balance = (UFDouble) balancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) balancemap.get(balabcekey);

				balancemap.remove(balabcekey);
				oldGroupBalance = oldGroupBalancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) oldGroupBalancemap.get(balabcekey);

				groupBalance = (UFDouble) groupBalancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) groupBalancemap.get(balabcekey);

				groupBalancemap.remove(balabcekey);
				oldGlobalBalance = oldGlobalBalancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) oldGlobalBalancemap.get(balabcekey);

				globalBalance = (UFDouble) globalBalancemap.get(balabcekey) == null ? UFDouble.ZERO_DBL : (UFDouble) globalBalancemap.get(balabcekey);

				globalBalancemap.remove(balabcekey);
			}
			debitbalance = debitbalance.sub(oldbalance == null ? new UFDouble(0) : oldbalance);

			debitbalance = debitbalance.add(balance == null ? new UFDouble(0) : balance);

			groupDebitbalance = groupDebitbalance.sub(oldGroupBalance == null ? new UFDouble(0) : oldGroupBalance);

			groupDebitbalance = groupDebitbalance.add(groupBalance == null ? new UFDouble(0) : groupBalance);

			globalDebitbalance = globalDebitbalance.sub(oldGlobalBalance == null ? new UFDouble(0) : oldGlobalBalance);

			globalDebitbalance = globalDebitbalance.add(globalBalance == null ? new UFDouble(0) : globalBalance);

			if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
				if (debitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
			} else {
				if (debitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
				if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
					oprs = new OperationResultVO();
					oprs.m_intSuccess = infoLevel;
					oprs.m_strPK = voucher.getPk_voucher();
					oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

					vecresult.addElement(oprs);
				}
			}
		}

		if ((rs == null) || (rs.length == 0) || (balancemap.size() > 0) || (groupBalancemap.size() > 0) || (globalBalancemap.size() > 0)) {
			if ((balancemap.size() > 0) || (groupBalancemap.size() > 0) || (globalBalancemap.size() > 0)) {
				for (int i = 0; (details != null) && (i < details.length); i++) {
					AccountVO accvo = (AccountVO) accsubjcache.get(details[i].getPk_accasoa());

					if (accvo == null) {
						accvo = new AccountVO();
						accvo.setPk_accasoa(details[i].getPk_accasoa());

						accvo = AccountUtilGL.findAccountVOByPrimaryKey(details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());

						accsubjcache.put(details[i].getPk_accasoa(), accvo);

						Vector<AccAssVO> subassvec = AccAssGL.getAccAssVOsByAccount(voucher.getPk_accountingbook(), accvo, voucher.getPrepareddate().toStdString());

						if ((subassvec != null) && (subassvec.size() > 0)) {
							accvo.setAccass((AccAssVO[]) subassvec.toArray(new AccAssVO[0]));
						}
					}
					if (accvo != null) {

						AccAssVO[] accass = accvo.getAccass();
						if ((accass != null) && (accass.length > 0)) {
							if (details[i].getAss() == null) {
								details[i].setAss(((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class.getName())).queryAssvosByid(details[i].getAssid(), Module.GL));
							}

							for (int j = 0; j < accass.length; j++) {
								AssVO assvo = matchAssBalance(accass[j], details[i].getAss());

								if (assvo != null) {

									balabcekey = details[i].getPk_accasoa() + assvo.getPk_Checktype() + assvo.getPk_Checkvalue();

									debitbalance = balancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) balancemap.get(balabcekey);

									groupDebitbalance = groupBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) groupBalancemap.get(balabcekey);

									globalDebitbalance = globalBalancemap.get(balabcekey) == null ? new UFDouble(0) : (UFDouble) globalBalancemap.get(balabcekey);

									if ((accvo != null) && (accvo.getBalanorient().intValue() == 0)) {
										if (debitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
										if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
										if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) < 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
									} else {
										if (debitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20033) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
										if (groupDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20034) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
										if (globalDebitbalance.compareTo(UFDouble.ZERO_DBL) > 0) {
											oprs = new OperationResultVO();
											oprs.m_intSuccess = infoLevel;
											oprs.m_strPK = voucher.getPk_voucher();
											oprs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20035) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000577") + accvo.getCode());

											vecresult.addElement(oprs);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkDetail(VoucherVO voucher, String sysid, int i, HashMap tempaccsubj, GLParameterVO glparam) throws Exception {
		OperationResultVO[] result = null;
		voucher.getDetail(i).setDetailindex(new Integer(i + 1));
		voucher.getDetail(i).setPk_org(voucher.getPk_org());
		voucher.getDetail(i).setPk_accountingbook(voucher.getPk_accountingbook());
		voucher.getDetail(i).setPk_glorg(voucher.getPk_org());
		voucher.getDetail(i).setPk_glbook(voucher.getPk_setofbook());
		DetailVO detail = voucher.getDetail(i);
		result = checkExplanatin(i, result, detail);
		result = checkBusiUnit(i, result, detail);
		result = checkAccasoa(i, result, detail);
		result = checkLocalAmount(voucher, i, result, detail);
		result = checkCurrtype(voucher, i, glparam, result, detail);

		boolean isEurUse = false;
		AccountingBookVO accountBookVo = AccountBookUtil.getAccountingBookVOByPrimaryKey(voucher.getPk_accountingbook());
		if (accountBookVo != null) {
			String pk_group = accountBookVo.getPk_group();
			isEurUse = GLStartCheckUtil.checkEURStart(pk_group);
		}
		if (isEurUse) {
			result = checkVatDetail(i, result, detail);
		} else {
			detail.setVatdetail(null);
		}

		AccountVO accvo = null;
		try {
			accvo = (AccountVO) tempaccsubj.get(voucher.getDetail(i).getPk_accasoa());
		} catch (ClassCastException e) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10050) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000578", null, new String[] { String.valueOf(i + 1) }) + voucher.getDetail(i).getPk_accasoa() + "}");

			return OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		if (accvo == null) {
			result = checkAccasoaInfo(i, result);
			return result;
		}
		processUnit(voucher, i, accvo);
		result = checkControlSystem(voucher, i, result, accvo);
		result = checkEndAccasoa(i, result, accvo);
		processCashtype(voucher, i, accvo);
		OperationResultVO[] t_result = checkVoucherAbsolutely(voucher, accvo, i);
		result = OperationResultVO.appendResultVO(result, t_result);
		result = checkSealAccasoa(i, result, accvo);
		if ((voucher.getDetail(i).getAssid() != null) && (voucher.getDetail(i).getAssid().trim().equals(""))) {
			voucher.getDetail(i).setAssid(null);
			voucher.getDetail(i).setAss(null);
		}
		result = checkAss(voucher, i, glparam, result, accvo);
		result = checkAssNotNull(voucher, i, result, accvo);
		result = checkOccurAmountAccasoa(voucher, i, result, detail, accvo);
		result = checkOutAccasoa(voucher, i, result, accvo);
		return result;
	}

	private OperationResultVO[] checkVatDetail(int i, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getVatdetail() != null) && ((detail.getVatdetail().getMoneyamount() == null) || (UFDouble.ZERO_DBL.equals(detail.getVatdetail().getMoneyamount()))) && ((detail.getVatdetail().getTaxamount() == null) || (UFDouble.ZERO_DBL.equals(detail.getVatdetail().getTaxamount())))) {

			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20040) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkBusiUnit(int i, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getPk_unit() == null) || (detail.getPk_unit().equals(""))) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20017) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		if ((detail.getPk_unit_v() == null) || (detail.getPk_unit_v().equals(""))) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20018) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkAss(VoucherVO voucher, int i, GLParameterVO glparam, OperationResultVO[] result, AccountVO accvo) throws GLBusinessException, Exception, NamingException, SystemException {
		boolean isCheckEmpty = true;
		if ("GLRC".equals(voucher.getPk_system())) {
			isCheckEmpty = false;
		}

		AccAssVO[] accass = accvo.getAccass();
		AssVO[] detailass = voucher.getDetail(i).getAss();
		if ((accass != null) && (accass.length > 0)) {
			if (voucher.getDetail(i).getAssid() == null) {
				if ((detailass == null) || (detailass.length == 0)||(null==detailass[0].getCheckvaluecode())||(""==detailass[0].getCheckvaluecode().trim())) {
					for (AccAssVO accAssVO : accass) {
						if ((accAssVO.getIsempty() != null) && (!accAssVO.getIsempty().booleanValue()) && (isCheckEmpty)) {

							OperationResultVO rs = new OperationResultVO();
							rs.m_intSuccess = 2;
							rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10022) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

							rs.m_strDescription += AccAssItemCache.getAccAssitemNameByPK(accAssVO.getPk_entity());

							result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
						} else {
							detailass = new AssVO[accass.length];
							for (int m = 0; m < detailass.length; m++) {
								detailass[m] = new AssVO();
								detailass[m].setPk_Checktype(accass[m].getPk_entity());
							}

							IFreevaluePub freevalue = (IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class);

							voucher.getDetail(i).setAssid(freevalue.getAssID(detailass, Boolean.TRUE, voucher.getPk_group(), Module.GL));

							voucher.getDetail(i).setAss(detailass);
						}
					}
				} else {
					HashMap tmpassmap = new HashMap();
					for (int j = 0; j < detailass.length; j++) {
						tmpassmap.put(detailass[j].getPk_Checktype(), detailass[j]);
					}

					Vector tmp_detailass = new Vector();
					detailass = new AssVO[tmp_detailass.size()];
					tmp_detailass.copyInto(detailass);
					IFreevaluePub freevalue = (IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class);

					voucher.getDetail(i).setAssid(freevalue.getAssID(detailass, Boolean.TRUE, voucher.getPk_group(), Module.GL));

					voucher.getDetail(i).setAss(detailass);
				}
			} else {
				if (detailass == null) {
					detailass = ((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class)).queryAssvosByid(voucher.getDetail(i).getAssid(), Module.GL);

					voucher.getDetail(i).setAss(detailass);
				}

				if ((detailass == null) || (detailass.length == 0)) {
					if ((!glparam.Parameter_isfreevalueallownull.booleanValue()) && (isCheckEmpty)) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10022) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					} else {
						detailass = new AssVO[accass.length];
						for (int m = 0; m < detailass.length; m++) {
							detailass[m] = new AssVO();
							detailass[m].setPk_Checktype(accass[m].getPk_entity());
						}

						IFreevaluePub freevalue = (IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class);

						voucher.getDetail(i).setAssid(freevalue.getAssID(detailass, Boolean.TRUE, voucher.getPk_group(), Module.GL));

						voucher.getDetail(i).setAss(detailass);
					}
				} else {
					HashMap tmpassmap = new HashMap();
					for (int j = 0; j < detailass.length; j++) {
						if (tmpassmap.get(detailass[j].getPk_Checktype()) != null) {
							OperationResultVO rs = new OperationResultVO();
							rs.m_intSuccess = 2;
							rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10024) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000580", null, new String[] { String.valueOf(i + 1) }) + voucher.getDetail(i).getAssid());

							result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
						}

						tmpassmap.put(detailass[j].getPk_Checktype(), detailass[j]);
					}

					Vector tmp_detailass = new Vector();
					for (int j = 0; j < accass.length; j++) {
						AccAssVO assVO = accass[j];
						AssVO tmp_assvo = (AssVO) tmpassmap.get(assVO.getPk_entity());

						if ((tmp_assvo == null) || (StringUtils.isEmpty(tmp_assvo.getPk_Checkvalue())) || ("~".equals(tmp_assvo.getPk_Checkvalue()))) {

							if ((assVO.getIsempty() != null) && (!assVO.getIsempty().booleanValue()) && (isCheckEmpty)) {

								OperationResultVO rs = new OperationResultVO();
								rs.m_intSuccess = 2;
								rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20014) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

								result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
							} else {
								tmp_assvo = new AssVO();
								tmp_assvo.setPk_Checktype(assVO.getPk_entity());
								tmp_detailass.addElement(tmp_assvo);
							}
						} else {
							tmp_detailass.addElement(tmp_assvo);
						}
					}
				}
			}
		}

		return result;
	}

	private OperationResultVO[] checkOutAccasoa(VoucherVO voucher, int i, OperationResultVO[] result, AccountVO accvo) {
		if ((voucher.getIsOutSubj() != null) && (voucher.getIsOutSubj().booleanValue()) && ((accvo.getOutflag() == null) || (!accvo.getOutflag().booleanValue()))) {

			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20006) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkOccurAmountAccasoa(VoucherVO voucher, int i, OperationResultVO[] result, DetailVO detail, AccountVO accvo) {
		if ((accvo.getIncurflag() != null) && (accvo.getIncurflag().booleanValue()) && (!voucher.getPk_system().trim().equals("PLCF")) && (!voucher.getPk_system().trim().equals("GLRC")) && (voucher.getVoucherkind().intValue() != 1)) {

			switch (accvo.getBalanorient().intValue()) {
				case 0:
					if (!detail.getLocalcreditamount().equals(new UFDouble(0))) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10026) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					}
					break;

				case 1:
					if (!detail.getLocaldebitamount().equals(new UFDouble(0))) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10027) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					}

					break;
			}

		}
		return result;
	}

	private OperationResultVO[] checkAssNotNull(VoucherVO voucher, int i, OperationResultVO[] result, AccountVO accvo) {
		AccAssVO[] accass = accvo.getAccass();
		OperationResultVO rs = new OperationResultVO();
		if (((accass == null) || (accass.length <= 0)) && (voucher.getDetail(i).getAssid() != null)) {

			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10025) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkSealAccasoa(int i, OperationResultVO[] result, AccountVO accvo) {
		if (accvo.getEnablestate().intValue() != 2) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10021) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private void processCashtype(VoucherVO voucher, int i, AccountVO accvo) {
		if (accvo.getCashtype().intValue() != 2) {
			voucher.getDetail(i).setCheckdate(null);
			voucher.getDetail(i).setCheckno(null);
			voucher.getDetail(i).setCheckstyle(null);
			voucher.getDetail(i).setCheckstylename(null);
			voucher.getDetail(i).setBilltype(null);
		}
		if ((accvo.getCashtype().intValue() == 1) || (accvo.getCashtype().intValue() == 2) || (accvo.getCashtype().intValue() == 3)) {

			voucher.setSignflag(UFBoolean.TRUE);
		}
	}

	private OperationResultVO[] checkEndAccasoa(int i, OperationResultVO[] result, AccountVO accvo) {
		if (!accvo.getEndflag().booleanValue()) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10020) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkControlSystem(VoucherVO voucher, int i, OperationResultVO[] result, AccountVO accvo) {
		if ((accvo.getCtrlmodules() != null) && (accvo.getCtrlmodules().trim().length() != 0) && (!accvo.getCtrlmodules().equals("CV"))) {

			StringTokenizer st = new StringTokenizer(accvo.getCtrlmodules(), ",");

			boolean isctl = false;
			String str = null;
			while (st.hasMoreTokens()) {
				str = st.nextToken();
				if (GLSystemControlTool.checkAccountControl(voucher.getPk_system(), str).booleanValue()) {
					isctl = true;
				}
			}

			if (!isctl) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10019) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		return result;
	}

	private void processUnit(VoucherVO voucher, int i, AccountVO accvo) {
		if (accvo.getUnit() == null) {
			voucher.getDetail(i).setDebitquantity(new UFDouble(0));
			voucher.getDetail(i).setCreditquantity(new UFDouble(0));
			voucher.getDetail(i).setPrice(new UFDouble(0));
		}
	}

	private OperationResultVO[] checkAccasoaInfo(int i, OperationResultVO[] result) {
		OperationResultVO rs = new OperationResultVO();
		rs.m_intSuccess = 2;
		rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10044) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

		result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

		return result;
	}

	private OperationResultVO[] checkCurrtype(VoucherVO voucher, int i, GLParameterVO glparam, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getPk_currtype() == null) || (detail.getPk_currtype().trim().length() == 0)) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10018) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

		} else if ((detail.getPk_currtype().equals(glparam.Parameter_pk_localcurr)) && (detail.getDebitamount().equals(new UFDouble(0))) && (detail.getCreditamount().equals(new UFDouble(0)))) {
			voucher.getDetail(i).setDebitamount(detail.getLocaldebitamount());

			voucher.getDetail(i).setCreditamount(detail.getLocalcreditamount());
		}

		return result;
	}

	private OperationResultVO[] checkLocalAmount(VoucherVO voucher, int i, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getLocalcreditamount().equals(new UFDouble(0))) && (detail.getLocaldebitamount().equals(new UFDouble(0))) && (detail.getVatdetail() == null)) {

			if ((voucher.getVoucherkind().intValue() != 3) && (!"EGL".equals(voucher.getPk_system()))) {

				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10016) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			} else if ((detail.getCreditquantity().equals(new UFDouble(0))) && (detail.getDebitquantity().equals(new UFDouble(0)))) {

				if (!"EGL".equals(voucher.getPk_system())) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10017) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
		}

		return result;
	}

	private OperationResultVO[] checkAccasoa(int i, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getPk_accasoa() == null) || (detail.getPk_accasoa().equals(""))) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10015) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkExplanatin(int i, OperationResultVO[] result, DetailVO detail) {
		if ((detail.getExplanation() == null) || (detail.getExplanation().equals(""))) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10014) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	private OperationResultVO[] checkVoucherAbsolutely(VoucherVO voucher, AccountVO accvo, int i) {
		OperationResultVO[] result = null;
		if (accvo.getUnit() != null) {
			if ((accvo.getQuantity().booleanValue()) && (voucher.getDetail(i).getDebitquantity().equals(new UFDouble(0))) && (voucher.getDetail(i).getCreditquantity().equals(new UFDouble(0)))) {

				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10055) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}

			if ((accvo.getPrice().booleanValue()) && (voucher.getDetail(i).getPrice().equals(new UFDouble(0)))) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10056) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		if ((accvo.getInneracc() != null) && (accvo.getInneracc().booleanValue()) && (accvo.getInnerinfo().booleanValue()) && ((voucher.getDetail(i).getInnerbusno() == null) || (voucher.getDetail(i).getInnerbusdate() == null))) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20007) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		if ((accvo.getCashtype() != null) && (accvo.getCashtype().intValue() == 2)) {
			if ((null != accvo.getBalancetype()) && (accvo.getBalancetype().booleanValue())) {
				if (voucher.getDetail(i).getCheckstyle() == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10054) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
			if ((null != accvo.getBankacc()) && (accvo.getBankacc().booleanValue()) && (voucher.getDetail(i).getBankaccount() == null)) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10057) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}

			if ((null != accvo.getBilltype()) && (accvo.getBilltype().booleanValue())) {
				if (voucher.getDetail(i).getBilltype() == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10058) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
			if ((null != accvo.getBilldate()) && (accvo.getBilldate().booleanValue())) {
				if (voucher.getDetail(i).getCheckdate() == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10059) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
			if ((null != accvo.getBillnumber()) && (accvo.getBillnumber().booleanValue())) {
				if (voucher.getDetail(i).getCheckno() == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(10060) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
		}
		return result;
	}

	protected OperationResultVO[] checkFracAmountBalance(VoucherVO voucher, Integer controlmode, GLParameterVO param) {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		if (voucher.getNumDetails() <= 0)
			return null;
		if ((voucher.getIsOutSubj() != null) && (voucher.getIsOutSubj().booleanValue())) {
			return null;
		}
		if (param == null)
			param = new GLParameterVO();
		if (param.Parameter_islocalfrac == null) {
			try {
				param.Parameter_islocalfrac = AccountBookUtil.getIsAccAccountByPk(voucher.getPk_accountingbook());

			} catch (Exception e) {

				Logger.error(e.getMessage(), e);
				throw new GlBusinessException(e.getMessage());
			}
		}
		if (!param.Parameter_islocalfrac.booleanValue())
			return null;
		OperationResultVO[] result = null;
		UFDouble fracDebit = new UFDouble(0);
		UFDouble fracCredit = new UFDouble(0);
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			DetailVO detail = voucher.getDetail(i);
			fracDebit = fracDebit.add(detail.getFracdebitamount());
			fracCredit = fracCredit.add(detail.getFraccreditamount());
		}
		if (!fracDebit.equals(fracCredit)) {
			switch (controlmode.intValue()) {
				case 0: {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 0;
					result = new OperationResultVO[] { rs };
					break;
				}
				case 1: {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 1;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20003);

					result = new OperationResultVO[] { rs };
					break;
				}
				case 2: {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20003);

					result = new OperationResultVO[] { rs };
					break;
				}
				default:
					throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
			}

		}

		return result;
	}

	protected OperationResultVO[] checkIsNoDetail(VoucherVO voucher) throws Exception {
		if ((voucher.getDetails() == null) || (voucher.getNumDetails() == 0)) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10003);

			return new OperationResultVO[] { rs };
		}
		return null;
	}

	protected OperationResultVO[] checkInputAttachment(VoucherVO voucher) throws Exception {
		UFBoolean isInputAttachment = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).isInputAttachment(voucher.getPk_accountingbook());

		if ((isInputAttachment.booleanValue()) && (voucher.getAttachment().intValue() == 0)) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20009);

			return new OperationResultVO[] { rs };
		}

		return null;
	}

	protected OperationResultVO[] checkPreparedDate(VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		OperationResultVO[] result = null;
		UFDate date = voucher.getPrepareddate();
		String[] settledperiod = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getSettlePeriod(voucher.getPk_accountingbook(), "2002");

		String[] startperiod = ((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getStartPeriod(voucher.getPk_accountingbook(), "2002");

		if (("GLRC".equals(voucher.getPk_system())) && ("00".equals(voucher.getPeriod()))) {

			return null;
		}

		GlPeriodVO vo = ((IGlPeriod) NCLocator.getInstance().lookup(IGlPeriod.class)).getPeriod(voucher.getPk_accountingbook(), date);

		if (vo == null) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10004);

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		} else {
			String year = vo.getYear();
			if (year == null) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10004);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			} else {
				if (!year.equals(voucher.getYear())) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10046);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}

				String period = vo.getMonth();
				if (period == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10004);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				} else {
					if ((voucher.getVoucherkind().intValue() != 1) && (!"PLCF".equals(voucher.getPk_system())) && (!"GLRC".equals(voucher.getPk_system()))) {

						result = checkCloseAccbook(result, voucher, tempaccsubj);
					}
					if (!period.equals(voucher.getPeriod())) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10046);

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					}

					if (startperiod == null) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10005);

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					} else if ((year.compareTo(startperiod[0]) < 0) || ((year.compareTo(startperiod[0]) == 0) && (period.compareTo(startperiod[1]) < 0))) {

						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10006);

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					} else {
						if ((voucher.getVoucherkind().intValue() != 1) || (

						(settledperiod == null) || (settledperiod.length == 0) || (settledperiod[0] == null) || (settledperiod[0].equals("")))) {

							if (voucher.getVoucherkind().intValue() == 1) {
								OperationResultVO rs = new OperationResultVO();
								rs.m_intSuccess = 2;
								rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10052);

								result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
							}

							return result;
						}
						if ((year.compareTo(settledperiod[0]) < 0) || ((year.compareTo(settledperiod[0]) == 0) && (period.compareTo(settledperiod[1]) <= 0))) {

							if (voucher.getVoucherkind().intValue() != 1) {
								OperationResultVO rs = new OperationResultVO();
								rs.m_intSuccess = 2;
								rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10007);

								result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
							}

						} else if (voucher.getVoucherkind().intValue() == 1) {
							OperationResultVO rs = new OperationResultVO();
							rs.m_intSuccess = 2;
							rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10052);

							result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
						}
					}
				}
			}
		}

		return result;
	}

	private OperationResultVO[] checkCloseAccbook(OperationResultVO[] result, VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		OperationResultVO resultVO = null;
		String periodnew = voucher.getYear() + "-" + voucher.getPeriod();
		if (CloseAccBookUtils.isCloseByAccountBookId(voucher.getPk_accountingbook(), periodnew)) {
			resultVO = new OperationResultVO();
			resultVO.m_intSuccess = 2;
			resultVO.m_strDescription = String.format(NCLangRes4VoTransl.getNCLangRes().getStrByID("voucherprivate_0", "02002005-0066"), new Object[] { periodnew });

		} else if (CloseAccBookUtils.isPreCloseByAccountBookId(voucher.getPk_accountingbook(), periodnew)) {
			DetailVO[] details = voucher.getDetails();
			if (details == null) {
				voucher = new VoucherBO().queryByPk(voucher.getPk_voucher());
				details = voucher.getDetails();
			}
			if (tempaccsubj == null) {
				tempaccsubj = new HashMap();
			}
			if (tempaccsubj.size() <= 0) {
				String[] pk_accsubjs = new String[voucher.getNumDetails()];
				for (int i = 0; i < details.length; i++) {
					pk_accsubjs[i] = details[i].getPk_accasoa();
				}
				AccountVO[] accvos = getAccsubj(pk_accsubjs, voucher.getPrepareddate().toStdString());

				AccountVO voTemp = null;
				if (accvos != null) {
					for (int i = 0; i < accvos.length; i++) {
						voTemp = accvos[i];
						tempaccsubj.put(voTemp.getPk_accasoa(), voTemp);
					}
				}
			}
			StringBuilder msg = new StringBuilder();
			AccountVO accvo = null;
			for (DetailVO detail : details) {
				accvo = (AccountVO) tempaccsubj.get(detail.getPk_accasoa());
				if (accvo == null) {
					accvo = AccountUtilGL.findAccountVOByPrimaryKey(detail.getPk_accasoa(), voucher.getPrepareddate().toStdString());

					tempaccsubj.put(detail.getPk_accasoa(), accvo);
				}
				if ((accvo.getAllowclose() != null) && (accvo.getAllowclose().booleanValue())) {
					msg.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("voucherprivate_0", "02002005-0067", null, new String[] { "" + detail.getDetailindex(), accvo.getCode() + " " + accvo.getName() }));

					break;
				}
			}
			if (!"".equals(msg.toString())) {
				resultVO = new OperationResultVO();
				resultVO.m_intSuccess = 2;
				resultVO.m_strDescription = (String.format(NCLangRes4VoTransl.getNCLangRes().getStrByID("voucherprivate_0", "02002005-0070"), new Object[] { periodnew }) + msg.toString());
			}
		}

		return OperationResultVO.appendResultVO(result, new OperationResultVO[] { resultVO == null ? null : resultVO });
	}

	protected OperationResultVO[] checkQuantityZero(VoucherVO voucher, Integer controlmode, HashMap accsubjcache) {
		if ((controlmode == null) || (controlmode.intValue() == 0)) {
			return null;
		}
		if (voucher.getNumDetails() <= 0)
			return null;
		OperationResultVO[] result = null;

		Vector vecresult = new Vector();
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			DetailVO detail = voucher.getDetail(i);
			AccountVO accvo = (AccountVO) accsubjcache.get(detail.getPk_accasoa());

			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 0;
			if ((accvo != null) && (accvo.getUnit() != null) && (detail.getDebitquantity().equals(new UFDouble(0))) && (detail.getCreditquantity().equals(new UFDouble(0)))) {

				switch (controlmode.intValue()) {
					case 1:
						rs.m_intSuccess = 1;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20004) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						vecresult.addElement(rs);
						break;

					case 2:
						rs.m_intSuccess = 2;
						rs.m_strDescription = (new VoucherCheckMessage().getVoucherMessage(20004) + NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000575", null, new String[] { String.valueOf(i + 1) }));

						vecresult.addElement(rs);
						break;

					default:
						throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000574"));
				}

			}
		}

		if (vecresult.size() > 0) {
			result = new OperationResultVO[vecresult.size()];
			vecresult.copyInto(result);
		}
		return result;
	}

	protected OperationResultVO[] checkRegulation(VoucherVO voucher) throws Exception {
		OperationResultVO[] result = null;

		return result;
	}

	protected OperationResultVO[] checkSubjRule(VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		OperationResultVO[] result = null;
		try {
			if ((voucher.getDetails() != null) && (voucher.getNumDetails() != 0)) {
				SubjRuleCheckBO rulecheck = new SubjRuleCheckBO();
				result = rulecheck.checkSubjRule(voucher, tempaccsubj);
			}
		} catch (ClassNotFoundException e) {
			return null;
		} catch (NoClassDefFoundError e) {
			return null;
		} catch (Exception e) {
			throw e;
		}
		return result;
	}

	public OperationResultVO[] checkTimeOrdered(VoucherVO voucher) throws BusinessException {
		OperationResultVO[] result = null;
		try {
			if (new VoucherExtendDMO().checkTimeOrderedNo(voucher).intValue() > 0) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20005);

				result = new OperationResultVO[] { rs };
			}
		} catch (Exception e) {
			log.error(e);
			throw new BusinessException(e.getMessage());
		}

		return result;
	}

	protected OperationResultVO[] checkVoucher(VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		return checkVoucher(voucher, null, tempaccsubj);
	}

	protected OperationResultVO[] checkVoucher(VoucherVO voucher, VoucherCheckConfigVO configVO, HashMap tempaccsubj) throws Exception {
		if (voucher == null) {
			return null;
		}
		long tb = System.currentTimeMillis();
		OperationResultVO[] result = null;

		if (tempaccsubj == null) {
			tempaccsubj = new HashMap();
		}
		if (tempaccsubj.size() == 0) {
			getAccountMap(voucher, tempaccsubj);
		}
		if (GlDebugFlag.$DEBUG) {
			Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::load accsubj:" + (System.currentTimeMillis() - tb) + "ms");

			tb = System.currentTimeMillis();
		}
		GLParameterVO glparam = new GLParameterVO();
		glparam.Parameter_isfreevalueallownull = UFBoolean.TRUE;
		SetOfBookVO glbookvo = AccountBookUtil.getSetOfBookVOByPk_accountingBook(voucher.getPk_accountingbook());

		glparam.Parameter_islocalfrac = AccountBookUtil.getIsAccAccountByPk(voucher.getPk_accountingbook());

		glparam.Parameter_pk_localcurr = glbookvo.getPk_standardcurr();
		voucher = catOutSubjVoucher(voucher, tempaccsubj);

		if (GlDebugFlag.$DEBUG) {
			Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::load GLParam:" + (System.currentTimeMillis() - tb) + "ms");

			tb = System.currentTimeMillis();
		}
		if (((configVO != null) && (configVO.isNeedNormalCheck() == null)) || ((configVO.isNeedNormalCheck() != null) && (configVO.isNeedNormalCheck().booleanValue()))) {

			OperationResultVO[] rs1 = checkVoucherHead(voucher, tempaccsubj);
			result = OperationResultVO.appendResultVO(result, rs1);
			if (GlDebugFlag.$DEBUG) {
				Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::check checkVoucherHead:" + (System.currentTimeMillis() - tb) + "ms");

				tb = System.currentTimeMillis();
			}

			OperationResultVO[] rs2 = checkIsNoDetail(voucher);
			result = OperationResultVO.appendResultVO(result, rs2);
			if (GlDebugFlag.$DEBUG) {
				Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::check checkIsNoDetail:" + (System.currentTimeMillis() - tb) + "ms");

				tb = System.currentTimeMillis();
			}

			for (int i = 0; i < voucher.getNumDetails(); i++) {
				OperationResultVO[] rs3 = checkDetail(voucher, voucher.getPk_system(), i, tempaccsubj, glparam);

				result = OperationResultVO.appendResultVO(result, rs3);
				if (GlDebugFlag.$DEBUG) {
					Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::check checkDetail" + i + ":" + (System.currentTimeMillis() - tb) + "ms");

					tb = System.currentTimeMillis();
				}
			}
		}
		if (configVO == null) {
			return result;
		}
		if (configVO.getAmountAllowZero() == null) {
			configVO.setAmountAllowZero(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getAmountAllowZero(voucher.getPk_accountingbook()));
		}

		if (configVO.getAmountMustBalance() == null) {
			configVO.setAmountMustBalance(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getAmountMustBalance(voucher.getPk_accountingbook()));
		}

		if (configVO.getQuantityAllowZero() == null) {
			configVO.setQuantityAllowZero(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getQuantityAllowZero(voucher.getPk_accountingbook()));
		}

		if (configVO.isVoucherTimeOrdered() == null) {
			configVO.setVoucherTimeOrdered(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).isVoucherTimeOrdered(voucher.getPk_accountingbook()));
		}

		if (configVO.getBalanceControl() == null) {
			configVO.setBalanceControl(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getBalanceControlStyle(voucher.getPk_accountingbook()));
		}

		if (configVO.getGroupBalanceControl() == null) {
			configVO.setGroupBalanceControl(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getGroupBalanceControl(voucher.getPk_accountingbook()));
		}

		if (configVO.getGlobalBalanceControl() == null) {
			configVO.setGlobalBalanceControl(((IGlPara) NCLocator.getInstance().lookup(IGlPara.class)).getGlobalBalanceControl(voucher.getPk_accountingbook()));
		}

		if (configVO.getSecondBUBalanceControl() == null) {
			UFBoolean secondBUStart = GLParaAccessor.isSecondBUStart(voucher.getPk_accountingbook());

			UFBoolean buBalanceCheck = GLParaAccessor.isBUBalanceCheck(voucher.getPk_accountingbook());

			if ((secondBUStart != null) && (secondBUStart.booleanValue()) && (buBalanceCheck != null) && (buBalanceCheck.booleanValue())) {
				configVO.setSecondBUBalanceControl(UFBoolean.TRUE);
			} else {
				configVO.setSecondBUBalanceControl(UFBoolean.FALSE);
			}
		}
		if (GlDebugFlag.$DEBUG) {
			Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::load GLParam2:" + (System.currentTimeMillis() - tb) + "ms");

			tb = System.currentTimeMillis();
		}
		OperationResultVO[] rs5 = checkVoucherWithConfigVO(voucher, configVO, tempaccsubj, glparam);

		result = OperationResultVO.appendResultVO(result, rs5);
		if (GlDebugFlag.$DEBUG) {
			Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::check checkVoucherWithConfigVO:" + (System.currentTimeMillis() - tb) + "ms");

			tb = System.currentTimeMillis();
		}

		OperationResultVO[] rs6 = checkSubjRule(voucher, tempaccsubj);
		result = OperationResultVO.appendResultVO(result, rs6);
		if (GlDebugFlag.$DEBUG) {
			Log.getInstance(getClass().getName()).debug("CheckVoucher time report::nc.bs.gl.voucher.VoucherCheckBO.checkVoucher(VoucherVO, VoucherCheckConfigVO)::check checkSubjRule:" + (System.currentTimeMillis() - tb) + "ms");

			tb = System.currentTimeMillis();
		}

		return result;
	}

	protected OperationResultVO[] checkVoucherHead(VoucherVO voucher, HashMap tempaccsubj) throws Exception {
		OperationResultVO[] result = null;

		voucher.setSignflag(UFBoolean.FALSE);

		AccountingBookVO bookVO = AccountBookUtil.getAccountingBookVOByPrimaryKey(voucher.getPk_accountingbook());

		if (bookVO.getAccounttype().intValue() == 2) {
			boolean accountBookOrgExceeded = ((IAccountingBookPubService) NCLocator.getInstance().lookup(IAccountingBookPubService.class)).isAccountBookOrgExceeded();

			if (accountBookOrgExceeded) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20043);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		if (2 != bookVO.getAccountenablestate().intValue()) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			if (1 == bookVO.getAccountenablestate().intValue()) {
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20041);
			} else if (3 == bookVO.getAccountenablestate().intValue()) {
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20042);
			}

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		String pk_accountingbook = voucher.getPk_accountingbook();
		String pk_prepared = voucher.getPk_prepared();
		String pk_group = voucher.getPk_group();

		if (!"NC_USER0000000000000".equals(pk_prepared)) {
			String[] userPermissionPkOrgs = ((IFunctionPermissionPubService) NCLocator.getInstance().lookup(IFunctionPermissionPubService.class)).getUserPermissionPkOrgs(pk_prepared, "20020PREPA", pk_group);

			if ((userPermissionPkOrgs == null) || (userPermissionPkOrgs.length == 0) || (!Arrays.asList(userPermissionPkOrgs).contains(pk_accountingbook))) {

				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20043);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		OperationResultVO[] rs1 = checkPreparedDate(voucher, tempaccsubj);
		result = OperationResultVO.appendResultVO(result, rs1);

		OperationResultVO[] rs2 = checkYearPeriod(voucher);
		result = OperationResultVO.appendResultVO(result, rs2);

		OperationResultVO[] rs3 = checkVoucherType(voucher);
		result = OperationResultVO.appendResultVO(result, rs3);

		if (voucher.getVoucherkind().intValue() == 1) {
			OperationResultVO[] rs4 = checkRegulation(voucher);
			result = OperationResultVO.appendResultVO(result, rs4);
		}

		if ((voucher.getPk_system() == null) || (DapSystemDataCache.getInstance().getDapsystem(voucher.getPk_system()) == null)) {

			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20011);

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		if (voucher.getOffervoucher() != null) {
			boolean isoffered = GLPubProxy.getRemoteVoucher().isOfferSetVoucher(voucher.getOffervoucher());

			if ((isoffered) && (StringUtils.isEmpty(voucher.getPk_voucher()))) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20081);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			}
		}

		return result;
	}

	protected Boolean checkVoucherNo(VoucherVO voucher) throws Exception {
		VoucherVO vo1 = new VoucherVO();
		vo1.setPk_vouchertype(voucher.getPk_vouchertype());
		vo1.setNo(voucher.getNo());
		vo1.setPk_org(voucher.getPk_org());
		vo1.setPk_accountingbook(voucher.getPk_accountingbook());
		vo1.setYear(voucher.getYear());
		vo1.setPeriod(voucher.getPeriod());
		VoucherVO[] vo2 = new VoucherDMO().queryByVO(vo1, new Boolean(true));
		if ((vo2 != null) && (vo2.length != 0) && (!vo2[0].getPk_voucher().equals(voucher.getPk_voucher()))) {
			throw new GlBusinessException(new VoucherCheckMessage().getVoucherMessage(10008));
		}

		return new Boolean(true);
	}

	protected OperationResultVO[] checkVoucherType(VoucherVO voucher) throws Exception {
		OperationResultVO[] result = null;
		if (voucher.getPk_vouchertype() == null) {
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10009);

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

		} else {

			VoucherTypeVO vo6 = VoucherTypeDataCache.getInstance().getVtBypkorgbookAndpkvt(voucher.getPk_accountingbook(), voucher.getPk_vouchertype());

			if (vo6 == null) {
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10010);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
			} else {
				if (vo6.getPk_vouchertype() == null) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10010);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });

				} else if (vo6.getEnablestate().intValue() != 2) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10053);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				} else if (!StringUtils.isEmpty(vo6.getPk_vouchertype())) {
					result = OperationResultVO.appendResultVO(result, checkVTStyle(voucher, vo6));
				}

				result = OperationResultVO.appendResultVO(result, checkBalance(voucher));
			}
		}

		return result;
	}

	private OperationResultVO[] checkVTStyle(VoucherVO voucher, VoucherTypeVO vo6) {
		VouchertypeRuleVO rulevo = VoucherTypeGL.getRuleVOByVTPk(voucher.getPk_accountingbook(), vo6.getPk_vouchertype());

		OperationResultVO[] result = null;
		if ((null == rulevo) || (StringUtils.isEmpty(rulevo.getRuletype())) || (rulevo.getRuletype().equals("0"))) {
			return result;
		}

		String[] pk_subjs = null;
		if (rulevo.getAccasoalist() != null) {
			pk_subjs = new String[rulevo.getAccasoalist().length];
		}
		for (int i = 0; i < rulevo.getAccasoalist().length; i++) {
			pk_subjs[i] = rulevo.getAccasoalist()[i].getPk_accasoa();
		}

		pk_subjs = AccasoaUtils.getEndPksByPks(pk_subjs, voucher.getPrepareddate().toStdString());

		HashMap debitsubj = new HashMap();
		HashMap creditsubj = new HashMap();
		for (int i = 0; i < voucher.getNumDetails(); i++) {
			if (voucher.getDetail(i).getLocalcreditamount().equals(new UFDouble(0))) {
				if (debitsubj.get(voucher.getDetail(i).getPk_accasoa()) == null) {
					debitsubj.put(voucher.getDetail(i).getPk_accasoa(), voucher.getDetail(i).getPk_accasoa());
				}
			} else if (creditsubj.get(voucher.getDetail(i).getPk_accasoa()) == null) {
				creditsubj.put(voucher.getDetail(i).getPk_accasoa(), voucher.getDetail(i).getPk_accasoa());
			}
		}

		boolean tmpflag = false;
		switch (Integer.valueOf(rulevo.getRuletype()).intValue()) {
			case 1:
				for (int i = 0; i < pk_subjs.length; i++) {
					if (debitsubj.get(pk_subjs[i]) != null) {
						tmpflag = true;
						break;
					}
				}
				if (!tmpflag) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10028);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
				break;

			case 2:
				for (int i = 0; i < pk_subjs.length; i++) {
					if (creditsubj.get(pk_subjs[i]) != null) {
						tmpflag = true;
						break;
					}
				}
				if (!tmpflag) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10029);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
				break;

			case 3:
				for (int i = 0; i < pk_subjs.length; i++) {
					if ((debitsubj.get(pk_subjs[i]) != null) || (creditsubj.get(pk_subjs[i]) != null)) {
						tmpflag = true;
						break;
					}
				}
				if (!tmpflag) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;
					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10030);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
				break;

			case 4:
				for (int i = 0; i < pk_subjs.length; i++) {
					if ((debitsubj.get(pk_subjs[i]) != null) || (creditsubj.get(pk_subjs[i]) != null)) {
						OperationResultVO rs = new OperationResultVO();
						rs.m_intSuccess = 2;
						rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10031);

						result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
					}
				}

				break;
			default:
				OperationResultVO rs = new OperationResultVO();
				rs.m_intSuccess = 2;
				rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10012);

				result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	protected OperationResultVO[] checkVoucherWithConfigVO(VoucherVO voucher, VoucherCheckConfigVO configVO, HashMap accsubjcache, GLParameterVO param) throws Exception {
		OperationResultVO[] result = null;

		boolean isEGL = false;
		String pk_system = voucher.getPk_system();
		if ((StringUtils.isNotEmpty(pk_system)) && ("EGL".equals(pk_system))) {
			isEGL = true;
		}

		if ((configVO.getAmountAllowZero() != null) && (!isEGL)) {
			OperationResultVO[] t_result = checkAmountZero(voucher, configVO.getAmountAllowZero());

			result = OperationResultVO.appendResultVO(result, t_result);
		}

		if ((configVO.getAmountMustBalance() != null) && (!isEGL)) {
			OperationResultVO[] t_result = checkAmountBalance(voucher, configVO.getAmountMustBalance());

			result = OperationResultVO.appendResultVO(result, t_result);
		}

		if (configVO.getQuantityAllowZero() != null) {
			OperationResultVO[] t_result = checkQuantityZero(voucher, configVO.getQuantityAllowZero(), accsubjcache);

			result = OperationResultVO.appendResultVO(result, t_result);
		}
		if (configVO.getBalanceControl() != null) {
			OperationResultVO[] t_result = checkBalanceControl(voucher, accsubjcache, param, configVO.getBalanceControl());

			result = OperationResultVO.appendResultVO(result, t_result);
			OperationResultVO[] a_result = checkAssBalanceControlNew(voucher, accsubjcache, param, configVO.getBalanceControl());

			result = OperationResultVO.appendResultVO(result, a_result);
		}

		if ((null != configVO.getSecondBUBalanceControl()) && (configVO.getSecondBUBalanceControl().booleanValue())) {
			if ((voucher.getVoucherkind() == null) || (voucher.getVoucherkind().intValue() != 2)) {
				OperationResultVO[] t_result = checkBUBalance(voucher);
				result = OperationResultVO.appendResultVO(result, t_result);
			}
		}
		return result;
	}

	private OperationResultVO[] checkBUBalance(VoucherVO voucher) {
		if ((voucher.getIsOutSubj() != null) && (voucher.getIsOutSubj().booleanValue())) {
			return null;
		}
		DetailVO[] detailVOs = voucher.getDetails();
		HashMap<String, UFDouble> bumap = new HashMap();

		for (DetailVO vo : detailVOs) {
			if (bumap.containsKey(vo.getPk_unit())) {
				bumap.put(vo.getPk_unit(), ((UFDouble) bumap.get(vo.getPk_unit())).add(vo.getLocaldebitamount().sub(vo.getLocalcreditamount())));

			} else {

				bumap.put(vo.getPk_unit(), vo.getLocaldebitamount().sub(vo.getLocalcreditamount()));
			}
		}

		OperationResultVO[] result = null;
		Set<String> bus = bumap.keySet();
		if ((null != bus) && (bus.size() > 0)) {
			for (String bu : bus) {
				if (((UFDouble) bumap.get(bu)).abs().compareTo(new UFDouble(9.0E-7D)) > 0) {
					OperationResultVO rs = new OperationResultVO();
					rs.m_intSuccess = 2;

					rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(20016);

					result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
				}
			}
		}
		return result;
	}

	protected OperationResultVO[] checkYearPeriod(VoucherVO voucher) throws Exception {
		if (("GLRC".equals(voucher.getPk_system())) && ("00".equals(voucher.getPeriod()))) {

			return null;
		}

		OperationResultVO[] result = null;
		AccperiodVO vo1 = new AccperiodVO();
		vo1.setPeriodyear(voucher.getYear());
		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());
		try {
			calendar.set(voucher.getYear());
		} catch (InvalidAccperiodExcetion e) {
			Logger.error(e.getMessage(), e);
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10013);

			return OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		try {
			calendar.set(voucher.getYear(), voucher.getPeriod());
		} catch (InvalidAccperiodExcetion e) {
			Logger.error(e.getMessage(), e);
			OperationResultVO rs = new OperationResultVO();
			rs.m_intSuccess = 2;
			rs.m_strDescription = new VoucherCheckMessage().getVoucherMessage(10033);

			result = OperationResultVO.appendResultVO(result, new OperationResultVO[] { rs });
		}

		return result;
	}

	protected AccountVO[] getAccsubj(String[] strPk_accsubj) throws Exception {
		if ((strPk_accsubj == null) || (strPk_accsubj.length == 0))
			return null;
		String[] pk_accsubj = null;
		Vector vecaccsubj = new Vector();
		HashMap tempmap = new HashMap();
		for (int i = 0; i < strPk_accsubj.length; i++) {
			if ((strPk_accsubj[i] != null) && (tempmap.get(strPk_accsubj[i]) == null)) {
				vecaccsubj.addElement(strPk_accsubj[i]);
				tempmap.put(strPk_accsubj[i], strPk_accsubj[i]);
			}
		}
		pk_accsubj = new String[vecaccsubj.size()];
		vecaccsubj.copyInto(pk_accsubj);
		AccountVO[] accsubj = AccountUtilGL.queryByPks(pk_accsubj);
		return accsubj;
	}

	protected AccountVO[] getAccsubj(String[] strPk_accsubj, String stddate) throws Exception {
		if ((strPk_accsubj == null) || (strPk_accsubj.length == 0))
			return null;
		String[] pk_accsubj = null;
		Vector vecaccsubj = new Vector();
		HashMap tempmap = new HashMap();
		for (int i = 0; i < strPk_accsubj.length; i++) {
			if ((strPk_accsubj[i] != null) && (tempmap.get(strPk_accsubj[i]) == null)) {
				vecaccsubj.addElement(strPk_accsubj[i]);
				tempmap.put(strPk_accsubj[i], strPk_accsubj[i]);
			}
		}
		pk_accsubj = new String[vecaccsubj.size()];
		vecaccsubj.copyInto(pk_accsubj);

		AccountVO[] accsubj = AccountUtilGL.queryByPks(pk_accsubj, stddate);

		return accsubj;
	}

	protected CurrtypeVO[] getCurrency(String pk_org) throws Exception {
		return CurrtypeGL.queryAll(pk_org);
	}

	public ISortTool getSortTool(Object objCompared) {
		try {
			if (this.m_assSortTool == null) {
				this.m_assSortTool = new CAssSortTool();
			}
			return this.m_assSortTool;
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
		return null;
	}

	private DetailVO[] catAss(DetailVO[] details) throws BusinessException, Exception {
		if ((details == null) || (details.length == 0))
			return details;
		String[] Ids = null;
		Vector vecIds = new Vector();
		HashMap tempmap = new HashMap();
		for (int j = 0; j < details.length; j++) {
			if (details[j].getAssid() != null) {
				if ((details[j].getAss() == null) && (tempmap.get(details[j].getAssid()) == null)) {
					vecIds.addElement(details[j].getAssid());
					tempmap.put(details[j].getAssid(), details[j].getAssid());
				}
			}
		}
		if (vecIds.size() == 0) {
			VoWizard wizard = new VoWizard();

			wizard.setMatchingIndex(new int[] { 107 }, null);

			wizard.sort(details, new int[] { 107 });
			return details;
		}
		Ids = new String[vecIds.size()];
		vecIds.copyInto(Ids);

		GlAssVO[] glAssVo = ((IFreevaluePub) NCLocator.getInstance().lookup(IFreevaluePub.class)).queryAllByIDs(Ids, null, Module.GL);

		if (glAssVo == null)
			throw new BusinessException("Error AssIDs::" + vecIds);
		HashMap assvocache = new HashMap();
		for (int i = 0; i < glAssVo.length; i++) {
			glAssVo[i].setAssID(glAssVo[i].getAssID().trim());
			assvocache.put(glAssVo[i].getAssID(), glAssVo[i].getAssVos());
		}

		for (int i = 0; i < details.length; i++) {
			if ((details[i].getAssid() != null) && (details[i].getAss() == null)) {
				details[i].setAss((AssVO[]) assvocache.get(details[i].getAssid()));
			}
		}

		VoWizard wizard = new VoWizard();

		wizard.setMatchingIndex(new int[] { 107 }, null);

		wizard.sort(details, new int[] { 107 });

		return details;
	}

	private DetailVO[] filterDetailForBalanceCtrl(DetailVO[] details) {
		if ((details == null) || (details.length <= 0)) {
			return new DetailVO[0];
		}
		DetailVO detail = details[0];
		if (((detail.getTempsaveflag() != null) && (detail.getTempsaveflag().booleanValue())) || ((detail.getDiscardflag() != null) && (detail.getDiscardflag().booleanValue())) || (detail.getErrmessage() != null) || (detail.getErrmessage2() != null) || (detail.getErrmessageh() != null)) {

			details = new DetailVO[0];
		}
		return details;
	}

	public String getStdDateForInitSave(VoucherVO voucher) throws BusinessException {
		if ((null == voucher) || (null == voucher.getPrepareddate())) {
			return null;
		}
		if (voucher.getPrepareddate().toStdString().startsWith("0001")) {
			String[] startPeriod = GLParaDataCache.getInstance().getStartPeriod(voucher.getPk_accountingbook(), "2002");

			String accMonth = "01";
			if (voucher.getYear().equals(startPeriod[0])) {
				accMonth = startPeriod[1];
			}
			AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(voucher.getPk_accountingbook());

			calendar.set(voucher.getYear(), accMonth);
			return calendar.getMonthVO().getBegindate().toStdString();
		}
		return voucher.getPrepareddate().toStdString();
	}

	private HashMap getAccountMap(VoucherVO voucher, HashMap tempaccsubj) {
		if (tempaccsubj == null) {
			tempaccsubj = new HashMap();
		}
		if (voucher.getDetails().length <= 0) {
			return tempaccsubj;
		}
		try {
			String[] pk_accsubj = new String[voucher.getNumDetails()];
			for (int i = 0; i < voucher.getNumDetails(); i++) {
				pk_accsubj[i] = voucher.getDetail(i).getPk_accasoa();
			}
			AccountVO[] acc = AccountUtilGL.queryByPks(pk_accsubj, getStdDateForInitSave(voucher));

			AccountVO voTemp = null;
			if (acc != null)
				for (int i = 0; i < acc.length; i++) {
					voTemp = acc[i];
					tempaccsubj.put(voTemp.getPk_accasoa(), voTemp);
				}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000582"));
		}

		return tempaccsubj;
	}

	private HashMap getAccountMap(DetailVO[] details, HashMap tempaccsubj) {
		if (tempaccsubj == null) {
			tempaccsubj = new HashMap();
		}
		if ((details == null) || (details.length <= 0)) {
			return tempaccsubj;
		}
		try {
			String[] pk_accsubj = new String[details.length];
			for (int i = 0; i < details.length; i++) {
				pk_accsubj[i] = details[i].getPk_accasoa();
			}

			AccountVO[] acc = AccountUtilGL.queryByPks(pk_accsubj, details[0].getPrepareddate().toStdString());

			AccountVO voTemp = null;
			if (acc != null)
				for (int i = 0; i < acc.length; i++) {
					voTemp = acc[i];

					tempaccsubj.put(voTemp.getPk_accasoa(), voTemp);
				}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new GlBusinessException(NCLangResOnserver.getInstance().getStrByID("20021005", "UPP20021005-000582"));
		}

		return tempaccsubj;
	}
}
