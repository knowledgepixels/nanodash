package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for issue #73: a "param_..." of the link that opened the form is applied before the form
 * is filled from the nanopublication to supersede, override, derive from or use. A parameter
 * value that differs from the source's used to make the statement it appears in fail to unify,
 * which left the rest of that statement, and the other members of its repetition group, empty and
 * reported as content that "could not be filled in". The parameter now overrides the source's
 * value instead of blocking the match.
 */
class ParamOverridesFillTest {

    private static final String SPACE_TEMPLATE = "https://w3id.org/np/RA6UoGXuBYW2keIaiV6ooNEUerTjrcb3PUJzcWh9ROgH4";
    private static final String SPACE_NANOPUB = "https://w3id.org/np/RAs8iN3vfYMHONchZ5G5mzcFKcbq8XcrX-Ap4-dbai5io";

    private static final String INTRO_TEMPLATE = "https://w3id.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA";
    private static final String INTRO_NANOPUB = "https://w3id.org/np/RAiBhLIsCpjGadbkCdmJffuM6Xi096bXrX233GOz4kVSo";

    private static final String OWN_LABEL = "SWIB26, renamed by the link that opened the form";
    private static final String OWN_KEY_LOCATION = "https://example.org/own-key-location/";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    private static ValueFiller fill(TemplateContext context, String sourceNanopubId) {
        Nanopub sourceNanopub = Utils.getNanopub(sourceNanopubId);
        assertNotNull(sourceNanopub, "source nanopublication should be fetchable");
        ValueFiller filler = new ValueFiller(sourceNanopub, ContextType.ASSERTION, true, context.getFillMode());
        filler.fill(context);
        context.finalizeStatements();
        return filler;
    }

    private static TemplateContext supersedingContext(String templateId, Map<String, String> params) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, templateId, "statement", (String) null);
        context.setFillMode(FillMode.SUPERSEDE);
        params.forEach(context::setParam);
        context.initStatements();
        return context;
    }

    private static String valueOf(TemplateContext context, String placeholderName) {
        for (Map.Entry<IRI, IModel<?>> entry : context.getComponentModels().entrySet()) {
            if (Utils.getUriPostfix(entry.getKey()).equals(placeholderName)) {
                Object value = entry.getValue().getObject();
                return value == null ? null : value.toString();
            }
        }
        return null;
    }

    @Test
    void parameterOverridesTheSupersededValue() {
        TemplateContext context = supersedingContext(SPACE_TEMPLATE, Map.of("label", OWN_LABEL));
        fill(context, SPACE_NANOPUB);

        assertEquals(OWN_LABEL, valueOf(context, "label"));
    }

    @Test
    void theRestOfTheSupersededNanopubIsStillFilledIn() {
        TemplateContext context = supersedingContext(SPACE_TEMPLATE, Map.of("label", OWN_LABEL));
        fill(context, SPACE_NANOPUB);

        assertEquals("swib/2026", valueOf(context, "space"));
        assertEquals("https://forum.swib.org/t/swib26-welcome", valueOf(context, "altId"));
        assertTrue(valueOf(context, "description").startsWith("SWIB26 is the 18th annual"));
    }

    @Test
    void theOverriddenStatementIsNotReportedAsUnfilled() {
        ValueFiller withoutParam = fill(supersedingContext(SPACE_TEMPLATE, Map.of()), SPACE_NANOPUB);
        ValueFiller withParam = fill(supersedingContext(SPACE_TEMPLATE, Map.of("label", OWN_LABEL)), SPACE_NANOPUB);

        assertEquals(withoutParam.getUnusedStatements().size(), withParam.getUnusedStatements().size(),
                "overriding a value must not leave the statement it replaces reported as unfilled");
    }

    @Test
    void parameterOverridesOneRepetitionWithoutEmptyingTheOthers() {
        TemplateContext context = supersedingContext(INTRO_TEMPLATE, Map.of("key-location", OWN_KEY_LOCATION));
        ValueFiller filler = fill(context, INTRO_NANOPUB);

        assertEquals(OWN_KEY_LOCATION, valueOf(context, "key-location"));
        assertEquals("https://nanodash.petapico.org/", valueOf(context, "key-location__1"));
        assertEquals("M..+Vopf..", valueOf(context, "key-declaration-ref"));
        assertEquals("M..A5FgU..", valueOf(context, "key-declaration-ref__1"));
        assertTrue(filler.getUnusedStatements().isEmpty(),
                "every statement of the superseded nanopublication should still be accounted for");
    }
}
