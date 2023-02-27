package com.knowledgepixels.nanodash.connector.ios;

import java.io.IOException;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.eclipse.rdf4j.model.IRI;
import com.knowledgepixels.nanodash.ApiAccess;
import com.knowledgepixels.nanodash.ApiResponse;
import com.knowledgepixels.nanodash.ApiResponseEntry;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NanopubItem;
import com.knowledgepixels.nanodash.PublishPage;
import com.knowledgepixels.nanodash.Template;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;

import com.opencsv.exceptions.CsvValidationException;

public class DsTypePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/type";

	private String type;

	public DsTypePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final NanodashSession session = NanodashSession.get();

		add(new Image("logo", new PackageResourceReference(this.getClass(), "DsLogo.png")));

		type = parameters.get("type").toString();

		String title = null;
		Template template = null;
		String exampleId = null;

		if (type.equals("superpattern")) {
			title = "General Super-Pattern Statements";
			template = Template.getTemplate("http://purl.org/np/RA1-ZSpmVB-KaxzlapMMJisnXo-7jlFTxhSFWIckg5T88");
			exampleId = "http://purl.org/np/RAu49Eu8w-jwQw6hZ2ZgYOYqmF9HpqxZgItA4oy4N8M-4";
		} else if (type.equals("linkflowsrel")) {
			title = "Simple Scientific Relations between Instances";
			template = Template.getTemplate("http://purl.org/np/RAiiJzByX8P2RrumnJii04htCcwe0CKcq4skFo49c1P7s");
			exampleId = "http://purl.org/np/RAQjB6Dc9lrIxjnFNCXYa6rfDHo5e1WiPSu33WdCEWTNY";
		} else if (type.equals("crel")) {
			title = "Simple Scientific Relations based on Classes";
			template = Template.getTemplate("http://purl.org/np/RAGPUlHIdEpKsoL6Gi6Q2ZXxa6hGyScnmdPhPLbYNslJg");
			exampleId = "http://purl.org/np/RAzw-5B7jP0Qj5UVkljfM89mkj1jDQRSzT-DIKtoMtUAQ";
		} else if (type.equals("aida")) {
			title = "AIDA Sentence";
			template = Template.getTemplate("http://purl.org/np/RAK8Xbu3t63p6wMU2WGz8t4BX63cL0Tu2HBRvlxIDNCX8");
			exampleId = "http://purl.org/np/RA4lYxH4ISq9pMZEO_CQXvpmvlx_qtNK6-y_SLkvKH2z0";
		} else if (type.equals("classdef")) {
			title = "Class Definitions";
			template = Template.getTemplate("http://purl.org/np/RA2FrMIx0lsjlUje7iLpQb8kB0KuouQz5EpOaO5gdqwWI");
			exampleId = "http://purl.org/np/RA_is9jmGdOi9hhZhX7nuZum8YAl76jrsqVhOR5KvYvyw";
		} else if (type.equals("inddef")) {
			title = "Definitions of Individuals";
			template = Template.getTemplate("http://purl.org/np/RAL9L_HSXsRpyC9KcxUGejL3qDiWF6Jeoihh09NYdCR7c");
			exampleId = "http://purl.org/np/RAJRFjNqKKBlxOhij8XnuqOYfjyjruF2jGZgLO2myu9O0";
		}

		add(new Label("title", title));

		add(new Label("template-name", template.getLabel()));
		String description = "<p><em>(This template doesn't have a description)</em></p>";
		if (template.getDescription() != null) description = template.getDescription();
		add(new Label("template-description", description).setEscapeModelStrings(false));

		IRI userIri = session.getUserIri();
		Map<String,String> params = null;
		if (userIri != null) {
			params = new HashMap<>();
			params.put("creator", userIri.stringValue());
		}

		add(new NanopubItem("example-nanopub", new NanopubElement(Utils.getAsNanopub(exampleId)), false, true, NanopubAction.noActions));

		if (userIri == null) {
			add(new WebMarkupContainer("candidate-nps")
				.add(new ExternalLink("nplink", session.getLoginUrl(MOUNT_PATH, parameters))
					.add(new Label("nplinktext", "(login to see them)"))));
		} else {
			try {
				ApiResponse resp = ApiAccess.getAll(DsOverviewPage.apiUrl, "get-" + type + "-nanopubs", params);
		
				add(new DataView<ApiResponseEntry>("candidate-nps", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
		
					private static final long serialVersionUID = 1L;
		
					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						item.add(new BookmarkablePageLink<WebPage>("nplink", DsNanopubPage.class,
								new PageParameters().add("id", e.get("np")).add("type", type))
							.add(new Label("nplinktext", "\"" + e.get("label") + "\", " + e.get("date").substring(0, 10))));
					}
		
				});
			} catch (IOException|CsvValidationException ex) {
				// TODO Report error somehow...
				add(new Label("candidate-nps"));
			}
		}

		String createNewParagraph = "";
		if (type.equals("superpattern") || type.equals("crel") || type.equals("inddef")) {
			createNewParagraph += "<p>If you don't find the class you need, <a href=\"/" + DsTypePage.MOUNT_PATH + "?type=classdef\">create a new class</a> first.</p>";
		}

		add(new BookmarkablePageLink<WebPage>("refresh-link", DsTypePage.class, parameters));

		add(new BookmarkablePageLink<WebPage>("create-new-link", PublishPage.class,
				new PageParameters().add("template", template.getId()).add("template-version", "latest")
					.add("prtemplate", "http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ")
					.add("link-message", "<p>Fill in the assertion (blue) part below, and then click \"Publish\" at the bottom " +
							"to <strong>publish a nanopublication that you can later link to your Data Science submission</strong>.</p>" +
							"<p>You can leave the provenance (red) part as is, unless you have a pre-print URL that you want to link by choosing " +
							"\"From research described in an article (published/preprint)\".</p>" +
							"<p>The publication info (yellow) part can also be left untouched, but you are free to add further elements there.</p>" +
							createNewParagraph +
							"<p>Open a <a href=\"mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[DS%20make-np]%20my%20problem/question&body=type%20your%20problem/question%20here\">support ticket</a> if you need help.</p>" +
							"<p>Close this tab to <strong>abort</strong> the nanopublication creation.</p>")
			));
	}

}
