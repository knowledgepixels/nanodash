package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LookupApis;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for issue #763: a guided choice field says when an API it looks its values up in cannot
 * be reached, and offers to try again. Per issue #815, that is all it says: a successful check,
 * and one that is still running, are passed over in silence.
 */
class LookupApiStatusTest {

    private static final String UNREACHABLE = "http://localhost:1/lookup?q=";
    private static final String REACHABLE = "https://api.example.org/lookup?q=";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        LookupApis.forgetReachability(List.of(UNREACHABLE));
    }

    @AfterEach
    void tearDown() {
        LookupApis.forgetReachability(List.of(UNREACHABLE));
        tester.destroy();
    }

    @Test
    void unreachableApisAreNamedByTheirHost() {
        assertEquals("lookup API not reachable: localhost", LookupApiStatus.getMessage(List.of(UNREACHABLE)));
        List<String> unreachable = List.of("https://b.org/x?q=", "https://b.org/y?q=", "https://c.org/?q=");
        assertEquals("lookup APIs not reachable: b.org, c.org", LookupApiStatus.getMessage(unreachable));
    }

    @Test
    void anApiUrlWithoutAHostIsNamedByItself() {
        assertEquals("not a url", LookupApiStatus.getHost("not a url"));
        assertEquals("api.ror.org", LookupApiStatus.getHost("https://api.ror.org/organizations?query= x"));
    }

    @Test
    void quotationMarksAroundAValueAreRemoved() {
        assertEquals(REACHABLE, LookupApiStatus.unquote("\"" + REACHABLE + "\""));
        assertEquals(REACHABLE, LookupApiStatus.unquote(REACHABLE));
        assertEquals("\"", LookupApiStatus.unquote("\""));
        assertEquals("", LookupApiStatus.unquote("\"\""));
        assertEquals("\"half-quoted", LookupApiStatus.unquote("\"half-quoted"));
    }

    @Test
    void aFieldWithoutApisShowsNoStatus() {
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of()));
        assertFalse(status.isVisible());
    }

    @Test
    void nothingIsSaidAboutAnApiThatCanBeReached() {
        try (MockedStatic<LookupApis> lookupApis = mockStatic(LookupApis.class, CALLS_REAL_METHODS)) {
            lookupApis.when(() -> LookupApis.isReachabilityKnown(List.of(REACHABLE))).thenReturn(true);
            lookupApis.when(() -> LookupApis.checkReachability(List.of(REACHABLE))).thenReturn(Map.of(REACHABLE, true));
            LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of(REACHABLE)));
            assertFalse(status.isVisible());
            tester.assertContainsNot("reachable");
        }
    }

    @Test
    void theApisAreCheckedAfterTheFormIsShownWithoutSayingSo() {
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        assertInstanceOf(AjaxLazyLoadPanel.class, status.get(LookupApiStatus.CHECK_ID));
        assertTrue(status.isVisible(), "the line is rendered, so that the check is made");
        tester.assertContains("class=\"" + LookupApiStatus.PENDING_CLASS + "\"");
        tester.assertContainsNot("checking the lookup");
    }

    @Test
    void anUnreachableApiIsShownInRedWithARetryButton() {
        LookupApis.checkReachability(List.of(UNREACHABLE));
        tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        tester.assertContains("class=\"api-status-failed\"");
        tester.assertContains("lookup API not reachable: localhost");
        tester.assertVisible("status:" + LookupApiStatus.CHECK_ID + ":retry");
    }

    @Test
    void theOutcomeOfASilentCheckIsShownWhenAnApiCannotBeReached() {
        tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        tester.executeAllTimerBehaviors(tester.getLastRenderedPage());
        String response = tester.getLastResponseAsString();
        assertTrue(response.contains("lookup API not reachable: localhost"), response);
    }

    @Test
    void retryChecksTheApiAgainAndSaysThatItIsChecking() {
        LookupApis.checkReachability(List.of(UNREACHABLE));
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        tester.clickLink("status:" + LookupApiStatus.CHECK_ID + ":retry");
        assertFalse(LookupApis.isReachabilityKnown(List.of(UNREACHABLE)));
        assertInstanceOf(AjaxLazyLoadPanel.class, status.get(LookupApiStatus.CHECK_ID));
        assertTrue(tester.getLastResponseAsString().contains("checking the lookup API..."));
    }

}
