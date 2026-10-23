package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Which spaces keep pages about their people (issue #822). A view declared
 * {@code gen:appliesToInstancesOf gen:IndividualAgent} is about people and nothing else; a
 * pinned-resources list declaring {@code gen:IndividualAgent, gen:MaintainedResource,
 * gen:Space} and shown on the space's own page merely fits a person. Only the first is a
 * decision to give every person in that space a page of their own.
 */
class PartSpecificDisplayTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/iswc/2026";
    private static final String ORCID = "https://orcid.org/0000-0002-1825-0097";
    private static final Set<IRI> AGENT = Set.of(KPXL_TERMS.INDIVIDUAL_AGENT);

    /** A display matching the person, and either also matching the space or not. */
    private static ViewDisplay display(boolean alsoTheSpaceItself) {
        ViewDisplay display = mock(ViewDisplay.class);
        when(display.appliesTo(anyString(), any())).thenReturn(false);
        when(display.appliesTo(ORCID, AGENT)).thenReturn(true);
        when(display.appliesTo(SPACE_ID, Set.of(KPXL_TERMS.SPACE))).thenReturn(alsoTheSpaceItself);
        return display;
    }

    private static Space spaceShowing(List<ViewDisplay> displays) {
        Space space = mock(Space.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(SPACE_ID).when(space).getId();
        doReturn(Set.of(KPXL_TERMS.SPACE)).when(space).getOwnClasses();
        doReturn(displays).when(space).getPartLevelViewDisplays(ORCID, AGENT);
        return space;
    }

    @Test
    void aViewAboutPeopleKeepsPagesAboutThem() {
        assertTrue(spaceShowing(List.of(display(false))).hasPartSpecificDisplay(ORCID, AGENT));
    }

    @Test
    void aGenericViewTheSpaceAlsoShowsItselfDoesNot() {
        assertFalse(spaceShowing(List.of(display(true))).hasPartSpecificDisplay(ORCID, AGENT));
    }

    @Test
    void oneViewAboutThemIsEnoughAmongGenericOnes() {
        assertTrue(spaceShowing(List.of(display(true), display(false), display(true)))
                .hasPartSpecificDisplay(ORCID, AGENT));
    }

    @Test
    void nothingAtAllIsNoReasonForAPage() {
        assertFalse(spaceShowing(List.of()).hasPartSpecificDisplay(ORCID, AGENT));
    }

}
