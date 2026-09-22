package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.TitleBar.CrumbPart;
import com.knowledgepixels.nanodash.page.SiteHomePage;
import com.knowledgepixels.nanodash.page.SpacePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests the breadcrumb in site mode (issue #692): the site's space tops every path and is
 * named at the top left already, so its crumb is left out.
 */
@ExtendWith(SystemStubsExtension.class)
class TitleBarSiteModeTest {

    private static final String SITE = "https://example.org/spaces/my-site";
    private static final String SUB = "https://example.org/spaces/my-site/sub";
    private static final String OTHER = "https://example.org/spaces/other";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private static NanodashPageRef spaceRef(String id, String label) {
        return new NanodashPageRef(SpacePage.class, new PageParameters().add("id", id), label);
    }

    private static List<String> labels(List<CrumbPart> parts) {
        return parts.stream().map(CrumbPart::label).toList();
    }

    @Test
    void outsideSiteModeThePathIsLeftAlone() {
        List<CrumbPart> parts = TitleBar.buildCrumbParts(new NanodashPageRef[]{spaceRef(SITE, "My Site"), spaceRef(SUB, "My Site Sub")});
        assertSame(parts, TitleBar.withoutSiteHome(parts));
    }

    @Test
    void theSiteSpaceIsDroppedFromTheTopOfThePath() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        List<CrumbPart> parts = TitleBar.buildCrumbParts(new NanodashPageRef[]{spaceRef(SITE, "My Site"), spaceRef(SUB, "My Site Sub")});
        // The child's label was shortened against its parent and stays that way.
        assertEquals(List.of("Sub"), labels(TitleBar.withoutSiteHome(parts)));
    }

    @Test
    void theHomePageRefIsDroppedToo() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        List<CrumbPart> parts = TitleBar.buildCrumbParts(new NanodashPageRef[]{
                new NanodashPageRef(SiteHomePage.class, "My Site"), spaceRef(SUB, "Sub")});
        assertEquals(List.of("Sub"), labels(TitleBar.withoutSiteHome(parts)));
    }

    @Test
    void aPathToppedByAnotherSpaceKeepsItsTop() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        List<CrumbPart> parts = TitleBar.buildCrumbParts(new NanodashPageRef[]{spaceRef(OTHER, "Other"), spaceRef(SUB, "Sub")});
        assertEquals(List.of("Other", "Sub"), labels(TitleBar.withoutSiteHome(parts)));
    }

    @Test
    void aPathOfOnlyTheSiteSpaceBecomesEmpty() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        List<CrumbPart> parts = TitleBar.buildCrumbParts(new NanodashPageRef[]{spaceRef(SITE, "My Site")});
        assertEquals(List.of(), labels(TitleBar.withoutSiteHome(parts)));
    }

}
