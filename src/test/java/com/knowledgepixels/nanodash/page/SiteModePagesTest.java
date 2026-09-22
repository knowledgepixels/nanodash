package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests what pages look like in site mode (issue #692), rendered by the real application.
 * The site's space is one that no service knows, so it never loads: the pages are rendered
 * the way a freshly started site renders them.
 */
@ExtendWith(SystemStubsExtension.class)
class SiteModePagesTest {

    private static final String SITE = "https://example.org/spaces/test-site";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        envVars.set("NANODASH_SITE_NAME", "Test Site");
        envVars.set("NANODASH_SITE_LOGO", "https://example.org/logo.svg");
        envVars.set("NANODASH_SITE_CSS", "https://example.org/site.css");
        tester = new WicketTester(new WicketApplication());
    }

    @Test
    void theHomePageIsTheSiteSpacesPage() {
        assertEquals(SiteHomePage.class, tester.getApplication().getHomePage());
    }

    /**
     * Until the repository knows the space, the home page is the loading page, which sends
     * the browser back shortly after.
     */
    @Test
    void homeShowsTheLoadingPageWhileTheSpaceIsUnknown() {
        tester.startPage(SiteHomePage.class);
        tester.assertRenderedPage(SiteLoadingPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("http-equiv=\"refresh\""), document);
        assertTrue(document.contains("<title>Test Site</title>"), document);
    }

    @Test
    void pagesWearTheSitesNameAndLogoInsteadOfNanodashs() {
        tester.startPage(ErrorPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("class=\"site-name\""), document);
        assertTrue(document.contains(">Test Site</a>"), document);
        assertTrue(document.contains("src=\"https://example.org/logo.svg\""), document);
        assertFalse(document.contains("class=\"logo\""), document);
        assertTrue(document.contains("<link rel=\"icon\" href=\"https://example.org/logo.svg\""), document);
        assertFalse(document.contains("favicon.svg"), document);
        assertTrue(document.contains("property=\"og:site_name\" content=\"Test Site\""), document);
        assertTrue(document.contains(" | Test Site</title>"), document);
        assertTrue(document.contains("href=\"https://example.org/site.css\""), document);
    }

    /**
     * The body carries the site class, which is what the stylesheet and the script key on to
     * mark links that leave the site.
     */
    @Test
    void theBodyIsMarkedAsASite() {
        tester.startPage(ErrorPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.matches("(?s).*<body[^>]*class=\"site\".*"), document);
    }

    /**
     * The back-link of a page without a context of its own leads to the site's home, named
     * after the site.
     */
    @Test
    void theBackLinkLeadsToTheSitesHome() {
        tester.startPage(ErrorPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("Test Site</span></a></span>"), document);
    }

}
