package nc.ui.erm.billpub.view.eventhandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import nc.itf.fi.pub.Currency;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.er.util.BXUiUtil;
import nc.ui.erm.billpub.model.ErmBillBillManageModel;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.billpub.view.ErmBillBillFormHelper;
import nc.ui.erm.costshare.common.ErmForCShareUiUtil;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.constenum.DefaultConstEnum;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillData;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillModel;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.er.djlx.DjLXVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.pub.BusinessException;
import nc.vo.pub.bill.BillTabVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class InitBodyEventHandle implements nc.ui.pub.bill.BillEditListener2, nc.ui.pub.bill.BillEditListener {
	private ErmBillBillForm editor = null;
	private EventHandleUtil eventUtil = null;
	private BodyEventHandleUtil bodyEventHandleUtil = null;

	public InitBodyEventHandle(ErmBillBillForm editor) {
		this.editor = editor;
		this.eventUtil = new EventHandleUtil(editor);
		this.bodyEventHandleUtil = new BodyEventHandleUtil(editor);
	}

	public boolean beforeEdit(BillEditEvent e) {
		String key = e.getKey();
		String fydwbm = this.bodyEventHandleUtil.getHeadItemStrValue("fydwbm");
		if (e.getTableCode().equalsIgnoreCase("er_cshare_detail")) {
			ErmForCShareUiUtil.doCShareBeforeEdit(e, getBillCardPanel());
		} else if ("szxmid".equals(key)) {
			UIRefPane refPane = this.bodyEventHandleUtil.getBodyItemUIRefPane(e.getTableCode(), key);
			refPane.getRefModel().setUseDataPower(true);
			refPane.setPk_org(fydwbm);
		} else if ("pk_resacostcenter".equals(key)) {
			UIRefPane refPane = this.bodyEventHandleUtil.getBodyItemUIRefPane(e.getTableCode(), "pk_resacostcenter");
			String wherePart = "pk_financeorg='" + fydwbm + "'";
			this.bodyEventHandleUtil.addWherePart2RefModel(refPane, fydwbm, wherePart);
		} else if ("pk_checkele".equals(key)) {
			UIRefPane refPane = this.bodyEventHandleUtil.getBodyItemUIRefPane(e.getTableCode(), key);
			String pk_pcorg = this.bodyEventHandleUtil.getBodyItemStrValue(e.getRow(), "pk_pcorg");
			if (pk_pcorg != null) {
				refPane.setEnabled(true);
				this.bodyEventHandleUtil.setPkOrg2RefModel(refPane, pk_pcorg);
			} else {
				refPane.setEnabled(false);
				getBillCardPanel().setBodyValueAt(null, e.getRow(), "pk_pcorg");
			}
		} else if ("projecttask".equals(key)) {
			String pk_project = this.bodyEventHandleUtil.getBodyItemStrValue(e.getRow(), "jobid");
			UIRefPane refPane = this.bodyEventHandleUtil.getBodyItemUIRefPane(e.getTableCode(), key);
			if (pk_project != null) {
				String wherePart = " pk_project='" + pk_project + "'";

				String pkOrg = this.bodyEventHandleUtil.getBodyItemUIRefPane(e.getTableCode(), "jobid").getRefModel().getPk_org();
				String pk_org = this.bodyEventHandleUtil.getHeadItemStrValue("fydwbm");
				if (BXUiUtil.getPK_group().equals(pkOrg)) {
					pk_org = BXUiUtil.getPK_group();
				}

				refPane.setEnabled(true);
				this.bodyEventHandleUtil.setWherePart2RefModel(refPane, pk_org, wherePart);
			} else {
				refPane.setPK(null);
				refPane.setEnabled(false);
			}
		} else if ((key != null) && (key.startsWith("defitem"))) {
			filterDefItemField(key);
		}
		try {
			CrossCheckUtil.checkRule("N", key, this.editor);
		} catch (BusinessException e1) {
			ExceptionHandler.handleExceptionRuntime(e1);
			return false;
		}
		return true;
	}

	private void filterDefItemField(String key) {
		BillItem bodyItem = this.editor.getBillCardPanel().getBodyItem(key);
		if (((bodyItem.getComponent() instanceof UIRefPane)) && (((UIRefPane) bodyItem.getComponent()).getRefModel() != null)) {
			ErmBillBillForm ermBillFom = this.editor;
			String pk_org = null;
			if ((ermBillFom.getOrgRefFields("pk_org") != null) && (ermBillFom.getOrgRefFields("pk_org").contains(key))) {
				BillItem item = ermBillFom.getBillCardPanel().getHeadItem("pk_org");
				if (item != null) {
					pk_org = (String) item.getValueObject();
				}
			} else if ((ermBillFom.getOrgRefFields("dwbm") != null) && (ermBillFom.getOrgRefFields("dwbm").contains(key))) {
				BillItem item = ermBillFom.getBillCardPanel().getHeadItem("dwbm");
				if (item != null) {
					pk_org = (String) item.getValueObject();
				}
			} else if ((ermBillFom.getOrgRefFields("fydwbm") != null) && (ermBillFom.getOrgRefFields("fydwbm").contains(key))) {
				BillItem item = ermBillFom.getBillCardPanel().getHeadItem("fydwbm");
				if (item != null) {
					pk_org = (String) item.getValueObject();
				}
			} else if ((ermBillFom.getOrgRefFields(JKBXHeaderVO.PK_PAYORG) != null) && (ermBillFom.getOrgRefFields(JKBXHeaderVO.PK_PAYORG).contains(key))) {
				BillItem item = ermBillFom.getBillCardPanel().getHeadItem(JKBXHeaderVO.PK_PAYORG);
				if (item != null) {
					pk_org = (String) item.getValueObject();
				}
			} else {
				BillItem item = ermBillFom.getBillCardPanel().getHeadItem("pk_org");
				if (item != null) {
					pk_org = (String) item.getValueObject();
				}
			}

			((UIRefPane) bodyItem.getComponent()).getRefModel().setPk_org(pk_org);
		}
	}

	public void afterEdit(BillEditEvent e) {
		BillItem bodyItem = getBillCardPanel().getBodyItem(e.getTableCode(), e.getKey());
		if (bodyItem == null) {
			return;
		}

		if (e.getTableCode().equals("er_cshare_detail")) {
			ErmForCShareUiUtil.doCShareAfterEdit(e, getBillCardPanel());
		} else {
			if ((bodyItem.getKey().equals("amount")) || (isAmoutField(bodyItem))) {
				Object amount = getBillCardPanel().getBodyValueAt(e.getRow(), "amount");
				getBillCardPanel().setBodyValueAt(amount, e.getRow(), "ybje");

				finBodyYbjeEdit();
				e.setKey("ybje");
				this.bodyEventHandleUtil.modifyFinValues(e.getKey(), e.getRow());
				e.setKey("amount");
				try {
					this.editor.getHelper().calculateFinitemAndHeadTotal(this.editor);
					this.eventUtil.setHeadYFB();
				} catch (BusinessException e1) {
					ExceptionHandler.handleExceptionRuntime(e1);
				}
			} else if ((bodyItem.getKey() != null) && (bodyItem.getKey().equals("szxmid"))) {
				e.setKey(bodyItem.getKey());
			} else if ((e.getKey().equals("ybje")) || (e.getKey().equals("cjkybje")) || (e.getKey().equals("zfybje")) || (e.getKey().equals("hkybje"))) {
				if (e.getKey().equals("ybje")) {
					finBodyYbjeEdit();
				}
				this.bodyEventHandleUtil.modifyFinValues(e.getKey(), e.getRow());
			} else if (e.getKey().equals("pk_pcorg_v")) {
				String pk_prong_v = this.bodyEventHandleUtil.getBodyItemStrValue(e.getRow(), e.getKey());
				UIRefPane refPane = (UIRefPane) getBillCardPanel().getBodyItem(e.getKey()).getComponent();

				String oldid = MultiVersionUtil.getBillFinanceOrg(refPane.getRefModel(), pk_prong_v);
				getBillCardPanel().getBillData().getBillModel().setValueAt(new DefaultConstEnum(oldid, "pk_pcorg"), e.getRow(), "pk_pcorg");

				getBillCardPanel().getBillData().getBillModel().loadLoadRelationItemValue(e.getRow(), "pk_pcorg");
				afterEditPk_corp(e);
			} else if (e.getKey().equals("pk_pcorg")) {
				BillItem pcorg_vItem = getBillCardPanel().getBodyItem("pk_pcorg_v");
				if (pcorg_vItem != null) {
					UFDate date = (UFDate) getBillCardPanel().getHeadItem("djrq").getValueObject();
					if (date != null) {
						String pk_pcorg = this.bodyEventHandleUtil.getBodyItemStrValue(e.getRow(), "pk_pcorg");
						Map<String, String> map =
								MultiVersionUtil.getFinanceOrgVersion(((UIRefPane) pcorg_vItem.getComponent()).getRefModel(), new String[] { pk_pcorg }, date);

						String vid = map.keySet().size() == 0 ? null : (String) map.keySet().iterator().next();
						getBillCardPanel().getBillModel().setValueAt(vid, e.getRow(), "pk_pcorg_v_ID");

						getBillCardPanel().getBillModel().loadLoadRelationItemValue(e.getRow(), "pk_pcorg_v");
					}
				}
				afterEditPk_corp(e);
			} else if (e.getKey().equals("jobid")) {
				getBillCardPanel().getBillData().getBillModel(e.getTableCode()).setValueAt(null, e.getRow(), "projecttask");
			}
			// 20170712 tsy 同步表头价税合计金额
			UFDouble newHeadzyx12 = null;
			BillTabVO[] billTabVOs = getBillCardPanel().getBillData().getBillTabVOs(1);
			if ((billTabVOs != null) && (billTabVOs.length > 0)) {
				for (BillTabVO billTabVO : billTabVOs) {

					BillModel billModel = getBillCardPanel().getBillModel(billTabVO.getTabcode());
					BXBusItemVO[] details = (BXBusItemVO[]) (BXBusItemVO[]) billModel.getBodyValueVOs(BXBusItemVO.class.getName());
					int length = details.length;
					for (int i = 0; i < length; i++) {
						if (details[i].getDefitem25() != null) {
							if (newHeadzyx12 == null)
								newHeadzyx12 = new UFDouble(details[i].getDefitem25().toString());
							else {
								newHeadzyx12 = newHeadzyx12.add(new UFDouble(details[i].getDefitem25().toString()));
							}
						}
					}
				}
			}

			getBillCardPanel().setHeadItem("zyx12", newHeadzyx12);
			getBillCardPanel().setHeadItem("zfybje", newHeadzyx12);
			getBillCardPanel().setHeadItem("zfbbje", newHeadzyx12);
			// 20170712 end
			if (this.bodyEventHandleUtil.getUserdefine(1, bodyItem.getKey(), 2) != null) {
				String formula = this.bodyEventHandleUtil.getUserdefine(1, bodyItem.getKey(), 2);
				String[] strings = formula.split(";");
				for (String form : strings) {
					this.bodyEventHandleUtil.doFormulaAction(form, e.getKey(), e.getRow(), e.getTableCode(), e.getValue());
				}
			}
			try {
				this.bodyEventHandleUtil.doContract(bodyItem, e);
			} catch (BusinessException e1) {
				ExceptionHandler.handleExceptionRuntime(e1);
			}

			this.bodyEventHandleUtil.doBodyReimAction();
		}
	}

	private void afterEditPk_corp(BillEditEvent e) {
		getBillCardPanel().getBillData().getBillModel(e.getTableCode()).setValueAt(null, e.getRow(), "pk_checkele");
	}

	private boolean isAmoutField(BillItem bodyItem) {
		String[] editFormulas = bodyItem.getEditFormulas();
		if (editFormulas == null) {
			return false;
		}
		for (String formula : editFormulas) {
			if (formula.indexOf("amount") != -1) {
				return true;
			}
		}
		return false;
	}

	public void finBodyYbjeEdit() {
		UFDouble newHeadYbje = null;
		String defaultMetaDataPath = "er_busitem";
		DjLXVO currentDjlx = ((ErmBillBillManageModel) this.editor.getModel()).getCurrentDjLXVO();

		if ("jk".equals(currentDjlx.getDjdl())) {
			defaultMetaDataPath = "jk_busitem";
		}

		BillTabVO[] billTabVOs = getBillCardPanel().getBillData().getBillTabVOs(1);
		if ((billTabVOs != null) && (billTabVOs.length > 0)) {
			for (BillTabVO billTabVO : billTabVOs) {
				String metaDataPath = billTabVO.getMetadatapath();
				if ((metaDataPath == null) || (defaultMetaDataPath.equals(metaDataPath))) {

					BillModel billModel = getBillCardPanel().getBillModel(billTabVO.getTabcode());
					BXBusItemVO[] details = (BXBusItemVO[]) billModel.getBodyValueVOs(BXBusItemVO.class.getName());

					int length = details.length;
					for (int i = 0; i < length; i++) {
						if (details[i].getYbje() != null) {
							if (newHeadYbje == null) {
								newHeadYbje = details[i].getYbje();
							} else {
								newHeadYbje = newHeadYbje.add(details[i].getYbje());
							}
						}
					}
				}
			}
		}
		getBillCardPanel().setHeadItem("ybje", newHeadYbje);
		// getBillCardPanel().setHeadItem("bbje", newHeadYbje);
		if (getHeadValue("pk_org") != null) {
			setHeadYfbByHead();
		}
	}

	protected void setHeadYfbByHead() {
		Object valueObject = getBillCardPanel().getHeadItem("ybje").getValueObject();

		if ((valueObject == null) || (valueObject.toString().trim().length() == 0)) {
			return;
		}
		UFDouble newYbje = new UFDouble(valueObject.toString());
		try {
			String bzbm = "null";
			if (getHeadValue("bzbm") != null) {
				bzbm = getHeadValue("bzbm").toString();
			}

			UFDouble hl = null;

			UFDouble globalhl =
					getBillCardPanel().getHeadItem("globalbbhl").getValueObject() != null ? new UFDouble(getBillCardPanel().getHeadItem("globalbbhl").getValueObject().toString()) : null;

			UFDouble grouphl =
					getBillCardPanel().getHeadItem("groupbbhl").getValueObject() != null ? new UFDouble(getBillCardPanel().getHeadItem("groupbbhl").getValueObject().toString()) : null;

			if (getBillCardPanel().getHeadItem("bbhl").getValueObject() != null) {
				hl = new UFDouble(getBillCardPanel().getHeadItem("bbhl").getValueObject().toString());
			}

			UFDouble[] je = Currency.computeYFB(this.eventUtil.getPk_org(), 1, bzbm, newYbje, null, null, null, hl, BXUiUtil.getSysdate());

			getBillCardPanel().setHeadItem("ybje", je[0]);
			getBillCardPanel().setHeadItem("bbje", je[2]);

			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, BXUiUtil.getSysdate(), getBillCardPanel().getHeadItem("pk_org").getValueObject().toString(), getBillCardPanel().getHeadItem("pk_group").getValueObject().toString(), globalhl, grouphl);

			DjLXVO currentDjlx = ((ErmBillBillManageModel) this.editor.getModel()).getCurrentDjLXVO();
			if (("jk".equals(currentDjlx.getDjdl())) || (this.editor.getResVO() != null)) {
				getBillCardPanel().setHeadItem("total", je[0]);
			}
			getBillCardPanel().setHeadItem("groupbbje", money[0]);
			getBillCardPanel().setHeadItem("globalbbje", money[1]);
			getBillCardPanel().setHeadItem("groupbbhl", money[2]);
			getBillCardPanel().setHeadItem("globalbbhl", money[3]);

			this.eventUtil.resetCjkjeAndYe(je[0], bzbm, hl);
		} catch (BusinessException e) {
			ExceptionHandler.handleExceptionRuntime(e);
		}
	}

	private BillCardPanel getBillCardPanel() {
		return this.editor.getBillCardPanel();
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

	public void bodyRowChange(BillEditEvent e) {
		if ((e.getOldrows() != null) && (e.getOldrows().length != e.getRows().length)) {
		}
	}

	public void resetJeAfterModifyRow() {
		if (!this.editor.getBillCardPanel().getCurrentBodyTableCode().equals("er_cshare_detail")) {
			this.editor.getHelper().calculateFinitemAndHeadTotal(this.editor);

			try {
				this.eventUtil.resetHeadYFB();

			} catch (BusinessException e) {
				ExceptionHandler.handleExceptionRuntime(e);
			}
		}
	}

	// 获取UFDouble金额 NOTE
	private UFDouble convert2UFdouble(Object def) {
		if (def == null)
			return new UFDouble(0.0, 2);

		if (def instanceof UFDouble) {
			return new UFDouble(((UFDouble) def).doubleValue(), 2);
		}
		if (def instanceof String) {
			return new UFDouble(((String) def).toString(), 2);
		}
		return new UFDouble(0.0, 2);
	}

	public BodyEventHandleUtil getBodyEventHandleUtil() {
		return this.bodyEventHandleUtil;
	}
}