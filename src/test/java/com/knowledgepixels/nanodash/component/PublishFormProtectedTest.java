package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.ProtectedNanopubs;
import com.knowledgepixels.nanodash.ServiceMode;
import com.knowledgepixels.nanodash.Utils;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the protected-nanopublication option of the publish form (#671): whether it is offered,
 * whether it starts on, and when the user has no say about it.
 */
@ExtendWith(SystemStubsExtension.class)
class PublishFormProtectedTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String PLAIN_TEMPLATE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Prot01";
    private static final String PROTECTED_SOURCE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Prot02";
    private static final String PLAIN_SOURCE = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Prot03";

    @SystemStub
    private final EnvironmentVariables envVars = new EnvironmentVariables();

    private WicketTester tester;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        // The real template, so that the hard-coded ID and the template's content are tested
        // together rather than against a stand-in built to match the code.
        Nanopub protectedTemplate = new NanopubImpl(
                new File("src/test/resources/np-protected-nanopub-template.trig"), RDFFormat.TRIG);
        assertNotNull(TemplateData.get().registerTemplate(protectedTemplate));
        registerAssertionTemplate(PLAIN_TEMPLATE);
    }

    @AfterEach
    @BeforeEach
    void clearProbedModes() throws Exception {
        setRegistryIsLocal(null);
    }

    /**
     * Gives the session a user, which the fixed "Creator" publication info element needs to
     * produce a complete statement. Without it, whether the nanopublication can be built at all
     * depends on there being an ORCID in the machine's ~/.nanopub -- true on a developer box,
     * false on CI.
     */
    private static void setSessionUser() throws Exception {
        Field f = NanodashSession.class.getDeclaredField("userIri");
        f.setAccessible(true);
        f.set(NanodashSession.get(), vf.createIRI("https://orcid.org/0000-0002-1267-0234"));
    }

    private static void setRegistryIsLocal(Boolean value) throws Exception {
        Field f = ServiceMode.class.getDeclaredField("registryIsLocal");
        f.setAccessible(true);
        f.set(null, value);
    }

    private static void registerAssertionTemplate(String npUri) throws Exception {
        registerAssertionTemplate(npUri, false);
    }

    private static void registerAssertionTemplate(String npUri, boolean requiresProtection) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        IRI thing = vf.createIRI(npUri + "/thing");
        IRI name = vf.createIRI(npUri + "/name");
        NanopubCreator creator = new NanopubCreator(npUri);
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Protected option test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        if (requiresProtection) {
            creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_REQUIRED_PUBINFO_ELEMENT,
                    vf.createIRI(ProtectedNanopubs.TEMPLATE_ID));
        }
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        assertNotNull(TemplateData.get().registerTemplate(creator.finalizeNanopub()));
    }

    /**
     * Caches a nanopublication the form can be filled from, so that no network lookup is needed.
     * It is made trusty because the form resolves the source by its artifact code.
     *
     * @return the URI of the cached nanopublication
     */
    private static String cacheSource(String npUri, boolean isProtected) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addAssertionStatement(vf.createIRI(npUri + "/thing"), RDFS.LABEL, vf.createLiteral("a thing"));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, vf.createIRI(PLAIN_TEMPLATE));
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        Nanopub np = MakeTrustyNanopub.transform(creator.finalizeNanopub());
        Utils.cacheNanopub(np);
        return np.getUri().stringValue();
    }

    private String renderPublishForm(PageParameters params) {
        tester.startComponentInPage(new PublishForm("panel", params, PublishPage.class, null));
        return tester.getLastResponseAsString();
    }

    private String renderPublishForm() {
        return renderPublishForm(new PageParameters().add("template", PLAIN_TEMPLATE));
    }

    @Test
    void publicDeploymentDoesNotOfferTheOption() throws Exception {
        // A public registry has no way to store a protected nanopublication, so the option would
        // do nothing but make publishing fail.
        setRegistryIsLocal(false);
        String html = renderPublishForm();

        assertFalse(html.contains("Protected nanopublication"), html);
        // The plain consent text: nothing here needs to distinguish open from protected.
        assertTrue(html.contains("I understand that published data cannot be fully removed"), html);
        assertFalse(html.contains("openly published"), html);
    }

    @Test
    void localInstanceOffersTheOptionOffByDefault() throws Exception {
        setRegistryIsLocal(true);
        String html = renderPublishForm();

        assertTrue(html.contains("Protected nanopublication"), html);
        // The consent checkbox is what says where this goes, so the unticked box needs no note:
        assertTrue(html.contains("I understand that this will be openly published"), html);
        assertFalse(html.contains("Not protected"), html);
    }

    @Test
    void privateByDefaultDeploymentStartsProtected() throws Exception {
        setRegistryIsLocal(true);
        envVars.set("NANODASH_PROTECTED_BY_DEFAULT", "true");
        String html = renderPublishForm();

        assertTrue(html.contains("will stay on the local instance"), html);
        // Nothing is openly published here, so there is nothing to consent to and only one box:
        assertFalse(html.contains("consentcheck"), html);
        assertFalse(html.contains("openly published"), html);
    }

    @Test
    void anUnprotectedFormKeepsItsOwnConsentCheckbox() throws Exception {
        setRegistryIsLocal(true);
        String html = renderPublishForm();

        assertTrue(html.contains("consentcheck"), html);
        assertTrue(html.contains("I understand that this will be openly published"), html);
    }

    @Test
    void supersedingAProtectedNanopubStaysProtected() throws Exception {
        // Even on a deployment that publishes publicly by default: the new version repeats the
        // content of the old one, so it must not be the thing that exposes it.
        setRegistryIsLocal(true);
        String source = cacheSource(PROTECTED_SOURCE, true);
        String html = renderPublishForm(new PageParameters()
                .add("template", PLAIN_TEMPLATE).add("supersede", source));

        assertTrue(html.contains("has to be protected"), html);
        assertTrue(html.contains("the nanopublication it is based on is protected"), html);
        assertTrue(html.contains("disabled=\"disabled\""), html);
        // Not openly published, so no consent checkbox — and none to tick, since it is disabled:
        assertFalse(html.contains("consentcheck"), html);
        // The marker comes back through its own template rather than through the catch-all,
        // which is what having a real template for it is for:
        assertFalse(html.contains("Hand-coded statements"), html);
    }

    @Test
    void theMarkerEndsUpInTheNanopublication() throws Exception {
        // The point of the whole option: what registries look at is this one triple.
        setRegistryIsLocal(true);
        setSessionUser();
        envVars.set("NANODASH_PROTECTED_BY_DEFAULT", "true");
        PageParameters params = new PageParameters().add("template", PLAIN_TEMPLATE)
                .add("param_thing", "https://example.org/thing")
                .add("param_name", "a name");
        PublishForm form = new PublishForm("panel", params, PublishPage.class, null);
        tester.startComponentInPage(form);

        Method createNanopub = PublishForm.class.getDeclaredMethod("createNanopub");
        createNanopub.setAccessible(true);
        Nanopub np = (Nanopub) createNanopub.invoke(form);

        assertTrue(NanopubServerUtils.isProtectedNanopub(np), NanopubUtils.writeToString(np, RDFFormat.TRIG));
        // The nanopublication really was built from the form's own elements, not an empty shell:
        assertTrue(NanopubUtils.writeToString(np, RDFFormat.TRIG).contains("0000-0002-1267-0234"),
                NanopubUtils.writeToString(np, RDFFormat.TRIG));
    }

    @Test
    void supersedingAnOrdinaryNanopubLeavesTheChoiceFree() throws Exception {
        setRegistryIsLocal(true);
        String source = cacheSource(PLAIN_SOURCE, false);
        String html = renderPublishForm(new PageParameters()
                .add("template", PLAIN_TEMPLATE).add("supersede", source));

        assertTrue(html.contains("Protected nanopublication"), html);
        assertFalse(html.contains("has to be protected"), html);
    }

    @Test
    void aTemplateCanRequireProtection() throws Exception {
        // No new code path for "this kind of content is always protected": the assertion template
        // lists the pubinfo template in nt:hasRequiredPubinfoElement, and the checkbox follows.
        setRegistryIsLocal(true);
        String templateId = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Prot04";
        registerAssertionTemplate(templateId, true);
        String html = renderPublishForm(new PageParameters().add("template", templateId));

        assertTrue(html.contains("has to be protected"), html);
        assertTrue(html.contains("the template it is based on requires it"), html);
        assertTrue(html.contains("disabled=\"disabled\""), html);
    }

    @Test
    void theProtectedTemplateIsTheOneTheCodeExpects() throws Exception {
        // Guards the hard-coded ID against the template file being replaced by another one.
        Nanopub np = new NanopubImpl(new File("src/test/resources/np-protected-nanopub-template.trig"), RDFFormat.TRIG);
        assertTrue(np.getUri().stringValue().equals(ProtectedNanopubs.TEMPLATE_ID), np.getUri().stringValue());
    }

}
