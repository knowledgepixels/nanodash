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
		final NanobenchPreferences prefs = NanobenchPreferences.get();

		String publishIntroLinkString = "./publish?template=http://purl.org/np/RAr2tFRzWYsYNdtfZBkT9b47gbLWiHM_Sd_uenlqcYKt8&" +
				"param_user=" + encode(Utils.getShortOrcidId(session.getUserIri())) + "&" +
				"param_name=" + encode(session.getOrcidName()) + "&" +
				"param_public-key=" + encode(session.getPubkeyString()) + "&" +
				"param_key-declaration=" + encode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
				"param_key-declaration-ref=" + encode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
				"param_key-location=" + encode(prefs.getWebsiteUrl());
		final ExternalLink publishIntroLink = new ExternalLink("publish-intro-link", publishIntroLinkString, "publish new introduction");
		add(publishIntroLink);

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
					String siteUrl = prefs.getWebsiteUrl();
					boolean ownLocation = location == null || siteUrl == null || location.stringValue().equals(siteUrl);
					if (session.getPubkeyString().equals(el.getPublicKeyString()) && ownLocation) {
						publishIntroLink.setVisible(false);
						System.err.println("ADD");
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
					String pubkeyLocation = "";
					if (NanobenchPreferences.get().getWebsiteUrl() != null) {
						pubkeyLocation = encode(NanobenchPreferences.get().getWebsiteUrl());
					}
					String addLocalKeyLink = locationString + "publish?template=http://purl.org/np/RAr2tFRzWYsYNdtfZBkT9b47gbLWiHM_Sd_uenlqcYKt8&" +
							"supersede-a=" + encode(inp.getNanopub().getUri()) + "&" +
							"param_public-key__.1=" + encode(session.getPubkeyString()) + "&" +
							"param_key-declaration__.1=" + encode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
							"param_key-declaration-ref__.1=" + encode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
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

	}

	private static String encode(Object o) {
		return URLEncoder.encode(o.toString(), Charsets.UTF_8);
	}

}
