package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class FcConfig extends ConnectorConfig {

	private static final long serialVersionUID = 1L;

	private static FcConfig instance;

	public static FcConfig get() {
		if (instance == null) instance = new FcConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/fc-nanopub-api/";

	private static final FcOverviewPage overviewPageInstance = new FcOverviewPage(null);
	private static final FcTypePage typePageInstance = new FcTypePage(null);
	private static final FcNanopubPage nanopubPageInstance = new FcNanopubPage(null);

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
		return "FcLogo.png";
	}

	@Override
	public String getSubmitImageFileName() {
		return "FcFormSubmit.png";
	}

	@Override
	public String getApiUrl(String operation) {
		if (isBaseApiOperation(operation)) return baseApi;
		return specificApi;
	}

	@Override
	public String getJournalName() {
		return "FAIR Connect";
	}

	@Override
	public String getJournalAbbrev() {
		return "FC";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://fc.kpxl.org/";
	}

}
