package nc.impl.fa.deployin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.impl.am.common.InSqlManager;
import nc.impl.am.db.DBAccessUtil;
import nc.impl.fa.deployout.DeployOutImpl;
import nc.itf.fa.service.IAssetService;
import nc.itf.fa.service.ICloseBookService;
import nc.itf.fa.service.IDepmethodService;
import nc.itf.fa.service.ILogService;
import nc.pub.fa.asset.manager.AccbookRelatCategoryUtil;
import nc.pub.fa.card.AssetUseConst;
import nc.pub.fa.common.manager.VOManager;
import nc.pub.fa.common.util.StringUtils;
import nc.pub.fa.dep.ReduceSimulateManager;
import nc.pub.fa.deploy.DeployManager;
import nc.pub.fa.deploy.DeployValueParamConst;
import nc.vo.am.common.util.ArrayUtils;
import nc.vo.am.common.util.CollectionUtils;
import nc.vo.am.common.util.ExceptionUtils;
import nc.vo.am.common.util.MapUtils;
import nc.vo.am.common.util.UFDoubleUtils;
import nc.vo.am.manager.AccbookManager;
import nc.vo.am.manager.AccperiodVO;
import nc.vo.am.manager.CurrencyManager;
import nc.vo.am.manager.ParameterManager;
import nc.vo.am.proxy.AMProxy;
import nc.vo.fa.accbookinfo.AccbookBodyVO;
import nc.vo.fa.asset.AssetVO;
import nc.vo.fa.common.Triple;
import nc.vo.fa.deployin.DeployInBodyVO;
import nc.vo.fa.deployin.DeployInHeadVO;
import nc.vo.fa.deployin.DeployInVO;
import nc.vo.fa.deployout.DeployOutBodyVO;
import nc.vo.fa.deployout.DeployOutHeadVO;
import nc.vo.fa.deployout.DeployOutVO;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class DeployInAssetGenerator {
	private Map<String, AssetVO> outBizAssetMap;
	private String inOrgMainBook;
	private String outOrgMainBook;
	private Map<String, String> inOrgBookSortMap;
	private Map<String, String> outOrgBookSortMap;
	private Triple<String, String, AssetVO> cardAccbookAssetTriple;
	Integer deployValueParamSelect;
	UFBoolean isDeployCardDefItem;
	UFBoolean isChangeAssetCode;

	public DeployInAssetGenerator() {
	}

	private List<String> deployOutAssetCodeList = new ArrayList();

	private Map<String, String> group_dep = new HashMap();

	private void initMap(String pk_group) throws BusinessException {
		if (this.group_dep.get(pk_group) == null) {
			String pk_depmethod = ((IDepmethodService) AMProxy.lookup(IDepmethodService.class)).queryDepmethodPKOfWorkLoan(pk_group);
			this.group_dep.put(pk_group, pk_depmethod);
		}
	}

	private void prepared(DeployInVO billVO) throws BusinessException {
		initMap((String) billVO.getParentVO().getAttributeValue("pk_group"));
		String pk_org_in = (String) billVO.getParentVO().getAttributeValue("pk_org");
		String pk_org_out = (String) billVO.getParentVO().getAttributeValue("pk_org_out");
		UFDate bizDateIn = (UFDate) billVO.getParentVO().getAttributeValue("business_date");

		String[] card_out_pks = (String[]) VOManager.getAttributeValueArray(billVO.getChildrenVO(), "pk_card_out");

		Set<String> inOrgBookPkSet = AccbookManager.queryBookIDsByOrg(pk_org_in, bizDateIn.toString());
		Set<String> outOrgBookPkSet = AccbookManager.queryBookIDsByOrg(pk_org_out, bizDateIn.toString());

		AssetVO[] assetVOs = ((IAssetService) AMProxy.lookup(IAssetService.class)).queryDeploywayAssetVO(
				(String[]) CollectionUtils.convertToArray(outOrgBookPkSet, new String[0]), card_out_pks);

		this.outBizAssetMap = new HashMap();
		for (AssetVO assetVO : assetVOs) {
			if (UFBoolean.TRUE.equals(assetVO.getBusiness_flag())) {
				this.outBizAssetMap.put(assetVO.getPk_card(), assetVO);
			}
		}

		this.inOrgBookSortMap = AccbookManager.querySetOfBookMapByAccountingbook(bizDateIn.toString(), (String[]) CollectionUtils.toArray(inOrgBookPkSet));
		this.outOrgBookSortMap = AccbookManager.querySetOfBookMapByAccountingbook(bizDateIn.toString(), (String[]) CollectionUtils.toArray(outOrgBookPkSet));
		this.inOrgMainBook = AccbookManager.queryMainAccbookIDByOrg(pk_org_in);
		this.outOrgMainBook = AccbookManager.queryMainAccbookIDByOrg(pk_org_out);

		this.cardAccbookAssetTriple = new Triple();
		for (AssetVO assetVO : assetVOs) {
			this.cardAccbookAssetTriple.add(assetVO.getPk_card(), assetVO.getPk_accbook(), assetVO);
		}
		DeployInHeadVO headVO = (DeployInHeadVO) billVO.getParentVO();

		this.deployValueParamSelect = ParameterManager.getParaInt(headVO.getPk_group(), DeployValueParamConst.DEPLOY_PARAM_CONST);

		this.isDeployCardDefItem = ParameterManager.getParaBoolean(headVO.getPk_org_in(), "FA40");

		this.isChangeAssetCode = ParameterManager.getParaBoolean(headVO.getPk_org_in(), "FA11");
	}

	public AssetVO[][] generateAsset(DeployInVO billVO) throws BusinessException {
		prepared(billVO);
		DeployInHeadVO headVO = (DeployInHeadVO) billVO.getParentVO();
		String pk_deployout = headVO.getPk_bill_src();
		DeployOutVO[] deployOutVOs = new DeployOutImpl().queryDeployOutVOByPKs(new String[] { pk_deployout }, true);
		UFDate bizDateIn = (UFDate) headVO.getAttributeValue("business_date");
		if (ArrayUtils.isNotEmpty(deployOutVOs)) {
			DeployOutVO outvo = deployOutVOs[0];
			DeployOutHeadVO outheadVO = (DeployOutHeadVO) outvo.getParent();
			bizDateIn = (UFDate) outheadVO.getAttributeValue("business_date");
		}
		DeployOutVO deployout = null;
		if (ArrayUtils.isNotEmpty(deployOutVOs)) {
			deployout = (DeployOutVO) ArrayUtils.getFirstElem(deployOutVOs);
		}
		DeployOutHeadVO outhead = (DeployOutHeadVO) deployout.getParentVO();
		String account_currency = outhead.getAccount_currency();
		String out_pk_accbook = AccbookManager.queryMainAccbookIDByFinanceOrg(outhead.getPk_org());
		String out_pk_currency = CurrencyManager.getCurrencyPKByAccbook(out_pk_accbook);
		DeployOutBodyVO[] outbodyVOs = (DeployOutBodyVO[]) deployout.getChildrenVO();
		Map<String, DeployOutBodyVO> card_bodyVO_map = new HashMap();
		if (ArrayUtils.isNotEmpty(outbodyVOs)) {
			for (DeployOutBodyVO outbody : outbodyVOs) {
				card_bodyVO_map.put(outbody.getPk_card(), outbody);
			}
		}

		int deployScene = 2;
		if (!account_currency.equals(out_pk_currency)) {
			deployScene = 1;
		}
		DeployInBodyVO[] bodyVOs = (DeployInBodyVO[]) billVO.getChildrenVO();
		if (ArrayUtils.isEmpty(bodyVOs)) {
			return (AssetVO[][]) null;
		}
		List<DeployInBodyVO> bodyList = new ArrayList();

		Map newDataMap = new HashMap();
		for (DeployInBodyVO bodyVO : bodyVOs) {
			newDataMap.put(bodyVO.getCard_code() + "begin_date", bodyVO.getBegin_date());

			newDataMap.put(bodyVO.getCard_code() + "allworkloan", bodyVO.getAllworkloan());

			newDataMap.put(bodyVO.getCard_code() + "accuworkloan", bodyVO.getAccuworkloan());

			newDataMap.put(bodyVO.getCard_code() + "workloanunit", bodyVO.getWorkloanunit());

			newDataMap.put(bodyVO.getCard_code() + "pk_usingstatus", bodyVO.getPk_usingstatus());

			newDataMap.put(bodyVO.getCard_code() + "paydept_flag", bodyVO.getPaydept_flag());

			newDataMap.put(bodyVO.getCard_code() + "asset_code", bodyVO.getAsset_code());

			newDataMap.put(bodyVO.getCard_code() + "naturemonth", bodyVO.getNaturemonth());

			newDataMap.put(bodyVO.getCard_code() + "asset_name", bodyVO.getAsset_name());

			newDataMap.put(bodyVO.getCard_code() + "tax_input", bodyVO.getTax_input());

			newDataMap.put(bodyVO.getCard_code() + "spec", bodyVO.getSpec());

			newDataMap.put(bodyVO.getCard_code() + "card_model", bodyVO.getCard_model());

			newDataMap.put(bodyVO.getCard_code() + "bar_code", bodyVO.getBar_code());

			newDataMap.put(bodyVO.getCard_code() + "close_date", bodyVO.getClose_date());
			bodyList.add(bodyVO);
		}

		List<String> sameWithInReportBook_OutBookPKList = new ArrayList();

		Map<String, String> sameWithInReportBook_Map = new HashMap();

		Set<String> notSameWithOutBook_InReportBookPKSet = new HashSet();

		for (Iterator i$ = this.inOrgBookSortMap.entrySet().iterator(); i$.hasNext();) {
			Map.Entry<String, String> entry = (Map.Entry) i$.next();
			String inBookSort = (String) entry.getValue();
			for (Map.Entry<String, String> innerEntry : this.outOrgBookSortMap.entrySet()) {
				String outBookSort = (String) innerEntry.getValue();
				if (inBookSort.equals(outBookSort)) {
					sameWithInReportBook_OutBookPKList.add(innerEntry.getKey());
					sameWithInReportBook_Map.put(innerEntry.getKey(), entry.getKey());
				} else {
					notSameWithOutBook_InReportBookPKSet.add( entry.getKey());
				}
			}
		}
		String inBookSort;
		if ((MapUtils.isNotEmpty(sameWithInReportBook_Map)) && (CollectionUtils.isNotEmpty(notSameWithOutBook_InReportBookPKSet))) {
			for (Map.Entry<String, String> innerEntry : sameWithInReportBook_Map.entrySet()) {
				String in_book = (String) innerEntry.getValue();
				if (notSameWithOutBook_InReportBookPKSet.contains(in_book)) {
					notSameWithOutBook_InReportBookPKSet.remove(in_book);
				}
			}
		}

		notSameWithOutBook_InReportBookPKSet.remove(this.inOrgMainBook);

		List<List<AssetVO>> assetList = new ArrayList();

		List<AssetVO> sameOutAssetList = new ArrayList();
		for (DeployInBodyVO body : bodyList) {
			String pk_card_out = body.getPk_card_out();
			Map<String, AssetVO> book_assetvo = this.cardAccbookAssetTriple.getValue(pk_card_out);
			for (Map.Entry<String, AssetVO> entry : book_assetvo.entrySet()) {
				String pk_accbook = (String) entry.getKey();
				if (sameWithInReportBook_OutBookPKList.contains(pk_accbook)) {
					sameOutAssetList.add(entry.getValue());
				}
			}
			List<AssetVO> innerList = new ArrayList();
			AssetVO mainAssetVO = mainBookCreateAsset(headVO, body, (AssetVO) this.outBizAssetMap.get(pk_card_out));
			if (mainAssetVO != null) {
				setAssetValue(headVO, body, mainAssetVO, (AssetVO) this.outBizAssetMap.get(pk_card_out));
				mainAssetVO.setPk_accbook(this.inOrgMainBook);
				innerList.add(mainAssetVO);
			}
			if (CollectionUtils.isNotEmpty(notSameWithOutBook_InReportBookPKSet)) {
				for (String pk_accbook : notSameWithOutBook_InReportBookPKSet) {
					AssetVO newAsset = notSameReportBookCreateAsset(headVO, body, mainAssetVO, pk_accbook, (DeployOutBodyVO) card_bodyVO_map.get(pk_card_out),
							deployScene);
					if (newAsset != null) {
						setAssetValue(headVO, body, newAsset, (AssetVO) this.outBizAssetMap.get(pk_card_out));
						innerList.add(newAsset);
					}
				}
			}
			if (CollectionUtils.isNotEmpty(sameOutAssetList)) {
				for (AssetVO outAssetVO : sameOutAssetList) {
					AssetVO newAsset = null;

					if (outAssetVO.getBusiness_flag().booleanValue()) {
						String outMainBookSort = (String) this.outOrgBookSortMap.get(outAssetVO.getPk_accbook());
						String inMainBookSort = (String) this.inOrgBookSortMap.get(this.inOrgMainBook);
						Set<String> inReportBook = new HashSet();
						for (Map.Entry<String, String> innerEntry : this.inOrgBookSortMap.entrySet()) {
							inReportBook.add(innerEntry.getValue());
						}
						inReportBook.remove(inMainBookSort);
						if (inReportBook.contains(outMainBookSort)) {
							setSimulateDepValue(new AssetVO[] { outAssetVO }, bizDateIn);
							newAsset = sameReportBookCreateAsset(headVO, body, (AssetVO) this.outBizAssetMap.get(outAssetVO.getPk_card()),
									sameWithInReportBook_Map, (DeployOutBodyVO) card_bodyVO_map.get(pk_card_out), deployScene, bizDateIn);
						} else {
							newAsset = sameReportBookCreateAsset(headVO, body, (AssetVO) this.outBizAssetMap.get(outAssetVO.getPk_card()),
									sameWithInReportBook_Map, (DeployOutBodyVO) card_bodyVO_map.get(pk_card_out), deployScene, bizDateIn);
						}
					} else {
						setSimulateDepValue(new AssetVO[] { outAssetVO }, bizDateIn);
						newAsset = sameReportBookCreateAsset(headVO, body, outAssetVO, sameWithInReportBook_Map,
								(DeployOutBodyVO) card_bodyVO_map.get(pk_card_out), deployScene, bizDateIn);
					}
					if (newAsset != null) {
						if ((!((String) this.outOrgBookSortMap.get(this.outOrgMainBook)).equals(this.inOrgBookSortMap.get(this.inOrgMainBook)))
								|| (!this.inOrgMainBook.equals(newAsset.getPk_accbook()))) {

							if (newAsset != null) {
								setAssetValue(headVO, body, newAsset, (AssetVO) this.outBizAssetMap.get(pk_card_out));
								innerList.add(newAsset);
							}
						}
					}
				}
			}
			if (CollectionUtils.isNotEmpty(innerList)) {
				assetList.add(innerList);
			}
		}

		String pk_group = headVO.getPk_group();

		String pk_org = headVO.getPk_org();
		if ((!this.isChangeAssetCode.booleanValue()) && (CollectionUtils.isNotEmpty(this.deployOutAssetCodeList))) {
			String pkListStr = InSqlManager.getInSQLValue(this.deployOutAssetCodeList);
			String sql = getSqlStr(pk_group, pk_org, pkListStr);
			List resList = new DBAccessUtil().querySingleColumn(sql);
			if ((null != resList) && (resList.size() > 0)) {
				String msg = getMessage(resList);
				ExceptionUtils.asBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("deploy_0", "02012020-0054") + msg);
			}
		}

		AssetVO[][] toAddAssetVOs = new AssetVO[assetList.size()][];
		for (int i = 0; i < toAddAssetVOs.length; i++) {
			toAddAssetVOs[i] = ((AssetVO[]) CollectionUtils.convertToArray((Collection) assetList.get(i), new AssetVO[0]));
		}

		for (AssetVO[] assetVOs : toAddAssetVOs) {
			for (AssetVO assetVO : assetVOs) {

				assetVO.setDeploy_flag(headVO.getPk_bill_src());
				String tPk_accbook = assetVO.getPk_accbook();
				headVO.setPk_currency(headVO.getAccount_currency());
				if ((StringUtils.isNotEmpty(tPk_accbook)) && (!this.inOrgMainBook.equals(tPk_accbook))) {
					assetVO.setBusiness_flag(UFBoolean.FALSE);

					String newPk_usingstatus = (String) newDataMap.get(assetVO.getCard_code() + "pk_usingstatus");
					assetVO.setPk_usingstatus(newPk_usingstatus);

					String newPaydept_flag = (String) newDataMap.get(assetVO.getCard_code() + "paydept_flag");
					assetVO.setPaydept_flag(newPaydept_flag);

					String spec = (String) newDataMap.get(assetVO.getCard_code() + "spec");
					assetVO.setSpec(spec);

					String card_model = (String) newDataMap.get(assetVO.getCard_code() + "card_model");
					assetVO.setCard_model(card_model);

					String bar_code = (String) newDataMap.get(assetVO.getCard_code() + "bar_code");
					assetVO.setBar_code(bar_code);

					UFDate close_date = (UFDate) newDataMap.get(assetVO.getCard_code() + "close_date");
					assetVO.setClose_date(close_date);

					String newAsset_code = (String) newDataMap.get(assetVO.getCard_code() + "asset_code");
					assetVO.setAsset_code(newAsset_code);

					String newAsset_name = (String) newDataMap.get(assetVO.getCard_code() + "asset_name");
					assetVO.setAsset_name(newAsset_name);
				}
				assetVO.setDepamount(UFDouble.ZERO_DBL);
			}
		}
		return toAddAssetVOs;
	}

	private void setAssetValue(DeployInHeadVO headVO, DeployInBodyVO bodyVO, AssetVO inAssetVO, AssetVO outAssetVO) {
		inAssetVO.setPk_bill_src(headVO.getPrimaryKey());
		inAssetVO.setPk_bill_b_src(bodyVO.getPrimaryKey());
		inAssetVO.setPk_card(null);
		inAssetVO.setPk_cardhistory(null);
		inAssetVO.setBusiness_date(headVO.getBusiness_date());
		inAssetVO.setBill_code_src(headVO.getBill_code());
		inAssetVO.setBillmaker(InvocationInfoProxy.getInstance().getUserId());
		inAssetVO.setCard_num(outAssetVO.getCard_num());
		//20170111 tsy "资产调入"默认写入原资产卡片的"项目档案"信息 
		inAssetVO.setPk_jobmngfil(outAssetVO.getPk_jobmngfil());
		//20170111 end
		if (this.isChangeAssetCode.booleanValue()) {
			inAssetVO.setAsset_code(null);
		} else {
			String outAssetCode = outAssetVO.getAsset_code();

			this.deployOutAssetCodeList.add(outAssetCode);
			inAssetVO.setAsset_code(outAssetVO.getAsset_code());
		}
		String[] allFields = inAssetVO.getAttributeNames();
		List<String> unAttrNames = new ArrayList();
		for (String field : allFields) {
			if (field.startsWith(AssetUseConst.Userdefitem_prefix)) {
				unAttrNames.add(field);
			}
		}

		for (String defField : unAttrNames) {
			if (StringUtils.isNotEmpty(defField)) {
				inAssetVO.setAttributeValue(defField, null);
			}
		}

		if ((null != this.isDeployCardDefItem) && (this.isDeployCardDefItem.booleanValue()) && (unAttrNames.size() > 0)) {
			VOManager.copyProperties(outAssetVO, inAssetVO, (String[]) CollectionUtils.toArray(unAttrNames));
		}
		if ((!((String) this.group_dep.get(inAssetVO.getPk_group())).equals(inAssetVO.getPk_depmethod()))
				&& (((String) this.group_dep.get(outAssetVO.getPk_group())).equals(outAssetVO.getPk_depmethod()))) {
			inAssetVO.setAllworkloan(null);
			inAssetVO.setAccuworkloan(null);
			inAssetVO.setWorkloanunit(null);
			inAssetVO.setMonthworkloan(null);
		}
	}

	private AssetVO sameReportBookCreateAsset(DeployInHeadVO headVO, DeployInBodyVO mainBody, AssetVO outAssetVO, Map<String, String> sameReportBookMap,
			DeployOutBodyVO outbody, int scene, UFDate outBizDate) throws BusinessException {
		AssetVO newAsset = null;
		String pk_accbook_out = outAssetVO.getPk_accbook();
		String pk_accbook_in = (String) sameReportBookMap.get(pk_accbook_out);
		if (!this.inOrgMainBook.equals(pk_accbook_in)) {
			Map<String, AccbookBodyVO> vomap = AccbookRelatCategoryUtil.queryAccbookBodyData(headVO.getPk_group(), headVO.getPk_org(),
					mainBody.getPk_category(), new String[] { pk_accbook_in }, headVO.getBusiness_date());

			if (MapUtils.isNotEmpty(vomap)) {
				AccbookBodyVO accbookBody = (AccbookBodyVO) vomap.get(pk_accbook_in);
				if (accbookBody != null) {
					newAsset = DeployManager.procAssetVOByDeployParameterForSameReportBook(headVO, mainBody, outBizDate, outAssetVO, accbookBody,
							this.deployValueParamSelect, pk_accbook_in, outbody, scene);
				}
			}
		}
		return newAsset;
	}

	private AssetVO mainBookCreateAsset(DeployInHeadVO headVO, DeployInBodyVO mainBody, AssetVO outAssetVO) throws BusinessException {
		AssetVO newAsset = DeployManager.procAssetVOByDeployParameterForMainBook(headVO, mainBody, outAssetVO, this.deployValueParamSelect);
		return newAsset;
	}

	private AssetVO notSameReportBookCreateAsset(DeployInHeadVO headVO, DeployInBodyVO mainBody, AssetVO mainAssetVO, String pk_accbook,
			DeployOutBodyVO outbody, int scene) throws BusinessException {
		AssetVO newAsset = null;
		Map<String, AccbookBodyVO> vomap = AccbookRelatCategoryUtil.queryAccbookBodyData(headVO.getPk_group(), headVO.getPk_org(), mainBody.getPk_category(),
				new String[] { pk_accbook }, headVO.getBusiness_date());

		if (MapUtils.isNotEmpty(vomap)) {
			AccbookBodyVO accbookBody = (AccbookBodyVO) vomap.get(pk_accbook);
			if (accbookBody != null) {
				newAsset = DeployManager.procAssetVOByDeployParameterForNotSameReportBook(headVO, mainBody, mainAssetVO, accbookBody,
						this.deployValueParamSelect, pk_accbook, outbody, scene);
			}
		}
		return newAsset;
	}

	private String getSqlStr(String pk_group, String pk_org, String idstr) {
		String sql = " select fa_card.asset_code from fa_card fa_card inner join fa_cardhistory fa_cardhistory on  fa_card.pk_card = fa_cardhistory.pk_card  where  fa_card.asset_code in "
				+ idstr
				+ " and "
				+ " fa_card.pk_group = '"
				+ pk_group
				+ "' and "
				+ " fa_card.pk_org = '"
				+ pk_org
				+ "' and "
				+ " fa_cardhistory.laststate_flag = '" + UFBoolean.TRUE + "' and " + " fa_card.dr = 0 ";

		return sql;
	}

	private String getMessage(List resList) {
		String msg = "";
		for (Object obj : resList) {
			if (null != obj) {
				String tstr = (String) obj;
				msg = msg + tstr + ",";
			}
		}
		if (msg.lastIndexOf(",") != -1) {
			msg = msg.substring(0, msg.lastIndexOf(","));
		}
		return msg;
	}

	private void setSimulateDepValue(AssetVO[] assetVOs, UFDate bizDate) throws BusinessException {
		List<AssetVO> cardsByAccbook = Arrays.asList(assetVOs);
		if (CollectionUtils.isNotEmpty(cardsByAccbook)) {
			for (int i = 0; i < cardsByAccbook.size(); i++) {
				AssetVO tempVO = (AssetVO) cardsByAccbook.get(i);
				ReduceSimulateManager depFactory = new ReduceSimulateManager();

				depFactory.procSimulateDep(new String[] { tempVO.getPk_card() }, tempVO.getPk_group(), tempVO.getPk_org(), tempVO.getPk_accbook(), bizDate,
						null);

				Map<String, UFDouble> simulateAccdep = depFactory.getSimulateAccudepMap();

				Map<String, UFDouble> simulateDepamout = depFactory.getSimulateDepamoutMap();
				String pk_card = tempVO.getPk_card();

				tempVO.setAccudep((UFDouble) simulateAccdep.get(pk_card));
				AccperiodVO accperiodVO = ((ICloseBookService) AMProxy.lookup(ICloseBookService.class)).queryMinUnClosebookPeriod(tempVO.getPk_org(),
						tempVO.getPk_accbook());
				String accyear = accperiodVO.getAccyear();
				String period = accperiodVO.getPeriod();

				boolean isDepComplete = ((ILogService) AMProxy.lookup(ILogService.class)).isDepComplete(tempVO.getPk_org(), tempVO.getPk_accbook(), accyear,
						period);
				if ((((UFDouble) simulateDepamout.get(pk_card)).compareTo(UFDouble.ZERO_DBL) > 0) && (!isDepComplete)) {
					tempVO.setUsedmonth(Integer.valueOf(((AssetVO) cardsByAccbook.get(i)).getUsedmonth().intValue() + 1));
				}

				tempVO.setNetvalue(UFDoubleUtils.sub(tempVO.getLocaloriginvalue(), tempVO.getAccudep()));

				tempVO.setNetrating(UFDoubleUtils.sub(tempVO.getNetvalue(), tempVO.getPredevaluate()));
			}
		}
	}
}