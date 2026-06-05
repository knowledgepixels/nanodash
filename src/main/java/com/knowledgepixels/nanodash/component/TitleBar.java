package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryListPage;
import com.knowledgepixels.nanodash.page.SpaceListPage;
import com.knowledgepixels.nanodash.page.UserListPage;

/**
 * TitleBar is the top bar of the Nanodash application, which contains
 * navigation elements such as profile, my channel, users, connectors,
 * publish, query, and breadcrumb navigation.
 */
public class TitleBar extends Panel {

    private String highlight;

    /**
     * The breadcrumb/tab strip row (the grey band just below the nav bar). Holds
     * the breadcrumb links on the left and, optionally, the resource tab strip on
     * the right (see {@link #setTabs(ResourceTabs)}).
     */
    private WebMarkupContainer breadcrumbPath;

    /**
     * Constructs a TitleBar with the specified id, page, highlight element,
     * and an array of path references for breadcrumb navigation.
     *
     * @param id        the component id
     * @param page      the current Nanodash page
     * @param highlight the id of the element to highlight
     * @param pathRefs  an array of NanodashPageRef for breadcrumb navigation
     */
    public TitleBar(String id, NanodashPage page, String highlight, NanodashPageRef... pathRefs) {
        super(id);
        this.highlight = highlight;
        add(new ProfileItem("profile", page));

        createNavLink("users", UserListPage.class);
        createNavLink("connectors", SpaceListPage.class);
        createNavLink("publish", PublishPage.class).setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createNavLink("query", QueryListPage.class);

        breadcrumbPath = new WebMarkupContainer("breadcrumbpath");
        WebMarkupContainer breadcrumbLinks = new WebMarkupContainer("breadcrumblinks");
        if (pathRefs.length > 0) {
            final String[] displayLabels = simplifyBreadcrumbLabels(pathRefs);
            breadcrumbLinks.add(pathRefs[0].createComponent("firstpathelement", displayLabels[0]));
            // Getting serialization exception if not using 'new ArrayList<...>(...)' here:
            List<NanodashPageRef> morePathElements = new ArrayList<NanodashPageRef>(Utils.subList(pathRefs, 1, pathRefs.length));
            breadcrumbLinks.add(new DataView<NanodashPageRef>("morepathelements", new ListDataProvider<NanodashPageRef>(morePathElements)) {

                @Override
                protected void populateItem(Item<NanodashPageRef> item) {
                    int index = (int) item.getIndex() + 1;
                    item.add(item.getModelObject().createComponent("furtherpathelement", displayLabels[index]));
                }

            });
        } else {
            breadcrumbLinks.setVisible(false);
        }
        breadcrumbPath.add(breadcrumbLinks);
        // Tab strip placeholder (right side of the strip); replaced via setTabs().
        breadcrumbPath.add(new EmptyPanel("tabs").setVisible(false));
        // The strip shows when there is a breadcrumb to display; setTabs() also
        // forces it visible so a tab strip shows even without a breadcrumb.
        breadcrumbPath.setVisible(pathRefs.length > 0);
        add(breadcrumbPath);
    }

    /**
     * Computes shortened display labels for a breadcrumb path.
     *
     * Two simplifications are applied:
     * <ul>
     *   <li>For each non-root crumb, the part it shares with its parent label
     *       is stripped (e.g. parent "Knowledge Pixels", child "Knowledge
     *       Pixels Incubator" renders as "Incubator"). The shared part is the
     *       longest common character prefix, but only stripped when it covers
     *       (almost) all of the parent and ends on a non-letter/digit boundary
     *       in the child — this also catches singular/plural variations like
     *       parent "Nano Sessions", child "Nano Session #30" → "#30".</li>
     *   <li>Any ": " in a label and everything after it is removed (e.g.
     *       "Incubator 1: Some title" becomes "Incubator 1"). Applied to all
     *       crumbs, including the first.</li>
     * </ul>
     *
     * The parent-prefix comparison uses the original (un-simplified) labels,
     * so that simplifications on the parent don't interfere with the child.
     */
    static String[] simplifyBreadcrumbLabels(NanodashPageRef[] pathRefs) {
        String[] displayLabels = new String[pathRefs.length];
        for (int i = 0; i < pathRefs.length; i++) {
            String label = pathRefs[i].getLabel();
            if (label != null) {
                if (i > 0) {
                    label = stripParentPrefix(pathRefs[i - 1].getLabel(), label);
                }
                int colonIdx = label.indexOf(": ");
                if (colonIdx > 0) {
                    label = label.substring(0, colonIdx);
                }
            }
            displayLabels[i] = label;
        }
        return displayLabels;
    }

    /**
     * If the child label appears to restate the parent's "topic" at the start,
     * strips that shared prefix and returns the remainder. Otherwise returns
     * the child label unchanged.
     */
    private static String stripParentPrefix(String parent, String child) {
        if (parent == null || parent.isEmpty() || child == null) return child;
        int lcp = 0;
        int max = Math.min(parent.length(), child.length());
        while (lcp < max && parent.charAt(lcp) == child.charAt(lcp)) lcp++;
        // Require a substantial match: at least 3 chars, and within 2 chars of
        // the full parent length (so things like "Sessions" vs "Session" still
        // match, but "Nanopublication Sessions" vs "Nano Session #30" doesn't).
        if (lcp < 3 || lcp < parent.length() - 2) return child;
        // The boundary in the child must not fall mid-word, otherwise we'd be
        // chopping off a partial word like "Foo Bar" -> "Foo Baz Quux" -> "z Quux".
        if (lcp >= child.length() || Character.isLetterOrDigit(child.charAt(lcp))) return child;
        String remainder = child.substring(lcp).replaceAll("^\\s+", "");
        if (remainder.isEmpty()) return child;
        return remainder;
    }

    /**
     * Places a resource tab strip (Content | About | Raw) on the right side of
     * the breadcrumb strip, and forces the strip visible so the tabs show even on
     * pages without a breadcrumb (e.g. user pages). Returns {@code this} for
     * fluent use at the {@code add(...)} call site.
     *
     * @param tabs the tab strip (its markup id must be {@code "tabs"})
     * @return this TitleBar
     */
    public TitleBar setTabs(ResourceTabs tabs) {
        breadcrumbPath.addOrReplace(tabs);
        breadcrumbPath.setVisible(true);
        return this;
    }

    private BookmarkablePageLink<Void> createNavLink(String id, Class<? extends WebPage> pageClass) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, pageClass);
        if (id.equals(highlight)) {
            link.add(new AttributeAppender("class", "selected"));
        }
        add(link);
        return link;
    }

}
