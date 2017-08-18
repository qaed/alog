package nc.ui.glcom.ass;

import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableColumn;
import nc.bs.logging.Logger;
import nc.itf.gl.pub.IFreevaluePub;
import nc.ui.gl.ass.FreeDefCellEditorFactory;
import nc.ui.gl.datacache.AccountCache;
import nc.ui.gl.datacache.BDInfoDataCache;
import nc.ui.gl.gateway.glworkbench.GlWorkBench;
import nc.ui.gl.gateway60.patterninfo.PatternInfo;
import nc.ui.gl.reconcilepub.ReconcileBusinessTablelEditor;
import nc.ui.ml.NCLangRes;
import nc.ui.pub.beans.UITable;
import nc.ui.pub.beans.table.VOTableModel;
import nc.vo.bd.account.AccAssVO;
import nc.vo.bd.account.AccountVO;
import nc.vo.fipub.freevalue.Module;
import nc.vo.fipub.freevalue.account.proxy.AccAssGL;
import nc.vo.fipub.freevalue.util.FreeValueDefUtil;
import nc.vo.gateway60.pub.GlBdinfoVO;
import nc.vo.glcom.ass.AssVO;
import nc.vo.glcom.tools.GLPubProxy;
import nc.vo.pub.BusinessException;
import nc.vo.pub.ValueObject;

public class FreeValueListTableModel extends VOTableModel {
	private PatternInfo[] m_colName = null;
	private String m_Org;
	private String pk_unit;
	private String m_stddate;
	AssVO[] m_vos = null;
	GlBdinfoVO[] m_info;
	private boolean mustFullFill = false;

	protected EventListenerList mylisteners = new EventListenerList();
	private AssRefPanel[] refpaneledit = null;
	private UITable uitable = null;
	private AssVO[] multiSelectedAssVos = null;

	public FreeValueListTableModel(ValueObject[] vos) {
		super(vos);
	}

	public void cloneVO() {
		AssVO[] vos = new AssVO[getAssVo().length];

		for (int i = 0; i < getAssVo().length; i++) {
			vos[i] = new AssVO();
			vos[i].setPk_Checktype(getAssVo()[i].getPk_Checktype());
			vos[i].setChecktypecode(getAssVo()[i].getChecktypecode());
			vos[i].setChecktypename(getAssVo()[i].getChecktypename());
			vos[i].setPk_Checkvalue(getAssVo()[i].getPk_Checkvalue());
		}

		clearTable();
		addVO(vos);
		setVOs(vos);
	}

	public void cloneNewVO() {
		if (getAssVo() == null)
			return;
		AssVO[] vos = new AssVO[getAssVo().length];

		for (int i = 0; i < getAssVo().length; i++) {
			vos[i] = new AssVO();
			vos[i].setPk_Checktype(getAssVo()[i].getPk_Checktype());
			vos[i].setChecktypecode(getAssVo()[i].getChecktypecode());
			vos[i].setChecktypename(getAssVo()[i].getChecktypename());
			vos[i].setM_classid(getAssVo()[i].getM_classid());
			vos[i].setM_digit(getAssVo()[i].getM_digit());
			vos[i].setM_length(getAssVo()[i].getM_length());
			vos[i].setUserData(getAssVo()[i].getUserData());
			vos[i].setPk_Checkvalue(null);
		}

		clearTable();
		addVO(vos);
		setVOs(vos);
	}

	public FreeValueListTableModel(Class c) {
		super(c);
	}

	public FreeValueListTableModel(ValueObject vo) {
		super(vo);
	}

	public void addRowsMovedListener(IRowsMovedListener l) {
		this.mylisteners.add(IRowsMovedListener.class, l);
	}

	protected void fireRowsMoved(RowsMoveEvent e) {
		Object[] listeners = this.mylisteners.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == IRowsMovedListener.class) {
				((IRowsMovedListener) listeners[(i + 1)]).rowsMoved(e);
			}
		}
	}

	public AssVO[] getAssVo() {
		return this.m_vos;
	}

	public AssVO[][] getAssVOs() {
		if ((this.multiSelectedAssVos != null) && (getAssVo() != null)) {
			AssVO[][] rst = new AssVO[this.multiSelectedAssVos.length][getAssVo().length];
			int indexMultiSelectedAss = -1;
			for (int i = 0; i < getAssVo().length; i++) {
				if (getAssVo()[i].getPk_Checktype().equals(this.multiSelectedAssVos[0].getPk_Checktype())) {
					indexMultiSelectedAss = i;
					break;
				}
			}
			for (int j = 0; j < this.multiSelectedAssVos.length; j++) {
				rst[j] = new AssVO[getAssVo().length];
				for (int k = 0; k < rst[j].length; k++) {
					if (k == indexMultiSelectedAss) {
						AssVO newAss = (AssVO) getAssVo()[indexMultiSelectedAss].clone();
						newAss.setPk_Checkvalue(this.multiSelectedAssVos[j].getPk_Checkvalue());
						newAss.setCheckvaluecode(this.multiSelectedAssVos[j].getCheckvaluecode());
						newAss.setCheckvaluename(this.multiSelectedAssVos[j].getCheckvaluename());
						rst[j][indexMultiSelectedAss] = newAss;
					} else {
						rst[j][k] = getAssVo()[k];
					}
				}
			}
			return rst;
		}
		return (AssVO[][]) null;
	}

	public GlBdinfoVO getBdInfo(String strPk) throws Exception {
		GlBdinfoVO[] bdinfos = getInfos();
		for (int i = 0; i < bdinfos.length; i++) {
			if (bdinfos[i].getPk_bdinfo().equals(strPk))
				return bdinfos[i];
		}
		return null;
	}

	public Class getColumnClass(int col) {
		if (col == 0) {
			return Boolean.class;
		}
		return String.class;
	}

	public int getColumnCount() {
		return 4;
	}

	public int getColumnKey(int col) {
		return getColumns()[col].getIntColKey();
	}

	public String getColumnName(int col) {
		return getColumns()[col].getStrColName();
	}

	private PatternInfo[] getColumns() {
		if (this.m_colName == null) {
			this.m_colName = new PatternInfo[4];
			this.m_colName[0] = new PatternInfo(7, NCLangRes.getInstance().getStrByID("2002033381", "UPP2002033381-000078"));
			this.m_colName[1] = new PatternInfo(3, NCLangRes.getInstance().getStrByID("2002033381", "UPP2002033381-000079"));
			this.m_colName[2] = new PatternInfo(4, NCLangRes.getInstance().getStrByID("2002033381", "UPP2002033381-000082"));
			this.m_colName[3] = new PatternInfo(6, NCLangRes.getInstance().getStrByID("2002033381", "UPP2002033381-000080"));
		}
		return this.m_colName;
	}

	public String getID() throws Exception {
		String ID = GLPubProxy.getRemoteFreevaluePub().getAssID(getAssVo(), Boolean.TRUE, GlWorkBench.getLoginGroup(), Module.GL);
		return ID;
	}

	public String[] getIDs() throws Exception {
		AssVO[][] multiSelectedAss = getAssVOs();
		if (multiSelectedAss == null)
			return new String[] { getID() };
		String[] IDs = new String[multiSelectedAss.length];
		for (int i = 0; i < IDs.length; i++) {
			IDs[i] = GLPubProxy.getRemoteFreevaluePub().getAssID(multiSelectedAss[i], Boolean.TRUE, GlWorkBench.getLoginGroup(), Module.GL);
		}
		return IDs;
	}

	public void clearMultiSelectedAssvos() {
		this.multiSelectedAssVos = null;
	}

	public GlBdinfoVO[] getInfos() throws Exception {
		this.m_info = BDInfoDataCache.getInstance().queryBdinfoAll(getOrgBook(), getStddate());
		return this.m_info;
	}

	public String getNodeName(String strPk, String classid) throws Exception {
		try {
			if (FreeValueDefUtil.getInstance().isDefDoc(classid)) {
				return null;
			}
			GlBdinfoVO[] infos = getInfos();
			for (int i = 0; (null != infos) && (i < infos.length); i++) {
				if (strPk.equals(infos[i].getPk_bdinfo())) {
					return infos[i].getRefnodename();
				}

			}
		} catch (Throwable e) {
			throw new Exception(e.getMessage());
		}
		return null;
	}

	public String getOrgBook() {
		return this.m_Org;
	}

	public AssRefPanel[] getRefpaneledit() {
		return this.refpaneledit;
	}

	public TableColumn getTableColumn() {
		TableColumn tc = getUitable().getColumn(NCLangRes.getInstance().getStrByID("2002033381", "UPP2002033381-000080"));
		return tc;
	}

	public UITable getUitable() {
		return this.uitable;
	}

	public Object getValueAt(int row, int col) {
		AssVO tempVO = (AssVO) getVO(row);
		int intKey = getColumnKey(col);
		if (col == 2) {

			Object res = nc.bs.glcom.ass.assitem.cache.AccAssItemCache.getAccAssitemNameByPK(tempVO.getValue(1).toString());
			if ((res == null) || (res.toString().trim().equals(tempVO.getValue(1).toString().trim()))) {
				res = tempVO.getValue(intKey);
			}
			return res;
		}
		return tempVO.getValue(intKey);
	}

	public boolean isCellEditable(int row, int col) {
		try {
			if ((null != getRefpaneledit()[row]) && (null != getRefpaneledit()[row].getM_classid()) && (getRefpaneledit()[row].getM_classid().trim().length() > 0) && (FreeValueDefUtil.getInstance().isDefDoc(getRefpaneledit()[row].getM_classid()))) {
				Object obj =
						FreeDefCellEditorFactory.getInstance().getCellEditor((Integer) FreeValueDefUtil.getInstance().getIdMap().get(getRefpaneledit()[row].getM_classid()), getRefpaneledit()[row].getM_digit(), getRefpaneledit()[row].getM_length());
				if ((obj instanceof DefaultCellEditor)) {
					getTableColumn().setCellEditor((DefaultCellEditor) obj);
				} else if ((obj instanceof ReconcileBusinessTablelEditor)) {
					getTableColumn().setCellEditor((ReconcileBusinessTablelEditor) obj);
				}

			} else {
				getTableColumn().setCellEditor(getRefpaneledit()[row]);
			}

			getRefpaneledit()[row].requestFocus();
			int intKey = getColumnKey(col);
			if (intKey == 6)
				return true;
			if (intKey == 7) {
				return !isMustFullFill();
			}
			return false;
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
		return true;
	}

	public boolean isMustFullFill() {
		return this.mustFullFill;
	}

	public void moveRow(int startIndex, int endIndex, int toIndex) {
		if ((startIndex < 0) || (startIndex >= getRowCount()))
			throw new ArrayIndexOutOfBoundsException(startIndex);
		if ((endIndex < 0) || (endIndex >= getRowCount()))
			throw new ArrayIndexOutOfBoundsException(endIndex);
		if (startIndex > endIndex) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if ((startIndex <= toIndex) && (toIndex <= endIndex)) {
			return;
		}
		if ((toIndex < 0) || (toIndex >= getRowCount())) {
			return;
		}
		boolean shift = toIndex < startIndex;

		ValueObject[] vos = getVOArray();
		Vector oldVec = new Vector();
		for (int i = 0; i < vos.length; i++) {
			oldVec.add(vos[i]);
		}

		int backup_Toindex = toIndex;
		for (int i = startIndex; i <= endIndex; i++) {
			Object aRow = oldVec.elementAt(i);
			oldVec.removeElementAt(i);
			oldVec.insertElementAt(aRow, toIndex);

			if (shift) {
				toIndex++;
			}
		}

		clearTable();
		ValueObject[] newArray = new ValueObject[oldVec.size()];
		oldVec.copyInto(newArray);
		addVO(newArray);

		AssVO[] tmpVos = new AssVO[oldVec.size()];
		oldVec.copyInto(tmpVos);
		setVOs(tmpVos);

		Logger.debug("startIndex=" + startIndex + "  endIndex=" + endIndex + "   toIndex=" + toIndex);
		fireRowsMoved(new RowsMoveEvent(this, startIndex, endIndex, backup_Toindex));
	}

	public void moveRowDown(int row) {
		moveRow(row, row, row + 1);
	}

	public void moveRowUp(int row) {
		moveRow(row, row, row - 1);
	}

	public void setAssVOs(AssVO[] vos) throws Exception {
		clearTable();
		addVO(vos);
		this.m_vos = vos;
	}

	public void setVOs(AssVO[] vos) {
		this.m_vos = vos;
	}

	public void setID(String ID, AssVO[] assVOs) throws Exception {
	}

	public void setMustFullFill(boolean newMustFullFill) {
		this.mustFullFill = newMustFullFill;
	}

	public void setPk_Accsubj(String strPk_Accasoa) throws BusinessException {
		try {
			AccAssVO objQuery = new AccAssVO();
			if (!strPk_Accasoa.equals(objQuery.getPk_accasoa())) {

				AssVO[] vos = null;

				AccountVO account = AccountCache.getInstance().getAccountVOByPK(getOrgBook(), strPk_Accasoa, getStddate());

				if (account.getEndflag().booleanValue()) {
					Vector<AccAssVO> accAssVOs = AccAssGL.getAccAssVOsByAccount(getOrgBook(), account, getStddate());
					this.lastAccAssVOs = accAssVOs;
					vos = strPk_Accasoa == null ? new AssVO[0] : AccAssGL.convertAssVO(accAssVOs);
				} else {
					String[] pk_accassitems = AccAssGL.queryAllBySubjPKIncludeSon(strPk_Accasoa, getStddate());
					nc.vo.bd.accassitem.AccAssItemVO[] accassitems = AccAssGL.getAccAssItemVOByPks(pk_accassitems);
					vos = AccAssGL.convertAssItemToAssVO(accassitems);
					this.lastAccAssVOs = null;
				}

				clearTable();
				addVO(vos);
				setVOs(vos);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new BusinessException(e.getMessage());
		}
	}

	private Vector<AccAssVO> lastAccAssVOs = null;

	public boolean compareAccAssVO(AccAssVO[] accAssVOs) {
		if ((this.lastAccAssVOs == null) || (accAssVOs == null))
			return false;
		if (this.lastAccAssVOs.size() != accAssVOs.length) {
			return false;
		}
		boolean flag = true;
		for (int i = 0; i < this.lastAccAssVOs.size(); i++) {
			AccAssVO lastVo = (AccAssVO) this.lastAccAssVOs.get(i);
			if (lastVo != accAssVOs[i])
				flag = false;
		}
		return flag;
	}

	public void addVO(ValueObject[] vos) {
		if ((vos == null) || (vos.length == 0))
			return;
		int iCount = 0;
		for (int i = 0; i < vos.length; i++) {
			getVOs().addElement(vos[i]);
			iCount++;
		}
		if (iCount > 0) {

			fireTableChanged(new javax.swing.event.TableModelEvent(this, getRowCount() - iCount, getRowCount() - 1, -1, 1));
		}
	}

	public void setOrgBook(String strOrgBook) {
		this.m_Org = strOrgBook;
	}

	public void setRefpaneledit(AssRefPanel[] newRefpaneledit) {
		this.refpaneledit = newRefpaneledit;
	}

	public void setUitable(UITable newUitable) {
		this.uitable = newUitable;
	}

	public void setValueAt(Object value, int r, int c) {
		try {
			if (c == 3) {
				if ((value != null) && (value.toString().length() != 0)) {
					AssVO tempVO = null;
					if ((value instanceof AssVO))
						tempVO = (AssVO) value;
					if ((value instanceof AssVO[])) {
						if (((AssVO[]) value).length > 1) {
							this.multiSelectedAssVos = ((AssVO[]) value);
							for (int j = 0; j < this.multiSelectedAssVos.length; j++) {
								this.multiSelectedAssVos[j].setPk_Checktype(getAssVo()[r].getPk_Checktype());
								this.multiSelectedAssVos[j].setChecktypecode(getAssVo()[r].getChecktypecode());
								this.multiSelectedAssVos[j].setChecktypename(getAssVo()[r].getChecktypename());
							}
						}
						tempVO = ((AssVO[]) (AssVO[]) value)[0];
					}

					getAssVo()[r].setPk_Checkvalue(tempVO.getPk_Checkvalue());
					getAssVo()[r].setCheckvaluecode(tempVO.getCheckvaluecode());
					getAssVo()[r].setCheckvaluename(tempVO.getCheckvaluename());

					getAssVo()[r].setM_classid(tempVO.getM_classid());
					getAssVo()[r].setM_digit(tempVO.getM_digit());
					getAssVo()[r].setM_length(tempVO.getM_length());
					//20170417 tsy 保存到值时，设置data为null，当data=null时，getCheckFlag为true
					if (getAssVo()[r].getPk_Checkvalue()!=null && !"".equals(getAssVo()[r].getPk_Checkvalue())) {
						getAssVo()[r].setUserData(null);
					}
					//20170417 end
				}
			}

			if (c == 0) {
				getAssVo()[r].setUserData(value);
			}
		} catch (Exception e) {
		}
	}

	public void setInfo(GlBdinfoVO[] m_info) {
		this.m_info = m_info;
	}

	public void setPk_unit(String pk_unit) {
		this.pk_unit = pk_unit;
	}

	public String getPk_unit() {
		return this.pk_unit;
	}

	public void setStddate(String m_stddate) {
		this.m_stddate = m_stddate;
	}

	public String getStddate() {
		if (this.m_stddate == null)
			return GlWorkBench.getBusiDate().toStdString();
		return this.m_stddate;
	}
}