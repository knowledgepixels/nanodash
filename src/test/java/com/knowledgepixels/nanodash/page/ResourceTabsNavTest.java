package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a tab switch carries along. A person's page reached from their part page in a space
 * shows a back-link to that part rather than ending the trail (issue #697), and it may not
 * lose it the moment the reader opens another of its tabs: the tab strip builds its links
 * from scratch, so the context and the part have to be put back on them.
 */
class ResourceTabsNavTest {

    private static final String SPACE = "https://w3id.org/spaces/iswc-2026";
    private static final String PART_LABEL = "Josiah Carberry";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private String render(PageParameters parameters) {
        tester.startPage(ResourceTabsNavTestPage.class, parameters);
        return tester.getLastResponseAsString();
    }

    @Test
    void aTabSwitchKeepsTheContextAndThePartTheReaderCameUnder() {
        String href = aboutTabHref(render(new PageParameters()
                .set("id", ResourceTabsNavTestPage.USER_IRI)
                .set(NavigationContext.CONTEXT_PARAM, SPACE)
                .set(NavigationContext.PART_PARAM, ResourceTabsNavTestPage.USER_IRI)
                .set(NavigationContext.PART_LABEL_PARAM, PART_LABEL)));

        assertAll(
                () -> assertTrue(href.contains(NavigationContext.CONTEXT_PARAM + "=" + SPACE), href),
                () -> assertTrue(href.contains(NavigationContext.PART_PARAM + "=" + ResourceTabsNavTestPage.USER_IRI), href),
                () -> assertTrue(href.contains(NavigationContext.PART_LABEL_PARAM + "="), href),
                () -> assertTrue(href.contains("tab=about"), href));
    }

    /**
     * A page that is its own context, reached under no other one, has nothing incoming to
     * carry: its tabs stay the plain ones.
     */
    @Test
    void aPageReachedDirectlyKeepsItsTabsClean() {
        String href = aboutTabHref(render(
                new PageParameters().set("id", ResourceTabsNavTestPage.USER_IRI)));

        assertFalse(href.contains(NavigationContext.CONTEXT_PARAM + "="), href);
        assertFalse(href.contains(NavigationContext.PART_PARAM + "="), href);
    }

    /** The href of the tab strip's About tab, whose anchor text is exactly "About". */
    private static String aboutTabHref(String html) {
        Matcher matcher = Pattern.compile("<a href=\"([^\"]*)\"[^>]*>About</a>").matcher(html);
        assertTrue(matcher.find(), "no About tab rendered");
        String href = matcher.group(1);
        assertNotNull(href);
        return href;
    }

}
