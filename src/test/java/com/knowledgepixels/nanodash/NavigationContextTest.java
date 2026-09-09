package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.page.ResourcePartPage;
import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationContextTest {

    private static final IRI SPACE_IRI = vf.createIRI("https://example.com/space/my-space");
    private static final IRI OTHER_SPACE_IRI = vf.createIRI("https://example.com/space/other-space");
    private static final IRI RESOURCE_IRI = vf.createIRI("https://example.com/resource/my-resource");
    private static final String PART_ID = "https://example.com/resource/my-resource/part/my-part";
    private static final String PART_LABEL = "My Part";

    private static NanopubCreator creatorWithType(IRI nanopubType) throws NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        TestUtils.fillProvenanceGraph(creator);
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.HAS_NANOPUB_TYPE, nanopubType));
        return creator;
    }

    @Test
    void declaredSpaceIdFromTypeTriple() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = creatorWithType(KPXL_TERMS.SPACE);
        creator.addAssertionStatement(vf.createStatement(SPACE_IRI, RDF.TYPE, KPXL_TERMS.SPACE));
        Nanopub np = creator.finalizeNanopub();
        assertEquals(SPACE_IRI.stringValue(), NavigationContext.getDeclaredSpaceId(np));
        assertNull(NavigationContext.getDeclaredMaintainedResourceId(np));
    }

    @Test
    void declaredSpaceIdFromRootDefinitionTriple() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // Definition update: the space IRI also appears as subject of gen:hasRootDefinition.
        NanopubCreator creator = creatorWithType(KPXL_TERMS.SPACE);
        creator.addAssertionStatement(vf.createStatement(SPACE_IRI, RDF.TYPE, KPXL_TERMS.SPACE));
        creator.addAssertionStatement(vf.createStatement(SPACE_IRI, KPXL_TERMS.HAS_ROOT_DEFINITION, TestUtils.anyIri));
        Nanopub np = creator.finalizeNanopub();
        assertEquals(SPACE_IRI.stringValue(), NavigationContext.getDeclaredSpaceId(np));
    }

    @Test
    void noSpaceIdWithoutSpaceNanopubType() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // The gen:Space assertion triple alone is not enough when the nanopub's type
        // set doesn't include gen:Space (multi-subject assertion, no npx:hasNanopubType):
        // nanopub-query would not ingest it as a space declaration.
        NanopubCreator creator = TestUtils.getNanopubCreator();
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        creator.addAssertionStatement(vf.createStatement(SPACE_IRI, RDF.TYPE, KPXL_TERMS.SPACE));
        creator.addAssertionStatement(vf.createStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri));
        Nanopub np = creator.finalizeNanopub();
        assertNull(NavigationContext.getDeclaredSpaceId(np));
    }

    @Test
    void noSpaceIdWhenAmbiguous() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = creatorWithType(KPXL_TERMS.SPACE);
        creator.addAssertionStatement(vf.createStatement(SPACE_IRI, RDF.TYPE, KPXL_TERMS.SPACE));
        creator.addAssertionStatement(vf.createStatement(OTHER_SPACE_IRI, RDF.TYPE, KPXL_TERMS.SPACE));
        Nanopub np = creator.finalizeNanopub();
        assertNull(NavigationContext.getDeclaredSpaceId(np));
    }

    @Test
    void noSpaceIdForUnrelatedNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        assertNull(NavigationContext.getDeclaredSpaceId(np));
        assertNull(NavigationContext.getDeclaredMaintainedResourceId(np));
    }

    @Test
    void declaredMaintainedResourceIdWithResourceType() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = creatorWithType(KPXL_TERMS.MAINTAINED_RESOURCE);
        creator.addAssertionStatement(vf.createStatement(RESOURCE_IRI, KPXL_TERMS.IS_MAINTAINED_BY, SPACE_IRI));
        Nanopub np = creator.finalizeNanopub();
        assertEquals(RESOURCE_IRI.stringValue(), NavigationContext.getDeclaredMaintainedResourceId(np));
        assertNull(NavigationContext.getDeclaredSpaceId(np));
    }

    @Test
    void declaredMaintainedResourceIdWithPredicateType() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // The alternative shape: the predicate marker gen:isMaintainedBy as nanopub type.
        NanopubCreator creator = creatorWithType(KPXL_TERMS.IS_MAINTAINED_BY);
        creator.addAssertionStatement(vf.createStatement(RESOURCE_IRI, KPXL_TERMS.IS_MAINTAINED_BY, SPACE_IRI));
        Nanopub np = creator.finalizeNanopub();
        assertEquals(RESOURCE_IRI.stringValue(), NavigationContext.getDeclaredMaintainedResourceId(np));
    }

    @Test
    void noMaintainedResourceIdWhenAmbiguous() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = creatorWithType(KPXL_TERMS.MAINTAINED_RESOURCE);
        creator.addAssertionStatement(vf.createStatement(RESOURCE_IRI, KPXL_TERMS.IS_MAINTAINED_BY, SPACE_IRI));
        creator.addAssertionStatement(vf.createStatement(TestUtils.anyIri, KPXL_TERMS.IS_MAINTAINED_BY, SPACE_IRI));
        Nanopub np = creator.finalizeNanopub();
        assertNull(NavigationContext.getDeclaredMaintainedResourceId(np));
    }

    private static PageParameters paramsWith(String id, String contextId) {
        PageParameters params = new PageParameters().set("id", id);
        if (contextId != null) params.set(NavigationContext.CONTEXT_PARAM, contextId);
        return params;
    }

    @Test
    void partIsCarriedAlongWithItsOwnContext() {
        PageParameters params = paramsWith("https://example.com/somewhere-else", RESOURCE_IRI.stringValue());
        NavigationContext.withPart(params, PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertEquals(PART_ID, params.get(NavigationContext.PART_PARAM).toString());
        assertEquals(PART_LABEL, params.get(NavigationContext.PART_LABEL_PARAM).toString());
    }

    @Test
    void partIsNotCarriedUnderADifferentContext() {
        // A link the caller pointed at another context: the part means nothing there.
        PageParameters params = paramsWith("https://example.com/somewhere-else", SPACE_IRI.stringValue());
        NavigationContext.withPart(params, PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertTrue(params.get(NavigationContext.PART_PARAM).isEmpty());
        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

    @Test
    void partIsNotCarriedOnALinkToItself() {
        PageParameters params = paramsWith(PART_ID, RESOURCE_IRI.stringValue());
        NavigationContext.withPart(params, PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertTrue(params.get(NavigationContext.PART_PARAM).isEmpty());
    }

    @Test
    void partIsNotCarriedUpToItsMaintainingResource() {
        // Stepping up to the maintaining resource leaves the part behind.
        PageParameters params = paramsWith(RESOURCE_IRI.stringValue(), RESOURCE_IRI.stringValue());
        NavigationContext.withPart(params, PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertTrue(params.get(NavigationContext.PART_PARAM).isEmpty());
    }

    @Test
    void explicitPartStaysAuthoritative() {
        PageParameters params = paramsWith("https://example.com/somewhere-else", RESOURCE_IRI.stringValue())
                .set(NavigationContext.PART_PARAM, "https://example.com/resource/my-resource/part/other-part");
        NavigationContext.withPart(params, PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertEquals("https://example.com/resource/my-resource/part/other-part", params.get(NavigationContext.PART_PARAM).toString());
        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

    @Test
    void partPageRefCarriesItsContext() {
        NanodashPageRef ref = NavigationContext.getPartPageRef(PART_ID, PART_LABEL, RESOURCE_IRI.stringValue());
        assertSame(ResourcePartPage.class, ref.getPageClass());
        assertEquals(PART_ID, ref.getParameters().get("id").toString());
        assertEquals(RESOURCE_IRI.stringValue(), ref.getParameters().get(NavigationContext.CONTEXT_PARAM).toString());
        assertEquals(PART_LABEL, ref.getParameters().get("label").toString());
        assertEquals(PART_LABEL, ref.getLabel());
    }

    @Test
    void partPageRefFallsBackToTheShortName() {
        NanodashPageRef ref = NavigationContext.getPartPageRef(PART_ID, null, RESOURCE_IRI.stringValue());
        assertEquals(Utils.getShortNameFromURI(PART_ID), ref.getLabel());
        assertTrue(ref.getParameters().get("label").isEmpty());
    }

    @Test
    void noPartPageRefWithoutAContext() {
        // A part page cannot resolve itself without its maintaining resource.
        assertNull(NavigationContext.getPartPageRef(PART_ID, PART_LABEL, null));
        assertNull(NavigationContext.getPartPageRef(null, null, RESOURCE_IRI.stringValue()));
    }

}
