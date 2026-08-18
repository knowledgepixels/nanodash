package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Regression tests for issue #564: a nt:LiteralPlaceholder with a datatype other
 * than xsd:date/xsd:dateTime (e.g. xsd:gYear) must produce a typed literal from
 * the entered text instead of silently dropping the value, which made non-optional
 * statements fail with "Field of statement not set." despite being filled in.
 */
public class LiteralPlaceholderDatatypeTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI THING = vf.createIRI(NP_URI + "/thing");
    private static final IRI VALUE = vf.createIRI(NP_URI + "/value");

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
     * Builds a one-statement template whose object is a nt:LiteralPlaceholder,
     * optionally with a datatype or language tag, and registers it with the
     * mocked TemplateData.
     */
    private void mockTemplate(IRI datatype, String languageTag) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Literal datatype test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, VALUE);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("thing"));
        creator.addAssertionStatement(VALUE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(VALUE, RDFS.LABEL, vf.createLiteral("value"));
        if (datatype != null) {
            creator.addAssertionStatement(VALUE, NTEMPLATE.HAS_DATATYPE, datatype);
        }
        if (languageTag != null) {
            creator.addAssertionStatement(VALUE, NTEMPLATE.HAS_LANGUAGE_TAG, vf.createLiteral(languageTag));
        }
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
    }

    private TemplateContext contextWithValue(String enteredText) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.getComponentModels().put(VALUE, Model.of(enteredText));
        return context;
    }

    @Test
    void gYearDatatypeProducesTypedLiteral() throws Exception {
        mockTemplate(vf.createIRI(XSD.NAMESPACE + "gYear"), null);
        Value result = contextWithValue("2026").processValue(VALUE);
        assertTrue(result instanceof Literal, "entered value must not be dropped");
        Literal l = (Literal) result;
        assertEquals("2026", l.stringValue());
        assertEquals(XSD.NAMESPACE + "gYear", l.getDatatype().stringValue());
    }

    @Test
    void integerDatatypeProducesTypedLiteral() throws Exception {
        mockTemplate(vf.createIRI(XSD.NAMESPACE + "integer"), null);
        Value result = contextWithValue("42").processValue(VALUE);
        assertTrue(result instanceof Literal);
        Literal l = (Literal) result;
        assertEquals("42", l.stringValue());
        assertEquals(XSD.NAMESPACE + "integer", l.getDatatype().stringValue());
    }

    @Test
    void dateTimeLiteralAlwaysHasSeconds() throws Exception {
        mockTemplate(XSD.DATETIME, null);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        // A time entered without seconds, which is what the picker asks for by default:
        context.getComponentModels().put(VALUE, Model.of(ZonedDateTime.parse("2026-05-17T10:15+02:00")));
        Literal l = (Literal) context.processValue(VALUE);
        assertEquals("2026-05-17T10:15:00+02:00", l.stringValue());
        assertEquals(XSD.DATETIME, l.getDatatype());
    }

    @Test
    void dateTimeLiteralKeepsEnteredSeconds() throws Exception {
        mockTemplate(XSD.DATETIME, null);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.getComponentModels().put(VALUE, Model.of(ZonedDateTime.parse("2026-05-17T10:15:30Z")));
        assertEquals("2026-05-17T10:15:30Z", context.processValue(VALUE).stringValue());
    }

    @Test
    void plainLiteralStillWorks() throws Exception {
        mockTemplate(null, null);
        Value result = contextWithValue("some text").processValue(VALUE);
        assertTrue(result instanceof Literal);
        Literal l = (Literal) result;
        assertEquals("some text", l.stringValue());
        assertEquals(XSD.STRING, l.getDatatype());
    }

    @Test
    void languageTagStillWorks() throws Exception {
        mockTemplate(null, "en");
        Value result = contextWithValue("some text").processValue(VALUE);
        assertTrue(result instanceof Literal);
        Literal l = (Literal) result;
        assertEquals("some text", l.stringValue());
        assertEquals("en", l.getLanguage().orElse(null));
    }

    @Test
    void emptyValueStillResolvesToNull() throws Exception {
        mockTemplate(vf.createIRI(XSD.NAMESPACE + "gYear"), null);
        assertNull(contextWithValue("").processValue(VALUE));
    }

}
