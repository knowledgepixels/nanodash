package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;

import java.util.List;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A publication aimed at a space or maintained resource refreshes what it can have changed,
 * and nothing else (#358).
 */
class PostPublishRefreshScopeTest {

    private static final long WAIT = 5000;
    private static final String RETRACTED_ID = "http://example.org/np/retracted";

    private final Space space = mock(Space.class);
    private final MaintainedResource resourceA = mock(MaintainedResource.class);
    private final MaintainedResource resourceB = mock(MaintainedResource.class);
    private final SpaceRepository spaces = mock(SpaceRepository.class);
    private final MaintainedResourceRepository maintainedResources = mock(MaintainedResourceRepository.class);

    private MockedStatic<SpaceRepository> spaceRepository;
    private MockedStatic<MaintainedResourceRepository> maintainedResourceRepository;
    private MockedStatic<Utils> utils;

    @BeforeEach
    void setUp() {
        when(space.getSpace()).thenReturn(space);
        when(resourceA.getSpace()).thenReturn(space);
        when(resourceB.getSpace()).thenReturn(space);
        when(maintainedResources.findResourcesBySpace(space)).thenReturn(List.of(resourceA, resourceB));
        spaceRepository = mockStatic(SpaceRepository.class);
        spaceRepository.when(SpaceRepository::get).thenReturn(spaces);
        maintainedResourceRepository = mockStatic(MaintainedResourceRepository.class);
        maintainedResourceRepository.when(MaintainedResourceRepository::get).thenReturn(maintainedResources);
        utils = mockStatic(Utils.class, Answers.CALLS_REAL_METHODS);
        utils.when(() -> Utils.getAsNanopub(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        utils.close();
        maintainedResourceRepository.close();
        spaceRepository.close();
    }

    private static Nanopub withType(IRI type) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, type);
        return creator.finalizeNanopub();
    }

    private static Nanopub withAssertionPredicate(IRI predicate) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, predicate, vf.createIRI(RETRACTED_ID));
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        return creator.finalizeNanopub();
    }

    private void verifyListingsRefreshed(boolean spaceListing, boolean maintainedResourceListing) {
        verify(spaces, spaceListing ? times(1) : never()).forceRootRefresh(anyLong());
        verify(maintainedResources, maintainedResourceListing ? times(1) : never()).forceRootRefresh(anyLong());
    }

    @Test
    @DisplayName("a view display refreshes the space it is for, and nothing else")
    void viewDisplayRefreshesOnlyTheSpace() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withType(KPXL_TERMS.VIEW_DISPLAY), space, WAIT);
        verify(space).requestViewDefinitionRefresh();
        verify(space).forceRefresh(WAIT);
        verifyListingsRefreshed(false, false);
        verify(resourceA, never()).forceRefresh(anyLong());
        verify(resourceB, never()).forceRefresh(anyLong());
    }

    @Test
    @DisplayName("a role assignment also refreshes the listings and the resources the space maintains")
    void roleInstantiationRefreshesTheSpacesResources() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withType(KPXL_TERMS.ROLE_INSTANTIATION), space, WAIT);
        verify(space).forceRefresh(WAIT);
        verifyListingsRefreshed(true, true);
        verify(resourceA).forceRefresh(WAIT);
        verify(resourceB).forceRefresh(WAIT);
    }

    @Test
    @DisplayName("a role assigned with the space's own role predicate counts as a role change")
    void spaceRolePredicateCountsAsRoleChange() throws Exception {
        IRI customRolePredicate = vf.createIRI("https://example.org/isCuratorOf");
        SpaceMemberRole role = mock(SpaceMemberRole.class);
        when(role.getRegularProperties()).thenReturn(new IRI[]{customRolePredicate});
        when(role.getInverseProperties()).thenReturn(new IRI[0]);
        SpaceMemberRoleRef roleRef = mock(SpaceMemberRoleRef.class);
        when(roleRef.getRole()).thenReturn(role);
        when(space.getRoles()).thenReturn(List.of(roleRef));

        PostPublishRefresh.refreshAfterPublication(withAssertionPredicate(customRolePredicate), space, WAIT);
        verify(resourceA).forceRefresh(WAIT);
        verify(resourceB).forceRefresh(WAIT);
    }

    @Test
    @DisplayName("a maintained-resource declaration refreshes only the maintained-resource listing")
    void maintainedResourceDeclarationRefreshesItsListing() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withType(KPXL_TERMS.MAINTAINED_RESOURCE), space, WAIT);
        verifyListingsRefreshed(false, true);
        verify(resourceA, never()).forceRefresh(anyLong());
    }

    @Test
    @DisplayName("a sub-space declaration refreshes only the space listing")
    void subSpaceDeclarationRefreshesTheSpaceListing() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withAssertionPredicate(KPXL_TERMS.IS_SUB_SPACE_OF), space, WAIT);
        verifyListingsRefreshed(true, false);
        verify(resourceA, never()).forceRefresh(anyLong());
    }

    @Test
    @DisplayName("a publication aimed at a maintained resource does not refresh it twice")
    void aimedResourceIsNotRefreshedAgain() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withType(KPXL_TERMS.ROLE_INSTANTIATION), resourceA, WAIT);
        verify(resourceA).forceRefresh(WAIT);
        verify(resourceB).forceRefresh(WAIT);
        verify(space, never()).forceRefresh(anyLong());
    }

    @Test
    @DisplayName("retracting a view display refreshes what the view display would have")
    void retractingAViewDisplayRefreshesOnlyTheSpace() throws Exception {
        Nanopub viewDisplay = withType(KPXL_TERMS.VIEW_DISPLAY);
        utils.when(() -> Utils.getAsNanopub(RETRACTED_ID)).thenReturn(viewDisplay);
        PostPublishRefresh.refreshAfterPublication(withAssertionPredicate(NPX.RETRACTS), space, WAIT);
        verifyListingsRefreshed(false, false);
        verify(resourceA, never()).forceRefresh(anyLong());
    }

    @Test
    @DisplayName("retracting a nanopub that cannot be retrieved refreshes everything it could have changed")
    void retractingAnUnknownNanopubRefreshesEverything() throws Exception {
        PostPublishRefresh.refreshAfterPublication(withAssertionPredicate(NPX.RETRACTS), space, WAIT);
        verifyListingsRefreshed(true, true);
        verify(resourceA).forceRefresh(WAIT);
        verify(resourceB).forceRefresh(WAIT);
    }

}
