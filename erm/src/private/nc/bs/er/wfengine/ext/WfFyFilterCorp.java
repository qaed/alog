package nc.bs.er.wfengine.ext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nc.bs.framework.common.NCLocator;
import nc.bs.pub.pf.ParticipantFilterContext;
import nc.pubitf.org.IOrgUnitPubService;
import nc.pubitf.rbac.IUserPubService;
import nc.vo.ep.bx.JKBXHeaderVO;
import nc.vo.ep.bx.JKBXVO;
import nc.vo.erm.costshare.CShareDetailVO;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;

import nc.vo.org.OrgVO;
import nc.vo.pub.BusinessException;
import nc.vo.sm.UserVO;
import nc.vo.uap.pf.PFBusinessException;

public class WfFyFilterCorp extends ErmBaseParticipantFilter {

	private ParticipantFilterContext pfc;

	public WfFyFilterCorp() {
		this.participantName = NCLangRes4VoTransl.getNCLangRes().getStrByID("common", "UPP2011-000907");

	}

	public HashSet<String> filterUsers(ParticipantFilterContext pfc) throws BusinessException {
		this.pfc = pfc;
		// 20170125 tsy 修复在此类中调用getParticipantTypeName()方法可能会发生空指针异常的问题
		if (super.pfc == null) {
			super.setPfc(pfc);
		}
		// 20170125 end
		HashSet<String> hsId = new HashSet();

		JKBXVO billvo = (JKBXVO) pfc.getBillEntity();

		String strbaoxiaoCorp = (String) billvo.getParentVO().getAttributeValue("pk_org");
		Set<String> orgs = new HashSet();
		if (!nc.util.erm.costshare.ErmForCShareUtil.isHasCShare(billvo)) {
			orgs.add((String) billvo.getParentVO().getAttributeValue("fydwbm"));
		} else {
			CShareDetailVO[] detailvos = billvo.getcShareDetailVo();
			for (CShareDetailVO detailvo : detailvos) {
				orgs.add(detailvo.getAssume_org());
			}
		}
		if ((strbaoxiaoCorp == null) || (orgs.isEmpty())) {
			return null;
		}

		List<String> alDistilledUserPKs = pfc.getUserIdsOfParticipant();
		if ((alDistilledUserPKs == null) || (alDistilledUserPKs.size() == 0)) {
			findUserOfOrgunitByCorp(pfc.getParticipantId(), pfc.getParticipantType(), hsId, (String[]) orgs.toArray(new String[0]));
		} else {
			hsId.addAll(alDistilledUserPKs);
		}
		return hsId;
	}

	private void findUserOfOrgunitByCorp(String participantId, String participantType, HashSet<String> hsId, String[] strCorps) throws BusinessException {
		HashSet<String> userList = getPfc().getUserList();

		List<String> fyusers = new java.util.ArrayList();
		for (String strCorp : strCorps) {
			fyusers.add(getOrgPrincipal(strCorp));
		}

		for (String fyUserId : fyusers) {
			if (userList.contains(fyUserId)) {
				hsId.add(fyUserId);
			} else {
				throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getString("common", null, "UPP2011-000909", null, new String[] { getParticipantName(), getParticipantTypeName() }));
			}
		}
	}

	private String getOrgPrincipal(String strCorp) throws BusinessException {
		IOrgUnitPubService og = (IOrgUnitPubService) NCLocator.getInstance().lookup(IOrgUnitPubService.class);
		OrgVO[] orgs = og.getOrgs(new String[] { strCorp }, null);
		String user = "";
		if ((orgs != null) && (orgs.length != 0)) {
			String psn = orgs[0].getPrincipal();
			if (psn != null) {
				IUserPubService ucQry = (IUserPubService) NCLocator.getInstance().lookup(IUserPubService.class);
				UserVO muser = ucQry.queryUserVOByPsnDocID(psn);
				if (muser != null) {
					user = muser.getPrimaryKey();
				} else {
					throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0083"));
				}
			} else {
				throw new PFBusinessException(NCLangRes4VoTransl.getNCLangRes().getStrByID("201107_0", "0201107-0083"));
			}
		}

		return user;
	}

	public String getCode() {
		return "[U]Stakeholder";
	}

	public ParticipantFilterContext getPfc() {
		return this.pfc;
	}

	public void setPfc(ParticipantFilterContext pfc) {
		this.pfc = pfc;
	}
}
