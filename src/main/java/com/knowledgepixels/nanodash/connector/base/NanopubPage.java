package com.knowledgepixels.nanodash.connector.base;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.ExplorePage;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NanopubItem;
import com.knowledgepixels.nanodash.PublishPage;
import com.knowledgepixels.nanodash.Template;
import com.knowledgepixels.nanodash.TemplateData;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;

import net.trustyuri.TrustyUriUtils;

public abstract class NanopubPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public NanopubPage(final PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this));

		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		String mode = "author";
		if (!getPageParameters().get("mode").isEmpty()) {
			mode = getPageParameters().get("mode").toString();
		}

		String requestUrl = RequestCycle.get().getRequest().getUrl().toString();
		if (requestUrl.matches(".*/RA[A-Za-z0-9\\-_]{43}(\\?.*)?")) {
			throw new RedirectToUrlException(getMountPath() + "?" + Utils.getPageParametersAsString(new PageParameters(getPageParameters()).set("id", Utils.getArtifactCode(requestUrl))));
		}

		String ref = getPageParameters().get("id").toString();
		Nanopub np = Utils.getAsNanopub(ref);
		add(new NanopubItem("nanopub", new NanopubElement(np)).hidePubinfo().expand().addActions(NanopubAction.ownActions));
		String uri = np.getUri().stringValue();
		String shortId = "np:" + Utils.getShortNanopubId(uri);
		String artifactCode = TrustyUriUtils.getArtifactCode(uri);
		String reviewUri = getConfig().getReviewUrlPrefix() + artifactCode;

		String backLink = " <a class=\"button light\" href=\"" + getConfig().getOverviewPage().getMountPath() + "\">&lt; Back to Overview</a> ";
		if (!getPageParameters().get("type").isEmpty()) {
			String type = getPageParameters().get("type").toString();
			backLink = " <a class=\"button light\" href=\"" + getConfig().getTypePage().getMountPath() + "?type=" + type + "\">&lt; Back to Type</a> ";
		}

		String navigationLinks = "";
		if (mode.equals("author")) {
			navigationLinks += backLink;
			navigationLinks += " <a class=\"button light\" href=\"" + getMountPath() + "?" + Utils.getPageParametersAsString(new PageParameters(getPageParameters()).set("mode", "reviewer")) + "\">Switch to Reviewer View</a> ";
		} else if (mode.equals("reviewer")) {
			navigationLinks += " <a class=\"button light\" href=\"" + getMountPath() + "?" + Utils.getPageParametersAsString(new PageParameters(getPageParameters()).set("mode", "author")) + "\">Switch to Author View</a> ";
		} else if (mode.equals("final") || mode.equals("candidate")) {
			navigationLinks = "";
		}
		add(new Label("navigation", "<p class=\"center\">" + navigationLinks + "</p>").setEscapeModelStrings(false));

		add(new WebMarkupContainer("author-instruction").setVisible(mode.equals("author")));
		add(new WebMarkupContainer("reviewer-instruction").setVisible(mode.equals("reviewer")));
		add(new WebMarkupContainer("candidate-instruction").setVisible(mode.equals("candidate")));

		HashMap<String,String> newerVersionParams = new HashMap<>();
		newerVersionParams.put("np", np.getUri().stringValue());
		String latest = null;
		try {
			ApiResponse newerVersionResponse = ApiAccess.getAll(ApiAccess.MAIN_GRLC_API_GENERIC_URL, "get_latest_version", newerVersionParams);
			if (newerVersionResponse.getData().size() == 1) latest = newerVersionResponse.getData().get(0).get("latest");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		WebMarkupContainer newerVersionText = new WebMarkupContainer("newer-version");
		if (latest == null || latest.equals(np.getUri().stringValue())) {
			newerVersionText.add(new Label("newer-version-link"));
			newerVersionText.setVisible(false);
		} else {
			newerVersionText.add(new BookmarkablePageLink<WebPage>("newer-version-link", this.getClass(), new PageParameters(getPageParameters()).set("id", latest)));
		}
		add(newerVersionText);

		try {

			Template template = TemplateData.get().getTemplate(np);
			if (template == null) {
				add(new Label("template-description", ""));
			} else {
				String description = "<p><em>(This template doesn't have a description)</em></p>";
				if (template.getDescription() != null) description = template.getDescription();
				add(new Label("template-description", description).setEscapeModelStrings(false));
			}

			Map<String,String> params = new HashMap<>();
			params.put("pub", uri);
			ApiResponse resp = callApi("get-reactions", params);
	
			add(new DataView<ApiResponseEntry>("reactions", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				protected void populateItem(Item<ApiResponseEntry> item) {
					ApiResponseEntry e = item.getModelObject();
					PageParameters params = new PageParameters();
					if (e.get("pub").equals(np.getUri().stringValue())) {
						item.add(new Label("reactionnote"));
					} else {
						item.add(new Label("reactionnote", "On earlier version:"));
					}
					item.add(new Label("reactiontext", "\"" + e.get("text") + "\" (" + e.get("reltext") + " the nanopublication above)"));
					params.add("id", e.get("np"));
					BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("reactionlink", ExplorePage.class, params);
					String username = User.getShortDisplayName(null, e.get("pubkey"));
					l.add(new Label("reactionlinktext", "by " + username + " on " + e.get("date").substring(0, 10)));
					item.add(l);
				}
	
			});

			add(new BookmarkablePageLink<WebPage>("refresh-link", this.getClass(), getPageParameters()));

			add(new BookmarkablePageLink<WebPage>("create-new-reaction", PublishPage.class,
					new PageParameters()
						.add("template", "http://purl.org/np/RAaRGtbno5qhDnHdw0Pae1CEyVmeqE5tuwAJ9bZTc4jaU")
						.add("param_paper", np.getUri().stringValue())
						.add("template-version", "latest")
						.add("link-message","Here you can publish a reaction to the given nanopublication by typing your text into the text field below and " +
								"choosing the relation that fits best (if unsure, you can choose 'cites as related').")
					)
				);

			WebMarkupContainer inclusionPart = new WebMarkupContainer("includeinstruction");
			inclusionPart.add(new Image("form-submit", new PackageResourceReference(this.getClass(), getConfig().getSubmitImageFileName())));
			inclusionPart.add(new ExternalLink("np-link", reviewUri, reviewUri));
			inclusionPart.add(new ExternalLink("word-np-link", reviewUri, shortId));
			inclusionPart.add(new Label("latex-np-uri", reviewUri));
			inclusionPart.add(new Label("latex-np-label", shortId.replace("_", "\\_")));
			add(inclusionPart.setVisible(mode.equals("author")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
