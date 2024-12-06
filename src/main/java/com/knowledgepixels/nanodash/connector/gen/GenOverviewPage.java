package com.knowledgepixels.nanodash.connector.gen;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;

public class GenOverviewPage extends OverviewPage {

	// TODO This page isn't linked yet, and only for testing so far.

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/gen";

	private ConnectorConfig config;

	public GenOverviewPage(PageParameters params) {
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
		init(params);
		add(new Label("pagetitle", config.getJournalName() + " | nanodash"));
		add(new Label("journal-name-title", config.getJournalName()));
		add(new ExternalLink("journal-link", config.getJournalUrl(), config.getJournalName()));
		add(new Label("extra-instructions", config.getExtraInstructions()).setEscapeModelStrings(false));

		if (getConfig().getGeneralReactionsApiCall() == null) {
			// TODO Fix this in OverviewPage code once refactoring is finished:
			add(new Label("reactions-component").setVisible(false));
		}
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

}
