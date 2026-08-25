package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.calendar.CalendarEvent;
import com.knowledgepixels.nanodash.calendar.CalendarUrls;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.CalendarFeedPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import org.apache.wicket.util.visit.IVisitor;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The dropdown next to a page title: a regular chevron menu (like the other dropdowns)
 * offering whatever the page as a whole can be asked to do. Every logged-in viewer gets
 * "refresh now", the page-level counterpart of a view display's own refresh entry; a space
 * additionally gets its calendar actions and, for maintainer-tier members and above, the
 * space-configuration shortcuts. Each calendar entry names a group of actions ("add to
 * calendar", "add events to calendar") and opens its entries in a submenu flyout on
 * hover; the configuration shortcuts are plain entries below them.
 *
 * <p>Just right of the chevron sits the {@link StructureRefreshIndicator}: the spinner
 * saying the page structure is being recalculated (issue #622).</p>
 *
 * <p>Each supplied link must use the markup id {@code "link"}; the links are rendered in
 * order as menu entries.</p>
 */
public class PageTitleMenu extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(PageTitleMenu.class);

    /** A submenu: its label in the top-level dropdown, and the entries of its flyout. */
    private record Group(String label, List<AbstractLink> entries) implements Serializable {
        private Group {
            // List.of() lists serialize via a proxy (java.util.CollSer) that Wicket's
            // page deserialization cannot resolve for back-references: the entries are
            // also reachable as components of the page tree, and restoring such a page
            // fails with a ClassCastException assigning the proxy to
            // ListDataProvider.list. A plain ArrayList serializes directly.
            entries = new ArrayList<>(entries);
        }
    }

    private PageTitleMenu(String id, List<Group> groups, List<AbstractLink> extraEntries, AbstractResourceWithProfile resource, boolean canRefresh) {
        super(id);
        add(new StructureRefreshIndicator("spinner", resource));
        add(new WebMarkupContainer("refreshSeparator")
                .setVisible(canRefresh && !(groups.isEmpty() && extraEntries.isEmpty())));
        add(refreshNowLink(resource).setVisible(canRefresh));
        add(new DataView<Group>("groups", new ListDataProvider<>(groups)) {
            @Override
            protected void populateItem(Item<Group> groupItem) {
                groupItem.add(new Label("sublabel", groupItem.getModelObject().label()));
                groupItem.add(new DataView<AbstractLink>("entries", new ListDataProvider<>(groupItem.getModelObject().entries())) {
                    @Override
                    protected void populateItem(Item<AbstractLink> item) {
                        item.add(item.getModelObject());
                    }
                });
            }
        });
        add(new WebMarkupContainer("separator").setVisible(!groups.isEmpty() && !extraEntries.isEmpty()));
        add(new DataView<AbstractLink>("extraEntries", new ListDataProvider<>(extraEntries)) {
            @Override
            protected void populateItem(Item<AbstractLink> item) {
                item.add(item.getModelObject());
            }
        });
    }

    /**
     * Brings the whole page up to date, in that order: first the resource's own structure —
     * which views it shows — and then the views that refreshed structure turns out to
     * contain. The two steps are deliberately sequential: refreshing the views that happen
     * to be on screen at the moment of the click would refresh the <em>old</em> list, so a
     * view display that has just been added would arrive with whatever the cache held for
     * it, and one that has just been removed would be re-queried for nothing. The request
     * is therefore left standing on the resource and honoured by the view list that is
     * built once the refreshed structure has landed (see
     * {@link AbstractResourceWithProfile#requestViewRefresh()} and
     * {@link RefreshingStructurePanel}).
     * <p>
     * Views that are not part of that structure-driven list — the About and Explore tabs
     * build their own — have no refreshed list to wait for and are marked right away.
     * <p>
     * The page-level counterpart of a view display's "refresh now", which does the same for
     * its one query.
     *
     * @param resource the page's resource, or null if the page has none
     * @return the menu entry
     */
    private static AbstractLink refreshNowLink(AbstractResourceWithProfile resource) {
        // Held by id: see StructureRefreshIndicator on why the singleton itself is not kept.
        final String resourceId = resource == null ? null : resource.getId();
        return new AjaxFallbackLink<Void>("refreshNow") {
            @Override
            public void onClick(Optional<AjaxRequestTarget> target) {
                AbstractResourceWithProfile r = resourceId == null ? null : AbstractResourceWithProfile.get(resourceId);
                // Only a page that shows a view list has views to refresh once the
                // refreshed structure lands. Asking for it on a page that has none (a
                // tab that builds its own views) would leave the request standing on the
                // process-wide resource for some later page to pick up.
                boolean hasViewList = Boolean.TRUE.equals(getPage().visitChildren(ViewList.class,
                        (IVisitor<ViewList, Boolean>) (list, visit) -> visit.stop(Boolean.TRUE)));
                if (r != null) {
                    r.forceRefresh(0);
                    if (hasViewList) r.requestViewRefresh();
                }
                getPage().visitChildren(QueryResult.class, (IVisitor<QueryResult, Void>) (view, visit) -> {
                    QueryRef queryRef = view.getQueryRef();
                    if (queryRef != null && view.findParent(ViewList.class) == null) {
                        ApiCache.clearCache(queryRef, 0);
                    }
                });
                setResponsePage(getPage().getClass(), getPage().getPageParameters());
            }
        };
    }

    /**
     * The title menu for a page showing a resource that is not a space: the refresh entry
     * and the structure spinner. Invisible for viewers who are not logged in, who have
     * nothing to ask of the page.
     *
     * @param id       the Wicket component id
     * @param resource the resource whose page this is
     * @return the menu, or an invisible {@link EmptyPanel}
     */
    public static Component forResource(String id, AbstractResourceWithProfile resource) {
        return build(id, new ArrayList<>(), new ArrayList<>(), resource);
    }

    private static Component build(String id, List<Group> groups, List<AbstractLink> extraEntries, AbstractResourceWithProfile resource) {
        boolean canRefresh = resource != null && NanodashSession.get().getUserIri() != null;
        if (groups.isEmpty() && extraEntries.isEmpty() && !canRefresh) {
            return new EmptyPanel(id).setVisible(false);
        }
        return new PageTitleMenu(id, groups, extraEntries, resource, canRefresh);
    }

    /**
     * The title menu for a space, or an invisible placeholder when there is nothing to
     * offer: no calendar-relevant dates, no configuration rights and nobody logged in.
     *
     * <p>An Event space offers its own date as a one-off copy (an {@code .ics} file, or a
     * pre-filled form at Google or Outlook). A space containing Events offers a
     * <em>subscription</em> to the series instead, which is the only form that keeps up when
     * an event is later rescheduled. A space that is both gets both submenus.</p>
     *
     * <p>For viewers of maintainer tier or above, the menu additionally offers "configure"
     * (leading to the space's About tab) and "add view display..." (the About tab's
     * view-displays action, as a direct shortcut).</p>
     *
     * @param id    the Wicket component id
     * @param space the space to build the menu for
     * @return the menu, or an invisible {@link EmptyPanel}
     */
    public static Component forSpace(String id, Space space) {
        List<Group> groups = new ArrayList<>();

        CalendarEvent ownEvent = CalendarFeedPage.isEvent(space)
                ? CalendarEvent.fromSpace(space, SpacePage.urlFor(space.getId())) : null;
        if (ownEvent != null) {
            groups.add(new Group("add to calendar", List.of(
                    link("📥", "download .ics", CalendarFeedPage.urlFor(space.getId(), true)),
                    externalLink("📅", "add to Google Calendar", CalendarUrls.addToGoogle(ownEvent)),
                    externalLink("📆", "add to Outlook", CalendarUrls.addToOutlook(ownEvent)))));
        }

        if (CalendarFeedPage.hasSubEvents(space)) {
            String feedUrl = CalendarFeedPage.urlFor(space.getId(), false);
            groups.add(new Group("add events to calendar", List.of(
                    link("🔔", "subscribe (webcal)", CalendarUrls.asWebcal(feedUrl)),
                    externalLink("📅", "subscribe in Google Calendar", CalendarUrls.subscribeInGoogle(feedUrl)),
                    externalLink("📆", "subscribe in Outlook", CalendarUrls.subscribeInOutlook(feedUrl, space.getLabel())),
                    copyLink(feedUrl),
                    link("📥", "download all events (.ics)", CalendarFeedPage.urlFor(space.getId(), true)))));
        }

        List<AbstractLink> extraEntries = new ArrayList<>();
        if (SpaceMemberRole.isViewerEntitled(Set.of(KPXL_TERMS.MAINTAINER_ROLE), space)) {
            BookmarkablePageLink<Void> configure = new BookmarkablePageLink<>("link", SpacePage.class,
                    new PageParameters().set("id", space.getId()).set("tab", "about"));
            configure.setBody(Model.of("<span class=\"actionmenu-icon\">⚙</span>configure")).setEscapeModelStrings(false);
            extraEntries.add(configure);
            AbstractLink addViewDisplay = addViewDisplayLink(space);
            if (addViewDisplay != null) extraEntries.add(addViewDisplay);
        }

        return build(id, groups, extraEntries, space);
    }

    /**
     * The "add view display..." shortcut: the result-level action of the About tab's
     * view-displays view ({@link AboutSpacePanel#VIEW_DISPLAYS_VIEW}), rendered the same
     * way the view itself renders it (template link with the space pre-filled as target),
     * so the shortcut stays in sync with the view nanopub's action declaration.
     */
    private static AbstractLink addViewDisplayLink(Space space) {
        try {
            View view = View.get(AboutSpacePanel.VIEW_DISPLAYS_VIEW);
            if (view == null) return null;
            for (IRI actionIri : view.getViewResultActionList()) {
                if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), space, null)) continue;
                Template t = view.getTemplateForAction(actionIri);
                if (t == null) continue;
                String targetField = view.getTemplateTargetFieldForAction(actionIri);
                if (targetField == null) targetField = "resource";
                PageParameters params = new PageParameters().set("template", t.getId())
                        .set("param_" + targetField, space.getId())
                        .set("context", space.getId())
                        .set("template-version", "latest")
                        .set("refresh-upon-publish", space.getId());
                String label = view.getLabelForAction(actionIri);
                if (label == null) label = "add view display";
                if (!label.endsWith("...")) label += "...";
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<>("link", PublishPage.class, params);
                String iconBody = Utils.menuEntryIconBodyHtml(label);
                if (iconBody != null) {
                    l.setBody(Model.of(iconBody)).setEscapeModelStrings(false);
                } else {
                    l.setBody(Model.of(label));
                }
                return l;
            }
        } catch (Exception ex) {
            logger.error("Couldn't build add-view-display shortcut for space {}", space.getId(), ex);
        }
        return null;
    }

    /**
     * An entry handled without leaving the page: a file download, or a {@code webcal:} URL
     * handed straight to the operating system's calendar application. Opening these in a new
     * tab would leave the user staring at a blank one.
     */
    private static AbstractLink link(String icon, String label, String url) {
        ExternalLink externalLink = new ExternalLink("link", url);
        externalLink.setBody(Model.of("<span class=\"actionmenu-icon\">" + icon + "</span>" + label))
                .setEscapeModelStrings(false);
        return externalLink;
    }

    /** An entry leading to a third-party web calendar, opened in a new tab. */
    private static AbstractLink externalLink(String icon, String label, String url) {
        AbstractLink externalLink = link(icon, label, url);
        externalLink.add(AttributeModifier.replace("target", "_blank"));
        externalLink.add(AttributeModifier.replace("rel", "noopener noreferrer"));
        return externalLink;
    }

    /**
     * The "copy feed URL" entry, for pasting into a calendar application that offers no
     * web-based subscription flow (Thunderbird, Apple Calendar on some platforms, most
     * self-hosted clients).
     */
    private static AbstractLink copyLink(String feedUrl) {
        AjaxLink<Void> copy = new AjaxLink<>("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String escaped = feedUrl.replace("\\", "\\\\").replace("'", "\\'");
                target.appendJavaScript(
                        "navigator.clipboard.writeText('" + escaped + "')" +
                        ".then(function() { showToast('Calendar feed URL copied to clipboard!'); })" +
                        ".catch(function(err) { console.error('Copy failed:', err); });"
                );
            }
        };
        copy.setBody(Model.of("<span class=\"actionmenu-icon\">⧉</span>copy feed URL")).setEscapeModelStrings(false);
        return copy;
    }

}
