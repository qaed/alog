package nc.ui.obm.ebankconfirmpay.action;

import nc.bs.logging.Logger;
import nc.ui.pubapp.uif2app.actions.pflow.SaveScriptAction;
import nc.ui.tmpub.security.ICommonClientSign;
import nc.vo.obm.ebankconfirmpay.AggConfirmPayHVO;
import nc.vo.obm.payroll.contant.ObmDfgzException;

public class EbankconfirmpaySaveAction extends SaveScriptAction {
	private static final long serialVersionUID = 8717963603100057334L;
	private ICommonClientSign caSigner;

	public EbankconfirmpaySaveAction() {
	}

	protected Object[] processBefore(Object[] vos) {
		super.processBefore(vos);
		try {
			getCaSigner().checkIfCASign();

			AggConfirmPayHVO[] aggPayVOs = (AggConfirmPayHVO[]) vos;
			if ((aggPayVOs != null) && (aggPayVOs.length > 0)) {
				for (int i = 0; i < aggPayVOs.length; i++) {
					this.caSigner.signWithClient(new nc.vo.pub.AggregatedValueObject[] { aggPayVOs[i] });
				}
			}
		} catch (Exception e) {
			//20170428 tsy 显示错误信息
			Logger.error("签名报错:" + e.getMessage(), e);
			//20170428 end
			throw new ObmDfgzException(e.getMessage(), e);
		}
		return vos;
	}

	public ICommonClientSign getCaSigner() {
		return this.caSigner;
	}

	public void setCaSigner(ICommonClientSign caSigner) {
		this.caSigner = caSigner;
	}
}