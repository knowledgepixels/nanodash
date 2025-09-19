package com.knowledgepixels.nanodash.connector;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.page.NanodashPage;

/**
 * Base class for connector pages in the Nanodash application.
 */
public abstract class ConnectorPage extends NanodashPage {

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
     * Checks if auto-refresh is enabled for this connector page.
     *
     * @return true if auto-refresh is enabled, false otherwise.
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
