package com.knowledgepixels.nanodash;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Derives the HTML {@code id} attributes (fragment identifiers) of the view-display
 * sections of a page, so a single section can be linked to directly, e.g.
 * {@code https://w3id.org/spaces/nanosuggestions#messages}.
 *
 * <p>The anchor of a section is a slug of its <em>title</em> — what the reader sees as
 * the section heading — because that is what makes a shared link self-explanatory. That
 * heading is the view display's own title when it has one, and the label of its query
 * otherwise, exactly as the view panels render it. When neither yields anything, the
 * label part of the {@link ViewDisplay#getStructuralPosition() structural position}
 * (e.g. {@code papers} of {@code "4.4.papers"}) is used instead, and failing that the
 * generic {@code "view"}. Anchors are made unique within the page by appending
 * {@code -2}, {@code -3}, … to later duplicates.</p>
 *
 * <p>Anchors are therefore <em>stable as long as the titles are</em>: renaming a view
 * breaks links to it, the same way it does on any documentation site. Positions within
 * the page do not matter — moving a section around keeps its anchor.</p>
 *
 * @see com.knowledgepixels.nanodash.component.ViewList
 */
public class ViewAnchors {

    private ViewAnchors() {
    }  // no instances

    /**
     * Ids used by the page chrome itself. A section slug that collides with one of these
     * is suffixed like any other duplicate, so the section never shadows them.
     */
    private static final Set<String> RESERVED_IDS = Set.of("content-pane", "titlebar", "nanodash-toast", "mode-toggle");

    /**
     * Anchor used when neither the title nor the structural position yields anything
     * usable (e.g. an emoji-only title).
     */
    private static final String FALLBACK_ANCHOR = "view";

    /**
     * Structural-position label that carries no meaning of its own (the default position
     * {@code "5.5.default"}), so it is not used as an anchor.
     */
    private static final String DEFAULT_POSITION_LABEL = "default";

    /**
     * Upper bound on the length of a generated anchor. Long titles still give readable,
     * copy-pasteable links; the uniqueness suffix is added on top of this.
     */
    private static final int MAX_LENGTH = 60;

    /**
     * Computes the anchor of each view display of a page, in the order the displays are
     * rendered in.
     *
     * @param viewDisplays the view displays of the page, in render order
     * @return one anchor per view display, in the same order; all distinct
     */
    public static List<String> forViewDisplays(List<ViewDisplay> viewDisplays) {
        Allocator allocator = new Allocator();
        List<String> anchors = new ArrayList<>(viewDisplays.size());
        for (ViewDisplay viewDisplay : viewDisplays) {
            anchors.add(allocator.next(viewDisplay));
        }
        return anchors;
    }

    /**
     * Class marking an element as a linkable view section for the client side
     * (see {@code nanodash.js}), used where the section is a view panel rendered directly
     * by a page rather than a {@code ViewList} item.
     */
    public static final String SECTION_CLASS = "view-section";

    /**
     * Hands out the anchors of one page, keeping them distinct from each other and from
     * the page chrome's own ids.
     *
     * <p>Pages that render their view panels themselves instead of through
     * {@link com.knowledgepixels.nanodash.component.ViewList} — the list pages, the
     * explore page, … — have no list item to carry the anchor, so {@link #anchor} puts it
     * on the panel itself.</p>
     */
    public static class Allocator implements Serializable {

        private final Set<String> used = new HashSet<>(RESERVED_IDS);

        /**
         * The anchor of the next section of the page.
         *
         * @param viewDisplay the view display rendered as that section
         * @return an anchor distinct from every one handed out before it
         */
        public String next(ViewDisplay viewDisplay) {
            String base = baseAnchor(viewDisplay);
            String candidate = base;
            int n = 2;
            while (!used.add(candidate)) {
                candidate = base + "-" + n++;
            }
            return candidate;
        }

        /**
         * Makes a view panel a linkable section of the page. The anchor becomes the
         * panel's <em>markup id</em> rather than a plain {@code id} attribute, so Wicket
         * addresses the panel by it for its own Ajax updates too and the anchor survives
         * the lazy load of the query results.
         *
         * @param panel       the view panel, as returned by one of the query-result builders
         * @param viewDisplay the view display it renders
         * @param <C>         the panel's type
         * @return the panel, for chaining into {@code add(…)}
         */
        public <C extends Component> C anchor(C panel, ViewDisplay viewDisplay) {
            panel.setMarkupId(next(viewDisplay));
            panel.add(AttributeAppender.append("class", SECTION_CLASS));
            return panel;
        }

    }

    /**
     * The anchor a view display would get on its own, before de-duplication.
     *
     * @param viewDisplay the view display
     * @return a non-empty slug
     */
    private static String baseAnchor(ViewDisplay viewDisplay) {
        String fromTitle = slugify(displayedTitle(viewDisplay));
        if (!fromTitle.isEmpty()) return fromTitle;
        String fromPosition = slugify(positionLabel(viewDisplay.getStructuralPosition()));
        if (!fromPosition.isEmpty() && !fromPosition.equals(DEFAULT_POSITION_LABEL)) return fromPosition;
        return FALLBACK_ANCHOR;
    }

    /**
     * The heading a view display actually renders. A view display without a title of its
     * own is not an untitled section: every query-result panel then shows the label of
     * the view's query as the heading (see e.g.
     * {@code QueryResultNanopubSet}), so the anchor has to follow the same fallback —
     * otherwise such a section ends up with the meaningless {@code "view"} anchor while
     * carrying a perfectly good heading.
     *
     * <p>An explicitly empty title ({@code ViewDisplay.withTitle("")}) means "hide the
     * heading" rather than "no title given", so it does not fall through to the query
     * label.</p>
     *
     * @param viewDisplay the view display
     * @return the heading text, or null if the section shows none
     */
    private static String displayedTitle(ViewDisplay viewDisplay) {
        if (viewDisplay.getTitle() != null) return viewDisplay.getTitle();
        View view = viewDisplay.getView();
        // Header views have no query; the panel then shows the title only, i.e. nothing.
        if (view == null || view.getQuery() == null) return null;
        return view.getQuery().getLabel();
    }

    /**
     * The free label part of a structural position, i.e. everything after the two leading
     * digit components: {@code "4.4.papers"} → {@code "papers"}. See
     * {@code docs/structural-position.md}.
     *
     * @param position the structural position, or null
     * @return the label part, or an empty string if there is none
     */
    private static String positionLabel(String position) {
        if (position == null) return "";
        int firstDot = position.indexOf('.');
        if (firstDot < 0) return "";
        int secondDot = position.indexOf('.', firstDot + 1);
        if (secondDot < 0) return "";
        return position.substring(secondDot + 1);
    }

    /**
     * Turns a human-readable string into a fragment-identifier slug: lower-case ASCII
     * letters and digits, separated by single hyphens. Accents are folded onto their base
     * letter ("Übersicht" → "ubersicht"); everything else that is not a letter or digit
     * (spaces, punctuation, emoji — titles here often lead with one) becomes a separator.
     * A slug starting with a digit is prefixed, so it stays usable as a CSS id selector.
     *
     * @param text the text to slugify, or null
     * @return the slug, possibly empty if the text has no usable characters
     */
    public static String slugify(String text) {
        if (text == null) return "";
        String slug = Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_LENGTH) {
            // Cut back to the last word boundary; hyphen runs are already collapsed and
            // the slug has no leading hyphen, so this can never empty it out.
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        if (!slug.isEmpty() && Character.isDigit(slug.charAt(0))) slug = FALLBACK_ANCHOR + "-" + slug;
        return slug;
    }

}
