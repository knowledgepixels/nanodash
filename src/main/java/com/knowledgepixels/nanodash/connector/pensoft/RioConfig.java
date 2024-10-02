package com.knowledgepixels.nanodash.connector.pensoft;

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

public class RioConfig extends ConnectorConfig {

	private static final long serialVersionUID = 1L;

	private static RioConfig instance;

	public static RioConfig get() {
		if (instance == null) instance = new RioConfig();
		return instance;
	}

	public static final String specificApi = "https://grlc.knowledgepixels.com/api-git/knowledgepixels/rio-nanopub-api/";

	private static final RioOverviewPage overviewPageInstance = new RioOverviewPage(null);
	private static final RioSelectPage selectPageInstance = new RioSelectPage(null);
	private static final RioPublishPage publishPageInstance = new RioPublishPage(null);
	private static final RioConnectPage connectPageInstance = new RioConnectPage(null, null);
	private static final RioNanopubPage nanopubPageInstance = new RioNanopubPage(null);

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
		return "RioLogo.svg";
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
		return "RIO Journal";
	}

	@Override
	public String getJournalAbbrev() {
		return "RIO";
	}

	@Override
	public String getReviewUrlPrefix() {
		return "http://rio.kpxl.org/";
		//return "https://w3id.org/kpxl/pensoft/rio/np/reviewer/";
	}

	@Override
	public String getCandidateNanopubsApiCall() {
		return "get-rio-nanopubs";
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
		return Utils.vf.createIRI("https://w3id.org/kpxl/pensoft/rio/terms/RIOJournalNanopub");
	}

	@Override
	public String getTargetNamespace() {
		return "https://w3id.org/kpxl/pensoft/rio/np/";
	}

	@Override
	public String getAcceptedNanopubsApiCall() {
		return "get-latest-accepted-rio";
	}

}
