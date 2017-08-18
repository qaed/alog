package nc.impl.bd.psn.psndoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.bd.baseservice.md.SingleBaseService;
import nc.bs.bd.baseservice.validator.RefPkExistsValidator;
import nc.bs.bd.psn.psndoc.validator.PsndocEnableValidator;
import nc.bs.bd.psn.psndoc.validator.PsndocHREnableValidator;
import nc.bs.bd.psn.psndoc.validator.PsndocNotNullValidator;
import nc.bs.bd.psn.psndoc.validator.PsndocRetraitValidator;
import nc.bs.bd.psn.psndoc.validator.PsndocUniqueValidator;
import nc.bs.bd.psn.psndoc.validator.PsnjobUniqueValidator;
import nc.bs.bd.psn.psndoc.validator.PsnjobValidator;
import nc.bs.businessevent.EventDispatcher;
import nc.bs.businessevent.bd.BDCommonEvent;
import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.NCLocator;
import nc.bs.uif2.validation.IValidationService;
import nc.bs.uif2.validation.ValidationException;
import nc.bs.uif2.validation.ValidationFrameworkUtil;
import nc.bs.uif2.validation.Validator;
import nc.itf.bd.config.uniquerule.UniqueRuleConst;
import nc.itf.bd.psn.psndoc.IPsndocService;
import nc.itf.bd.psnbankacc.IPsnBankaccQueryService;
import nc.itf.bd.psnbankacc.IPsnBankaccService;
import nc.itf.bd.pub.IBDMetaDataIDConst;
import nc.itf.uap.rbac.IRoleManage;
import nc.itf.uap.rbac.IRoleManageQuery;
import nc.itf.uap.rbac.IUserManage;
import nc.itf.uap.rbac.IUserManageQuery;
import nc.itf.uap.sf.ICreateCorpQueryService;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.login.vo.INCUserTypeConstant;
import nc.md.MDBaseQueryFacade;
import nc.md.model.IBean;
import nc.md.persist.framework.IMDPersistenceQueryService;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.billcode.itf.IBillcodeManage;
import nc.pub.billcode.vo.BillCodeContext;
import nc.vo.bd.meta.BatchOperateVO;
import nc.vo.bd.psn.IPsnConst;
import nc.vo.bd.psn.PsndocExtend;
import nc.vo.bd.psn.PsndocVO;
import nc.vo.bd.psn.PsnjobVO;
import nc.vo.bd.psnbankacc.PsnBankaccUnionVO;
import nc.vo.bd.pub.DistributedAddBaseValidator;
import nc.vo.bd.pub.IPubEnumConst;
import nc.vo.bd.pub.SingleDistributedUpdateValidator;
import nc.vo.ml.LanguageVO;
import nc.vo.ml.MultiLangContext;
import nc.vo.pub.BusinessException;
import nc.vo.pub.SuperVO;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.sm.UserVO;
import nc.vo.uap.rbac.UserGroupVO;
import nc.vo.uap.rbac.role.RoleVO;
import nc.vo.uap.rbac.util.IRbacConst;
import nc.vo.util.BDUniqueRuleValidate;
import nc.vo.util.BDVersionValidationUtil;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 人员档案保存接口实现
 * 
 * @author jiangjuna
 * 
 */
public class PsndocServiceImpl extends SingleBaseService<PsndocVO> implements IPsndocService {
	private IBillcodeManage billcodeManage = null;// 单据编码管理服务
	private IUserManage userManageSerivce = null;// 生成自助用户的服务
	private IRoleManage roleManageSercvice = null;// 生成角色的服务
	private IRoleManageQuery roleManageQuery = null;// 角色查询服务
	private IUserManageQuery userManageQuery = null;// 用户查询服务
	private IPsnBankaccService psnBankaccService = null;
	private IPsnBankaccQueryService psnBankaccQryService = null;
	private BaseDAO dao = null;
	private ICreateCorpQueryService corpqueryService = null;
	private IPsndocService psndocService = null;

	public PsndocServiceImpl() {
		super(IBDMetaDataIDConst.PSNDOC, new String[] { IPsnConst.PSNDOC_PSNJOB });
	}

	@Override
	public void deletePsndoc(PsndocVO vo) throws BusinessException {
		hrEnabledValidate(vo.getPk_org());
		deleteVO(vo);
		// 人员自动编码退号
		BillCodeContext billCodeContext =
				getBillcodeManage().getBillCodeContext(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org());
		// 人员存在编码规则时，退号
		if (billCodeContext != null) {
			getBillcodeManage().returnBillCodeOnDelete(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo.getCode(), vo);
		}
		// 工作信息自动编码退号
		BillCodeContext billCodeContext2 =
				getBillcodeManage().getBillCodeContext(IPsnConst.PSNDOC_PSNJOB, vo.getPk_group(), vo.getPk_org());
		// 人员存在编码规则时，退号
		if (billCodeContext2 != null) {
			getBillcodeManage().returnBillCodeOnDelete(IPsnConst.PSNDOC_PSNJOB, vo.getPk_group(), vo.getPk_org(), vo.getCode(), vo);
		}
	}

	@SuppressWarnings("null")
	@Override
	public PsndocVO insertPsndoc(PsndocVO vo, boolean isCheckRepeat) throws BusinessException {
		if (vo == null)
			return vo;
		hrEnabledValidate(vo.getPk_org());
		BillCodeContext billCodeContext =
				getBillcodeManage().getBillCodeContext(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org());
		boolean sucessed = false;
		IValidationService validateService =
				ValidationFrameworkUtil.createValidationService(new Validator[] { new BDUniqueRuleValidate() });
		while (!sucessed) {
			try {
				if (billCodeContext != null && !billCodeContext.isPrecode()) {
					String billcode =
							getBillcodeManage().getBillCode_RequiresNew(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo);
					vo.setCode(billcode);
				}
				validateService.validate(vo);
				sucessed = true;
				break;
			} catch (BusinessException e) {
				if (billCodeContext != null && UniqueRuleConst.CODEBREAKUNIQUE.equals(e.getErrorCodeString())) {
					getBillcodeManage().AbandonBillCode_RequiresNew(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo.getCode());
					if (!billCodeContext.isPrecode())
						continue;
				}
				throw e;
			}
		}
		// 去掉证件号的空格
		vo.setId(vo.getId().replaceAll(" ", ""));
		IValidationService validatejobService =
				ValidationFrameworkUtil.createValidationService(new Validator[] { new PsnjobUniqueValidator() });
		// 工作信息后编码
		BillCodeContext billCodeContext2 =
				getBillcodeManage().getBillCodeContext(IPsnConst.PSNDOC_PSNJOB, vo.getPk_group(), vo.getPk_org());
		String billcode2 = null;
		PsnjobVO mainjob = null;
		PsnjobVO[] psnjobVOs = vo.getPsnjobs();
		for (PsnjobVO jobvo : psnjobVOs) {
			if (jobvo.getIsmainjob() == UFBoolean.TRUE) {
				mainjob = jobvo;
				break;
			}
		}
		sucessed = false;
		while (!sucessed) {
			try {
				if (billCodeContext2 != null && !billCodeContext2.isPrecode()) {
					billcode2 =
							getBillcodeManage().getBillCode_RequiresNew(IPsnConst.PSNDOC_PSNJOB, mainjob.getPk_group(), mainjob.getPk_org(), mainjob);
					// TODO
					// jobvo.setPsncode(billcode2);
					for (PsnjobVO jobvo : psnjobVOs) {
						jobvo.setPsncode(billcode2);
					}
					vo.setPsnjobs(psnjobVOs);
				}
				validatejobService.validate(vo);
				sucessed = true;
			} catch (BusinessException e) {
				if (UniqueRuleConst.CODEBREAKUNIQUE.equals(e.getErrorCodeString())) {
					getBillcodeManage().AbandonBillCode_RequiresNew(IPsnConst.PSNDOC_PSNJOB, mainjob.getPk_group(), mainjob.getPk_org(), mainjob.getPsncode());
					if (!billCodeContext2.isPrecode())
						continue;
				}
				throw e;
			}
		}
		try {
			// 新增时的加锁操作
			insertlockOperate(vo);
			// try {
			// 逻辑校验
			IValidationService validateServices = ValidationFrameworkUtil.createValidationService(getInsertValidator(isCheckRepeat));
			validateServices.validate(vo);
			// }
			// catch (BusinessException e) {
			// if (billCodeContext != null) {
			// // 编码重复时，弃号
			// if (UniqueRuleConst.CODEBREAKUNIQUE.equals(e
			// .getErrorCodeString())) {
			// getBillcodeManage().AbandonBillCode_RequiresNew(
			// IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(),
			// vo.getPk_org(), vo.getCode());
			// }
			// }
			// throw e;
			// }
			// 设置审计信息
			setInsertAuditInfo(vo);
			// 插入前事件通知
			fireBeforeInsertEvent(vo);
			// 库操作
			String pk = dbInsertVO(vo);
			// 通知更新缓存
			notifyVersionChangeWhenDataInserted(vo);
			// 重新检索出插入的VO
			vo = retrieveVO(pk);
			// 插入事件后通知
			fireAfterInsertEvent(vo);
			// 前编码时提交编码（接口里会判断是否连续）
			if (billCodeContext != null && billCodeContext.isPrecode()) {
				getBillcodeManage().commitPreBillCode(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo.getCode());
			}
			if (billCodeContext2 != null && billCodeContext2.isPrecode()) {
				getBillcodeManage().commitPreBillCode(IPsnConst.PSNDOC_PSNJOB, mainjob.getPk_group(), mainjob.getPk_org(), mainjob.getPsncode());
			}
			writeInsertBusiLog(vo);
		} catch (BusinessException ex) {
			throw ex;
		}
		return vo;
	}

	// /**
	// * 判断是否已启用hr，如果启用，则不允许新增修改，主要是为了控制数据导入
	// *
	// * @param vo
	// * @throws BusinessException
	// */
	// private void enabledValidate(PsndocVO vo) throws BusinessException {
	// boolean flag = getCorpQueryService().isEnabled(vo.getPk_group(),
	// IPsnConst.HR_CODE);
	// if (flag) {
	// throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl
	// .getNCLangRes().getStrByID("10140psn", "010140psn0101")/*
	// * @res
	// * "已启用HR产品，不允许新增或修改人员档案"
	// */);
	// }
	// }
	@SuppressWarnings("boxing")
	@Override
	protected String dbInsertVO(PsndocVO vo) throws BusinessException {
		// 工作信息为空时，封存数据
		if (vo.getPsnjobs() != null && vo.getPsnjobs().length > 0) {
			vo.setEnablestate(IPubEnumConst.ENABLESTATE_ENABLE);
		} else {
			vo.setEnablestate(IPubEnumConst.ENABLESTATE_DISABLE);
		}
		return super.dbInsertVO(vo);
	}

	/**
	 * 定义新增校验类
	 * 
	 * @param isAlart
	 *            是否校验提示性字段
	 * @return
	 * @throws BusinessException
	 */
	protected Validator[] getInsertValidator(boolean isCheckRepeat) throws BusinessException {
		IBean bean = MDBaseQueryFacade.getInstance().getBeanByID(getMDId());
		// 非空校验
		PsndocNotNullValidator notNullValidator = new PsndocNotNullValidator();
		PsndocRetraitValidator retraitValidator = new PsndocRetraitValidator();
		// 工作信息校验
		PsnjobValidator psnjobValidator = new PsnjobValidator(null);
		// 唯一性校验
		PsndocUniqueValidator uniqueValidator = new PsndocUniqueValidator(isCheckRepeat, bean);
		// 工作信息子表唯一性校验
		PsnjobUniqueValidator psnjobUniqueValidator = new PsnjobUniqueValidator();
		RefPkExistsValidator refPkExistsValidator = new RefPkExistsValidator();
		refPkExistsValidator.addChildAttrs("psnjobs", new String[] { PsnjobVO.PK_PSNCL });
		DistributedAddBaseValidator distributAddValidator = new DistributedAddBaseValidator();
		PsndocHREnableValidator hrEnableValidator = new PsndocHREnableValidator();
		return new Validator[] { notNullValidator, retraitValidator, psnjobValidator, uniqueValidator, psnjobUniqueValidator, refPkExistsValidator, hrEnableValidator, distributAddValidator };
	}

	@Override
	protected PsndocVO retrieveVO(String pk) throws BusinessException {
		if (StringUtils.isBlank(pk))
			return null;
		PsndocVO newVO = getMDQueryService().queryBillOfVOByPK(PsndocVO.class, pk, false);
		return newVO;
	}

	@Override
	public PsndocVO updatePsndoc(PsndocVO vo, boolean isCheckRepeat) throws BusinessException {
		if (vo == null)
			return vo;
		// 检查是否启用了HR模块
		hrEnabledValidate(vo.getPk_org());
		// 去掉证件号的空格
		String id = vo.getId().replaceAll(" ", "");
		vo.setId(id);
		try {
			// 更新时的加锁操作
			updatelockOperate(vo);
			// 校验版本
			BDVersionValidationUtil.validateSuperVO(vo);
			// 获取更新前的OldVOs
			PsndocVO oldVO = retrieveVO(vo.getPrimaryKey());
			// 业务校验逻辑
			IValidationService validateService = ValidationFrameworkUtil.createValidationService(getUpdateValidator(oldVO, isCheckRepeat));
			validateService.validate(vo);
			// 设置审计信息
			setUpdateAuditInfo(vo);
			// 更新前事件处理
			fireBeforeUpdateEvent(oldVO, vo);
			// 库操作
			dbUpdateVO(vo);
			// 更新缓存
			notifyVersionChangeWhenDataUpdated(vo);
			// 重新检索出新数据
			vo = retrieveVO(vo.getPrimaryKey());
			// 更新后事件通知
			fireAfterUpdateEvent(oldVO, vo);
			// 业务日志
			writeUpdatedBusiLog(oldVO, vo);
		} catch (BusinessException ex) {
			throw ex;
		}
		return vo;
	}

	/**
	 * 定义修改校验类
	 * 
	 * @param oldVO
	 * @param isAlart
	 *            是否校验提示性字段
	 * @return
	 * @throws BusinessException
	 */
	protected Validator[] getUpdateValidator(PsndocVO oldVO, boolean isCheckRepeat) throws BusinessException {
		IBean bean = MDBaseQueryFacade.getInstance().getBeanByID(getMDId());
		// 非空校验
		PsndocNotNullValidator notNullValidator = new PsndocNotNullValidator();
		PsndocRetraitValidator retraitValidator = new PsndocRetraitValidator();
		// 工作信息校验
		PsnjobValidator psnjobValidator = new PsnjobValidator(oldVO);
		// 唯一性校验
		PsndocUniqueValidator uniqueValidator = new PsndocUniqueValidator(isCheckRepeat, bean);
		// 工作信息子表唯一性校验
		PsnjobUniqueValidator psnjobUniqueValidator = new PsnjobUniqueValidator();
		RefPkExistsValidator refPkExistsValidator = new RefPkExistsValidator();
		refPkExistsValidator.addChildAttrs("psnjob", new String[] { PsnjobVO.PK_PSNCL });
		PsndocHREnableValidator hrEnableValidator = new PsndocHREnableValidator();
		return new Validator[] { notNullValidator, retraitValidator, psnjobValidator, uniqueValidator, psnjobUniqueValidator, refPkExistsValidator, hrEnableValidator, new SingleDistributedUpdateValidator() };
	}

	@Override
	public PsndocVO disEnablePsndoc(PsndocVO vo) throws BusinessException {
		return super.disableSingleVO(vo);
	}

	@Override
	public PsndocVO enablePsndoc(PsndocVO vo) throws BusinessException {
		return super.enableSingleVO(vo);
	}

	@Override
	protected Validator[] getEnableValidator() {
		PsndocEnableValidator enableValidator = new PsndocEnableValidator();
		return new Validator[] { enableValidator, new SingleDistributedUpdateValidator() };
	}

	protected void writeTransferBusiLog(PsndocVO vo) throws BusinessException {
		getBusiLogUtil().writeBusiLog("Transfer", null, vo);
	}

	/*
	 * 生成自助用户
	 * 
	 * @see
	 * nc.itf.bd.psn.psndoc.IPsndocService#createUser(nc.vo.bd.psn.PsndocVO[])
	 */
	@Override
	public PsndocExtend[] createUser(Object[] vo, String pk_userGroup) throws BusinessException {
		if (vo == null || vo.length == 0)
			return null;
		UFDate enableDate = new UFDate(true);
		PsndocExtend[] psndocExtends = new PsndocExtend[vo.length];// 当发现有校验错误的时候开始返回带有信息的人员扩展类
		List<PsndocExtend> listWithSameCode = new ArrayList<PsndocExtend>();
		UserGroupVO userGroupVo = (UserGroupVO) getDao().retrieveByPK(UserGroupVO.class, pk_userGroup, new String[] { UserGroupVO.PK_ORG });
		// 预置角色数据已经写入数据库中
		Map<String, RoleVO> groupid_rolevo_map = new HashMap<String, RoleVO>();
		BatchOperateVO batchvo = new BatchOperateVO();
		List<UserVO> uservos = new ArrayList<UserVO>();
		for (int i = 0; i < vo.length; i++) {
			PsndocVO psndocVO = (PsndocVO) vo[i];
			psndocExtends[i] = new PsndocExtend();
			if (isWithSameCode(psndocVO.getCode())) {
				psndocExtends[i].setPsnDocVo(psndocVO);
				psndocExtends[i].setMessage(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("10140psn", "010140psn0097"));// 用户编码重复
				listWithSameCode.add(psndocExtends[i]);
				continue;
			}
			UserVO uservo = createNewUservo(pk_userGroup, enableDate, userGroupVo, psndocVO);
			uservos.add(uservo);
			getBusiLogUtil().writeBusiLog("CreateUser", null, psndocVO);
		}
		batchvo.setAddObjs(uservos.toArray(new UserVO[0]));
		batchvo = getUserManageSerivce().userbatchSave(batchvo);
		// 生成用户后，需再关联角色
		for (int i = 0; i < batchvo.getAddObjs().length; i++) {
			UserVO uservo = (UserVO) batchvo.getAddObjs()[i];
			RoleVO rolevo = groupid_rolevo_map.get(uservo.getPk_group());
			if (rolevo == null) {
				rolevo = getIRoleManageQuery().queryRoleByCode("SF_Role", uservo.getPk_group());
				groupid_rolevo_map.put(uservo.getPk_group(), rolevo);
			}
			if (rolevo == null) {
				continue;
			}
			getIRoleManage().assignRole2User(uservo.getCuserid(), new RoleVO[] { rolevo }, uservo.getPk_org(), enableDate, null);
		}
		return listWithSameCode.size() == 0 ? null : listWithSameCode.toArray(new PsndocExtend[0]);
	}

	private UserVO createNewUservo(String pk_userGroup, UFDate enableDate, UserGroupVO userGroupVo, PsndocVO psndocVO) {
		LanguageVO currentLang = MultiLangContext.getInstance().getCurrentLangVO();
		UserVO uservo = new UserVO();
		uservo.setBase_doc_type(new Integer(0));
		uservo.setPk_base_doc(psndocVO.getPk_psndoc());
		uservo.setPk_group(psndocVO.getPk_group());
		uservo.setPk_org(userGroupVo == null ? null : userGroupVo.getPk_org());
		uservo.setUser_code(psndocVO.getCode());
		uservo.setUser_name(psndocVO.getName());
		uservo.setUser_type(new Integer(INCUserTypeConstant.USER_TYPE_USER));
		// 新建用户的时候得需要有这两个字段
		uservo.setIdentityverifycode("staticpwd");
		uservo.setContentlang(currentLang.getPk_multilang());
		uservo.setPwdlevelcode("update");
		uservo.setEnablestate(new Integer(IPubEnumConst.ENABLESTATE_ENABLE));
		uservo.setPk_usergroupforcreate(pk_userGroup);
		uservo.setFormat(IRbacConst.PK_ZH_CN_FROMAT);
		uservo.setAbledate(enableDate);
		return uservo;
	}

	@Override
	public PsndocVO deletePsnJob(PsndocVO vo, String pk_org) throws BusinessException {
		if (vo == null)
			return vo;
		try {
			// 更新时的加锁操作
			updatelockOperate(vo);
			// 校验版本
			BDVersionValidationUtil.validateSuperVO(vo);
			// 获取更新前的OldVOs
			PsndocVO oldVO = retrieveVO(vo.getPrimaryKey());
			getDao().deleteByClause(PsnjobVO.class, "pk_org='" + pk_org + "' and pk_psndoc='" + vo.getPk_psndoc() + "'");
			// 设置审计信息
			setUpdateAuditInfo(vo);
			fireBeforeDeleteJobEvent(oldVO, vo);
			// 更新前事件处理
			fireBeforeUpdateEvent(oldVO, vo);
			// 更新缓存
			notifyVersionChangeWhenDataUpdated(vo);
			// 重新检索出新数据
			vo = retrieveVO(vo.getPrimaryKey());
			// 更新后事件通知
			fireAfterUpdateEvent(oldVO, vo);
			writeUpdatedBusiLog(oldVO, vo);
		} catch (ValidationException e) {
			throw new BusinessException(e.getMessage());
		}
		return vo;
	}

	protected void fireBeforeDeleteJobEvent(PsndocVO oldVO, PsndocVO vo) throws BusinessException {
		EventDispatcher.fireEvent(new BDCommonEvent(getMDId(), "1097", new Object[] { oldVO }, vo));
	}

	@SuppressWarnings("boxing")
	@Override
	public void transferUser(PsndocVO vo, boolean innerGroup) throws BusinessException {
		if (vo == null)
			return;
		UserVO uservo = getIUserManageQuery().queryUserVOByPsnDocID(vo.getPk_psndoc());
		if (uservo == null) {
			return;
		}
		if (innerGroup) {
			// 集团内调动人员时，直接停用用户
			if (uservo.getEnablestate() != IPubEnumConst.ENABLESTATE_DISABLE)
				getUserManageSerivce().disableUser(uservo);
		} else {
			// 跨集团调动人员时，调动相应用户
			getUserManageSerivce().migrateUserToGroupWithDocRelation(new UserVO[] { uservo }, vo.getPk_group());
		}
	}

	@Override
	public PsndocVO transferPsndoc(PsndocVO vo, boolean keepSrcJob) throws BusinessException {
		PsnjobVO[] vos = vo.getPsnjobs();
		// 获取更新前的OldVOs
		PsndocVO oldVO = retrieveVO(vo.getPrimaryKey());
		if (vos != null && vos.length > 0) {
			// 20170626 tsy 设置正确的状态
			if (vos[0].getPrimaryKey() != null) {
				vos[0].setStatus(VOStatus.UPDATED);
			} else {
				vos[0].setStatus(VOStatus.NEW);
			}
			vos[1].setStatus(VOStatus.UPDATED);
			if ((vos.length == 3) && (vos[2] != null)) {
				vos[2].setStatus(VOStatus.DELETED);
			}
			// 20170626 end
			// 若不保存原业务单元的工作信息，把工作信息的状态改为删除
			if (keepSrcJob) {
				for (int i = 0; i < vos.length; i++) {
					if (vos[i].getPk_org().equals(oldVO.getPk_org())) {
						vos[i].setStatus(VOStatus.DELETED);
					}
				}
			}
		}
		BillCodeContext billCodeContext =
				getBillcodeManage().getBillCodeContext(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org());
		if (billCodeContext != null) {
			getBillcodeManage().returnBillCodeOnDelete(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo.getCode(), vo);
		}
		boolean sucessed = false;
		IValidationService validateService =
				ValidationFrameworkUtil.createValidationService(new Validator[] { new BDUniqueRuleValidate() });
		while (!sucessed) {
			try {
				if (billCodeContext != null && !billCodeContext.isPrecode()) {
					String billcode =
							getBillcodeManage().getBillCode_RequiresNew(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo);
					vo.setCode(billcode);
				}
				validateService.validate(vo);
				sucessed = true;
				break;
			} catch (BusinessException e) {
				if (billCodeContext != null && UniqueRuleConst.CODEBREAKUNIQUE.equals(e.getErrorCodeString())) {
					getBillcodeManage().AbandonBillCode_RequiresNew(IPsnConst.PSNDOC_BILLCODE, vo.getPk_group(), vo.getPk_org(), vo.getCode());
					if (!billCodeContext.isPrecode())
						continue;
				}
				throw e;
			}
		}
		// 更新对应个人银行账户所属组织
		updatePsnBankaccInfo(getPsnBankacc(vo.getPk_psndoc()), vo.getPk_org());
		// 更新时的加锁操作
		updatelockOperate(vo);
		// 校验版本
		BDVersionValidationUtil.validateSuperVO(vo);
		// 20170626 tsy 同步是否主职
		if ((oldVO != null) && (oldVO.getPk_org() != vo.getPk_org()) && (vos != null) && (vos.length > 0)) {
			vos[0].setIsmainjob(UFBoolean.TRUE);
			vos[1].setIsmainjob(UFBoolean.FALSE);
		}
		// 20170626 end
		// 业务校验逻辑
		updateValidateVO(oldVO, vo);
		// 设置审计信息
		setUpdateAuditInfo(vo);
		if (keepSrcJob) {
			fireBeforeDeleteJobEvent(oldVO, vo);
		}
		// 更新前事件处理，缺调整业务单元的事件通知
		fireBeforeUpdateEvent(oldVO, vo);
		// 库操作
		dbUpdateVO(vo);
		// 更新缓存
		notifyVersionChangeWhenDataUpdated(vo);
		// 重新检索出新数据
		vo = retrieveVO(vo.getPrimaryKey());
		// 更新后事件通知
		fireAfterUpdateEvent(oldVO, vo);
		// 业务日志
		writeTransferBusiLog(vo);
		return vo;
	}

	private IMDPersistenceQueryService getMDQueryService() {
		return MDPersistenceService.lookupPersistenceQueryService();
	}

	// 检查编码是否重复
	private boolean isWithSameCode(String code) throws BusinessException {
		if (code == null)
			return false;
		String sql = "select 1 from sm_user where user_code=?";
		SQLParameter param = new SQLParameter();
		param.addParam(code);
		Object uservo = getDao().executeQuery(sql, param, new ColumnProcessor());
		return uservo != null;
	}

	private IBillcodeManage getBillcodeManage() {
		if (this.billcodeManage == null) {
			this.billcodeManage = NCLocator.getInstance().lookup(IBillcodeManage.class);
		}
		return this.billcodeManage;
	}

	private IUserManage getUserManageSerivce() {
		if (this.userManageSerivce == null)
			this.userManageSerivce = NCLocator.getInstance().lookup(IUserManage.class);
		return this.userManageSerivce;
	}

	private ICreateCorpQueryService getCorpQueryService() {
		if (this.corpqueryService == null) {
			this.corpqueryService = NCLocator.getInstance().lookup(ICreateCorpQueryService.class);
		}
		return this.corpqueryService;
	}

	private IRoleManage getIRoleManage() {
		if (this.roleManageSercvice == null)
			this.roleManageSercvice = NCLocator.getInstance().lookup(IRoleManage.class);
		return this.roleManageSercvice;
	}

	private IRoleManageQuery getIRoleManageQuery() {
		if (this.roleManageQuery == null)
			this.roleManageQuery = NCLocator.getInstance().lookup(IRoleManageQuery.class);
		return this.roleManageQuery;
	}

	private IUserManageQuery getIUserManageQuery() {
		if (this.userManageQuery == null)
			this.userManageQuery = NCLocator.getInstance().lookup(IUserManageQuery.class);
		return this.userManageQuery;
	}

	private BaseDAO getDao() {
		if (this.dao == null)
			this.dao = new BaseDAO();
		return this.dao;
	}

	@Override
	public boolean isHREnabled(String pk_org) throws BusinessException {
		// 默认不启用，装HR模块后，HR模块后台服务覆盖此方法
		return false;
	}

	@Override
	public Map<String, UFBoolean> isHREnabled(String[] pk_orgs) throws BusinessException {
		Map<String, UFBoolean> map = new HashMap<String, UFBoolean>();
		for (String pk_org : pk_orgs) {
			map.put(pk_org, UFBoolean.valueOf(getPsndocService().isHREnabled(pk_org)));
		}
		return map;
	}

	private void hrEnabledValidate(String pk_org) throws BusinessException {
		if (getPsndocService().isHREnabled(pk_org)) {
			throw new BusinessException(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes().getStrByID("10140psn", "010140psn0103")/*
																														 * @
																														 * res
																														 * "当前组织已启用HR，请至HR处进行人员维护"
																														 */);
		}
	}

	private IPsndocService getPsndocService() {
		if (this.psndocService == null) {
			this.psndocService = NCLocator.getInstance().lookup(IPsndocService.class);
		}
		return this.psndocService;
	}

	private void updatePsnBankaccInfo(PsnBankaccUnionVO[] psnbankaccs, String pk_org) throws BusinessException {
		if (ArrayUtils.isEmpty(psnbankaccs)) {
			return;
		}
		for (PsnBankaccUnionVO idx : psnbankaccs) {
			idx.getPsnbankaccVO().setPk_org(pk_org);
			getPsnBankaccService().updatePsnBankacc(idx);
		}
	}

	private PsnBankaccUnionVO[] getPsnBankacc(String pk_psndoc) throws BusinessException {
		String condition = " pk_psndoc = '" + pk_psndoc + "'";
		return getPsnBankaccQryService().queryPsnBankaccUnionVOsByCon(condition, false);
	}

	public IPsnBankaccQueryService getPsnBankaccQryService() {
		if (psnBankaccQryService == null) {
			psnBankaccQryService = NCLocator.getInstance().lookup(IPsnBankaccQueryService.class);
		}
		return psnBankaccQryService;
	}

	private IPsnBankaccService getPsnBankaccService() {
		if (psnBankaccService == null) {
			psnBankaccService = NCLocator.getInstance().lookup(IPsnBankaccService.class);
		}
		return psnBankaccService;
	}
}
