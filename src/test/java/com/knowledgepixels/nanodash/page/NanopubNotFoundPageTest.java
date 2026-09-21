package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanopubLookup;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.utils.TestUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubAlreadyFinalizedException;

import static org.junit.jupiter.api.Assertions.*;

class NanopubNotFoundPageTest {

    // Well-formed nanopublication id: "RA" plus exactly 43 artifact-code characters.
    private static final String VALID_NANOPUB_URI = "https://w3id.org/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_t";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    @Test
    void getMountPathReturnsCorrectPath() {
        NanopubNotFoundPage page = new NanopubNotFoundPage(new PageParameters().add(NanopubNotFoundPage.ID_PARAM, VALID_NANOPUB_URI));
        assertEquals(NanopubNotFoundPage.MOUNT_PATH, page.getMountPath());
        assertTrue(page.isErrorPage());
    }

    @Test
    void rendersWithTheIdThatCouldNotBeResolved() {
        tester.startPage(NanopubNotFoundPage.class, new PageParameters().add(NanopubNotFoundPage.ID_PARAM, VALID_NANOPUB_URI));
        tester.assertRenderedPage(NanopubNotFoundPage.class);
        tester.assertContains(VALID_NANOPUB_URI);
        tester.assertContains("couldn't be found on the network");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, tester.getLastResponse().getStatus());
    }

    @Test
    void rendersTimedOutLookupWithGatewayTimeoutStatus() {
        tester.startPage(NanopubNotFoundPage.class, new PageParameters()
                .add(NanopubNotFoundPage.ID_PARAM, VALID_NANOPUB_URI)
                .add(NanopubNotFoundPage.TIMEOUT_PARAM, "true"));
        tester.assertRenderedPage(NanopubNotFoundPage.class);
        tester.assertContains("took too long");
        assertEquals(HttpServletResponse.SC_GATEWAY_TIMEOUT, tester.getLastResponse().getStatus());
    }

    @Test
    void unretrievableNanopubForwardsHere() {
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_NANOPUB_URI, 10_000, id -> null);
        RestartResponseException ex = assertThrows(RestartResponseException.class, () -> NanopubNotFoundPage.forwardFor(lookup));
        assertEquals(NanopubNotFoundPage.class, TestUtils.forwardedPageClass(ex));
        assertEquals(VALID_NANOPUB_URI, TestUtils.forwardedPageParameters(ex).get(NanopubNotFoundPage.ID_PARAM).toString());
    }

    @Test
    void timedOutLookupForwardsHereAndSaysSo() {
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_NANOPUB_URI, 1, id -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertEquals(NanopubLookup.Status.TIMEOUT, lookup.getStatus());
        RestartResponseException ex = assertThrows(RestartResponseException.class, () -> NanopubNotFoundPage.forwardFor(lookup));
        assertEquals(NanopubNotFoundPage.class, TestUtils.forwardedPageClass(ex));
        assertTrue(TestUtils.forwardedPageParameters(ex).get(NanopubNotFoundPage.TIMEOUT_PARAM).toBoolean(false));
    }

    // A malformed identifier is a problem with the request rather than with the network,
    // so it goes to the error page, which spells out what is wrong with it (#270).
    @Test
    void malformedIdForwardsToErrorPageWithDetails() {
        String malformedId = VALID_NANOPUB_URI.substring(0, VALID_NANOPUB_URI.length() - 1);
        NanopubLookup lookup = NanopubLookup.lookUp(malformedId);
        RestartResponseException ex = assertThrows(RestartResponseException.class, () -> NanopubNotFoundPage.forwardFor(lookup));
        assertEquals(ErrorPage.class, TestUtils.forwardedPageClass(ex));
        assertTrue(TestUtils.forwardedPageParameters(ex).get(ErrorPage.MESSAGE_PARAM).toString().contains(malformedId));
        // It is the asking user's to correct, which the error page needs to be told (#616).
        assertEquals(ErrorPage.Kind.REQUEST.getParamValue(),
                TestUtils.forwardedPageParameters(ex).get(ErrorPage.KIND_PARAM).toString());
    }

    @Test
    void foundNanopubHasNothingToForwardTo() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubLookup lookup = NanopubLookup.found(TestUtils.createNanopub(VALID_NANOPUB_URI));
        assertThrows(IllegalArgumentException.class, () -> NanopubNotFoundPage.forwardFor(lookup));
    }

}
