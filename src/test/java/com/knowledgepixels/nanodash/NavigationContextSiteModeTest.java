package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.HomePage;
import com.knowledgepixels.nanodash.page.SiteHomePage;
import com.knowledgepixels.nanodash.page.SpacePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests how the navigation context changes in site mode (issue #692): the site's space is
 * the context of everything that names none, and its page is the home page.
 */
@ExtendWith(SystemStubsExtension.class)
class NavigationContextSiteModeTest {

    private static final String SITE = "https://example.org/spaces/my-site";
    private static final String OTHER = "https://example.org/spaces/other";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    void withoutASiteThereIsNoContextFallback() {
        assertNull(NavigationContext.getContextId(new PageParameters()));
        assertNull(NavigationContext.getContextId(null));
        assertEquals(HomePage.class, NavigationContext.homePageClass());
        assertEquals("Home", NavigationContext.homePageRef().getLabel());
    }

    @Test
    void theSiteSpaceIsTheContextWhereNoneIsNamed() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertEquals(SITE, NavigationContext.getContextId(new PageParameters()));
        assertEquals(SITE, NavigationContext.getContextId(null));
        // A context named in the URL still wins: a sub-space's page stays that sub-space's.
        assertEquals(OTHER, NavigationContext.getContextId(new PageParameters().set("context", OTHER)));
    }

    @Test
    void theSiteSpacesPageIsTheHomePage() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        envVars.set("NANODASH_SITE_NAME", "My Site");
        assertEquals(SiteHomePage.class, NavigationContext.homePageClass());
        NanodashPageRef home = NavigationContext.homePageRef();
        assertEquals(SiteHomePage.class, home.getPageClass());
        assertEquals("My Site", home.getLabel());
        NanodashPageRef siteRef = NavigationContext.getPageRef(SITE);
        assertNotNull(siteRef);
        assertEquals(SiteHomePage.class, siteRef.getPageClass());
        Space site = mock(Space.class);
        when(site.getId()).thenReturn(SITE);
        assertEquals(SiteHomePage.class, NavigationContext.getPageClass(site));
        Space other = mock(Space.class);
        when(other.getId()).thenReturn(OTHER);
        assertEquals(SpacePage.class, NavigationContext.getPageClass(other));
    }

    @Test
    void nanodashsHomeResourceIsNotAHomeInSiteMode() {
        String homeResource = NanodashPreferences.get().getHomeResource();
        assertTrue(NavigationContext.isHomeResource(homeResource));
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertFalse(NavigationContext.isHomeResource(homeResource));
    }

}
