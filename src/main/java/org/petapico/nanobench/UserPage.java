package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class UserPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private Model<String> progress;
	private Model<String> selected = new Model<>();

	public UserPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final User user = User.getUser(parameters.get("id").toString());
		if (user == null) throw new RedirectToUrlException("./profile");
		add(new Label("username", user.getDisplayName()));

		// TODO: Progress bar doesn't update at the moment:
		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
//		progressLabel.setOutputMarkupId(true);
//		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)));
		add(progressLabel);

		ArrayList<String> pubKeyList = new ArrayList<>();
		pubKeyList.add(user.getPubkeyString().replaceFirst("^(.).{39}(.{10}).*$", "$1..$2.."));

		RadioChoice<String> pubkeySelection = new RadioChoice<String>("pubkeygroup", selected, pubKeyList);
		pubkeySelection.setDefaultModelObject(pubKeyList.get(0));
		pubkeySelection.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			private static final long serialVersionUID = -6398658082085108029L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				// TODO: implement this
				System.err.println("PUBKEY SELECTED: " + selected.getObject());
			}

		});
		add(pubkeySelection);
		add(new Label("pubkey", Utils.getShortPubkeyLabel(user.getPubkeyString())));

		add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

			private static final long serialVersionUID = 1L;

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				Map<String,String> nanopubParams = new HashMap<>();
				List<ApiResponseEntry> nanopubResults = new ArrayList<>();
				nanopubParams.put("pubkey", user.getPubkeyString());  // TODO: only using first public key here
				nanopubResults = ApiAccess.getRecent("find_signed_nanopubs", nanopubParams, progress).getData();
				List<NanopubElement> nanopubs = new ArrayList<>();
				while (!nanopubResults.isEmpty() && nanopubs.size() < 100) {
					ApiResponseEntry resultEntry = nanopubResults.remove(0);
					String npUri = resultEntry.get("np");
					// Hide retracted nanopublications:
					if (resultEntry.get("retracted").equals("1") || resultEntry.get("retracted").equals("true")) continue;
					// Hide superseded nanopublications:
					if (resultEntry.get("superseded").equals("1") || resultEntry.get("superseded").equals("true")) continue;
					nanopubs.add(new NanopubElement(npUri, false));
				}
				NanopubResults r = new NanopubResults(markupId, nanopubs);
				progress.setObject("");
				return r;
			}
		});

	}

//	@Override
//	public void onBeforeRender() {
//		super.onBeforeRender();
//		if (hasBeenRendered()) {
//			setResponsePage(getPageClass(), getPageParameters());
//		}
//	}

}
