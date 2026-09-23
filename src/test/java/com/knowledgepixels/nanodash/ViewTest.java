package com.knowledgepixels.nanodash;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class ViewTest {

    // Three versions of the same header view: the original a page might reference by a
    // hard-coded id, the version that superseded it, and the one published after that.
    private static final String NP_V0 = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAV0";
    private static final String NP_V1 = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAV1";
    private static final String NP_V2 = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAV2";
    private static final String VIEW_V0 = NP_V0 + "/view";
    private static final String VIEW_V1 = NP_V1 + "/view";
    private static final String VIEW_V2 = NP_V2 + "/view";

    private static Nanopub load(String fileName) throws MalformedNanopubException, IOException {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
    }

    @Test
    void parseMappingLiteralSplitsOnWhitespace() {
        // single mapping
        assertEquals(List.of("np:nanopubToBeRetracted"),
                View.parseMappingLiteral("np:nanopubToBeRetracted"));
        // multiple mappings share one literal (templates can't repeat the statement)
        assertEquals(List.of("derive_target:@derive-a", "local_pubkey:public-key__.1"),
                View.parseMappingLiteral("derive_target:@derive-a local_pubkey:public-key__.1"));
        // irregular whitespace is tolerated
        assertEquals(List.of("a:foo", "b:@bar"),
                View.parseMappingLiteral("  a:foo   b:@bar  "));
    }

    @Test
    void parseMappingLiteralHandlesVoidAndEmpty() {
        assertEquals(List.of(), View.parseMappingLiteral("void"));
        assertEquals(List.of(), View.parseMappingLiteral(""));
        assertEquals(List.of(), View.parseMappingLiteral("   "));
        assertEquals(List.of(), View.parseMappingLiteral(null));
        // a stray "void" token among real mappings is dropped
        assertEquals(List.of("a:foo"), View.parseMappingLiteral("a:foo void"));
    }

    /**
     * The point of issue #654: a view display shows the version its page resolved to, and
     * that version can have been superseded since. Asking for the view to be refreshed has
     * to go back to the API, even where a resolution was memoized moments ago.
     */
    /** The observers view's query, cut down to the pattern that pins the role. */
    private static final String OBSERVERS_SPARQL = """
            prefix npa: <http://purl.org/nanopub/admin/>
            prefix gen: <https://w3id.org/kpxl/gen/terms/>
            select ?user_iri where {
              graph npa:graph { npa:thisRepo npa:hasCurrentSpaceState ?g . }
              graph ?g {
                ?ri a gen:RoleInstantiation ; npa:forSpace ?_space_multi_iri ; npa:forAgent ?user_iri ;
                    gen:hasRole <https://w3id.org/np/RAqAgIgZHRhzOVArfNvPPGPPfQEUK_b_155d6vGPbMMbc/observer-role> .
              }
            }""";

    @Test
    void readsTheRoleAQueryIsPinnedTo() {
        assertEquals(
                List.of("https://w3id.org/np/RAqAgIgZHRhzOVArfNvPPGPPfQEUK_b_155d6vGPbMMbc/observer-role"),
                View.rolesPinnedBy(OBSERVERS_SPARQL).stream().map(Object::toString).toList());
    }

    @Test
    void readsEveryRoleAQueryNames() {
        String sparql = """
                prefix gen: <https://w3id.org/kpxl/gen/terms/>
                select ?a where {
                  { ?ri gen:hasRole <https://example.org/role-a> }
                  union
                  { ?ri gen:hasRole <https://example.org/role-b> }
                  optional { ?ri gen:hasRole <https://example.org/role-a> }
                }""";
        assertEquals(List.of("https://example.org/role-a", "https://example.org/role-b"),
                View.rolesPinnedBy(sparql).stream().map(Object::toString).toList());
    }

    @Test
    void aQueryThatLeavesTheRoleOpenIsPinnedToNone() {
        // A view listing every role-holder of a space works whichever roles it has, so there
        // is nothing to warn about (issue #648).
        String sparql = """
                prefix gen: <https://w3id.org/kpxl/gen/terms/>
                select ?a ?role where { ?ri gen:hasRole ?role ; gen:forAgent ?a }""";
        assertTrue(View.rolesPinnedBy(sparql).isEmpty());
    }

    @Test
    void anotherPredicateIsNotARole() {
        String sparql = """
                prefix gen: <https://w3id.org/kpxl/gen/terms/>
                select ?a where { ?s gen:hasAdmin <https://example.org/not-a-role> }""";
        assertTrue(View.rolesPinnedBy(sparql).isEmpty());
    }

    @Test
    void sparqlThatCannotBeReadPinsNothing() {
        assertTrue(View.rolesPinnedBy("select ?a where { this is not sparql").isEmpty());
        assertTrue(View.rolesPinnedBy(null).isEmpty());
        assertTrue(View.rolesPinnedBy("   ").isEmpty());
    }

    @Test
    void refreshLatestVersionPicksUpASupersedingVersion() throws Exception {
        Nanopub v1 = load("np-header-view-v1.trig");
        Nanopub v2 = load("np-header-view-v2.trig");
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_V1)).thenReturn(v1);
            utils.when(() -> Utils.getAsNanopub(NP_V2)).thenReturn(v2);
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_V1)).thenReturn(NP_V2);

            View shown = View.get(VIEW_V1, false);
            assertEquals("First version", shown.getTitle());

            View refreshed = View.refreshLatestVersion(VIEW_V1);

            assertEquals(VIEW_V2, refreshed.getId());
            assertEquals("Second version", refreshed.getTitle());
        }
    }

    /**
     * A page can reach the same view by another id — a built-in view is looked up by the id
     * hard-coded for it, which the memo maps to whatever that id resolves to. Refreshing the
     * shown version has to drop those memos too, or the next render of such a page would put
     * the superseded version back on screen.
     */
    @Test
    void refreshLatestVersionDropsMemosLeadingToTheRefreshedVersion() throws Exception {
        Nanopub v0 = load("np-header-view-v0.trig");
        Nanopub v1 = load("np-header-view-v1.trig");
        Nanopub v2 = load("np-header-view-v2.trig");
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_V0)).thenReturn(v0);
            utils.when(() -> Utils.getAsNanopub(NP_V1)).thenReturn(v1);
            utils.when(() -> Utils.getAsNanopub(NP_V2)).thenReturn(v2);
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_V1)).thenReturn(NP_V2);

            // The hard-coded id V0 was resolved to V1 a moment ago and memoized as such.
            Map<String, Pair<Long, View>> memo = new HashMap<>();
            memo.put(VIEW_V0, Pair.of(System.currentTimeMillis(), View.get(VIEW_V1, false)));
            View.importResolvedViews(memo, Long.MAX_VALUE);
            assertTrue(View.isCached(VIEW_V0));

            View.refreshLatestVersion(VIEW_V1);

            assertFalse(View.isCached(VIEW_V0));
        }
    }

}
