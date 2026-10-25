package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * What the back-crumb says when it points at a part page. "&lt; Tobias Kuhn" on Tobias
 * Kuhn's own page reads as a link back to where the reader already is; it is really the
 * page about him inside a space, so it says which space (issue #822). A part that is not
 * a person is named by itself alone, as its own page's heading names it.
 */
class BackCrumbContextTest {

    private static final String SPACE = "https://w3id.org/spaces/iswc/2026";
    private static final String PART_LABEL = "Josiah Carberry";
    private static final String PAPER = SPACE + "/in-depth-analysis-of-transformer-architectures";
    private static final String PAPER_LABEL = "In-depth Analysis of Transformer Architectures";

    private WicketTester tester;
    private MockedStatic<NavigationContext> navigationContextMock;
    private MockedStatic<IndividualAgent> agentMock;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        // Resolving the context goes over the network; what the crumb does with the
        // resolved space is the point. Everything else in NavigationContext stays real.
        navigationContextMock = mockStatic(NavigationContext.class, CALLS_REAL_METHODS);
        // Asking whether an id is a known user goes over the network too; the ORCID check
        // next to it, which is what tells a person from a paper here, stays real.
        agentMock = mockStatic(IndividualAgent.class, CALLS_REAL_METHODS);
        agentMock.when(() -> IndividualAgent.isUser(anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        agentMock.close();
        navigationContextMock.close();
        tester.destroy();
    }

    private void contextIs(String label, boolean event) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(SPACE);
        when(space.getLabel()).thenReturn(label);
        when(space.isOfType(any())).thenReturn(false);
        when(space.isOfType(KPXL_TERMS.EVENT)).thenReturn(event);
        navigationContextMock.when(() -> NavigationContext.resolve(SPACE)).thenReturn(space);
    }

    private String backCrumb() {
        return backCrumb(ResourceTabsNavTestPage.USER_IRI, PART_LABEL);
    }

    private String backCrumb(String partId, String partLabel) {
        tester.startPage(ResourceTabsNavTestPage.class, new PageParameters()
                .set("id", ResourceTabsNavTestPage.USER_IRI)
                .set(NavigationContext.CONTEXT_PARAM, SPACE)
                .set(NavigationContext.PART_PARAM, partId)
                .set(NavigationContext.PART_LABEL_PARAM, partLabel));
        String html = tester.getLastResponseAsString();
        Matcher matcher = Pattern.compile(
                "backcontext-container[\\s\\S]{0,400}?<a href=\"([^\"]*part\\?[^\"]*)\"[^>]*><span[^>]*>([^<]*)</span>")
                .matcher(html);
        if (!matcher.find()) {
            // The container is rendered without its wicket:id; fall back to the one
            // anchor on the page that leads to a part page.
            matcher = Pattern.compile("<a href=\"([^\"]*part\\?[^\"]*)\"[^>]*><span[^>]*>([^<]*)</span>")
                    .matcher(html);
            assertTrue(matcher.find(), "no back-crumb to a part page rendered: " + html);
        }
        return matcher.group(2);
    }

    @Test
    void namesTheEventThePartIsShownAt() {
        contextIs("ISWC 2026: 25th International Semantic Web Conference", true);

        assertEquals(PART_LABEL + " at ISWC 2026", backCrumb());
    }

    @Test
    void andTheSpaceAPartIsShownIn() {
        contextIs("Nanodash", false);

        assertEquals(PART_LABEL + " in Nanodash", backCrumb());
    }

    /**
     * A context that cannot (yet) be resolved leaves the crumb as it always was, rather
     * than blocking the render on a lookup.
     */
    @Test
    void saysJustTheNameWhileTheContextIsUnknown() {
        navigationContextMock.when(() -> NavigationContext.resolve(SPACE)).thenReturn(null);

        assertEquals(PART_LABEL, backCrumb());
    }

    /**
     * A paper belongs to the space that defines it, which its part page's heading
     * therefore does not repeat; the crumb leading back there says the same.
     */
    @Test
    void andJustTheNameForAPartThatIsNotAPerson() {
        contextIs("ISWC 2026", true);

        assertEquals(PAPER_LABEL, backCrumb(PAPER, PAPER_LABEL));
    }

}
