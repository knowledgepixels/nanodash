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
 * Reproduction for the "derive new introduction" bug: when deriving from an
 * existing introduction, the relative {@code __.1} params (which add the local
 * key alongside the derived key declarations) created the extra repetition
 * group but left its fields empty. Drives the same sequence as
 * {@code PublishForm}: set params, initStatements, derive-fill, finalizeStatements.
 */
class DeriveAppendKeyFillTest {

    // Introduction template — key declarations are a repeatable grouped statement.
    private static final String INTRO_TEMPLATE = "https://w3id.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA";
    // A published introduction with several key declarations, used as derive source.
    private static final String DERIVE_FROM = "https://w3id.org/np/RAiBhLIsCpjGadbkCdmJffuM6Xi096bXrX233GOz4kVSo";
    private static final String LOCAL_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCwUtewGCpT5vIfXYE1bmf/Uqu1ojqnWdYxv+ySO80ul8Gu7m8KoyPAwuvaPj0lvPtHrg000qMmkxzKhYknEjq8v7EerxZNYp5B3/3+5ZpuWOYAs78UnQVjbHSmDdmryr4D4VvvNIiUmd0yxci47dTFUj4DvfHnGd6hVe5+goqdcwIDAQAB";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void deriveAppendsLocalKeyValue() throws Exception {
        String targetNamespace = "https://example.org/np/~~~ARTIFACTCODE~~~/";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, INTRO_TEMPLATE, "statement", targetNamespace);
        context.setFillMode(FillMode.DERIVE);

        // The relative __.1 params the derive button passes to declare the local
        // key alongside the derived nanopub's existing key declarations.
        context.setParam("public-key__.1", LOCAL_KEY);
        context.setParam("key-declaration__.1", "M..UtewG..");
        context.setParam("key-declaration-ref__.1", "M..UtewG..");
        context.setParam("key-location__.1", "http://localhost:37373/");

        context.initStatements();

        Nanopub deriveFrom = Utils.getNanopub(DERIVE_FROM);
        assertNotNull(deriveFrom, "derive-source nanopub should be fetchable");
        ValueFiller filler = new ValueFiller(deriveFrom, ContextType.ASSERTION, true, FillMode.DERIVE);
        filler.fill(context);

        context.finalizeStatements();

        boolean localKeyPresent = context.getComponentModels().values().stream()
                .map(IModel::getObject)
                .filter(o -> o != null)
                .anyMatch(o -> LOCAL_KEY.equals(o.toString()));
        assertTrue(localKeyPresent,
                "the appended key-declaration block should be seeded with the local public key");
    }
}
