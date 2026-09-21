package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NPX;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class PostPublishRefreshTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/test";
    private static final String PART_ID = "https://w3id.org/np/RAppppppppppppppppppppppppppppppppppppppppppp/a-talk";
    private static final QueryRef PART_LOOKUP =
            new QueryRef("RAddddddddddddddddddddddddddddddddddddddddddd/get-term-definitions", "term", PART_ID);

    private static Nanopub withType(IRI type) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        creator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, type);
        return creator.finalizeNanopub();
    }

    private static Nanopub withAssertionPredicate(IRI predicate) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(vf.createIRI(SPACE_ID), predicate, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        return creator.finalizeNanopub();
    }

    @Test
    @DisplayName("a plain content publication does not change the page structure")
    void plainPublicationIsNotStructural() throws Exception {
        assertFalse(PostPublishRefresh.changesPageStructure(TestUtils.createNanopub(), SPACE_ID));
    }

    @Test
    @DisplayName("null nanopub does not change the page structure")
    void nullIsNotStructural() {
        assertFalse(PostPublishRefresh.changesPageStructure(null, SPACE_ID));
    }

    @Test
    @DisplayName("view-display publications change the page structure")
    void viewDisplayTypesAreStructural() throws Exception {
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.ACTIVATED_VIEW_DISPLAY), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.DEACTIVATED_VIEW_DISPLAY), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.VIEW_DISPLAY), SPACE_ID));
    }

    @Test
    @DisplayName("preset, space, resource and role-instantiation publications change the page structure")
    void otherStructuralTypes() throws Exception {
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.ACTIVATED_PRESET_ASSIGNMENT), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.SPACE), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.MAINTAINED_RESOURCE), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.ROLE_INSTANTIATION), SPACE_ID));
    }

    @Test
    @DisplayName("structural assertion predicates change the page structure")
    void structuralPredicates() throws Exception {
        assertTrue(PostPublishRefresh.changesPageStructure(withAssertionPredicate(KPXL_TERMS.IS_DISPLAY_OF_VIEW), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withAssertionPredicate(KPXL_TERMS.HAS_ADMIN_PREDICATE), SPACE_ID));
        assertTrue(PostPublishRefresh.changesPageStructure(withAssertionPredicate(KPXL_TERMS.IS_MAINTAINED_BY), SPACE_ID));
    }

    @Test
    @DisplayName("a retraction is treated as changing the page structure")
    void retractionIsStructural() throws Exception {
        assertTrue(PostPublishRefresh.changesPageStructure(withAssertionPredicate(NPX.RETRACTS), SPACE_ID));
    }

    @Test
    @DisplayName("an unrelated predicate on the context resource is not structural")
    void unrelatedPredicateIsNotStructural() throws Exception {
        assertFalse(PostPublishRefresh.changesPageStructure(
                withAssertionPredicate(vf.createIRI("http://purl.org/dc/terms/description")), SPACE_ID));
    }

    @Test
    @DisplayName("an unknown context is tolerated")
    void unknownContext() throws Exception {
        assertFalse(PostPublishRefresh.changesPageStructure(TestUtils.createNanopub(), null));
        assertFalse(PostPublishRefresh.changesPageStructure(TestUtils.createNanopub(), ""));
        assertTrue(PostPublishRefresh.changesPageStructure(withType(KPXL_TERMS.SPACE), null));
    }

    private static Nanopub introducing(String iri) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(vf.createIRI(iri), TestUtils.anyIri, TestUtils.anyIri);
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        creator.addPubinfoStatement(NPX.INTRODUCES, vf.createIRI(iri));
        return creator.finalizeNanopub();
    }

    @Test
    @DisplayName("a publication introducing the part being viewed invalidates the part's definition lookup")
    void introducingThePartRefreshesItsLookup() throws Exception {
        try (MockedStatic<AbstractResourceWithProfile> resources = mockStatic(AbstractResourceWithProfile.class);
             MockedStatic<ViewDataFetcher> fetcher = mockStatic(ViewDataFetcher.class)) {
            resources.when(() -> AbstractResourceWithProfile.get(SPACE_ID)).thenReturn(mock(AbstractResourceWithProfile.class));
            fetcher.when(() -> ViewDataFetcher.partDefinitionQueryRef(anyString(), anyString(), any())).thenReturn(PART_LOOKUP);

            assertEquals(PART_LOOKUP.getAsUrlString(),
                    PostPublishRefresh.partDefinitionRefreshTarget(introducing(PART_ID), PART_ID, SPACE_ID));
        }
    }

    @Test
    @DisplayName("a publication introducing something else leaves the part's definition lookup alone")
    void introducingSomethingElseRefreshesNothing() throws Exception {
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(
                introducing("https://w3id.org/np/RAppppppppppppppppppppppppppppppppppppppppppp/another-talk"),
                PART_ID, SPACE_ID));
    }

    @Test
    @DisplayName("no part in view, no part-definition lookup to invalidate")
    void noPartMeansNoRefresh() throws Exception {
        Nanopub np = introducing(PART_ID);
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(np, "", SPACE_ID));
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(np, null, SPACE_ID));
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(np, PART_ID, ""));
        // The resource's own page is not a part page.
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(np, SPACE_ID, SPACE_ID));
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(null, PART_ID, SPACE_ID));
    }

    @Test
    @DisplayName("an unknown context resolves to no lookup rather than failing")
    void unknownContextRefreshesNothing() throws Exception {
        assertNull(PostPublishRefresh.partDefinitionRefreshTarget(introducing(PART_ID), PART_ID, "https://example.org/not-a-resource"));
    }

}
