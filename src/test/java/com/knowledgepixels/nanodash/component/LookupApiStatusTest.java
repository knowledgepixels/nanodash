package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LookupApis;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for issue #763: a guided choice field says whether the APIs it looks its values up in
 * can be reached, and offers to try again when one cannot.
 */
class LookupApiStatusTest {

    private static final String UNREACHABLE = "http://localhost:1/lookup?q=";

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

    private static Map<String, Boolean> reachability(Object... urlsAndOutcomes) {
        Map<String, Boolean> reachability = new LinkedHashMap<>();
        for (int i = 0; i < urlsAndOutcomes.length; i += 2) {
            reachability.put((String) urlsAndOutcomes[i], (Boolean) urlsAndOutcomes[i + 1]);
        }
        return reachability;
    }

    @Test
    void reachableApisAreSaidToBeReachable() {
        assertEquals("lookup API reachable", LookupApiStatus.getMessage(reachability("https://api.ror.org/organizations?query=", true)));
        assertEquals("lookup APIs reachable", LookupApiStatus.getMessage(reachability("https://a.org/?q=", true, "https://b.org/?q=", true)));
    }

    @Test
    void unreachableApisAreNamedByTheirHost() {
        Map<String, Boolean> reachability = reachability("https://a.org/x?q=", true, "https://b.org/x?q=", false, "https://b.org/y?q=", false, "https://c.org/?q=", false);
        assertEquals("lookup APIs not reachable: b.org, c.org", LookupApiStatus.getMessage(reachability));
    }

    @Test
    void anApiUrlWithoutAHostIsNamedByItself() {
        assertEquals("not a url", LookupApiStatus.getHost("not a url"));
        assertEquals("api.ror.org", LookupApiStatus.getHost("https://api.ror.org/organizations?query= x"));
    }

    @Test
    void aFieldWithoutApisShowsNoStatus() {
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of()));
        assertFalse(status.isVisible());
    }

    @Test
    void theApisAreCheckedAfterTheFormIsShown() {
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        assertInstanceOf(AjaxLazyLoadPanel.class, status.get(LookupApiStatus.CHECK_ID));
        tester.assertContains("checking the lookup API...");
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
    void retryChecksTheApiAgain() {
        LookupApis.checkReachability(List.of(UNREACHABLE));
        LookupApiStatus status = tester.startComponentInPage(new LookupApiStatus("status", List.of(UNREACHABLE)));
        tester.clickLink("status:" + LookupApiStatus.CHECK_ID + ":retry");
        assertFalse(LookupApis.isReachabilityKnown(List.of(UNREACHABLE)));
        assertInstanceOf(AjaxLazyLoadPanel.class, status.get(LookupApiStatus.CHECK_ID));
        assertTrue(tester.getLastResponseAsString().contains("checking the lookup API..."));
    }

}
