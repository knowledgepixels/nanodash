package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Publish-path tests for language-tag-selectable literal placeholders
 * (docs/language-tag-picker.md): the tag chosen in the language model is
 * attached (normalized) to the published literal; a missing tag never
 * produces an untagged literal.
 */
public class LanguageTagPublishTest {

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
     * Builds a template with one optional statement (subject rdfs:comment [comment])
     * where the comment placeholder carries the given types, and returns an
     * initialized context for it.
     */
    private TemplateContext contextWith(boolean selectable, String fixedOrDefaultTag) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Language tag test template"));
        // Constant statement st0 keeps the assertion graph non-empty when the
        // optional statement st1 is dropped:
        IRI st0 = vf.createIRI(NP_URI + "/st0");
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st0);
        creator.addAssertionStatement(st0, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(st0, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(st0, RDF.OBJECT, vf.createIRI("http://example.com/SomeClass"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
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
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @SuppressWarnings("unchecked")
    private void setText(TemplateContext context, String value) {
        ((IModel<Object>) context.getComponentModels().get(COMMENT)).setObject(value);
    }

    private void setLang(TemplateContext context, String tag) {
        context.getComponentModels().put(TemplateContext.getLanguageModelKey(COMMENT), Model.of(tag));
    }

    private Nanopub publish(TemplateContext context) throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/result/");
        context.propagateStatements(creator);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static Literal commentLiteral(Nanopub np) {
        for (Statement st : np.getAssertion()) {
            if (st.getPredicate().equals(RDFS.COMMENT) && st.getObject() instanceof Literal l) {
                return l;
            }
        }
        return null;
    }

    @Test
    void chosenTagIsPublished() throws Exception {
        TemplateContext context = contextWith(true, null);
        setText(context, "Haus");
        setLang(context, "de");
        Literal l = commentLiteral(publish(context));
        assertNotNull(l);
        assertEquals("Haus", l.stringValue());
        assertEquals("de", l.getLanguage().orElse(null));
    }

    @Test
    void chosenTagIsNormalized() throws Exception {
        TemplateContext context = contextWith(true, null);
        setText(context, "hello");
        setLang(context, "EN-gb");
        Literal l = commentLiteral(publish(context));
        assertNotNull(l);
        assertEquals("en-GB", l.getLanguage().orElse(null));
    }

    @Test
    void textWithoutTagIsNeverPublishedUntagged() throws Exception {
        TemplateContext context = contextWith(true, null);
        setText(context, "Haus");
        // no language model at all
        assertNull(commentLiteral(publish(context)), "optional statement with unresolved object must be dropped");
    }

    @Test
    void emptyTagModelIsNeverPublishedUntagged() throws Exception {
        TemplateContext context = contextWith(true, null);
        setText(context, "Haus");
        setLang(context, "");
        assertNull(commentLiteral(publish(context)));
    }

    @Test
    void tagWithoutTextPublishesNothing() throws Exception {
        TemplateContext context = contextWith(true, null);
        setLang(context, "de");
        assertNull(commentLiteral(publish(context)));
    }

    @Test
    void preselectedDefaultTagApplies() throws Exception {
        // The default (nt:hasLanguageTag on a selectable placeholder) pre-fills the
        // language model when the component is built, so an untouched dropdown
        // publishes the default tag.
        TemplateContext context = contextWith(true, "en");
        setText(context, "Haus");
        Literal l = commentLiteral(publish(context));
        assertNotNull(l);
        assertEquals("en", l.getLanguage().orElse(null));
    }

    @Test
    void fixedTagPlaceholderIsUnchanged() throws Exception {
        TemplateContext context = contextWith(false, "en");
        setText(context, "house");
        Literal l = commentLiteral(publish(context));
        assertNotNull(l);
        assertEquals("house", l.stringValue());
        assertEquals("en", l.getLanguage().orElse(null));
    }

    @Test
    void plainLiteralPlaceholderIsUnchanged() throws Exception {
        TemplateContext context = contextWith(false, null);
        setText(context, "house");
        Literal l = commentLiteral(publish(context));
        assertNotNull(l);
        assertFalse(l.getLanguage().isPresent());
    }

}
