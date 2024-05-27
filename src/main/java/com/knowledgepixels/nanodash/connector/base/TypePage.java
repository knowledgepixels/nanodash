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
import com.knowledgepixels.nanodash.Template;
import com.knowledgepixels.nanodash.TemplateData;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.opencsv.exceptions.CsvValidationException;

public abstract class TypePage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	protected String type;
	protected NanodashSession session;

	public TypePage(final PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this, "connectors"));

		session = NanodashSession.get();

		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		type = getPageParameters().get("type").toString();

		String title = null;
		Template template = null;
		String exampleId = null;
		String prTemplateId = "http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ";
		String prTemplateOptions = null;

		String biodivPrTemplateOptions = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU " +
				"http://purl.org/np/RALfxe37qzq5rEk6tLLcsSeKtKQZ1FcoHZdf2nYVfU66w " +
				"http://purl.org/np/RAYfEAP8KAu9qhBkCtyq_hshOvTAJOcdfIvGhiGwUqB-M ";
				//"http://purl.org/np/RA-4AE_X28pW3dkmCNNP06QSnsxqMiduN7gX3HxSciX5k"

		final TemplateData td = TemplateData.get();

		if (type.equals("superpattern")) {
			title = "Fully-formal Advanced Statements (Super-Pattern)";
			template = td.getTemplate("http://purl.org/np/RAy3tITXPlULFLXxAxek39GLqLdqKkNM5hIzUOZxxjMmI");
			exampleId = "http://purl.org/np/RAu49Eu8w-jwQw6hZ2ZgYOYqmF9HpqxZgItA4oy4N8M-4";
		} else if (type.equals("linkflowsrel")) {
			title = "Simple Scientific Relations between Individuals";
			template = td.getTemplate("http://purl.org/np/RAsz-9JwiOPQufQ5AGSNepkPG0hkWWYutBDWtsMRgKaaU");
			exampleId = "http://purl.org/np/RAQjB6Dc9lrIxjnFNCXYa6rfDHo5e1WiPSu33WdCEWTNY";
		} else if (type.equals("crel")) {
			title = "Simple Scientific Relations based on Classes";
			template = td.getTemplate("http://purl.org/np/RAQ9wpGlnll9o034hWI9tGZw6oTBvcN9azFS7hzdESY0I");
			exampleId = "http://purl.org/np/RAkX1V_9VIscbvJ6Nz4BKUdgywO0UWgBQlzJUEvdMYG-M";
		} else if (type.equals("aida")) {
			title = "Semi-formal Statements Based on English Sentences (AIDA)";
			template = td.getTemplate("http://purl.org/np/RAdc8fxS-WgxHFUtTFWOKYJc1ICkDwBH11_f1sBZeQwBY");
			exampleId = "http://purl.org/np/RAa5RbYolIrUNlBoAUY5HUmGr-ci6G1pX6lWiNMkZMcYs";
		} else if (type.equals("classdef")) {
			title = "Class Definitions";
			template = td.getTemplate("http://purl.org/np/RAcWWF8kSXfwZ77XdC59IyH1MJ24wFp-dDeXkPjKHAXM0");
			exampleId = "http://purl.org/np/RAj26TjulpgBHXJGe0OwZZZ-cZCJ9WE7ICug5EW2tGH7s";
		} else if (type.equals("inddef")) {
			title = "Definitions of Individuals";
			template = td.getTemplate("http://purl.org/np/RAL9L_HSXsRpyC9KcxUGejL3qDiWF6Jeoihh09NYdCR7c");
			exampleId = "http://purl.org/np/RAJRFjNqKKBlxOhij8XnuqOYfjyjruF2jGZgLO2myu9O0";
		} else if (type.equals("ml")) {
			title = "Evaluation results of a Machine Learning experiment";
			template = td.getTemplate("http://purl.org/np/RAGEv4a9uT48mgnIlWqAvL-XnupfNYojIUmszJDa50xoc");
			exampleId = "http://purl.org/np/RAQV0grGVX22NUdKxBijorX0vy06-hdDY3BpScIeQzhx0";
		} else if (type.equals("biorel")) {
			title = "Simple Biological Relation";
			template = td.getTemplate("http://purl.org/np/RAEbbFIMF_kwp2rY1NqhhKHBiIXXL4_UgTC1hyd6l-cJs");
			exampleId = "http://purl.org/np/RASe3c2pjmN9TvAoxD_CZ-qBVhVMUk9dSMRHOOVxdXbqM";
		} else if (type.equals("taxontaxon")) {
			title = "Association between taxa";
			template = td.getTemplate("http://purl.org/np/RAh16oLqLJKo8I8R2CebR1n8Dwv95KL_H-azFfGt2FGW0");
			exampleId = "http://purl.org/np/RALX2suiPKea3pm65RjS97EL6k9iY1Jew_mb30hO5Zjv0";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("taxonenv")) {
			title = "Association between taxa and environments";
			template = td.getTemplate("http://purl.org/np/RAwy5ZloUugunk3gafYppW6MfZGQXD554XgHfCAFHH08k");
			exampleId = "http://purl.org/np/RATlLG_xH-woxWfvHYC-7LyV5F0V6Sq4EbDWS86h6CrUI";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("taxongene")) {
			title = "Association between taxa and genes";
			template = td.getTemplate("http://purl.org/np/RAQcn-NHXcqWvEsguTCnFspUqCBBbzQis2KafZf3IDMpk");
			exampleId = "http://purl.org/np/RArCW84nAUo14Z3GSoIppZKxoIpdJ0V12_ytDMsDTsVmk";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("orgorg")) {
			title = "Association between organisms";
			template = td.getTemplate("http://purl.org/np/RAMUbJ6gvqXzVP5-7VzaSW6CAu15OwJR5FtnC1ENqkZDI");
			exampleId = "http://purl.org/np/RAafGz8UV6Nz4m8wyCLV4eJ_bdNfXKpGie0FH6TQZFa84";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("orgenv")) {
			title = "Association between organisms and environments";
			template = td.getTemplate("http://purl.org/np/RA3eSKxteBFrsGXdQhllmJa71Od71rPd4wJ8ik7fIMWsc");
			exampleId = "http://purl.org/np/RAWxvypUU6OabNGlBz9jDGLnftqRXdokzI2oemf9vQRGY";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
		} else if (type.equals("taxonnames")) {
			title = "Association between taxon names (nomenclature)";
			template = td.getTemplate("http://purl.org/np/RAf9CyiP5zzCWN-J0Ts5k7IrZY52CagaIwM-zRSBmhrC8");
			exampleId = "http://purl.org/np/RAIz2ACUDvk3OAcXc-OjYSuLglUZu-fsJXrC4UtoAF7k4";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("orgns")) {
			title = "Association between organisms and nucleotide sequences";
			template = td.getTemplate("http://purl.org/np/RACPgaoRptG1W-IZpNk6r-MBSMgcdZlaMrAtli9GkmKaE");
			exampleId = "http://purl.org/np/RAyOMh3jJ2PWrgis-My3-QbN9yoBXVBulweANHdvJPhak";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("taxonns")) {
			title = "Association between taxa and nucleotide sequences";
			template = td.getTemplate("http://purl.org/np/RA1ooazhkXacK_3jibfpfdwqJWNO0yLoN1nrlpuHHi_uM");
			exampleId = "http://purl.org/np/RA5lyV7V98AAm6BM_s8gv17eRRFZsJqvp2TqCGV6QD3gk";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
		} else if (type.equals("biolinkrel")) {
			title = "Expressing a biological relation between two entities";
			template = td.getTemplate("http://purl.org/np/RAjvwGtEUz07hIGUZ_kHpW8R1TSeUhbWH8NdpC0MIvv_A");
			exampleId = "http://purl.org/np/RAPeBcr6fuTot6fU5bKyspkFD_5RX9tLXcCIs1UkEEUUo";
			prTemplateId = "http://purl.org/np/RAo6MkgozE1DB-3XwjvEO-sgaN02SmsAIEPbiE8xEdHGU";
			prTemplateOptions = biodivPrTemplateOptions;
		} else if (type.equals("eqrel")) {
			title = "Mapping two equivalent or related resource identifiers";
			template = td.getTemplate("http://purl.org/np/RAiBrF-il77MccafOpqR5pZgdlBFOreh9TAeKBga-Gxsk");
			exampleId = "http://purl.org/np/RAEdHUFvCt1jOsW14rU12X2n42iQS0IshN-j4syY2-IuI";
		} else if (type.equals("reaction")) {
			// This is still experimental and not yet used
			title = "A reaction or comment on a paper or nanopublication";
			template = td.getTemplate("http://purl.org/np/RANWGVogb5j_VQ6A4nabA34_-zkZTRYNYtItRJXGf2TVQ");
			exampleId = "http://purl.org/np/RAxjCU5pZDoGox98Hb36mquM9Bc1xv0qz4P19p2avnNPI";
			prTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";
		}

		add(new Label("pagetitle", title + " | nanodash"));
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

		add(new NanopubItem("example-nanopub", new NanopubElement(Utils.getAsNanopub(exampleId))).hidePubinfo().expand().noActions());

		if (userIri == null) {
			add(new WebMarkupContainer("candidate-nps")
				.add(new ExternalLink("nplink", session.getLoginUrl(getMountPath(), getPageParameters()))
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

		add(new BookmarkablePageLink<WebPage>("refresh-link", this.getClass(), getPageParameters()));

		String createNewMessage = "";
		if (type.equals("superpattern") || type.equals("crel") || type.equals("inddef")) {
			createNewMessage += "<p>If you don't find the class you need, <a href=\"" + getMountPath() + "?type=classdef\">create a new class</a> first.</p>";
		} else if (type.equals("linkflowsrel") || type.equals("ml")) {
			createNewMessage += "<p>If you don't find the individual you need, <a href=\"" + getMountPath() + "?type=inddef\">create a new individual</a> first.</p>";
		}

		String publishFormMessage = getConfig().getPublishFormMessage();
		if (publishFormMessage == null) {
			publishFormMessage = "<p><strong>Fill in the assertion (blue) part below, and then click \"Publish\" at the bottom " +
				"to publish a nanopublication that you can later link to your " + getConfig().getJournalName() + " submission</strong>.</p>" +
				"<p>To specify the URL of a preprint or source, you can switch to another provenance template (red). You can leave the pubinfo (yellow) part as it is.</p>";
		}

		String supportMessage = "<p>Open a <a href=\"mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() +
			"%20make-np]%20my%20problem/question&body=type%20your%20problem/question%20here\">support ticket</a> if you need help.</p>";

		PageParameters createNewParams = new PageParameters().add("template", template.getId()).add("template-version", "latest")
			.add("prtemplate", prTemplateId)
			.add("postpub-redirect-url", getConfig().getNanopubPage().getMountPath())
			.add("link-message",  publishFormMessage + createNewMessage + supportMessage);

		if (prTemplateOptions != null) {
			createNewParams.add("prtemplate-options", prTemplateOptions);
		}

		Class<? extends WebPage> publishPageClass = PublishPage.class;
		if (getConfig().getPublishPage() != null) publishPageClass = getConfig().getPublishPage().getClass();
		add(new BookmarkablePageLink<WebPage>("create-new-link", publishPageClass, createNewParams));
	}

}
