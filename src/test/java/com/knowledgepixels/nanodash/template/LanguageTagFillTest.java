package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Fill/unification tests for language-tag-selectable literal placeholders
 * (docs/language-tag-picker.md): any tag from the allowed set unifies, the
 * declared default is not a constraint, untagged literals never unify, and
 * fixed-tag placeholders keep their strict behavior.
 */
public class LanguageTagFillTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");

    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    /**
     * Builds a one-statement template (subject rdfs:comment [comment]) and registers
     * it with the mocked TemplateData.
     */
    private void mockTemplate(boolean selectable, String fixedOrDefaultTag, String... possibleTags) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Fill test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        if (selectable) {
            creator.addAssertionStatement(COMMENT, RDF.TYPE, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        }
        if (fixedOrDefaultTag != null) {
            creator.addAssertionStatement(COMMENT, NTEMPLATE.HAS_LANGUAGE_TAG, vf.createLiteral(fixedOrDefaultTag));
        }
        for (String tag : possibleTags) {
            creator.addAssertionStatement(COMMENT, Template.POSSIBLE_LANGUAGE_TAG, vf.createLiteral(tag));
        }
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
    }

    private static Nanopub dataNanopub(Value comment) throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/data/");
        creator.addAssertionStatement(vf.createStatement(SUBJECT, RDFS.COMMENT, comment));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    /**
     * Mirrors the viewer flow: read-only context (ReadonlyItem) filled from the data nanopub.
     */
    private TemplateContext fillReadOnly(Nanopub dataNp, ValueFiller filler) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", dataNp);
        context.initStatements();
        filler.fill(context);
        return context;
    }

    /**
     * Mirrors the form flow (derive/supersede): editable context (LiteralTextfieldItem).
     */
    private TemplateContext fillEditable(Nanopub dataNp) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        new ValueFiller(dataNp, ContextType.ASSERTION, true).fill(context);
        return context;
    }

    @Test
    void readOnlyUnifiesAnyTagWhenUnrestricted() throws Exception {
        mockTemplate(true, null);
        Nanopub dataNp = dataNanopub(vf.createLiteral("maison", "fr"));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fillReadOnly(dataNp, filler);
        assertTrue(context.getStatementItems().get(0).isMatched());
        assertTrue(filler.getUnusedStatements().isEmpty());
    }

    @Test
    void readOnlyRejectsTagOutsideRestrictedSet() throws Exception {
        mockTemplate(true, null, "en", "de");
        Nanopub dataNp = dataNanopub(vf.createLiteral("maison", "fr"));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fillReadOnly(dataNp, filler);
        assertFalse(context.getStatementItems().get(0).isMatched(),
                "a tag outside the restricted set must not unify");
        assertEquals(1, filler.getUnusedStatements().size());
    }

    @Test
    void readOnlyAcceptsTagInsideRestrictedSet() throws Exception {
        mockTemplate(true, null, "en", "de");
        Nanopub dataNp = dataNanopub(vf.createLiteral("Haus", "de"));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fillReadOnly(dataNp, filler);
        assertTrue(context.getStatementItems().get(0).isMatched());
        assertTrue(filler.getUnusedStatements().isEmpty());
    }

    @Test
    void readOnlyRejectsUntaggedLiteral() throws Exception {
        mockTemplate(true, null);
        Nanopub dataNp = dataNanopub(vf.createLiteral("maison"));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fillReadOnly(dataNp, filler);
        assertFalse(context.getStatementItems().get(0).isMatched(),
                "an untagged literal must not unify with a language-tagged placeholder");
    }

    @Test
    void defaultTagIsNotAConstraint() throws Exception {
        mockTemplate(true, "en");
        Nanopub dataNp = dataNanopub(vf.createLiteral("maison", "fr"));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fillReadOnly(dataNp, filler);
        assertTrue(context.getStatementItems().get(0).isMatched(),
                "the declared default must not block other tags");
    }

    @Test
    void editableFillSetsBothModelsAndRoundTrips() throws Exception {
        mockTemplate(true, null);
        TemplateContext context = fillEditable(dataNanopub(vf.createLiteral("maison", "fr")));
        assertEquals("maison", context.getComponentModels().get(COMMENT).getObject());
        IModel<?> langModel = context.getComponentModels().get(TemplateContext.getLanguageModelKey(COMMENT));
        assertEquals("fr", langModel.getObject(), "fill must set the language model");
        Value processed = context.processValue(COMMENT);
        assertTrue(processed instanceof Literal);
        assertEquals("fr", ((Literal) processed).getLanguage().orElse(null),
                "the filled tag must round-trip through processValue");
    }

    @Test
    void editableFillPreselectedDefaultIsOverwritten() throws Exception {
        mockTemplate(true, "en");
        TemplateContext context = fillEditable(dataNanopub(vf.createLiteral("maison", "fr")));
        IModel<?> langModel = context.getComponentModels().get(TemplateContext.getLanguageModelKey(COMMENT));
        assertEquals("fr", langModel.getObject(),
                "the value's tag must win over the pre-selected default");
    }

    @Test
    void fixedTagPlaceholderStaysStrict() throws Exception {
        mockTemplate(false, "en");
        TemplateContext context = fillEditable(dataNanopub(vf.createLiteral("maison", "fr")));
        // Pre-entered text simulates the situation where the fixed-tag check applies:
        assertEquals("maison", context.getComponentModels().get(COMMENT).getObject());
        Nanopub dataNp2 = dataNanopub(vf.createLiteral("house", "en"));
        ValueFiller filler = new ValueFiller(dataNp2, ContextType.ASSERTION, false);
        TemplateContext roContext = fillReadOnly(dataNp2, filler);
        assertTrue(roContext.getStatementItems().get(0).isMatched(),
                "matching fixed tag must still unify");
        Nanopub dataNp3 = dataNanopub(vf.createLiteral("maison", "fr"));
        ValueFiller filler3 = new ValueFiller(dataNp3, ContextType.ASSERTION, false);
        TemplateContext roContext3 = fillReadOnly(dataNp3, filler3);
        assertFalse(roContext3.getStatementItems().get(0).isMatched(),
                "non-matching fixed tag must still be rejected in read-only display");
    }

}
