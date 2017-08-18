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
	 * 标示当前save操作是否可以从缓存中取值 相当于锁
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
	 * 返回空 标示导入的数据合法
	 */
	@Override
	public ImportableInfo getImportableInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 在这里要创建导出的项目数组
	 */
	@Override
	public List<InputItem> getInputItems() {
		/**
		 * 在这里 写死要导出那些项目
		 */
		List<InputItem> list = new ArrayList<InputItem>();
		list.addAll(this.getVoucherAttributes());
		list.addAll(this.getVoucherDetailVOAttributes());
		list.addAll(this.getVoucherAssVOAttributes());
		list.addAll(this.getVoucherCashFlowVOAttributes());
		return list;
	}

	/**
	 * 因为没有使用UI工厂2 因此要在这里进行具体对象的封装
	 * 
	 * @param exportItems
	 *            要导出的项目， 用来生成excel中的代码 名称 数据 位置等
	 */
	@Override
	public ExportDataInfo getValue(List<InputItem> exportItems) {
		/**
		 * 内部要根据exportItems构造 导出ExportDataInfo 把detail表作为主表 把现金流量的作为字表 其中主表
		 * 和辅助核算都拼接在detail中
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
		// 得到了选中的凭证
		VoucherVO vouchers[] = getVoucherVOs(voucherpkList.toArray(new String[0]));
		// 针对每条凭证的分录创建多个主数据
		DetailVO detail[] = null;
		VoucherExportMainTableVO main = null;
		List<VoucherExportForExcelVO> exportMainVOs = new ArrayList<VoucherExportForExcelVO>();
		for (int i = 0; i < vouchers.length; i++) {
			detail = vouchers[i].getDetails();
			for (int j = 0; j < detail.length; j++) {
				VoucherExportSubVO cashFlow[] = null;
				// 创建一个主导出vo
				main = new VoucherExportMainTableVO(vouchers[i], detail[j], detail[j].getAss());
				main.setAttributeValue("unitname", BusiUnitDataCache.getOrgByPk(detail[j].getPk_unit()).getCode());
				// 然后创建现金流量vo
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
			// 校验组织
			checkUtil.checkUnit((VoucherVO) value);

			// 校验数据是否相等
			checkUtil.checkAmountEqual((VoucherVO) value);

			// 处理精度
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
		 * 1、根据凭证主表和分录的关联关系字段 检索 是否已经生成了对应的vouchervo 2、如果生成了、那么
		 * 就吧当前的分录再追加到该vouchervo中 3、如果没有生成，那么根据当前detail分录vo开始构造vouchervo
		 */
		String voucherKey = "";

		/**
		 * 首先构造 voucher 的key
		 */
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				voucherKey += detail.getAttributeValue(detailRowCodes[i]);
			}
		}

		String accountbookpk = VoucherExcelImporter.getAccountintBookPKWithCode(detail.getAttributeValue("main_m_pk_accountingbook").toString());

		/**
		 * 构造当前的detail
		 */
		DetailVO detailVo = new DetailVO();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				// 表示是vouchervo的信息
			} else if (detailRowCodes[i].startsWith("ass_")) {
				// 表示是辅助核算的信息
			} else if (detailRowCodes[i].startsWith(GLStringConst.VAT)) {
				// 表示税务明细
			} else {
				// 表示是真正的detail表中的数据
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
																																			 * "科目编码{0}无法翻译！"
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
																																			 * "币种{0}无法翻译！"
																																			 */+ IDetailLogger.N);
		}

		String pk_unit = VoucherExportImportConvert.getUnitPkByCode(mainVoucherVo.getPk_accountingbook(), detail.getAttributeValue("unitname").toString());
		if (pk_unit == null || pk_unit.trim().equals("")) {
			pk_unit = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());
		}

		// 如果启用了二级核算单位
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
			// hurh 数量单价
			detailVo.setDebitquantity(new UFDouble(detail.getAttributeValue("m_debitquantity").toString()));
			detailVo.setCreditquantity(new UFDouble(detail.getAttributeValue("m_creditquantity").toString()));
		} catch (Exception e) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0280")/*
																												 * @
																												 * res
																												 * "数量格式不正确！"
																												 */+ IDetailLogger.N);
		}

		try {
			detailVo.setPrice(new UFDouble(detail.getAttributeValue("m_price").toString()));
		} catch (Exception e) {
			errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0281")/*
																												 * @
																												 * res
																												 * "单价格式不正确！"
																												 */+ IDetailLogger.N);
		}

		// hurh辅助信息
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
																																								 * "找不到对应{0}编码的结算方式！"
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
																																								 * "找不到对应{0}编码票据类型！"
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

		// 是否启用欧盟报表，如果未启用则将vat信息清空
		boolean isEurUse = GLStartCheckUtil.checkEURStart(mainVoucherVo.getPk_group());

		// 税务明细
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
			// 报税国家
			Object vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_VATCOUNTRY);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setPk_vatcountry(GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.COUNTRYZONE).getDocByCode(detailVo.getPk_unit(), vatinfo.toString()).getPk());
			}
			// 收货国
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_RECEIVECOUNTRY);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setPk_receivecountry(GeneralAccessorFactory.getAccessor(IBDMetaDataIDConst.COUNTRYZONE).getDocByCode(detailVo.getPk_unit(), vatinfo.toString()).getPk());
			}
			// 交易代码
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.BUSINESSCODE);
			vatdetail.setBusinesscode(vatinfo == null ? null : vatinfo.toString());
			// 客户VAT注册码
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_CLIENTVATCODE);
			vatdetail.setPk_clientvatcode(vatinfo == null ? null : vatinfo.toString());
			// 供应商VAT注册码
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.PK_SUPPLIERVATCODE);
			vatdetail.setPk_suppliervatcode(vatinfo == null ? null : vatinfo.toString());
			// 税码
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

			// 方向
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.DIRECTION);
			if (DirectionEnum.DEBIT.getName().equals(vatinfo)) {
				vatdetail.setDirection(DirectionEnum.DEBIT.value().toString());
			} else {
				vatdetail.setDirection(DirectionEnum.CREDIT.value().toString());
			}
			// 计税金额
			vatinfo = detail.getAttributeValue(GLStringConst.VAT + VatDetailVO.MONEYAMOUNT);
			if (vatinfo != null && !StringUtils.isEmpty(vatinfo.toString())) {
				vatdetail.setMoneyamount(new UFDouble(vatinfo.toString()));
			}
			// 税额
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
				// 设置到detailVO中
				detailVo.setVatdetail(vatdetail);
			}
		}

		// detailVo.setNo(0);//去掉可能存在的凭证号
		if (mainVoucherVo.getPk_org() != null)
			detailVo.setPk_org(mainVoucherVo.getPk_org());
		if (mainVoucherVo.getPk_org_v() != null)
			detailVo.setPk_org(mainVoucherVo.getPk_org_v());

		String pk_org = AccountBookUtil.getPk_orgByAccountBookPk(mainVoucherVo.getPk_accountingbook());

		/**
		 * 构造现金流量
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
																																												 * "找不到编码为{0}的现金流量项目！"
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
		 * 创建辅助核算
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

				if (itemVO == null) {// 会计辅助核算项目为空
					errorMessage.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0273", null, new String[] { value.split(":")[1] })/*
																																									 * @
																																									 * res
																																									 * "{0}编码对应的会计辅助核算项目不存在！"
																																									 */+ "\n");
				} else {//
					if (StringUtils.isEmpty(itemVO.getRefnodename())) {
						// 说明是基本类型，则直接当作字符串保存
						tempAss.setPk_Checkvalue(value.split(":")[0]);
					} else {
						IBDData valuedata = null;
						try {
							// hurh 银行账户档案特殊
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
																																												 * "{0}编码对应的档案不存在！"
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
		 * 1、根据凭证主表和分录的关联关系字段 检索 是否已经生成了对应的vouchervo 2、如果生成了、那么
		 * 就吧当前的分录再追加到该vouchervo中 3、如果没有生成，那么根据当前detail分录vo开始构造vouchervo
		 */
		String voucherKey = "";
		/**
		 * 首先构造 voucher 的key
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
																											 * "核算账簿"
																											 */+ "、" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000479")/*
																																																		 * @
																																																		 * res
																																																		 * "凭证类别"
																																																		 */+ "、" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000475")/*
																																																																									 * @
																																																																									 * res
																																																																									 * "凭证号"
																																																																									 */+ "、" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000661")/*
																																																																																																 * @
																																																																																																 * res
																																																																																																 * "制单人"
																																																																																																 */+ "、" + nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000669")/*
																																																																																																																							 * @
																																																																																																																							 * res
																																																																																																																							 * "制单日期"
																																																																																																																							 */+ "]");

		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1、根据凭证主表和分录的关联关系字段 检索 是否已经生成了对应的vouchervo 2、如果生成了、那么
		 * 就吧当前的分录再追加到该vouchervo中 3、如果没有生成，那么根据当前detail分录vo开始构造vouchervo
		 */
		/**
		 * 首先构造 voucher 的key
		 */
		rtMessage.append("[");
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				rtMessage.append(detail.getAttributeValue(detailRowCodes[i])).append("、");/*
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
		 * 当前传入的是以凭证字表为主数据的信息，子表信息是现金流量 处理过程： 1、根据凭证主表和分录的关联关系字段 检索
		 * 是否已经生成了对应的vouchervo 如果生成了,就把当前的分录再追加到该vouchervo中
		 * 如果没有生成，那么根据当前detail分录vo开始构造vouchervo 并且吧该vouchervo保存到一个map中
		 * 并且吧当前分录页追加到当前主表vo厘米那 2、在追加detailvo之前，首先处理cashflowvo然后把之追加到当前detail中
		 * 同时还要处理detail后面追加的assvo的问题 3 每次执行当前方法 会吧凭证主表保存到一个map中
		 */
		ExtendedAggregatedValueObject aggvo = (ExtendedAggregatedValueObject) obj;
		CircularlyAccessibleValueObject detail = aggvo.getParentVO();
		/**
		 * 1、根据凭证主表和分录的关联关系字段 检索 是否已经生成了对应的vouchervo 2、如果生成了、那么
		 * 就吧当前的分录再追加到该vouchervo中 3、如果没有生成，那么根据当前detail分录vo开始构造vouchervo
		 */
		String voucherKey = "";
		/**
		 * 首先构造 voucher 的key
		 */
		String detailRowCodes[] = detail.getAttributeNames();
		for (int i = 0; i < detailRowCodes.length; i++) {
			if (detailRowCodes[i].startsWith("main_")) {
				voucherKey += detail.getAttributeValue(detailRowCodes[i]);
			}
		}

		StringBuffer errorMessage = new StringBuffer();

		/**
		 * 得到凭证主表的引用
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
																												 * "核算账簿编码不存在!"
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
																												 * "找不到对应制单人!"
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
																												 * "找不到对应的凭证类别!"
																												 */+ IDetailLogger.N);
		}

		if (errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString())) {
			throw new BusinessException(errorMessage.toString());
		}

		if (detail.getAttributeValue("main_m_num") != null) {
			try {
				mainVoucherVo.setNo(Integer.parseInt(detail.getAttributeValue("main_m_num").toString()));
			} catch (Exception e) {
				Logger.error("导入凭证时，获取凭证号出错，将重新生成凭证号。");
				Logger.error(e.getMessage(), e);
			}
		}

		// 附单据数
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
																																												 * "警告"
																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("gl20111017public_0", "02002001-0074")/*
																																																																				 * @
																																																																				 * res
																																																																				 * "制单人错误"
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
		 * 把detailvo 追加到当前mainvoucherVo上面
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
	 * 获得凭证主表的定义属性
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
																											 * "核算账簿"
																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000760")/*
																																																		 * @
																																																		 * res
																																																		 * "凭证类别编码"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000475")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "凭证号"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000395")/*
																																																																																														 * @
																																																																																														 * res
																																																																																														 * "附单据数"
																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000761")/*
																																																																																																																					 * @
																																																																																																																					 * res
																																																																																																																					 * "制单人编码"
																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000669") /*
																																																																																																																																										 * @
																																																																																																																																										 * res
																																																																																																																																										 * "制单日期"
																																																																																																																																										 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey("main_" + itemKey[i]);// 属性的代码
			// 设置为0标示是主表数据
			newItem.setPos(0);
			newItem.setOrder(i + 1);
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "凭证信息"
																												 */);
			newItem.setNotNull(false);
			newItem.setEdit(true);
			newItem.setMultiLang(false);
			list.add(newItem);
		}
		return list;
	}

	/**
	 * 获得凭证字表的属性
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
																													 * "摘要"
																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003072")/*
																																																		 * @
																																																		 * res
																																																		 * "科目编码"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0001755")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "币种"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0222")/*
																																																																																												 * @
																																																																																												 * res
																																																																																												 * "原币借方金额"
																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0223")/*
																																																																																																																	 * @
																																																																																																																	 * res
																																																																																																																	 * "本币借方金额"
																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0224")/*
																																																																																																																																						 * @
																																																																																																																																						 * res
																																																																																																																																						 * "集团本币借方金额"
																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0225")/*
																																																																																																																																																											 * @
																																																																																																																																																											 * res
																																																																																																																																																											 * "全局本币借方金额"
																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000763")/*
																																																																																																																																																																																		 * @
																																																																																																																																																																																		 * res
																																																																																																																																																																																		 * "公司主键"
																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000741")/*
																																																																																																																																																																																																							 * @
																																																																																																																																																																																																							 * res
																																																																																																																																																																																																							 * "单价"
																																																																																																																																																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000343")/*
																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																												 * "借方数量"
																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003856")/*
																																																																																																																																																																																																																																																	 * @
																																																																																																																																																																																																																																																	 * res
																																																																																																																																																																																																																																																	 * "贷方数量"
																																																																																																																																																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0226")/*
																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																						 * "原币贷方金额"
																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0227")/*
																																																																																																																																																																																																																																																																																											 * @
																																																																																																																																																																																																																																																																																											 * res
																																																																																																																																																																																																																																																																																											 * "本币贷方金额"
																																																																																																																																																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0228")/*
																																																																																																																																																																																																																																																																																																																 * @
																																																																																																																																																																																																																																																																																																																 * res
																																																																																																																																																																																																																																																																																																																 * "集团本币贷方金额"
																																																																																																																																																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0229")/*
																																																																																																																																																																																																																																																																																																																																					 * @
																																																																																																																																																																																																																																																																																																																																					 * res
																																																																																																																																																																																																																																																																																																																																					 * "全局本币贷方金额"
																																																																																																																																																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003245")/*
																																																																																																																																																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																																																																																																																																																										 * "结算号"
																																																																																																																																																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003252")/*
																																																																																																																																																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																																																																																																																																																															 * "结算日期"
																																																																																																																																																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002791")/*
																																																																																																																																																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																																																																																																																																																				 * "核销号"
																																																																																																																																																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000584")/*
																																																																																																																																																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																																																																																																																																																									 * "业务日期"
																																																																																																																																																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0004117")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																														 * "银行账户"
																																																																																																																																																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003020")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * "票据类型"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003249")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * "结算方式"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0013")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 * "报税国家"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0014")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 * "收货国"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0015")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 * "交易代码"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0016")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 * "客户VAT注册码"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0017")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * "供应商VAT注册码"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0018")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "税码"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("20021505", "UPP20021505-000762")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 * "VAT方向"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0019")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 * "计税金额"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0021")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 * "组织本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0022")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 * "集团本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0023") /*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 * "全局本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																											 */};
		} else {
			itemKey =
					new String[] { "m_explanation", "m_accsubjcode", "m_pk_currtype", "m_debitamount", "m_localdebitamount", "m_groupdebitamount", "m_globaldebitamount", "unitname", "m_price", "m_debitquantity", "m_creditquantity", "m_creditamount", "m_localcreditamount", "m_groupcreditamount", "m_globalcreditamount", "m_checkno", "m_checkdate", "verifyno", "verifydate", DetailVO.BANKACCOUNT, DetailVO.BILLTYPE, DetailVO.CHECKSTYLE, "m_excrate2", "excrate3", "excrate4" };
			showNames =
					new String[] { nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002185")/*
																													 * @
																													 * res
																													 * "摘要"
																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003072")/*
																																																		 * @
																																																		 * res
																																																		 * "科目编码"
																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0001755")/*
																																																																							 * @
																																																																							 * res
																																																																							 * "币种"
																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0222")/*
																																																																																												 * @
																																																																																												 * res
																																																																																												 * "原币借方金额"
																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0223")/*
																																																																																																																	 * @
																																																																																																																	 * res
																																																																																																																	 * "本币借方金额"
																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0224")/*
																																																																																																																																						 * @
																																																																																																																																						 * res
																																																																																																																																						 * "集团本币借方金额"
																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0225")/*
																																																																																																																																																											 * @
																																																																																																																																																											 * res
																																																																																																																																																											 * "全局本币借方金额"
																																																																																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000185")/*
																																																																																																																																																																																 * @
																																																																																																																																																																																 * res
																																																																																																																																																																																 * "公司主键"
																																																																																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000741")/*
																																																																																																																																																																																																					 * @
																																																																																																																																																																																																					 * res
																																																																																																																																																																																																					 * "单价"
																																																																																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0000343")/*
																																																																																																																																																																																																																										 * @
																																																																																																																																																																																																																										 * res
																																																																																																																																																																																																																										 * "借方数量"
																																																																																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003856")/*
																																																																																																																																																																																																																																															 * @
																																																																																																																																																																																																																																															 * res
																																																																																																																																																																																																																																															 * "贷方数量"
																																																																																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0226")/*
																																																																																																																																																																																																																																																																				 * @
																																																																																																																																																																																																																																																																				 * res
																																																																																																																																																																																																																																																																				 * "原币贷方金额"
																																																																																																																																																																																																																																																																				 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0227")/*
																																																																																																																																																																																																																																																																																									 * @
																																																																																																																																																																																																																																																																																									 * res
																																																																																																																																																																																																																																																																																									 * "本币贷方金额"
																																																																																																																																																																																																																																																																																									 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0228")/*
																																																																																																																																																																																																																																																																																																														 * @
																																																																																																																																																																																																																																																																																																														 * res
																																																																																																																																																																																																																																																																																																														 * "集团本币贷方金额"
																																																																																																																																																																																																																																																																																																														 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0229")/*
																																																																																																																																																																																																																																																																																																																																			 * @
																																																																																																																																																																																																																																																																																																																																			 * res
																																																																																																																																																																																																																																																																																																																																			 * "全局本币贷方金额"
																																																																																																																																																																																																																																																																																																																																			 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003245")/*
																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																								 * "结算号"
																																																																																																																																																																																																																																																																																																																																																								 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003252")/*
																																																																																																																																																																																																																																																																																																																																																																													 * @
																																																																																																																																																																																																																																																																																																																																																																													 * res
																																																																																																																																																																																																																																																																																																																																																																													 * "结算日期"
																																																																																																																																																																																																																																																																																																																																																																													 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0002791")/*
																																																																																																																																																																																																																																																																																																																																																																																																		 * @
																																																																																																																																																																																																																																																																																																																																																																																																		 * res
																																																																																																																																																																																																																																																																																																																																																																																																		 * "核销号"
																																																																																																																																																																																																																																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000584")/*
																																																																																																																																																																																																																																																																																																																																																																																																																							 * @
																																																																																																																																																																																																																																																																																																																																																																																																																							 * res
																																																																																																																																																																																																																																																																																																																																																																																																																							 * "业务日期"
																																																																																																																																																																																																																																																																																																																																																																																																																							 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0004117")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "银行账户"
																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003020")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 * "票据类型"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UC000-0003249")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 * "结算方式"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0021")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 * "组织本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0022")/*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 * "集团本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																		 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("2002001_0", "02002001-0023") /*
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * @
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * res
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 * "全局本币汇率"
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																								 */};
		}

		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// 属性的代码
			// 设置为0标示是主表数据
			newItem.setPos(0);
			// 这里应该修改起始位置
			newItem.setOrder(i + 1 + getVoucherAttributes().size());
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "凭证信息"
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
																												 * "辅助核算1"
																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0233")/*
																																																	 * @
																																																	 * res
																																																	 * "辅助核算2"
																																																	 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0234")/*
																																																																						 * @
																																																																						 * res
																																																																						 * "辅助核算3"
																																																																						 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0235")/*
																																																																																											 * @
																																																																																											 * res
																																																																																											 * "辅助核算4"
																																																																																											 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0236")/*
																																																																																																																 * @
																																																																																																																 * res
																																																																																																																 * "辅助核算5"
																																																																																																																 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0237")/*
																																																																																																																																					 * @
																																																																																																																																					 * res
																																																																																																																																					 * "辅助核算6"
																																																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0260")/*
																																																																																																																																																										 * @
																																																																																																																																																										 * res
																																																																																																																																																										 * "辅助核算7"
																																																																																																																																																										 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0261")/*
																																																																																																																																																																															 * @
																																																																																																																																																																															 * res
																																																																																																																																																																															 * "辅助核算8"
																																																																																																																																																																															 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0262") /*
																																																																																																																																																																																																					 * @
																																																																																																																																																																																																					 * res
																																																																																																																																																																																																					 * "辅助核算9"
																																																																																																																																																																																																					 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// 属性的代码
			// 设置为0标示是主表数据
			newItem.setPos(0);
			// 这里应该修改起始位置
			newItem.setOrder(i + 1 + getVoucherAttributes().size() + getVoucherDetailVOAttributes().size());
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0221")/*
																												 * @
																												 * res
																												 * "凭证信息"
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
																												 * "方向"
																												 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UCMD1-000581")/*
																																																	 * @
																																																	 * res
																																																	 * "分析币种"
																																																	 */, CurrTypeConst.CURRTYPE(), CurrTypeConst.LOC_CURRTYPE(), CurrTypeConst.GROUP_CURRTYPE(), CurrTypeConst.GLOBAL_CURRTYPE(), nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0238")/*
																																																																																																					 * @
																																																																																																					 * res
																																																																																																					 * "现金流量名称"
																																																																																																					 */, nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0239") /*
																																																																																																																											 * @
																																																																																																																											 * res
																																																																																																																											 * "现金流量编码"
																																																																																																																											 */};
		for (int i = 0; i < itemKey.length; i++) {
			newItem = new VoucherExportItem();
			newItem.setItemKey(itemKey[i]);// 属性的代码
			// 设置为0标示是主表数据
			newItem.setPos(1);
			// 这里应该修改起始位置
			newItem.setOrder(i + 1);
			newItem.setShow(true);
			newItem.setShowName(showNames[i]);
			newItem.setNotNull(false);
			newItem.setTabCode("cashflow");
			newItem.setTabName(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("glpub_0", "02002003-0240")/*
																												 * @
																												 * res
																												 * "现金流量.."
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
																																			 * "凭证信息有错，请检查后重新尝试该操作。"
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