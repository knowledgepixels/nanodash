package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.DatePrecision;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
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
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filling in a date placeholder from the form the user actually gets: the value is entered at
 * whatever precision the user has it, and the literal it publishes is typed by that precision.
 */
class LiteralFlexibleDateItemTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String ITEM =
            "panel:form:statements:1:statement:statement-group:0:statement:obj:value";
    private static final String PARTS = "statements:1:statement:statement-group:0:statement:obj:value:";
    private static final String YEAR_FIELD = PARTS + "year";
    private static final String MONTH_FIELD = PARTS + "month";
    private static final String DAY_FIELD = PARTS + "day";
    private static final String TIME_FIELD = PARTS + "time";
    private static final String ZONE_FIELD = PARTS + "zone";

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
     * Registers a template with a plain literal ("the name") and a date placeholder ("the
     * moment") that lists the given datatypes. Each test needs its own nanopub URI, because
     * templates are cached by URI.
     */
    private static Template registerTemplate(String npUri, List<IRI> datatypes, Literal defaultValue) throws Exception {
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
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Date placeholder test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(st2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(st2, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st2, RDF.OBJECT, moment);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        creator.addAssertionStatement(moment, RDF.TYPE, Template.DATE_PLACEHOLDER);
        creator.addAssertionStatement(moment, RDFS.LABEL, vf.createLiteral("the moment"));
        for (IRI datatype : datatypes) creator.addAssertionStatement(moment, NTEMPLATE.HAS_DATATYPE, datatype);
        if (defaultValue != null) creator.addAssertionStatement(moment, NTEMPLATE.HAS_DEFAULT_VALUE, defaultValue);
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return TemplateData.get().getTemplate(npUri);
    }

    private void startForm(String npUri, IRI... datatypes) throws Exception {
        startForm(npUri, new PageParameters(), null, datatypes);
    }

    private void startForm(String npUri, PageParameters extraParams, Literal defaultValue, IRI... datatypes) throws Exception {
        registerTemplate(npUri, List.of(datatypes), defaultValue);
        PageParameters params = new PageParameters(extraParams).add("template", npUri);
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
    }

    /**
     * Fills in everything but the moment, keeping the parts of the moment that are already
     * entered, so that submitting exercises validation while the rest of the form is complete.
     */
    private FormTester filledForm() {
        FormTester form = tester.newFormTester("panel:form");
        form.setValue("statements:0:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue("statements:0:statement:statement-group:0:statement:obj:value:textfield", "some name");
        form.setValue("statements:1:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        return form;
    }

    /**
     * Enters one part the way the browser sends it: alone, in an Ajax update of that field.
     */
    private void enter(String field, String value) {
        tester.newFormTester("panel:form").setValue(field, value);
        tester.executeAjaxEvent("panel:form:" + field, "change");
    }

    private LiteralFlexibleDateItem item() {
        return (LiteralFlexibleDateItem) tester.getComponentFromLastRenderedPage(ITEM);
    }

    private String value() {
        return item().getValueModel().getObject();
    }

    private Value published(String npUri) {
        return item().context.processValue(vf.createIRI(npUri + "/moment"));
    }

    private boolean isShown(String field) {
        return tester.getComponentFromLastRenderedPage("panel:form:" + field, false).isVisible();
    }

    private void assertErrorMessage(String expectedFragment) {
        List<FeedbackMessage> messages = tester.getFeedbackMessages(m -> m.getLevel() == FeedbackMessage.ERROR);
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().toString().contains(expectedFragment)),
                "Expected an error mentioning '" + expectedFragment + "', got " + messages);
    }

    @Test
    void aYearAlonePublishesAGYear() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex01";
        startForm(np);
        FormTester form = filledForm();
        form.setValue(YEAR_FIELD, "2026");
        form.submit();

        assertEquals("2026", value());
        assertEquals(vf.createLiteral("2026", XSD.GYEAR), published(np));
    }

    @Test
    void theFieldWidensAsItIsFilledIn() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex02";
        startForm(np);
        assertTrue(isShown(YEAR_FIELD));
        assertFalse(isShown(MONTH_FIELD), "no month before there is a year");

        enter(YEAR_FIELD, "2026");
        assertTrue(isShown(MONTH_FIELD));
        assertFalse(isShown(DAY_FIELD), "no day before there is a month");

        enter(MONTH_FIELD, "11");
        assertEquals(vf.createLiteral("2026-11", XSD.GYEARMONTH), published(np));
        assertTrue(isShown(DAY_FIELD));
        assertFalse(isShown(TIME_FIELD), "no time before there is a day");

        enter(DAY_FIELD, "16");
        assertEquals(vf.createLiteral("2026-11-16", XSD.DATE), published(np));
        assertTrue(isShown(TIME_FIELD));
        assertFalse(isShown(ZONE_FIELD), "no timezone before there is a time");

        enter(TIME_FIELD, "09:00");
        assertTrue(isShown(ZONE_FIELD));
        enter(ZONE_FIELD, "+01:00");

        assertEquals(vf.createLiteral("2026-11-16T09:00:00+01:00", XSD.DATETIME), published(np));
        assertTrue(tester.getLastResponseAsString().contains("xsd:dateTime"), "the datatype shown follows the value");
    }

    @Test
    void aFullDateAndTimeIsSubmittedInOneGo() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex03";
        startForm(np);
        enter(YEAR_FIELD, "2026");
        enter(MONTH_FIELD, "11");
        enter(DAY_FIELD, "16");
        enter(TIME_FIELD, "09:00");

        FormTester form = filledForm();
        form.setValue(ZONE_FIELD, "-05:00");
        form.submit();

        assertEquals(vf.createLiteral("2026-11-16T09:00:00-05:00", XSD.DATETIME), published(np));
    }

    @Test
    void aDateAndTimeGetsTheUsersOffsetAtThatDate() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex04";
        startForm(np);
        enter(YEAR_FIELD, "2026");
        enter(MONTH_FIELD, "07");
        enter(DAY_FIELD, "01");
        enter(TIME_FIELD, "12:00");

        String expectedZone = AjaxZonedDateTimePicker.getUserZoneId().getRules()
                .getOffset(java.time.LocalDateTime.of(2026, 7, 1, 12, 0)).getId();
        assertEquals("2026-07-01T12:00:00" + expectedZone, value());
    }

    @Test
    void partsFinerThanTheTemplateTakesAreNotOffered() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex05", XSD.GYEAR, XSD.GYEARMONTH);
        enter(YEAR_FIELD, "2026");
        enter(MONTH_FIELD, "11");

        assertFalse(isShown(DAY_FIELD), "a template that takes a year and month at most asks for no day");
    }

    @Test
    void aValueCoarserThanTheTemplateTakesIsReported() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex06", XSD.DATE, XSD.DATETIME);
        enter(YEAR_FIELD, "2026");
        assertTrue(isShown(MONTH_FIELD), "a year on its way to becoming a date is not rejected");

        FormTester form = filledForm();
        form.submit();

        assertErrorMessage("Enter a date or a date and time for 'the moment'");
    }

    @Test
    void aDayTheMonthDoesNotHaveIsReported() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex07");
        enter(YEAR_FIELD, "2026");
        enter(MONTH_FIELD, "02");

        FormTester form = filledForm();
        form.setValue(DAY_FIELD, "29");
        form.submit();

        assertErrorMessage("February 2026 has no day 29");
    }

    @Test
    void aValueGivenInTheUrlFillsThePartsIn() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex08";
        startForm(np, new PageParameters().add("param_moment", "2026-11-16"), null);

        assertEquals(vf.createLiteral("2026-11-16", XSD.DATE), published(np));
        assertTrue(isShown(DAY_FIELD));
        assertTrue(tester.getLastResponseAsString().contains("value=\"2026\""), "the year field shows it");
    }

    @Test
    void anExistingValueOfAnyAcceptedPrecisionIsTakenAsItIs() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex09");
        LiteralFlexibleDateItem item = item();

        assertTrue(item.isUnifiableWith(vf.createLiteral("2026", XSD.GYEAR)));
        assertTrue(item.isUnifiableWith(vf.createLiteral("2026-11-16T09:00:00Z", XSD.DATETIME)));
        assertFalse(item.isUnifiableWith(vf.createLiteral("2026-11-16", XSD.GYEAR)), "the datatype has to fit the value");
        assertFalse(item.isUnifiableWith(vf.createLiteral("--11-16", XSD.GMONTHDAY)));
        assertFalse(item.isUnifiableWith(vf.createLiteral("2026")), "a plain string is not a date");

        item.unifyWith(vf.createLiteral("2026-11-16T09:00:00", XSD.DATETIME));
        assertEquals("2026-11-16T09:00:00", value(), "a date and time without a timezone keeps having none");
    }

    @Test
    void anExistingValueOfAPrecisionTheTemplateDoesNotTakeIsRefused() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex10", XSD.DATE);
        LiteralFlexibleDateItem item = item();

        assertFalse(item.isUnifiableWith(vf.createLiteral("2026", XSD.GYEAR)));
        assertThrows(UnificationException.class, () -> item.unifyWith(vf.createLiteral("2026", XSD.GYEAR)));
    }

    @Test
    void aDefaultValueFillsTheFieldIn() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex11";
        startForm(np, new PageParameters(), vf.createLiteral("2026-11", XSD.GYEARMONTH));

        assertEquals(vf.createLiteral("2026-11", XSD.GYEARMONTH), published(np));
    }

    @Test
    void theBrowsersTimezoneIsTakenUpWhereATimeCanBeEntered() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex14", XSD.DATE, XSD.DATETIME);
        List<AjaxClientInfoBehavior> behaviors = tester.getComponentFromLastRenderedPage(ITEM).getBehaviors(AjaxClientInfoBehavior.class);
        assertEquals(1, behaviors.size());

        tester.executeBehavior(behaviors.get(0));

        assertEquals(200, tester.getLastResponse().getStatus());
    }

    @Test
    void noTimezoneIsAskedForWhereNoTimeCanBeEntered() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex15", XSD.GYEAR, XSD.GYEARMONTH);

        assertTrue(tester.getComponentFromLastRenderedPage(ITEM).getBehaviors(AjaxClientInfoBehavior.class).isEmpty(),
                "a template that takes a year and month at most has no timezone part to repaint");
    }

    @Test
    void theTemplateListsThePrecisionsItTakes() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex12";
        Template template = registerTemplate(np, List.of(XSD.DATETIME, XSD.GMONTHDAY, XSD.DATE), null);
        IRI moment = vf.createIRI(np + "/moment");

        assertTrue(template.isDatePlaceholder(moment));
        assertTrue(template.isLiteralPlaceholder(moment));
        assertEquals(List.of(DatePrecision.DATE, DatePrecision.DATE_TIME), template.getDatePrecisions(moment),
                "coarsest first, and a datatype a date placeholder cannot publish is left out");
        assertNull(template.getDatatype(moment), "no single datatype stands for all of them");
    }

    @Test
    void aTemplateThatListsNoPrecisionTakesThemAll() throws Exception {
        String np = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Flex13";
        Template template = registerTemplate(np, List.of(), null);

        assertEquals(DatePrecision.ALL, template.getDatePrecisions(vf.createIRI(np + "/moment")));
    }

}
