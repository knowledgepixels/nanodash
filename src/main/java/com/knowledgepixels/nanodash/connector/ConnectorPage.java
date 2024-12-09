package com.knowledgepixels.nanodash.connector;

import java.io.IOException;
import java.util.Map;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.opencsv.exceptions.CsvValidationException;

public abstract class ConnectorPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	private String connectorId;
	private ConnectorConfig config;

	public ConnectorPage(PageParameters parameters) {
		super(parameters);

		if (parameters.get("journal") != null) {
			connectorId = parameters.get("journal").toString();
		}
		if (connectorId != null) {
			config = ConnectorConfig.get(connectorId);
		}
	}

	public final ConnectorConfig getConfig() {
		return config;
	}

	public String getConnectorId() {
		return connectorId;
	}

	public ApiResponse callApi(String operation, Map<String,String> params) throws CsvValidationException, IOException {
		return ApiCache.retrieveResponse(ConnectorConfig.getQueryId(operation), params);
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
