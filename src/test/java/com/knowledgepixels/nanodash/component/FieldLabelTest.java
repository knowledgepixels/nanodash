package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Messages about a form field name the field after its placeholder, rather than after the
 * Wicket component id that every field of a kind shares.
 */
class FieldLabelTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI LABELLED_FIELD = vf.createIRI(NP_URI + "/title");
    private static final IRI UNLABELLED_FIELD = vf.createIRI(NP_URI + "/literal2");

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

    /**
     * Builds a template with two literal placeholders, one labelled "the title" and one without
     * a label.
     *
     * @return an initialized context for the template
     * @throws Exception if the template can't be built
     */
    private TemplateContext context() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI st2 = vf.createIRI(NP_URI + "/st2");
        IRI subject = vf.createIRI("http://example.com/subject");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Field label test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, subject);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, LABELLED_FIELD);
        creator.addAssertionStatement(st2, RDF.SUBJECT, subject);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st2, RDF.OBJECT, UNLABELLED_FIELD);
        creator.addAssertionStatement(LABELLED_FIELD, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(LABELLED_FIELD, RDFS.LABEL, vf.createLiteral("the title"));
        creator.addAssertionStatement(UNLABELLED_FIELD, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    /**
     * Validates a required field left empty and returns the message it reports.
     *
     * @param field the placeholder of the field
     * @return the message reported for the empty field
     * @throws Exception if the template can't be built
     */
    private String messageForEmptyRequiredField(IRI field) throws Exception {
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", field, false, context());
        tester.startComponentInPage(item);
        FormComponent<String> textComponent = item.getTextComponent();
        textComponent.validate();
        return textComponent.getFeedbackMessages().first().getMessage().toString();
    }

    @Test
    void requiredFieldIsNamedByItsLabel() throws Exception {
        assertEquals("'the title' is required.", messageForEmptyRequiredField(LABELLED_FIELD));
    }

    @Test
    void requiredFieldWithoutLabelIsNamedByItsPlaceholder() throws Exception {
        assertEquals("'literal2' is required.", messageForEmptyRequiredField(UNLABELLED_FIELD));
    }

}
