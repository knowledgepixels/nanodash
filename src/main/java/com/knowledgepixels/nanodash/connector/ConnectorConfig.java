package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;
import org.eclipse.rdf4j.model.IRI;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract class representing the configuration for different connectors in the Nanodash application.
 */
public abstract class ConnectorConfig implements Serializable {

    /**
     * Returns the ConnectorConfig instance for a given connector ID.
     *
     * @param connectorId the ID of the connector
     * @return the ConnectorConfig instance corresponding to the connector ID.
     */
    public static ConnectorConfig get(String connectorId) {
        if (connectorId.equals("ios/ds")) {
            return DsConfig.get();
        } else if (connectorId.equals("pensoft/bdj")) {
            return BdjConfig.get();
        } else if (connectorId.equals("pensoft/rio")) {
            return RioConfig.get();
        } else {
            return null;
        }
    }

    /**
     * Returns the logo file name for the connector.
     *
     * @return the logo file name
     */
    public abstract String getLogoFileName();

    /**
     * Returns the file name for the submit image.
     *
     * @return the submit image file name
     */
    public abstract String getSubmitImageFileName();

    /**
     * Returns the name of the journal associated with the connector.
     *
     * @return the journal name
     */
    public abstract String getJournalName();

    /**
     * Returns the abbreviation of the journal associated with the connector.
     *
     * @return the journal abbreviation
     */
    public abstract String getJournalAbbrev();

    /**
     * Returns the URL of the journal's homepage.
     *
     * @return the journal URL
     */
    public abstract String getJournalUrl();

    /**
     * Returns the ISSN of the journal associated with the connector.
     *
     * @return the journal ISSN
     */
    public abstract String getJournalIssn();

    /**
     * Returns the prefix URL for review pages associated with the connector.
     *
     * @return the review URL prefix
     */
    public abstract String getReviewUrlPrefix();

    /**
     * Returns the API call URL to fetch candidate nanopublications.
     *
     * @return the API call URL for candidate nanopublications
     */
    public abstract String getCandidateNanopubsApiCall();

    /**
     * Returns the connection instruction for the connector.
     *
     * @return the connection instruction
     */
    public abstract String getConnectInstruction();

    /**
     * Returns a list of option groups for the connector.
     *
     * @return a list of ConnectorOptionGroup instances
     */
    public abstract List<ConnectorOptionGroup> getOptions();

    /**
     * Returns the general reactions API call.
     *
     * @return the general reactions API call, or {@code null} if not applicable
     */
    public String getGeneralReactionsApiCall() {
        return null;
    }

    /**
     * Returns the accepted nanopublications API call.
     *
     * @return the accepted nanopublications API call
     */
    public String getAcceptedNanopubsApiCall() {
        return null;
    }

    /**
     * Returns the IDs of technical editors for the connector.
     *
     * @return a set of IRI IDs representing technical editors, or an empty set if not applicable
     */
    public Set<IRI> getTechnicalEditorIds() {
        return Collections.emptySet();
    }

    /**
     * Returns the IRI of the nanopublication type for this connector.
     *
     * @return the IRI of the nanopublication type, or null if not applicable
     */
    public IRI getNanopubType() {
        return null;
    }

    /**
     * Returns the target namespace for the connector.
     *
     * @return the target namespace as a String, or null if not applicable
     */
    public String getTargetNamespace() {
        return null;
    }

    /**
     * Returns any extra instructions for the connector.
     *
     * @return extra instructions as a String, or an empty string if there are none
     */
    public String getExtraInstructions() {
        return "";
    }

    private static Map<String, String> queryIds = new HashMap<>();

    static {
        load("RAkoDiXZG_CYt978-dZ_vffK-UTbN6e1bmtFy6qdmFzC4/get-latest-accepted-bdj");
        load("RAgnLJH8kcI_e488VdoyQ0g3-wcumj4mSiusxPmeAYsSI/get-latest-biodiv-candidates");
        load("RATpsBysLf8yXeMpY7PHKj-aKNCa4-4Okg1hi97OLDXIo/get-latest-accepted-ds");
        load("RAFNTW3jhWKnNvhMSOfYvG53ZAurxrFv_-vnIJkZyfAuo/get-latest-ds-candidates");
        load("RA0FiH8gukovvEHPBMn72zUDdMQylQmUwtIGNLYBZXGfk/get-ds-reactions");
        load("RAAXmnJdXHO86GqJs8VTdqapUWqCrHKRgRT2b4NfjAfgk/get-latest-accepted-rio");
        load("RAehKOCOnZ3uDBmI0kkCNTh5k9Nl6YYNj7tyc20tVymxY/get-latest-rio-candidates");
        load("RAe7k3L0oElPOrFoUMkUhqU9dGUqfBaUSw3cVplOUn3Fk/get-reactions");
    }

    private static void load(String queryId) {
        queryIds.put(queryId.substring(46), queryId);
    }

    /**
     * Returns the query ID for a given query name.
     *
     * @param queryName the name of the query
     * @return the query ID if it exists, otherwise null
     */
    public static String getQueryId(String queryName) {
        return queryIds.get(queryName);
    }

}
