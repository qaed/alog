package nc.impl.tb.control;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nc.itf.tb.control.IAccessableBusiVO;
import nc.itf.tb.control.IBusiSysExecDataProvider;
import nc.itf.tb.control.IBusiSysReg;
import nc.itf.tb.control.ILinkQuery;
import nc.itf.tb.sysmaintain.BdContrastCache;
import nc.itf.tb.sysmaintain.BusiSysReg;
import nc.ms.mdm.convertor.IStringConvertor;
import nc.ms.mdm.convertor.StringConvertorFactory;
import nc.ms.mdm.cube.CubeServiceGetter;
import nc.ms.tb.control.BudgetControlCTL;
import nc.ms.tb.control.CtlBdinfoCTL;
import nc.ms.tb.control.CtlSchemeCTL;
import nc.ms.tb.control.CtrltacticsCache;
import nc.ms.tb.pub.NtbBSLangRes;
import nc.ms.tb.pub.NtbSuperDMO;
import nc.ms.tb.rule.SubLevelOrgGetter;
import nc.ms.tb.rule.fmlset.FormulaParser;
import nc.pubitf.bbd.CurrtypeQuery;
import nc.ui.bank_cvp.formulainterface.RefCompilerClient;
import nc.vo.bank_cvp.compile.datastruct.ArrayValue;
import nc.vo.mdm.cube.CubeDef;
import nc.vo.mdm.cube.DataCell;
import nc.vo.mdm.cube.DimVector;
import nc.vo.mdm.cube.ICubeDataSet;
import nc.vo.mdm.pub.NtbLogger;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.vo.tb.control.CtrlSchemeVO;
import nc.vo.tb.control.IdBdcontrastVO;
import nc.vo.tb.control.IdSysregVO;
import nc.vo.tb.obj.NtbParamVO;
import nc.vo.tb.rule.IdCtrlformulaVO;
import nc.vo.tb.rule.IdCtrlschemeVO;
import nc.vo.tb.task.MdTask;

public class LinkQueryImpl implements ILinkQuery {
	public LinkQueryImpl() {
	}

	public NtbParamVO[] getLinkDatas(IAccessableBusiVO[] busivo) throws BusinessException {
		try {
			NtbParamVO[] returnvo = null;
			IdCtrlschemeVO[] schemes = BudgetControlCTL.filterScheme(busivo);
			if ((schemes == null) || (schemes.length == 0)) {
				return returnvo;
			}
			HashMap<IdCtrlschemeVO, IAccessableBusiVO[]> hashEffect = BudgetControlCTL.getEffectVOs(schemes, busivo, false, true);
			ArrayList<IdCtrlschemeVO> c_list = new ArrayList();
			ArrayList<IdCtrlschemeVO> zero_list = new ArrayList();
			Iterator iter = hashEffect.entrySet().iterator();
			boolean isExistCommonScheme = false;
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				IdCtrlschemeVO vo = (IdCtrlschemeVO) entry.getKey();
				if (!vo.getSchemetype().equals("TBRULE000SCHEMA_ZERO")) {
					c_list.add(vo);
					isExistCommonScheme = true;
				} else {
					zero_list.add(vo);
				}
			}
			if (!isExistCommonScheme) {
				c_list = zero_list;
			}
			HashMap<String, ArrayList<IdCtrlschemeVO>> schemeMap = new HashMap();
			HashMap<String, IdCtrlformulaVO> formulaMap = new HashMap();

			if ((c_list != null) && (c_list.size() != 0)) {
				returnvo = constructVO((IdCtrlschemeVO[]) c_list.toArray(new IdCtrlschemeVO[0]));
				returnvo = setRunValAndPlanName(returnvo, (IdCtrlschemeVO[]) c_list.toArray(new IdCtrlschemeVO[0]), schemeMap);
				returnvo = setPlanVal(returnvo, schemeMap, formulaMap, c_list);
				//20170222 tsy 过滤多余的vo（找不到对应的规则）
				returnvo = FilterExtraIdCtrlschemeVO(returnvo,formulaMap);
				//20170222 end
				returnvo = combineRunAndReadyData(returnvo, schemeMap, formulaMap);

				returnvo = getBalanceAndCanUseData(returnvo, schemeMap, formulaMap);
				returnvo = overLapedData(returnvo);
				return replaceWithRuleName(returnvo, formulaMap);
			}

			return null;
		} catch (Exception e) {
			throw new BusinessException((e.getMessage() == null) || (e.getMessage().length() == 0) ? "nc.impl.ntb.LinkQueryImpl.getLinkDatas(IAccessableBusiVO paramvo)" + NtbBSLangRes.getInstance().getStrByID("UPPntbbs-000040") : e.getMessage());
		}
	}
	/**
	 * @author tsy 20170222
	 * <p>数据库存在一些在tb_ctrlscheme表可以找到数据，但是在tb_ctrlformula中却没有对应的数据的记录（不知道是不是垃圾数据）。</p>
	 * 如果这些规则对应的单处于保存态（PREFIND）或审批态（UFIND），联查预算的时候就会报错<br/>
	 * 前台：nc.impl.ntb.LinkQueryImpl.getLinkDatas(IAccessableBusiVO paramvo)出现异常<br/>
	 * 后台：NullPointerException
	 * @param voArr 
	 * @param formulaMap 
	 * @return
	 */
	private NtbParamVO[] FilterExtraIdCtrlschemeVO(NtbParamVO[] voArr, Map<String, IdCtrlformulaVO> formulaMap) {
		List<NtbParamVO> ntbParamVOList =Arrays.asList(voArr);
		List<NtbParamVO> tempList = new ArrayList<NtbParamVO>();
		
		Iterator<NtbParamVO> ntbParamVOIterator = ntbParamVOList.iterator();
		while (ntbParamVOIterator.hasNext()) {
			NtbParamVO vo = ntbParamVOIterator.next();
			if (formulaMap.get(vo.getGroupname())==null) {//找不到对应的限制规则,狠心地抛弃它
				continue;
			}
			//只保留有效的vo
			tempList.add(vo);
		}
		return tempList.toArray(new NtbParamVO[0]);
	}

	public UFDouble[] calculateSchemeData(NtbParamVO vo, IdCtrlformulaVO formulaVO, List<IdCtrlschemeVO> schemeList, String methodName) {
		String express = formulaVO.getExpressformula();
		String expressAno = formulaVO.getExpressformula();
		int power = 2;
		Map<String, String> valueMap1 = new HashMap();
		Map<String, String> valueMap2 = new HashMap();
		for (IdCtrlschemeVO schemeVO : schemeList) {
			String value = "";
			String value2 = "";
			if (methodName.equals("UFIND")) {
				value = schemeVO.getMethodname().equals("PREFIND") ? schemeVO.getReadydata().toString() : "0";
			} else if (methodName.equals("PREFIND")) {
				value = schemeVO.getMethodname().equals("UFIND") ? schemeVO.getRundata().toString() : "0";
			}

			valueMap1.put(schemeVO.getVarno(), value);
			value2 = schemeVO.getMethodname().equals("UFIND") ? schemeVO.getRundata().toString() : schemeVO.getReadydata().toString();
			valueMap2.put(schemeVO.getVarno(), value2);
		}

		String express1 = express.split(formulaVO.getCtrlsign())[0];
		String express2 = express.split(formulaVO.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap1);
		express = express1 + formulaVO.getCtrlsign() + express2;

		express1 = expressAno.split(formulaVO.getCtrlsign())[0];
		express2 = expressAno.split(formulaVO.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap2);
		expressAno = express1 + formulaVO.getCtrlsign() + express2;

		String rvaluestr = "0.00";
		String[] expArr = express.split(formulaVO.getCtrlsign());
		String[] expArrStandard = expressAno.split(formulaVO.getCtrlsign());
		try {
			ArrayValue resultPlan = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(expArrStandard[1]));
			ArrayValue result = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(expArr[1]));
			BigDecimal decimal = result.getDecimal()[0];
			BigDecimal decimalPlan = resultPlan.getDecimal()[0];
			double realRes = decimalPlan.doubleValue() - decimal.doubleValue();
			rvaluestr = String.valueOf(realRes);
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}
		UFDouble[] newRunData = new UFDouble[4];
		for (int i = 0; i < 4; i++) {
			newRunData[i] = (i == vo.getCurr_type() ? new UFDouble(rvaluestr).setScale(power, 4) : new UFDouble(0.0D));
		}
		return newRunData;
	}

	public UFDouble getOccupiedData(NtbParamVO vo, IdCtrlformulaVO formulaVO, List<IdCtrlschemeVO> schemeList, UFDouble[] newRunData, UFDouble[] newReadyData) {
		String express = formulaVO.getExpressformula();
		Map<String, String> valueMap2 = new HashMap();

		for (IdCtrlschemeVO schemeVO : schemeList) {
			String value2 = "";
			value2 = schemeVO.getMethodname().equals("UFIND") ? schemeVO.getRundata().toString() : schemeVO.getReadydata().toString();
			valueMap2.put(schemeVO.getVarno(), value2);
		}

		String express1 = express.split(formulaVO.getCtrlsign())[0];
		String express2 = express.split(formulaVO.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap2);
		express = express1 + formulaVO.getCtrlsign() + express2;

		String[] expArr = express.split(formulaVO.getCtrlsign());
		double realRes = 0.0D;
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(expArr[1]));
			BigDecimal decimal = result.getDecimal()[0];
			realRes = decimal.doubleValue() - newRunData[vo.getCurr_type()].doubleValue() - newReadyData[vo.getCurr_type()].doubleValue();
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}
		return new UFDouble(realRes);
	}

	public NtbParamVO[] combineRunAndReadyData(NtbParamVO[] voArr, Map<String, ArrayList<IdCtrlschemeVO>> schemeMap, Map<String, IdCtrlformulaVO> formulaMap) throws BusinessException {
		Map<String, ArrayList<NtbParamVO>> uFindExtraMap = new HashMap();
		Map<String, ArrayList<NtbParamVO>> preFindExtraMap = new HashMap();

		for (NtbParamVO vo : voArr) {
			ArrayList<NtbParamVO> extraParamArr1 = vo.getUfindNtbParamVO();
			if (!uFindExtraMap.containsKey(vo.getGroupname())) {
				uFindExtraMap.put(vo.getGroupname(), extraParamArr1);
			}
			ArrayList<NtbParamVO> extraParamArr2 = vo.getPrefindNtbParamVO();
			if (!preFindExtraMap.containsKey(vo.getGroupname())) {
				preFindExtraMap.put(vo.getGroupname(), extraParamArr2);
			}
		}
		
		for (NtbParamVO vo : voArr) {
			ArrayList<IdCtrlschemeVO> schemeList = (ArrayList) schemeMap.get(vo.getGroupname());
			IdCtrlformulaVO formulaVO = (IdCtrlformulaVO) formulaMap.get(vo.getGroupname());

			UFDouble[] newRunData = calculateSchemeData(vo, formulaVO, schemeList, "UFIND");

			UFDouble[] newReadyData = calculateSchemeData(vo, formulaVO, schemeList, "PREFIND");

			List<IdCtrlformulaVO> fVOList = new ArrayList();
			fVOList.add(formulaVO);
			if (formulaVO.getSchemetype().equals("TBRULEVELSCHEMA_SPEC")) {
				Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap = BudgetControlCTL.getSepRelationScheme(fVOList, schemeList);
				if (relaSchemeMap.size() > 0) {

					boolean isInSchemeList = false;
					for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : relaSchemeMap.entrySet()) {
						List<IdCtrlschemeVO> schemeCList = (List) entry.getValue();
						for (IdCtrlschemeVO schemeCVO : schemeCList) {
							for (NtbParamVO vo0 : voArr) {
								String idx1 = schemeCVO.getStridx();
								String[] idx2 = vo0.getPkDim();

								boolean isInList = true;
								for (int i = 0; i < idx2.length; i++) {
									if (idx1.indexOf(idx2[i]) < 0) {
										isInList = false;
										break;
									}
								}
								if (isInList) {
									isInSchemeList = true;
									break;
								}
							}
							if (isInSchemeList)
								break;
						}
						if (isInSchemeList) {
							break;
						}
					}

					UFDouble[] subLevData1 = new UFDouble[4];
					for (int i = 0; i < 4; i++)
						subLevData1[i] = new UFDouble(0.0D);
					UFDouble[] subLevData2 = new UFDouble[4];
					for (int i = 0; i < 4; i++)
						subLevData2[i] = new UFDouble(0.0D);
					if (!isInSchemeList) {
						for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : relaSchemeMap.entrySet()) {
							UFDouble[] subLev1 = calculateSchemeData(vo, (IdCtrlformulaVO) entry.getKey(), (List) entry.getValue(), "UFIND");
							UFDouble[] subLev2 = calculateSchemeData(vo, (IdCtrlformulaVO) entry.getKey(), (List) entry.getValue(), "PREFIND");

							for (int i = 0; i < 4; i++) {
								subLevData1[i] = subLevData1[i].add(subLev1[i]);
								subLevData2[i] = subLevData2[i].add(subLev2[i]);
							}
						}
					}
					for (int i = 0; i < 4; i++) {
						newRunData[i] = newRunData[i].sub(subLevData1[i]).setScale(2, 4);
						newReadyData[i] = newReadyData[i].sub(subLevData2[i]).setScale(2, 4);
					}
				}
			}

			vo.setRundata(newRunData);
			vo.setReadydata(newReadyData);
			vo.setUfindNtbParamVO((ArrayList) uFindExtraMap.get(vo.getGroupname()));
			vo.setPrefindNtbParamVO((ArrayList) preFindExtraMap.get(vo.getGroupname()));
		}
		return voArr;
	}

	public NtbParamVO[] overLapedData(NtbParamVO[] returnvo) throws Exception {
		HashMap<String, NtbParamVO> dataMap = new HashMap();
		ArrayList<NtbParamVO> vosList = new ArrayList();
		for (int n = 0; n < (returnvo == null ? 0 : returnvo.length); n++) {
			NtbParamVO vo = returnvo[n];
			String groupName = vo.getGroupname();
			if (dataMap.get(groupName) == null) {
				dataMap.put(groupName, vo);
			}
		}
		Iterator iter = dataMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			NtbParamVO value = (NtbParamVO) entry.getValue();
			vosList.add(value);
		}

		return (NtbParamVO[]) vosList.toArray(new NtbParamVO[0]);
	}

	public NtbParamVO[] replaceWithRuleName(NtbParamVO[] returnvo, HashMap<String, IdCtrlformulaVO> formulaMap) {
		for (int i = 0; i < (returnvo == null ? 0 : returnvo.length); i++) {
			String pk_formula = returnvo[i].getGroupname();
			returnvo[i].setGroupname(formulaMap.get(pk_formula) == null ? "" : ((IdCtrlformulaVO) formulaMap.get(pk_formula)).getCtrlname());
		}
		return returnvo;
	}

	public NtbParamVO[] setPlanVal(NtbParamVO[] returnvo, HashMap<String, ArrayList<IdCtrlschemeVO>> schemeMap, HashMap<String, IdCtrlformulaVO> formulaMap, List<IdCtrlschemeVO> c_list) throws Exception {
		try {
			NtbSuperDMO dmo = new NtbSuperDMO();
			StringBuffer buffer = new StringBuffer();
			IdCtrlformulaVO[] formulavos = null;
			for (int i = 0; i < returnvo.length; i++) {
				if (buffer.length() == 0) {
					buffer.append(" pk_obj in ('");
					buffer.append(returnvo[i].getGroupname());
					buffer.append("'");

				} else if (buffer.toString().indexOf(returnvo[i].getGroupname()) < 0) {

					buffer.append(",'");
					buffer.append(returnvo[i].getGroupname());
					buffer.append("'");
				}
			}
			if (buffer.length() != 0) {
				buffer.append(")");
				formulavos = (IdCtrlformulaVO[]) dmo.queryByWhereClause(IdCtrlformulaVO.class, buffer.toString());
			}

			for (IdCtrlformulaVO vo : formulavos) {
				if (vo.getSchemetype().equals("TBRULE000SCHEMA_FLEX")) {
					String formulaExpress = vo.getExpressformula();
					String pk_cube = vo.getPk_cube();
					String pk_dimvector = vo.getPk_dimvector();
					IStringConvertor cvt = StringConvertorFactory.getConvertor(DimVector.class);
					DimVector dimvector = (DimVector) cvt.fromString(pk_dimvector);
					CubeDef cubedef = CubeServiceGetter.getCubeDefQueryService().queryCubeDefByPK(pk_cube);

					ArrayList<DimVector> newDatacells = new ArrayList();
					newDatacells.add(dimvector);
					ICubeDataSet dataset = CubeServiceGetter.getDataSetService().queryDataSet(cubedef, newDatacells);
					DataCell datacell = dataset.getDataCell(dimvector);
					ArrayList<CtrlSchemeVO> vos = new ArrayList();
					CtrlSchemeVO _vo = new CtrlSchemeVO(formulaExpress, vo.getPk_parent(), datacell, vo.getPk_plan());
					vos.add(_vo);
					formulaExpress = formulaExpress.replaceAll("FLEXEXPRESS\\(\\)", CtlSchemeCTL.parseFlexAlgorithm(vos));
					vo.setExpressformula(formulaExpress);
				}
			}

			for (IdCtrlformulaVO formulaVO : formulavos) {
				formulaMap.put(formulaVO.getPrimaryKey(), formulaVO);
			}
			for (int i = 0; i < returnvo.length; i++) {
				for (int j = 0; j < formulavos.length; j++) {
					if (formulavos[j].getPrimaryKey().equals(returnvo[i].getGroupname())) {
						returnvo[i].setPlanData(getPlanData(formulavos[j], (List) schemeMap.get(formulavos[j].getPrimaryKey()), returnvo[i]));
						returnvo[i].setCtrlData(getCtrlData(formulavos[j], (List) schemeMap.get(formulavos[j].getPrimaryKey()), returnvo[i]));
						break;
					}
				}
			}
			return returnvo;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	public NtbParamVO[] getBalanceAndCanUseData(NtbParamVO[] returnvo, HashMap<String, ArrayList<IdCtrlschemeVO>> schemeMap, HashMap<String, IdCtrlformulaVO> formulaMap) throws Exception {
		try {
			for (int i = 0; i < returnvo.length; i++) {
				List<IdCtrlschemeVO> schemeList = (List) schemeMap.get(returnvo[i].getGroupname());
				IdCtrlformulaVO formulaVO = (IdCtrlformulaVO) formulaMap.get(returnvo[i].getGroupname());

				UFDouble balance = getBalance(returnvo[i], formulaVO, schemeList);
				returnvo[i].setBalance(balance);
				UFDouble useData = getCanUseData(returnvo[i], formulaVO, schemeList);
				returnvo[i].setCanuseData(useData);
			}

			return returnvo;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	private UFDouble getCtrlData(IdCtrlformulaVO vo, List<IdCtrlschemeVO> c_list, NtbParamVO returnvo) throws BusinessException {
		UFDouble planData = null;

		int power = getPlanData(vo, c_list, returnvo).getPower();
		String formula = vo.getExpressformula();

		Map<String, String> valueMap2 = new HashMap();

		for (IdCtrlschemeVO schemeVO : c_list) {
			valueMap2.put(schemeVO.getVarno(), "0");
		}

		String express1 = formula.split(vo.getCtrlsign())[0];
		String express2 = formula.split(vo.getCtrlsign())[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap2);
		formula = express1 + vo.getCtrlsign() + express2;
		String ctrlDataFml = formula.split(vo.getCtrlsign())[0];
		String rightExpr = formula.split(vo.getCtrlsign())[1];
		String realValue = "0.00";
		try {
			ArrayValue resultLeft = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(ctrlDataFml));
			ArrayValue resultRight = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(rightExpr));

			double occupiedValue = resultRight.getDecimal()[0].doubleValue();
			double leftValue = resultLeft.getDecimal()[0].doubleValue();
			realValue = String.valueOf(leftValue - occupiedValue);
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}

		planData = new UFDouble(realValue);
		return planData.setScale(power, 4);
	}

	private UFDouble getPlanData(IdCtrlformulaVO vo, List<IdCtrlschemeVO> c_list, NtbParamVO returnvo) throws BusinessException {
		UFDouble planData = null;

		String[] expData = getExpData(vo);

		planData = new UFDouble(expData[0]);

		List<IdCtrlschemeVO> newCList = new ArrayList();
		for (IdCtrlschemeVO cVO : c_list) {
			boolean isInList = true;

			String idx1 = cVO.getStridx();
			String[] idx2 = returnvo.getPkDim();

			for (int i = 0; i < idx2.length; i++) {
				if (idx1.indexOf(idx2[i]) < 0) {
					isInList = false;
					break;
				}
			}
			if (isInList) {
				newCList.add(cVO);
			}
		}
		if (vo.getSchemetype().equals("TBRULEVELSCHEMA_SPEC")) {
			List<IdCtrlformulaVO> fVOList = new ArrayList();
			fVOList.add(vo);
			Map<IdCtrlformulaVO, List<IdCtrlschemeVO>> relaSchemeMap = BudgetControlCTL.getSepRelationScheme(fVOList, newCList);

			List<IdCtrlformulaVO> _vos = new ArrayList();
			if (relaSchemeMap.size() > 0) {
				boolean isInSchemeList = false;
				for (Map.Entry<IdCtrlformulaVO, List<IdCtrlschemeVO>> entry : relaSchemeMap.entrySet()) {
					_vos.add(entry.getKey());
					for (IdCtrlschemeVO vo1 : (List<IdCtrlschemeVO>) entry.getValue()) {
						for (IdCtrlschemeVO vo2 : c_list) {
							if (vo1.getStridx().equals(vo2.getStridx())) {
								isInSchemeList = true;
								break;
							}
						}
						if (isInSchemeList)
							break;
					}
					if (isInSchemeList)
						break;
				}
				if (!isInSchemeList) {
					UFDouble sumDouble = new UFDouble(0);
					for (int n = 0; n < _vos.size(); n++) {
						UFDouble planvalue = ((IdCtrlformulaVO) _vos.get(n)).getPlanvalue();
						sumDouble = sumDouble.add(planvalue);
					}
					planData = planData.add(sumDouble.multiply(-1.0D));
					String express = vo.getExpressformula();
					express = String.valueOf(vo.getPlanvalue().add(sumDouble.multiply(-1.0D))) + express.substring(express.indexOf("*"), express.length());
					vo.setExpressformula(express);
				}
			}
		}

		return planData;
	}

	private String[] getExpData(IdCtrlformulaVO vo) {
		if (vo == null)
			return null;
		String exp = vo.getExpressformula();
		String[] express = exp.split("%");
		if ((express[0] == null) || (express[0].equals("")))
			return null;
		String[] expData = express[0].split("\\*");
		return expData;
	}

	private int getDataPower(NtbParamVO param) {
		int dataPower = 2;
		int type = param.getCurr_type();
		UFDouble zero = new UFDouble(0);
		if (param.getReadydata()[type].compareTo(zero) != 0) {
			int ready_dataPower = param.getReadydata()[type].getPower();
			if (Math.abs(ready_dataPower) > Math.abs(dataPower)) {
				dataPower = ready_dataPower;
			}
		}
		if (param.getRundata()[type].compareTo(zero) != 0) {
			int run_dataPower = param.getRundata()[type].getPower();
			if (Math.abs(run_dataPower) > Math.abs(dataPower)) {
				dataPower = run_dataPower;
			}
		}
		if (param.getCtrlData().compareTo(zero) != 0) {
			int ctrl_dataPower = param.getCtrlData().getPower();
			if (Math.abs(ctrl_dataPower) > Math.abs(dataPower)) {
				dataPower = ctrl_dataPower;
			}
		}
		return dataPower;
	}

	private String getDataWithoutRealValue(String express, List<IdCtrlschemeVO> schemeList, String ctrlSign) {
		Map<String, String> valueMap = new HashMap();
		for (IdCtrlschemeVO schemeVO : schemeList) {

			valueMap.put(schemeVO.getVarno(), "0");
		}

		String express1 = express.split(ctrlSign)[0];
		String express2 = express.split(ctrlSign)[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap);
		express = express1 + ctrlSign + express2;

		String rvaluestr = "0.00";
		String[] expArr = express.split(ctrlSign);
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(expArr[1]));
			BigDecimal decimal = result.getDecimal()[0];
			rvaluestr = decimal.toString();
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}
		return rvaluestr;
	}

	private String getExpRightData(String express, List<IdCtrlschemeVO> schemeList, String ctrlSign) {
		Map<String, String> valueMap = new HashMap();
		for (IdCtrlschemeVO schemeVO : schemeList) {
			String value = schemeVO.getMethodname().equals("UFIND") ? schemeVO.getRundata().toString() : schemeVO.getReadydata().toString();

			valueMap.put(schemeVO.getVarno(), value);
		}

		String express1 = express.split(ctrlSign)[0];
		String express2 = express.split(ctrlSign)[1];
		express2 = CtlSchemeCTL.replaceExpressWithVar(express2, valueMap);
		express = express1 + ctrlSign + express2;

		String rvaluestr = "0.00";
		String[] expArr = express.split(ctrlSign);
		try {
			ArrayValue result = RefCompilerClient.getExpressionResult(FormulaParser.parseToNumSrc(expArr[1]));
			BigDecimal decimal = result.getDecimal()[0];
			rvaluestr = decimal.toString();
		} catch (Exception e) {
			NtbLogger.print(e.getMessage());
		}
		return rvaluestr;
	}

	private UFDouble getCanUseData(NtbParamVO vo, IdCtrlformulaVO formulaVO, List<IdCtrlschemeVO> schemeList) {
		String express = formulaVO.getExpressformula();

		UFDouble rValue = new UFDouble(getExpRightData(express, schemeList, formulaVO.getCtrlsign()));
		UFDouble orginValue = new UFDouble(getDataWithoutRealValue(express, schemeList, formulaVO.getCtrlsign()));
		UFDouble realValue = rValue.sub(orginValue);

		UFDouble valueAbs = new UFDouble(vo.getCtrlData().doubleValue() - realValue.doubleValue());
		UFDouble zero = new UFDouble(0.0D);

		if (valueAbs.compareTo(zero) == 0) {
			return valueAbs.setScale(vo.getPlanData().getPower(), 4);
		}

		return valueAbs.setScale(getDataPower(vo), 4);
	}

	private UFDouble getBalance(NtbParamVO vo, IdCtrlformulaVO formulaVO, List<IdCtrlschemeVO> schemeList) {
		String express = formulaVO.getExpressformula();

		UFDouble rValue = new UFDouble(getExpRightData(express, schemeList, formulaVO.getCtrlsign()));
		UFDouble orginValue = new UFDouble(getDataWithoutRealValue(express, schemeList, formulaVO.getCtrlsign()));
		UFDouble realValue = rValue.sub(orginValue);

		UFDouble valueAbs = new UFDouble(vo.getPlanData().doubleValue() - realValue.doubleValue());
		UFDouble zero = new UFDouble(0.0D);

		if (valueAbs.compareTo(zero) == 0) {
			return valueAbs.setScale(vo.getPlanData().getPower(), 4);
		}

		return valueAbs.setScale(getDataPower(vo), 4);
	}

	private NtbParamVO[] setRunValAndPlanName(NtbParamVO[] paramvos, IdCtrlschemeVO[] ctlvos, HashMap<String, ArrayList<IdCtrlschemeVO>> map) throws Exception {
		try {
			NtbSuperDMO dmo = new NtbSuperDMO();
			StringBuffer bufferPlans = new StringBuffer();
			StringBuffer bufferCtrls = new StringBuffer();

			MdTask[] sdtasks = null;
			IdCtrlschemeVO[] children = null;

			for (int i = 0; i < ctlvos.length; i++) {
				if (ctlvos[i].getPk_plan() != null) {
					if (bufferPlans.length() == 0) {
						bufferPlans.append(" pk_obj in ('");
						bufferPlans.append(ctlvos[i].getPk_plan());
						bufferPlans.append("'");

					} else if (bufferPlans.toString().indexOf(ctlvos[i].getPk_plan()) < 0) {

						bufferPlans.append(",'");
						bufferPlans.append(ctlvos[i].getPk_plan());
						bufferPlans.append("'");
					}
				}
			}
			if (bufferPlans.length() != 0) {
				bufferPlans.append(")");
				sdtasks = (MdTask[]) dmo.queryByWhereClause(MdTask.class, bufferPlans.toString());
			}

			for (int i = 0; i < ctlvos.length; i++) {
				for (int j = 0; j < (sdtasks == null ? 0 : sdtasks.length); j++) {
					if (sdtasks[j].getPrimaryKey().equals(ctlvos[i].getPk_plan())) {
						paramvos[i].setPlanname(sdtasks[j].getObjname());
						break;
					}
				}
			}

			for (int i = 0; i < ctlvos.length; i++) {
				if (bufferCtrls.length() == 0) {
					bufferCtrls.append("pk_ctrlformula in ('");
					bufferCtrls.append(ctlvos[i].getPk_ctrlformula());
					bufferCtrls.append("'");

				} else if (bufferCtrls.toString().indexOf(ctlvos[i].getPk_ctrlformula()) < 0) {

					bufferCtrls.append(",'");
					bufferCtrls.append(ctlvos[i].getPk_ctrlformula());
					bufferCtrls.append("'");
				}
			}
			if (bufferCtrls.length() != 0) {
				bufferCtrls.append(")");
				children = (IdCtrlschemeVO[]) dmo.queryByWhereClause(IdCtrlschemeVO.class, bufferCtrls.toString());
			}

			for (int i = 0; i < children.length; i++) {
				if (map.containsKey(children[i].getPk_ctrlformula())) {
					ArrayList list = (ArrayList) map.get(children[i].getPk_ctrlformula());
					list.add(children[i]);
				} else {
					ArrayList<IdCtrlschemeVO> list = new ArrayList();
					list.add(children[i]);
					map.put(children[i].getPk_ctrlformula(), list);
				}
			}

			for (int i = 0; i < paramvos.length; i++) {
				if (map.containsKey(paramvos[i].getGroupname())) {
					ArrayList schemeitem = (ArrayList) map.get(paramvos[i].getGroupname());

					double value = 0.0D;
					int power = 2;

					UFDouble[] yfb = { new UFDouble(0), new UFDouble(0), new UFDouble(0), new UFDouble(0) };
					ArrayList<IdCtrlschemeVO> tmpList = new ArrayList();
					for (int j = 0; j < schemeitem.size(); j++) {
						IdCtrlschemeVO vo = (IdCtrlschemeVO) schemeitem.get(j);

						if ((vo.getPk_ctrlformula().equals(paramvos[i].getGroupname())) && ("UFIND".equals(vo.getMethodname()))) {
							tmpList.add(vo);
							if ((vo != null) && (vo.getRundata() != null)) {

								value = vo.getRundata().doubleValue();
								power = vo.getRundata().getPower();
							}
						}
					}

					paramvos[i].getUfindNtbParamVO().addAll(Arrays.asList(constructVO((IdCtrlschemeVO[]) tmpList.toArray(new IdCtrlschemeVO[0]))));
				}
			}

			for (int i = 0; i < paramvos.length; i++) {
				if (map.containsKey(paramvos[i].getGroupname())) {
					ArrayList schemeitem = (ArrayList) map.get(paramvos[i].getGroupname());

					double value = 0.0D;
					int power = 2;

					UFDouble[] yfb = { new UFDouble(0), new UFDouble(0), new UFDouble(0), new UFDouble(0) };
					ArrayList<IdCtrlschemeVO> tmpList = new ArrayList();
					for (int j = 0; j < schemeitem.size(); j++) {
						IdCtrlschemeVO vo = (IdCtrlschemeVO) schemeitem.get(j);

						if ((vo.getPk_ctrlformula().equals(paramvos[i].getGroupname())) && ("PREFIND".equals(vo.getMethodname()))) {
							tmpList.add(vo);
							if ((vo != null) && (vo.getReadydata() != null)) {

								value = vo.getReadydata().doubleValue();
								power = vo.getReadydata().getPower();
							}
						}
					}

					paramvos[i].getPrefindNtbParamVO().addAll(Arrays.asList(constructVO((IdCtrlschemeVO[]) tmpList.toArray(new IdCtrlschemeVO[0]))));
				}
			}

			return paramvos;
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw ex;
		}
	}

	private int getCurrtypeDigit(String pk_currtype) throws BusinessException {
		if (pk_currtype == null) {
			return 2;
		}
		return CurrtypeQuery.getInstance().getCurrdigit(pk_currtype);
	}

	public String getBdinfoType(String fromitem, String sysid) throws Exception {
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

	private NtbParamVO[] constructVO(IdCtrlschemeVO[] ctlvos) throws Exception {
		ArrayList<NtbParamVO> vos = new ArrayList();

		for (int i = 0; i < ctlvos.length; i++) {
			NtbParamVO returnvo = new NtbParamVO();

			String[] nameItem = ctlvos[i].getNameidx().split(":");
			String[] stridxItem = ctlvos[i].getStridx().split(":");

			HashMap map = new HashMap();
			for (int j = 0; j < nameItem.length; j++) {
				map.put(stridxItem[j], nameItem[j]);
			}
			returnvo.setPk_Org(ctlvos[i].getPk_org());
			returnvo.setPk_org_book(ctlvos[i].getPk_org());
			returnvo.setPk_accentity(ctlvos[i].getPk_org());
			returnvo.setHashDescription(map);
			returnvo.setSys_id(ctlvos[i].getCtrlsys());
			returnvo.setBill_type(CtlSchemeCTL.parseBillTypes(ctlvos[i].getBilltype()));
			returnvo.setData_attr(ctlvos[i].getCtrlobj());
			returnvo.setData_attrExt(ctlvos[i].getCtrlobjValue());
			returnvo.setDirection(ctlvos[i].getCtrldirection());
			returnvo.setDateType(ctlvos[i].getDatetype());
			String bdinfotype = getBdinfoType(ctlvos[i].getFromitems(), ctlvos[i].getCtrlsys());

			String[] bdinfotypeidx = bdinfotype.split(":");

			returnvo.setTypeDim(bdinfotypeidx);
			returnvo.setPkDim(ctlvos[i].getStridx().split(":"));
			returnvo.setBusiAttrs(ctlvos[i].getFromitems().split(":"));
			returnvo.setCode_dims(ctlvos[i].getCodeidx().split(":"));
			returnvo.setBegDate(ctlvos[i].getStartdate());
			returnvo.setEndDate(ctlvos[i].getEnddate());

			returnvo.setPk_ctrl(ctlvos[i].getPrimaryKey());

			returnvo.setGroupname(ctlvos[i].getPk_ctrlformula());

			returnvo.setCurr_type(ctlvos[i].getCurrtype().intValue());
			returnvo.setMethodCode(ctlvos[i].getMethodname());

			String[] ctrllevels = ctlvos[i].getCtllevels().split(":");
			boolean[] value = new boolean[ctrllevels.length];
			HashMap<String, String[]> leveldownMap = new HashMap();
			for (int j = 0; j < ctrllevels.length; j++) {
				value[j] = UFBoolean.valueOf(ctrllevels[j]).booleanValue();

				if (value[j]) {
					if (!returnvo.getPkDim()[j].equals(returnvo.getPk_Org())) {
						String[] levelDowsPks = CtlBdinfoCTL.getBdChilddataVO(returnvo.getPkDim()[j], returnvo.getBusiAttrs()[j], returnvo.getPk_Org(), returnvo.getSys_id(), true);

						leveldownMap.put(returnvo.getBusiAttrs()[j], levelDowsPks);
					} else {
						SubLevelOrgGetter orgLevGetter = new SubLevelOrgGetter();
						String[] levelDownPks = orgLevGetter.getSubLevelOrgsByOrgAndBd(returnvo.getPk_Org(), returnvo.getBusiAttrs()[j], returnvo.getSys_id());

						leveldownMap.put(returnvo.getBusiAttrs()[j], levelDownPks);
					}
				}
			}
			returnvo.setLowerArrays(leveldownMap);
			if (nc.itf.tb.control.OutEnum.GLSYS.equals(ctlvos[i].getCtrlsys())) {
				CtlBdinfoCTL.getLinkActualPk(ctlvos[i]);
				returnvo.setPkDim(ctlvos[i].getStridx().split(":"));
				HashMap mapGl = returnvo.getHashDescription();
				String[] stridxItemGl = ctlvos[i].getStridx().split(":");
				String[] nameItemGl = ctlvos[i].getNameidx().split(":");

				for (int j = 0; j < nameItemGl.length; j++) {
					mapGl.put(stridxItemGl[j], nameItemGl[j]);
				}
			} else {
				returnvo.setPkDim(ctlvos[i].getStridx().split(":"));
			}
			for (int j = 0; j < ctrllevels.length; j++) {
				value[j] = UFBoolean.valueOf(ctrllevels[j]).booleanValue();
			}

			String billtype = CtlSchemeCTL.parseBillTypes(ctlvos[i].getBilltype());
			if (billtype != null) {
				if (billtype.indexOf(",") <= 0) {
					CtrltacticsCache.getNewInstance();
					HashMap<String, String> actionMap = CtrltacticsCache.getActionByBillTypeAndSysId(returnvo.getSys_id(), billtype, ctlvos[i].getMethodname());

					returnvo.setActionMap(actionMap);
					HashMap<String, HashMap<String, String>> _map = new HashMap();
					_map.put(billtype, actionMap);
					returnvo.setBillTypesActionMap(_map);
				} else {
					String[] billtypes = CtlSchemeCTL.parseBillTypes(ctlvos[i].getBilltype()).split(",");
					HashMap<String, HashMap<String, String>> map1 = new HashMap();
					for (int n = 0; n < billtypes.length; n++) {
						String _billtype = billtypes[n];
						CtrltacticsCache.getNewInstance();
						HashMap<String, String> actionMap = CtrltacticsCache.getActionByBillTypeAndSysId(returnvo.getSys_id(), _billtype, returnvo.getMethodCode());

						map1.put(_billtype, actionMap);
					}
					returnvo.setBillTypesActionMap(map1);
				}
			}
			returnvo.setIncludelower(value);

			String pk_currency = CtlSchemeCTL.getPk_currency(ctlvos[i].getPk_currency(), ctlvos[i].getPk_org(), ctlvos[i].getCtrlsys());

			returnvo.setPk_currency(pk_currency);

			returnvo.setCurr_type(CtlSchemeCTL.getCurrencyType(ctlvos[i].getPk_currency()));

			IBusiSysExecDataProvider exeprovider = getExcProvider(returnvo.getSys_id());

			setIncludeEff(exeprovider, new NtbParamVO[] { returnvo });

			vos.add(returnvo);
		}

		return (NtbParamVO[]) vos.toArray(new NtbParamVO[0]);
	}

	private IBusiSysExecDataProvider getExcProvider(String sys) throws Exception {
		try {
			BusiSysReg sysreg = BusiSysReg.getSharedInstance();
			IdSysregVO[] sysregvos = sysreg.getAllSysVOs();
			for (int i = 0; i < sysregvos.length; i++) {

				if ((sys.equals(sysregvos[i].getSysid())) || (sys.equals(sysregvos[i].getSysname()))) {
					return ((IBusiSysReg) Class.forName(sysregvos[i].getRegclass()).newInstance()).getExecDataProvider();
				}
			}
		} catch (Exception ex) {
			NtbLogger.error(ex);
			throw new Exception(ex);
		}
		return null;
	}

	private NtbParamVO[] setIncludeEff(IBusiSysExecDataProvider exeprovider, NtbParamVO[] params) throws Exception {
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

			if (ctlpoint == 0) {
				isIncludeeff = true;
			} else if (ctlpoint == 1) {
				isIncludeeff = false;
			}
			params[i].setIsUnInure(isIncludeeff);
		}

		return params;
	}
}
