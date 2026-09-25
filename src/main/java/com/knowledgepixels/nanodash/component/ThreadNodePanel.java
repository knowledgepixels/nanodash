package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.DiscussionThread;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.page.ViewResultsPage;

public class ThreadNodePanel extends Panel {

    private final ThreadContext context;
    private final DiscussionThread.Node node;
    private final int depth;

    private int shown;
    private boolean collapsed = false;
    private IRI openAction = null;

    ThreadNodePanel(String markupId, ThreadContext context, DiscussionThread.Node node, int depth) {
        super(markupId);
        this.context = context;
        this.node = node;
        this.depth = depth;
        this.shown = Math.min(context.getPageSize(), node.getChildren().size());
        setOutputMarkupId(true);
        build();
    }

    private void rebuild(AjaxRequestTarget target) {
        build();
        target.add(this);
    }

    private void build() {
        addOrReplace(collapsed ? buildCollapsedLine() : new Label("collapsedLine").setVisible(false));
        addOrReplace(collapsed ? new Label("card").setVisible(false) : buildCard());
        addOrReplace(buildResponseForm());
        addOrReplace(collapsed ? new Label("childrenArea").setVisible(false) : buildChildrenArea());
    }

    private WebMarkupContainer buildCollapsedLine() {
        WebMarkupContainer line = new WebMarkupContainer("collapsedLine");
        AjaxLink<Void> expand = new AjaxLink<>("expand") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                collapsed = false;
                rebuild(target);
            }
        };
        line.add(expand);
        Label badge = new Label("collapsedBadge", node.getBadge());
        badge.add(new org.apache.wicket.behavior.AttributeAppender("class", " " + node.getRelation().getCssClass()));
        line.add(badge);
        StringBuilder summary = new StringBuilder(node.getLabel());
        String author = displayAuthor();
        if (author != null) {
            summary.append(" · ").append(author);
        }
        summary.append(" (collapsed, ").append(countText(node.getDescendantCount())).append(")");
        line.add(new Label("collapsedSummary", summary.toString()));
        return line;
    }

    private WebMarkupContainer buildCard() {
        WebMarkupContainer card = new WebMarkupContainer("card");
        card.add(new org.apache.wicket.behavior.AttributeAppender("class", " " + node.getRelation().getCssClass()));

        AjaxLink<Void> collapse = new AjaxLink<>("collapse") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                collapsed = true;
                openAction = null;
                rebuild(target);
            }
        };
        collapse.setVisible(!node.getChildren().isEmpty());
        card.add(collapse);

        Label badge = new Label("badge", node.getBadge());
        badge.add(new AttributeAppender("class", " " + node.getRelation().getCssClass()));
        card.add(badge);

        int direct = node.getChildren().size();
        Label count = new Label("count", depth == 0
                ? direct + " direct " + (direct == 1 ? "response" : "responses")
                : countText(node.getDescendantCount()));
        count.setVisible(direct > 0);
        card.add(count);

        card.add(new NanodashLink("statement", node.getId(), null, null, node.getLabel(), context.getContextId()));
        card.add(authorLabel());
        String date = node.getDate();
        Label dateLabel = new Label("date", date == null ? "" : Utils.friendlyDateHtml(date, date));
        dateLabel.setEscapeModelStrings(false);
        dateLabel.setVisible(date != null);
        card.add(dateLabel);

        card.add(buildActions());
        return card;
    }

    private WebMarkupContainer buildActions() {
        RepeatingView actions = new RepeatingView("actions");
        for (IRI actionIri : context.getResponseActions()) {
            DiscussionThread.Relation relation = context.getView().getResponseRelationForAction(actionIri);
            if (relation == null) {
                continue;
            }
            if (!SpaceMemberRole.isViewerEntitled(context.getView().getActionVisibleTo(actionIri),
                    context.getEntitlementResource(), context.getRefRoot())) {
                continue;
            }
            // An action whose mappings this response does not satisfy is not offered on it,
            // exactly as its button would be left out of a result row.
            if (ViewActionMappings.buildResponseActionParams(context.getView(), actionIri, node,
                    context.getQueryRef(), context.getContextId(), context.getPartId(),
                    context.getPostPublishTab()) == null) {
                continue;
            }
            WebMarkupContainer item = new WebMarkupContainer(actions.newChildId());
            AjaxLink<Void> link = new AjaxLink<>("action") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    // A second click on the open action closes its form again.
                    openAction = actionIri.equals(openAction) ? null : actionIri;
                    rebuild(target);
                }
            };
            if (actionIri.equals(openAction)) {
                link.add(new AttributeAppender("class", " thread-action-open"));
            }
            link.add(new Label("actionLabel", relation.getActionLabel() == null
                    ? relation.getId() : relation.getActionLabel()));
            item.add(link);
            actions.add(item);
        }
        WebMarkupContainer container = new WebMarkupContainer("actionsContainer");
        container.add(actions);
        container.setVisible(actions.size() > 0);
        return container;
    }

    private Component buildResponseForm() {
        if (openAction == null) {
            return new Label("responseForm").setVisible(false);
        }
        PageParameters params = ViewActionMappings.buildResponseActionParams(context.getView(), openAction,
                node, context.getQueryRef(), context.getContextId(), context.getPartId(),
                context.getPostPublishTab());
        if (params == null) {
            openAction = null;
            return new Label("responseForm").setVisible(false);
        }
        return new PublishForm("responseForm", params, PublishPage.class, null);
    }

    private WebMarkupContainer buildChildrenArea() {
        WebMarkupContainer area = new WebMarkupContainer("childrenArea");
        List<DiscussionThread.Node> children = node.getChildren();

        RepeatingView childViews = new RepeatingView("children");
        boolean tooDeep = depth + 1 >= context.getMaxDepth();
        if (!tooDeep) {
            for (int i = 0; i < Math.min(shown, children.size()); i++) {
                childViews.add(new ThreadNodePanel(childViews.newChildId(), context, children.get(i), depth + 1));
            }
        }
        area.add(childViews);

        int remaining = children.size() - shown;
        AjaxLink<Void> loadMore = new AjaxLink<>("loadMore") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                shown = Math.min(shown + context.getPageSize(), node.getChildren().size());
                rebuild(target);
            }
        };
        loadMore.add(new Label("loadMoreLabel", "+ load " + remaining + " more "
                + (remaining == 1 ? "response" : "responses")));
        loadMore.setVisible(!tooDeep && remaining > 0);
        area.add(loadMore);

        BookmarkablePageLink<Void> continueLink = new BookmarkablePageLink<>("continueThread",
                ViewResultsPage.class, continueParams());
        continueLink.add(new Label("continueLabel", "↳ continue this thread ("
                + countText(node.getDescendantCount()) + ", "
                + node.getDepthBelow() + " " + (node.getDepthBelow() == 1 ? "level" : "levels") + " deep)"));
        continueLink.setVisible(tooDeep && !children.isEmpty() && context.getViewId() != null);
        area.add(continueLink);

        area.setVisible(!children.isEmpty());
        return area;
    }

    private PageParameters continueParams() {
        PageParameters params = new PageParameters();
        if (context.getViewId() == null) {
            return params;
        }
        params.set("view", context.getViewId());
        params.set("queryparam_" + context.getQueryField(), node.getId());
        return params;
    }

    private static String countText(int count) {
        return count + " " + (count == 1 ? "response" : "responses");
    }

    private String displayAuthor() {
        String label = node.getUserLabel();
        if (label != null) {
            return label;
        }
        String iri = node.getUserIri();
        if (iri == null) {
            return null;
        }
        return User.getShortDisplayName(Utils.vf.createIRI(iri));
    }

    private Label authorLabel() {
        String iriString = node.getUserIri();
        if (iriString == null) {
            return (Label) new Label("author", "").setVisible(false);
        }
        IRI userIri = Utils.vf.createIRI(iriString);
        ProfilePicture picture = User.getProfilePicture(userIri);
        String imgSrc;
        String iconClass;
        if (picture != null) {
            imgSrc = Strings.escapeMarkup(picture.getSrc()).toString();
            iconClass = "user-icon";
        } else if (IndividualAgent.isSoftware(userIri)) {
            imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/bot-icon.svg", false), null).toString();
            iconClass = "bot-icon";
        } else {
            imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/user-icon.svg", false), null).toString();
            iconClass = "user-icon";
        }
        String name = displayAuthor();
        String url = UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(iriString);
        String html = "<img class=\"" + iconClass + "\" src=\"" + imgSrc + "\" /> <a href=\""
                + Strings.escapeMarkup(url) + "\">" + Strings.escapeMarkup(name) + "</a>";
        Label label = new Label("author", html);
        label.setEscapeModelStrings(false);
        return label;
    }

    static class ThreadContext implements Serializable {

        private static final long serialVersionUID = 1L;

        private final View view;
        private final String viewId;
        private final String queryField;
        private final org.nanopub.extra.services.QueryRef queryRef;
        private final String contextId;
        private final String partId;
        private final String postPublishTab;
        private final String refRoot;
        private final com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile entitlementResource;
        private final int pageSize;
        private final int maxDepth;
        private final List<IRI> responseActions;

        ThreadContext(View view, org.nanopub.extra.services.QueryRef queryRef, String contextId, String partId,
                String postPublishTab, String refRoot,
                com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile entitlementResource,
                int pageSize, int maxDepth) {
            this.view = view;
            this.viewId = view == null ? null : view.getId();
            String field = view == null ? null : view.getQueryField();
            this.queryField = field == null ? "resource" : field;
            this.queryRef = queryRef;
            this.contextId = contextId;
            this.partId = partId;
            this.postPublishTab = postPublishTab;
            this.refRoot = refRoot;
            this.entitlementResource = entitlementResource;
            this.pageSize = pageSize;
            this.maxDepth = maxDepth;
            List<IRI> actions = new ArrayList<>();
            if (view != null) {
                // Wherever an action is declared, naming a relation is what makes it a
                // response action here. A result action that names one is offered on every
                // card as well as at the top of the view: at the top it responds to the
                // page's resource, on a card to that response.
                for (IRI actionIri : view.getViewEntryActionList()) {
                    if (view.getResponseRelationForAction(actionIri) != null) {
                        actions.add(actionIri);
                    }
                }
                for (IRI actionIri : view.getViewResultActionList()) {
                    if (view.getResponseRelationForAction(actionIri) != null) {
                        actions.add(actionIri);
                    }
                }
            }
            this.responseActions = actions;
        }

        View getView() {
            return view;
        }

        String getViewId() {
            return viewId;
        }

        String getQueryField() {
            return queryField;
        }

        org.nanopub.extra.services.QueryRef getQueryRef() {
            return queryRef;
        }

        String getContextId() {
            return contextId;
        }

        String getPartId() {
            return partId;
        }

        String getPostPublishTab() {
            return postPublishTab;
        }

        String getRefRoot() {
            return refRoot;
        }

        com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile getEntitlementResource() {
            return entitlementResource;
        }

        /**
         * How many responses of a level are shown before "load more".
         */
        int getPageSize() {
            return pageSize;
        }

        /**
         * The depth at which responses give way to a "continue this thread"
         * link.
         */
        int getMaxDepth() {
            return maxDepth;
        }

        List<IRI> getResponseActions() {
            return responseActions;
        }

    }

}
