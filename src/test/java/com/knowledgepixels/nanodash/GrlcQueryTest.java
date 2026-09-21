package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.page.ErrorPage;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.*;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteCategory;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.vocabulary.KPXL_GRLC;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static com.knowledgepixels.nanodash.utils.TestUtils.randomIri;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class GrlcQueryTest {

    private static final String NANOPUB_ID = "RA6T-YLqLnYd5XfnqR9PaGUjCzudvHdYjcG4GvOc7fdpA";
    private static final String QUERY_SUFFIX = "get-participation";
    private static final String NANOPUB_URI = "https://w3id.org/np/" + NANOPUB_ID;
    private static final String QUERY_DESCRIPTION = "This query returns all participation links.";
    private static final String QUERY_LABEL = "Get participation links";
    private static final String ENDPOINT = "https://w3id.org/np/l/nanopub-query-1.1/repo/full";

    /**
     * SPARQL with a non-breaking space (U+00A0) where a plain space belongs — the kind of
     * character that a query picks up on its way through a word processor, and that the SPARQL
     * parser rejects with nothing but a numeric character code to go on (#284).
     */
    private static final String SPARQL_WITH_NON_BREAKING_SPACE = "select ?thing where { ?thing ?p\u00A0?o }";

    /**
     * The published "Get participation links" query. It lives in the nanopub test suite rather
     * than in this repository, so that the fixture is shared with the other implementations
     * instead of being copied into each of them (#620). Its absence is a failure rather than a
     * reason to skip: it would mean the entry has been renamed or removed.
     */
    private static Nanopub queryNanopub() throws MalformedNanopubException, IOException {
        TestSuiteEntry entry = NanopubTestSuite.getLatest()
                .getByArtifactCode(NANOPUB_ID, TestSuiteCategory.VALID)
                .orElseThrow(() -> new IllegalStateException("Not in the nanopub test suite: " + NANOPUB_ID));
        return new NanopubImpl(entry.toFile(), RDFFormat.TRIG);
    }

    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        // Using reflection to clear the instance map to ensure a fresh start for each test
        var field = GrlcQuery.class.getDeclaredField("instanceMap");
        field.setAccessible(true);
        ((com.google.common.cache.Cache<?, ?>) field.get(null)).invalidateAll();
    }

    @Test
    void getFromQueryRef() {
        QueryRef ref = new QueryRef(QueryApiAccess.GET_MOST_RECENT_NANOPUBS);
        GrlcQuery query = GrlcQuery.get(ref);
        assertEquals(QueryApiAccess.GET_MOST_RECENT_NANOPUBS, query.getQueryId());
    }

    @Test
    void getNullForMissingQuery() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);

            assertNull(GrlcQuery.get(NANOPUB_URI));
        }
    }

    @Test
    void getNullForMoreThanOneQuery() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator nanopubCreator = TestUtils.getNanopubCreator();
        nanopubCreator.addAssertionStatement(anyIri, RDF.TYPE, KPXL_GRLC.GRLC_QUERY);
        nanopubCreator.addAssertionStatement(randomIri(), RDF.TYPE, KPXL_GRLC.GRLC_QUERY);

        TestUtils.fillProvenanceGraph(nanopubCreator);
        TestUtils.fillPubInfoGraph(nanopubCreator);

        Nanopub nanopub = nanopubCreator.finalizeNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);
            assertNull(GrlcQuery.get(NANOPUB_URI));
        }
    }

    @Test
    void getNullForNullId() {
        assertNull(GrlcQuery.get((String) null));
    }

    @Test
    void getNullForMalformedSparql() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(queryNanopubWith(SPARQL_WITH_NON_BREAKING_SPACE));
            assertNull(GrlcQuery.get(NANOPUB_URI));
        }
    }

    // A query whose SPARQL doesn't parse can't be loaded, and the reason is worth passing on:
    // the query is published by someone else, so the user can only be told what is wrong with
    // it (#284).
    @Test
    void loadExplainsMalformedSparql() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(queryNanopubWith(SPARQL_WITH_NON_BREAKING_SPACE));

            QueryLoadException ex = assertThrows(QueryLoadException.class, () -> GrlcQuery.load(NANOPUB_URI));
            assertTrue(ex.getMessage().contains("SPARQL code"), ex.getMessage());
            // Only the query's author can put this right, and the error page says so (#616).
            assertEquals(ErrorPage.Kind.CONTENT, ex.getKind());
            // The offending character is invisible, so naming it is the only way the author of
            // the query can find it.
            assertTrue(ex.getMessage().contains("U+00A0"), ex.getMessage());
            assertTrue(ex.getMessage().contains("NO-BREAK SPACE"), ex.getMessage());
        }
    }

    // An ordinary syntax error has nothing invisible about it, so the parser's own report is
    // passed on as it is.
    @Test
    void loadExplainsSyntaxErrorWithoutNamingACharacter() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(queryNanopubWith("select ?thing where { ?thing"));

            QueryLoadException ex = assertThrows(QueryLoadException.class, () -> GrlcQuery.load(NANOPUB_URI));
            assertTrue(ex.getMessage().contains("SPARQL code"), ex.getMessage());
            assertFalse(ex.getMessage().contains("U+"), ex.getMessage());
        }
    }

    @Test
    void loadExplainsNanopubWithoutQuery() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);

            QueryLoadException ex = assertThrows(QueryLoadException.class, () -> GrlcQuery.load(NANOPUB_URI));
            assertTrue(ex.getMessage().contains("No query found in nanopublication"), ex.getMessage());
        }
    }

    // Asking without saying for what, or with an ID that holds no artifact code, is a
    // request the asker can correct — unlike a query that is there but unusable (#616).
    @Test
    void loadThrowsForMissingId() {
        assertEquals(ErrorPage.Kind.REQUEST, assertThrows(QueryLoadException.class, () -> GrlcQuery.load(null)).getKind());
        assertEquals(ErrorPage.Kind.REQUEST, assertThrows(QueryLoadException.class, () -> GrlcQuery.load("  ")).getKind());
    }

    @Test
    void loadThrowsForIdThatIsNotAQueryId() {
        QueryLoadException ex = assertThrows(QueryLoadException.class, () -> GrlcQuery.load("https://example.com/not-a-query"));
        assertEquals(ErrorPage.Kind.REQUEST, ex.getKind());
    }

    /**
     * Builds a nanopublication holding a single query with the given SPARQL code.
     */
    private static Nanopub queryNanopubWith(String sparql) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator(NANOPUB_URI);
        IRI queryUri = iri(NANOPUB_URI + "/" + QUERY_SUFFIX);
        creator.addAssertionStatement(queryUri, RDF.TYPE, KPXL_GRLC.GRLC_QUERY);
        creator.addAssertionStatement(queryUri, KPXL_GRLC.SPARQL, literal(sparql));
        creator.addAssertionStatement(queryUri, KPXL_GRLC.ENDPOINT, iri(ENDPOINT));
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        return creator.finalizeNanopub();
    }

    @Test
    void getReturnsSingletonInstanceForSameId() {
        GrlcQuery instance1 = GrlcQuery.get(NANOPUB_URI);
        GrlcQuery instance2 = GrlcQuery.get(NANOPUB_URI);
        assertSame(instance1, instance2);
    }

    @Test
    void getQueryId() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(NANOPUB_ID + "/" + QUERY_SUFFIX, query.getQueryId());
    }

    @Test
    void getArtifactCode() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(NANOPUB_ID, query.getArtifactCode());
    }

    @Test
    void getQuerySuffix() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(QUERY_SUFFIX, query.getQuerySuffix());
    }

    @Test
    void getNanopub() throws MalformedNanopubException, IOException {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        Nanopub nanopub = queryNanopub();
        assertEquals(query.getNanopub(), nanopub);
    }

    @Test
    void getQueryUri() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(iri(NANOPUB_URI + "/" + QUERY_SUFFIX), query.getQueryUri());
    }

    @Test
    void getLabel() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(QUERY_LABEL, query.getLabel());
    }

    @Test
    void getDescription() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(QUERY_DESCRIPTION, query.getDescription());
    }

    @Test
    void getEndpoint() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertEquals(iri(ENDPOINT), query.getEndpoint());
    }

    @Test
    void getPlaceholdersList() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        assertNotNull(query.getPlaceholdersList());
        assertTrue(query.getPlaceholdersList().isEmpty(), "Expected no placeholders in the test query.");
    }

    // TODO add test with param fields

    @Test
    void createParamFields() {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        List<QueryParamField> paramFields = query.createParamFields("paramfield");
        assertNotNull(paramFields);
        assertTrue(paramFields.isEmpty(), "Expected no parameters in the test query.");
    }

    @Test
    void getSparql() throws MalformedNanopubException, IOException {
        GrlcQuery query = GrlcQuery.get(NANOPUB_URI);
        String sparql = query.getSparql();
        Nanopub nanopub = queryNanopub();
        AtomicReference<String> sparqlFromNanopub = new AtomicReference<>();
        nanopub.getAssertion().forEach(st -> {
            if (st.getPredicate().equals(KPXL_GRLC.SPARQL)) {
                sparqlFromNanopub.set(st.getObject().stringValue());
            }
        });
        assertEquals(sparql, sparqlFromNanopub.get());
    }

}