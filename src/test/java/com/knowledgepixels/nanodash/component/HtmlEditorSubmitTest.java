package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.form.FormComponent;
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
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that markup submitted from the rich-text editor is cleaned up on its way into the form
 * (issue #672). A nanopublication cannot be edited after publication, so this has to happen
 * where the value actually arrives -- with the request -- rather than when the form is built.
 */
class HtmlEditorSubmitTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NAME_FIELD =
            "statements:0:statement:statement-group:0:statement:obj:value:textfield";
    private static final String HTML_FIELD =
            "statements:1:statement:statement-group:0:statement:obj:value:editorinput";

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
     * Registers a template with a plain literal and an rdf:HTML literal, and returns its URI.
     * Each test needs its own nanopub URI, because templates are cached by URI.
     */
    private static String registerTemplate(String npUri) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI st2 = vf.createIRI(npUri + "/st2");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        IRI description = vf.createIRI(npUri + "/description");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("HTML editor test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(st2, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st2, RDF.OBJECT, description);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        creator.addAssertionStatement(description, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(description, NTEMPLATE.HAS_DATATYPE, RDF.HTML);
        creator.addAssertionStatement(description, RDFS.LABEL, vf.createLiteral("the description"));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return npUri;
    }

    /**
     * Submits the given markup as the editor would, and gives back the value the form holds
     * afterwards. The consent box is left unchecked, so the form comes back with an error
     * instead of publishing anything.
     */
    private String submitted(String npUri, String markup) throws Exception {
        PageParameters params = new PageParameters().add("template", registerTemplate(npUri));
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
        FormTester form = tester.newFormTester("panel:form");
        form.setValue("statements:0:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue(NAME_FIELD, "some name");
        form.setValue("statements:1:statement:statement-group:0:statement:subj:value:textfield", "http://example.org/thing");
        form.setValue(HTML_FIELD, markup);
        form.submit();
        return ((FormComponent<?>) tester.getComponentFromLastRenderedPage("panel:form:" + HTML_FIELD)).getValue();
    }

    @Test
    void aSpaceTypedAfterTheLastWordIsNotKept() throws Exception {
        // What the published nanopublication showed: the editor writes a trailing space as a
        // non-breaking space, and it stayed in the assertion.
        assertEquals("<div>I'm trying the editor.</div>",
                submitted("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Html1",
                        "<div>I'm trying the editor.&nbsp;</div>"));
    }

    @Test
    void scriptingIsNotKept() throws Exception {
        assertEquals("<div>Hi</div>",
                submitted("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Html2",
                        "<div onclick=\"alert('x')\">Hi</div><script>alert('x')</script>"));
    }

    @Test
    void formattingIsKept() throws Exception {
        assertEquals("<div>Text with <strong>weight</strong> and a <em>voice</em>.</div>",
                submitted("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Html3",
                        "<div>Text with <strong>weight</strong> and a <em>voice</em>.</div>"));
    }

}
