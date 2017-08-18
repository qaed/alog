package nc.vo.er.settle;

import java.util.ArrayList;
import java.util.List;
import nc.bs.framework.common.NCLocator;
import nc.cmp.utils.NetPayHelper;
import nc.itf.cmp.busi.ISettlementInfoFetcher;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.ArrayProcessor;
import nc.md.persist.framework.IMDPersistenceQueryService;
import nc.md.persist.framework.MDPersistenceService;
import nc.pubitf.uapbd.ISupplierPubService;
import nc.vo.arap.bx.util.BXConstans;
import nc.vo.bd.freecustom.FreeCustomVO;
import nc.vo.bd.psn.PsndocVO;
import nc.vo.bd.supplier.SupplierVO;
import nc.vo.cmp.netpay.NetPayHelperVO;
import nc.vo.cmp.settlement.SettleEnumCollection;
import nc.vo.cmp.settlement.SettleEnumCollection.Direction;
import nc.vo.cmp.settlement.SettlementBodyVO;
import nc.vo.ep.bx.BXBusItemVO;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;

public class ErSettlementInfo implements ISettlementInfoFetcher {
	public ErSettlementInfo() {
	}

	private JKBXVO bxvo = null;

	public SettlementBodyVO[] getSettleMetaInfo() throws BusinessException {
		BXBusItemVO[] childrenVO = this.bxvo.getChildrenVO();
		JKBXHeaderVO head = this.bxvo.getParentVO();

		boolean isBXBill = "bx".equals(this.bxvo.getParentVO().getDjdl());

		String jkbxr = this.bxvo.getParentVO().getJkbxr();
		if ((childrenVO == null) || (childrenVO.length == 0)) {
			BXBusItemVO bxBusItemVO = new BXBusItemVO();
			String[] money_fields = BXBusItemVO.MONEY_FIELDS;
			if (head.getPk_jkbx() != null) {
				bxBusItemVO.setPrimaryKey(head.getPk_jkbx());
				bxBusItemVO.setPk_jkbx(head.getPk_jkbx());
			} else {
				bxBusItemVO.setPrimaryKey("ER_SET_HTEMPK_");
				bxBusItemVO.setPk_jkbx("ER_SET_HTEMPK_");
			}
			for (String money : money_fields) {
				bxBusItemVO.setAttributeValue(money, head.getAttributeValue(money));
			}
			bxBusItemVO.setJkbxr(head.getJkbxr());
			bxBusItemVO.setSzxmid(head.getSzxmid());
			bxBusItemVO.setJobid(head.getJobid());
			bxBusItemVO.setCashproj(head.getCashproj());
			childrenVO = new BXBusItemVO[] { bxBusItemVO };
		}
		List<SettlementBodyVO> metavoList = new ArrayList();
		// 对方类型（个人、供应商、散户）
		int traderType = 0;
		// 对方名称（个人名称、供应商名称、散户名称）
		String traderName = null;
		// 对方的pk（个人pk、 供应商pk、散户pk）
		String pk_trader = null;
		// 账户银行名称
		String bankDocName = null;
		// 账户银行pk（设置pk后，名称则不需要设置）
		String pk_oopBank = null;
		// 收款银行账户(pk)
		String pk_oppAccount = head.getSkyhzh();
		// 收款银行账户（String，账号， 有pk，就不用设置该属性）
		String oppAccount = null;

		String oppAccountName = null;

		Integer accountType = null;
		// 散户
		FreeCustomVO freeCustVO = null;

		if ((isBXBill) && (head.getHbbm() != null)) {
			if (head.getFreecust() != null) {
				freeCustVO =
						(FreeCustomVO) MDPersistenceService.lookupPersistenceQueryService().queryBillOfVOByPK(FreeCustomVO.class, head.getFreecust(), false);

				if (freeCustVO != null) {
					traderType = 4;
					pk_trader = freeCustVO.getPk_freecustom();
					traderName = freeCustVO.getName();
					oppAccountName = freeCustVO.getName();
					oppAccount = freeCustVO.getBankaccount();
					pk_oopBank = freeCustVO.getBank();
					accountType = freeCustVO.getAccountproperty();
				}
			} else {
				traderType = 1;
				pk_trader = head.getHbbm();
				pk_oppAccount = head.getCustaccount();

				SupplierVO[] hbbmname =
						((ISupplierPubService) NCLocator.getInstance().lookup(ISupplierPubService.class)).getSupplierVO(new String[] { head.getHbbm() }, new String[] { "name" });

				traderName = hbbmname[0].getName();
			}
		} else {
			traderType = 3;

			pk_trader = head.getReceiver();

			if (pk_trader == null) {
				pk_trader = head.getJkbxr();
			}

			if (pk_trader != null) {
				traderName =
						((nc.itf.bd.psn.psndoc.IPsndocQueryService) NCLocator.getInstance().lookup(nc.itf.bd.psn.psndoc.IPsndocQueryService.class)).queryPsndocVOsByCondition("pk_psndoc='" + pk_trader + "'")[0].getName();
			}

			pk_oppAccount = head.getSkyhzh();
		}

		if (pk_oopBank != null) {
			Object[] result =
					(Object[]) ((IUAPQueryBS) NCLocator.getInstance().lookup(IUAPQueryBS.class)).executeQuery("select name from bd_bankdoc where pk_bankdoc='" + pk_oopBank + "'", new ArrayProcessor());

			if ((result != null) && (result.length != 0) && (result[0] != null)) {
				bankDocName = (String) result[0];
			}
		}

		if (pk_oppAccount != null) {
			NetPayHelperVO opphelperVO = NetPayHelper.instance.getNetPayVO(pk_oppAccount);
			if (opphelperVO != null) {
				oppAccount = opphelperVO.getAccount();
				oppAccountName = opphelperVO.getAccname();
				if (bankDocName == null) {
					bankDocName = opphelperVO.getBankdocname();
				}
			}
		}

		int i = 0;
		for (int size = childrenVO.length; i < size; i++) {
			BXBusItemVO item = childrenVO[i];

			UFDouble ufZero = new UFDouble(0);
			if (((item.getZfybje() != null) && (!item.getZfybje().equals(ufZero))) || ((item.getHkybje() != null) && (!item.getHkybje().equals(ufZero)))) {

				SettlementBodyVO metavo = new SettlementBodyVO();
				metavo.setBillcode(head.getDjbh());
				metavo.setBilldate(head.getDjrq());
				metavo.setLocalrate(head.getBbhl());
				metavo.setMemo(head.getZy());
				// 收付方向(收0,付1)
				Integer direction =
						(item.getZfybje() != null) && (!item.getZfybje().equals(ufZero)) ? SettleEnumCollection.Direction.PAY.VALUE : SettleEnumCollection.Direction.REC.VALUE;
				metavo.setDirection(direction);
				if (item.getAmount().getDouble() < 0.0D) {
					metavo.setDirection(Integer.valueOf(1));
					metavo.setPay(UFDouble.ZERO_DBL.sub(item.getHkybje()));
					metavo.setPaylocal(UFDouble.ZERO_DBL.sub(item.getHkbbje()));
					UFDouble grouphkbbje = head.getGrouphkbbje() == null ? ufZero : item.getGrouphkbbje();
					UFDouble globalhkbbje = head.getGlobalhkbbje() == null ? ufZero : item.getGlobalhkbbje();
					metavo.setGrouppaylocal(UFDouble.ZERO_DBL.sub(grouphkbbje));
					metavo.setGlobalpaylocal(UFDouble.ZERO_DBL.sub(globalhkbbje));
				} else {
					metavo.setPay(item.getZfybje());
					metavo.setPaylocal(item.getZfbbje());
					metavo.setGrouppaylocal(head.getGroupzfbbje() == null ? ufZero : item.getGroupzfbbje());
					metavo.setGlobalpaylocal(head.getGlobalzfbbje() == null ? ufZero : item.getGlobalzfbbje());
					metavo.setReceive(item.getHkybje());
					metavo.setReceivelocal(item.getHkbbje());
					metavo.setGroupreceivelocal(head.getGrouphkbbje() == null ? ufZero : item.getGrouphkbbje());
					metavo.setGlobalreceivelocal(head.getGlobalhkbbje() == null ? ufZero : item.getGlobalhkbbje());
				}
				//20170712 tsy 表体自定义22作为结算金额
				String defitem25 = (String) item.getDefitem25();
				if(null != defitem25){
					UFDouble zfybje = new UFDouble(defitem25);
					metavo.setPay(zfybje);
					metavo.setPaylocal(zfybje);
				}
				
				//20170712 end 
				metavo.setSystemcode(BXConstans.ERM_MD_NAMESPACE);

				metavo.setPk_billdetail(item.getPrimaryKey());
				metavo.setPk_billtype(head.getDjlxbm());
				metavo.setPk_bill(head.getPk_jkbx());
				metavo.setPk_org(head.getPk_payorg());
				metavo.setPk_org_v(head.getPk_payorg_v());
				metavo.setPk_group(head.getPk_group());
				metavo.setGrouprate(head.getGroupbbhl() == null ? ufZero : head.getGroupbbhl());
				metavo.setGlobalrate(head.getGlobalbbhl() == null ? ufZero : head.getGlobalbbhl());
				metavo.setPk_rescenter(head.getFydeptid());
				metavo.setNotenumber(head.getPjh());
				metavo.setPk_notetype(head.getChecktype());
				metavo.setPk_currtype(head.getBzbm());

				metavo.setTradertype(Integer.valueOf(traderType));
				metavo.setPk_trader(pk_trader);
				metavo.setTradername(traderName);
				metavo.setPk_oppaccount(pk_oppAccount);
				metavo.setOppaccount(oppAccount);
				metavo.setOppaccname(oppAccountName);
				metavo.setPk_oppbank(pk_oopBank);
				metavo.setOppbank(bankDocName);
				metavo.setAccounttype(accountType);

				metavo.setTranstype(Integer.valueOf(2));
				metavo.setPk_costsubj(item.getSzxmid());
				metavo.setPk_plansubj(head.getCashproj());
				metavo.setPk_balatype(head.getJsfs());

				metavo.setPk_psndoc(jkbxr);
				metavo.setPk_account(head.getFkyhzh());

				metavo.setPk_cashaccount(head.getPk_cashaccount());

				metavo.setPk_job(item.getJobid());
				metavo.setPk_cashflow(head.getCashitem());
				metavo.setPk_deptdoc(head.getDeptid());
				metavo.setPk_busitype(head.getBusitype());
				metavo.setPk_cubasdoc(head.getHbbm());
				metavo.setPk_jobphase(item.getProjecttask());
				metavo.setPk_invbasdoc(null);
				metavo.setPk_invcl(null);
				metavo.setFundsflag(metavo.getDirection());
				metavoList.add(metavo);
			}
		}
		return (SettlementBodyVO[]) metavoList.toArray(new SettlementBodyVO[0]);
	}

	public void setBillInfo(AggregatedValueObject billinfo) {
		this.bxvo = ((JKBXVO) billinfo);
	}

	public SettlementBodyVO[] getSettleMetaInfo(Object arg0) throws BusinessException {
		this.bxvo = ((JKBXVO) arg0);
		return getSettleMetaInfo();
	}
}