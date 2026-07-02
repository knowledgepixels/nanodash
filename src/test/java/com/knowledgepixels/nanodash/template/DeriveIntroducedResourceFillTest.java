package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduction for issue #529: when deriving from a nanopub whose introduced
 * resource IRI embeds a concrete artifact code (e.g.
 * {@code .../biochementities/RAZdhuW9...}), the template's introduced-resource
 * IRI still carries the {@code ~~~ARTIFACTCODE~~~} marker. The introduced
 * resource is the subject of every assertion statement, so if the marker fails
 * to unify with the source's concrete code, none of the statements match and
 * every placeholder (name, group label, external ids, …) is left empty.
 */
class DeriveIntroducedResourceFillTest {

    // "Defining a biochementity" template — introduced resource IRI is
    // https://w3id.org/peh/biochementities/~~ARTIFACTCODE~~ .
    private static final String TEMPLATE = "https://w3id.org/np/RAD4mKOVqsJc7nAVVR0dXXcVFU2IOjrFywv2GspkpalfQ";
    // A published biochementity, used as the derive source.
    private static final String DERIVE_FROM = "https://w3id.org/np/RAZdhuW9ExtLpk8aVBcYyaaGO0mXbYGGER7kZDfqlH_qc";
    // A distinctive literal only reachable if the introduced-resource statements unify.
    private static final String GROUP_LABEL = "flame retardants";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void deriveFillsIntroducedResourceStatements() throws Exception {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE, "statement", targetNamespace);
        context.setFillMode(FillMode.DERIVE);
        context.initStatements();

        Nanopub deriveFrom = Utils.getNanopub(DERIVE_FROM);
        assertNotNull(deriveFrom, "derive-source nanopub should be fetchable");
        ValueFiller filler = new ValueFiller(deriveFrom, ContextType.ASSERTION, true, FillMode.DERIVE);
        filler.fill(context);

        context.finalizeStatements();

        boolean groupLabelPresent = context.getComponentModels().values().stream()
                .map(IModel::getObject)
                .filter(o -> o != null)
                .anyMatch(o -> GROUP_LABEL.equals(o.toString()));
        assertTrue(groupLabelPresent,
                "deriving should fill the introduced resource's statements into the form");
    }
}
