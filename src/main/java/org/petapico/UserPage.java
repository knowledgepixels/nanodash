package org.petapico;

import java.util.ArrayList;
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
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.KeyDeclaration;


public class UserPage extends WebPage {

	public UserPage(final PageParameters parameters) {
		String userId = parameters.getString("id");
		add(new Label("userid", userId));


		IntroNanopub introNanopub = null;
		try {
			introNanopub = IntroNanopub.get(userId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		List<KeyDeclaration> keyDeclarations;
		if (introNanopub != null && introNanopub.getNanopub() != null) {
			Nanopub np = introNanopub.getNanopub();
			ExternalLink l = new ExternalLink("intro-nanopub", np.getUri().stringValue());
			l.add(new Label("intro-nanopub-linktext", np.getUri().stringValue()));
			add(l);
	
			Map<String,String> p = new HashMap<>();
			p.put("user", userId);
			keyDeclarations = introNanopub.getKeyDeclarations();
		} else {
			ExternalLink l = new ExternalLink("intro-nanopub", "#");
			l.add(new Label("intro-nanopub-linktext", "none found"));
			add(l);
			keyDeclarations = new ArrayList<>();
		}
		add(new DataView<KeyDeclaration>("pubkeys", new ListDataProvider<KeyDeclaration>(keyDeclarations)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<KeyDeclaration> item) {
				KeyDeclaration d = item.getModelObject();
				String s = d.getPublicKeyString() + "";
				if (s.length() > 30) s = s.substring(0, 10) + "..." + s.substring(s.length() - 10);
				item.add(new Label("pubkey", s));
			}

		});

		List<String> nanopubs;
		if (keyDeclarations.isEmpty()) {
			nanopubs = new ArrayList<>();
		} else {
			Map<String,String> nanopubParams = new HashMap<>();
			nanopubParams.put("publickey", keyDeclarations.get(0).getPublicKeyString());  // TODO: only using first public key here
			nanopubParams.put("creator", userId);
			nanopubs = ApiAccess.getAll("find_latest_nanopubs", nanopubParams, 0);
		}
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
