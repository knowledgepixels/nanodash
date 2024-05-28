package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.base.ConnectPage;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPublishPage;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.SelectPage;

public class BdjConfig extends ConnectorConfig {

	private static final long serialVersionUID = 1L;

	private static BdjConfig instance;

	public static BdjConfig get() {
		if (instance == null) instance = new BdjConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/bdj-nanopub-api/";

	private static final BdjOverviewPage overviewPageInstance = new BdjOverviewPage(null);
	private static final BdjSelectPage selectPageInstance = new BdjSelectPage(null);
	private static final BdjPublishPage publishPageInstance = new BdjPublishPage(null);
	private static final BdjConnectPage connectPageInstance = new BdjConnectPage(null, null);
	private static final BdjNanopubPage nanopubPageInstance = new BdjNanopubPage(null);

	@Override
	public OverviewPage getOverviewPage() {
		return overviewPageInstance;
	}

	@Override
	public SelectPage getSelectPage() {
		return selectPageInstance;
	}

	@Override
	public ConnectorPublishPage getPublishPage() {
		return publishPageInstance;
	}

	@Override
	public ConnectPage getConnectPage() {
		return connectPageInstance;
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

	@Override
	public String getGeneralApiCall() {
		return "get-biodiv-nanopubs";
	}

}
