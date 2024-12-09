package com.knowledgepixels.nanodash.connector.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorOption;
import com.knowledgepixels.nanodash.connector.base.ConnectorOptionGroup;
import com.knowledgepixels.nanodash.connector.base.SelectPage;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;

public class GenSelectPage extends SelectPage {

	// TODO This page isn't linked yet, and only for testing so far.

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/gen/select";

	private ConnectorConfig config;
	private Form<?> form;
	private RadioGroup<String> radioGroup;

	public GenSelectPage(PageParameters params) {
		super(params, false);
		String journalId = params.get("journal").toString();
		if (journalId.equals("ios/ds")) {
			config = DsConfig.get();
		} else if (journalId.equals("pensoft/bdj")) {
			config = BdjConfig.get();
		} else if (journalId.equals("pensoft/rio")) {
			config = RioConfig.get();
		} else {
			throw new IllegalArgumentException("'journal' parameter not recognized");
		}
		add(new Label("pagetitle", config.getJournalName() + ": Create Nanopublication | nanodash"));
		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef("Create Nanopublication")
			));
		add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));


		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				ConnectorOption option = ConnectorOption.valueOf(radioGroup.getModelObject());
				PageParameters params = new PageParameters();
				params.add("type", option.name().toLowerCase());
				params.add("template", option.getTemplateId());
				params.add("prtemplate", option.getPrTemplateId());
				params.add("pitemplate1", "https://w3id.org/np/RA16U9Wo30ObhrK1NzH7EsmVRiRtvEuEA_Dfc-u8WkUCA");  // Author list
				throw new RestartResponseException(getConfig().getPublishPage().getClass(), params);
			}

		};
		form.setOutputMarkupId(true);

		radioGroup = new RadioGroup<>("radio-group", new Model<String>(options.get(0).getOptions()[0].name()));
		radioGroup.setOutputMarkupId(true);
		radioGroup.add(new ListView<ConnectorOptionGroup>("option-group", options) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ConnectorOptionGroup> item) {
				ConnectorOptionGroup g = item.getModelObject();
				item.add(new Label("option-group-title", g.getTitle()));
				item.add(new ListView<>("option-container", Arrays.asList(g.getOptions())) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<ConnectorOption> item) {
						ConnectorOption o = item.getModelObject();
						WebMarkupContainer c = new WebMarkupContainer("option");
						Radio<String> radio = new Radio<String>("option-input", new Model<String>(o.name()), radioGroup);
						radio.setOutputMarkupId(true);
						radioGroup.add(radio);
						c.add(radio);
						c.add(new Label("option-title", o.getName()));
						c.add(new Label("option-explanation", o.getExplanation()).setEscapeModelStrings(false).setVisible(o.getExplanation() != null));
						item.add(c);
					}

				});
			}

		});
		form.add(radioGroup);
		add(form);
		add(new ExternalLink("type-support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + config.getJournalAbbrev() + "%20type]%20my%20problem/question&body=type%20your%20problem/question%20here"));
		add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + config.getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return config;
	}

	@Override
	protected String[] getOptions() {
		return new String[] {};
	}


	// TODO This is just for testing so far. This page uses always the BDJ options.

	private static List<ConnectorOptionGroup> options;

	static {
		options = new ArrayList<>();
		options.add(new ConnectorOptionGroup("Biodiversity Associations",
				ConnectorOption.SPECTAXON,
				ConnectorOption.ORGORG,
				ConnectorOption.TAXONTAXON,
				ConnectorOption.TAXONENV,
				ConnectorOption.ORGENV,
				ConnectorOption.TAXONNAMES,
				ConnectorOption.ORGNS,
				ConnectorOption.TAXONNS
			));
		options.add(new ConnectorOptionGroup("General Links",
				ConnectorOption.SPECPUB,
				ConnectorOption.BIOLINKREL,
				ConnectorOption.EQREL
			));
		options.add(new ConnectorOptionGroup("Definitions",
				ConnectorOption.TAXONDEF,
				ConnectorOption.CLASSDEF,
				ConnectorOption.INDDEF
			));
	}

}
