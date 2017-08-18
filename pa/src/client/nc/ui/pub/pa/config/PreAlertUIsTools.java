package nc.ui.pub.pa.config;

import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.itf.uap.pa.IPreAlertConfigQueryService;
import nc.ui.pub.beans.UIScrollBar;
import nc.ui.pub.beans.UITextField;
import nc.vo.jcom.lang.StringUtil;
import nc.vo.pub.BusinessException;
import nc.vo.pub.pa.AlertregistryVO;

public class PreAlertUIsTools {
	public PreAlertUIsTools() {
	}

	public static void scb_validateTextField(UITextField textField, UIScrollBar scrollBar) {
		int value = Integer.parseInt(textField.getText());
		int min = scrollBar.getMinimum();
		int max = scrollBar.getMaximum();
		if ((min <= value) && (value < max)) {
			scrollBar.setValue(max - value);
		} else {
			textField.grabFocus();
			textField.setSelectionStart(0);
			textField.setSelectionEnd(textField.getText().length());
		}
	}

	public static void scb_initial(UIScrollBar scrollBar, int min, int max, int value, int unitIncrement, int blockIncrement, int extent) {
		scrollBar.setMinimum(min);
		scrollBar.setMaximum(max + 1);
		scrollBar.setBlockIncrement(blockIncrement);
		scrollBar.setUnitIncrement(unitIncrement);
		scrollBar.setVisibleAmount(extent);
		scrollBar.setValue(scrollBar.getMaximum() - value);
	}

	public static AlertregistryVO[] getRegistryAggVOsOfTypeByGroup(String pkAlerttype, String groupid) throws BusinessException {
		String whereSql = null;
		if ("0001".equals(groupid)) {
			whereSql = "pk_alerttype='" + pkAlerttype + "' and (groupid='" + groupid + "' or groupid='')";
		} else
			whereSql = "pk_alerttype='" + pkAlerttype + "' and groupid='" + groupid + "'";
		IPreAlertConfigQueryService paQry = (IPreAlertConfigQueryService) NCLocator.getInstance().lookup(IPreAlertConfigQueryService.class.getName());

		return paQry.queryRegistriesByClause(whereSql, null);
	}

	public static AlertregistryVO[] getRegistryAggVOsWithGlobal(String groupid, String pk_alerttype) throws BusinessException {
		StringBuffer whereSb = new StringBuffer();

		whereSb.append("(groupid='");
		whereSb.append(groupid);
		whereSb.append("' or groupid='");
		whereSb.append("GLOBLE00000000000000");
		whereSb.append("')");
		// 20170106 tsy ֻ����δɾ�������ݣ�ͬʱ���޸����ݿ���5���쳣���ݵ�drֵΪ1
		whereSb.append(" and dr=0 ");
		// 20170106 end

		if (!StringUtil.isEmptyWithTrim(pk_alerttype)) {
			whereSb.append(" and pk_alerttype='");
			whereSb.append(pk_alerttype);
			whereSb.append("'");
		}
		// 20170106 tsy ��Ӱ��Ԥ���ڵ㣬ȡ�������޸�
		// 20170105 tsy Ĭ�ϲ��Һ�̨���� 
		// registrytype=0��Ԥ������
		// registrytype=1����̨����
		// registrytype=2����ʱ����
		// else{
		// whereSb.append(" and registrytype=2 ");
		// }
		// 20170105 end
		// 20170106 end

		IPreAlertConfigQueryService qry = (IPreAlertConfigQueryService) NCLocator.getInstance().lookup(IPreAlertConfigQueryService.class);
		return qry.queryRegistriesByClause(whereSb.toString(), null);
	}

	public static AlertregistryVO[] getRegistryAggVOsByGroup(String groupid, String alertTypePK) throws BusinessException {
		String whereSql = null;
		if ("0001".equals(groupid)) {
			whereSql = "groupid='" + groupid + "' or groupid='' or groupid ='~'";
		} else
			whereSql = "groupid='" + groupid + "'";
		if (!StringUtil.isEmptyWithTrim(alertTypePK)) {
			whereSql = whereSql + " and pk_alerttype='" + alertTypePK + "'";
		}
		IPreAlertConfigQueryService paQry = (IPreAlertConfigQueryService) NCLocator.getInstance().lookup(IPreAlertConfigQueryService.class.getName());

		return paQry.queryRegistriesByClause(whereSql, null);
	}

	public static void fillCreatorOfRegistry(AlertregistryVO registry) {
		registry.setCreator(InvocationInfoProxy.getInstance().getUserId());
	}
}