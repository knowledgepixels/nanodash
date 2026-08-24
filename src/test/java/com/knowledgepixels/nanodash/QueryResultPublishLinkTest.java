package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryResultPublishLinkTest {

    @Test
    void publishLinksAreRecognized() {
        assertTrue(QueryResult.isPublishLink("/publish"));
        assertTrue(QueryResult.isPublishLink("/publish?template=https://example.com/t"));
        // The sanitizer writes "=" as "&#61;", which only affects the parameters.
        assertTrue(QueryResult.isPublishLink("/publish?template&#61;https://example.com/t"));
    }

    @Test
    void otherLinksAreNotPublishLinks() {
        assertFalse(QueryResult.isPublishLink(null));
        assertFalse(QueryResult.isPublishLink("/query?id=https://example.com/q"));
        assertFalse(QueryResult.isPublishLink("/publisher?id=https://example.com/p"));
        assertFalse(QueryResult.isPublishLink("https://example.com/publish?template=x"));
    }

    @Test
    void publishLinksBecomeButtons() {
        String html = "<a href=\"/publish?template&#61;x\" rel=\"nofollow\">add a comment</a>";
        assertEquals("<a class=\"smallbutton button light\" href=\"/publish?template&#61;x\" rel=\"nofollow\">add a comment</a>",
                QueryResult.withPublishLinksAsButtons(html));
    }

    @Test
    void otherLinksAreLeftAlone() {
        String html = "see <a href=\"/query?id&#61;x\" rel=\"nofollow\">this query</a> and "
                      + "<a href=\"https://example.com/\" rel=\"nofollow\">this page</a>";
        assertEquals(html, QueryResult.withPublishLinksAsButtons(html));
        assertNull(QueryResult.withPublishLinksAsButtons(null));
    }

    @Test
    void everyPublishLinkInTheContentBecomesAButton() {
        String html = "<a href=\"/publish?a\">one</a> and <a href=\"/publish?b\">two</a>";
        String result = QueryResult.withPublishLinksAsButtons(html);
        assertEquals(2, result.split("smallbutton button light", -1).length - 1);
    }

    @Test
    void linksWithTheirOwnClassAreLeftAlone() {
        String html = "<a class=\"source\" href=\"/publish?a\">one</a>";
        assertEquals(html, QueryResult.withPublishLinksAsButtons(html));
    }
}
