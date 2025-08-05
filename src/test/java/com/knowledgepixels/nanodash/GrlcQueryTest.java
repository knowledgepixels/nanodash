package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static com.knowledgepixels.nanodash.utils.TestUtils.randomIri;
import static org.eclipse.rdf4j.model.util.Values.iri;
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

    @AfterEach
    void tearDown() throws NoSuchFieldException, IllegalAccessException {
        // Using reflection to clear the instance map to ensure a fresh start for each test
        var field = GrlcQuery.class.getDeclaredField("instanceMap");
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(null)).clear();
    }

    @Test
    void constructorThrowsExceptionForMissingQuery() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);

            assertThrows(IllegalArgumentException.class, () -> GrlcQuery.get(NANOPUB_URI));
        }
    }

    @Test
    void constructorThrowsExceptionForMoreThanOneQuery() throws MalformedNanopubException {
        NanopubCreator nanopubCreator = TestUtils.getNanopubCreator();
        nanopubCreator.addAssertionStatement(anyIri, RDF.TYPE, GrlcQuery.GRLC_QUERY_CLASS);
        nanopubCreator.addAssertionStatement(randomIri(), RDF.TYPE, GrlcQuery.GRLC_QUERY_CLASS);

        TestUtils.fillProvenanceGraph(nanopubCreator);
        TestUtils.fillPubInfoGraph(nanopubCreator);

        Nanopub nanopub = nanopubCreator.finalizeNanopub();

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.getNanopub(any())).thenReturn(nanopub);
            assertThrows(IllegalArgumentException.class, () -> GrlcQuery.get(NANOPUB_URI));
        }
    }

    @Test
    void getThrowsExceptionForNullId() {
        assertThrows(IllegalArgumentException.class, () -> GrlcQuery.get(null));
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
        Nanopub nanopub = new NanopubImpl(new File("src/test/resources/np-grlc-query.trig"), RDFFormat.TRIG);
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
        String sparql = query.getSparql().replace("\r\n", "\n");
        Nanopub nanopub = new NanopubImpl(new File("src/test/resources/np-grlc-query.trig"), RDFFormat.TRIG);
        AtomicReference<String> sparqlFromNanopub = new AtomicReference<>();
        nanopub.getAssertion().forEach(st -> {
            if (st.getPredicate().equals(GrlcQuery.GRLC_HAS_SPARQL)) {
                sparqlFromNanopub.set(st.getObject().stringValue());
            }
        });
        assertEquals(sparql, sparqlFromNanopub.get());
    }


}