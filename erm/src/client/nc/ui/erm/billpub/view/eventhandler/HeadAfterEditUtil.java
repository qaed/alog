package nc.ui.erm.billpub.view.eventhandler;

import java.util.ArrayList;
import java.util.List;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.desktop.ui.WorkbenchEnvironment;
import nc.itf.org.IOrgVersionQryService;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.er.util.BXUiUtil;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.remote.UserBankAccVoCall;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillData;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillModel;
import nc.vo.bd.bankaccount.BankAccSubVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.util.StringUtils;
import nc.vo.fipub.exception.ExceptionHandler;
import nc.vo.pub.BusinessException;
import nc.vo.uif2.LoginContext;
import nc.vo.vorg.OrgVersionVO;

public class HeadAfterEditUtil {
	private ErmBillBillForm editor = null;

	public HeadAfterEditUtil(ErmBillBillForm editor) {
		this.editor = editor;
	}

	private BillCardPanel getBillCardPanel() {
		return editor.getBillCardPanel();
	}

	public String getHeadItemStrValue(String itemKey) {
		BillItem headItem = getBillCardPanel().getHeadItem(itemKey);
		return headItem == null ? null : (String) headItem.getValueObject();
	}

	public void initPayentityItems(boolean isEdit) {
		initItemsBelong(editor.getOrgRefFields("pk_org"), editor.getAllOrgRefFields(), "pk_org", null, isEdit);
	}

	public void initUseEntityItems(boolean isEdit) {
		initItemsBelong(editor.getOrgRefFields("dwbm"), editor.getAllOrgRefFields(), "dwbm", null, isEdit);
	}

	public void initCostentityItems(boolean isEdit) {
		initItemsBelong(editor.getOrgRefFields("fydwbm"), editor.getAllOrgRefFields(), "fydwbm", null, isEdit);
	}

	public void initPayorgentityItems(boolean isEdit) {
		initItemsBelong(editor.getOrgRefFields(JKBXHeaderVO.PK_PAYORG), editor.getAllOrgRefFields(), JKBXHeaderVO.PK_PAYORG, null, isEdit);
	}

	public void initItemsBelong(List<String> costentity_billitems, List<String> allitems, String key, Object fydwbm, boolean isEdit) {
		if (fydwbm == null) {
			fydwbm = getHeadValue(key);
		}
		String fyPkCorp = fydwbm == null ? null : fydwbm.toString();
		for (String item : costentity_billitems) {
			if (!item.equals(key)) {

				BillItem[] headItems = getItemsById(item);
				if (headItems != null) {

					for (BillItem headItem : headItems)
						if (headItem != null) {
							String refType = headItem.getRefType();
							if ((refType != null) && (!refType.equals("")) && (headItem.getComponent() != null) && ((headItem.getComponent() instanceof UIRefPane)))
								try {
									UIRefPane ref = (UIRefPane) headItem.getComponent();

									boolean isInitGroup = false;
									isInitGroup =
											((ErmBillBillManageModel) editor.getModel()).getContext().getNodeCode().equals("20110CBSG");

									if (((!isInitGroup) && ((fyPkCorp == null) || (fyPkCorp.equals("")))) || (!headItem.isEnabled())) {
										ref.setEnabled(false);

									} else if (!ref.isEnabled()) {
										ref.setEnabled(true);
									}

									if (((fyPkCorp == null) || (fyPkCorp.equals(""))) && (!"zy".equals(headItem.getKey()))) {
										headItem.setValue(null);
									}

									AbstractRefModel model = ref.getRefModel();
									if (model != null) {
										model.setPk_org(fyPkCorp);
									}
									if (isEdit) {
										if (headItem.getPos() == 0) {
											if (!"zy".equals(headItem.getKey())) {
												headItem.setValue(null);
											}
										} else if (headItem.getPos() == 1) {
											String tableCode = headItem.getTableCode();
											int rowCount = getBillCardPanel().getBillModel(tableCode).getRowCount();
											for (int i = 0; i < rowCount; i++) {
												getBillCardPanel().setBodyValueAt(null, i, headItem.getKey(), tableCode);
											}
										}
									}
								} catch (ClassCastException e) {
									ExceptionHandler.consume(e);
								}

							// 支付单位编辑后事件，带出默认付款银行帐号 add by LinZB 2016-12-21
							if (isEdit && "fkyhzh".equals(item)) {
								try {
									String pk_org_v = (String) getHeadValue("pk_payorg_v");
									OrgVersionVO orgV =
											NCLocator.getInstance().lookup(IOrgVersionQryService.class).getOrgVersionVOByVID(pk_org_v);
									headItem.setValue(orgV == null ? null : orgV.getDef6());
								} catch (BusinessException e) {
									Logger.error(e.getMessage());
									e.printStackTrace();
								}
							}// end
						}
				}
			}
		}
		if (!isEdit) {
			String[] tables = getBillCardPanel().getBillData().getBodyTableCodes();
			for (String tab : tables) {
				if ((tab == null) || (!tab.equals("er_cshare_detail"))) {

					BillItem[] bodyItems = getBillCardPanel().getBillData().getBodyShowItems(tab);

					if (bodyItems != null) {
						List<BillItem> list = new ArrayList();
						for (BillItem bodyItem : bodyItems) {
							boolean flag =
									(costentity_billitems.contains(bodyItem.getKey())) || ((bodyItem.getIDColName() != null) && (costentity_billitems.contains(bodyItem.getIDColName())));

							boolean fflag =
									(allitems.contains(bodyItem.getKey())) || ((bodyItem.getIDColName() != null) && (allitems.contains(bodyItem.getIDColName())));

							if ((flag) || ((key.equals("pk_org")) && (!fflag))) {
								list.add(bodyItem);
							}
						}

						initAllitemsToCurrcorp((BillItem[]) list.toArray(new BillItem[0]), fyPkCorp);
					}
				}
			}
		}
	}

	private void initAllitemsToCurrcorp(BillItem[] headItems, String pk_org) {
		boolean isInitGroup = false;
		isInitGroup = ((ErmBillBillManageModel) editor.getModel()).getContext().getNodeCode().equals("20110CBSG");

		for (BillItem headItem : headItems) {
			String refType = headItem.getRefType();
			if ((!headItem.getKey().equals("dwbm")) && (!headItem.getKey().equals("fydwbm")) && (!headItem.getKey().equals("pk_org"))) {

				if ((refType != null) && (!refType.equals("")) && (headItem.getComponent() != null) && ((headItem.getComponent() instanceof UIRefPane))) {
					try {
						UIRefPane ref = (UIRefPane) headItem.getComponent();
						AbstractRefModel refModel = ref.getRefModel();
						if (refModel != null) {
							if ((pk_org == null) && (!isInitGroup)) {
								ref.setEnabled(false);
								ref.setValue(null);
							} else if ((refModel.getPk_org() == null) || (!refModel.getPk_org().equals(pk_org))) {
								ref.setPk_org(pk_org);
								ref.setValue(null);
								if (!ref.isEnabled())
									ref.setEnabled(true);
							}
						}
					} catch (ClassCastException e) {
						ExceptionHandler.consume(e);
					}
				}
			}
		}
	}

	public void editSkyhzh(boolean autotake, String pk_org) throws BusinessException {
		BillItem headItem = getBillCardPanel().getHeadItem("receiver");
		String jkbxr = getHeadItemStrValue("jkbxr");
		String receiver = headItem == null ? null : (String) headItem.getValueObject();
		if (!StringUtils.isNullWithTrim(receiver)) {
			jkbxr = receiver;
		}
		if (jkbxr == null)
			return;
		if (autotake) {
			try {
				String key = UserBankAccVoCall.USERBANKACC_VOCALL + BXUiUtil.getPk_psndoc();
				if (WorkbenchEnvironment.getInstance().getClientCache(key) != null) {
					BankAccSubVO[] vos = (BankAccSubVO[]) WorkbenchEnvironment.getInstance().getClientCache(key);
					if ((vos != null) && (vos.length > 0) && (vos[0] != null)) {
						setHeadValue("skyhzh", vos[0].getPk_bankaccsub());
					}
				}
			} catch (Exception e) {
				setHeadValue("skyhzh", "");
			}
		}
	}

	protected BillItem[] getItemsById(String item) {
		if ((item.equals("szxmid")) || (item.equals("jobid")) || (item.equals("jkbxr")) || (item.equals("cashproj")) || (item.equals("projecttask")) || (item.equals("pk_checkele")) || (item.equals(JKBXHeaderVO.PK_RESACOSTCENTER)) || (item.equals("pk_pcorg")) || (item.equals(JKBXHeaderVO.PK_PCORG_V)) || (item.startsWith("defitem"))) {

			String[] tables = getBillCardPanel().getBillData().getBodyTableCodes();
			List<BillItem> results = new ArrayList();
			if (getBillCardPanel().getHeadItem(item) != null) {
				results.add(getBillCardPanel().getHeadItem(item));
			}
			for (String tab : tables) {
				BillItem[] bodyItems = getBillCardPanel().getBillData().getBodyItemsForTable(tab);
				if (bodyItems != null) {

					for (BillItem key : bodyItems) {
						if ((key.getKey().equals(item)) || ((key.getIDColName() != null) && (key.getIDColName().equals(item))))
							results.add(key);
					}
				}
			}
			return (BillItem[]) results.toArray(new BillItem[0]);
		}
		return new BillItem[] { getBillCardPanel().getHeadItem(item) };
	}

	protected Object getHeadValue(String key) {
		BillItem headItem = getBillCardPanel().getHeadItem(key);
		if (headItem == null) {
			headItem = getBillCardPanel().getTailItem(key);
		}
		if (headItem == null) {
			return null;
		}
		return headItem.getValueObject();
	}

	protected void setHeadValue(String key, Object value) {
		if (getBillCardPanel().getHeadItem(key) != null) {
			getBillCardPanel().getHeadItem(key).setValue(value);
		}
	}
}