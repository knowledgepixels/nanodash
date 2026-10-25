package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * What a part's name says about where it is being shown. A person's name reads the same
 * wherever it is met, so the part page's heading and the back-crumb pointing at it both
 * carry the context: " at ISWC 2026" (issue #822). One is <em>at</em> an event and
 * <em>in</em> anything else, and the label is shortened the way a crumb is. Anything else
 * a context has parts for is named by itself alone.
 */
class TitleBarContextSuffixTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/iswc/2026";
    private static final String PERSON = "https://orcid.org/0000-0002-1825-0097";
    private static final String PAPER = SPACE_ID + "/in-depth-analysis-of-transformer-architectures";

    private MockedStatic<IndividualAgent> agentMock;

    @BeforeEach
    void setUp() {
        // Asking whether an id is a known user goes over the network; the ORCID check
        // next to it, which is what decides here, stays real.
        agentMock = mockStatic(IndividualAgent.class, CALLS_REAL_METHODS);
        agentMock.when(() -> IndividualAgent.isUser(anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        agentMock.close();
    }

    private static Space space(String label, boolean event) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(SPACE_ID);
        when(space.getLabel()).thenReturn(label);
        when(space.isOfType(any())).thenReturn(false);
        when(space.isOfType(KPXL_TERMS.EVENT)).thenReturn(event);
        return space;
    }

    @Test
    void oneIsAtAnEvent() {
        assertEquals(" at ISWC 2026", TitleBar.contextSuffix(PERSON, space("ISWC 2026", true)));
    }

    @Test
    void andInAnythingElse() {
        assertEquals(" in Nanodash", TitleBar.contextSuffix(PERSON, space("Nanodash", false)));
    }

    @Test
    void aMaintainedResourceIsSomethingElse() {
        MaintainedResource resource = mock(MaintainedResource.class);
        when(resource.getId()).thenReturn(SPACE_ID + "/r/kgxl-query");
        when(resource.getLabel()).thenReturn("KGXL-Query");

        assertEquals(" in KGXL-Query", TitleBar.contextSuffix(PERSON, resource));
    }

    /**
     * A conference's full name runs to a subtitle, which would take over the heading. The
     * breadcrumb already shortens it to the part before the colon, and this follows suit so
     * the two name the same space the same way.
     */
    @Test
    void aLongNameIsShortenedTheWayABreadcrumbShortensIt() {
        Space space = space("ISWC 2026: 25th International Semantic Web Conference", true);

        assertEquals(" at ISWC 2026", TitleBar.contextSuffix(PERSON, space));
    }

    @Test
    void saysNothingWithoutAContext() {
        assertEquals("", TitleBar.contextSuffix(PERSON, null));
    }

    @Test
    void saysNothingForAContextWithNoLabel() {
        assertEquals("", TitleBar.contextSuffix(PERSON, space("   ", true)));
    }

    /**
     * A paper, a training, a paragraph: it belongs to the context that defines it, so
     * naming that context again says nothing and reads as part of the title.
     */
    @Test
    void saysNothingForAPartThatIsNotAPerson() {
        assertEquals("", TitleBar.contextSuffix(PAPER, space("ISWC 2026", true)));
    }

    @Test
    void saysNothingWithoutAPart() {
        assertEquals("", TitleBar.contextSuffix("", space("ISWC 2026", true)));
    }

}
