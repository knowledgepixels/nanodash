package com.knowledgepixels.nanodash.component;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsPanelTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void getLongReturnsZeroForMissingKey() {
        Map<String, String> statsMap = Map.of(StatsPanel.VALID_NP_COUNT_KEY, "10");
        StatsPanel panel = new StatsPanel("statsPanel", null, null, statsMap);

        assertEquals(0L, panel.getLong("nonExistentKey"));
    }

    @Test
    void getLongReturnsParsedValueForExistingKey() {
        Map<String, String> statsMap = Map.of(StatsPanel.VALID_NP_COUNT_KEY, "15");
        StatsPanel panel = new StatsPanel("statsPanel", null, null, statsMap);

        assertEquals(15L, panel.getLong(StatsPanel.VALID_NP_COUNT_KEY));
    }

    @Test
    void statsPanelRendersAllLabels() {
        Map<String, String> statsMap = Map.of(
                StatsPanel.VALID_NP_COUNT_KEY, "5",
                StatsPanel.INVALIDATED_NP_COUNT_KEY, "3",
                StatsPanel.ACCEPTED_NP_COUNT_KEY, "7"
        );
        StatsPanel panel = new StatsPanel("statsPanel", null, null, statsMap);

        assertTrue(panel.getOutputMarkupId());

        tester.startComponentInPage(panel);

        tester.assertComponent("statsPanel", StatsPanel.class);
        tester.assertLabel("statsPanel:latestcount", "5");
        tester.assertLabel("statsPanel:previouscount", "3");
        tester.assertLabel("statsPanel:acceptedcount", "7");
    }

    @Test
    void statsPanelHandlesMissingStatsGracefully() {
        Map<String, String> statsMap = Map.of();
        StatsPanel panel = new StatsPanel("statsPanel", null, null, statsMap);

        tester.startComponentInPage(panel);

        tester.assertComponent("statsPanel", StatsPanel.class);
        tester.assertLabel("statsPanel:latestcount", "0");
        tester.assertLabel("statsPanel:previouscount", "0");
        tester.assertLabel("statsPanel:acceptedcount", "0");
    }

}