package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.NanodashLink;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests site mode (issue #692): an instance configured as the website of one space.
 */
@ExtendWith(SystemStubsExtension.class)
class SiteModeTest {

    private static final String SITE = "https://example.org/spaces/my-site";
    private static final String SITE_ALT = "https://alt.example.org/my-site";
    private static final String NANOPUB = "https://w3id.org/np/RAAbcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHI";
    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final String USER = "https://example.org/people/someone";
    private static final String ELSEWHERE = "https://elsewhere.org/things/42";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private SpaceRepository spaces;
    private MaintainedResourceRepository resources;
    private MockedStatic<SpaceRepository> spaceRepository;
    private MockedStatic<MaintainedResourceRepository> resourceRepository;
    private MockedStatic<IndividualAgent> agents;

    @BeforeEach
    void mockRepositories() {
        spaces = mock(SpaceRepository.class);
        resources = mock(MaintainedResourceRepository.class);
        spaceRepository = mockStatic(SpaceRepository.class);
        spaceRepository.when(SpaceRepository::get).thenReturn(spaces);
        resourceRepository = mockStatic(MaintainedResourceRepository.class);
        resourceRepository.when(MaintainedResourceRepository::get).thenReturn(resources);
        // Known users come from the user data, which is loaded over the network; the ORCID
        // pattern check stays real.
        agents = mockStatic(IndividualAgent.class, CALLS_REAL_METHODS);
        agents.when(() -> IndividualAgent.isUser(any())).thenReturn(false);
        agents.when(() -> IndividualAgent.isUser(USER)).thenReturn(true);
    }

    @AfterEach
    void closeMocks() {
        agents.close();
        resourceRepository.close();
        spaceRepository.close();
    }

    private Space space(String id, List<Space> ancestors) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(id);
        when(space.getAltIDs()).thenReturn(List.of());
        when(space.getAllSuperSpacesUntilRoot()).thenReturn(List.copyOf(ancestors));
        when(spaces.findById(id)).thenReturn(space);
        return space;
    }

    private Space siteSpace() {
        Space site = space(SITE, List.of());
        when(site.getLabel()).thenReturn("My Site");
        when(site.getAltIDs()).thenReturn(List.of(SITE_ALT));
        when(spaces.findByAltId(SITE_ALT)).thenReturn(site);
        return site;
    }

    @Test
    void notASiteUntilASpaceIsConfigured() {
        assertFalse(SiteMode.isEnabled());
        assertNull(SiteMode.getSpaceId());
        assertNull(SiteMode.getName());
        assertNull(SiteMode.getLogoSrc());
        assertFalse(SiteMode.isSiteSpace(SITE));
        // Without a site there is no outside: every link stays what it always was.
        assertTrue(SiteMode.belongsToSite(ELSEWHERE));
        assertFalse(SiteMode.rendersExternally(ELSEWHERE));
    }

    @Test
    void theEnvironmentVariableMakesTheInstanceASite() {
        envVars.set("NANODASH_SITE_SPACE", " " + SITE + " ");
        assertTrue(SiteMode.isEnabled());
        assertEquals(SITE, SiteMode.getSpaceId());
        assertTrue(SiteMode.isSiteSpace(SITE));
    }

    @Test
    void theSpaceIsAlsoKnownUnderItsAlternativeIri() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        siteSpace();
        assertTrue(SiteMode.isSiteSpace(SITE_ALT));
        assertFalse(SiteMode.isSiteSpace(ELSEWHERE));
    }

    @Test
    void nameComesFromTheConfigurationThenTheSpaceThenItsIri() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        // The space is not loaded yet: the IRI's last part has to do.
        assertEquals("my-site", SiteMode.getName());
        siteSpace();
        assertEquals("My Site", SiteMode.getName());
        envVars.set("NANODASH_SITE_NAME", "Configured Name");
        assertEquals("Configured Name", SiteMode.getName());
    }

    @Test
    void logoComesFromTheConfigurationThenTheSpacesProfilePicture() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertNull(SiteMode.getLogoSrc());
        Space site = siteSpace();
        when(site.getProfilePicture()).thenReturn(ProfilePicture.of("https://example.org/pic.png"));
        assertEquals("https://example.org/pic.png", SiteMode.getLogoSrc());
        envVars.set("NANODASH_SITE_LOGO", "https://example.org/logo.svg");
        assertEquals("https://example.org/logo.svg", SiteMode.getLogoSrc());
    }

    @Test
    void theSpaceAndWhatLiesUnderItBelongToTheSite() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        siteSpace();
        assertTrue(SiteMode.belongsToSite(SITE));
        assertTrue(SiteMode.belongsToSite(SITE + "/r/dataset-1"));
        assertTrue(SiteMode.belongsToSite(SITE + "#section"));
        assertTrue(SiteMode.belongsToSite(SITE_ALT + "/anything"));
        // A different space whose IRI merely starts with the same characters is not under it.
        assertFalse(SiteMode.belongsToSite(SITE + "-other"));
        assertFalse(SiteMode.belongsToSite(ELSEWHERE));
    }

    @Test
    void nanopublicationsAndUsersBelongToEverySite() {
        // What was just published must be viewable within the site, and a user's own page is
        // where their publications are found (docs/site-views.md).
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertTrue(SiteMode.belongsToSite(NANOPUB));
        assertTrue(SiteMode.belongsToSite(NANOPUB + "/minted-inside"));
        assertTrue(SiteMode.belongsToSite(ORCID));
        assertTrue(SiteMode.belongsToSite(USER));
    }

    @Test
    void subSpacesAndMaintainedResourcesOfTheSiteBelongToIt() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        Space site = siteSpace();
        Space sub = space("https://example.org/spaces/sub", List.of(site));
        Space foreign = space("https://example.org/spaces/foreign", List.of());
        MaintainedResource ofSite = mock(MaintainedResource.class);
        when(ofSite.getSpace()).thenReturn(sub);
        when(resources.findById("https://example.org/resources/mine")).thenReturn(ofSite);
        when(resources.findByNamespace("https://example.org/resources/mine/")).thenReturn(ofSite);
        MaintainedResource ofForeign = mock(MaintainedResource.class);
        when(ofForeign.getSpace()).thenReturn(foreign);
        when(resources.findById("https://example.org/resources/theirs")).thenReturn(ofForeign);

        assertTrue(SiteMode.belongsToSite(sub.getId()));
        assertFalse(SiteMode.belongsToSite(foreign.getId()));
        assertTrue(SiteMode.belongsToSite("https://example.org/resources/mine"));
        assertTrue(SiteMode.belongsToSite("https://example.org/resources/mine/part"));
        assertFalse(SiteMode.belongsToSite("https://example.org/resources/theirs"));
    }

    @Test
    void outsideLinksLeaveTheSiteUnlessSwitchedOff() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertTrue(SiteMode.rendersExternally(ELSEWHERE));
        assertFalse(SiteMode.rendersExternally(SITE + "/inside"));
        assertFalse(SiteMode.rendersExternally(NANOPUB));
        envVars.set("NANODASH_SITE_EXTERNAL_LINKS", "false");
        assertFalse(SiteMode.rendersExternally(ELSEWHERE));
    }

    @Test
    void pageUrlsFollowTheLinkPolicy() {
        envVars.set("NANODASH_SITE_SPACE", SITE);
        assertEquals(ELSEWHERE, NanodashLink.getPageUrl(ELSEWHERE));
        assertTrue(NanodashLink.getPageUrl(SITE + "/inside").startsWith("/explore?id="));
        assertTrue(NanodashLink.getPageUrl(NANOPUB).startsWith("/explore?id="));
        assertTrue(NanodashLink.getPageUrl(ORCID).startsWith("/user?id="));
    }

}
