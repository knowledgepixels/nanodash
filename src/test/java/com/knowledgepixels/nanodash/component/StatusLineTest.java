package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatusLineTest {

    private WicketTester wicketTester;

    @BeforeEach
    void setUp() {
        wicketTester = new WicketTester();
    }

    @Test
    void createComponentReturnsNonNullComponent() throws InterruptedException {
        Component component = StatusLine.createComponent("statusline", "https://w3id.org/np/RA58YcJyv1h-UmS8jI6UfFP6_LTAh59GTgpU_4lvBv7a4");
        assertNotNull(component);

        ((ApiResultComponent) component).getLazyLoadComponent("statusline");
        wicketTester.startComponentInPage((ApiResultComponent) component);
        while (!((ApiResultComponent) component).isContentReady()) {
            Thread.sleep(50);
        }
        String renderedHtml = wicketTester.getLastResponseAsString();
        //assertTrue(renderedHtml.contains("Status"));
    }

/*    @Test
    void statusLineDisplaysLatestVersionMessageWhenNoNewerVersionsOrRetractions() {
        ApiResponse response = new ApiResponse();
        response.getData().add(new ApiResponseEntry().put("newerVersion", "").put("retractedBy", "").put("supersededBy", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This is the latest version."));
    }

    @Test
    void statusLineDisplaysNewerVersionLinkWhenNewerVersionExists() {
        ApiResponse response = new ApiResponse();
        response.getData().add(new ApiResponseEntry().put("newerVersion", "newVersionId").put("retractedBy", "").put("supersededBy", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has a <strong>newer version</strong>:"));
        assertTrue(renderedHtml.contains("<a href=\"/explore?id=newVersionId\">"));
    }

    @Test
    void statusLineDisplaysRetractionMessageWhenRetracted() {
        ApiResponse response = new ApiResponse();
        response.getData().add(new ApiResponseEntry().put("newerVersion", "").put("retractedBy", "retractionId").put("supersededBy", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has been <strong>retracted</strong>:"));
        assertTrue(renderedHtml.contains("<a href=\"/explore?id=retractionId\">"));
    }

    @Test
    void statusLineDisplaysMultipleNewerVersions() {
        ApiResponse response = new ApiResponse();
        response.getData().add(new ApiResponseEntry().put("newerVersion", "version1").put("retractedBy", "").put("supersededBy", ""));
        response.getData().add(new ApiResponseEntry().put("newerVersion", "version2").put("retractedBy", "").put("supersededBy", ""));
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication has <strong>newer versions</strong>:"));
        assertTrue(renderedHtml.contains("<a href=\"/explore?id=version1\">"));
        assertTrue(renderedHtml.contains("<a href=\"/explore?id=version2\">"));
    }

    @Test
    void statusLineDisplaysMessageWhenNotProperlyPublished() {
        ApiResponse response = new ApiResponse();
        StatusLine statusLine = new StatusLine("testMarkupId", "testNpId", response);

        wicketTester.startComponentInPage(statusLine);
        String renderedHtml = wicketTester.getLastResponseAsString();

        assertTrue(renderedHtml.contains("This nanopublication doesn't seem to be properly published (yet)."));
    }*/

}