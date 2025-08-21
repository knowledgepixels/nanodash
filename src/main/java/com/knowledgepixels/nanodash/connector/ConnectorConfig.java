package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;
import org.eclipse.rdf4j.model.IRI;

import java.io.Serializable;
import java.util.*;

public abstract class ConnectorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ConnectorConfig get(String connectorId) {
        if (connectorId.equals("ios/ds")) {
            return DsConfig.get();
        } else if (connectorId.equals("pensoft/bdj")) {
            return BdjConfig.get();
        } else if (connectorId.equals("pensoft/rio")) {
            return RioConfig.get();
        } else {
            throw new IllegalArgumentException("'journal' parameter not recognized");
        }
    }

    public abstract String getLogoFileName();

    public abstract String getSubmitImageFileName();

    public abstract String getJournalName();

    public abstract String getJournalAbbrev();

    public abstract String getJournalUrl();

    public abstract String getJournalIssn();

    public abstract String getReviewUrlPrefix();

    public abstract String getCandidateNanopubsApiCall();

    public abstract String getConnectInstruction();

    public abstract List<ConnectorOptionGroup> getOptions();

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

    public static String getQueryId(String queryName) {
        return queryIds.get(queryName);
    }

}
