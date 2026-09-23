package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what a site's Explore page makes of a foreign term (issue #692): a page that names
 * the term's own address and nothing of what the network knows about it.
 */
@ExtendWith(SystemStubsExtension.class)
class ExplorePageSiteModeTest {

    private static final String SITE = "https://example.org/spaces/test-site";
    private static final String ELSEWHERE = "https://elsewhere.example.org/things/42";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        envVars.set("NANODASH_SITE_NAME", "Test Site");
        tester = new WicketTester(new WicketApplication());
    }

    /**
     * A foreign term gets a page that names its address and says it is not the site's, and
     * nothing of what the network knows about it: no tabs, no info or references sections.
     */
    @Test
    void aForeignTermGetsAPageWithNothingButItsAddress() {
        tester.startPage(ExplorePage.class, new PageParameters().set("id", ELSEWHERE));
        tester.assertRenderedPage(ExplorePage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("This is not part of Test Site. Follow the address above to open it."), document);
        assertTrue(document.contains("href=\"" + ELSEWHERE + "\""), document);
        assertFalse(document.contains("tabs-container"), document);
        assertFalse(document.contains("references-section"), document);
    }

}
