package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What a part's name says about where it is being shown. The name alone reads the same
 * wherever it is met, so the part page's heading and the back-crumb pointing at it both
 * carry the context: " at ISWC 2026" (issue #822). One is <em>at</em> an event and
 * <em>in</em> anything else, and the label is shortened the way a crumb is.
 */
class TitleBarContextSuffixTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/iswc/2026";

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
        assertEquals(" at ISWC 2026", TitleBar.contextSuffix(space("ISWC 2026", true)));
    }

    @Test
    void andInAnythingElse() {
        assertEquals(" in Nanodash", TitleBar.contextSuffix(space("Nanodash", false)));
    }

    @Test
    void aMaintainedResourceIsSomethingElse() {
        MaintainedResource resource = mock(MaintainedResource.class);
        when(resource.getId()).thenReturn(SPACE_ID + "/r/kgxl-query");
        when(resource.getLabel()).thenReturn("KGXL-Query");

        assertEquals(" in KGXL-Query", TitleBar.contextSuffix(resource));
    }

    /**
     * A conference's full name runs to a subtitle, which would take over the heading. The
     * breadcrumb already shortens it to the part before the colon, and this follows suit so
     * the two name the same space the same way.
     */
    @Test
    void aLongNameIsShortenedTheWayABreadcrumbShortensIt() {
        Space space = space("ISWC 2026: 25th International Semantic Web Conference", true);

        assertEquals(" at ISWC 2026", TitleBar.contextSuffix(space));
    }

    @Test
    void saysNothingWithoutAContext() {
        assertEquals("", TitleBar.contextSuffix(null));
    }

    @Test
    void saysNothingForAContextWithNoLabel() {
        assertEquals("", TitleBar.contextSuffix(space("   ", true)));
    }

}
