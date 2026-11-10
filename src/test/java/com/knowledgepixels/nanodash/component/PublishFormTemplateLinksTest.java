package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NTEMPLATE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the publish form passes over provenance and pubinfo template links of the
 * nanopublication it is filled from when they do not lead to a template of that kind (issue
 * #908), instead of showing, say, a whole assertion template as the provenance.
 */
class PublishFormTemplateLinksTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String PLAIN_TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Link11";
    private static final String WRONG_KIND_TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Link12";
    private static final String SOURCE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Link13";
    private static final IRI DERIVED_FROM = vf.createIRI("https://example.org/source-of-issue-908");

    private WicketTester tester;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        registerTemplate(PLAIN_TEMPLATE, "Template links test template", "the name");
        // Stands for the meta-templates for making provenance and pubinfo templates, which are
        // assertion templates and which the nanopublication in the issue named instead:
        registerTemplate(WRONG_KIND_TEMPLATE, "Wrong-kind test template", "the wrong-kind field");
    }

    private static void registerTemplate(String npUri, String label, String fieldLabel) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        NanopubCreator creator = new NanopubCreator(npUri);
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral(label));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral(fieldLabel));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        assertNotNull(TemplateData.get().registerTemplate(creator.finalizeNanopub()));
    }

    /**
     * Caches the nanopublication to fill the form from, so that no network lookup is needed for
     * it. It is made trusty because the form resolves the source by its artifact code.
     */
    private static String cacheSourceWithWrongLinks() throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE);
        creator.addAssertionStatement(vf.createIRI("https://example.org/thing"), RDFS.LABEL, vf.createLiteral("a thing"));
        creator.addProvenanceStatement(creator.getAssertionUri(), PROV.WAS_DERIVED_FROM, DERIVED_FROM);
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, vf.createIRI(PLAIN_TEMPLATE));
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, vf.createIRI(WRONG_KIND_TEMPLATE));
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE, vf.createIRI(WRONG_KIND_TEMPLATE));
        Nanopub np = MakeTrustyNanopub.transform(creator.finalizeNanopub());
        Utils.cacheNanopub(np);
        return np.getUri().stringValue();
    }

    private static void setSessionUser() throws Exception {
        Field f = NanodashSession.class.getDeclaredField("userIri");
        f.setAccessible(true);
        f.set(NanodashSession.get(), vf.createIRI("https://orcid.org/0000-0002-1267-0234"));
    }

    @Test
    void wrongKindTemplateLinksArePassedOver() throws Exception {
        setSessionUser();
        String source = cacheSourceWithWrongLinks();
        PublishForm form = new PublishForm("panel", new PageParameters()
                .add("template", PLAIN_TEMPLATE).add("override", source), PublishPage.class, null);
        tester.startComponentInPage(form);
        String html = tester.getLastResponseAsString();

        // The assertion template's field is there once, for the assertion, and not again as the
        // provenance or a pubinfo element:
        assertTrue(html.contains("the name"), html);
        assertFalse(html.contains("the wrong-kind field"), html);

        // The provenance still comes over in full, via the generic template:
        Method createNanopub = PublishForm.class.getDeclaredMethod("createNanopub");
        createNanopub.setAccessible(true);
        Nanopub np = (Nanopub) createNanopub.invoke(form);
        String trig = NanopubUtils.writeToString(np, RDFFormat.TRIG);
        boolean derivedFromCarried = false;
        for (Statement st : np.getProvenance()) {
            if (st.getPredicate().equals(PROV.WAS_DERIVED_FROM) && st.getObject().equals(DERIVED_FROM)) {
                derivedFromCarried = true;
            }
        }
        assertTrue(derivedFromCarried, trig);
        // And the new version names templates of the right kind, so the links are repaired:
        assertFalse(trig.contains(WRONG_KIND_TEMPLATE), trig);
    }

}
