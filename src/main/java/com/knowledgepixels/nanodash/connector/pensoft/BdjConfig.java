package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class BdjConfig extends ConnectorConfig {

	private static final long serialVersionUID = 1L;

	private static BdjConfig instance;

	public static BdjConfig get() {
		if (instance == null) instance = new BdjConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/bdj-nanopub-api/";

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

	@Override
	public String getPublishFormMessage() {
		return "<p><strong>Fill in the assertion (blue) part below, and then click \"Publish\" at the bottom to publish a nanopublication.</strong></p> " +
			"<p>You can later include the nanopublication link(s) in your Biodiversity Data Journal submission or leave it as a standalone publication related to other articles or resources.</p>" +
			"<p>To specify the URL of a preprint or source, you can switch to another provenance template (red). You can leave the pubinfo (yellow) part as it is.</p>";
	}

}
