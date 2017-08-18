package nc.vo.tmpub.security.formater;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nc.itf.tmpub.security.IAttributeFormater;
import nc.itf.tmpub.security.IEncryptFormater;
import nc.vo.logging.Debug;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFDouble;
import nc.vo.tmpub.security.IEncryptVO;
import nc.vo.tmpub.util.StringUtil;

/**
 * 默认签名加密格式化 抽象类
 * 
 * @author panf
 * @version 6.0
 */
public abstract class AbstractEncryptFormater implements IEncryptFormater {
	private Map<String, List<IAttributeFormater>> parentatrrs;
	private Map<String, Map<String, List<IAttributeFormater>>> childrenatrrs;
	private AggregatedValueObject[] markvos;
	private String rowname;

	@Override
	public String[] getUnEncryptString() throws BusinessException {
		if (markvos == null) {
			return null;
		}
		int index = 0;
		String[] retunEncryptarrs = new String[markvos.length];
		for (AggregatedValueObject billvo : markvos) {
			String signstr = null;
			String parentstr = createParentStr(billvo);
			if (parentstr != null) {
				signstr = parentstr;
			}
			String children = createChildrenStr(billvo);
			if (children != null) {
				signstr += children;
			}

			retunEncryptarrs[index] = signstr;
			index++;
		}
		return retunEncryptarrs;
	}

	protected String createParentStr(AggregatedValueObject billvo) {
		if (parentatrrs == null || parentatrrs.size() == 0) {
			return null;
		}

		if (billvo.getParentVO() == null) {
			return null;
		}

		return ConvertUnEncryptString(parentatrrs, billvo.getParentVO());
	}

	protected String createChildrenStr(AggregatedValueObject billvo) throws BusinessException {
		if (childrenatrrs == null || childrenatrrs.size() == 0) {
			return null;
		}
		return childrensConvert(billvo, childrenatrrs);
	}

	protected String ConvertUnEncryptString(Map<String, List<IAttributeFormater>> attributes, CircularlyAccessibleValueObject... invo) {
		StringBuilder result = new StringBuilder();

		if (invo == null || attributes == null) {
			return null;
		}

		CircularlyAccessibleValueObject[] ordervos = orderByRowNo(invo);

		for (CircularlyAccessibleValueObject tempVO : ordervos) {
			Object drobj = tempVO.getAttributeValue("dr");
			if (drobj == null || "0".equals(drobj.toString())) {
				Iterator<String> iterator = attributes.keySet().iterator();
				while (iterator.hasNext()) {
					String keyname = iterator.next();
					Object value = tempVO.getAttributeValue(keyname);
					if (value == null) {
						continue;
					}
					List<IAttributeFormater> formaterlist = attributes.get(keyname);
					if (formaterlist != null) {
						for (IAttributeFormater formater : formaterlist) {
							value = formater.getValue(value);
						}
					}
					result.append(value);// 使用于XML中配置字段的情形
				}
			}
		}

		return result.toString();
	}

	/**
	 * 按行号排序，降序排列
	 * 
	 * @param billvos
	 * @return
	 */
	private CircularlyAccessibleValueObject[] orderByRowNo(CircularlyAccessibleValueObject[] billvos) {
		if (StringUtil.isNull(rowname)) {
			return billvos;
		}
		// CircularlyAccessibleValueObject[] retordervos = new
		// CircularlyAccessibleValueObject[billvos.length];
		TreeMap<UFDouble, CircularlyAccessibleValueObject> bodys = new TreeMap<UFDouble, CircularlyAccessibleValueObject>();
		for (CircularlyAccessibleValueObject tmpvo : billvos) {
			if (tmpvo.getAttributeValue(rowname) == null || tmpvo.getAttributeValue(rowname).toString().equals("")) {
				// 有一行为空，不进行排序
				Debug.error(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("3601tmpub_0", "03601tmpub-0379")/*
																													 * @
																													 * res
																													 * "签名进行表体排序时，有一行为空，不进行排序！"
																													 */);
				return sortByPK(billvos);
			}
			bodys.put(getRowNo(tmpvo.getAttributeValue(rowname)), tmpvo);
		}
		return bodys.values().toArray(new CircularlyAccessibleValueObject[bodys.values().size()]);
	}

	// added by zhufeng 2014-10-10 start
	private CircularlyAccessibleValueObject[] sortByPK(CircularlyAccessibleValueObject[] billvos) {
		// 20170503 tsy 修复数据异常，clone方法会导致数据改变
//		 CircularlyAccessibleValueObject[] cloneBillvos = billvos.clone();
		CircularlyAccessibleValueObject[] cloneBillvos = Arrays.copyOf(billvos, billvos.length);
		// 20170503 end
		Arrays.sort(cloneBillvos, new Comparator<CircularlyAccessibleValueObject>() {
			@Override
			public int compare(CircularlyAccessibleValueObject o1, CircularlyAccessibleValueObject o2) {
				try {
					return o1.getPrimaryKey().compareTo(o2.getPrimaryKey());
//				} catch (BusinessException e) {
				} catch (Exception e) {// 20170503 tsy 顺手捕捉空指针异常
					Debug.debug(e.getMessage(), e);
					return -1;
				}
				// 20170503 end
			}
		});
		return cloneBillvos;
	}

	// added by zhufeng 2014-10-10 end

	private UFDouble getRowNo(Object rowno) {
		if (String.class.equals(rowno.getClass()) || Integer.class.equals(rowno.getClass())) {
			return new UFDouble(rowno.toString());
		}
		return (UFDouble) rowno;
	}

	/**
	 * 子表签名串组装，延迟到子类按不同情况实现处理
	 * 
	 * @param markvo
	 * @return
	 */
	protected abstract String childrensConvert(AggregatedValueObject markvo, Map<String, Map<String, List<IAttributeFormater>>> childrenatrrs);

	@Override
	public void setEncryptVO(IEncryptVO invo) {
		parentatrrs = invo.getEncryptAtrributesByParent();
		childrenatrrs = invo.getEncryptAtrributesByMultiChildrens();
		markvos = invo.getAggVO();
		rowname = invo.getChildrenVORowName();
	}

}