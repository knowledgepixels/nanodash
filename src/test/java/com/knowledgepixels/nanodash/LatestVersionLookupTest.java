package com.knowledgepixels.nanodash;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How often the latest published version is asked for (issue #686). GitHub allows 60
 * unauthenticated requests an hour per address, so a lookup that fails has to be remembered as
 * such: otherwise the home page asks again on every render and keeps the limit spent.
 */
class LatestVersionLookupTest {

    @BeforeEach
    @AfterEach
    void forgetWhatWasLookedUp() throws Exception {
        set("latestVersion", null);
        set("nextVersionLookup", 0L);
    }

    private void set(String fieldName, Object value) throws Exception {
        Field f = WicketApplication.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    private long get(String fieldName) throws Exception {
        Field f = WicketApplication.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (long) f.get(null);
    }

    private HttpResponse answerWithRateLimitReset(String headerValue) {
        HttpResponse resp = mock(HttpResponse.class);
        Header header = headerValue == null ? null : new BasicHeader("x-ratelimit-reset", headerValue);
        when(resp.getFirstHeader("x-ratelimit-reset")).thenReturn(header);
        return resp;
    }

    @Test
    void theLookupIsClaimedOnceAndThenLeftAlone() {
        assertTrue(WicketApplication.claimVersionLookup());
        // A failed lookup must not be tried again straight away: that is what spent the limit.
        assertFalse(WicketApplication.claimVersionLookup());
    }

    @Test
    void nothingIsLookedUpOnceTheVersionIsKnown() throws Exception {
        set("latestVersion", "5.13.0");

        assertFalse(WicketApplication.claimVersionLookup());
        assertEquals("5.13.0", WicketApplication.getLatestVersion());
    }

    @Test
    void anUnknownVersionIsAnsweredWithNullRatherThanWaitedFor() throws Exception {
        // A lookup is not due, so this can only answer with what is held: null, and at once —
        // it is read while a page is being built, and must not go to the network there.
        set("nextVersionLookup", System.currentTimeMillis() + 60 * 60 * 1000);

        assertNull(WicketApplication.getLatestVersion());
    }

    @Test
    void aSpentRateLimitIsWaitedOutForAsLongAsItSays() throws Exception {
        long resetAt = System.currentTimeMillis() + 3 * 60 * 60 * 1000; // beyond the usual hour

        WicketApplication.postponeUntilRateLimitReset(answerWithRateLimitReset(Long.toString(resetAt / 1000)));

        assertEquals(resetAt / 1000, get("nextVersionLookup") / 1000);
        assertFalse(WicketApplication.claimVersionLookup());
    }

    @Test
    void aResetAlreadyPastDoesNotShortenTheWait() throws Exception {
        long inAnHour = System.currentTimeMillis() + 60 * 60 * 1000;
        set("nextVersionLookup", inAnHour);

        WicketApplication.postponeUntilRateLimitReset(
                answerWithRateLimitReset(Long.toString((System.currentTimeMillis() - 60000) / 1000)));

        assertEquals(inAnHour, get("nextVersionLookup"));
    }

    @Test
    void anAnswerWithoutAUsableResetHeaderLeavesTheIntervalAlone() throws Exception {
        long inAnHour = System.currentTimeMillis() + 60 * 60 * 1000;
        set("nextVersionLookup", inAnHour);

        WicketApplication.postponeUntilRateLimitReset(answerWithRateLimitReset(null));
        assertEquals(inAnHour, get("nextVersionLookup"));

        WicketApplication.postponeUntilRateLimitReset(answerWithRateLimitReset("soon"));
        assertEquals(inAnHour, get("nextVersionLookup"));
    }
}
