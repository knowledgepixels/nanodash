package com.knowledgepixels.nanodash.connector.base;

public abstract class ConnectorConfig {

	public abstract OverviewPage getOverviewPage();

	public abstract TypePage getTypePage();

	public abstract NanopubPage getNanopubPage();

	public abstract String getLogoFileName();

	public abstract String getSubmitImageFileName();

	public abstract String getApiUrl(String operation);

	public abstract String getJournalAbbrev();

	public abstract String getReviewUrlPrefix();

}
