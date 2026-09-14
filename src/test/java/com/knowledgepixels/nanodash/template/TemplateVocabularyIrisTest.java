package com.knowledgepixels.nanodash.template;

import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The template terms Nanodash reads are the ones already published in templates out in the
 * network, so their IRIs are a fixed contract rather than an implementation detail of
 * whichever library declares them. These were spelled out in {@link Template} until
 * nanopub-java gained them; pinning the strings here keeps a rename upstream from quietly
 * turning every template that uses them into one Nanodash no longer recognises.
 */
class TemplateVocabularyIrisTest {

    @Test
    void theTermsNanodashReadsKeepTheirPublishedIris() {
        assertEquals("https://w3id.org/np/o/ntemplate/AdvancedStatement",
                NTEMPLATE.ADVANCED_STATEMENT.stringValue());
        assertEquals("https://w3id.org/np/o/ntemplate/LanguageTaggedLiteralPlaceholder",
                NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER.stringValue());
        assertEquals("https://w3id.org/np/o/ntemplate/possibleLanguageTag",
                NTEMPLATE.POSSIBLE_LANGUAGE_TAG.stringValue());
    }

    // Still declared in Template, pending the nanopub-java release that carries it.
    @Test
    void theTransientTemplateTypeKeepsItsPublishedIri() {
        assertEquals("https://w3id.org/np/o/ntemplate/TransientTemplate",
                Template.TRANSIENT_TEMPLATE.stringValue());
    }

}
