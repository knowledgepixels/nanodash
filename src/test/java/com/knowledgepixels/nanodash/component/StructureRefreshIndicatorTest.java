package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spinner beside the page title while the page structure is being recalculated
 * (issue #622).
 */
class StructureRefreshIndicatorTest {

    private static int counter = 0;

    private static class TestResource extends AbstractResourceWithProfile {

        TestResource(String id) {
            super(id);
        }

        @Override
        public String getNanopubId() {
            return null;
        }

        @Override
        public Nanopub getNanopub() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public String getLabel() {
            return getId();
        }

    }

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * A resource with a loaded, non-empty structure, so that forceRefresh takes the
     * "keep what is on screen" branch. The update itself is disarmed afterwards: the
     * indicator triggers it whenever it checks, and here there is no API to reach.
     */
    @SuppressWarnings("unchecked")
    private static TestResource resourceWithStructure() throws Exception {
        TestResource r = new TestResource("https://example.org/test/indicator/" + (counter++));
        Field dataField = AbstractResourceWithProfile.class.getDeclaredField("data");
        dataField.setAccessible(true);
        Object data = dataField.get(r);
        Field viewDisplays = data.getClass().getDeclaredField("viewDisplays");
        viewDisplays.setAccessible(true);
        ((List<ViewDisplay>) viewDisplays.get(data)).add(new ViewDisplay(10));
        set(r, "dataInitialized", true);
        return r;
    }

    private static void disarmUpdate(TestResource r) throws Exception {
        set(r, "dataNeedsUpdate", false);
    }

    // Returned straight from the tester: getComponentFromLastRenderedPage hands back null
    // for a component that is not visible, which is half of what is under test here.
    private StructureRefreshIndicator start(AbstractResourceWithProfile r) {
        return (StructureRefreshIndicator) tester.startComponentInPage(new StructureRefreshIndicator("indicator", r));
    }

    @Test
    @DisplayName("no spinner and no polling when nothing is being recalculated")
    void hiddenWhenIdle() throws Exception {
        TestResource r = resourceWithStructure();
        disarmUpdate(r);

        StructureRefreshIndicator indicator = start(r);

        assertFalse(indicator.isVisible(), "nothing is refreshing, so nothing should be shown");
        assertTrue(timersOf(indicator).isEmpty(), "an idle page should carry no polling timer");
    }

    @Test
    @DisplayName("the spinner shows and polls while the structure is being recalculated")
    void shownWhileRefreshing() throws Exception {
        TestResource r = resourceWithStructure();
        r.forceRefresh(5000);
        disarmUpdate(r);

        StructureRefreshIndicator indicator = start(r);

        assertTrue(indicator.isVisible(), "the recalculation should be visible as a spinner");
        assertFalse(timersOf(indicator).isEmpty(), "the indicator should poll for the end of the refresh");
    }

    @Test
    @DisplayName("the spinner goes away when the refreshed structure lands")
    void hiddenAgainWhenRefreshLands() throws Exception {
        TestResource r = resourceWithStructure();
        r.forceRefresh(5000);
        disarmUpdate(r);

        StructureRefreshIndicator indicator = start(r);
        assertTrue(indicator.isVisible());

        set(r, "structureRefreshPending", false);
        tester.executeBehavior(timersOf(indicator).getFirst());

        assertFalse(indicator.isVisible(), "the spinner should be taken away once the refresh has landed");
    }

    @Test
    @DisplayName("a page without a resource carries no spinner")
    void noResource() {
        StructureRefreshIndicator indicator = start(null);
        assertFalse(indicator.isVisible());
        assertTrue(timersOf(indicator).isEmpty());
    }

    private static List<AbstractAjaxTimerBehavior> timersOf(StructureRefreshIndicator indicator) {
        return indicator.getBehaviors(AbstractAjaxTimerBehavior.class);
    }

}
