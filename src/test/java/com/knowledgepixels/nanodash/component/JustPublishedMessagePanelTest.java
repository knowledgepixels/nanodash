package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.domain.UserData;
import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * The post-publish confirmation retires itself after a few seconds, counted down by a ring
 * around its close symbol (issue #777). The countdown lives in {@code nanodash.js} and the
 * ring in {@code style.css}, and both find their way in through this panel's markup: the
 * script looks for the box by its "transient" class and drives the ring's one circle. What
 * is checked here is that the markup those two hang off is actually rendered, and that the
 * close button still dismisses the box for someone who does not wait for the countdown.
 */
class JustPublishedMessagePanelTest {

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";

    private WicketTester tester;
    private MockedStatic<User> userMockedStatic;
    private MockedStatic<Utils> utilsMockedStatic;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        userMockedStatic = mockStatic(User.class, CALLS_REAL_METHODS);
        userMockedStatic.when(User::getUserData).thenReturn(mock(UserData.class));
        // The nanopub itself is not what is under test here; without it the panel falls back
        // to the short name from the URI, which is enough to render the message.
        utilsMockedStatic = mockStatic(Utils.class, CALLS_REAL_METHODS);
        utilsMockedStatic.when(() -> Utils.getAsNanopub(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        utilsMockedStatic.close();
        userMockedStatic.close();
        tester.destroy();
    }

    private JustPublishedMessagePanel start(PageParameters parameters) {
        return (JustPublishedMessagePanel) tester.startComponentInPage(
                new JustPublishedMessagePanel("panel", parameters));
    }

    private String render(PageParameters parameters) {
        start(parameters);
        return tester.getLastResponse().getDocument();
    }

    private static PageParameters justPublished() {
        return new PageParameters().set("just-published", NP_URI);
    }

    @Test
    void confirmationCarriesWhatTheCountdownNeeds() {
        String html = render(justPublished());
        assertTrue(html.contains("titlebar-message-box-transient"),
                "the script finds the box to count down by this class");
        assertTrue(html.contains("titlebar-message-timer-sweep"),
                "the ring's depleting circle is what shows the time left");
        assertTrue(html.contains("titlebar-message-close-cross"), "the close symbol is still there");
        assertTrue(html.contains("has been successfully published"), "the message is still said");
    }

    @Test
    void messagesThatAreNotTransientAreNotCountedDown() {
        // The introduction warning and the pending-account notice stay until what they are
        // about is dealt with, so neither may carry the marker that retires a box.
        String html = render(justPublished());
        for (String persistent : new String[]{"titlebar-message-box-warning", "titlebar-message-box-pending"}) {
            int at = html.indexOf(persistent);
            if (at < 0) continue;
            int boxStart = html.lastIndexOf("<span", at);
            assertFalse(html.substring(boxStart, at).contains("titlebar-message-box-transient"),
                    persistent + " must not be counted down");
        }
    }

    @Test
    void closeButtonStillDismissesTheConfirmation() {
        JustPublishedMessagePanel panel = start(justPublished());
        Component box = panel.get("confirmBox");
        assertTrue(box.isVisible());
        tester.executeAjaxEvent(panel.get("confirmBox:confirmClose"), "click");
        assertFalse(box.isVisible());
    }

    @Test
    void nothingIsShownWithoutAJustPublishedNanopub() {
        String html = render(new PageParameters());
        assertFalse(html.contains("titlebar-message-box-transient"));
        assertFalse(html.contains("has been successfully published"));
    }
}
