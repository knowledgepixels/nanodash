package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.utils.TestUtils;
import com.google.common.cache.Cache;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.KPXL_GRLC;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class QueryPageTest {

    private static final String NANOPUB_URI = "https://w3id.org/np/RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        var field = GrlcQuery.class.getDeclaredField("instanceMap");
        field.setAccessible(true);
        ((Cache<?, ?>) field.get(null)).invalidateAll();
    }

    // A query whose SPARQL doesn't parse used to take this page down with a
    // NullPointerException; it now says what is wrong with the query instead (#284).
    @Test
    void malformedSparqlForwardsToErrorPageWithDetails() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator(NANOPUB_URI);
        IRI queryUri = iri(NANOPUB_URI + "/get-things");
        creator.addAssertionStatement(queryUri, RDF.TYPE, KPXL_GRLC.GRLC_QUERY);
        // A non-breaking space where a plain space belongs: invisible, and rejected by the
        // SPARQL parser.
        creator.addAssertionStatement(queryUri, KPXL_GRLC.SPARQL, literal("select ?thing where { ?thing ?p\u00A0?o }"));
        creator.addAssertionStatement(queryUri, KPXL_GRLC.ENDPOINT, iri("https://w3id.org/np/l/nanopub-query-1.1/repo/full"));
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        Nanopub nanopub = creator.finalizeNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);

            RestartResponseException ex = assertThrows(RestartResponseException.class,
                    () -> new QueryPage(new PageParameters().add("id", NANOPUB_URI)));
            assertEquals(ErrorPage.class, TestUtils.forwardedPageClass(ex));
            String message = TestUtils.forwardedPageParameters(ex).get(ErrorPage.MESSAGE_PARAM).toString();
            assertTrue(message.contains("NO-BREAK SPACE"), message);
        }
    }

    // Asking for a query without saying which one is a request that can't be answered, but it
    // shouldn't take the page down either (#284).
    @Test
    void missingQueryIdForwardsToErrorPage() {
        RestartResponseException ex = assertThrows(RestartResponseException.class,
                () -> new QueryPage(new PageParameters()));
        assertEquals(ErrorPage.class, TestUtils.forwardedPageClass(ex));
    }

}
