package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class BdjConfig extends ConnectorConfig {

	private static BdjConfig instance;

	public static BdjConfig get() {
		if (instance == null) instance = new BdjConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.petapico.org/api-git/knowledgepixels/bdj-nanopub-api/";

	private static final BdjOverviewPage overviewPageInstance = new BdjOverviewPage(null);
	private static final BdjTypePage typePageInstance = new BdjTypePage(null);
	private static final BdjNanopubPage nanopubPageInstance = new BdjNanopubPage(null);

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
		return "BdjLogo.svg";
	}

	@Override
	public String getSubmitImageFileName() {
		return "RioFormSubmit.png";
	}

	@Override
	public String getApiUrl(String operation) {
		if (isBaseApiOperation(operation)) return baseApi;
		return specificApi;
	}

	@Override
	public String getJournalName() {
		return "Biodiversity Data Journal";
	}

	@Override
	public String getJournalAbbrev() {
		return "BDJ";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://bdj.kpxl.org/";
	}

}
