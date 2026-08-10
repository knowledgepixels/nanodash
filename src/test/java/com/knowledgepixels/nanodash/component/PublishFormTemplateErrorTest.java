package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the publish form itself reports templates that cannot produce valid RDF, such as a
 * literal placeholder in subject position. Without this, filling in such a template silently
 * publishes something other than what the template describes.
 */
class PublishFormTemplateErrorTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    /**
     * Builds and registers a minimal assertion template with one statement, whose subject is
     * either the URI placeholder ("the thing") or the literal placeholder ("the name"). Each
     * template gets its own nanopub URI, because templates are cached by URI.
     */
    private static String registerTemplate(String npUri, boolean literalSubject) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Publish form test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, literalSubject ? name : thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        Nanopub np = creator.finalizeNanopub();
        TemplateData.get().registerTemplate(np);
        return npUri;
    }

    private String renderPublishForm(String templateId) {
        PageParameters params = new PageParameters().add("template", templateId);
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
        return tester.getLastResponseAsString();
    }

    @Test
    void literalPlaceholderInSubjectPositionIsShownAsError() throws Exception {
        String id = registerTemplate("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Pub01", true);
        String html = renderPublishForm(id);
        assertTrue(html.contains("based on an invalid template"), html);
        assertTrue(html.contains("Assertion: "), html);
        assertTrue(html.contains("subject position"), html);
        assertTrue(html.contains("&quot;the name&quot; (name)"), html);
    }

    @Test
    void invalidTemplateHidesConsentAndButtons() throws Exception {
        String id = registerTemplate("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Pub03", true);
        String html = renderPublishForm(id);
        assertFalse(html.contains("value=\"Publish\""), html);
        assertFalse(html.contains(">Preview</button>"), html);
        assertFalse(html.contains("I understand that published data"), html);
        assertTrue(html.contains("Publishing is disabled"), html);
    }

    @Test
    void wellFormedTemplateShowsNoError() throws Exception {
        String id = registerTemplate("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Pub02", false);
        String html = renderPublishForm(id);
        assertTrue(html.contains("Create Nanopublication"), html);
        // The default provenance and publication-info templates are loaded too, so this also
        // guards against the standard set of templates reporting errors:
        assertFalse(html.contains("based on an invalid template"), html);
        assertTrue(html.contains("value=\"Publish\""), html);
        assertTrue(html.contains(">Preview</button>"), html);
        assertTrue(html.contains("I understand that published data"), html);
        assertFalse(html.contains("Publishing is disabled"), html);
    }

}
