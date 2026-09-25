package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewAnchors;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The view displays of a resource, laid out in the groups and sections of its page.
 *
 * <p>The resource is held as a model rather than as an object, so a page restored from the page
 * store renders the resource the repositories hold now rather than the one serialized with the
 * component tree (issue #459). Everything derived from it -- which view displays there are, how
 * they group, their section anchors -- is worked out when the list renders, not when it is built.
 */
public class ViewList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(ViewList.class);

    // The ref (root definition) this list is pinned to (?root=), used to gate each view's
    // action visibility against the claimant being viewed rather than the resource's
    // representative ref. Null = representative ref. Read at render time by the inner
    // ListView, so it may be set after the delegating constructor. See docs/space-ref-identity.md.
    private String refRoot;

    private final String partId;
    private final String nanopubId;
    private final Set<IRI> partClasses;
    private final List<ViewDisplay> explicitViewDisplays;
    private final boolean showEmptyNotice;

    // The queries already marked as outdated, so that a re-render of this same list does not keep
    // marking them: the inner ListView repopulates on every render.
    private final Set<String> refreshMarked = new HashSet<>();

    private final IModel<Layout> layoutModel = new LoadableDetachableModel<Layout>() {

        @Override
        protected Layout load() {
            return layOut();
        }

    };

    /**
     * The view displays of this list as they are laid out on the page, worked out afresh on every
     * request.
     *
     * @param viewDisplays  the view displays to show
     * @param groups        the same view displays, grouped by the first segment of their structural
     *                      position (e.g. "4" from "4.4.1.papers")
     * @param anchorGroups  the section anchors of those view displays, grouped in lock-step with
     *                      {@code groups}, so a rendered item is matched to its anchor by group
     *                      index and item index
     * @param refreshViews  whether the queries behind these views are to be marked as outdated, so
     *                      that each view comes back through {@link RefreshingResultPanel} and swaps
     *                      in new results once its query has run
     */
    private record Layout(List<ViewDisplay> viewDisplays, List<List<ViewDisplay>> groups,
                          List<List<String>> anchorGroups, boolean refreshViews) {
    }

    /**
     * The view displays a resource shows on its own page.
     *
     * @param markupId the component id
     * @param resource the resource whose views to show
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource) {
        this(markupId, resource, null, null, null, null, null, true, null);
    }

    /**
     * The view displays of one part of a resource.
     *
     * @param markupId    the component id
     * @param resource    the resource the part belongs to
     * @param partId      the id of the part
     * @param nanopubId   the id of the nanopublication the part comes from
     * @param partClasses the classes the part is an instance of
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, String partId, String nanopubId, Set<IRI> partClasses) {
        this(markupId, resource, partId, nanopubId, partClasses, null, null, true, null);
    }

    /**
     * The view displays of one part of a resource, with a footer of admin buttons.
     *
     * @param markupId           the component id
     * @param resource           the resource the part belongs to
     * @param partId             the id of the part
     * @param nanopubId          the id of the nanopublication the part comes from
     * @param partClasses        the classes the part is an instance of
     * @param footerResource     the resource the footer buttons act on, or null for the resource itself
     * @param footerAdminButtons the buttons to show to admins in the footer
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, String partId, String nanopubId, Set<IRI> partClasses, IModel<? extends AbstractResourceWithProfile> footerResource, List<AbstractLink> footerAdminButtons) {
        this(markupId, resource, partId, nanopubId, partClasses, footerResource, footerAdminButtons, true, null);
    }

    /**
     * As {@link #ViewList(String, IModel, String, String, Set, IModel, List)} but able to leave out
     * the notice shown when there is nothing to display.
     *
     * @param markupId           the component id
     * @param resource           the resource the part belongs to
     * @param partId             the id of the part
     * @param nanopubId          the id of the nanopublication the part comes from
     * @param partClasses        the classes the part is an instance of
     * @param footerResource     the resource the footer buttons act on, or null for the resource itself
     * @param footerAdminButtons the buttons to show to admins in the footer
     * @param showEmptyNotice    whether to show the notice when there is no view display to show
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, String partId, String nanopubId, Set<IRI> partClasses, IModel<? extends AbstractResourceWithProfile> footerResource, List<AbstractLink> footerAdminButtons, boolean showEmptyNotice) {
        this(markupId, resource, partId, nanopubId, partClasses, footerResource, footerAdminButtons, showEmptyNotice, null);
    }

    /**
     * The given view displays of a resource, instead of the ones its page would show.
     *
     * @param markupId             the component id
     * @param resource             the resource the views are about
     * @param explicitViewDisplays the view displays to show
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, List<ViewDisplay> explicitViewDisplays) {
        this(markupId, resource, null, null, null, null, null, false, explicitViewDisplays);
    }

    /**
     * As {@link #ViewList(String, IModel, List)} but pinned to a specific ref (root definition), so
     * each view's action visibility is gated against that claimant's authority. Used for
     * {@code ?root=}-pinned space pages. See docs/space-ref-identity.md.
     *
     * @param markupId             the component id
     * @param resource             the resource the views are about
     * @param explicitViewDisplays the view displays to show
     * @param refRoot              the ref this list is pinned to
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, List<ViewDisplay> explicitViewDisplays, String refRoot) {
        this(markupId, resource, null, null, null, null, null, false, explicitViewDisplays);
        this.refRoot = refRoot;
    }

    /**
     * As {@link #ViewList(String, IModel, List)} but with a footer of admin buttons.
     *
     * @param markupId             the component id
     * @param resource             the resource the views are about
     * @param explicitViewDisplays the view displays to show
     * @param footerResource       the resource the footer buttons act on, or null for the resource itself
     * @param footerAdminButtons   the buttons to show to admins in the footer
     */
    public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, List<ViewDisplay> explicitViewDisplays, IModel<? extends AbstractResourceWithProfile> footerResource, List<AbstractLink> footerAdminButtons) {
        this(markupId, resource, null, null, null, footerResource, footerAdminButtons, false, explicitViewDisplays);
    }

    private ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, String partId, String nanopubId, Set<IRI> partClasses, IModel<? extends AbstractResourceWithProfile> footerResource, List<AbstractLink> footerAdminButtons, boolean showEmptyNotice, List<ViewDisplay> explicitViewDisplays) {
        super(markupId, resource);
        this.partId = partId;
        this.nanopubId = nanopubId;
        this.partClasses = partClasses;
        this.explicitViewDisplays = explicitViewDisplays;
        this.showEmptyNotice = showEmptyNotice;

        add(new ListView<List<ViewDisplay>>("groups", groupsModel()) {

            @Override
            protected void populateItem(ListItem<List<ViewDisplay>> groupItem) {
                final List<String> groupAnchors = layoutModel.getObject().anchorGroups().get(groupItem.getIndex());
                groupItem.add(new ListView<ViewDisplay>("views", groupItem.getModelObject()) {

                    @Override
                    protected void populateItem(ListItem<ViewDisplay> item) {
                        // The section's fragment identifier. Set on the item (the element
                        // wrapping the whole view panel) rather than inside the panel, so
                        // it is there from the first render even while the panel itself is
                        // still loading over Ajax.
                        item.add(AttributeModifier.replace("id", groupAnchors.get(item.getIndex())));
                        populateView(item);
                    }

                });
            }

        });

        add(new WebMarkupContainer("emptynotice") {

            @Override
            public boolean isVisible() {
                return ViewList.this.showEmptyNotice && layoutModel.getObject().viewDisplays().isEmpty();
            }

        });

        WebMarkupContainer footerSection = new WebMarkupContainer("footer-section");
        if (footerAdminButtons != null) {
            footerSection.add(new ButtonList("footer-buttons",
                    footerResource != null ? footerResource : resource,
                    null, null, footerAdminButtons));
        } else {
            footerSection.setVisible(false);
            footerSection.add(new Label("footer-buttons").setVisible(false));
        }
        add(footerSection);

        add(new WebMarkupContainer("page-footer").setVisible(false));
    }

    /**
     * @return the resource whose views this list shows, as it is now
     */
    public AbstractResourceWithProfile getResource() {
        return (AbstractResourceWithProfile) getDefaultModelObject();
    }

    private IModel<List<List<ViewDisplay>>> groupsModel() {
        return new LoadableDetachableModel<List<List<ViewDisplay>>>() {

            @Override
            protected List<List<ViewDisplay>> load() {
                return layoutModel.getObject().groups();
            }

        };
    }

    private Layout layOut() {
        AbstractResourceWithProfile resource = getResource();
        boolean refreshViews = resource != null && resource.isViewRefreshDue(explicitViewDisplays == null);
        final List<ViewDisplay> viewDisplays;
        if (explicitViewDisplays != null) {
            viewDisplays = explicitViewDisplays;
        } else if (partId == null) {
            viewDisplays = resource.getTopLevelViewDisplays();
        } else {
            viewDisplays = resource.getPartLevelViewDisplays(partId, partClasses);
        }

        // The fragment identifier of each view display's section, so a single section can
        // be linked to (e.g. ".../space?id=...#messages"). Computed over the whole list
        // before grouping, since uniqueness is page-wide. See ViewAnchors.
        final List<String> anchors = ViewAnchors.forViewDisplays(viewDisplays);

        List<List<ViewDisplay>> groups = new ArrayList<>();
        final List<List<String>> anchorGroups = new ArrayList<>();
        String currentGroupKey = null;
        List<ViewDisplay> currentGroup = null;
        List<String> currentAnchorGroup = null;
        for (int i = 0; i < viewDisplays.size(); i++) {
            ViewDisplay vd = viewDisplays.get(i);
            String pos = vd.getStructuralPosition();
            int firstDot = pos.indexOf('.');
            String key = firstDot > 0 ? pos.substring(0, firstDot) : pos;
            if (!key.equals(currentGroupKey)) {
                currentGroup = new ArrayList<>();
                groups.add(currentGroup);
                currentAnchorGroup = new ArrayList<>();
                anchorGroups.add(currentAnchorGroup);
                currentGroupKey = key;
            }
            currentGroup.add(vd);
            currentAnchorGroup.add(anchors.get(i));
        }
        return new Layout(viewDisplays, groups, anchorGroups, refreshViews);
    }

    private void populateView(ListItem<ViewDisplay> item) {
        // This populate runs at render time (onBeforeRender) and is the only
        // view-rendering path without a guard; every other one (the QueryResult
        // builders, populateComponent, per-cell populateItem) already degrades a
        // failure to an inline error. Without this catch, a single view whose
        // build/render dereferences a null takes down the whole page render.
        try {
            AbstractResourceWithProfile resource = getResource();
            final String id = (partId == null ? resource.getId() : partId);
            final String npId = (nanopubId == null ? resource.getNanopubId() : nanopubId);
            View view = item.getModelObject().getView();
            if (KPXL_TERMS.HEADER_VIEW.equals(view.getViewType())) {
                // Header views have no query (issue #572), so they skip all the
                // query machinery below and render directly.
                Component header = new HeaderViewPanel("view", item.getModelObject(), resource, id, resource.getId(), refRoot);
                header.add(new AttributeAppender("class", " col-" + item.getModelObject().getDisplayWidth() + " section-header"));
                item.add(header);
                return;
            }
            Multimap<String, String> queryRefParams = ArrayListMultimap.create();
            for (String p : view.getQuery().getPlaceholdersList()) {
                String paramName = QueryTemplate.getParamName(p);
                if (paramName.equals(view.getQueryField())) {
                    queryRefParams.put(view.getQueryField(), id);
                    if (QueryTemplate.isMultiPlaceholder(p) && resource instanceof Space space) {
                        // TODO Support this also for maintained resources and users.
                        for (String altId : space.getAltIDs()) {
                            queryRefParams.put(view.getQueryField(), altId);
                        }
                    }
                } else if (paramName.equals(view.getQueryField() + "Namespace") && resource.getNamespace() != null) {
                    queryRefParams.put(view.getQueryField() + "Namespace", resource.getNamespace());
                } else if (paramName.equals(view.getQueryField() + "Np")) {
                    if (!QueryTemplate.isOptionalPlaceholder(p) && npId == null) {
                        queryRefParams.put(view.getQueryField() + "Np", "x:");
                    } else {
                        queryRefParams.put(view.getQueryField() + "Np", npId);
                    }
                } else if (paramName.equals("context")) {
                    // Auto-fill the page's context (the resource maintaining the shown
                    // element) so queries like get-part-info can render as content-tab
                    // view displays; on a resource's own page this is the resource itself.
                    queryRefParams.put("context", resource.getId());
                } else if (paramName.equals("root_np")) {
                    // Auto-fill the ref scope (root nanopub) from the page's effective ref,
                    // the same way the resource IRI above is filled, so a content-tab view
                    // whose query opts into ref-scoping is scoped without the panel threading
                    // it. Left empty when no ref is known (an optional placeholder tolerates
                    // the empty VALUES; the ref-scoped query then yields its no-ref result).
                    if (refRoot != null && !refRoot.isEmpty()) {
                        queryRefParams.put("root_np", refRoot);
                    }
                } else if (!QueryTemplate.isOptionalPlaceholder(p)) {
                    // For a query-form view, an unmatched placeholder is not an
                    // error: it becomes a form field the user fills in.
                    if (!view.hasQueryForm()) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter</span>").setEscapeModelStrings(false));
                        logger.error("Error: Query has non-optional parameter: {} {}", view.getQuery().getQueryId(), p);
                        return;
                    }
                }
            }
            if (view.hasQueryForm()) {
                Component form = new QueryFormPanel("view", item.getModelObject(), queryRefParams, null, resource);
                form.add(new AttributeAppender("class", " col-" + item.getModelObject().getDisplayWidth()));
                item.add(form);
                return;
            }
            QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
            if (layoutModel.getObject().refreshViews() && refreshMarked.add(queryRef.getAsUrlString())) {
                ApiCache.clearCache(queryRef, 0);
            }
            Component resultComponent = QueryResultComponentFactory.build("view", queryRef, item.getModelObject(),
                    resource, id, resource.getId(), refRoot);
            if (resultComponent != null) {
                item.add(resultComponent);
            } else if (view.getViewType() != null && View.getSupportedViewTypes().contains(view.getViewType())) {
                item.add(new Label("view", "<span class=\"negative\">View type \"" + view.getViewType().stringValue() + "\" is supported but its view is not implemented yet</span>").setEscapeModelStrings(false));
                logger.error("View type \"{}\" is supported but its view is not implemented yet", view.getViewType().stringValue());
            } else {
                item.add(new Label("view", "<span class=\"negative\">Unsupported view type</span>").setEscapeModelStrings(false));
                logger.error("Unsupported view type.");
            }
        } catch (Exception ex) {
            logger.error("Failed to render view display", ex);
            // Guard against a partial add before the failure so we never add a
            // second component with the same id.
            if (item.get("view") == null) {
                item.add(new Label("view", "<span class=\"negative\">Error rendering this view</span>").setEscapeModelStrings(false));
            }
        }
    }

    /**
     * Replaces the footer of the page this list is the content of.
     *
     * @param footer the footer component to show
     */
    public void setPageFooter(Component footer) {
        replace(footer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetach() {
        layoutModel.detach();
        super.onDetach();
    }

}
