package nc.vo.er.check;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.erm.util.ErAccperiodUtil;
import nc.bs.erm.util.ErUtil;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Log;
import nc.itf.arap.pub.IBxUIControl;
import nc.itf.bd.bankacc.subinfo.IBankAccSubInfoQueryService;
import nc.itf.fi.pub.Currency;
import nc.itf.fi.pub.SysInit;
import nc.itf.uap.pf.IPFConfig;
import nc.jdbc.framework.JdbcSession;
import nc.jdbc.framework.PersistenceManager;
import nc.jdbc.framework.exception.DbException;
import nc.jdbc.framework.processor.ResultSetProcessor;
import nc.pubitf.accperiod.AccountCalendar;
import nc.pubitf.erm.erminit.IErminitQueryService;
import nc.pubitf.erm.matterapp.IErmMatterAppBillQuery;
import nc.pubitf.org.IOrgUnitPubService;
import nc.pubitf.para.SysInitQuery;
import nc.pubitf.uapbd.ISupplierPubService;
import nc.util.erm.costshare.ErmForCShareUtil;
import nc.utils.crosscheckrule.FipubCrossCheckRuleChecker;
import nc.vo.arap.bx.util.BXParamConstant;
import nc.vo.arap.bx.util.BXUtil;
import nc.vo.bd.bankaccount.BankAccSubVO;
import nc.vo.bd.period.AccperiodVO;
import nc.vo.bd.period2.AccperiodmonthVO;
import nc.vo.bd.supplier.finance.SupFinanceVO;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.BusiTypeVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.er.exception.ContrastBusinessException;
import nc.vo.er.exception.ContrastBusinessException.ContrastBusinessExceptionType;
import nc.vo.er.exception.CrossControlMsgException;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.er.util.UFDoubleTool;
import nc.vo.erm.costshare.CShareDetailVO;
import nc.vo.erm.matterapp.AggMatterAppVO;
import nc.vo.erm.matterapp.MatterAppVO;
import nc.vo.erm.matterapp.MtAppDetailVO;
import nc.vo.erm.termendtransact.DataValidateException;
import nc.vo.erm.util.VOUtils;
import nc.vo.fipub.utils.KeyLock;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.ValidationException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.util.BDVersionValidationUtil;
import org.apache.commons.lang.ArrayUtils;

public class VOChecker {
	private List<String> notRepeatFields;
	private FipubCrossCheckRuleChecker crossChecker;

	public VOChecker() {
	}

	public static void prepare(JKBXVO bxvo) throws BusinessException {
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		try {
			prepareForNullJe(bxvo);
			prepareHeader(parentVO, bxvo.getContrastVO());
			prepareBusItemvo(bxvo);
		} catch (ValidationException e) {
			if ((!parentVO.isInit()) && (parentVO.getDjzt().intValue() != 0)) {
				throw ExceptionHandler.handleException(e);
			}
		}
	}

	private void chkIsMustContrast(JKBXVO bxvo) throws BusinessException {
		if (("bx".equals(bxvo.getParentVO().getDjdl())) && (bxvo.getParentVO().getDjzt() != null) && (bxvo.getParentVO().getDjzt().intValue() == 1)) {
			if ((bxvo.getParentVO().getYbje() != null) && (bxvo.getParentVO().getYbje().doubleValue() >= 0.0D)) {
				if ((bxvo.getParentVO().getCjkybje() != null) && (bxvo.getParentVO().getCjkybje().compareTo(new UFDouble(0.0D)) != 0)) {
					return;
				}
				boolean paramIsMustContrast = false;
				try {
					paramIsMustContrast =
							SysInit.getParaBoolean(bxvo.getParentVO().getPk_org(), BXParamConstant.PARAM_IS_FORCE_CONTRAST).booleanValue();
				} catch (Throwable e) {
					ExceptionHandler.consume(e);
				}
				if (paramIsMustContrast) {
					boolean hasJKD =
							((IBxUIControl) NCLocator.getInstance().lookup(IBxUIControl.class)).getJKD(bxvo, bxvo.getParentVO().getDjrq(), null).size() > 0;
					if (hasJKD) {
						throw new ContrastBusinessException(ContrastBusinessException.ContrastBusinessExceptionType.FORCE, NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UPP2011V57-000002"));
					}
				}
			}
		}
	}

	private void prepareAccPeriodBack(JKBXHeaderVO parentVO) throws BusinessException {
		if ((!parentVO.getQcbz().booleanValue()) && (!parentVO.isInit())) {
			AccountCalendar calendar = AccountCalendar.getInstanceByPk_org(parentVO.getPk_org());
			if ((parentVO.getPk_org() != null) && (calendar == null)) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0060"));
			}
			if (null != parentVO.getDjrq()) {
				calendar.setDate(parentVO.getDjrq());
			}
			AccperiodVO accperiod = calendar.getYearVO();
			accperiod.setAccperiodmonth(new AccperiodmonthVO[] { calendar.getMonthVO() });
			parentVO.setKjnd(accperiod.getPeriodyear());
			parentVO.setKjqj(accperiod.getAccperiodmonth()[0].getAccperiodmth());
		}
	}

	private void prepareBackGround(JKBXVO bxvo) throws BusinessException {
		prepareAccPeriodBack(bxvo.getParentVO());
		prepareBusinessType(bxvo);
	}

	private static void prepareForNullJe(JKBXVO bxvo) {
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		String[] jeField = JKBXHeaderVO.getJeField();
		String[] bodyJeField = BXBusItemVO.getBodyJeFieldForDecimal();
		for (String field : jeField) {
			if (parentVO.getAttributeValue(field) == null) {
				parentVO.setAttributeValue(field, UFDouble.ZERO_DBL);
			}
		}
		for (String field : bodyJeField) {
			BXBusItemVO[] bxBusItemVOS = bxvo.getBxBusItemVOS();
			if (bxBusItemVOS != null) {
				for (BXBusItemVO item : bxBusItemVOS) {
					if (item.getAttributeValue(field) == null) {
						item.setAttributeValue(field, UFDouble.ZERO_DBL);
					}
				}
			}
		}
	}

	public static Map<String, List<String>> getCrossItems(JKBXVO bxvo) {
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		BusiTypeVO busTypeVO = BXUtil.getBusTypeVO(parentVO.getDjlxbm(), parentVO.getDjdl());
		String fydwbm = parentVO.getFydwbm();
		String zfdwbm = parentVO.getPk_org();
		String corp = parentVO.getDwbm();
		Map<String, List<String>> corpItems = new HashMap();
		List<String> useentity_billitems = busTypeVO.getUseentity_billitems();
		List<String> payentity_billitems = busTypeVO.getPayentity_billitems();
		List<String> costentity_billitems = busTypeVO.getCostentity_billitems();
		changeItemsToMap(useentity_billitems, corp, corpItems);
		changeItemsToMap(payentity_billitems, zfdwbm, corpItems);
		changeItemsToMap(costentity_billitems, fydwbm, corpItems);
		return corpItems;
	}

	private static void changeItemsToMap(List<String> busiitems, String corp, Map<String, List<String>> corpItems) {
		List<String> newItems = new ArrayList();
		newItems.addAll(busiitems);
		if (!corpItems.containsKey(corp)) {
			corpItems.put(corp, newItems);
		} else {
			List<String> items = (List) corpItems.get(corp);
			items.addAll(newItems);
			corpItems.put(corp, items);
		}
	}

	public void checkkSaveBackground(JKBXVO vo) throws BusinessException {
		if (!vo.getParentVO().isInit()) {
			fillMtapp(vo);
			addMtAppLock(vo);
			checkMtAppTs(vo);
			checkDate(vo);
			checkBillLineAmountFromMtapp(vo);
			addContrastLock(vo);
			checkContrastJkTs(vo);
			if (0 != vo.getParentVO().getDjzt().intValue()) {
				if ((vo.getParentVO().getQcbz() != null) && (vo.getParentVO().getQcbz().booleanValue())) {
					checkQCClose(vo.getParentVO().getPk_org());
				}
				checkHeadItemJe(vo);
				doCrossCheck(vo);
				checkAuditMan(vo);
				checkBillDate(vo.getParentVO());
				checkCurrencyRate(vo.getParentVO());
				chkBankAccountCurrency(vo);
				chkIsMustContrast(vo);
				chkCustomerPayFreezeFlag(vo);
				if (vo.getParentVO().getDjdl().equals("bx")) {
					checkToPublicPay(vo);
				}
				chkCashaccountAndFkyhzh(vo.getParentVO());
			}
			prepareBackGround(vo);
			getBusitype(vo.getParentVO());
		}
	}

	private void fillMtapp(JKBXVO vo) throws BusinessException {
		if ((vo.getParentVO().getPk_item() != null) && (vo.getMt_aggvos() == null)) {
			String pk_item = vo.getParentVO().getPk_item();
			AggMatterAppVO aggvo =
					((IErmMatterAppBillQuery) NCLocator.getInstance().lookup(IErmMatterAppBillQuery.class)).queryBillByPK(pk_item);
			if (aggvo == null) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0106"));
			}
			vo.setMt_aggvos(new AggMatterAppVO[] { aggvo });
		}
	}

	private void checkBillLineAmountFromMtapp(JKBXVO vo) throws BusinessException {
		if (vo.getParentVO().getPk_item() != null) {
			for (BXBusItemVO child : vo.getBxBusItemVOS()) {
				if ((child.getAmount().compareTo(UFDouble.ZERO_DBL) <= 0) && (child.getPk_item() != null)) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0170"));
				}
			}
		}
	}

	private void checkDate(JKBXVO vo) throws BusinessException {
		if (vo.getMt_aggvos() != null) {
			UFDate busiDate = vo.getParentVO().getDjrq();
			for (AggMatterAppVO mtvo : vo.getMt_aggvos()) {
				if (mtvo.getParentVO().getApprovetime().afterDate(busiDate)) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0168"));
				}
			}
		}
	}

	private void addContrastLock(JKBXVO vo) throws BusinessException {
		if ((vo.getContrastVO() != null) && (vo.getContrastVO().length > 0)) {
			List<String> pkList = new ArrayList();
			for (BxcontrastVO contrast : vo.getContrastVO()) {
				pkList.add(contrast.getPk_jkd());
			}
			KeyLock.dynamicLockWithException(pkList);
		}
	}

	private void checkContrastJkTs(JKBXVO vo) throws BusinessException {
		if ((vo.getJkHeadVOs() != null) && (vo.getJkHeadVOs().length > 0)) {
			BDVersionValidationUtil.validateVersion(vo.getJkHeadVOs());
		}
	}

	private void addMtAppLock(JKBXVO vo) throws BusinessException {
		if ((vo.getChildrenVO() != null) && (vo.getChildrenVO().length > 0)) {
			List<String> pkList = new ArrayList();
			List<String> lockList = new ArrayList();
			for (BXBusItemVO busitem : vo.getChildrenVO()) {
				if (busitem.getPk_item() != null) {
					pkList.add(busitem.getPk_item());
				}
			}
			if ((pkList == null) || (pkList.size() == 0)) {
				return;
			}
			for (String pk : pkList) {
				lockList.add("ERM_matterapp-" + pk);
			}
			KeyLock.dynamicLockWithException(lockList);
		}
	}

	public void checkQCClose(String pk_org) throws BusinessException {
		boolean flag = ((IErminitQueryService) NCLocator.getInstance().lookup(IErminitQueryService.class)).queryStatusByOrg(pk_org);
		if (flag == true) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0002"));
		}
	}

	private void checkMtAppTs(JKBXVO vo) throws BusinessException {
		AggMatterAppVO[] mt_aggvos = vo.getMt_aggvos();
		if (ArrayUtils.isEmpty(mt_aggvos)) {
			return;
		}
		String[] pks = VOUtils.getAttributeValues(mt_aggvos, "pk_mtapp_bill");
		AggMatterAppVO[] newMtAppVos =
				((IErmMatterAppBillQuery) NCLocator.getInstance().lookup(IErmMatterAppBillQuery.class)).queryBillByPKs(pks);
		if ((newMtAppVos == null) || (newMtAppVos.length != pks.length)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0106"));
		}
		Map<String, List<AggMatterAppVO>> newMtAppMap =
				VOUtils.changeCollection2MapList(Arrays.asList(newMtAppVos), new String[] { "pk_mtapp_bill" });
		for (AggMatterAppVO oldMtAppVO : mt_aggvos) {
			AggMatterAppVO newVo = (AggMatterAppVO) ((List) newMtAppMap.get(oldMtAppVO.getParentVO().getPrimaryKey())).get(0);
			if (!oldMtAppVO.getParentVO().getTs().equals(newVo.getParentVO().getTs())) {
				throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0107") + oldMtAppVO.getParentVO().getBillno() + NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0108"));
			}
			Map<String, List<CircularlyAccessibleValueObject>> newChildrenMap =
					VOUtils.changeArrayToMapList(newVo.getChildrenVO(), new String[] { "pk_mtapp_detail" });
			for (MtAppDetailVO oldChildVo : oldMtAppVO.getChildrenVO()) {
				if (!oldChildVo.getTs().equals(((CircularlyAccessibleValueObject) ((List) newChildrenMap.get(oldChildVo.getPrimaryKey())).get(0)).getAttributeValue("ts"))) {
					throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0107") + oldMtAppVO.getParentVO().getBillno() + NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0108"));
				}
			}
		}
	}

	private void chkCustomerPayFreezeFlag(JKBXVO bxvo) throws ValidationException {
		String hbbm = bxvo.getParentVO().getHbbm();
		if (org.apache.commons.lang.StringUtils.isEmpty(hbbm))
			return;
		ISupplierPubService qryservice = (ISupplierPubService) NCLocator.getInstance().lookup(ISupplierPubService.class);
		SupFinanceVO[] supfivos = null;
		try {
			supfivos =
					qryservice.getSupFinanceVO(new String[] { hbbm }, bxvo.getParentVO().getFydwbm(), new String[] { "payfreezeflag", "pk_supplier" });
		} catch (BusinessException e) {
			ExceptionHandler.error(e);
		}
		UFBoolean flag = UFBoolean.FALSE;
		if (!ArrayUtils.isEmpty(supfivos)) {
			for (SupFinanceVO vo : supfivos) {
				flag = vo.getPayfreezeflag();
				if ((flag != null) && (flag.booleanValue())) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0160"));
				}
			}
		}
	}

	private void prepareBusinessType(JKBXVO bxvo) {
		JKBXHeaderVO headvo = bxvo.getParentVO();
		if (!bxvo.getParentVO().getQcbz().booleanValue()) {
			try {
				IPFConfig ipf = (IPFConfig) NCLocator.getInstance().lookup(IPFConfig.class);
				String pk_busitype = null;
				if ((!org.apache.commons.lang.StringUtils.isEmpty(headvo.getDjdl())) && (!org.apache.commons.lang.StringUtils.isEmpty(headvo.getDjlxmc())) && (!org.apache.commons.lang.StringUtils.isEmpty(headvo.getCreator()))) {
					if (headvo.getDjdl().equals("bx")) {
						pk_busitype = ipf.retBusitypeCanStart("264X", headvo.getDjlxbm(), headvo.getPk_org(), headvo.getCreator());
					} else if (headvo.getDjdl().equals("jk")) {
						pk_busitype = ipf.retBusitypeCanStart("263X", headvo.getDjlxbm(), headvo.getPk_org(), headvo.getCreator());
					}
					headvo.setBusitype(pk_busitype);
				}
			} catch (Exception e) {
				ExceptionHandler.consume(e);
			}
		}
	}

	private void getBusitype(JKBXHeaderVO parentVO) throws BusinessException, DAOException {
		if ((parentVO.getFkyhzh() != null) && (isInneracc(parentVO.getFkyhzh()).equals("Y"))) {
			IPFConfig pFConfig = (IPFConfig) NCLocator.getInstance().lookup(IPFConfig.class);
			String pk_busiflow = parentVO.getBusitype();
			String trade_type = parentVO.getDjlxbm();
			if (StringUtil.isEmpty(pk_busiflow)) {
				String billtype = parentVO.getDjdl().equals("bx") ? "264X" : "263X";
				String userid = InvocationInfoProxy.getInstance().getUserId();
				String pk_busiflowValue = pFConfig.retBusitypeCanStart(billtype, trade_type, parentVO.getPk_org(), userid);
				if (parentVO.getDjdl().equals("bx")) {
					if (pk_busiflowValue == null) {
						throw ExceptionHandler.createException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0061"));
					}
					parentVO.setBusitype(pk_busiflowValue);
					new BaseDAO().updateVO(parentVO, new String[] { "busitype" });
				}
			}
		}
	}

	public String isInneracc(String pk_account) {
		String sql = "select  isinneracc from bd_bankaccbas  where pk_bankaccbas ='" + pk_account + "'";
		PersistenceManager manager = null;
		try {
			manager = PersistenceManager.getInstance(InvocationInfoProxy.getInstance().getUserDataSource());
			JdbcSession session = manager.getJdbcSession();
			String str1 = (String) session.executeQuery(sql, new ResultSetProcessor() {
				private static final long serialVersionUID = 4040766420632132035L;

				public Object handleResultSet(ResultSet rs) throws SQLException {
					String flag = "N";
					if (rs.next()) {
						flag = rs.getString("isinneracc").toString();
					}
					return flag;
				}
			});
			return str1;
		} catch (DbException e) {
			Log.getInstance(getClass()).error(e);
		} finally {
			if (manager != null)
				manager.release();
		}
		return null;
	}

	public void checkSave(JKBXVO bxvo) throws BusinessException {
		prepare(bxvo);
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		BXBusItemVO[] childrenVO = bxvo.getBxBusItemVOS();
		if (!parentVO.isInit()) {
			checkValidHeader(parentVO);
			checkValidChildrenVO(childrenVO);
			checkCShareDetail(bxvo);
			checkCurrency(parentVO);
			checkFinRange(bxvo, parentVO);
			checkHeadItemJe(bxvo);
			checkValidFinItemVO(bxvo);
			checkExpamortizeinfo(bxvo);
		} else {
			checkRepeatCShareDetailRow(bxvo);
		}
	}

	public void checkRepeatCShareDetailRow(JKBXVO bxvo) throws ValidationException {
		CShareDetailVO[] cShareVos = bxvo.getcShareDetailVo();
		if (!bxvo.isHasCShareDetail()) {
			return;
		}
		if (bxvo.getParentVO().getDjdl().equals("bx")) {
			List<String> controlKeys = new ArrayList();
			StringBuffer controlKey = null;
			String[] attributeNames = cShareVos[0].getAttributeNames();
			for (int i = 0; i < cShareVos.length; i++) {
				controlKey = new StringBuffer();
				for (int j = 0; j < attributeNames.length; j++) {
					if ((getNotRepeatFields().contains(attributeNames[j])) || (attributeNames[j].startsWith("defitem"))) {
						controlKey.append(cShareVos[i].getAttributeValue(attributeNames[j]));
					}
				}
				if (!controlKeys.contains(controlKey.toString())) {
					controlKeys.add(controlKey.toString());
				} else {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0114"));
				}
			}
		}
	}

	private void checkExpamortizeinfo(JKBXVO bxvo) throws BusinessException {
		if (bxvo.getParentVO().getIsexpamt().equals(UFBoolean.TRUE)) {
			if (nc.vo.er.util.StringUtils.isNullWithTrim(bxvo.getParentVO().getStart_period())) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0109"));
			}
			AccperiodmonthVO accperiodmonthVO = null;
			AccperiodmonthVO startperiodmonthVO = null;
			accperiodmonthVO = ErAccperiodUtil.getAccperiodmonthByUFDate(bxvo.getParentVO().getPk_org(), bxvo.getParentVO().getDjrq());
			startperiodmonthVO = ErAccperiodUtil.getAccperiodmonthByPk(bxvo.getParentVO().getStart_period());
			if (startperiodmonthVO.getYearmth().compareTo(accperiodmonthVO.getYearmth()) < 0) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0110"));
			}
			if ((bxvo.getParentVO().getTotal_period() == null) || (bxvo.getParentVO().getTotal_period().intValue() <= 0)) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0111"));
			}
		} else {
			if (!nc.vo.er.util.StringUtils.isNullWithTrim(bxvo.getParentVO().getStart_period())) {
				bxvo.getParentVO().setStart_period(null);
			}
			if (bxvo.getParentVO().getTotal_period() != null) {
				bxvo.getParentVO().setTotal_period(null);
			}
		}
	}

	private void checkCShareDetail(JKBXVO bxvo) throws ValidationException {
		CShareDetailVO[] cShareVos = bxvo.getcShareDetailVo();
		if ((bxvo.getParentVO().getIscostshare().equals(UFBoolean.TRUE)) && (bxvo.getParentVO().getYbje().compareTo(UFDouble.ZERO_DBL) < 0)) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0007"));
		}
		if (!bxvo.isHasCShareDetail()) {
			return;
		}
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		if (bxvo.getParentVO().getDjdl().equals("bx")) {
			// 20170711 tsy 修改为自定义项12
			// UFDouble total = parentVO.getYbje();
			UFDouble total = new UFDouble(parentVO.getTotal());
			// 20170711 end
			if (total == null) {
				total = new UFDouble(0);
			}
			UFDouble amount = new UFDouble(0);
			UFDouble ratio = new UFDouble(0);
			List<String> controlKeys = new ArrayList();
			StringBuffer controlKey = null;
			String[] attributeNames = cShareVos[0].getAttributeNames();
			for (int i = 0; i < cShareVos.length; i++) {
				UFDouble shareAmount = ErmForCShareUtil.formatUFDouble(cShareVos[i].getAssume_amount(), -99);
				UFDouble shareRatio = ErmForCShareUtil.formatUFDouble(cShareVos[i].getShare_ratio(), -99);
				if (!ErmForCShareUtil.isUFDoubleGreaterThanZero(shareAmount)) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0112"));
				}
				if (!ErmForCShareUtil.isUFDoubleGreaterThanZero(shareRatio)) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0113"));
				}
				amount = amount.add(shareAmount);
				ratio = ratio.add(shareRatio);
				controlKey = new StringBuffer();
				for (int j = 0; j < attributeNames.length; j++) {
					if ((getNotRepeatFields().contains(attributeNames[j])) || (attributeNames[j].startsWith("defitem"))) {
						controlKey.append(cShareVos[i].getAttributeValue(attributeNames[j]));
					}
				}
				if (!controlKeys.contains(controlKey.toString())) {
					controlKeys.add(controlKey.toString());
				} else {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0114"));
				}
			}
			if (total.toDouble().compareTo(amount.toDouble()) != 0) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0115"));
			}
		}
	}

	private void chkCashaccountAndFkyhzh(JKBXHeaderVO headerVO) throws BusinessException {
		String fkyhzh = headerVO.getFkyhzh();
		String pkCashaccount = headerVO.getPk_cashaccount();
		if ((!StringUtil.isEmpty(fkyhzh)) && (!StringUtil.isEmpty(pkCashaccount))) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61_1215_0", "02011v61215-0000"));
		}
	}

	private void chkBankAccountCurrency(JKBXVO vo) throws BusinessException {
		if ((vo == null) || (vo.getParentVO() == null)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0135"));
		}
		JKBXHeaderVO headerVO = vo.getParentVO();
		String pk_bankaccsub = headerVO.getSkyhzh();
		if ((pk_bankaccsub == null) || (pk_bankaccsub.trim().length() == 0)) {
			return;
		}
		String pk_currtype = headerVO.getBzbm();
		IBankAccSubInfoQueryService service =
				(IBankAccSubInfoQueryService) NCLocator.getInstance().lookup(IBankAccSubInfoQueryService.class);
		BankAccSubVO[] vos = service.querySubInfosByPKs(new String[] { pk_bankaccsub });
		if ((vos == null) || (vos.length == 0) || (!pk_currtype.equals(vos[0].getPk_currtype()))) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0136"));
		}
	}

	private FipubCrossCheckRuleChecker getCrossChecker() {
		if (this.crossChecker == null) {
			this.crossChecker = new FipubCrossCheckRuleChecker();
		}
		return this.crossChecker;
	}

	private void doCrossCheck(JKBXVO billVO) throws CrossControlMsgException {
		if (billVO.getHasCrossCheck()) {
			return;
		}
		String retMsg = null;
		try {
			retMsg = getCrossChecker().check(billVO.getParentVO().getPk_org(), billVO.getParentVO().getDjlxbm(), billVO);
		} catch (BusinessException e) {
			ExceptionHandler.handleExceptionRuntime(e);
		}
		if ((retMsg != null) && (retMsg.length() > 0)) {
			throw new CrossControlMsgException(retMsg);
		}
	}

	private void checkAuditMan(JKBXVO bxvo) throws BusinessException {
		String auditman = bxvo.getParentVO().getAuditman();
		if (auditman == null) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000365"));
		}
	}

	private void checkToPublicPay(JKBXVO bxvo) throws BusinessException {
		String receiver = bxvo.getParentVO().getReceiver();
		String hbbm = bxvo.getParentVO().getHbbm();
		if ((receiver != null) && (hbbm != null)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0137"));
		}
		if ((receiver == null) && (hbbm == null)) {
			throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0138"));
		}
	}

	private void checkBillDate(JKBXHeaderVO parentVO) throws ValidationException {
		if (parentVO.getDjrq() == null) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011ermpub0316_0", "02011ermpub0316-0012"));
		}
		String pk_org = parentVO.getPk_org();
		UFDate startDate = null;
		try {
			String yearMonth =
					((IOrgUnitPubService) NCLocator.getInstance().lookup(IOrgUnitPubService.class)).getOrgModulePeriodByOrgIDAndModuleID(pk_org, "2011");
			if ((yearMonth != null) && (yearMonth.length() != 0)) {
				String year = yearMonth.substring(0, 4);
				String month = yearMonth.substring(5, 7);
				if ((year != null) && (month != null)) {
					AccountCalendar calendar = AccountCalendar.getInstanceByPk_org(pk_org);
					if (calendar == null) {
						throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0021"));
					}
					calendar.set(year, month);
					if (calendar.getMonthVO() == null) {
						throw new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0022"));
					}
					startDate = calendar.getMonthVO().getBegindate();
				}
			}
		} catch (BusinessException e) {
			ExceptionHandler.consume(e);
		}
		if (startDate == null) {
			ExceptionHandler.consume(new BusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0001")));
		}
		if (startDate != null) {
			if (parentVO.getQcbz().booleanValue()) {
				if ((parentVO.getDjrq() != null) && (!parentVO.getDjrq().beforeDate(startDate))) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0139"));
				}
			} else if ((parentVO.getDjrq() != null) && (parentVO.getDjrq().beforeDate(startDate))) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0140"));
			}
		} else {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0141"));
		}
	}

	private void checkCurrency(JKBXHeaderVO parentVO) throws BusinessException {
	}

	private void checkCurrencyRate(JKBXHeaderVO parentVO) throws BusinessException {
		UFDouble hl = parentVO.getBbhl();
		UFDouble globalhl = parentVO.getGlobalbbhl();
		UFDouble grouphl = parentVO.getGroupbbhl();
		String paramValue = SysInitQuery.getParaString("GLOBLE00000000000000", "NC002");
		boolean isGlobalmodel = (org.apache.commons.lang.StringUtils.isNotBlank(paramValue)) && (!paramValue.equals("不启用全局本位币"));
		paramValue = SysInitQuery.getParaString(parentVO.getPk_group(), "NC001");
		boolean isGroupmodel = (org.apache.commons.lang.StringUtils.isNotBlank(paramValue)) && (!paramValue.equals("不启用集团本位币"));
		if ((hl == null) || (hl.toDouble().doubleValue() == 0.0D)) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000395"));
		}
		if ((isGlobalmodel) && ((globalhl == null) || (globalhl.toDouble().doubleValue() == 0.0D))) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0142"));
		}
		if ((isGroupmodel) && ((grouphl == null) || (grouphl.toDouble().doubleValue() == 0.0D))) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0143"));
		}
	}

	private static void prepareBusItemvo(JKBXVO bxvo) throws BusinessException {
		BXBusItemVO[] busItemVOs = bxvo.getChildrenVO();
		if ((busItemVOs != null) && (busItemVOs.length != 0)) {
			if (busItemVOs[0].getSzxmid() != null) {
				bxvo.getParentVO().setSzxmid(busItemVOs[0].getSzxmid());
			}
			if (busItemVOs[0].getJobid() != null) {
				bxvo.getParentVO().setJobid(busItemVOs[0].getJobid());
			}
			if (busItemVOs[0].getProjecttask() != null) {
				bxvo.getParentVO().setProjecttask(busItemVOs[0].getProjecttask());
			}
			if (busItemVOs[0].getCashproj() != null) {
				bxvo.getParentVO().setCashproj(busItemVOs[0].getCashproj());
			}
			if (busItemVOs[0].getJkbxr() != null) {
				bxvo.getParentVO().setJkbxr(busItemVOs[0].getJkbxr());
			}
			if (busItemVOs[0].getPk_pcorg() != null) {
				bxvo.getParentVO().setPk_pcorg(busItemVOs[0].getPk_pcorg());
			}
			if (busItemVOs[0].getPk_pcorg_v() != null) {
				bxvo.getParentVO().setPk_pcorg_v(busItemVOs[0].getPk_pcorg_v());
			}
			if (busItemVOs[0].getPk_checkele() != null) {
				bxvo.getParentVO().setPk_checkele(busItemVOs[0].getPk_checkele());
			}
			if (busItemVOs[0].getPk_resacostcenter() != null) {
				bxvo.getParentVO().setPk_resacostcenter(busItemVOs[0].getPk_resacostcenter());
			}
			for (BXBusItemVO item : busItemVOs) {
				UFDouble zero = new UFDouble(0);
				item.setDr(Integer.valueOf(0));
				// 20170712 tsy
				// item.setYbye(item.getYbje());
				// item.setBbye(item.getBbje());
				item.setYbye(new UFDouble((String) item.getDefitem25()));
				item.setBbye(new UFDouble((String) item.getDefitem25()));
				// 20170712 end
				item.setGroupbbye(item.getGroupbbje());
				item.setGlobalbbye(item.getGlobalbbje());
				item.setYjye(item.getYbje());
				if (item.getCjkybje() == null) {
					item.setCjkybje(zero);
				}
				if (item.getCjkbbje() == null) {
					item.setCjkbbje(zero);
					item.setGroupcjkbbje(zero);
					item.setGlobalcjkbbje(zero);
				}
				String[] bodyJeField = BXBusItemVO.getBodyJeFieldForDecimal();
				for (String field : bodyJeField) {
					if (item.getAttributeValue(field) == null) {
						item.setAttributeValue(field, UFDouble.ZERO_DBL);
					}
				}
				if (UFDoubleTool.isZero(item.getCjkybje())) {
					// 20170712 tsy
					// if (item.getYbje().doubleValue() > 0.0D) {
					// item.setZfybje(item.getYbje());
					// item.setZfbbje(item.getBbje());
					if (new UFDouble((String) item.getDefitem25()).doubleValue() > 0.0D) {
						item.setZfybje(new UFDouble((String) item.getDefitem25()));
						item.setZfbbje(new UFDouble((String) item.getDefitem25()));
						// 20170712 end
						item.setGroupzfbbje(item.getGroupbbje());
						item.setGlobalzfbbje(item.getGlobalbbje());
						item.setHkybje(zero);
						item.setHkbbje(zero);
						item.setGrouphkbbje(zero);
						item.setGlobalhkbbje(zero);
					} else {
						item.setHkybje(item.getYbje().abs());
						item.setHkbbje(item.getBbje().abs());
						item.setGrouphkbbje(item.getGroupbbje().abs());
						item.setGlobalhkbbje(item.getGlobalbbje().abs());
						item.setZfybje(zero);
						item.setZfbbje(zero);
						item.setGroupzfbbje(zero);
						item.setGlobalzfbbje(zero);
					}
					// tsy
					// } else if (UFDoubleTool.isXiaoyu(item.getYbje(),
					// item.getCjkybje())) {
				} else if (UFDoubleTool.isXiaoyu(new UFDouble((String) item.getDefitem25()), item.getCjkybje())) {
					item.setZfybje(zero);
					item.setZfbbje(zero);
					item.setGroupzfbbje(zero);
					item.setGlobalzfbbje(zero);
					// tsy
					// item.setHkybje(item.getCjkybje().sub(item.getYbje()));
					// item.setHkbbje(item.getCjkbbje().sub(item.getBbje()));
					item.setHkybje(item.getCjkybje().sub(new UFDouble(item.getDefitem25().toString())));
					item.setHkbbje(item.getCjkybje().sub(new UFDouble(item.getDefitem25().toString())));
					item.setGrouphkbbje(item.getGroupcjkbbje().sub(item.getGroupbbje()));
					item.setGlobalhkbbje(item.getGlobalcjkbbje().sub(item.getGlobalbbje()));
				} else {
					// tsy
					// item.setZfybje(item.getYbje().sub(item.getCjkybje()));
					// item.setZfbbje(item.getBbje().sub(item.getCjkbbje()));
					item.setZfybje(new UFDouble((String) item.getDefitem25()).sub(item.getCjkybje()));
					item.setZfbbje(new UFDouble((String) item.getDefitem25()).sub(item.getCjkbbje()));
					item.setGroupzfbbje(item.getGroupbbje() != null ? item.getGroupbbje().sub(item.getGroupcjkbbje()) : zero);
					item.setGlobalzfbbje(item.getGlobalbbje() != null ? item.getGlobalbbje().sub(item.getGlobalcjkbbje()) : zero);
					item.setHkybje(zero);
					item.setHkbbje(zero);
					item.setGrouphkbbje(zero);
					item.setGlobalhkbbje(zero);
				}
			}
		}
	}

	private static void prepareHeader(JKBXHeaderVO parentVO, BxcontrastVO[] bxcontrastVOs) throws BusinessException {
		if (parentVO == null) {
			return;
		}
		parentVO.setDr(Integer.valueOf(0));
		if ((parentVO.getSpzt() == null) || (parentVO.getSpzt().intValue() != 2)) {
			parentVO.setSpzt(Integer.valueOf(-1));
		}
		parentVO.setQzzt(Integer.valueOf(0));
		parentVO.setPayflag(Integer.valueOf(1));
		if ((parentVO.getDjdl() == null) || (parentVO.getDjzt() == null)) {
			return;
		}
		if ((parentVO.getPk_group() == null) && (parentVO.isInit())) {
			parentVO.setPk_group("0001");
			return;
		}
		parentVO.setContrastenddate(new UFDate("3000-01-01"));
		String djdl = parentVO.getDjdl();
		UFDouble bbhl = parentVO.getBbhl();
		UFDouble globalbbhl = parentVO.getGlobalbbhl();
		UFDouble groupbbhl = parentVO.getGroupbbhl();
		String bzbm = parentVO.getBzbm();
		UFDate djrq = parentVO.getDjrq();
		String zfdwbm = parentVO.getPk_org();
		// 20170712 tsy
		// if ((zfdwbm != null) && (bzbm != null) && (parentVO.getYbje() !=
		// null) && (djrq != null)) {
		// parentVO.setBbje(Currency.computeYFB(zfdwbm, 2, bzbm,
		// parentVO.getYbje(), null, null, null, bbhl, djrq)[2]);
		if ((zfdwbm != null) && (bzbm != null) && (parentVO.getZyx12() != null) && (djrq != null)) {
			parentVO.setBbje(Currency.computeYFB(zfdwbm, 2, bzbm, new UFDouble(parentVO.getZyx12()), null, null, null, bbhl, djrq)[2]);
			parentVO.setYbje(Currency.computeYFB(zfdwbm, 2, bzbm, new UFDouble(parentVO.getZyx12()), null, null, null, bbhl, djrq)[2]);
			// 20170712 end
		}

		if ((!parentVO.isInit()) && (parentVO.getDjzt().intValue() != 0)) {
			if (djdl.equals("jk")) {
				if (parentVO.getIscheck().booleanValue()) {
					parentVO.setYbje(null);
					parentVO.setBbje(null);
					if (parentVO.getZpxe() == null) {
						throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000311"));
					}
					if (parentVO.getZpxe().doubleValue() <= 0.0D) {
						throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000312"));
					}
				} else {
					// 20170712 tsy
					// if (parentVO.getYbje() == null) {
					// throw new
					// ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011",
					// "UPP2011-000313"));
					// }
					// if (parentVO.getYbje() == null) {
					// throw new
					// ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011",
					// "UPP2011-000314"));
					// }
					if (parentVO.getZyx12() == null) {
						throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000313"));
					}
					if (parentVO.getZyx12() == null) {
						throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000314"));
					}
					// 20170712 end
					parentVO.setZpxe(null);
				}
				// 20170712 tsy
				// } else if ((parentVO.getYbje() == null) &&
				// (parentVO.getYbje().doubleValue() < 0.0D)) {
			} else if ((new UFDouble(parentVO.getZyx12()) == null) && (new UFDouble(parentVO.getZyx12()).doubleValue() < 0.0D)) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000315"));
			}
			UFDouble cjkybje = UFDouble.ZERO_DBL;
			UFDouble cjkbbje = UFDouble.ZERO_DBL;
			UFDouble groupcjkbbje = UFDouble.ZERO_DBL;
			UFDouble globalcjkbbje = UFDouble.ZERO_DBL;
			if (bxcontrastVOs != null) {
				for (BxcontrastVO vo : bxcontrastVOs) {
					vo.validate();
					vo.setCjkbbje(Currency.computeYFB(zfdwbm, 2, bzbm, vo.getCjkybje(), null, null, null, bbhl, djrq)[2]);
					vo.setFybbje(Currency.computeYFB(zfdwbm, 2, bzbm, vo.getFyybje(), null, null, null, bbhl, djrq)[2]);
					UFDouble[] ggcjkbbje =
							Currency.computeGroupGlobalAmount(vo.getCjkybje(), vo.getCjkbbje(), bzbm, djrq, parentVO.getPk_org(), parentVO.getPk_group(), globalbbhl, groupbbhl);
					UFDouble[] ggfybbje =
							Currency.computeGroupGlobalAmount(vo.getFyybje(), vo.getFybbje(), bzbm, djrq, parentVO.getPk_org(), parentVO.getPk_group(), globalbbhl, groupbbhl);
					vo.setGroupcjkbbje(ggcjkbbje[0]);
					vo.setGlobalcjkbbje(ggcjkbbje[1]);
					vo.setGroupfybbje(ggfybbje[0]);
					vo.setGlobalfybbje(ggfybbje[1]);
					vo.setYbje(vo.getCjkybje());
					vo.setBbje(vo.getCjkbbje());
					vo.setGroupbbje(vo.getGroupcjkbbje());
					vo.setGlobalbbje(vo.getGlobalcjkbbje());
					cjkybje = cjkybje.add(vo.getCjkybje());
					cjkbbje = cjkbbje.add(vo.getCjkbbje());
					groupcjkbbje = groupcjkbbje.add(vo.getGroupcjkbbje());
					globalcjkbbje = globalcjkbbje.add(vo.getGlobalcjkbbje());
				}
			}
			adjuestCjkje(parentVO, cjkybje, cjkbbje, groupcjkbbje, globalcjkbbje);
		}
		parentVO.setYbye(parentVO.getYbje());
		parentVO.setBbye(parentVO.getBbje());
		parentVO.setGroupbbye(parentVO.getGroupbbje());
		parentVO.setGlobalbbye(parentVO.getGlobalbbje());
		parentVO.setYjye(parentVO.getYbje());
		if (!parentVO.getDjzt().equals(Integer.valueOf(0))) {
			if (parentVO.getQcbz().booleanValue()) {
				parentVO.setDjzt(Integer.valueOf(3));
			} else {
				parentVO.setDjzt(Integer.valueOf(1));
			}
		}
		if (parentVO.getTotal() == null) {
			parentVO.setTotal(parentVO.getYbje());
		}
	}

	public static void adjuestCjkje(JKBXHeaderVO parentVO, UFDouble cjkybje, UFDouble cjkbbje, UFDouble groupcjkbbje, UFDouble globalcjkbbje) {
		UFDouble zero = new UFDouble(0);
		if (parentVO.getDjdl().equals("bx")) {
			// if (UFDoubleTool.isXiangdeng(parentVO.getYbje(), cjkybje)) {
			if (UFDoubleTool.isXiangdeng(new UFDouble(parentVO.getZyx12()), cjkybje)) {
				parentVO.setZfybje(zero);
				parentVO.setZfbbje(parentVO.getBbje().sub(cjkbbje).compareTo(zero) > 0 ? parentVO.getBbje().sub(cjkbbje) : zero);
				parentVO.setGroupzfbbje(parentVO.getGroupbbje().sub(groupcjkbbje).compareTo(zero) > 0 ? parentVO.getGroupbbje().sub(groupcjkbbje) : zero);
				parentVO.setGlobalzfbbje(parentVO.getGlobalbbje().sub(globalcjkbbje).compareTo(zero) > 0 ? parentVO.getGlobalbbje().sub(globalcjkbbje) : zero);
				parentVO.setHkybje(zero);
				// parentVO.setHkbbje(cjkbbje.sub(parentVO.getBbje()).compareTo(zero)
				// > 0 ? cjkbbje.sub(parentVO.getBbje()) : zero);
				parentVO.setHkbbje(cjkbbje.sub(new UFDouble(parentVO.getZyx12())).compareTo(zero) > 0 ? cjkbbje.sub(new UFDouble(parentVO.getZyx12())) : zero);
				parentVO.setGrouphkbbje(groupcjkbbje.sub(parentVO.getGroupbbje()).compareTo(zero) > 0 ? groupcjkbbje.sub(parentVO.getGroupbbje()) : zero);
				parentVO.setGlobalhkbbje(globalcjkbbje.sub(parentVO.getGlobalbbje()).compareTo(zero) > 0 ? globalcjkbbje.sub(parentVO.getGlobalbbje()) : zero);
			} else if (UFDoubleTool.isZero(cjkybje)) {
				// if (parentVO.getYbje().doubleValue() > 0.0D) {
				// parentVO.setZfybje(parentVO.getYbje());
				// parentVO.setZfbbje(parentVO.getBbje());
				if (new UFDouble((String) parentVO.getZyx12()).doubleValue() > 0.0D) {
					parentVO.setZfybje(new UFDouble((String) parentVO.getZyx12()));
					parentVO.setZfbbje(new UFDouble((String) parentVO.getZyx12()));
					parentVO.setGroupzfbbje(parentVO.getGroupbbje());
					parentVO.setGlobalzfbbje(parentVO.getGlobalbbje());
					parentVO.setHkybje(zero);
					parentVO.setHkbbje(zero);
					parentVO.setGrouphkbbje(zero);
					parentVO.setGlobalhkbbje(zero);
				} else {
					parentVO.setHkybje(parentVO.getYbje().abs());
					parentVO.setHkbbje(parentVO.getBbje().abs());
					parentVO.setGrouphkbbje(parentVO.getGroupbbje().abs());
					parentVO.setGlobalhkbbje(parentVO.getGlobalbbje().abs());
					parentVO.setZfybje(zero);
					parentVO.setZfbbje(zero);
					parentVO.setGroupzfbbje(zero);
					parentVO.setGlobalzfbbje(zero);
				}
				// } else if (UFDoubleTool.isXiaoyu(parentVO.getYbje(),
				// cjkybje)) {
			} else if (UFDoubleTool.isXiaoyu(new UFDouble(parentVO.getZyx12()), cjkybje)) {

				parentVO.setZfybje(zero);
				parentVO.setZfbbje(zero);
				parentVO.setGroupzfbbje(zero);
				parentVO.setGlobalzfbbje(zero);
				// parentVO.setHkybje(cjkybje.sub(parentVO.getYbje()));
				// parentVO.setHkbbje(cjkbbje.sub(parentVO.getBbje()));
				parentVO.setHkybje(cjkybje.sub(new UFDouble(parentVO.getZyx12())));
				parentVO.setHkbbje(cjkbbje.sub(new UFDouble(parentVO.getZyx12())));
				parentVO.setGrouphkbbje(groupcjkbbje.sub(parentVO.getGroupbbje()));
				parentVO.setGlobalhkbbje(globalcjkbbje.sub(parentVO.getGlobalbbje()));
				// } else if (UFDoubleTool.isXiaoyu(cjkybje,
				// parentVO.getYbje())) {
				// parentVO.setZfybje(parentVO.getYbje().sub(cjkybje));
			} else if (UFDoubleTool.isXiaoyu(cjkybje, new UFDouble(parentVO.getZyx12()))) {

				parentVO.setZfybje(new UFDouble((String) parentVO.getZyx12()).sub(cjkybje));
				parentVO.setZfbbje(parentVO.getBbje().sub(cjkbbje));
				parentVO.setGroupzfbbje(parentVO.getGroupbbje().sub(groupcjkbbje));
				parentVO.setGlobalzfbbje(parentVO.getGlobalbbje().sub(globalcjkbbje));
				parentVO.setHkybje(zero);
				parentVO.setHkbbje(zero);
				parentVO.setGrouphkbbje(zero);
				parentVO.setGlobalhkbbje(zero);
			}
			parentVO.setCjkybje(cjkybje);
			parentVO.setCjkbbje(cjkbbje);
			parentVO.setGroupcjkbbje(groupcjkbbje);
			parentVO.setGlobalcjkbbje(globalcjkbbje);
		} else {
			// parentVO.setZfybje(parentVO.getYbje());
			parentVO.setZfybje(new UFDouble(parentVO.getZyx12()));

			parentVO.setZfbbje(parentVO.getBbje());
			parentVO.setGroupzfbbje(parentVO.getGroupbbje());
			parentVO.setGlobalzfbbje(parentVO.getGlobalbbje());
			parentVO.setHkybje(zero);
			parentVO.setHkbbje(zero);
			parentVO.setGrouphkbbje(zero);
			parentVO.setGlobalhkbbje(zero);
		}
	}

	private void checkValidHeader(JKBXHeaderVO parentVO) throws ValidationException {
		parentVO.validate();
	}

	private void checkHeadFinItemJe(JKBXVO bxvo) throws ValidationException {
		BXBusItemVO[] childrenVO = bxvo.getChildrenVO();
		if ((childrenVO == null) || (childrenVO.length == 0)) {
			return;
		}
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		// 20170712 tsy
		String[] keys =
		// "zyx12", "defitem25",
				{ "zyx12", "ybje", "bbje", "ybye", "bbye", "hkybje", "hkbbje", "zfybje", "zfbbje", "cjkybje", "cjkbbje" };
		String[] name =
				{ NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000280"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000245"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000318"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000246"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000319"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000320"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000321"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000322"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000323"), NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000324") };

		int length = keys.length;
		for (int j = 0; j < length; j++) {
			// 20170713 tsy 有些字段无法转为UFDouble，需要手动转
			UFDouble headJe = UFDouble.ZERO_DBL;
			if ("zyx12".equals(keys[j])) {
				headJe =
						new UFDouble((String) parentVO.getAttributeValue(keys[j])) == null ? new UFDouble(0) : new UFDouble((String) parentVO.getAttributeValue(keys[j]));
			} else {
				headJe = parentVO.getAttributeValue(keys[j]) == null ? new UFDouble(0) : (UFDouble) parentVO.getAttributeValue(keys[j]);
			}
			UFDouble bodyJe = new UFDouble(0);
			for (int i = 0; i < childrenVO.length; i++) {
				// 20170713 tsy 表头zyx12对应表体defitem25
				UFDouble je = UFDouble.ZERO_DBL;
				if ("zyx12".equals(keys[j])) {
					je =
							new UFDouble((String)childrenVO[i].getAttributeValue("defitem25")) == null ? new UFDouble(0) : new UFDouble((String) childrenVO[i].getAttributeValue("defitem25"));

				} else {

					je =
							childrenVO[i].getAttributeValue(keys[j]) == null ? new UFDouble(0) : (UFDouble) childrenVO[i].getAttributeValue(keys[j]);
				}

				if (je != null) {
					bodyJe = bodyJe.add(je);
				}
			}
			if (headJe.compareTo(bodyJe) != 0) {
				if ((j % 2 == 1) && (headJe.sub(bodyJe).abs().compareTo(new UFDouble(1)) < 0)) {
					parentVO.setAttributeValue(keys[j], bodyJe);
				} else {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000325", null, new String[] { name[j] }));
				}
			}
		}
	}

	private void checkHeadItemJe(JKBXVO bxvo) throws ValidationException {
		BXBusItemVO[] childrenVO = bxvo.getBxBusItemVOS();
		if ((childrenVO == null) || (childrenVO.length == 0)) {
			return;
		}
		JKBXHeaderVO parentVO = bxvo.getParentVO();
		if (bxvo.getParentVO().getDjdl().equals("bx")) {
			UFDouble total = parentVO.getTotal();
			if (total == null) {
				total = UFDouble.ZERO_DBL;
			}
			UFDouble amount = UFDouble.ZERO_DBL;
			for (int i = 0; i < childrenVO.length; i++) {
				amount = amount.add(childrenVO[i].getAmount());
			}
			if (total.compareTo(amount) != 0) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000327"));
			}
		} else {
			// UFDouble ybje = parentVO.getYbje();
			UFDouble ybje = new UFDouble(parentVO.getZyx12());

			if (ybje == null) {
				ybje = new UFDouble(0);
			}
			UFDouble amount = new UFDouble(0);
			for (int i = 0; i < childrenVO.length; i++) {
				amount = amount.add(childrenVO[i].getAmount());
			}
			if (ybje.compareTo(amount) != 0) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000328"));
			}
		}
	}

	private void checkValidChildrenVO(BXBusItemVO[] childrenVO) throws ValidationException {
		childrenVO = removeNullItem(childrenVO);
		if ((childrenVO == null) || (childrenVO.length == 0)) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0089"));
		}
		for (BXBusItemVO child : childrenVO) {
			child.validate();
			if (child.getTablecode() == null) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0144"));
			}
		}
	}

	private BXBusItemVO[] removeNullItem(BXBusItemVO[] childrenVO) {
		List<BXBusItemVO> bxBusItemVOs = new ArrayList();
		boolean hasNullItem = false;
		for (BXBusItemVO child : childrenVO) {
			if (!child.isNullItem()) {
				bxBusItemVOs.add(child);
			} else {
				hasNullItem = true;
			}
		}
		if (hasNullItem)
			childrenVO = (BXBusItemVO[]) bxBusItemVOs.toArray(new BXBusItemVO[0]);
		return childrenVO;
	}

	private void checkFinRange(JKBXVO bxvo, JKBXHeaderVO parentVO) throws ValidationException {
		if (bxvo.getParentVO().getDjdl().equals("bx")) {
			Double range = Double.valueOf(UFDouble.ZERO_DBL.getDouble());
			try {
				if (SysInit.getParaInt(parentVO.getPk_org(), BXParamConstant.PARAM_ER_FI_RANGE) == null) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000332"));
				}
				range = Double.valueOf(SysInit.getParaInt(parentVO.getPk_org(), BXParamConstant.PARAM_ER_FI_RANGE).doubleValue());
			} catch (BusinessException e) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000332"));
			}
			if (range == null) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000333"));
			}
			// Double total = Double.valueOf(parentVO.getTotal() == null ? 0.0D
			// : parentVO.getTotal().toDouble().doubleValue());
			Double total =
					Double.valueOf(new UFDouble(parentVO.getZyx12()) == null ? 0.0D : new UFDouble(parentVO.getZyx12()).toDouble().doubleValue());

			Double ybje = Double.valueOf(parentVO.getYbje() == null ? 0.0D : parentVO.getYbje().toDouble().doubleValue());
			if ((range.doubleValue() < 0.0D) && (ybje.doubleValue() > total.doubleValue())) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0145"));
			}
			if (Math.abs(total.doubleValue() - ybje.doubleValue()) > Math.abs(range.doubleValue())) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000334"));
			}
		}
	}

	private void checkValidFinItemVO(JKBXVO bxvo) throws ValidationException {
		BXBusItemVO[] childrenVO = bxvo.getChildrenVO();
		boolean ispay = false;
		boolean isreceive = false;
		if ((childrenVO != null) && (childrenVO.length > 0)) {
			for (BXBusItemVO child : childrenVO) {
				child.validate();
				if ((bxvo.getParentVO().getDjdl().equals("jk")) && (child.getYbje().compareTo(UFDouble.ZERO_DBL) <= 0) && (child.getCjkybje().compareTo(UFDouble.ZERO_DBL) <= 0)) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0088"));
				}
				if ((bxvo.getParentVO().getDjdl().equals("bx")) && (child.getYbje().compareTo(UFDouble.ZERO_DBL) == 0) && (child.getCjkybje().compareTo(UFDouble.ZERO_DBL) == 0)) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011v61013_0", "02011v61013-0090"));
				}
				if (child.getZfybje().compareTo(UFDouble.ZERO_DBL) > 0) {
					ispay = true;
				}
				if (child.getHkybje().compareTo(UFDouble.ZERO_DBL) > 0) {
					isreceive = true;
				}
			}
			if ((ispay) && (isreceive)) {
				throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000397"));
			}
		}
		checkHeadFinItemJe(bxvo);
	}

	public static void checkErmIsCloseAcc(JKBXVO bxvo) throws BusinessException {
		JKBXHeaderVO head = bxvo.getParentVO();
		String moduleCode = "2011";
		String pk_org = head.getPk_org();
		UFDate date = head.getDjrq();
		if (((bxvo.getParentVO().getQcbz() == null) || (!bxvo.getParentVO().getQcbz().booleanValue())) && (ErUtil.isOrgCloseAcc(moduleCode, pk_org, date))) {
			throw new DataValidateException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0146"));
		}
	}

	public List<String> getNotRepeatFields() {
		if (this.notRepeatFields == null) {
			this.notRepeatFields = new ArrayList();
			this.notRepeatFields.add("assume_org");
			this.notRepeatFields.add("assume_dept");
			this.notRepeatFields.add("pk_iobsclass");
			this.notRepeatFields.add("pk_pcorg");
			this.notRepeatFields.add("pk_resacostcenter");
			this.notRepeatFields.add("jobid");
			this.notRepeatFields.add("projecttask");
			this.notRepeatFields.add("pk_checkele");
			this.notRepeatFields.add("customer");
			this.notRepeatFields.add("hbbm");
		}
		return this.notRepeatFields;
	}
}