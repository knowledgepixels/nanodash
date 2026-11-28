package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Overriding (and superseding) must keep the source's introduced-resource IRI, as
 * documented in docs/fill-modes.md. The pinning in Nanodash's own way of making the
 * nanopublication used to sit inside the {@code isLocalResource} branch, and
 * {@code LOCAL_RESOURCE} is only ever attached to sub-IRIs of the template nanopub. A template
 * whose introduced resource is an absolute IRI in a foreign namespace carrying the
 * {@code ~~ARTIFACTCODE~~} marker -- the idiom used by "Defining a biochementity" -- therefore
 * fell through to the catch-all branch, kept the marker, and had a fresh artifact code
 * substituted at signing time. The override then minted a brand-new term instead of
 * publishing a variant of the existing one.
 * <p>
 * The form is filled from the real source and the nanopublication is made by the filler of
 * nanopub-java, as the publishing form does (#936).
 */
class OverrideIntroducedResourceIriTest {

    // "Defining a biochementity" -- introduced resource is
    // https://w3id.org/peh/biochementities/~~ARTIFACTCODE~~ , not a template sub-IRI.
    private static final String TEMPLATE = "https://w3id.org/np/RAD4mKOVqsJc7nAVVR0dXXcVFU2IOjrFywv2GspkpalfQ";
    // The marker as the nanopublication has it before it is signed.
    private static final String MARKER_IRI = "https://w3id.org/peh/biochementities/~~~ARTIFACTCODE~~~";
    // The resource being overridden, and the nanopublication that introduces it (made with an
    // earlier version of the template).
    private static final String SOURCE_IRI =
            "https://w3id.org/peh/biochementities/RABgnhJuxuXZpUkUQVoi2femjGVEm2Ij-tRFFkwKgJ-AA";
    private static final String SOURCE_NANOPUB = "https://w3id.org/np/RABgnhJuxuXZpUkUQVoi2femjGVEm2Ij-tRFFkwKgJ-AA";

    private static final String TARGET_NAMESPACE = "https://example.org/np/~~~ARTIFACTCODE~~~/";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void overrideKeepsIntroducedResourceIri() throws Exception {
        assertEquals(Set.of(SOURCE_IRI), NanopubUtils.getIntroducedIriIds(madeFromSource(FillMode.OVERRIDE)),
                "override must keep the source's introduced-resource IRI, not re-mint it");
    }

    @Test
    void supersedeKeepsIntroducedResourceIri() throws Exception {
        assertEquals(Set.of(SOURCE_IRI), NanopubUtils.getIntroducedIriIds(madeFromSource(FillMode.SUPERSEDE)),
                "supersede must keep the source's introduced-resource IRI, not re-mint it");
    }

    @Test
    void derivingMintsFromTheArtifactCodeMarker() throws Exception {
        // A new resource: the marker stays, so that the signer substitutes this nanopub's own
        // artifact code.
        assertEquals(Set.of(MARKER_IRI), NanopubUtils.getIntroducedIriIds(madeFromSource(FillMode.DERIVE)),
                "a derived nanopub must mint the term IRI from the artifact-code marker");
    }

    // The form filled from the source in the given mode, and the nanopublication it makes.
    private static Nanopub madeFromSource(FillMode fillMode) throws Exception {
        Nanopub source = Utils.getNanopub(SOURCE_NANOPUB);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE, "statement", TARGET_NAMESPACE);
        context.setFillMode(fillMode);
        context.setFillSource(source);
        new SourceFill(source, fillMode, null).fillAssertion(context);
        context.initStatements();
        context.finalizeStatements();
        return TestFill.fill(context);
    }

}
