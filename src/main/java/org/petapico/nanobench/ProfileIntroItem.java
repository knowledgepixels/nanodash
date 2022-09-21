package org.petapico.nanobench;

import static org.petapico.nanobench.Utils.urlEncode;

import java.net.URLEncoder;
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

	private NanobenchSession session = NanobenchSession.get();
	private NanobenchPreferences prefs = NanobenchPreferences.get();
	private List<IntroNanopub> introList = User.getIntroNanopubs(session.getUserIri());
	private Integer introWithLocalKeyCount = null;

	public ProfileIntroItem(String id) {
		super(id);

		String publishIntroLinkString = "./publish?template=http://purl.org/np/RAr2tFRzWYsYNdtfZBkT9b47gbLWiHM_Sd_uenlqcYKt8&" +
				"param_user=" + urlEncode(Utils.getShortOrcidId(session.getUserIri())) + "&" +
				"param_name=" + urlEncode(session.getOrcidName()) + "&" +
				"param_public-key=" + urlEncode(session.getPubkeyString()) + "&" +
				"param_key-declaration=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
				"param_key-declaration-ref=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
				"param_key-location=" + urlEncode(prefs.getWebsiteUrl());
		WebMarkupContainer publishIntroItem = new WebMarkupContainer("publish-intro-item");
		publishIntroItem.add(new ExternalLink("publish-intro-link", publishIntroLinkString, "publish introduction"));
		add(publishIntroItem);
		publishIntroItem.setVisible(false);
		if (introList.isEmpty()) {
			add(new Label("intro-note", "<em>There are no introductions yet.</em>").setEscapeModelStrings(false));
		} else if (getIntroWithLocalKeyCount() == 0) {
			// TODO: Check whether it's part of an introduction for a different ORCID, and show a warning if so
			add(new Label("intro-note", "The local key from this site is <strong class=\"negative\">not part of an introduction</strong> yet.").setEscapeModelStrings(false));
		} else if (getIntroWithLocalKeyCount() == 1) {
			add(new Label("intro-note", ""));
		} else {
			add(new Label("intro-note", "You have <strong class=\"negative\">multiple introduction records from this site</strong>.").setEscapeModelStrings(false));
		}
		if (getIntroWithLocalKeyCount() == 0) {
			publishIntroItem.setVisible(true);
			add(new Label("action-note"));
		} else {
			add(new Label("action-note", "<em>There are no recommended actions.</em>").setEscapeModelStrings(false));
		}

		add(new DataView<IntroNanopub>("intro-nps", new ListDataProvider<IntroNanopub>(introList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IntroNanopub> item) {
				final IntroNanopub inp = item.getModelObject();
				NanopubSignatureElement el = getNanopubSignatureElement(inp);
				IRI location = getLocation(inp);
				String uri = inp.getNanopub().getUri().stringValue();
				ExternalLink link = new ExternalLink("intro-uri", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8));
				link.add(new Label("intro-uri-label", TrustyUriUtils.getArtifactCode(uri).substring(0, 10)));
				item.add(link);
				if (User.isApproved(inp)) {
					item.add(new Label("intro-note", " <strong class=\"positive\">(approved)</strong>").setEscapeModelStrings(false));
				} else {
					item.add(new Label("intro-note", ""));
				}
				String locationString = "";
				if (isIntroWithLocalKey(inp)) {
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
					pubkeyLocation = urlEncode(NanobenchPreferences.get().getWebsiteUrl());
				}
				String addLocalKeyLink = locationString + "publish?template=http://purl.org/np/RAr2tFRzWYsYNdtfZBkT9b47gbLWiHM_Sd_uenlqcYKt8&" +
						"supersede-a=" + urlEncode(inp.getNanopub().getUri()) + "&" +
						"param_public-key__.1=" + urlEncode(session.getPubkeyString()) + "&" +
						"param_key-declaration__.1=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
						"param_key-declaration-ref__.1=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
						"param_key-location__.1=" + pubkeyLocation + "&" +
						"sigkey=" + urlEncode(el.getPublicKeyString());
				addLocalKeyPart.add(new ExternalLink("add-local-key", addLocalKeyLink, "add local key"));
				addLocalKeyPart.add(new Label("location2", locationString));
				item.add(addLocalKeyPart);
				if (session.getPubkeyString().equals(el.getPublicKeyString()) || location == null) {
					addLocalKeyPart.setVisible(false);
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

	private int getIntroWithLocalKeyCount() {
		if (introWithLocalKeyCount == null) {
			introWithLocalKeyCount = 0;
			for (IntroNanopub inp : introList) {
				if (isIntroWithLocalKey(inp)) introWithLocalKeyCount++;
			}
		}
		return introWithLocalKeyCount;
	}

	private boolean isIntroWithLocalKey(IntroNanopub inp) {
		IRI location = getLocation(inp);
		NanopubSignatureElement el = getNanopubSignatureElement(inp);
		String siteUrl = prefs.getWebsiteUrl();
		if (location != null && siteUrl != null && !location.stringValue().equals(siteUrl)) return false;
		if (!session.getPubkeyString().equals(el.getPublicKeyString())) return false;
		for (KeyDeclaration kd : inp.getKeyDeclarations()) {
			if (session.getPubkeyString().equals(kd.getPublicKeyString())) return true;
		}
		return false;
	}

	private IRI getLocation(IntroNanopub inp) {
		NanopubSignatureElement el = getNanopubSignatureElement(inp);
		for (KeyDeclaration kd : inp.getKeyDeclarations()) {
			if (kd.getPublicKeyString().equals(el.getPublicKeyString())) {
				return kd.getKeyLocation();
			}
		}
		return null;
	}

	public NanopubSignatureElement getNanopubSignatureElement(IntroNanopub inp) {
		try {
			return SignatureUtils.getSignatureElement(inp.getNanopub());
		} catch (MalformedCryptoElementException ex) {
			throw new RuntimeException(ex);
		}
	}

}
