package nc.vo.arap.verify;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import nc.bs.logging.Log;
import nc.vo.arap.agiotage.ArapBusiDataVO;
import nc.vo.arap.global.ArapBillDealVOConsts;
import nc.vo.arap.verifynew.MethodVO;
import nc.vo.jcom.util.SortUtils;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;

public class VerifyTool implements Serializable {
	public VerifyTool() {
	}

	public static final Integer CLBZ_VERIFY_ZK_N2 = Integer.valueOf(-2);
	public static final Integer CLBZ_VERIFY_UNSAME_N1 = Integer.valueOf(-1);
	public static final Integer CLBZ_VERIFY_SAME_0 = Integer.valueOf(0);
	public static final Integer CLBZ_VERIFY_RB_2 = Integer.valueOf(2);

	public static MethodVO[] getMethods() throws BusinessException {
		Vector<MethodVO> temp = new Vector();
		MethodVO vo = new MethodVO();
		vo.setFa("RED_BLUE");
		vo.setImplname("nc.vo.arap.verify.RBVerify");
		vo.setCheck_class("nc.ui.arap.verifynew.RBDataChecker");
		temp.add(vo);

		MethodVO vo1 = new MethodVO();
		vo1.setFa("SAME_VERIFY");
		vo1.setImplname("nc.vo.arap.verify.SameMnyVerify");
		vo1.setCheck_class("nc.ui.arap.verifynew.SameDataChecker");
		temp.add(vo1);

		MethodVO vo2 = new MethodVO();
		vo2.setFa("UN_SAME_VERIFY");
		vo2.setImplname("nc.vo.arap.verify.UnsameMnyVerify");
		vo2.setCheck_class("nc.ui.arap.verifynew.UnsameDataChecker");
		temp.add(vo2);

		return (MethodVO[]) temp.toArray(new MethodVO[0]);
	}

	public static void bb_fb_jisuan_new(ArapBusiDataVO jf_vo, UFDouble[] verifyRate, VerifyDetailVO detailVO, UFDate clrq, String dwbm) {
		if ((jf_vo == null) || (detailVO == null)) {
			return;
		}

		detailVO.setRate(verifyRate[1]);
	}

	public static UFDouble[] getVerifyRate(ArapBusiDataVO jf_vo, ArapBusiDataVO df_vo) {
		ArapBusiDataVO rateVO = null;

		String jf_billtype = jf_vo.getAttributeValue("pk_billtype").toString();
		String df_billtype = df_vo.getAttributeValue("pk_billtype").toString();

		if ((df_billtype.equals("F2")) || (df_billtype.equals("23F2")) || (df_billtype.equals("F3")) || (df_billtype.equals("23F3"))) {

			if ((jf_billtype.equals("F2")) || (jf_billtype.equals("23F2")) || (jf_billtype.equals("F3")) || (jf_billtype.equals("23F3"))) {

				if (jf_vo.getBilldate().toString().equals(df_vo.getBilldate().toString())) {
					boolean after = new UFDateTime(jf_vo.getAttributeValue("ts").toString()).after(new UFDateTime(df_vo.getAttributeValue("ts").toString()));

					rateVO = after ? jf_vo : df_vo;
				} else {
					boolean after = jf_vo.getBilldate().after(df_vo.getBilldate());
					rateVO = after ? jf_vo : df_vo;
				}
			} else {
				rateVO = df_vo;
			}
		} else if ((jf_billtype.equals("F2")) || (jf_billtype.equals("23F2")) || (jf_billtype.equals("F3")) || (jf_billtype.equals("23F3"))) {
			rateVO = jf_vo;
		} else if (jf_vo.getBilldate().toString().equals(df_vo.getBilldate().toString())) {
			boolean after = new UFDateTime(jf_vo.getAttributeValue("ts").toString()).after(new UFDateTime(df_vo.getAttributeValue("ts").toString()));

			rateVO = after ? jf_vo : df_vo;
		} else {
			boolean after = jf_vo.getBilldate().after(df_vo.getBilldate());
			rateVO = after ? jf_vo : df_vo;
		}

		UFDouble zfhv = new UFDouble(0.0D);
		UFDouble zbhv = new UFDouble(rateVO.getAttributeValue("rate").toString());
		UFDouble grouphv = new UFDouble(rateVO.getAttributeValue("grouprate").toString());
		UFDouble globalhv = new UFDouble(rateVO.getAttributeValue("globalrate").toString());

		return new UFDouble[] { zfhv, zbhv, grouphv, globalhv };
	}

	public static UFDate getVerifyDate(ArapBusiDataVO jf_vo, ArapBusiDataVO df_vo) {
		ArapBusiDataVO rateVO = null;

		String jf_billtype = jf_vo.getAttributeValue("pk_billtype").toString();
		String df_billtype = df_vo.getAttributeValue("pk_billtype").toString();

		if ((df_billtype.equals("F2")) || (df_billtype.equals("23F2")) || (df_billtype.equals("F3")) || (df_billtype.equals("23F3"))) {

			if ((jf_billtype.equals("F2")) || (jf_billtype.equals("23F2")) || (jf_billtype.equals("F3")) || (jf_billtype.equals("23F3"))) {

				if (jf_vo.getBilldate().toString().equals(df_vo.getBilldate().toString())) {
					boolean after = new UFDateTime(jf_vo.getAttributeValue("ts").toString()).after(new UFDateTime(df_vo.getAttributeValue("ts").toString()));

					rateVO = after ? jf_vo : df_vo;
				} else {
					boolean after = jf_vo.getBilldate().after(df_vo.getBilldate());
					rateVO = after ? jf_vo : df_vo;
				}
			} else {
				rateVO = df_vo;
			}
		} else if ((jf_billtype.equals("F2")) || (jf_billtype.equals("23F2")) || (jf_billtype.equals("F3")) || (jf_billtype.equals("23F3"))) {
			rateVO = jf_vo;
		} else if (jf_vo.getBilldate().toString().equals(df_vo.getBilldate().toString())) {
			boolean after = new UFDateTime(jf_vo.getAttributeValue("ts").toString()).after(new UFDateTime(df_vo.getAttributeValue("ts").toString()));

			rateVO = after ? jf_vo : df_vo;
		} else {
			boolean after = jf_vo.getBilldate().after(df_vo.getBilldate());
			rateVO = after ? jf_vo : df_vo;
		}

		return rateVO.getBilldate();
	}

	public static boolean isAuto(Integer clbz) {
		return (clbz.equals(ArapBillDealVOConsts.AGIOTAGE_YSX_FLAG)) || (clbz.equals(ArapBillDealVOConsts.AGIOTAGE_WSXHC_FLAG));
	}

	public static String getAllkeys(ArapBusiDataVO vo, String[] keys) {
		if (vo == null) {
			return null;
		}
		if ((keys == null) || (keys.length == 0)) {
			return "SHOU_GONG_HE_XIAO";
		}
		String allKeys = "";

		for (int j = 0; j < keys.length; j++) {
			Object obj = null;
			if (keys[j].equals("ybje")) {
				UFDouble cr = (UFDouble) vo.getAttributeValue("money_cr");
				obj = cr.add((UFDouble) vo.getAttributeValue("money_de"));

			} else if (keys[j].toLowerCase().equals("wldxpk")) {
				obj = vo.getWldxpk();
			} else {
				obj = vo.getAttributeValue(keys[j].toLowerCase());
			}

			if (obj != null) {
				allKeys = allKeys + obj;
			}
		}
		return allKeys;
	}

	public static void sortVector(Vector<ArapBusiDataVO> v, int flag) {
		if ((v == null) || (v.size() < 2)) {
			return;
		}

		VoComparer comp = new VoComparer();
		comp.setAscend(new boolean[] { flag == 0 ? true : false });
		comp.setCompareKey(new String[] { "expiredate" });
		comp.setDaterange(0);
		SortUtils.sort(v, null, comp);
	}

	public static void sortVector(ArapBusiDataVO[] vos, int flag) {
		if ((vos == null) || (vos.length < 2)) {
			return;
		}
		VoComparer comp = new VoComparer();
		comp.setAscend(new boolean[] { flag == 0 ? true : false });
		comp.setCompareKey(new String[] { "expiredate" });
		comp.setDaterange(0);
		SortUtils.sort(vos, null, comp);
	}
	/**
	 * @author tsy 20170203 
	 * <p>通用排序</p>
	 * @param vos
	 * @param flag 0为正顺 1为倒序
	 * @param compareKey 排序依据，默认为expiredate
	 */
	public static void sortVector(ArapBusiDataVO[] vos, int flag, String compareKey) {
		if ((vos == null) || (vos.length < 2)) {
			return;
		}
		VoComparer comp = new VoComparer();
		comp.setAscend(new boolean[] { flag == 0 ? true : false });
		//compareKey默认为"expiredate"
		if (compareKey == null || "".equals(compareKey.trim())) {
			compareKey = "expiredate";
		}
		comp.setCompareKey(new String[] { compareKey });
		comp.setDaterange(0);
		SortUtils.sort(vos, null, comp);
	}

	public static void main(String[] ars) {
		ArapBusiDataVO[] vos = new ArapBusiDataVO[5];
		ArapBusiDataVO arapBusiDataVO = new ArapBusiDataVO();
		arapBusiDataVO.setExpiredate(new UFDate("2010-09-01"));
		arapBusiDataVO.setInsurance(UFBoolean.TRUE);
		vos[0] = arapBusiDataVO;

		ArapBusiDataVO arapBusiDataVO1 = new ArapBusiDataVO();
		arapBusiDataVO1.setExpiredate(new UFDate("2010-09-01"));
		arapBusiDataVO1.setInsurance(UFBoolean.FALSE);
		vos[1] = arapBusiDataVO1;

		ArapBusiDataVO arapBusiDataVO2 = new ArapBusiDataVO();
		arapBusiDataVO2.setExpiredate(new UFDate("2010-09-05"));
		arapBusiDataVO2.setInsurance(UFBoolean.FALSE);
		vos[2] = arapBusiDataVO2;

		ArapBusiDataVO arapBusiDataVO3 = new ArapBusiDataVO();
		arapBusiDataVO3.setExpiredate(new UFDate("2010-09-05"));
		arapBusiDataVO3.setInsurance(UFBoolean.TRUE);
		vos[3] = arapBusiDataVO3;

		ArapBusiDataVO arapBusiDataVO4 = new ArapBusiDataVO();
		arapBusiDataVO4.setExpiredate(new UFDate("2010-09-03"));
		arapBusiDataVO4.setInsurance(UFBoolean.TRUE);
		vos[4] = arapBusiDataVO4;

		sortVector(vos, 0);

		for (ArapBusiDataVO v : vos) {
			Log.getInstance("arapSystemOut").warn(v.getExpiredate());
			Log.getInstance("arapSystemOut").warn("--" + v.getInsurance());
		}
	}

	public static Hashtable<String, Vector<ArapBusiDataVO>> createHash(ArapBusiDataVO[] vos, boolean isAnDanjv, Integer hxseq, Set<String> showKeys) {
		if ((vos == null) || (vos.length == 0)) {
			return new Hashtable();
		}
		Hashtable<String, Vector<ArapBusiDataVO>> verifyhash = new Hashtable();
		for (int i = 0; i < vos.length; i++)
			if (vos[i] != null) {

				String billpk = vos[i].getPk_bill();
				String objtypepk = "";

				if (((Integer) vos[i].getAttributeValue("objtype")).intValue() == 0) {
					objtypepk = vos[i].getAttributeValue("customer").toString();
				} else if (((Integer) vos[i].getAttributeValue("objtype")).intValue() == 1) {
					objtypepk = vos[i].getAttributeValue("supplier").toString();
				} else if (((Integer) vos[i].getAttributeValue("objtype")).intValue() == 2) {
					objtypepk = vos[i].getAttributeValue("pk_deptid").toString();
				} else if (((Integer) vos[i].getAttributeValue("objtype")).intValue() == 3) {
					objtypepk = vos[i].getAttributeValue("pk_psndoc").toString();
				}
				String billitempk = vos[i].getPk_item();
				if ((billpk != null) && (billpk.trim().length() != 0) && (billitempk != null) && (billitempk.trim().length() != 0)) {

					String key = null;
					if (isAnDanjv) {
						key = billpk + "_" + objtypepk;
					} else {
						key = billitempk + "_" + objtypepk;
					}

					Vector<ArapBusiDataVO> verifyvo = null;
					if (verifyhash.containsKey(key)) {
						verifyvo = (Vector) verifyhash.get(key);
					} else {
						verifyvo = new Vector();
					}
					verifyvo.add(vos[i]);
					verifyhash.put(key, verifyvo);
				}
			}
		sort(verifyhash, hxseq);
		return verifyhash;
	}

	private static Hashtable<String, Vector<ArapBusiDataVO>> sort(Hashtable<String, Vector<ArapBusiDataVO>> h, Integer hxseq) {
		if ((h == null) || (h.isEmpty())) {
			return null;
		}
		Iterator iter = h.keySet().iterator();

		while (iter.hasNext()) {
			Vector<ArapBusiDataVO> v = (Vector) h.get(iter.next());
			sortVector(v, hxseq.intValue());
		}
		return h;
	}

	private static final long serialVersionUID = 2304997787172591993L;

	private static final String SHOU_GONG_HE_XIAO = "SHOU_GONG_HE_XIAO";

	public static ArapBusiDataVO[] sumVerifyVO(Hashtable<String, Vector<ArapBusiDataVO>> h, Integer hxseq) {
		if ((h == null) || (h.isEmpty())) {
			return null;
		}
		Iterator iter = h.keySet().iterator();
		Vector<ArapBusiDataVO> vect = new Vector();
		while (iter.hasNext()) {
			Object key = iter.next();
			Vector<ArapBusiDataVO> v = (Vector) h.get(key);
			ArapBusiDataVO vo = sumVerifyVO(v);
			vect.add(vo);
		}
		sortVector(vect, hxseq.intValue());
		ArapBusiDataVO[] vos = new ArapBusiDataVO[vect.size()];
		vect.copyInto(vos);
		return vos;
	}

	public static ArapBusiDataVO[] sumVerifyVO(Hashtable<String, Vector<ArapBusiDataVO>> h) {
		if ((h == null) || (h.isEmpty())) {
			return null;
		}
		Iterator iter = h.keySet().iterator();
		Vector<ArapBusiDataVO> vect = new Vector();
		while (iter.hasNext()) {
			Object key = iter.next();
			Vector<ArapBusiDataVO> v = (Vector) h.get(key);
			ArapBusiDataVO vo = sumVerifyVO(v);
			vect.add(vo);
		}
		ArapBusiDataVO[] vos = new ArapBusiDataVO[vect.size()];
		vect.copyInto(vos);
		return vos;
	}

	private static ArapBusiDataVO sumVerifyVO(Vector v) {
		if ((v == null) || (v.size() == 0)) {
			return null;
		}

		UFDouble money_bal = new UFDouble(0);

		UFDouble money = new UFDouble(0);
		ArapBusiDataVO sumvo = null;
		for (int i = 0; i < v.size(); i++) {
			ArapBusiDataVO verifyvo = (ArapBusiDataVO) v.elementAt(i);
			if (sumvo == null) {
				sumvo = (ArapBusiDataVO) verifyvo.clone();
				money = verifyvo.getMoney();
				money_bal = (UFDouble) verifyvo.getAttributeValue("occupationmny");
			} else {
				money = verifyvo.getMoney() == null ? money : verifyvo.getMoney().add(money);
				money_bal = verifyvo.getAttributeValue("occupationmny") == null ? money_bal : ((UFDouble) verifyvo.getAttributeValue("occupationmny")).add(money_bal);
			}
		}
		sumvo.setSum_money(money);
		sumvo.setMoney_bal(money_bal);

		if (sumvo.getDirection().intValue() == 1) {
			sumvo.setMoney_de(money);
			sumvo.setMoney_cr(UFDouble.ZERO_DBL);
		} else {
			sumvo.setMoney_de(UFDouble.ZERO_DBL);
			sumvo.setMoney_cr(money);
		}
		return sumvo;
	}
}