package com.knowledgepixels.nanodash.connector.base;

public abstract class ConnectorConfig {

	public static final String baseApi = "https://grlc.petapico.org/api-git/knowledgepixels/connector-nanopub-api/";

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
		if ("get-reactions".equals(operation)) return true;
		return false;
	}

	public abstract OverviewPage getOverviewPage();

	public abstract TypePage getTypePage();

	public abstract NanopubPage getNanopubPage();

	public abstract String getLogoFileName();

	public abstract String getSubmitImageFileName();

	public abstract String getApiUrl(String operation);

	public abstract String getJournalName();

	public abstract String getJournalAbbrev();

	public abstract String getReviewUrlPrefix();

	public String getPublishFormMessage() {
		return null;
	}

}
