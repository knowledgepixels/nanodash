package com.knowledgepixels.nanodash.connector.pensoft;

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
 * Configuration for the RIO Journal connector.
 */
public class RioConfig extends ConnectorConfig {

    private static final long serialVersionUID = 1L;

    private static RioConfig instance;

    /**
     * Get the singleton instance of the RioConfig.
     *
     * @return the singleton instance of RioConfig
     */
    public static RioConfig get() {
        if (instance == null) instance = new RioConfig();
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLogoFileName() {
        return "RioLogo.svg";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSubmitImageFileName() {
        return "RioFormSubmit.png";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalName() {
        return "RIO Journal";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalAbbrev() {
        return "RIO";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalUrl() {
        return "https://riojournal.com/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJournalIssn() {
        return "2367-7163";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReviewUrlPrefix() {
        return "http://rio.kpxl.org/";
        //return "https://w3id.org/kpxl/pensoft/rio/np/reviewer/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCandidateNanopubsApiCall() {
        return "get-latest-rio-candidates";
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
        return Utils.vf.createIRI("https://w3id.org/kpxl/pensoft/rio/terms/RIOJournalNanopub");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTargetNamespace() {
        return "https://w3id.org/kpxl/pensoft/rio/np/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAcceptedNanopubsApiCall() {
        return "get-latest-accepted-rio";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtraInstructions() {
        return "<br><br>Nanopublications can be linked in your manuscript via the <a href=\"https://arpha.pensoft.net/\" target=\"_blank\">ARPHA Writing Tool</a>.\n"
                + "See the \"Nanopublications\" guidelines in the <a href=\"https://bdj.pensoft.net/about\" target=\"_blank\">About</a> info pages of the journal.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectInstruction() {
        return "Paste it in the ARPHA Writing Tool as a \"Nanopublications\" element:";
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
                ConnectorOption.EQREL
        ));
        options.add(new ConnectorOptionGroup("Definitions",
                ConnectorOption.CLASSDEF,
                ConnectorOption.INDDEF
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
