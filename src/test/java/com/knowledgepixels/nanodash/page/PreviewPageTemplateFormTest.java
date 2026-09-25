package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests which previews carry a template form (issue #597). The type of an introduced
 * resource counts as a type of the nanopublication itself, so registering a template kind
 * as a maintained resource makes the nanopublication an {@code nt:AssertionTemplate}
 * without giving it a template body — and the preview page used to try to build a form out
 * of it, failing with a null-pointer message in the page.
 */
class PreviewPageTemplateFormTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    /**
     * Signs with a throwaway key, because the preview renders a NanopubItem, which reads the
     * signature element. Nothing is published: the nanopub only lives in the session.
     */
    private String renderPreview(Nanopub unsigned) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.generateKeyPair();
        Nanopub np = SignNanopub.signAndTransform(unsigned, new TransformContext(
                SignatureAlgorithm.RSA, keyPair, vf.createIRI("https://example.org/preview-tester"),
                false, false, false));
        String previewId = np.getUri().stringValue();
        NanodashSession.get().setPreviewNanopub(previewId, new NanodashSession.PreviewNanopub(
                np, new PageParameters(), null, false, null));
        tester.startPage(PreviewPage.class, new PageParameters().set("id", previewId));
        return tester.getLastResponseAsString();
    }

    private static NanopubCreator newCreator(String npUri) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        return creator;
    }

    /**
     * The shape that broke: the kind IRI is typed as an assertion template and declared a
     * maintained resource of a space, with no template body anywhere.
     */
    @Test
    void aTemplateKindRegistrationGetsNoFormPreview() throws Exception {
        String npUri = "https://example.org/preview-kind-registration/";
        NanopubCreator creator = newCreator(npUri);
        IRI kind = vf.createIRI(npUri + "intentionToSample");
        creator.addAssertionStatement(kind, RDF.TYPE, KPXL_TERMS.MAINTAINED_RESOURCE);
        creator.addAssertionStatement(kind, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(kind, RDFS.LABEL, vf.createLiteral("Declaring intention to sample"));
        creator.addAssertionStatement(kind, DCTERMS.DESCRIPTION, vf.createLiteral("Allows to declare the intention to sample."));
        creator.addAssertionStatement(kind, KPXL_TERMS.IS_MAINTAINED_BY, vf.createIRI("https://w3id.org/spaces/example"));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, kind));

        String html = renderPreview(creator.finalizeNanopub());

        assertFalse(html.contains("Error generating template form preview"), html);
        assertFalse(html.contains("template-form-preview"), html);
        // The preview itself is there; only the form section is left out.
        assertTrue(html.contains("Declaring intention to sample"), html);
    }

    /**
     * The same registration, with the kind IRI minted by an earlier nanopublication rather
     * than by this one — the shape seen in the wild. The template cannot be read at all here,
     * where the previous case produced a body-less one, so it takes a different route to the
     * same wrong end.
     */
    @Test
    void aTemplateKindRegisteredFromAnotherNanopubGetsNoFormPreviewEither() throws Exception {
        NanopubCreator creator = newCreator("https://example.org/preview-foreign-kind/");
        IRI kind = vf.createIRI("https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV/intentionToSample");
        creator.addAssertionStatement(kind, RDF.TYPE, KPXL_TERMS.MAINTAINED_RESOURCE);
        creator.addAssertionStatement(kind, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(kind, RDFS.LABEL, vf.createLiteral("Declaring intention to sample"));
        creator.addAssertionStatement(kind, KPXL_TERMS.IS_MAINTAINED_BY, vf.createIRI("https://w3id.org/spaces/example"));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, kind));

        String html = renderPreview(creator.finalizeNanopub());

        assertFalse(html.contains("Error generating template form preview"), html);
        assertFalse(html.contains("template-form-preview"), html);
    }

}
