package nc.bs.er.exp.cmd.edit;

import nc.bs.er.exp.util.YerCShareUtil;
import nc.bs.logging.Logger;
import nc.uap.lfw.core.data.Dataset;
import nc.uap.lfw.core.data.Row;
import nc.uap.lfw.core.data.RowData;
import nc.uap.lfw.core.event.DatasetCellEvent;
import nc.uap.lfw.core.exception.LfwBusinessException;
import nc.uap.lfw.core.page.LfwView;
import nc.uap.lfw.core.page.ViewModels;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;

public class Edit4Total extends ExpBillEditAction {
	public Edit4Total() {
	}

	public void editListener() throws LfwBusinessException {
		try {
			DatasetCellEvent cellevent = getCellEvent();
			if (new UFDouble((String) cellevent.getOldValue()).compareTo(new UFDouble((String) cellevent.getNewValue())) == 0) {
				return;
			}
			setHeadYFB();
			modify1BusiRows();
			Dataset cShareDs = getCurrentWidget().getViewModels().getDataset("bx_cshare_detail");
			if (cShareDs != null) {
				Row[] rowArr = cShareDs.getCurrentRowData().getRows();
				int rowCount = rowArr.length;
				if (rowCount > 0) {
					Row currentRow = this.headDs.getSelectedRow();
					// 20170711 tsy 改为表头zyx11
					UFDouble total = UFDouble.ZERO_DBL;
					Object zyx12 = currentRow.getValue(this.headDs.nameToIndex("zyx12"));
					if (null != zyx12) {
						total = new UFDouble(zyx12.toString());
					}
					
					if (currentRow != null) {
//						total = (UFDouble) currentRow.getValue(this.headDs.nameToIndex("total"));
						// 20170711 end
						YerCShareUtil.reComputeAllJeByRatio(cShareDs, total);
					}
				}
			}
		} catch (BusinessException e) {
			Logger.error(e.getMessage(), e);
		}
	}
}