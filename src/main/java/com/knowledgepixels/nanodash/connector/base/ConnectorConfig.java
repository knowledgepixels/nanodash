package com.knowledgepixels.nanodash.connector.base;

import java.io.Serializable;

public abstract class ConnectorConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String baseApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/connector-nanopub-api/";

	public static boolean isBaseApiOperation(String operation) {
		if ("get-aida-nanopubs".equals(operation)) return true;
		if ("get-biorel-nanopubs".equals(operation)) return true;
		if ("get-classdef-nanopubs".equals(operation)) return true;
		if ("get-crel-nanopubs".equals(operation)) return true;
		if ("get-inddef-nanopubs".equals(operation)) return true;
		if ("get-linkflowsrel-nanopubs".equals(operation)) return true;
		if ("get-ml-nanopubs".equals(operation)) return true;
		if ("get-superpattern-nanopubs".equals(operation)) return true;
		if ("get-reaction-nanopubs".equals(operation)) return true;
		if ("get-eqrel-nanopubs".equals(operation)) return true;
		if ("get-biolinkrel-nanopubs".equals(operation)) return true;
		if ("get-reactions".equals(operation)) return true;
		return false;
	}

	public abstract OverviewPage getOverviewPage();

	public abstract SelectPage getSelectPage();

	public abstract ConnectorPublishPage getPublishPage();

	public abstract ConnectPage getConnectPage();

	public abstract NanopubPage getNanopubPage();

	public abstract String getLogoFileName();

	public abstract String getSubmitImageFileName();

	public abstract String getApiUrl(String operation);

	public abstract String getJournalName();

	public abstract String getJournalAbbrev();

	public abstract String getReviewUrlPrefix();

	public abstract String getGeneralApiCall();

	public String getGeneralReactionsApiCall() {
		return null;
	}

	public String getAcceptedNanopubsApiCall() {
		return null;
	}

}
