package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPage;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class DsConfig extends ConnectorConfig {

	private static final DsConfig instance = new DsConfig();

	public static DsConfig get() {
		return instance;
	}

	private static final DsOverviewPage overviewPageInstance = new DsOverviewPage();
	private static final DsTypePage typePageInstance = new DsTypePage();
	private static final DsNanopubPage nanopubPageInstance = new DsNanopubPage();

	@Override
	public ConnectorPage getOverviewPage() {
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
	public String getApiUrl() {
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
