package nc.ms.tb.control;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nc.bs.arap.util.IArapBillTypeCons;
import nc.bs.deploy.ejb.util.Logger;
import nc.bs.erm.cache.ErmBillFieldContrastCache;
import nc.bs.framework.common.NCLocator;
import nc.bs.pf.pub.PfDataCache;
import nc.itf.bd.accsystem.IAccSystemQryService;
import nc.itf.mdm.cube.ICubeDefQueryService;
import nc.itf.mdm.dim.IDimManager;
import nc.itf.mdm.dim.INtbSuper;
import nc.itf.org.IFinanceOrgQryService;
import nc.itf.tb.control.IAccessableBusiVO;
import nc.itf.tb.control.IAccessableExtBusiVO;
import nc.itf.tb.control.IAccessableOrgsBusiVO;
import nc.itf.tb.control.IBillsControl;
import nc.itf.tb.control.IBusiSysReg;
import nc.itf.tb.control.IDateType;
import nc.itf.tb.control.OutEnum;
import nc.itf.tb.rule.IBusiRuleQuery;
import nc.itf.tb.sysmaintain.BdContrastCache;
import nc.itf.tb.sysmaintain.BusiSysReg;
import nc.ms.mdm.convertor.IStringConvertor;
import nc.ms.mdm.cube.CubeServiceGetter;
import nc.ms.mdm.dim.DimMemberReader;
import nc.ms.mdm.dim.NtbSuperServiceGetter;
import nc.ms.tb.pub.NtbSuperDMO;
import nc.ms.tb.rule.RuleServiceGetter;
import nc.ms.tb.rule.SubLevelOrgGetter;
import nc.ms.tb.rule.fmlset.FormulaParser;
import nc.pubitf.bbd.CurrtypeQuery;
import nc.pubitf.bd.accessor.GeneralAccessorFactory;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.pubitf.org.cache.IOrgUnitPubService_C;
import nc.ui.bank_cvp.formulainterface.RefCompilerClient;
import nc.vo.bank_cvp.compile.datastruct.ArrayValue;
import nc.vo.bd.accessor.IBDData;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.mdm.cube.DimVector;
import nc.vo.mdm.cube.ICubeDataSet;
import nc.vo.mdm.dim.DimDef;
import nc.vo.mdm.dim.DimHierarchy;
import nc.vo.mdm.dim.DimMember;
import nc.vo.mdm.dim.LevelValue;
import nc.vo.mdm.pub.NtbEnv;
import nc.vo.mdm.pub.NtbLogger;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.org.FinanceOrgTreeVO;
import nc.vo.org.OrgVO;
import nc.vo.org.orgmodel.OrgTypeVO;
import nc.vo.org.util.OrgTypeManager;
import nc.vo.pub.BusinessException;
import nc.vo.pub.billtype.BilltypeVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pub.msg.UserNameObject;
import nc.vo.sm.UserVO;
import nc.vo.tb.control.ControlBillType;
import nc.vo.tb.control.CtrlInfoMacroConst;
import nc.vo.tb.control.CtrlSchemeVO;
import nc.vo.tb.control.IdBdcontrastVO;
import nc.vo.tb.control.IdSysregVO;
import nc.vo.tb.control.NtbCtlInfoVO;
import nc.vo.tb.formula.DimFormulaVO;
import nc.vo.tb.pubutil.BusiTermConst;
import nc.vo.tb.rule.IdCtrlInfoVO;
import nc.vo.tb.rule.IdCtrlformulaVO;
import nc.vo.tb.rule.IdCtrlschemeVO;
import nc.vo.tb.task.MdTask;

public class BudgetControlCTL {
	private static final String defaultDataStr = "9999-99-99";
	private static final String defaultMonthDay = "01";
	private static final String zero = "0";
	private static final String sep = "-";

	public BudgetControlCTL() {
	}

	private static IdSysregVO getSelectSystem(String sysid) throws BusinessException {
		if ((sysid == null) || (sysid.length() == 0)) {
			return null;
		}
		IdSysregVO[] m_regvos = BusiSysReg.getSharedInstance().getAllSysVOs();
		for (int i = 0; i < m_regvos.length; i++) {
			if (m_regvos[i].getSysid().equals(sysid)) {
				return m_regvos[i];
			}
		}
		return null;
	}

	public static String getBdinfoType(String[] fromitem, String sysid) throws Exception {
		IdBdcontrastVO[] bdcontrasts = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fromitem.length; i++) {
			for (int j = 0; j < bdcontrasts.length; j++) {
				if (bdcontrasts[j].getAtt_fld().equals(fromitem[i])) {
					String bdinfotype = bdcontrasts[j].getBdinfo_type();
					buffer.append(bdinfotype + ":");
					break;
				}
			}
		}
		return buffer.toString();
	}

	private static String getBdinfoType(String fromitem, String sysid) throws Exception {
		IdBdcontrastVO[] bdcontrasts = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		StringBuffer buffer = new StringBuffer();
		String[] ss = fromitem.split(":");
		for (int i = 0; i < ss.length; i++) {
			for (int j = 0; j < bdcontrasts.length; j++) {
				if (bdcontrasts[j].getAtt_fld().equals(ss[i])) {
					String bdinfotype = bdcontrasts[j].getBdinfo_type();
					buffer.append(bdinfotype + ":");
					break;
				}
			}
		}
		return buffer.toString();
	}

	private static String parseBillTypes(String billtyes) {
		if ((billtyes == null) || (billtyes.trim().length() == 0)) {
			return null;
		}
		StringBuffer buffer = new StringBuffer();
		while (billtyes.indexOf("]") >= 0) {
			if (buffer.toString().length() == 0) {
				buffer.append(billtyes.substring(billtyes.indexOf("[") + 1, billtyes.indexOf("]")));
			} else {
				buffer.append(",");
				buffer.append(billtyes.substring(billtyes.indexOf("[") + 1, billtyes.indexOf("]")));
			}
			billtyes = billtyes.substring(billtyes.indexOf("]") + 1);
		}
		return buffer.toString();
	}

	private static UFDouble calcValue(UFDouble[] values) {
		Double sum = Double.valueOf(0.0D);
		for (int i = 0; i < values.length; i++) {
			sum = Double.valueOf(sum.doubleValue() + values[i].getDouble());
		}
		return new UFDouble(sum);
	}

	private static String parseVarno(String formulaExpress, HashMap exeVarnoMap, IdCtrlschemeVO[] vos, IdCtrlformulaVO vo) {
		Map<String, String> valueMap = new HashMap();
		for (int i = vos.length - 1; i >= 0; i--) {
			String var = vos[i].getVarno();
			UFDouble value = (UFDouble) exeVarnoMap.get(var);
			valueMap.put(var, value.toString());
		}
		String express1 = formulaExpress.split(vo.getCtrlsign())[0];
		String express2 = formulaExpress.split(vo.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap);
		formulaExpress = express1 + vo.getCtrlsign() + express2;
		return formulaExpress;
	}

	private static Boolean compareLeftValueAndMaxValue(UFDouble leftvalue, UFDouble value) {
		Boolean[] resultValue = null;
		StringBuffer str = new StringBuffer();
		str.append(leftvalue).append(">=").append(value);
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(str.toString(), null);
			resultValue = result.getBoolean();
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return resultValue[0];
	}

	private static Boolean compareLeftValueAndMinValue(UFDouble leftvalue, UFDouble value) {
		Boolean[] resultValue = null;
		StringBuffer str = new StringBuffer();
		str.append(leftvalue).append("<=").append(value);
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(str.toString(), null);
			resultValue = result.getBoolean();
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return resultValue[0];
	}

	public static UFDouble getComplexZxs(String complexformula) throws Exception {
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(complexformula, null);
			Object tmpResult = result.getValue();
			if ((tmpResult instanceof BigInteger)) {
				BigInteger bvalue = (BigInteger) tmpResult;
				Integer temp = Integer.valueOf(bvalue.intValue());
				return new UFDouble(temp.intValue());
			}
			if ((tmpResult instanceof String)) {
				Integer temp = new Integer((String) tmpResult);
				return new UFDouble(temp.intValue());
			}
			BigDecimal bvalue = (BigDecimal) tmpResult;
			Double temp = Double.valueOf(bvalue.doubleValue());
			return new UFDouble(temp);
		} catch (Exception e) {
			throw e;
		}
	}

	static Boolean[] needCtl(String exp) {
		Boolean[] value = null;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(exp, null);
			value = result.getBoolean();
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return value;
	}

	private static boolean needCtl(String expLeft, String expRight) {
		boolean valueLeft = true;
		boolean valueRight = true;
		try {
			if (!"".equals(expLeft)) {
				ArrayValue resultLeft = RefCompilerClient.getExpressionResult(expLeft, null);
				valueLeft = resultLeft.getBoolean()[0].booleanValue();
			}
			if (!"".equals(expRight)) {
				ArrayValue resultRight = RefCompilerClient.getExpressionResult(expRight, null);
				valueRight = resultRight.getBoolean()[0].booleanValue();
			}
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		if ((valueLeft) && (valueRight)) {
			return true;
		}
		return false;
	}

	private static UserNameObject[] getUserObjectByName(String pk_user) throws Exception {
		if ((pk_user == null) || (pk_user.trim().length() == 0)) {
			return null;
		}
		UserVO[] userVOs = (UserVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(UserVO.class, "user_name='" + pk_user + "'");
		if ((userVOs == null) || (userVOs.length == 0))
			return null;
		UserNameObject[] ret = new UserNameObject[userVOs.length];
		for (int i = 0; i < userVOs.length; i++) {
			UserVO userVO = userVOs[i];
			ret[i] = new UserNameObject(userVO.getUser_name());
			ret[i].setUserPK(userVO.getPrimaryKey());
			ret[i].setUserCode(userVO.getUser_code());
		}
		return ret;
	}

	private static UFDouble sumRunData(HashMap exeVarnoMap) {
		Iterator iter = exeVarnoMap.values().iterator();
		UFDouble sum = new UFDouble(0);
		while (iter.hasNext()) {
			UFDouble value = (UFDouble) iter.next();
			sum = sum.add(value);
		}
		return sum;
	}

	private static StringBuffer getZeroRuleCtlInfo(HashMap hashEffect) throws BusinessException {
		if ((hashEffect == null) || (hashEffect.isEmpty()))
			return null;
		StringBuffer sb = new StringBuffer();
		Map<IdCtrlschemeVO, IdCtrlformulaVO> schemeMap = new HashMap();
		List<String> schemePkList = new ArrayList();
		for (Object key : hashEffect.keySet()) {
			if ((key instanceof IdCtrlschemeVO)) {
				schemePkList.add(((IdCtrlschemeVO) key).getPk_ctrlformula());
			}
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append("pk_obj in (");
		for (String pk : schemePkList)
			buffer.append("'").append(pk).append("',");
		buffer.replace(buffer.length() - 1, buffer.length(), "");
		buffer.append(")");
		IdCtrlformulaVO[] formulaVOArr =
				(IdCtrlformulaVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlformulaVO.class, buffer.toString());
		for (Object key : hashEffect.keySet()) {
			if ((key instanceof IdCtrlschemeVO)) {
				IdCtrlschemeVO vo = (IdCtrlschemeVO) key;
				for (IdCtrlformulaVO formulaVO : formulaVOArr) {
					if (vo.getPk_ctrlformula().equals(formulaVO.getPrimaryKey())) {
						schemeMap.put(vo, formulaVO);
					}
				}
			}
		}
		for (Object key : hashEffect.keySet()) {
			if ((key instanceof IdCtrlschemeVO)) {
				IdCtrlschemeVO tmp = (IdCtrlschemeVO) key;
				if (sb.length() <= 0) {
					sb.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000049"));
				}
				sb.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(schemeMap.get(tmp) == null ? "null" : ((IdCtrlformulaVO) schemeMap.get(tmp)).getCtrlname()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
				sb.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(tmp.getNameidx()).append("  ").append(tmp.getStartdate()).append("~").append(tmp.getEnddate());
				sb.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050")).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb;
	}

	public static StringBuffer isZeroRuleControl(HashMap zeroSchmMap) throws BusinessException {
		HashMap hashEffect = filterBillsCtrl(zeroSchmMap);
		return getZeroRuleCtlInfo(hashEffect);
	}

	public static UFDouble[] getUFDouble(UFDouble[] data) {
		if (data == null) {
			data = new UFDouble[] { UFDouble.ZERO_DBL, UFDouble.ZERO_DBL, UFDouble.ZERO_DBL, UFDouble.ZERO_DBL };
		} else {
			for (int i = 0; i < (data == null ? 0 : data.length); i++) {
				if ((data == null) || (data.length == 0))
					data[i] = new UFDouble(0);
			}
		}
		return data;
	}

	public static HashMap getAddValueMap(HashMap exeValuemap, HashMap isStartMap, Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap)
			throws BusinessException {
		HashMap<String, UFDouble> map = new HashMap();
		Iterator iter = exeValuemap.keySet().iterator();
		try {
			while (iter.hasNext()) {
				IdCtrlschemeVO vo = (IdCtrlschemeVO) iter.next();
				boolean isSpecLev = false;
				UFDouble runDouble = new UFDouble(0);
				UFDouble readyDouble = new UFDouble(0);
				Iterator iter1 = isStartMap.keySet().iterator();
				IdCtrlformulaVO vo2 = null;
				IdCtrlformulaVO vo1;
				while (iter1.hasNext()) {
					vo1 = (IdCtrlformulaVO) iter1.next();
					ArrayList<IdCtrlschemeVO> schemeList = (ArrayList) isStartMap.get(vo1);
					for (IdCtrlschemeVO vo3 : schemeList)
						if (vo3.getPrimaryKey().equals(vo.getPrimaryKey()))
							vo2 = vo1;
				}
				if (vo2 != null) {
					for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : relaSchemeMap.entrySet()) {
						if (vo2.getPk_parent().equals(((IdCtrlformulaVO) entry.getKey()).getPk_parent())) {
							for (IdCtrlschemeVO _vo : (List<IdCtrlschemeVO>) entry.getValue()) {
								UFDouble runvalue = _vo.getRundata();
								UFDouble readyvalue = _vo.getReadydata();
								runDouble = runDouble.add(runvalue);
								readyDouble = readyDouble.add(readyvalue);
								isSpecLev = true;
							}
						}
					}
				}
				HashMap<String, UFDouble> busivalue = (HashMap) exeValuemap.get(vo);
				UFDouble runvalue = vo.getRundata() == null ? new UFDouble(0, 2) : vo.getRundata();
				UFDouble readyvalue = vo.getReadydata() == null ? new UFDouble(0, 2) : vo.getReadydata();
				if (isSpecLev) {
					runvalue = runvalue.sub(runDouble);
					readyvalue = readyvalue.sub(readyvalue);
				}
				UFDouble run_value =
						(busivalue.get("UFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("UFIND")).add(runvalue);
				UFDouble ready_value =
						(busivalue.get("PREFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("PREFIND")).add(readyvalue);
				UFDouble oldZxs = runvalue.add(readyvalue);
				map.put(vo.getPrimaryKey(), run_value.add(ready_value));
				map.put("ROWPOWER", busivalue.get("ROWPOWER"));
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000014"));
		}
		return map;
	}

	public static HashMap getOldValueMap(HashMap exeValuemap) throws BusinessException {
		HashMap<String, UFDouble> map = new HashMap();
		Iterator iter = exeValuemap.keySet().iterator();
		try {
			while (iter.hasNext()) {
				IdCtrlschemeVO vo = (IdCtrlschemeVO) iter.next();
				HashMap<String, UFDouble> busivalue = (HashMap) exeValuemap.get(vo);
				UFDouble runvalue = vo.getRundata() == null ? new UFDouble(0, 2) : vo.getRundata();
				UFDouble readyvalue = vo.getReadydata() == null ? new UFDouble(0, 2) : vo.getReadydata();
				UFDouble run_value =
						(busivalue.get("UFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("UFIND")).add(runvalue);
				UFDouble ready_value =
						(busivalue.get("PREFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("PREFIND")).add(readyvalue);
				UFDouble oldZxs = runvalue.add(readyvalue);
				map.put(vo.getPrimaryKey(), oldZxs);
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000014"));
		}
		return map;
	}

	public static ArrayList<String> getWouldExeSql(HashMap exeValuemap) throws BusinessException {
		ArrayList<String> sqlList = new ArrayList();
		Iterator iter = exeValuemap.keySet().iterator();
		while (iter.hasNext()) {
			IdCtrlschemeVO vo = (IdCtrlschemeVO) iter.next();
			HashMap busivalue = (HashMap) exeValuemap.get(vo);
			UFDouble runvalue = vo.getRundata() == null ? new UFDouble(0, 2) : vo.getRundata();
			UFDouble readyvalue = vo.getReadydata() == null ? new UFDouble(0, 2) : vo.getReadydata();
			UFDouble run_value = (busivalue.get("UFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("UFIND")).add(runvalue);
			UFDouble ready_value =
					(busivalue.get("PREFIND") == null ? new UFDouble(0, 2) : (UFDouble) busivalue.get("PREFIND")).add(readyvalue);
			String sql =
					"update tb_ctrlscheme set rundata = " + run_value.toString() + " , readydata = " + ready_value + "  where pk_obj = '" + vo.getPrimaryKey() + "'";
			sqlList.add(sql);
		}
		return sqlList;
	}

	private static String[] decomposeExpress(String express, HashMap exeVarnoMap, IdCtrlschemeVO[] vos) {
		String[] arrayExpress = new String[2];
		if (express.indexOf(">=") > 0) {
			arrayExpress = express.split(">=");
		} else if (express.indexOf("<=") > 0) {
			arrayExpress = express.split("<=");
		} else if (express.indexOf("<") > 0) {
			arrayExpress = express.split("<");
		} else if (express.indexOf(">") > 0) {
			arrayExpress = express.split(">");
		} else if (express.indexOf("=") > 0) {
			arrayExpress = express.split("=");
		}
		return arrayExpress;
	}

	public static UFDouble getPlanValue(String express, ArrayList<IdCtrlschemeVO> vos) throws Exception {
		UFDouble planvalue = null;
		String tmpExpress = express;
		for (int n = 0; n < vos.size(); n++) {
			UFDouble tmpValue = new UFDouble(0);
			tmpValue = tmpValue.add(((IdCtrlschemeVO) vos.get(n)).getReadydata()).add(((IdCtrlschemeVO) vos.get(n)).getRundata());
			tmpExpress = tmpExpress.replaceAll(((IdCtrlschemeVO) vos.get(n)).getVarno(), tmpValue.toString());
		}
		String[] strs = tmpExpress.split(">=");
		strs[0] = strs[0].replaceAll("%", "/100");
		planvalue = getComplexZxs(strs[0]);
		return planvalue;
	}

	private static String getExpressValueType(String express, HashMap exeVarnoMap, IdCtrlschemeVO[] vos, IdCtrlformulaVO vo) {
		String[] arrayExpress = new String[4];
		ArrayList<String> nameList = new ArrayList();
		Map<String, String> valueMap = new HashMap();
		for (int i = vos.length - 1; i >= 0; i--) {
			String var = vos[i].getVarno();
			UFDouble value = (UFDouble) exeVarnoMap.get(var);
			if ((var.indexOf("var") >= 0) && (express.indexOf("var") > express.indexOf(">="))) {
				arrayExpress[1] = value.toString();
				nameList.add(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000009"));
			} else if ((var.indexOf("rar") >= 0) && (express.indexOf("rar") > express.indexOf(">="))) {
				arrayExpress[2] = value.toString();
				nameList.add(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000010"));
			}
			valueMap.put(var, "0");
		}
		String express1 = express.split(vo.getCtrlsign())[0];
		String express2 = express.split(vo.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap);
		express = express1 + vo.getCtrlsign() + express2;
		StringBuffer sbStr = new StringBuffer(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007"));
		for (int n = 0; n < nameList.size(); n++) {
			if (n != nameList.size() - 1) {
				sbStr.append((String) nameList.get(n)).append(",");
			} else {
				sbStr.append((String) nameList.get(n));
			}
		}
		sbStr.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
		String[] _arrayExpress = new String[2];
		if (express.indexOf(">=") > 0) {
			_arrayExpress = express.split(">=");
		}
		arrayExpress[0] = _arrayExpress[0];
		try {
			UFDouble value = getComplexZxs(_arrayExpress[1]);
			if (UFDouble.ZERO_DBL.compareTo(value) != 0) {
				arrayExpress[1] = value.toString();
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
		}
		return sbStr.toString();
	}

	private static boolean compareOldValueAndValue(String oldValueExperss, String valueExpress) throws Exception {
		boolean isCheckScheme = true;
		String[] oldarrayExpress = new String[2];
		String[] arrayExpress = new String[2];
		if (oldValueExperss.indexOf(">=") > 0) {
			oldarrayExpress = oldValueExperss.split(">=");
		} else if (oldValueExperss.indexOf("<=") > 0) {
			oldarrayExpress = oldValueExperss.split("<=");
		} else if (oldValueExperss.indexOf("<") > 0) {
			oldarrayExpress = oldValueExperss.split("<");
		} else if (oldValueExperss.indexOf(">") > 0) {
			oldarrayExpress = oldValueExperss.split(">");
		} else if (oldValueExperss.indexOf("=") > 0) {
			oldarrayExpress = oldValueExperss.split("=");
		}
		String oldExperss = oldarrayExpress[1];
		if (valueExpress.indexOf(">=") > 0) {
			arrayExpress = valueExpress.split(">=");
		} else if (valueExpress.indexOf("<=") > 0) {
			arrayExpress = valueExpress.split("<=");
		} else if (valueExpress.indexOf("<") > 0) {
			arrayExpress = valueExpress.split("<");
		} else if (valueExpress.indexOf(">") > 0) {
			arrayExpress = valueExpress.split(">");
		} else if (valueExpress.indexOf("=") > 0) {
			arrayExpress = valueExpress.split("=");
		}
		String experss = arrayExpress[1];
		UFDouble oldValue = getComplexZxs(oldExperss);
		UFDouble value = getComplexZxs(experss);
		// 20170426 tsy 金额不变时，也不需要进行控制
		if (oldValue.compareTo(value) >= 0) {
			// 20170426 end
			isCheckScheme = false;
		} else {
			isCheckScheme = true;
		}
		return isCheckScheme;
	}

	private static String getDimInfo(IdCtrlformulaVO vo, IdCtrlschemeVO[] vosDb, String planSysName) {
		StringBuffer buffer = new StringBuffer();
		IdCtrlschemeVO tempvosDb = vosDb[0];
		String entityPk = vosDb[0].getPk_org();
		String entityName = "";
		String[] ss = tempvosDb.getNameidx().split(":");
		String[] pkidx = tempvosDb.getStridx().split(":");
		for (int n = 0; n < pkidx.length; n++) {
			if ((entityPk != null) && (entityPk.equals(pkidx[n]))) {
				entityName = ss[n];
				break;
			}
		}
		buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000235", null, new String[] { planSysName })).append(entityName).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000236"));
		for (int i = 0; i < vosDb.length; i++) {
			String[] ss1 = vosDb[i].getNameidx().split(":");
			for (int j = 0; j < ss1.length; j++) {
				buffer.append(ss1[j] + "/");
			}
			if (!vo.getSchemetype().equals(String.valueOf(2))) {
				buffer.append(vosDb[i].getStartdate()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000237")).append(vosDb[i].getEnddate()).append("/");
			}
			buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
			if (i != vosDb.length - 1)
				buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007"));
		}
		String diminfo = buffer.toString();
		return diminfo;
	}

	private static HashMap<String, String> getPlanSysName(IdCtrlformulaVO vo) {
		HashMap<String, String> map = new HashMap();
		MdTask plan = null;
		try {
			plan = nc.ms.tb.task.TbTaskCtl.getMdTaskByPk(vo.getPk_plan(), true);
		} catch (BusinessException e) {
			NtbLogger.printException(e);
		}
		String planname = plan == null ? "" : plan.getObjname();
		map.put(CtrlInfoMacroConst.taskinfoMacro, planname);
		map.put(CtrlInfoMacroConst.taskInfoInsysMacro, BusiTermConst.getSysNameByCode(plan.getAvabusisystem()));
		return map;
	}

	public static String getCtrlModelName(IdCtrlformulaVO vo) {
		String ctrltype = "";
		if (vo.getCtlmode().equals(CtrlTypeEnum.RigidityControl.toCodeString())) {
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000161");
		} else if (vo.getCtlmode().equals(CtrlTypeEnum.FlexibleControl.toCodeString())) {
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000162");
		} else if (vo.getCtlmode().equals(CtrlTypeEnum.WarningControl.toCodeString())) {
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000163");
		}
		return ctrltype;
	}

	public static NtbCtlInfoVO getInfos(HashMap hashEffect, HashMap oldhashEffect, ArrayList<String> sql, HashMap groupMap, IAccessableBusiVO busivo, boolean isExeSql)
			throws BusinessException {
		try {
			NtbCtlInfoVO returnvo = new NtbCtlInfoVO();
			ArrayList<String> infolist_ctrl = new ArrayList();
			ArrayList<String> infolist_flexCtrl = new ArrayList();
			ArrayList<String> infolist_alarm = new ArrayList();
			Iterator itera = groupMap.keySet().iterator();
			HashMap<String, ArrayList<String>> messageInfols = new HashMap();
			ArrayList<String> resultList = new ArrayList();
			UFDouble resultValue = UFDouble.ZERO_DBL;
			while (itera.hasNext()) {
				HashMap<String, UFDouble> exeVarnoMap = new HashMap();
				IdCtrlformulaVO vo = (IdCtrlformulaVO) itera.next();
				String formulaExpress = vo.getExpressformula();
				ArrayList ls = (ArrayList) groupMap.get(vo);
				IdCtrlschemeVO[] vosDB = (IdCtrlschemeVO[]) ls.toArray(new IdCtrlschemeVO[0]);
				IdCtrlschemeVO tempvosDb = vosDB[0];
				boolean isCheckScheme = true;
				for (int i = 0; i < vosDB.length; i++) {
					if (hashEffect.containsKey(vosDB[i].getPrimaryKey())) {
						tempvosDb = vosDB[i];
						UFDouble exevalue = (UFDouble) hashEffect.get(vosDB[i].getPrimaryKey());
						exeVarnoMap.put(vosDB[i].getVarno(), exevalue);
					} else {
						UFDouble runvalue = vosDB[i].getRundata() == null ? UFDouble.ZERO_DBL : vosDB[i].getRundata();
						UFDouble readyvalue = vosDB[i].getReadydata() == null ? UFDouble.ZERO_DBL : vosDB[i].getReadydata();
						UFDouble zs = new UFDouble(0);
						zs = zs.add(runvalue).add(readyvalue);
						exeVarnoMap.put(vosDB[i].getVarno(), zs);
					}
				}
				HashMap<String, UFDouble> exeOldVarnoMap = new HashMap();
				for (int i = 0; i < vosDB.length; i++) {
					if (oldhashEffect.containsKey(vosDB[i].getPrimaryKey())) {
						tempvosDb = vosDB[i];
						UFDouble exevalue = (UFDouble) oldhashEffect.get(vosDB[i].getPrimaryKey());
						exeOldVarnoMap.put(vosDB[i].getVarno(), exevalue);
					} else {
						UFDouble runvalue = vosDB[i].getRundata() == null ? UFDouble.ZERO_DBL : vosDB[i].getRundata();
						UFDouble readyvalue = vosDB[i].getReadydata() == null ? UFDouble.ZERO_DBL : vosDB[i].getReadydata();
						UFDouble zs = new UFDouble(0);
						zs = zs.add(runvalue).add(readyvalue);
						exeOldVarnoMap.put(vosDB[i].getVarno(), zs);
					}
				}
				String valuenameType = getExpressValueType(formulaExpress, exeVarnoMap, vosDB, vo);
				formulaExpress = parseVarno(formulaExpress, exeVarnoMap, vosDB, vo);
				String oldformulaExpress = parseVarno(vo.getExpressformula(), exeOldVarnoMap, vosDB, vo);
				if (vo.getSchemetype().equals("TBRULE000SCHEMA_FLEX")) {
					String pk_cube = vo.getPk_cube();
					String pk_dimvector = vo.getPk_dimvector();
					IStringConvertor cvt = nc.ms.mdm.convertor.StringConvertorFactory.getConvertor(DimVector.class);
					DimVector dimvector = (DimVector) cvt.fromString(pk_dimvector);
					nc.vo.mdm.cube.CubeDef cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(pk_cube);
					ArrayList<DimVector> newDatacells = new ArrayList();
					newDatacells.add(dimvector);
					ICubeDataSet dataset = CubeServiceGetter.getDataSetService().queryDataSet(cubedef, newDatacells);
					nc.vo.mdm.cube.DataCell datacell = dataset.getDataCell(dimvector);
					ArrayList<CtrlSchemeVO> vos = new ArrayList();
					CtrlSchemeVO _vo = new CtrlSchemeVO(formulaExpress, vo.getPk_parent(), datacell, vo.getPk_plan());
					vos.add(_vo);
					formulaExpress = formulaExpress.replaceAll("FLEXEXPRESS\\(\\)", CtlSchemeCTL.parseFlexAlgorithm(vos));
				}
				UFDouble zxs_complex = new UFDouble(0);
				formulaExpress = FormulaParser.parseToNumSrc(formulaExpress);
				oldformulaExpress = FormulaParser.parseToNumSrc(oldformulaExpress);
				isCheckScheme = compareOldValueAndValue(oldformulaExpress, formulaExpress);
				String[] arrayExpress = decomposeExpress(formulaExpress, exeVarnoMap, vosDB);
				resultValue = getMaxBudget(arrayExpress, resultValue);
				Boolean[] needctl = needCtl(formulaExpress);
				// 20170426 tsy 审批时，不进行预算控制
				while ((!needctl[0].booleanValue()) && (isCheckScheme)) {// 超过预算了
					String approvestatus = null;// 审批状态spzt,busivo.getBusiSys().toLowerCase()+_h_approvestatus
					String money = null;// 发生金额
					String approvedate = null;// 审批时间 shrq
					try {
						approvedate = busivo.getAttributesValue("shrq");
					} catch (IndexOutOfBoundsException e) {// 方法具体实现不同，部分可能发生越界错误
						Logger.log("获取approvedate时出错，转为获取" + busivo.getBusiSys().toLowerCase() + "_h_approvedate");
						approvedate = busivo.getAttributesValue(busivo.getBusiSys().toLowerCase() + "_h_approvedate");
					}
					if (approvedate != null && !"".equals(approvedate.trim())) {// 已审<--->未审,两种转化，时间都是有的
						try {
							money = busivo.getAttributesValue("total");
						} catch (IndexOutOfBoundsException e) {// 方法具体实现不同，部分可能发生越界错误
							Logger.log("获取total时出错，转为获取" + busivo.getBusiSys().toLowerCase() + "_h_local_money");
							money = busivo.getAttributesValue(busivo.getBusiSys().toLowerCase() + "_h_local_money");
						}
						if (money != null && new UFDouble(money).compareTo(UFDouble.ZERO_DBL) <= 0) {
							Logger.log("当前单据金额字段为" + money + "(负数)，不需要进行预算控制");
							needctl[0] = true;
							break;
						}
						break;
					}
					break;
				}
				// 20170426 end
				UFDouble powerInt = (UFDouble) hashEffect.get("ROWPOWER");
				if ((!needctl[0].booleanValue()) && (isCheckScheme)) {
					IdCtrlInfoVO infovo = RuleServiceGetter.getIBusiRuleQuery().queryCtrlInfoVOByPk(vo.getPk_parent());
					HashMap<String, String> infoMap = getCtrlInfoMap(arrayExpress, vo, vosDB);
					if (vo.getCtlmode().equals(CtrlTypeEnum.RigidityControl.toCodeString())) {
						String message = null;
						if ((infovo != null) && (infovo.getInfoexpress() != null)) {
							message = CtlSchemeCTL.getFinalCtrlInfoMessage(infoMap, infovo.getInfoexpress());
						} else {
							message =
									CtlSchemeCTL.getControlCtlMessage(vo, tempvosDb, exeVarnoMap, zxs_complex, arrayExpress, valuenameType, Integer.parseInt(powerInt.toString()));
						}
						infolist_ctrl.add(message);
						String pk_plan = vo.getPk_plan();
						if (messageInfols.containsKey(pk_plan)) {
							ArrayList<String> list = (ArrayList) messageInfols.get(pk_plan);
							list.add(message);
						} else {
							ArrayList<String> list = new ArrayList();
							list.add(message);
							messageInfols.put(pk_plan, list);
						}
					} else if (vo.getCtlmode().equals(CtrlTypeEnum.FlexibleControl.toCodeString())) {
						String message = null;
						if ((infovo != null) && (infovo.getInfoexpress() != null)) {
							message = CtlSchemeCTL.getFinalCtrlInfoMessage(infoMap, infovo.getInfoexpress());
						} else {
							message =
									CtlSchemeCTL.getControlCtlMessage(vo, tempvosDb, exeVarnoMap, zxs_complex, arrayExpress, valuenameType, Integer.parseInt(powerInt.toString()));
						}
						infolist_flexCtrl.add(message);
						String pk_plan = vo.getPk_plan();
						if (messageInfols.containsKey(pk_plan)) {
							ArrayList<String> list = (ArrayList) messageInfols.get(pk_plan);
							list.add(message);
						} else {
							ArrayList<String> list = new ArrayList();
							list.add(message);
							messageInfols.put(pk_plan, list);
						}
					} else if (vo.getCtlmode().equals(CtrlTypeEnum.WarningControl.toCodeString())) {
						String message = null;
						if ((infovo != null) && (infovo.getInfoexpress() != null)) {
							message = CtlSchemeCTL.getFinalCtrlInfoMessage(infoMap, infovo.getInfoexpress());
						} else {
							message =
									CtlSchemeCTL.getControlAlarmMessage(vo, tempvosDb, exeVarnoMap, zxs_complex, arrayExpress, valuenameType, Integer.parseInt(powerInt.toString()));
						}
						infolist_alarm.add(message);
						String pk_plan = vo.getPk_plan();
						if (messageInfols.containsKey(pk_plan)) {
							ArrayList<String> list = (ArrayList) messageInfols.get(pk_plan);
							list.add(message);
						} else {
							ArrayList<String> list = new ArrayList();
							list.add(message);
							messageInfols.put(pk_plan, list);
						}
					}
					resultList.add(vo.getCtlmode());
				} else if ((vo.getSpecialUsage() != null) && (vo.getSpecialUsage().booleanValue())) {
					String pkPlan = vo.getPk_plan();
					IdCtrlformulaVO alertPerVO = AlertPercentHandler.getCurrAlertPerCtrlFormula(vo, formulaExpress);
					if (alertPerVO != null) {
						IdCtrlInfoVO infovo = RuleServiceGetter.getIBusiRuleQuery().queryCtrlInfoVOByPk(vo.getPk_parent());
						String[] arrExp2 = decomposeExpress(alertPerVO.getExpressformula(), exeVarnoMap, vosDB);
						String message = null;
						HashMap<String, String> infoMap = getCtrlInfoMap(arrExp2, alertPerVO, vosDB);
						if ((infovo != null) && (infovo.getInfoexpress() != null)) {
							message = CtlSchemeCTL.getFinalCtrlInfoMessage(infoMap, infovo.getInfoexpress());
						} else {
							message =
									CtlSchemeCTL.getControlAlarmMessage(alertPerVO, tempvosDb, exeVarnoMap, zxs_complex, arrExp2, valuenameType, Integer.parseInt(powerInt.toString()));
						}
						if (messageInfols.containsKey(pkPlan)) {
							ArrayList<String> list = (ArrayList) messageInfols.get(pkPlan);
							list.add(message);
						} else {
							ArrayList<String> list = new ArrayList();
							list.add(message);
							messageInfols.put(pkPlan, list);
						}
						infolist_alarm.add(message);
						resultList.add(CtrlTypeEnum.WarningControl.toCodeString());
					}
				}
			}
			if (isExeSql) {
				if (resultList.contains(CtrlTypeEnum.RigidityControl.toCodeString())) {
					returnvo.setIsControl(true);
				} else if (resultList.contains(CtrlTypeEnum.FlexibleControl.toCodeString())) {
					returnvo.setIsMayBeControl(true);
					returnvo.setExeSql(sql);
					NtbSuperServiceGetter.getINtbSuper().execSqlAndReturnTs((String[]) sql.toArray(new String[0]));
				} else if (resultList.contains(CtrlTypeEnum.WarningControl.toCodeString())) {
					returnvo.setIsAlarm(true);
					NtbSuperServiceGetter.getINtbSuper().execSqlAndReturnTs((String[]) sql.toArray(new String[0]));
				} else {
					NtbSuperServiceGetter.getINtbSuper().execSqlAndReturnTs((String[]) sql.toArray(new String[0]));
				}
			} else if (resultList.contains(CtrlTypeEnum.RigidityControl.toCodeString())) {
				returnvo.setIsControl(true);
			} else if (resultList.contains(CtrlTypeEnum.FlexibleControl.toCodeString())) {
				returnvo.setIsMayBeControl(true);
			} else if (resultList.contains(CtrlTypeEnum.WarningControl.toCodeString())) {
				returnvo.setIsAlarm(true);
			}
			if (infolist_ctrl.size() > 0) {
				String[] allMessages = (String[]) infolist_ctrl.toArray(new String[0]);
				String oneMessage = "";
				for (int i = 0; i < allMessages.length; i++) {
					if (i == 0) {
						oneMessage = allMessages[i];
					} else {
						oneMessage = oneMessage + "\n" + allMessages[i];
					}
				}
				returnvo.setControlInfos(new String[] { oneMessage });
			}
			if (infolist_flexCtrl.size() > 0) {
				String[] allMessages = (String[]) infolist_flexCtrl.toArray(new String[0]);
				String oneMessage = "";
				for (int i = 0; i < allMessages.length; i++) {
					if (i == 0) {
						oneMessage = allMessages[i];
					} else {
						oneMessage = oneMessage + "\n" + allMessages[i];
					}
				}
				returnvo.setFlexibleControlInfos(new String[] { oneMessage });
			}
			if (infolist_alarm.size() > 0) {
				String[] allMessages = (String[]) infolist_alarm.toArray(new String[0]);
				String oneMessage = "";
				for (int i = 0; i < allMessages.length; i++) {
					if (i == 0) {
						oneMessage = allMessages[i];
					} else {
						oneMessage = oneMessage + "\n" + allMessages[i];
					}
				}
				returnvo.setAlarmInfos(new String[] { oneMessage });
			}
			UFDouble[] tmpValue = { resultValue, resultValue, resultValue, resultValue };
			returnvo.setPlanData(tmpValue);
			return returnvo;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex);
		}
	}

	public static HashMap<String, String> getCtrlInfoMap(String[] arrayExpress, IdCtrlformulaVO vo, IdCtrlschemeVO[] vosDb) {
		HashMap<String, String> infoMap = new HashMap();
		String budgetValue = null;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(arrayExpress[0]);
			BigDecimal decimal = result.getDecimal()[0];
			budgetValue = decimal.toString();
		} catch (Exception e) {
			NtbLogger.error(e.getMessage());
			budgetValue = arrayExpress[0];
		}
		String realValue = null;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(arrayExpress[1]);
			BigDecimal decimal = result.getDecimal()[0];
			realValue = decimal.toString();
		} catch (Exception e) {
			NtbLogger.error(e.getMessage());
			realValue = arrayExpress[1];
		}
		UFDouble budgetDatVal;
		UFDouble realValueSc;
		UFDouble balanceData;
		try {
			String[] planValues = arrayExpress[0].split("\\*");
			StringBuffer planBuffer = new StringBuffer();
			for (int i = 0; i < planValues[0].length(); i++) {
				if ((planValues[0].charAt(i) != '.') && (!Character.isDigit(planValues[0].charAt(i))))
					break;
				planBuffer.append(planValues[0].charAt(i));
			}
			UFDouble planVal = new UFDouble(planBuffer.toString());
			int power1 = planVal.getPower();
			budgetDatVal = new UFDouble(budgetValue).setScale(power1, 4);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < arrayExpress[1].length(); i++) {
				if ((arrayExpress[1].charAt(i) != '.') && (!Character.isDigit(arrayExpress[1].charAt(i))))
					break;
				buffer.append(arrayExpress[1].charAt(i));
			}
			int power2 = 2;
			realValueSc = new UFDouble(realValue).setScale(power2, 4);
			UFDouble realVal = new UFDouble(realValue);
			int powerPlan = planVal.getPower();
			int powerReal = realValueSc.getPower();
			balanceData = new UFDouble();
			if (Math.abs(powerPlan) > Math.abs(powerReal)) {
				balanceData = planVal.sub(realValueSc, powerPlan);
			} else {
				balanceData = planVal.sub(realValueSc, powerReal);
			}
		} catch (Exception e) {
			NtbLogger.error(e.getMessage());
			realValueSc = new UFDouble(0.0D);
			budgetDatVal = new UFDouble(0.0D);
			balanceData = new UFDouble(0.0D);
		}
		infoMap.put(CtrlInfoMacroConst.rundataMacro, realValueSc.toString());
		infoMap.put(CtrlInfoMacroConst.controldataMacro, budgetDatVal.toString());
		infoMap.put(CtrlInfoMacroConst.taskInfoInsysMacro, getPlanSysName(vo).get(CtrlInfoMacroConst.taskInfoInsysMacro));
		infoMap.put(CtrlInfoMacroConst.diminfoMacro, getDimInfo(vo, vosDb, (String) infoMap.get(CtrlInfoMacroConst.taskInfoInsysMacro)));
		infoMap.put(CtrlInfoMacroConst.taskinfoMacro, getPlanSysName(vo).get(CtrlInfoMacroConst.taskinfoMacro));
		infoMap.put(CtrlInfoMacroConst.sysinfoMacro, "");
		infoMap.put(CtrlInfoMacroConst.ctrlTypeMacro, getCtrlModelName(vo));
		infoMap.put(CtrlInfoMacroConst.isStartFinishMacro, "");
		infoMap.put(CtrlInfoMacroConst.ctrlschemeMacro, vo.getCtrlname());
		infoMap.put(CtrlInfoMacroConst.budgetBalanceDataMacro, balanceData.toString());
		return infoMap;
	}

	public static UFDouble getMaxBudget(String[] expressValue, UFDouble resultValue) {
		String value1 = expressValue[0];
		String value2 = expressValue[1];
		double[] sums = new double[2];
		sums[0] = (-getValue(value1).doubleValue());
		sums[1] = getValue(value2).doubleValue();
		UFDouble result = UFDouble.sum(sums);
		if (result.compareTo(resultValue) > 0) {
			resultValue = result;
		}
		return resultValue;
	}

	public static UFDouble getValue(String exp) {
		Object reValue = null;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(exp, null);
			if (result.getType() == 0) {
				reValue = (String) result.getValue();
			} else if (result.getType() == 5) {
				reValue = (UFDouble) result.getValue();
			} else if (result.getType() == 6) {
				reValue = (Double) result.getValue();
			} else if (result.getType() == 1) {
				reValue = new UFDouble(result.getValue().toString());
			} else if (result.getType() == 4) {
				reValue = new UFDouble(result.getValue().toString());
			} else if (result.getType() == 3)
				reValue = UFBoolean.valueOf(result.getValue().toString());
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return (UFDouble) reValue;
	}

	public static int getCurrtypeDigit(String pk_currtype) throws BusinessException {
		if (pk_currtype == null) {
			return 2;
		}
		return CurrtypeQuery.getInstance().getCurrdigit(pk_currtype);
	}

	public static HashMap calcExeValue(HashMap hashEffect) throws BusinessException {
		HashMap<IdCtrlschemeVO, HashMap<String, UFDouble>> map = new HashMap();
		Iterator itera = hashEffect.keySet().iterator();
		while (itera.hasNext()) {
			UFDouble powerInt = new UFDouble(0, 0);
			IdCtrlschemeVO vo = (IdCtrlschemeVO) itera.next();
			IAccessableBusiVO[] busivos = (IAccessableBusiVO[]) hashEffect.get(vo);
			String funCode = vo.getMethodname();
			UFDouble[] datas = new UFDouble[busivos.length];
			for (int i = 0; i < busivos.length; i++) {
				if (funCode.equals(busivos[i].getDataType() == null ? "UFIND" : busivos[i].getDataType())) {
					UFDouble[] data = null;
					int curr_type = CtlSchemeCTL.getCurrencyType(vo.getPk_currency());
					String pk_currency = CtlSchemeCTL.getActualPkcurrency(vo.getPk_currency(), vo.getPk_org(), vo.getCtrlsys());
					if ((busivos[i] instanceof IAccessableExtBusiVO)) {
						// 20170711 tsy F1开头的单据要计算无税金额
						if (((IAccessableExtBusiVO) busivos[i]).getBillType().startsWith(IArapBillTypeCons.F1)) {
							data =
									getUFDouble(((IAccessableExtBusiVO) busivos[i]).getExeData(vo.getCtrldirection(), "notax", vo.getCtrlobjValue(), curr_type, pk_currency));
						} else {
							data =
									getUFDouble(((IAccessableExtBusiVO) busivos[i]).getExeData(vo.getCtrldirection(), vo.getCtrlobj(), vo.getCtrlobjValue(), curr_type, pk_currency));
						}
						// data =
						// getUFDouble(((IAccessableExtBusiVO)
						// busivos[i]).getExeData(vo.getCtrldirection(),
						// vo.getCtrlobj(), vo.getCtrlobjValue(), curr_type,
						// pk_currency));
						// 20170711 end
					} else {
						// 20170711 tsy F1开头的单据要计算无税金额
						if (busivos[i].getBillType().startsWith(IArapBillTypeCons.F1)) {
							data = getUFDouble(busivos[i].getExeData(vo.getCtrldirection(), "notax", vo.getCtrlobjValue()));
						} else {
							data = getUFDouble(busivos[i].getExeData(vo.getCtrldirection(), vo.getCtrlobj(), vo.getCtrlobjValue()));
						}
						// data =
						// getUFDouble(busivos[i].getExeData(vo.getCtrldirection(),
						// vo.getCtrlobj(), vo.getCtrlobjValue()));
						// 20170711 end
					}
					datas[i] = data[curr_type];
					if ((datas[i] != null) && (Math.abs(datas[i].getPower()) > Math.abs(powerInt.doubleValue()))) {
						int currDigit = getCurrtypeDigit(pk_currency);
						powerInt = new UFDouble(currDigit, 0);
					}
				} else {
					datas[i] = UFDouble.ZERO_DBL;
				}
			}
			HashMap<String, UFDouble> datamap = new HashMap();
			if ("UFIND".equals(funCode)) {
				datamap.put("UFIND", calcValue(datas));
			} else if ("PREFIND".equals(funCode)) {
				datamap.put("PREFIND", calcValue(datas));
			}
			datamap.put("ROWPOWER", powerInt);
			map.put(vo, datamap);
		}
		return map;
	}

	public static HashMap<String, UFDouble> calcExePkValue(HashMap hashEffect) throws BusinessException {
		HashMap<String, UFDouble> map = new HashMap();
		Iterator itera = hashEffect.keySet().iterator();
		while (itera.hasNext()) {
			IdCtrlschemeVO vo = (IdCtrlschemeVO) itera.next();
			IAccessableBusiVO[] busivos = (IAccessableBusiVO[]) hashEffect.get(vo);
			String funCode = vo.getMethodname();
			UFDouble[] datas = new UFDouble[busivos.length];
			for (int i = 0; i < busivos.length; i++) {
				if (funCode.equals(busivos[i].getDataType() == null ? "UFIND" : busivos[i].getDataType())) {
					UFDouble[] data = null;
					int curr_type = CtlSchemeCTL.getCurrencyType(vo.getPk_currency());
					String pk_currency = CtlSchemeCTL.getPk_currency(vo.getPk_currency(), vo.getPk_org(), vo.getCtrlsys());
					if ((busivos[i] instanceof IAccessableExtBusiVO)) {
						data =
								getUFDouble(((IAccessableExtBusiVO) busivos[i]).getExeData(vo.getCtrldirection(), vo.getCtrlobj(), vo.getCtrlobjValue(), curr_type, pk_currency));
					} else {
						data = getUFDouble(busivos[i].getExeData(vo.getCtrldirection(), vo.getCtrlobj(), vo.getCtrlobjValue()));
					}
					datas[i] = data[curr_type];
				} else {
					datas[i] = UFDouble.ZERO_DBL;
				}
			}
			HashMap<String, UFDouble> datamap = new HashMap();
			if ("UFIND".equals(funCode)) {
				datamap.put("UFIND", calcValue(datas));
			} else if ("PREFIND".equals(funCode)) {
				datamap.put("PREFIND", calcValue(datas));
			}
			map.put(vo.getPrimaryKey(), datamap.get(vo.getMethodname()));
		}
		return map;
	}

	public static boolean isNeedTranRate(String ctrlObj) {
		return true;
	}

	public static int getDataLoaction(String busiSysId) {
		if ("GL".equals(busiSysId))
			return 1;
		if ("FA".equals(busiSysId))
			return 2;
		if ("PS".equals(busiSysId)) {
			return 0;
		}
		return 2;
	}

	public static HashMap getCtrlformualGroup(HashMap hashEffect, Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap)
			throws BusinessException {
		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> groupmap = new HashMap();
		StringBuffer buffer1 = new StringBuffer();
		StringBuffer buffer2 = new StringBuffer();
		IdCtrlschemeVO[] vo = null;
		IdCtrlformulaVO[] formulavo = null;
		try {
			Iterator itera = hashEffect.keySet().iterator();
			while (itera.hasNext()) {
				IdCtrlschemeVO schemeVO = (IdCtrlschemeVO) itera.next();
				String groupPk = schemeVO.getPk_ctrlformula();
				if (buffer1.length() == 0) {
					buffer1.append("pk_ctrlformula in ('");
					buffer1.append(groupPk);
					buffer1.append("'");
				} else if (buffer1.toString().indexOf(groupPk) < 0) {
					buffer1.append(",'");
					buffer1.append(groupPk);
					buffer1.append("'");
				}
			}
			if (buffer1.length() != 0) {
				buffer1.append(")");
				vo = (IdCtrlschemeVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlschemeVO.class, buffer1.toString());
			}
			Iterator itera1 = hashEffect.keySet().iterator();
			while (itera1.hasNext()) {
				IdCtrlschemeVO schemeVO = (IdCtrlschemeVO) itera1.next();
				String groupPk = schemeVO.getPk_ctrlformula();
				if (buffer2.length() == 0) {
					buffer2.append("pk_obj in ('");
					buffer2.append(groupPk);
					buffer2.append("'");
				} else if (buffer2.toString().indexOf(groupPk) < 0) {
					buffer2.append(",'");
					buffer2.append(groupPk);
					buffer2.append("'");
				}
			}
			if (buffer2.length() != 0) {
				buffer2.append(")");
				formulavo =
						(IdCtrlformulaVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlformulaVO.class, buffer2.toString());
			}
			for (int i = 0; i < (formulavo == null ? 0 : formulavo.length); i++) {
				if (!groupmap.containsKey(formulavo[i])) {
					ArrayList<IdCtrlschemeVO> list = new ArrayList();
					groupmap.put(formulavo[i], list);
				}
			}
			Iterator iter = groupmap.keySet().iterator();
			while (iter.hasNext()) {
				IdCtrlformulaVO tempvo = (IdCtrlformulaVO) iter.next();
				for (int j = 0; j < vo.length; j++) {
					if (vo[j].getPk_ctrlformula().equals(tempvo.getPrimaryKey())) {
						ArrayList templist = (ArrayList) groupmap.get(tempvo);
						templist.add(vo[j]);
					}
				}
			}
			if ((NtbEnv.isSepControl) && (hashEffect.size() == 1)) {
				reformSepControl(hashEffect, groupmap, relaSchemeMap);
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000051"));
		}
		return groupmap;
	}

	public static HashMap filterBillsCtrl(HashMap hashEffect) throws BusinessException {
		if (hashEffect.size() == 0) {
			return hashEffect;
		}
		IdCtrlschemeVO[] keyvos = (IdCtrlschemeVO[]) hashEffect.keySet().toArray(new IdCtrlschemeVO[0]);
		IBillsControl billcontrol = null;
		try {
			IAccessableBusiVO[] accessBO = (IAccessableBusiVO[]) hashEffect.get(keyvos[0]);
			billcontrol = (IBillsControl) accessBO[0];
		} catch (ClassCastException ex) {
			return hashEffect;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(e);
		}
		String[] billtypes = new String[keyvos.length];
		IAccessableBusiVO[][] busivos = new IAccessableBusiVO[keyvos.length][];
		HashMap<IdCtrlschemeVO, IAccessableBusiVO[]> hashReturn = new HashMap();
		for (int i = 0; i < keyvos.length; i++) {
			billtypes[i] = parseBillTypes(keyvos[i].getBilltype());
			busivos[i] = ((IAccessableBusiVO[]) (IAccessableBusiVO[]) hashEffect.get(keyvos[i]));
		}
		try {
			boolean[][] bctls = billcontrol.isBillControl(billtypes, busivos);
			for (int i = 0; i < keyvos.length; i++) {
				ArrayList<IAccessableBusiVO> list = new ArrayList();
				for (int j = 0; j < bctls[i].length; j++) {
					if (bctls[i][j]) {
						list.add(busivos[i][j]);
					}
				}
				if (list.size() > 0) {
					hashReturn.put(keyvos[i], (IAccessableBusiVO[]) list.toArray(new IAccessableBusiVO[0]));
				}
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex);
		}
		return hashReturn;
	}

	private static String[] getFatherOrgs(String pk_org, String sysId) {
		IFinanceOrgQryService ibOrg = (IFinanceOrgQryService) NCLocator.getInstance().lookup(IFinanceOrgQryService.class.getName());
		ArrayList<String> vosList = new ArrayList();
		try {
			FinanceOrgTreeVO[] vos = ibOrg.queryAllFinanceOrgTreeVOS();
			FinanceOrgTreeVO currentFinanceOrg = null;
			if (vos != null) {
				for (int n = 0; n < vos.length; n++) {
					if (pk_org.equals(vos[n].getPk_financeorg())) {
						currentFinanceOrg = vos[n];
						break;
					}
				}
				while ((currentFinanceOrg != null) && (currentFinanceOrg.getPk_fatherorg() != null)) {
					for (int n = 0; n < vos.length; n++) {
						if (currentFinanceOrg.getPk_fatherorg().equals(vos[n].getPk_financeorg())) {
							vosList.add(vos[n].getPk_financeorg());
							currentFinanceOrg = vos[n];
							break;
						}
					}
				}
			}
		} catch (BusinessException e) {
			NtbLogger.error(e);
		}
		return (String[]) vosList.toArray(new String[0]);
	}

	public static String getOrgFromIAccBusiVO(IAccessableBusiVO busiVO, String orgAtt) {
		return (busiVO instanceof IAccessableOrgsBusiVO) ? ((IAccessableOrgsBusiVO) busiVO).getPKOrg(orgAtt) : busiVO.getPKOrg();
	}

	public static void refreshNtbAccountCache(IAccessableBusiVO[] busivos) throws BusinessException {
		NtbAccountCacheManager.getNewInstance().clear();
		ArrayList<String> asoaPkList = new ArrayList();
		IAccessableBusiVO vo;
		for (int j = 0; j < busivos.length; j++) {
			vo = busivos[j];
			IdBdcontrastVO voDetail = BdContrastCache.getNewInstance().getVOByField(vo.getBusiSys(), "DETAIL103");
			IdBdcontrastVO voArap = BdContrastCache.getNewInstance().getVOByField(vo.getBusiSys(), "arap_b_subjcode");
			if ((voDetail != null) || (voArap != null)) {
				String tempPk = vo.getAttributesValue("DETAIL103");
				String _tempPk = vo.getAttributesValue("arap_b_subjcode");
				if ((tempPk != null) || (_tempPk != null)) {
					if ((tempPk != null) && (!asoaPkList.contains(tempPk))) {
						asoaPkList.add(tempPk);
					}
					if ((_tempPk != null) && (!asoaPkList.contains(_tempPk))) {
						asoaPkList.add(_tempPk);
					}
				}
			} else {
				ArrayList<IdBdcontrastVO> accBdVos =
						BdContrastCache.getNewInstance().getBdcontrastVOsByClassIdAndSysId(vo.getBusiSys(), "23a89307-5992-460e-95dd-c628c85f7f95");
				if ((accBdVos != null) && (accBdVos.size() > 0)) {
					for (IdBdcontrastVO bdvo : accBdVos) {
						String tempPk = vo.getAttributesValue(bdvo.getAtt_fld());
						if ((tempPk != null) && (!asoaPkList.contains(tempPk)))
							asoaPkList.add(tempPk);
					}
				}
			}
		}
		NtbAccountCacheManager.getNewInstance().setPks(asoaPkList);
	}

	public static String getFromItemWhereSQL(IAccessableBusiVO[] busivos) throws BusinessException {
		String sql_findgroup = "select distinct fromitems, ctllevels from tb_ctrlscheme where ctrlsys = '" + busivos[0].getBusiSys() + "'";
		Collection fromItemGroup = NtbSuperServiceGetter.getINtbSuper().execSql(sql_findgroup);
		refreshNtbAccountCache(busivos);
		List<String[]> fromitemResult = new ArrayList();
		List<String[]> ctllevelResult = new ArrayList();
		for (Object obj : fromItemGroup) {
			Object[] group = (Object[]) obj;
			String fromitems = (String) group[0];
			String ctllevels = (String) group[1];
			String[] fromitemArr = fromitems.split(":");
			String[] ctllevelArr = ctllevels.split(":");
			fromitemResult.add(fromitemArr);
			ctllevelResult.add(ctllevelArr);
		}
		int tableIndex = 0;
		StringBuffer sql = new StringBuffer();
		sql.append("(");
		for (int i = 0; i < fromitemResult.size(); i++) {
			String[] fromitems = (String[]) fromitemResult.get(i);
			String[] ctllevels = (String[]) ctllevelResult.get(i);
			List<String[]> itemPkList = new ArrayList();
			boolean isLackItem = false;
			for (int j = 0; j < fromitems.length; j++) {
				String fromItem = fromitems[j];
				String ctlLevel = ctllevels[j];
				List<String> item_pk_objs = new ArrayList();
				IdBdcontrastVO contrast = BdContrastCache.getNewInstance().getVOByField(busivos[0].getBusiSys(), fromItem);
				for (IAccessableBusiVO busivo : busivos) {
					String pk = busivo.getAttributesValue(fromItem);
					if ((pk != null) && (!item_pk_objs.contains(pk)))
						item_pk_objs.add(pk);
					if (UFBoolean.valueOf(ctlLevel).booleanValue()) {
						List<IBDData> bddata = BDAccessorCache.getInstance().getFatherDocs(contrast.getPk_bdinfo(), busivo.getPKOrg(), pk);
						for (IBDData data : bddata) {
							if (!item_pk_objs.contains(data.getPk())) {
								item_pk_objs.add(data.getPk());
							}
						}
					}
				}
				isLackItem = item_pk_objs.size() == 0;
				if (isLackItem)
					break;
				if (contrast.getPk_bdinfo().equals("23a89307-5992-460e-95dd-c628c85f7f95")) {
					List<String> temp_item_pk = new ArrayList();
					for (String s : item_pk_objs) {
						String realPk = getAccountVOByPK_Accasoa(s);
						temp_item_pk.add(realPk);
					}
					itemPkList.add(temp_item_pk.toArray(new String[temp_item_pk.size()]));
				} else {
					itemPkList.add(item_pk_objs.toArray(new String[item_pk_objs.size()]));
				}
			}
			if (!isLackItem) {
				String fromItemMixed = StringUtil.getUnionStr(fromitems, ":", "") + ":";
				if ((i != 0) && (sql.length() != 1))
					sql.append(" or ");
				sql.append("(");
				sql.append("fromitems = '").append(fromItemMixed).append("'");
				sql.append(" and ");
				List<String> pkGroup = getPkMixedGroup(itemPkList);
				if (pkGroup.size() < 800) {
					sql.append("stridx in (");
					for (int j = 0; j < pkGroup.size(); j++) {
						sql.append("'").append((String) pkGroup.get(j)).append("'");
						if (j != pkGroup.size() - 1)
							sql.append(",");
					}
					sql.append(")");
				} else {
					String tempTableName = "tb_" + busivos[0].getBusiSys() + "_" + tableIndex;
					if (tempTableName.length() > 29)
						tempTableName = tempTableName.substring(0, 29);
					String tableName = nc.ms.tb.rule.CtlSchemeServiceGetter.getICtlScheme().createTempTable(tempTableName, pkGroup);
					sql.append("stridx in (select pk from ").append(tableName).append(")");
					tableIndex++;
				}
				sql.append(")");
			}
		}
		sql.append(")");
		return sql.toString();
	}

	public static List<String> getPkMixedGroup(List<String[]> itemPkList) {
		int count = 1;
		for (String[] arr : itemPkList) {
			count *= arr.length;
		}
		List<String> pkGroup = new ArrayList(count);
		String[] pks = (String[]) itemPkList.get(0);
		List<String> nextPks = null;
		if (itemPkList.size() > 1)
			nextPks = getPkMixedGroup(itemPkList.subList(1, itemPkList.size()));
		for (String pk : pks) {
			if (nextPks == null) {
				pkGroup.add(pk + ":");
			} else {
				for (String nextPk : nextPks) {
					pkGroup.add(pk + ":" + nextPk);
				}
			}
		}
		return pkGroup;
	}

	public static IdCtrlschemeVO[] filterScheme(IAccessableBusiVO[] busivos) throws BusinessException {
		IdCtrlschemeVO[] returnvo = null;
		SubLevelOrgGetter orgGet = new SubLevelOrgGetter();
		try {
			HashMap<String, String> map = new HashMap();
			String sysid = null;
			if (busivos.length > 0) {
				sysid = busivos[0].getBusiSys();
			}
			HashSet<String> set = new HashSet();
			IOrgUnitPubService_C orgUnitService = (IOrgUnitPubService_C) NCLocator.getInstance().lookup(IOrgUnitPubService_C.class);
			HashMap<String, ArrayList<String>> uplevelPk = new HashMap();
			for (IAccessableBusiVO vo : busivos) {
				if (OutEnum.GLSYS.equals(sysid)) {
					String pk_org = vo.getAttributesValue("VOUCHER55");
					if ((pk_org != null) && (!"".equals(pk_org))) {
						set.add(pk_org);
					}
				} else if (!OutEnum.MPPSYS.equals(sysid)) {
					String billtype = vo.getBillType();
					ControlBillType bType = null;
					if (sysid.equals(OutEnum.FIBILLSYS)) {
						bType = BillTypeBySysCache.getInstance().getBillTypeById(sysid, vo.getPKGroup(), billtype);
					} else {
						bType = BillTypeBySysCache.getInstance().getBillTypeById(sysid, billtype);
					}
					ArrayList<String> pk_orgs = bType.getPk_orgs();
					for (String pk_bdcontrast : pk_orgs) {
						IdBdcontrastVO bdvo = BdContrastCache.getNewInstance().getVoByPK(pk_bdcontrast);
						OrgTypeVO typevo = OrgTypeManager.getInstance().getOrgTypeByMdclassID(bdvo.getPk_bdinfo());
						String pk_org = vo.getAttributesValue(bdvo.getAtt_fld());
						if ((pk_org != null) && (!"".equals(pk_org))) {
							set.add(pk_org);
							if (!"2ee58f9b-781b-469f-b1d8-1816842515c3".equals(bdvo.getPk_bdinfo())) {
								List<IBDData> bddata =
										BDAccessorCache.getInstance().getFatherDocs("985be8a4-3a36-4778-8afe-2d8ed3902659", pk_org, pk_org);
								if (uplevelPk.get(typevo.getFieldname()) != null) {
									for (IBDData data : bddata) {
										((ArrayList) uplevelPk.get(typevo.getFieldname())).add(data.getPk());
									}
								} else {
									ArrayList<String> levelpk = new ArrayList();
									for (IBDData data : bddata) {
										levelpk.add(data.getPk());
									}
									uplevelPk.put(typevo.getFieldname(), levelpk);
								}
							}
						}
					}
				}
			}
			Iterator iter = uplevelPk.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String orgTypeFieldName = (String) entry.getKey();
				ArrayList<String> orgs = (ArrayList) entry.getValue();
				OrgVO[] orgvos =
						orgUnitService.getOrgs((String[]) orgs.toArray(new String[0]), new String[] { "pk_org", orgTypeFieldName });
				if ((orgvos != null) && (orgvos.length > 0)) {
					for (OrgVO vo : orgvos) {
						if (vo.getAttributeValue(orgTypeFieldName).equals(UFBoolean.TRUE)) {
							set.add(vo.getPk_org());
						}
					}
				}
			}
			StringBuffer sOrgWhere = new StringBuffer(" pk_org in (");
			int count = 0;
			for (String s : set) {
				sOrgWhere.append("'").append(s).append("'");
				if (count != set.size() - 1) {
					sOrgWhere.append(",");
				}
				count++;
			}
			sOrgWhere.append(")");
			HashMap<String, IBusiSysReg> sysregMap = new HashMap();
			for (int i = 0; i < busivos.length; i++) {
				StringBuffer bufferWhere = new StringBuffer();
				bufferWhere.append("(");
				bufferWhere.append(" ctrlsys='");
				bufferWhere.append(busivos[i].getBusiSys());
				bufferWhere.append("' and ");
				StringBuffer billTypeCondition = new StringBuffer();
				String billType = busivos[i].getBillType();
				billTypeCondition.append("(");
				if ((billType == null) || (billType.length() == 0)) {
					billTypeCondition.append(nc.vo.tb.util.VoConvertor.getIsNullSql("billtype") + " or rtrim(billtype) = ''");
				} else {
					billTypeCondition.append("(billtype like '%" + billType + "%')");
					ControlBillType billTypeInfo =
							sysid.equals(OutEnum.FIBILLSYS) ? BillTypeBySysCache.getInstance().getBillTypeById(sysid, busivos[i].getPKGroup(), billType) : BillTypeBySysCache.getInstance().getBillTypeById(sysid, billType);
					if ((billTypeInfo != null) && (billTypeInfo.isControlByParentBill())) {
						BilltypeVO billtypevo = PfDataCache.getBillType(billType);
						billTypeCondition.append(" or ");
						billTypeCondition.append("(billtype like '%" + billtypevo.getParentbilltype() + "%')");
					}
				}
				billTypeCondition.append(")");
				bufferWhere.append(billTypeCondition);
				IdSysregVO selectedSysVO = getSelectSystem(busivos[i].getBusiSys());
				IBusiSysReg m_sysReg = null;
				if (sysregMap.get(selectedSysVO.getSysid()) != null) {
					m_sysReg = (IBusiSysReg) sysregMap.get(selectedSysVO.getSysid());
				} else {
					m_sysReg = (IBusiSysReg) Class.forName(selectedSysVO.getRegclass()).newInstance();
					sysregMap.put(selectedSysVO.getSysid(), m_sysReg);
				}
				if ((m_sysReg instanceof IDateType)) {
					boolean isDateEmpty = true;
					String[] datatype = ((IDateType) m_sysReg).getDataType();
					bufferWhere.append(" and (");
					for (int j = 0; j < datatype.length; j++) {
						String temp = busivos[i].getAttributesValue(datatype[j]);
						if ((temp != null) && (!"".equals(temp))) {
							isDateEmpty = false;
							bufferWhere.append(" startdate <= '" + temp + "' and enddate >= '" + temp + "'");
							bufferWhere.append(" or ");
						}
					}
					if (isDateEmpty) {
						bufferWhere.setLength(bufferWhere.length() - 6);
					} else {
						bufferWhere.setLength(bufferWhere.length() - 4);
						bufferWhere.append(")");
					}
				} else {
					String[] occrDates = busivos[i].getBusiDate().split(";");
					if (occrDates.length > 1) {
						String occrDateStart = occrDates[0];
						String occrDateEnd = occrDates[1];
						bufferWhere.append(" and (startdate<='" + occrDateStart + "' and enddate>='" + occrDateEnd + "')");
					} else {
						String occrDate = occrDates[0];
						bufferWhere.append(" and (startdate<='" + occrDate + "' and enddate>='" + occrDate + "')");
					}
				}
				bufferWhere.append(" and isstarted = 'Y'");
				bufferWhere.append(")");
				map.put(bufferWhere.toString(), "");
			}
			String[] strWheres = (String[]) map.keySet().toArray(new String[0]);
			StringBuffer lastBuffer = new StringBuffer();
			for (int i = 0; i < strWheres.length; i++) {
				if (lastBuffer.length() == 0) {
					lastBuffer.append("(");
					lastBuffer.append(strWheres[i]);
				} else {
					lastBuffer.append(" or ");
					lastBuffer.append(strWheres[i]);
				}
			}
			if (strWheres.length > 0)
				lastBuffer.append(")");
			if (!OutEnum.MPPSYS.equals(sysid)) {
				lastBuffer.append(" and ").append(sOrgWhere.toString());
			}
			StringBuffer countSql = new StringBuffer();
			countSql.append("select count(1) from ").append(new IdCtrlschemeVO().getTableName()).append(" where ").append(lastBuffer);
			Collection res = NtbSuperServiceGetter.getINtbSuper().execSql(countSql.toString());
			int qry_result = 0;
			if (res.size() > 0) {
				for (Object obj : res) {
					Object[] objs = (Object[]) obj;
					if (objs.length > 0) {
						qry_result = Integer.parseInt(String.valueOf(objs[0]));
					}
				}
			}
			if (qry_result == 0) {
				return new IdCtrlschemeVO[0];
			}
			if (qry_result > 1000) {
				String fromitemWhereSql = getFromItemWhereSQL(busivos);
				if (fromitemWhereSql.length() < 3) {
					return new IdCtrlschemeVO[0];
				}
				lastBuffer.append(" and ").append(fromitemWhereSql);
			}
			returnvo =
					(IdCtrlschemeVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlschemeVO.class, lastBuffer.toString());
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage());
		}
		return returnvo;
	}

	public static IdCtrlschemeVO[] findCtrlScheme(IdCtrlschemeVO[] vos, boolean isZeroRule) {
		if (vos == null) {
			return new IdCtrlschemeVO[0];
		}
		ArrayList<IdCtrlschemeVO> templist = new ArrayList();
		for (int i = 0; i < vos.length; i++) {
			if (((vos[i].getSchemetype().equals("TBRULE000SCHEMA_ZERO")) && (isZeroRule)) || ((!vos[i].getSchemetype().equals("TBRULE000SCHEMA_ZERO")) && (!isZeroRule))) {
				templist.add(vos[i]);
			}
		}
		return (IdCtrlschemeVO[]) templist.toArray(new IdCtrlschemeVO[0]);
	}

	public static Object[] findCtrlScheme(Map allSchemeMap) {
		Map<IAccessableBusiVO, List<IdCtrlschemeVO>> accessSchmMap = new HashMap();
		Iterator iter = allSchemeMap.keySet().iterator();
		while (iter.hasNext()) {
			IdCtrlschemeVO keyVO = (IdCtrlschemeVO) iter.next();
			IAccessableBusiVO[] values = (IAccessableBusiVO[]) allSchemeMap.get(keyVO);
			for (IAccessableBusiVO busiVO : values) {
				if (accessSchmMap.containsKey(busiVO)) {
					((List) accessSchmMap.get(busiVO)).add(keyVO);
				} else {
					List<IdCtrlschemeVO> schemeList = new ArrayList();
					schemeList.add(keyVO);
					accessSchmMap.put(busiVO, schemeList);
				}
			}
		}
		boolean isControledByZero;
		boolean isControledByOthers;
		IAccessableBusiVO busiVO;
		Map<IdCtrlschemeVO, List<IAccessableBusiVO>> zeroSchmMap = new HashMap();
		Map<IdCtrlschemeVO, List<IAccessableBusiVO>> commonSchmMap = new HashMap();
		for (Map.Entry<IAccessableBusiVO, List<IdCtrlschemeVO>> entry : accessSchmMap.entrySet()) {
			isControledByZero = false;
			isControledByOthers = false;
			busiVO = (IAccessableBusiVO) entry.getKey();
			List<IdCtrlschemeVO> schmList = (List) entry.getValue();
			for (IdCtrlschemeVO schmVO : schmList) {
				if (schmVO.getSchemetype().equals("TBRULE000SCHEMA_ZERO")) {
					isControledByZero = true;
				} else
					isControledByOthers = true;
			}
			isControledByZero = !isControledByOthers;
			for (IdCtrlschemeVO schmVO : schmList)
				if ((isControledByOthers) && (!schmVO.getSchemetype().equals("TBRULE000SCHEMA_ZERO"))) {
					if (commonSchmMap.containsKey(schmVO)) {
						((List) commonSchmMap.get(schmVO)).add(busiVO);
					} else {
						List<IAccessableBusiVO> busiVOList = new ArrayList();
						busiVOList.add(busiVO);
						commonSchmMap.put(schmVO, busiVOList);
					}
				} else if (isControledByZero)
					if (zeroSchmMap.containsKey(schmVO)) {
						((List) zeroSchmMap.get(schmVO)).add(busiVO);
					} else {
						List<IAccessableBusiVO> busiVOList = new ArrayList();
						busiVOList.add(busiVO);
						zeroSchmMap.put(schmVO, busiVOList);
					}
		}
		Map<IdCtrlschemeVO, IAccessableBusiVO[]> zeroBusiArrMap = new HashMap();
		Map<IdCtrlschemeVO, IAccessableBusiVO[]> commonBusiArrMap = new HashMap();
		for (Map.Entry<IdCtrlschemeVO, List<IAccessableBusiVO>> entry : zeroSchmMap.entrySet())
			zeroBusiArrMap.put(entry.getKey(), ((List<IAccessableBusiVO>) entry.getValue()).toArray(new IAccessableBusiVO[((List) entry.getValue()).size()]));
		for (Map.Entry<IdCtrlschemeVO, List<IAccessableBusiVO>> entry : commonSchmMap.entrySet()) {
			commonBusiArrMap.put(entry.getKey(), ((List<IAccessableBusiVO>) entry.getValue()).toArray(new IAccessableBusiVO[((List) entry.getValue()).size()]));
		}
		Object[] mapArr = new Object[2];
		mapArr[0] = commonBusiArrMap;
		mapArr[1] = zeroBusiArrMap;
		return mapArr;
	}

	public static HashMap getEffectVOs(IdCtrlschemeVO[] schemes, IAccessableBusiVO[] busivo, boolean isZeroRule, boolean isLinkQuery)
			throws BusinessException {
		HashMap<IdCtrlschemeVO, IAccessableBusiVO[]> map = new HashMap();
		try {
			HashMap hashLevelKeys = new HashMap();
			refreshNtbAccountCache(busivo);
			for (int i = 0; i < schemes.length; i++) {
				ArrayList<IAccessableBusiVO> list = new ArrayList();
				String strFields = schemes[i].getFromitems();
				String[] arrFields = strFields.split(":");
				String strPks = schemes[i].getStridx();
				String[] arrPks = strPks.split(":");
				String strcode = schemes[i].getCodeidx();
				String[] arrBaseCodes = strcode.split(":");
				String sysid = schemes[i].getCtrlsys();
				String strtype = getBdinfoType(strFields, sysid);
				String[] arrBaseTypes = strtype.split(":");
				String strlevels = schemes[i].getCtllevels();
				String[] arrlevels = strlevels.split(":");
				HashMap<String, String> maplevelfields = new HashMap();
				for (int count = 0; count < arrlevels.length; count++) {
					if (arrlevels[count].equals("Y")) {
						maplevelfields.put(arrFields[count], null);
					}
				}
				String currfield = getCurr_FldByVO(schemes[i]);
				String orgfield = getOrg_FldByVO(schemes[i]);
				for (int j = 0; j < busivo.length; j++) {
					if (isContrastFix(schemes[i], busivo[j], isZeroRule)) {
						boolean iscontrast = false;
						iscontrast =
								isContrast(schemes[i], busivo[j], hashLevelKeys, arrFields, maplevelfields, currfield, orgfield, isLinkQuery);
						if (iscontrast) {
							list.add(busivo[j]);
						}
					}
				}
				if (list.size() > 0) {
					map.put(schemes[i], (IAccessableBusiVO[]) list.toArray(new IAccessableBusiVO[0]));
				}
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex);
		} finally {
			NtbAccountCacheManager.getNewInstance().clear();
		}
		return map;
	}

	public static String getOrg_FldByVO(IdCtrlschemeVO vo) {
		String[] fromItem = vo.getFromitems().split(":");
		String[] pkIdx = vo.getStridx().split(":");
		for (int i = 0; i < pkIdx.length; i++) {
			if (pkIdx[i].equals(vo.getPk_org())) {
				return fromItem[i];
			}
		}
		try {
			ArrayList<IdBdcontrastVO> mainOrgs = CtlSchemeCTL.getMainOrgBdcontrastVO(vo.getCtrlsys(), vo.getBilltype(), false);
			ArrayList<String> orgAttFld = new ArrayList();
			for (IdBdcontrastVO bdVO : mainOrgs)
				orgAttFld.add(bdVO.getAtt_fld());
			for (String item : fromItem) {
				if (orgAttFld.contains(item))
					return item;
			}
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}
		return null;
	}

	public static String getCurr_FldByVO(IdCtrlschemeVO vo) {
		String billtype = vo.getBilltype();
		String sysid = vo.getCtrlsys();
		String currfield = null;
		try {
			if (OutEnum.GLSYS.equals(sysid)) {
				currfield = BdContrastCache.getNewInstance().getCurrfieldSysid(sysid);
			} else {
				IdBdcontrastVO[] bdvos = BdContrastCache.getNewInstance().getVoBySysAndBill(sysid, billtype);
				for (int k = 0; k < bdvos.length; k++) {
					IdBdcontrastVO bdvo = bdvos[k];
					String pk_bdinfo = bdvo.getPk_bdinfo();
					if ("b498bc9a-e5fd-4613-8da8-bdae2a05704a".equals(pk_bdinfo)) {
						currfield = bdvo.getAtt_fld();
					}
				}
			}
		} catch (Exception ex) {
			NtbLogger.print(ex);
		}
		return currfield;
	}

	public static boolean isContrast(IdCtrlschemeVO parentvo, IAccessableBusiVO busivo, HashMap hashLevelKeys, String[] arrFields, HashMap hashLevelFields, String currfield, String corpfield, boolean isLinkQuery)
			throws Exception {
		try {
			String strParStrIdx = parentvo.getStridx();
			boolean _isContrast = true;
			for (int i = 0; i < arrFields.length; i++) {
				String method = parentvo.getMethodname();
				if (arrFields[i].equals(corpfield)) {
					if ((!isLinkQuery) || ((!OutEnum.FTSSYS.equals(parentvo.getCtrlsys())) && (!OutEnum.SFSYS.equals(parentvo.getCtrlsys())))) {
						if ((busivo.getDataType() != null) && (!method.equals(busivo.getDataType()))) {
							_isContrast = false;
						}
						if ((busivo.getDataType() == null) && (!method.equals("UFIND"))) {
							_isContrast = false;
						}
					}
					String tempPk = getOrgFromIAccBusiVO(busivo, corpfield);
					if (tempPk == null) {
						_isContrast = false;
					} else if (!parentvo.getPk_org().equals(tempPk)) {
						List<IBDData> bddata =
								BDAccessorCache.getInstance().getFatherDocs("985be8a4-3a36-4778-8afe-2d8ed3902659", tempPk, tempPk);
						List<String> tmppks = new ArrayList();
						for (IBDData data : bddata) {
							tmppks.add(data.getPk());
						}
						if ((hashLevelFields.containsKey(arrFields[i])) && (tmppks.contains(parentvo.getPk_org()))) {
							_isContrast = true;
						} else {
							_isContrast = false;
						}
					}
					if (((parentvo.getCtrlsys().equals(OutEnum.ERMSYS)) && (corpfield.equals("pk_project"))) || ((parentvo.getCtrlsys().equals(OutEnum.FIBILLSYS)) && (corpfield.equals("arap_b_pk_project")))) {
						_isContrast = parentvo.getPk_org().equals(busivo.getAttributesValue(corpfield));
					}
				} else {
					boolean isContrast = false;
					if (hashLevelFields.containsKey(arrFields[i])) {
						String tempPk = busivo.getAttributesValue(arrFields[i]);
						IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVOByField(parentvo.getCtrlsys(), arrFields[i]);
						if (vo.getPk_bdinfo().equals("23a89307-5992-460e-95dd-c628c85f7f95")) {
							String voPk = getAccountVOByPK_Accasoa(tempPk);
							if (voPk != null) {
								String[] stridx = strParStrIdx.split(":");
								String pk_account = voPk;
								String _pk_account = stridx[i];
								if (_pk_account.equals(pk_account)) {
									isContrast = true;
								} else {
									try {
										IGeneralAccessor accesssor =
												GeneralAccessorFactory.getAccessor("23a89307-5992-460e-95dd-c628c85f7f95");
										List<IBDData> bddata =
												BDAccessorCache.getInstance().getFatherDocs("23a89307-5992-460e-95dd-c628c85f7f95", parentvo.getPk_org(), tempPk);
										for (int n = 0; n < (bddata == null ? 0 : bddata.size()); n++) {
											String _tmp = ((IBDData) bddata.get(n)).getPk();
											String pk_Vo = getAccountVOByPK_Accasoa(_tmp);
											if (_pk_account.equals(pk_Vo)) {
												isContrast = true;
											}
										}
									} catch (Exception ex) {
										NtbLogger.error(ex);
									}
								}
							}
						} else if (tempPk != null) {
							String[] stridx = strParStrIdx.split(":");
							if (stridx[i].indexOf(tempPk) >= 0) {
								isContrast = true;
							} else {
								try {
									List<IBDData> bddata =
											BDAccessorCache.getInstance().getFatherDocs(vo.getPk_bdinfo(), parentvo.getPk_org(), tempPk);
									for (int n = 0; n < (bddata == null ? 0 : bddata.size()); n++) {
										String _tmp = ((IBDData) bddata.get(n)).getPk();
										if (_tmp.equals(stridx[i])) {
											isContrast = true;
										}
									}
								} catch (Exception ex) {
									NtbLogger.error(ex);
								}
							}
						} else {
							isContrast = false;
						}
					} else {
						String temPk = busivo.getAttributesValue(arrFields[i]);
						IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVOByField(parentvo.getCtrlsys(), arrFields[i]);
						if (vo.getPk_bdinfo().equals("23a89307-5992-460e-95dd-c628c85f7f95")) {
							String voPk = getAccountVOByPK_Accasoa(temPk);
							if (voPk != null) {
								temPk = voPk;
							}
						}
						String[] stridx = strParStrIdx.split(":");
						if ((temPk != null) && (temPk.length() != 0) && (stridx[i].indexOf(temPk) >= 0)) {
							isContrast = true;
						} else if ((temPk != null) && (temPk.length() != 0) && (arrFields[i].equals(currfield))) {
							int currtype = parentvo.getCurrtype().intValue();
							String methodcode = parentvo.getMethodname();
							String ctrldirction = parentvo.getCtrldirection();
							String ctrlobj = parentvo.getCtrlobj();
							String ctrlobjExt = parentvo.getCtrlobjValue();
							String pk_currency =
									CtlSchemeCTL.getPk_currency(parentvo.getPk_currency(), parentvo.getPk_org(), parentvo.getCtrlsys());
							UFDouble value = null;
							if (methodcode.equals(busivo.getDataType() == null ? "UFIND" : busivo.getDataType())) {
								UFDouble[] tmpValue = null;
								if ((busivo instanceof IAccessableExtBusiVO)) {
									tmpValue =
											getUFDouble(((IAccessableExtBusiVO) busivo).getExeData(ctrldirction, ctrlobj, ctrlobjExt, currtype, pk_currency));
								} else {
									tmpValue = busivo.getExeData(ctrldirction, ctrlobj, ctrlobjExt);
								}
								value = tmpValue == null ? UFDouble.ZERO_DBL : tmpValue[currtype];
							} else {
								value = UFDouble.ZERO_DBL;
							}
							if ((value != null) && (value.getDouble() != 0.0D) && ((parentvo.getCurrtype().intValue() == 2) || (parentvo.getCurrtype().intValue() == 1) || (parentvo.getCurrtype().intValue() == 0))) {
								isContrast = true;
							}
						}
					}
					if ((!isLinkQuery) || ((!OutEnum.FTSSYS.equals(parentvo.getCtrlsys())) && (!OutEnum.SFSYS.equals(parentvo.getCtrlsys())))) {
						if ((busivo.getDataType() != null) && (!method.equals(busivo.getDataType()))) {
							isContrast = false;
						}
						if ((busivo.getDataType() == null) && (!method.equals("UFIND"))) {
							isContrast = false;
						}
					}
					if (OutEnum.GLSYS.equals(parentvo.getCtrlsys())) {
						if (!parentvo.getCtrldirection().equals(busivo.getAttributesValue("direction"))) {
							isContrast = false;
						}
						if ((parentvo.getCtrlobjValue() != null) && (!"null".equals(parentvo.getCtrlobjValue())) && (!parentvo.getCtrlobjValue().equals(busivo.getAttributesValue("SOURCESYSTEM")))) {
							isContrast = false;
						}
					}
					if (!isContrast) {
						return false;
					}
				}
			}
			return _isContrast;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	public static String getAccountVOByPK_Accasoa(String pk_accasoa) throws BusinessException {
		String pk_account = NtbAccountCacheManager.getNewInstance().getAccountByAsoa(pk_accasoa);
		return pk_account;
	}

	public static nc.vo.bd.accsystem.AccSystemVO[] getAllSystem() throws BusinessException {
		IAccSystemQryService queryService = (IAccSystemQryService) NCLocator.getInstance().lookup(IAccSystemQryService.class);
		return queryService.queryAccSystemVOs();
	}

	public static ArrayList<String> getAllAccountVOByPKAccount(String name_account) throws BusinessException {
		IDimManager dm = nc.ms.mdm.dim.DimServiceGetter.getDimManager();
		DimDef dimdef = dm.getDimDefByPK("TB_DIMDEF_MEASURE_00");
		ArrayList<String> pk_accounts = new ArrayList();
		List<DimHierarchy> dimhiers = dimdef.getHierarchies();
		for (int n = 0; n < dimhiers.size(); n++) {
			DimHierarchy dimhier = (DimHierarchy) dimhiers.get(n);
			List<DimMember> members = dimhier.getMemberReader().getMembers();
			for (int m = 0; m < members.size(); m++) {
				DimMember member = (DimMember) members.get(m);
				if (name_account.equals(member.getObjName())) {
					pk_accounts.add((String) member.getLevelValue().getKey());
				}
			}
		}
		return pk_accounts;
	}

	public static String getUAPDataStr(String year, String month, String day) {
		if ((year == null) || (year.length() != 4))
			return "9999-99-99";
		StringBuffer sb = new StringBuffer();
		sb.append(year).append("-");
		if (month == null) {
			sb.append("01");
		} else if (month.length() == 1) {
			sb.append("0").append(month);
		} else
			sb.append(month);
		sb.append("-");
		if (day == null) {
			sb.append("01");
		} else if (day.length() == 1) {
			sb.append("0").append(day);
		} else
			sb.append(day);
		sb.append("-");
		return sb.toString();
	}

	public static boolean isInclude(String str1, String str2) {
		if ((str1 == null) || (str1.length() == 0)) {
			if ((str2 == null) || (str2.length() == 0)) {
				return true;
			}
			return false;
		}
		if ((str2 == null) || (str2.length() == 0)) {
			return false;
		}
		if (str1.indexOf(str2) >= 0) {
			return true;
		}
		return false;
	}

	private static boolean isCheckBillType(IdCtrlschemeVO scheme, IAccessableBusiVO busivo) throws Exception {
		if (!isInclude(scheme.getBilltype(), busivo.getBillType())) {
			return false;
		}
		return true;
	}

	private static boolean isBillTypeMatched(String schemeBillType, String busiBillType) {
		if (((schemeBillType == null) || (schemeBillType.length() == 0)) && ((busiBillType == null) || (busiBillType.length() == 0))) {
			return true;
		}
		if (schemeBillType.indexOf("[") >= 0) {
			int index1;
			while ((index1 = schemeBillType.indexOf("[")) >= 0) {
				int index2 = schemeBillType.indexOf("]");
				if (index2 < 0)
					break;
				String billType = schemeBillType.substring(index1 + 1, index2);
				schemeBillType = schemeBillType.substring(index2 + 1, schemeBillType.length());
				if ((billType != null) && (billType.equals(busiBillType)))
					return true;
			}
		}
		return isInclude(schemeBillType, busiBillType);
	}

	private static boolean isContrastFix(IdCtrlschemeVO scheme, IAccessableBusiVO busivo, boolean isZeroRule) throws Exception {
		if (!isBillTypeMatched(scheme.getBilltype(), busivo.getBillType())) {
			ControlBillType currType = BillTypeBySysCache.getInstance().getBillTypeById(busivo.getBusiSys(), busivo.getBillType());
			if ((currType != null) && (currType.isControlByParentBill())) {
				String typeStr = CtlSchemeCTL.parseBillTypes(scheme.getBilltype());
				if ((typeStr == null) || (!currType.getParentBillType().equals(typeStr)))
					return false;
			} else {
				return false;
			}
		}
		boolean isDateType = false;
		IdSysregVO selectedSysVO = getSelectSystem(busivo.getBusiSys());
		IBusiSysReg m_sysReg = (IBusiSysReg) Class.forName(selectedSysVO.getRegclass()).newInstance();
		if ((m_sysReg instanceof IDateType)) {
			isDateType = true;
		}
		if (isDateType) {
			String actualdate = busivo.getAttributesValue(scheme.getDatetype());
			if ((actualdate != null) && (!"".equals(actualdate))) {
				if ((actualdate.compareTo(scheme.getStartdate()) < 0) || (actualdate.compareTo(scheme.getEnddate()) > 0)) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			String actualdate = busivo.getBusiDate();
			if ((actualdate != null) && (!"".equals(actualdate)) && ((actualdate.compareTo(scheme.getStartdate()) < 0) || (actualdate.compareTo(scheme.getEnddate()) > 0))) {
				return false;
			}
		}
		return true;
	}

	public static IdCtrlschemeVO[] filterCtrlByEntity(IdCtrlschemeVO[] schemes, IAccessableBusiVO[] busivo) throws BusinessException {
		HashMap<String, IdCtrlschemeVO> map = new HashMap();
		for (int n = 0; n < schemes.length; n++) {
			String orgfield = getOrg_FldByVO(schemes[n]);
			String strPks = schemes[n].getStridx();
			String[] arrPks = strPks.split(":");
			String strFields = schemes[n].getFromitems();
			String[] arrFields = strFields.split(":");
			String strlevels = schemes[n].getCtllevels();
			String[] arrlevels = strlevels.split(":");
			HashMap<String, String> maplevelfields = new HashMap();
			for (int count = 0; count < arrlevels.length; count++) {
				if (arrlevels[count].equals("Y")) {
					maplevelfields.put(arrFields[count], null);
				}
			}
			for (int k = 0; k < busivo.length; k++) {
				if (map.get(schemes[n].getPrimaryKey()) == null) {
					boolean _isContrast = true;
					for (int i = 0; i < arrFields.length; i++) {
						String method = schemes[n].getMethodname();
						if (arrFields[i].equals(orgfield)) {
							String tempPk = getOrgFromIAccBusiVO(busivo[k], orgfield);
							if (tempPk == null) {
								_isContrast = false;
							} else if (!schemes[n].getPk_org().equals(tempPk)) {
								if (maplevelfields.containsKey(arrFields[i])) {
									SubLevelOrgGetter orgGetter = new SubLevelOrgGetter();
									if (orgGetter.isUpLevelOrgIsBeControlled(schemes[n].getPk_org(), tempPk, arrFields[i], schemes[n].getCtrlsys())) {
										_isContrast = true;
									} else
										_isContrast = false;
								} else {
									_isContrast = false;
								}
							}
						}
					}
					if (_isContrast) {
						map.put(schemes[n].getPrimaryKey(), schemes[n]);
					}
				}
			}
		}
		ArrayList<IdCtrlschemeVO> vos = new ArrayList();
		Iterator iter = map.values().iterator();
		while (iter.hasNext()) {
			vos.add((IdCtrlschemeVO) iter.next());
		}
		return (IdCtrlschemeVO[]) vos.toArray(new IdCtrlschemeVO[0]);
	}

	public static Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> getSepRelationScheme(List<IdCtrlformulaVO> fatherList, List<IdCtrlschemeVO> schemeList)
			throws BusinessException {
		Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap = new HashMap();
		for (IdCtrlformulaVO faVO : fatherList) {
			if (faVO.getSchemetype().equals("TBRULEVELSCHEMA_SPEC")) {
				List<String> item = new ArrayList();
				List<String> levelflag = new ArrayList();
				List<String> pkidx = new ArrayList();
				List<String> sysid = new ArrayList();
				IdCtrlschemeVO schemeVO = null;
				for (IdCtrlschemeVO vo1 : schemeList) {
					if (vo1.getPk_ctrlformula().equals(faVO.getPrimaryKey())) {
						item.add(vo1.getFromitems());
						levelflag.add(vo1.getCtllevels());
						pkidx.add(vo1.getStridx());
						sysid.add(vo1.getCtrlsys());
						schemeVO = vo1;
					}
				}
				if (item.size() > 0) {
					for (int i = 0; i < item.size(); i++) {
						String[] levels = ((String) levelflag.get(i)).split(":");
						String[] items = ((String) item.get(i)).split(":");
						String[] pksidx = ((String) pkidx.get(i)).split(":");
						Map<String, List<String>> map = new HashMap();
						for (int n = 0; n < levels.length; n++) {
							if (UFBoolean.valueOf(levels[n]).booleanValue()) {
								String _item = items[n];
								IdBdcontrastVO _vo = BdContrastCache.getNewInstance().getVOByField((String) sysid.get(i), _item);
								IGeneralAccessor accesssor = GeneralAccessorFactory.getAccessor(_vo.getPk_bdinfo());
								List<IBDData> bddata = accesssor.getChildDocs(schemeVO.getPk_org(), pksidx[n], false);
								List<String> list = new ArrayList();
								if ((bddata != null) && (bddata.size() > 0)) {
									for (IBDData data : bddata) {
										String pk_parent = data.getParentPk();
										if (pksidx[n].equals(pk_parent)) {
											list.add(data.getPk());
										}
									}
									map.put(_item, list);
								}
							}
						}
						if (map.size() > 0) {
							List<IdCtrlschemeVO> s_list = new ArrayList();
							String sql =
									" pk_ctrlformula in(select pk_obj from tb_ctrlformula where pk_parent='" + faVO.getPk_parent() + "')";
							NtbSuperDMO dmo = new NtbSuperDMO();
							IdCtrlschemeVO[] vos = (IdCtrlschemeVO[]) dmo.queryByWhereClause(IdCtrlschemeVO.class, sql);
							for (IdCtrlschemeVO _vo : vos) {
								String _item = _vo.getFromitems();
								String _pksidx = _vo.getStridx();
								if (checkItemAndPkValue(_item, _pksidx, map, schemeVO, _vo)) {
									s_list.add(_vo);
								}
							}
							if (s_list.size() > 0) {
								StringBuffer sqlS = new StringBuffer(" pk_obj in (");
								for (int n = 0; n < s_list.size(); n++) {
									String _ctrlformula = ((IdCtrlschemeVO) s_list.get(n)).getPk_ctrlformula();
									if (n != s_list.size() - 1) {
										sqlS.append("'").append(_ctrlformula).append("',");
									} else {
										sqlS.append("'").append(_ctrlformula).append("'");
									}
								}
								sqlS.append(")");
								IdCtrlformulaVO[] _vos = (IdCtrlformulaVO[]) dmo.queryByWhereClause(IdCtrlformulaVO.class, sqlS.toString());
								for (IdCtrlformulaVO fVO : _vos) {
									List<IdCtrlschemeVO> relaSchemes = new ArrayList();
									for (IdCtrlschemeVO sVO : s_list) {
										if (sVO.getPk_ctrlformula().equals(fVO.getPrimaryKey())) {
											relaSchemes.add(sVO);
										}
									}
									relaSchemeMap.put(fVO, relaSchemes);
								}
							}
						}
					}
				}
			}
		}
		return relaSchemeMap;
	}

	public static void reformSepControl(HashMap hashEffect, HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> groupmap, Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap) {
		Iterator itera = hashEffect.entrySet().iterator();
		Map.Entry obj = (Map.Entry) itera.next();
		IdCtrlschemeVO schemeVO = (IdCtrlschemeVO) obj.getKey();
		String pk_ctrlformula = schemeVO.getPk_ctrlformula();
		UFDouble sumDouble = new UFDouble(0);
		try {
			NtbSuperDMO dmo = new NtbSuperDMO();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) dmo.queryByPrimaryKey(IdCtrlformulaVO.class, pk_ctrlformula);
			String pk_formula = vo.getPk_parent();
			DimFormulaVO dimvo = (DimFormulaVO) dmo.queryByPrimaryKey(DimFormulaVO.class, pk_formula);
			String ctrltype = dimvo.getPk_ruleclass();
			List<IdCtrlformulaVO> fVOList = new ArrayList();
			List<IdCtrlschemeVO> sVOList = new ArrayList();
			fVOList.add(vo);
			sVOList.add(schemeVO);
			if (ctrltype.equals("TBRULEVELSCHEMA_SPEC")) {
				Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> schemeMap = getSepRelationScheme(fVOList, sVOList);
				for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : schemeMap.entrySet()) {
					relaSchemeMap.put(entry.getKey(), entry.getValue());
				}
				List<IdCtrlformulaVO> _vos = new ArrayList();
				List<IdCtrlschemeVO> s_list = new ArrayList();
				for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : relaSchemeMap.entrySet()) {
					_vos.add(entry.getKey());
					s_list.addAll((Collection) entry.getValue());
				}
				for (int n = 0; n < _vos.size(); n++) {
					UFDouble planvalue = ((IdCtrlformulaVO) _vos.get(n)).getPlanvalue();
					sumDouble = sumDouble.add(planvalue);
				}
				IdCtrlformulaVO tmpvo = null;
				Iterator _iter = groupmap.entrySet().iterator();
				while (_iter.hasNext()) {
					Map.Entry _obj = (Map.Entry) _iter.next();
					IdCtrlformulaVO _vo = (IdCtrlformulaVO) _obj.getKey();
					if (_vo.getPrimaryKey().equals(vo.getPrimaryKey())) {
						tmpvo = _vo;
						break;
					}
				}
				ArrayList<IdCtrlschemeVO> vosList = (ArrayList) groupmap.get(tmpvo);
				boolean isExist = false;
				for (Iterator i$ = vosList.iterator(); i$.hasNext();) {
					IdCtrlschemeVO schemeVO1 = (IdCtrlschemeVO) i$.next();
					for (IdCtrlschemeVO msvo : s_list) {
						if (schemeVO1.getStridx().equals(msvo.getStridx()))
							isExist = true;
					}
				}
				if (isExist) {
					groupmap.remove(tmpvo);
				} else {
					String express = vo.getExpressformula();
					express =
							String.valueOf(vo.getPlanvalue().add(sumDouble.multiply(-1.0D))) + express.substring(express.indexOf("*"), express.length());
					vo.setExpressformula(express);
					groupmap.put(vo, vosList);
				}
			} else {
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkItemAndPkValue(String item, String pkidx, Map<String, List<String>> map, IdCtrlschemeVO vo, IdCtrlschemeVO _vo) {
		boolean isExist = false;
		Iterator iter = map.entrySet().iterator();
		String _item;
		while (iter.hasNext()) {
			Map.Entry obj = (Map.Entry) iter.next();
			_item = (String) obj.getKey();
			List<String> _value = (List) obj.getValue();
			if (item.indexOf(_item) >= 0) {
				for (String pk : _value) {
					if ((pkidx.indexOf(pk) >= 0) && (vo.getPk_org().equals(_vo.getPk_org())) && (vo.getStartdate().equals(_vo.getStartdate())) && (vo.getEnddate().equals(_vo.getEnddate()))) {
						isExist = true;
						String strIdx = vo.getStridx();
						String _strIdx = _vo.getStridx();
						String itemIdx = vo.getFromitems();
						String _itemIdx = _vo.getFromitems();
						String[] strIdxArr = strIdx.split(":");
						String[] _strIdxArr = _strIdx.split(":");
						String[] itemIdxArr = itemIdx.split(":");
						String[] _itemIdxArr = _itemIdx.split(":");
						for (int i = 0; i < _itemIdxArr.length; i++) {
							if (!_itemIdxArr[i].equals(_item)) {
								for (int j = 0; j < itemIdxArr.length; j++) {
									if ((itemIdxArr[j].equals(_itemIdxArr[i])) && (!strIdxArr[j].equals(_strIdxArr[i]))) {
										isExist = false;
									}
								}
							}
						}
					}
				}
			}
		}
		return isExist;
	}
}