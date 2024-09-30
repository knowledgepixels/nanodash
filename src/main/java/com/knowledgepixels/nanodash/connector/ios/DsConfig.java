package com.knowledgepixels.nanodash.connector.ios;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.connector.base.ConnectPage;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPublishPage;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.base.SelectPage;

public class DsConfig extends ConnectorConfig {

	private static final long serialVersionUID = 1L;

	private static DsConfig instance;

	public static DsConfig get() {
		if (instance == null) instance = new DsConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/ds-nanopub-api/";

	private static final DsOverviewPage overviewPageInstance = new DsOverviewPage(null);
	private static final DsSelectPage selectPageInstance = new DsSelectPage(null);
	private static final DsPublishPage publishPageInstance = new DsPublishPage(null);
	private static final DsConnectPage connectPageInstance = new DsConnectPage(null, null);
	private static final DsNanopubPage nanopubPageInstance = new DsNanopubPage(null);

	@Override
	public OverviewPage getOverviewPage() {
		return overviewPageInstance;
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
		if (isBaseApiOperation(operation)) return baseApi;
		return specificApi;
	}

	@Override
	public String getJournalName() {
		return "Data Science";
	}

	@Override
	public String getJournalAbbrev() {
		return "DS";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://ds.kpxl.org/";
		//return "https://w3id.org/kpxl/ios/ds/np/reviewer/";
	}

	@Override
	public String getCandidateNanopubsApiCall() {
		return "get-ds-nanopubs";
	}

	@Override
	public String getGeneralReactionsApiCall() {
		return "get-ds-reactions";
	}

	@Override
	public String getAcceptedNanopubsApiCall() {
		// Still deactivated:
		//return "get-accepted-nanopubs";
		return null;
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

	private Set<IRI> technicalEditorIds;

	@Override
	public Set<IRI> getTechnicalEditorIds() {
		if (technicalEditorIds == null) {
			technicalEditorIds = new HashSet<IRI>();
			technicalEditorIds.add(Utils.vf.createIRI("https://orcid.org/0000-0002-1267-0234"));
		}
		return technicalEditorIds;
	}

	@Override
	public IRI getNanopubType() {
		return Utils.vf.createIRI("https://w3id.org/kpxl/ios/ds/terms/DataScienceNanopub");
	}

	@Override
	public String getTargetNamespace() {
		return "https://w3id.org/kpxl/ios/ds/np/";
	}

}
