package nc.ui.erm.billpub.view.eventhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import nc.itf.fi.pub.Currency;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.er.util.BXUiUtil;
import nc.ui.erm.billpub.action.ContrastAction;
import nc.ui.erm.billpub.view.ErmBillBillForm;
import nc.ui.erm.billpub.view.ErmBillBillFormHelper;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.constenum.IConstEnum;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillData;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillModel;
import nc.vo.arap.bx.util.BodyEditVO;
import nc.vo.arap.bx.util.BxUIControlUtil;
import nc.vo.ep.bx.BxcontrastVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.er.exception.ExceptionHandler;
import nc.vo.pub.BusinessException;
import nc.vo.pub.ValidationException;
import nc.vo.pub.bill.BillTempletBodyVO;
import nc.vo.pub.bill.BillTempletVO;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class BodyEventHandleUtil {
	private ErmBillBillForm editor = null;

	public BodyEventHandleUtil(ErmBillBillForm editor) {
		this.editor = editor;
	}

	public void modifyFinValues(String key, int row) {
		BillCardPanel panel = getBillCardPanel();
		// 20170713 tsy 取自定义25进行冲借款
		// UFDouble ybje = getAmountValue(row, panel, "ybje");
		Object zyx25 = panel.getBillModel().getValueAt(row, "defitem25");
		UFDouble ybje = convert2UFdouble(zyx25);
		// 20170713 end
		UFDouble cjkybje = getAmountValue(row, panel, "cjkybje");
		UFDouble zfybje = getAmountValue(row, panel, "zfybje");
		UFDouble hkybje = getAmountValue(row, panel, "hkybje");

		if ((key.equals("ybje")) || (key.equals("cjkybje"))) {
			if (ybje.getDouble() > cjkybje.getDouble()) {
				panel.setBodyValueAt(ybje.sub(cjkybje), row, "zfybje");
				panel.setBodyValueAt(UFDouble.ZERO_DBL, row, "hkybje");
				panel.setBodyValueAt(cjkybje, row, "cjkybje");
			} else {
				panel.setBodyValueAt(cjkybje.sub(ybje), row, "hkybje");
				panel.setBodyValueAt(UFDouble.ZERO_DBL, row, "zfybje");
				panel.setBodyValueAt(cjkybje, row, "cjkybje");
			}
		} else if (key.equals("zfybje")) {
			if (zfybje.toDouble().doubleValue() > ybje.toDouble().doubleValue()) {
				zfybje = ybje;
				panel.setBodyValueAt(zfybje, row, "zfybje");
			}
			panel.setBodyValueAt(ybje.sub(zfybje), row, "cjkybje");
			panel.setBodyValueAt(UFDouble.ZERO_DBL, row, "hkybje");
		} else if (key.equals("hkybje")) {
			panel.setBodyValueAt(ybje.add(hkybje), row, "cjkybje");
			panel.setBodyValueAt(UFDouble.ZERO_DBL, row, "zfybje");
		}

		panel.setBodyValueAt(ybje, row, "ybye");

		String bzbm = null;
		if (getHeadValue("bzbm") != null) {
			bzbm = getHeadValue("bzbm").toString();
		}

		if ((getHeadValue("pk_org") != null) && (bzbm != null)) {
			transFinYbjeToBbje(row, bzbm);
		}
	}

	private UFDouble getAmountValue(int row, BillCardPanel panel, String itemKey) {
		Object bodyValue = panel.getBodyValueAt(row, itemKey);
		return bodyValue == null ? UFDouble.ZERO_DBL : (UFDouble) bodyValue;
	}

	protected void transFinYbjeToBbje(int row, String bzbm) {
		BillCardPanel panel = getBillCardPanel();

		UFDouble ybje = (UFDouble) panel.getBillModel().getValueAt(row, "ybje");
		UFDouble cjkybje = (UFDouble) panel.getBillModel().getValueAt(row, "cjkybje");
		UFDouble hkybje = (UFDouble) panel.getBillModel().getValueAt(row, "hkybje");
		UFDouble zfybje = (UFDouble) panel.getBillModel().getValueAt(row, "zfybje");
		UFDouble hl = null;
		UFDouble globalhl = null;
		UFDouble grouphl = null;
		if (getBillCardPanel().getHeadItem("bbhl").getValueObject() != null) {
			hl = new UFDouble(getBillCardPanel().getHeadItem("bbhl").getValueObject().toString());
		}
		if (getBillCardPanel().getHeadItem("groupbbhl").getValueObject() != null) {
			grouphl = new UFDouble(getBillCardPanel().getHeadItem("groupbbhl").getValueObject().toString());
		}
		if (getBillCardPanel().getHeadItem("globalbbhl").getValueObject() != null) {
			globalhl = new UFDouble(getBillCardPanel().getHeadItem("globalbbhl").getValueObject().toString());
		}
		try {
			UFDouble[] bbje = Currency.computeYFB(getPk_org(), 1, bzbm, ybje, null, null, null, hl, BXUiUtil.getSysdate());

			panel.getBillModel().setValueAt(bbje[2], row, "bbje");
			panel.getBillModel().setValueAt(bbje[2], row, "bbye");

			bbje = Currency.computeYFB(getPk_org(), 1, bzbm, cjkybje, null, null, null, hl, BXUiUtil.getSysdate());

			panel.getBillModel().setValueAt(bbje[2], row, "cjkbbje");

			bbje = Currency.computeYFB(getPk_org(), 1, bzbm, hkybje, null, null, null, hl, BXUiUtil.getSysdate());

			panel.getBillModel().setValueAt(bbje[2], row, "hkbbje");
			bbje = Currency.computeYFB(getPk_org(), 1, bzbm, zfybje, null, null, null, hl, BXUiUtil.getSysdate());

			panel.getBillModel().setValueAt(bbje[2], row, "zfbbje");

			UFDouble[] je = Currency.computeYFB(getPk_org(), 1, bzbm, ybje, null, null, null, hl, BXUiUtil.getSysdate());

			UFDouble[] money =
					Currency.computeGroupGlobalAmount(je[0], je[2], bzbm, BXUiUtil.getSysdate(), getBillCardPanel().getHeadItem("pk_org").getValueObject().toString(), getBillCardPanel().getHeadItem("pk_group").getValueObject().toString(), globalhl, grouphl);

			panel.getBillModel().setValueAt(money[0], row, "groupbbje");

			panel.getBillModel().setValueAt(money[0], row, "groupbbye");

			panel.getBillModel().setValueAt(money[1], row, "globalbbje");

			panel.getBillModel().setValueAt(money[1], row, "globalbbye");

			panel.getBillModel().setValueAt(money[0], row, "groupzfbbje");

			panel.getBillModel().setValueAt(money[1], row, "globalzfbbje");
		} catch (BusinessException e) {
			ExceptionHandler.handleExceptionRuntime(e);
		}
	}

	public void doContract(BillItem bodyItem, BillEditEvent eve) throws ValidationException, BusinessException {
		BillModel billModel = getBillCardPanel().getBillModel("er_bxcontrast");
		if (billModel == null) {
			return;
		}
		UFDouble ybje = (UFDouble) getBillCardPanel().getBodyValueAt(eve.getRow(), "ybje");

		if ((ybje != null) && (!ybje.equals(UFDouble.ZERO_DBL)) && ("bx".equals(((JKBXVO) this.editor.getValue()).getParentVO().getDjdl())) && ((bodyItem.getDataType() == 2) || (bodyItem.getDataType() == 1))) {

			BxcontrastVO[] bxcontrastVO = (BxcontrastVO[]) billModel.getBodyValueVOs(BxcontrastVO.class.getName());

			if ((bxcontrastVO != null) && (bxcontrastVO.length > 0)) {
				ContrastAction.doContrastToUI(getBillCardPanel(), this.editor.getHelper().getJKBXVO(this.editor), Arrays.asList(bxcontrastVO), this.editor);
			}
		}
	}

	public void doBodyReimAction() {
		JKBXVO bxvo = this.editor.getHelper().getJKBXVO(this.editor);
		HashMap<String, String> bodyReimRuleMap = this.editor.getBodyReimRuleMap();
		List<BodyEditVO> result = BxUIControlUtil.doBodyReimAction(bxvo, this.editor.getReimRuleDataMap(), bodyReimRuleMap);

		for (BodyEditVO vo : result) {
			getBillCardPanel().setBodyValueAt(vo.getValue(), vo.getRow(), vo.getItemkey(), vo.getTablecode());
		}
	}

	public String getUserdefine(int pos, String key, int def) {
		if ((getBillCardPanel().getBillData().getBillTempletVO() == null) || (getBillCardPanel().getBillData().getBillTempletVO().getChildrenVO() == null)) {

			return null;
		}
		BillTempletBodyVO[] tbodyvos = (BillTempletBodyVO[]) getBillCardPanel().getBillData().getBillTempletVO().getChildrenVO();

		for (BillTempletBodyVO bodyvo : tbodyvos) {
			if (((pos == 0) && (bodyvo.getItemkey().equals(key))) || ((bodyvo.getPos().intValue() == pos) && (bodyvo.getItemkey().equals(key)) && (bodyvo.getTableCode().equals(getBillCardPanel().getCurrentBodyTableCode())))) {

				if (def == 1)
					return bodyvo.getUserdefine1();
				if (def == 2)
					return bodyvo.getUserdefine2();
				if (def == 3)
					return bodyvo.getUserdefine3();
			}
		}
		return null;
	}

	public void doFormulaAction(String formula, String skey, int srow, String stable, Object svalue) {
		if (formula == null) {
			return;
		}

		try {
			formula = formula.replace('(', '#');
			formula = formula.replace(')', '#');
			formula = formula.replace(',', '#');
			formula = formula.trim();

			if (formula.startsWith("toHead")) {
				String[] values = formula.split("#");
				String headKey = values[1];
				String func = values[2];
				String prow = values[3];
				String pkey = values[4];
				String ptab = values[5];

				String key = pkey.equals("%key%") ? skey : pkey;
				String table = ptab.equals("%table%") ? stable : ptab;

				Object resultvalue = getResultValue(func, svalue, prow, key, table);

				if (resultvalue != null) {
					setHeadValue(headKey, resultvalue);
				}
			}
			if (formula.startsWith("toBody")) {
				String[] values = formula.split("#");
				String bodyRow = values[1];
				String bodyKey = values[2];
				String bodyTab = values[3];
				String func = values[4];
				String prow = values[5];
				String pkey = values[6];
				String ptab = values[7];

				String key = pkey.equals("%key%") ? skey : pkey;
				String table = ptab.equals("%table%") ? stable : ptab;

				Object resultvalue = getResultValue(func, svalue, prow, key, table);

				BillItem item = getBillCardPanel().getBodyItem(bodyTab, bodyKey);

				BillModel bm = getBillCardPanel().getBillModel(bodyTab);

				if (resultvalue != null) {
					bodyKey = bodyKey.equals("%key%") ? skey : bodyKey;
					bodyTab = bodyTab.equals("%table%") ? stable : bodyTab;

					if (bodyRow.equals("%all%")) {
						int rowCount = getBillCardPanel().getRowCount(bodyTab);
						for (int i = 0; i < rowCount; i++) {
							getBillCardPanel().setBodyValueAt(resultvalue, i, bodyKey, bodyTab);

							if (bm != null)
								bm.execFormulas(i, item.getEditFormulas());
						}
					} else if (bodyRow.equals("%row%")) {
						getBillCardPanel().setBodyValueAt(resultvalue, srow, bodyKey, bodyTab);

						if (bm != null)
							bm.execFormulas(srow, item.getEditFormulas());
					} else {
						getBillCardPanel().setBodyValueAt(resultvalue, Integer.parseInt(bodyRow), bodyKey, bodyTab);

						if (bm != null) {
							bm.execFormulas(Integer.parseInt(bodyRow), item.getEditFormulas());
						}
					}
				}
			}
		} catch (Exception e) {
			ExceptionHandler.handleExceptionRuntime(e);
		}
	}

	private Object getResultValue(String func, Object svalue, String prow, String key, String table) {
		int row = 0;
		Object[] value = null;
		if (prow.equals("%all%")) {
			BillModel billModel = getBillCardPanel().getBillModel(table);
			int rowCount = getBillCardPanel().getRowCount(table);
			List<Object> arrayValue = new ArrayList();
			for (int i = 0; i < rowCount; i++) {
				Object valueAt = billModel.getValueAt(i, key);
				if ((valueAt != null) && (!valueAt.equals(""))) {
					arrayValue.add(valueAt);
				}
			}
			value = arrayValue.toArray(new Object[0]);
		} else if (prow.equals("%row%")) {
			value = new Object[] { svalue };
		} else if (Integer.valueOf(prow).intValue() == -1) {
			value = new Object[] { getHeadValue(key) };
		} else {
			row = Integer.parseInt(prow);
			value = new Object[] { getBillCardPanel().getBillModel(table).getValueAt(row, key) };
		}
		if (value == null)
			return null;
		if (value.length <= 1)
			return value[0];
		if (func.equals("sum")) {
			Object revalue = null;
			for (Object sv : value) {
				if (sv != null) {
					if ((sv instanceof UFDouble)) {
						if (revalue == null) {
							revalue = sv;
						} else
							revalue = ((UFDouble) revalue).add((UFDouble) sv);
					}
					if ((sv instanceof Integer)) {
						if (revalue == null) {
							revalue = sv;
						} else
							revalue = Integer.valueOf(((Integer) revalue).intValue() + Integer.parseInt(sv.toString()));
					}
				}
			}
			return revalue;
		}
		if (func.equals("avg")) {
			Object revalue = null;
			for (Object sv : value) {
				if (sv != null) {
					if ((sv instanceof UFDouble)) {
						if (revalue == null) {
							revalue = sv;
						} else
							revalue = ((UFDouble) revalue).add((UFDouble) sv);
					}
					if ((sv instanceof Integer)) {
						if (revalue == null) {
							revalue = sv;
						} else
							revalue = Integer.valueOf(((Integer) revalue).intValue() + Integer.parseInt(sv.toString()));
					}
				}
			}
			if (revalue == null)
				return null;
			if ((revalue instanceof UFDouble))
				return ((UFDouble) revalue).div(value.length);
			if ((revalue instanceof Integer))
				return Integer.valueOf(((Integer) revalue).intValue() / value.length);
		}
		if (func.equals("min")) {
			Object revalue = value[0];
			for (Object sv : value)
				if (sv != null) {
					if ((sv instanceof UFDouble)) {
						UFDouble new_name = (UFDouble) sv;
						if (new_name.compareTo(revalue) < 0)
							revalue = new_name;
					}
					if ((sv instanceof Integer)) {
						Integer new_name = (Integer) sv;
						if (new_name.compareTo((Integer) revalue) < 0)
							revalue = new_name;
					}
					if ((sv instanceof UFDate)) {
						UFDate new_name = (UFDate) sv;

						if (new_name.compareTo((UFDate) revalue) < 0)
							revalue = new_name;
					}
					if ((sv instanceof String)) {
						String new_name = (String) sv;
						if (new_name.compareTo((String) revalue) < 0)
							revalue = new_name;
					}
				}
			return revalue;
		}
		if (func.equals("max")) {
			Object revalue = value[0];
			for (Object sv : value)
				if (sv != null) {
					if ((sv instanceof UFDouble)) {
						UFDouble new_name = (UFDouble) sv;
						if (new_name.compareTo(revalue) > 0)
							revalue = new_name;
					}
					if ((sv instanceof Integer)) {
						Integer new_name = (Integer) sv;
						if (new_name.compareTo((Integer) revalue) > 0)
							revalue = new_name;
					}
					if ((sv instanceof UFDate)) {
						UFDate new_name = (UFDate) sv;
						if (new_name.compareTo((UFDate) revalue) > 0)
							revalue = new_name;
					}
					if ((sv instanceof String)) {
						String new_name = (String) sv;
						if (new_name.compareTo((String) revalue) > 0)
							revalue = new_name;
					}
				}
			return revalue;
		}
		return value;
	}

	private BillCardPanel getBillCardPanel() {
		return this.editor.getBillCardPanel();
	}

	public String getPk_org() {
		if (!this.editor.isShowing())
			return null;
		if (this.editor.isShowing()) {
			return (String) getBillCardPanel().getHeadItem("pk_org").getValueObject();
		}

		return BXUiUtil.getBXDefaultOrgUnit();
	}

	public String getBodyItemStrValue(int row, String key) {
		Object obj = getBillCardPanel().getBillModel().getValueObjectAt(row, key);

		if (obj == null)
			return null;
		if ((obj instanceof IConstEnum)) {
			return (String) ((IConstEnum) obj).getValue();
		}

		return (String) obj;
	}

	public String getHeadItemStrValue(String itemKey) {
		BillItem headItem = getBillCardPanel().getHeadItem(itemKey);
		return headItem == null ? null : (String) headItem.getValueObject();
	}

	public UIRefPane getBodyItemUIRefPane(String tableCode, String key) {
		return (UIRefPane) getBillCardPanel().getBodyItem(tableCode, key).getComponent();
	}

	protected void setHeadValue(String key, Object value) {
		if (getBillCardPanel().getHeadItem(key) != null) {
			getBillCardPanel().getHeadItem(key).setValue(value);
		}
	}

	public void setPkOrg2RefModel(UIRefPane refPane, String pk_org) {
		refPane.getRefModel().setPk_org(pk_org);
	}

	public void addWherePart2RefModel(UIRefPane refPane, String pk_org, String addwherePart) {
		filterRefModelWithWherePart(refPane, pk_org, null, addwherePart);
	}

	public void setWherePart2RefModel(UIRefPane refPane, String pk_org, String wherePart) {
		filterRefModelWithWherePart(refPane, pk_org, wherePart, null);
	}

	public void filterRefModelWithWherePart(UIRefPane refPane, String pk_org, String wherePart, String addWherePart) {
		AbstractRefModel model = refPane.getRefModel();
		model.setPk_org(pk_org);
		model.setWherePart(wherePart);
		if (addWherePart != null) {
			model.setPk_org(pk_org);
			model.addWherePart(" and " + addWherePart);
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
}