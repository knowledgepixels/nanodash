package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
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
        // Centered title-bar message: the post-publish confirmation and/or the
        // always-on "you haven't published an introduction yet" warning.
        add(new JustPublishedMessagePanel("justPublishedMessage", page.getPageParameters()));

        createNavLink("users", UserListPage.class);
        createNavLink("connectors", SpaceListPage.class);
        createNavLink("publish", PublishPage.class).setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createNavLink("query", QueryListPage.class);

        breadcrumbPath = new WebMarkupContainer("breadcrumbpath");
        WebMarkupContainer breadcrumbLinks = new WebMarkupContainer("breadcrumblinks");
        List<CrumbPart> crumbParts = buildCrumbParts(pathRefs);
        if (!crumbParts.isEmpty()) {
            CrumbPart first = crumbParts.get(0);
            breadcrumbLinks.add(first.ref().createComponent("firstpathelement", first.label()));
            // Getting serialization exception if not using 'new ArrayList<...>(...)' here:
            List<CrumbPart> moreParts = new ArrayList<CrumbPart>(crumbParts.subList(1, crumbParts.size()));
            breadcrumbLinks.add(new DataView<CrumbPart>("morepathelements", new ListDataProvider<CrumbPart>(moreParts)) {

                @Override
                protected void populateItem(Item<CrumbPart> item) {
                    CrumbPart part = item.getModelObject();
                    item.add(part.ref().createComponent("furtherpathelement", part.label()));
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
     * One breadcrumb segment: the {@link NanodashPageRef} it links to and the
     * (possibly split) label text to show for it.
     */
    public record CrumbPart(NanodashPageRef ref, String label) implements Serializable {
    }

    /**
     * Flattens a breadcrumb path into the segments to render. Three transforms are
     * applied to each ref's label:
     * <ul>
     *   <li>For each non-root crumb, the part it shares with its parent label
     *       is stripped (e.g. parent "Knowledge Pixels", child "Knowledge
     *       Pixels Incubator" renders as "Incubator"). The shared part is the
     *       longest common character prefix, but only stripped when it covers
     *       (almost) all of the parent and ends on a non-letter/digit boundary
     *       in the child — this also catches singular/plural variations like
     *       parent "Nano Sessions", child "Nano Session #30" → "#30".</li>
     *   <li>For each non-root crumb, a word-level overlap where the child's
     *       leading word(s) restate the parent's trailing word(s) is stripped
     *       (e.g. parent "Abc Def Ghi", child "Ghi Jkl" renders as "Jkl"). The
     *       longest such parent-suffix/child-prefix overlap wins, matched on
     *       whole words case-insensitively, and never strips the whole child
     *       away.</li>
     *   <li>Only the leading segment of the remaining label is kept — the part
     *       before the first list/title separator ({@code ,}, {@code :},
     *       {@code ;}, {@code |}, or a spaced {@code -}) — so each path level
     *       renders as one concise crumb. E.g. "FIP.38.T.8 | FAIR Implementation
     *       Profile Training Session 8" renders as "FIP.38.T.8" and "3PFF: the
     *       Three Point FAIRification Framework" as "3PFF".</li>
     * </ul>
     * The parent-prefix comparison uses the original (un-simplified) labels.
     */
    static List<CrumbPart> buildCrumbParts(NanodashPageRef[] pathRefs) {
        List<CrumbPart> parts = new ArrayList<>();
        for (int i = 0; i < pathRefs.length; i++) {
            String label = pathRefs[i].getLabel();
            if (label == null) {
                parts.add(new CrumbPart(pathRefs[i], null));
                continue;
            }
            if (i > 0) {
                String parentLabel = pathRefs[i - 1].getLabel();
                label = stripParentPrefix(parentLabel, label);
                label = stripParentOverlap(parentLabel, label);
            }
            // Show only the leading segment (before the first list/title separator)
            // so each path level renders as one concise crumb — e.g.
            // "FIP.38.T.8 | FAIR Implementation Profile Training Session 8" → "FIP.38.T.8",
            // "3PFF: the Three Point FAIRification Framework" → "3PFF".
            List<String> segments = splitLabel(label);
            if (!segments.isEmpty()) {
                parts.add(new CrumbPart(pathRefs[i], segments.get(0)));
            }
        }
        return parts;
    }

    /**
     * Splits a label on list/title separators — comma, colon, semicolon, pipe
     * (with or without surrounding spaces), or a space-surrounded hyphen — into
     * trimmed, non-empty segments. Returns the trimmed whole label if there is
     * no separator (so the result always has at least one element).
     */
    static List<String> splitLabel(String label) {
        List<String> segments = new ArrayList<>();
        if (label == null) return segments;
        for (String s : label.split("\\s*[,;:]\\s+|\\s*\\|\\s*|\\s+-\\s+")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) segments.add(trimmed);
        }
        if (segments.isEmpty()) {
            String trimmed = label.trim();
            if (!trimmed.isEmpty()) segments.add(trimmed);
        }
        return segments;
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
     * If the child label's leading word(s) restate the parent label's trailing
     * word(s), strips that overlap and returns the remainder. E.g. parent
     * "Abc Def Ghi", child "Ghi Jkl" → "Jkl", or parent "Knowledge Pixels
     * Incubator Office Hours", child "Office Hour 24 June 2026" → "24 June 2026".
     * The overlap is matched on whole words, case-insensitively and tolerating
     * singular/plural variation ("Hours" ≈ "Hour"); the longest parent-suffix that
     * matches a child-prefix wins. Returns the child unchanged when there is no
     * overlap, or when the overlap would consume the whole child label (at least
     * one word is always kept).
     */
    static String stripParentOverlap(String parent, String child) {
        if (parent == null || parent.isEmpty() || child == null || child.isEmpty()) return child;
        String[] parentWords = parent.trim().split("\\s+");
        String[] childWords = child.trim().split("\\s+");
        // Keep at least one child word, so the overlap can cover at most all but
        // the last child word.
        int maxK = Math.min(parentWords.length, childWords.length - 1);
        for (int k = maxK; k >= 1; k--) {
            boolean match = true;
            for (int j = 0; j < k; j++) {
                if (!wordsEquivalent(parentWords[parentWords.length - k + j], childWords[j])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return String.join(" ", java.util.Arrays.asList(childWords).subList(k, childWords.length));
            }
        }
        return child;
    }

    /**
     * Whether two breadcrumb words count as the same topic word: equal ignoring
     * case, or equal once a single trailing plural "s" is dropped from each (so
     * "Hours" matches "Hour", "Sessions" matches "Session").
     */
    private static boolean wordsEquivalent(String a, String b) {
        if (a.equalsIgnoreCase(b)) return true;
        return depluralize(a).equalsIgnoreCase(depluralize(b));
    }

    private static String depluralize(String word) {
        return word.length() > 1 && (word.endsWith("s") || word.endsWith("S"))
                ? word.substring(0, word.length() - 1)
                : word;
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
