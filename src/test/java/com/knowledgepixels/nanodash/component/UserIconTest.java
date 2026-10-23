package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import org.apache.wicket.Component;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * The face a page puts on a person. A part page shows the same one as that person's own
 * page, so the two read as one resource seen from two sides (issue #822); a part that is no
 * person — a sub-space, a plain term — is not given one.
 */
class UserIconTest {

    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final String TERM = "https://w3id.org/spaces/iswc/2026/r/some-term";

    private WicketTester tester;
    private MockedStatic<IndividualAgent> agentMock;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        // Whether an ORCID is a known user is looked up over the network; what the icon
        // does with the answer is the point. An unintroduced ORCID is still a person.
        agentMock = mockStatic(IndividualAgent.class, CALLS_REAL_METHODS);
        agentMock.when(() -> IndividualAgent.isSoftware(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        agentMock.close();
        tester.destroy();
    }

    private String render(Component component) {
        tester.startComponentInPage(component, Markup.of("<img wicket:id=\"userIcon\"/>"));
        return tester.getLastResponseAsString();
    }

    @Test
    void showsThePersonSymbolForSomeoneWithoutAProfilePicture() {
        String html = render(UserIcon.ofPerson("userIcon", ORCID));

        assertTrue(html.contains("user-icon.svg"), html);
    }

    @Test
    void showsTheSoftwareSymbolForASoftwareAgent() {
        agentMock.when(() -> IndividualAgent.isSoftware(any())).thenReturn(true);

        assertTrue(render(UserIcon.ofPerson("userIcon", ORCID)).contains("bot-icon.svg"));
    }

    @Test
    void showsNothingForAPartThatIsNoPerson() {
        String html = render(UserIcon.ofPerson("userIcon", TERM));

        assertFalse(html.contains("user-icon.svg"), html);
        assertFalse(html.contains("bot-icon.svg"), html);
    }

}
