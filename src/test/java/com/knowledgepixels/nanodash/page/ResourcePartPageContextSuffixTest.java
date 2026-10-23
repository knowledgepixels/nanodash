package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What the part page's heading says about where the part is being shown. The name alone
 * reads the same wherever it is met, so it carries the context: " at ISWC 2026" (issue
 * #822). One is <em>at</em> an event and <em>in</em> anything else, and the label is
 * shortened the way a breadcrumb shortens it.
 */
class ResourcePartPageContextSuffixTest {

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
        assertEquals(" at ISWC 2026", ResourcePartPage.contextSuffix(space("ISWC 2026", true)));
    }

    @Test
    void andInAnythingElse() {
        assertEquals(" in Nanodash", ResourcePartPage.contextSuffix(space("Nanodash", false)));
    }

    @Test
    void aMaintainedResourceIsSomethingElse() {
        MaintainedResource resource = mock(MaintainedResource.class);
        when(resource.getId()).thenReturn(SPACE_ID + "/r/kgxl-query");
        when(resource.getLabel()).thenReturn("KGXL-Query");

        assertEquals(" in KGXL-Query", ResourcePartPage.contextSuffix(resource));
    }

    /**
     * A conference's full name runs to a subtitle, which would take over the heading. The
     * breadcrumb already shortens it to the part before the colon, and this follows suit so
     * the two name the same space the same way.
     */
    @Test
    void aLongNameIsShortenedTheWayABreadcrumbShortensIt() {
        Space space = space("ISWC 2026: 25th International Semantic Web Conference", true);

        assertEquals(" at ISWC 2026", ResourcePartPage.contextSuffix(space));
    }

    @Test
    void saysNothingWithoutAContext() {
        assertEquals("", ResourcePartPage.contextSuffix(null));
    }

    @Test
    void saysNothingForAContextWithNoLabel() {
        assertEquals("", ResourcePartPage.contextSuffix(space("   ", true)));
    }

}
