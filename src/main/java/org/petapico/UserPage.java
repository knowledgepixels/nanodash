package org.petapico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.KeyDeclaration;

public class UserPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private Model<String> progress;
	private boolean nanopubsReady = false;

	public UserPage(final PageParameters parameters) {
		String userId = parameters.get("id").toString();
		add(new Label("userid", userId));

		IntroNanopub introNanopub = Utils.getIntroNanopub(userId);
		List<KeyDeclaration> keyDeclarations;
		if (introNanopub != null && introNanopub.getNanopub() != null) {
			Nanopub np = introNanopub.getNanopub();
			ExternalLink l = new ExternalLink("intro-nanopub", np.getUri().stringValue());
			l.add(new Label("intro-nanopub-linktext", "Introduction"));
			add(l);
	
			Map<String,String> p = new HashMap<>();
			p.put("user", userId);
			keyDeclarations = introNanopub.getKeyDeclarations();
		} else {
			ExternalLink l = new ExternalLink("intro-nanopub", "#");
			l.add(new Label("intro-nanopub-linktext", "No introduction found"));
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

		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
		progressLabel.setOutputMarkupId(true);
		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(500)));
		add(progressLabel);

		final List<NanopubElement> nanopubs = new ArrayList<>();

		add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isContentReady() {
				return nanopubsReady;
			};

			@Override
			protected Duration getUpdateInterval() {
				return Duration.milliseconds(1000);
			};

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				progress.setObject("");
				return new NanopubResults(markupId, nanopubs);
			}
		});
		

		Thread loadContent = new Thread() {
			@Override
			public void run() {
				Map<String,String> nanopubParams = new HashMap<>();
				List<Map<String,String>> nanopubResults = new ArrayList<>();
				nanopubParams.put("pubkey", keyDeclarations.get(0).getPublicKeyString());  // TODO: only using first public key here
				nanopubParams.put("creator", userId);
				nanopubResults = ApiAccess.getRecent("find_signed_nanopubs", nanopubParams, progress);
				while (!nanopubResults.isEmpty() && nanopubs.size() < 10) {
					Map<String,String> resultEntry = nanopubResults.remove(0);
					String npUri = resultEntry.get("np");
					nanopubs.add(new NanopubElement(npUri, !resultEntry.get("retraction").isEmpty()));
				}
				nanopubsReady = true;
			}
		};
		loadContent.start();
	}

}
