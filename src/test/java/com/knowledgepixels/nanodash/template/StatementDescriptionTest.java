package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A template can describe each of its statements, to tell the user how to fill it in (#45).
 */
class StatementDescriptionTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI DESCRIBED_STATEMENT = vf.createIRI(NP_URI + "/st1");
    private static final IRI UNDESCRIBED_STATEMENT = vf.createIRI(NP_URI + "/st2");
    private static final IRI TITLE = vf.createIRI(NP_URI + "/title");
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");
    private static final String DESCRIPTION = "The title as it appears on the <b>first page</b>.";
    private static final String TEMPLATE_DESCRIPTION = "A template for describing a thing.";

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
        tester.destroy();
    }

    private static NanopubCreator newCreator(String npUri) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator;
    }

    /**
     * Builds a template with two literal statements, of which only the first is described.
     *
     * @return the parsed template
     * @throws Exception if the template can't be built
     */
    private Template template() throws Exception {
        NanopubCreator creator = newCreator(NP_URI);
        IRI templateNode = creator.getAssertionUri();
        IRI subject = vf.createIRI("http://example.com/subject");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Statement description test template"));
        creator.addAssertionStatement(templateNode, DCTERMS.DESCRIPTION, vf.createLiteral(TEMPLATE_DESCRIPTION));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, DESCRIBED_STATEMENT);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, UNDESCRIBED_STATEMENT);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.SUBJECT, subject);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.OBJECT, TITLE);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, DCTERMS.DESCRIPTION, vf.createLiteral(DESCRIPTION));
        creator.addAssertionStatement(DESCRIBED_STATEMENT, NTEMPLATE.STATEMENT_ORDER, vf.createLiteral(1));
        creator.addAssertionStatement(UNDESCRIBED_STATEMENT, RDF.SUBJECT, subject);
        creator.addAssertionStatement(UNDESCRIBED_STATEMENT, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(UNDESCRIBED_STATEMENT, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(UNDESCRIBED_STATEMENT, NTEMPLATE.STATEMENT_ORDER, vf.createLiteral(2));
        creator.addAssertionStatement(TITLE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
        return template;
    }

    /**
     * Renders one statement of the template in the given context.
     *
     * @param context the template context, already initialized
     * @param index   the position of the statement in the form
     * @return the rendered markup
     */
    private String render(TemplateContext context, int index) {
        tester.startComponentInPage(context.getStatementItems().get(index));
        return tester.getLastResponseAsString();
    }

    private TemplateContext formContext() throws Exception {
        template();
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @Test
    void describedStatementHasItsDescription() throws Exception {
        assertEquals(DESCRIPTION, template().getStatementDescription(DESCRIBED_STATEMENT));
    }

    @Test
    void undescribedStatementHasNoDescription() throws Exception {
        assertNull(template().getStatementDescription(UNDESCRIBED_STATEMENT));
    }

    @Test
    void templateDescriptionStaysTheTemplates() throws Exception {
        Template template = template();
        assertEquals(TEMPLATE_DESCRIPTION, template.getDescription());
        assertNull(template.getStatementDescription(vf.createIRI(NP_URI + "/assertion")));
    }

    @Test
    void descriptionIsSanitized() throws Exception {
        NanopubCreator creator = newCreator(NP_URI);
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Sanitizing test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, DESCRIBED_STATEMENT);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, RDF.OBJECT, TITLE);
        creator.addAssertionStatement(DESCRIBED_STATEMENT, DCTERMS.DESCRIPTION,
                vf.createLiteral("Fill in the title.<script>alert('x')</script>"));
        creator.addAssertionStatement(TITLE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());
        String description = template.getStatementDescription(DESCRIBED_STATEMENT);
        assertTrue(description.contains("Fill in the title."), description);
        assertFalse(description.contains("<script"), description);
    }

    @Test
    void shaclPropertyShapeDescriptionIsTheStatementDescription() throws Exception {
        String shaclNpUri = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Shacl1";
        NanopubCreator creator = newCreator(shaclNpUri);
        IRI shape = vf.createIRI(shaclNpUri + "/shape");
        IRI property = vf.createIRI(shaclNpUri + "/nameProperty");
        creator.addAssertionStatement(shape, RDF.TYPE, SHACL.NODE_SHAPE);
        creator.addAssertionStatement(shape, SHACL.TARGET_CLASS, vf.createIRI("http://example.com/Thing"));
        creator.addAssertionStatement(shape, SHACL.PROPERTY, property);
        creator.addAssertionStatement(property, SHACL.PATH, RDFS.LABEL);
        creator.addAssertionStatement(property, DCTERMS.DESCRIPTION, vf.createLiteral("The name of the thing."));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());
        assertEquals("The name of the thing.", template.getStatementDescription(property));
    }

    @Test
    void formShowsTheDescriptionAboveTheStatement() throws Exception {
        String html = render(formContext(), 0);
        assertTrue(html.contains("class=\"statement-description\""), html);
        assertTrue(html.contains(DESCRIPTION), html);
    }

    @Test
    void formShowsNoDescriptionForAnUndescribedStatement() throws Exception {
        String html = render(formContext(), 1);
        assertFalse(html.contains("statement-description"), html);
    }

    @Test
    void viewingANanopubShowsNoDescription() throws Exception {
        template();
        NanopubCreator creator = newCreator("http://purl.org/nanopub/temp/data/");
        creator.addAssertionStatement(vf.createStatement(vf.createIRI("http://example.com/subject"), RDFS.LABEL, vf.createLiteral("A title")));
        Nanopub dataNp = creator.finalizeNanopub();
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", dataNp);
        context.initStatements();
        new ValueFiller(dataNp, ContextType.ASSERTION, false).fill(context);
        String html = render(context, 0);
        assertFalse(html.contains("statement-description"), html);
    }

}
