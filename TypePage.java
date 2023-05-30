package com.knowledgepixels.nanodash.connector.base;

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
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NanopubItem;
import com.knowledgepixels.nanodash.PublishPage;
import com.knowledgepixels.nanodash.Template;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;
import com.opencsv.exceptions.CsvValidationException;

public abstract class TypePage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	protected String type;
	protected NanodashSession session;

	public TypePage(final PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar"));

		session = NanodashSession.get();

		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		type = getParams().get("type").toString();

		String title = null;
		Template template = null;
		String exampleId = null;
		String prTemplateId = "http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ";

		if (type.equals("superpattern")) {
			title = "Fully-formal Advanced Statements (Super-Pattern)";
			template = Template.getTemplate("http://purl.org/np/RAy3tITXPlULFLXxAxek39GLqLdqKkNM5hIzUOZxxjMmI");
			exampleId = "http://purl.org/np/RAu49Eu8w-jwQw6hZ2ZgYOYqmF9HpqxZgItA4oy4N8M-4";
		} else if (type.equals("linkflowsrel")) {
			title = "Simple Scientific Relations between Individuals";
			template = Template.getTemplate("http://purl.org/np/RAsz-9JwiOPQufQ5AGSNepkPG0hkWWYutBDWtsMRgKaaU");
			exampleId = "http://purl.org/np/RAQjB6Dc9lrIxjnFNCXYa6rfDHo5e1WiPSu33WdCEWTNY";
		} else if (type.equals("crel")) {
			title = "Simple Scientific Relations based on Classes";
			template = Template.getTemplate("http://purl.org/np/RAQ9wpGlnll9o034hWI9tGZw6oTBvcN9azFS7hzdESY0I");
			exampleId = "http://purl.org/np/RAkX1V_9VIscbvJ6Nz4BKUdgywO0UWgBQlzJUEvdMYG-M";
		} else if (type.equals("aida")) {
			title = "Semi-formal Statements Based on English Sentences (AIDA)";
			template = Template.getTemplate("http://purl.org/np/RAdc8fxS-WgxHFUtTFWOKYJc1ICkDwBH11_f1sBZeQwBY");
			exampleId = "http://purl.org/np/RAa5RbYolIrUNlBoAUY5HUmGr-ci6G1pX6lWiNMkZMcYs";
		} else if (type.equals("classdef")) {
			title = "Class Definitions";
			template = Template.getTemplate("http://purl.org/np/RAcWWF8kSXfwZ77XdC59IyH1MJ24wFp-dDeXkPjKHAXM0");
			exampleId = "http://purl.org/np/RAj26TjulpgBHXJGe0OwZZZ-cZCJ9WE7ICug5EW2tGH7s";
		} else if (type.equals("inddef")) {
			title = "Definitions of Individuals";
			template = Template.getTemplate("http://purl.org/np/RAL9L_HSXsRpyC9KcxUGejL3qDiWF6Jeoihh09NYdCR7c");
			exampleId = "http://purl.org/np/RAJRFjNqKKBlxOhij8XnuqOYfjyjruF2jGZgLO2myu9O0";
		} else if (type.equals("ml")) {
			title = "Evaluation results of a Machine Learning experiment";
			template = Template.getTemplate("http://purl.org/np/RAGEv4a9uT48mgnIlWqAvL-XnupfNYojIUmszJDa50xoc");
			exampleId = "http://purl.org/np/RAQV0grGVX22NUdKxBijorX0vy06-hdDY3BpScIeQzhx0";
		} else if (type.equals("biorel")) {
			title = "Simple Biological Relation";
			template = Template.getTemplate("http://purl.org/np/RAEbbFIMF_kwp2rY1NqhhKHBiIXXL4_UgTC1hyd6l-cJs");
			exampleId = "http://purl.org/np/RASe3c2pjmN9TvAoxD_CZ-qBVhVMUk9dSMRHOOVxdXbqM";
		} else if (type.equals("taxontaxon")) {
			title = "Biotic association between two biological taxa";
			template = Template.getTemplate("http://purl.org/np/RAfkBEHGmvTphfKcesMTreOcXTekxZmf3iO50SXoRBX48");
			exampleId = "http://purl.org/np/RAmlZqUxZndYaflGLxhspiGCdN6UNi4G2W7JQEiSApmFc";
		} else if (type.equals("taxonenv")) {
			title = "Association between biological species and environments";
			template = Template.getTemplate("http://purl.org/np/RARkaOvs0_ssbo7_yxtkvEcd6QvOwn0e6MUHdiNrYjFM0");
			exampleId = "http://purl.org/np/RAZbVnR5w1ui3i_VTM7cfljCyiywneCE_B7f6uDmiLV7c";
		} else if (type.equals("taxongene")) {
			title = "Association between an Organism Taxon and a gene";
			template = Template.getTemplate("http://purl.org/np/RAQcn-NHXcqWvEsguTCnFspUqCBBbzQis2KafZf3IDMpk");
			exampleId = "http://purl.org/np/RArCW84nAUo14Z3GSoIppZKxoIpdJ0V12_ytDMsDTsVmk";
		} else if (type.equals("reaction")) {
			// This is still experimental and not yet used
			title = "A reaction or comment on a paper or nanopublication";
			template = Template.getTemplate("http://purl.org/np/RANWGVogb5j_VQ6A4nabA34_-zkZTRYNYtItRJXGf2TVQ");
			exampleId = "http://purl.org/np/RAxjCU5pZDoGox98Hb36mquM9Bc1xv0qz4P19p2avnNPI";
			prTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";
		}

		add(new Label("title", title));

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
				.add(new ExternalLink("nplink", session.getLoginUrl(getMountPath(), getParams()))
					.add(new Label("nplinktext", "(login to see them)"))));
		} else {
			try {
				ApiResponse resp = callApi("get-" + type + "-nanopubs", params);
		
				add(new DataView<ApiResponseEntry>("candidate-nps", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
		
					private static final long serialVersionUID = 1L;
		
					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						item.add(new BookmarkablePageLink<WebPage>("nplink", getConfig().getNanopubPage().getClass(),
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
			createNewParagraph += "<p>If you don't find the class you need, <a href=\"" + getMountPath() + "?type=classdef\">create a new class</a> first.</p>";
		} else if (type.equals("linkflowsrel") || type.equals("ml")) {
			createNewParagraph += "<p>If you don't find the individual you need, <a href=\"" + getMountPath() + "?type=inddef\">create a new individual</a> first.</p>";
		}

		add(new BookmarkablePageLink<WebPage>("refresh-link", this.getClass(), getParams()));

		add(new BookmarkablePageLink<WebPage>("create-new-link", PublishPage.class,
				new PageParameters().add("template", template.getId()).add("template-version", "latest")
					.add("prtemplate", prTemplateId)
					.add("link-message", "<p><strong>Fill in the assertion (blue) part below, and then click \"Publish\" at the bottom " +
							"to publish a nanopublication that you can later link to your " + getConfig().getJournalName() + " submission</strong>.</p>" +
							"<p>You can leave the provenance (red) and publication info (yellow) parts as they are.</p>" +
							createNewParagraph +
							"<p>Open a <a href=\"mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() + "%20make-np]%20my%20problem/question&body=type%20your%20problem/question%20here\">support ticket</a> if you need help.</p>" +
							"<p>Close this tab to <strong>abort</strong> the nanopublication creation.</p>")
			));
	}

}
