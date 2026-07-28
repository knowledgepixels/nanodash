package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanopubLookup;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class ViewPageTest {

    // Well-formed nanopublication id: "RA" plus exactly 43 artifact-code characters.
    private static final String VALID_NANOPUB_URI = "https://w3id.org/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_t";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    @Test
    void getMountPathReturnsCorrectPath() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub mockNanopub = TestUtils.createNanopub();
        NanopubLookup found = NanopubLookup.found(mockNanopub);
        try (MockedStatic<NanopubLookup> lookupMock = mockStatic(NanopubLookup.class)) {
            lookupMock.when(() -> NanopubLookup.lookUp(anyString())).thenReturn(found);

            ViewPage page = new ViewPage(new PageParameters().add("id", TestUtils.NANOPUB_URI));
            assertEquals(ViewPage.MOUNT_PATH, page.getMountPath());
        }
    }

    // A nanopub that cannot be retrieved leaves this page with nothing to show, so the
    // user is sent to the page explaining that instead of to a generic error (#270).
    @Test
    void unresolvableNanopubForwardsToNotFoundPage() {
        NanopubLookup notFound = NanopubLookup.lookUp(VALID_NANOPUB_URI, 10_000, id -> null);
        assertEquals(NanopubLookup.Status.NOT_FOUND, notFound.getStatus());
        try (MockedStatic<NanopubLookup> lookupMock = mockStatic(NanopubLookup.class)) {
            lookupMock.when(() -> NanopubLookup.lookUp(anyString())).thenReturn(notFound);

            RestartResponseException ex = assertThrows(RestartResponseException.class,
                    () -> new ViewPage(new PageParameters().add("id", VALID_NANOPUB_URI)));
            assertEquals(NanopubNotFoundPage.class, TestUtils.forwardedPageClass(ex));
        }
    }

    // A mistyped or truncated identifier is a different matter: there the user gets the
    // error page, with the details of what is wrong with it (#270).
    @Test
    void malformedNanopubIdForwardsToErrorPage() {
        String malformedId = VALID_NANOPUB_URI.substring(0, VALID_NANOPUB_URI.length() - 1);
        RestartResponseException ex = assertThrows(RestartResponseException.class,
                () -> new ViewPage(new PageParameters().add("id", malformedId)));
        assertEquals(ErrorPage.class, TestUtils.forwardedPageClass(ex));
    }

}
