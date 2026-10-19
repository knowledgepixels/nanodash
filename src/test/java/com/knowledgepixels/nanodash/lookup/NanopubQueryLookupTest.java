package com.knowledgepixels.nanodash.lookup;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for how {@link NanopubQueryLookup} reads the description of a query result.
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

}
