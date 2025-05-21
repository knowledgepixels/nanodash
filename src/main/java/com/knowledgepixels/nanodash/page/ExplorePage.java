package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.commonjava.mimeparse.MIMEParse;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ExploreDataTable;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.StatusLine;
import com.knowledgepixels.nanodash.component.ThingListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;

import jakarta.servlet.http.HttpServletRequest;
import net.trustyuri.TrustyUriUtils;

public class ExplorePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/explore";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ExplorePage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));

		String tempRef = parameters.get("id").toString();

		WebMarkupContainer raw = new WebMarkupContainer("raw");
		add(raw);

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", tempRef);
		Nanopub np = Utils.getAsNanopub(tempRef);
		boolean isNanopubId = (np != null);
		if (isNanopubId) {
			tempRef = np.getUri().stringValue();
		}
		if (!isNanopubId && tempRef.matches("^.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}[^A-Za-z0-9-_].*$")) {
			String npId = tempRef.replaceFirst("(^.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
			Map<String,String> params = new HashMap<>();
			params.put("thing", tempRef);
			params.put("np", npId);
			ApiResponse resp = QueryApiAccess.forcedGet("get-latest-thing-nanopub", params);
			if (!resp.getData().isEmpty()) {
				// TODO We take the most recent in case more than one latest version exists. Make other latest versions visible too.
				npId = resp.getData().get(0).get("latestVersion");
			}
			np = Utils.getAsNanopub(npId);
		}
		if (np == null) {
			raw.setVisible(false);
			add(new Label("nanopub-header", ""));
			add(new Label("nanopub", ""));
			add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
		} else {

			// Check whether we should redirect to Nanopub Registry for machine-friendly formats:
			String mimeType = Utils.TYPE_HTML;
			try {
				HttpServletRequest httpRequest = (HttpServletRequest) getRequest().getContainerRequest();
				mimeType = MIMEParse.bestMatch(Utils.SUPPORTED_TYPES_LIST, httpRequest.getHeader("Accept"));
			} catch (Exception ex) {}
			if (!mimeType.equals(Utils.TYPE_HTML)) {
				System.err.println("Non-HTML content type: " + mimeType);
				// TODO Make this registry URL configurable/dynamic:
				String redirectUrl = "https://registry.knowledgepixels.com/np/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
				System.err.println("Redirecting to: " + redirectUrl);
				throw new RedirectToUrlException(redirectUrl, 302);
			}

			if (isNanopubId) {
				add(new Label("nanopub-header", "<h4>Nanopublication</h4>").setEscapeModelStrings(false));
			} else {
				add(new Label("nanopub-header", "<h4>Minted in Nanopublication</h4>").setEscapeModelStrings(false));
			}
			add(new NanopubItem("nanopub", NanopubElement.get(np)));
			String url = "http://np.knowledgepixels.com/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
			raw.add(new ExternalLink("trig-txt", url + ".trig.txt"));
			raw.add(new ExternalLink("jsonld-txt", url + ".jsonld.txt"));
			raw.add(new ExternalLink("nq-txt", url + ".nq.txt"));
			raw.add(new ExternalLink("xml-txt", url + ".xml.txt"));
			raw.add(new ExternalLink("trig", url + ".trig"));
			raw.add(new ExternalLink("jsonld", url + ".jsonld"));
			raw.add(new ExternalLink("nq", url + ".nq"));
			raw.add(new ExternalLink("xml", url + ".xml"));
			if (Utils.isNanopubOfClass(np, Template.ASSERTION_TEMPLATE_CLASS)) {
				add(new WebMarkupContainer("use-template").add(
						new BookmarkablePageLink<Void>("template-link", PublishPage.class, new PageParameters().add("template", np.getUri())))
					);
			} else {
				add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
			}
		}

		final String ref = tempRef;
		final String shortName;
		if (parameters.get("label").isEmpty()) {
			shortName = IriItem.getShortNameFromURI(ref);
		} else {
			shortName = parameters.get("label").toString();
		}
		add(new Label("pagetitle", shortName + " (explore) | nanodash"));
		add(new Label("termname", shortName));
		add(new ExternalLink("urilink", ref, ref));
		if (isNanopubId && SignatureUtils.seemsToHaveSignature(np)) {
			add(StatusLine.createComponent("statusline", ref));
		} else {
			add(new Label("statusline").setVisible(false));
		}
		add(ThingListPanel.createComponent("classes-panel", ThingListPanel.Mode.CLASSES, ref, "<em>Searching for classes...</em>", 10));
		if (isNanopubId) {
			add(new Label("instances-panel").setVisible(false));
			add(new Label("templates-panel").setVisible(false));
		} else {
			add(ThingListPanel.createComponent("instances-panel", ThingListPanel.Mode.INSTANCES, ref, "<em>Searching for instances...</em>", 10));
			add(ThingListPanel.createComponent("templates-panel", ThingListPanel.Mode.TEMPLATES, ref, "<em>Searching for templates...</em>", 10));
		}
		add(ExploreDataTable.createComponent("reftable", ref, 10));
	}

}
