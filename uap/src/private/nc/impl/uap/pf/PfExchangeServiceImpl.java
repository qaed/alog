package nc.impl.uap.pf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.ml.NCLangResOnserver;
import nc.bs.pf.busiflow.PfBusiflowServiceImpl;
import nc.bs.pf.change.BillMappingConvertor;
import nc.bs.pf.change.BillSplitHelper;
import nc.bs.pf.pub.BillTypeCacheKey;
import nc.bs.pf.pub.ExchangeRuleVOListProcessor;
import nc.bs.pf.pub.PFRequestDataCacheProxy;
import nc.bs.pf.pub.PfDataCache;
import nc.bs.pf.pub.cache.CondStringKey;
import nc.bs.pf.pub.cache.ICacheDataQueryCallback;
import nc.bs.pf.pub.cache.IRequestDataCacheKey;
import nc.bs.pub.formulaparse.FormulaParse;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.uap.pf.IPfExchangeService;
import nc.itf.uap.pf.busiflow.ClassifyContext;
import nc.itf.uap.pf.busiflow.IPfBusiflowService;
import nc.jdbc.framework.SQLParameter;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.pf.change.ExchangeRuleVO;
import nc.vo.pf.change.ExchangeSplitVO;
import nc.vo.pf.change.ExchangeVO;
import nc.vo.pf.change.PFExchangeUtil;
import nc.vo.pf.change.PfUtilBaseTools;
import nc.vo.pf.change.RuleTypeEnum;
import nc.vo.pf.change.SplitItemVO;
import nc.vo.pf.pub.FunctionVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.billtype.BilltypeVO;
import nc.vo.pub.change.PublicHeadVO;
import nc.vo.pub.compiler.PfParameterVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.uap.pf.PFBusinessException;

public class PfExchangeServiceImpl
  implements IPfExchangeService
{
  public PfExchangeServiceImpl() {}
  
  public AggregatedValueObject runChangeData(String srcBillOrTranstype, String destBillOrTranstype, AggregatedValueObject srcBillVO, PfParameterVO paraVo) throws BusinessException
  {
    if (srcBillVO == null) {
      return null;
    }
    Logger.debug(">>开始单据VO交换=" + srcBillOrTranstype + "到" + destBillOrTranstype);
    long start = System.currentTimeMillis();
    

    String group = null;
    if ((paraVo != null) && (paraVo.m_pkGroup != null)) {
      group = paraVo.m_pkGroup;
    } else {
      group = InvocationInfoProxy.getInstance().getGroupId();
    }
    ExchangeVO chgVo = findVOConversionFromDB(srcBillOrTranstype, destBillOrTranstype, srcBillVO, group);
    


    BillMappingConvertor bmc = createChangeImpl(srcBillOrTranstype, destBillOrTranstype, paraVo);
    initBMC(bmc, chgVo);
    

    AggregatedValueObject[] destBillVOs = bmc.retChangeBusiVOs(srcBillOrTranstype, destBillOrTranstype, new AggregatedValueObject[] { srcBillVO });
    AggregatedValueObject destBillVO = null;
    if ((destBillVOs != null) && (destBillVOs.length > 0)) {
      destBillVO = destBillVOs[0];
    }
    Logger.debug(">>结束单据VO交换=" + srcBillOrTranstype + "到" + destBillOrTranstype + ",耗时=" + (System.currentTimeMillis() - start) + "ms");
    
    return destBillVO;
  }
  

  public AggregatedValueObject[] runChangeDataAry(String srcBillOrTranstype, String destBillOrTranstype, AggregatedValueObject[] sourceBillVOs, PfParameterVO paraVo)
    throws BusinessException
  {
    return runChangeDataAryNeedClassify(srcBillOrTranstype, destBillOrTranstype, sourceBillVOs, paraVo, -1);
  }
  

  public AggregatedValueObject[] runChangeDataAryNeedClassify(String srcBillOrTranstype, String destBillOrTranstype, AggregatedValueObject[] sourceBillVOs, PfParameterVO paraVo, int classifyMode)
    throws BusinessException
  {
    Logger.debug(">>开始单据VO批量交换=" + srcBillOrTranstype + "到" + destBillOrTranstype);
    long start = System.currentTimeMillis();
    
    String group = null;
    if ((paraVo != null) && (paraVo.m_pkGroup != null)) {
      group = paraVo.m_pkGroup;
    } else {
      group = InvocationInfoProxy.getInstance().getGroupId();
    }
    Map<String, AggregatedValueObject[]> vosWithDesttype;
    if (!PfUtilBaseTools.isTranstype(destBillOrTranstype))
    {
      ClassifyContext context = new ClassifyContext();
      context.setClassifyMode(classifyMode);
      context.setDestBilltype(destBillOrTranstype);
      context.setSrcBilltype(srcBillOrTranstype);
      context.setPk_group(group);
      context.setSrcBillVOs(sourceBillVOs);
      IPfBusiflowService busiflowService = new PfBusiflowServiceImpl();
      vosWithDesttype = busiflowService.fillRetVOsWithDestTrantype(context);
    } else {
      vosWithDesttype = new HashMap();
      vosWithDesttype.put(destBillOrTranstype, sourceBillVOs);
    }
    

    ArrayList<AggregatedValueObject> destBillVOAry = new ArrayList();
    
    for (Iterator iterator = vosWithDesttype.keySet().iterator(); iterator.hasNext();) {
      String desttype = (String)iterator.next();
      ArrayList<AggregatedValueObject> tmpDestBillVOAry = changeDataWithDesttype(srcBillOrTranstype, desttype, (AggregatedValueObject[])vosWithDesttype.get(desttype), paraVo);
      
      destBillVOAry.addAll(tmpDestBillVOAry);
    }
    
    AggregatedValueObject[] retDestVOs = PfUtilBaseTools.createArrayWithBilltype(destBillOrTranstype, destBillVOAry.size());
    retDestVOs = (AggregatedValueObject[])destBillVOAry.toArray(retDestVOs);
    

    retDestVOs = new BillSplitHelper().splitBill(retDestVOs);
    Logger.debug(">>结束单据VO批量交换=" + srcBillOrTranstype + "到" + destBillOrTranstype + ",耗时=" + (System.currentTimeMillis() - start) + "ms");
    
    return retDestVOs;
  }
  



  private ArrayList<AggregatedValueObject> changeDataWithDesttype(String srcBillOrTranstype, String destBillOrTranstype, AggregatedValueObject[] sourceBillVOs, PfParameterVO paraVo)
    throws BusinessException
  {
    HashMap<String, ArrayList<AggregatedValueObject>> srcVOMap = new HashMap();
    splitSrcVOsByBusiTransType(srcBillOrTranstype, sourceBillVOs, srcVOMap);
    

    HashMap<ExchangeVO, ArrayList<AggregatedValueObject>> ruleVOMap = new HashMap();
    String group = null;
    if ((paraVo != null) && (paraVo.m_pkGroup != null)) {
      group = paraVo.m_pkGroup;
    } else
      group = InvocationInfoProxy.getInstance().getGroupId();
    findVOConversionFromDBAry(destBillOrTranstype, srcVOMap, ruleVOMap, group);
    

    ArrayList<AggregatedValueObject> destBillVOAry = new ArrayList();
    Iterator<ExchangeVO> iter = ruleVOMap.keySet().iterator();
    

    BillMappingConvertor bmc = createChangeImpl(srcBillOrTranstype, destBillOrTranstype, paraVo);
    
    while (iter.hasNext()) {
      ExchangeVO chgVO = (ExchangeVO)iter.next();
      ArrayList<AggregatedValueObject> tmpSrcVOs = (ArrayList)ruleVOMap.get(chgVO);
      if ((tmpSrcVOs != null) && (tmpSrcVOs.size() != 0))
      {



        initBMC(bmc, chgVO);
        

        AggregatedValueObject[] destBillVOs = bmc.retChangeBusiVOs(srcBillOrTranstype, destBillOrTranstype, (AggregatedValueObject[])tmpSrcVOs.toArray(new AggregatedValueObject[0]));
        
        if (destBillVOs != null)
          destBillVOAry.addAll(Arrays.asList(destBillVOs));
      } }
    return destBillVOAry;
  }
  

  private String getBusiTransTypeOfBillVO(String srcBillOrTranstype, AggregatedValueObject srcVO)
  {
    PublicHeadVO standHeadVo = new PublicHeadVO();
    
    PfUtilBaseTools.getHeadInfoByMeta(standHeadVo, srcVO, srcBillOrTranstype);
    
    String busitype = null;
    if (StringUtil.isEmptyWithTrim(standHeadVo.businessType)) {
      busitype = "KHHH0000000000000001";
    } else {
      busitype = standHeadVo.businessType;
    }
    String strType = busitype + "," + standHeadVo.billType;
    if ((standHeadVo.transType != null) && (!standHeadVo.billType.equals(standHeadVo.transType)))
      strType = strType + "," + standHeadVo.transType;
    return strType;
  }
  

  private void splitSrcVOsByBusiTransType(String srcBillOrTranstype, AggregatedValueObject[] sourceBillVOs, HashMap<String, ArrayList<AggregatedValueObject>> srcVOMap)
  {
    for (AggregatedValueObject srcVO : sourceBillVOs) {
      String strType = getBusiTransTypeOfBillVO(srcBillOrTranstype, srcVO);
      if (!srcVOMap.containsKey(strType)) {
        ArrayList<AggregatedValueObject> voList = new ArrayList();
        srcVOMap.put(strType, voList);
      }
      ((ArrayList)srcVOMap.get(strType)).add(srcVO);
    }
  }
  























  private BillMappingConvertor createChangeImpl(String srcBillOrTranstype, String destBillOrTranstype, PfParameterVO paraVo)
    throws BusinessException
  {
    BillMappingConvertor bmc = new BillMappingConvertor(new FormulaParse());
    
    initBMCWithRules2(srcBillOrTranstype, destBillOrTranstype, bmc);
    
    initConversionEnv(paraVo, bmc);
    


    bmc.initFormulaParse();
    
    return bmc;
  }
  




  private void initBMC(BillMappingConvertor bmc, ExchangeVO chgVo)
  {
    ArrayList<String[]> aRules = new ArrayList();
    ArrayList<String[]> mRules = new ArrayList();
    ArrayList<String> fRules = new ArrayList();
    for (ExchangeRuleVO ruleVO : chgVo.getRuleVOList()) {
      if (ruleVO.getRuleType().intValue() == RuleTypeEnum.ASSIGN.toInt()) {
        aRules.add(new String[] { ruleVO.getDest_attr(), ruleVO.getRuleData() });
      } else if (ruleVO.getRuleType().intValue() == RuleTypeEnum.MOVE.toInt()) {
        mRules.add(new String[] { ruleVO.getDest_attr(), ruleVO.getRuleData() });
      } else if (ruleVO.getRuleType().intValue() == RuleTypeEnum.FORMULA.toInt()) {
        fRules.add(ruleVO.getDest_attr() + "->" + ruleVO.getRuleData());
      }
    }
    
    bmc.setAssignRules((String[][])aRules.toArray(new String[0][0]));
    bmc.setMoveRules((String[][])mRules.toArray(new String[0][0]));
    bmc.setFormulaRules((String[])fRules.toArray(new String[0]));
    
    bmc.getSplitVOList().clear();
    bmc.getSplitVOList().addAll(chgVo.getSplitItemVOList());
    
    bmc.setBackClass(chgVo.getBackClass());
    bmc.setFrontClass(chgVo.getFrontClass());
    bmc.setReserveBackClass(chgVo.getReserveBackClass());
    bmc.setReserveFrontClass(chgVo.getReserveFrontClass());
  }
  

  private void initBMCWithRules2(String srcBillOrTranstype, String destBillOrTranstype, BillMappingConvertor bmc)
  {
    ArrayList<FunctionVO> alSrcFuncVOs = PfDataCache.getFunctionsOfBilltype(srcBillOrTranstype);
    ArrayList<FunctionVO> alDestFuncVOs = PfDataCache.getFunctionsOfBilltype(destBillOrTranstype);
    ArrayList<FunctionVO> alFuncVOs = new ArrayList();
    alFuncVOs.addAll(alSrcFuncVOs);
    alFuncVOs.addAll(alDestFuncVOs);
    bmc.setUserDefineFunctions(PfUtilBaseTools.changeFunctionVOs(alFuncVOs));
    bmc.setSourceBilltype(srcBillOrTranstype);
    BilltypeVO billtypeVO = PfDataCache.getBillTypeInfo(new BillTypeCacheKey().buildBilltype(destBillOrTranstype).buildPkGroup(InvocationInfoProxy.getInstance().getGroupId()));
    if ((billtypeVO.getIstransaction() != null) && (billtypeVO.getIstransaction().booleanValue())) {
      bmc.setDestTranstype(destBillOrTranstype);
      bmc.setDestBilltype(billtypeVO.getParentbilltype());
    } else {
      bmc.setDestBilltype(destBillOrTranstype);
    }
  }
  




  private void initConversionEnv(PfParameterVO paraVo, BillMappingConvertor bmc)
  {
    bmc.setSysDate(new UFDate().toString());
    
    if ((paraVo == null) || (paraVo.m_operator == null)) {
      bmc.setSysOperator(InvocationInfoProxy.getInstance().getUserId());
    } else {
      bmc.setSysOperator(paraVo.m_operator);
    }
    bmc.setSysGroup(InvocationInfoProxy.getInstance().getGroupId());
    
    bmc.setSysTime(new UFDateTime(new Date()).toString());
    

    bmc.setBuziDate(new UFDate(InvocationInfoProxy.getInstance().getBizDateTime()));
    

    bmc.setBuziTime(new UFDateTime(InvocationInfoProxy.getInstance().getBizDateTime()));
  }
  




  public String getLoginDs()
  {
    return InvocationInfoProxy.getInstance().getUserDataSource();
  }
  

  public ExchangeVO queryMostSuitableExchangeVO(String srcType, String destType, String pkBusitype, String pkgroup, ExchangeVO excludeVO)
    throws BusinessException
  {
    String wsql = "(pk_busitype ='~' ";
    if (!StringUtil.isEmptyWithTrim(pkBusitype)) {
      wsql = wsql + " or pk_busitype='" + pkBusitype + "'";
    }
    wsql = wsql + ") and (pk_group ='~' ";
    if (!StringUtil.isEmptyWithTrim(pkgroup)) {
      wsql = wsql + " or pk_group='" + pkgroup + "'";
    }
    wsql = wsql + ") ";
    
    String billTypeWhereSQL = PFExchangeUtil.getBlurMatchExchangeByBillTypeSQL(srcType, destType);
    Collection<ExchangeVO> coRet = ((IUAPQueryBS)NCLocator.getInstance().lookup(IUAPQueryBS.class)).retrieveByClause(ExchangeVO.class, wsql + billTypeWhereSQL);
    


    ExchangeVO resultVO = PFExchangeUtil.getMostExactExchangeVO(coRet, excludeVO);
    
    ExchangeVO returnvo = loadExchangeVOWithDetail(resultVO);
    return returnvo;
  }
  
  public ExchangeVO loadExchangeVOWithDetail(final ExchangeVO resultVO) throws BusinessException {
    if (resultVO != null)
    {
      resultVO.getRuleVOList().clear();
      resultVO.getSplitVOList().clear();
      resultVO.getSplitItemVOList().clear();
      


      IRequestDataCacheKey key = new CondStringKey("PfExchangeServiceImpl.loadExchangeVOWithDetail", new String[] { resultVO.getPrimaryKey() });
      ICacheDataQueryCallback<ExchangeVO> callback = new ICacheDataQueryCallback()
      {
        public ExchangeVO queryData() throws BusinessException
        {
          ExchangeVO queryResult = (ExchangeVO)resultVO.clone();
          
          return PfExchangeServiceImpl.this._load(queryResult);
        }
        
      };
      return (ExchangeVO)PFRequestDataCacheProxy.get(key, callback);
    }
    return resultVO;
  }
  
  private ExchangeVO _load(ExchangeVO resultVO) throws BusinessException
  {
    Logger.debug("PfExchangeServiceImpl.loadExchangeVOWitheDetail begins");
    
    String sql = "select " + ExchangeRuleVOListProcessor.getFieldString() + " from pub_vochange_b where pk_vochange=?";
    SQLParameter pk_vochangeParam = new SQLParameter();
    pk_vochangeParam.addParam(resultVO.getPrimaryKey());
    
    List<ExchangeRuleVO> coRules = (List)new BaseDAO().executeQuery(sql, pk_vochangeParam, new ExchangeRuleVOListProcessor());
    
    resultVO.getRuleVOList().addAll(coRules);
    Collection<ExchangeSplitVO> coSplitVOs = ((IUAPQueryBS)NCLocator.getInstance().lookup(IUAPQueryBS.class)).retrieveByClause(ExchangeSplitVO.class, "pk_vochange=?", null, pk_vochangeParam);
    
    resultVO.getSplitVOList().addAll(coSplitVOs);
    HashMap<String, Integer> splitTimespaceMap = new HashMap();
    for (ExchangeSplitVO exchangeSplitVO : coSplitVOs) {
      splitTimespaceMap.put(exchangeSplitVO.getPk_vosplititem(), exchangeSplitVO.getTimespace());
    }
    Collection<SplitItemVO> coSplitItemVos = ((IUAPQueryBS)NCLocator.getInstance().lookup(IUAPQueryBS.class)).retrieveByClause(SplitItemVO.class, "pk_vosplititem in (select pk_vosplititem from pub_vochange_s where pk_vochange=?)", null, pk_vochangeParam);
    
    for (SplitItemVO splitItemVO : coSplitItemVos) {
      Integer timeSpace = (Integer)splitTimespaceMap.get(splitItemVO.getPk_vosplititem());
      splitItemVO.setTimeSpace(timeSpace == null ? 0 : timeSpace.intValue());
    }
    resultVO.getSplitItemVOList().addAll(coSplitItemVos);
    
    Logger.debug("PfExchangeServiceImpl.loadExchangeVOWitheDetail ends");
    
    return resultVO;
  }
  






  private boolean isLastSuitable(ExchangeVO resultVO, ExchangeVO exchangeVO)
  {
    if (!isEqual(resultVO.getSrc_transtype(), exchangeVO.getSrc_transtype())) {
      if (!StringUtil.isEmptyWithTrim(resultVO.getSrc_transtype()))
        return false;
      return true; }
    if (!isEqual(resultVO.getDest_transtype(), exchangeVO.getDest_transtype())) {
      if (!StringUtil.isEmptyWithTrim(resultVO.getDest_transtype()))
        return false;
      return true; }
    if (StringUtil.isEmptyWithTrim(resultVO.getPk_busitype()))
    {
      if ((!StringUtil.isEmptyWithTrim(resultVO.getPk_group())) && (StringUtil.isEmptyWithTrim(exchangeVO.getPk_group())))
      {
        return false; }
      return true;
    }
    return false;
  }
  







  private static boolean isEqual(String str1, String str2)
  {
    if (!StringUtil.isEmptyWithTrim(str1)) {
      if (StringUtil.isEmptyWithTrim(str2))
        return false;
      return str1.trim().equals(str2.trim());
    }
    if (StringUtil.isEmptyWithTrim(str2))
      return true;
    return false;
  }
  











  public ExchangeVO findVOConversionFromDB(String srcBillOrTranstype, String destBillOrTranstype, AggregatedValueObject sourceBillVO, String pk_group)
    throws BusinessException
  {
    String busi_trans_type = getBusiTransTypeOfBillVO(srcBillOrTranstype, sourceBillVO);
    String[] typeAry = busi_trans_type.split(",");
    String busitype = typeAry[0];
    

    ExchangeVO chgVo = queryMostSuitableExchangeVO(srcBillOrTranstype, destBillOrTranstype, busitype, pk_group, null);
    
    if (chgVo == null) {
      throw new PFBusinessException(NCLangResOnserver.getInstance().getStrByID("pfworkflow", "PfExchangeServiceImpl-0000", null, new String[] { srcBillOrTranstype, destBillOrTranstype }));
    }
    return chgVo;
  }
  
  private void findVOConversionFromDBAry(String destBillOrTranstype, HashMap<String, ArrayList<AggregatedValueObject>> srcVOMap, HashMap<ExchangeVO, ArrayList<AggregatedValueObject>> ruleVOMap, String group)
    throws BusinessException
  {
    if (srcVOMap == null)
      return;
    String types = null;
    String[] aryType = null;
    String busitype = null;
    String srcBillOrTranstype = null;
    if (ruleVOMap == null) {
      ruleVOMap = new HashMap();
    }
    

    HashMap<String, ExchangeVO> VOchangeCacheMap = new HashMap();
    
    for (Iterator<String> iter = srcVOMap.keySet().iterator(); iter.hasNext();) {
      types = (String)iter.next();
      aryType = types.split(",");
      busitype = aryType[0];
      srcBillOrTranstype = aryType.length > 2 ? aryType[2] : aryType[1];
      String key = fetchVOchangeMapKey(srcBillOrTranstype, destBillOrTranstype, busitype, group, null);
      ExchangeVO chgVo = null;
      if (VOchangeCacheMap.containsKey(key)) {
        chgVo = (ExchangeVO)VOchangeCacheMap.get(key);
      }
      else {
        chgVo = queryMostSuitableExchangeVO(srcBillOrTranstype, destBillOrTranstype, busitype, group, null);
        if (chgVo == null)
          throw new PFBusinessException(NCLangResOnserver.getInstance().getStrByID("busitype", "busitypehint-000013", null, new String[] { srcBillOrTranstype, destBillOrTranstype }));
        VOchangeCacheMap.put(key, chgVo);
      }
      
      if (ruleVOMap.containsKey(chgVo)) {
        ((ArrayList)ruleVOMap.get(chgVo)).addAll((Collection)srcVOMap.get(types));
      } else {
        ruleVOMap.put(chgVo, (ArrayList)((ArrayList)srcVOMap.get(types)).clone());
      }
    }
  }
  


  private String fetchVOchangeMapKey(String srcType, String destType, String pkBusitype, String pkgroup, ExchangeVO excludeVO)
  {
    String key = (StringUtil.isEmptyWithTrim(srcType) ? "" : srcType) + (StringUtil.isEmptyWithTrim(destType) ? "" : destType) + (StringUtil.isEmptyWithTrim(pkBusitype) ? "" : pkBusitype) + (StringUtil.isEmptyWithTrim(pkgroup) ? "" : pkgroup) + (excludeVO == null ? "" : String.valueOf(excludeVO.getPk_vochange()));
    



    return key;
  }
}
