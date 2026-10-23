package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.CrawlerRequest;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ResourcePartPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Where a link to a person leads (issue #822). Inside a space that shows something about
 * people, their name leads to their part page there — their role, their contributions —
 * rather than to the profile that reads the same wherever it is met. Outside such a
 * context it still leads to the profile.
 */
class NanodashLinkAgentRoutingTest {

    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final String SPACE = "https://w3id.org/spaces/iswc-2026";
    private static final String NAME = "Josiah Carberry";

    private WicketTester tester;
    private MockedStatic<NavigationContext> navigationContextMock;
    private MockedStatic<User> userMock;

    @BeforeEach
    void setUp() {
        // Components need an application to be constructed against.
        tester = new WicketTester(new WicketApplication());
        // Resolving the context and naming the user both go over the network; neither is
        // what these tests are about. Everything else in NavigationContext stays real —
        // the parameters the link carries are the point.
        navigationContextMock = mockStatic(NavigationContext.class, CALLS_REAL_METHODS);
        userMock = mockStatic(User.class, CALLS_REAL_METHODS);
        userMock.when(() -> User.getName(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        userMock.close();
        navigationContextMock.close();
        tester.destroy();
    }

    /** A context whose view displays render something for a person, or nothing. */
    private void contextShowsPeople(String contextId, boolean shows) {
        AbstractResourceWithProfile context = mock(Space.class);
        when(context.appliesTo(anyString(), any())).thenReturn(shows);
        navigationContextMock.when(() -> NavigationContext.resolve(contextId)).thenReturn(context);
    }

    @Test
    void aPersonInASpaceThatShowsPeopleLeadsToTheirPartPage() {
        contextShowsPeople(SPACE, true);

        Component link = NanodashLink.createLink("link", ORCID, NAME, SPACE, false, true);

        BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
        assertAll(
                () -> assertEquals(ResourcePartPage.class, internal.getPageClass()),
                () -> assertEquals(ORCID, internal.getPageParameters().get("id").toString()),
                () -> assertEquals(SPACE, internal.getPageParameters().get(NavigationContext.CONTEXT_PARAM).toString()),
                // The name the linking page had already resolved: without it the part page
                // would be titled with the bare ORCID number.
                () -> assertEquals(NAME, internal.getPageParameters().get("label").toString()));
    }

    @Test
    void aPersonInASpaceThatShowsNothingAboutThemLeadsToTheirProfile() {
        contextShowsPeople(SPACE, false);

        Component link = NanodashLink.createLink("link", ORCID, NAME, SPACE, false, true);

        BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
        assertEquals(UserPage.class, internal.getPageClass());
        assertEquals(SPACE, internal.getPageParameters().get(NavigationContext.CONTEXT_PARAM).toString());
    }

    @Test
    void aPersonMetOutsideAnyContextLeadsToTheirProfile() {
        Component link = NanodashLink.createLink("link", ORCID, NAME, null, false, true);

        BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
        assertEquals(UserPage.class, internal.getPageClass());
    }

    /**
     * A person's own page is not a part of itself: there is no part page to send them to,
     * and the link would name the same resource as its context.
     */
    @Test
    void aPersonLinkedUnderTheirOwnContextLeadsToTheirProfile() {
        contextShowsPeople(ORCID, true);

        Component link = NanodashLink.createLink("link", ORCID, NAME, ORCID, false, true);

        BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
        assertEquals(UserPage.class, internal.getPageClass());
    }

    /**
     * The part page is the one page linked here that cannot resolve without its context,
     * so the context travels even on a crawler's link, which every other page is served
     * without (issue #804). Leaving it off rendered a link that answered 500.
     */
    @Test
    void aPartLinkCarriesItsContextEvenForACrawler() {
        contextShowsPeople(SPACE, true);
        try (MockedStatic<CrawlerRequest> crawler = mockStatic(CrawlerRequest.class, CALLS_REAL_METHODS)) {
            crawler.when(CrawlerRequest::isCrawler).thenReturn(true);

            Component link = NanodashLink.createLink("link", ORCID, NAME, SPACE, false, true);

            BookmarkablePageLink<?> internal = assertInstanceOf(BookmarkablePageLink.class, link);
            assertEquals(ResourcePartPage.class, internal.getPageClass());
            assertEquals(SPACE, internal.getPageParameters().get(NavigationContext.CONTEXT_PARAM).toString());
        }
    }

    /** The same routing where there is no link component to carry it: a network view's nodes. */
    @Test
    void aNetworkNodeForAPersonGetsThePartPageUrl() {
        contextShowsPeople(SPACE, true);

        String url = NanodashLink.getPageUrl(ORCID, SPACE);

        assertTrue(url.startsWith(ResourcePartPage.MOUNT_PATH + "?"), url);
        assertEquals(ORCID, param(url, "id"));
        assertEquals(SPACE, param(url, NavigationContext.CONTEXT_PARAM));
    }

    @Test
    void aNetworkNodeForAPersonTheSpaceShowsNothingAboutGetsTheProfileUrl() {
        contextShowsPeople(SPACE, false);

        assertEquals(UserPage.MOUNT_PATH + "?id=" + URLEncoder.encode(ORCID, StandardCharsets.UTF_8),
                NanodashLink.getPageUrl(ORCID, SPACE));
    }

    /** Reads a query parameter back out of a URL a link helper built. */
    private static String param(String url, String name) {
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

}
