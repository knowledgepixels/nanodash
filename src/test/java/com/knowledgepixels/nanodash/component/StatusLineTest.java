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
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;
import org.nanopub.testsuite.TestSuiteEntry;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusLineTest {

    /**
     * A published retraction, which is one of the shapes a status line is drawn from. It lives
     * in the nanopub test suite rather than in this repository, so that the fixture is shared
     * with the other implementations instead of being copied into each of them (#620). Its
     * absence is a failure rather than a reason to skip: it would mean the entry has been
     * renamed or removed.
     */
    private static final String RETRACTION_CODE = "RA58YcJyv1h-UmS8jI6UfFP6_LTAh59GTgpU_4lvBv7a4";

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
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByArtifactCode(RETRACTION_CODE, TestSuiteCategory.VALID)
                .orElseThrow(() -> new IllegalStateException("Not in the nanopub test suite: " + RETRACTION_CODE));
        Nanopub np = new NanopubImpl(entry.toFile(), RDFFormat.TRIG);
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
    void statusLineIgnoresSelfAndDuplicateRows() {
        // The query's supersedes path is reflexive and joins on the indexed
        // signature keys and creation dates, so the nanopublication comes back as
        // its own "newer version", possibly many times over.
        ApiResponse response = new ApiResponse();
        for (int i = 0; i < 12; i++) {
            response.getData().add(entry(TRUSTY_C, "", ""));
        }
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This is the latest version."));
        assertFalse(renderedHtml.contains("newer version"));
    }

    @Test
    void statusLineListsEachNewerVersionOnce() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry(TRUSTY_C, "", ""));
        response.getData().add(entry(TRUSTY_A, "", ""));
        response.getData().add(entry(TRUSTY_A, "", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response, null);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has a <strong>newer version</strong>:"));
        // One rendered link label, not one per duplicate row.
        assertEquals(1, renderedHtml.split("<span>RAAAAAAAAA</span>", -1).length - 1, renderedHtml);
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
