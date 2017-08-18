package nc.ms.tb.control;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.pf.pub.PfDataCache;
import nc.itf.mdm.cube.IDataSetService;
import nc.itf.mdm.dim.IDimManager;
import nc.itf.tb.control.IBusiSysExecDataProvider;
import nc.itf.tb.control.IBusiSysReg;
import nc.itf.tb.control.OutEnum;
import nc.itf.tb.sysmaintain.BdContrastCache;
import nc.itf.tb.sysmaintain.BusiSysReg;
import nc.itf.uap.pa.IPreAlertConfigService;
import nc.ms.mdm.convertor.IStringConvertor;
import nc.ms.mdm.convertor.StringConvertorFactory;
import nc.ms.mdm.cube.CubeServiceGetter;
import nc.ms.mdm.dim.DimServiceGetter;
import nc.ms.mdm.dim.NtbSuperServiceGetter;
import nc.ms.mdm.dim.TimeDimTool;
import nc.ms.tb.adjbill.AdjustBillTaskDataModelCache;
import nc.ms.tb.formula.context.DefaultFormulaContext;
import nc.ms.tb.formula.context.IFormulaContext;
import nc.ms.tb.formula.core.cutcube.WhereDataCellInfo;
import nc.ms.tb.formula.script.Calculator;
import nc.ms.tb.formula.script.core.parser.Expression;
import nc.ms.tb.formula.script.core.parser.TbbLexer;
import nc.ms.tb.formula.script.core.parser.TbbParser;
import nc.ms.tb.formula.util.CountTimeCost;
import nc.ms.tb.pubutil.DateUtil;
import nc.ms.tb.pubutil.UtilServiceGetter;
import nc.ms.tb.rule.CtlSchemeServiceGetter;
import nc.ms.tb.rule.FormulaDimCI;
import nc.ms.tb.rule.NtbContext;
import nc.ms.tb.rule.RuleCacheManager;
import nc.ms.tb.rule.RuleServiceGetter;
import nc.ms.tb.rule.SingleSchema;
import nc.ms.tb.rule.SubLevelOrgGetter;
import nc.ms.tb.rule.fmlset.FormulaCTL;
import nc.ms.tb.rule.fmlset.FormulaParser;
import nc.ms.tb.task.TbTaskCtl;
import nc.ms.tb.task.TbTaskServiceGetter;
import nc.ms.tb.task.data.TaskDataCtl;
import nc.ms.tb.task.data.TaskDataModel;
import nc.pubitf.accperiod.AccountCalendar;
import nc.pubitf.bbd.CurrtypeQuery;
import nc.pubitf.bd.accessor.GeneralAccessorFactory;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.pubitf.org.IAccountingBookPubService;
import nc.pubitf.org.IFinanceOrgPubService;
import nc.pubitf.org.ILiabilityBookPubService;
import nc.pubitf.rbac.IFunctionPermissionPubService;
import nc.pubitf.uapbd.IAccountPubService;
import nc.ui.bank_cvp.formulainterface.RefCompilerClient;
import nc.vo.bank_cvp.compile.datastruct.ArrayValue;
import nc.vo.bd.accessor.IBDData;
import nc.vo.bd.account.AccountVO;
import nc.vo.mdm.cube.CubeDef;
import nc.vo.mdm.cube.DataCell;
import nc.vo.mdm.cube.DataCellValue;
import nc.vo.mdm.cube.DimSectionTuple;
import nc.vo.mdm.cube.DimVector;
import nc.vo.mdm.cube.ICubeDataSet;
import nc.vo.mdm.dim.DimHierarchy;
import nc.vo.mdm.dim.DimLevel;
import nc.vo.mdm.dim.DimMember;
import nc.vo.mdm.dim.LevelValue;
import nc.vo.mdm.pub.NtbLogger;
import nc.vo.mdm.pub.StringUtil;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.org.FinanceOrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.SuperVO;
import nc.vo.pub.billtype.BilltypeVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.tb.control.ControlBillType;
import nc.vo.tb.control.ControlObjectType;
import nc.vo.tb.control.ConvertToCtrlSchemeVO;
import nc.vo.tb.control.CtlAggregatedVO;
import nc.vo.tb.control.CtrlInfoMacroConst;
import nc.vo.tb.control.CtrlSchemeVO;
import nc.vo.tb.control.DataContrastVO;
import nc.vo.tb.control.DimRelUapVO;
import nc.vo.tb.control.IdBdcontrastVO;
import nc.vo.tb.control.IdCtrlschmBVO;
import nc.vo.tb.control.IdCtrlschmVO;
import nc.vo.tb.control.IdFlexAreaTypeEnum;
import nc.vo.tb.control.IdSysregVO;
import nc.vo.tb.form.MdWorkbook;
import nc.vo.tb.formula.DimFormulaMVO;
import nc.vo.tb.formula.DimFormulaVO;
import nc.vo.tb.formula.FormulaDataCell;
import nc.vo.tb.ntbenum.AccumulateEnum;
import nc.vo.tb.obj.NtbParamVO;
import nc.vo.tb.prealarm.IdAlarmDimVectorVO;
import nc.vo.tb.prealarm.IdAlarmschemeVO;
import nc.vo.tb.pubutil.BusiTermConst;
import nc.vo.tb.rule.AllotFormulaVo;
import nc.vo.tb.rule.BusiRuleVO;
import nc.vo.tb.rule.CtrlSpecialUsage;
import nc.vo.tb.rule.IdCtrlInfoVO;
import nc.vo.tb.rule.IdCtrlformulaVO;
import nc.vo.tb.rule.IdCtrlschemeVO;
import nc.vo.tb.rule.IdFlexElementVO;
import nc.vo.tb.rule.IdFlexZoneVO;
import nc.vo.tb.task.MdTask;
import nc.vo.tb.util.VoConvertor;
import nc.vo.uap.rbac.profile.IFunctionPermProfile;

import org.apache.commons.lang.RandomStringUtils;

import com.ufsoft.table.Cell;

public class CtlSchemeCTL {
	private static final String text_seperator = "#";

	public CtlSchemeCTL() {
	}

	private static Map<String, Expression> parserMap = new HashMap();

	public static Map<String, Expression> getParserMap() {
		return parserMap;
	}

	public static String getSysOrgByCode(String sysCode) {
		return BusiTermConst.getSysOrgByCode(sysCode);
	}

	public static String getSysNameByCode(String sysCode) {
		return BusiTermConst.getSysNameByCode(sysCode);
	}

	public static HashMap<String, ArrayList<String>> reloadZeroCtrlScheme() throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().reloadZeroCtrlScheme();
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static IdSysregVO[] getRegVOs() throws Exception {
		return BusiSysReg.getSharedInstance().getAllSysVOs();
	}

	public static String getDimMCode(DataCell datacell) {
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		String pk_cell = cvt.convertToString(datacell.getDimVector());
		return pk_cell;
	}

	public static String getBdinfoType(String fromitem, String sysid) throws BusinessException {
		IdBdcontrastVO[] bdcontrasts = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		StringBuffer buffer = new StringBuffer();
		String[] ss = fromitem.split(":");
		for (int i = 0; i < ss.length; i++) {
			boolean bFind = false;
			for (int j = 0; j < bdcontrasts.length; j++) {
				if (bdcontrasts[j].getAtt_fld().equals(ss[i])) {
					String bdinfotype = bdcontrasts[j].getBdinfo_type();
					buffer.append(bdinfotype + ":");
					bFind = true;
					break;
				}
			}
			if (!bFind) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000261"));
			}
		}

		return buffer.toString();
	}

	public static String getCtlType(String formulaPk) throws Exception {
		DimFormulaVO vo = NtbFormulaCache.getNewInstance().getDimFormulaVOByPk(formulaPk);
		if (vo != null) {
			return vo.getPk_ruleclass();
		}
		return "TBRULE000SCHEMA_FLEX";
	}

	public static SingleSchema zeroSchema(DimFormulaVO d_vo, DimMember entity) throws BusinessException, Exception {
		String srcf = FormulaParser.getNoNameExp(d_vo.getFullcontent());
		SingleSchema schema = new SingleSchema(srcf, "TBRULE000SCHEMA_ZERO");
		DimFormulaMacro macro = new DimFormulaMacro();
		FormulaDimCI m_env = new FormulaDimCI();
		srcf = DimFormulaMacro.getParsedFormula(m_env, srcf, d_vo.getPk_parent());
		schema.instanceSchema(srcf);
		srcf = macro.getZeroComplexParsedCorpAndCurrency(srcf, schema, entity);
		schema.instanceSchema(srcf);
		srcf = macro.getParsedZeroFormula(schema, entity);
		schema.instanceSchema(srcf);
		return schema;
	}

	public static String[] addCtrlScheme(ArrayList<IdCtrlschemeVO> vos) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().addCtrlScheme(vos);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static String[] addCtrlformulas(ArrayList<IdCtrlformulaVO> vos) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().addCtrlformulas(vos);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static void updateCtrlSchemeTable(NtbParamVO[] param) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().updateCtrlSchemeTable(param);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static IBusiSysReg getBusiSysReg(String sys) throws BusinessException {
		BusiSysReg sysreg = BusiSysReg.getSharedInstance();
		IdSysregVO[] sysregvos = sysreg.getAllSysVOs();
		boolean isFind = false;
		IBusiSysReg sysReg = null;
		try {
			for (int i = 0; i < sysregvos.length; i++) {
				if (sysregvos[i].getSysid().equals(sys)) {
					isFind = true;
					sysReg = (IBusiSysReg) Class.forName(sysregvos[i].getRegclass()).newInstance();
				}
			}
		} catch (Exception ex) {
			NtbLogger.print(ex.getMessage());
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000262", null, new String[] { sys }), ex);
		}
		if (!isFind) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000262", null, new String[] { sys }));
		}
		return sysReg;
	}

	public static ArrayList<DimFormulaVO> queryDimFormulas(ArrayList<String> pks) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().queryDimFormulas(pks);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000231", null, new String[] { e.getMessage() }));
		}
	}

	public static ArrayList<IdCtrlformulaVO> queryCtrlFormula(String sWhere) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().queryCtrlFormula(sWhere);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000231", null, new String[] { e.getMessage() }));
		}
	}

	public static void deleteZeroCtrlScheme(ArrayList<String> pks, boolean deleteFormulaVO) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().deleteZeroCtrlScheme(pks, deleteFormulaVO);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	protected static String getTextSeperator() {
		return "#";
	}

	public static String simpleBillTypeDisp(String billtypes, int maxLength) {
		if ((billtypes == null) || ("".equals(billtypes)))
			return null;
		StringBuffer billTypeDisp = new StringBuffer();
		String[] billtypeArr = billtypes.split("#|,");

		for (int i = 0; i < billtypeArr.length; i++) {
			String billtype = billtypeArr[i];
			if (billtype.indexOf("]") > 0) {
				int index = billtype.indexOf("]");
				String billtypeName = billtype.substring(index + 1);

				int length = billTypeDisp.length();
				if (length + billtypeName.length() > maxLength / 2) {
					int lastLen = maxLength / 2 - length - 2;
					if (lastLen < 0) {
						billTypeDisp.replace(billTypeDisp.length() - Math.abs(lastLen), billTypeDisp.length(), "..");
						break;
					}
					String lastBillTypeName = billtypeName.substring(0, lastLen) + "..";
					billTypeDisp.append(lastBillTypeName);

					break;
				}
				billTypeDisp.append(billtypeName);
				if (i != billtypeArr.length - 1) {
					billTypeDisp.append(",");
				}
			}
		}
		return billTypeDisp.toString();
	}

	public static String parseBillTypes(String billtyes) {
		if ((billtyes == null) || (billtyes.trim().length() == 0)) {
			return null;
		}
		StringBuffer buffer = new StringBuffer();

		if (billtyes.indexOf("]") == -1) {
			buffer.append(billtyes);
		}

		if (billtyes.indexOf(getTextSeperator()) >= 0) {
			String[] billtypes = billtyes.split(getTextSeperator());
			for (int n = 0; n < billtypes.length; n++) {
				String tmp_bill = billtypes[n];

				while (tmp_bill.indexOf("]") >= 0) {
					if (buffer.toString().length() == 0) {
						buffer.append(tmp_bill.substring(tmp_bill.indexOf("[") + 1, tmp_bill.indexOf("]")));
					} else {
						buffer.append(getTextSeperator());
						buffer.append(tmp_bill.substring(tmp_bill.indexOf("[") + 1, tmp_bill.indexOf("]")));
					}

					tmp_bill = tmp_bill.substring(tmp_bill.indexOf("]") + 1);
				}
			}
		} else {
			while (billtyes.indexOf("]") >= 0) {
				if (buffer.toString().length() == 0) {
					buffer.append(billtyes.substring(billtyes.indexOf("[") + 1, billtyes.indexOf("]")));
				} else {
					buffer.append(",");
					buffer.append(billtyes.substring(billtyes.indexOf("[") + 1, billtyes.indexOf("]")));
				}

				billtyes = billtyes.substring(billtyes.indexOf("]") + 1);
			}
		}
		return buffer.toString();
	}

	public static ArrayList<IdBdcontrastVO> getMainOrgBdcontrastVO(String sysid, String billtype, boolean isDefault) throws Exception {
		ArrayList<IdBdcontrastVO> vos = new ArrayList();
		IBusiSysReg reg = getBusiSysReg(sysid);

		ArrayList<ControlBillType> billtypeList = BillTypeBySysCache.getInstance().getUfindPanelBySysid(sysid);
		String pkMainOrg = reg.getMainPkOrg();

		if (billtypeList == null) {
			IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVoByPK(pkMainOrg);
			vos.add(vo);
			return vos;
		}
		if ((billtype != null) && (OutEnum.MPPSYS.equals(sysid))) {
			billtype = billtype.split("-")[0];
		}

		for (int n = 0; n < (billtypeList == null ? 0 : billtypeList.size()); n++) {
			ControlBillType billtypeVO = (ControlBillType) billtypeList.get(n);

			String billtypeStr = billtypeVO.getBillType_code();

			if ((billtype != null) && (billtype.indexOf(billtypeStr) >= 0)) {
				ArrayList<String> orgTypeList = billtypeVO.getPk_orgs();
				for (int m = 0; m < (orgTypeList == null ? 0 : orgTypeList.size()); m++) {
					IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVoByPK((String) orgTypeList.get(m));
					vos.add(vo);
				}
				break;
			}
		}

		if ((sysid.equals(OutEnum.ERMSYS)) && (billtype != null)) {
			IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVOByField(sysid, "pk_project");
			vos.add(vo);
		}

		if ((sysid.equals(OutEnum.FIBILLSYS)) && ("BAL".equals(billtype))) {
			IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVOByField(sysid, "arap_b_pk_project");
			vos.add(vo);
		}
		if (isDefault) {
			ArrayList<IdBdcontrastVO> voList = new ArrayList();
			if ((vos != null) && (vos.size() > 0)) {
				voList.add(vos.get(0));
			}
			return voList;
		}
		return vos;
	}

	public static IdBdcontrastVO getBdContrastVOByPk(String pk_contrast) throws BusinessException {
		IdBdcontrastVO m_vo = BdContrastCache.getNewInstance().getVoByPK(pk_contrast);
		return m_vo;
	}

	public static boolean getSysMainBdinfo(String sysid, String m_billtype, String pk_bdcontrast) throws BusinessException {
		String billtypeCode = parseBillTypes(m_billtype);
		IBusiSysReg reg = getBusiSysReg(sysid);
		String m_pk_bdcontrast = null;
		ArrayList<String> orgsList = null;
		if (reg.getBillType() == null) {
			m_pk_bdcontrast = reg.getMainPkOrg();
		} else {
			ArrayList<ControlBillType> billtypeList = reg.getBillType();

			for (int n = 0; n < (billtypeList == null ? 0 : billtypeList.size()); n++) {
				ControlBillType billtype = (ControlBillType) billtypeList.get(n);
				if (billtype.getBillType_code().equals(billtypeCode)) {
					orgsList = billtype.getPk_orgs();
					break;
				}
			}
		}
		if (((orgsList != null) && (orgsList.contains(pk_bdcontrast))) || (pk_bdcontrast.equals(m_pk_bdcontrast))) {
			return true;
		}
		return false;
	}

	public static String getSysMainPKOrg(CtlAggregatedVO m_aggvo) {
		String pk_org = null;
		IdCtrlschmVO vo = (IdCtrlschmVO) m_aggvo.getParentVO();
		IdCtrlschmBVO[] childvos = (IdCtrlschmBVO[]) m_aggvo.getChildrenVO();
		String billtypeCode = parseBillTypes(vo.getBilltype());
		IBusiSysReg reg = null;
		try {
			reg = getBusiSysReg(vo.getCtrlsys());
		} catch (BusinessException ex) {
			NtbLogger.error(ex);
		}
		String m_pk_bdcontrast = null;
		ArrayList<String> orgsList = null;
		ArrayList<ControlBillType> billtypeList = BillTypeBySysCache.getInstance().getUfindPanelBySysid(vo.getCtrlsys());
		if (billtypeList == null) {
			m_pk_bdcontrast = reg.getMainPkOrg();
		} else {
			for (int n = 0; n < (billtypeList == null ? 0 : billtypeList.size()); n++) {
				ControlBillType billtype = (ControlBillType) billtypeList.get(n);
				if (billtype.getBillType_code().equals(billtypeCode)) {
					orgsList = billtype.getPk_orgs();
					break;
				}
			}
		}
		for (int n = 0; n < childvos.length; n++) {
			IdCtrlschmBVO m_vo = childvos[n];
			if (((m_pk_bdcontrast != null) && (m_pk_bdcontrast.equals(m_vo.getPk_bdcontrast()))) || ((orgsList != null) && (orgsList.contains(m_vo.getPk_bdcontrast())))) {
				pk_org = m_vo.getPk_base();
			}
		}

		return pk_org;
	}

	public static IdCtrlschmBVO[] filterCtrlschmBVO(IdCtrlschmBVO[] vos) {
		ArrayList<IdCtrlschmBVO> list = new ArrayList();
		for (int n = 0; n < (vos == null ? 0 : vos.length); n++) {
			if (vos[n].getPk_bdinfo() != null) {
				list.add(vos[n]);
			}
		}
		return (IdCtrlschmBVO[]) list.toArray(new IdCtrlschmBVO[0]);
	}

	public static String getFinalCtrlInfoMessage(HashMap<String, String> mapvalue, String ctrlinfo) {
		HashMap<String, String> mapInfo = CtrlInfoMacroConst.getAllCtrlMacro();
		Iterator map = mapInfo.entrySet().iterator();
		while (map.hasNext()) {
			Map.Entry entry = (Map.Entry) map.next();
			String key = (String) entry.getKey();
			ctrlinfo = ctrlinfo.replaceAll(key, (String) mapvalue.get(key));
		}
		return ctrlinfo;
	}

	public static String getControlCtlMessage(IdCtrlformulaVO vo, IdCtrlschemeVO vos, HashMap exeVarnoMap, UFDouble zxs_complex, String[] arrayS, String valueNameType, int powerInt) throws Exception {
		boolean isNumber = false;
		if (vos.getCtrlobj() != null) {
			isNumber = OutEnum.OCCORAMOUNT.indexOf(vos.getCtrlobj()) >= 0;
		}
		String ctrlObjName = parseCtrlObjName(vos.getCtrlsys(), vos.getCtrlobj());
		MdTask plan = TbTaskCtl.getMdTaskByPk(vo.getPk_plan(), true);
		String planname = plan.getObjname();
		String planSysName = getSysNameByCode(plan.getAvabusisystem());
		UFDouble rundata = new UFDouble(0);

		if (((vo.getPk_parent() == null) || ("".equals(vo.getPk_parent()))) && (vo.getSchemetype().equals(String.valueOf(3)))) {
			rundata = zxs_complex;
		} else {
			rundata = sumRunData(exeVarnoMap);
		}
		String[] ss = vos.getNameidx().split(":");
		StringBuffer buffer = new StringBuffer();

		String entityName = "";
		String entityPk = vos.getPk_org();
		String[] pkidx = vos.getStridx().split(":");
		for (int n = 0; n < pkidx.length; n++) {
			if ((entityPk != null) && (entityPk.equals(pkidx[n]))) {
				entityName = ss[n];
				break;
			}
		}

		buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000235", null, new String[] { planSysName })).append(entityName).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000236"));
		for (int i = 0; i < ss.length; i++) {
			buffer.append(ss[i] + "/");
		}
		if (!vo.getSchemetype().equals(String.valueOf(2))) {
			buffer.append(vos.getStartdate()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000237")).append(vos.getEnddate()).append("/");
		}

		buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
		String diminfo = buffer.toString();
		return getControlHintMessage(vo, planname, diminfo, isNumber, ctrlObjName, getValue(arrayS[0]), getValue(arrayS[1]), valueNameType, false, powerInt);
	}

	public static String getControlAlarmMessage(IdCtrlformulaVO vo, IdCtrlschemeVO vos, HashMap exeVarnoMap, UFDouble zxs_complex, String[] arrayS, String name, int powerInt) throws Exception {
		boolean isNumber = false;
		if (vos.getCtrlobj() != null) {
			isNumber = OutEnum.OCCORAMOUNT.indexOf(vos.getCtrlobj()) >= 0;
		}
		String ctrlObjName = parseCtrlObjName(vos.getCtrlsys(), vos.getCtrlobj());
		MdTask plan = TbTaskCtl.getMdTaskByPk(vo.getPk_plan(), true);
		String planname = plan.getObjname();

		UFDouble rundata = new UFDouble(0);

		if (((vo.getPk_parent() == null) || ("".equals(vo.getPk_parent()))) && (vo.getSchemetype().equals(String.valueOf(3)))) {
			rundata = zxs_complex;
		} else {
			rundata = sumRunData(exeVarnoMap);
		}

		String[] ss = vos.getNameidx().split(":");
		StringBuffer buffer = new StringBuffer();

		String entityName = "";
		String entityPk = vos.getPk_org();
		String[] pkidx = vos.getStridx().split(":");
		for (int n = 0; n < pkidx.length; n++) {
			if ((entityPk != null) && (entityPk.equals(pkidx[n]))) {
				entityName = ss[n];
				break;
			}
		}

		buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000239")).append(entityName).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000236"));

		for (int i = 0; i < ss.length; i++) {
			buffer.append(ss[i] + "/");
		}
		if (!vo.getSchemetype().equals(String.valueOf(2))) {
			buffer.append(vos.getStartdate()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000237")).append(vos.getEnddate()).append("/");
		}

		buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
		String diminfo = buffer.toString();
		return getControlHintMessage(vo, planname, diminfo, isNumber, ctrlObjName, getValue(arrayS[0]), getValue(arrayS[1]), name, false, powerInt);
	}

	public static int getCurrencyType(String pk_currency) {
		if ("@@@@Z0GLOBE000000001".equals(pk_currency))
			return 0;
		if ("@@@@Z0GROUP000000001".equals(pk_currency))
			return 1;
		if ("@@@@Z0ORG00000000001".equals(pk_currency)) {
			return 2;
		}
		return 3;
	}

	public static String parseCtrlObjName(String sysId, String objcode) throws BusinessException, Exception {
		IBusiSysReg glReg = getBusiSysReg(sysId);
		ArrayList<ControlObjectType> objs = glReg.getControlableObjects();
		ControlObjectType obj = null;
		for (int n = 0; n < objs.size(); n++) {
			ControlObjectType objTmp = (ControlObjectType) objs.get(n);
			if (objcode.equals(objTmp.getM_code())) {
				obj = objTmp;
			}
		}
		return obj == null ? "" : obj.getM_description();
	}

	public static UFDouble sumRunData(HashMap exeVarnoMap) {
		Iterator iter = exeVarnoMap.values().iterator();
		UFDouble sum = new UFDouble(0);
		while (iter.hasNext()) {
			UFDouble value = (UFDouble) iter.next();
			sum = sum.add(value);
		}
		return sum;
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

	public static String getControlHintMessage(IdCtrlformulaVO parentVO, String planname, String diminfo, boolean isNumber, String objName, UFDouble plnValue, UFDouble zxsValue, String name, boolean isStartCtrl, int powerInt) throws Exception {
		StringBuffer message = new StringBuffer();
		DecimalFormat formatter = null;
		String spliter = ":";
		String HHF = "\n";
		String FLEXMESSAGE = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_rule", "01801rul_000407");
		String infoss = null;
		String ctrlType = null;
		String frontHint = null;
		String ctrltype = null;
		StringBuffer diminfoStr = new StringBuffer().append(diminfo);
		MdTask plan = TbTaskCtl.getMdTaskByPk(parentVO.getPk_plan(), true);
		String planSysName = getSysNameByCode(plan.getAvabusisystem());
		boolean isShowDimInfo = true;
		if (parentVO.getCtlmode().equals(CtrlTypeEnum.RigidityControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000240", null, new String[] { planSysName }) + HHF;
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000241");

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000310") + HHF;
		} else if (parentVO.getCtlmode().equals(CtrlTypeEnum.WarningControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000243", null, new String[] { planSysName }) + HHF;

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000244") + HHF;
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000245");
		} else if (parentVO.getCtlmode().equals(CtrlTypeEnum.FlexibleControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000246", null, new String[] { planSysName }) + HHF;

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000247") + FLEXMESSAGE + "\n";
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000248");
		} else if (parentVO.getCtlmode().equals(String.valueOf(4))) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000249", null, new String[] { planSysName });
			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000250");
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000251");
		}

		if (parentVO.getSchemetype().equals(String.valueOf(1))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000201");
			isShowDimInfo = true;
		} else if (parentVO.getSchemetype().equals(String.valueOf(2))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000252");
			isShowDimInfo = true;
		} else if (parentVO.getSchemetype().equals(String.valueOf(3))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000203");
			isShowDimInfo = true;
		} else if (parentVO.getSchemetype().equals(String.valueOf(6))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000204");
			isShowDimInfo = true;
		} else if (parentVO.getSchemetype().equals(String.valueOf(4))) {
			ctrlType = "";
			isShowDimInfo = true;
		}

		String ctrlsign = replaceCtrlsign(parentVO.getCtrlsign());
		NtbCtrlMath.setPrice(plnValue.toString());
		String value = NtbCtrlMath.getPrice();

		StringBuffer sbStr = new StringBuffer("##,##0.");
		for (int n = 0; n < Math.abs(powerInt); n++) {
			sbStr.append("0");
		}
		DecimalFormat format = new DecimalFormat(sbStr.toString());
		String zValue = format.format(zxsValue.doubleValue());
		message.append(isStartCtrl ? NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000253") : "").append(HHF).append(frontHint).append((CharSequence) (isShowDimInfo ? diminfoStr : "")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000254")).append(name).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000254")).append(objName).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(zValue).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050")).append(ctrlsign).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000255")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(value).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000256") + HHF).append(isStartCtrl ? "" : infoss).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000257", null, new String[] { planSysName }) + HHF).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000258", null, new String[] { planSysName })).append(planname).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000259")).append(ctrltype).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(parentVO.getCtrlname()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000260")).append("\n");

		return message.toString();
	}

	public static String replaceCtrlsign(String ctrlsign) {
		String[][] ctrlSignReplacePattern = { { ">=", ">" }, { "<=", "<" }, { ">", ">=" }, { "<", "<=" }, { "=", "<>" } };

		for (int i = 0; i < ctrlSignReplacePattern.length; i++) {
			if (ctrlsign.equals(ctrlSignReplacePattern[i][0]))
				return ctrlSignReplacePattern[i][1];
		}
		return null;
	}

	public static String getActualPkcurrency(String pk_currency, String pk_org, String sysId) {
		if ("@@@@Z0GLOBE000000001".equals(pk_currency))
			return CurrencyManager.getGlobalDefaultCurrencyPK();
		if ("@@@@Z0GROUP000000001".equals(pk_currency)) {
			String pk_group = null;
			return CurrencyManager.getLocalCurrencyPKByGroup(pk_group);
		}
		if ("@@@@Z0ORG00000000001".equals(pk_currency)) {
			return CurrencyManager.getLocalCurrencyPK(pk_org);
		}
		return pk_currency;
	}

	public static String getPk_currency(String pk_currency, String pk_org, String sysId) {
		if ("@@@@Z0GLOBE000000001".equals(pk_currency)) {
			if (!OutEnum.FIBILLSYS.equals(sysId)) {
				return CurrencyManager.getGlobalDefaultCurrencyPK();
			}
			return null;
		}
		if ("@@@@Z0GROUP000000001".equals(pk_currency)) {
			String pk_group = null;
			if (!OutEnum.FIBILLSYS.equals(sysId)) {
				return CurrencyManager.getLocalCurrencyPKByGroup(pk_group);
			}
			return null;
		}
		if ("@@@@Z0ORG00000000001".equals(pk_currency)) {
			if (!OutEnum.FIBILLSYS.equals(sysId)) {
				return CurrencyManager.getLocalCurrencyPK(pk_org);
			}
			return null;
		}

		return pk_currency;
	}

	private static String spliter = ":";

	public static HashMap<String, String> getMainOrgBySysidAndBillType(String sysid, String billtype, String pk_group) throws BusinessException {
		HashMap<String, String> pkList = new HashMap();
		if (sysid.equals("GL")) {
			IFinanceOrgPubService ibFinanceOrg = (IFinanceOrgPubService) NCLocator.getInstance().lookup(IFinanceOrgPubService.class.getName());
			FinanceOrgVO[] vos = ibFinanceOrg.queryAllFinanceOrgVOSByGroupID(pk_group);
			for (int n = 0; n < vos.length; n++) {
				pkList.put(vos[n].getPk_financeorg(), vos[n].getCode());
			}
		} else if (sysid.equals("FA")) {
			IFinanceOrgPubService ibFinanceOrg = (IFinanceOrgPubService) NCLocator.getInstance().lookup(IFinanceOrgPubService.class.getName());
			ibFinanceOrg.queryAllFinanceOrgVOSByGroupID(pk_group);
			FinanceOrgVO[] vos = ibFinanceOrg.queryAllFinanceOrgVOSByGroupID(pk_group);
			for (int n = 0; n < vos.length; n++) {
				pkList.put(vos[n].getPk_financeorg(), vos[n].getCode());
			}
		} else if (!sysid.equals("FP")) {
		}

		return pkList;
	}

	public static ArrayList<String[]> getActualAccsubjPK(String pk_org, String pk_account, NtbParamVO ntbvo) throws BusinessException {
		ArrayList<String[]> pkList = new ArrayList();
		AccountVO newvo = null;
		if (!ntbvo.getSys_id().equals("GL")) {
			pkList.add(new String[] { pk_org, null, null });
			return pkList;
		}

		IAccountingBookPubService accountbook = (IAccountingBookPubService) NCLocator.getInstance().lookup(IAccountingBookPubService.class);
		String[] pk_accountbooks = accountbook.queryAccountingBookIDSByFinanceOrgID(pk_org);
		for (int n = 0; n < (pk_accountbooks == null ? 0 : pk_accountbooks.length); n++) {
			newvo = getAccountByBaAndAccount(pk_accountbooks[n], pk_account);
			String[] tmpStr = { pk_accountbooks[n], newvo.getPk_accasoa(), newvo.getCode() };
			pkList.add(tmpStr);
		}
		return pkList;
	}

	public static AccountVO getAccountByBaAndAccount(String pk_bookaccount, String pk_account) throws BusinessException {
		IAccountPubService account = (IAccountPubService) NCLocator.getInstance().lookup(IAccountPubService.class);
		UFDate date = UFDate.getDate(System.currentTimeMillis());
		AccountVO[] vos = account.queryAccountVOs(pk_bookaccount, TimeDimTool.getUAPDataStr(String.valueOf(date.getYear()), String.valueOf(date.getMonth()), null), UFBoolean.valueOf(false));

		AccountVO vo = null;
		for (int n = 0; n < (vos == null ? 0 : vos.length); n++) {
			if (pk_account.equals(vos[n].getPk_account())) {
				vo = vos[n];
				break;
			}
		}
		return vo;
	}

	public static void addMainOrgInfo(NtbParamVO param) {
		String pk_org = param.getPk_Org();
		ArrayList<String> typedim = new ArrayList();
		typedim.addAll(Arrays.asList(param.getTypeDim()));
		ArrayList<String> pkdim = new ArrayList();
		pkdim.addAll(Arrays.asList(param.getPkDim()));
		ArrayList<String> codedim = new ArrayList();
		codedim.addAll(Arrays.asList(param.getCode_dims()));
		pkdim.add(pk_org);
		codedim.add("");
		if (param.getSys_id().equals("GL")) {
			typedim.add(OutEnum.ZHANGBU);
		} else if (param.getSys_id().equals("FA")) {
			typedim.add(OutEnum.FINANCEORG);
		} else {
			typedim.add(OutEnum.FINANCEORG);
		}
		param.setTypeDim((String[]) typedim.toArray(new String[0]));
		param.setPkDim((String[]) pkdim.toArray(new String[0]));
		param.setCode_dims((String[]) codedim.toArray(new String[0]));
	}

	public static void addGroupDownAllOrgParams(NtbParamVO paramvo, ArrayList<NtbParamVO> paramvoList, IdCtrlschemeVO schemevo) throws BusinessException {
		HashMap<String, String> orgPkList = getMainOrgBySysidAndBillType(paramvo.getSys_id(), null, paramvo.getPk_Org());

		try {
			String bdinfotype = getBdinfoType(schemevo.getFromitems(), schemevo.getCtrlsys());
			String[] bdinfotypeidx = bdinfotype.split(spliter);
			String[] pkidx = schemevo.getStridx().split(spliter);
			String[] codeidx = schemevo.getCodeidx().split(spliter);

			String pk_account = null;
			String m_code = null;
			for (int j = 0; j < bdinfotypeidx.length; j++) {
				if (OutEnum.ACCSUBJDOC.equals(bdinfotypeidx[j])) {
					pk_account = pkidx[j];
					m_code = codeidx[j];
				}
			}
			Iterator iter = orgPkList.entrySet().iterator();

			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String pk_org = (String) entry.getKey();
				String code = (String) entry.getValue();
				if (schemevo.getCtrlsys().equals("GL")) {
					if (pk_account != null) {
						ArrayList<String[]> tmpList = getActualAccsubjPK(pk_org, pk_account, paramvo);
						for (int n = 0; n < (tmpList == null ? 0 : tmpList.size()); n++) {
							String pk_fatherParamVO = paramvo.getPk_ctrl();
							NtbParamVO param = (NtbParamVO) paramvo.clone();
							param.setFatherCorpPk(pk_fatherParamVO);
							param.setPk_Org(((String[]) tmpList.get(n))[0]);
							param.setCode_corp(code);
							param.setCode_dims(schemevo.getCodeidx().replace(m_code, ((String[]) tmpList.get(n))[2]).split(spliter));
							param.setPkDim(schemevo.getStridx().replace(pk_account, ((String[]) tmpList.get(n))[1]).split(spliter));
							addMainOrgInfo(param);
							paramvoList.add(param);
						}
					} else {
						ArrayList<String> tmpList = getActualOrgPK(pk_org, paramvo);
						for (int n = 0; n < (tmpList == null ? 0 : tmpList.size()); n++) {
							String pk_fatherParamVO = paramvo.getPk_ctrl();
							NtbParamVO param = (NtbParamVO) paramvo.clone();
							param.setFatherCorpPk(pk_fatherParamVO);
							param.setPk_Org((String) tmpList.get(n));
							param.setCode_corp(code);
							param.setCode_dims(codeidx);
							param.setPkDim(pkidx);
							addMainOrgInfo(param);
							paramvoList.add(param);
						}
					}
				} else {
					String pk_fatherParamVO = paramvo.getPk_ctrl();
					NtbParamVO param = (NtbParamVO) paramvo.clone();
					param.setFatherCorpPk(pk_fatherParamVO);
					param.setPk_Org(pk_org);
					param.setCode_corp(code);
					param.setCode_dims(codeidx);
					param.setPkDim(pkidx);
					addMainOrgInfo(param);
					paramvoList.add(param);
				}
			}
		} catch (BusinessException ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	public static ArrayList<String> getActualOrgPK(String pk_org, NtbParamVO ntbvo) throws BusinessException {
		ArrayList<String> pkList = new ArrayList();
		if (!ntbvo.getSys_id().equals("GL")) {
			pkList.add(pk_org);
			return pkList;
		}
		IAccountingBookPubService accountbook = (IAccountingBookPubService) NCLocator.getInstance().lookup(IAccountingBookPubService.class);
		String[] pk_accountbooks = accountbook.queryAccountingBookIDSByFinanceOrgID(pk_org);
		for (int n = 0; n < (pk_accountbooks == null ? 0 : pk_accountbooks.length); n++) {
			pkList.add(pk_accountbooks[n]);
		}
		return pkList;
	}

	public static String getFullDest(ArrayList<DataContrastVO> voList, UFBoolean isPlDeal) throws BusinessException {
		String sExpress = null;
		try {
			if ((isPlDeal != null) && (isPlDeal.booleanValue())) {
				sExpress = CtrlRuleCTL.getSumFindString(voList, true);
			} else {
				sExpress = CtrlRuleCTL.getSumFindString(voList, false);
			}
		} catch (BusinessException ex) {
			NtbLogger.printException(ex);
			throw ex;
		}
		return sExpress;
	}

	public static String getUFindExpress(ArrayList<DataContrastVO> voList, CtlAggregatedVO[][] aggvos, boolean isPlDeal, boolean flag) throws BusinessException {
		CtlAggregatedVO[] memvos = aggvos[1];
		CtlAggregatedVO vo = aggvos[0][0];
		IdCtrlschmVO parent = (IdCtrlschmVO) vo.getParentVO();
		String isAcctroll = parent.getAccctrollflag();
		StringBuffer express = new StringBuffer();
		if (isPlDeal) {
			flag = false;
			for (CtlAggregatedVO memvo : memvos) {
				IdCtrlschmVO _parent = (IdCtrlschmVO) memvo.getParentVO();
				_parent.setAccctrollflag(isAcctroll);
				int arNo = ((IdCtrlschmVO) memvo.getParentVO()).getVarno().indexOf("var") >= 0 ? 0 : 1;
				String contentExp = null;
				if (arNo == 0) {
					contentExp = "UFIND('" + toUFindAttrExpress(voList, _parent, (IdCtrlschmBVO[]) memvo.getChildrenVO(), flag, memvos) + "')";
				} else
					contentExp = "PREFIND('" + toUFindAttrExpress(voList, _parent, (IdCtrlschmBVO[]) memvo.getChildrenVO(), flag, memvos) + "')";
				express.append(contentExp + "+");
			}
			express.replace(express.length() - 1, express.length(), "");

		} else {

			DataContrastVO _vo = (DataContrastVO) voList.get(0);
			if (voList.size() > 1) {
				_vo.setNoSaveLevelValue(true);
			}

			String runFormula = parent.getRunformula();
			if (!StringUtil.isEmpty(runFormula)) {
				express.append(parent.getRunformula());
			}
			for (int i = 0; i < memvos.length; i++) {
				ArrayList<CtlAggregatedVO> tmpList = new ArrayList();
				tmpList.addAll(Arrays.asList(memvos));
				tmpList.remove(i);
				String vars = ((IdCtrlschmVO) memvos[i].getParentVO()).getVarno();

				int arNo = ((IdCtrlschmVO) memvos[i].getParentVO()).getVarno().indexOf("var") >= 0 ? 0 : 1;
				if (arNo == 0) {
					IdCtrlschmVO _parent = (IdCtrlschmVO) memvos[i].getParentVO();
					_parent.setAccctrollflag(isAcctroll);
					List<DataContrastVO> contrastVos = new ArrayList();
					contrastVos.add(_vo);
					String ufindexpress = "UFIND('" + toUFindAttrExpress(contrastVos, _parent, (IdCtrlschmBVO[]) memvos[i].getChildrenVO(), flag, (CtlAggregatedVO[]) tmpList.toArray(new CtlAggregatedVO[0])) + "')";
					int location = express.indexOf(vars);
					if (location >= 0) {
						express.replace(location, location + vars.length(), ufindexpress);
					} else
						express.append(ufindexpress);
				} else {
					IdCtrlschmVO _parent = (IdCtrlschmVO) memvos[i].getParentVO();
					_parent.setAccctrollflag(isAcctroll);
					List<DataContrastVO> contrastVos = new ArrayList();
					contrastVos.add(_vo);
					String ufindexpress = "PREFIND('" + toUFindAttrExpress(contrastVos, _parent, (IdCtrlschmBVO[]) memvos[i].getChildrenVO(), flag, (CtlAggregatedVO[]) tmpList.toArray(new CtlAggregatedVO[0])) + "')";
					int location = express.indexOf(vars);
					if (location >= 0) {
						express.replace(location, location + vars.length(), ufindexpress);
					} else {
						express.append(ufindexpress);
					}
				}
			}
		}

		return express.toString();
	}

	private static String getVirtualBilltype(CtlAggregatedVO[] memvos, IdCtrlschmVO parentvo) {
		ArrayList<String> otherBilltype = new ArrayList();
		String varNo = parentvo.getVarno();
		String othersizeNO = varNo.indexOf("var") == 0 ? varNo.replaceAll("var", "rar") : varNo.replaceAll("rar", "var");
		for (int n = 0; n < memvos.length; n++) {
			CtlAggregatedVO vo = memvos[n];
			IdCtrlschmVO voTmp = (IdCtrlschmVO) vo.getParentVO();
			if (othersizeNO.equals(voTmp.getVarno())) {
				String code = parseBillTypes(voTmp.getBilltype());
				otherBilltype.add(code);
			}
		}
		StringBuffer sbStr = new StringBuffer();
		sbStr.append(parseBillTypes(parentvo.getBilltype())).append("-");
		for (int n = 0; n < otherBilltype.size(); n++) {
			if (n != otherBilltype.size() - 1) {
				sbStr.append((String) otherBilltype.get(n)).append("-");
			} else {
				sbStr.append((String) otherBilltype.get(n));
			}
		}
		return sbStr.toString();
	}

	private static boolean isCheckVarNO(CtlAggregatedVO[] memvos, IdCtrlschmVO parentvo) {
		ArrayList<String> varNos = new ArrayList();
		ArrayList<String> rarNos = new ArrayList();
		String varNo = parentvo.getVarno();
		if (varNo.indexOf("var") == 0) {
			varNos.add(varNo);
		}
		if (varNo.indexOf("rar") == 0) {
			rarNos.add(varNo);
		}
		for (int n = 0; n < memvos.length; n++) {
			CtlAggregatedVO vo = memvos[n];
			IdCtrlschmVO voTmp = (IdCtrlschmVO) vo.getParentVO();
			String rarNo = voTmp.getVarno();
			if (rarNo.indexOf("var") == 0) {
				varNos.add(varNo);
			}
			if (rarNo.indexOf("rar") == 0) {
				rarNos.add(varNo);
			}
		}
		if (varNos.size() == rarNos.size()) {
			return true;
		}
		return false;
	}

	public static String toUFindAttrExpress(List<DataContrastVO> contrastVos, IdCtrlschmVO parentvo, IdCtrlschmBVO[] ctrlSchmBvos, boolean flag, CtlAggregatedVO[] memvos) throws BusinessException {
		StringBuffer ufindexpress = new StringBuffer();
		HashMap<String, String> hm = new HashMap();

		String sysid = parentvo.getCtrlsys();
		if (flag) {
			hm = getSingleAndComplexArrtibuteMaps(sysid, ctrlSchmBvos);
		} else {
			hm = getAttributeMaps(contrastVos, sysid, ctrlSchmBvos, parentvo.getBilltype());
		}
		ufindexpress.append(sysid);
		ufindexpress.append(",");

		if ((parentvo.getBilltypeObj() != null) && (parentvo.getBilltypeObj().isUseVirtualBilltype())) {
			String billtype = null;
			billtype = getVirtualBilltype(memvos, parentvo);
			if (billtype != null) {
				ufindexpress.append(billtype);
			} else {
				ufindexpress.append(parentvo.getBilltype());
			}
		} else {
			ufindexpress.append(parentvo.getBilltype());
		}
		ufindexpress.append(",");

		ufindexpress.append(parentvo.getCtrldirection() == null ? "" : parentvo.getCtrldirection().trim());
		ufindexpress.append(",");

		ufindexpress.append(parentvo.getCtrlobj());
		ufindexpress.append(",");

		ufindexpress.append(parentvo.getCtrlObjValue());
		ufindexpress.append(",");
		ufindexpress.append(",");
		ufindexpress.append(parentvo.getStartdate() == null ? "" : parentvo.getStartdate());
		ufindexpress.append(",");
		ufindexpress.append(parentvo.getEnddate() == null ? "" : parentvo.getEnddate());
		ufindexpress.append(",");

		ufindexpress.append(parentvo.getDateType());
		ufindexpress.append(",");
		ufindexpress.append(parentvo.getAccctrollflag());
		ufindexpress.append(",");
		ufindexpress.append("@pkcorp");
		ufindexpress.append(",");

		ufindexpress.append(parentvo.getPk_currency() == null ? "@pkcurrency" : parentvo.getPk_currency());
		ufindexpress.append(",");
		ufindexpress.append("@pkncentity");
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("fromitem"));
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("stridx"));
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("codeidx"));
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("nameidx"));
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("ctrllevel"));
		ufindexpress.append(",");
		ufindexpress.append((String) hm.get("mainorgs"));

		if ((parentvo.getMemo() == null) || ("".equals(parentvo.getMemo()))) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000054"));
		}

		if (parentvo.getCtrlsys() == null) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000055"));
		}
		if (OutEnum.MPPSYS.equals(parentvo.getCtrlsys())) {
			boolean isCheckVarNo = isCheckVarNO(memvos, parentvo);
			if (!isCheckVarNo) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000316"));
			}
		}

		IBusiSysReg reg = getBusiSysReg(parentvo.getCtrlsys());
		ArrayList<ControlBillType> billtypes = BillTypeBySysCache.getInstance().getUfindPanelBySysid(sysid);
		if ((billtypes != null) && (billtypes.size() > 0) && ((parentvo.getBilltype() == null) || (parentvo.getBilltype().length() == 0))) {
			IdSysregVO vo = BusiSysReg.getSharedInstance().getSysregByName(parentvo.getCtrlsys());
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000056", null, new String[] { vo.toString() }));
		}

		String billtype = parseBillTypes(parentvo.getBilltype());
		if (reg.isUseAccountDate(billtype)) {
			if ((parentvo.getStartdate() == null) || (parentvo.getEnddate() == null) || ("".equals(parentvo.getStartdate())) || ("".equals(parentvo.getEnddate()))) {
				if (((parentvo.getStartdate() == null) || ("".equals(parentvo.getStartdate()))) && (parentvo.getEnddate() != null) && (!"".equals(parentvo.getEnddate()))) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000057"));
				}
				if (((parentvo.getEnddate() == null) || ("".equals(parentvo.getEnddate()))) && (parentvo.getStartdate() != null) && (!"".equals(parentvo.getStartdate()))) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000058"));
				}
			} else {
				String _startdate = parentvo.getStartdate();
				String _enddate = parentvo.getEnddate();
				if (DateUtil.getDiffAccountDate(_startdate, _enddate) > 0L) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000059"));
				}

			}

		} else if ((parentvo.getStartdate() == null) || (parentvo.getEnddate() == null) || ("".equals(parentvo.getStartdate())) || ("".equals(parentvo.getEnddate()))) {
			if (((parentvo.getStartdate() == null) || ("".equals(parentvo.getStartdate()))) && (parentvo.getEnddate() != null) && (!"".equals(parentvo.getEnddate()))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000060"));
			}
			if (((parentvo.getEnddate() == null) || ("".equals(parentvo.getEnddate()))) && (parentvo.getStartdate() != null) && (!"".equals(parentvo.getStartdate()))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000061"));
			}
		} else {
			String _startdate = parentvo.getStartdate();
			String _enddate = parentvo.getEnddate();
			if (DateUtil.getDiff(_startdate, _enddate) > 0L) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000059"));
			}
		}

		if ((ctrlSchmBvos == null) || (ctrlSchmBvos.length == 0)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000141"));
		}
		for (int i = 0; i < ctrlSchmBvos.length; i++) {
			if (ctrlSchmBvos[i].getPk_bdcontrast() == null) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000062"));
			}
		}
		return ufindexpress.toString();
	}

	public static void checkSchmByCube(String acctrollFlag, IdCtrlschmVO schparentvo, CubeDef cubedef) throws BusinessException {
		String startDate = schparentvo.getStartdate();
		String endDate = schparentvo.getEnddate();

		if (((startDate != null) || (endDate != null)) && ((!"".equals(startDate)) || (!"".equals(endDate)))) {
			AccumulateEnum accEnum = AccumulateEnum.fromCodeString(acctrollFlag);
			if (accEnum.toInt() != -1) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl_0", "01050ctrl003-0000"));
			}
		}
		DimHierarchy dimhier = cubedef.getDimHierarchy("TB_DIMDEF_TIME_00000");

		if (dimhier.getPrimaryKey().equals("TB_DIMHIER_TIME_DEFP")) {
			AccumulateEnum accEnum = AccumulateEnum.fromCodeString(acctrollFlag);
			int type = accEnum.toInt();
			if ((type == 0) || (type == 1) || (type == 2)) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl_0", "01050ctrl002-0000"));
			}
		}

		if ((dimhier.getTableex() != null) && (dimhier.getTableex().indexOf("tb_time") >= 0)) {
			List<DimLevel> levelList = dimhier.getDimLevels();
			AccumulateEnum accEnum = AccumulateEnum.fromCodeString(acctrollFlag);

			int type = accEnum.toInt();
			String accPk = null;
			if (type == 0) {
				accPk = "TB_DIMLEV_MONTH_0000";
			} else if (type == 1) {
				accPk = "TB_DIMLEV_QUARTER_00";
			} else if (type == 2) {
				accPk = "TB_DIMLEV_YEAR_00000";
			}
			if (accPk != null) {
				boolean isContainInHier = false;
				List<String> levelPks = new ArrayList();
				for (DimLevel level : levelList) {
					if (level.getPrimaryKey().equals(accPk))
						isContainInHier = true;
					levelPks.add(level.getPrimaryKey());
				}
				if (!isContainInHier) {

					if ((type != 1) || (!levelPks.contains("TB_DIMLEV_YEAR_00000")) || (!levelPks.contains("TB_DIMLEV_MONTH_0000")))
						throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl_0", "01050ctrl002-0001") + dimhier.getObjName() + NCLangRes4VoTransl.getNCLangRes().getStrByID("ctrl_0", "01050ctrl002-0002") + accEnum.toString());
				}
			}
		}
	}

	public static void checkSchmByCube(CtlAggregatedVO[][] aggvos, CubeDef cubedef) throws BusinessException {
		CtlAggregatedVO aggvo = aggvos[0][0];
		IdCtrlschmVO parentvo = (IdCtrlschmVO) aggvo.getParentVO();

		String acctrollFlag = parentvo.getAccctrollflag();

		CtlAggregatedVO schaggvo = aggvos[1][0];
		IdCtrlschmVO schparentvo = (IdCtrlschmVO) schaggvo.getParentVO();

		checkSchmByCube(acctrollFlag, schparentvo, cubedef);
	}

	public static void checkSchmLegal(CtlAggregatedVO[][] aggvos) throws BusinessException {
		CtlAggregatedVO[] memvos = aggvos[1];

		for (CtlAggregatedVO memvo : memvos) {
			IdCtrlschmVO parentvo = (IdCtrlschmVO) memvo.getParentVO();
			IdCtrlschmBVO[] schmvos = (IdCtrlschmBVO[]) memvo.getChildrenVO();

			if ((parentvo.getCtrlsys().equals(OutEnum.MPPSYS)) && (parentvo.getCtrlmode() != null) && (parentvo.getCtrlmode().equals("2"))) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl_0", "01050ctrl001-0061"));
			}

			if (parentvo.getCtrlsys().equals(OutEnum.GLSYS)) {
				boolean isHasAccount = false;
				for (IdCtrlschmBVO schmVO : schmvos) {
					if (schmVO.getDatafrom().equals("DETAIL103"))
						isHasAccount = true;
				}
				if (!isHasAccount) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_rule", "01420rul_000141"));
				}
				UFDateTime startTime = null;
				UFDateTime endTime = null;

				if (!StringUtil.isEmpty(parentvo.getStartdate()))
					startTime = new UFDateTime(parentvo.getStartdate());
				if (!StringUtil.isEmpty(parentvo.getEnddate()))
					endTime = new UFDateTime(parentvo.getEnddate());
				if ((startTime != null) && (endTime != null)) {
					int startYear = startTime.getYear();
					int endYear = endTime.getYear();
					if (startYear != endYear) {
						throw new CheckSchmException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl_0", "01050ctrl001-0062"), false);
					}
				}
			}

			if ((parentvo.getCtrlsys().equals(OutEnum.SFSYS)) && (parentvo.getBilltype() != null) && (parentvo.getBilltype().indexOf("36K6") >= 0)) {
				boolean isHasCorrOrg = false;
				for (IdCtrlschmBVO schmVO : schmvos) {
					if ((schmVO.getDatafrom().equals("pk_org_p36K6")) || (schmVO.getDatafrom().equals("pk_org_r36K6"))) {
						isHasCorrOrg = true;
					}
				}
				if (!isHasCorrOrg) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000320"));
				}
			}
		}
	}

	public static HashMap<String, String> getSingleAndComplexArrtibuteMaps(String sys, IdCtrlschmBVO[] ctrlSchmBvos) {
		HashMap<String, String> hm = new HashMap();

		StringBuffer fromitem = new StringBuffer();
		StringBuffer nameidx = new StringBuffer();
		StringBuffer codeidx = new StringBuffer();
		StringBuffer ctrllevel = new StringBuffer();
		StringBuffer stridx = new StringBuffer();
		StringBuffer mainorgs = new StringBuffer();

		for (int i = 0; i < ctrlSchmBvos.length; i++) {
			if (i != ctrlSchmBvos.length - 1) {
				fromitem.append(ctrlSchmBvos[i].getDatafrom() + ":");
				nameidx.append(ctrlSchmBvos[i].getBasename() + ":");
				codeidx.append(ctrlSchmBvos[i].getBasecode() + ":");
				ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag() + ":");
				stridx.append(ctrlSchmBvos[i].getPk_base() + ":");
				mainorgs.append(ctrlSchmBvos[i].isMainOrg() + ":");
			} else {
				fromitem.append(ctrlSchmBvos[i].getDatafrom());
				nameidx.append(ctrlSchmBvos[i].getBasename());
				codeidx.append(ctrlSchmBvos[i].getBasecode());
				ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag());
				stridx.append(ctrlSchmBvos[i].getPk_base());
				mainorgs.append(ctrlSchmBvos[i].isMainOrg());
			}
		}
		hm.put("fromitem", fromitem.toString());
		hm.put("nameidx", replace(nameidx.toString()));
		hm.put("codeidx", codeidx.toString());
		hm.put("ctrllevel", ctrllevel.toString());
		hm.put("stridx", stridx.toString());
		hm.put("mainorgs", mainorgs.toString());
		return hm;
	}

	public static HashMap<String, String> getAttributeMaps(List<DataContrastVO> contrastVos, String sys, IdCtrlschmBVO[] ctrlSchmBvos, String billtype) throws BusinessException {
		HashMap<String, String> hm = new HashMap();

		Map<String, List<LevelValue>> map = new HashMap();
		StringBuffer fromitem = new StringBuffer();
		StringBuffer nameidx = new StringBuffer();
		StringBuffer codeidx = new StringBuffer();
		StringBuffer ctrllevel = new StringBuffer();
		StringBuffer stridx = new StringBuffer();
		StringBuffer mainorgs = new StringBuffer();

		IdBdcontrastVO[] bdcontrast = null;
		billtype = parseBillTypes(billtype);
		if ((billtype != null) && (!"".equals(billtype))) {
			bdcontrast = BdContrastCache.getNewInstance().getVoBySysAndBill(sys, billtype);
		} else {
			bdcontrast = BdContrastCache.getNewInstance().getVoBySysid(sys);
		}
		for (DataContrastVO contrastVO : contrastVos) {
			LevelValue[] levelvalues = (LevelValue[]) contrastVO.getLevelValueList().toArray(new LevelValue[0]);
			for (int i = 0; i < levelvalues.length; i++) {
				LevelValue levelvalue = levelvalues[i];
				String classId = CtrlRuleCTL.getClassIDByDimLevel(levelvalue);

				if ("TB_DIMLEV_ENTITYUNIT".equals(classId)) {
					for (int n = 0; n < (ctrlSchmBvos == null ? 0 : ctrlSchmBvos.length); n++) {
						IdCtrlschmBVO bvo = ctrlSchmBvos[n];
						if (bvo.isMainOrg().booleanValue()) {
							IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVoByPK(bvo.getPk_bdcontrast());
							String realClassId = vo.getPk_bdinfo();
							IGeneralAccessor accesssor = GeneralAccessorFactory.getAccessor(realClassId);
							IBDData data = accesssor.getDocByPk((String) levelvalue.getKey());
							if (data != null) {
								if (map.containsKey(vo.getAtt_fld())) {
									((List) map.get(vo.getAtt_fld())).add(levelvalue);
								} else {
									List<LevelValue> valueList = new ArrayList();
									valueList.add(levelvalue);
									map.put(vo.getAtt_fld(), valueList);
								}
							}
						}
					}
				}
				if (bdcontrast != null) {
					boolean isExistExt = false;
					for (int j = 0; j < bdcontrast.length; j++) {
						if ((classId != null) && (classId.equals(bdcontrast[j].getPk_bdinfo()))) {
							if (map.containsKey(bdcontrast[j].getAtt_fld())) {
								((List) map.get(bdcontrast[j].getAtt_fld())).add(levelvalue);
							} else {
								List<LevelValue> valueList = new ArrayList();
								valueList.add(levelvalue);
								map.put(bdcontrast[j].getAtt_fld(), valueList);
							}
							isExistExt = true;
							break;
						}
					}

					if (isExistExt) {
					}
				}
			}
		}

		for (int i = 0; i < ctrlSchmBvos.length; i++) {
			if (map.containsKey(ctrlSchmBvos[i].getDatafrom())) {
				List<LevelValue> dms = (List) map.get(ctrlSchmBvos[i].getDatafrom());
				if (i != ctrlSchmBvos.length - 1) {
					fromitem.append(ctrlSchmBvos[i].getDatafrom() + ":");
					ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag() + ":");
					mainorgs.append(ctrlSchmBvos[i].isMainOrg() + ":");

					for (int j = 0; j < dms.size(); j++) {
						if (((LevelValue) dms.get(j)).getName() != null) {
							if (((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue()) {
								nameidx.append("null");
							} else {
								nameidx.append(((LevelValue) dms.get(j)).getName());
								if (j != dms.size() - 1) {
									nameidx.append("#");
								}
							}
						} else {
							nameidx.append("null");
						}
						if (((LevelValue) dms.get(j)).getCode() != null) {
							if (((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue()) {
								codeidx.append("null");
							} else {
								codeidx.append(((LevelValue) dms.get(j)).getCode());
								if (j != dms.size() - 1) {
									codeidx.append("#");
								}
							}
						} else {
							codeidx.append("null");
						}
						if (((LevelValue) dms.get(j)).getKey() != null) {
							if (((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue()) {
								stridx.append("null");
							} else {
								stridx.append(((LevelValue) dms.get(j)).getKey());
								if (j != dms.size() - 1) {
									stridx.append("#");
								}
							}
						} else {
							stridx.append("null");
						}
					}

					nameidx.append(":");
					codeidx.append(":");
					stridx.append(":");
				} else {
					fromitem.append(ctrlSchmBvos[i].getDatafrom());
					ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag());
					mainorgs.append(ctrlSchmBvos[i].isMainOrg());
					for (int j = 0; j < dms.size(); j++) {
						if ((((LevelValue) dms.get(j)).getName() != null) && (!((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue())) {
							nameidx.append(((LevelValue) dms.get(j)).getName());
							if (j != dms.size() - 1) {
								nameidx.append("#");
							}
						} else {
							nameidx.append("null");
						}
						if ((((LevelValue) dms.get(j)).getCode() != null) && (!((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue())) {
							codeidx.append(((LevelValue) dms.get(j)).getCode());
							if (j != dms.size() - 1) {
								codeidx.append("#");
							}
						} else {
							codeidx.append("null");
						}
						if ((((LevelValue) dms.get(j)).getKey() != null) && (!((DataContrastVO) contrastVos.get(0)).isNoSaveLevelValue())) {
							stridx.append(((LevelValue) dms.get(j)).getKey());
							if (j != dms.size() - 1) {
								stridx.append("#");
							}
						} else {
							stridx.append("null");
						}
					}

					nameidx.append(":");
					codeidx.append(":");
					stridx.append(":");

				}

			} else if (i != ctrlSchmBvos.length - 1) {
				fromitem.append(ctrlSchmBvos[i].getDatafrom() + ":");
				nameidx.append(ctrlSchmBvos[i].getBasename() + ":");
				codeidx.append(ctrlSchmBvos[i].getBasecode() + ":");
				ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag() + ":");
				mainorgs.append(ctrlSchmBvos[i].isMainOrg() + ":");
				stridx.append(ctrlSchmBvos[i].getPk_base() + ":");
			} else {
				fromitem.append(ctrlSchmBvos[i].getDatafrom());
				nameidx.append(ctrlSchmBvos[i].getBasename());
				codeidx.append(ctrlSchmBvos[i].getBasecode());
				ctrllevel.append(ctrlSchmBvos[i].getCtllevelflag());
				mainorgs.append(ctrlSchmBvos[i].isMainOrg());
				stridx.append(ctrlSchmBvos[i].getPk_base());
			}
		}

		hm.put("fromitem", fromitem.toString());
		hm.put("nameidx", replace(nameidx.toString()));
		hm.put("codeidx", codeidx.toString());
		hm.put("ctrllevel", ctrllevel.toString());
		hm.put("stridx", stridx.toString());
		hm.put("mainorgs", mainorgs.toString());

		return hm;
	}

	public static String replace(String content) {
		String replace = "@CONTENT@";
		if ((content != null) && (content.contains(","))) {
			content = content.replaceAll(",", replace);
		}
		return content;
	}

	public static String place(String content) {
		String replace = "@CONTENT@";
		if ((content != null) && (content.contains(replace))) {
			content = content.replaceAll(replace, ",");
		}
		return content;
	}

	public static IBDData[] getBddataVo(String pk, String pk_bdinfo, String pk_org, String sys) throws Exception {
		List<IBDData> datavos = null;
		if (pk == null) {
			return null;
		}

		IdBdcontrastVO vo = BdContrastCache.getNewInstance().getVOByField(sys, pk_bdinfo);
		IGeneralAccessor accessor = GeneralAccessorFactory.getAccessor(vo.getPk_bdinfo());
		if ((pk.indexOf("|") < 0) && (!pk.equals(OutEnum.NOSUCHBASEPKATSUBCORP))) {
			datavos = accessor.getChildDocs(pk_org, pk, false);
		}
		return datavos == null ? null : (IBDData[]) datavos.toArray(new IBDData[0]);
	}

	public static ArrayList<IdCtrlformulaVO> getPlanStartAndStopCtrlformulaVO(String pk_cube) throws BusinessException {
		try {
			HashMap<String, IdCtrlformulaVO> map = new HashMap();
			String sWhere = "pk_cube = '" + pk_cube + "'";
			IdCtrlformulaVO[] vos = (IdCtrlformulaVO[]) NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlformulaVO.class, sWhere);
			ArrayList<IdCtrlformulaVO> list = new ArrayList();
			if (vos != null) {
				list.addAll(Arrays.asList(vos));
			}
			return list;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static SingleSchema singleAndSepSchema(String express, DataCell cell, CubeDef def, String scheme_type) throws BusinessException, Exception {
		String srcf = express;
		SingleSchema schema = new SingleSchema(srcf, cell, scheme_type);
		DimFormulaMacro macro = new DimFormulaMacro();
		FormulaDimCI m_env = new FormulaDimCI();
		m_env.setDataCell(cell);
		schema.instanceSchema(srcf);
		srcf = macro.getComplexParsedCorpAndCurrency(m_env, srcf, schema);
		schema.instanceSchema(srcf);
		srcf = macro.getParsedSingleAndComplexFormula(m_env, srcf, schema, scheme_type);
		schema.instanceSchema(srcf);
		return schema;
	}

	public static SingleSchema groupSchema(String express, DataCell cell, CubeDef def) throws BusinessException, Exception {
		String srcf = express;

		SingleSchema schema = new SingleSchema(srcf, cell, "TBRULE00SCHEMA_GROUP");
		DimFormulaMacro macro = new DimFormulaMacro();
		FormulaDimCI m_env = new FormulaDimCI();
		m_env.setDataCell(cell);
		schema.instanceSchema(srcf);

		srcf = macro.getComplexParsedCorpAndCurrency(m_env, srcf, schema);
		schema.instanceSchema(srcf);
		srcf = macro.getParsedGroupFormula(cell, srcf, schema);
		schema.instanceSchema(srcf);

		return schema;
	}

	public static ArrayList<IdCtrlschemeVO> convertIdCtrlscheme(SingleSchema schema, String formulaPk) throws Exception {
		ArrayList<IdCtrlschemeVO> list = new ArrayList();
		String[] src_ufind = schema.getUFind();
		String[] src_prefind = schema.getPREUFind();
		try {
			int count = 0;
			int pre_count = 0;
			for (int n = 0; n < (src_ufind == null ? 0 : src_ufind.length); n++) {
				convertUfindCtrlscheme(src_ufind[n], formulaPk, "UFIND", list, count);
				count++;
			}
			for (int m = 0; m < (src_prefind == null ? 0 : src_prefind.length); m++) {
				convertUfindCtrlscheme(src_prefind[m], formulaPk, "PREFIND", list, pre_count);
				pre_count++;
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}

		return list;
	}

	public static ArrayList convertUfindCtrlscheme(String express, String formulaPk, String methodName, ArrayList<IdCtrlschemeVO> list, int i) throws Exception {
		ConvertToCtrlSchemeVO convertor = new ConvertToCtrlSchemeVO(express, methodName);
		IdCtrlschemeVO schemevos = new IdCtrlschemeVO();

		if ((convertor.getPkOrg().equals("null")) || (convertor.getPkOrg().equals(""))) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000177"));
		}
		schemevos.setPk_org(convertor.getPkOrg());

		if ((convertor.getPkCurrency().equals("null")) || (convertor.getPkCurrency().equals(""))) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000178"));
		}
		schemevos.setPk_currency(convertor.getPkCurrency());

		schemevos.setMethodname(methodName);

		schemevos.setStridx(CtlBdinfoCTL.getAccountActualPk(convertor));
		schemevos.setCtrlsys(convertor.getCtrlSys());
		schemevos.setBilltype(convertor.getBillType());
		schemevos.setCtrldirection(convertor.getCtrlDirection());
		schemevos.setCtrlobj(convertor.getCtrlObject());
		schemevos.setCtrlobjValue(convertor.getCtrlObjectValue());
		schemevos.setIncludeuneffected(convertor.getUneffenctdata());
		schemevos.setStartdate(convertor.getStartDate());
		schemevos.setEnddate(convertor.getEndDate());
		schemevos.setAccctrollflag(convertor.getAccCtrlFlag());
		schemevos.setCurrtype(Integer.valueOf(getCurrencyType(convertor.getPkCurrency())));
		schemevos.setPk_ncentity(convertor.getPkNcentity());
		schemevos.setFromitems(convertor.getFromItem());
		schemevos.setCodeidx(convertor.getCodeIdx());
		schemevos.setCtllevels(convertor.getCtrlLevel());
		schemevos.setSchtype(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000092"));
		schemevos.setSchemetype("TBRULE000SCHEMA_SPEC");
		schemevos.setRundata(new UFDouble(0));
		if ("UFIND".equals(methodName)) {
			schemevos.setVarno("var" + i);
		} else if ("PREFIND".equals(methodName)) {
			schemevos.setVarno("rar" + i);
		}
		schemevos.setNameidx(convertor.getNameIdx());

		schemevos.setDatetype(convertor.getDataCatalg());
		schemevos.setIsstarted(UFBoolean.valueOf("Y"));

		list.add(schemevos);
		return list;
	}

	public static IdCtrlformulaVO convertIdCtrlFormula(DataCell cell, SingleSchema vo, IdCtrlschemeVO[] vos, String formulaPk) throws Exception {
		IdCtrlformulaVO formulavo = new IdCtrlformulaVO();

		String[] src_ufind = vo.getUFind();
		String[] src_prefind = vo.getPREUFind();
		formulavo.setSchemetype(getCtlType(formulaPk));
		String[] temp = vo.getExpressFormula(vos);
		String formulaSrc = temp[0];
		String pkList = temp[1];
		formulavo.setExpressformula(formulaSrc);
		if ((cell.getCellValue() != null) && (cell.getCellValue().getValue() != null)) {

			int index1 = formulaSrc.indexOf("%");
			String left_formula = formulaSrc.substring(0, index1);
			UFDouble complexPlanValue = null;
			try {
				complexPlanValue = getComplexZxs(left_formula + "/100");
			} catch (Exception ex) {
				NtbLogger.print(ex);
				formulavo.setPlanvalue(new UFDouble(0));
			}
			formulavo.setPlanvalue(complexPlanValue);
		} else {
			formulavo.setPlanvalue(new UFDouble(0));
		}

		if ((!formulavo.getSchemetype().equals("TBRULE0SCHEMA_SINGLE")) && (!formulavo.getSchemetype().equals("TBRULE00SCHEMA_GROUP"))) {

			if (formulavo.getSchemetype().equals("TBRULE000SCHEMA_SPEC")) {

				formulavo.setExpressformula(formulaSrc);
				if (cell.getCellValue().getValue() != null) {
					Number cellvalue = cell.getCellValue().getValue();
					UFDouble value = new UFDouble(cellvalue.doubleValue());
					formulavo.setPlanvalue(value);
				} else {
					formulavo.setPlanvalue(new UFDouble(0));
				}
			} else {
				formulavo.setExpressformula(formulaSrc);
				if (cell.getCellValue().getValue() != null) {
					Number cellvalue = cell.getCellValue().getValue();
					UFDouble value = new UFDouble(cellvalue.doubleValue());
					formulavo.setPlanvalue(value);
				} else {
					formulavo.setPlanvalue(new UFDouble(0));
				}
			}
		}

		DimFormulaVO temp_dimformulavo = NtbFormulaCache.getNewInstance().getDimFormulaVOByPk(formulaPk);
		formulavo.setPk_parent(formulaPk);

		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		String pk_cell = cvt.convertToString(cell.getDimVector());
		formulavo.setPk_dimvector(pk_cell);
		formulavo.setPk_cube(cell.getCubeDef().getPrimaryKey());
		formulavo.setCtlmode(String.valueOf(temp_dimformulavo.getAtt() == null ? "" : temp_dimformulavo.getAtt()));
		formulavo.setIsstarted(UFBoolean.valueOf("Y"));

		formulavo.setCtrlname(temp_dimformulavo.getObjname());

		formulavo.setCtrlpercent(vo.getControlpercent(temp_dimformulavo.getFullcontent()));

		formulavo.setCtrlsign(vo.getControlsign());

		formulavo.setPlanlist(pkList);
		formulavo.setSpecialUsage(temp_dimformulavo.getSpecialUsage());

		return formulavo;
	}

	public static HashMap<String, ArrayList<NtbParamVO>> sortVOsBySys(IdCtrlschemeVO[] ctlvos) throws Exception {
		try {
			NtbParamVO[] params = parseCtrls(ctlvos);

			HashMap<String, ArrayList<NtbParamVO>> map = new HashMap();
			for (int i = 0; i < params.length; i++) {
				String sys = params[i].getSys_id();
				if (map.containsKey(sys)) {
					ArrayList<NtbParamVO> list = (ArrayList) map.get(sys);

					list.add(params[i]);
				} else {
					ArrayList<NtbParamVO> list = new ArrayList();
					list.add(params[i]);
					map.put(sys, list);
				}
			}

			return map;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	public static NtbParamVO[] setIncludeEff(IBusiSysExecDataProvider exeprovider, NtbParamVO[] params) throws Exception {
		HashMap hashCorp2Point = new HashMap();
		for (int i = 0; i < params.length; i++) {
			int ctlpoint = 0;
			boolean isIncludeeff = false;
			if (hashCorp2Point.containsKey(params[i].getPk_Org())) {
				ctlpoint = ((Integer) hashCorp2Point.get(params[i].getPk_Org())).intValue();
			} else {
				try {
					ctlpoint = exeprovider.getCtlPoint(params[i].getPk_Org());
				} catch (Exception ex) {
					NtbLogger.error(ex);
					ctlpoint = 0;
				}
				hashCorp2Point.put(params[i].getPk_Org(), Integer.valueOf(ctlpoint));
			}

			if ((ctlpoint == 0) || (params[i].isUnInure())) {
				isIncludeeff = true;
			} else if (ctlpoint == 1) {
				isIncludeeff = false;
			}
			params[i].setIsUnInure(isIncludeeff);
		}

		return params;
	}

	private static NtbParamVO[] parseCtrls(IdCtrlschemeVO[] ctlvos) throws Exception {
		try {
			String spliter = ":";
			IBusiSysReg resaReg = null;
			ArrayList<NtbParamVO> listParams = new ArrayList();
			SubLevelOrgGetter orgLevGetter = new SubLevelOrgGetter();
			for (int i = 0; i < ctlvos.length; i++) {
				NtbParamVO paramvo = new NtbParamVO();
				String funName = null;
				if (ctlvos[i].getMethodname() != null) {
					funName = ctlvos[i].getMethodname();
				}

				String pk_org = ctlvos[i].getPk_org();
				String billtype = parseBillTypes(ctlvos[i].getBilltype());
				String sysId = ctlvos[i].getCtrlsys();
				paramvo.setMethodCode(funName);
				paramvo.setSys_id(sysId);

				if (billtype != null) {
					if (billtype.indexOf(",") <= 0) {
						CtrltacticsCache.getNewInstance();
						HashMap<String, String> actionMap = CtrltacticsCache.getActionByBillTypeAndSysId(paramvo.getSys_id(), parseBillTypes(ctlvos[i].getBilltype()), paramvo.getMethodCode());

						paramvo.setActionMap(actionMap);
						HashMap<String, HashMap<String, String>> _map = new HashMap();
						_map.put(billtype, actionMap);
						paramvo.setBillTypesActionMap(_map);
					} else {
						String[] billtypes = parseBillTypes(ctlvos[i].getBilltype()).split(",");

						HashMap<String, HashMap<String, String>> map = new HashMap();
						for (int n = 0; n < billtypes.length; n++) {
							String _billtype = billtypes[n];
							CtrltacticsCache.getNewInstance();
							HashMap<String, String> actionMap = CtrltacticsCache.getActionByBillTypeAndSysId(paramvo.getSys_id(), _billtype, paramvo.getMethodCode());

							map.put(_billtype, actionMap);
						}
						paramvo.setBillTypesActionMap(map);
					}
				}

				paramvo.setIsUnInure(ctlvos[i].getIncludeuneffected().booleanValue());

				if (OutEnum.RESASYS.equalsIgnoreCase(sysId)) {
					resaReg = getBusiSysReg(OutEnum.RESASYS);
					paramvo.setIsKjqj(resaReg.isUseAccountDate(billtype));

				} else {

					paramvo.setIsKjqj(false);
				}
				if (ctlvos[i].getStartdate() != null) {
					paramvo.setBegDate(ctlvos[i].getStartdate());
					paramvo.setEndDate(ctlvos[i].getEnddate());
				}
				paramvo.setPk_Org(pk_org);
				paramvo.setBill_type(billtype);
				paramvo.setData_attr(ctlvos[i].getCtrlobj());
				paramvo.setData_attrExt(ctlvos[i].getCtrlobjValue());

				dealAccountDate(paramvo);

				paramvo.setPk_ctrl(ctlvos[i].getPrimaryKey());

				paramvo.setGroupname(ctlvos[i].getPk_ctrlformula());

				paramvo.setPk_org_book(pk_org);
				paramvo.setPk_accentity(pk_org);
				String bdinfotype = getBdinfoType(ctlvos[i].getFromitems(), ctlvos[i].getCtrlsys());

				String[] bdinfotypeidx = bdinfotype.split(spliter);

				String[] ctrllevel = ctlvos[i].getCtllevels().split(spliter);

				boolean isControlDownCorp = false;
				for (int j = 0; j < bdinfotypeidx.length; j++) {
					if ((bdinfotypeidx[j].equals(OutEnum.ZJORG)) || (bdinfotypeidx[j].equals(OutEnum.XSOGR))) {
						Boolean value = new Boolean(ctrllevel[j]);
						isControlDownCorp = value.booleanValue();
						break;
					}
				}

				boolean isControlAllCorp = false;

				String pk_currency = getPk_currency(ctlvos[i].getPk_currency(), ctlvos[i].getPk_org(), sysId);

				paramvo.setPk_currency(pk_currency);

				paramvo.setCurr_type(getCurrencyType(ctlvos[i].getPk_currency()));

				paramvo.setSys_id(ctlvos[i].getCtrlsys());

				paramvo.setDateType(ctlvos[i].getDatetype());
				paramvo.setDirection(ctlvos[i].getCtrldirection());

				paramvo.setCtrlstatus(0);

				String[] att = filterStridx(ctlvos[i], bdinfotypeidx);
				if (OutEnum.GLSYS.equals(ctlvos[i].getCtrlsys())) {
					CtlBdinfoCTL.getLinkActualPk(ctlvos[i]);
					paramvo.setPkDim(ctlvos[i].getStridx().split(":"));
				} else {
					paramvo.setPkDim(ctlvos[i].getStridx().split(":"));
				}

				paramvo.setBusiAttrs(att[1].split(spliter));
				String[] ctrllevels = att[2].split(spliter);
				boolean[] value = new boolean[ctrllevels.length];
				HashMap<String, String[]> leveldownMap = new HashMap();
				for (int j = 0; j < ctrllevels.length; j++) {
					value[j] = UFBoolean.valueOf(ctrllevels[j]).booleanValue();

					if (value[j]) {
						if (!paramvo.getPkDim()[j].equals(paramvo.getPk_Org())) {
							String[] levelDowsPks = CtlBdinfoCTL.getBdChilddataVO(paramvo.getPkDim()[j], paramvo.getBusiAttrs()[j], paramvo.getPk_Org(), paramvo.getSys_id(), true);

							leveldownMap.put(paramvo.getBusiAttrs()[j], levelDowsPks);
						} else {
							String[] levelDownPks = orgLevGetter.getSubLevelOrgsByOrgAndBd(paramvo.getPk_Org(), paramvo.getBusiAttrs()[j], paramvo.getSys_id());

							leveldownMap.put(paramvo.getBusiAttrs()[j], levelDownPks);
						}
					}
				}
				paramvo.setLowerArrays(leveldownMap);
				paramvo.setIncludelower(value);
				paramvo.setTypeDim(att[3].split(spliter));
				paramvo.setCode_dims(att[4].split(spliter));
				paramvo.setVarno(ctlvos[i].getVarno());
				paramvo.setCtrlscheme(ctlvos[i].getPrimaryKey());

				if (isControlDownCorp) {
					listParams.add(paramvo);

				} else if (isControlAllCorp) {
					listParams.add(paramvo);
					addGroupDownAllOrgParams(paramvo, listParams, ctlvos[i]);
				} else {
					listParams.add(paramvo);
				}
				validateNtbParamVO(paramvo);
			}
			return (NtbParamVO[]) listParams.toArray(new NtbParamVO[0]);
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	public static String[] filterStridx(IdCtrlschemeVO vo, String[] bdinfotypeidx) throws Exception {
		String sysid = vo.getCtrlsys();
		String[] att = new String[5];
		StringBuffer bf_PkDim = new StringBuffer();
		StringBuffer bf_BusiAttrs = new StringBuffer();
		StringBuffer bf_Includelower = new StringBuffer();
		StringBuffer bf_TypeDim = new StringBuffer();
		StringBuffer bf_Code_dims = new StringBuffer();

		String[] stridx = vo.getStridx().split(":");
		String[] fromitem = vo.getFromitems().split(":");
		String[] ctllevel = vo.getCtllevels().split(":");
		String[] nameidx = vo.getNameidx().split(":");
		String[] codeidx = vo.getCodeidx().split(":");
		String[] bdinfotype = BudgetControlCTL.getBdinfoType(fromitem, sysid).split(":");

		for (int i = 0; i < fromitem.length; i++) {
			if ((!stridx[i].equals(OutEnum.NOSUCHBASEPKATSUBCORP)) || (!bdinfotype[i].equals(OutEnum.CURRDOC))) {
				bf_PkDim.append(stridx[i] + ":");
				bf_BusiAttrs.append(fromitem[i] + ":");
				bf_Includelower.append(ctllevel[i] + ":");
				bf_TypeDim.append(bdinfotypeidx[i] + ":");
				bf_Code_dims.append(codeidx[i] + ":");
			}
		}
		att[0] = bf_PkDim.toString();
		att[1] = bf_BusiAttrs.toString();
		att[2] = bf_Includelower.toString();
		att[3] = bf_TypeDim.toString();
		att[4] = bf_Code_dims.toString();
		return att;
	}

	public static void dealAccountDate(NtbParamVO vo) throws BusinessException {
		String sysid = vo.getSys_id();
		boolean iskjqj = vo.isKjqj();
		IBusiSysReg resaReg = getBusiSysReg(sysid);
		boolean isUseAccountDate = resaReg.isUseAccountDate(vo.getBill_type());
		if ((isUseAccountDate) || (iskjqj)) {

			IGeneralAccessor financeorg_accesssor = GeneralAccessorFactory.getAccessor("2cfe13c5-9757-4ae8-9327-f5c2d34bcb46");

			IBDData financeorg_bddata = financeorg_accesssor.getDocByPk(vo.getPk_Org());

			String pk_accountingBook = null;
			if (OutEnum.RESASYS.equals(vo.getSys_id())) {
				financeorg_accesssor = GeneralAccessorFactory.getAccessor("13a0d3b2-4d5b-4314-9e75-481193f993f2");

				financeorg_bddata = financeorg_accesssor.getDocByPk(vo.getPk_Org());

				if (financeorg_bddata != null) {
					pk_accountingBook = vo.getPk_Org();
				}
			}
			if (financeorg_bddata == null) {
				String start = vo.getBegDate();
				String end = vo.getEndDate();
				String _strat = start.substring(0, 7);
				vo.setBegDate(_strat);
				String _end = end.substring(0, 7);
				vo.setEndDate(_end);
			} else {
				if (pk_accountingBook == null) {
					pk_accountingBook = getPKORGByFINANCEId(vo.getPk_Org());
				}
				if (pk_accountingBook == null) {
					String start = vo.getBegDate();
					String end = vo.getEndDate();
					String _strat = start.substring(0, 7);
					vo.setBegDate(_strat);
					String _end = end.substring(0, 7);
					vo.setEndDate(_end);
				} else {
					String accperiod = null;
					if (OutEnum.RESASYS.equals(vo.getSys_id())) {
						ILiabilityBookPubService bookPubService = (ILiabilityBookPubService) NCLocator.getInstance().lookup(ILiabilityBookPubService.class.getName());

						accperiod = bookPubService.queryAccperiodCalendarIDByLiabilityBookID(pk_accountingBook);
					} else {
						IAccountingBookPubService bookPubService = (IAccountingBookPubService) NCLocator.getInstance().lookup(IAccountingBookPubService.class.getName());

						accperiod = bookPubService.queryAccperiodSchemeByAccountingBookID(pk_accountingBook);
					}

					AccountCalendar accountCalendar = getAccountCalendar(accperiod);
					String start = vo.getBegDate();
					String end = vo.getEndDate();
					if (start.length() > 7) {
						accountCalendar.setDate(new UFDate(start));
						vo.setBegDate(accountCalendar.getMonthVO().getYearmth());
					}
					if (end.length() > 7) {
						accountCalendar.setDate(new UFDate(end));
						vo.setEndDate(accountCalendar.getMonthVO().getYearmth());
					}
				}
			}

			if ((OutEnum.IASYS.equals(sysid)) && ("JC".equals(vo.getBill_type()))) {
				vo.setBegDate(vo.getEndDate());
			}
		}
	}

	private static AccountCalendar getAccountCalendar(String accperiod) throws BusinessException {
		AccountCalendar accountCalendar = AccountCalendar.getInstanceByPeriodScheme(accperiod);
		return accountCalendar;
	}

	private static String getPKORGByFINANCEId(String pk_finance) throws BusinessException {
		String[] pk_orgs = { pk_finance };
		String pk_accountingBook = null;

		IAccountingBookPubService bookPubService = (IAccountingBookPubService) NCLocator.getInstance().lookup(IAccountingBookPubService.class.getName());

		Map<String, String> map = bookPubService.queryAccountingBookIDByFinanceOrgIDWithMainAccountBook(pk_orgs);

		if (map == null) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000207"));
		}

		pk_accountingBook = (String) map.get(pk_finance);
		if (pk_accountingBook == null) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000207"));
		}

		return pk_accountingBook;
	}

	private static int getCurrtypeDigit(String pk_currtype) throws BusinessException {
		if (pk_currtype == null) {
			return 2;
		}
		return CurrtypeQuery.getInstance().getCurrdigit(pk_currtype);
	}

	public static UFDouble sumRunData(UFDouble[] value) {
		UFDouble sum = new UFDouble(0);
		for (int i = 0; i < value.length; i++) {
			sum = sum.add(value[i]);
		}
		return sum;
	}

	public static int getIndex(String src) {
		int index = -1;
		if (src.indexOf(OutEnum.CTLSIGNARR[0]) > -1) {
			return src.indexOf(OutEnum.CTLSIGNARR[0]) + 2;
		}
		if (src.indexOf(OutEnum.CTLSIGNARR[3]) > -1) {
			return src.indexOf(OutEnum.CTLSIGNARR[3]) + 2;
		}
		if (src.indexOf(OutEnum.CTLSIGNARR[1]) > -1) {
			return src.indexOf(OutEnum.CTLSIGNARR[1]) + 1;
		}
		if (src.indexOf(OutEnum.CTLSIGNARR[2]) > -1) {
			return src.indexOf(OutEnum.CTLSIGNARR[2]) + 1;
		}
		if (src.indexOf(OutEnum.CTLSIGNARR[4]) > -1) {
			return src.indexOf(OutEnum.CTLSIGNARR[4]) + 1;
		}
		return index;
	}

	public static String[] compare(NtbParamVO[] paramvos, HashMap map, HashMap map1, ArrayList<CtrlSchemeVO> ctrlvos) throws Exception {
		HashMap<String, ArrayList<NtbParamVO>> paramap = new HashMap();
		Map<String, String> currencyMap = new HashMap();
		Map<String, String> ctrlObjNameMap = new HashMap();
		Map<String, MdTask> taskMap = new HashMap();

		ArrayList info = new ArrayList();
		for (int i = 0; i < paramvos.length; i++) {
			String pk = paramvos[i].getGroupname();
			if (paramap.containsKey(pk)) {
				ArrayList list = (ArrayList) paramap.get(pk);
				list.add(paramvos[i]);
			} else {
				ArrayList list = new ArrayList();
				list.add(paramvos[i]);
				paramap.put(pk, list);
			}
		}
		Iterator iter = map.keySet().iterator();

		while (iter.hasNext()) {
			String ctrlObjName = null;
			String pk = (String) iter.next();
			ArrayList ntbparamlist = (ArrayList) paramap.get(pk);

			if (ntbparamlist != null) {

				NtbParamVO[] paramvo = (NtbParamVO[]) ntbparamlist.toArray(new NtbParamVO[0]);
				ArrayList schemelist = (ArrayList) map.get(pk);
				IdCtrlschemeVO[] schemevo = (IdCtrlschemeVO[]) schemelist.toArray(new IdCtrlschemeVO[0]);

				IdCtrlformulaVO vo = (IdCtrlformulaVO) map1.get(pk);
				String formulaExpress = vo.getExpressformula();
				String pk_plan = vo.getPk_plan();
				IdCtrlschemeVO vos = schemevo[0];
				ArrayList<UFDouble> runls = new ArrayList();
				ArrayList<String> list = new ArrayList();
				int powerInt = 0;
				for (int i = 0; i < schemevo.length; i++) {
					UFDouble rundata = new UFDouble();
					UFDouble readydata = new UFDouble();
					String var = schemevo[i].getVarno();
					for (int j = 0; j < paramvo.length; j++) {
						if ((paramvo[j].getVarno().equals(var)) && (paramvo[j].getRundata() != null)) {
							int currtype = paramvo[j].getCurr_type();
							if (paramvo[j].getRundata()[currtype] != null) {
								rundata = paramvo[j].getRundata()[currtype];
							}
							list.add(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000009"));

							runls.add(rundata);
						}
						if ((paramvo[j].getVarno().equals(var)) && (paramvo[j].getReadydata() != null)) {
							int currtype = paramvo[j].getCurr_type();
							if (paramvo[j].getReadydata()[currtype] != null) {
								readydata = paramvo[j].getReadydata()[currtype];
							}
							list.add(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000010"));

							runls.add(readydata);
						}
					}
					if (formulaExpress.indexOf(var) > -1) {
						formulaExpress = formulaExpress.replaceAll(var, rundata.add(readydata).toString());
					}

					String pk_currency = (String) currencyMap.get(schemevo[i].getPk_currency() + schemevo[i].getPk_org() + schemevo[i].getCtrlsys());
					if (pk_currency == null) {
						pk_currency = getActualPkcurrency(schemevo[i].getPk_currency(), schemevo[i].getPk_org(), schemevo[i].getCtrlsys());

						currencyMap.put(schemevo[i].getPk_currency() + schemevo[i].getPk_org() + schemevo[i].getCtrlsys(), pk_currency);
					}

					powerInt = getCurrtypeDigit(pk_currency);
					ctrlObjName = (String) ctrlObjNameMap.get(schemevo[i].getCtrlsys() + schemevo[i].getCtrlobj());
					if (ctrlObjName == null) {
						ctrlObjName = parseCtrlObjName(schemevo[i].getCtrlsys(), schemevo[i].getCtrlobj());

						ctrlObjNameMap.put(schemevo[i].getCtrlsys() + schemevo[i].getCtrlobj(), ctrlObjName);
					}
				}

				StringBuffer sbStr = new StringBuffer(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007"));

				for (int n = 0; n < list.size(); n++) {
					if (n != list.size() - 1) {
						sbStr.append((String) list.get(n)).append(",");
					} else {
						sbStr.append((String) list.get(n));
					}
				}
				sbStr.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));

				UFDouble sumvalue = sumRunData((UFDouble[]) runls.toArray(new UFDouble[0]));

				String[] ss = vos.getNameidx().split(":");
				StringBuffer buffer = new StringBuffer();

				String entityName = "";
				String entityPk = vos.getPk_org();
				String[] pkidx = vos.getStridx().split(":");
				for (int n = 0; n < pkidx.length; n++) {
					if ((entityPk != null) && (entityPk.equals(pkidx[n]))) {
						entityName = ss[n];
						break;
					}
				}

				MdTask plan = (MdTask) taskMap.get(vo.getPk_plan());
				if (plan == null) {
					plan = TbTaskCtl.getMdTaskByPk(vo.getPk_plan(), true);
					taskMap.put(vo.getPk_plan(), plan);
				}

				String planSysName = getSysNameByCode(plan.getAvabusisystem());

				buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000235", null, new String[] { planSysName })).append(entityName).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000236"));
				for (int i = 0; i < ss.length; i++) {
					buffer.append(ss[i] + "/");
				}
				if (!vo.getSchemetype().equals(Integer.valueOf(2))) {
					buffer.append(vos.getStartdate()).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000237")).append(vos.getEnddate()).append("/");
				}

				buffer.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050"));
				String diminfo = buffer.toString();
				formulaExpress = FormulaParser.parseToNumSrc(formulaExpress);

				UFDouble complexPlanValue = new UFDouble(0);
				UFDouble complexZxsValue = new UFDouble(0);
				String leftValue = null;
				if (vo.getSchemetype().equals("TBRULE000SCHEMA_FLEX")) {
					leftValue = parseFlexAlgorithm(ctrlvos);
					formulaExpress = formulaExpress.replaceAll("FLEXEXPRESS\\(\\)", leftValue);
				} else {
					int index = getIndex(formulaExpress);
					int index1 = formulaExpress.indexOf("/");
					String left_formula = formulaExpress.substring(0, index1);
					complexPlanValue = getComplexZxs(left_formula + "/100");
					String right_formula = formulaExpress.substring(index);
					complexZxsValue = getComplexZxs(right_formula);
				}

				Boolean[] needctl = needCtl(formulaExpress);

				if (!needctl[0].booleanValue()) {
					UFDouble planvalue = new UFDouble(0);
					UFDouble zxsvalue = new UFDouble(0);

					if ((leftValue != null) && (vo.getSchemetype().equals("TBRULE000SCHEMA_FLEX"))) {
						planvalue = new UFDouble(leftValue);
						zxsvalue = sumvalue;
					} else {
						planvalue = complexPlanValue;
						zxsvalue = complexZxsValue;
					}

					boolean isNumber = OutEnum.OCCORAMOUNT.equals(paramvo[0].getData_attr());

					String planname = plan.getObjname();
					IdCtrlInfoVO infovo = RuleServiceGetter.getIBusiRuleQuery().queryCtrlInfoVOByPk(vo.getPk_parent());
					String message = null;
					String[] arrayExpress = formulaExpress.split(vo.getCtrlsign());
					HashMap<String, String> infoMap = BudgetControlCTL.getCtrlInfoMap(arrayExpress, vo, schemevo);
					if ((infovo != null) && (infovo.getInfoexpress() != null)) {
						message = getFinalCtrlInfoMessage(infoMap, infovo.getInfoexpress());
					} else {
						message = getControlHintMessage(vo, planname, diminfo, isNumber, ctrlObjName, planvalue, zxsvalue, sbStr.toString(), true, powerInt);
					}

					info.add(message);
				}
			}
		}
		return (String[]) info.toArray(new String[0]);
	}

	public static IdFlexElementVO getFlexelement(String formulaPk) throws BusinessException {
		StringBuffer str = new StringBuffer();
		str.append(" pk_formula = '").append(formulaPk).append("'");
		SuperVO[] vos = NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdFlexElementVO.class, str.toString());
		if (vos != null) {
			return (IdFlexElementVO) vos[0];
		}
		return null;
	}

	public static UFDouble getExpressValue(String express, DataCell datacell) throws Exception {
		String className = "nc.ms.tb.formula.core.RuleExecuteHelper";
		String methodName = "getExpressResult";
		Object[] objs = { express, datacell };
		Class cls = Class.forName(className);
		Class[] argclass = new Class[objs.length];
		argclass[0] = String.class;
		argclass[1] = DataCell.class;
		Method method = cls.getDeclaredMethod(methodName, argclass);
		Object value = (UFDouble) method.invoke(cls.newInstance(), objs);
		return (UFDouble) value;
	}

	public static String parseFlexAlgorithm(ArrayList<CtrlSchemeVO> ctrlvos) throws Exception {
		String lastFormulaExpress = "";
		CtrlSchemeVO ctrlvo = (CtrlSchemeVO) ctrlvos.get(0);
		DataCell datacell = ctrlvo.getAllotCell();
		String pk_formula = ctrlvo.getPk_formula();

		IdFlexElementVO m_vo = getFlexelement(pk_formula);

		String ruleExpress = m_vo.getFlexruleexpress();
		String baseExpress = m_vo.getFlexbaseexpress();
		String minValue = m_vo.getMinexpress();
		String maxValue = m_vo.getMaxexpress();
		ArrayList<DimFormulaMVO> formulaMList = RuleCacheManager.getNewInstance().getDimFormulaMVOByPk(pk_formula);
		for (int n = 0; n < formulaMList.size(); n++) {
			DimFormulaMVO vo = (DimFormulaMVO) formulaMList.get(n);
			String varNo = vo.getVarno();
			String express = vo.getContent();
			if (ruleExpress.indexOf(varNo) >= 0) {
				UFDouble _value = getExpressValue(express, datacell);
				ruleExpress = ruleExpress.replaceAll(varNo, _value.toString());
			}
			if ((baseExpress != null) && (baseExpress.indexOf(varNo) >= 0)) {
				UFDouble _value = getExpressValue(express, datacell);
				baseExpress = baseExpress.replaceAll(varNo, _value.toString());
			}
			if ((minValue != null) && (minValue.indexOf(varNo) >= 0)) {
				UFDouble _value = getExpressValue(express, datacell);
				minValue = minValue.replaceAll(varNo, _value.toString());
			}
			if ((maxValue != null) && (maxValue.indexOf(varNo) >= 0)) {
				UFDouble _value = getExpressValue(express, datacell);
				maxValue = maxValue.replaceAll(varNo, _value.toString());
			}
		}

		UFDouble leftValue = getComplexZxs(ruleExpress);
		if ((minValue != null) && (!"".equals(minValue))) {
			Boolean result = compareLeftValueAndMinValue(leftValue, new UFDouble(minValue));
			if (result.booleanValue()) {
				return minValue;
			}
		}
		if ((maxValue != null) && (!"".equals(maxValue))) {
			Boolean result = compareLeftValueAndMaxValue(leftValue, new UFDouble(maxValue));
			if (result.booleanValue()) {
				return maxValue;
			}
		}

		UFDouble baseValue = getComplexZxs(baseExpress);
		lastFormulaExpress = analysisFlexZone(pk_formula, baseValue, leftValue, ruleExpress);
		return lastFormulaExpress;
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

	private static String analysisFlexZone(String pk_dimformula, UFDouble baseValue, UFDouble leftValue, String leftExpress) throws Exception {
		IdFlexZoneVO[] vos = getFlexZone(pk_dimformula);
		for (int n = 0; n < (vos == null ? 0 : vos.length); n++) {
			IdFlexZoneVO vo = vos[n];
			UFDouble downValue = vo.getZoneDown();
			if (downValue == null) {
				downValue = new UFDouble(-1.0E16D);
			}
			UFDouble upValue = vo.getZoneUp();
			if (upValue == null) {
				upValue = new UFDouble(1.0E16D);
			}
			int type = vo.getZoneType().intValue();
			if (type == Integer.parseInt(IdFlexAreaTypeEnum.FinalValueType.toCodeString())) {
				StringBuffer expressLeft = new StringBuffer();
				StringBuffer expressRigth = new StringBuffer();
				if (downValue != null) {
					expressLeft.append(downValue).append("<=");
					expressLeft.append(leftValue);
				}
				if (upValue != null) {
					expressRigth.append(leftValue);
					expressRigth.append("<").append(upValue);
				}
				boolean needctl = needCtl(expressLeft.toString(), expressRigth.toString());
				if (needctl) {
					return vo.getPlannum().toString();
				}

			} else {
				StringBuffer expressLeft = new StringBuffer();
				StringBuffer expressRigth = new StringBuffer();
				if (downValue != null) {
					expressLeft.append(downValue).append("<=");
					expressLeft.append(leftValue);
				}
				if (upValue != null) {
					expressRigth.append(leftValue);
					expressRigth.append("<").append(upValue);
				}
				boolean needctl = needCtl(expressLeft.toString(), expressRigth.toString());
				if (needctl) {
					return recursiveFlexZone(vos, n, baseValue, leftExpress).toString();
				}
			}
		}

		return "";
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

	private static UFDouble recursiveFlexZone(IdFlexZoneVO[] vos, int location, UFDouble baseValue, String leftExpress) throws Exception {
		StringBuffer express = new StringBuffer();
		for (int m = 0; m < vos.length; m++) {
			IdFlexZoneVO vo = vos[m];
			Integer type = vo.getZoneType();
			if (Integer.parseInt(IdFlexAreaTypeEnum.PrecentType.toCodeString()) == type.intValue()) {
				if (leftExpress.indexOf("/") > 0) {
					String result = leftExpress.substring(leftExpress.indexOf("/") + 1, leftExpress.lastIndexOf(")"));
					vo.setZoneDown(vo.getZoneDown() == null ? null : vo.getZoneDown().multiply(new UFDouble(result)));
					vo.setZoneUp(vo.getZoneUp() == null ? null : vo.getZoneUp().multiply(new UFDouble(result)));
				}
			}
		}

		for (int n = location; n >= 0; n--) {
			if (n == location) {
				UFDouble value = baseValue.sub(vos[n].getZoneDown() == null ? new UFDouble(0) : vos[n].getZoneDown());
				UFDouble planNum = value.multiply(vos[n].getPlannum());
				express.append(planNum);
				express.append("+");
			} else if (n == 0) {
				UFDouble value = new UFDouble(vos[n].getPlannum());
				express.append(value);
			} else {
				UFDouble value = vos[n].getZoneUp().sub(vos[n].getZoneDown());
				UFDouble planNum = value.multiply(vos[n].getPlannum());
				express.append(planNum);
				express.append("+");
			}
		}
		UFDouble lastValue = calcPlanValue(express.toString());
		return lastValue;
	}

	public static UFDouble calcPlanValue(String src) throws Exception {
		ArrayValue result = null;
		UFDouble value = null;
		NtbContext m_context = new NtbContext();
		result = RefCompilerClient.getExpressionResult(src, m_context);
		Object tmpResult = result.getValue();
		if (tmpResult != null) {
			value = new UFDouble((BigDecimal) tmpResult);
			return value;
		}

		return new UFDouble(0);
	}

	public static IdFlexZoneVO[] getFlexZone(String pk_dimformula) throws BusinessException {
		StringBuffer str = new StringBuffer();
		str.append(" pk_formula = '").append(pk_dimformula).append("'");
		str.append(" order by idx asc");
		SuperVO[] vos = NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdFlexZoneVO.class, str.toString());
		if (vos != null) {
			return (IdFlexZoneVO[]) vos;
		}
		return null;
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

	private static UFDouble getComplexZxs(String complexformula) throws Exception {
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(complexformula, null);

			Object tmpResult = result.getValue();
			BigDecimal bvalue = (BigDecimal) tmpResult;
			Double temp = Double.valueOf(bvalue.doubleValue());
			return new UFDouble(temp);
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return new UFDouble(0);
	}

	public static Boolean[] needCtl(String exp) {
		Boolean[] value = null;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(exp, null);

			value = result.getBoolean();
		} catch (Exception e) {
			NtbLogger.error(e);
		}
		return value;
	}

	public static String[] startCtrlScheme(ArrayList<CtrlSchemeVO> vos) throws BusinessException {
		String[] messages = null;
		messages = CtlSchemeServiceGetter.getICtlScheme().startCtrlScheme(vos);
		return messages;
	}

	public static void startCtrlScheme(String express, DataCell datacell, String pk_formula, String pk_task) {
	}

	public static HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> filterStopCtrlScheme(DataCell[] selectedcells) {
		StringBuffer sWhere_plan = new StringBuffer();

		ArrayList<ArrayList> list = new ArrayList();
		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> ctlmap_cube = null;
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);

		for (int n = 0; n < selectedcells.length; n++) {
			ArrayList<String> tmpList = new ArrayList();
			String pk_cell = cvt.convertToString(selectedcells[n].getDimVector());
			tmpList.add(pk_cell == null ? " " : pk_cell);
			tmpList.add(pk_cell);
			list.add(tmpList);
		}
		try {
			ctlmap_cube = createNtbTempTable(selectedcells[0].getCubeDef(), list);

		} catch (BusinessException ex) {

			NtbLogger.print(ex);
		}
		return ctlmap_cube;
	}

	public static HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> queryCtrlScheme(String sWhere) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().queryCtrlScheme(sWhere);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static void deleteTmpTable(String name) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().deleteTempTable(name);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static void deleteCtrlScheme(Map<String, List<String>> map) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().deleteCtrlScheme(map);
		} catch (BusinessException ex) {
			throw ex;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> createNtbTempTable(CubeDef cube, ArrayList<ArrayList> list) throws BusinessException {
		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> ctlmap_cube = null;
		try {
			String tmpTableName = "NTB_TMP_CUBE_";
			if (cube != null)
				tmpTableName = tmpTableName + cube.getObjcode();
			if (tmpTableName.length() > 30)
				tmpTableName = tmpTableName.substring(0, 29);
			ctlmap_cube = CtlSchemeServiceGetter.getICtlScheme().createNtbTempTable(cube, tmpTableName, list);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
		return ctlmap_cube;
	}

	public static String createNtbTempTable_new(CubeDef cube, String tempTableName, ArrayList<ArrayList> list) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().createNtbTempTable_new(cube, tempTableName, list);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static boolean orgCheck(SuperVO vo, int bdMode, String currPkGroup, String currOrgPk, int nodeType) {
		String pk_org = (String) vo.getAttributeValue("pk_org");
		String pk_group = (String) vo.getAttributeValue("pk_group");
		if (nodeType == 0) {
			switch (bdMode) {
				case 2:
				case 3:
					return "GLOBLE00000000000000".equals(pk_org);
				case 4:
					return ("GLOBLE00000000000000".equals(pk_org)) || ((pk_org != null) && (pk_org.equals(pk_group)));
			}

			return true;
		}

		if (nodeType == 1) {
			switch (bdMode) {
				case 2:
					return ("GLOBLE00000000000000".equals(pk_org)) || ((currPkGroup != null) && (currPkGroup.equals(pk_org))) || ((currPkGroup != null) && (currPkGroup.equals(pk_group)));

				case 3:
					return ("GLOBLE00000000000000".equals(pk_org)) || ((currPkGroup != null) && (currPkGroup.equals(pk_org)));

				case 1:
				case 4:
					return false;
			}
			return true;
		}

		switch (bdMode) {
			case 1:
			case 2:
				return false;
			case 3:
				return ("GLOBLE00000000000000".equals(pk_org)) || ((currPkGroup != null) && (currPkGroup.equals(pk_org))) || ((currOrgPk != null) && (currOrgPk.equals(pk_org)));

			case 4:
				return ("GLOBLE00000000000000".equals(pk_org)) || ((currOrgPk != null) && (currOrgPk.equals(pk_org))) || ((pk_org != null) && (pk_org.equals(pk_group)));
		}

		return true;
	}

	public static String[] replaceDataCellAttributeItem(DataCell env, IdBdcontrastVO[] bdcontrast, String sysid) throws BusinessException {
		StringBuffer fromitem = new StringBuffer();
		StringBuffer nameidx = new StringBuffer();
		StringBuffer codeidx = new StringBuffer();
		StringBuffer ctrllevel = new StringBuffer();
		StringBuffer startTime = new StringBuffer();
		StringBuffer endTime = new StringBuffer();
		StringBuffer typeitem = new StringBuffer();
		StringBuffer pkidx = new StringBuffer();
		String[] src = new String[9];

		DimMember[] dimmember = (DimMember[]) env.getDimVector().getDimMembers().toArray(new DimMember[0]);
		try {
			for (int i = 0; i < dimmember.length; i++) {
				if (!OutEnum.LISTTYPE.contains(dimmember[i].getDimDef().getPrimaryKey())) {

					if (bdcontrast != null) {
						String classId = CtrlRuleCTL.getClassIDByDimLevel(dimmember[i].getLevelValue());
						String pk_dimlevel = dimmember[i].getDimLevel().getPrimaryKey();
						ArrayList<IdBdcontrastVO> resultVo = new ArrayList();

						for (int j = 0; j < bdcontrast.length; j++) {
							DimRelUapVO vo = BdContrastCache.getNewInstance().getRelUapVOByPK(bdcontrast[j].getPrimaryKey());
							if ((vo != null) && (vo.getPk_Dimlevel() != null) && (pk_dimlevel.equals(vo.getPk_Dimlevel()))) {
								resultVo.add(bdcontrast[j]);
								break;
							}
						}

						if (resultVo.size() == 0) {
							for (int j = 0; j < bdcontrast.length; j++) {
								if (((classId != null) && (classId.equals(bdcontrast[j].getPk_bdinfo()))) || ((OutEnum.HRPSYS.equals(sysid)) && ("TB_DIMLEV_ENTITYUNIT".equals(classId)) && ("a0ec952c-e4e5-416a-b3e0-d402725f76be".equals(bdcontrast[j].getPk_bdinfo())))) {
									resultVo.add(bdcontrast[j]);
								}
							}
						}
						for (int n = 0; n < resultVo.size(); n++) {
							fromitem.append(((IdBdcontrastVO) resultVo.get(n)).getAtt_fld() + ":");
							nameidx.append(dimmember[i].getLevelValue().getName() + ":");
							if (dimmember[i].getLevelValue().getCode() != null) {
								codeidx.append(dimmember[i].getLevelValue().getCode() + ":");
							} else {
								codeidx.append("null:");
							}
							if (dimmember[i].getLevelValue().getKey() != null) {
								pkidx.append(dimmember[i].getLevelValue().getKey() + ":");
							} else {
								pkidx.append("null:");
							}
							ctrllevel.append(((IdBdcontrastVO) resultVo.get(n)).getLevelctlflag() + ":");
							typeitem.append(((IdBdcontrastVO) resultVo.get(n)).getBdinfo_type() + ":");
						}
					}
				}
			}
			String[] time = TimeDimTool.getStartEndDataByDataCell(env, false);
			startTime.append(time[0]);
			endTime.append(time[1]);

			src[0] = fromitem.toString();
			src[1] = nameidx.toString();
			src[2] = codeidx.toString();
			src[3] = ctrllevel.toString();
			src[4] = startTime.toString();
			src[5] = endTime.toString();
			src[6] = typeitem.toString();
			src[7] = pkidx.toString();
			src[8] = time[2];
		} catch (BusinessException ex) {
			NtbLogger.error(ex);
			throw ex;
		}
		return src;
	}

	public static NtbParamVO[] convertDataCell2NtbParamVO(List<DataCell> selectcells, String pk_task, String sysid) throws BusinessException {
		ArrayList<NtbParamVO> ntbParam = new ArrayList();
		MdTask plan = TbTaskCtl.getMdTaskByPk(pk_task, true);
		for (int n = 0; n < selectcells.size(); n++) {
			DataCell datacell = (DataCell) selectcells.get(n);
			if (datacell != null) {
				NtbParamVO paramvo = new NtbParamVO();
				DimVector dv = datacell.getDimVector();
				DimMember[] dimmember = (DimMember[]) dv.getDimMembers().toArray(new DimMember[0]);
				ArrayList<IdBdcontrastVO> voAllList = new ArrayList();
				IdBdcontrastVO[] vos = BdContrastCache.getNewInstance().getVoBySysid(sysid);
				voAllList.addAll(Arrays.asList(vos));
				String[] strArrays = replaceDataCellAttributeItem(datacell, (IdBdcontrastVO[]) voAllList.toArray(new IdBdcontrastVO[0]), sysid);
				for (int m = 0; m < dimmember.length; m++) {
					DimMember dim = dimmember[m];

					if ("TB_DIMDEF_ENTITY_000".equals(dim.getDimDef().getPrimaryKey())) {
						paramvo.setPk_Org((String) dim.getLevelValue().getKey());
					}

					if ("TB_DIMDEF_CURR_00000".equals(dim.getDimDef().getPrimaryKey())) {
						paramvo.setPk_currency((String) dim.getLevelValue().getKey());
						paramvo.setCurr_type(getCurrencyType((String) dim.getLevelValue().getKey()));
					}
				}
				paramvo.setBusiAttrs(strArrays[0].split(":"));
				paramvo.setTypeDim(strArrays[1].split(":"));
				paramvo.setCode_dims(strArrays[2].split(":"));
				paramvo.setPkDim(strArrays[7].split(":"));
				paramvo.setBegDate(strArrays[4]);
				paramvo.setEndDate(strArrays[5]);
				paramvo.setPk_plan(pk_task);
				paramvo.setHrAccountTime(strArrays[8]);

				IDimManager dm = DimServiceGetter.getDimManager();
				DimMember dimm = datacell.getDimVector().getDimMember(dm.getDimDefByPK("TB_DIMDEF_MEASURE_00"));
				if ((dimm != null) && (dimm.getLevelValue().getKey().equals(OutEnum.HRPAMOUNT))) {
					paramvo.setData_attr(OutEnum.HRPAMOUNT);
				} else if ((dimm != null) && (dimm.getLevelValue().getKey().equals(OutEnum.HRPNUMBER))) {
					paramvo.setData_attr(OutEnum.HRPNUMBER);
				}

				paramvo.setPlanname(plan.getObjname());
				paramvo.setPk_Group(plan.getPk_group());
				String _pk_currency = plan.getPk_currency() == null ? "" : plan.getPk_currency();
				String pk_entity = plan.getPk_planent() == null ? "" : plan.getPk_planent();
				if ((!"TBDIMM1GLOBECURRENCY".equals(_pk_currency)) && (!"TBDIMM1GROUPCURRENCY".equals(_pk_currency)) && (!"TBDIMM1000BUCURRENCY".equals(_pk_currency))) {

					_pk_currency = plan.getPk_currency() == null ? "" : plan.getPk_currency();
				}
				String pk_currency = getPk_currency(_pk_currency, pk_entity, sysid);
				paramvo.setPk_currency(pk_currency);
				if (datacell.getCellValue().getValue() == null) {
					paramvo.setPlanData(null);
				} else {
					double value = datacell.getCellValue().getValue().doubleValue();
					paramvo.setPlanData(new UFDouble(value));
				}
				paramvo.setCreatePlanTime(plan.getCreationtime());

				ntbParam.add(paramvo);
			}
		}
		if (ntbParam.size() == 0) {
			return null;
		}
		return (NtbParamVO[]) ntbParam.toArray(new NtbParamVO[0]);
	}

	public static NtbParamVO[] convertDataCell2NtbParamVO(List<DataCell> selectcells, MdTask plan, String pk_sheet, String button_code, String sysId) throws Exception {
		List<NtbParamVO> ntbParam = new ArrayList();
		for (DataCell datacell : selectcells) {
			NtbParamVO paramvo = convertDataCell2NtbParamVO(datacell, plan, pk_sheet, button_code, sysId, new ArrayList());
			ntbParam.add(paramvo);
		}
		if (ntbParam.size() == 0) {
			return null;
		}
		return (NtbParamVO[]) ntbParam.toArray(new NtbParamVO[0]);
	}

	public static NtbParamVO[] convertDataCell2NtbParamVO(List<Cell> selectcells, String pk_task, String pk_sheet, String button_code, String sysid, ArrayList<String> busiSysList) throws Exception {
		ArrayList<NtbParamVO> ntbParam = new ArrayList();
		MdTask plan = TbTaskCtl.getMdTaskByPk(pk_task, true);
		for (int n = 0; n < selectcells.size(); n++) {
			Cell cell = (Cell) selectcells.get(n);
			DataCell datacell = cell == null ? null : (DataCell) cell.getExtFmt("dc");
			if (datacell != null) {

				NtbParamVO paramvo = convertDataCell2NtbParamVO(datacell, plan, pk_sheet, button_code, sysid, busiSysList);
				ntbParam.add(paramvo);
			}
		}
		if (ntbParam.size() == 0) {
			return null;
		}
		return (NtbParamVO[]) ntbParam.toArray(new NtbParamVO[0]);
	}

	public static NtbParamVO[] convertDataCell2NtbParamVO(List<Cell> selectcells, MdTask plan, String pk_sheet, String button_code, String sysid, ArrayList<String> busiSysList) throws Exception {
		ArrayList<NtbParamVO> ntbParam = new ArrayList();
		for (int n = 0; n < selectcells.size(); n++) {
			Cell cell = (Cell) selectcells.get(n);
			DataCell datacell = cell == null ? null : (DataCell) cell.getExtFmt("dc");
			if (datacell != null) {

				NtbParamVO paramvo = convertDataCell2NtbParamVO(datacell, plan, pk_sheet, button_code, sysid, busiSysList);
				ntbParam.add(paramvo);
			}
		}
		if (ntbParam.size() == 0) {
			return null;
		}
		return (NtbParamVO[]) ntbParam.toArray(new NtbParamVO[0]);
	}

	private static NtbParamVO convertDataCell2NtbParamVO(DataCell datacell, MdTask plan, String pk_sheet, String button_code, String sysid, ArrayList<String> busiSysList) throws Exception {
		NtbParamVO paramvo = new NtbParamVO();
		DimVector dv = datacell.getDimVector();
		DimMember[] dimmember = (DimMember[]) dv.getDimMembers().toArray(new DimMember[0]);
		ArrayList<IdBdcontrastVO> voAllList = new ArrayList();
		IdBdcontrastVO[] vos = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		if ((vos != null) && (vos.length > 0))
			voAllList.addAll(Arrays.asList(vos));
		for (int m = 0; m < busiSysList.size(); m++) {
			IdBdcontrastVO[] vTmps = BdContrastCache.getNewInstance().getVoBySysid((String) busiSysList.get(m));
			if ((vos != null) && (vos.length > 0))
				voAllList.addAll(Arrays.asList(vTmps));
		}
		IdBdcontrastVO[] voAllArr = voAllList == null ? null : (IdBdcontrastVO[]) voAllList.toArray(new IdBdcontrastVO[0]);
		String[] strArrays = replaceDataCellAttributeItem(datacell, voAllArr, sysid);

		DimHierarchy dimhier = datacell.getCubeDef().getDimHierarchy("TB_DIMDEF_TIME_00000");
		List<DimLevel> dimLevs = dimhier.getDimLevels();

		DimLevel bottomLevel = null;
		if ((dimLevs != null) && (dimLevs.size() > 0)) {
			for (int i = dimLevs.size() - 1; i >= 0; i--) {
				LevelValue value = datacell.getDimVector().getLevelValue((DimLevel) dimLevs.get(i));
				if (value != null) {
					bottomLevel = (DimLevel) dimLevs.get(i);
					break;
				}
			}
		}

		if (bottomLevel != null) {
			paramvo.setTbbDateType(bottomLevel.getObjCode());
		}
		for (int m = 0; m < dimmember.length; m++) {
			DimMember dim = dimmember[m];

			if ("TB_DIMDEF_ENTITY_000".equals(dim.getDimDef().getPrimaryKey())) {
				paramvo.setPk_Org((String) dim.getLevelValue().getKey());
			}

			if ("TB_DIMDEF_CURR_00000".equals(dim.getDimDef().getPrimaryKey())) {
				paramvo.setPk_currency((String) dim.getLevelValue().getKey());
				paramvo.setCurr_type(getCurrencyType((String) dim.getLevelValue().getKey()));
			}
		}
		if ((strArrays[0] != null) && (!"".equals(strArrays[0]))) {
			paramvo.setBusiAttrs(strArrays[0].split(":"));
			paramvo.setTypeDim(strArrays[1].split(":"));
			paramvo.setCode_dims(strArrays[2].split(":"));
			paramvo.setPkDim(strArrays[7].split(":"));
		}
		paramvo.setBegDate(strArrays[4]);
		paramvo.setEndDate(strArrays[5]);
		paramvo.setPk_plan(plan.getPrimaryKey());

		IDimManager dm = DimServiceGetter.getDimManager();
		DimMember dimm = datacell.getDimVector().getDimMember(dm.getDimDefByPK("TB_DIMDEF_MEASURE_00"));
		if ((dimm != null) && (dimm.getLevelValue().getKey().equals("TBPTZ410000000001DDX"))) {
			paramvo.setData_attr("CG_AMOUNT");
		} else if ((dimm != null) && (dimm.getLevelValue().getKey().equals("TBPTZ410000000001DDW"))) {
			paramvo.setData_attr("CG_NUMBER");
		}
		paramvo.setPk_measure(dimm.getUniqKey());

		paramvo.setPlanname(plan.getObjname());
		paramvo.setPk_Group(plan.getPk_group());
		String _pk_currency = plan.getPk_currency() == null ? "" : plan.getPk_currency();
		String pk_entity = plan.getPk_planent() == null ? "" : plan.getPk_planent();
		if ((!"TBDIMM1GLOBECURRENCY".equals(_pk_currency)) && (!"TBDIMM1GROUPCURRENCY".equals(_pk_currency)) && (!"TBDIMM1000BUCURRENCY".equals(_pk_currency))) {

			_pk_currency = plan.getPk_currency() == null ? "" : plan.getPk_currency();
		}
		String pk_currency = getPk_currency(_pk_currency, pk_entity, sysid);
		paramvo.setPk_currency(pk_currency);
		paramvo.setButton_code(button_code);
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		String pk_datacell = cvt.convertToString(datacell.getDimVector());
		paramvo.setPk_datacell(pk_sheet);
		double value = datacell.getCellValue().getValue() == null ? 0.0D : datacell.getCellValue().getValue().doubleValue();
		paramvo.setPlanData(new UFDouble(value));
		paramvo.setCreatePlanTime(plan.getCreationtime());

		if ((!OutEnum.MPPSYS.equals(sysid)) && (!OutEnum.FPSYS.equals(sysid)) && (!OutEnum.SOPSYS.equals(sysid))) {
			StringBuffer sWhere_cube = new StringBuffer();
			sWhere_cube.append("pk_dimvector in (");
			sWhere_cube.append("'").append(pk_datacell).append("'");
			sWhere_cube.append(") and isstarted = 'Y' and pk_plan = '");
			sWhere_cube.append(plan.getPrimaryKey());
			sWhere_cube.append("' and pk_parent is not null");
			UFDouble values = new UFDouble();
			HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> tempNotStartCtrlscheme = queryCtrlScheme(sWhere_cube.toString());
			if (tempNotStartCtrlscheme.size() > 0) {
				Iterator iter = tempNotStartCtrlscheme.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					ArrayList<IdCtrlschemeVO> schemevos = (ArrayList) entry.getValue();
					IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
					String expressformula = vo.getExpressformula();
					for (int a = 0; a < schemevos.size(); a++) {
						UFDouble tmpValue = new UFDouble();
						expressformula = expressformula.replaceAll(((IdCtrlschemeVO) schemevos.get(a)).getVarno(), tmpValue.add(((IdCtrlschemeVO) schemevos.get(a)).getReadydata()).add(((IdCtrlschemeVO) schemevos.get(a)).getRundata()).toString());
					}
					expressformula = expressformula.replaceAll(">=", "-");
					UFDouble _value = BudgetControlCTL.getComplexZxs(expressformula);
					values = values.add(_value);
				}
				paramvo.setPlanData(values);
			}
		}
		return paramvo;
	}

	public static void createBillType(NtbParamVO[] ntbParamvos, String syscode) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().createBillType(ntbParamvos, syscode);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000164"), e);
		}
	}

	public static void startControlSchemeWhenRevEffected(MdTask[] tasks) throws BusinessException {
		ArrayList<MdTask> comPlanList = new ArrayList();
		ArrayList<MdTask> adjPlanList = new ArrayList();
		for (MdTask task : tasks) {
			String status = task.getVersionstatus();
			if ("TBPTZ410000000001CME".equals(task.getPk_mvtype())) {

				if (("00".equals(status)) || ("10".equals(status)) || ("02".equals(status))) {

					comPlanList.add(task);
				} else if (("16".equals(status)) || ("06".equals(status)) || ("08".equals(status)) || ("18".equals(status))) {

					adjPlanList.add(task);
				}
			}
		}
		try {
			if (!comPlanList.isEmpty()) {
				UFBoolean bb = UtilServiceGetter.getIUtil().getParaBoolan("GLOBLE00000000000000", "TBB017");
				if ((bb != null) && (bb.booleanValue())) {
					MdTask[] comPlanArr = (MdTask[]) comPlanList.toArray(new MdTask[0]);
					onUseAllCrelSchemeInClient(comPlanArr);
				}
			}

			if (!adjPlanList.isEmpty()) {
				UFBoolean bb = UtilServiceGetter.getIUtil().getParaBoolan("GLOBLE00000000000000", "TBB017");
				if ((bb != null) && (bb.booleanValue())) {
					MdTask[] comPlanArr = (MdTask[]) adjPlanList.toArray(new MdTask[0]);
					onUseAllCrelSchemeInClient(comPlanArr);
				}
			}

			if (!adjPlanList.isEmpty()) {
				updateExistCtrlSchemeFind(adjPlanList);
			}
		} catch (Exception e) {
			throw new BusinessException(e.getMessage());
		}
	}

	public static HashMap<String, HashMap<DataCell, ArrayList<String>>> getCubeStartCtrlformulaVO(MdTask[] plans) throws BusinessException {
		try {
			String tmpTableName = "NTB_TMP_CUBE_" + RandomStringUtils.randomNumeric(3);
			ArrayList<ArrayList> _list = new ArrayList();
			for (int n = 0; n < plans.length; n++) {
				ArrayList<String> tmpList = new ArrayList();
				tmpList.add(plans[n].getPrimaryKey() == null ? " " : plans[n].getPrimaryKey());
				tmpList.add(plans[n].getPrimaryKey() == null ? " " : plans[n].getPrimaryKey());
				_list.add(tmpList);
			}
			createNtbTempTable(null, _list);
			StringBuffer sWhere_cube = new StringBuffer();
			sWhere_cube.append("pk_plan in (");
			sWhere_cube.append("select DATACELLCODE from ").append(tmpTableName);
			sWhere_cube.append(") and pk_parent is not null");
			SuperVO[] vos = NtbSuperServiceGetter.getINtbSuper().queryByCondition(IdCtrlformulaVO.class, sWhere_cube.toString());
			deleteTmpTable(tmpTableName);

			HashMap<String, HashMap<DataCell, ArrayList<String>>> map = new HashMap();

			if ((vos != null) && (vos.length > 0)) {
				for (int i = 0; i < vos.length; i++) {
					IdCtrlformulaVO vo = (IdCtrlformulaVO) vos[i];
					String pk_cubeformula = vo.getPk_parent();
					String pk_plan = vo.getPk_plan();
					String pk_cube = vo.getPk_cube();
					String dimcode = vo.getPk_dimvector();
					HashMap<DataCell, ArrayList<String>> tmpMap = (HashMap) map.get(pk_plan);
					DataCell tempcell = (DataCell) getDataCellByDimVector(pk_cube, new String[] { dimcode }).get(dimcode);
					if (tmpMap == null) {
						tmpMap = new HashMap();
						map.put(pk_plan, tmpMap);
					}
					if (tmpMap.containsKey(tempcell)) {
						ArrayList<String> list = (ArrayList) tmpMap.get(tempcell);
						list.add(pk_cubeformula);
					} else {
						ArrayList<String> list = new ArrayList();
						list.add(pk_cubeformula);
						tmpMap.put(tempcell, list);
					}
				}
			}
			return map;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static HashMap<String, DataCell> getDataCellByDimVector(String pk_cubeDef, String[] src) throws Exception {
		IDataSetService idss = CubeServiceGetter.getDataSetService();
		CubeDef cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(pk_cubeDef);
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		ArrayList<DimVector> dimvectors = new ArrayList();
		HashMap<String, DataCell> map = new HashMap();
		for (int n = 0; n < src.length; n++) {
			String pk_dimvector = src[n];
			DimVector dimvector = (DimVector) cvt.fromString(pk_dimvector);
			dimvectors.add(dimvector);
		}
		ICubeDataSet dataSet = idss.queryDataSet(cubedef, dimvectors);
		for (int n = 0; n < src.length; n++) {
			DataCell datacell = dataSet.getDataCell((DimVector) dimvectors.get(n));
			if (map.get(src[n]) == null) {
				map.put(src[n], datacell);
			}
		}
		return map;
	}

	public static HashMap<String, DataCell> getDataCellPkCubeByDimVector(String pk_cubeDef, String[] src) throws Exception {
		IDataSetService idss = CubeServiceGetter.getDataSetService();
		CubeDef cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(pk_cubeDef);
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		ArrayList<DimVector> dimvectors = new ArrayList();
		HashMap<String, DataCell> map = new HashMap();
		for (int n = 0; n < src.length; n++) {
			String pk_dimvector = src[n];
			DimVector dimvector = (DimVector) cvt.fromString(pk_dimvector);
			dimvectors.add(dimvector);
		}
		ICubeDataSet dataSet = idss.queryDataSet(cubedef, dimvectors);
		for (int n = 0; n < src.length; n++) {
			DataCell datacell = dataSet.getDataCell((DimVector) dimvectors.get(n));
			if (map.get(src[n]) == null) {
				map.put(pk_cubeDef + src[n], datacell);
			}
		}
		return map;
	}

	public static HashMap<String, DataCell> getDataCellPkCubeByDimVector(String pk_cubeDef, String[] src, HashMap<String, HashMap<DimVector, DataCellValue>> cubeMap) throws Exception {
		IDataSetService idss = CubeServiceGetter.getDataSetService();
		CubeDef cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(pk_cubeDef);
		IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
		ArrayList<DimVector> dimvectors = new ArrayList();
		HashMap<String, DataCell> map = new HashMap();
		for (int n = 0; n < src.length; n++) {
			String pk_dimvector = src[n];
			DimVector dimvector = (DimVector) cvt.fromString(pk_dimvector);
			dimvectors.add(dimvector);
		}
		ICubeDataSet dataSet = idss.queryDataSet(cubedef, dimvectors);
		for (int n = 0; n < src.length; n++) {
			DataCell datacell = dataSet.getDataCell((DimVector) dimvectors.get(n));
			DataCellValue value = null;
			HashMap<DimVector, DataCellValue> valueMap = (HashMap) cubeMap.get(pk_cubeDef);
			if (valueMap != null) {
				value = (DataCellValue) valueMap.get(dimvectors.get(n));
			}
			if (value != null)
				datacell.setCellValue(value);
			if (map.get(src[n]) == null) {
				map.put(pk_cubeDef + src[n], datacell);
			}
		}
		return map;
	}

	public static HashMap<String, List<AllotFormulaVo>> allotFormula(MdTask[] plans) throws BusinessException {
		HashMap<String, List<AllotFormulaVo>> map = new HashMap();
		for (int n = 0; n < plans.length; n++) {
			List<AllotFormulaVo> formulaVO = new ArrayList();
			ArrayList<BusiRuleVO> allRules = RuleServiceGetter.getIBusiRuleQuery().queryByRuleAndMdWorkbook(plans[n].getPk_workbook());
			DimSectionTuple defaultSectionTuple = TbTaskCtl.getTaskParadim(plans[n]);

			if ((allRules != null) && (!allRules.isEmpty())) {

				TaskDataModel taskDataModel = TaskDataCtl.getTaskDataModel(plans[n].getPrimaryKey(), null, false, null);

				formulaVO = TbTaskServiceGetter.getTaskRuleExecuteAdapter().getCtrlAllotForWorkBook(taskDataModel.getMdTask(), allRules);
			}

			map.put(plans[n].getPrimaryKey(), formulaVO);
		}
		return map;
	}

	public static void onUseAllCrelSchemeInClient(MdTask[] plans) throws BusinessException {
		ArrayList<String> alPKs = new ArrayList();
		ArrayList<DataCell[]> alDataCells = new ArrayList();

		HashMap<String, List<AllotFormulaVo>> formulaVO = new HashMap();
		stopCtrlScheme(plans);
		formulaVO = allotFormula(plans);
		ArrayList<CtrlSchemeVO> list = new ArrayList();
		for (int m = 0; m < plans.length; m++) {
			String pk_plan = plans[m].getPrimaryKey();
			List<AllotFormulaVo> vos = (List) formulaVO.get(plans[m].getPrimaryKey());
			for (int n = 0; n < vos.size(); n++) {
				AllotFormulaVo vo = (AllotFormulaVo) vos.get(n);
				String express = vo.getCellExpress().replaceAll("'", "\"");
				String formulaBd = formatBudgetData(2, express);
				CtrlSchemeVO schemevo = new CtrlSchemeVO(formulaBd, vo.getFormulaVoPk(), vo.getAllotCell(), pk_plan);
				list.add(schemevo);
			}
		}
		startCtrlScheme(list);
	}

	public static String formatBudgetData(int digit, String formula) {
		String planData = formula.substring(0, formula.indexOf('*'));
		boolean isDigit = true;
		if (planData.indexOf("FLEXEXPRESS") >= 0) {
			isDigit = false;
		}
		try {
			Double.parseDouble(planData);
		} catch (NumberFormatException e) {
			isDigit = false;
		}

		if (isDigit) {
			UFDouble data = new UFDouble(planData).setScale(digit, 4);
			String anoFormula = formula.replaceFirst(planData, data.toString());
			return anoFormula;
		}

		return formula;
	}

	public static String asynStartControl(MdTask[] plans) throws BusinessException {
		IPreAlertConfigService access = (IPreAlertConfigService) NCLocator.getInstance().lookup(IPreAlertConfigService.class.getName());
		try {
			access.startReportLikeWork(new AsynStartContrlWork(plans), null);
		} catch (BusinessException e) {
			throw new BusinessException(e);
		}
		return null;
	}

	public static void stopCtrlScheme(MdTask[] plans) throws BusinessException {
		try {
			StringBuffer sWhere = new StringBuffer();
			sWhere.append("pk_plan in (");
			for (int i = 0; i < plans.length; i++) {
				sWhere.append("'");
				sWhere.append(plans[i].getPrimaryKey());
				sWhere.append("'");
				if (i != plans.length - 1) {
					sWhere.append(",");
				}
			}
			sWhere.append(") and isstarted = 'Y'");
			String sWhere_cube = sWhere.toString() + " and pk_parent is not null";
			String sWhere_plan = sWhere.toString() + " and " + VoConvertor.getIsNullSql("pk_parent");

			Map<String, List<String>> ctlmap_Cube = queryCtrlSchemeSimply(sWhere_cube);

			if ((ctlmap_Cube != null) && (!ctlmap_Cube.isEmpty())) {
				deleteCtrlScheme(ctlmap_Cube);

			}

		} catch (BusinessException ex) {

			throw ex;
		} catch (Exception e) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static Map<String, List<String>> queryCtrlSchemeSimply(String sWhere) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().queryCtrlSchemeSimply(sWhere);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			NtbLogger.error(e);
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000052"));
		}
	}

	public static void updateCtrlSchemeVOs(HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> notStartCtrlscheme) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().updateCtrlSchemeVOs(notStartCtrlscheme);
		} catch (BusinessException ex) {
			throw ex;
		}
	}

	public static void updateExistCtrlSchemeFind(ArrayList<MdTask> planLists) throws Exception {
		CountTimeCost.clear();
		CountTimeCost cost = new CountTimeCost();
		cost.beginCost();
		StringBuffer sWhere = new StringBuffer();
		sWhere.append("isstarted = 'Y' and pk_plan in ('");
		for (int n = 0; n < (planLists == null ? 0 : planLists.size()); n++) {
			if (n != planLists.size() - 1) {
				sWhere.append(((MdTask) planLists.get(n)).getPrimaryKey()).append("','");
			} else {
				sWhere.append(((MdTask) planLists.get(n)).getPrimaryKey());
			}
		}
		sWhere.append("')");
		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> ctrlscheme = queryCtrlScheme(sWhere.toString());

		String sqlWhere = " select pk_dimvector from tb_ctrlformula where " + sWhere;
		StringBuffer _sWhere = new StringBuffer();
		_sWhere.append("isstarted = 'Y' and pk_dimvector in (").append(sqlWhere).append(")");

		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> _ctrlscheme = queryCtrlScheme(_sWhere.toString());
		ctrlscheme.putAll(_ctrlscheme);

		HashMap<String, DataCell> map = new HashMap();
		HashMap<String, ArrayList<String>> pkMap = new HashMap();
		Iterator _iter = ctrlscheme.entrySet().iterator();
		while (_iter.hasNext()) {
			Map.Entry entry = (Map.Entry) _iter.next();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
			if (pkMap.get(vo.getPk_cube()) == null) {
				ArrayList<String> list = new ArrayList();
				list.add(vo.getPk_dimvector());
				pkMap.put(vo.getPk_cube(), list);
			} else {
				((ArrayList) pkMap.get(vo.getPk_cube())).add(vo.getPk_dimvector());
			}
		}
		Iterator iterPkMap = pkMap.entrySet().iterator();
		while (iterPkMap.hasNext()) {
			Map.Entry entry = (Map.Entry) iterPkMap.next();
			String str = (String) entry.getKey();
			ArrayList<String> sList = (ArrayList) entry.getValue();
			map.putAll(getDataCellPkCubeByDimVector(str, (String[]) sList.toArray(new String[0])));
		}

		ArrayList<IdCtrlformulaVO> updatevo = new ArrayList();
		HashMap<String, DimFormulaVO> formulaMap = new HashMap();
		Iterator iter = ctrlscheme.entrySet().iterator();
		parserMap.clear();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
			if (formulaMap.get(vo.getPk_parent()) == null) {
				DimFormulaVO dimformulavo = FormulaCTL.getDimFormulaByPrimaryKey(vo.getPk_parent());
				formulaMap.put(vo.getPk_parent(), dimformulavo);
			}
			ArrayList<IdCtrlschemeVO> vos = (ArrayList) entry.getValue();
			IdCtrlschemeVO[] schemeArr = getLinkedSchemeVOs(vos);
			String express = againCalculate((DataCell) map.get(vo.getPk_cube() + vo.getPk_dimvector()), schemeArr, (DimFormulaVO) formulaMap.get(vo.getPk_parent()));
			express = formatBudgetData(2, express);
			vo.setExpressformula(express);
			vo.setPlanvalue(getPlanValue(express, vos));
			updatevo.add(vo);
		}

		cost.addCost("ctrl totle1", cost.getCost());
		CountTimeCost.reportCost();

		CtlSchemeServiceGetter.getICtlScheme().updateCtrl((IdCtrlformulaVO[]) updatevo.toArray(new IdCtrlformulaVO[0]));
	}

	public static IdCtrlschemeVO[] getLinkedSchemeVOs(List<IdCtrlschemeVO> schemevos) {
		Map<String, IdCtrlschemeVO> varToScheme = new HashMap();
		List<IdCtrlschemeVO> linkedschemes = new ArrayList();

		int varIndex = 0;
		int rarIndex = 0;
		String var = "var";
		String rar = "rar";

		for (IdCtrlschemeVO schemevo : schemevos) {
			if (schemevo.getVarno().indexOf(var) >= 0) {
				varIndex++;
			} else if (schemevo.getVarno().indexOf(rar) >= 0)
				rarIndex++;
			varToScheme.put(schemevo.getVarno(), schemevo);
		}

		for (int i = 0; i < varIndex; i++) {
			String varNo = var + String.valueOf(i);
			if (varToScheme.containsKey(varNo)) {
				linkedschemes.add(varToScheme.get(varNo));
			}
		}
		for (int i = 0; i < rarIndex; i++) {
			String rarNo = rar + String.valueOf(i);
			if (varToScheme.containsKey(rarNo))
				linkedschemes.add(varToScheme.get(rarNo));
		}
		return (IdCtrlschemeVO[]) linkedschemes.toArray(new IdCtrlschemeVO[linkedschemes.size()]);
	}

	public static void checkExistCtrlSchemeFindByDv(HashMap<String, HashMap<DimVector, DataCellValue>> cubeMap) throws Exception {
		CtlSchemeServiceGetter.getICtlScheme().checkExistCtrlSchemeFindByDv(cubeMap);
	}

	public static void updateExistCtrlSchemeFindByDv(ArrayList<ICubeDataSet> dataSets) throws Exception {
		HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> ctrlscheme = new HashMap();
		for (int n = 0; n < dataSets.size(); n++) {
			String pk_cube = ((ICubeDataSet) dataSets.get(n)).getCubeDef().getPrimaryKey();
			List<DataCell> datacells = ((ICubeDataSet) dataSets.get(n)).getDataResult();
			ArrayList<DimVector> dvList = new ArrayList();
			for (int m = 0; m < datacells.size(); m++) {
				DataCell datacell = (DataCell) datacells.get(m);
				dvList.add(datacell.getDimVector());
			}

			String tmpTableName = "NTB_TMP_CUBE_" + RandomStringUtils.randomNumeric(3);
			ArrayList<ArrayList> _list = new ArrayList();
			IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
			for (int m = 0; m < dvList.size(); m++) {
				ArrayList<String> tmpList = new ArrayList();
				tmpList.add(cvt.convertToString(dvList.get(m)) == null ? " " : cvt.convertToString(dvList.get(m)));
				tmpList.add(cvt.convertToString(dvList.get(m)) == null ? " " : cvt.convertToString(dvList.get(m)));
				_list.add(tmpList);
			}

			HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> qryResult = createNtbTempTable(((ICubeDataSet) dataSets.get(n)).getCubeDef(), _list);
			ctrlscheme.putAll(qryResult);
		}

		HashMap<String, DataCell> map = new HashMap();
		HashMap<String, ArrayList<String>> pkMap = new HashMap();
		Iterator _iter = ctrlscheme.entrySet().iterator();
		while (_iter.hasNext()) {
			Map.Entry entry = (Map.Entry) _iter.next();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
			if (pkMap.get(vo.getPk_cube()) == null) {
				ArrayList<String> list = new ArrayList();
				list.add(vo.getPk_dimvector());
				pkMap.put(vo.getPk_cube(), list);
			} else {
				((ArrayList) pkMap.get(vo.getPk_cube())).add(vo.getPk_dimvector());
			}
		}
		Iterator iterPkMap = pkMap.entrySet().iterator();
		while (iterPkMap.hasNext()) {
			Map.Entry entry = (Map.Entry) iterPkMap.next();
			String str = (String) entry.getKey();
			ArrayList<String> sList = (ArrayList) entry.getValue();
			map.putAll(getDataCellPkCubeByDimVector(str, (String[]) sList.toArray(new String[0])));
		}

		ArrayList<IdCtrlformulaVO> updatevo = new ArrayList();
		HashMap<String, DimFormulaVO> formulaMap = new HashMap();
		Iterator iter = ctrlscheme.entrySet().iterator();
		parserMap.clear();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
			if (formulaMap.get(vo.getPk_parent()) == null) {
				DimFormulaVO dimformulavo = FormulaCTL.getDimFormulaByPrimaryKey(vo.getPk_parent());
				formulaMap.put(vo.getPk_parent(), dimformulavo);
			}
			ArrayList<IdCtrlschemeVO> vos = (ArrayList) entry.getValue();
			IdCtrlschemeVO[] schemeArr = getLinkedSchemeVOs(vos);
			String express = againCalculate((DataCell) map.get(vo.getPk_cube() + vo.getPk_dimvector()), schemeArr, (DimFormulaVO) formulaMap.get(vo.getPk_parent()));
			vo.setExpressformula(express);
			vo.setPlanvalue(getPlanValue(express, vos));
			updatevo.add(vo);
		}
		CtlSchemeServiceGetter.getICtlScheme().updateCtrl((IdCtrlformulaVO[]) updatevo.toArray(new IdCtrlformulaVO[0]));
	}

	public static String againCalculate(DataCell datacell, IdCtrlschemeVO[] vosDB, DimFormulaVO fvo) throws Exception {
		CountTimeCost totalCost = new CountTimeCost();
		totalCost.beginCost();
		String fullcontent = getExpress(fvo);
		totalCost.addCost("againCalculate 0.2", totalCost.getCost());

		FormulaDimCI m_env = new FormulaDimCI();
		m_env.setDataCell(datacell);
		fullcontent = DimFormulaMacro.getParsedFormula(m_env, fullcontent, "TBRULECLASS0BIZ_CALC");
		totalCost.addCost("againCalculate 0.5", totalCost.getCost());
		fullcontent = fullcontent.substring(0, fullcontent.length() - 1);
		if (parserMap.get(fullcontent) == null) {
			StringReader reader = new StringReader(fullcontent);
			TbbLexer lexer = new TbbLexer(reader);
			TbbParser parser = new TbbParser(lexer);
			parserMap.put(fullcontent, parser.parse());
		}

		Calculator calculator = new Calculator();
		IFormulaContext context = new DefaultFormulaContext(datacell.getCubeDef());
		context.setOwnerCell(new FormulaDataCell(datacell));
		context.setCurrentCubeDef(datacell.getCubeDef());
		//20170220 tsy 
		if(vosDB!=null && vosDB.length>0){
			
			context.setWorkBook(AdjustBillTaskDataModelCache.getTaskDataModelByPk(vosDB[0].getPk_plan()));
		}
		
		calculator.setContext(context);

		WhereDataCellInfo whereDataCellInfo = new WhereDataCellInfo();
		List<DataCell> dCells = new ArrayList();

		dCells.add(datacell);

		whereDataCellInfo.addCells(dCells);
		calculator.getContext().setValue("AbstractCutCube.WhereCells", whereDataCellInfo);

		totalCost.addCost("againCalculate 1", totalCost.getCost());
		String result = ((Expression) parserMap.get(fullcontent)).toValue(calculator);
		totalCost.addCost("againCalculate 2", totalCost.getCost());
		SingleSchema schema = new SingleSchema(result, datacell);
		String returnValue = schema.getExpressFindOrSmFindFormula(vosDB);
		totalCost.addCost("againCalculate 3", totalCost.getCost());
		return returnValue;
	}

	public static String againCalculate(DataCell datacell, IdCtrlschemeVO[] vosDB, DimFormulaVO fvo, HashMap<DimVector, DataCellValue> cubeMap) throws Exception {
		CountTimeCost totalCost = new CountTimeCost();
		totalCost.beginCost();
		DataCellValue value = (DataCellValue) cubeMap.get(datacell.getDimVector());
		datacell.setCellValue(value);
		String fullcontent = getExpress(fvo);
		totalCost.addCost("againCalculate 0.2", totalCost.getCost());

		FormulaDimCI m_env = new FormulaDimCI();
		m_env.setDataCell(datacell);
		fullcontent = DimFormulaMacro.getParsedFormula(m_env, fullcontent, "TBRULECLASS0BIZ_CALC");
		totalCost.addCost("againCalculate 0.5", totalCost.getCost());
		fullcontent = fullcontent.substring(0, fullcontent.length() - 1);
		if (parserMap.get(fullcontent) == null) {
			StringReader reader = new StringReader(fullcontent);
			TbbLexer lexer = new TbbLexer(reader);
			TbbParser parser = new TbbParser(lexer);
			parserMap.put(fullcontent, parser.parse());
		}

		Calculator calculator = new Calculator();
		IFormulaContext context = new DefaultFormulaContext(datacell.getCubeDef());
		context.setOwnerCell(new FormulaDataCell(datacell));
		context.setCurrentCubeDef(datacell.getCubeDef());
		calculator.setContext(context);

		WhereDataCellInfo whereDataCellInfo = new WhereDataCellInfo();
		List<DataCell> dCells = new ArrayList();

		dCells.add(datacell);

		whereDataCellInfo.addCells(dCells);
		calculator.getContext().setValue("AbstractCutCube.WhereCells", whereDataCellInfo);

		totalCost.addCost("againCalculate 1", totalCost.getCost());
		String result = ((Expression) parserMap.get(fullcontent)).toValue(calculator);
		totalCost.addCost("againCalculate 2", totalCost.getCost());

		DataCellValue oldvalue = datacell.getCellValue();

		SingleSchema schema = new SingleSchema(result, datacell);
		String returnValue = schema.getExpressFindOrSmFindFormulaVar(vosDB);
		datacell.setCellValue(oldvalue);
		totalCost.addCost("againCalculate 3", totalCost.getCost());
		return returnValue;
	}

	private static String getExpress(DimFormulaVO fvo) throws BusinessException {
		StringBuffer sbStr = new StringBuffer();
		DimFormulaVO vo = fvo;
		sbStr.append(FormulaCTL.getFullExpress(vo.getFullcontent(), vo.getPrimaryKey())).append(";");
		return sbStr.toString();
	}

	public static UFDouble getPlanValue(String express, ArrayList<IdCtrlschemeVO> vos) throws Exception {
		UFDouble planvalue = null;
		String tmpExpress = express;
		for (int n = 0; n < vos.size(); n++) {
			UFDouble tmpValue = new UFDouble(0);
			UFDouble readydata = ((IdCtrlschemeVO) vos.get(n)).getReadydata() == null ? UFDouble.ZERO_DBL : ((IdCtrlschemeVO) vos.get(n)).getReadydata();
			UFDouble rundata = ((IdCtrlschemeVO) vos.get(n)).getRundata() == null ? UFDouble.ZERO_DBL : ((IdCtrlschemeVO) vos.get(n)).getRundata();
			tmpValue = tmpValue.add(readydata).add(rundata);
			tmpExpress = tmpExpress.replaceAll(((IdCtrlschemeVO) vos.get(n)).getVarno(), tmpValue.toString());
		}
		String[] strs = tmpExpress.split(">=");
		strs[0] = strs[0].replaceAll("%", "/100");
		planvalue = getComplexZxs(strs[0]);
		return planvalue;
	}

	public static ArrayList<DataContrastVO> getDataVOByFindFormula(DimFormulaVO fmlVO, MdWorkbook book) throws BusinessException {
		ArrayList<DataContrastVO> newVos = new ArrayList();
		String pk_formula = fmlVO.getPrimaryKey();
		ArrayList<DimFormulaMVO> formulaMList = RuleServiceGetter.getIBusiRuleQuery().queryMVOByPkFormula(pk_formula);
		IDimManager dm = DimServiceGetter.getDimManager();
		HashMap<String, ArrayList<LevelValue>> mapValue = new HashMap();
		CubeDef cubedef = null;
		try {
			cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(fmlVO.getPk_cube());
		} catch (BusinessException ex) {
			NtbLogger.print(ex);
		}
		ArrayList root = new ArrayList();
		for (int n = 0; n < (formulaMList == null ? 0 : formulaMList.size()); n++) {
			DimFormulaMVO vo = (DimFormulaMVO) formulaMList.get(n);

			String content = vo.getContent();
			String[] strs = parseDimExpressValues(content);
			if (strs[0].charAt(0) != '\'') {
				DimLevel dimlevel = dm.getDimLevelByPK(strs[0]);

				for (int m = 1; m < strs.length; m++) {
					String pk_levelValue = strs[m];
					LevelValue levelvalue = null;

					if (pk_levelValue.indexOf("@") == 0) {
						levelvalue = new LevelValue(dimlevel, pk_levelValue.substring(1));
					} else if (pk_levelValue.indexOf("N") == 0) {
						levelvalue = new LevelValue(dimlevel, pk_levelValue);
					} else {
						levelvalue = dimlevel.getLevelValueByKey(dimlevel.getKeyByString(pk_levelValue));
					}
					if (levelvalue != null) {
						if (mapValue.get(dimlevel.getPrimaryKey()) == null) {
							ArrayList<LevelValue> values = new ArrayList();
							values.add(levelvalue);
							root.add(values);
							mapValue.put(dimlevel.getPrimaryKey(), values);
						} else {
							ArrayList<LevelValue> values = (ArrayList) mapValue.get(dimlevel.getPrimaryKey());
							boolean isExist = false;
							for (int k = 0; k < values.size(); k++) {
								LevelValue _levelvalue = (LevelValue) values.get(k);
								if (_levelvalue.getUniqCode().equals(levelvalue.getUniqCode())) {
									isExist = true;
								}
							}
							if (!isExist)
								values.add(levelvalue);
						}
					}
				}
			}
		}
		DescartesMultiplication cation = new DescartesMultiplication();
		if (root.size() > 0) {
			ArrayList nodeList = (ArrayList) root.get(0);
			Stack<String> stack = new Stack();
			cation.traverse(root, 0, nodeList, stack);
		}
		ArrayList list = cation.getResult();
		for (int n = 0; n < list.size(); n++) {
			DataContrastVO vo = new DataContrastVO(cubedef, (Object[]) list.get(n));
			vo.setBook(book);
			newVos.add(vo);
		}

		return newVos;
	}

	public static String[] parseDimExpressValues(String express) {
		String reg = "\\'[^\\)]*\\'";
		Pattern p = Pattern.compile(reg);
		Matcher m = p.matcher(express);
		ArrayList<String> alFunc = new ArrayList();
		while (m.find()) {
			String exp = m.group();
			alFunc.add(exp);
		}
		String finds = (String) alFunc.get(0);
		String[] strs = finds.split(",");
		alFunc.clear();
		for (int n = 0; n < strs.length; n++) {
			String str = strs[n].substring(strs[n].indexOf("'") + 1, strs[n].lastIndexOf("'"));
			alFunc.add(str);
		}
		return (String[]) alFunc.toArray(new String[0]);
	}

	public static HashMap<IdCtrlformulaVO, Map<IdCtrlschemeVO, String>> convertFormulaExpress2CtrlVOs(String[] express, String[] formulaPks, DataCell cell, HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> map) throws Exception {
		HashMap<IdCtrlformulaVO, Map<IdCtrlschemeVO, String>> ctlMap = new HashMap();

		for (int i = 0; i < express.length; i++) {
			if (!checkIsStartCtrl(formulaPks[i], map)) {
				express[i] = express[i].replaceAll("'", "\"");
				SingleSchema schema = new SingleSchema(express[i], cell);
				Map<IdCtrlschemeVO, String> lm = convertIdCtrlscheme(cell, schema, formulaPks[i]);
				IdCtrlschemeVO[] childrenVOs = (IdCtrlschemeVO[]) lm.keySet().toArray(new IdCtrlschemeVO[0]);
				IdCtrlformulaVO parentVO = convertIdCtrlFormula(cell, schema, childrenVOs, formulaPks[i]);
				for (int m = 0; m < (childrenVOs == null ? 0 : childrenVOs.length); m++) {
					IdCtrlschemeVO vo = childrenVOs[m];
					vo.setIsstarted(UFBoolean.valueOf(false));
				}
				parentVO.setIsstarted(UFBoolean.valueOf(false));
				ctlMap.put(parentVO, lm);
			}
		}

		return ctlMap;
	}

	private static boolean checkIsStartCtrl(String pk_formula, HashMap<IdCtrlformulaVO, ArrayList<IdCtrlschemeVO>> cubeMap) {
		boolean isStratCtrl = false;
		ArrayList<String> startCtrlPK = new ArrayList();
		Iterator iter = cubeMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			IdCtrlformulaVO vo = (IdCtrlformulaVO) entry.getKey();
			startCtrlPK.add(vo.getPk_parent());
		}
		if (startCtrlPK.contains(pk_formula)) {
			isStratCtrl = true;
		}
		return isStratCtrl;
	}

	public static HashMap<CtlAggregatedVO, ArrayList<CtlAggregatedVO>> ConvertCtrlFormulaVO2AggregateVO(Map<IdCtrlformulaVO, Map<IdCtrlschemeVO, String>> ctlmap, MdTask plan) throws Exception {
		HashMap<CtlAggregatedVO, ArrayList<CtlAggregatedVO>> aggregatemap = new HashMap();

		Iterator<IdCtrlformulaVO> iteraKey = ctlmap.keySet().iterator();

		while (iteraKey.hasNext()) {
			CtlAggregatedVO aggregateVOParent = new CtlAggregatedVO();

			IdCtrlformulaVO formulaVO = (IdCtrlformulaVO) iteraKey.next();
			String pk_formula = formulaVO.getPk_parent();
			DimFormulaVO vo = RuleServiceGetter.getIBusiRuleQuery().queryDimFormulaByPK(pk_formula);
			IdCtrlInfoVO infovo = RuleServiceGetter.getIBusiRuleQuery().queryCtrlInfoVOByPk(vo.getPrimaryKey());
			IdCtrlschmVO tempvo = new IdCtrlschmVO();

			String temptype = formulaVO.getSchemetype();

			if (temptype.equals("TBRULE0SCHEMA_SINGLE")) {
				tempvo.setIscomplex(UFBoolean.valueOf("N"));
				tempvo.setIsactive(UFBoolean.valueOf("N"));
				tempvo.setIsgroup(UFBoolean.valueOf("N"));
			}
			if (temptype.equals("TBRULE00SCHEMA_GROUP")) {
				tempvo.setIscomplex(UFBoolean.valueOf("N"));
				tempvo.setIsactive(UFBoolean.valueOf("N"));
				tempvo.setIsgroup(UFBoolean.valueOf("Y"));
			}
			if ((temptype.equals("TBRULE000SCHEMA_SPEC")) || (temptype.equals("TBRULEVELSCHEMA_SPEC"))) {
				tempvo.setIscomplex(UFBoolean.valueOf("Y"));
				tempvo.setIsactive(UFBoolean.valueOf("N"));
				tempvo.setIsgroup(UFBoolean.valueOf("N"));
			}
			if (temptype.equals("TBRULE000SCHEMA_FLEX")) {
				tempvo.setIscomplex(UFBoolean.valueOf("Y"));
				tempvo.setIsactive(UFBoolean.valueOf("Y"));
				tempvo.setIsgroup(UFBoolean.valueOf("N"));
			}
			tempvo.setM_rule_class(temptype);

			tempvo.setMemo(formulaVO.getCtrlname());
			tempvo.setPk_org(formulaVO.getPk_org());

			tempvo.setCtrlmode(formulaVO.getCtlmode());

			tempvo.setCtrlsign(formulaVO.getCtrlsign());
			tempvo.setCtrlpercent(formulaVO.getCtrlpercent());
			String s = FormulaParser.parseNumber(formulaVO.getExpressformula());
			tempvo.setRunformula(s);
			tempvo.setPk_cell(formulaVO.getPk_dimvector());
			tempvo.setIsstarted(formulaVO.getIsstarted());
			tempvo.setGroupname(formulaVO.getPrimaryKey());

			tempvo.setPrimaryKey(formulaVO.getPk_parent());
			if (infovo != null) {
				tempvo.setPk_ctrlinfo(infovo.getPrimaryKey());
			}

			if ((vo.getSpecialUsage() != null) && (vo.getSpecialUsage().booleanValue())) {
				CtrlSpecialUsage[] ctrlOt = RuleServiceGetter.getIBusiRuleQuery().queryCtrlSpecialUsageByRuleFormula(new String[] { vo.getPrimaryKey() }, true);
				if ((ctrlOt != null) && (ctrlOt.length > 0)) {
					tempvo.setAlert_percent(ctrlOt[0].getAlertPercent());
				}
			}
			Map<IdCtrlschemeVO, String> schmOrgMap = (Map) ctlmap.get(formulaVO);
			List<IdCtrlschemeVO> ls = Arrays.asList(schmOrgMap.keySet().toArray(new IdCtrlschemeVO[0]));

			IdCtrlschemeVO temps = (IdCtrlschemeVO) ls.get(0);
			tempvo.setAccctrollflag(temps.getAccctrollflag());
			aggregateVOParent.setParentVO(tempvo);

			ArrayList<CtlAggregatedVO> aggregateVo_ls = new ArrayList();

			for (int i = 0; i < ls.size(); i++) {

				CtlAggregatedVO aggregateVOChildren = new CtlAggregatedVO();

				IdCtrlschemeVO schemeVO = (IdCtrlschemeVO) ls.get(i);

				IdCtrlschmVO parentVO = new IdCtrlschmVO();
				parentVO.setPrimaryKey(schemeVO.getPrimaryKey());
				parentVO.setAccctrollflag(schemeVO.getAccctrollflag());
				parentVO.setBilltype(schemeVO.getBilltype());
				parentVO.setCtrldirection(schemeVO.getCtrldirection());
				parentVO.setDateType(schemeVO.getDatetype());
				parentVO.setRunformula(tempvo.getRunformula());

				parentVO.setCtrlmode(formulaVO.getCtlmode());
				parentVO.setCtrlobj(schemeVO.getCtrlobj());
				parentVO.setCtrlObjValue(schemeVO.getCtrlobjValue());
				parentVO.setCtrlpercent(formulaVO.getCtrlpercent());
				parentVO.setCtrlsign(formulaVO.getCtrlsign());
				parentVO.setCtrlsys(schemeVO.getCtrlsys());
				parentVO.setCurrtype(schemeVO.getCurrtype());
				parentVO.setEnddate(schemeVO.getEnddate());
				parentVO.setAlert_percent(tempvo.getAlert_percent());
				parentVO.setIncludeuneffected(schemeVO.getIncludeuneffected());
				String type = formulaVO.getSchemetype();
				String pk_corp = schemeVO.getPk_org();
				if (type.equals("TBRULE0SCHEMA_SINGLE")) {
					parentVO.setIscomplex(UFBoolean.valueOf("N"));
					parentVO.setIsactive(UFBoolean.valueOf("N"));
					parentVO.setIsgroup(UFBoolean.valueOf("N"));
				}
				if (type.equals("TBRULE00SCHEMA_GROUP")) {
					parentVO.setIscomplex(UFBoolean.valueOf("N"));
					parentVO.setIsactive(UFBoolean.valueOf("N"));
					parentVO.setIsgroup(UFBoolean.valueOf("Y"));
				}
				if (type.equals("TBRULE000SCHEMA_SPEC")) {
					parentVO.setIscomplex(UFBoolean.valueOf("Y"));
					parentVO.setIsactive(UFBoolean.valueOf("N"));
					parentVO.setIsgroup(UFBoolean.valueOf("N"));
				}
				if (type.equals("TBRULE000SCHEMA_FLEX")) {
					parentVO.setIscomplex(UFBoolean.valueOf("Y"));
					parentVO.setIsactive(UFBoolean.valueOf("Y"));
					parentVO.setIsgroup(UFBoolean.valueOf("N"));
					parentVO.setGroupname(formulaVO.getPrimaryKey());
				}

				parentVO.setGroupname(formulaVO.getPrimaryKey());
				parentVO.setIsstarted(formulaVO.getIsstarted());
				parentVO.setMemo(formulaVO.getCtrlname());
				parentVO.setPk_cell(schemeVO.getPk_dimvector());
				parentVO.setPk_corp(schemeVO.getPk_org());
				parentVO.setPk_currency(schemeVO.getPk_currency());
				parentVO.setPk_ncentity(schemeVO.getPk_ncentity());
				parentVO.setPk_plan(schemeVO.getPk_plan());
				parentVO.setIsPlDeal(vo.getIsPlDeal());
				parentVO.setPk_ctrlinfo(tempvo.getPk_ctrlinfo());
				parentVO.setM_rule_class(tempvo.getM_rule_class());

				parentVO.setSchtype(schemeVO.getSchtype());

				parentVO.setStartdate(schemeVO.getStartdate());
				String ss = FormulaParser.parseNumber(schemeVO.getVarno());
				parentVO.setVarno(ss);

				ArrayList<IdCtrlschmBVO> childrenVOls = new ArrayList();

				String dimcode = schemeVO.getPk_dimvector();
				DimMember[] dm = null;

				if (dimcode != null) {
					IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
					DimVector dimvector = (DimVector) cvt.fromString(dimcode);
					dm = (DimMember[]) dimvector.getDimMembers().toArray(new DimMember[0]);
				}

				String[] fromitem = schemeVO.getFromitems().split(":");
				String[] codeitem = schemeVO.getCodeidx().split(":");
				String[] nameitem = schemeVO.getNameidx().split(":");
				String[] ctllevelitem = schemeVO.getCtllevels().split(":");
				String[] stridx = schemeVO.getStridx().split(":");

				String sysid = schemeVO.getCtrlsys();

				String datafromdesc = getDatafromdesc(fromitem, sysid);
				String[] datafromdescitem = datafromdesc.split(":");

				String pk_bdcontrast = getPkBdConstrast(fromitem, sysid);
				String[] pks_bdcontrast = pk_bdcontrast.split(":");

				String bdinfoType = BudgetControlCTL.getBdinfoType(fromitem, sysid);
				String[] bdinfoTypeitem = bdinfoType.split(":");

				String orgItem = (String) schmOrgMap.get(schemeVO);

				for (int j = 0; j < fromitem.length; j++) {
					IdCtrlschmBVO childrenVO = new IdCtrlschmBVO();

					childrenVO.setBasecode(codeitem[j]);

					childrenVO.setBasename(nameitem[j]);

					childrenVO.setBasetypename(bdinfoTypeitem[j]);
					childrenVO.setCtllevelflag(UFBoolean.valueOf(ctllevelitem[j]));

					childrenVO.setDatafrom(fromitem[j]);

					childrenVO.setDatafromdesc(datafromdescitem[j]);
					childrenVO.setPk_bdcontrast(pks_bdcontrast[j]);

					childrenVO.setPk_base(stridx[j]);

					if (fromitem[j].equals(orgItem))
						childrenVO.setIsMainOrg(UFBoolean.valueOf("Y"));
					if ((dm != null) && (dm.length > 0)) {
						for (int k = 0; k < dm.length; k++) {
							if (!bdinfoTypeitem[j].equals(OutEnum.CURRDOC)) {
								if ((dm[k] != null) && (dm[k].getLevelValue().getCode().equals(codeitem[j]))) {
									childrenVO.setDimdefname(dm[k].getDimDef().getObjName());
									childrenVO.setDimmembername(dm[k].getLevelValue().getName());
									childrenVO.setPk_dimdef(dm[k].getDimDef().getPrimaryKey());
									childrenVO.setPk_dim_m((String) dm[k].getLevelValue().getKey());
									break;
								}

							} else if (bdinfoTypeitem[j].equals(OutEnum.CURRDOC)) {

								if ((dm[k].getDimDef() != null) && (dm[k].getDimDef().getPrimaryKey().equals("TB_DIMDEF_CURR_00000"))) {
									childrenVO.setDimdefname(dm[k].getDimDef().getObjName());
									childrenVO.setDimmembername(dm[k].getLevelValue().getName());
									childrenVO.setPk_dimdef(dm[k].getDimDef().getPrimaryKey());
									childrenVO.setPk_dim_m((String) dm[k].getLevelValue().getKey());
									break;
								}
							}
						}
					}

					childrenVOls.add(childrenVO);
				}

				aggregateVOChildren.setParentVO(parentVO);
				aggregateVOChildren.setChildrenVO((CircularlyAccessibleValueObject[]) childrenVOls.toArray(new IdCtrlschmBVO[0]));

				aggregateVo_ls.add(aggregateVOChildren);
			}
			aggregatemap.put(aggregateVOParent, aggregateVo_ls);
		}
		return aggregatemap;
	}

	public static String getDatafromdesc(String[] fromitem, String sysid) throws Exception {
		IdBdcontrastVO[] bdcontrasts = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fromitem.length; i++) {
			for (int j = 0; j < bdcontrasts.length; j++) {
				if (bdcontrasts[j].getAtt_fld().equals(fromitem[i])) {
					String bdinfotype = bdcontrasts[j].getAtt_fld_desc();

					buffer.append(bdinfotype + ":");
					break;
				}
			}
		}

		return buffer.toString();
	}

	public static String getPkBdConstrast(String[] fromitem, String sysid) throws Exception {
		IdBdcontrastVO[] bdcontrasts = BdContrastCache.getNewInstance().getVoBySysid(sysid);
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fromitem.length; i++) {
			for (int j = 0; j < bdcontrasts.length; j++) {
				if (bdcontrasts[j].getAtt_fld().equals(fromitem[i])) {
					String bdinfotype = bdcontrasts[j].getPrimaryKey();

					buffer.append(bdinfotype + ":");
					break;
				}
			}
		}

		return buffer.toString();
	}

	public static void validateNtbParamVO(NtbParamVO vo) throws BusinessException {
		String sysId = vo.getSys_id();
		String[] busiAttr = vo.getBusiAttrs();

		if (sysId.equals(OutEnum.GLSYS)) {
			boolean hasAccount = false;
			for (String busiStr : busiAttr) {
				if (busiStr.equals("DETAIL103")) {
					hasAccount = true;
					break;
				}
			}
			if (!hasAccount) {
				String e = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000321");
				throw new BusinessException(e);
			}
		}
	}

	public static Map convertIdCtrlscheme(DataCell cell, SingleSchema schema, String formulaPk) throws Exception {
		Map<IdCtrlschemeVO, String> schemeMap = new HashMap();
		String[] src_ufind = schema.getUFind();
		String[] src_prefind = schema.getPREUFind();
		try {
			for (int i = 0; i < src_ufind.length; i++) {
				ConvertToCtrlSchemeVO convertor = new ConvertToCtrlSchemeVO(src_ufind[i]);
				IdCtrlschemeVO schemevos = new IdCtrlschemeVO();

				String[] temp = convertor.getFromItem().split(":");
				StringBuffer buffer = new StringBuffer();
				for (int j = 0; j < temp.length; j++) {
					buffer.append("null");
					buffer.append(":");
				}
				schemevos.setStridx(buffer.toString());
				schemevos.setCtrlsys(convertor.getCtrlSys());
				schemevos.setBilltype(convertor.getBillType());
				schemevos.setCtrldirection(convertor.getCtrlDirection());
				schemevos.setCtrlobj(convertor.getCtrlObject());
				schemevos.setCtrlobjValue(convertor.getCtrlObjectValue());
				schemevos.setStartdate(convertor.getStartDate());
				schemevos.setEnddate(convertor.getEndDate());
				schemevos.setAccctrollflag(convertor.getAccCtrlFlag());
				schemevos.setFromitems(convertor.getFromItem());
				schemevos.setCodeidx(convertor.getCodeIdx());
				schemevos.setCtllevels(convertor.getCtrlLevel());
				schemevos.setNameidx(convertor.getNameIdx());

				schemevos.setSchtype(OutEnum.SCHEMETYPE[0]);
				schemevos.setSchemetype(getCtlType(formulaPk));
				schemevos.setVarno("var" + (i == 0 ? 1 : i + 1));

				schemevos.setDatetype(convertor.getDataCatalg());
				schemevos.setIsstarted(UFBoolean.valueOf("N"));

				String mainOrg = null;
				String[] isMainOrgs = convertor.getMainOrg().split(":");
				for (int j = 0; j < isMainOrgs.length; j++) {
					UFBoolean isMainOrg = UFBoolean.valueOf(isMainOrgs[j]);
					if (isMainOrg.booleanValue())
						mainOrg = temp[j];
				}
				schemeMap.put(schemevos, mainOrg);
			}

			for (int i = 0; i < src_prefind.length; i++) {
				ConvertToCtrlSchemeVO convertor = new ConvertToCtrlSchemeVO(src_prefind[i], "PREFIND");
				IdCtrlschemeVO schemevos = new IdCtrlschemeVO();

				String[] temp = convertor.getFromItem().split(":");
				StringBuffer buffer = new StringBuffer();
				for (int j = 0; j < temp.length; j++) {
					buffer.append("null");
					buffer.append(":");
				}
				schemevos.setStridx(buffer.toString());
				schemevos.setCtrlsys(convertor.getCtrlSys());
				schemevos.setBilltype(convertor.getBillType());
				schemevos.setCtrldirection(convertor.getCtrlDirection());
				schemevos.setCtrlobj(convertor.getCtrlObject());
				schemevos.setCtrlobjValue(convertor.getCtrlObjectValue());
				schemevos.setStartdate(convertor.getStartDate());
				schemevos.setEnddate(convertor.getEndDate());
				schemevos.setAccctrollflag(convertor.getAccCtrlFlag());
				schemevos.setFromitems(convertor.getFromItem());
				schemevos.setCodeidx(convertor.getCodeIdx());
				schemevos.setCtllevels(convertor.getCtrlLevel());
				schemevos.setNameidx(convertor.getNameIdx());

				schemevos.setSchtype(OutEnum.SCHEMETYPE[0]);
				schemevos.setSchemetype(getCtlType(formulaPk));
				schemevos.setVarno("rar" + (i == 0 ? 1 : i + 1));

				schemevos.setDatetype(convertor.getDataCatalg());
				schemevos.setIsstarted(UFBoolean.valueOf("N"));
				String mainOrg = null;
				String[] isMainOrgs = convertor.getMainOrg().split(":");
				for (int j = 0; j < isMainOrgs.length; j++) {
					UFBoolean isMainOrg = UFBoolean.valueOf(isMainOrgs[j]);
					if (isMainOrg.booleanValue())
						mainOrg = temp[j];
				}
				schemeMap.put(schemevos, mainOrg);
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}

		return schemeMap;
	}

	public static void saveDimRelUapVO(ArrayList<DimRelUapVO> addvos, ArrayList<DimRelUapVO> updatevos, ArrayList<DimRelUapVO> delvos) {
		try {
			NtbSuperServiceGetter.getINtbSuper().updateDimRelUapVos(addvos, updatevos, delvos);
			BdContrastCache.getNewInstance().refreshDimRelUapDoc();
		} catch (BusinessException ex) {
			NtbLogger.print(ex);
		}
	}

	public static ArrayList<DimRelUapVO> getAllDimRelUapVO() {
		ArrayList<DimRelUapVO> voList = new ArrayList();
		try {
			DimRelUapVO[] vos = (DimRelUapVO[]) NtbSuperServiceGetter.getINtbSuper().queryAll(DimRelUapVO.class);
			voList.addAll(Arrays.asList(vos));
		} catch (BusinessException ex) {
			NtbLogger.print(ex);
		}
		return voList;
	}

	public static String getControlHintMessage_new(String ctrlruletype, String ctrltype) {
		StringBuffer message = new StringBuffer();
		message.append(CtrlInfoMacroConst.ctrlTypeMacro).append(":").append("\n").append(CtrlInfoMacroConst.diminfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000165")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.rundataMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050")).append(">").append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000255")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.controldataMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000166")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000167")).append("\n").append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000168")).append("\n").append(CtrlInfoMacroConst.taskinfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01420ctl_000169")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.ctrlschemeMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050")).append("\n");

		return message.toString();
	}

	public static String getControlHintMessage_new_(String ctrlruletype, String ctrltype) {
		StringBuffer message = new StringBuffer();
		DecimalFormat formatter = null;
		String spliter = ":";
		String HHF = "\n";
		String FLEXMESSAGE = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_rule", "01801rul_000407");
		String infoss = null;
		String ctrlType = null;
		String frontHint = null;
		StringBuffer diminfoStr = new StringBuffer().append(CtrlInfoMacroConst.diminfoMacro);

		boolean isShowDimInfo = true;
		if (ctrltype.equals(CtrlTypeEnum.RigidityControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000240", null, new String[] { CtrlInfoMacroConst.sysinfoMacro }) + HHF;
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000241");

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000310") + HHF;
		} else if (ctrltype.equals(CtrlTypeEnum.WarningControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000243", null, new String[] { CtrlInfoMacroConst.sysinfoMacro }) + HHF;

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000244") + HHF;
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000245");
		} else if (ctrltype.equals(CtrlTypeEnum.FlexibleControl.toCodeString())) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000246", null, new String[] { CtrlInfoMacroConst.sysinfoMacro }) + HHF;

			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000247") + FLEXMESSAGE + "\n";
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000248");
		} else if (ctrltype.equals(String.valueOf(4))) {
			frontHint = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000249", null, new String[] { CtrlInfoMacroConst.sysinfoMacro });
			infoss = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000250");
			ctrltype = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000251");
		}

		if (ctrlruletype.equals(String.valueOf(1))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000201");
			isShowDimInfo = true;
		} else if (ctrlruletype.equals(String.valueOf(2))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000252");
			isShowDimInfo = true;
		} else if (ctrlruletype.equals(String.valueOf(3))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000203");
			isShowDimInfo = true;
		} else if (ctrlruletype.equals(String.valueOf(6))) {
			ctrlType = NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000204");
			isShowDimInfo = true;
		} else if (ctrlruletype.equals(String.valueOf(4))) {
			ctrlType = "";
			isShowDimInfo = true;
		}

		String ctrlsign = "";

		message.append(CtrlInfoMacroConst.isStartFinishMacro).append(HHF).append(frontHint).append((CharSequence) (isShowDimInfo ? diminfoStr : "")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000254")).append(CtrlInfoMacroConst.taskinfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000254")).append(CtrlInfoMacroConst.taskinfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.rundataMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000050")).append(ctrlsign).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000255")).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.budgetdataMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000256") + HHF).append(infoss).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000257", null, new String[] { CtrlInfoMacroConst.taskinfoMacro }) + HHF).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000258", null, new String[] { CtrlInfoMacroConst.taskinfoMacro })).append(CtrlInfoMacroConst.taskinfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000259")).append(ctrltype).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000007")).append(CtrlInfoMacroConst.taskinfoMacro).append(NCLangRes4VoTransl.getNCLangRes().getStrByID("tbb_ctrl", "01801ctl_000260")).append("\n");

		return message.toString();
	}

	public static boolean isUserHasBillFuncPermission(String billTypeCode) throws BusinessException {
		if (StringUtil.isEmpty(billTypeCode))
			return true;
		BilltypeVO vo = PfDataCache.getBillType(billTypeCode);
		if (vo != null) {
			String userId = InvocationInfoProxy.getInstance().getUserId();
			String groupId = InvocationInfoProxy.getInstance().getGroupId();
			IFunctionPermissionPubService funcPermServ = (IFunctionPermissionPubService) NCLocator.getInstance().lookup(IFunctionPermissionPubService.class);
			IFunctionPermProfile funcPermProfile = funcPermServ.getFunctionPermProfileWithGroup(userId, groupId);

			return funcPermProfile.hasPermissionOfFuncode(vo.getNodecode());
		}
		return true;
	}

	public static boolean checkTaskStatus(MdTask task) {
		boolean checkin = true;
		String taskStatus = task.getPlanstatus();
		if (!taskStatus.equals("320")) {
			checkin = false;
		}
		return checkin;
	}

	public static String[] addAlarmScheme(IdAlarmschemeVO[] vos) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().addAlarmScheme(vos);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static String[] addAlarmDimVector(IdAlarmDimVectorVO[] vos) throws BusinessException {
		String[] pks = CtlSchemeServiceGetter.getICtlScheme().addAlarmDimVector(vos);
		return pks;
	}

	public static Collection<IdAlarmschemeVO> queryAlarmScheme(String sqlWhere) throws BusinessException {
		try {
			return CtlSchemeServiceGetter.getICtlScheme().queryAlarmScheme(sqlWhere);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static Collection<IdAlarmDimVectorVO> queryAlarmDimvector(String sqlWhere) throws BusinessException {
		Collection<IdAlarmDimVectorVO> vos = CtlSchemeServiceGetter.getICtlScheme().queryAlarmDimvector(sqlWhere);
		return vos;
	}

	public static void updateAlarmScheme(IdAlarmschemeVO[] vos) throws BusinessException {
		try {
			CtlSchemeServiceGetter.getICtlScheme().updateAlarmScheme(vos);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new BusinessException(ex.getMessage(), ex);
		}
	}

	public static void deleteAlarmScheme(ArrayList<IdAlarmschemeVO> vos) throws BusinessException {
		CtlSchemeServiceGetter.getICtlScheme().deleteAlarmScheme(vos);
	}

	public static void deleteAlarmDimVector(List<IdAlarmDimVectorVO> list) throws BusinessException {
		CtlSchemeServiceGetter.getICtlScheme().deleteAlarmDimVector(list);
	}

	public static String replaceExpressWithVar(String express, Map<String, String> valueMap) {
		StringBuffer newExpress = new StringBuffer(express.length());
		int j = 0;
		for (int i = 0; i < express.length(); i++) {
			char[] var = new char[50];
			j = i;
			for (; (j < express.length()) && (express.charAt(j) != '+') && (express.charAt(j) != '-') && (express.charAt(j) != '(') && (express.charAt(j) != ')'); j++) {
				if ((express.charAt(j) == '+') || (express.charAt(j) == '-') || (express.charAt(j) == '(') || (express.charAt(j) == ')'))
					break;
				var[(j - i)] = express.charAt(j);
			}
			char[] var1 = new char[j - i];
			for (int k = 0; k < var1.length; k++)
				var1[k] = var[k];
			String varstr = new String(var1);
			if (valueMap.containsKey(varstr)) {
				newExpress.append((String) valueMap.get(varstr));
			} else {
				newExpress.append(varstr);
			}
			if (j != express.length()) {
				char c = express.charAt(j);

				while ((j < express.length()) && (!Character.isLetterOrDigit(c))) {
					newExpress.append(c);
					j++;
					if (j < express.length())
						c = express.charAt(j);
				}
				j--;
			}
			i = j++;
		}
		return newExpress.toString();
	}
}
