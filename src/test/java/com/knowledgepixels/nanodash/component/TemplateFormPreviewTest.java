package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
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
 * Tests that the template form preview (shown when previewing or viewing a template
 * nanopublication) reports statements that cannot produce valid RDF, such as a literal
 * placeholder in subject position.
 */
class TemplateFormPreviewTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    /**
     * Builds a minimal assertion template with one statement, whose subject is either the URI
     * placeholder ("the thing") or the literal placeholder ("the name"). Each template gets its
     * own nanopub URI, because templates are cached by URI in the global template registry.
     */
    private static Nanopub templateWithLiteralSubject(String npUri, boolean literalSubject) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Preview test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, literalSubject ? name : thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        return creator.finalizeNanopub();
    }

    @Test
    void literalPlaceholderInSubjectPositionIsShownAsError() throws Exception {
        Nanopub np = templateWithLiteralSubject("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Bad01", true);
        tester.startComponentInPage(new TemplateFormPreview("panel", np));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("This template is invalid"), html);
        assertTrue(html.contains("subject position"), html);
        assertTrue(html.contains("&quot;the name&quot; (name)"), html);
    }

    @Test
    void wellFormedTemplateShowsNoError() throws Exception {
        Nanopub np = templateWithLiteralSubject("https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Ok001", false);
        tester.startComponentInPage(new TemplateFormPreview("panel", np));
        String html = tester.getLastResponseAsString();
        assertFalse(html.contains("This template is invalid"), html);
        assertTrue(html.contains("Template Form Preview"), html);
    }

    /**
     * A plain literal (rather than a placeholder) in subject position used to be dropped at
     * parse time and then fail the whole preview with a NullPointerException.
     */
    @Test
    void literalInSubjectPositionIsShownAsErrorAndRendered() throws Exception {
        String npUri = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Bad02";
        IRI st1 = vf.createIRI(npUri + "/st1");
        NanopubCreator creator = templateCreator(npUri);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createLiteral("not a URI"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, vf.createIRI(npUri + "/name"));
        tester.startComponentInPage(new TemplateFormPreview("panel", creator.finalizeNanopub()));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("This template is invalid"), html);
        assertTrue(html.contains("subject position"), html);
        assertTrue(html.contains("not a URI"), html);
    }

    /**
     * A statement listed with nt:hasStatement but never defined used to fail the whole preview
     * with a NullPointerException; it now renders as "(missing)" plus an error message.
     */
    @Test
    void undefinedStatementIsShownAsErrorAndRendered() throws Exception {
        String npUri = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Bad03";
        tester.startComponentInPage(new TemplateFormPreview("panel", templateCreator(npUri).finalizeNanopub()));
        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("This template is invalid"), html);
        assertTrue(html.contains("no subject is defined"), html);
        assertTrue(html.contains("(missing)"), html);
    }

    /**
     * Template with a single statement st1 that the caller still has to define.
     */
    private static NanopubCreator templateCreator(String npUri) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI name = vf.createIRI(npUri + "/name");
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Preview test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        return creator;
    }

}
