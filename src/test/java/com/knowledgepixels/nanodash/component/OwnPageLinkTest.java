package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.Component;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for the link a part page offers to the part's own page. What the link carries is the
 * point: a context page reached under a different context shows a back-link to the part
 * instead of ending the trail (issue #697), which it can only do if the context and the part
 * travel with the link.
 */
class OwnPageLinkTest {

    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final String SPACE = "https://w3id.org/spaces/nanopub-ecosystem-paper";

    private WicketTester tester;
    private MockedStatic<NavigationContext> navigationContextMock;

    @BeforeEach
    void setUp() {
        // Components need an application to be constructed against, and resolving the part
        // goes over the network; what the resolved kind decides is the point here.
        tester = new WicketTester(new WicketApplication());
        navigationContextMock = mockStatic(NavigationContext.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        navigationContextMock.close();
        tester.destroy();
    }

    private void partResolvesTo(AbstractResourceWithProfile resource) {
        navigationContextMock.when(() -> NavigationContext.resolve(ORCID)).thenReturn(resource);
    }

    private String render(String partId, String partLabel, AbstractResourceWithProfile context) {
        Component link = OwnPageLink.of("ownpagelink", partId, partLabel, context);
        tester.startComponentInPage(link, Markup.of("<a wicket:id=\"ownpagelink\"></a>"));
        return tester.getLastResponseAsString();
    }

    private static AbstractResourceWithProfile contextResource() {
        AbstractResourceWithProfile context = mock(Space.class);
        when(context.getId()).thenReturn(SPACE);
        return context;
    }

    @Test
    void namesThePersonsPageForAnAgent() {
        partResolvesTo(mock(IndividualAgent.class));

        String html = render(ORCID, "Josiah Carberry", contextResource());

        assertTrue(html.contains("open user page"), html);
        assertTrue(html.contains("\uD83D\uDC64"), html);
    }

    /**
     * The part page offers the link as a visible control rather than hiding it in the title
     * menu, so the way on from a person's page inside a space to their profile is seen
     * without going looking for it (issue #822). The styling lives in the page's markup,
     * which the component itself cannot carry.
     */
    @Test
    void thePartPageOffersTheLinkAsASecondaryButton() throws IOException {
        String markup = Files.readString(Path.of(
                "src/main/java/com/knowledgepixels/nanodash/page/ResourcePartPage.html"));

        assertTrue(markup.contains("class=\"button light\""), markup);
        assertTrue(markup.contains("wicket:id=\"ownpagelink\""), markup);
    }

    @Test
    void carriesTheContextAndThePartIntoTheRenderedHref() {
        partResolvesTo(mock(IndividualAgent.class));

        String html = render(ORCID, "Josiah Carberry", contextResource());

        assertTrue(html.contains(NavigationContext.CONTEXT_PARAM + "=" + SPACE), html);
        assertTrue(html.contains(NavigationContext.PART_PARAM + "=" + ORCID), html);
        assertTrue(html.contains(NavigationContext.PART_LABEL_PARAM + "="), html);
    }

    @Test
    void namesTheResourcePageForAMaintainedResource() {
        partResolvesTo(mock(MaintainedResource.class));

        assertTrue(render(ORCID, "Some sub-resource", contextResource()).contains("open resource page"));
    }

    /**
     * A part that is no resource of its own -- a plain term defined inside the context --
     * has no page to offer, so nothing is shown.
     */
    @Test
    void showsNothingForAPartWithNoPageOfItsOwn() {
        String term = SPACE + "/r/some-term";
        navigationContextMock.when(() -> NavigationContext.resolve(term)).thenReturn(null);

        assertFalse(render(term, "Some term", contextResource()).contains("open "));
    }

    /**
     * A person the space knows but Nanodash has no introduction for still reaches this page
     * -- the space knows their role and their talks -- so the way on to their own page has
     * to be offered, or the reader is stranded (issue #822).
     */
    @Test
    void offersTheUserPageForAnOrcidNobodyHasIntroduced() {
        partResolvesTo(null);

        String html = render(ORCID, "Josiah Carberry", contextResource());

        assertTrue(html.contains("open user page"), html);
        assertTrue(html.contains("/user"), html);
        assertTrue(html.contains(NavigationContext.PART_PARAM + "=" + ORCID), html);
    }

    @Test
    void showsNothingWithoutAContext() {
        partResolvesTo(mock(IndividualAgent.class));

        assertFalse(render(ORCID, "Josiah Carberry", null).contains("open "));
    }

    @Test
    void pointsTheLinkAtThePartItself() {
        PageParameters params = OwnPageLink.ownPageParameters(ORCID, "Josiah Carberry", SPACE);

        assertEquals(ORCID, params.get("id").toString());
    }

    @Test
    void carriesTheContextAndThePart() {
        PageParameters params = OwnPageLink.ownPageParameters(ORCID, "Josiah Carberry", SPACE);

        assertEquals(SPACE, params.get(NavigationContext.CONTEXT_PARAM).toString());
        assertEquals(ORCID, params.get(NavigationContext.PART_PARAM).toString());
        assertEquals("Josiah Carberry", params.get(NavigationContext.PART_LABEL_PARAM).toString());
    }

    /**
     * The subtle one. {@link NavigationContext#withPart} drops the part when the link's
     * target id is the part itself, because within a context such a target is the part page
     * and its back-link would point at itself. This link's target id <em>is</em> the part —
     * it is the same resource on its own page — and there the part page is exactly what to
     * go back to, so the part has to survive.
     */
    @Test
    void carriesThePartEvenThoughTheTargetIsThatSameResource() {
        PageParameters viaWithPart = NavigationContext.withPart(
                new PageParameters().set("id", ORCID).set(NavigationContext.CONTEXT_PARAM, SPACE),
                ORCID, "Josiah Carberry", SPACE);
        assertTrue(viaWithPart.get(NavigationContext.PART_PARAM).isEmpty(),
                "withPart is expected to refuse this shape");

        PageParameters params = OwnPageLink.ownPageParameters(ORCID, "Josiah Carberry", SPACE);
        assertEquals(ORCID, params.get(NavigationContext.PART_PARAM).toString());
    }

    @Test
    void carriesNoPartWithoutAContextToCarryItUnder() {
        PageParameters params = OwnPageLink.ownPageParameters(ORCID, "Josiah Carberry", null);

        assertEquals(ORCID, params.get("id").toString());
        assertTrue(params.get(NavigationContext.CONTEXT_PARAM).isEmpty());
        assertTrue(params.get(NavigationContext.PART_PARAM).isEmpty());
    }

    @Test
    void omitsTheLabelWhenThereIsNone() {
        PageParameters params = OwnPageLink.ownPageParameters(ORCID, null, SPACE);

        assertEquals(ORCID, params.get(NavigationContext.PART_PARAM).toString());
        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

    @Test
    void omitsTheLabelWhenItIsBlank() {
        PageParameters params = OwnPageLink.ownPageParameters(ORCID, "   ", SPACE);

        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

}
