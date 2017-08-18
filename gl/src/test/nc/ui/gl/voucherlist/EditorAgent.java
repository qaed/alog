package nc.ui.gl.voucherlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;

import nc.bs.framework.common.NCLocator;
import nc.bs.glcom.ass.assitem.cache.AccAssItemCache;
import nc.bs.logging.Logger;
import nc.bs.uap.sf.facility.SFServiceFacility;
import nc.desktop.ui.WorkbenchEnvironment;
import nc.gl.account.glconst.CurrTypeConst;
import nc.itf.bd.pub.IBDMetaDataIDConst;
import nc.itf.bd.taxcode.ITaxcodeQueryService;
import nc.itf.gl.pub.GLStartCheckUtil;
import nc.itf.glcom.para.GLParaAccessor;
import nc.itf.trade.excelimport.ExportDataInfo;
import nc.itf.trade.excelimport.IImportableEditor;
import nc.itf.trade.excelimport.ImportableInfo;
import nc.pubitf.accperiod.AccountCalendar;
import nc.pubitf.bd.accessor.GeneralAccessorFactory;
import nc.pubitf.bd.accessor.IGeneralAccessor;
import nc.ui.gl.datacache.AccountCache;
import nc.ui.gl.datacache.BusiUnitDataCache;
import nc.ui.gl.datacache.FreeValueDataCache;
import nc.ui.gl.eventprocess.VoucherPowerCheckUtil;
import nc.ui.gl.gateway.glworkbench.GlWorkBench;
import nc.ui.gl.voucherdata.VoucherDataBridge;
import nc.ui.trade.excelimport.ExcelImportInfo;
import nc.ui.trade.excelimport.IDetailLogger;
import nc.ui.trade.excelimport.InputItem;
import nc.vo.bd.accassitem.AccAssItemVO;
import nc.vo.bd.accessor.IBDData;
import nc.vo.bd.accessor.bankaccsub.BankaccSubGeneralAccessor;
import nc.vo.bd.account.AccAssVO;
import nc.vo.bd.account.AccountVO;
import nc.vo.bd.taxcode.TaxcodeVO;
import nc.vo.fipub.utils.StrTools;
import nc.vo.gateway60.accountbook.AccountBookUtil;
import nc.vo.gateway60.accountbook.GlOrgUtils;
import nc.vo.gateway60.itfs.CalendarUtilGL;
import nc.vo.gl.cashflowcase.CashflowcaseVO;
import nc.vo.gl.pubvoucher.DetailVO;
import nc.vo.gl.pubvoucher.VoucherExportForExcelVO;
import nc.vo.gl.pubvoucher.VoucherExportMainTableVO;
import nc.vo.gl.pubvoucher.VoucherExportSubVO;
import nc.vo.gl.pubvoucher.VoucherVO;
import nc.vo.gl.vatdetail.DirectionEnum;
import nc.vo.gl.vatdetail.VatDetailVO;
import nc.vo.gl.vatdetail.VoucherkindEnum;
import nc.vo.glcom.ass.AssVO;
import nc.vo.glcom.constant.GLStringConst;
import nc.vo.glcom.tools.ProductInstallCheckTool;
import nc.vo.pub.BeanHelper;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.ExtendedAggregatedValueObject;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.sm.UserVO;

import org.apache.commons.lang.StringUtils;

public class EditorAgent implements IImportableEditor {

	private ExcelImportInfo importInfo = null;
	private HashMap<String, VoucherVO> voucherMap = new HashMap<String, VoucherVO>();
	private int currentIndex = 0;
	private JComponent parent;

	private BankaccSubGeneralAccessor bankAccountAccessor;

	private IGeneralAccessor checkStyleAccessor;

	private IGeneralAccessor billTypeAccessor;

	private Exception setValueEx = null;

	/**
	 * ��ʾ��ǰsave�����Ƿ���Դӻ�����ȡֵ �൱����
	 */
	private boolean canSave = false;

	public EditorAgent() {
		super();
	}

	public EditorAgent(JComponent parent, ExcelImportInfo importInfo) {
		super();
		this.importInfo = importInfo;
		this.parent = parent;
	}

	@Override
	public void addNew() {
	}

	@Override
	public void cancel() {
	}

	/**
	 * ���ؿ� ��ʾ��������ݺϷ�
	 */
	@Override
	public ImportableInfo getImportableInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * ������Ҫ������������Ŀ����
	 */
	@Override
	public List<InputItem> getInputItems() {
		/**
		 * ������ д��Ҫ������Щ��Ŀ
		 */
		List<InputItem> list = new ArrayList<InputItem>();
		list.addAll(this.getVoucherAttributes());
		list.addAll(this.getVoucherDetailVOAttributes());
		list.addAll(this.getVoucherAssVOAttributes());
		list.addAll(this.getVoucherCashFlowVOAttributes());
		return list;
	}

	/**
	 * ��Ϊû��ʹ��UI����2 ���Ҫ��������о������ķ�װ
	 * 
	 * @param exportItems
	 *            Ҫ��������Ŀ�� ��������excel�еĴ��� ���� ���� λ�õ�
	 */
	@Override
	public ExportDataInfo getValue(List<InputItem> exportItems) {
		/**
		 * �ڲ�Ҫ����exportItems���� ����ExportDataInfo ��detail����Ϊ���� ���ֽ���������Ϊ�ֱ� ��������
		 * �͸������㶼ƴ����detail��
		 */
		nc.ui.gl.pubvoucher.ListModel listmodel = ((nc.ui.gl.voucherlist.ListView) this.getJComponent()).getListModel();
		List<String> voucherpkList = new ArrayList<String>();
		int selectIndex[] = listmodel.getCurrentIndexs();
		for (int i = 0; i < selectIndex.length; i++) {
			try {
				voucherpkList.add(listmodel.getVoucherVO(selectIndex[i]).getPk_voucher());
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}
		// �õ���ѡ�е�ƾ֤
		VoucherVO vouchers[] = getVoucherVOs(voucherpkList.toArray(new String[0]));
		// ���ÿ��ƾ֤�ķ�¼�������������
		DetailVO detail[] = null;
		VoucherExportMainTableVO main = null;
		List<VoucherExportForExcelVO> exportMainVOs = new ArrayList<VoucherExportForExcelVO>();
		for (int i = 0; i < vouchers.length; i++) {
			detail = vouchers[i].getDetails();
			for (int j = 0; j < detail.length; j++) {
				VoucherExportSubVO cashFlow[] = null;
				// ����һ��������vo
				main = new VoucherExportMainTableVO(vouchers[i], detail[j], detail[j].getAss());
				main.setAttributeValue("unitname", BusiUnitDataCache.getOrgByPk(detail[j].getPk_unit()).getCode());
				// Ȼ�󴴽��ֽ�����vo
				CashflowcaseVO cashFlowVOs[] = detail[j].getCashFlow();
				if (cashFlowVOs != null) {
					cashFlow = new VoucherExportSubVO[cashFlowVOs.length];
					for (int k = 0; k < cashFlow.length; k++) {
						cashFlow[k] = new VoucherExportSubVO(cashFlowVOs[k]);
					}
				}
				exportMainVOs.add(new VoucherExportForExcelVO(main, cashFlow));
			}
		}
		ExportDataInfo exportDataInfo = new ExportDataInfo();
		exportDataInfo.setExportDatas(exportMainVOs.toArray(new VoucherExportForExcelVO[0]));
		return exportDataInfo;
	}

	@Override
	public void save() throws Exception {
		Object value = getValue();
		if (value != null && value instanceof VoucherVO) {
			VoucherPowerCheckUtil.checkVoucherPower((VoucherVO) value);

			VoucherImporterCheckUtil checkUtil = new VoucherImporterCheckUtil();
			// У����֯
			checkUtil.checkUnit((VoucherVO) value);

			// У�������Ƿ����
			checkUtil.checkAmountEqual((VoucherVO) value);

			// ������
			checkUtil.formatNumberDetail((VoucherVO) value);
			VoucherDataBridge.getInstance().save((VoucherVO) value, new Boolean(true));
		}
	}

	private Object obj;

	@Override
	public void setValue(Object obj) {
		this.obj = obj;
	}

	public Object getValue() {
		return this.obj;
	}

	public DetailVO convertToDetailVo(VoucherVO mainVoucherVo, Object obj) throws Exception {

		StringBuffer errorMessage = new StringBuffer();

		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1������ƾ֤����ͷ�¼�Ĺ�����ϵ�ֶ� ���� �Ƿ��Ѿ������˶�Ӧ��vouchervo 2����������ˡ���ô
		 * �Ͱɵ�ǰ�ķ�¼��׷�ӵ���vouchervo�� 3�����û�����ɣ���ô���ݵ�ǰdetail��¼vo��ʼ����vouchervo
		 */
		String voucherKey = "";

		/**
		 * ���ȹ��� voucher ��key
		 */
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				voucherKey += detail.getAttributeValue(detailRowCodes[i]);
			}
		}

		String accountbookpk = VoucherExcelImporter.getAccountintBookPKWithCode(detail.getAttributeValue("main_m_pk_accountingbook").toString());

		/**
		 * ���쵱ǰ��detail
		 */
		DetailVO detailVo = new DetailVO();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				// ��ʾ��vouchervo����Ϣ
			} else if (detailRowCodes[i].startsWith("ass_")) {
				// ��ʾ�Ǹ����������Ϣ
			} else if (detailRowCodes[i].startsWith(GLStringConst.VAT)) {
				// ��ʾ˰����ϸ
			} else {
				// ��ʾ��������detail���е�����
				BeanHelper.setProperty(detailVo, detailRowCodes[i], VoucherAttributeConverte.getConvertData(detail.getAttributeValue(detailRowCodes[i]).toString(), detailRowCodes[i], VoucherAttributeConverte.getDetailVoucherClass()));
			}
		}

		try {
			detailVo.setPk_accasoa(AccountCache.getInstance().getAccountVOByCode(accountbookpk, detail.getAttributeValue("m_accsubjcode").toString()).getPk_accasoa());
		} catch (Exception e) {
			Object attributeValue = detail.getAttributeValue("m_accsubjcode");
			String code = attributeValue == null ? "" : attributeValue.toString();
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0278", null, new String[] { code })/*
																																			 * @
																																			 * res
																																			 * "��Ŀ����{0}�޷����룡"
																																			 */+ IDetailLogger.N);
		}

		try {
			String CurrtypePK = VoucherExportImportConvert.getCurrtypePKByName(detail.getAttributeValue("m_pk_currtype").toString());
			detailVo.setPk_currtype(CurrtypePK);
		} catch (Exception e) {
			Object attributeValue = detail.getAttributeValue("m_pk_currtype");
			String code = attributeValue == null ? "" : attributeValue.toString();
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0279", null, new String[] { code })/*
																																			 * @
																																			 * res
																																			 * "����{0}�޷����룡"
																																			 */+ IDetailLogger.N);
		}

		String pk_unit = VoucherExportImportConvert.getUnitPkByCode(mainVoucherVo.getPk_accountingbook(), detail.getAttributeValue("unitname").toString());
		if (pk_unit == null || pk_unit.trim().equals("")) {
			pk_unit = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());
		}

		// ��������˶������㵥λ
		UFBoolean secondBUStart = GLParaAccessor.isSecondBUStart(mainVoucherVo.getPk_accountingbook());
		if (secondBUStart == null || !secondBUStart.booleanValue() || pk_unit == null || pk_unit.trim().equals("")) {
			pk_unit = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());
		}

		detailVo.setPk_unit(pk_unit);
		HashMap<String, String> versionMap = null;
		try {
			versionMap = GlOrgUtils.getNewVIDSByOrgIDSAndDate(new String[] { pk_unit }, mainVoucherVo.getPrepareddate());
		} catch (BusinessException e) {
			Logger.error(e);
		}
		if (versionMap != null) {
			detailVo.setPk_unit_v(versionMap.get(pk_unit));
		} else {
			detailVo.setPk_unit_v(pk_unit);
		}
		detailVo.setPrepareddate(mainVoucherVo.getPrepareddate());
		detailVo.setExplanation(detail.getAttributeValue("m_explanation").toString());//
		detailVo.setDebitamount(new UFDouble(detail.getAttributeValue("m_debitamount").toString()));
		detailVo.setCreditamount(new UFDouble(detail.getAttributeValue("m_creditamount").toString()));
		detailVo.setLocalcreditamount(new UFDouble(detail.getAttributeValue("m_localcreditamount").toString()));
		detailVo.setLocaldebitamount(new UFDouble(detail.getAttributeValue("m_localdebitamount").toString()));
		detailVo.setPk_accountingbook(mainVoucherVo.getPk_accountingbook());
		detailVo.setGroupdebitamount(new UFDouble(detail.getAttributeValue("m_groupdebitamount").toString()));
		detailVo.setGroupcreditamount(new UFDouble(detail.getAttributeValue("m_groupcreditamount").toString()));
		detailVo.setGlobalcreditamount(new UFDouble(detail.getAttributeValue("m_globalcreditamount").toString()));
		detailVo.setGlobaldebitamount(new UFDouble(detail.getAttributeValue("m_globaldebitamount").toString()));

		detailVo.setExcrate2(new UFDouble(detail.getAttributeValue("m_excrate2").toString()));
		detailVo.setExcrate3(new UFDouble(detail.getAttributeValue("excrate3").toString()));
		detailVo.setExcrate4(new UFDouble(detail.getAttributeValue("excrate4").toString()));

		detailVo.setPk_group(GlWorkBench.getDefaultGroup());

		try {
			// hurh ��������
			detailVo.setDebitquantity(new UFDouble(detail.getAttributeValue("m_debitquantity").toString()));
			detailVo.setCreditquantity(new UFDouble(detail.getAttributeValue("m_creditquantity").toString()));
		} catch (Exception e) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0280")/*
																												 * @
																												 * res
																												 * "������ʽ����ȷ��"
																												 */+ IDetailLogger.N);
		}

		try {
			detailVo.setPrice(new UFDouble(detail.getAttributeValue("m_price").toString()));
		} catch (Exception e) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0281")/*
																												 * @
																												 * res
																												 * "���۸�ʽ����ȷ��"
																												 */+ IDetailLogger.N);
		}

		// hurh������Ϣ
		Object assInfo = detail.getAttributeValue(DetailVO.BANKACCOUNT);
		if (assInfo != null && !StringUtils.isEmpty(assInfo.toString())) {
			try {
				detailVo.setBankaccount(getBankAccountAccessor().getDocByAccnum(assInfo.toString(), detailVo.getPk_currtype()).getPk());
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
				// errorMessage.append(e.getMessage());
			}
		}
		assInfo = detail.getAttributeValue(DetailVO.CHECKSTYLE);
		if (assInfo != null && !StringUtils.isEmpty(assInfo.toString())) {
			IBDData docByCode = getCheckStyleAccessor().getDocByCode(detailVo.getPk_unit(), assInfo.toString());
			if (docByCode == null) {
				errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0290", null, new String[] { assInfo.toString() })/*
																																								 * @
																																								 * res
																																								 * "�Ҳ�����Ӧ{0}����Ľ��㷽ʽ��"
																																								 */+ IDetailLogger.N);
			} else {
				detailVo.setCheckstyle(docByCode.getPk());
			}
		}
		assInfo = detail.getAttributeValue(DetailVO.BILLTYPE);
		if (assInfo != null && !StringUtils.isEmpty(assInfo.toString())) {
			IBDData docByCode = getBillTypeAccessor().getDocByCode(detailVo.getPk_unit(), assInfo.toString());
			if (docByCode == null) {
				errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0291", null, new String[] { assInfo.toString() })/*
																																								 * @
																																								 * res
																																								 * "�Ҳ�����Ӧ{0}����Ʊ�����ͣ�"
																																								 */+ IDetailLogger.N);
			} else {
				detailVo.setBilltype(docByCode.getPk());
			}
		}
		assInfo = detail.getAttributeValue("m_checkno");
		if (assInfo != null && !StringUtils.isEmpty(assInfo.toString())) {
			detailVo.setCheckno(assInfo.toString());
		}
		assInfo = detail.getAttributeValue("m_checkdate");
		if (assInfo != null && !StringUtils.isEmpty(assInfo.toString())) {
			detailVo.setCheckdate(new UFDate(assInfo.toString()));
		}

		Object verifyDateObj = detail.getAttributeValue("verifydate");
		if (verifyDateObj != null && !StringUtils.isEmpty(verifyDateObj.toString())) {
			detailVo.setVerifydate(new UFDate(verifyDateObj.toString()).toString());
		}

		// �Ƿ�����ŷ�˱������δ������vat��Ϣ���
		boolean isEurUse = GLStartCheckUtil.checkEURStart(mainVoucherVo.getPk_group());

		// ˰����ϸ
		if (isEurUse) {
			VatDetailVO vatdetail = new VatDetailVO();
			vatdetail.setPk_accountingbook(detailVo.getPk_accountingbook());
			vatdetail.setPrepareddate(detailVo.getPrepareddate());
			vatdetail.setPk_group(detailVo.getPk_group());
			vatdetail.setPk_org(detailVo.getPk_org());
			vatdetail.setPk_unit(detailVo.getPk_unit());
			vatdetail.setPk_accasoa(detailVo.getPk_accasoa());
			vatdetail.setVoucherno(detailVo.getNo());
			vatdetail.setDetailindex(detailVo.getDetailindex());
			vatdetail.setVoucherkind(VoucherkindEnum.VOUCHER.value().toString());
			// ��˰����
			Object vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_VATCOUNTRY);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setPk_vatcountry(GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.COUNTRYZONE).getDocByCode(detailVo.getPk_unit(), vatinfo.toString()).getPk());
			}
			// �ջ���
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_RECEIVECOUNTRY);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setPk_receivecountry(GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.COUNTRYZONE).getDocByCode(detailVo.getPk_unit(), vatinfo.toString()).getPk());
			}
			// ���״���
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.BUSINESSCODE);
			vatdetail.setBusinesscode(vatinfo == null ? null : vatinfo.toString());
			// �ͻ�VATע����
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_CLIENTVATCODE);
			vatdetail.setPk_clientvatcode(vatinfo == null ? null : vatinfo.toString());
			// ��Ӧ��VATע����
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_SUPPLIERVATCODE);
			vatdetail.setPk_suppliervatcode(vatinfo == null ? null : vatinfo.toString());
			// ˰��
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_TAXCODE);

			if (vatinfo != null && !"".equals(vatinfo)) {
				TaxcodeVO[] taxCodeVOs = null;
				try {
					taxCodeVOs = NCLocator.getInstance().lookup(ITaxcodeQueryService.class).getTaxcodeVOsByCond(TaxcodeVO.CODE + "='" + vatinfo + "' and " + TaxcodeVO.REPTAXCOUNTRY + "='" + vatdetail.getPk_vatcountry() + "'");
				} catch (BusinessException e) {
					Logger.error(e.getMessage(), e);
					// errorMessage.append(e.getMessage());
				}
				if (taxCodeVOs != null && taxCodeVOs.length > 0) {
					String pk_taxcode = taxCodeVOs[0].getPk_taxcode();
					vatdetail.setPk_taxcode(pk_taxcode);
				}
			}

			// ����
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.DIRECTION);
			if (DirectionEnum.DEBIT.getName().equals(vatinfo)) {
				vatdetail.setDirection(DirectionEnum.DEBIT.value().toString());
			} else {
				vatdetail.setDirection(DirectionEnum.CREDIT.value().toString());
			}
			// ��˰���
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.MONEYAMOUNT);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setMoneyamount(new UFDouble(vatinfo.toString()));
			}
			// ˰��
			if (detailVo.getLocaldebitamount() == null || UFDouble.ZERO_DBL.equals(detailVo.getLocaldebitamount())) {
				vatdetail.setTaxamount(detailVo.getLocalcreditamount());
			} else {
				vatdetail.setTaxamount(detailVo.getLocaldebitamount());
			}

			boolean nullFlag = false;

			if ((vatdetail.getTaxamount() == null || UFDouble.ZERO_DBL.equals(vatdetail.getTaxamount())) && (vatdetail.getMoneyamount() == null || UFDouble.ZERO_DBL.equals(vatdetail.getMoneyamount()))) {
				nullFlag = true;
			}

			if (StringUtils.isEmpty(vatdetail.getPk_taxcode()) || StringUtils.isEmpty(vatdetail.getBusinesscode())) {
				nullFlag = true;
			}

			if (!nullFlag) {
				// ���õ�detailVO��
				detailVo.setVatdetail(vatdetail);
			}
		}

		// detailVo.setNo(0);//ȥ�����ܴ��ڵ�ƾ֤��
		if (mainVoucherVo.getPk_org() != null)
			detailVo.setPk_org(mainVoucherVo.getPk_org());
		if (mainVoucherVo.getPk_org_v() != null)
			detailVo.setPk_org(mainVoucherVo.getPk_org_v());

		String pk_org = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());

		/**
		 * �����ֽ�����
		 */
		CircularlyAccessibleValueObject cashFlows[] = aggvo.getTableVO("cashflow");
		aggvo.getTableCodes();
		if (cashFlows != null) {
			CashflowcaseVO cashFlowVos[] = new CashflowcaseVO[cashFlows.length];
			for (int i = 0; i < cashFlowVos.length; i++) {
				cashFlowVos[i] = new CashflowcaseVO();
				cashFlowVos[i].setCashflowFlag(new Integer(cashFlows[i].getAttributeValue("m_flag").toString()));
				cashFlowVos[i].setCashflowcode(cashFlows[i].getAttributeValue("cashflowCode").toString());
				cashFlowVos[i].setCashflowName(cashFlows[i].getAttributeValue("cashflowName").toString());
				IGeneralAccessor accessor = GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.CASHFLOW);
				String pk_cashFlow = null;
				IBDData docByCode = accessor.getDocByCode(pk_org, cashFlowVos[i].getCashflowcode());
				if (docByCode == null) {
					errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0292", null, new String[] { cashFlowVos[i].getCashflowcode() })/*
																																												 * @
																																												 * res
																																												 * "�Ҳ�������Ϊ{0}���ֽ�������Ŀ��"
																																												 */+ "\n");
				} else {
					pk_cashFlow = docByCode.getPk();
				}
				cashFlowVos[i].setPk_cashflow(pk_cashFlow);
				cashFlowVos[i].setMoney(new UFDouble(cashFlows[i].getAttributeValue("m_money").toString()));
				cashFlowVos[i].setMoneymain(new UFDouble(cashFlows[i].getAttributeValue("m_moneymain").toString()));
				cashFlowVos[i].setMoneygroup(new UFDouble(cashFlows[i].getAttributeValue("m_moneygroup").toString()));
				cashFlowVos[i].setMoneyglobal(new UFDouble(cashFlows[i].getAttributeValue("m_moneyglobal").toString()));
				cashFlowVos[i].setPk_unit(detailVo.getPk_unit());
				cashFlowVos[i].setPk_glorgbook(detailVo.getPk_accountingbook());
				String cfcurr = VoucherExportImportConvert.getCurrtypePKByName(cashFlows[i].getAttributeValue("cashflowcurr").toString());
				cashFlowVos[i].setM_pk_currtype(cfcurr);

			}
			if (cashFlowVos.length > 0) {
				detailVo.setCashFlow(cashFlowVos);
			}
		}

		/**
		 * ������������
		 */
		List<AssVO> assVoList = new ArrayList<AssVO>();
		int assIndex = 1;
		AccountVO account = AccountCache.getInstance().getAccountVOByPK(detailVo.getPk_accountingbook(), detailVo.getPk_accasoa(), detailVo.getPrepareddate().toStdString());
		HashMap<String, AccAssVO> accassMap = new HashMap<String, AccAssVO>();
		if (account != null && account.getAccass() != null && account.getAccass().length > 0) {
			for (AccAssVO ass : account.getAccass()) {
				accassMap.put(ass.getPk_entity(), ass);
			}
		}
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("ass_")) {
				AssVO tempAss = new AssVO();
				String value = detail.getAttributeValue("ass_" + (assIndex++)).toString();
				if (value == null || value.trim().equals("")) {
					continue;
				}
				AccAssItemVO itemVO = AccAssItemCache.getAccAssItemVOByName(value.split(":")[1]);
				if (itemVO == null) {
					itemVO = AccAssItemCache.getAccAssItemVOByCode(value.split(":")[1]);
				}

				if (itemVO == null) {// ��Ƹ���������ĿΪ��
					errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0273", null, new String[] { value.split(":")[1] })/*
																																									 * @
																																									 * res
																																									 * "{0}�����Ӧ�Ļ�Ƹ���������Ŀ�����ڣ�"
																																									 */+ "\n");
				} else {//
					if (StringUtils.isEmpty(itemVO.getRefnodename())) {
						// ˵���ǻ������ͣ���ֱ�ӵ����ַ�������
						tempAss.setPk_Checkvalue(value.split(":")[0]);
					} else {
						IBDData valuedata = null;
						try {
							// hurh �����˻���������
							if (IBDMetaDataIDConst.BANKACCSUB.equals(itemVO.getClassid())) {
								valuedata = getBankAccountAccessor().getDocByCode(pk_unit, value.split(":")[0]);
							} else {
								valuedata = GeneralAccessorFactory.getAccessor(itemVO.getClassid()).getDocByCode(pk_unit, value.split(":")[0]);
							}
						} catch (Exception e) {
							Logger.error(e.getMessage(), e);
						}
						tempAss.setPk_Checktype(itemVO.getPk_accassitem());
						tempAss.setChecktypecode(itemVO.getCode());
						tempAss.setChecktypename(itemVO.getName());
						if (valuedata == null) {
							if (!(StringUtils.isEmpty(value.split(":")[0]) || StrTools.NULL.equals(value.split(":")[0]))) {
								errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0274", null, new String[] { value.split(":")[0] })/*
																																												 * @
																																												 * res
																																												 * "{0}�����Ӧ�ĵ��������ڣ�"
																																												 */+ "\n");
							}
						} else {
							tempAss.setPk_Checkvalue(valuedata.getPk());
							tempAss.setCheckvaluecode(valuedata.getCode());
							tempAss.setCheckvaluename(valuedata.getName().toString());

						}
					}
				}
				assVoList.add(tempAss);
			}
		}
		if (assVoList.size() > 0) {
			detailVo.setAss(assVoList.toArray(new AssVO[0]));
			String assID = FreeValueDataCache.getInstance().getIdByAssvos(detailVo.getAss());
			detailVo.setAssid(assID);

		}

		if (errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString())) {
			throw new BusinessException(errorMessage.toString());
		}

		return detailVo;
	}

	public VoucherVO convertToVoucherVOByObjDetail(VoucherVO voucherVo, Object obj) throws Exception {
		DetailVO convertToDetailVo = convertToDetailVo(voucherVo, obj);
		DetailVO oldDetailVos[] = voucherVo.getDetails();
		List<DetailVO> detailList = new ArrayList<DetailVO>();
		for (int i = 0; i < oldDetailVos.length; i++) {
			detailList.add(oldDetailVos[i]);
		}
		detailList.add(convertToDetailVo);
		voucherVo.setDetails(detailList.toArray(new DetailVO[0]));
		return voucherVo;
	}

	public String getVoucherKey(Object obj) {

		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1������ƾ֤����ͷ�¼�Ĺ�����ϵ�ֶ� ���� �Ƿ��Ѿ������˶�Ӧ��vouchervo 2����������ˡ���ô
		 * �Ͱɵ�ǰ�ķ�¼��׷�ӵ���vouchervo�� 3�����û�����ɣ���ô���ݵ�ǰdetail��¼vo��ʼ����vouchervo
		 */
		String voucherKey = "";
		/**
		 * ���ȹ��� voucher ��key
		 */
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				voucherKey += detail.getAttributeValue(detailRowCodes[i]);
			}
		}
		return voucherKey;
	}

	public String getErrorMessageTitle(Object obj) {

		StringBuffer rtMessage = new StringBuffer();

		rtMessage.append("[" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000045")/*
																											 * @
																											 * res
																											 * "�����˲�"
																											 */+ "��" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000479")/*
																																																		 * @
																																																		 * res
																																																		 * "ƾ֤���"
																																																		 */+ "��" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000475")/*
																																																																									 * @
																																																																									 * res
																																																																									 * "ƾ֤��"
																																																																									 */+ "��" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000661")/*
																																																																																																 * @
																																																																																																 * res
																																																																																																 * "�Ƶ���"
																																																																																																 */+ "��" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000669")/*
																																																																																																																							 * @
																																																																																																																							 * res
																																																																																																																							 * "�Ƶ�����"
																																																																																																																							 */+ "]");

		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1������ƾ֤����ͷ�¼�Ĺ�����ϵ�ֶ� ���� �Ƿ��Ѿ������˶�Ӧ��vouchervo 2����������ˡ���ô
		 * �Ͱɵ�ǰ�ķ�¼��׷�ӵ���vouchervo�� 3�����û�����ɣ���ô���ݵ�ǰdetail��¼vo��ʼ����vouchervo
		 */
		/**
		 * ���ȹ��� voucher ��key
		 */
		rtMessage.append("[");
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				rtMessage.append(detail.getAttributeValue(detailRowCodes[i])).append("��");/*
																						 * -=
																						 * notranslate
																						 * =
																						 * -
																						 */
			}
		}

		rtMessage.replace(rtMessage.length() - 1, rtMessage.length(), "]");

		return rtMessage.toString();
	}

	public VoucherVO convertToVoucherVOByObj(Object obj) throws Exception {
		/**
		 * ��ǰ���������ƾ֤�ֱ�Ϊ�����ݵ���Ϣ���ӱ���Ϣ���ֽ����� ������̣� 1������ƾ֤����ͷ�¼�Ĺ�����ϵ�ֶ� ����
		 * �Ƿ��Ѿ������˶�Ӧ��vouchervo ���������,�Ͱѵ�ǰ�ķ�¼��׷�ӵ���vouchervo��
		 * ���û�����ɣ���ô���ݵ�ǰdetail��¼vo��ʼ����vouchervo ���Ұɸ�vouchervo���浽һ��map��
		 * ���Ұɵ�ǰ��¼ҳ׷�ӵ���ǰ����vo������ 2����׷��detailvo֮ǰ�����ȴ���cashflowvoȻ���֮׷�ӵ���ǰdetail��
		 * ͬʱ��Ҫ����detail����׷�ӵ�assvo������ 3 ÿ��ִ�е�ǰ���� ���ƾ֤�����浽һ��map��
		 */
		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1������ƾ֤����ͷ�¼�Ĺ�����ϵ�ֶ� ���� �Ƿ��Ѿ������˶�Ӧ��vouchervo 2����������ˡ���ô
		 * �Ͱɵ�ǰ�ķ�¼��׷�ӵ���vouchervo�� 3�����û�����ɣ���ô���ݵ�ǰdetail��¼vo��ʼ����vouchervo
		 */
		String voucherKey = "";
		/**
		 * ���ȹ��� voucher ��key
		 */
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				voucherKey += detail.getAttributeValue(detailRowCodes[i]);
			}
		}

		StringBuffer errorMessage = new StringBuffer();

		/**
		 * �õ�ƾ֤���������
		 */
		VoucherVO mainVoucherVo = null;
		mainVoucherVo = new VoucherVO();
		mainVoucherVo.setDetailmodflag(UFBoolean.TRUE);
		String accountbookpk = null;
		if (detail.getAttributeValue("main_m_pk_accountingbook") != null) {
			accountbookpk = VoucherExcelImporter.getAccountintBookPKWithCode(detail.getAttributeValue("main_m_pk_accountingbook").toString());
		}

		if (accountbookpk == null) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0264")/*
																												 * @
																												 * res
																												 * "�����˲����벻����!"
																												 */+ IDetailLogger.N);
		}
		mainVoucherVo.setPk_accountingbook(accountbookpk);// pk_account

		UserVO userVo = null;
		if (detail.getAttributeValue("main_pk_prepared") != null) {
			try {
				userVo = SFServiceFacility.getIUserManageQuery().findUserByCode(detail.getAttributeValue("main_pk_prepared").toString(), WorkbenchEnvironment.getInstance().getDSName());
			} catch (BusinessException e) {
				Logger.error(e.getMessage(), e);
			}
		}
		if (userVo == null) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0265")/*
																												 * @
																												 * res
																												 * "�Ҳ�����Ӧ�Ƶ���!"
																												 */+ IDetailLogger.N);
		}
		mainVoucherVo.setPk_prepared(detail.getAttributeValue("main_pk_prepared").toString());// pk_prepared

		Object voucherType = detail.getAttributeValue("main_m_pk_vouchertype");

		String pk_voucherType = null;
		if (voucherType != null) {
			try {
				pk_voucherType = VoucherExportImportConvert.getVoucherTypePKByCode(accountbookpk, voucherType.toString());
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}

		mainVoucherVo.setPk_vouchertype(pk_voucherType);

		if (StringUtils.isEmpty(pk_voucherType)) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0266")/*
																												 * @
																												 * res
																												 * "�Ҳ�����Ӧ��ƾ֤���!"
																												 */+ IDetailLogger.N);
		}

		if (errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString())) {
			throw new BusinessException(errorMessage.toString());
		}

		if (detail.getAttributeValue("main_m_num") != null) {
			try {
				mainVoucherVo.setNo(Integer.parseInt(detail.getAttributeValue("main_m_num").toString()));
			} catch (Exception e) {
				Logger.error("����ƾ֤ʱ����ȡƾ֤�ų�������������ƾ֤�š�");
				Logger.error(e.getMessage(), e);
			}
		}

		// ��������
		if (detail.getAttributeValue("main_m_attachment") != null && !"".equals(detail.getAttributeValue("main_m_attachment"))) {
			mainVoucherVo.setAttachment(Integer.parseInt(detail.getAttributeValue("main_m_attachment").toString()));
		} else {
			mainVoucherVo.setAttachment(0);
		}

		// hurh
		UFDate preparedDate = new UFDate(detail.getAttributeValue("main_m_prepareddate").toString());
		AccountCalendar calendar = CalendarUtilGL.getAccountCalendarByAccountBook(mainVoucherVo.getPk_accountingbook());
		calendar.setDate(preparedDate);
		mainVoucherVo.setPrepareddate(preparedDate);// m_prepareddate
		mainVoucherVo.setYear(calendar.getMonthVO().getYearmth().split("-")[0]);
		mainVoucherVo.setPeriod(calendar.getMonthVO().getAccperiodmth());

		mainVoucherVo.setVoucherkind(0);
		// mainVoucherVo.setPrepareddate(GlWorkBench.getBusiDate());
		mainVoucherVo.setPk_system("GL");
		mainVoucherVo.setDiscardflag(UFBoolean.FALSE);
		// pk_prepared

		String userPK = userVo.getCuserid();
		if (userPK == null || userPK.trim().equals(""))
			nc.vo.fipub.utils.uif2.FiUif2MsgUtil.showUif2DetailMessage(this.parent, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("gl20111017public_0", "02002001-0018")/*
																																												 * @
																																												 * res
																																												 * "����"
																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("gl20111017public_0", "02002001-0074")/*
																																																																				 * @
																																																																				 * res
																																																																				 * "�Ƶ��˴���"
																																																																				 */);
		mainVoucherVo.setCreator(userPK);
		mainVoucherVo.setPk_prepared(userPK);
		// mainVoucherVo.setExplanation("import");
		mainVoucherVo.setPk_group(GlWorkBench.getDefaultGroup());
		mainVoucherVo.setExplanation(detail.getAttributeValue("m_explanation").toString());//
		// mainVoucherVo.setNo(0);
		if (mainVoucherVo.getPk_accountingbook() != null) {
			String pk_org = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());
			mainVoucherVo.setPk_org(pk_org);
			HashMap<String, String> versionMap = null;
			try {
				versionMap = GlOrgUtils.getNewVIDSByOrgIDSAndDate(new String[] { pk_org }, mainVoucherVo.getPrepareddate());
			} catch (BusinessException e2) {
				Logger.error(e2.getMessage(), e2);
			}
			if (versionMap != null) {
				mainVoucherVo.setPk_org_v(versionMap.get(pk_org));
			}
		}

		DetailVO convertToDetailVo = convertToDetailVo(mainVoucherVo, obj);

		/**
		 * ��detailvo ׷�ӵ���ǰmainvoucherVo����
		 */
		DetailVO oldDetailVos[] = mainVoucherVo.getDetails();
		List<DetailVO> detailList = new ArrayList<DetailVO>();
		for (int i = 0; i < oldDetailVos.length; i++) {
			detailList.add(oldDetailVos[i]);
		}
		detailList.add(convertToDetailVo);
		mainVoucherVo.setDetails(detailList.toArray(new DetailVO[0]));

		return mainVoucherVo;
	}

	public boolean isCanSave() {
		return canSave;
	}

	public void setCanSave(boolean canSave) {
		this.canSave = canSave;
	}

	/**
	 * ���ƾ֤����Ķ�������
	 * 
	 * @return
	 */
	private List<InputItem> getVoucherAttributes() {
		List<InputItem> list = new ArrayList<InputItem>();
		VoucherExportItem newItem = null;
		String itemKey[] = new String[] { "m_pk_accountingbook", "m_pk_vouchertype", "m_num", "m_attachment", "pk_prepared", "m_prepareddate" };
		String showNames[] =
				new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000045")/*
																											 * @
																											 * res
																											 * "�����˲�"
																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000760")/*
																																																		 * @
																																																		 * res
																																																		 * "ƾ֤������"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000475")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "ƾ֤��"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000395")/*
																																																																																														 * @
																																																																																														 * res
																																																																																														 * "��������"
																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000761")/*
																																																																																																																					 * @
																																																																																																																					 * res
																																																																																																																					 * "�Ƶ��˱���"
																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000669") /*
																																																																																																																																										 * @
																																																																																																																																										 * res
																																																																																																																																										 * "�Ƶ�����"
																																																																																																																																										 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey("main_" + itemKey[i]);// ���ԵĴ���
			// ����Ϊ0��ʾ����������
			newItem.setPos(0);
			newItem.setOrder(i + 1);
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "ƾ֤��Ϣ"
																												 */);
			newItem.setNotNull(false);
			newItem.setEdit(true);
			newItem.setMultiLang(false);
			list.add(newItem);
		}
		return list;
	}

	/**
	 * ���ƾ֤�ֱ������
	 * 
	 * @return
	 */
	private List<InputItem> getVoucherDetailVOAttributes() {
		List<InputItem> list = new ArrayList<InputItem>();
		VoucherExportItem newItem = null;
		String itemKey[] = null;
		String showNames[] = null;
		if (ProductInstallCheckTool.checkEURInstall().booleanValue()) {
			itemKey =
					new String[] { "m_explanation", "m_accsubjcode", "m_pk_currtype", "m_debitamount", "m_localdebitamount", "m_groupdebitamount", "m_globaldebitamount", "unitname", "m_price", "m_debitquantity", "m_creditquantity", "m_creditamount", "m_localcreditamount", "m_groupcreditamount", "m_globalcreditamount", "m_checkno", "m_checkdate", "verifyno", "verifydate", DetailVO.BANKACCOUNT, DetailVO.BILLTYPE, DetailVO.CHECKSTYLE, GLStringConst.VAT + VatDetailVO.PK_VATCOUNTRY, GLStringConst.VAT + VatDetailVO.PK_RECEIVECOUNTRY, GLStringConst.VAT + VatDetailVO.BUSINESSCODE, GLStringConst.VAT + VatDetailVO.PK_CLIENTVATCODE, GLStringConst.VAT + VatDetailVO.PK_SUPPLIERVATCODE, GLStringConst.VAT + VatDetailVO.PK_TAXCODE, GLStringConst.VAT + VatDetailVO.DIRECTION, GLStringConst.VAT + VatDetailVO.MONEYAMOUNT, "m_excrate2", "excrate3", "excrate4" };
			showNames =
					new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002185")/*
																													 * @
																													 * res
																													 * "ժҪ"
																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003072")/*
																																																		 * @
																																																		 * res
																																																		 * "��Ŀ����"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0001755")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "����"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0222")/*
																																																																																												 * @
																																																																																												 * res
																																																																																												 * "ԭ�ҽ跽���"
																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0223")/*
																																																																																																																	 * @
																																																																																																																	 * res
																																																																																																																	 * "���ҽ跽���"
																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0224")/*
																																																																																																																																						 * @
																																																																																																																																						 * res
																																																																																																																																						 * "���ű��ҽ跽���"
																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0225")/*
																																																																																																																																																											 * @
																																																																																																																																																											 * res
																																																																																																																																																											 * "ȫ�ֱ��ҽ跽���"
																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000763")/*
																																																																																																																																																																																		 * @
																																																																																																																																																																																		 * res
																																																																																																																																																																																		 * "��˾����"
																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000741")/*
																																																																																																																																																																																																							 * @
																																																																																																																																																																																																							 * res
																																																																																																																																																																																																							 * "����"
																																																																																																																																																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000343")/*
																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																												 * "�跽����"
																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003856")/*
																																																																																																																																																																																																																																																	 * @
																																																																																																																																																																																																																																																	 * res
																																																																																																																																																																																																																																																	 * "��������"
																																																																																																																																																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0226")/*
																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																						 * "ԭ�Ҵ������"
																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0227")/*
																																																																																																																																																																																																																																																																																											 * @
																																																																																																																																																																																																																																																																																											 * res
																																																																																																																																																																																																																																																																																											 * "���Ҵ������"
																																																																																																																																																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0228")/*
																																																																																																																																																																																																																																																																																																																 * @
																																																																																																																																																																																																																																																																																																																 * res
																																																																																																																																																																																																																																																																																																																 * "���ű��Ҵ������"
																																																																																																																																																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0229")/*
																																																																																																																																																																																																																																																																																																																																					 * @
																																																																																																																																																																																																																																																																																																																																					 * res
																																																																																																																																																																																																																																																																																																																																					 * "ȫ�ֱ��Ҵ������"
																																																																																																																																																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003245")/*
																																																																																																																																																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																																																																																																																																																										 * "�����"
																																																																																																																																																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003252")/*
																																																																																																																																																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																																																																																																																																																															 * "��������"
																																																																																																																																																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002791")/*
																																																																																																																																																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																																																																																																																																																				 * "������"
																																																																																																																																																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000584")/*
																																																																																																																																																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																																																																																																																																																									 * "ҵ������"
																																																																																																																																																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0004117")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * "�����˻�"
																																																																																																																																																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003020")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * "Ʊ������"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003249")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * "���㷽ʽ"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0013")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * "��˰����"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0014")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * "�ջ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0015")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * "���״���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0016")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * "�ͻ�VATע����"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0017")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * "��Ӧ��VATע����"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0018")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "˰��"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000762")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * "VAT����"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0019")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * "��˰���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0021")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * "��֯���һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0022")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * "���ű��һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0023") /*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * "ȫ�ֱ��һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 */};
		} else {
			itemKey =
					new String[] { "m_explanation", "m_accsubjcode", "m_pk_currtype", "m_debitamount", "m_localdebitamount", "m_groupdebitamount", "m_globaldebitamount", "unitname", "m_price", "m_debitquantity", "m_creditquantity", "m_creditamount", "m_localcreditamount", "m_groupcreditamount", "m_globalcreditamount", "m_checkno", "m_checkdate", "verifyno", "verifydate", DetailVO.BANKACCOUNT, DetailVO.BILLTYPE, DetailVO.CHECKSTYLE, "m_excrate2", "excrate3", "excrate4" };
			showNames =
					new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002185")/*
																													 * @
																													 * res
																													 * "ժҪ"
																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003072")/*
																																																		 * @
																																																		 * res
																																																		 * "��Ŀ����"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0001755")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "����"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0222")/*
																																																																																												 * @
																																																																																												 * res
																																																																																												 * "ԭ�ҽ跽���"
																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0223")/*
																																																																																																																	 * @
																																																																																																																	 * res
																																																																																																																	 * "���ҽ跽���"
																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0224")/*
																																																																																																																																						 * @
																																																																																																																																						 * res
																																																																																																																																						 * "���ű��ҽ跽���"
																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0225")/*
																																																																																																																																																											 * @
																																																																																																																																																											 * res
																																																																																																																																																											 * "ȫ�ֱ��ҽ跽���"
																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000185")/*
																																																																																																																																																																																 * @
																																																																																																																																																																																 * res
																																																																																																																																																																																 * "��˾����"
																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000741")/*
																																																																																																																																																																																																					 * @
																																																																																																																																																																																																					 * res
																																																																																																																																																																																																					 * "����"
																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000343")/*
																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																										 * "�跽����"
																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003856")/*
																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																															 * "��������"
																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0226")/*
																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																				 * "ԭ�Ҵ������"
																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0227")/*
																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																									 * "���Ҵ������"
																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0228")/*
																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																														 * "���ű��Ҵ������"
																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0229")/*
																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																			 * "ȫ�ֱ��Ҵ������"
																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003245")/*
																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																								 * "�����"
																																																																																																																																																																																																																																																																																																																																																								 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003252")/*
																																																																																																																																																																																																																																																																																																																																																																													 * @
																																																																																																																																																																																																																																																																																																																																																																													 * res
																																																																																																																																																																																																																																																																																																																																																																													 * "��������"
																																																																																																																																																																																																																																																																																																																																																																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002791")/*
																																																																																																																																																																																																																																																																																																																																																																																																		 * @
																																																																																																																																																																																																																																																																																																																																																																																																		 * res
																																																																																																																																																																																																																																																																																																																																																																																																		 * "������"
																																																																																																																																																																																																																																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000584")/*
																																																																																																																																																																																																																																																																																																																																																																																																																							 * @
																																																																																																																																																																																																																																																																																																																																																																																																																							 * res
																																																																																																																																																																																																																																																																																																																																																																																																																							 * "ҵ������"
																																																																																																																																																																																																																																																																																																																																																																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0004117")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "�����˻�"
																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003020")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * "Ʊ������"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003249")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * "���㷽ʽ"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0021")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "��֯���һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0022")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * "���ű��һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0023") /*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * "ȫ�ֱ��һ���"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 */};
		}

		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// ���ԵĴ���
			// ����Ϊ0��ʾ����������
			newItem.setPos(0);
			// ����Ӧ���޸���ʼλ��
			newItem.setOrder(i + 1 + getVoucherAttributes().size());
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "ƾ֤��Ϣ"
																												 */);
			newItem.setNotNull(false);
			newItem.setEdit(true);
			newItem.setMultiLang(false);
			list.add(newItem);
		}
		return list;
	}

	private List<InputItem> getVoucherAssVOAttributes() {
		List<InputItem> list = new ArrayList<InputItem>();
		VoucherExportItem newItem = null;
		String itemKey[] = new String[] { "ass_1", "ass_2", "ass_3", "ass_4", "ass_5", "ass_6", "ass_7", "ass_8", "ass_9" };
		String showNames[] =
				new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0232")/*
																												 * @
																												 * res
																												 * "��������1"
																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0233")/*
																																																	 * @
																																																	 * res
																																																	 * "��������2"
																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0234")/*
																																																																						 * @
																																																																						 * res
																																																																						 * "��������3"
																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0235")/*
																																																																																											 * @
																																																																																											 * res
																																																																																											 * "��������4"
																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0236")/*
																																																																																																																 * @
																																																																																																																 * res
																																																																																																																 * "��������5"
																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0237")/*
																																																																																																																																					 * @
																																																																																																																																					 * res
																																																																																																																																					 * "��������6"
																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0260")/*
																																																																																																																																																										 * @
																																																																																																																																																										 * res
																																																																																																																																																										 * "��������7"
																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0261")/*
																																																																																																																																																																															 * @
																																																																																																																																																																															 * res
																																																																																																																																																																															 * "��������8"
																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0262") /*
																																																																																																																																																																																																					 * @
																																																																																																																																																																																																					 * res
																																																																																																																																																																																																					 * "��������9"
																																																																																																																																																																																																					 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// ���ԵĴ���
			// ����Ϊ0��ʾ����������
			newItem.setPos(0);
			// ����Ӧ���޸���ʼλ��
			newItem.setOrder(i + 1 + getVoucherAttributes().size() + getVoucherDetailVOAttributes().size());
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "ƾ֤��Ϣ"
																												 */);
			newItem.setNotNull(false);
			newItem.setEdit(true);
			newItem.setMultiLang(false);
			list.add(newItem);
		}
		return list;
	}

	private List<InputItem> getVoucherCashFlowVOAttributes() {
		List<InputItem> list = new ArrayList<InputItem>();
		VoucherExportItem newItem = null;
		String itemKey[] = new String[] { "m_flag", "cashflowcurr", "m_money", "m_moneymain", "m_moneygroup", "m_moneyglobal", "cashflowName", "cashflowCode" };
		String showNames[] =
				new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002301")/*
																												 * @
																												 * res
																												 * "����"
																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000581")/*
																																																	 * @
																																																	 * res
																																																	 * "��������"
																																																	 */, CurrTypeConst.CURRTYPE(), CurrTypeConst.LOC_CURRTYPE(), CurrTypeConst.GROUP_CURRTYPE(), CurrTypeConst.GLOBAL_CURRTYPE(), nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0238")/*
																																																																																																					 * @
																																																																																																					 * res
																																																																																																					 * "�ֽ���������"
																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0239") /*
																																																																																																																											 * @
																																																																																																																											 * res
																																																																																																																											 * "�ֽ���������"
																																																																																																																											 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// ���ԵĴ���
			// ����Ϊ0��ʾ����������
			newItem.setPos(1);
			// ����Ӧ���޸���ʼλ��
			newItem.setOrder(i + 1);
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setNotNull(false);
			newItem.setTabCode("cashflow");
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0240")/*
																												 * @
																												 * res
																												 * "�ֽ�����.."
																												 */);
			newItem.setEdit(true);
			newItem.setMultiLang(false);
			list.add(newItem);
		}
		return list;
	}

	private VoucherVO[] getVoucherVOs(String[] pk_vouchers) {
		VoucherVO[] vouchers;
		try {
			vouchers = VoucherDataBridge.getInstance().queryByPks(pk_vouchers);
			HashMap<String, VoucherVO> tmp_map = new HashMap<String, VoucherVO>();
			if (vouchers != null)
				for (int i = 0; i < vouchers.length; i++) {
					if (vouchers[i] != null)
						tmp_map.put(vouchers[i].getPk_voucher(), vouchers[i]);
				}
			Vector<VoucherVO> vv = new Vector<VoucherVO>();
			for (int i = 0; i < pk_vouchers.length; i++) {
				if (tmp_map.get(pk_vouchers[i]) != null)
					vv.addElement(tmp_map.get(pk_vouchers[i]));
			}
			VoucherVO[] rr = null;
			if (vv.size() > 0) {
				rr = new VoucherVO[vv.size()];
				vv.copyInto(rr);
			}
			return rr;
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new nc.vo.gateway60.pub.GlBusinessException(nc.ui.ml.NCLangRes.getInstance().getStrByID("20021005", "UPP20021005-000487")/*
																																			 * @
																																			 * res
																																			 * "ƾ֤��Ϣ�д���������³��Ըò�����"
																																			 */);
		}
	}

	@Override
	public JComponent getJComponent() {
		return parent;
	}

	private BankaccSubGeneralAccessor getBankAccountAccessor() {
		if (bankAccountAccessor == null) {
			bankAccountAccessor = (BankaccSubGeneralAccessor) GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.BANKACCSUB);
		}
		return bankAccountAccessor;
	}

	private IGeneralAccessor getCheckStyleAccessor() {
		if (checkStyleAccessor == null) {
			checkStyleAccessor = GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.BALATYPE);
		}
		return checkStyleAccessor;
	}

	private IGeneralAccessor getBillTypeAccessor() {
		if (billTypeAccessor == null) {
			billTypeAccessor = GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.NOTETYPE);
		}
		return billTypeAccessor;
	}

	public void setSetValueEx(Exception setValueEx) {
		this.setValueEx = setValueEx;
	}

	public Exception getSetValueEx() {
		return setValueEx;
	}

}