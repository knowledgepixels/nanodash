package org.petapico.nanobench;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.server.PublishNanopub;

import net.trustyuri.TrustyUriException;

public class ProfileIntroItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileIntroItem(String id) {
		super(id);

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
				if (ProfilePage.getUserIri() == null || ProfilePage.getKeyPair() == null) return;
				try {
					Nanopub np = createIntroNanopub();
					Nanopub signedNp = SignNanopub.signAndTransform(np, SignatureAlgorithm.RSA, ProfilePage.getKeyPair());
					PublishNanopub.publish(signedNp);
					ProfilePage.setIntroNanopub(signedNp);
//					System.err.println(NanopubUtils.writeToString(signedNp, RDFFormat.TRIG));
					throw new RestartResponseException(ProfilePage.class);
				} catch (IOException | MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
//				} catch (MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
					ex.printStackTrace();
				}
			}

		};
		if (ProfilePage.getUserIri() != null && ProfilePage.getKeyPair() != null) {
			if (ProfilePage.getIntroNanopub() != null) {
				String introUri = ProfilePage.getIntroNanopub().getNanopub().getUri().stringValue();
				introlink = new ExternalLink("introlink", introUri);
				introlink.add(new Label("introlinktext", introUri));
				if (ProfilePage.doPubkeysMatch()) {
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
			introlink.setVisible(false);
			intromessage = new Label("intromessage", "Lastly, you need to publish an introduction nanopublication to publicly link your ORCID to the public key above:");
		}
		if (ProfilePage.getUserIri() == null || ProfilePage.getKeyPair() == null) {
			intromessage.setVisible(false);
			createIntroLink.setVisible(false);
		}
		add(introlink);
		add(intromessage);
		add(createIntroLink);
	}

	private Nanopub createIntroNanopub() throws MalformedNanopubException {
		if (ProfilePage.getUserIri() == null || ProfilePage.getKeyPair() == null) return null;
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
		npCreator.addAssertionStatement(keyDecl, CryptoElement.HAS_PUBLIC_KEY, vf.createLiteral(ProfilePage.getPubkeyString()));
		npCreator.addAssertionStatement(keyDecl, KeyDeclaration.DECLARED_BY, ProfilePage.getUserIri());
		String orcidName = ProfilePage.getOrcidName();
		if (orcidName != null) {
			npCreator.addAssertionStatement(ProfilePage.getUserIri(), FOAF.NAME, vf.createLiteral(orcidName));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, ProfilePage.getUserIri());
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(DCTERMS.CREATOR, ProfilePage.getUserIri());
		return npCreator.finalizeNanopub();
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
