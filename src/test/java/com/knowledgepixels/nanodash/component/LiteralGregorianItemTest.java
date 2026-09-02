package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.Component;
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

import java.time.Year;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final int MAY = 4, FEBRUARY = 1, DECEMBER = 11, DAY_17 = 16, DAY_30 = 29;

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
        return registerTemplate(npUri, datatype, momentIsOptional, null);
    }

    private static String registerTemplate(String npUri, IRI datatype, boolean momentIsOptional, String regex) throws Exception {
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
        if (regex != null) creator.addAssertionStatement(moment, NTEMPLATE.HAS_REGEX, vf.createLiteral(regex));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return npUri;
    }

    private void startForm(String npUri, IRI datatype) throws Exception {
        startForm(npUri, datatype, true, new PageParameters());
    }

    private void startForm(String npUri, IRI datatype, boolean optional, PageParameters extraParams) throws Exception {
        startForm(npUri, datatype, optional, extraParams, null);
    }

    private void startForm(String npUri, IRI datatype, boolean optional, PageParameters extraParams, String regex) throws Exception {
        PageParameters params = new PageParameters(extraParams)
                .add("template", registerTemplate(npUri, datatype, optional, regex));
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
        assertErrorMessage("'month of 'the moment'' is required");
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
    void aCorrectedYearIsPickedUpAfterTheError() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg11", XSD.GYEAR);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "26");
        form.submit();
        assertEquals("", value(), "the rejected year is not kept as a value");

        FormTester corrected = filledForm();
        corrected.setValue(YEAR_FIELD, "2026");
        corrected.submit();

        assertEquals("2026", value(), "the corrected year has to reach the value");
    }

    @Test
    void aCorrectedYearIsPickedUpOverAjax() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg12", XSD.GYEAR);
        String yearPath = "panel:form:" + YEAR_FIELD;

        FormTester form = tester.newFormTester("panel:form");
        form.setValue(YEAR_FIELD, "26");
        tester.executeAjaxEvent(yearPath, "change");
        assertEquals("", value(), "the rejected year is not kept as a value");

        FormTester corrected = tester.newFormTester("panel:form");
        corrected.setValue(YEAR_FIELD, "2026");
        tester.executeAjaxEvent(yearPath, "change");

        assertEquals("2026", value(), "the corrected year has to reach the value");
        // The Ajax response re-renders only what the update asked for, so what the field will
        // render next is asserted on its model rather than on this response.
        assertEquals("2026", tester.getComponentFromLastRenderedPage(yearPath).getDefaultModelObject(),
                "and has to be what the field holds afterwards");
    }

    @Test
    void theTwoPartsSurvivePickingThemOneAtATime() throws Exception {
        // Each Ajax update carries only the field that changed, so the part picked first has to
        // survive the request that picks the second.
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg13", XSD.GMONTHDAY);
        String monthPath = "panel:form:" + MONTH_FIELD;
        String dayPath = "panel:form:" + DAY_FIELD;

        tester.newFormTester("panel:form").select(MONTH_FIELD, MAY);
        tester.executeAjaxEvent(monthPath, "change");
        assertEquals("", value(), "a month alone is not yet a gMonthDay");

        tester.newFormTester("panel:form").select(DAY_FIELD, DAY_17);
        tester.executeAjaxEvent(dayPath, "change");

        assertEquals("--05-17", value());
    }

    @Test
    void aValueSurvivesAChangeToAnotherField() throws Exception {
        // What KeepValueAfterRefreshBehavior was there for, and what this item's own updating
        // behavior has to go on providing now that it no longer carries both.
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg14", XSD.GYEAR);
        String yearPath = "panel:form:" + YEAR_FIELD;
        String namePath = "panel:form:statements:0:statement:statement-group:0:statement:obj:value:textfield";

        tester.newFormTester("panel:form").setValue(YEAR_FIELD, "2026");
        tester.executeAjaxEvent(yearPath, "change");
        assertEquals("2026", value());

        FormTester other = tester.newFormTester("panel:form");
        other.setValue("statements:0:statement:statement-group:0:statement:obj:value:textfield", "some name");
        tester.executeAjaxEvent(namePath, "change");

        assertEquals("2026", value(), "a change elsewhere on the form must not clear the year");
        assertEquals("2026", tester.getComponentFromLastRenderedPage(yearPath).getDefaultModelObject());
    }

    @Test
    void aValueRejectedByTheTemplatePatternDoesNotOutliveItsCorrection() throws Exception {
        // A template may narrow a gYear further, e.g. to four digits exactly. The value the
        // pattern judges has to be the one being entered, not the one entered before it.
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg15", XSD.GYEAR,
                true, new PageParameters(), "[0-9]{4}");
        String yearPath = "panel:form:" + YEAR_FIELD;

        tester.newFormTester("panel:form").setValue(YEAR_FIELD, "46464");
        tester.executeAjaxEvent(yearPath, "change");
        assertEquals("", value(), "a value the pattern rejects is not stored");
        assertErrorMessage("Value '46464' doesn't match the pattern");

        tester.clearFeedbackMessages();
        tester.newFormTester("panel:form").setValue(YEAR_FIELD, "2026");
        tester.executeAjaxEvent(yearPath, "change");

        assertEquals("2026", value(), "the corrected year has to be accepted");
        List<FeedbackMessage> errors = tester.getFeedbackMessages(m -> m.getLevel() == FeedbackMessage.ERROR);
        assertTrue(errors.stream().noneMatch(m -> m.getMessage().toString().contains("46464")),
                "the rejected value must not still be the one judged, got " + errors);
    }

    @Test
    void aPatternOverATwoPartValueJudgesBothPartsAsTheyStand() throws Exception {
        // Only the first half of the year, so the month decides -- and the month is the part
        // that is not being changed when the year is, and the other way round.
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg16", XSD.GYEARMONTH,
                true, new PageParameters(), "[0-9]{4}-0[1-6]");
        String yearPath = "panel:form:" + YEAR_FIELD;
        String monthPath = "panel:form:" + MONTH_FIELD;

        tester.newFormTester("panel:form").setValue(YEAR_FIELD, "2026");
        tester.executeAjaxEvent(yearPath, "change");

        tester.newFormTester("panel:form").select(MONTH_FIELD, DECEMBER);
        tester.executeAjaxEvent(monthPath, "change");
        assertEquals("", value(), "December is outside the pattern, so nothing is stored");
        assertErrorMessage("Value '2026-12' doesn't match the pattern");

        tester.clearFeedbackMessages();
        tester.newFormTester("panel:form").select(MONTH_FIELD, MAY);
        tester.executeAjaxEvent(monthPath, "change");

        assertEquals("2026-05", value(), "the corrected month has to be accepted");
    }

    @Test
    void eachPartIsStoredAsItIsEnteredRatherThanWaitingForTheOther() throws Exception {
        // The way a browser sends it: one field per request. A part rejected for the absence of
        // one the user has not reached yet would never be stored, and the value could then never
        // be completed at all.
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg17", XSD.GYEARMONTH);
        String yearPath = "panel:form:" + YEAR_FIELD;
        String monthPath = "panel:form:" + MONTH_FIELD;

        tester.newFormTester("panel:form").setValue(YEAR_FIELD, "2026");
        tester.executeAjaxEvent(yearPath, "change");
        assertEquals("", value(), "a year alone is not yet a gYearMonth");
        assertTrue(tester.getFeedbackMessages(m -> m.getLevel() == FeedbackMessage.ERROR).isEmpty(),
                "and is not an error while the month is still being reached for");

        tester.newFormTester("panel:form").select(MONTH_FIELD, MAY);
        tester.executeAjaxEvent(monthPath, "change");

        assertEquals("2026-05", value(), "the year entered a request earlier has to still be there");
        assertEquals("2026", tester.getComponentFromLastRenderedPage(yearPath).getDefaultModelObject());
    }

    @Test
    void anEmptyYearFieldStartsItsSpinnerAtTheCurrentYear() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg18", XSD.GYEAR);
        Component yearField = tester.getComponentFromLastRenderedPage("panel:form:" + YEAR_FIELD);

        assertFalse(yearField.getBehaviors(LiteralGregorianItem.CurrentYearOnFirstStep.class).isEmpty(),
                "the year field should carry the stepping script");

        String script = LiteralGregorianItem.CurrentYearOnFirstStep.script(yearField.getMarkupId(), Year.of(2026));
        assertTrue(script.contains("document.getElementById('" + yearField.getMarkupId() + "')"),
                "the script has to name this field: " + script);
        assertTrue(script.contains("var currentYear = '2026'"),
                "an empty field steps to the current year, not to 1");
        assertTrue(script.contains("previous === ''"), "and only while it is empty");
        assertTrue(script.contains("ArrowUp") && script.contains("ArrowDown"),
                "either arrow starts it off");
        assertTrue(script.contains("new Event('change'"),
                "and the value has to reach the form, not just the field");
    }

    @Test
    void anEmptyOptionalValueStaysEmpty() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Greg10", XSD.GYEAR);
        filledForm().submit();

        assertEquals("", value());
    }

}
