package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.SignatureUtils;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ExploreDataTable;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.ThingListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;

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

		WebMarkupContainer npStatusLine = new WebMarkupContainer("npstatusline");
		add(npStatusLine);

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", tempRef);
		Nanopub np = Utils.getAsNanopub(tempRef);
		if (np == null) {
			npStatusLine.setVisible(false);
			add(new Label("name", ""));
			add(new Label("nanopub", ""));
			add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
		} else {
			tempRef = np.getUri().stringValue();
			add(new Label("name", "Nanopublication:"));
			add(new NanopubItem("nanopub", NanopubElement.get(np)));
			String url = "http://np.knowledgepixels.com/" + TrustyUriUtils.getArtifactCode(tempRef);
			npStatusLine.add(new ExternalLink("trig-txt", url + ".trig.txt"));
			npStatusLine.add(new ExternalLink("jsonld-txt", url + ".jsonld.txt"));
			npStatusLine.add(new ExternalLink("nq-txt", url + ".nq.txt"));
			npStatusLine.add(new ExternalLink("xml-txt", url + ".xml.txt"));
			npStatusLine.add(new ExternalLink("trig", url + ".trig"));
			npStatusLine.add(new ExternalLink("jsonld", url + ".jsonld"));
			npStatusLine.add(new ExternalLink("nq", url + ".nq"));
			npStatusLine.add(new ExternalLink("xml", url + ".xml"));
			if (Utils.isNanopubOfClass(np, Template.ASSERTION_TEMPLATE_CLASS)) {
				add(new WebMarkupContainer("use-template").add(
						new BookmarkablePageLink<Void>("template-link", PublishPage.class, new PageParameters().add("template", np.getUri())))
					);
			} else {
				add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
			}
			if (SignatureUtils.seemsToHaveSignature(np)) {
				npStatusLine.add(new WebMarkupContainer("updateline"));
			} else {
				npStatusLine.add(new WebMarkupContainer("updateline").setVisible(false));
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
		if (np != null) {
			add(new Label("classes-panel").setVisible(false));
			add(new Label("instances-panel").setVisible(false));
			add(new Label("templates-panel").setVisible(false));
		} else {
			add(ThingListPanel.createComponent("classes-panel", ThingListPanel.Mode.CLASSES, ref, "<em>Searching for classes...</em>", 10));
			add(ThingListPanel.createComponent("instances-panel", ThingListPanel.Mode.INSTANCES, ref, "<em>Searching for instances...</em>", 10));
			add(ThingListPanel.createComponent("templates-panel", ThingListPanel.Mode.TEMPLATES, ref, "<em>Searching for templates...</em>", 10));
		}
		add(ExploreDataTable.createComponent("reftable", ref, 10));
	}

}
