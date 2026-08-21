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
 * Tests for issue #378: whether a literal is rendered as HTML is decided by its
 * datatype (rdf:HTML), the way dates are decided by xsd:date, rather than by the
 * pattern heuristic. The heuristic only remains as a fallback for literals whose
 * placeholder declares no datatype, so content published before HTML was tagged
 * keeps rendering.
 */
public class HtmlDatatypeTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");

    private static final String HTML_CONTENT = "<p>Hello <em>world</em></p>";

    private WicketTester tester;
    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    /**
     * Builds a one-statement template (subject rdfs:comment [comment]) whose object is a
     * long literal placeholder with the given datatype, and registers it with the mocked
     * TemplateData.
     */
    private void mockTemplate(IRI datatype) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("HTML datatype test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        if (datatype != null) {
            creator.addAssertionStatement(COMMENT, NTEMPLATE.HAS_DATATYPE, datatype);
        }
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
     * Mirrors the viewer flow: fills a read-only context (ReadonlyItem) from the data
     * nanopub and renders the resulting statement.
     */
    private String renderReadOnly(Value comment) throws Exception {
        Nanopub dataNp = dataNanopub(comment);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", dataNp);
        context.initStatements();
        new ValueFiller(dataNp, ContextType.ASSERTION, false).fill(context);
        assertTrue(context.getStatementItems().get(0).isMatched(), "value must unify with the placeholder");
        tester.startComponentInPage(context.getStatementItems().get(0));
        return tester.getLastResponseAsString();
    }

    @Test
    void htmlDatatypeIsRendered() throws Exception {
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT, RDF.HTML));
        assertTrue(html.contains(HTML_CONTENT), html);
        assertFalse(html.contains("&lt;p&gt;"), html);
    }

    @Test
    void htmlDatatypeGetsNoDatatypeMarker() throws Exception {
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT, RDF.HTML));
        assertFalse(html.contains("rdf-syntax-ns#HTML"), "the rendered content already shows what it is: " + html);
    }

    @Test
    void htmlContentIsSanitized() throws Exception {
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral("<p onclick=\"alert('x')\">Hi</p><script>alert('x')</script>", RDF.HTML));
        // (the panel's own markup carries an onclick, so look for the injected payload)
        assertFalse(html.contains("alert("), html);
        assertFalse(html.contains("<script>"), html);
        assertTrue(html.contains("<p>Hi</p>"), html);
    }

    @Test
    void htmlDatatypeIsRenderedEvenWithoutLeadingTag() throws Exception {
        // The pattern heuristic requires a leading block tag; the datatype does not.
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral("Hello <em>world</em>", RDF.HTML));
        assertTrue(html.contains("Hello <em>world</em>"), html);
    }

    @Test
    void declaredStringDatatypeIsEscapedDespiteLookingLikeHtml() throws Exception {
        // The template says xsd:string, so the heuristic must not kick in.
        mockTemplate(XSD.STRING);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT));
        assertTrue(html.contains("&lt;p&gt;"), html);
        assertFalse(html.contains(HTML_CONTENT), html);
    }

    @Test
    void undeclaredDatatypeStillFallsBackToPattern() throws Exception {
        // Legacy content: published as a plain string before HTML was tagged.
        mockTemplate(null);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT));
        assertTrue(html.contains(HTML_CONTENT), html);
    }

    @Test
    void htmlDatatypeRoundTripsThroughPublishing() throws Exception {
        mockTemplate(RDF.HTML);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.getComponentModels().put(COMMENT, Model.of(HTML_CONTENT));
        Value result = context.processValue(COMMENT);
        assertTrue(result instanceof Literal, "entered value must not be dropped");
        Literal literal = (Literal) result;
        assertEquals(HTML_CONTENT, literal.stringValue());
        assertEquals(RDF.HTML, literal.getDatatype());
    }

}
