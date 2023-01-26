package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;

public class NanobenchSession extends WebSession {

	private static final long serialVersionUID = -7920814788717089213L;

	public static NanobenchSession get() {
		return (NanobenchSession) Session.get();
	}

	public NanobenchSession(Request request) {
		super(request);
		bind();
		loadProfileInfo();
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private IntroExtractor introExtractor;

	private String userDir = System.getProperty("user.home") + "/.nanopub/";

	private KeyPair keyPair;
	private IRI userIri;
	private Map<IRI,IntroNanopub> introNps;
	private Boolean isOrcidLinked;
	private String orcidLinkError;

	private boolean showProvenance = true;
	private boolean showPubinfo = false;

	private Integer localIntroCount = null;
	private IntroNanopub localIntro = null;

	public void loadProfileInfo() {
		localIntroCount = null;
		localIntro = null;
		NanobenchPreferences prefs = NanobenchPreferences.get();
		if (prefs.isOrcidLoginMode()) {
			File usersDir = new File(System.getProperty("user.home") + "/.nanopub/nanobench-users/");
			if (!usersDir.exists()) usersDir.mkdir();
		}
		if (userIri == null && !prefs.isReadOnlyMode() && !prefs.isOrcidLoginMode()) {
			if (getOrcidFile().exists()) {
				try {
					String orcid = FileUtils.readFileToString(getOrcidFile(), StandardCharsets.UTF_8).trim();
					//String orcid = Files.readString(orcidFile.toPath(), StandardCharsets.UTF_8).trim();
					if (orcid.matches(ProfilePage.ORCID_PATTERN)) {
						userIri = vf.createIRI("https://orcid.org/" + orcid);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		if (userIri != null && keyPair == null) {
			File keyFile = getKeyFile();
			if (keyFile.exists()) {
				try {
					keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
				} catch (Exception ex) {
					System.err.println("Couldn't load key pair");
				}
			} else {
				// Automatically generate new keys
				makeKeys();
			}
		}
		if (userIri != null && keyPair != null && introNps == null) {
			introNps = User.getIntroNanopubs(getPubkeyString());
		}
		checkOrcidLink();
	}

	public boolean isProfileComplete() {
		return userIri != null && keyPair != null && introNps != null;
	}

	public void redirectToLoginIfNeeded(String path, PageParameters parameters) {
		String loginUrl = getLoginUrl(path, parameters);
		if (loginUrl == null) return;
		throw new RedirectToUrlException(loginUrl);
	}

	public String getLoginUrl(String path, PageParameters parameters) {
		if (isProfileComplete()) return null;
		if (NanobenchPreferences.get().isOrcidLoginMode()) {
			return OrcidLoginPage.getOrcidLoginUrl("." + path, parameters);
		} else {
			return "." + ProfilePage.MOUNT_PATH;
		}
	}

	public String getPubkeyString() {
		if (keyPair == null) return null;
		return DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
	}

	public boolean isPubkeyApproved() {
		if (keyPair == null || userIri == null) return false;
		return User.isApprovedKeyForUser(getPubkeyString(), userIri);
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public void makeKeys() {
		try {
			MakeKeys.make(getKeyFile().getAbsolutePath().replaceFirst("_rsa$", ""), SignatureAlgorithm.RSA);
			keyPair = SignNanopub.loadKey(getKeyFile().getPath(), SignatureAlgorithm.RSA);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public IRI getUserIri() {
		return userIri;
	}

	public List<IntroNanopub> getUserIntroNanopubs() {
		return User.getIntroNanopubs(userIri);
	}

	public int getLocalIntroCount() {
		if (localIntroCount == null) {
			localIntroCount = 0;
			for (IntroNanopub inp : getUserIntroNanopubs()) {
				if (isIntroWithLocalKey(inp)) {
					localIntroCount++;
					localIntro = inp;
				}
			}
			if (localIntroCount > 1) localIntro = null;
		}
		return localIntroCount;
	}

	public IntroNanopub getLocalIntro() {
		getLocalIntroCount();
		return localIntro;
	}

	public boolean isIntroWithLocalKey(IntroNanopub inp) {
		IRI location = Utils.getLocation(inp);
		NanopubSignatureElement el = Utils.getNanopubSignatureElement(inp);
		String siteUrl = NanobenchPreferences.get().getWebsiteUrl();
		if (location != null && siteUrl != null && !location.stringValue().equals(siteUrl)) return false;
		if (!getPubkeyString().equals(el.getPublicKeyString())) return false;
		for (KeyDeclaration kd : inp.getKeyDeclarations()) {
			if (getPubkeyString().equals(kd.getPublicKeyString())) return true;
		}
		return false;
	}

	public void setOrcid(String orcid) {
		if (!orcid.matches(ProfilePage.ORCID_PATTERN)) {
			throw new RuntimeException("Illegal ORCID identifier: " + orcid);
		}
		if (NanobenchPreferences.get().isOrcidLoginMode()) {
			userDir = System.getProperty("user.home") + "/.nanopub/nanobench-users/" + orcid + "/";
			File f = new File(userDir);
			if (!f.exists()) f.mkdir();
		} else {
			try {
				FileUtils.writeStringToFile(getOrcidFile(), orcid + "\n", StandardCharsets.UTF_8);
	//			Files.writeString(orcidFile.toPath(), orcid + "\n");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		userIri = vf.createIRI("https://orcid.org/" + orcid);
		loadProfileInfo();
	}

	public Map<IRI,IntroNanopub> getIntroNanopubs() {
		return introNps;
	}

	public void checkOrcidLink() {
		if (isOrcidLinked == null && userIri != null) {
			orcidLinkError = "";
			introExtractor = null;
			try {
				introExtractor = IntroNanopub.extract(userIri.stringValue(), null);
				if (introExtractor.getIntroNanopub() == null) {
					orcidLinkError = "ORCID account is not linked.";
					isOrcidLinked = false;
				} else {
					IntroNanopub inp = IntroNanopub.get(userIri.stringValue(), introExtractor);
					if (introNps != null && introNps.containsKey(inp.getNanopub().getUri())) {
						// TODO: also check whether introduction contains local key
						isOrcidLinked = true;
					} else {
						isOrcidLinked = false;
						orcidLinkError = "Error: ORCID is linked to another introduction nanopublication.";
					}
				}
			} catch (Exception ex) {
				System.err.println("ORCID check failed");
				orcidLinkError = "ORCID check failed.";
			}
		}
	}

	public void resetOrcidLinked() {
		isOrcidLinked = null;
	}

	public boolean isOrcidLinked() {
		checkOrcidLink();
		return isOrcidLinked != null && isOrcidLinked == true;
	}

	public String getOrcidLinkError() {
		return orcidLinkError;
	}

	public String getOrcidName() {
		if (introExtractor == null || introExtractor.getName() == null) return null;
		if (introExtractor.getName().trim().isEmpty()) return null;
		return introExtractor.getName();
	}

	public boolean isShowProvenanceEnabled() {
		return showProvenance;
	}

	public void setShowProvenanceEnabled(boolean showProvenance) {
		this.showProvenance = showProvenance;
	}

	public boolean isShowPubinfoEnabled() {
		return showPubinfo;
	}

	public void setShowPubinfoEnabled(boolean showPubinfo) {
		this.showPubinfo = showPubinfo;
	}

	private File getOrcidFile() {
		return new File(userDir + "orcid");
	}

	public File getKeyFile() {
		return new File(userDir + "id_rsa");
	}

}
