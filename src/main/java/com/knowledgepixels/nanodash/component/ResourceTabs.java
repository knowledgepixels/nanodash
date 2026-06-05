package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.MaintainedResourcePage;
import com.knowledgepixels.nanodash.page.ResourcePartPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Tab strip shown at the top of a resource's page, switching between the
 * <b>Content</b> tab (the rendered view displays), the <b>About</b> tab (the
 * listing of roles/presets/view displays), the <b>Explore</b> tab (the generic
 * exploration panels and references), and the <b>Raw</b> tab (the downloadable
 * RDF of all nanopubs on the page).
 *
 * <p>All tabs are the same page, selected via the {@code tab} query parameter
 * ({@code content} is the default and carries no parameter). Parts
 * ({@code type == "part"}) have no About tab.</p>
 */
public class ResourceTabs extends Panel {

    /**
     * Which tab is currently shown (rendered as the selected tab).
     */
    public enum Tab {CONTENT, ABOUT, EXPLORE, RAW}

    /**
     * Maps the {@code tab} query parameter to a {@link Tab} (defaulting to
     * {@link Tab#CONTENT}). Used by the resource pages to pick which tab body to
     * render and which tab to mark active.
     *
     * @param parameters the page parameters
     * @return the selected tab
     */
    public static Tab activeFromParam(PageParameters parameters) {
        switch (parameters.get("tab").toString("content")) {
            case "about":
                return Tab.ABOUT;
            case "explore":
                return Tab.EXPLORE;
            case "raw":
                return Tab.RAW;
            default:
                return Tab.CONTENT;
        }
    }

    /**
     * Constructs the tab strip for a top-level resource (space, user, resource).
     *
     * @param id         the Wicket markup id
     * @param type       the resource kind: {@code "space"}, {@code "user"}, or {@code "resource"}
     * @param resourceId the resource IRI
     * @param active     the tab to mark as selected
     */
    public ResourceTabs(String id, String type, String resourceId, Tab active) {
        this(id, type, resourceId, null, active);
    }

    /**
     * Constructs the tab strip, optionally for a part (which carries a context).
     *
     * @param id         the Wicket markup id
     * @param type       the resource kind: {@code "space"}, {@code "user"}, {@code "resource"}, or {@code "part"}
     * @param resourceId the resource (or part) IRI
     * @param contextId  the context resource IRI (for parts), or {@code null}
     * @param active     the tab to mark as selected
     */
    public ResourceTabs(String id, String type, String resourceId, String contextId, Tab active) {
        super(id);

        Class<? extends WebPage> pageClass;
        boolean hasAbout;
        switch (type) {
            case "space":
                pageClass = SpacePage.class;
                hasAbout = true;
                break;
            case "user":
                pageClass = UserPage.class;
                hasAbout = true;
                break;
            case "resource":
                pageClass = MaintainedResourcePage.class;
                hasAbout = true;
                break;
            case "part":
                pageClass = ResourcePartPage.class;
                hasAbout = false;
                break;
            default:
                throw new IllegalArgumentException("Unknown resource type: " + type);
        }

        add(tabLink("content-tab", pageClass, params(resourceId, contextId, null), active == Tab.CONTENT));
        if (hasAbout) {
            add(tabLink("about-tab", pageClass, params(resourceId, contextId, "about"), active == Tab.ABOUT));
        } else {
            add(new WebMarkupContainer("about-tab").setVisible(false));
        }
        add(tabLink("explore-tab", pageClass, params(resourceId, contextId, "explore"), active == Tab.EXPLORE));
        add(tabLink("raw-tab", pageClass, params(resourceId, contextId, "raw"), active == Tab.RAW));
    }

    /**
     * The gray-italic title suffix shown after the resource name on non-content
     * tabs (e.g. " – About"); empty for the content tab.
     *
     * @param tab the active tab
     * @return the suffix string (possibly empty)
     */
    public static String titleSuffix(Tab tab) {
        switch (tab) {
            case ABOUT:
                return " – About";
            case EXPLORE:
                return " – Explore";
            case RAW:
                return " – Raw";
            default:
                return "";
        }
    }

    private PageParameters params(String resourceId, String contextId, String tab) {
        PageParameters p = new PageParameters().set("id", resourceId);
        if (contextId != null) p.set("context", contextId);
        if (tab != null) p.set("tab", tab);
        return p;
    }

    private BookmarkablePageLink<Void> tabLink(String id, Class<? extends WebPage> pageClass, PageParameters params, boolean selected) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, pageClass, params);
        if (selected) {
            link.add(new AttributeAppender("class", " selected"));
        }
        return link;
    }

}
