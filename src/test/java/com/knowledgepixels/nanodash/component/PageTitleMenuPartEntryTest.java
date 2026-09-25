package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the link a part page's title menu offers to the part's own page. What the link
 * carries is the point: a context page reached under a different context shows a back-link
 * to the part instead of ending the trail (issue #697), which it can only do if the context
 * and the part travel with the link.
 */
class PageTitleMenuPartEntryTest {

    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final String SPACE = "https://w3id.org/spaces/nanopub-ecosystem-paper";

    @Test
    void pointsTheLinkAtThePartItself() {
        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, "Josiah Carberry", SPACE);

        assertEquals(ORCID, params.get("id").toString());
    }

    @Test
    void carriesTheContextAndThePart() {
        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, "Josiah Carberry", SPACE);

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

        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, "Josiah Carberry", SPACE);
        assertEquals(ORCID, params.get(NavigationContext.PART_PARAM).toString());
    }

    @Test
    void carriesNoPartWithoutAContextToCarryItUnder() {
        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, "Josiah Carberry", null);

        assertEquals(ORCID, params.get("id").toString());
        assertTrue(params.get(NavigationContext.CONTEXT_PARAM).isEmpty());
        assertTrue(params.get(NavigationContext.PART_PARAM).isEmpty());
    }

    @Test
    void omitsTheLabelWhenThereIsNone() {
        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, null, SPACE);

        assertEquals(ORCID, params.get(NavigationContext.PART_PARAM).toString());
        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

    @Test
    void omitsTheLabelWhenItIsBlank() {
        PageParameters params = PageTitleMenu.ownPageParameters(ORCID, "   ", SPACE);

        assertTrue(params.get(NavigationContext.PART_LABEL_PARAM).isEmpty());
    }

}
