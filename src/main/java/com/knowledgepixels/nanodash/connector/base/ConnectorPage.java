package com.knowledgepixels.nanodash.connector.base;

import java.io.IOException;
import java.util.Map;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryAccess;

import com.knowledgepixels.nanodash.page.NanodashPage;
import com.opencsv.exceptions.CsvValidationException;

public abstract class ConnectorPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPage(PageParameters parameters) {
		super(parameters);
	}

	protected abstract ConnectorConfig getConfig();

	public ApiResponse callApi(String operation, Map<String,String> params) throws CsvValidationException, IOException {
		// TODO Run these queries through ApiCache too:
		String secondGenQueryId = getConfig().get2ndGenerationQueryId(operation);
		if (secondGenQueryId != null) {
			try {
				return QueryAccess.get(secondGenQueryId, params);
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
			}
		}
		return ApiAccess.getAll(getConfig().getApiUrl(operation), operation, params);
	}

}
