package com.knowledgepixels.nanodash.connector.base;

import java.io.IOException;
import java.util.Map;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.opencsv.exceptions.CsvValidationException;

public abstract class ConnectorPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPage(PageParameters parameters) {
		super(parameters);
	}

	protected abstract ConnectorConfig getConfig();

	public ApiResponse callApi(String operation, Map<String,String> params) throws CsvValidationException, IOException {
		return ApiCache.retrieveResponse(ConnectorConfig.getQueryId(operation), params);
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
