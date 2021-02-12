package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

public class UserPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private Model<String> progress;

	public UserPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final User user = User.getUser(parameters.get("id").toString());
		if (user == null) throw new RedirectToUrlException("./profile");
		add(new Label("username", user.getDisplayName()));

		// TODO: Progress bar doesn't update at the moment:
		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
		progressLabel.setOutputMarkupId(true);
		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)));
		add(progressLabel);

		add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

			private static final long serialVersionUID = 1L;

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				Map<String,String> nanopubParams = new HashMap<>();
				List<ApiResponseEntry> nanopubResults = new ArrayList<>();
				nanopubParams.put("pubkey", user.getPubkeyString());  // TODO: only using first public key here
				nanopubResults = ApiAccess.getRecent("find_signed_nanopubs", nanopubParams, progress).getData();
				List<NanopubElement> nanopubs = new ArrayList<>();
				while (!nanopubResults.isEmpty() && nanopubs.size() < 10) {
					ApiResponseEntry resultEntry = nanopubResults.remove(0);
					String npUri = resultEntry.get("np");
					// Hide retracted nanopublications:
					if (resultEntry.get("retracted").equals("1") || resultEntry.get("retracted").equals("true")) continue;
					// Hide superseded nanopublications:
					if (resultEntry.get("superseded").equals("1") || resultEntry.get("superseded").equals("true")) continue;
					nanopubs.add(new NanopubElement(npUri, false));
				}
				return new NanopubResults(markupId, nanopubs);
			}
		});

	}

	@Override
	public void onBeforeRender() {
		super.onBeforeRender();
		if (hasBeenRendered()) {
			setResponsePage(getPageClass(), getPageParameters());
		}
	}

}
