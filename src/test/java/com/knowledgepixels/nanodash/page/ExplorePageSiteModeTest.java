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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a site's Explore page sends a foreign term on to itself (issue #692), rather
 * than showing a page about it inside the site.
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
        tester = new WicketTester(new WicketApplication());
    }

    @Test
    void aForeignTermIsSentOnToItself() {
        tester.startPage(ExplorePage.class, new PageParameters().set("id", ELSEWHERE));
        // A 303 is written as status plus Location header, not as a servlet redirect.
        assertEquals(303, tester.getLastResponse().getStatus());
        assertEquals(ELSEWHERE, tester.getLastResponse().getHeader("Location"));
    }

}
