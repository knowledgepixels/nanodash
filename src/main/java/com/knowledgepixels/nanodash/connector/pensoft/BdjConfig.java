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
		//return "https://w3id.org/kpxl/pensoft/bdj/np/reviewer/";
	}

	@Override
	public String getCandidateNanopubsApiCall() {
		return "get-latest-biodiv-candidates";
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
		return Utils.vf.createIRI("https://w3id.org/kpxl/pensoft/bdj/terms/BiodiversityDataJournalNanopub");
	}

	@Override
	public String getTargetNamespace() {
		return "https://w3id.org/kpxl/pensoft/bdj/np/";
	}

	@Override
	public String getAcceptedNanopubsApiCall() {
		return "get-latest-accepted-bdj";
	}

}
