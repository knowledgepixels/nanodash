package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.visit.IVisit;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.template.TemplateUnavailableException;
import com.knowledgepixels.nanodash.DiscussionThread;
import com.knowledgepixels.nanodash.NanodashSession;
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
    // The parameters of the response form that is open, for its preview to publish with.
    private PageParameters openParams = null;

    ThreadNodePanel(String markupId, ThreadContext context, DiscussionThread.Node node, int depth) {
        super(markupId);
        this.context = context;
        this.node = node;
        this.depth = depth;
        this.shown = Math.min(context.getPageSize(), node.getChildren().size());
        setOutputMarkupId(true);
        build();
    }

    // Builds this card and everything below it anew, responses included: only for folding and
    // unfolding the branch. Folding asks first if that would throw away a response being
    // written (see askBeforeDiscarding); an unfolded branch has no form open yet.
    private void rebuild(AjaxRequestTarget target) {
        build();
        target.add(this);
    }

    // Updates the responses below this card, and not the card: its response form stays as it is.
    private void updateChildrenArea(AjaxRequestTarget target) {
        Component area = buildChildrenArea();
        addOrReplace(area);
        target.add(area);
    }

    // Updates the card (for which action shows as open) and the response box below it, and
    // nothing else: the responses below keep their state, open forms included.
    private void updateResponseBox(AjaxRequestTarget target) {
        Component card = buildCard();
        addOrReplace(card);
        Component box = buildResponseBox();
        addOrReplace(box);
        target.add(card, box);
    }

    /**
     * Asks, before an AJAX link goes ahead, whether to throw away the response someone has
     * started writing in a form it would close or rebuild (threadFormsMayClose in nanodash.js).
     *
     * @param attributes the link's AJAX attributes
     * @param scope      which response forms the link would close or rebuild, seen from the
     *                   link: "page" (any), "box" (the one it is in), "node" (those of its card
     *                   and the responses below), or "children" (those of the responses below)
     */
    private static void askBeforeDiscarding(AjaxRequestAttributes attributes, String scope) {
        attributes.getAjaxCallListeners().add(new AjaxCallListener()
                .onPrecondition("return threadFormsMayClose(this, '" + scope + "');"));
    }

    /**
     * Closes the response form of any other card on the page, of this and any other thread
     * view: one response form at a time. Two would not only be one too many to keep track of,
     * but share the element ids of the publish form's markup, which would mix them up (a click
     * on the consent text of one ticking the box of the other). The cards are collected first
     * and closed after, as closing changes the component tree being looked through.
     *
     * @param target the AJAX target of the request opening this card's form
     */
    private void closeOtherResponseForms(AjaxRequestTarget target) {
        List<ThreadNodePanel> open = new ArrayList<>();
        getPage().visitChildren(ThreadNodePanel.class, (ThreadNodePanel panel, IVisit<Void> visit) -> {
            if (panel != this && panel.isResponseFormOpen()) {
                open.add(panel);
            }
        });
        for (ThreadNodePanel panel : open) {
            panel.openAction = null;
            panel.updateResponseBox(target);
        }
    }

    /**
     * Whether this card has its response form open. Content that holds such a card is not to
     * be replaced by a refresh: the form would go with it, and what was typed into it.
     *
     * @return true if a response form is open on this card
     */
    public boolean isResponseFormOpen() {
        return openAction != null;
    }

    /**
     * Whether the given content holds a thread card with its response form open, which
     * replacing the content would throw away together with what was typed into it.
     *
     * @param content the content to look into
     * @return true if a response form is open in it
     */
    public static boolean hasOpenResponseForm(MarkupContainer content) {
        Boolean open = content.visitChildren(ThreadNodePanel.class,
                (ThreadNodePanel panel, IVisit<Boolean> visit) -> {
                    if (panel.isResponseFormOpen()) {
                        visit.stop(true);
                    }
                });
        return Boolean.TRUE.equals(open);
    }

    private void build() {
        addOrReplace(collapsed ? buildCollapsedLine() : new Label("collapsedLine").setVisible(false));
        addOrReplace(collapsed ? new Label("card").setVisible(false) : buildCard());
        addOrReplace(buildResponseBox());
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
        card.setOutputMarkupId(true);
        card.add(new org.apache.wicket.behavior.AttributeAppender("class", " " + node.getRelation().getCssClass()));

        AjaxLink<Void> collapse = new AjaxLink<>("collapse") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                askBeforeDiscarding(attributes, "node");
            }

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
            PageParameters params = ViewActionMappings.buildResponseActionParams(context.getView(), actionIri,
                    node, context.getQueryRef(), context.getContextId(), context.getPartId(),
                    context.getPostPublishTab());
            if (params == null) {
                continue;
            }
            WebMarkupContainer item = new WebMarkupContainer(actions.newChildId());
            AjaxLink<Void> link = new AjaxLink<>("action") {
                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    // Opening a form closes whichever is open on the page, this card's too.
                    askBeforeDiscarding(attributes, "page");
                }

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (actionIri.equals(openAction)) {
                        // A second click on the open action closes its form again.
                        openAction = null;
                        updateResponseBox(target);
                        return;
                    }
                    // Only a logged-in user can publish, and the inline form fails without
                    // a word for anyone else: send them through the login to the full
                    // publish form instead, as the view's own action buttons do (#937).
                    String loginUrl = NanodashSession.get().getLoginUrl(PublishPage.MOUNT_PATH, params);
                    if (loginUrl != null) {
                        throw new RedirectToUrlException(loginUrl);
                    }
                    closeOtherResponseForms(target);
                    openAction = actionIri;
                    updateResponseBox(target);
                    if (openAction != null) {
                        target.appendJavaScript("updateInsertedForm('"
                                + ThreadNodePanel.this.get("responseBox").getMarkupId() + "');");
                    }
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

    private Component buildResponseBox() {
        PageParameters params = null;
        // Checked again here, as the user might have logged out since opening the form.
        if (openAction != null && NanodashSession.get().isProfileComplete()) {
            params = ViewActionMappings.buildResponseActionParams(context.getView(), openAction,
                    node, context.getQueryRef(), context.getContextId(), context.getPartId(),
                    context.getPostPublishTab());
        }
        openParams = params;
        if (params == null) {
            openAction = null;
            // With a placeholder, so that opening a form can put the box in its place.
            return new Label("responseBox").setOutputMarkupPlaceholderTag(true).setVisible(false);
        }
        WebMarkupContainer box = new WebMarkupContainer("responseBox");
        box.setOutputMarkupId(true);

        DiscussionThread.Relation relation = context.getView().getResponseRelationForAction(openAction);
        box.add(new Label("responseAction", relation == null || relation.getActionLabel() == null
                ? "respond" : relation.getActionLabel()));
        box.add(new Label("responseTarget", "in response to “" + Utils.truncateLinkLabel(node.getLabel()) + "”"));
        box.add(new AjaxLink<Void>("closeForm") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                askBeforeDiscarding(attributes, "box");
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                openAction = null;
                updateResponseBox(target);
            }
        });

        Component form;
        try {
            form = new PublishForm("responseForm", params, PublishPage.class, null, this::showPreview);
        } catch (TemplateUnavailableException ex) {
            form = PublishForm.templateUnavailableNotice("responseForm", ex);
        }
        box.add(form);

        WebMarkupContainer previewSlot = new WebMarkupContainer("previewSlot");
        previewSlot.setOutputMarkupId(true);
        previewSlot.add(new WebMarkupContainer("preview").setVisible(false));
        box.add(previewSlot);
        return box;
    }

    // Shows the preview of the response form in its place, keeping the form as it is.
    private void showPreview(AjaxRequestTarget target, Nanopub signedNp, boolean consentGiven) {
        MarkupContainer box = (MarkupContainer) get("responseBox");
        Component form = box.get("responseForm");
        MarkupContainer previewSlot = (MarkupContainer) box.get("previewSlot");
        form.setVisible(false);
        previewSlot.addOrReplace(new InlinePreviewPanel("preview", signedNp, openParams, consentGiven, this::hidePreview));
        target.add(form, previewSlot);
        // A preview means something written, even if every field came filled in.
        target.appendJavaScript("markThreadFormDirty('" + box.getMarkupId() + "');");
    }

    // Goes back from the preview to the form, as it was left.
    private void hidePreview(AjaxRequestTarget target) {
        MarkupContainer box = (MarkupContainer) get("responseBox");
        Component form = box.get("responseForm");
        MarkupContainer previewSlot = (MarkupContainer) box.get("previewSlot");
        form.setVisible(true);
        previewSlot.addOrReplace(new WebMarkupContainer("preview").setVisible(false));
        target.add(form, previewSlot);
        target.appendJavaScript("updateInsertedForm('" + form.getMarkupId() + "');");
    }

    private WebMarkupContainer buildChildrenArea() {
        WebMarkupContainer area = new WebMarkupContainer("childrenArea");
        area.setOutputMarkupId(true);
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
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                askBeforeDiscarding(attributes, "children");
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                shown = Math.min(shown + context.getPageSize(), node.getChildren().size());
                updateChildrenArea(target);
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
