package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();
	private static IntroExtractor introExtractor;

	private File orcidFile = new File(System.getProperty("user.home") + "/.nanopub/orcid");
	private File keyFile = new File(System.getProperty("user.home") + "/.nanopub/id_rsa");

	private KeyPair keyPair;
	private IRI userIri;
	private IntroNanopub introNp;
	private Boolean isOrcidLinked;
	private String orcidLinkError;

	private boolean showProvenance = true;
	private boolean showPubinfo = false;

	public void loadProfileInfo() {
		NanobenchPreferences prefs = NanobenchPreferences.get();
		if (userIri == null && !prefs.isReadOnlyMode() && !prefs.isOrcidLoginMode()) {
			if (orcidFile.exists()) {
				try {
					String orcid = FileUtils.readFileToString(orcidFile, StandardCharsets.UTF_8).trim();
	//				String orcid = Files.readString(orcidFile.toPath(), StandardCharsets.UTF_8).trim();
					if (orcid.matches(ProfilePage.ORCID_PATTERN)) {
						userIri = vf.createIRI("https://orcid.org/" + orcid);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		if (keyPair == null) {
			try {
				keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
			} catch (Exception ex) {
				System.err.println("Key pair not found");
			}
		}
		if (userIri != null && introNp == null) {
			User user = User.getUser(getUserIri().toString());
			if (user != null) {
				Nanopub np = Utils.getNanopub(user.getIntropubIri().stringValue());
				introNp = new IntroNanopub(np, user.getId());
			}
		}
		checkOrcidLink();
	}

	public boolean isProfileComplete() {
		return userIri != null && keyPair != null && introNp != null && doPubkeysMatch();
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
			MakeKeys.make(keyFile.getAbsolutePath().replaceFirst("_rsa$", ""), SignatureAlgorithm.RSA);
			keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public IRI getUserIri() {
		return userIri;
	}

	public void setOrcid(String orcid) {
		if (orcid.matches(ProfilePage.ORCID_PATTERN)) {
			try {
				FileUtils.writeStringToFile(orcidFile, orcid + "\n", StandardCharsets.UTF_8);
//				Files.writeString(orcidFile.toPath(), orcid + "\n");
				userIri = vf.createIRI("https://orcid.org/" + orcid);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
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

	public File getKeyfile() {
		return keyFile;
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

	public void setShowPubinfooEnabled(boolean showPubinfo) {
		this.showPubinfo = showPubinfo;
	}

}