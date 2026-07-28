package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for parsing language-tag-selectable literal placeholders
 * (nt:LanguageTaggedLiteralPlaceholder, nt:possibleLanguageTag).
 */
public class LanguageTagTemplateTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI COMMENT_PLACEHOLDER = vf.createIRI(NP_URI + "/comment");

    /**
     * Builds a template nanopub with one statement whose object is the comment
     * placeholder, typed with the given placeholder types. The returned creator
     * can take further statements before finalizing.
     */
    private static NanopubCreator newCreator(IRI... placeholderTypes) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st1, RDF.OBJECT, COMMENT_PLACEHOLDER);
        for (IRI type : placeholderTypes) {
            creator.addAssertionStatement(COMMENT_PLACEHOLDER, RDF.TYPE, type);
        }
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, RDFS.LABEL, vf.createLiteral("comment"));
        return creator;
    }

    @Test
    void selectablePlaceholderParses() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isLanguageTagSelectable(COMMENT_PLACEHOLDER));
        assertTrue(t.isLiteralPlaceholder(COMMENT_PLACEHOLDER));
        assertNull(t.getPossibleLanguageTags(COMMENT_PLACEHOLDER));
        assertNull(t.getLanguageTag(COMMENT_PLACEHOLDER));
    }

    @Test
    void possibleLanguageTagsAreNormalized() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, Template.POSSIBLE_LANGUAGE_TAG, vf.createLiteral("en"));
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, Template.POSSIBLE_LANGUAGE_TAG, vf.createLiteral("DE-de"));
        Template t = new Template(creator.finalizeNanopub());
        List<String> tags = t.getPossibleLanguageTags(COMMENT_PLACEHOLDER);
        assertEquals(2, tags.size());
        assertTrue(tags.contains("en"));
        assertTrue(tags.contains("de-DE"));
    }

    @Test
    void defaultLanguageTagParses() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, NTEMPLATE.HAS_LANGUAGE_TAG, vf.createLiteral("en"));
        Template t = new Template(creator.finalizeNanopub());
        assertEquals("en", t.getLanguageTag(COMMENT_PLACEHOLDER));
        assertTrue(t.isLanguageTagSelectable(COMMENT_PLACEHOLDER));
    }

    @Test
    void plainLiteralPlaceholderIsNotSelectable() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, NTEMPLATE.HAS_LANGUAGE_TAG, vf.createLiteral("en"));
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLanguageTagSelectable(COMMENT_PLACEHOLDER));
        assertEquals("en", t.getLanguageTag(COMMENT_PLACEHOLDER));
    }

    @Test
    void selectableTypeAloneIsLiteralPlaceholder() throws Exception {
        NanopubCreator creator = newCreator(Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isLiteralPlaceholder(COMMENT_PLACEHOLDER));
        assertTrue(t.isPlaceholder(COMMENT_PLACEHOLDER));
        assertTrue(t.isLanguageTagSelectable(COMMENT_PLACEHOLDER));
    }

    @Test
    void conflictingDatatypeIsDropped() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, NTEMPLATE.HAS_DATATYPE, XSD.DATE);
        Template t = new Template(creator.finalizeNanopub());
        assertNull(t.getDatatype(COMMENT_PLACEHOLDER));
    }

    @Test
    void datatypeIsKeptOnPlainPlaceholder() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, NTEMPLATE.HAS_DATATYPE, XSD.DATE);
        Template t = new Template(creator.finalizeNanopub());
        assertEquals(XSD.DATE, t.getDatatype(COMMENT_PLACEHOLDER));
    }

    @Test
    void repetitionSuffixedLookupWorks() throws Exception {
        NanopubCreator creator = newCreator(NTEMPLATE.LITERAL_PLACEHOLDER, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT_PLACEHOLDER, Template.POSSIBLE_LANGUAGE_TAG, vf.createLiteral("en"));
        Template t = new Template(creator.finalizeNanopub());
        IRI suffixed = vf.createIRI(COMMENT_PLACEHOLDER.stringValue() + "__1");
        assertTrue(t.isLanguageTagSelectable(suffixed));
        assertEquals(List.of("en"), t.getPossibleLanguageTags(suffixed));
    }

}
