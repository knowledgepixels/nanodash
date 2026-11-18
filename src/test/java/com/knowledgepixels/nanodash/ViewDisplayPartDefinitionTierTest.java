package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * A view display's own {@code gen:hasPartDefinitionTier}: read off the display, and deciding
 * which parts it renders for, while displays declaring none keep the space's rule.
 */
class ViewDisplayPartDefinitionTierTest {

    private static final String VIEW_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADESC";
    private static final String VIEW = VIEW_NP + "/view";
    private static final String OPEN_DISPLAY_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADISP3";
    private static final String PLAIN_DISPLAY_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADISP2";
    private static final String MEMBER_DISPLAY_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADISP4";
    private static final String PART = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPART1/statement";
    private static final Set<IRI> STATEMENT = Set.of(Values.iri("https://schema.org/Statement"));

    private static Nanopub load(String fileName) throws Exception {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
    }

    /**
     * Stands in for fetching the given nanopublications by id. One stub for all of them:
     * stubbing an id of its own would, with the real methods called by default, go and ask
     * the registries for it while being set up.
     */
    private static void stubNanopubs(MockedStatic<Utils> utils, Map<String, Nanopub> nanopubs) {
        utils.when(() -> Utils.getAsNanopub(anyString())).thenAnswer(inv -> nanopubs.get(inv.<String>getArgument(0)));
    }

    private interface Check {
        void run(ViewDisplay open, ViewDisplay plain) throws Exception;
    }

    private void withDisplays(Check check) throws Exception {
        Nanopub view = load("np-described-view.trig");
        Nanopub open = load("np-view-display-open-parts.trig");
        Nanopub plain = load("np-view-display-plain.trig");
        try (MockedStatic<Utils> utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            stubNanopubs(utils, Map.of(VIEW_NP, view,
                    OPEN_DISPLAY_NP.replaceAll("^.*/", ""), open,
                    PLAIN_DISPLAY_NP.replaceAll("^.*/", ""), plain));
            check.run(ViewDisplay.get(OPEN_DISPLAY_NP + "/display", VIEW), ViewDisplay.get(PLAIN_DISPLAY_NP + "/display", VIEW));
        }
    }

    @Test
    void theTierIsReadOffTheDisplay() throws Exception {
        withDisplays((open, plain) -> {
            assertEquals(KPXL_TERMS.EVERYONE_ROLE, open.getPartDefinitionTier());
            assertNull(plain.getPartDefinitionTier());
        });
    }

    @Test
    void aDisplayOpenToEveryoneAcceptsAnyDefinition() throws Exception {
        withDisplays((open, plain) -> {
            assertTrue(open.acceptsPartDefinition(TierRanks.ANYONE, TierRanks.EVERYONE));
            assertTrue(open.acceptsPartDefinition(TierRanks.ANYONE, 2), "whatever the space requires");
        });
    }

    @Test
    void aDisplayDeclaringNoTierKeepsTheSpacesRule() throws Exception {
        withDisplays((open, plain) -> {
            assertFalse(plain.acceptsPartDefinition(TierRanks.ANYONE, TierRanks.EVERYONE));
            assertTrue(plain.acceptsPartDefinition(TierRanks.EVERYONE, TierRanks.EVERYONE));
            assertTrue(plain.acceptsPartDefinition(2, 2));
            assertFalse(plain.acceptsPartDefinition(1, 2), "an observer's definition in a member-tier space");
        });
    }

    /**
     * A display can only lower the bar: a tier at or above the space's counts as none, so
     * that the lowest-ranking of those the space accepts cannot take the display off a part's
     * page by publishing a newer definition of it.
     */
    @Test
    void aDisplayTierAtOrAboveTheSpacesHasNoEffect() throws Exception {
        Nanopub view = load("np-described-view.trig");
        Nanopub member = load("np-view-display-member-parts.trig");
        try (MockedStatic<Utils> utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            stubNanopubs(utils, Map.of(VIEW_NP, view, MEMBER_DISPLAY_NP.replaceAll("^.*/", ""), member));
            ViewDisplay display = ViewDisplay.get(MEMBER_DISPLAY_NP + "/display", VIEW);
            assertEquals(KPXL_TERMS.MEMBER_ROLE, display.getPartDefinitionTier());

            // Above the space's rule (every role-holder): an observer's definition is as good
            // as the space says it is.
            assertTrue(display.acceptsPartDefinition(1, TierRanks.EVERYONE));
            assertTrue(display.acceptsPartDefinition(TierRanks.EVERYONE, TierRanks.EVERYONE));
            assertFalse(display.acceptsPartDefinition(TierRanks.ANYONE, TierRanks.EVERYONE));
            // Below the space's rule (admins only), it opens the display to members.
            assertTrue(display.acceptsPartDefinition(2, TierRanks.ADMIN));
            assertFalse(display.acceptsPartDefinition(1, TierRanks.ADMIN));
        }
    }

    /**
     * The part page's own selection: for a definition by someone without a role, the display
     * that opened itself to anyone renders, and so do the displays that apply to the part
     * whatever its definition says, as they did before it had one; a display that applies
     * only through what the definition declares does not. With no rank given, nothing is
     * filtered.
     */
    @Test
    void thePartPageRendersOnlyTheDisplaysAcceptingTheDefinition() {
        ViewDisplay open = display("https://example.org/kind/open", true, false);
        ViewDisplay plain = display("https://example.org/kind/plain", false, false);
        ViewDisplay targeted = display("https://example.org/kind/targeted", false, true);
        Space space = mock(Space.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(null).when(space).triggerDataUpdate();
        doReturn(space).when(space).getSpace();
        doReturn(TierRanks.EVERYONE).when(space).getPartDefinitionTierRank();
        doReturn(List.of(open, plain, targeted)).when(space).getViewDisplays();

        // Whether the part is an agent is asked of the user data, which is not loaded here.
        try (MockedStatic<IndividualAgent> agents = mockStatic(IndividualAgent.class)) {
            assertEquals(List.of(open, targeted), space.getPartLevelViewDisplays(PART, STATEMENT, TierRanks.ANYONE));
            assertEquals(List.of(open, plain, targeted), space.getPartLevelViewDisplays(PART, STATEMENT, TierRanks.EVERYONE));
            assertEquals(List.of(open, plain, targeted), space.getPartLevelViewDisplays(PART, STATEMENT, Integer.MAX_VALUE));
            assertEquals(List.of(open, plain, targeted), space.getPartLevelViewDisplays(List.of(open, plain, targeted), PART, STATEMENT));
            assertTrue(space.appliesTo(PART, STATEMENT, TierRanks.ANYONE));
        }
    }

    /**
     * A display as the part page sees it: applying to the part by the class its definition
     * declares, or, when {@code targetsThePart}, whatever the definition says.
     */
    private static ViewDisplay display(String kind, boolean openToAnyone, boolean targetsThePart) {
        ViewDisplay display = mock(ViewDisplay.class);
        when(display.getViewKindIri()).thenReturn(Values.iri(kind));
        when(display.hasType(any())).thenReturn(false);
        when(display.getStructuralPosition()).thenReturn("5.5." + kind.replaceAll("^.*/", ""));
        when(display.compareTo(any())).thenCallRealMethod();
        when(display.appliesTo(anyString(), any())).thenAnswer(inv -> {
            Set<IRI> classes = inv.getArgument(1);
            return targetsThePart || (classes != null && !classes.isEmpty());
        });
        when(display.acceptsPartDefinition(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> {
                    int signerRank = inv.getArgument(0);
                    int baseRank = inv.getArgument(1);
                    return signerRank >= (openToAnyone ? TierRanks.ANYONE : baseRank);
                });
        return display;
    }

}
