package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

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
    void createComponentReturnsNonNullComponent() throws InterruptedException {
        Component component = StatusLine.createComponent("statusLine", "https://w3id.org/np/RA58YcJyv1h-UmS8jI6UfFP6_LTAh59GTgpU_4lvBv7a4");
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
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has a <strong>newer version</strong>:"));
    }

    @Test
    void statusLineDisplaysRetractionMessageWhenRetracted() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry("", TRUSTY_A, ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has been <strong>retracted</strong>:"));
    }

    @Test
    void statusLineDisplaysMultipleNewerVersions() {
        ApiResponse response = new ApiResponse();
        response.getData().add(entry(TRUSTY_A, "", ""));
        response.getData().add(entry(TRUSTY_B, "", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", TRUSTY_C, response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has <strong>newer versions</strong>:"));
    }

    @Test
    void statusLineDisplaysMessageWhenNotProperlyPublished() {
        ApiResponse response = new ApiResponse();
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication doesn't seem to be properly published (yet)."));
    }

}
