package org.petapico.nanobench;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.codec.Charsets;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;

import net.trustyuri.TrustyUriUtils;

public class ProfileIntroItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileIntroItem(String id) {
		super(id);

		final NanobenchSession session = NanobenchSession.get();

		List<IntroNanopub> introList = new ArrayList<>(User.getIntroNanopubs(session.getUserIri()).values());
		add(new DataView<IntroNanopub>("intro-nps", new ListDataProvider<IntroNanopub>(introList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IntroNanopub> item) {
				final IntroNanopub inp = item.getModelObject();
				String uri = inp.getNanopub().getUri().stringValue();
				ExternalLink link = new ExternalLink("intro-uri", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8));
				link.add(new Label("intro-uri-label", TrustyUriUtils.getArtifactCode(uri).substring(0, 10)));
				item.add(link);
				if (User.isApproved(inp)) {
					item.add(new Label("intro-note", " <strong class=\"positive\">(approved)</strong>").setEscapeModelStrings(false));
				} else {
					item.add(new Label("intro-note", ""));
				}
				try {
					NanopubSignatureElement el = SignatureUtils.getSignatureElement(inp.getNanopub());
					IRI location = null;
					for (KeyDeclaration kd : inp.getKeyDeclarations()) {
						if (kd.getPublicKeyString().equals(el.getPublicKeyString())) {
							location = kd.getKeyLocation();
							break;
						}
					}
					String locationString = "";
					String siteUrl = NanobenchPreferences.get().getWebsiteUrl();
					boolean sameLocation = location == null || siteUrl == null || location.stringValue().equals(siteUrl);
					if (session.getPubkeyString().equals(el.getPublicKeyString()) && sameLocation) {
						item.add(new Label("location", "this site you are currently using"));
					} else if (location == null) {
						item.add(new Label("location", "unknown site"));
					} else {
						locationString = location.stringValue();
						item.add(new ExternalLink("location", locationString, locationString));
					}
					Calendar creationDate = SimpleTimestampPattern.getCreationTime(inp.getNanopub());
					item.add(new Label("date", (creationDate == null ? "unknown date" : NanopubItem.simpleDateFormat.format(creationDate.getTime()))));

					WebMarkupContainer addLocalKeyPart = new WebMarkupContainer("add-local-key-part");
					String pubkeyLabel = encode(Utils.getShortPubkeyName(session.getPubkeyString()));
					String pubkeyLocation = "";
					if (NanobenchPreferences.get().getWebsiteUrl() != null) {
						pubkeyLocation = encode(NanobenchPreferences.get().getWebsiteUrl());
					}
					String addLocalKeyLink = locationString + "publish?template=http://purl.org/np/RAr2tFRzWYsYNdtfZBkT9b47gbLWiHM_Sd_uenlqcYKt8&" +
							"supersede-a=" + encode(inp.getNanopub().getUri()) + "&" +
							"param_public-key__.1=" + encode(session.getPubkeyString()) + "&" +
							"param_key-declaration__.1=" + pubkeyLabel + "&" +
							"param_key-declaration-ref__.1=" + pubkeyLabel + "&" +
							"param_key-location__.1=" + pubkeyLocation + "&" +
							"sigkey=" + encode(el.getPublicKeyString());
					addLocalKeyPart.add(new ExternalLink("add-local-key", addLocalKeyLink, "add local key"));
					addLocalKeyPart.add(new Label("location2", locationString));
					item.add(addLocalKeyPart);
					if (session.getPubkeyString().equals(el.getPublicKeyString()) || location == null) {
						addLocalKeyPart.setVisible(false);
					}
				} catch (MalformedCryptoElementException ex) {
					throw new RuntimeException(ex);
				}
				
				item.add(new DataView<KeyDeclaration>("intro-keys", new ListDataProvider<KeyDeclaration>(inp.getKeyDeclarations())) {
	
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<KeyDeclaration> kdi) {
						kdi.add(new Label("intro-key", Utils.getShortPubkeyName(kdi.getModelObject().getPublicKeyString())));
					}
					
				});
			}

		});

//		ExternalLink introlink = null;
//		Label intromessage = null;
//		Link<String> createIntroLink = new Link<String>("createintro") {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public MarkupContainer setDefaultModel(IModel<?> arg0) {
//				return null;
//			}
//
//			@Override
//			public void onClick() {
//				if (session.getUserIri() == null || session.getKeyPair() == null) return;
//				try {
//					Nanopub np = createIntroNanopub();
//					TransformContext tc = new TransformContext(SignatureAlgorithm.RSA, session.getKeyPair(), null, false, false);
//					Nanopub signedNp = SignNanopub.signAndTransform(np, tc);
//					PublishNanopub.publish(signedNp);
//					// TODO This needs testing:
//					User.register(User.toIntroNanopub(signedNp), false);
////					System.err.println(NanopubUtils.writeToString(signedNp, RDFFormat.TRIG));
//					throw new RestartResponseException(ProfilePage.class);
//				} catch (IOException | MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
////				} catch (MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
//					ex.printStackTrace();
//				}
//			}
//
//		};
//		if (session.getUserIri() != null && session.getKeyPair() != null) {
//			if (session.getIntroNanopubs() != null && !session.getIntroNanopubs().isEmpty()) {
//				// TODO Consider all intro nanopubs:
//				String introUri = session.getIntroNanopubs().values().iterator().next().getNanopub().getUri().stringValue();
//				introlink = new ExternalLink("introlink", introUri);
//				introlink.add(new Label("introlinktext", introUri));
//				if (session.doPubkeysMatch()) {
//					intromessage = new Label("intromessage", "");
//					intromessage.setVisible(false);
//				} else {
//					intromessage = new Label("intromessage", "Public key of the introduction nanopublication doesn't match.");
//				}
//				createIntroLink.setVisible(false);
//			}
//		}
//		if (introlink == null) {
//			introlink = new ExternalLink("introlink", "#");
//			introlink.add(new Label("introlinktext", ""));
//			introlink.setVisible(false);
//			intromessage = new Label("intromessage", "Lastly, you need to publish an introduction nanopublication to publicly link your ORCID to the public key above:");
//		}
//		if (session.getUserIri() == null || session.getKeyPair() == null) {
//			intromessage.setVisible(false);
//			createIntroLink.setVisible(false);
//		}
//		add(introlink);
//		add(intromessage);
//		add(createIntroLink);
	}

	private static String encode(Object o) {
		return URLEncoder.encode(o.toString(), Charsets.UTF_8);
	}

//	private Nanopub createIntroNanopub() throws MalformedNanopubException {
//		NanobenchSession session = NanobenchSession.get();
//		if (session.getUserIri() == null || session.getKeyPair() == null) return null;
//		String tns = "http://purl.org/nanopub/temp/";
//		NanopubCreator npCreator = new NanopubCreator(vf.createIRI(tns));
//		npCreator.addNamespace("", tns);
//		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/#");
//		npCreator.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
//		npCreator.addNamespace("dct", "http://purl.org/dc/terms/");
//		npCreator.addNamespace("prov", "http://www.w3.org/ns/prov#");
//		npCreator.addNamespace("orcid", "https://orcid.org/");
//		npCreator.addNamespace("foaf", "http://xmlns.com/foaf/0.1/");
//		npCreator.addNamespace("np", "http://www.nanopub.org/nschema#");
//		npCreator.addNamespace("npx", "http://purl.org/nanopub/x/");
//		IRI keyDecl = vf.createIRI(tns + "keyDeclaration");
//		npCreator.addAssertionStatement(keyDecl, CryptoElement.HAS_ALGORITHM, vf.createLiteral("RSA"));
//		npCreator.addAssertionStatement(keyDecl, CryptoElement.HAS_PUBLIC_KEY, vf.createLiteral(session.getPubkeyString()));
//		npCreator.addAssertionStatement(keyDecl, KeyDeclaration.DECLARED_BY, session.getUserIri());
//		String orcidName = session.getOrcidName();
//		if (orcidName != null) {
//			npCreator.addAssertionStatement(session.getUserIri(), FOAF.NAME, vf.createLiteral(orcidName));
//		}
//		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, session.getUserIri());
//		npCreator.addTimestampNow();
//		npCreator.addPubinfoStatement(DCTERMS.CREATOR, session.getUserIri());
//		return npCreator.finalizeNanopub();
//	}

}
