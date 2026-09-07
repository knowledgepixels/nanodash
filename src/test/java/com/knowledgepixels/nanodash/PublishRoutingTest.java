package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.vocabulary.NPX;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Where {@link Utils#publishNanopub(Nanopub)} sends a nanopublication (#671).
 * <p>
 * A protected one can only go to the local instance this Nanodash is configured against, and is
 * addressed to it directly so that {@code NANODASH_MAIN_REGISTRY} alone is enough. An unprotected
 * one keeps going to the library's own registry list: registries pull from their peers rather than
 * pushing to them, so sending it only to a private registry would keep it off the public network.
 */
class PublishRoutingTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final String LOCAL_REGISTRY = "http://localhost:19292/";

    @BeforeEach
    @AfterEach
    void clearResolvedState() throws Exception {
        set(ServiceMode.class, "registryIsLocal", null);
        set(Utils.class, "resolvedMainRegistryUrl", null);
    }

    private static void set(Class<?> cls, String fieldName, Object value) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Nanopub nanopub(String npUri, boolean isProtected) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addAssertionStatement(vf.createIRI(npUri + "/thing"), RDFS.LABEL, vf.createLiteral("a thing"));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        return creator.finalizeNanopub();
    }

    @Test
    void aProtectedNanopubGoesToTheConfiguredLocalInstance() throws Exception {
        set(ServiceMode.class, "registryIsLocal", true);
        set(Utils.class, "resolvedMainRegistryUrl", LOCAL_REGISTRY);
        Nanopub np = nanopub("https://example.org/publish-routing-protected/", true);

        try (MockedStatic<PublishNanopub> publish = mockStatic(PublishNanopub.class)) {
            publish.when(() -> PublishNanopub.publish(np, LOCAL_REGISTRY)).thenReturn(LOCAL_REGISTRY + "np");

            assertEquals(LOCAL_REGISTRY + "np", Utils.publishNanopub(np));
            publish.verify(() -> PublishNanopub.publish(np, LOCAL_REGISTRY));
            publish.verify(() -> PublishNanopub.publish(np), times(0));
        }
    }

    @Test
    void anUnprotectedNanopubGoesToTheLibraryList() throws Exception {
        // Even on a deployment whose own registry is a local instance: unprotected means public,
        // and the public network would never pull it out of a private registry.
        set(ServiceMode.class, "registryIsLocal", true);
        set(Utils.class, "resolvedMainRegistryUrl", LOCAL_REGISTRY);
        Nanopub np = nanopub("https://example.org/publish-routing-open/", false);

        try (MockedStatic<PublishNanopub> publish = mockStatic(PublishNanopub.class)) {
            publish.when(() -> PublishNanopub.publish(np)).thenReturn("https://registry.example.org/np");

            assertEquals("https://registry.example.org/np", Utils.publishNanopub(np));
            publish.verify(() -> PublishNanopub.publish(np));
        }
    }

    @Test
    void aProtectedNanopubOnAPublicDeploymentIsLeftToTheLibrary() throws Exception {
        // Nothing to address it to, so the library gets to refuse it with its own explicit
        // message about no registry being a local instance, rather than this posting it to a
        // public registry that will reject it.
        set(ServiceMode.class, "registryIsLocal", false);
        set(Utils.class, "resolvedMainRegistryUrl", "https://registry.knowledgepixels.com/");
        Nanopub np = nanopub("https://example.org/publish-routing-protected-public/", true);

        try (MockedStatic<PublishNanopub> publish = mockStatic(PublishNanopub.class)) {
            publish.when(() -> PublishNanopub.publish(np)).thenReturn(null);

            Utils.publishNanopub(np);
            publish.verify(() -> PublishNanopub.publish(np));
        }
    }

}
