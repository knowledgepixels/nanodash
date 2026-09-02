package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filling in a literal placeholder typed with one of the XSD Gregorian datatypes, from the form
 * the user actually gets: what the parts assemble into is what a published nanopublication would
 * carry.
 */
class LiteralGregorianItemTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String ITEM =
            "panel:form:statements:1:statement:statement-group:0:statement:obj:value";
    private static final String YEAR_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:year";
    private static final String MONTH_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:month";
    private static final String DAY_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:day";

    /** Index of a month or day in its dropdown: the choices run from 01. */
    private static final int MAY = 4, FEBRUARY = 1, DAY_17 = 16, DAY_30 = 29;

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        tester.getSession().setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    /**
     * Registers a template with a plain literal ("the name") and a literal of the given
     * datatype ("the moment"). Each test needs its own nanopub URI, because templates are
     * cached by URI.
     */
    private static String registerTemplate(String npUri, IRI datatype, boolean momentIsOptional) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI st2 = vf.createIRI(npUri + "/st2");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        IRI moment = vf.createIRI(npUri + "/moment");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Gregorian test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        if (momentIsOptional) creator.addAssertionStatement(st2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(st2, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st2, RDF.OBJECT, moment);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        creator.addAssertionStatement(moment, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(moment, NTEMPLATE.HAS_DATATYPE, datatype);
        creator.addAssertionStatement(moment, RDFS.LABEL, vf.createLiteral("the moment"));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return npUri;
    }

    private void startForm(String npUri, IRI datatype) throws Exception {
        startForm(npUri, datatype, true, new PageParameters());
    }

    private void startForm(String npUri, IRI datatype, boolean optional, PageParameters extraParams) throws Exception {
        PageParameters params = new PageParameters(extraParams)
                .add("template", registerTemplate(npUri, datatype, optional));
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
    }

    /**
     * Fills in everything but the moment, so that submitting exercises validation while the
     * rest of the form is complete.
     */
    private FormTester filledForm() {
        FormTester form = tester.newFormTester("panel:form");
        form.setValue("statements:0:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue("statements:0:statement:statement-group:0:statement:obj:value:textfield", "some name");
        form.setValue("statements:1:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        return form;
    }

    private LiteralGregorianItem item() {
        return (LiteralGregorianItem) tester.getComponentFromLastRenderedPage(ITEM);
    }

    private String value() {
        return item().getValueModel().getObject();
    }

    private void assertErrorMessage(String expectedFragment) {
        List<FeedbackMessage> messages = tester.getFeedbackMessages(m -> m.getLevel() == FeedbackMessage.ERROR);
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().toString().contains(expectedFragment)),
                "Expected an error mentioning '" + expectedFragment + "', got " + messages);
    }

    @Test
    void aYearIsEnteredAsAYear() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg01", XSD.GYEAR);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "2026");
        form.submit();

        assertEquals("2026", value());
        assertTrue(tester.getLastResponseAsString().contains("type=\"number\""),
                "a year is entered in a number field, not a free-text one");
    }

    @Test
    void aYearAndMonthAssembleIntoOneValue() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg02", XSD.GYEARMONTH);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "2026");
        form.select(MONTH_FIELD, MAY);
        form.submit();

        assertEquals("2026-05", value());
    }

    @Test
    void aMonthIsPickedFromItsName() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg03", XSD.GMONTH);
        FormTester form = filledForm();
        form.select(MONTH_FIELD, MAY);
        form.submit();

        assertEquals("--05", value());
        assertTrue(tester.getLastResponseAsString().contains("May"), "months are named, not numbered");
    }

    @Test
    void aMonthAndDayAssembleIntoOneValue() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg04", XSD.GMONTHDAY);
        FormTester form = filledForm();
        form.select(MONTH_FIELD, MAY);
        form.select(DAY_FIELD, DAY_17);
        form.submit();

        assertEquals("--05-17", value());
    }

    @Test
    void aDayIsPickedOnItsOwn() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg05", XSD.GDAY);
        FormTester form = filledForm();
        form.select(DAY_FIELD, DAY_17);
        form.submit();

        assertEquals("---17", value());
    }

    @Test
    void halfOfATwoPartValueIsReportedRatherThanPublished() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg06", XSD.GYEARMONTH);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "2026");
        form.submit();

        assertEquals("", value(), "a year alone is not a gYearMonth");
        assertErrorMessage("Please also select a month");
    }

    @Test
    void aDayItsMonthCannotHaveIsReported() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg07", XSD.GMONTHDAY);
        FormTester form = filledForm();
        form.select(MONTH_FIELD, FEBRUARY);
        form.select(DAY_FIELD, DAY_30);
        form.submit();

        assertErrorMessage("February has no day 30");
    }

    @Test
    void aYearOfTheWrongShapeIsReported() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg08", XSD.GYEAR);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "26");
        form.submit();

        assertEquals("", value());
        assertErrorMessage("A year is four or more digits");
    }

    @Test
    void aValueGivenInTheUrlFillsTheFieldsIn() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg09", XSD.GYEARMONTH,
                true, new PageParameters().add("param_moment", "2026-05"));

        assertEquals("2026-05", value(), "the value from the URL should reach the fields");
        assertTrue(tester.getLastResponseAsString().contains("value=\"2026\""), "the year field shows it");
    }

    @Test
    void anEmptyOptionalValueStaysEmpty() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg10", XSD.GYEAR);
        filledForm().submit();

        assertEquals("", value());
    }

}
