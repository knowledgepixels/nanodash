package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TimingStats}. The line formats are checked against the same patterns the
 * Grafana dashboard uses to take them apart, so a change that would break the dashboard fails
 * here first.
 */
class TimingStatsTest {

    private static final Pattern FETCH_LINE = Pattern.compile(
            "Query fetch stats: query=(?<query>\\S+) status=(?<status>ok|failed) thread=(?<thread>request|background) durationMs=(?<duration>\\d+)");

    private static final Pattern REQUEST_LINE = Pattern.compile(
            "Request stats: page=(?<page>\\S+) kind=(?<kind>\\S+) durationMs=(?<duration>\\d+) queryWaitMs=(?<wait>\\d+) retrievals=(?<retrievals>\\d+) inlineFetches=(?<fetches>\\d+) inlineFetchMs=(?<fetchMs>\\d+)");

    private static final Pattern DATA_BUILD_LINE = Pattern.compile(
            "Data build stats: step=(?<step>\\S+) trigger=(?<trigger>\\S+) complete=(?<complete>true|false) durationMs=(?<duration>\\d+)");

    @Test
    @DisplayName("queryOf should drop the parameters of a cache id")
    void queryOfDropsParameters() {
        assertEquals("RAabc/get-spaces", TimingStats.queryOf("RAabc/get-spaces?space=https%3A%2F%2Fexample.org"));
    }

    @Test
    @DisplayName("queryOf should keep a cache id without parameters as it is")
    void queryOfKeepsIdWithoutParameters() {
        assertEquals("RAabc/get-spaces", TimingStats.queryOf("RAabc/get-spaces"));
    }

    @Test
    @DisplayName("fetchLine should match the dashboard pattern, without the query parameters")
    void fetchLineMatchesDashboardPattern() {
        String line = TimingStats.fetchLine("RAabc/get-all-user-intros?user=x", true, false, 8123);
        Matcher matcher = FETCH_LINE.matcher(line);
        assertTrue(matcher.matches(), line);
        assertEquals("RAabc/get-all-user-intros", matcher.group("query"));
        assertEquals("ok", matcher.group("status"));
        assertEquals("background", matcher.group("thread"));
        assertEquals("8123", matcher.group("duration"));
    }

    @Test
    @DisplayName("fetchLine should report a failed fetch on a request thread")
    void fetchLineReportsFailedRequestThreadFetch() {
        Matcher matcher = FETCH_LINE.matcher(TimingStats.fetchLine("RAabc/q", false, true, 5));
        assertTrue(matcher.matches());
        assertEquals("failed", matcher.group("status"));
        assertEquals("request", matcher.group("thread"));
    }

    @Test
    @DisplayName("requestLine should match the dashboard pattern")
    void requestLineMatchesDashboardPattern() {
        Matcher matcher = REQUEST_LINE.matcher(TimingStats.requestLine("SpacePage", "render", 212, 3, 8350, 1, 2));
        assertTrue(matcher.matches());
        assertEquals("SpacePage", matcher.group("page"));
        assertEquals("render", matcher.group("kind"));
        assertEquals("212", matcher.group("duration"));
        assertEquals("3", matcher.group("wait"));
        assertEquals("8350", matcher.group("retrievals"));
        assertEquals("1", matcher.group("fetches"));
        assertEquals("2", matcher.group("fetchMs"));
    }

    @Test
    @DisplayName("dataBuildLine should match the dashboard pattern")
    void dataBuildLineMatchesDashboardPattern() {
        Matcher matcher = DATA_BUILD_LINE.matcher(TimingStats.dataBuildLine("user-data", "initial", true, 11800));
        assertTrue(matcher.matches());
        assertEquals("user-data", matcher.group("step"));
        assertEquals("initial", matcher.group("trigger"));
        assertEquals("true", matcher.group("complete"));
        assertEquals("11800", matcher.group("duration"));
    }

    @Test
    @DisplayName("timeDataBuild should return what the build returns")
    void timeDataBuildReturnsBuildResult() {
        assertTrue(TimingStats.timeDataBuild("step", "trigger", () -> true));
        assertFalse(TimingStats.timeDataBuild("step", "trigger", () -> false));
    }

    @Test
    @DisplayName("timeDataBuild should pass on an exception thrown by the build")
    void timeDataBuildPassesOnException() {
        assertThrows(IllegalStateException.class, () -> TimingStats.timeDataBuild("step", "trigger", () -> {
            throw new IllegalStateException("build failed");
        }));
    }

    @Test
    @DisplayName("recordFetch and recordSyncRetrieval should work outside a request")
    void recordingWorksOutsideRequest() {
        TimingStats.recordFetch("RAabc/q?x=1", 10, true);
        TimingStats.recordSyncRetrieval(10);
    }
}
