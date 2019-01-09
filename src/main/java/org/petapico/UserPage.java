package org.petapico;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.GetIntroNanopub;


public class UserPage extends WebPage {

	public UserPage(final PageParameters parameters) {
		String userId = parameters.getString("id");
		add(new Label("userid", userId));

		Map<String,String> p = new HashMap<>();
		p.put("user", userId);
		List<String> pubkeys = ApiAccess.getAll("get_publickeys_for_user", p, 0);
		add(new DataView<String>("pubkeys", new ListDataProvider<String>(pubkeys)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				String s = item.getModelObject();
				if (s.length() > 50) s = s.substring(0, 10) + "..." + s.substring(s.length() - 20);
				item.add(new Label("pubkey", s));
			}

		});

		Nanopub introNanopub = null;
		try {
			introNanopub = GetIntroNanopub.get(userId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (introNanopub != null) {
			ExternalLink l = new ExternalLink("intro-nanopub", introNanopub.getUri().stringValue());
			l.add(new Label("intro-nanopub-linktext", introNanopub.getUri().stringValue()));
			add(l);
		} else {
			ExternalLink l = new ExternalLink("intro-nanopub", "#");
			l.add(new Label("intro-nanopub-linktext", "none found"));
			add(l);
		}

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("publickey", pubkeys.get(0));  // TODO: only using first public key here
		nanopubParams.put("creator", userId);
		List<String> nanopubs = ApiAccess.getAll("find_latest_nanopubs", nanopubParams, 0);
		add(new DataView<String>("nanopubs", new ListDataProvider<String>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				ExternalLink l = new ExternalLink("nanopub", item.getModelObject());
				l.add(new Label("nanopub-linktext", item.getModelObject()));
				item.add(l);
			}

		});
	}

}
