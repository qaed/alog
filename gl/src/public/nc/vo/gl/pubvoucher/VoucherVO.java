
package nc.vo.gl.pubvoucher;

import java.util.*;
import nc.bs.logging.Logger;
import nc.itf.gl.voucher.IBusiOperation;
import nc.pubitf.fip.external.IDesBillVOInterface;
import nc.vo.fip.service.FipRelationInfoVO;
import nc.vo.gl.aggvoucher.FreeValueVO;
import nc.vo.gl.aggvoucher.MDDetail;
import nc.vo.gl.vatdetail.VatDetailVO;
import nc.vo.glcom.ass.AssVO;
import nc.vo.glpub.IVoAccess;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.*;
import nc.vo.pub.lang.*;

import org.apache.commons.lang.StringUtils;

// Referenced classes of package nc.vo.gl.pubvoucher:
//            DetailVO, VoucherModflagTool, ModifyFlagConvertTool

public class VoucherVO extends CircularlyAccessibleValueObject
    implements IVoAccess, IBusiOperation, IDesBillVOInterface
{

    public UFBoolean getIsmodelrecflag()
    {
        if(ismodelrecflag == null)
            return UFBoolean.FALSE;
        else
            return ismodelrecflag;
    }

    public void setIsmodelrecflag(UFBoolean ismodelrecflag)
    {
        this.ismodelrecflag = ismodelrecflag;
    }

    public VoucherVO()
    {
        ismodelrecflag = UFBoolean.FALSE;
        m_num = Integer.valueOf(-2147483648);
        isdifflag = UFBoolean.FALSE;
        hasCashflowModified = false;
        isNoFactorChanged = false;
        preaccountflag = null;
        editflag = null;
        fipInfo = null;
    }

    public VoucherVO(String newPk_voucher)
    {
        ismodelrecflag = UFBoolean.FALSE;
        m_num = Integer.valueOf(-2147483648);
        isdifflag = UFBoolean.FALSE;
        hasCashflowModified = false;
        isNoFactorChanged = false;
        preaccountflag = null;
        editflag = null;
        fipInfo = null;
        m_pk_voucher = newPk_voucher;
    }

    public void addDetail(DetailVO detail)
    {
        getDetail_Create().addElement(detail.clone());
    }

    public void clearEmptyDetail()
    {
        int numDetails = getNumDetails();
        for(int i = 0; i < numDetails; i++)
        {
            DetailVO detail = getDetail(i);
            if((detail.getPk_accasoa() == null || detail.getPk_accasoa().trim().equals("")) && detail.getAssid() == null && (detail.getLocalcreditamount() == null || detail.getLocalcreditamount().doubleValue() == 0.0D) && (detail.getLocaldebitamount() == null || detail.getLocaldebitamount().doubleValue() == 0.0D) && (detail.getDebitamount() == null || detail.getDebitamount().doubleValue() == 0.0D) && (detail.getCreditamount() == null || detail.getCreditamount().doubleValue() == 0.0D) && (detail.getDebitquantity() == null || detail.getDebitquantity().doubleValue() == 0.0D) && (detail.getCreditquantity() == null || detail.getCreditquantity().doubleValue() == 0.0D))
            {
                deleteDetail(i);
                i--;
                numDetails--;
            }
        }

    }

    public void clearDetail()
    {
        int numDetails = getNumDetails();
        boolean isbalance = getTotaldebit().sub(getTotalcredit()).abs().compareTo(new UFDouble(8.9999999999999996E-007D)) <= 0;
        for(int i = 0; i < numDetails; i++)
        {
            DetailVO detail = getDetail(i);
            if((detail.getPk_accasoa() == null || detail.getPk_accasoa().trim().equals("")) && detail.getAssid() == null && (detail.getLocalcreditamount() == null || detail.getLocalcreditamount().doubleValue() == 0.0D) && (detail.getLocaldebitamount() == null || detail.getLocaldebitamount().doubleValue() == 0.0D) && (detail.getDebitamount() == null || detail.getDebitamount().doubleValue() == 0.0D) && (detail.getCreditamount() == null || detail.getCreditamount().doubleValue() == 0.0D) && (detail.getDebitquantity() == null || detail.getDebitquantity().doubleValue() == 0.0D) && (detail.getCreditquantity() == null || detail.getCreditquantity().doubleValue() == 0.0D) && (detail.getGroupdebitamount() == null || detail.getGroupdebitamount().doubleValue() == 0.0D) && (detail.getGroupcreditamount() == null || detail.getGroupcreditamount().doubleValue() == 0.0D) && (detail.getGlobaldebitamount() == null || detail.getGlobaldebitamount().doubleValue() == 0.0D) && (detail.getGlobalcreditamount() == null || detail.getGlobalcreditamount().doubleValue() == 0.0D))
            {
                deleteDetail(i);
                i--;
                numDetails--;
            }
            if(isbalance && detail.getPk_accasoa() != null && detail.getVatdetail() == null && (detail.getLocalcreditamount() == null || detail.getLocalcreditamount().doubleValue() == 0.0D) && (detail.getLocaldebitamount() == null || detail.getLocaldebitamount().doubleValue() == 0.0D) && (detail.getDebitamount() == null || detail.getDebitamount().doubleValue() == 0.0D) && (detail.getCreditamount() == null || detail.getCreditamount().doubleValue() == 0.0D) && (detail.getDebitquantity() == null || detail.getDebitquantity().doubleValue() == 0.0D) && (detail.getCreditquantity() == null || detail.getCreditquantity().doubleValue() == 0.0D) && (detail.getFraccreditamount() == null || detail.getFraccreditamount().doubleValue() == 0.0D) && (detail.getFracdebitamount() == null || detail.getFracdebitamount().doubleValue() == 0.0D) && (detail.getGroupdebitamount() == null || detail.getGroupdebitamount().doubleValue() == 0.0D) && (detail.getGroupcreditamount() == null || detail.getGroupcreditamount().doubleValue() == 0.0D) && (detail.getGlobaldebitamount() == null || detail.getGlobaldebitamount().doubleValue() == 0.0D) && (detail.getGlobalcreditamount() == null || detail.getGlobalcreditamount().doubleValue() == 0.0D))
            {
                deleteDetail(i);
                i--;
                numDetails--;
            }
        }

    }

    public Object clone()
    {
        Object o = null;
        try
        {
            o = super.clone();
        }
        catch(Exception e) { }
        VoucherVO voucher = (VoucherVO)o;
        if(voucher == null)
            voucher = new VoucherVO();
        voucher.setTs(getTs());
        voucher.setPk_voucher(getPk_voucher());
        voucher.setPk_vouchertype(getPk_vouchertype());
        voucher.setYear(getYear());
        voucher.setPeriod(getPeriod());
        voucher.setNo(getNo());
        voucher.setPrepareddate(getPrepareddate());
        voucher.setTallydate(getTallydate());
        voucher.setAttachment(getAttachment());
        voucher.setPk_prepared(getPk_prepared());
        voucher.setPk_checked(getPk_checked());
        voucher.setPk_casher(getPk_casher());
        voucher.setPk_manager(getPk_manager());
        voucher.setSignflag(getSignflag());
        voucher.setModifyflag(getModifyflag());
        voucher.setDetailmodflag(getDetailmodflag());
        voucher.setDiscardflag(getDiscardflag());
        voucher.setPk_system(getPk_system());
        voucher.setAddclass(getAddclass());
        voucher.setModifyclass(getModifyclass());
        voucher.setDeleteclass(getDeleteclass());
        voucher.setVoucherkind(getVoucherkind());
        voucher.setTotaldebit(getTotaldebit());
        voucher.setTotalcredit(getTotalcredit());
        voucher.setTotaldebitgroup(getTotaldebitgroup());
        voucher.setTotalcreditgroup(getTotalcreditgroup());
        voucher.setTotaldebitglobal(getTotaldebitglobal());
        voucher.setTotalcreditglobal(getTotalcreditglobal());
        voucher.setExplanation(getExplanation());
        voucher.setContrastflag(getContrastflag());
        voucher.setErrmessage(getErrmessage());
        voucher.setM_adjustperiod(getM_adjustperiod());
        voucher.setFree1(getFree1());
        voucher.setFree2(getFree2());
        voucher.setFree3(getFree3());
        voucher.setFree4(getFree4());
        voucher.setFree5(getFree5());
        voucher.setFree6(getFree6());
        voucher.setFree7(getFree7());
        voucher.setFree8(getFree8());
        voucher.setFree9(getFree9());
        voucher.setFree10(getFree10());
        voucher.setPk_setofbook(getPk_setofbook());
        voucher.setPk_org(getPk_org());
        voucher.setPk_sourcepk(getPk_sourcepk());
        voucher.setConvertflag(getConvertflag());
        voucher.setOffervoucher(getOffervoucher());
        voucher.setIsdifflag(getIsdifflag());
        voucher.setErrmessageh(getErrmessageh());
        voucher.setPk_group(getPk_group());
        voucher.setM_adjustperiod(getM_adjustperiod());
        voucher.setM_isVerify(getM_isVerify());
        voucher.setCreator(getCreator());
        voucher.setCreationtime(getCreationtime());
        voucher.setModifier(getModifier());
        voucher.setModifiedtime(getModifiedtime());
        voucher.setFreevalue1(getFreevalue1());
        voucher.setFreevalue2(getFreevalue2());
        voucher.setFreevalue3(getFreevalue3());
        voucher.setFreevalue4(getFreevalue4());
        voucher.setFreevalue5(getFreevalue5());
        voucher.setBillmaker(getBillmaker());
        voucher.setApprover(getApprover());
        voucher.setTempsaveflag(getTempsaveflag());
        voucher.setPk_org_v(getPk_org_v());
        Vector t_details = new Vector();
        for(int i = 0; i < getNumDetails(); i++)
        {
            DetailVO t_detail = (DetailVO)getDetail(i).clone();
            t_details.addElement(t_detail);
        }

        voucher.setDetail(t_details);
        voucher.setIsmatched(getIsmatched());
        voucher.setUserData(getUserData());
        voucher.setFipInfo(getFipInfo());
        return voucher;
    }

    public void deleteDetail(int iIndex)
    {
        DetailVO details[] = getDetails();
        Vector vecdetails = new Vector();
        if(iIndex < 0)
            return;
        DetailVO detail = getDetail(iIndex);
        for(int i = 0; i < iIndex; i++)
        {
            details[i].setDetailindex(new Integer(i + 1));
            vecdetails.addElement(details[i]);
        }

        for(int i = iIndex + 1; i < details.length; i++)
        {
            if(details[i].getDetailindex() != null)
                details[i].setDetailindex(new Integer(i));
            vecdetails.addElement(details[i]);
        }

        setDetail(vecdetails);
        if(detail.getLocalcreditamount() != null && detail.getLocaldebitamount() != null)
        {
            setTotalcredit(getTotalcredit().sub(detail.getLocalcreditamount()));
            setTotaldebit(getTotaldebit().sub(detail.getLocaldebitamount()));
        }
        if(getTotaldebitgroup() == null)
            setTotaldebitgroup(UFDouble.ZERO_DBL);
        if(getTotalcreditgroup() == null)
            setTotalcreditgroup(UFDouble.ZERO_DBL);
        if(getTotaldebitglobal() == null)
            setTotaldebitglobal(UFDouble.ZERO_DBL);
        if(getTotalcreditglobal() == null)
            setTotalcreditglobal(UFDouble.ZERO_DBL);
        setTotaldebitgroup(getTotaldebitgroup().sub(detail.getGroupdebitamount()));
        setTotalcreditgroup(getTotalcreditgroup().sub(detail.getGroupcreditamount()));
        setTotaldebitglobal(getTotaldebitglobal().sub(detail.getGlobaldebitamount()));
        setTotalcreditglobal(getTotalcreditglobal().sub(detail.getGlobalcreditamount()));
    }

    public String getAddclass()
    {
        return m_addclass;
    }

    public Integer getAttachment()
    {
        return m_attachment;
    }

    public String[] getAttributeNames()
    {
        return (new String[] {
            "addclass", "attachment", "contrastflag", "deleteclass", "detailmodflag", "discardflag", "errmessage", "explanation", "free1", "free10", 
            "free2", "free3", "free4", "free5", "free6", "free7", "free8", "free9", "modifyclass", "modifyflag", 
            "no", "period", "pk_casher", "pk_checked", "pk_corp", "pk_manager", "pk_prepared", "pk_sob", "pk_system", "pk_vouchertype", 
            "prepareddate", "signflag", "tallydate", "totalcredit", "totaldebit", "voucherkind", "year", "signdate", "checkeddate", "billmaker", 
            "approver", "creator", "tempsaveflag"
        });
    }

    public Object getAttributeValue(String attributeName)
    {
        if(attributeName.equals("pk_voucher"))
            return m_pk_voucher;
        if(attributeName.equals("addclass"))
            return m_addclass;
        if(attributeName.equals("attachment"))
            return m_attachment;
        if(attributeName.equals("contrastflag"))
            return m_contrastflag;
        if(attributeName.equals("deleteclass"))
            return m_deleteclass;
        if(attributeName.equals("detailmodflag"))
            return m_detailmodflag;
        if(attributeName.equals("discardflag"))
            return m_discardflag;
        if(attributeName.equals("errmessage"))
            return m_errmessage;
        if(attributeName.equals("explanation"))
            return m_explanation;
        if(attributeName.equals("free1"))
            return m_free1;
        if(attributeName.equals("adjustperiod"))
            return m_adjustperiod;
        if(attributeName.equals("free10"))
            return m_free10;
        if(attributeName.equals("free2"))
            return m_free2;
        if(attributeName.equals("free3"))
            return m_free3;
        if(attributeName.equals("free4"))
            return m_free4;
        if(attributeName.equals("free5"))
            return m_free5;
        if(attributeName.equals("free6"))
            return m_free6;
        if(attributeName.equals("free7"))
            return m_free7;
        if(attributeName.equals("free8"))
            return m_free8;
        if(attributeName.equals("free9"))
            return m_free9;
        if(attributeName.equals("modifyclass"))
            return m_modifyclass;
        if(attributeName.equals("modifyflag"))
            return m_modifyflag;
        if(attributeName.equals("num"))
            return m_num;
        if(attributeName.equals("period"))
            return m_period;
        if(attributeName.equals("pk_casher"))
            return m_pk_casher;
        if(attributeName.equals("pk_checked"))
            return m_pk_checked;
        if(attributeName.equals("pk_manager"))
            return m_pk_manager;
        if(attributeName.equals("pk_prepared"))
            return m_pk_prepared;
        if(attributeName.equals("pk_system"))
            return m_pk_system;
        if(attributeName.equals("pk_vouchertype"))
            return m_pk_vouchertype;
        if(attributeName.equals("prepareddate"))
            return m_prepareddate;
        if(attributeName.equals("signflag"))
            return m_signflag;
        if(attributeName.equals("tallydate"))
            return m_tallydate;
        if(attributeName.equals("totalcredit"))
            return m_totalcredit;
        if(attributeName.equals("totaldebit"))
            return m_totaldebit;
        if(attributeName.equals("voucherkind"))
            return m_voucherkind;
        if(attributeName.equals("year"))
            return m_year;
        if(attributeName.equals("signdate"))
            return m_signdate;
        if(attributeName.equals("checkeddate"))
            return m_checkeddate;
        if(attributeName.equalsIgnoreCase("billmaker"))
            return billmaker;
        if(attributeName.equalsIgnoreCase("approver"))
            return approver;
        if(attributeName.equals("creator"))
            return creator;
        if(attributeName.equals("tempsaveflag"))
            return m_tempsaveflag;
        else
            return null;
    }

    /**
     * @deprecated Method getCashername is deprecated
     */

    public String getCashername()
    {
        return m_cashername;
    }

    public UFDate getCheckeddate()
    {
        return m_checkeddate;
    }

    /**
     * @deprecated Method getCheckedname is deprecated
     */

    public String getCheckedname()
    {
        return m_checkedname;
    }

    public Integer getContrastflag()
    {
        return m_contrastflag;
    }

    public UFBoolean getConvertflag()
    {
        return m_convertflag;
    }

    /**
     * @deprecated Method getCorpname is deprecated
     */

    public String getCorpname()
    {
        return m_corpname;
    }

    public String getDeleteclass()
    {
        return m_deleteclass;
    }

    public Vector getDetail()
    {
        return m_detail;
    }

    public DetailVO getDetail(int iIndex)
    {
        if(iIndex >= getNumDetails())
            return null;
        else
            return (DetailVO)(DetailVO)getDetail_Create().elementAt(iIndex);
    }

    protected Vector getDetail_Create()
    {
        if(m_detail == null)
            m_detail = new Vector();
        return m_detail;
    }

    public UFBoolean getDetailmodflag()
    {
        return m_detailmodflag;
    }

    public DetailVO[] getDetails()
    {
        DetailVO t_detail[] = new DetailVO[getDetail_Create().size()];
        getDetail_Create().copyInto(t_detail);
        return t_detail;
    }

    public UFBoolean getDiscardflag()
    {
        return m_discardflag;
    }

    public String getEntityName()
    {
        return "Voucher";
    }

    public String getErrmessage()
    {
        return m_errmessage;
    }

    public String getExplanation()
    {
        return m_explanation;
    }

    public String getFree1()
    {
        return m_free1;
    }

    public String getM_adjustperiod()
    {
        return m_adjustperiod;
    }

    public String getFree10()
    {
        return m_free10;
    }

    public String getFree2()
    {
        return m_free2;
    }

    public String getFree3()
    {
        return m_free3;
    }

    public String getFree4()
    {
        return m_free4;
    }

    public String getFree5()
    {
        return m_free5;
    }

    public String getFree6()
    {
        return m_free6;
    }

    public String getFree7()
    {
        return m_free7;
    }

    public String getFree8()
    {
        return m_free8;
    }

    public String getFree9()
    {
        return m_free9;
    }

    public String getFreevalue1()
    {
        return m_freevalue1;
    }

    public String getFreevalue2()
    {
        return m_freevalue2;
    }

    public String getFreevalue3()
    {
        return m_freevalue3;
    }

    public String getFreevalue4()
    {
        return m_freevalue4;
    }

    public String getFreevalue5()
    {
        return m_freevalue5;
    }

    public Boolean getIsmatched()
    {
        return m_ismatched;
    }

    public UFBoolean getIsOutSubj()
    {
        return m_isoutsubj;
    }

    /**
     * @deprecated Method getManagername is deprecated
     */

    public String getManagername()
    {
        return m_managername;
    }

    public String getModifyclass()
    {
        return m_modifyclass;
    }

    public String getModifyflag()
    {
        return m_modifyflag;
    }

    public Integer getNo()
    {
        if(m_num != null && -2147483648 == m_num.intValue())
            return Integer.valueOf(0);
        else
            return m_num;
    }

    public int getNumDetails()
    {
        return getDetail_Create().size();
    }

    public String getPeriod()
    {
        return m_period;
    }

    public String getPk_casher()
    {
        return m_pk_casher;
    }

    public String getPk_checked()
    {
        return m_pk_checked;
    }

    public String getPk_setofbook()
    {
        return m_pk_setofbook;
    }

    public String getPk_org()
    {
        return m_pk_org;
    }

    public String getPk_accountingbook()
    {
        return m_pk_accountingbook;
    }

    public String getPk_manager()
    {
        return m_pk_manager;
    }

    public String getPk_prepared()
    {
        return m_pk_prepared;
    }

    public String getPk_sourcepk()
    {
        return m_pk_sourcepk;
    }

    public String getPk_system()
    {
        return m_pk_system;
    }

    public String getPk_voucher()
    {
        return m_pk_voucher;
    }

    public String getPk_vouchertype()
    {
        return m_pk_vouchertype;
    }

    public UFDate getPrepareddate()
    {
        return m_prepareddate;
    }

    /**
     * @deprecated Method getPreparedname is deprecated
     */

    public String getPreparedname()
    {
        return m_preparedname;
    }

    public String getPrimaryKey()
    {
        return m_pk_voucher;
    }

    public UFDate getSigndate()
    {
        return m_signdate;
    }

    public UFBoolean getSignflag()
    {
        return m_signflag;
    }

    /**
     * @deprecated Method getSystemname is deprecated
     */

    public String getSystemname()
    {
        return m_systemname;
    }

    public UFDate getTallydate()
    {
        return m_tallydate;
    }

    public UFDouble getTotalcredit()
    {
        return m_totalcredit;
    }

    public UFDouble getTotaldebit()
    {
        return m_totaldebit;
    }

    public Object getUserData()
    {
        return m_userdata;
    }

    public Object getValue(int intKey)
    {
        switch(intKey)
        {
        case 2: // '\002'
            return getDetails();

        case 0: // '\0'
            return this;

        case 10: // '\n'
            return getPk_voucher();

        case 11: // '\013'
            return getPk_vouchertype();

        case 14: // '\016'
            return getYear();

        case 15: // '\017'
            return getPeriod();

        case 16: // '\020'
            return getNo();

        case 17: // '\021'
            return getPrepareddate();

        case 18: // '\022'
            return getTallydate();

        case 19: // '\023'
            return getAttachment();

        case 20: // '\024'
            return getPk_prepared();

        case 602: 
            return getCreator();

        case 21: // '\025'
            return getPk_checked();

        case 22: // '\026'
            return getPk_casher();

        case 23: // '\027'
            return getPk_manager();

        case 24: // '\030'
            return getSignflag();

        case 25: // '\031'
            return getModifyflag();

        case 26: // '\032'
            return getDetailmodflag();

        case 27: // '\033'
            return getDiscardflag();

        case 28: // '\034'
            return getPk_system();

        case 29: // '\035'
            return getAddclass();

        case 30: // '\036'
            return getModifyclass();

        case 31: // '\037'
            return getDeleteclass();

        case 32: // ' '
            return getVoucherkind();

        case 33: // '!'
            return getTotaldebit();

        case 34: // '"'
            return getTotalcredit();

        case 381: 
            return getTotaldebitgroup();

        case 382: 
            return getTotalcreditgroup();

        case 383: 
            return getTotaldebitglobal();

        case 384: 
            return getTotalcreditglobal();

        case 35: // '#'
            return getExplanation();

        case 36: // '$'
            return getContrastflag();

        case 37: // '%'
            return getErrmessage();

        case 12: // '\f'
            return getM_adjustperiod();

        case 13: // '\r'
            return getM_isVerify();

        case 38: // '&'
            return getFree1();

        case 39: // '\''
            return getFree2();

        case 40: // '('
            return getFree3();

        case 41: // ')'
            return getFree4();

        case 42: // '*'
            return getFree5();

        case 43: // '+'
            return getFree6();

        case 44: // ','
            return getFree7();

        case 45: // '-'
            return getFree8();

        case 46: // '.'
            return getFree9();

        case 47: // '/'
            return getFree10();

        case 54: // '6'
            return getPk_setofbook();

        case 53: // '5'
            return getPk_org();

        case 55: // '7'
            return getPk_accountingbook();

        case 56: // '8'
            return getPk_sourcepk();

        case 57: // '9'
            return getConvertflag();

        case 48: // '0'
            return getFreevalue1();

        case 49: // '1'
            return getFreevalue2();

        case 50: // '2'
            return getFreevalue3();

        case 51: // '3'
            return getFreevalue4();

        case 52: // '4'
            return getFreevalue5();

        case 201: 
            return getVouchertypename();

        case 202: 
            return getCorpname();

        case 203: 
            return getPreparedname();

        case 204: 
            return getCheckedname();

        case 205: 
            return getCashername();

        case 206: 
            return getManagername();

        case 207: 
            return getSystemname();

        case 1: // '\001'
            return getDetail();

        case 209: 
            return getIsmatched();

        case 217: 
            return getUserData();

        case 218: 
            return getTotalcredit().sub(getTotaldebit(), getTotalcredit().getPower()).abs();

        case 405: 
            return getTempsaveflag();

        case 63: // '?'
            return getPk_org();

        case 64: // '@'
            return getPk_org_v();
        }
        throw new RuntimeException((new StringBuilder()).append("No such VoucherKey::").append(intKey).toString());
    }

    public Integer getVoucherkind()
    {
        return m_voucherkind;
    }

    /**
     * @deprecated Method getVouchertypename is deprecated
     */

    public String getVouchertypename()
    {
        return m_vouchertypename;
    }

    public String getYear()
    {
        return m_year;
    }

    public void insertDetail(DetailVO detail, int index)
    {
        getDetail_Create().insertElementAt(detail.clone(), index);
    }

    public void setAddclass(String newAddclass)
    {
        m_addclass = newAddclass;
    }

    public void setAttachment(Integer newAttachment)
    {
        m_attachment = newAttachment;
    }

    public void setAttributeValue(String name, Object value)
    {
        try
        {
            if(name.equals("pk_voucher"))
                m_pk_voucher = (String)value;
            else
            if(name.equals("addclass"))
                m_addclass = (String)value;
            else
            if(name.equals("attachment"))
                m_attachment = (Integer)value;
            else
            if(name.equals("contrastflag"))
                m_contrastflag = (Integer)value;
            else
            if(name.equals("deleteclass"))
                m_deleteclass = (String)value;
            else
            if(name.equals("detailmodflag"))
                m_detailmodflag = (UFBoolean)value;
            else
            if(name.equals("discardflag"))
                m_discardflag = (UFBoolean)(value != null ? value : UFBoolean.FALSE);
            else
            if(name.equals("errmessage"))
                m_errmessage = (String)value;
            else
            if(name.equals("explanation"))
                m_explanation = (String)value;
            else
            if(name.equals("free1"))
                m_free1 = (String)value;
            else
            if(name.equals("adjustperiod"))
                m_adjustperiod = (String)value;
            else
            if(name.equals("free10"))
                m_free10 = (String)value;
            else
            if(name.equals("free2"))
                m_free2 = (String)value;
            else
            if(name.equals("free3"))
                m_free3 = (String)value;
            else
            if(name.equals("free4"))
                m_free4 = (String)value;
            else
            if(name.equals("free5"))
                m_free5 = (String)value;
            else
            if(name.equals("free6"))
                m_free6 = (String)value;
            else
            if(name.equals("free7"))
                m_free7 = (String)value;
            else
            if(name.equals("free8"))
                m_free8 = (String)value;
            else
            if(name.equals("free9"))
                m_free9 = (String)value;
            else
            if(name.equals("modifyclass"))
                m_modifyclass = (String)value;
            else
            if(name.equals("modifyflag"))
                m_modifyflag = (String)value;
            else
            if(name.equals("num"))
                m_num = (Integer)value;
            else
            if(name.equals("period"))
                m_period = (String)value;
            else
            if(name.equals("pk_casher"))
                m_pk_casher = (String)value;
            else
            if(name.equals("pk_checked"))
                m_pk_checked = (String)value;
            else
            if(name.equals("pk_manager"))
                m_pk_manager = (String)value;
            else
            if(name.equals("pk_prepared"))
                m_pk_prepared = (String)value;
            else
            if(name.equals("pk_system"))
                m_pk_system = (String)value;
            else
            if(name.equals("pk_vouchertype"))
                m_pk_vouchertype = (String)value;
            else
            if(name.equals("prepareddate"))
                m_prepareddate = (UFDate)value;
            else
            if(name.equals("signflag"))
                m_signflag = (UFBoolean)value;
            else
            if(name.equals("tallydate"))
                m_tallydate = (UFDate)value;
            else
            if(name.equals("totalcredit"))
                m_totalcredit = (UFDouble)value;
            else
            if(name.equals("totaldebit"))
                m_totaldebit = (UFDouble)value;
            else
            if(name.equals("voucherkind"))
                m_voucherkind = (Integer)value;
            else
            if(name.equals("year"))
                m_year = (String)value;
            else
            if(name.equals("signdate"))
                m_signdate = (UFDate)value;
            else
            if(name.equals("checkeddate"))
                m_checkeddate = (UFDate)value;
            else
            if(name.equals("billmaker"))
                billmaker = (String)value;
            else
            if(name.equals("approver"))
                approver = (String)value;
            else
            if(name.equals("creator"))
                creator = (String)value;
            else
            if(name.equals("tempsaveflag"))
                m_tempsaveflag = (UFBoolean)value;
            else
            if(name.equals("aggdetails"))
                setDetails((MDDetail[])(MDDetail[])value);
            else
            if(name.equals("details"))
                setDetails((DetailVO[])(DetailVO[])value);
        }
        catch(ClassCastException e)
        {
            Logger.error(e.getMessage(), e);
            if(value == null)
                value = "";
            throw new ClassCastException(NCLangRes4VoTransl.getNCLangRes().getStrByID("2002GL502", "UPP2002GL502-000264", null, new String[] {
                name, (String)value
            }));
        }
    }

    private void setDetails(MDDetail value[])
    {
        setDetailsBySetter(value);
    }

    private void setDetailsBySetter(MDDetail value[])
    {
        if(value == null)
            return;
        MDDetail srcDetail = null;
        for(int index = 0; index < value.length; index++)
        {
            srcDetail = value[index];
            DetailVO newDetail = new DetailVO();
            newDetail.setUnitname(srcDetail.getUnitname());
            newDetail.setPeriod(srcDetail.getPeriodv());
            newDetail.setErrmessageh(srcDetail.getErrmessage());
            newDetail.setExcrate3(srcDetail.getExcrate3());
            newDetail.setExcrate2(srcDetail.getExcrate2());
            newDetail.setExcrate1(srcDetail.getExcrate1());
            newDetail.setExcrate4(srcDetail.getExcrate4());
            newDetail.setDebitamount(srcDetail.getDebitamount() != null ? srcDetail.getDebitamount() : UFDouble.ZERO_DBL);
            newDetail.setCreditamount(srcDetail.getCreditamount() != null ? srcDetail.getCreditamount() : UFDouble.ZERO_DBL);
            newDetail.setPk_otherorgbook(srcDetail.getPk_otherorgbook());
            newDetail.setIsdifflag(srcDetail.getIsdifflag());
            newDetail.setPk_sob(srcDetail.getPk_setofbook());
            newDetail.setDebitquantity(srcDetail.getDebitquantity() != null ? srcDetail.getDebitquantity() : UFDouble.ZERO_DBL);
            newDetail.setDiscardflag(srcDetail.getDiscardflagv());
            newDetail.setConvertflag(srcDetail.getConvertflag());
            newDetail.setPrice(srcDetail.getPrice());
            newDetail.setYear(srcDetail.getYearv());
            if(srcDetail.getVerifydate() != null)
                newDetail.setVerifydate(srcDetail.getVerifydate().toString());
            newDetail.setNo(srcDetail.getNov());
            newDetail.setPk_group(srcDetail.getPk_group());
            newDetail.setFree9(srcDetail.getFreevalue9());
            newDetail.setFreevalue5(srcDetail.getFreevalue5());
            newDetail.setPk_vouchertype(srcDetail.getPk_vouchertypev());
            newDetail.setFreevalue6(srcDetail.getFreevalue6());
            newDetail.setTempsaveflag(srcDetail.getTempsaveflag());
            newDetail.setFreevalue7(srcDetail.getFreevalue7());
            newDetail.setFreevalue8(srcDetail.getFreevalue8());
            newDetail.setPk_manager(srcDetail.getPk_managerv());
            newDetail.setFree7(srcDetail.getFree7());
            newDetail.setDetailindex(srcDetail.getDetailindex());
            newDetail.setFree9(srcDetail.getFree9());
            newDetail.setFree10(srcDetail.getFree10());
            newDetail.setFree8(srcDetail.getFree8());
            if(srcDetail.getSigndatev() != null)
                newDetail.setSigndate(srcDetail.getSigndatev().toString());
            newDetail.setFreevalue23(srcDetail.getFreevalue23());
            newDetail.setFreevalue24(srcDetail.getFreevalue24());
            newDetail.setAdjustperiod(srcDetail.getAdjustperiod());
            newDetail.setFreevalue21(srcDetail.getFreevalue21());
            newDetail.setFreevalue22(srcDetail.getFreevalue22());
            newDetail.setBusireconno(srcDetail.getBusireconno());
            newDetail.setCheckdate(srcDetail.getCheckdate());
            newDetail.setFreevalue20(srcDetail.getFreevalue20());
            newDetail.setFreevalue2(srcDetail.getFreevalue2());
            newDetail.setFreevalue1(srcDetail.getFreevalue1());
            newDetail.setFreevalue4(srcDetail.getFreevalue4());
            newDetail.setFreevalue29(srcDetail.getFreevalue29());
            newDetail.setFreevalue3(srcDetail.getFreevalue3());
            newDetail.setFreevalue27(srcDetail.getFreevalue27());
            newDetail.setFreevalue28(srcDetail.getFreevalue28());
            newDetail.setFreevalue25(srcDetail.getFreevalue25());
            newDetail.setFreevalue26(srcDetail.getFreevalue26());
            newDetail.setPk_system(srcDetail.getPk_systemv());
            newDetail.setOppositesubj(srcDetail.getOppositesubj());
            newDetail.setPk_innersob(srcDetail.getPk_innersob());
            newDetail.setFreevalue30(srcDetail.getFreevalue30());
            newDetail.setPk_accasoa(srcDetail.getPk_accasoa());
            if(srcDetail.getInnerbusdate() != null)
                newDetail.setInnerbusdate(srcDetail.getInnerbusdate().toString());
            newDetail.setGlobalcreditamount(srcDetail.getGlobalcreditamount() != null ? srcDetail.getGlobalcreditamount() : UFDouble.ZERO_DBL);
            newDetail.setGlobaldebitamount(srcDetail.getGlobaldebitamount() != null ? srcDetail.getGlobaldebitamount() : UFDouble.ZERO_DBL);
            newDetail.setBankaccount(srcDetail.getBankaccount());
            FreeValueVO srcVOs[] = srcDetail.getAssidarray();
            if(srcVOs != null)
            {
                AssVO newVOs[] = new AssVO[srcVOs.length];
                for(int i = 0; i < newVOs.length; i++)
                {
                    newVOs[i] = new AssVO();
                    newVOs[i].setPk_Checktype(srcVOs[i].getChecktype());
                    newVOs[i].setPk_Checkvalue(srcVOs[i].getCheckvalue());
                    newVOs[i].setCheckvaluecode(srcVOs[i].getValuecode());
                    newVOs[i].setCheckvaluename(srcVOs[i].getValuename());
                }

                newDetail.setAss(newVOs);
            }
            VatDetailVO vatDetailVo = srcDetail.getVatdetailarray();
            newDetail.setVatdetail(vatDetailVo);
            newDetail.setPk_sourcepk(srcDetail.getPk_sourcepk());
            newDetail.setAssid(srcDetail.getAssid());
            newDetail.setBilltype(srcDetail.getBilltype());
            newDetail.setPk_currtype(srcDetail.getPk_currtype());
            newDetail.setNetbankflag(srcDetail.getNetbankflag());
            newDetail.setPk_othercorp(srcDetail.getPk_othercorp());
            newDetail.setExplanation(srcDetail.getExplanation());
            newDetail.setPk_voucher(srcDetail.getPk_voucher());
            newDetail.setFreevalue15(srcDetail.getFreevalue15());
            newDetail.setFreevalue14(srcDetail.getFreevalue14());
            newDetail.setFreevalue17(srcDetail.getFreevalue17());
            newDetail.setModifyflag(srcDetail.getModifyflag());
            newDetail.setFreevalue16(srcDetail.getFreevalue16());
            newDetail.setFreevalue19(srcDetail.getFreevalue19());
            newDetail.setFreevalue19(srcDetail.getFreevalue19());
            newDetail.setCheckstyle(srcDetail.getCheckstyle());
            newDetail.setFreevalue11(srcDetail.getFreevalue11());
            newDetail.setPrepareddate(srcDetail.getPrepareddatev());
            newDetail.setGroupdebitamount(srcDetail.getGroupdebitamount() != null ? srcDetail.getGroupdebitamount() : UFDouble.ZERO_DBL);
            newDetail.setGroupcreditamount(srcDetail.getGroupcreditamount() != null ? srcDetail.getGroupcreditamount() : UFDouble.ZERO_DBL);
            newDetail.setInnerbusno(srcDetail.getInnerbusno());
            newDetail.setFreevalue10(srcDetail.getFreevalue10());
            newDetail.setPk_detail(srcDetail.getPk_detail());
            newDetail.setFreevalue13(srcDetail.getFreevalue13());
            newDetail.setFreevalue12(srcDetail.getFreevalue12());
            newDetail.setErrmessage(srcDetail.getErrmessage());
            newDetail.setFracdebitamount(srcDetail.getFracdebitamount());
            newDetail.setPk_org(srcDetail.getPk_org());
            newDetail.setPk_unit(srcDetail.getPk_unit());
            newDetail.setPk_unit_v(srcDetail.getPk_unit_v());
            if(srcDetail.getDirection() != null)
                newDetail.setDirection(srcDetail.getDirection().toString());
            newDetail.setPk_innercorp(srcDetail.getPk_innerorg());
            newDetail.setPk_offerdetail(srcDetail.getPk_offerdetail());
            newDetail.setPk_org(srcDetail.getPk_org());
            newDetail.setPk_org_v(srcDetail.getPk_org_v());
            newDetail.setCreditquantity(srcDetail.getCreditquantity() != null ? srcDetail.getCreditquantity() : UFDouble.ZERO_DBL);
            newDetail.setCheckno(srcDetail.getCheckno());
            newDetail.setErrmessage2(srcDetail.getErrmessage2());
            newDetail.setContrastflag(srcDetail.getContrastflag());
            newDetail.setRecieptclass(srcDetail.getRecieptclass());
            newDetail.setVoucherkind(srcDetail.getVoucherkindv());
            newDetail.setFraccreditamount(srcDetail.getFraccreditamount());
            newDetail.setLocaldebitamount(srcDetail.getLocaldebitamount() != null ? srcDetail.getLocaldebitamount() : UFDouble.ZERO_DBL);
            newDetail.setLocalcreditamount(srcDetail.getLocalcreditamount() != null ? srcDetail.getLocalcreditamount() : UFDouble.ZERO_DBL);
            newDetail.setPk_accountingbook(srcDetail.getPk_accountingbook());
            newDetail.setVerifyno(srcDetail.getVerifyno());
            newDetail.setPk_cashflow(srcDetail.getCashflowitem());
            insertDetail(newDetail, index);
        }

    }

    public void setCashername(String newCashername)
    {
        m_cashername = newCashername;
    }

    public void setCheckeddate(UFDate newCheckeddate)
    {
        m_checkeddate = newCheckeddate;
    }

    public void setCheckedname(String newCheckedname)
    {
        m_checkedname = newCheckedname;
    }

    public void setContrastflag(Integer newContrastflag)
    {
        m_contrastflag = newContrastflag;
    }

    public void setConvertflag(UFBoolean newConvertflag)
    {
        m_convertflag = newConvertflag;
    }

    public void setCorpname(String newCorpname)
    {
        m_corpname = newCorpname;
    }

    public void setDeleteclass(String newDeleteclass)
    {
        m_deleteclass = newDeleteclass;
    }

    public void setDetail(Vector newDetail)
    {
        m_detail = newDetail;
    }

    public void setDetail(DetailVO newDetail, int iIndex)
    {
        m_detail.setElementAt(newDetail, iIndex);
    }

    public void setDetailmodflag(UFBoolean newDetailmodflag)
    {
        m_detailmodflag = newDetailmodflag;
    }

    public void setDetails(DetailVO newDetail[])
    {
        getDetail_Create().clear();
        if(newDetail == null)
            return;
        for(int i = 0; i < newDetail.length; i++)
            if(newDetail[i] != null)
                getDetail_Create().addElement(newDetail[i].clone());

        details = getDetails();
    }

    public void setDiscardflag(UFBoolean newDiscardflag)
    {
        m_discardflag = newDiscardflag;
    }

    public void setErrmessage(String newErrmessage)
    {
        m_errmessage = newErrmessage;
    }

    public void setExplanation(String newExplanation)
    {
        m_explanation = newExplanation;
    }

    public void setFree1(String newFree1)
    {
        m_free1 = newFree1;
    }

    public void setM_adjustperiod(String newFree1)
    {
        m_adjustperiod = newFree1;
    }

    public void setFree10(String newFree10)
    {
        m_free10 = newFree10;
    }

    public void setFree2(String newFree2)
    {
        m_free2 = newFree2;
    }

    public void setFree3(String newFree3)
    {
        m_free3 = newFree3;
    }

    public void setFree4(String newFree4)
    {
        m_free4 = newFree4;
    }

    public void setFree5(String newFree5)
    {
        m_free5 = newFree5;
    }

    public void setFree6(String newFree6)
    {
        m_free6 = newFree6;
    }

    public void setFree7(String newFree7)
    {
        m_free7 = newFree7;
    }

    public void setFree8(String newFree8)
    {
        m_free8 = newFree8;
    }

    public void setFree9(String newFree9)
    {
        m_free9 = newFree9;
    }

    public void setFreevalue1(String newM_freevalue1)
    {
        m_freevalue1 = newM_freevalue1;
    }

    public void setFreevalue2(String newM_freevalue)
    {
        m_freevalue2 = newM_freevalue;
    }

    public void setFreevalue3(String newM_freevalue)
    {
        m_freevalue3 = newM_freevalue;
    }

    public void setFreevalue4(String newM_freevalue)
    {
        m_freevalue4 = newM_freevalue;
    }

    public void setFreevalue5(String newM_freevalue)
    {
        m_freevalue5 = newM_freevalue;
    }

    public void setIsmatched(Boolean newM_ismatched)
    {
        m_ismatched = newM_ismatched;
    }

    public void setIsOutSubj(UFBoolean newM_isoutsubj)
    {
        m_isoutsubj = newM_isoutsubj;
    }

    public void setManagername(String newManagername)
    {
        m_managername = newManagername;
    }

    public void setModifyclass(String newModifyclass)
    {
        m_modifyclass = newModifyclass;
    }

    public void setModifyflag(String newModifyflag)
    {
        m_modifyflag = newModifyflag;
    }

    public void setNo(Integer newNo)
    {
        m_num = newNo;
        DetailVO details[] = getDetails();
        if(details != null && details.length > 0)
        {
            for(int i = 0; i < details.length; i++)
            {
                DetailVO detailVO = details[i];
                detailVO.setNo(newNo);
            }

        }
    }

    public void setPeriod(String newPeriod)
    {
        m_period = newPeriod;
    }

    public void setPk_casher(String newPk_casher)
    {
        m_pk_casher = newPk_casher;
    }

    public void setPk_checked(String newPk_checked)
    {
        m_pk_checked = newPk_checked;
        setApprover(newPk_checked);
    }

    public void setPk_setofbook(String newPk_setofbook)
    {
        m_pk_setofbook = newPk_setofbook;
    }

    public void setPk_org(String newPk_financeorg)
    {
        m_pk_org = newPk_financeorg;
    }

    public void setPk_accountingbook(String newPk_accountingbook)
    {
        m_pk_accountingbook = newPk_accountingbook;
    }

    public void setPk_manager(String newPk_manager)
    {
        m_pk_manager = newPk_manager;
    }

    public void setPk_prepared(String newPk_prepared)
    {
        m_pk_prepared = newPk_prepared;
        setBillmaker(newPk_prepared);
    }

    public void setPk_sourcepk(String newPk_sourcepk)
    {
        m_pk_sourcepk = newPk_sourcepk;
    }

    public void setPk_system(String newPk_system)
    {
        if("2002".equals(newPk_system))
            m_pk_system = "GL";
        else
            m_pk_system = newPk_system;
    }

    public void setPk_voucher(String newPk_voucher)
    {
        m_pk_voucher = newPk_voucher;
    }

    public void setPk_vouchertype(String newPk_vouchertype)
    {
        m_pk_vouchertype = newPk_vouchertype;
    }

    public void setPrepareddate(UFDate newPrepareddate)
    {
        m_prepareddate = newPrepareddate;
    }

    public void setPreparedname(String newPreparedname)
    {
        m_preparedname = newPreparedname;
    }

    public void setPrimaryKey(String newPk_voucher)
    {
        m_pk_voucher = newPk_voucher;
    }

    public void setSigndate(UFDate newSigndate)
    {
        m_signdate = newSigndate;
    }

    public void setSignflag(UFBoolean newSignflag)
    {
        m_signflag = newSignflag;
    }

    public void setSystemname(String newSystemname)
    {
        m_systemname = newSystemname;
    }

    public void setTallydate(UFDate newTallydate)
    {
        m_tallydate = newTallydate;
    }

    public void setTotalcredit(UFDouble newTotalcredit)
    {
        m_totalcredit = newTotalcredit;
    }

    public void setTotaldebit(UFDouble newTotaldebit)
    {
        m_totaldebit = newTotaldebit;
    }

    public void setUserData(Object objUserData)
    {
        m_userdata = objUserData;
    }

    public void setValue(int iKey, Object objNewValue)
    {
        switch(iKey)
        {
        case 2: // '\002'
            setDetails((DetailVO[])(DetailVO[])objNewValue);
            break;

        case 10: // '\n'
            setPk_voucher(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 11: // '\013'
            setPk_vouchertype(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 14: // '\016'
            setYear(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 15: // '\017'
            setPeriod(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 16: // '\020'
            setNo(objNewValue != null ? new Integer(objNewValue.toString().trim()) : new Integer(0));
            break;

        case 17: // '\021'
            setPrepareddate(objNewValue != null ? new UFDate(objNewValue.toString().trim()) : null);
            break;

        case 18: // '\022'
            setTallydate(objNewValue != null ? new UFDate(objNewValue.toString().trim()) : null);
            break;

        case 19: // '\023'
            setAttachment(objNewValue != null ? new Integer(objNewValue.toString().trim()) : new Integer(0));
            break;

        case 20: // '\024'
            setPk_prepared(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 21: // '\025'
            setPk_checked(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 22: // '\026'
            setPk_casher(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 23: // '\027'
            setPk_manager(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 24: // '\030'
            setSignflag(objNewValue != null ? UFBoolean.valueOf(objNewValue.toString().trim()) : UFBoolean.FALSE);
            break;

        case 25: // '\031'
            setModifyflag(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 26: // '\032'
            setDetailmodflag(objNewValue != null ? UFBoolean.valueOf(objNewValue.toString().trim()) : UFBoolean.TRUE);
            break;

        case 27: // '\033'
            setDiscardflag(objNewValue != null ? UFBoolean.valueOf(objNewValue.toString().trim()) : UFBoolean.FALSE);
            break;

        case 28: // '\034'
            setPk_system(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 29: // '\035'
            setAddclass(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 30: // '\036'
            setModifyclass(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 31: // '\037'
            setDeleteclass(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 32: // ' '
            setVoucherkind(objNewValue != null ? new Integer(objNewValue.toString().trim()) : new Integer(0));
            break;

        case 33: // '!'
            setTotaldebit(objNewValue != null ? new UFDouble(objNewValue.toString().trim()) : new UFDouble(0));
            break;

        case 34: // '"'
            setTotalcredit(objNewValue != null ? new UFDouble(objNewValue.toString().trim()) : new UFDouble(0));
            break;

        case 35: // '#'
            setExplanation(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 36: // '$'
            setContrastflag(objNewValue != null ? new Integer(objNewValue.toString().trim()) : new Integer(0));
            break;

        case 37: // '%'
            setErrmessage(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 12: // '\f'
            setM_adjustperiod(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 13: // '\r'
            setM_isVerify(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 38: // '&'
            setFree1(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 39: // '\''
            setFree2(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 40: // '('
            setFree3(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 41: // ')'
            setFree4(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 42: // '*'
            setFree5(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 43: // '+'
            setFree6(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 44: // ','
            setFree7(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 45: // '-'
            setFree8(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 46: // '.'
            setFree9(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 47: // '/'
            setFree10(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 54: // '6'
            setPk_setofbook(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 53: // '5'
            setPk_org(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 55: // '7'
            setPk_accountingbook(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 56: // '8'
            setPk_sourcepk(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 57: // '9'
            setConvertflag(objNewValue != null ? UFBoolean.valueOf(objNewValue.toString().trim()) : null);
            break;

        case 48: // '0'
            setFreevalue1(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 49: // '1'
            setFreevalue2(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 50: // '2'
            setFreevalue3(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 51: // '3'
            setFreevalue4(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 52: // '4'
            setFreevalue5(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 201: 
            setVouchertypename(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 202: 
            setCorpname(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 203: 
            setPreparedname(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 204: 
            setCheckedname(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 205: 
            setCashername(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 206: 
            setManagername(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 207: 
            setSystemname(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 1: // '\001'
            setDetail(objNewValue != null ? (Vector)objNewValue : null);
            break;

        case 209: 
            setIsmatched(objNewValue != null ? (Boolean)objNewValue : new Boolean(false));
            break;

        case 217: 
            setUserData(objNewValue != null ? objNewValue : null);
            break;

        case 601: 
            setPk_group(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 602: 
            setCreator(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 603: 
            setCreationtime(objNewValue != null ? (UFDateTime)objNewValue : null);
            break;

        case 604: 
            setModifier(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 605: 
            setModifiedtime(objNewValue != null ? (UFDateTime)objNewValue : null);
            break;

        case 405: 
            setTempsaveflag(objNewValue != null ? (UFBoolean)objNewValue : null);
            break;

        case 63: // '?'
            setPk_org(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 64: // '@'
            setPk_org_v(objNewValue != null ? objNewValue.toString().trim() : null);
            break;

        case 218: 
            throw new RuntimeException((new StringBuilder()).append("This Key can not be use in setValue() method::").append(iKey).toString());

        default:
            throw new RuntimeException((new StringBuilder()).append("No such VoucherKey::").append(iKey).toString());
        }
    }

    public void setVoucherkind(Integer newVoucherkind)
    {
        m_voucherkind = newVoucherkind;
    }

    public void setVouchertypename(String newVouchertypename)
    {
        m_vouchertypename = newVouchertypename;
    }

    public void setYear(String newYear)
    {
        m_year = newYear;
    }

    public void validate()
        throws ValidationException
    {
        ArrayList errFields = new ArrayList();
        if(m_pk_voucher == null)
            errFields.add(new String("m_pk_voucher"));
        if(m_discardflag == null)
            errFields.add(new String("m_discardflag"));
        if(m_pk_system == null)
            errFields.add(new String("m_pk_system"));
        if(m_voucherkind == null)
            errFields.add(new String("m_voucherkind"));
        StringBuffer message = new StringBuffer();
        message.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("2002GL502", "UPP2002GL502-000262"));
        if(errFields.size() > 0)
        {
            String temp[] = (String[])(String[])errFields.toArray(new String[0]);
            message.append(temp[0]);
            for(int i = 1; i < temp.length; i++)
            {
                message.append(NCLangRes4VoTransl.getNCLangRes().getStrByID("2002GL502", "UPP2002GL502-000263"));
                message.append(temp[i]);
            }

            throw new NullFieldException(message.toString());
        } else
        {
            return;
        }
    }

    public String getOffervoucher()
    {
        return offervoucher;
    }

    public void setOffervoucher(String offervoucher)
    {
        this.offervoucher = offervoucher;
    }

    public UFBoolean getIsdifflag()
    {
        return isdifflag;
    }

    public void setIsdifflag(UFBoolean isdifflag)
    {
        this.isdifflag = isdifflag;
    }

    public String getErrmessageh()
    {
        return errmessageh;
    }

    public void setErrmessageh(String errmessageh)
    {
        this.errmessageh = errmessageh;
    }

    public boolean isHasCashflowModified()
    {
        return hasCashflowModified;
    }

    public void setHasCashflowModified(boolean hasCashflowModified)
    {
        this.hasCashflowModified = hasCashflowModified;
    }

    public String getCreator()
    {
        return creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public UFDateTime getCreationtime()
    {
        return creationtime;
    }

    public void setCreationtime(UFDateTime creationtime)
    {
        this.creationtime = creationtime;
    }

    public String getModifier()
    {
        return modifier;
    }

    public void setModifier(String modifier)
    {
        this.modifier = modifier;
    }

    public UFDateTime getModifiedtime()
    {
        return modifiedtime;
    }

    public void setModifiedtime(UFDateTime modifiedtime)
    {
        this.modifiedtime = modifiedtime;
    }

    public String getPk_group()
    {
        return pk_group;
    }

    public void setPk_group(String pk_group)
    {
        this.pk_group = pk_group;
    }

    public String getM_isVerify()
    {
        return m_isVerify;
    }

    public void setM_isVerify(String verify)
    {
        m_isVerify = verify;
    }

    public boolean isNoFactorChanged()
    {
        return isNoFactorChanged;
    }

    public void setNoFactorChanged(boolean isNoFactorChanged)
    {
        this.isNoFactorChanged = isNoFactorChanged;
    }

    public Object getNoFeature()
    {
        return noFeature;
    }

    public void setNoFeature(Object noFeature)
    {
        this.noFeature = noFeature;
    }

    public String getPk_org_v()
    {
        return pk_org_v;
    }

    public void setPk_org_v(String pkOrgV)
    {
        pk_org_v = pkOrgV;
    }

    public void afterOperationLog(String s)
        throws BusinessException
    {
    }

    public void beforeOperationLog(String s)
        throws BusinessException
    {
    }

    public String getBillmaker()
    {
        return billmaker;
    }

    public void setBillmaker(String billmaker)
    {
        this.billmaker = billmaker;
    }

    public String getApprover()
    {
        return approver;
    }

    public void setApprover(String approver)
    {
        this.approver = approver;
    }

    public UFBoolean getTempsaveflag()
    {
        return m_tempsaveflag;
    }

    public void setTempsaveflag(UFBoolean newTempsaveflag)
    {
        m_tempsaveflag = newTempsaveflag;
        DetailVO arr$[] = getDetails();
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            DetailVO detail = arr$[i$];
            detail.setTempsaveflag(newTempsaveflag);
        }

    }

    public UFDouble getTotaldebitgroup()
    {
        return m_totaldebitgroup != null ? m_totaldebitgroup : UFDouble.ZERO_DBL;
    }

    public void setTotaldebitgroup(UFDouble newTotaldebitgroup)
    {
        m_totaldebitgroup = newTotaldebitgroup;
    }

    public UFDouble getTotalcreditgroup()
    {
        return m_totalcreditgroup != null ? m_totalcreditgroup : UFDouble.ZERO_DBL;
    }

    public void setTotalcreditgroup(UFDouble newTotalcreditgroup)
    {
        m_totalcreditgroup = newTotalcreditgroup;
    }

    public UFDouble getTotaldebitglobal()
    {
        return m_totaldebitglobal != null ? m_totaldebitglobal : UFDouble.ZERO_DBL;
    }

    public void setTotaldebitglobal(UFDouble newTotaldebitglobal)
    {
        m_totaldebitglobal = newTotaldebitglobal;
    }

    public UFDouble getTotalcreditglobal()
    {
        return m_totalcreditglobal != null ? m_totalcreditglobal : UFDouble.ZERO_DBL;
    }

    public void setTotalcreditglobal(UFDouble newTotalcreditglobal)
    {
        m_totalcreditglobal = newTotalcreditglobal;
    }

    public Object getFipInfo()
    {
        return fipInfo;
    }

    public FipRelationInfoVO getFipRelationInfoVO()
    {
        FipRelationInfoVO returnFipRelationInfoVO = new FipRelationInfoVO();
        returnFipRelationInfoVO.setPk_group(getPk_group());
        returnFipRelationInfoVO.setPk_org(getPk_accountingbook());
        returnFipRelationInfoVO.setPk_system("GL");
        returnFipRelationInfoVO.setPk_billtype("C0");
        returnFipRelationInfoVO.setRelationID(getPk_voucher());
        returnFipRelationInfoVO.setPk_operator(getPk_prepared());
        returnFipRelationInfoVO.setFreedef1(getVoucherNoStr(getNo()));
        returnFipRelationInfoVO.setFreedef2(getExplanation());
        returnFipRelationInfoVO.setFreedef3((new StringBuilder()).append("").append(UFDouble.ZERO_DBL.equals(getTotaldebit()) ? ((Object) (getTotalcredit())) : ((Object) (getTotaldebit()))).toString());
        returnFipRelationInfoVO.setBusidate(getPrepareddate());
        return returnFipRelationInfoVO;
    }

    private String getVoucherNoStr(Integer voucherNo)
    {
        StringBuffer rtStr = new StringBuffer();
        if(voucherNo == null)
            voucherNo = Integer.valueOf(0);
        String string = voucherNo.toString();
        for(int i = string.length(); i < 8; i++)
            rtStr.append("0");

        rtStr.append(string);
        return rtStr.toString();
    }

    public void setFipInfo(Object info)
    {
        fipInfo = info;
    }

    public void setControlFlag(String editflag)
    {
        if(StringUtils.isEmpty(editflag))
            editflag = "YYYYYYYYYYYYYYYYYYYYYYYYYYYY".substring(1);
        this.editflag = editflag;
        if(editflag != null && editflag.length() > 4)
        {
            setModifyflag(editflag.substring(VoucherModflagTool.getControlRuleStrIndex(17) - 1, VoucherModflagTool.getControlRuleStrIndex(19)));
            setDetailmodflag(UFBoolean.valueOf(String.valueOf(editflag.charAt(VoucherModflagTool.getControlRuleStrIndex(26) - 1))));
            Vector detail2 = getDetail();
            if(detail2 != null && detail2.size() > 0)
            {
                for(Iterator i$ = detail2.iterator(); i$.hasNext();)
                {
                    Object object = i$.next();
                    DetailVO detailVo = (DetailVO)object;
                    if(detailVo.getModifyflag() == null)
                        detailVo.setModifyflag(editflag.substring(4));
                    else
                        detailVo.setModifyflag(ModifyFlagConvertTool.detailFlayConvertTo61(detailVo.getModifyflag()));
                }

            }
        }
    }

    public String getEditflag()
    {
        return editflag;
    }

    public void setPreaccountflag(UFBoolean preaccountflag)
    {
        this.preaccountflag = preaccountflag;
    }

    public UFBoolean getPreaccountflag()
    {
        return preaccountflag;
    }

    public void setTs(UFDateTime m_ts)
    {
        this.m_ts = m_ts;
    }

    public UFDateTime getTs()
    {
        return m_ts;
    }
    
    private UFBoolean isOffer;
    
    public UFBoolean getIsOffer() {
		return isOffer;
	}

	public void setIsOffer(UFBoolean isOffer) {
		this.isOffer = isOffer;
	}

    public static final String PK_ACCOUNTINGBOOK = "gl_voucher.pk_accountingbook";
    public static final String PK_VOUCHERTYPE = "gl_voucher.pk_vouchertype";
    public static final String PREPAREDDATE = "gl_voucher.prepareddate";
    public static final String PK_PREPARED = "gl_voucher.pk_prepared";
    public static final String PK_CASHER = "gl_voucher.pk_casher";
    public static final String PK_CHEEKED = "gl_voucher.pk_checked";
    public static final String PK_MANAGER = "gl_voucher.pk_manager";
    public static final String EXPLANATION = "gl_voucher.explanation";
    public static final String YEAR = "gl_voucher.year";
    public static final String PERIOD = "gl_voucher.period";
    public static final String PK_VOUCHER = "pk_voucher";
    public static final String NUM = "gl_voucher.num";
    private UFBoolean ismodelrecflag;
    static final long serialVersionUID = -734532366925764037L;
    public String m_pk_voucher;
    public String m_pk_vouchertype;
    public String m_year;
    public String m_period;
    public Integer m_num;
    public UFDate m_prepareddate;
    public UFDate m_tallydate;
    public Integer m_attachment;
    public String m_pk_prepared;
    public String m_pk_checked;
    public String m_pk_casher;
    public String m_pk_manager;
    public UFBoolean m_signflag;
    public String m_modifyflag;
    public UFBoolean m_detailmodflag;
    public UFBoolean m_discardflag;
    public String m_pk_system;
    public String m_addclass;
    public String m_modifyclass;
    public String m_deleteclass;
    public Integer m_voucherkind;
    public UFDouble m_totaldebit;
    public UFDouble m_totalcredit;
    public UFDouble m_totaldebitgroup;
    public UFDouble m_totalcreditgroup;
    public UFDouble m_totaldebitglobal;
    public UFDouble m_totalcreditglobal;
    public String m_explanation;
    public UFDateTime m_ts;
    public Integer m_contrastflag;
    public String m_errmessage;
    public UFBoolean m_tempsaveflag;
    public String m_isVerify;
    public String m_adjustperiod;
    public String m_free1;
    public String m_free2;
    public String m_free3;
    public String m_free4;
    public String m_free5;
    public String m_free6;
    public String m_free7;
    public String m_free8;
    public String m_free9;
    public String m_free10;
    public UFDate m_signdate;
    public UFDate m_checkeddate;
    public String m_pk_setofbook;
    public String m_pk_org;
    public String m_pk_accountingbook;
    public String m_pk_sourcepk;
    public UFBoolean m_convertflag;
    public String m_freevalue1;
    public String m_freevalue2;
    public String m_freevalue3;
    public String m_freevalue4;
    public String m_freevalue5;
    public String m_vouchertypename;
    public String m_corpname;
    public String m_preparedname;
    public String m_checkedname;
    public String m_cashername;
    public String m_managername;
    public String m_systemname;
    public Vector m_detail;
    public Boolean m_ismatched;
    public Object m_userdata;
    public UFBoolean m_isoutsubj;
    public String offervoucher;
    public UFBoolean isdifflag;
    public String errmessageh;
    private boolean hasCashflowModified;
    private boolean isNoFactorChanged;
    public Object noFeature;
    private String creator;
    private UFDateTime creationtime;
    private String modifier;
    private UFDateTime modifiedtime;
    private String pk_group;
    private String pk_org_v;
    private UFBoolean preaccountflag;
    private String billmaker;
    private String approver;
    private DetailVO details[];
    private String editflag;
    private Object fipInfo;
    private static final int maxLength = 8;
}

