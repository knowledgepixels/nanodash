package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The view-display sections of a page carry fragment identifiers, so a single section
 * can be linked to (e.g. {@code .../space?id=...#messages}).
 */
class ViewListAnchorTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    private static ViewDisplay viewDisplay(String title, String structuralPosition) {
        ViewDisplay vd = mock(ViewDisplay.class);
        when(vd.getTitle()).thenReturn(title);
        when(vd.getStructuralPosition()).thenReturn(structuralPosition);
        when(vd.getDisplayWidth()).thenReturn(12);
        return vd;
    }

    @Test
    void sectionsCarryTheirFragmentIdentifier() {
        AbstractResourceWithProfile resource = mock(AbstractResourceWithProfile.class);
        when(resource.getId()).thenReturn("https://w3id.org/spaces/example");

        // The mocked view displays have no view attached, so each section body renders as
        // the inline error; the anchor is set before that and is what matters here.
        tester.startComponentInPage(new ViewList("views", resource, List.of(
                viewDisplay("Highlightings", "4.4.highlightings"),
                viewDisplay("💬 Messages", "4.5.messages"))));

        String markup = tester.getLastResponseAsString();
        assertTrue(markup.contains("id=\"highlightings\""), markup);
        assertTrue(markup.contains("id=\"messages\""), markup);
    }

}
