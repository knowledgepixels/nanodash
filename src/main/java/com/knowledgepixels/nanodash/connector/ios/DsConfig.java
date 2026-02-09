package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.ConnectorOption;
import com.knowledgepixels.nanodash.connector.ConnectorOptionGroup;
import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration class for the Data Science (DS) connector.
 */
public class DsConfig extends ConnectorConfig {

    private static DsConfig instance;

    /**
     * Gets the singleton instance of the DsConfig class.
     *
     * @return the singleton instance of DsConfig
     */
    public static DsConfig get() {
        if (instance == null) instance = new DsConfig();
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLogoFileName() {
        return "DsLogo.png";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubmitImageFileName() {
        return "DsFormSubmit.png";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalName() {
        return "Data Science";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalAbbrev() {
        return "DS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalUrl() {
        return "https://datasciencehub.net/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalIssn() {
        return "2451-8492";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReviewUrlPrefix() {
        return "http://ds.kpxl.org/";
        //return "https://w3id.org/kpxl/ios/ds/np/reviewer/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCandidateNanopubsApiCall() {
        return QueryApiAccess.GET_LATEST_DS_CANDIDATES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGeneralReactionsApiCall() {
        return QueryApiAccess.GET_DS_REACTIONS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAcceptedNanopubsApiCall() {
        return QueryApiAccess.GET_LATEST_ACCEPTED_DS;
    }

    private Set<IRI> technicalEditorIds;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<IRI> getTechnicalEditorIds() {
        if (technicalEditorIds == null) {
            technicalEditorIds = new HashSet<IRI>();
            technicalEditorIds.add(Utils.vf.createIRI("https://orcid.org/0000-0002-1267-0234"));
        }
        return technicalEditorIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRI getNanopubType() {
        return Utils.vf.createIRI("https://w3id.org/kpxl/ios/ds/terms/DataScienceNanopub");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTargetNamespace() {
        return "https://w3id.org/kpxl/ios/ds/np/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectInstruction() {
        return "Paste it in one of the textfields under \"Nanopublication URLs\":";
    }

    private static final List<ConnectorOptionGroup> options;

    static {
        options = new ArrayList<>();
        options.add(new ConnectorOptionGroup("Simple Scientific Statements",
                ConnectorOption.LINKFLOWSREL,
                ConnectorOption.CREL
        ));
        options.add(new ConnectorOptionGroup("Complex Scientific Statements",
                ConnectorOption.SUPERPATTERN,
                ConnectorOption.AIDA
        ));
        options.add(new ConnectorOptionGroup("Specific Types of Statements",
                ConnectorOption.BIOREL,
                ConnectorOption.ML
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConnectorOptionGroup> getOptions() {
        return options;
    }

}
