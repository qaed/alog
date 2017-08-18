package nc.vo.arap.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import nc.bs.arap.util.ArapVOUtils;
import nc.bs.logging.Log;
import nc.bs.logging.Logger;
import nc.itf.fi.pub.Currency;
import nc.pubitf.uapbd.DefaultCurrtypeQryUtil;
import nc.uap.ws.log.NCLogger;
import nc.vo.arap.agiotage.ArapBusiDataVO;
import nc.vo.arap.global.ArapBillDealVOConsts;
import nc.vo.arap.global.ArapCommonTool;
import nc.vo.bd.currtype.CurrtypeVO;
import nc.vo.fipub.exception.ExceptionHandler;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.tb.obj.NtbParamVO;
import nc.vo.verifynew.pub.DefaultVerifyRuleVO;
import nc.vo.verifynew.pub.IVerifyMethod;
import nc.vo.verifynew.pub.VerifyCom;
import org.apache.commons.lang.ArrayUtils;

public class SameMnyVerify implements IVerifyMethod, IVerifyBusiness {
	private static final long serialVersionUID = 6560472319130057992L;
	private VerifyCom verifycom;
	private DefaultVerifyRuleVO rulevo;
	private int pph = 100;
	private static long count = 0L;

	private final ArrayList<VerifyDetailVO> retArr = new ArrayList();

	public SameMnyVerify() {
	}

	public SameMnyVerify(VerifyCom verifycom) {
		this.verifycom = verifycom;
		initPph();
	}

	private void initPph() {
		if ((this.pph == 100) && (this.verifycom != null)) {
			Integer inte_pph = (Integer) this.verifycom.getM_context().get("PPH");

			if (inte_pph == null) {
				this.pph = Integer.valueOf(0).intValue();
			} else {
				try {
					this.pph = (inte_pph.intValue() + 1);
				} catch (Exception e) {
				}
			}
		}
	}

	private void setRuleVO(DefaultVerifyRuleVO vo) {
		this.rulevo = vo;
	}

	private DefaultVerifyRuleVO getRuleVO() {
		return this.rulevo;
	}

	public AggverifyVO onVerify(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, DefaultVerifyRuleVO rulevo) {
		long start = System.currentTimeMillis();

		getLogs().clear();
		setRuleVO(rulevo);
		if ((jf_vos == null) || (df_vos == null)) {
			return null;
		}
		if (!ArrayUtils.isEmpty(jf_vos)) {
			for (ArapBusiDataVO vo : jf_vos) {
				if (vo.getMoney_bal().compareTo(vo.getAttributeValue("SETT_MONEY")) == 0) {
					vo.setIsshlclear(true);
				} else {
					vo.setIsshlclear(false);
				}
			}
		}
		if (!ArrayUtils.isEmpty(df_vos)) {
			for (ArapBusiDataVO vo : df_vos) {
				if (vo.getMoney_bal().compareTo(vo.getAttributeValue("SETT_MONEY")) == 0) {
					vo.setIsshlclear(true);
				} else {
					vo.setIsshlclear(false);
				}
			}
		}

		int flag = getHxSuanFa(rulevo);

		if (!getVerifycom().isAutoVerify()) {
			// 20170303 tsy 使用pk_item(行的pk值)排序
			// VerifyTool.sortVector(jf_vos, flag);
			// VerifyTool.sortVector(df_vos, flag);
			// 20170305 tsy 添加排序的判断条件
			if (jf_vos[0].getSrc_itemid() != null && "" != jf_vos[0].getSrc_itemid().trim()) {
				VerifyTool.sortVector(jf_vos, flag, "src_itemid");
				VerifyTool.sortVector(df_vos, flag, "pk_item");
			} else {
				VerifyTool.sortVector(jf_vos, flag, "occupationmny");
				VerifyTool.sortVector(df_vos, flag, "occupationmny");
			}
			// 20170305 end
			// VerifyTool.sortVector(jf_vos, flag, "src_itemid");
			// VerifyTool.sortVector(df_vos, flag, "pk_item");
			// 20170303 end
		}

		Logger.debug("同币种核销前排序结束=" + (System.currentTimeMillis() - start) + "ms");
		long start1 = System.currentTimeMillis();
		String[] jf_keys = rulevo.getM_debtObjKeys();
		if (jf_keys == null) {
			jf_keys = new String[0];
		}
		Vector<String> temp = new Vector();
		for (int i = 0; i < jf_keys.length; i++) {
			temp.add(jf_keys[i]);
		}

		String[] keys = new String[temp.size()];
		temp.copyInto(keys);
		temp.add("wldxpk");

		String[] keys2 = new String[temp.size()];
		temp.copyInto(keys2);

		// 20170303 tsy 核销对应行
		// 20170307 tsy 不一定全部都核销对应行，如果是没指定要核销对应行的可以依次核销
		// 本次添加条件判断，有选择性地进行核销方式
		if (jf_vos[0] != null) {
			if (jf_vos[0].getSrc_itemid() != null && !"".equals(jf_vos[0].getSrc_itemid().trim())) {
				contractInCorrespondingLine(jf_vos, df_vos, rulevo, keys, keys2);
			}else{
				contract(jf_vos, df_vos, rulevo, keys, keys2);
			}
		}
		// 20170307 end
		// 20170303 end
		ArrayList<VerifyDetailVO> detailVOList = getLogs();
		VerifyDetailVO[] detailVOArr = new VerifyDetailVO[detailVOList.size()];
		detailVOList.toArray(detailVOArr);
		AggverifyVO aggverifyVO = new AggverifyVO();
		aggverifyVO.setChildrenVO(detailVOArr);
		Logger.debug("同币种核销结束=" + (System.currentTimeMillis() - start1) + "ms");
		Logger.debug("同币种核销次数=" + count);
		return aggverifyVO;
	}

	private void contract(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, DefaultVerifyRuleVO rulevo, String[] keys, String[] keys2) {
		if (getVerifycom().isExactVerify()) {
			contractSame(jf_vos, df_vos, keys);
		} else {

			contractSame(jf_vos, df_vos, keys2);
			contractSame(jf_vos, df_vos, keys);
		}
	}

	/**
	 * <p>
	 * 核销对应行，用于“付款排程”等只核销一部分金额的情况<br/>
	 * 具体实现方法为：
	 * {@link nc.vo.arap.verify.SameMnyVerify#contractSameInCorrespondingLine(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, String[] keys)}
	 * </p>
	 * 
	 * @author tsy 20170303
	 * @param jf_vos
	 * @param df_vos
	 * @param rulevo
	 * @param keys
	 * @param keys2
	 */
	private void contractInCorrespondingLine(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, DefaultVerifyRuleVO rulevo, String[] keys, String[] keys2) {
		if (getVerifycom().isExactVerify()) {
			contractSameInCorrespondingLine(jf_vos, df_vos, keys);
		} else {
			contractSameInCorrespondingLine(jf_vos, df_vos, keys2);
			contractSameInCorrespondingLine(jf_vos, df_vos, keys);
		}
	}

	private void contractSame(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, String[] keys) {
		Hashtable<String, Vector<Vector<ArapBusiDataVO>>> hash2 = new Hashtable();
		for (int i = 0; i < jf_vos.length; i++) {
			String allkeys = VerifyTool.getAllkeys(jf_vos[i], keys);

			if (hash2.containsKey(allkeys)) {
				((Vector) ((Vector) hash2.get(allkeys)).get(0)).add(jf_vos[i]);
			} else {
				Vector<Vector<ArapBusiDataVO>> vec = new Vector();
				vec.add(new Vector());
				vec.add(new Vector());
				hash2.put(allkeys, vec);
				((Vector) ((Vector) hash2.get(allkeys)).get(0)).add(jf_vos[i]);
			}
		}

		for (int i = 0; i < df_vos.length; i++) {
			String allkeys = VerifyTool.getAllkeys(df_vos[i], keys);

			if (hash2.containsKey(allkeys)) {
				((Vector) ((Vector) hash2.get(allkeys)).get(1)).add(df_vos[i]);
			} else {
				Vector<Vector<ArapBusiDataVO>> vec = new Vector();
				vec.add(new Vector());
				vec.add(new Vector());
				hash2.put(allkeys, vec);
				((Vector) ((Vector) hash2.get(allkeys)).get(1)).add(df_vos[i]);
			}
		}

		Iterator it = hash2.keySet().iterator();
		while (it.hasNext()) {
			Vector vec = (Vector) hash2.get(it.next());
			Vector jf = (Vector) vec.get(0);
			Vector df = (Vector) vec.get(1);
			int k = 0;
			for (int i = 0; i < jf.size(); i++) {
				ArapBusiDataVO vo = (ArapBusiDataVO) jf.elementAt(i);
				if (!ArapCommonTool.isZero(new UFDouble(vo.getAttributeValue("SETT_MONEY").toString()))) {

					for (int j = k; j < df.size(); j++) {
						ArapBusiDataVO vo2 = (ArapBusiDataVO) df.elementAt(j);
						if (!ArapCommonTool.isZero(new UFDouble(vo2.getAttributeValue("SETT_MONEY").toString()))) {

							doBusiness(vo, vo2);
							if (ArapCommonTool.isZero(new UFDouble(vo.getAttributeValue("SETT_MONEY").toString()))) {

								k = j;
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * 核销对应行，用于只核销一部分金额。
	 * </p>
	 * 如应付单有3行记录:10 20 30<br/>
	 * 用付款排程，分别核销:5 8 10<br/>
	 * 最后剩余:5 12 20<br/>
	 * 如果用 contractSame(..)方法最后剩余:0 7 30
	 * 
	 * @author tsy 20170303
	 * @param jf_vos
	 * @param df_vos
	 * @param keys
	 */
	private void contractSameInCorrespondingLine(ArapBusiDataVO[] jf_vos, ArapBusiDataVO[] df_vos, String[] keys) {
		if (jf_vos != null && df_vos != null && jf_vos.length != df_vos.length) {
			Logger.debug(getClass() + ",核销行数不一致");
		}
		Hashtable<String, Vector<Vector<ArapBusiDataVO>>> hash2 = new Hashtable();
		for (int i = 0; i < jf_vos.length; i++) {
			String allkeys = VerifyTool.getAllkeys(jf_vos[i], keys);

			if (hash2.containsKey(allkeys)) {
				((Vector) ((Vector) hash2.get(allkeys)).get(0)).add(jf_vos[i]);
			} else {
				Vector<Vector<ArapBusiDataVO>> vec = new Vector();
				vec.add(new Vector());
				vec.add(new Vector());
				hash2.put(allkeys, vec);
				((Vector) ((Vector) hash2.get(allkeys)).get(0)).add(jf_vos[i]);
			}
		}

		for (int i = 0; i < df_vos.length; i++) {
			String allkeys = VerifyTool.getAllkeys(df_vos[i], keys);

			if (hash2.containsKey(allkeys)) {
				((Vector) ((Vector) hash2.get(allkeys)).get(1)).add(df_vos[i]);
			} else {
				Vector<Vector<ArapBusiDataVO>> vec = new Vector();
				vec.add(new Vector());
				vec.add(new Vector());
				hash2.put(allkeys, vec);
				((Vector) ((Vector) hash2.get(allkeys)).get(1)).add(df_vos[i]);
			}
		}

		Iterator it = hash2.keySet().iterator();
		while (it.hasNext()) {
			Vector vec = (Vector) hash2.get(it.next());
			Vector jf = (Vector) vec.get(0);
			Vector df = (Vector) vec.get(1);
			// 20170305_01 tsy 添加偏移
			int j = 0;// 贷方（左）偏移
			// 20170305_01 end
			for (int i = 0; i < jf.size(); i++) {
				// 仅以下部分与contractSame(..)方法不同
				ArapBusiDataVO vo = (ArapBusiDataVO) jf.elementAt(i);
				if (!ArapCommonTool.isZero(new UFDouble(vo.getAttributeValue("SETT_MONEY").toString()))) {
					// 20170305_02 tsy 添加偏移
					if (i - j < df.size()) {
						ArapBusiDataVO vo2 = (ArapBusiDataVO) df.elementAt(i - j);
						// 20170305_02 end
						if (!ArapCommonTool.isZero(new UFDouble(vo2.getAttributeValue("SETT_MONEY").toString()))) {
							doBusiness(vo, vo2);
						}
					}
				}
				// 20170305_03 tsy 添加偏移
				if (i + 1 < jf.size() && vo.getSrc_itemid() != null && vo.getSrc_itemid().equals(((ArapBusiDataVO) jf.elementAt(i + 1)).getSrc_itemid())) {
					// 本行来源与下一行的来源相同，使用同一行的贷方（应付单）进行核销
					j++;
				}
				// 20170305_03 end
			}
		}
	}

	private int getHxSuanFa(DefaultVerifyRuleVO rulevo) {
		if (rulevo == null) {
			return 0;
		}
		Integer inte = rulevo.getM_verifySeq();
		int flag = 0;
		try {
			flag = inte.intValue();
		} catch (Exception e) {
			Log.getInstance(getClass()).debug(e.getMessage());
		}

		return flag;
	}

	public VerifyCom getVerifycom() {
		return this.verifycom;
	}

	public void setVerifycom(VerifyCom verifycom) {
		this.verifycom = verifycom;
	}

	public void doBusiness(ArapBusiDataVO vo, ArapBusiDataVO vo2) {
		if ((vo == null) || (vo2 == null)) {
			return;
		}

		UFDouble jsybje = new UFDouble(vo.getAttributeValue("SETT_MONEY").toString());

		UFDouble jsybje2 = new UFDouble(vo2.getAttributeValue("SETT_MONEY").toString());

		if (!UFDoubleTool.isTonghao(jsybje, jsybje2)) {
			return;
		}

		ArapBusiDataVO jf_vo = null;
		ArapBusiDataVO df_vo = null;

		if (isJiefang(vo)) {
			jf_vo = vo;
			df_vo = vo2;
		} else {
			jf_vo = vo2;
			df_vo = vo;
		}

		count += 1L;
		verify(jf_vo, df_vo);
	}

	private void verify(ArapBusiDataVO jf_vo, ArapBusiDataVO df_vo) {
		if ((jf_vo == null) || (df_vo == null)) {
			return;
		}

		initPph();
		UFDouble zero = new UFDouble(0);
		Boolean isOccuption = getRuleVO().getIsOccuption();

		UFDouble jf_jsybje = (UFDouble) jf_vo.getAttributeValue("SETT_MONEY");

		UFDouble jf_zk = (UFDouble) jf_vo.getAttributeValue("DISCTION");

		if (jf_zk == null) {
			jf_zk = zero;
		}

		UFDouble df_jsybje = (UFDouble) df_vo.getAttributeValue("SETT_MONEY");

		UFDouble df_zk = (UFDouble) df_vo.getAttributeValue("DISCTION");

		if (df_zk == null) {
			df_zk = zero;
		}

		boolean isJfClear = false;
		boolean isDfClear = false;

		UFDouble jf_clybje = null;
		UFDouble df_clybje = null;

		if (UFDoubleTool.isXiangdeng(jf_jsybje, df_jsybje)) {
			jf_clybje = jf_jsybje;
			df_clybje = df_jsybje;

			isDfClear = true;
			isJfClear = true;
		}

		if (UFDoubleTool.isAbsDayu(jf_jsybje, df_jsybje)) {
			jf_clybje = df_jsybje;
			df_clybje = df_jsybje;

			isDfClear = true;
		} else {
			jf_clybje = jf_jsybje;
			df_clybje = jf_jsybje;

			isJfClear = true;
		}
		jf_vo.setAttributeValue("SETT_MONEY", ((UFDouble) jf_vo.getAttributeValue("SETT_MONEY")).sub(jf_clybje));

		df_vo.setAttributeValue("SETT_MONEY", ((UFDouble) df_vo.getAttributeValue("SETT_MONEY")).sub(df_clybje));

		VerifyDetailVO detaildeVO = new VerifyDetailVO();
		detaildeVO.setIsreserve(getVerifycom().getReserveFlag());
		deepclone(detaildeVO, jf_vo);
		detaildeVO.setSrc_org(df_vo.getPk_org());
		detaildeVO.setStatus(2);
		detaildeVO.setMoney_de(jf_clybje);

		if ((isOccuption != null) && (isOccuption.booleanValue())) {
			detaildeVO.setOccupationmny(jf_clybje);
		}
		detaildeVO.setJd_flag(1);

		UFDouble[] verifyRate = VerifyTool.getVerifyRate(jf_vo, df_vo);

		UFDouble jf_shl = null;
		UFDouble df_shl = null;
		UFDouble jf_dj = null;
		UFDouble df_dj = null;

		jf_dj = (UFDouble) jf_vo.getAttributeValue("price");
		df_dj = (UFDouble) df_vo.getAttributeValue("price");

		if ((isJfClear) && (jf_vo.isIsshlclear())) {
			detaildeVO.setQuantity_de(jf_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(jf_vo.getMaterial()), 4));
		} else if ((jf_dj != null) && (!UFDoubleTool.isZero(jf_dj))) {
			if ((ArapCommonTool.isZero(jf_clybje)) && (jf_vo.isIsshlclear())) {
				jf_shl = jf_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(jf_vo.getMaterial()), 4);
				jf_vo.setAttributeValue("quantity_bal", new UFDouble(0));
			} else {
				jf_shl = jf_clybje.div(jf_dj, ArapVOUtils.getDecimalFromSource(jf_vo.getMaterial()));
				jf_vo.setAttributeValue("quantity_bal", jf_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(jf_vo.getMaterial()), 4).sub(jf_shl));
			}

			detaildeVO.setQuantity_de(jf_shl);
		}

		Integer inte_pph = Integer.valueOf(this.pph++);

		this.verifycom.getM_context().put("PPH", inte_pph);

		detaildeVO.setMoney_cr(new UFDouble(0));
		setDetailVO(detaildeVO, jf_vo, df_vo);

		detaildeVO.setRate(verifyRate[1]);

		detaildeVO.setRowno(inte_pph);

		UFDate busidate = detaildeVO.getBusidate();
		try {
			if ((detaildeVO.getMoney_de() == null) || (detaildeVO.getMoney_de().compareTo(UFDouble.ZERO_DBL) == 0)) {
				detaildeVO.setLocal_money_de(UFDouble.ZERO_DBL);
			} else {
				detaildeVO.setLocal_money_de(Currency.getAmountByOpp(jf_vo.getPk_org(), jf_vo.getPk_currtype(), DefaultCurrtypeQryUtil.getInstance().getDefaultCurrtypeByOrgID(jf_vo.getPk_org()).getPk_currtype(), detaildeVO.getMoney_de(), verifyRate[1], busidate));
			}

			detaildeVO.setLocal_money_cr(UFDouble.ZERO_DBL);
		} catch (Exception e) {
			ExceptionHandler.handleRuntimeException(e);
		}

		getLogs().add(detaildeVO);

		VerifyDetailVO detailcrVO = new VerifyDetailVO();
		detailcrVO.setIsreserve(getVerifycom().getReserveFlag());
		detailcrVO.setStatus(2);
		detailcrVO.setJd_flag(-1);
		deepclone(detailcrVO, df_vo);
		setDetailVO(detailcrVO, df_vo, jf_vo);

		detailcrVO.setMoney_cr(df_clybje);

		if ((isOccuption != null) && (isOccuption.booleanValue())) {
			detailcrVO.setOccupationmny(df_clybje);
		}
		detailcrVO.setSrc_org(jf_vo.getPk_org());

		detailcrVO.setMoney_de(UFDouble.ZERO_DBL);
		detailcrVO.setLocal_money_de(UFDouble.ZERO_DBL);

		detailcrVO.setRate(verifyRate[1]);

		detailcrVO.setRowno(inte_pph);
		try {
			if ((detailcrVO.getMoney_cr() == null) || (detailcrVO.getMoney_cr().compareTo(UFDouble.ZERO_DBL) == 0)) {
				detailcrVO.setLocal_money_cr(UFDouble.ZERO_DBL);
			} else {
				detailcrVO.setLocal_money_cr(Currency.getAmountByOpp(df_vo.getPk_org(), df_vo.getPk_currtype(), DefaultCurrtypeQryUtil.getInstance().getDefaultCurrtypeByOrgID(df_vo.getPk_org()).getPk_currtype(), detailcrVO.getMoney_cr(), verifyRate[1], busidate));
			}

			detailcrVO.setLocal_money_de(UFDouble.ZERO_DBL);
		} catch (Exception e) {
			ExceptionHandler.consume(e);
		}

		if ((isDfClear) && (df_vo.isIsshlclear())) {
			detailcrVO.setQuantity_cr(df_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(df_vo.getMaterial()), 4));
		} else if ((df_dj != null) && (!UFDoubleTool.isZero(df_dj))) {
			if ((ArapCommonTool.isZero(df_clybje)) && (df_vo.isIsshlclear())) {
				df_shl = df_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(df_vo.getMaterial()), 4);
				df_vo.setAttributeValue("quantity_bal", new UFDouble(0));
			} else {
				df_shl = df_clybje.div(df_dj, ArapVOUtils.getDecimalFromSource(df_vo.getMaterial()));
				df_vo.setAttributeValue("quantity_bal", df_vo.getQuantity_bal().setScale(ArapVOUtils.getDecimalFromSource(df_vo.getMaterial()), 4).sub(df_shl));
			}

			detailcrVO.setQuantity_cr(df_shl);
		}

		getLogs().add(detailcrVO);

		createZKLogvo(jf_vo, df_vo, verifyRate, inte_pph = Integer.valueOf(inte_pph.intValue() + 1));
		createZKLogvo(df_vo, jf_vo, verifyRate, inte_pph = Integer.valueOf(inte_pph.intValue() + 1));
	}

	private void createZKLogvo(ArapBusiDataVO jf_vo, ArapBusiDataVO df_vo, UFDouble[] verifyRate, Integer inte_pph) {
		if (jf_vo == null) {
			return;
		}
		UFDouble jf_zk = (UFDouble) jf_vo.getAttributeValue("DISCTION");

		if ((jf_zk != null) && (!UFDoubleTool.isZero(jf_zk))) {
			VerifyDetailVO detaildeVO = new VerifyDetailVO();
			detaildeVO.setIsreserve(getVerifycom().getReserveFlag());
			deepclone(detaildeVO, jf_vo);

			detaildeVO.setScomment(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0322"));
			detaildeVO.setStatus(2);

			detaildeVO.setBusiflag(ArapBillDealVOConsts.DISCTIONVERIFY_FLAG);
			setDetailVO(detaildeVO, jf_vo, df_vo);

			detaildeVO.setRate(verifyRate[1]);

			detaildeVO.setRowno(inte_pph);

			UFDate busidate = detaildeVO.getBusidate();
			if (jf_vo.getDirection().intValue() == 1) {
				detaildeVO.setSrc_org(df_vo.getPk_org());
				detaildeVO.setMoney_de(jf_zk);
				detaildeVO.setMoney_cr(UFDouble.ZERO_DBL);
				try {
					if ((detaildeVO.getMoney_de() == null) || (detaildeVO.getMoney_de().compareTo(UFDouble.ZERO_DBL) == 0)) {
						detaildeVO.setLocal_money_de(UFDouble.ZERO_DBL);
					} else {
						detaildeVO.setLocal_money_de(Currency.getAmountByOpp(jf_vo.getPk_org(), jf_vo.getPk_currtype(), DefaultCurrtypeQryUtil.getInstance().getDefaultCurrtypeByOrgID(jf_vo.getPk_org()).getPk_currtype(), detaildeVO.getMoney_de(), verifyRate[1], busidate));
					}

					detaildeVO.setLocal_money_cr(UFDouble.ZERO_DBL);

				} catch (Exception e) {

					ExceptionHandler.handleRuntimeException(e);
				}
			} else if (jf_vo.getDirection().intValue() == -1) {
				detaildeVO.setSrc_org(jf_vo.getPk_org());
				detaildeVO.setMoney_cr(jf_zk);
				detaildeVO.setMoney_de(UFDouble.ZERO_DBL);
				try {
					if ((detaildeVO.getMoney_cr() == null) || (detaildeVO.getMoney_cr().compareTo(UFDouble.ZERO_DBL) == 0)) {
						detaildeVO.setLocal_money_cr(UFDouble.ZERO_DBL);
					} else {
						detaildeVO.setLocal_money_cr(Currency.getAmountByOpp(jf_vo.getPk_org(), jf_vo.getPk_currtype(), DefaultCurrtypeQryUtil.getInstance().getDefaultCurrtypeByOrgID(jf_vo.getPk_org()).getPk_currtype(), detaildeVO.getMoney_cr(), verifyRate[1], busidate));
					}

					detaildeVO.setLocal_money_de(UFDouble.ZERO_DBL);

				} catch (Exception e) {

					ExceptionHandler.handleRuntimeException(e);
				}
			}

			getLogs().add(detaildeVO);
			jf_vo.setAttributeValue("DISCTION", null);
		}
	}

	private ArrayList<VerifyDetailVO> getLogs() {
		return this.retArr;
	}

	private boolean isJiefang(ArapBusiDataVO vo) {
		if (vo == null) {
			return false;
		}
		Integer fx = Integer.valueOf(vo.getAttributeValue("direction").toString());
		try {
			int int_fx = fx.intValue();
			return int_fx == 1;
		} catch (Exception e) {
		}

		return false;
	}

	public void deepclone(VerifyDetailVO detailVO, ArapBusiDataVO verifyvo) {
		detailVO.setBusiflag(ArapBillDealVOConsts.SAMEVERIFY_FLAG);

		detailVO.setRedflag(Integer.valueOf(0));

		detailVO.setBusidate(getRuleVO().getM_Clrq());

		detailVO.setBusiyear(getRuleVO().getM_Clnd());

		detailVO.setBusiperiod(getRuleVO().getM_Clqj());

		detailVO.setCreator(getRuleVO().getM_clr());

		detailVO.setPk_group(verifyvo.getAttributeValue("pk_group").toString());

		detailVO.setPk_org(verifyvo.getAttributeValue("pk_org").toString());

		detailVO.setSrc_org(verifyvo.getSrc_org());

		detailVO.setIsauto(UFBoolean.FALSE);

		detailVO.setBusidata(verifyvo);

		detailVO.setScomment(NCLangRes4VoTransl.getNCLangRes().getStrByID("2006ver_0", "02006ver-0157"));
	}

	private void setDetailVO(VerifyDetailVO detailvo, ArapBusiDataVO jf_vo, ArapBusiDataVO df_vo) {
		detailvo.setBillno((String) jf_vo.getAttributeValue("billno"));
		detailvo.setBillno2((String) df_vo.getAttributeValue("billno"));

		detailvo.setPk_bill(jf_vo.getPk_bill());
		detailvo.setPk_bill2(df_vo.getPk_bill());

		detailvo.setPk_item(jf_vo.getPk_item());
		detailvo.setPk_item2(df_vo.getPk_item());

		detailvo.setPk_termitem(jf_vo.getPk_termitem());
		detailvo.setPk_termitem2(df_vo.getPk_termitem());

		detailvo.setPk_busidata(jf_vo.getPk_busidata());
		detailvo.setPk_busidata2(df_vo.getPk_busidata());

		detailvo.setPk_tradetype(jf_vo.getPk_tradetypeid());
		detailvo.setPk_tradetype2(df_vo.getPk_tradetypeid());

		detailvo.setPk_billtype(jf_vo.getAttributeValue("pk_billtype").toString());

		detailvo.setPk_billtype2(df_vo.getAttributeValue("pk_billtype").toString());

		detailvo.setBillclass(jf_vo.getBillclass());
		detailvo.setBillclass2(df_vo.getBillclass());

		detailvo.setGrouprate((UFDouble) jf_vo.getAttributeValue("grouprate"));

		detailvo.setGlobalrate((UFDouble) jf_vo.getAttributeValue("globalrate"));

		detailvo.setMaterial(jf_vo.getAttributeValue("material") == null ? null : jf_vo.getAttributeValue("material").toString());

		detailvo.setPk_costsubj(jf_vo.getAttributeValue("pk_costsubj") == null ? null : jf_vo.getAttributeValue("pk_costsubj").toString());

		detailvo.setCustomer(jf_vo.getAttributeValue("customer") == null ? null : jf_vo.getAttributeValue("customer").toString());

		detailvo.setSupplier(jf_vo.getAttributeValue("supplier") == null ? null : jf_vo.getAttributeValue("supplier").toString());

		detailvo.setOrdercubasdoc(jf_vo.getAttributeValue("ordercubasdoc") == null ? null : jf_vo.getAttributeValue("ordercubasdoc").toString());

		detailvo.setPk_deptid(jf_vo.getAttributeValue("pk_deptid") == null ? null : jf_vo.getAttributeValue("pk_deptid").toString());

		detailvo.setPk_psndoc(jf_vo.getAttributeValue("pk_psndoc") == null ? null : jf_vo.getAttributeValue("pk_psndoc").toString());

		detailvo.setPk_currtype(jf_vo.getAttributeValue("pk_currtype").toString());

		detailvo.setExpiredate((UFDate) jf_vo.getAttributeValue("expiredate"));

		detailvo.setInnerctldeferdays((UFDate) jf_vo.getAttributeValue("innerctldeferdays"));
	}
}