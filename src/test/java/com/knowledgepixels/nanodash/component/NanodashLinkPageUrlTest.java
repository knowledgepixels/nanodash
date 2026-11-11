package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Which values {@link NanodashLink#getPageUrl(String)} makes a link for. It must accept what
 * the Explore page accepts: tightening it on its own has twice left the two disagreeing.
 */
class NanodashLinkPageUrlTest {

    private static final String CODE = "RA" + "a".repeat(43);
    private static final String DID = "did:plc:z72i7hdynmk6r22z27h6tvur";

    private MockedStatic<IndividualAgent> agentMock;
    private MockedStatic<SpaceRepository> spaceRepoMock;
    private MockedStatic<MaintainedResourceRepository> resourceRepoMock;

    @BeforeEach
    void setUp() {
        // Knowing the users, spaces and maintained resources means going over the network.
        // Here nothing is any of them, so every value falls through to the Explore page.
        agentMock = mockStatic(IndividualAgent.class, CALLS_REAL_METHODS);
        agentMock.when(() -> IndividualAgent.isUser(anyString())).thenReturn(false);
        SpaceRepository spaces = mock(SpaceRepository.class);
        spaceRepoMock = mockStatic(SpaceRepository.class);
        spaceRepoMock.when(SpaceRepository::get).thenReturn(spaces);
        MaintainedResourceRepository resources = mock(MaintainedResourceRepository.class);
        resourceRepoMock = mockStatic(MaintainedResourceRepository.class);
        resourceRepoMock.when(MaintainedResourceRepository::get).thenReturn(resources);
    }

    @AfterEach
    void tearDown() {
        resourceRepoMock.close();
        spaceRepoMock.close();
        agentMock.close();
    }

    @Test
    void aNanopubSubIriInShortFormIsExplored() {
        // The form w3id.org forwards https://w3id.org/np/RA…/declaration in (issue #780).
        assertEquals(exploreUrl(CODE + "/declaration"), NanodashLink.getPageUrl(CODE + "/declaration"));
        assertEquals(exploreUrl(CODE), NanodashLink.getPageUrl(CODE));
    }

    @Test
    void aFullIriIsExplored() {
        String iri = "https://w3id.org/np/" + CODE + "/declaration";
        assertEquals(exploreUrl(iri), NanodashLink.getPageUrl(iri));
    }

    @Test
    void aDidGoesToItsResolver() {
        // A DID has no "://", so it must reach the resolver before the explorable-id check.
        assertEquals("https://dev.uniresolver.io/1.0/identifiers/" + DID, NanodashLink.getPageUrl(DID));
    }

    @Test
    void whatTheExplorePageRefusesGetsNoLink() {
        assertEquals("", NanodashLink.getPageUrl("download-rdf"));
        assertEquals("", NanodashLink.getPageUrl("RAtooShort/declaration"));
        assertEquals("", NanodashLink.getPageUrl(null));
    }

    private static String exploreUrl(String id) {
        return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

}
