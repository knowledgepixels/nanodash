package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the non-derive introduction actions pre-fill their forms:
 * "Create Introduction" (recommendations view) and "retract" (introductions
 * table). The derive action is covered by {@link DeriveAppendKeyFillTest}.
 */
class IntroActionFillTest {

    private static final String INTRO_TEMPLATE = "https://w3id.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA";
    private static final String RETRACT_TEMPLATE = "https://w3id.org/np/RA0QOsYNphQCityVcDIJEuldhhuJOX3GlBLw6QylRBhEI";
    private static final String TARGET_NS = "https://example.org/np/~~~ARTIFACTCODE~~~/";
    private static final String USER_IRI = "https://orcid.org/0000-0002-1267-0234";
    private static final String LOCAL_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCwUtewGCpT5vIfXYE1bmf/Uqu1ojqnWdYxv+ySO80ul8Gu7m8KoyPAwuvaPj0lvPtHrg000qMmkxzKhYknEjq8v7EerxZNYp5B3/3+5ZpuWOYAs78UnQVjbHSmDdmryr4D4VvvNIiUmd0yxci47dTFUj4DvfHnGd6hVe5+goqdcwIDAQAB";
    private static final String SITE_URL = "http://localhost:37373/";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    private static boolean modelHasValue(TemplateContext context, String expected) {
        return context.getComponentModels().values().stream()
                .map(IModel::getObject)
                .filter(o -> o != null)
                .anyMatch(o -> expected.equals(o.toString()));
    }

    /** create-action: fresh form, base (non-indexed) key-bundle params + introduced user. */
    @Test
    void createIntroductionSeedsKeyBundle() {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, INTRO_TEMPLATE, "statement", TARGET_NS);
        context.setParam("user", USER_IRI);
        context.setParam("public-key", LOCAL_KEY);
        context.setParam("key-declaration", "M..UtewG..");
        context.setParam("key-declaration-ref", "M..UtewG..");
        context.setParam("key-location", SITE_URL);

        context.initStatements();
        context.finalizeStatements();

        assertTrue(modelHasValue(context, LOCAL_KEY), "public key should be seeded");
        assertTrue(modelHasValue(context, SITE_URL), "key location should be seeded");
        assertTrue(modelHasValue(context, USER_IRI), "introduced user should be seeded");
    }

    /** retract-action: single TrustyUriPlaceholder param. */
    @Test
    void retractSeedsTarget() {
        String npToRetract = "https://w3id.org/np/RAiBhLIsCpjGadbkCdmJffuM6Xi096bXrX233GOz4kVSo";
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, RETRACT_TEMPLATE, "statement", TARGET_NS);
        context.setParam("nanopubToBeRetracted", npToRetract);

        context.initStatements();
        context.finalizeStatements();

        assertTrue(modelHasValue(context, npToRetract), "retract target should be seeded");
    }
}
