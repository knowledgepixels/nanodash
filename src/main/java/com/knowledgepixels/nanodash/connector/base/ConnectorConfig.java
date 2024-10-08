package com.knowledgepixels.nanodash.connector.base;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

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

	public abstract String getCandidateNanopubsApiCall();

	public String getGeneralReactionsApiCall() {
		return null;
	}

	public String getAcceptedNanopubsApiCall() {
		return null;
	}

	public Set<IRI> getTechnicalEditorIds() {
		return Collections.emptySet();
	}

	public IRI getNanopubType() {
		return null;
	}

	public String getTargetNamespace() {
		return null;
	}


	private static Map<String,String> queryIds = new HashMap<>();

	static {
		load("RAkoDiXZG_CYt978-dZ_vffK-UTbN6e1bmtFy6qdmFzC4/get-latest-accepted-bdj");
		load("RAgnLJH8kcI_e488VdoyQ0g3-wcumj4mSiusxPmeAYsSI/get-latest-biodiv-candidates");
		load("RATpsBysLf8yXeMpY7PHKj-aKNCa4-4Okg1hi97OLDXIo/get-latest-accepted-ds");
		load("RAAXmnJdXHO86GqJs8VTdqapUWqCrHKRgRT2b4NfjAfgk/get-latest-accepted-rio");
		load("RAe7k3L0oElPOrFoUMkUhqU9dGUqfBaUSw3cVplOUn3Fk/get-reactions");
	}

	private static void load(String queryId) {
		queryIds.put(queryId.substring(46), queryId);
	}

	public String get2ndGenerationQueryId(String queryName) {
		return queryIds.get(queryName);
	}

}
