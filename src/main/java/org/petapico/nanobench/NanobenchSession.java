package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MakeKeys;
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
	private static IntroExtractor introExtractor;

	private String userDir = System.getProperty("user.home") + "/.nanopub/";

	private KeyPair keyPair;
	private User user;
	private IRI userIri;
	private IntroNanopub introNp;
	private Boolean isOrcidLinked;
	private String orcidLinkError;

	private boolean showProvenance = true;
	private boolean showPubinfo = false;

	public void loadProfileInfo() {
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
			} else if (prefs.isOrcidLoginMode()) {
				// Automatically generate new keys in ORCID login mode:
				makeKeys();
			}
		}
		if (userIri != null && introNp == null) {
			if (getUser() != null) {
				Nanopub np = Utils.getNanopub(user.getIntropubIri().stringValue());
				introNp = new IntroNanopub(np, user.getId());
			}
		}
		checkOrcidLink();
	}

	public boolean isProfileComplete() {
		return userIri != null && keyPair != null && introNp != null; // && doPubkeysMatch();
	}

	public User getUser() {
		if (user == null) {
			if (getUserIri() == null) return null;
			user = User.getUser(getUserIri().toString());
		}
		return user;
	}

	public boolean doPubkeysMatch() {
		if (keyPair == null) return false;
		if (introNp == null) return false;
		// TODO: Handle case of multiple key declarations
		return getPubkeyString().equals(introNp.getKeyDeclarations().get(0).getPublicKeyString());
	}

	public String getPubkeyString() {
		if (keyPair == null) return null;
		return DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
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

	public IntroNanopub getIntroNanopub() {
		return introNp;
	}

	public void setIntroNanopub(Nanopub np) {
		if (np == null) {
			introNp = null;
		} else {
			introNp = new IntroNanopub(np, userIri);
		}
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
					if (introNp != null && inp.getNanopub().getUri().equals(introNp.getNanopub().getUri())) {
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

	public Boolean isOrcidLinked() {
		return isOrcidLinked;
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

	public String getLocalPublicKeyString() {
		return DatatypeConverter.printBase64Binary(getKeyPair().getPublic().getEncoded()).replaceAll("\\s", "");
	}

	public List<KeyDeclaration> getOrcidKeyDeclarations() {
		List<KeyDeclaration> orcidPubkeys = new ArrayList<>();
		for (KeyDeclaration kd : getIntroNanopub().getKeyDeclarations()) {
			if (!kd.getPublicKeyString().equals(getLocalPublicKeyString())) orcidPubkeys.add(kd);
		}
		return orcidPubkeys;
	}
}
