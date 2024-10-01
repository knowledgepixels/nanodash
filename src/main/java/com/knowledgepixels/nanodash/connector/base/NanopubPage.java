package com.knowledgepixels.nanodash.connector.base;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.ReactionList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

import net.trustyuri.TrustyUriUtils;

public abstract class NanopubPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public NanopubPage(final PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef("Nanopublication")
			));

		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		String requestUrl = RequestCycle.get().getRequest().getUrl().toString();
		if (requestUrl.matches(".*/RA[A-Za-z0-9\\-_]{43}(\\?.*)?")) {
			throw new RedirectToUrlException(getMountPath() + "?" + Utils.getPageParametersAsString(new PageParameters(getPageParameters()).set("id", Utils.getArtifactCode(requestUrl))));
		}

		String mode = "author";
		if (!getPageParameters().get("mode").isEmpty()) {
			mode = getPageParameters().get("mode").toString();
		}
		String ref = getPageParameters().get("id").toString();

		if (mode.equals("head")) {
			Map<String,String> params = new HashMap<>();
			params.put("npid", ref);
			params.put("type", getConfig().getNanopubType().stringValue());
			ApiResponse resp = QueryApiAccess.get("get-publisher-version", params);
			if (resp != null && resp.getData().size() == 1) {
				ref = resp.getData().get(0).get("publisher_version_np");
				mode = "final";
			}
		}

		Nanopub np = Utils.getAsNanopub(ref);
		add(new NanopubItem("nanopub", NanopubElement.get(np)).addActions(NanopubAction.ownActions));
		String uri = np.getUri().stringValue();
		String shortId = "np:" + Utils.getShortNanopubId(uri);
		String artifactCode = TrustyUriUtils.getArtifactCode(uri);
		String reviewUri = getConfig().getReviewUrlPrefix() + artifactCode;

		add(new BookmarkablePageLink<Void>(
				"switch-to-reviewer-view",
				getConfig().getNanopubPage().getClass(),
				new PageParameters(getPageParameters()).set("mode", "reviewer")
			).setVisible(mode.equals("author")));
		add(new BookmarkablePageLink<Void>(
				"switch-to-author-view",
				getConfig().getNanopubPage().getClass(),
				new PageParameters(getPageParameters()).set("mode", "author")
			).setVisible(mode.equals("reviewer")));

		add(new WebMarkupContainer("author-instruction").setVisible(mode.equals("author")));
		add(new WebMarkupContainer("reviewer-instruction").setVisible(mode.equals("reviewer")));
		add(new WebMarkupContainer("candidate-instruction").setVisible(mode.equals("candidate")));

		if (mode.equals("reviewer") && getConfig().getTechnicalEditorIds().contains(NanodashSession.get().getUserIri())) {
			WebMarkupContainer technicalEditorActions = new WebMarkupContainer("technical-editor-actions");

			// TODO Store/handle general templates better:
			String templateId = "http://purl.org/np/RAFu2BNmgHrjOTJ8SKRnKaRp-VP8AOOb7xX88ob0DZRsU";
			if (TemplateData.get().getTemplateId(np) != null) {
				templateId = TemplateData.get().getTemplateId(np).stringValue();
			}

			technicalEditorActions.add(new BookmarkablePageLink<Void>("make-final-version", PublishPage.class,
					new PageParameters().add("template", templateId)
						.add("derive", np.getUri().stringValue())
						.add("template-version", "latest")
						.add("pitemplate1", "https://w3id.org/np/RA5R_qv3VsZIrDKd8Mr37x3HoKCsKkwN5tJVqgQsKhjTE")
						.add("piparam1_type", getConfig().getNanopubType() == null ? "" : getConfig().getNanopubType().stringValue())
						.add("pitemplate2", "https://w3id.org/np/RA_JdI7pfDcyvEXLr_gper3h8egmNggeTqkJbyHrlMEdo")
						.add("pitemplate3", "https://w3id.org/np/RAIabr2sRVJ-YOIwZRD__BVMJKnq3QtQw_mjLIGSACPAI")
						.add("target-namespace", getConfig().getTargetNamespace() == null ? "https://w3id.org/np/" : getConfig().getTargetNamespace())
				));
			add(technicalEditorActions);
		} else {
			add(new WebMarkupContainer("technical-editor-actions").setVisible(false));
		}

		String latest = QueryApiAccess.getLatestVersionId(np.getUri().stringValue());

		WebMarkupContainer newerVersionText = new WebMarkupContainer("newer-version");
		if (latest == null || latest.equals(np.getUri().stringValue())) {
			newerVersionText.add(new Label("newer-version-link"));
			newerVersionText.setVisible(false);
		} else {
			newerVersionText.add(new BookmarkablePageLink<Void>("newer-version-link", this.getClass(), new PageParameters(getPageParameters()).set("id", latest)));
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

			String queryName = "get-reactions";
			final HashMap<String,String> params = new HashMap<>();
			params.put("pub", uri);
			ApiResponse resp = callApi(queryName, params);
			String secondGenQueryId = getConfig().get2ndGenerationQueryId(queryName);
			if (secondGenQueryId != null) queryName = secondGenQueryId;
			if (resp != null) {
				add(new ReactionList("reactions", resp, np));
			} else {
				add(new ApiResultComponent("reactions", queryName, params) {

					private static final long serialVersionUID = 1L;

					@Override
					public Component getApiResultComponent(String markupId, ApiResponse response) {
						return new ReactionList(markupId, response, np);
					}

				});

			}

			add(new BookmarkablePageLink<Void>("refresh-link", this.getClass(), getPageParameters()));

			add(new BookmarkablePageLink<Void>("create-new-reaction", PublishPage.class,
					new PageParameters()
						.add("template", "http://purl.org/np/RAaRGtbno5qhDnHdw0Pae1CEyVmeqE5tuwAJ9bZTc4jaU")
						.add("param_paper", np.getUri().stringValue())
						.add("template-version", "latest")
						.add("link-message","Here you can publish a reaction to the given nanopublication by typing your text into the text field below and " +
								"choosing the relation that fits best (if unsure, you can choose 'cites as related').")
						.add("postpub-redirect-url", this.getMountPath() + "?" + Utils.getPageParametersAsString(parameters))
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
