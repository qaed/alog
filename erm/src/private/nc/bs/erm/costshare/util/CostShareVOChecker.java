package nc.bs.erm.costshare.util;

import java.util.ArrayList;
import java.util.List;
import nc.bs.erm.util.ErUtil;
import nc.vo.arap.transaction.DataValidateException;
import nc.vo.cmp.util.StringUtils;
import nc.vo.erm.costshare.AggCostShareVO;
import nc.vo.erm.costshare.CShareDetailVO;
import nc.vo.erm.costshare.CostShareVO;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.ValidationException;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class CostShareVOChecker {
	private List<String> notRepeatFields;

	public CostShareVOChecker() {
	}

	public void checkSave(AggCostShareVO vo) throws BusinessException {
		prepare(vo);

		if (!((CostShareVO) vo.getParentVO()).getBillstatus().equals(Integer.valueOf(0))) {
			checkHeader(vo);
			checkChildren(vo);
			checkIsCloseAcc(vo);
		}
	}

	public static void checkIsCloseAcc(AggCostShareVO aggVo) throws BusinessException {
		CostShareVO head = (CostShareVO) aggVo.getParentVO();
		String moduleCode = "2011";
		String pk_org = head.getPk_org();
		UFDate date = head.getBilldate();
		if (ErUtil.isOrgCloseAcc(moduleCode, pk_org, date)) {
			throw new DataValidateException(NCLangRes4VoTransl.getNCLangRes().getStrByID("expensepub_0", "02011002-0146"));
		}
	}

	private void checkHeader(AggCostShareVO vo) throws ValidationException {
		CostShareVO header = (CostShareVO) vo.getParentVO();
		// 20170510 tsy 去除"小于0不进行分摊"校验
		/*
		 * if (header.getYbje().compareTo(UFDouble.ZERO_DBL) <= 0) { throw new
		 * ValidationException
		 * (NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0",
		 * "0upp2012V575-0070")); }
		 */
		// 20170510
	}

	private void prepare(AggCostShareVO vo) {
		prepareForNullJe(vo);
	}

	private void prepareForNullJe(AggCostShareVO vo) {
		CircularlyAccessibleValueObject parentVO = vo.getParentVO();

		String[] jeField = { "total", "ybje", "bbje" };

		String[] bodyJeField = { "assume_amount", "bbje" };

		for (String field : jeField) {
			if (parentVO.getAttributeValue(field) == null) {
				parentVO.setAttributeValue(field, UFDouble.ZERO_DBL);
			}
		}

		for (String field : bodyJeField) {
			CircularlyAccessibleValueObject[] childrenVO = vo.getChildrenVO();
			if (childrenVO != null) {
				for (CircularlyAccessibleValueObject item : childrenVO) {
					if (item.getAttributeValue(field) == null) {
						item.setAttributeValue(field, UFDouble.ZERO_DBL);
					}
				}
			}
		}
	}

	private void checkChildren(AggCostShareVO vo) throws ValidationException {
		CShareDetailVO[] cShareVos = (CShareDetailVO[]) vo.getChildrenVO();

		if ((cShareVos == null) || (cShareVos.length <= 0)) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0071"));
		}

		CostShareVO parentVO = (CostShareVO) vo.getParentVO();
		// 20170712 tsy 取表头合计金额
		// UFDouble total = parentVO.getYbje();
		UFDouble total = parentVO.getTotal();
		// 20170712 end
		UFDouble amount = UFDouble.ZERO_DBL;
		List<String> controlKeys = new ArrayList();
		StringBuffer controlKey = null;
		String[] attributeNames = cShareVos[0].getAttributeNames();

		for (int i = 0; i < cShareVos.length; i++) {
			if (cShareVos[i].getStatus() != 3) {
				UFDouble shareAmount = cShareVos[i].getAssume_amount();
				UFDouble share_ratio = cShareVos[i].getShare_ratio();
				if (shareAmount == null) {
					shareAmount = UFDouble.ZERO_DBL;
				}
				// 20170510 tsy 取消"小于0不可以分摊"校验
				// if (shareAmount.compareTo(UFDouble.ZERO_DBL) <= 0) {
				// throw new
				// ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0",
				// "0upp2012V575-0072"));
				// }
				// 20170510 end

				if (share_ratio.compareTo(UFDouble.ZERO_DBL) < 0) {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0087"));
				}

				amount = amount.add(cShareVos[i].getAssume_amount());

				controlKey = new StringBuffer();

				for (int j = 0; j < attributeNames.length; j++) {
					if ((getNotRepeatFields().contains(attributeNames[j])) || (attributeNames[j].startsWith("defitem"))) {
						controlKey.append(cShareVos[i].getAttributeValue(attributeNames[j]));
					}
				}

				if (!controlKeys.contains(controlKey.toString())) {
					controlKeys.add(controlKey.toString());
				} else {
					throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0073"));
				}
			}
		}

		if (total.toDouble().compareTo(amount.toDouble()) != 0) {
			throw new ValidationException(NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0074"));
		}
	}

	public void checkDelete(AggCostShareVO[] vos) throws BusinessException {
		if (vos == null) {
			return;
		}

		for (int i = 0; i < vos.length; i++) {
			AggCostShareVO aggCostShareVO = vos[i];
			CostShareVO head = (CostShareVO) aggCostShareVO.getParentVO();
			String msg = checkBillStatus(head.getBillstatus().intValue(), 6, new int[] { 1, 0 });

			if ((head.getEffectstate() != null) && (head.getEffectstate().equals(Integer.valueOf(1)))) {
				msg = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0075");
			}

			if (!StringUtils.isNullWithTrim(msg)) {
				throw new DataValidateException(msg);
			}

			checkIsCloseAcc(vos[i]);
		}
	}

	public void checkApprove(AggCostShareVO[] vos, UFDate buDate) throws DataValidateException {
		if (vos == null) {
			return;
		}

		for (int i = 0; i < vos.length; i++) {
			AggCostShareVO aggCostShareVO = vos[i];
			CostShareVO head = (CostShareVO) aggCostShareVO.getParentVO();
			String msg = checkBillStatus(head.getBillstatus().intValue(), 3, new int[] { 1 });
			if (StringUtils.isNullWithTrim(msg)) {
				msg = "";
			} else {
				msg = msg + "\n";
			}
			if (head.getBilldate().after(buDate)) {
				msg = msg + NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0111");
			}
			if (!StringUtils.isNullWithTrim(msg)) {
				throw new DataValidateException(msg);
			}
		}
	}

	public void checkunApprove(AggCostShareVO[] vos) throws DataValidateException, ValidationException {
		if (vos == null) {
			return;
		}

		for (int i = 0; i < vos.length; i++) {
			AggCostShareVO aggCostShareVO = vos[i];
			CostShareVO head = (CostShareVO) aggCostShareVO.getParentVO();
			String msg = checkBillStatus(head.getBillstatus().intValue(), 4, new int[] { 3 });
			if (!StringUtils.isNullWithTrim(msg)) {
				throw new DataValidateException(msg);
			}
		}
	}

	public static String checkBillStatus(int djzt, int operation, int[] statusAllowed) {
		String strMessage = null;

		for (int i = 0; i < statusAllowed.length; i++) {
			if (statusAllowed[i] == djzt) {
				return null;
			}
		}
		String operationName = null;

		switch (operation) {
			case 3:
				operationName = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0076");
				break;
			case 4:
				operationName = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0077");
				break;
			case 6:
				operationName = NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000307");
				break;
		}

		switch (djzt) {
			case 0:
				strMessage = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0126");
				break;

			case 1:
				strMessage = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0078");
				break;

			case 3:
				strMessage = NCLangRes4VoTransl.getNCLangRes().getStrByID("upp2012v575_0", "0upp2012V575-0079");
				break;
			case 2:
			default:
				strMessage = NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000266");
		}

		return strMessage + NCLangRes4VoTransl.getNCLangRes().getStrByID("2011", "UPP2011-000267") + operationName;
	}

	public List<String> getNotRepeatFields() {
		if (this.notRepeatFields == null) {
			this.notRepeatFields = new ArrayList();
			this.notRepeatFields.add("assume_org");
			this.notRepeatFields.add("assume_dept");
			this.notRepeatFields.add("pk_iobsclass");
			this.notRepeatFields.add("pk_pcorg");
			this.notRepeatFields.add("pk_resacostcenter");
			this.notRepeatFields.add("jobid");
			this.notRepeatFields.add("projecttask");
			this.notRepeatFields.add("pk_checkele");
			this.notRepeatFields.add("customer");
			this.notRepeatFields.add("hbbm");
		}
		return this.notRepeatFields;
	}
}