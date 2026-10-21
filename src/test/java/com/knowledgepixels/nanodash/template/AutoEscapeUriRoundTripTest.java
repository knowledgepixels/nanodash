package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * An auto-escape placeholder must give back the IRI it was filled from, character for
 * character. {@link Utils#urlEncode} is not the inverse of {@link Utils#urlDecode} — both
 * "+" and "%20" decode to a space, and encoding only ever gives back "+" — so rebuilding
 * the IRI from the decoded text used to respell a value written with "%20", pointing the
 * form's links, and any superseding version, at a different resource: one nothing else
 * refers to, so its /explore page showed no references at all.
 */
class AutoEscapeUriRoundTripTest {

    // "Expressing a statement about research as an AIDA sentence": sub:aida is an
    // nt:AutoEscapeUriPlaceholder with nt:hasPrefix "http://purl.org/aida/".
    private static final String TEMPLATE = "https://w3id.org/np/RALmXhDw3rHcMveTgbv8VtWxijUHwnSqhCmtJFIPKWVaA";
    private static final IRI AIDA = Utils.vf.createIRI(TEMPLATE + "/aida");
    private static final String PREFIX = "http://purl.org/aida/";

    private static final String SENTENCE = "Between the 1951–1980 and 1981–2010 periods, Lisbon "
            + "experienced a summer temperature increase of 0.90 °C for daily maximums and 0.76 °C "
            + "for daily minimums.";
    // How https://w3id.org/sciencelive/np/RACxKJTud2NB0PIkVILlmE_fPqpjB9hwTiFDl1xe_eFwk spells it:
    // percent-encoded throughout, as encodeURIComponent would.
    private static final String SOURCE_ESCAPED = "Between%20the%201951%E2%80%931980%20and%201981%E2%80%932010"
            + "%20periods%2C%20Lisbon%20experienced%20a%20summer%20temperature%20increase%20of%200.90%20%C2%B0C"
            + "%20for%20daily%20maximums%20and%200.76%20%C2%B0C%20for%20daily%20minimums.";

    private static final String TARGET_NAMESPACE = "https://example.org/np/~~~ARTIFACTCODE~~~/";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void publishKeepsTheSpellingTheValueWasFilledFrom() {
        TemplateContext context = context();
        context.setAutoEscapeSource(AIDA, SOURCE_ESCAPED);
        context.getComponentModels().put(AIDA, Model.of(SENTENCE));
        String result = context.processValue(AIDA).stringValue();
        assertEquals(PREFIX + SOURCE_ESCAPED, result,
                "an unedited value must keep the exact IRI it was filled from");
        assertNotEquals(PREFIX + Utils.urlEncode(SENTENCE), result,
                "re-escaping would respell the IRI and point at a different resource");
    }

    @Test
    void editedTextIsEscapedAfresh() {
        TemplateContext context = context();
        context.setAutoEscapeSource(AIDA, SOURCE_ESCAPED);
        context.getComponentModels().put(AIDA, Model.of("A different sentence."));
        assertEquals(PREFIX + "A+different+sentence.", context.processValue(AIDA).stringValue(),
                "text the user changed must be escaped afresh, not kept at the source spelling");
    }

    @Test
    void aFreshValueUsesTheAidaConventionSpelling() {
        TemplateContext context = context();
        context.getComponentModels().put(AIDA, Model.of(SENTENCE));
        // No source to preserve: the AIDA spec's own spelling, with "+" for the spaces.
        assertEquals(PREFIX + Utils.urlEncode(SENTENCE), context.processValue(AIDA).stringValue(),
                "a newly typed sentence must still be escaped the way nanodash always has");
    }

    @Test
    void bothSpellingsDecodeAlikeButAreDifferentIris() {
        assertEquals(Utils.urlDecode(SOURCE_ESCAPED), Utils.urlDecode(Utils.urlEncode(SENTENCE)),
                "the two spellings decode to the same sentence, which is why the respelling went unnoticed");
        assertNotEquals(SOURCE_ESCAPED, Utils.urlEncode(SENTENCE),
                "but they are different strings, and so different IRIs");
    }

    private TemplateContext context() {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE, "statement", TARGET_NAMESPACE);
        context.initStatements();
        return context;
    }

}
