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

    // Behind a reverse proxy the request says only how the proxy reached this container. A
    // calendar client, or Google fetching a feed on the user's behalf, has to reach the
    // instance from outside — so the published address wins over the observed one.
    @Test
    void theOriginComesFromTheWebsiteUrlRatherThanTheRequest() {
        assertEquals("https://nanodash.example.org/calendar.ics?space=https%3A%2F%2Fw3id.org%2Fnp%2Fx",
                Utils.rebaseOnWebsiteUrl(
                        "http://127.0.1.1:37373/calendar.ics?space=https%3A%2F%2Fw3id.org%2Fnp%2Fx",
                        "https://nanodash.example.org/"));
    }

    @Test
    void aWebsiteUrlWithoutATrailingSlashWorksToo() {
        assertEquals("https://nanodash.example.org/space?id=x",
                Utils.rebaseOnWebsiteUrl("http://127.0.1.1:37373/space?id=x", "https://nanodash.example.org"));
    }

    // An instance published under a path keeps it: the proxy strips it before the container
    // sees the request, so it is missing from the rendered URL and has to be put back.
    @Test
    void aPathInTheWebsiteUrlIsPrefixed() {
        assertEquals("https://example.org/nanodash/calendar.ics?space=x",
                Utils.rebaseOnWebsiteUrl("http://127.0.1.1:37373/calendar.ics?space=x",
                        "https://example.org/nanodash/"));
    }

    // ...but not twice, when the container is mounted under that path itself and Wicket has
    // therefore already rendered it.
    @Test
    void aPathAlreadyPresentInTheRequestUrlIsNotDuplicated() {
        assertEquals("https://example.org/nanodash/calendar.ics?space=x",
                Utils.rebaseOnWebsiteUrl("http://127.0.1.1:37373/nanodash/calendar.ics?space=x",
                        "https://example.org/nanodash/"));
    }

    // A path that merely starts with the same characters is a different path.
    @Test
    void aPathThatOnlyLooksLikeThePrefixIsStillPrefixed() {
        assertEquals("https://example.org/nano/nanodash-x?id=1",
                Utils.rebaseOnWebsiteUrl("http://127.0.1.1:37373/nanodash-x?id=1", "https://example.org/nano/"));
    }

    // Without a configured address, deriving one from the request is the best available guess:
    // substituting the localhost default would be a downgrade, not a fix.
    @Test
    void anUnconfiguredWebsiteUrlLeavesTheRequestUrlAlone() {
        String url = "http://127.0.1.1:37373/calendar.ics?space=x";
        assertEquals(url, Utils.rebaseOnWebsiteUrl(url, null));
        assertEquals(url, Utils.rebaseOnWebsiteUrl(url, "  "));
    }

    @Test
    void anUnparsableWebsiteUrlLeavesTheRequestUrlAlone() {
        String url = "http://127.0.1.1:37373/calendar.ics?space=x";
        assertEquals(url, Utils.rebaseOnWebsiteUrl(url, "nanodash.example.org"));
    }

}
