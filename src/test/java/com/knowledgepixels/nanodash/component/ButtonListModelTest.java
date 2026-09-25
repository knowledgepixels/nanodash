package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.IndividualAgent;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Which buttons a resource offers depends on the roles the current user holds there, so the button
 * list reads them when it renders rather than keeping the answer it got when the page was built
 * (issue #459).
 */
class ButtonListModelTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    private static AbstractLink button(String label) {
        Link<Void> link = new Link<Void>("button") {

            @Override
            public void onClick() {
            }

        };
        link.setBody(Model.of(label));
        return link;
    }

    /**
     * The admin-only buttons of a user's own page appear once the viewer is that user, without the
     * page being built again.
     */
    @Test
    void adminButtonsFollowTheRolesTheViewerHoldsNow() {
        IndividualAgent agent = mock(IndividualAgent.class);
        when(agent.isCurrentUser()).thenReturn(false);

        ButtonList buttonList = new ButtonList("buttons", Model.of(agent), null, null, List.of(button("edit profile")));
        tester.startComponentInPage(buttonList);
        assertFalse(tester.getLastResponseAsString().contains("edit profile"), tester.getLastResponseAsString());

        when(agent.isCurrentUser()).thenReturn(true);

        tester.startPage(tester.getLastRenderedPage());
        assertTrue(tester.getLastResponseAsString().contains("edit profile"), tester.getLastResponseAsString());
    }

    /**
     * The buttons everyone sees are shown whatever the model resolves to, including nothing.
     */
    @Test
    void plainButtonsAreShownWithoutAResource() {
        ButtonList buttonList = new ButtonList("buttons", Model.of((IndividualAgent) null), List.of(button("publish")), null, null);
        tester.startComponentInPage(buttonList);

        assertTrue(tester.getLastResponseAsString().contains("publish"), tester.getLastResponseAsString());
    }

}
