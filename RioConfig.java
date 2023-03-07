package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class RioConfig extends ConnectorConfig {

	private static RioConfig instance;

	public static RioConfig get() {
		if (instance == null) instance = new RioConfig();
		return instance;
	}

	private static final RioOverviewPage overviewPageInstance = new RioOverviewPage(null);
	private static final RioTypePage typePageInstance = new RioTypePage(null);
	private static final RioNanopubPage nanopubPageInstance = new RioNanopubPage(null);

	@Override
	public OverviewPage getOverviewPage() {
		return overviewPageInstance;
	}

	@Override
	public TypePage getTypePage() {
		return typePageInstance;
	}

	@Override
	public NanopubPage getNanopubPage() {
		return nanopubPageInstance;
	}

	@Override
	public String getLogoFileName() {
		return "RioLogo.svg";
	}

	@Override
	public String getSubmitImageFileName() {
		return "RioFormSubmit.png";
	}

	@Override
	public String getApiUrl(String operation) {
		return "https://grlc.petapico.org/api-git/knowledgepixels/connector-nanopub-api/";
	}

	@Override
	public String getJournalName() {
		return "RIO Journal";
	}

	@Override
	public String getJournalAbbrev() {
		return "RIO";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://rio.kpxl.org/";
	}

}
