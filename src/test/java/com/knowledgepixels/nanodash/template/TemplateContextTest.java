package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import static org.junit.jupiter.api.Assertions.*;

class TemplateContextTest {

    // Template: "Testing root nanopub placeholder" — has sub:rootNanopub typed as nt:RootNanopubPlaceholder.
    private static final String rootNpPlaceholderTemplateUri = "https://w3id.org/np/RAcws59tX-7nxdPpgl6FhRNq5KLIwJBJSZsHilqmOyT8Q";
    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI rootSlot = vf.createIRI(rootNpPlaceholderTemplateUri + "/rootNanopub");

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void fillSourceAccessorsAndReference() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, rootNpPlaceholderTemplateUri, "statement", (String) null);
        assertNull(context.getFillSource());
        assertNull(context.getExistingNanopub());
        assertNull(context.getReferenceNanopub());

        Nanopub np = TestUtils.createNanopub();
        context.setFillSource(np);
        assertSame(np, context.getFillSource());
        // existingNanopub still null, so isReadOnly stays false
        assertFalse(context.isReadOnly());
        // getReferenceNanopub falls back to fillSource
        assertSame(np, context.getReferenceNanopub());

        context.setFillSource(null);
        assertNull(context.getReferenceNanopub());
    }

    @Test
    void processValueRootNanopubFreshPublishResolvesToTargetNamespace() {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, rootNpPlaceholderTemplateUri, "statement", targetNamespace);
        // Simulate the ReadonlyItem seed for fresh publish.
        context.getComponentModels().put(rootSlot, Model.of(LocalUri.of("nanopub").stringValue()));

        Value result = context.processValue(rootSlot);
        assertTrue(result instanceof IRI);
        assertEquals(targetNamespace, result.stringValue());
    }

    @Test
    void processValueRootNanopubSupersedeResolvesToFillSourceUri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, rootNpPlaceholderTemplateUri, "statement", targetNamespace);
        context.setFillMode(FillMode.SUPERSEDE);
        Nanopub fillNp = TestUtils.createNanopub();
        context.setFillSource(fillNp);
        // The sentinel represents "the nanopub being filled from was the root".
        context.getComponentModels().put(rootSlot, Model.of(LocalUri.of("nanopub").stringValue()));

        Value result = context.processValue(rootSlot);
        assertTrue(result instanceof IRI);
        assertEquals(fillNp.getUri().stringValue(), result.stringValue());
    }

    @Test
    void processValueRootNanopubKeepsExplicitPriorValue() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, rootNpPlaceholderTemplateUri, "statement", targetNamespace);
        context.setFillMode(FillMode.SUPERSEDE);
        context.setFillSource(TestUtils.createNanopub());
        String priorRoot = "https://w3id.org/np/RAoriginalRootXYZ";
        context.getComponentModels().put(rootSlot, Model.of(priorRoot));

        Value result = context.processValue(rootSlot);
        assertTrue(result instanceof IRI);
        assertEquals(priorRoot, result.stringValue());
    }

    @Test
    void processValueRootNanopubEmptyModelDefaultsToTargetNamespace() {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, rootNpPlaceholderTemplateUri, "statement", targetNamespace);
        // No componentModels entry for the slot (simulates supersede with no matching statement).

        Value result = context.processValue(rootSlot);
        assertTrue(result instanceof IRI);
        assertEquals(targetNamespace, result.stringValue());
    }

}
