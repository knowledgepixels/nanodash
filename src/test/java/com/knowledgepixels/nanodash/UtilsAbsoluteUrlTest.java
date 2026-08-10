package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsAbsoluteUrlTest {

    // Wicket appends ";jsessionid=..." whenever the client has not returned a cookie — which
    // is exactly the case for a machine fetching a feed or a file. Such a URL is bound to one
    // visitor's session: it stops working when the session expires, and sharing it hands out
    // a live session identifier. It must not survive into a URL meant to outlive the request.
    @Test
    void sessionIdIsStrippedFromAPathParameter() {
        assertEquals("http://example.org/space?id=x",
                Utils.stripSessionId("http://example.org/space;jsessionid=node0abc123.node0?id=x"));
    }

    @Test
    void sessionIdIsStrippedFromAUrlWithoutAQuery() {
        assertEquals("http://example.org/space",
                Utils.stripSessionId("http://example.org/space;jsessionid=node0abc123.node0"));
    }

    @Test
    void sessionIdIsMatchedCaseInsensitively() {
        assertEquals("http://example.org/a?b=c",
                Utils.stripSessionId("http://example.org/a;JSESSIONID=XYZ?b=c"));
    }

    @Test
    void urlsWithoutASessionIdAreLeftAlone() {
        String url = "http://example.org/calendar.ics?space=https%3A%2F%2Fw3id.org%2Fnp%2Fx";
        assertEquals(url, Utils.stripSessionId(url));
    }

}
