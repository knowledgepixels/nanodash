package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Base class for connector pages in the Nanodash application.
 */
public abstract class ConnectorPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    private String connectorId;
    private ConnectorConfig config;

    /**
     * Constructor for ConnectorPage.
     *
     * @param parameters Page parameters containing the connector ID.
     */
    public ConnectorPage(PageParameters parameters) {
        super(parameters);

        if (parameters.get("journal") != null) {
            connectorId = parameters.get("journal").toString();
        }
        if (connectorId != null) {
            config = ConnectorConfig.get(connectorId);
        }
    }

    /**
     * Returns the configuration for the connector.
     *
     * @return the ConnectorConfig object for this connector.
     */
    public final ConnectorConfig getConfig() {
        return config;
    }

    /**
     * Returns the ID of the connector.
     *
     * @return the ID of the connector as a String.
     */
    public String getConnectorId() {
        return connectorId;
    }

    /**
     * Calls the API for the specified operation with the given parameters.
     *
     * @param operation the operation to perform, typically a query ID.
     * @param params    the parameters to pass to the API call.
     * @return ApiResponse containing the result of the API call.
     * @throws com.opencsv.exceptions.CsvValidationException if there is an error in CSV validation.
     * @throws java.io.IOException                           if there is an error during the API call.
     */
    public ApiResponse callApi(String operation, Map<String, String> params) throws CsvValidationException, IOException {
        return ApiCache.retrieveResponse(ConnectorConfig.getQueryId(operation), params);
    }

    /**
     * Checks if auto-refresh is enabled for this connector page.
     *
     * @return true if auto-refresh is enabled, false otherwise.
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
