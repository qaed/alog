package nc.bs.er.exp.cmd.edit;

import java.util.List;

import nc.bs.er.exp.util.ExpUtil;
import nc.bs.framework.common.NCLocator;
import nc.itf.org.IOrgVersionQryService;
import nc.uap.lfw.core.exception.LfwBusinessException;
import nc.vo.ep.bx.BXHeaderVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.org.OrgQueryUtil;
import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.vorg.OrgVersionVO;

public class Edit4PayOrgV extends ExpBillEditAction {
	public void editListener() throws LfwBusinessException {
		String pk_org_v = currentRow.getString(headDs.nameToIndex(BXHeaderVO.PK_PAYORG_V));
		String pk_org = currentRow.getString(headDs.nameToIndex(BXHeaderVO.PK_PAYORG));
		afterEditMultiVersionOrgField(JKBXHeaderVO.PK_PAYORG_V, pk_org_v, JKBXHeaderVO.getOrgFieldByVField(JKBXHeaderVO.PK_PAYORG_V));

		List<String> orgItem = getOrgRefFields(BXHeaderVO.PK_PAYORG);
		for (String item : orgItem) {
			boolean isReference = ExpUtil.isReferenceField(item, getCurrentWidget(), headDs);
			if (isReference) {
				// 带出付款银行帐号
				if ("fkyhzh".equals(item)) {
					String fkyhzh;
					try {
						fkyhzh = getDefaultPayAcc(pk_org_v);
						ExpUtil.setRowValue(headDs.getSelectedRow(), headDs, item, fkyhzh);
					} catch (BusinessException e) {
						e.printStackTrace();
					}
				} else if ("jsfs".equals(item)) {//结算方式不变
					// 测试-银企直连：1002A11000000000CY5H
					// 正式-银企直连：1001A4100000000F5WOM
					// 测试、正式-网银：0001Z0100000000000Y2
//					ExpUtil.setRowValue(headDs.getSelectedRow(), headDs, item, "0001Z0100000000000Y2");
					continue;
				} else {
					ExpUtil.setRowValue(headDs.getSelectedRow(), headDs, item, null);
				}

			}
		}
	}

	/**
	 * 获取默认支付帐号
	 */
	private String getDefaultPayAcc(String pk_org_v) throws BusinessException {
		IOrgVersionQryService orgqry = NCLocator.getInstance().lookup(IOrgVersionQryService.class);
		OrgVersionVO orgVvo = orgqry.getOrgVersionVOByVID(pk_org_v);
		return orgVvo == null ? null : orgVvo.getDef6();
	}
}