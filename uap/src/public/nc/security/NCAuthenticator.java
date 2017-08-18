package nc.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import nc.bs.logging.Logger;
import nc.security.impl.SoftSigner;
import nc.security.itf.ISigner;
import nc.security.itf.IVerify;
import nc.security.vo.BindModel;
import nc.security.vo.CAContext;
import nc.security.vo.CARegEntry;
import nc.secutity.toolkit.AuthenticatorResultMessage;
import nc.vo.framework.rsa.Encode;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class NCAuthenticator {
	private static final String defaultCharSet = "GBK";
	public static final String AUTHEN_TYPE_BUSINESS = "BUSINESS";
	public static final String AUTHEN_TYPE_SERVER = "SERVER";
	public static final String AUTHEN_TYPE_LOGIN = "LOGIN";
	private static Encode coder = new Encode();
	private CARegEntry caRegEntry = null;
	private CAOwner caOwner = null;
	private IUsbkeyPwdProvider usbKeyPwdProvider = null;
	private ISigner softSigner = null;
	private ISigner caSigner = null;
	private String authenType = "BUSINESS";
	private boolean needCheckPWD = true;

	private String usbPwd = null;

	NCAuthenticator(CAOwner caOwner, CARegEntry caRegEntry) {
		this.caOwner = caOwner;
		this.caRegEntry = caRegEntry;
		initialize();
	}

	NCAuthenticator(CAOwner caOwner, CARegEntry caRegEntry, String authenType) {
		this.caOwner = caOwner;
		this.caRegEntry = caRegEntry;
		this.authenType = authenType;
		initialize();
	}

	private void initialize() {
	}

	private char[] getPwd() {
		if (this.usbPwd != null) {
			String pwdStr = coder.decode(this.usbPwd);
			return pwdStr.toCharArray();
		}
		return null;
	}

	private void setPwd(char[] pwd) {
		if (pwd == null) {
			this.usbPwd = null;
		} else
			this.usbPwd = coder.encode(new String(pwd));
	}

	public CAOwner getCAOwner() {
		return this.caOwner;
	}

	void setCAOwner(CAOwner owner) {
		this.caOwner = owner;
	}

	IUsbkeyPwdProvider getUsbKeyPwdProvider() {
		return this.usbKeyPwdProvider;
	}

	void setUsbKeyPwdProvider(IUsbkeyPwdProvider usbKeyPwdProvider) {
		this.usbKeyPwdProvider = usbKeyPwdProvider;
	}

	public boolean isCAUser() {
		return this.caOwner.isCA();
	}

	public ISigner getSigner(boolean adapted) {
		if ((adapted) && (!isCAUser())) {
			return getSoftSinger();
		}
		return getCASigner();
	}

	private CARegEntry getRealCARegEntry(boolean adapted) {
		if ((adapted) && (!isCAUser())) {
			try {
				return NCAuthenticatorToolkit.getNCSoftRegEntry();
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return this.caRegEntry;
	}

	private ISigner getSoftSinger() {
		if (this.softSigner == null) {
			try {
				CARegEntry ncSoftRegEntry = NCAuthenticatorToolkit.getNCSoftRegEntry();
				CAContext context = new CAContext(this.caOwner.getPK(), this.caOwner.getCode(), ncSoftRegEntry);
				this.softSigner = SignerFactory.createSigner(context);
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return this.softSigner;
	}

	private ISigner getCASigner() {
		if (this.caSigner == null) {
			CAContext context = new CAContext(this.caOwner.getPK(), this.caOwner.getCode(), this.caRegEntry);
			this.caSigner = SignerFactory.createSigner(context);
		}
		return this.caSigner;
	}

	public String sign(String plainText) throws Exception {
		return sign(plainText, true);
	}

	public String[] batchSign(String[] plainTexts) throws Exception {
		return batchSign(plainTexts, true);
	}

	public String sign(String plainText, boolean adapted) throws Exception {
		String signStr = null;
		try {
			ISigner signer = getSigner(adapted);
			// 20170713 tsy È¥³ýCAÐ£Ñé
			// checkUsbKey(signer);
			// checkPWD(signer);
			// 20170713 end
			CARegEntry realCARegEntry = getRealCARegEntry(adapted);
			signStr = signImpl(signer, plainText, realCARegEntry);
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw e;
		}
		return signStr;
	}

	public String[] batchSign(String[] plainTexts, boolean adapted) throws Exception {
		if (plainTexts == null) {
			return null;
		}
		int count = plainTexts.length;
		String[] signStrs = new String[count];
		try {
			ISigner signer = getSigner(adapted);
			checkUsbKey(signer);
			checkPWD(signer);
			CARegEntry realCARegEntry = getRealCARegEntry(adapted);
			for (int i = 0; i < count; i++) {
				signStrs[i] = signImpl(signer, plainTexts[i], realCARegEntry);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw e;
		}
		return signStrs;
	}

	private String signImpl(ISigner signer, String plainText, CARegEntry regEntry) throws Exception {
		String signStr = null;
		DataOutputStream dos = null;
		try {
			Logger.debug("===sign: userid=" + this.caOwner.getPK());
			Logger.debug("===sign: userCode=" + this.caOwner.getCode());
			Logger.debug("===sign: caRegEntryId=" + regEntry.getId());
			Logger.debug("===sign: plainText=" + plainText);
			Logger.debug("===sign: singer=" + signer.getClass().getName());
			String sn = signer.getCertSN();
			if (sn == null) {
				throw new Exception(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0006"));
			}
			Logger.debug("===sign: cert sn=" + sn);
			Logger.debug("===sign: cert sn length = " + (sn == null ? 0 : sn.length()));
			byte[] dataBytes = plainText.getBytes("GBK");
			byte[] signBytes = signer.sign(dataBytes);
			if (signBytes == null) {
				throw new Exception(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0000"));
			}

			signStr = dealWithSignResult(signBytes, sn, regEntry.getId());
			Logger.debug("===sign: sign str=" + signStr);
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw e;
		} finally {
			if (dos != null) {
				dos.close();
			}
		}
		return signStr;
	}

	public static String dealWithSignResult(byte[] signBytes, String sn, String caRegEntryId) throws Exception {
		String signStr = null;
		DataOutputStream dos = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			dos = new DataOutputStream(baos);
			dos.writeUTF(caRegEntryId);
			dos.writeUTF(sn);
			dos.writeInt(signBytes.length);
			dos.write(signBytes);
			byte[] retrBytes = baos.toByteArray();
			signStr = new BASE64Encoder().encode(retrBytes);
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw e;
		} finally {
			if (dos != null) {
				dos.close();
			}
		}
		return signStr;
	}

	private void checkPWD(ISigner signer) throws SecurityException {
		if ((signer instanceof SoftSigner)) {
			return;
		}
		boolean hasLoginOn = signer.hasLoginOn();
		boolean isNeedCheckPWD = isNeedCheckPWD();
		char[] usbkeyPWD = getPwd();
		Logger.debug("===check pwd: hasLoginOn=" + hasLoginOn + "  isNeedCheckPWD=" + isNeedCheckPWD + "  keypwd is " + (usbkeyPWD == null ? " null " : " not null"));
		if ((usbkeyPWD != null) && (!isNeedCheckPWD) && (hasLoginOn)) {
			return;
		}
		if ((!"SERVER".equalsIgnoreCase(this.authenType)) && (getUsbKeyPwdProvider() != null)) {
			char[] pwd = getUsbKeyPwdProvider().getUsbkeyPwd();
			boolean validate = false;
			if ((hasLoginOn) && (usbkeyPWD != null)) {
				validate = Arrays.equals(usbkeyPWD, pwd);
			} else {
				validate = signer.loginUsbkey(pwd);
			}
			if (validate) {
				setPwd(pwd);
			} else {
				throw new SecurityException(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0001"), 301);
			}
		}
		checkUserIdentifier(signer);
	}

	public boolean isNeedCheckPWD() {
		return this.needCheckPWD;
	}

	public void setNeedCheckPWD(boolean needCheckPWD) {
		this.needCheckPWD = needCheckPWD;
	}

	private void checkUserIdentifier(ISigner signer) throws SecurityException {
		if (!"SERVER".equalsIgnoreCase(this.authenType)) {

			String userIdentifier = signer.getNCUserIdentifier();
			if ((userIdentifier == null) || (userIdentifier.trim().length() == 0)) {
				throw new SecurityException(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0002"), 203);
			}
			BindModel model = this.caRegEntry.getBindMode();
			String currUserIdentifier = null;
			if (BindModel.USERID == model) {
				currUserIdentifier = this.caOwner.getPK();
			} else {
				currUserIdentifier = this.caOwner.getCode();
			}
			if (!userIdentifier.equals(currUserIdentifier)) {
				throw new SecurityException(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0003", null, new String[] { currUserIdentifier, userIdentifier }), 302);
			}
		}
	}

	private void checkUsbKey(ISigner signer) throws SecurityException {
		if (!signer.isUsbKeyExist()) {
			throw new SecurityException(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0004"), 202);
		}
	}

	public boolean verify(String plainText, String signStr) throws SecurityException, Exception {
		int verifyValue = verifyImple(plainText, signStr);
		if (verifyValue == 201) {
			return true;
		}
		String msg = AuthenticatorResultMessage.getMLResultMessage(verifyValue);
		throw new SecurityException(msg, verifyValue);
	}

	private int verifyImple(String plainText, String signStr) throws Exception {
		if (signStr == null)
			throw new Exception(NCLangRes4VoTransl.getNCLangRes().getStrByID("cares", "authen-0005"));
		DataInputStream dis = null;
		int verifyValue = -1;
		try {
			Logger.debug("===verify: userid=" + this.caOwner.getPK());
			Logger.debug("===verify: userCode=" + this.caOwner.getCode());
			Logger.debug("===verify: plainText=" + plainText);
			Logger.debug("===verify: signStr=" + signStr);
			byte[] signBytes = new BASE64Decoder().decodeBuffer(signStr);
			ByteArrayInputStream bais = new ByteArrayInputStream(signBytes);
			dis = new DataInputStream(bais);
			String entryID = dis.readUTF();
			Logger.debug("===verify: caRegEntryId=" + entryID);
			String certSN = dis.readUTF();
			Logger.debug("===verify: certSN=" + certSN);
			Logger.debug("===verify: cert certSN length = " + (certSN == null ? 0 : certSN.length()));
			int len = dis.readInt();
			byte[] sign = new byte[len];
			dis.read(sign);

			byte[] orignBytes = plainText.getBytes("GBK");
			CARegEntry regEntry = NCAuthenticatorToolkit.getCARegEntryById(entryID);
			if (regEntry == null) {
				throw new RuntimeException("invalidate entryID,can't find register entry:" + entryID);
			}
			CAContext context = new CAContext(this.caOwner.getPK(), this.caOwner.getCode(), regEntry);
			IVerify verify = VerifyFactory.createVerify(context);
			Logger.debug("===verify: verifier=" + verify.getClass().getName());

			verifyValue = verify.verify(certSN, orignBytes, sign);
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw e;
		}

		return verifyValue;
	}
}