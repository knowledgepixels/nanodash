package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
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

class NavigationContextTest {

    private static final IRI SPACE_IRI = vf.createIRI("https://example.com/space/my-space");
    private static final IRI OTHER_SPACE_IRI = vf.createIRI("https://example.com/space/other-space");
    private static final IRI RESOURCE_IRI = vf.createIRI("https://example.com/resource/my-resource");

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

}
