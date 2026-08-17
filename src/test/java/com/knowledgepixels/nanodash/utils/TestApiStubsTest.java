package com.knowledgepixels.nanodash.utils;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the stubbing itself, and with it the thing it is for: a publish form that fills in and
 * validates without a live API and without the profile of whoever is running the tests.
 * <p>
 * A form that cannot validate is what made {@code DateTimeKeepValueTest} pass locally and fail on
 * CI: Wicket only updates a form's models once the whole form validates, so a form left invalid by
 * an unfilled agent field takes a different path through the code under test.
 */
class TestApiStubsTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Stub01";

    private WicketTester tester;
    private TestApiStubs apiStubs;

    @BeforeEach
    void setUp() {
        TestProfile.install();
        apiStubs = TestApiStubs.open();
        tester = new WicketTester(new WicketApplication());
    }

    @AfterEach
    void tearDown() {
        apiStubs.close();
        tester.destroy();
    }

    /**
     * Registers a minimal template: one thing with one name.
     */
    private static void registerTemplate() throws Exception {
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI thing = vf.createIRI(NP_URI + "/thing");
        IRI name = vf.createIRI(NP_URI + "/name");
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Stub test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
    }

    /**
     * The test profile stands in for the one a developer has and CI does not, so that there is a
     * user to publish as either way.
     */
    @Test
    void thereIsAUserToPublishAs() {
        assertEquals(TestProfile.userIri(), NanodashSession.get().getUserIri());
    }

    /**
     * The point of the whole arrangement: a filled-in publish form validates, so that a test
     * submitting one exercises the same path locally and on CI.
     */
    @Test
    void aFilledInPublishFormValidates() throws Exception {
        registerTemplate();
        tester.startComponentInPage(new PublishForm("panel", new PageParameters().add("template", NP_URI), PublishPage.class, null));

        FormTester form = tester.newFormTester("panel:form");
        form.setValue("statements:0:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue("statements:0:statement:statement-group:0:statement:obj:value:textfield", "some name");
        form.submit();

        Form<?> publishForm = (Form<?>) tester.getComponentFromLastRenderedPage("panel:form");
        assertFalse(publishForm.hasError(), "the form must validate, or a submitting test takes a different path");
        // The queries the form runs were served from the stubs rather than from the network. Which
        // ones those are is not asserted: some of the data behind a form is cached for the lifetime
        // of the JVM, so a query is only asked for by whichever test happens to run first.
        assertFalse(apiStubs.requestedQueryIds().isEmpty(), "expected the form's lookups to go through the stubs");
    }

    /**
     * A query a test says nothing about answers empty, so that a lookup it does not care about
     * neither reaches the network nor fails.
     */
    @Test
    void anUnregisteredQueryAnswersEmpty() throws Exception {
        ApiResponse response = QueryApiAccess.get(new org.nanopub.extra.services.QueryRef(QueryApiAccess.GET_LATEST_USERS));
        assertTrue(response.getData().isEmpty());
    }

    /**
     * A query a test does care about is answered with what it registered.
     */
    @Test
    void aRegisteredQueryAnswersWithTheGivenRows() throws Exception {
        apiStubs.answer(QueryApiAccess.GET_LATEST_USERS,
                new String[]{"user"},
                List.<String[]>of(new String[]{"https://orcid.org/0000-0002-1825-0097"}));

        ApiResponse response = QueryApiAccess.get(new org.nanopub.extra.services.QueryRef(QueryApiAccess.GET_LATEST_USERS));

        assertEquals(1, response.getData().size());
        assertEquals("https://orcid.org/0000-0002-1825-0097", response.getData().getFirst().get("user"));
    }

}
