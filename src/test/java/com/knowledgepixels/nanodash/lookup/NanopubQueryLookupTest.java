package com.knowledgepixels.nanodash.lookup;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.QueryApiAccess;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for how {@link NanopubQueryLookup} reads the description of a query result, and which
 * query it runs for a lookup API URL.
 */
class NanopubQueryLookupTest {

    private static ApiResponseEntry withDescription(String description) {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("thing", "https://w3id.org/spaces/isws/2025");
        if (description != null) entry.add("description", description);
        return entry;
    }

    @Test
    void anHtmlDescriptionIsShownAsItsText() {
        String html = "<span>ISWS is a <b>full immersion</b> week &amp; more.\n<br />\n<br />\nISWS means networking.</span>";
        assertEquals(" - ISWS is a full immersion week & more. ISWS means networking.", NanopubQueryLookup.descriptionOf(withDescription(html)));
    }

    @Test
    void aLongDescriptionIsShortenedAfterItsMarkupIsRemoved() {
        String html = "<p>" + "a".repeat(100) + "</p>";
        assertEquals(" - " + "a".repeat(77) + "...", NanopubQueryLookup.descriptionOf(withDescription(html)));
    }

    @Test
    void aDescriptionThatIsOnlyMarkupIsLeftOut() {
        assertEquals("", NanopubQueryLookup.descriptionOf(withDescription("<br /> <span></span>")));
    }

    @Test
    void aResultWithoutADescriptionHasNone() {
        assertEquals("", NanopubQueryLookup.descriptionOf(withDescription(null)));
    }

    @Test
    void aUrlNamingTheCurrentFindThingsRunsIt() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.QUERY_API_PREFIX + QueryApiAccess.FIND_THINGS + "?type=x"));
    }

    @Test
    void aUrlNamingASupersededFindThingsRunsTheCurrentOne() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.QUERY_API_PREFIX
                        + "RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things"
                        + "?type=https://w3id.org/kpxl/gen/terms/Space"));
    }

    @Test
    void aUrlNamingTheSiblingForkHeadRunsTheCurrentOne() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.QUERY_API_PREFIX
                        + "RARqGauUpDMEA1o4KBSKC8AeP694qJjpbf7x7FOWHDfM8/find-valid-things"));
    }

    @Test
    void theOldestSupersededFindThingsIsMappedToo() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.QUERY_API_PREFIX
                        + "RARSKb2yBuB8IqiQ_6MEmdPK07XxY-c-YlsRGocHqSR8E/find-things"));
    }

    @Test
    void aUrlNamingTheChainlessFindThingsRunsTheCurrentOne() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.QUERY_API_PREFIX
                        + "RA99xFu2qrCrpOYc1zc7h0SYV4m6Z4OE530dguEhYeoOM/find-things?type=x"));
    }

    @Test
    void aUrlNamingAnotherQueryRunsThatQuery() {
        assertEquals("RAz6f1v82BCG0SjYMfHUe-m927VTVKdwvsuq1X7j1qcA8/get-something-else",
                NanopubQueryLookup.queryId(NanopubQueryLookup.QUERY_API_PREFIX
                        + "RAz6f1v82BCG0SjYMfHUe-m927VTVKdwvsuq1X7j1qcA8/get-something-else?a=b"));
    }

    @Test
    void aLegacyUrlRunsTheCurrentFindThings() {
        assertEquals(QueryApiAccess.FIND_THINGS, NanopubQueryLookup.queryId(
                NanopubQueryLookup.LEGACY_FIND_THINGS_PREFIX + "type=x&searchterm="));
    }

    /**
     * A lookup the query service doesn't answer fails as an I/O problem, so that it is logged
     * without a stack trace, not cached, and counted against the API's reachability (issue #916).
     */
    @Test
    void aLookupWithoutAnAnswerFailsWithAnIoException() {
        GrlcQuery query = mock(GrlcQuery.class);
        when(query.getEndpoint()).thenReturn(SimpleValueFactory.getInstance().createIRI("https://example.org/api/repo/full"));
        when(query.getPlaceholdersList()).thenReturn(List.of());
        List<String> values = new ArrayList<>();
        try (MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
            grlc.when(() -> GrlcQuery.get(anyString())).thenReturn(query);
            apiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean())).thenReturn(null);

            assertThrows(IOException.class, () -> new NanopubQueryLookup().lookUp(
                    NanopubQueryLookup.QUERY_API_PREFIX + QueryApiAccess.FIND_THINGS, "term", new HashMap<>(), values));
        }
        assertTrue(values.isEmpty());
    }

}
