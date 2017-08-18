package nc.bs.arap.bx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nc.itf.fi.pub.Currency;
import nc.pubitf.accperiod.AccountCalendar;
import nc.vo.bd.period.AccperiodVO;
import nc.vo.bd.period2.AccperiodmonthVO;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessRuntimeException;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class ErContrastUtil {
	public static void addinfotoContrastVos(List<BxcontrastVO> saveContrast) {
		for (BxcontrastVO vo : saveContrast)
			try {
				AccountCalendar periodVO = AccountCalendar.getInstance();
				periodVO.setDate(vo.getCxrq());
				vo.setCxnd(periodVO.getYearVO().getPeriodyear());
				vo.setCxqj(periodVO.getMonthVO().getAccperiodmth());
			} catch (Exception e3) {
				ExceptionHandler.consume(e3);
			}
	}

	public static List<BxcontrastVO> dealContrastForNew(JKBXHeaderVO parentVO, BxcontrastVO[] bxcontrastVOs, BXBusItemVO[] items) {
		List contrastVos = new ArrayList();
		UFDouble bbhl = parentVO.getBbhl();
		UFDouble globalbbhl = parentVO.getGlobalbbhl();
		UFDouble groupbbhl = parentVO.getGroupbbhl();
		String bzbm = parentVO.getBzbm();
		UFDate djrq = parentVO.getDjrq();
		String zfdwbm = parentVO.getPk_org();
		String pk_group = parentVO.getPk_group();

		if ((items == null) || (items.length == 0) || (bxcontrastVOs == null) || (bxcontrastVOs.length == 0)) {
			return contrastVos;
		}
		String pk_jkbx = items[0].getPk_jkbx();

		for (BxcontrastVO vo : bxcontrastVOs) {
			vo.setPk_bxd(pk_jkbx);
			if (vo.getHkybje() == null)
				vo.setHkybje(vo.getCjkybje().sub(vo.getFyybje()));
			vo.setBxdjbh(parentVO.getDjbh());
			if (parentVO.getDjzt().intValue() == 0)
				vo.setSxbz(Integer.valueOf(2));
			else {
				vo.setSxbz(Integer.valueOf(0));
			}

		}

		dealMultipModify(bxcontrastVOs, items);

		List contrastVosNew = null;
		if (items.length == 1) {
			for (BxcontrastVO vo : bxcontrastVOs) {
				vo.setPk_finitem(items[0].getPrimaryKey());
				contrastVos.add(vo);
			}

			contrastVosNew = combineSameContrasts(contrastVos);
		} else {
			List busItemList = new ArrayList();
			for (BXBusItemVO item : items) {
				busItemList.add((BXBusItemVO) item.clone());
			}

			contrastVosNew = dealMultiDistribute(bxcontrastVOs, contrastVos, busItemList);
		}

		resetCjkJeToBB(bbhl, globalbbhl, groupbbhl, bzbm, djrq, zfdwbm, pk_group, contrastVosNew);
		return contrastVosNew;
	}

	private static List<BxcontrastVO> dealMultiDistribute(BxcontrastVO[] bxcontrastVOs, List<BxcontrastVO> contrastVos, List<BXBusItemVO> busitems) {
		for (BXBusItemVO itemVO : busitems) {
			UFDouble cjkybje = itemVO.getCjkybje();
			UFDouble hkybje = itemVO.getHkybje();

			if (cjkybje.compareTo(UFDouble.ZERO_DBL) == 0) {
				continue;
			}
			if (hkybje.compareTo(UFDouble.ZERO_DBL) != 0) {
				for (int i = 0; i < bxcontrastVOs.length; i++) {
					cjkybje = itemVO.getCjkybje();
					hkybje = itemVO.getHkybje();

					if (itemVO.getHkybje().compareTo(UFDouble.ZERO_DBL) == 0) {
						break;
					}
					BxcontrastVO bxcontrastVO = bxcontrastVOs[i];
					UFDouble cjkybje2 = bxcontrastVO.getCjkybje();
					UFDouble hkybje2 = bxcontrastVO.getHkybje();
					if (cjkybje2.compareTo(UFDouble.ZERO_DBL) == 0)
						continue;
					if (hkybje2.compareTo(UFDouble.ZERO_DBL) != 0) {
						UFDouble clje = hkybje.compareTo(hkybje2) > 0 ? hkybje2 : hkybje;
						BxcontrastVO vonew = (BxcontrastVO) bxcontrastVO.clone();
						vonew.setHkybje(clje);
						vonew.setCjkybje(clje);
						vonew.setPk_finitem(itemVO.getPrimaryKey());
						vonew.setPk_bxd(itemVO.getPk_jkbx());
						vonew.setFyybje(UFDouble.ZERO_DBL);
						contrastVos.add(vonew);

						itemVO.setHkybje(itemVO.getHkybje().sub(clje));
						itemVO.setCjkybje(itemVO.getCjkybje().sub(clje));
						bxcontrastVO.setHkybje(bxcontrastVO.getHkybje().sub(clje));
						bxcontrastVO.setCjkybje(bxcontrastVO.getCjkybje().sub(clje));
					}
				}

			}

		}

		for (BXBusItemVO itemVO : busitems) {
			for (int i = 0; i < bxcontrastVOs.length; i++) {
				UFDouble cjkybje = itemVO.getCjkybje();

				if (cjkybje.compareTo(UFDouble.ZERO_DBL) == 0) {
					break;
				}
				BxcontrastVO bxcontrastVO = bxcontrastVOs[i];
				UFDouble cjkybje2 = bxcontrastVO.getCjkybje();

				if (cjkybje2.compareTo(UFDouble.ZERO_DBL) == 0) {
					continue;
				}
				UFDouble clje = cjkybje.compareTo(cjkybje2) > 0 ? cjkybje2 : cjkybje;
				BxcontrastVO vonew = (BxcontrastVO) bxcontrastVO.clone();
				vonew.setHkybje(UFDouble.ZERO_DBL);
				vonew.setCjkybje(clje);
				vonew.setPk_finitem(itemVO.getPrimaryKey());
				vonew.setPk_bxd(itemVO.getPk_jkbx());
				vonew.setFyybje(clje);
				contrastVos.add(vonew);

				itemVO.setCjkybje(itemVO.getCjkybje().sub(clje));
				bxcontrastVO.setCjkybje(bxcontrastVO.getCjkybje().sub(clje));
			}
		}

		List contrastVosNew = combineSameContrasts(contrastVos);

		return contrastVosNew;
	}

	private static List<BxcontrastVO> combineSameContrasts(List<BxcontrastVO> contrastVos) {
		Map map = new HashMap();
		for (BxcontrastVO cvo : contrastVos) {
			String key = cvo.getPk_finitem() + cvo.getPk_jkd() + cvo.getPk_busitem();
			if (map.containsKey(key)) {
				BxcontrastVO bxcontrastVO = (BxcontrastVO) map.get(key);
				bxcontrastVO.setCjkybje(bxcontrastVO.getCjkybje().add(cvo.getCjkybje()));
				bxcontrastVO.setHkybje(bxcontrastVO.getHkybje().add(cvo.getHkybje()));
				bxcontrastVO.setFyybje(bxcontrastVO.getFyybje().add(cvo.getFyybje()));
				map.put(key, bxcontrastVO);
			} else {
				map.put(key, cvo);
			}
		}

		List contrastVosNew = new ArrayList();
		contrastVosNew.addAll(map.values());
		return contrastVosNew;
	}

	private static void dealMultipModify(BxcontrastVO[] bxcontrastVOs, BXBusItemVO[] busItems) {
		UFDouble contrastHkSum = UFDouble.ZERO_DBL;
		UFDouble busItemHkYbSum = UFDouble.ZERO_DBL;

		for (BxcontrastVO vo : bxcontrastVOs) {
			contrastHkSum = contrastHkSum.add(vo.getHkybje());
		}
		for (BXBusItemVO vo : busItems) {
			busItemHkYbSum = busItemHkYbSum.add(new UFDouble((String) vo.getDefitem25()));// jss
																							// 2016-08-25
																							// 专用发票报销，冲销金额取Defitem22
		}

		if (busItemHkYbSum.equals(contrastHkSum)) {
			return;
		}

		for (BxcontrastVO vo : bxcontrastVOs) {
			UFDouble contrastHkje = UFDouble.ZERO_DBL;
			if (busItemHkYbSum.compareTo(UFDouble.ZERO_DBL) > 0) {
				if (busItemHkYbSum.compareTo(vo.getCjkybje()) > 0)
					contrastHkje = vo.getCjkybje();
				else {
					contrastHkje = busItemHkYbSum;
				}
				busItemHkYbSum = busItemHkYbSum.sub(contrastHkje);
			}
			vo.setHkybje(contrastHkje);
			vo.setFyybje(busItemHkYbSum);// vo.getCjkybje().sub(vo.getHkybje())
		}
	}

	public static void resetCjkJeToBB(UFDouble bbhl, UFDouble globalhl, UFDouble grouphl, String bzbm, UFDate djrq, String zfdwbm, String pk_group, List<BxcontrastVO> contrastVosNew) {
		for (BxcontrastVO cvo : contrastVosNew) {
			try {
				UFDouble cjkbbje = Currency.computeYFB(zfdwbm, 2, bzbm, cvo.getCjkybje(), null, null, null, bbhl, djrq)[2];
				cvo.setCjkbbje(cjkbbje);
				UFDouble[] money =
						Currency.computeGroupGlobalAmount(cvo.getCjkybje(), cjkbbje, bzbm, djrq, cvo.getPk_org(), pk_group, globalhl, grouphl);
				cvo.setGroupcjkbbje(money[0]);
				cvo.setGlobalcjkbbje(money[1]);
				UFDouble fybbje = Currency.computeYFB(zfdwbm, 2, bzbm, cvo.getFyybje(), null, null, null, bbhl, djrq)[2];
				cvo.setFybbje(fybbje);
				UFDouble[] money2 =
						Currency.computeGroupGlobalAmount(cvo.getFyybje(), fybbje, bzbm, djrq, cvo.getPk_org(), pk_group, globalhl, grouphl);
				cvo.setGroupfybbje(money2[0]);
				cvo.setGlobalfybbje(money2[1]);
			} catch (Exception e) {
				throw new BusinessRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000387"));
			}
			cvo.setYbje(cvo.getCjkybje());
			cvo.setBbje(cvo.getCjkbbje());

			cvo.setGlobalbbje(cvo.getGlobalcjkbbje());
			cvo.setGroupbbje(cvo.getGroupcjkbbje());
		}
	}
}