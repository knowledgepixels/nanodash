package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostPublishRefreshTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/test";

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

}
