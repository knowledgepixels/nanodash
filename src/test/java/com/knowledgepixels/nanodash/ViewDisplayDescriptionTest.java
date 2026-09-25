package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests the explaining paragraph a view can carry below its title (issue #735): declared by
 * the view, and overridable by a display of it for one resource.
 */
class ViewDisplayDescriptionTest {

    private static final String VIEW_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADESC";
    private static final String VIEW = VIEW_NP + "/view";
    private static final String DISPLAY_WITH_DESCRIPTION_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADISP1";
    private static final String DISPLAY_PLAIN_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADISP2";
    private static final String HEADER_VIEW_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAV1";

    private static Nanopub load(String fileName) throws MalformedNanopubException, IOException {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
    }

    /**
     * Runs the given check with the fixtures standing in for the network, so no view or
     * display is fetched.
     */
    private void withFixtures(ThrowingRunnable body) throws Exception {
        Nanopub view = load("np-described-view.trig");
        Nanopub withDescription = load("np-view-display-with-description.trig");
        Nanopub plain = load("np-view-display-plain.trig");
        Nanopub undescribedView = load("np-header-view-v1.trig");
        // CALLS_REAL_METHODS: the descriptions go through the real Utils.sanitizeHtml, which
        // is what is being checked here; only the nanopub lookups are stood in for.
        try (MockedStatic<Utils> utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(VIEW_NP)).thenReturn(view);
            utils.when(() -> Utils.getAsNanopub(DISPLAY_WITH_DESCRIPTION_NP.replaceAll("^.*/", ""))).thenReturn(withDescription);
            utils.when(() -> Utils.getAsNanopub(DISPLAY_PLAIN_NP.replaceAll("^.*/", ""))).thenReturn(plain);
            utils.when(() -> Utils.getAsNanopub(HEADER_VIEW_NP)).thenReturn(undescribedView);
            body.run();
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * A description may carry markup, as descriptions elsewhere do, and is sanitized where
     * it is read rather than where it is shown — so every consumer gets safe markup.
     */
    @Test
    void aViewsOwnDescriptionIsReadAsSanitizedMarkup() throws Exception {
        withFixtures(() -> {
            View view = View.get(VIEW, false);
            String description = view.getDescription();
            assertTrue(description.contains("<em>emphasis</em>"), description);
            assertTrue(description.contains("href=\"https://example.org/more\""), description);
            assertFalse(description.contains("<script"), description);
            assertFalse(description.contains("alert(1)"), description);
            // A display that adds nothing of its own answers with the view's text.
            assertEquals(description, new ViewDisplay(view).getDescription());
        });
    }

    @Test
    void aDisplayOverridesTheViewsDescription() throws Exception {
        withFixtures(() -> {
            ViewDisplay display = ViewDisplay.get(DISPLAY_WITH_DESCRIPTION_NP + "/display", VIEW);
            assertEquals("What this view is for on this page.", display.getDescription());
            // The view itself is untouched by the override.
            assertTrue(display.getView().getDescription().contains("<em>emphasis</em>"));
        });
    }

    @Test
    void aDisplayWithoutOneFallsBackToTheView() throws Exception {
        withFixtures(() -> {
            ViewDisplay display = ViewDisplay.get(DISPLAY_PLAIN_NP + "/display", VIEW);
            assertEquals(display.getView().getDescription(), display.getDescription());
            // The title override still works the same way, which is the pattern followed.
            assertEquals("A title of its own", display.getTitle());
        });
    }

    @Test
    void noDescriptionAnywhereIsNull() throws Exception {
        withFixtures(() -> {
            View view = View.get(HEADER_VIEW_NP + "/view", false);
            assertNull(view.getDescription());
            assertNull(new ViewDisplay(view).getDescription());
        });
    }

}
