package org.petapico.nanobench.connector.ios;

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
import org.petapico.nanobench.ApiAccess;
import org.petapico.nanobench.ApiResponse;
import org.petapico.nanobench.ApiResponseEntry;
import org.petapico.nanobench.ExplorePage;
import org.petapico.nanobench.NanopubElement;
import org.petapico.nanobench.NanopubItem;
import org.petapico.nanobench.PublishPage;
import org.petapico.nanobench.Template;
import org.petapico.nanobench.TitleBar;
import org.petapico.nanobench.User;
import org.petapico.nanobench.Utils;

import net.trustyuri.TrustyUriUtils;

public class DsNanopubPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector-ios-ds-np";

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
		add(new NanopubItem("nanopub", new NanopubElement(np), false, true));
		String uri = np.getUri().stringValue();
		String shortId = "np:" + Utils.getShortNanopubId(uri);
		String artifactCode = TrustyUriUtils.getArtifactCode(uri);
		String reviewUri = "http://ds.kpxl.org/" + artifactCode;

		String navigationLinks = "|";
		if (mode.equals("author")) {
			navigationLinks += " <a href=\"/connector-ios-ds\">&lt; Back to Overview</a> |";
			navigationLinks += " <a href=\"" + MOUNT_PATH + "/" + artifactCode + "?mode=reviewer\">Switch to Reviewer View</a> |";
		} else if (mode.equals("reviewer")) {
			navigationLinks += " <a href=\"" + MOUNT_PATH + "/" + artifactCode + "?mode=author\">Switch to Author View</a> |";
		} else if (mode.equals("example")) {
			navigationLinks += " <a href=\"/connector-ios-ds\">&lt; Back to Overview</a> |";
		}
		add(new Label("navigation", "<p>" + navigationLinks + "</p>").setEscapeModelStrings(false));

		add(new WebMarkupContainer("author-instruction").setVisible(mode.equals("author")));
		add(new WebMarkupContainer("reviewer-instruction").setVisible(mode.equals("reviewer")));
		add(new WebMarkupContainer("example-message").setVisible(mode.equals("example")));

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
