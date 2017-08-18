package nc.ui.gl.voucher.opmodels;

import javax.swing.JComponent;

import nc.vo.gl.pubvoucher.*;
import nc.ui.gl.vouchermodels.*;
import nc.ui.gl.vouchertools.VoucherDataCenter;
import nc.ui.pub.beans.MessageDialog;

/**
 * 粘贴分录
 */
public class PasteDetailOperationModel extends AbstractOperationModel {
	public Object doOperation() {
		Boolean isInSum = (Boolean) getMasterModel().getParameter("isInSumMode");
		if (isInSum != null && isInSum.booleanValue()) {
			return null;
		}
		getMasterModel().setParameter("stopediting", null);
		VoucherVO voucher = (VoucherVO) getMasterModel().getParameter("vouchervo");
		if (voucher.getPk_accountingbook() == null) {
			nc.vo.fipub.utils.uif2.FiUif2MsgUtil.showUif2DetailMessage((JComponent) getMasterModel().getUI(), nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0102")/*
																																																 * @
																																																 * res
																																																 * "提示"
																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0193")/*
																																																																					 * @
																																																																					 * res
																																																																					 * "请选择核算账簿。"
																																																																					 */);
			return null;
		}
		int[] indexes = (int[]) getMasterModel().getParameter("selectedindexes");
		if (indexes == null || indexes.length == 0) {
			return null;
		}
		if (voucher == null || voucher.getDiscardflag().booleanValue() || voucher.getPk_casher() != null || voucher.getPk_checked() != null || voucher.getPk_manager() != null || voucher.getDetailmodflag() == null || !voucher.getDetailmodflag().booleanValue())
			return null;
		DetailVO[] details = VoucherDataCenter.getInstance().getCopiedDetails();
		if (details == null || details.length == 0)
			return null;
		if (!voucher.getPk_accountingbook().equals(details[0].getPk_glorgbook())) {
			int r = nc.ui.pub.beans.MessageDialog.showYesNoDlg((java.awt.Container) getMasterModel().getUI(), nc.ui.ml.NCLangRes.getInstance().getStrByID("20021005", "UPP20021005-000048")/*
																																															 * @
																																															 * res
																																															 * "警告"
																																															 */, nc.ui.ml.NCLangRes.getInstance().getStrByID("20021005", "UPP20021005-000095")/*
																																																																			 * @
																																																																			 * res
																																																																			 * "要复制的分录与当前凭证的所属公司不同，有可能发生数据错误，您确信要继续吗？\n注意：确定的话所有被复制分录的辅助核算信息将被清除。"
																																																																			 */);
			if (r == nc.ui.pub.beans.MessageDialog.ID_YES) {
				DetailVO[] tmp_detail = new DetailVO[details.length];
				for (int i = 0; i < details.length; i++) {
					tmp_detail[i] = (DetailVO) details[i].clone();
					tmp_detail[i].setPk_glorgbook(voucher.getPk_accountingbook());
					tmp_detail[i].setPk_glorg(voucher.getPk_org());
					tmp_detail[i].setPk_glbook(voucher.getPk_setofbook());
					// 60x tmp_detail[i].setPk_corp(voucher.getPk_corp());
					tmp_detail[i].setAss(null);
					tmp_detail[i].setAssid(null);
					tmp_detail[i].setCheckstyle(null);
					tmp_detail[i].setBilltype(null);
					tmp_detail[i].setUserData(null);
					tmp_detail[i].setOtheruserdata(null);
					tmp_detail[i].setPk_org(voucher.getPk_org());
					tmp_detail[i].setPk_org_v(voucher.getPk_org_v());
					tmp_detail[i].setPk_unit(voucher.getPk_org());
					tmp_detail[i].setPk_unit_v(voucher.getPk_org_v());
					// tmp_detail[i].setSubjfreevalue(null);
					nc.vo.bd.account.AccountVO oldacc = VoucherDataCenter.getAccsubjByPK(voucher.getPk_accountingbook(), details[i].getPk_accasoa(), voucher.getPrepareddate().toStdString());
					if (oldacc == null)
						tmp_detail[i].setPk_accasoa(null);
					else {
						nc.vo.bd.account.AccountVO newacc = VoucherDataCenter.getAccsubjByCode(voucher.getPk_accountingbook(), oldacc.getCode(), voucher.getPrepareddate().toStdString());
						if (newacc == null)
							tmp_detail[i].setPk_accasoa(null);
						else
							tmp_detail[i].setPk_accasoa(newacc.getPk_accasoa());
					}
				}
				details = tmp_detail;
			} else {
				return null;
			}
		}
		DetailVO[] tmp_details = new DetailVO[voucher.getNumDetails() + details.length];
		for (int i = 0; i < indexes[0]; i++) {
			if (voucher.getDetail(i) == null) {
				nc.vo.fipub.utils.uif2.FiUif2MsgUtil.showUif2DetailMessage((JComponent) getMasterModel().getUI(), nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0102")/*
																																																	 * @
																																																	 * res
																																																	 * "提示"
																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0295")/*
																																																																						 * @
																																																																						 * res
																																																																						 * "请选择表体行"
																																																																						 */);
				return null;
			}
			tmp_details[i] = (DetailVO) voucher.getDetail(i).clone();
		}
		for (int i = 0; i < details.length; i++) {
			tmp_details[indexes[0] + i] = (DetailVO) details[i].clone();
		}
		for (int i = indexes[0]; i < voucher.getNumDetails(); i++) {
			tmp_details[details.length + i] = (DetailVO) voucher.getDetail(i).clone();
		}
		for (int i = 0; i < tmp_details.length; i++) {
			if (tmp_details[i] != null && tmp_details[i].getDetailindex() != null)
				tmp_details[i].setDetailindex(new Integer(i + 1));
		}

		getMasterModel().setParameter("details", tmp_details);
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = indexes[i] + details.length;
		}
		getMasterModel().setParameter("selectedindexes", indexes);
		return null;
	}
}