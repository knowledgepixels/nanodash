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
 * Configuration for the Biodiversity Data Journal (BDJ) connector.
 */
public class BdjConfig extends ConnectorConfig {

    private static final long serialVersionUID = 1L;

    private static BdjConfig instance;

    /**
     * Get the singleton instance of the BDJ configuration.
     *
     * @return the BDJ configuration instance
     */
    public static BdjConfig get() {
        if (instance == null) instance = new BdjConfig();
        return instance;
    }

    @Override
    public String getLogoFileName() {
        return "BdjLogo.svg";
    }

    @Override
    public String getSubmitImageFileName() {
        return "RioFormSubmit.png";
    }

    @Override
    public String getJournalName() {
        return "Biodiversity Data Journal";
    }

    @Override
    public String getJournalAbbrev() {
        return "BDJ";
    }

    @Override
    public String getJournalUrl() {
        return "https://bdj.pensoft.net/";
    }

    @Override
    public String getJournalIssn() {
        return "1314-2828";
    }

    @Override
    public String getReviewUrlPrefix() {
        return "http://bdj.kpxl.org/";
        //return "https://w3id.org/kpxl/pensoft/bdj/np/reviewer/";
    }

    @Override
    public String getCandidateNanopubsApiCall() {
        return "get-latest-biodiv-candidates";
    }

    private Set<IRI> technicalEditorIds;

    @Override
    public Set<IRI> getTechnicalEditorIds() {
        if (technicalEditorIds == null) {
            technicalEditorIds = new HashSet<IRI>();
            technicalEditorIds.add(Utils.vf.createIRI("https://orcid.org/0000-0002-1267-0234"));
        }
        return technicalEditorIds;
    }

    @Override
    public IRI getNanopubType() {
        return Utils.vf.createIRI("https://w3id.org/kpxl/pensoft/bdj/terms/BiodiversityDataJournalNanopub");
    }

    @Override
    public String getTargetNamespace() {
        return "https://w3id.org/kpxl/pensoft/bdj/np/";
    }

    @Override
    public String getAcceptedNanopubsApiCall() {
        return "get-latest-accepted-bdj";
    }

    @Override
    public String getExtraInstructions() {
        return "<br><br>Nanopublications can be linked in your manuscript via the <a href=\"https://arpha.pensoft.net/\" target=\"_blank\">ARPHA Writing Tool</a>.\n"
                + "See the \"Nanopublications\" guidelines in the <a href=\"https://bdj.pensoft.net/about\" target=\"_blank\">About</a> info pages of the journal.";
    }

    @Override
    public String getConnectInstruction() {
        return "Paste it in the ARPHA Writing Tool as a \"Nanopublications\" element:";
    }

    private static final List<ConnectorOptionGroup> options;

    static {
        options = new ArrayList<>();
        options.add(new ConnectorOptionGroup("Biodiversity Associations",
                ConnectorOption.SPECTAXON,
                ConnectorOption.ORGORG,
                ConnectorOption.TAXONTAXON,
                ConnectorOption.TAXONENV,
                ConnectorOption.ORGENV,
                ConnectorOption.TAXONNAMES,
                ConnectorOption.ORGNS,
                ConnectorOption.TAXONNS
        ));
        options.add(new ConnectorOptionGroup("General Links",
                ConnectorOption.SPECPUB,
                ConnectorOption.BIOLINKREL,
                ConnectorOption.EQREL
        ));
        options.add(new ConnectorOptionGroup("Definitions",
                ConnectorOption.TAXONDEF,
                ConnectorOption.CLASSDEF,
                ConnectorOption.INDDEF
        ));
    }

    @Override
    public List<ConnectorOptionGroup> getOptions() {
        return options;
    }

}
