package nc.vo.arap.ntb;

import java.io.Serializable;
import java.util.ArrayList;

import nc.bs.arap.util.IArapBillTypeCons;
import nc.itf.tb.control.IAccessableOrgsBusiVO;
import nc.pubitf.bd.accessor.GeneralAccessorFactory;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.vo.arap.basebill.BaseBillVO;
import nc.vo.arap.basebill.BaseItemVO;
import nc.vo.arap.pub.ArapConstant;
import nc.vo.bd.accessor.IBDData;

import nc.vo.pub.lang.UFDouble;

public class ArapAccessableBusiVO implements IAccessableOrgsBusiVO, Serializable {
	private static final long serialVersionUID = 1L;
	private BaseBillVO billVO = null;
	private BaseItemVO itemVO = null;
	private String dataType = null;
	private boolean isAdd = true;

	public boolean isAdd() {
		return isAdd;
	}

	public void setAdd(boolean isAdd) {
		this.isAdd = isAdd;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public ArapAccessableBusiVO(BaseBillVO billVO, BaseItemVO itemVO) {
		setBillVO(billVO);
		setItemVO(itemVO);
	}

	public BaseBillVO getBillVO() {
		return billVO;
	}

	public void setBillVO(BaseBillVO billVO) {
		this.billVO = billVO;
	}

	public BaseItemVO getItemVO() {
		return itemVO;
	}

	public void setItemVO(BaseItemVO itemVO) {
		this.itemVO = itemVO;
	}

	public String[] getAllUpLevels(String fieldname, String pk) throws Exception {
		// 自定义向，不能通过fieldname 取到mdId，取到的accessor也是null
		String mdId = getMdIdByFieldName(fieldname);
		IGeneralAccessor accessor = GeneralAccessorFactory.getAccessor(mdId);
		if (accessor == null) {
			return new String[] { pk };
		}
		IBDData[] bdDatas = (IBDData[]) accessor.getFatherDocs("", pk, true).toArray();
		if (bdDatas == null) {
			return new String[] { pk };
		} else {
			String[] pks = new String[bdDatas.length];
			for (int i = 0; i < pks.length; i++) {
				pks[i] = bdDatas[i].getPk();
			}
			return pks;
		}
	}

	private String getMdIdByFieldName(String fieldname) {
		String mdId = null;
		if (ArapConstant.ARAP_ORG.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_ORG;
		} else if (ArapConstant.ARAP_DEPT.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_DEPT;
		} else if (ArapConstant.ARAP_ARAPBILLTYPE.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_ARAPBILLTYPE;
		} else if (ArapConstant.ARAP_CUSTOMER.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_CUSTOMER;
		} else if (ArapConstant.ARAP_SUPPLIER.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_SUPPLIER;
		} else if (ArapConstant.ARAP_PK_SUBJCODE.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_PKSUBJCODE;
		} else if (ArapConstant.ARAP_PSNDOC.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_PSNDOC;
		} else if (ArapConstant.ARAP_BANKROLLPROJET.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_BANKROLLPROJET;
		} else if (ArapConstant.ARAP_SUBJCODE.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_SUBJCODE;
		} else if (ArapConstant.ARAP_MATERIAL.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_MATERIAL;
		} else if (ArapConstant.ARAP_PRODUCTLINE.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_PRODUCTLINE;
		} else if (ArapConstant.ARAP_COSTCENTER.equals(fieldname)) {
			mdId = ArapConstant.ARAP_MDID_COSTCENTER;
		}
		return mdId;
	}

	public String[] getAttributesValue(String[] arg0) {
		ArrayList<String> list = new ArrayList<String>();
		if (arg0 != null) {
			for (int n = 0; n < arg0.length; n++) {
				list.add(getAttributesValue(arg0[n]));
			}
		}
		return list.toArray(new String[0]);
	}

	public String getAttributesValue(String attr) {
		String col = attr.substring(7);
		Object value = null;
		int i = attr.indexOf("arap_h_");
		if (i >= 0) {
			value = getBillVO().getAttributeValue(col);
		} else {
			value = getItemVO().getAttributeValue(col);
		}
		return value == null ? null : value.toString();
	}

	public String getBillType() {
		return getBillVO().getPk_tradetype();
	}

	public String getBusiDate() {
		return billVO.getBilldate().toStdString();
	}

	public String getBusiSys() {
		return ArapConstant.SYS_ID;
	}

	public String getBusiType() {
		return null;
	}

	/**
	 * 控制的时候不需要
	 */
	public String getCurrency() {
		return billVO.getPk_currtype();
	}

	public String getPKGroup() {
		return getBillVO().getPk_group();
	}

	public String getPKOrg() {
		return getBillVO().getPk_org();
	}

	public String getPkNcEntity() {
		return null;
	}

	public UFDouble[] getExeData(String direction, String obj, String extObj) {
		UFDouble zero = UFDouble.ZERO_DBL;
		BaseItemVO billItemVO = getItemVO();
		UFDouble money_de = billItemVO.getMoney_de() == null ? zero : billItemVO.getMoney_de();
		UFDouble local_money_de = billItemVO.getLocal_money_de() == null ? zero : billItemVO.getLocal_money_de();
		UFDouble groupdebit = billItemVO.getGroupdebit() == null ? zero : billItemVO.getGroupdebit();
		UFDouble globaldebt = billItemVO.getGlobaldebit() == null ? zero : billItemVO.getGlobaldebit();
		UFDouble money_cr = billItemVO.getMoney_cr() == null ? zero : billItemVO.getMoney_cr();
		UFDouble local_money_cr = billItemVO.getLocal_money_cr() == null ? zero : billItemVO.getLocal_money_cr();
		UFDouble groupcrebit = billItemVO.getGroupcrebit() == null ? zero : billItemVO.getGroupcrebit();
		UFDouble globalcrebit = billItemVO.getGlobalcrebit() == null ? zero : billItemVO.getGlobalcrebit();
		//20170711 tsy 获取不含税金额
		UFDouble globalnotax_cre = billItemVO.getGlobalnotax_cre();//对应globalcrebit 全局本币无税金额(贷方) 
//		UFDouble globalnotax_de = billItemVO.getGlobalnotax_de();//对应globaldebt
		UFDouble groupnotax_cre = billItemVO.getGroupnotax_cre();//对应groupcrebit 集团本币无税金额(贷方)
//		UFDouble groupnotax_de = billItemVO.getGroupnotax_de();//对应groupdebit
		UFDouble local_notax_cr = billItemVO.getLocal_notax_cr();//对应local_money_cr
//		UFDouble local_notax_de = billItemVO.getLocal_notax_de();//对应local_money_de
		UFDouble notax_cr = billItemVO.getNotax_cr();//对应money_cr
//		UFDouble notax_de = billItemVO.getNotax_de();//对应money_de
		//20170711 end
		if (isAdd()) {
			if (IArapBillTypeCons.F2.equals(billVO.getPk_billtype()) || IArapBillTypeCons.F1.equals(billVO.getPk_billtype())) {
				//20170711 tsy 
				if ("notax".equals(obj)) {
					return new UFDouble[] { globalnotax_cre, groupnotax_cre, local_notax_cr, notax_cr };
				}
				//20170711 end
				return new UFDouble[] { globalcrebit, groupcrebit, local_money_cr, money_cr };
			} else {
				return new UFDouble[] { globaldebt, groupdebit, local_money_de, money_de };
			}
		} else {
			if (IArapBillTypeCons.F2.equals(billVO.getPk_billtype()) || IArapBillTypeCons.F1.equals(billVO.getPk_billtype())) {
				//20170711 tsy 
				if ("notax".equals(obj)) {
					return new UFDouble[] { zero.sub(globalnotax_cre), zero.sub(groupnotax_cre), zero.sub(local_notax_cr), zero.sub(notax_cr) };
				}
				//20170711 end
				return new UFDouble[] { zero.sub(globalcrebit), zero.sub(groupcrebit), zero.sub(local_money_cr), zero.sub(money_cr) };
			} else {
				return new UFDouble[] { zero.sub(globaldebt), zero.sub(groupdebit), zero.sub(local_money_de), zero.sub(money_de) };
			}
		}
	}

	/**
	 * 不用实现
	 */
	public boolean isUnInure() {
		return false;
	}

	/**
	 * 暂不用实现
	 */
	public String[] getHasLevelFlds() {
		return null;
	}

	public String getDataType() {
		return this.dataType;
	}

	public String getDateType() {
		return null;
	}

	@Override
	public String getPKOrg(String orgAtt) {
		if (ArapConstant.ARAP_PK_PCORG.equals(orgAtt)) { // 利润中心
			return getItemVO().getPk_pcorg();
		} else if (ArapConstant.ARAP_PK_PROJECT.equals(orgAtt)) { // 项目
			return getItemVO().getProject();
		}
		return getPKOrg();
	}
}
