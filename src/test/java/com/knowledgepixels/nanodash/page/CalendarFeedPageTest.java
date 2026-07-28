package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarFeedPageTest {

    private final WicketTester tester = new WicketTester(new WicketApplication());

    // The mount path ends in ".ics" because some calendar clients decide whether a URL is
    // subscribable from its extension; this guards that Wicket still routes such a path.
    @Test
    void mountPathEndsInIcsAndResolves() {
        assertTrue(CalendarFeedPage.MOUNT_PATH.endsWith(".ics"), CalendarFeedPage.MOUNT_PATH);
        tester.executeUrl("./calendar.ics");
        assertEquals(400, tester.getLastResponse().getStatus(),
                "a feed URL without a 'space' parameter should be rejected, not routed elsewhere");
    }

}
