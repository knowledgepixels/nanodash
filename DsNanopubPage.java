package com.knowledgepixels.nanodash.connector.ios;

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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;
import com.knowledgepixels.nanodash.ApiAccess;
import com.knowledgepixels.nanodash.ApiResponse;
import com.knowledgepixels.nanodash.ApiResponseEntry;
import com.knowledgepixels.nanodash.ExplorePage;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NanopubItem;
import com.knowledgepixels.nanodash.PublishPage;
import com.knowledgepixels.nanodash.Template;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;

import net.trustyuri.TrustyUriUtils;

public class DsNanopubPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/np";

	public DsNanopubPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		add(new Image("logo", new PackageResourceReference(this.getClass(), "DsLogo.png")));

		String mode = "author";
		if (!parameters.get("mode").isEmpty()) {
			mode = parameters.get("mode").toString();
		}

		String ref = null;
		String requestUrl = RequestCycle.get().getRequest().getUrl().toString();
		if (requestUrl.matches(MOUNT_PATH.substring(1) + "/RA[A-Za-z0-9\\-_]{43,}(\\?.*)?")) {
			// TODO: Don't assume purl.org namespace here:
			ref = "http://purl.org/np/" + requestUrl.replaceFirst("^" + MOUNT_PATH.substring(1) + "/(RA[A-Za-z0-9\\-_]{43,})(\\?.*)?.", "$1");
		}
		if (ref == null) {
			ref = parameters.get("id").toString();
		}

		Nanopub np = Utils.getAsNanopub(ref);
		add(new NanopubItem("nanopub", new NanopubElement(np), false, true, NanopubAction.ownActions));
		String uri = np.getUri().stringValue();
		String shortId = "np:" + Utils.getShortNanopubId(uri);
		String artifactCode = TrustyUriUtils.getArtifactCode(uri);
		String reviewUri = "http://ds.kpxl.org/" + artifactCode;

		String backLink = " <a href=\"" + DsOverviewPage.MOUNT_PATH + "\">&lt; Back to Overview</a> |";
		String typeParam = "";
		if (!parameters.get("type").isEmpty()) {
			String type = parameters.get("type").toString();
			backLink = " <a href=\"" + DsTypePage.MOUNT_PATH + "?type=" + type + "\">&lt; Back to Type Overview</a> |";
			typeParam = "&type=" + type;
		}

		String navigationLinks = "|";
		if (mode.equals("author")) {
			navigationLinks += backLink;
			navigationLinks += " <a href=\"" + MOUNT_PATH + "/" + artifactCode + "?mode=reviewer" + typeParam + "\">Switch to Reviewer View</a> |";
		} else if (mode.equals("reviewer")) {
			navigationLinks += " <a href=\"" + MOUNT_PATH + "/" + artifactCode + "?mode=author" + typeParam + "\">Switch to Author View</a> |";
//		} else if (mode.equals("example")) {
//			navigationLinks += backLink;
		}
		add(new Label("navigation", "<p>" + navigationLinks + "</p>").setEscapeModelStrings(false));

		add(new WebMarkupContainer("author-instruction").setVisible(mode.equals("author")));
		add(new WebMarkupContainer("reviewer-instruction").setVisible(mode.equals("reviewer")));
//		add(new WebMarkupContainer("example-message").setVisible(mode.equals("example")));

		try {

			Template template = Template.getTemplate(np);
			if (template == null) {
				add(new Label("template-name", "(none)"));
				add(new Label("template-description", ""));
			} else {
				add(new Label("template-name", template.getLabel()));
				String description = "<p><em>(This template doesn't have a description)</em></p>";
				if (template.getDescription() != null) description = template.getDescription();
				add(new Label("template-description", description).setEscapeModelStrings(false));
			}

			Map<String,String> params = new HashMap<>();
			params.put("pub", uri);
			ApiResponse resp = ApiAccess.getAll(FcOverviewPage.apiUrl, "get-reactions", params);
	
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
					params.add("id", e.get("np"));
					BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("reaction", ExplorePage.class, params);
					String username = User.getShortDisplayNameForPubkey(e.get("pubkey"));
					l.add(new Label("reactiontext", "\"" + e.get("text") + "\" (" + e.get("reltext") + ") by " + username + " on " + e.get("date").substring(0, 10)));
					item.add(l);
				}
	
			});

			add(new BookmarkablePageLink<WebPage>("refresh-link", DsNanopubPage.class, parameters));

			add(new BookmarkablePageLink<WebPage>("create-new-reaction", PublishPage.class,
					new PageParameters()
						.add("template", "http://purl.org/np/RA4qeqqwcQGKQX9AgSd_3nNzECBYsohceseJ5FdFU_kjQ")
						.add("param_paper", np.getUri().stringValue())
						.add("template-version", "latest")));

			WebMarkupContainer submissionPart = new WebMarkupContainer("submissionpart");
			submissionPart.add(new Image("form-submit", new PackageResourceReference(this.getClass(), "DsFormSubmit.png")));
			submissionPart.add(new ExternalLink("np-link", reviewUri, reviewUri));
			add(submissionPart.setVisible(mode.equals("author")));

			WebMarkupContainer mentionPart = new WebMarkupContainer("mentionpart");
			mentionPart.add(new ExternalLink("word-np-link", reviewUri, shortId));
			mentionPart.add(new Label("latex-np-uri", reviewUri));
			mentionPart.add(new Label("latex-np-label", shortId.replace("_", "\\_")));
			add(mentionPart.setVisible(mode.equals("author")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
