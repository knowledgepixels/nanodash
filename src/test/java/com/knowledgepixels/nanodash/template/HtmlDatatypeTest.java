package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.model.IModel;
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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void htmlDatatypeIsNamedBesideTheContent() throws Exception {
        // A published literal says what it is, the way an xsd:dateTime one does -- named by
        // its prefix rather than by the full IRI.
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT, RDF.HTML));
        assertTrue(html.contains("(rdf:HTML)"), html);
        assertFalse(html.contains("22-rdf-syntax-ns#HTML"), html);
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



    @Test
    void shortHtmlIsShownWhole() throws Exception {
        // The "show more" arrow and the fade-out that comes with it are for content too long
        // to show: on one line of HTML they covered the line and sat on top of it.
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral(HTML_CONTENT, RDF.HTML));
        assertFalse(html.contains("long-literal"), html);
        assertFalse(html.contains("show-more"), html);
    }

    @Test
    void longHtmlIsCutOffWithAnArrow() throws Exception {
        mockTemplate(RDF.HTML);
        String html = renderReadOnly(vf.createLiteral("<p>" + "word ".repeat(40) + "</p>", RDF.HTML));
        assertTrue(html.contains("long-literal collapsed"), html);
        assertTrue(html.contains("show-more"), html);
    }

    /**
     * Renders the publish form for the template, the way an author sees it.
     */
    private String renderEditable() {
        return renderEditable(false);
    }

    /**
     * Renders the publish form for the template, the way an author sees it, with the field
     * locked or free to fill.
     */
    private String renderEditable(boolean locked) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        if (locked) {
            context.setParam("comment", HTML_CONTENT);
            context.setLocked("comment");
        }
        context.initStatements();
        tester.startComponentInPage(context.getStatementItems().get(0));
        return tester.getLastResponseAsString();
    }

    @Test
    void htmlDatatypeIsWrittenWithTheRichTextEditor() throws Exception {
        // Content declared as rdf:HTML is rendered as HTML, so it is written as HTML too
        // rather than as markup typed into a plain text area.
        mockTemplate(RDF.HTML);
        String html = renderEditable();
        assertTrue(html.contains("class=\"nanopub-html-editor\""), html);
        assertTrue(html.contains("<trix-editor"), html);
        assertFalse(html.contains("class=\"nanopub-textfield\""), html);
    }

    @Test
    void theEditorIsPointedAtTheFieldThatIsSubmitted() throws Exception {
        // Trix edits a hidden form field, named by the editor's "input" attribute; without
        // that pairing the editor would show but nothing typed would reach the form.
        mockTemplate(RDF.HTML);
        String html = renderEditable();
        Matcher field = Pattern.compile("<input type=\"hidden\"[^>]*\\bid=\"([^\"]+)\"").matcher(html);
        assertTrue(field.find(), html);
        assertTrue(html.contains("input=\"" + field.group(1) + "\""), html);
    }

    /**
     * Finds a webjar resource by the path a resource reference resolves to, which is where
     * the webjar resource finder reads it from.
     */
    private URL shippedResource(String resolvedName) {
        return getClass().getClassLoader().getResource("META-INF/resources/" + resolvedName.replaceFirst("^/", ""));
    }

    @Test
    void theEditorWidgetIsAmongTheResourcesWeShip() throws Exception {
        // The editor this started out with turned out not to be in the library that was
        // supposed to carry it, which nothing but the browser console said. So: the file
        // the field asks for is on the classpath, and it defines the element the markup
        // uses.
        String editorJs = new WebjarsJavaScriptResourceReference("trix/current/dist/trix.umd.min.js").getName();
        URL onClasspath = shippedResource(editorJs);
        assertNotNull(onClasspath, editorJs + " is not on the classpath");
        assertTrue(new String(onClasspath.openStream().readAllBytes(), StandardCharsets.UTF_8).contains("trix-editor"),
                editorJs + " does not define the trix-editor element");
        String editorCss = new WebjarsCssResourceReference("trix/current/dist/trix.css").getName();
        assertNotNull(shippedResource(editorCss), editorCss + " is not on the classpath");
    }

    @Test
    void theFormDoesNotNameTheDatatype() throws Exception {
        // What the template asks for is the editor, and that is what the author gets; the
        // datatype is no more written beside it than "(xsd:dateTime)" is beside a date picker.
        mockTemplate(RDF.HTML);
        String html = renderEditable();
        assertFalse(html.contains("rdf:HTML"), html);
        assertFalse(html.contains("22-rdf-syntax-ns#HTML"), html);
    }

    @Test
    void aLockedValueIsNotEditedInTheEditor() throws Exception {
        // The editor writes into a field a locked form doesn't read back (issue #678), so it
        // has to say as much rather than take input that goes nowhere.
        mockTemplate(RDF.HTML);
        Matcher editorTag = Pattern.compile("<trix-editor[^>]*>").matcher(renderEditable(true));
        assertTrue(editorTag.find(), "no editor rendered");
        assertTrue(editorTag.group().contains("locked-value"), editorTag.group());
    }

    /**
     * Publishes the given markup the way the form does, and gives back what the assertion
     * would carry.
     */
    private Value published(String markup) throws Exception {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        @SuppressWarnings("unchecked")
        IModel<String> model = (IModel<String>) context.getComponentModels().get(COMMENT);
        model.setObject(markup);
        context.finalizeStatements();
        return context.processValue(COMMENT);
    }

    @Test
    void aSpaceTypedAfterTheLastWordIsNotPublished() throws Exception {
        // The editor writes a trailing space as a non-breaking space, since that is the only
        // way it survives in HTML at all -- and a nanopublication carries it forever.
        mockTemplate(RDF.HTML);
        assertEquals("<div>Text.</div>", published("<div>Text.&nbsp;</div>").stringValue());
    }

    @Test
    void anEmptyLastParagraphIsNotPublished() throws Exception {
        mockTemplate(RDF.HTML);
        assertEquals("<div>Text.</div>", published("<div>Text.</div><div><br></div>").stringValue());
        assertEquals("<div>Text.</div>", published("<div>Text.<br></div>").stringValue());
    }

    @Test
    void blanksWithinTheMarkupAreLeftAlone() throws Exception {
        // Only the edges are trimmed: a non-breaking space between two words was put there on
        // purpose, and so was the empty paragraph between two others.
        mockTemplate(RDF.HTML);
        assertEquals("<div>A\u00A0B</div>", published("<div>A&nbsp;B</div>").stringValue(),
                "the sanitizer writes the entity as the character it stands for");
        assertEquals("<div>A</div><div><br /></div><div>B</div>",
                published("<div>A</div><div><br></div><div>B</div>").stringValue());
    }

    @Test
    void markupThatIsOnlyBlankPublishesNoValue() throws Exception {
        mockTemplate(RDF.HTML);
        assertNull(published("<div>&nbsp;</div>"), "an editor holding nothing but a space states nothing");
    }

    @Test
    void editorMarkupIsSanitizedBeforeItIsPublished() throws Exception {
        // A nanopublication cannot be edited afterwards, so what the editor produced (or
        // what was pasted into it) is cleaned before signing, not only when rendered.
        mockTemplate(RDF.HTML);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        @SuppressWarnings("unchecked")
        IModel<String> model = (IModel<String>) context.getComponentModels().get(COMMENT);
        model.setObject("<p onclick=\"alert('x')\">Hi</p><script>alert('x')</script>");
        context.finalizeStatements();
        Value result = context.processValue(COMMENT);
        assertTrue(result instanceof Literal, "entered value must not be dropped");
        Literal literal = (Literal) result;
        assertEquals(RDF.HTML, literal.getDatatype());
        assertFalse(literal.stringValue().contains("alert("), literal.stringValue());
        assertFalse(literal.stringValue().contains("<script"), literal.stringValue());
        assertTrue(literal.stringValue().contains("<p>Hi</p>"), literal.stringValue());
    }

}
