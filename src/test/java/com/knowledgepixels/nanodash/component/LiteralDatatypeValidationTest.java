package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A literal whose value doesn't fit its built-in XSD datatype makes a nanopublication invalid, so
 * the form field reports it instead of leaving it to fail at signing (#751).
 */
class LiteralDatatypeValidationTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI VALUE_FIELD = vf.createIRI(NP_URI + "/value");

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
     * Builds a one-statement template whose object is a literal placeholder of the given datatype.
     *
     * @param datatype the datatype declared for the placeholder, or null for none
     * @return an initialized context for the template
     * @throws Exception if the template can't be built
     */
    private TemplateContext contextWithDatatype(IRI datatype) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Datatype test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDF.VALUE);
        creator.addAssertionStatement(st1, RDF.OBJECT, VALUE_FIELD);
        creator.addAssertionStatement(VALUE_FIELD, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(VALUE_FIELD, RDFS.LABEL, vf.createLiteral("value"));
        if (datatype != null) {
            creator.addAssertionStatement(VALUE_FIELD, NTEMPLATE.HAS_DATATYPE, datatype);
        }
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    /**
     * Runs every validator the field carries, the way the form does on submit.
     *
     * @param item  the literal field
     * @param value the value to validate
     * @return the validated value with any errors
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Validatable<String> validate(LiteralTextfieldItem item, String value) {
        Validatable<String> v = new Validatable<>(value);
        for (IValidator validator : item.getTextComponent().getValidators()) {
            validator.validate(v);
        }
        return v;
    }

    private LiteralTextfieldItem itemFor(IRI datatype) throws Exception {
        return new LiteralTextfieldItem("value", VALUE_FIELD, true, contextWithDatatype(datatype));
    }

    @Test
    void wellTypedValuePasses() throws Exception {
        assertTrue(validate(itemFor(XSD.INT), "42").isValid());
    }

    @Test
    void illTypedValueIsRejectedWithTheShortDatatypeName() throws Exception {
        Validatable<String> v = validate(itemFor(XSD.INT), "abc");
        assertFalse(v.isValid());
        assertEquals("'abc' is not a valid xsd:int", ((ValidationError) v.getErrors().getFirst()).getMessage());
    }

    @Test
    void otherBuiltInDatatypesAreCheckedToo() throws Exception {
        assertFalse(validate(itemFor(XSD.BOOLEAN), "yes").isValid());
        assertTrue(validate(itemFor(XSD.BOOLEAN), "true").isValid());
        assertFalse(validate(itemFor(XSD.DECIMAL), "1,5").isValid());
        assertTrue(validate(itemFor(XSD.DECIMAL), "1.5").isValid());
    }

    @Test
    void untypedAndNonXsdPlaceholdersAcceptAnything() throws Exception {
        assertTrue(validate(itemFor(null), "abc").isValid());
        assertTrue(validate(itemFor(vf.createIRI("http://example.com/customType")), "abc").isValid());
    }

    @Test
    void illTypedValueFilledFromANanopubIsHighlighted() throws Exception {
        LiteralTextfieldItem item = itemFor(XSD.INT);
        item.unifyWith(vf.createLiteral("abc", XSD.INT));
        tester.startComponentInPage(item);
        assertTrue(textfieldClass().contains("invalid"), textfieldClass());
    }

    @Test
    void wellTypedValueFilledFromANanopubIsNotHighlighted() throws Exception {
        LiteralTextfieldItem item = itemFor(XSD.INT);
        item.unifyWith(vf.createLiteral("42", XSD.INT));
        tester.startComponentInPage(item);
        assertFalse(textfieldClass().contains("invalid"), textfieldClass());
    }

    @Test
    void illTypedValueIsHighlightedWhileTyping() throws Exception {
        LiteralTextfieldItem item = itemFor(XSD.INT);
        tester.startComponentInPage(item);
        type(item, "abc");
        assertTrue(tester.getLastResponseAsString().contains("classList.toggle('invalid', true)"),
                tester.getLastResponseAsString());
    }

    @Test
    void correctedValueLosesItsHighlightWhileTyping() throws Exception {
        LiteralTextfieldItem item = itemFor(XSD.INT);
        tester.startComponentInPage(item);
        type(item, "abc");
        type(item, "42");
        assertTrue(tester.getLastResponseAsString().contains("classList.toggle('invalid', false)"),
                tester.getLastResponseAsString());
        assertEquals("42", item.getTextComponent().getModelObject());
    }

    /**
     * Sends a value the way the browser does on each keystroke, to the behavior that validates it.
     *
     * @param item  the literal field, already rendered
     * @param value the value typed so far
     */
    private void type(LiteralTextfieldItem item, String value) {
        tester.getRequest().getPostParameters().setParameterValue(item.getTextComponent().getInputName(), value);
        tester.executeBehavior(item.getTextComponent().getBehaviors(OnChangeAjaxBehavior.class).stream()
                .filter(b -> !(b instanceof ValueItem.KeepValueAfterRefreshBehavior))
                .findFirst()
                .orElseThrow());
    }

    private String textfieldClass() {
        return TagTester.createTagByName(tester.getLastResponseAsString(), "input").getAttribute("class");
    }

}
