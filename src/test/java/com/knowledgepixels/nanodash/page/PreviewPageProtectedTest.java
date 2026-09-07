package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.ServiceMode;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.vocabulary.NPX;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the preview page says whether the nanopublication it is about to publish is
 * protected (#671). The marker is part of the signed nanopublication by then, so the checkbox is
 * there to state that, not to change it.
 */
class PreviewPageProtectedTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private WicketTester tester;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        setRegistryIsLocal(true);
    }

    @AfterEach
    void clearProbedMode() throws Exception {
        setRegistryIsLocal(null);
    }

    private static void setRegistryIsLocal(Boolean value) throws Exception {
        Field f = ServiceMode.class.getDeclaredField("registryIsLocal");
        f.setAccessible(true);
        f.set(null, value);
    }

    private String renderPreview(String npUri, boolean isProtected) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addAssertionStatement(vf.createIRI(npUri + "/thing"), RDFS.LABEL, vf.createLiteral("a thing"));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        // Signed with a throwaway key, because the preview renders a NanopubItem, which reads the
        // signature element. Nothing is published: the nanopub only lives in the session.
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.generateKeyPair();
        Nanopub np = SignNanopub.signAndTransform(creator.finalizeNanopub(), new TransformContext(
                SignatureAlgorithm.RSA, keyPair, vf.createIRI("https://example.org/preview-tester"),
                false, false, false));

        String previewId = np.getUri().stringValue();
        NanodashSession.get().setPreviewNanopub(previewId, new NanodashSession.PreviewNanopub(
                np, new PageParameters(), null, isProtected, null));
        tester.startPage(PreviewPage.class, new PageParameters().set("id", previewId));
        return tester.getLastResponseAsString();
    }

    @Test
    void aProtectedPreviewShowsTheTickedCheckbox() throws Exception {
        String html = renderPreview("https://example.org/preview-protected/", true);

        assertTrue(html.contains("Protected nanopublication"), html);
        assertTrue(html.contains("will stay on the local instance"), html);
        // Read-only: the marker is signed into the nanopublication and cannot be taken back here.
        assertTrue(html.contains("disabled=\"disabled\""), html);
        // Nothing is openly published, so the consent checkbox is gone here too.
        assertFalse(html.contains("consentcheck"), html);
    }

    @Test
    void anOpenPreviewKeepsTheConsentCheckbox() throws Exception {
        String html = renderPreview("https://example.org/preview-open/", false);

        assertFalse(html.contains("Protected nanopublication"), html);
        assertTrue(html.contains("consentcheck"), html);
        // Same wording as the publish form on a deployment where protection is possible:
        assertTrue(html.contains("I understand that this will be openly published"), html);
    }

}
