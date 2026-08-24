package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorPageTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private String renderedPage() {
        return tester.getLastResponse().getDocument();
    }

    private PageParameters params(ErrorPage.Kind kind, String message) {
        PageParameters params = new PageParameters().add(ErrorPage.KIND_PARAM, kind.getParamValue());
        if (message != null) params.add(ErrorPage.MESSAGE_PARAM, message);
        return params;
    }

    // An error nobody foresaw is Nanodash's own until something says otherwise, so it is
    // answered as one: a server error, with a way to report it (#616).
    @Test
    void rendersWithoutMessage() {
        tester.startPage(ErrorPage.class);
        tester.assertRenderedPage(ErrorPage.class);
        tester.assertContains("Something went wrong here");
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tester.getLastResponse().getStatus());
        assertTrue(renderedPage().contains("https://github.com/knowledgepixels/nanodash/issues/new"), renderedPage());
    }

    // Details about what went wrong are shown to the user, so that e.g. a mistyped
    // nanopublication identifier can be told apart from a malfunction (#270).
    @Test
    void rendersGivenMessage() {
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, "'x' is not a valid nanopublication identifier."));
        tester.assertRenderedPage(ErrorPage.class);
        tester.assertContains("is not a valid nanopublication identifier");
    }

    // Every error page says who can act on the problem, which differs per kind, and answers
    // with the HTTP status that matches it (#616).
    @Test
    void aMistypedAddressIsTheUsersToCorrect() {
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, "'x' is not a valid nanopublication identifier."));
        tester.assertContains("If you typed or edited it");
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, tester.getLastResponse().getStatus());
        // Nothing to report here: this one isn't Nanodash's to fix.
        tester.assertInvisible("report-link");
    }

    @Test
    void anInvalidPublishedQueryIsItsAuthorsToCorrect() {
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.CONTENT, "The SPARQL code of the query is not valid."));
        tester.assertContains("only its author can put it right");
        tester.assertInvisible("report-link");
        assertEquals(422, tester.getLastResponse().getStatus());
    }

    @Test
    void aMalfunctionIsOursToFixAndCanBeReported() {
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.MALFUNCTION, "Something unexpected happened."));
        tester.assertContains("on Nanodash rather than on you");
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tester.getLastResponse().getStatus());
        assertTrue(renderedPage().contains("Something+unexpected+happened"), renderedPage());
    }

    // Every error page offers a way onward, whatever kind it is (#616).
    @Test
    void alwaysLinksHome() {
        for (ErrorPage.Kind kind : ErrorPage.Kind.values()) {
            tester.startPage(ErrorPage.class, params(kind, null));
            tester.assertRenderedPage(ErrorPage.class);
            tester.assertVisible("home-link");
        }
    }

    // The container's own error handling forwards here (see web.xml); the status it settled
    // on says what kind of error it is, and is answered with unchanged (#616).
    @Test
    void takesOverTheContainersErrorStatus() {
        tester.getRequest().setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
        tester.getRequest().setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/no-such-page");
        tester.startPage(ErrorPage.class);
        tester.assertRenderedPage(ErrorPage.class);
        tester.assertContains("no page at this address");
        tester.assertContains("/no-such-page");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, tester.getLastResponse().getStatus());
    }

    // Where the user came from is a way back, so the error page isn't a dead end (#616).
    @Test
    void linksBackToTheReferringPage() {
        String referrer = "http://localhost/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_t";
        tester.getRequest().setHeader("Referer", referrer);
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, null));
        tester.assertVisible("back-link");
        assertTrue(renderedPage().contains("href=\"" + referrer + "\""), renderedPage());
    }

    // Off-site is not where the user was working, and following the referrer there would
    // take them further away rather than back.
    @Test
    void doesNotLinkBackOffSite() {
        tester.getRequest().setHeader("Referer", "https://example.org/somewhere");
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, null));
        tester.assertInvisible("back-link");
    }

    // An error page is no way back either; offering one would just loop.
    @Test
    void doesNotLinkBackToAnErrorPage() {
        tester.getRequest().setHeader("Referer", "http://localhost" + ErrorPage.MOUNT_PATH + "/500");
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, null));
        tester.assertInvisible("back-link");
    }

    @Test
    void withoutAReferrerThereIsNoWayBack() {
        tester.startPage(ErrorPage.class, params(ErrorPage.Kind.REQUEST, null));
        tester.assertInvisible("back-link");
    }

}
