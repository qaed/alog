package nc.ui.erm.billinput.action;

import java.awt.event.ActionEvent;

import nc.ui.erm.erminitbill.action.CopyAction;
import nc.vo.pub.lang.UFDouble;

public class BXCopyAction extends CopyAction {
	public BXCopyAction() {
		super();
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -7750909742841310761L;

	@Override
	public void doAction(ActionEvent e) throws Exception {
		super.doAction(e);
		// 20170712 tsy 清空表体amount（不含税金额）
		int rowcount = getEditor().getBillCardPanel().getRowCount();
		UFDouble total = UFDouble.ZERO_DBL;
		for (int i = 0; i < rowcount; i++) {
			total = total.add((UFDouble) getEditor().getBillCardPanel().getBodyValueAt(i, "amount"));
//			getEditor().getBillCardPanel().setBodyValueAt(null, i, "amount");
		}
		getEditor().getBillCardPanel().setHeadItem("total", total);
		// 20170712 end
	}

}
