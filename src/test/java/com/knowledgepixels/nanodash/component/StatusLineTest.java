package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GovernedVersions;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusLineTest {

    private WicketTester wicketTester;

    @BeforeEach
    void setUp() {
        wicketTester = new WicketTester();
    }

    private static ApiResponseEntry entry(String newerVersion, String retractedBy, String supersededBy) {
        ApiResponseEntry e = new ApiResponseEntry();
        e.add("newerVersion", newerVersion);
        e.add("retractedBy", retractedBy);
        e.add("supersededBy", supersededBy);
        return e;
    }

    @Test
    void createComponentReturnsNonNullComponent() throws Exception {
        Nanopub np = new NanopubImpl(new File("src/test/resources/np-statusline-example.trig"), RDFFormat.TRIG);
        Component component = StatusLine.createComponent("statusLine", np);
        assertNotNull(component);

        ((ApiResultComponent) component).getLazyLoadComponent("statusLine");
        wicketTester.startComponentInPage((ApiResultComponent) component);
        while (!((ApiResultComponent) component).isContentReady()) {
            Thread.sleep(50);
        }
        String renderedHtml = wicketTester.getLastResponseAsString();
        //assertTrue(renderedHtml.contains("Status"));
    }

    // Valid trusty artifact codes (43 chars starting with RA) used for link rendering.
    private static final String TRUSTY_A = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String TRUSTY_B = "https://w3id.org/np/RABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
    private static final String TRUSTY_C = "https://w3id.org/np/RACCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";

    @Test
    void statusLineDisplaysNewerVersionLinkWhenNewerVersionExists() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry(TRUSTY_A, "", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has a <strong>newer version</strong>:"));
    }

    @Test
    void statusLineDisplaysRetractionMessageWhenRetracted() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry("", TRUSTY_A, ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has been <strong>retracted</strong>:"));
    }

    @Test
    void statusLineDisplaysMultipleNewerVersions() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry(TRUSTY_A, "", ""));
        response.getData().add(entry(TRUSTY_B, "", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has <strong>newer versions</strong>:"));
    }

    @Test
    void statusLineDisplaysMessageWhenNotProperlyPublished() {
        ApiResponse response = new ApiResponse();
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication doesn't seem to be properly published (yet)."));
    }

    @Test
    void governedLookupDoesNotOverrideRetractionMessage() throws Exception {
        // A governed definition that has been retracted keeps the retraction as its
        // status: the governed lookup only overrides the "latest version" verdict
        // (and, being guarded on that verdict, isn't even issued here).
        Nanopub governed = new NanopubImpl(new File("src/test/resources/np-governed-definition.trig"), RDFFormat.TRIG);
        GovernedVersions.GovernedRef governedRef = GovernedVersions.findGovernedRef(governed);
        assertNotNull(governedRef);
        ApiResponse response = new ApiResponse();
        response.getData().add(entry("", TRUSTY_A, ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, governedRef);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has been <strong>retracted</strong>:"));
    }

}
