package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests where links lead in site mode (issue #692): an IRI that nothing places in the site is
 * linked as itself when met in a nanopublication's statements, but kept inside the site when
 * a view lists it, for the Explore page to settle on click.
 */
@ExtendWith(SystemStubsExtension.class)
class NanodashLinkSiteModeTest {

    private static final String SITE = "https://example.org/spaces/my-site";
    private static final String ELSEWHERE = "https://fairsharing.org/fairsharing_records/3517";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    @BeforeEach
    void setUp() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        // Components need an application to be constructed against.
        new WicketTester(new WicketApplication());
    }

    @Test
    void anIriMetInStatementsLeavesTheSite() {
        Component link = NanodashLink.createLink("link", ELSEWHERE, "3517", null, false, false);
        ExternalLink external = assertInstanceOf(ExternalLink.class, link);
        assertEquals(ELSEWHERE, external.getDefaultModelObjectAsString());
    }

    @Test
    void anIriListedByAViewStaysInsideTheSite() {
        Component link = NanodashLink.createLink("link", ELSEWHERE, "3517", SITE, false, true);
        BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
        assertEquals(ExplorePage.class, internal.getPageClass());
        assertEquals("true", internal.getPageParameters().get("forward-to-part").toString());
        assertEquals(SITE, internal.getPageParameters().get("context").toString());
    }

    @Test
    void whatIsTheSitesStaysInsideEitherWay() {
        Component link = NanodashLink.createLink("link", SITE + "/r/dataset", "dataset", null, false, false);
        assertInstanceOf(BookmarkablePageLink.class, link);
    }

}
