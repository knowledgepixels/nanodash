package com.knowledgepixels.nanodash.connector.base;

import java.io.IOException;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;

import com.opencsv.exceptions.CsvValidationException;

public abstract class ConnectorPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private final PageParameters parameters;

	public ConnectorPage(PageParameters parameters) {
		if (parameters == null) {
			this.parameters = new PageParameters();
		} else {
			this.parameters = parameters;
		}
	}

	protected abstract String getMountPath();

	protected abstract ConnectorConfig getConfig();

	public PageParameters getParams() {
		return parameters;
	}

	public ApiResponse callApi(String operation, Map<String,String> params) throws CsvValidationException, IOException {
		return ApiAccess.getAll(getConfig().getApiUrl(operation), operation, params);
	}

}
