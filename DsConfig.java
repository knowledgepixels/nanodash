package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class DsConfig extends ConnectorConfig {

	private static DsConfig instance;

	public static DsConfig get() {
		if (instance == null) instance = new DsConfig();
		return instance;
	}

	private static final DsOverviewPage overviewPageInstance = new DsOverviewPage(null);
	private static final DsTypePage typePageInstance = new DsTypePage(null);
	private static final DsNanopubPage nanopubPageInstance = new DsNanopubPage(null);

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
		return "DsLogo.png";
	}

	@Override
	public String getSubmitImageFileName() {
		return "DsFormSubmit.png";
	}

	@Override
	public String getApiUrl(String operation) {
		return "https://grlc.petapico.org/api-git/knowledgepixels/ds-nanopub-api/";
	}

	@Override
	public String getJournalAbbrev() {
		return "DS";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://ds.kpxl.org/";
	}

}
