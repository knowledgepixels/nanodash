package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.extra.security.CryptoElement;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.server.PublishNanopub;

import net.trustyuri.TrustyUriException;

public class ProfilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static final String ORCID_PATTERN = "[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]";

	Model<String> messageModel = Model.of("");

	public ProfilePage(final PageParameters parameters) {
		super();
		User.getUsers(true);  // refresh

		add(new TitleBar("titlebar"));

		add(new Label("message", messageModel));
		updateMessage();

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

		Label keymessage = new Label("keymessage", "No key file found.");
		Link<String> createKeyLink = new Link<String>("createkey") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				try {
					MakeKeys.make(keyFile.getAbsolutePath().replaceFirst("_rsa$", ""), SignatureAlgorithm.RSA);
					throw new RestartResponseException(ProfilePage.class);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

		};
		add(new Label("keyfile", keyFile.getPath()));
		if (keyFile.exists()) {
			if (getKeyPair() == null) {
				add(new Label("pubkey", "Error loading key file"));
			} else {
				String pubkeyString = DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
				add(new Label("pubkey", pubkeyString));
			}
			keymessage.setVisible(false);
			createKeyLink.setVisible(false);
		} else {
			add(new Label("pubkey", ""));
		}
		add(keymessage);
		add(createKeyLink);

		ExternalLink introlink = null;
		Label intromessage = null;
		Link<String> createIntroLink = new Link<String>("createintro") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				if (userIri == null || keyPair == null) return;
				try {
					Nanopub np = createIntroNanopub();
					Nanopub signedNp = SignNanopub.signAndTransform(np, SignatureAlgorithm.RSA, ProfilePage.getKeyPair());
					PublishNanopub.publish(signedNp);
					introNp = new IntroNanopub(signedNp, userIri);
//					System.err.println(NanopubUtils.writeToString(signedNp, RDFFormat.TRIG));
					throw new RestartResponseException(ProfilePage.class);
				} catch (IOException | MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
//				} catch (MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
					ex.printStackTrace();
				}
			}

		};
		if (getUserIri() != null && getKeyPair() != null) {
			if (getIntroNanopub() != null) {
				String id = getIntroNanopub().getNanopub().getUri().stringValue();
				introlink = new ExternalLink("introlink", id);
				introlink.add(new Label("introlinktext", id));
				if (doPubkeysMatch()) {
					intromessage = new Label("intromessage", "");
					intromessage.setVisible(false);
				} else {
					intromessage = new Label("intromessage", "Public key of the introduction nanopublication doesn't match.");
				}
				createIntroLink.setVisible(false);
			}
		}
		if (introlink == null) {
			introlink = new ExternalLink("introlink", "#");
			introlink.add(new Label("introlinktext", ""));
			intromessage = new Label("intromessage", "No introduction nanopublication found.");
		}
		if (getUserIri() == null || getKeyPair() == null) {
			intromessage.setVisible(false);
			createIntroLink.setVisible(false);
		}
		add(introlink);
		add(intromessage);
		add(createIntroLink);

		Link<String> retryLink = new Link<String>("retry") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		retryLink.setVisible(false);
		if (userIri != null && introNp != null) {
			if (isOrcidLinked == null) {
				add(new Label("orcidlinkmessage", ""));
				retryLink.setVisible(true);
			} else if (isOrcidLinked) {
				add(new Label("orcidlinkmessage", "ORCID is correctly linked."));
			} else {
				add(new Label("orcidlinkmessage", "ORCID is not yet linked."));
			}
		} else {
			add(new Label("orcidlinkmessage", ""));
		}
		add(new Label("orcidlinkerror", orcidLinkError));
		add(retryLink);
	}

	private Nanopub createIntroNanopub() throws MalformedNanopubException {
		if (userIri == null || keyPair == null) return null;
		String tns = "http://purl.org/nanopub/temp/";
		NanopubCreator npCreator = new NanopubCreator(vf.createIRI(tns));
		npCreator.addNamespace("", tns);
		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/#");
		npCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
		npCreator.addNamespace("dct", "http://purl.org/dc/terms/");
		npCreator.addNamespace("prov", "http://www.w3.org/ns/prov#");
		npCreator.addNamespace("orcid", "https://orcid.org/");
		npCreator.addNamespace("foaf", "http://xmlns.com/foaf/0.1/");
		npCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
		npCreator.addNamespace("npx", "http://purl.org/nanopub/x/");
		IRI keyDecl = vf.createIRI(tns + "keyDeclaration");
		npCreator.addAssertionStatement(keyDecl, CryptoElement.HAS_ALGORITHM, vf.createLiteral("RSA"));
		npCreator.addAssertionStatement(keyDecl, CryptoElement.HAS_PUBLIC_KEY, vf.createLiteral(getPubkeyString()));
		npCreator.addAssertionStatement(keyDecl, KeyDeclaration.DECLARED_BY, userIri);
		String orcidName = getOrcidName();
		if (orcidName != null) {
			npCreator.addAssertionStatement(userIri, FOAF.NAME, vf.createLiteral(orcidName));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, userIri);
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(DCTERMS.CREATOR, userIri);
		return npCreator.finalizeNanopub();
	}

	private void updateMessage() {
		if (isComplete()) {
			messageModel.setObject("");
		} else {
			messageModel.setObject("You need to set an ORCID identifier, load the signature keys, and publish an introduction before you can publish nanopublications.");
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

	static boolean isComplete() {
		return getUserIri() != null && getKeyPair() != null && getIntroNanopub() != null && doPubkeysMatch();
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
		if (keyPair == null) {
			try {
				keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return keyPair;
	}

	static IRI getUserIri() {
		if (userIri == null) {
			if (orcidFile.exists()) {
				try {
					String orcid = FileUtils.readFileToString(orcidFile, StandardCharsets.UTF_8).trim();
//					String orcid = Files.readString(orcidFile.toPath(), StandardCharsets.UTF_8).trim();
					if (orcid.matches(ORCID_PATTERN)) {
						userIri = vf.createIRI("https://orcid.org/" + orcid);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
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
		if (introNp == null) {
			User user = User.getUser(getUserIri().toString());
			if (user != null) {
				Nanopub np = Utils.getNanopub(user.getIntropubIri().stringValue());
				introNp = new IntroNanopub(np, user.getId());
			}
		}
		return introNp;
	}

	static void checkOrcidLink() {
		if (isOrcidLinked == null) {
			orcidLinkError = null;
			introExtractor = null;
			try {
				introExtractor = IntroNanopub.extract(userIri.stringValue(), null);
				IntroNanopub inp = IntroNanopub.get(userIri.stringValue(), introExtractor);
				if (inp.getNanopub() == null) {
					isOrcidLinked = false;
				} else if (inp.getNanopub().getUri().equals(introNp.getNanopub().getUri())) {
					isOrcidLinked = true;
				} else {
					isOrcidLinked = false;
					orcidLinkError = "Error: ORCID is linked to another introduction nanopublication.";
				}
			} catch (RDF4JException | IOException ex) {
				ex.printStackTrace();
				orcidLinkError = "ORCID check failed.";
				isOrcidLinked = false;
			} catch (Exception ex) {
				ex.printStackTrace();
				isOrcidLinked = false;
			}
		}
	}

	static String getOrcidName() {
		checkOrcidLink();
		if (introExtractor == null || introExtractor.getName() == null) return null;
		if (introExtractor.getName().trim().isEmpty()) return null;
		return introExtractor.getName();
	}

}
