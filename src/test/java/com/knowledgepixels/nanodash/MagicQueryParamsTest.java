package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicQueryParamsTest {

    private static GrlcQuery queryWithPlaceholders(String... placeholders) {
        GrlcQuery q = mock(GrlcQuery.class);
        when(q.getPlaceholdersList()).thenReturn(List.of(placeholders));
        return q;
    }

    @Test
    void isMagicMatchesRegistryNamesOnly() {
        assertTrue(MagicQueryParams.isMagic("_LOCALPUBKEY_multi_val"));
        assertTrue(MagicQueryParams.isMagic("_SITEURL_multi_val"));
        assertTrue(MagicQueryParams.isMagic("_CURRENTUSER_multi_iri"));
        assertFalse(MagicQueryParams.isMagic("_user_iri"));
        // detection is registry membership; the lowercase stem isn't registered
        assertFalse(MagicQueryParams.isMagic("_localpubkey_multi_val"));
        assertFalse(MagicQueryParams.isMagic(null));
    }

    @Test
    void noMagicPlaceholdersLeavesRefUnchanged() {
        QueryRef ref = new QueryRef("RAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/q", "user", "https://example.org/u");
        assertSame(ref, MagicQueryParams.augment(ref, queryWithPlaceholders("_user_iri", "_resource")));
        assertSame(ref, MagicQueryParams.augment(ref, queryWithPlaceholders()));
    }

    @Test
    void nullSafe() {
        assertNull(MagicQueryParams.augment((QueryRef) null, queryWithPlaceholders("_SITEURL_multi_val")));
        QueryRef ref = new QueryRef("RAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/q");
        assertSame(ref, MagicQueryParams.augment(ref, (GrlcQuery) null));
    }

    @Test
    void bindsSiteUrlAndKeepsOriginalParamsWithStableKey() {
        QueryRef ref = new QueryRef("RAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/q", "resource", "https://example.org/r");
        QueryRef out = MagicQueryParams.augment(ref, queryWithPlaceholders("_SITEURL_multi_val"));
        assertNotSame(ref, out);
        String url = NanodashPreferences.get().getWebsiteUrl();
        assertTrue(out.getParams().get("SITEURL").contains(url), out.getAsUrlString());
        assertTrue(out.getParams().get("resource").contains("https://example.org/r"));
        // deterministic -> stable ApiCache key across calls
        QueryRef out2 = MagicQueryParams.augment(ref, queryWithPlaceholders("_SITEURL_multi_val"));
        assertEquals(out.getAsUrlString(), out2.getAsUrlString());
    }

    @Test
    void loggedOutLocalPubkeyIsOmitted() {
        // No Wicket session in a unit test -> LOCALPUBKEY resolves empty -> unchanged.
        QueryRef ref = new QueryRef("RAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/q", "user", "https://example.org/u");
        assertSame(ref, MagicQueryParams.augment(ref, queryWithPlaceholders("_LOCALPUBKEY_multi_val")));
    }

}
