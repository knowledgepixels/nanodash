package com.knowledgepixels.nanodash;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

import com.google.common.base.Charsets;

import net.trustyuri.TrustyUriUtils;

public class ExplorePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/explore";

	
	private final String ref;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ExplorePage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this));

		ref = parameters.get("id").toString();
		final String shortName = IriItem.getShortNameFromURI(ref);
		add(new Label("pagetitle", shortName + " (explore) | nanodash"));
		add(new Label("termname", shortName));
		add(new ExternalLink("urilink", ref, ref));

		WebMarkupContainer npStatusLine = new WebMarkupContainer("npstatusline");
		add(npStatusLine);

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", ref);
		try {
			Nanopub np = Utils.getAsNanopub(ref);
			if (np == null) {
				npStatusLine.setVisible(false);
				add(new Label("name", "Term"));
				add(new Label("nanopub", ""));
			} else {
				add(new Label("name", "Nanopublication"));
				add(new NanopubItem("nanopub", new NanopubElement(np)).expand());
				String url = "http://np.knowledgepixels.com/" + TrustyUriUtils.getArtifactCode(ref);
				npStatusLine.add(new ExternalLink("trig-html", url + ".html"));
				npStatusLine.add(new ExternalLink("trig-txt", url + ".trig.txt"));
				npStatusLine.add(new ExternalLink("jsonld-txt", url + ".jsonld.txt"));
				npStatusLine.add(new ExternalLink("nq-txt", url + ".nq.txt"));
				npStatusLine.add(new ExternalLink("xml-txt", url + ".xml.txt"));
				npStatusLine.add(new ExternalLink("trig", url + ".trig"));
				npStatusLine.add(new ExternalLink("jsonld", url + ".jsonld"));
				npStatusLine.add(new ExternalLink("nq", url + ".nq"));
				npStatusLine.add(new ExternalLink("xml", url + ".xml"));
			}

			add(new ExternalLink("show-references", ReferenceTablePage.MOUNT_PATH + "?id=" + URLEncoder.encode(ref, Charsets.UTF_8), "show references"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
