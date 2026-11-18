package com.knowledgepixels.nanodash;

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
    private static final String PART = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPART1/statement";
    private static final Set<IRI> STATEMENT = Set.of(Values.iri("https://schema.org/Statement"));

    private static Nanopub load(String fileName) throws Exception {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
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
            utils.when(() -> Utils.getAsNanopub(VIEW_NP)).thenReturn(view);
            utils.when(() -> Utils.getAsNanopub(OPEN_DISPLAY_NP.replaceAll("^.*/", ""))).thenReturn(open);
            utils.when(() -> Utils.getAsNanopub(PLAIN_DISPLAY_NP.replaceAll("^.*/", ""))).thenReturn(plain);
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
     * The part page's own selection: for a definition by someone without a role, only the
     * display that opened itself to anyone renders; with no rank given, nothing is filtered.
     */
    @Test
    void thePartPageRendersOnlyTheDisplaysAcceptingTheDefinition() {
        ViewDisplay open = display("https://example.org/kind/open", true);
        ViewDisplay plain = display("https://example.org/kind/plain", false);
        Space space = mock(Space.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        doReturn(null).when(space).triggerDataUpdate();
        doReturn(space).when(space).getSpace();
        doReturn(TierRanks.EVERYONE).when(space).getPartDefinitionTierRank();
        doReturn(List.of(open, plain)).when(space).getViewDisplays();

        assertEquals(List.of(open), space.getPartLevelViewDisplays(PART, STATEMENT, TierRanks.ANYONE));
        assertEquals(List.of(open, plain), space.getPartLevelViewDisplays(PART, STATEMENT, TierRanks.EVERYONE));
        assertEquals(List.of(open, plain), space.getPartLevelViewDisplays(PART, STATEMENT, Integer.MAX_VALUE));
        assertTrue(space.appliesTo(PART, STATEMENT, TierRanks.ANYONE));
    }

    private static ViewDisplay display(String kind, boolean openToAnyone) {
        ViewDisplay display = mock(ViewDisplay.class);
        when(display.getViewKindIri()).thenReturn(Values.iri(kind));
        when(display.hasType(any())).thenReturn(false);
        when(display.appliesTo(anyString(), any())).thenReturn(true);
        when(display.acceptsPartDefinition(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> {
                    int signerRank = inv.getArgument(0);
                    int baseRank = inv.getArgument(1);
                    return signerRank >= (openToAnyone ? TierRanks.ANYONE : baseRank);
                });
        return display;
    }

}
