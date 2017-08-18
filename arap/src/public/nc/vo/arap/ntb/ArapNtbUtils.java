package nc.vo.arap.ntb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import nc.bs.arap.util.IArapBillTypeCons;
import nc.bs.arap.util.SqlUtils;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.itf.arap.bill.IArapBillService;
import nc.md.persist.framework.MDPersistenceService;
import nc.pubitf.org.IAccountingBookPubService;
//import nc.ui.gl.contrastpub.AccountBookUtil;
import nc.vo.arap.pub.ArapConstant;
import nc.vo.arap.pub.BillEnumCollection;
import nc.vo.arap.receivable.ReceivableBillItemVO;
import nc.vo.bd.account.AccAsoaVO;
import nc.vo.gateway60.accountbook.AccountBookUtil;
import nc.vo.org.AccountingBookVO;
import nc.vo.pub.BusinessException;
import nc.vo.tb.obj.NtbParamVO;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class ArapNtbUtils {

	public static ArapQueryVO analyseParam(NtbParamVO ntbParamVO) throws BusinessException {
		ArapQueryVO queryVO = new ArapQueryVO();
		// param.getTypeDim()对应于ntb_id_bdcontrast中的bdinfo_type字段
		if (ntbParamVO.getTypeDim() == null || ntbParamVO.getTypeDim().length == 0) {
			throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2006pub_0","02006pub-0365")/*@res "辅助对象类型为空！"*/);
		}

		String pk_group = InvocationInfoProxy.getInstance().getGroupId();
		String billType = NCLocator.getInstance().lookup(IArapBillService.class)
				.getParentBillTypeByTradeType(ntbParamVO.getBill_type(), pk_group);
		queryVO.setPk_group(pk_group);
		queryVO.setPk_org(new String[] { ntbParamVO.getPk_Org() });
		queryVO.setPk_currency(ntbParamVO.getPk_currency());
		queryVO.setCurr_type(ntbParamVO.getCurr_type());
		queryVO.setBegdate(ntbParamVO.getBegDate());
		queryVO.setEnddate(ntbParamVO.getEndDate());
		queryVO.setBill_code(billType);
		queryVO.setPk_tradetypes(new String[] { ntbParamVO.getBill_type() });

		queryVO.setIncludeInit(ntbParamVO.isIncludeInit());
		queryVO.setDatetype(ntbParamVO.getDateType());

		queryVO.setOrgatt_fld(ntbParamVO.getOrg_Attr());
		String[] busiAttrs = ntbParamVO.getBusiAttrs();
		String[] pkDims = ntbParamVO.getPkDim();

		for (int i = 0; i < busiAttrs.length; i++) {
			String fieldName = null;
			if ( busiAttrs[i].startsWith("arap_b_")) {
				fieldName = busiAttrs[i].substring(7);
				List<String> condList = queryVO.getQueryCondMap().get(fieldName);
				if (condList == null) {
					condList = new ArrayList<String>();
					queryVO.getQueryCondMap().put(fieldName, condList);
				}

				if (ReceivableBillItemVO.SUBJCODE.equalsIgnoreCase(fieldName)) {
					if(ntbParamVO.getLowerArrays().get(busiAttrs[i])!=null && ntbParamVO.getLowerArrays().get(busiAttrs[i]).length!=0){
						condList.addAll(getAccAsoaVO(ntbParamVO, ntbParamVO.getLowerArrays().get(busiAttrs[i])));
					}else{
						condList.addAll(getAccAsoaVO(ntbParamVO, new String[]{pkDims[i]}));
					}
				}else if (ntbParamVO.getLowerArrays().get(busiAttrs[i])!=null && ntbParamVO.getLowerArrays().get(busiAttrs[i]).length!=0){
					condList.addAll(Arrays.asList(ntbParamVO.getLowerArrays().get(busiAttrs[i])));
				}else {
					condList.add(pkDims[i]);
				}
			}else if(busiAttrs[i].startsWith("arap_h_")){
				fieldName = busiAttrs[i].substring(7);
				List<String> condList = queryVO.getQueryHeadCondMap().get(fieldName);
				if (condList == null) {
					condList = new ArrayList<String>();
					queryVO.getQueryHeadCondMap().put(fieldName, condList);
				}

				if (ReceivableBillItemVO.SUBJCODE.equalsIgnoreCase(fieldName)) {
					if(ntbParamVO.getLowerArrays().get(busiAttrs[i])!=null && ntbParamVO.getLowerArrays().get(busiAttrs[i]).length!=0){
						condList.addAll(getAccAsoaVO(ntbParamVO, ntbParamVO.getLowerArrays().get(busiAttrs[i])));
					}else{
						condList.addAll(getAccAsoaVO(ntbParamVO, new String[]{pkDims[i]}));
					}
				}else if (ntbParamVO.getLowerArrays().get(busiAttrs[i])!=null && ntbParamVO.getLowerArrays().get(busiAttrs[i]).length!=0){
					condList.addAll(Arrays.asList(ntbParamVO.getLowerArrays().get(busiAttrs[i])));
				}else {
					condList.add(pkDims[i]);
				}	
			}
		}
		return queryVO;
	}

	private static List<String> getAccAsoaVO(NtbParamVO ntbParamVO, String[] pk_account)
			throws BusinessException {
		// 根据财务组织取得账簿主键
		String pk_accbook = NCLocator.getInstance().lookup(IAccountingBookPubService.class)
				.getDefaultMainAccountingBookIDByOrgID(ntbParamVO.getPk_Org());
		// 根据账簿取得账簿的科目表主键
		AccountingBookVO accBookVO = AccountBookUtil.getAccountingBookVOByPrimaryKey(pk_accbook);
		if (accBookVO == null) {
			throw new BusinessException("Can not find accountingbook by financeorg: " + ntbParamVO.getPk_Org());
		}

		String whereSql = SqlUtils.getInStr(AccAsoaVO.PK_ACCOUNT, pk_account) + " and "
				+ AccAsoaVO.PK_ACCCHART + " = '" + accBookVO.getPk_curraccchart() + "' ";
		//String whereSql = SqlUtils.getInStr(AccAsoaVO.PK_ACCASOA, pk_account) ;  
		/**
		 * modify by zyq 说明
		 * 去除源码中的103-111的注释，
		 * 1、启动控制方案时，在CtlSchemeCTL的1880行已经将实际单据中的科目主键EHV转换成了bd_account基本档案中的主键BRE
		 * 2、之后，在update汇总数据时，根据科目主键查询单据表，必须将bd_account的主键进行还原，此处之前的sql逻辑正确，为何注释？？
		 * 
		 */
		Collection<AccAsoaVO> accAsoaVOs = MDPersistenceService.lookupPersistenceQueryService()
				.queryBillOfVOByCond(AccAsoaVO.class, whereSql, false);
		
		List<String> pks=new ArrayList<String>();
		for(AccAsoaVO asoavo:accAsoaVOs){
			pks.add(asoavo.getPk_accasoa());
		}
		return pks;
	}

	/**
	 * 创建sql语句的where部分
	 * 
	 * @param vo
	 * @return
	 */
	public static String createWherePart(ArapQueryVO vo) {
		StringBuffer sb = new StringBuffer();
		sb.append(" dr = 0 and  exists (").append(createInSql(vo)).append(")");

		if (!StringUtils.isEmpty(vo.getPk_currency())) {
			sb.append(" and ").append("pk_currtype = '" + vo.getPk_currency() + "'");
		}
		Iterator<Entry<String, List<String>>> iterator = vo.getQueryCondMap().entrySet().iterator();
		if (!ArrayUtils.isEmpty(vo.getPk_org())) {
			String[] pk_orgs = vo.getPk_org();
			Entry<String, List<String>> entry;
			if (ArapConstant.ARAP_PK_PCORG.equals(vo.getOrgatt_fld())) { // vo.getPk_org()返回的是利润中心PK
				while (iterator.hasNext()) {
					entry = iterator.next();
					if(("pk_pcorg".equals(entry.getKey()))){
						List<String> list = entry.getValue();
						list.addAll(Arrays.asList(vo.getPk_org()));
						pk_orgs = (String[]) list.toArray(new String[0]);
						break;
					}
				}
				sb.append(" and ").append(SqlUtils.getInStr("pk_pcorg", pk_orgs));
			} else if(ArapConstant.ARAP_PK_PROJECT.equals(vo.getOrgatt_fld())){ // vo.getPk_org()返回的是项目PK
				sb.append(" and ").append(SqlUtils.getInStr("project", vo.getPk_org()));
			}else{ // vo.getPk_org()返回的是财务组织PK
				while (iterator.hasNext()) {
					entry = iterator.next();
					if(("pk_org".equals(entry.getKey()))){
						List<String> list = entry.getValue();
						list.addAll(Arrays.asList(vo.getPk_org()));
						pk_orgs = (String[]) list.toArray(new String[0]);
						break;
					}
				}
				sb.append(" and ").append(SqlUtils.getInStr("pk_org", pk_orgs));
			}
		}
//		sb.append(" and pk_group = '").append(vo.getPk_group()).append("'");
		sb.append(" and ").append(SqlUtils.getInStr("pk_tradetype", vo.getPk_tradetypes()));
		Iterator<Entry<String, List<String>>> iterator2 = vo.getQueryCondMap().entrySet().iterator();
		while (iterator2.hasNext()) {
			Entry<String, List<String>> entry = iterator2.next();
			if ("pk_pcorg".equals(entry.getKey())) { 
				continue;
			}
			sb.append(" and ").append(SqlUtils.getInStr(entry.getKey(), (String[]) entry.getValue().toArray(new String[0])));
		}

		return sb.toString();
	}

	private static String createInSql(ArapQueryVO vo) {
		StringBuffer sb = new StringBuffer();
		sb.append(createSelectFrom2(vo.getBill_code()));
//		sb.append(" where dr = 0 ");

		if (ArapConstant.ARAP_NTB_BILLDATE_KEY.equals(vo.getDatetype())) {
			sb.append(" and (billdate >= '").append(vo.getBegdate()).append("' and billdate <= '").append(vo.getEnddate()).append("')");
		} else if (ArapConstant.ARAP_NTB_APPRDATE_KEY.equals(vo.getDatetype())) {
			sb.append(" and (approvedate >= '").append(vo.getBegdate()).append("' and approvedate <= '").append(vo.getEnddate()).append("')");
		} else if (ArapConstant.ARAP_NTB_EFFDATE_KEY.equals(vo.getDatetype())) {
			sb.append(" and (effectdate >= '").append(vo.getBegdate()).append("' and effectdate <= '").append(vo.getEnddate()).append("')");
		}

		if (!vo.isIncludeInit()) {
			sb.append(" and isinit = 'N'");
		}

		if (ArapQueryVO.BILLSTATUS_ALL.equals(vo.getBillstatus())) {
			sb.append(" and effectstatus in ('" + BillEnumCollection.InureSign.OKINURE.VALUE
					+ "','" + BillEnumCollection.InureSign.NOINURE.VALUE + "')");
		} else if (ArapQueryVO.BILLSTATUS_SAVE.equals(vo.getBillstatus())) {
			sb.append(" and billstatus in (-1,1) and effectstatus = '" + BillEnumCollection.InureSign.NOINURE.VALUE + "'");
		} else if (ArapQueryVO.BILLSTATUS_EFFECT.equals(vo.getBillstatus())) {
			sb.append(" and effectstatus = '" + BillEnumCollection.InureSign.OKINURE.VALUE + "'");
		}

		sb.append(" and ").append(SqlUtils.getInStr("pk_tradetype", vo.getPk_tradetypes()));
		
		Iterator<Entry<String, List<String>>> iterator2 = vo.getQueryHeadCondMap().entrySet().iterator();
		while (iterator2.hasNext()) {
			Entry<String, List<String>> entry = iterator2.next();
			if (("pk_pcorg".equals(entry.getKey()))) { 
				continue;
			}
			sb.append(" and ").append(SqlUtils.getInStr(entry.getKey(), (String[]) entry.getValue().toArray(new String[0])));
		}
		
		if (!ArrayUtils.isEmpty(vo.getPk_org())) {
			String[] pk_orgs = vo.getPk_org();
			if (ArapConstant.ARAP_PK_PCORG.equals(vo.getOrgatt_fld())) { // vo.getPk_org()返回的是利润中心PK
				sb.append(" and ").append(SqlUtils.getInStr("pk_pcorg", pk_orgs));
			} else if(ArapConstant.ARAP_PK_PROJECT.equals(vo.getOrgatt_fld())){ // vo.getPk_org()返回的是项目PK
				sb.append(" and ").append(SqlUtils.getInStr("project", vo.getPk_org()));
			}else{ // vo.getPk_org()返回的是财务组织PK
				sb.append(" and ").append(SqlUtils.getInStr("pk_org", pk_orgs));
			}
		}

		return sb.toString();
	}
	
	/**
	 * 创建sql的select和from部分
	 * @param billCode
	 * @return
	 */
	private static String createSelectFrom2(String billCode) {
		String tableName = null;
		String pk_tableName = null;
		if (IArapBillTypeCons.F0.equals(billCode)) {
			tableName = IArapBillTypeCons.AR_RECBILL;
			pk_tableName = "PK_RECBILL";
		} else if (IArapBillTypeCons.F0S.equals(billCode)) {
			tableName = IArapBillTypeCons.AR_SUPRECBILL;
			pk_tableName = "PK_SUPRECBILL";
		} else if (IArapBillTypeCons.F1.equals(billCode)) {
			tableName = IArapBillTypeCons.AP_PAYABLEBILL;
			pk_tableName = "PK_PAYABLEBILL";
		} else if (IArapBillTypeCons.F1C.equals(billCode)) {
			tableName = IArapBillTypeCons.AP_CUSPAYABLEBILL;
			pk_tableName = "PK_CUSPAYABLEBILL";
		} else if (IArapBillTypeCons.F2.equals(billCode)) {
			tableName = IArapBillTypeCons.AR_GATHERBILL;
			pk_tableName = "PK_GATHERBILL";
		} else if (IArapBillTypeCons.F2S.equals(billCode)) {
			tableName = IArapBillTypeCons.AR_SUPGATHERBILL;
			pk_tableName = "PK_SUPGATHERBILL";
		} else if (IArapBillTypeCons.F3.equals(billCode)) {
			tableName = IArapBillTypeCons.AP_PAYBILL;
			pk_tableName = "PK_PAYBILL";
		} else if (IArapBillTypeCons.F3C.equals(billCode)) {
			tableName = IArapBillTypeCons.AP_CUSPAYBILL;
			pk_tableName = "PK_CUSPAYBILL";
		}

		return "select 1 from " + tableName +" fih where fih."+pk_tableName +"=fib."+pk_tableName+" and dr =0";
	}

}
