package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
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

import java.time.ZonedDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that a date-time field keeps what the user entered when the publish form comes back with
 * an error, just like every other field on it.
 */
class DateTimeKeepValueTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String PICKER =
            "panel:form:statements:1:statement:statement-group:0:statement:obj:value:zoned-datetime";
    private static final String NAME_FIELD =
            "statements:0:statement:statement-group:0:statement:obj:value:textfield";
    private static final String DATE_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:zoned-datetime:datetime:datepicker";
    private static final String TIME_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:zoned-datetime:datetime:timepicker";

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
     * Registers a template with a plain literal ("the name") and a date-time literal ("the
     * moment"). Each test needs its own nanopub URI, because templates are cached by URI.
     */
    private static String registerTemplate(String npUri) throws Exception {
        return registerTemplate(npUri, false);
    }

    private static String registerTemplate(String npUri, boolean momentIsOptional) throws Exception {
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
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Datetime test template"));
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
        creator.addAssertionStatement(moment, NTEMPLATE.HAS_DATATYPE, XSD.DATETIME);
        creator.addAssertionStatement(moment, RDFS.LABEL, vf.createLiteral("the moment"));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return npUri;
    }

    private void startForm(String npUri) throws Exception {
        startForm(npUri, false);
    }

    private void startForm(String npUri, boolean momentIsOptional) throws Exception {
        PageParameters params = new PageParameters().add("template", registerTemplate(npUri, momentIsOptional));
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
    }

    /**
     * Fills in everything but the consent checkbox, so that submitting fails and the page comes
     * back with an error.
     */
    private FormTester filledForm() {
        FormTester form = tester.newFormTester("panel:form");
        form.setValue("statements:0:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue(NAME_FIELD, "some name");
        form.setValue("statements:1:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        return form;
    }

    private AjaxZonedDateTimePicker picker() {
        return (AjaxZonedDateTimePicker) tester.getComponentFromLastRenderedPage(PICKER);
    }

    /**
     * Returns the value the given field is rendered with, so that what the user gets to see is
     * what is asserted on.
     */
    private String renderedValue(String fieldPath) {
        String name = "name=\"" + fieldPath + "\"";
        for (String line : tester.getLastResponseAsString().split("\n")) {
            int at = line.indexOf(name);
            if (at < 0) continue;
            String before = line.substring(0, at);
            int valueAt = before.lastIndexOf("value=\"");
            if (valueAt < 0) return "";
            return before.substring(valueAt + "value=\"".length(), before.indexOf('"', valueAt + "value=\"".length()));
        }
        throw new AssertionError("field not rendered: " + fieldPath);
    }

    @Test
    void completeDateTimeSurvivesFailedPublish() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Dtm01");
        FormTester form = filledForm();
        form.setValue(DATE_FIELD, "17 Aug 2026");
        form.setValue(TIME_FIELD, "14:30");
        form.submit();

        assertEquals("some name", renderedValue(NAME_FIELD), "the other fields keep their value");
        assertEquals("17 Aug 2026", renderedValue(DATE_FIELD));
        assertEquals("14:30", renderedValue(TIME_FIELD));
        // The submitted date and time reach the model, rather than only the fields:
        assertEquals("2026-08-17T14:30", picker().getModelObject().toLocalDateTime().toString());
    }

    /**
     * A date without a time is exactly the kind of half-entered value that makes the page come
     * back with an error; the date the user typed has to still be there.
     */
    @Test
    void partialDateTimeSurvivesFailedPublish() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Dtm02");
        FormTester form = filledForm();
        form.setValue(DATE_FIELD, "17 Aug 2026");
        form.submit();

        assertEquals("some name", renderedValue(NAME_FIELD), "the other fields keep their value");
        assertEquals("17 Aug 2026", renderedValue(DATE_FIELD));
        assertEquals("", renderedValue(TIME_FIELD));
    }

    /**
     * The other way round: a date-time the user empties out must not be kept alive by the value
     * that is still in the model.
     */
    @Test
    void emptyingADateTimeClearsTheValue() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Dtm03", true);
        picker().setModelObject(ZonedDateTime.parse("2026-08-17T14:30+02:00"));

        FormTester form = filledForm();
        form.setValue(DATE_FIELD, "");
        form.setValue(TIME_FIELD, "");
        form.submit();

        assertNull(picker().getModelObject(), "an emptied date-time field clears the value");
        assertEquals("", renderedValue(DATE_FIELD));
        assertEquals("", renderedValue(TIME_FIELD));
    }

    /**
     * The same, with the rest of the form left invalid so that Wicket does not get as far as
     * updating the form's models. Clearing the value has to empty the fields by itself, or the
     * date stays on screen as a value the field no longer holds.
     */
    @Test
    void emptyingADateTimeClearsTheFieldsEvenIfTheFormIsInvalid() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Dtm05", true);
        picker().setModelObject(ZonedDateTime.parse("2026-08-17T14:30+02:00"));

        // The other fields are left empty, so the form does not validate:
        FormTester form = tester.newFormTester("panel:form");
        form.setValue(DATE_FIELD, "");
        form.setValue(TIME_FIELD, "");
        form.submit();

        assertNull(picker().getModelObject());
        assertEquals("", renderedValue(DATE_FIELD));
        assertEquals("", renderedValue(TIME_FIELD));
    }

    /**
     * An ajax request that does not carry the date and time fields (the zone dropdown, the client
     * time zone, another field on the form) must leave the entered value alone.
     */
    @Test
    void ajaxWithoutTheDateTimeFieldsKeepsTheValue() throws Exception {
        startForm("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Dtm04");
        ZonedDateTime value = ZonedDateTime.parse("2026-08-17T14:30+02:00");
        picker().setModelObject(value);

        tester.executeAjaxEvent(PICKER, "change");

        assertEquals(value, picker().getModelObject());
    }

}
