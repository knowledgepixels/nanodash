package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Overriding (and superseding) must keep the source's introduced-resource IRI, as
 * documented in docs/fill-modes.md. The pinning in
 * {@link TemplateContext#processValue} used to sit inside the
 * {@code isLocalResource} branch, and {@code LOCAL_RESOURCE} is only ever attached
 * to sub-IRIs of the template nanopub. A template whose introduced resource is an
 * absolute IRI in a foreign namespace carrying the {@code ~~ARTIFACTCODE~~} marker
 * — the idiom used by "Defining a biochementity" — therefore fell through to the
 * catch-all branch, kept the marker, and had a fresh artifact code substituted at
 * signing time. The override then minted a brand-new term instead of publishing a
 * variant of the existing one.
 */
class OverrideIntroducedResourceIriTest {

    // "Defining a biochementity" — introduced resource is
    // https://w3id.org/peh/biochementities/~~ARTIFACTCODE~~ , not a template sub-IRI.
    private static final String TEMPLATE = "https://w3id.org/np/RAD4mKOVqsJc7nAVVR0dXXcVFU2IOjrFywv2GspkpalfQ";
    // What RepetitionGroup.transform hands to processValue (see StatementItem#transform).
    private static final IRI RENDERED_INTRODUCED =
            Utils.vf.createIRI("https://w3id.org/peh/biochementities/~~~ARTIFACTCODE~~~");
    // The concrete IRI of the resource being overridden, as ValueFiller leaves it in
    // the (read-only) component model on supersede/override.
    private static final String SOURCE_IRI =
            "https://w3id.org/peh/biochementities/RABgnhJuxuXZpUkUQVoi2femjGVEm2Ij-tRFFkwKgJ-AA";

    private static final String TARGET_NAMESPACE = "https://example.org/np/~~~ARTIFACTCODE~~~/";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void overrideKeepsIntroducedResourceIri() {
        Value result = processIntroducedResource(FillMode.OVERRIDE);
        assertEquals(SOURCE_IRI, result.stringValue(),
                "override must keep the source's introduced-resource IRI, not re-mint it");
    }

    @Test
    void supersedeKeepsIntroducedResourceIri() {
        Value result = processIntroducedResource(FillMode.SUPERSEDE);
        assertEquals(SOURCE_IRI, result.stringValue(),
                "supersede must keep the source's introduced-resource IRI, not re-mint it");
    }

    @Test
    void freshPublishStillMintsFromTheArtifactCodeMarker() {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE, "statement", TARGET_NAMESPACE);
        context.initStatements();
        // No fill mode and no seeded model: a fresh publish must keep the marker so the
        // signer substitutes this nanopub's own artifact code.
        Value result = context.processValue(RENDERED_INTRODUCED);
        assertEquals(RENDERED_INTRODUCED.stringValue(), result.stringValue(),
                "a fresh publish must still mint the term IRI from the artifact-code marker");
    }

    private Value processIntroducedResource(FillMode fillMode) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE, "statement", TARGET_NAMESPACE);
        context.setFillMode(fillMode);
        context.initStatements();
        context.getComponentModels().put(RENDERED_INTRODUCED, Model.of(SOURCE_IRI));
        return context.processValue(RENDERED_INTRODUCED);
    }

}
