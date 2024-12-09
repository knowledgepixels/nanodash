package com.knowledgepixels.nanodash.connector.gen;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorOption;
import com.knowledgepixels.nanodash.connector.base.ConnectorPublishPage;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;

public class GenPublishPage extends ConnectorPublishPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/gen/publish";

	private ConnectorConfig config;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GenPublishPage(final PageParameters parameters) {
		super(parameters, false);
		String journalId = parameters.get("journal").toString();
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
				new NanodashPageRef(getConfig().getSelectPage().getClass(), "Create Nanopublication"),
				new NanodashPageRef("Publish")
			));
		add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));

		if (parameters.get("template").toString() != null) {
			parameters.add("template-version", "latest");
			add(new PublishForm("form", parameters, getConfig().getPublishPage().getClass(), getConfig().getConnectPage().getClass()));
		} else {
			throw new RuntimeException("no template parameter");
		}

		PageParameters pageParams = new PageParameters();
		ConnectorOption option = ConnectorOption.valueOf(parameters.get("type").toString().toUpperCase());
		pageParams.add("id", option.getExampleId());
		pageParams.add("mode", "final");
		add(new BookmarkablePageLink<Void>("show-example", getConfig().getNanopubPage().getClass(), pageParams));
		add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + config.getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
	}

	@Override
	protected ConnectorConfig getConfig() {
		return config;
	}

}
