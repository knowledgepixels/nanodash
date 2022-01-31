package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;

public class ProfilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static final String ORCID_PATTERN = "[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]";

	public ProfilePage(final PageParameters parameters) {
		super();
		loadProfileInfo();
		User.refreshUsers();

		add(new TitleBar("titlebar"));

		if (isComplete()) {
			if (isOrcidLinked == null) {
				add(new Label("message", "Congratulations, your profile is complete, but we couldn't verify whether your ORCID account is linked (see below)."));
			} else if (isOrcidLinked) {
				add(new Label("message", "Congratulations, your profile complete. Use the menu items above to browse and publish nanopublications."));
			} else {
				add(new Label("message", "Congratulations, your profile is sufficiently completed to publish your own nanopublications, " +
						"but consider also linking your ORCID account as explained below."));
			}
		} else {
			add(new Label("message", "You need to set an ORCID identifier, load the signature keys, and publish an " +
					"introduction before you can publish nanopublications."));
		}

		if (userIri == null) {
			add(new Label("orcidmessage", "First, you need to enter your ORCID identifier below and press 'update'. " +
					"If you don't yet have an ORCID account, you can make one via the " +
					"<a href=\"https://orcid.org/\">ORCID website</a>.").setEscapeModelStrings(false));
		} else {
			add(new Label("orcidmessage", ""));
		}

		Model<String> model = Model.of("");
		if (getUserIri() != null) {
			model.setObject(getUserIri().stringValue().replaceFirst("^https://orcid.org/", ""));
		}
		final TextField<String> orcidField = new TextField<>("orcidfield", model);
		orcidField.add(new PatternValidator(ORCID_PATTERN));
		Form<Void> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				setOrcid(orcidField.getModelObject());
				introNp = null;
				isOrcidLinked = null;
				getSession().invalidateNow();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		form.add(orcidField);
		String orcidName = getOrcidName();
		if (orcidName == null) {
			form.add(new Label("orcidname", ""));
		} else {
			form.add(new Label("orcidname", orcidName));
		}
		add(form);
		add(new FeedbackPanel("feedback"));

		if (userIri != null) {
			add(new ProfileSigItem("sigpart"));
		} else {
			add(new Label("sigpart"));
		}

		if (userIri != null && keyPair != null) {
			add(new ProfileIntroItem("intropart"));
		} else {
			add(new Label("intropart"));
		}

		if (userIri != null && keyPair != null && introNp != null) {
			add(new ProfileOrcidLinkItem("orcidlinkpart"));
		} else {
			add(new Label("orcidlinkpart"));
		}
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static File orcidFile = new File(System.getProperty("user.home") + "/.nanopub/orcid");
	private static File keyFile = new File(System.getProperty("user.home") + "/.nanopub/id_rsa");

	private static KeyPair keyPair;
	private static IRI userIri;
	private static IntroNanopub introNp;
	private static IntroExtractor introExtractor;
	private static Boolean isOrcidLinked;
	private static String orcidLinkError;

	static void loadProfileInfo() {
		if (userIri == null && !NanobenchPreferences.get().isReadOnlyMode()) {
			if (orcidFile.exists()) {
				try {
					String orcid = FileUtils.readFileToString(orcidFile, StandardCharsets.UTF_8).trim();
	//				String orcid = Files.readString(orcidFile.toPath(), StandardCharsets.UTF_8).trim();
					if (orcid.matches(ORCID_PATTERN)) {
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

	static boolean isComplete() {
		return userIri != null && keyPair != null && introNp != null && doPubkeysMatch();
	}

	static boolean doPubkeysMatch() {
		if (keyPair == null) return false;
		if (introNp == null) return false;
		// TODO: Handle case of multiple key declarations
		return getPubkeyString().equals(introNp.getKeyDeclarations().get(0).getPublicKeyString());
	}

	static String getPubkeyString() {
		if (keyPair == null) return null;
		return DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
	}

	static KeyPair getKeyPair() {
		return keyPair;
	}

	static void makeKeys() {
		try {
			MakeKeys.make(keyFile.getAbsolutePath().replaceFirst("_rsa$", ""), SignatureAlgorithm.RSA);
			keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	static IRI getUserIri() {
		return userIri;
	}

	private static void setOrcid(String orcid) {
		if (orcid.matches(ORCID_PATTERN)) {
			try {
				FileUtils.writeStringToFile(orcidFile, orcid + "\n", StandardCharsets.UTF_8);
//				Files.writeString(orcidFile.toPath(), orcid + "\n");
				userIri = vf.createIRI("https://orcid.org/" + orcid);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	static IntroNanopub getIntroNanopub() {
		return introNp;
	}

	static void setIntroNanopub(Nanopub np) {
		introNp = new IntroNanopub(np, userIri);
	}

	static void checkOrcidLink() {
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

	static void resetOrcidLinked() {
		isOrcidLinked = null;
	}

	static Boolean isOrcidLinked() {
		return isOrcidLinked;
	}

	static String getOrcidLinkError() {
		return orcidLinkError;
	}

	static String getOrcidName() {
		if (introExtractor == null || introExtractor.getName() == null) return null;
		if (introExtractor.getName().trim().isEmpty()) return null;
		return introExtractor.getName();
	}

	static File getKeyfile() {
		return keyFile;
	}

}
