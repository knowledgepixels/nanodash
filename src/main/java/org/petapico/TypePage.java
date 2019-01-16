package org.petapico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.Nanopub2Html;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;

public class TypePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public TypePage(final PageParameters parameters) {
		String typeId = parameters.get("id").toString();
		add(new Label("typeid", typeId));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("type", typeId);
		List<String> nanopubs = ApiAccess.getAll("find_latest_nanopubs_with_type", nanopubParams, 0);

		List<String> displayNanopubs = new ArrayList<String>();
		for (int i = 0 ; i < 10 && i < nanopubs.size() ; i++) {
			displayNanopubs.add(nanopubs.get(i));
		}

		add(new DataView<String>("nanopubs", new ListDataProvider<String>(displayNanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				ExternalLink link = new ExternalLink("nanopub-id-link", item.getModelObject());
				link.add(new Label("nanopub-id-text", item.getModelObject()));
				item.add(link);
				Nanopub np = GetNanopub.get(item.getModelObject());
				item.add(new Label("datetime", SimpleTimestampPattern.getCreationTime(np).getTime().toString()));
				String signatureNote = "not signed";
				if (SignatureUtils.seemsToHaveSignature(np)) {
					try {
						if (SignatureUtils.hasValidSignature(SignatureUtils.getSignatureElement(np))) {
							signatureNote = "valid signature";
						} else {
							signatureNote = "invalid signature";
						}
					} catch (Exception ex) {
						signatureNote = "malformed signature";
					}
				}
				item.add(new Label("notes", signatureNote));
				String html = Nanopub2Html.createHtmlString(np, false, false);
				Label l = new Label("nanopub", html);
				l.setEscapeModelStrings(false);
				item.add(l);
			}

		});
	}

}
