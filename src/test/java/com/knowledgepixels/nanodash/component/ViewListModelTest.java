package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A page restored from the page store on browser refresh keeps the component tree it was
 * serialized with, so a view list that held its resource in a field rendered from that snapshot
 * rather than from the resource the repositories hold now (issue #459). Holding the resource in a
 * model, and deriving the list from it when it renders, is what makes the restored page current.
 */
class ViewListModelTest {

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

    private static AbstractResourceWithProfile resource(String id) {
        AbstractResourceWithProfile resource = mock(AbstractResourceWithProfile.class);
        when(resource.getId()).thenReturn(id);
        return resource;
    }

    /**
     * Renders the list for the first time, the way a page render does.
     *
     * @param viewList the list to render
     * @return the markup it rendered
     */
    private String renderFirst(ViewList viewList) {
        tester.startComponentInPage(viewList);
        return tester.getLastResponseAsString();
    }

    /**
     * Renders the page again, the way a browser refresh does with the stored page instance.
     *
     * @return the markup it rendered
     */
    private String renderAgain() {
        tester.startPage(tester.getLastRenderedPage());
        return tester.getLastResponseAsString();
    }

    /**
     * The view displays are read from the resource on every render, so a section added to the
     * resource after the page was built shows up when the stored page is rendered again.
     */
    @Test
    void viewDisplaysAreReadAgainOnEveryRender() {
        ViewDisplay papers = viewDisplay("Papers", "4.4.papers");
        ViewDisplay messages = viewDisplay("Messages", "4.5.messages");
        AbstractResourceWithProfile resource = resource("https://w3id.org/spaces/example");
        when(resource.getTopLevelViewDisplays()).thenReturn(List.of(papers));

        ViewList viewList = new ViewList("views", Model.of(resource));
        String first = renderFirst(viewList);
        assertTrue(first.contains("id=\"papers\""), first);
        assertFalse(first.contains("id=\"messages\""), first);

        when(resource.getTopLevelViewDisplays()).thenReturn(List.of(papers, messages));

        String second = renderAgain();
        assertTrue(second.contains("id=\"papers\""), second);
        assertTrue(second.contains("id=\"messages\""), second);
    }

    /**
     * The resource itself is read from the model on every render, so a restored page follows the
     * model to the resource the repositories hold now rather than to the one it was built with.
     */
    @Test
    void theResourceIsFollowedThroughTheModel() {
        ViewDisplay papers = viewDisplay("Papers", "4.4.papers");
        ViewDisplay messages = viewDisplay("Messages", "4.5.messages");
        AbstractResourceWithProfile before = resource("https://w3id.org/spaces/example");
        when(before.getTopLevelViewDisplays()).thenReturn(List.of(papers));
        AbstractResourceWithProfile after = resource("https://w3id.org/spaces/example");
        when(after.getTopLevelViewDisplays()).thenReturn(List.of(messages));

        Model<AbstractResourceWithProfile> model = Model.of(before);
        ViewList viewList = new ViewList("views", model);
        assertTrue(renderFirst(viewList).contains("id=\"papers\""));

        model.setObject(after);

        String second = renderAgain();
        assertTrue(second.contains("id=\"messages\""), second);
        assertFalse(second.contains("id=\"papers\""), second);
    }

    /**
     * The notice shown when a resource has nothing to display follows the resource too: it is there
     * while the resource has no view display, and gone once it has one.
     */
    @Test
    void theEmptyNoticeFollowsTheResource() {
        ViewDisplay papers = viewDisplay("Papers", "4.4.papers");
        AbstractResourceWithProfile resource = resource("https://w3id.org/spaces/example");
        when(resource.getTopLevelViewDisplays()).thenReturn(List.of());

        ViewList viewList = new ViewList("views", Model.of(resource));
        assertSame(resource, viewList.getResource());
        String first = renderFirst(viewList);

        when(resource.getTopLevelViewDisplays()).thenReturn(List.of(papers));
        String second = renderAgain();

        assertTrue(first.length() != second.length(), "the notice and the section cannot render the same");
        assertTrue(second.contains("id=\"papers\""), second);
    }

}
