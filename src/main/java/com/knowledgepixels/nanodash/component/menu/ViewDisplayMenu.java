package com.knowledgepixels.nanodash.component.menu;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.MagicQueryParams;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.GuidedChoiceItem;
import com.knowledgepixels.nanodash.component.RefreshingResultPanel;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.page.ViewResultsPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.util.List;
import java.util.Optional;

/**
 * A dropdown menu panel for view displays, replacing the "^" source link.
 * Provides options to show the query, adjust the view display, and see its declaration.
 */
public class ViewDisplayMenu extends BaseDisplayMenu {

    /**
     * Constructs a ViewDisplayMenu.
     *
     * @param id           the Wicket component ID
     * @param viewDisplay  the view display this menu acts on (must have a non-null nanopub)
     * @param queryRef     the query reference used by this view display, or null for a
     *                     query-less view (a header view), which hides the
     *                     query-dependent entries (show query, full screen). "refresh now"
     *                     stays: since issue #654 it brings the view definition up to date
     *                     too, which is the whole of what a header view has to refresh.
     * @param pageResource the page-level resource used to determine whether "adjust" is visible
     * @param viewActions  the view-level actions to show as top entries (may be empty)
     */
    public ViewDisplayMenu(String id, ViewDisplay viewDisplay, QueryRef queryRef, AbstractResourceWithProfile pageResource, List<QueryResult.MenuAction> viewActions) {
        super(id);

        // View-level actions become the top entries of the menu, followed by a
        // separator and then the standard view-display options below.
        DataView<QueryResult.MenuAction> viewActionView = new DataView<>("viewActions", new ListDataProvider<>(viewActions)) {
            @Override
            protected void populateItem(Item<QueryResult.MenuAction> item) {
                QueryResult.MenuAction action = item.getModelObject();
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("viewAction", action.pageClass(), action.params());
                // A label that starts with a leading symbol/emoji renders that as the entry icon.
                String iconBody = Utils.menuEntryIconBodyHtml(action.label());
                if (iconBody != null) {
                    link.setBody(Model.of(iconBody)).setEscapeModelStrings(false);
                } else {
                    link.setBody(Model.of(action.label()));
                }
                item.add(link);
            }
        };
        addEntry("viewActions", viewActionView);
        WebMarkupContainer separator = new WebMarkupContainer("separator");
        separator.setVisible(!viewActions.isEmpty());
        addEntry("separator", separator);

        if (queryRef != null) {
            PageParameters showQueryParams = new PageParameters().set("id", queryRef.getQueryId());
            for (var entry : queryRef.getParams().entries()) {
                showQueryParams.add("queryparam_" + entry.getKey(), entry.getValue());
            }
            addEntry("showQuery", new BookmarkablePageLink<Void>("showQuery", QueryPage.class, showQueryParams)
                    .add(NavigationContext.pageContextFallback()));
        } else {
            addEntry("showQuery", new WebMarkupContainer("showQuery").setVisible(false));
        }

        addEntry("showView", new BookmarkablePageLink<Void>("showView", ExplorePage.class,
                new PageParameters().set("id", viewDisplay.getView().getNanopub().getUri()))
                .add(NavigationContext.pageContextFallback()));

        // Full-screen version of this view on the standalone view-results page, with the
        // current query parameters carried along (magic ones excluded — the results page
        // re-binds them from the session, so carrying them would duplicate the bindings).
        if (queryRef != null) {
            PageParameters fullScreenParams = new PageParameters().set("view", viewDisplay.getView().getId());
            for (var entry : queryRef.getParams().entries()) {
                if (MagicQueryParams.isMagic(entry.getKey())) continue;
                fullScreenParams.add("queryparam_" + entry.getKey(), entry.getValue());
            }
            BookmarkablePageLink<Void> fullScreenLink = new BookmarkablePageLink<>("fullScreen", ViewResultsPage.class, fullScreenParams) {
                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    // Self-referential on the full-screen page itself.
                    setVisible(!(getPage() instanceof ViewResultsPage));
                }
            };
            fullScreenLink.add(NavigationContext.pageContextFallback());
            addEntry("fullScreen", fullScreenLink);
        } else {
            addEntry("fullScreen", new WebMarkupContainer("fullScreen").setVisible(false));
        }

        IRI nanopubId = viewDisplay.getNanopubId();

        // Determine whether "adjust" should be visible for this user on this page
        boolean showAdjust = false;
        NanodashSession session = NanodashSession.get();
        if (pageResource instanceof IndividualAgent ia) {
            showAdjust = ia.isCurrentUser();
        } else if (pageResource instanceof Space s) {
            String pubkeyhash = session.getPubkeyhash();
            showAdjust = pubkeyhash != null && s.isAdminPubkey(pubkeyhash);
        } else if (pageResource != null) {
            Space space = pageResource.getSpace();
            if (space != null) {
                String pubkeyhash = session.getPubkeyhash();
                showAdjust = pubkeyhash != null && space.isAdminPubkey(pubkeyhash);
            }
        }

        // Determine supersede vs derive based on whether this user's pubkey matches the nanopub's.
        // These are only needed when showAdjust is true (i.e. pageResource is non-null).
        String adjustUrl = "";
        String pageResourceId = pageResource != null ? pageResource.getId() : "";
        if (showAdjust) {
            String nanopubPubkey = NanopubElement.get(viewDisplay.getNanopub()).getPubkey();
            String sessionPubkey = session.getPubkeyString();
            String adjustParam = (nanopubPubkey != null && nanopubPubkey.equals(sessionPubkey))
                    ? "supersede" : "derive";
            IRI templateId = TemplateData.get().getTemplateId(viewDisplay.getNanopub());
            String templateUri = templateId != null ? templateId.stringValue()
                    : "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
            adjustUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(templateUri)
                    + "&" + adjustParam + "=" + Utils.urlEncode(nanopubId.stringValue())
                    + "&template-version=latest"
                    + "&context=" + Utils.urlEncode(pageResourceId);
        }
        // "edit"/"deactivate view display" only make sense for an actual view-display
        // assignment (one with a resolved view IRI). Built-in views rendered directly —
        // e.g. a space's About-tab meta-views (roles/members/presets/view-displays) — have
        // no view-display nanopub, so these options are hidden for them.
        boolean isViewDisplay = viewDisplay.getViewIri() != null;
        // Label (with its leading icon) comes from the markup body, so no label arg here.
        ExternalLink adjustLink = new ExternalLink("adjust", adjustUrl);
        adjustLink.setVisible(showAdjust && isViewDisplay);
        addEntry("adjust", adjustLink);

        BookmarkablePageLink<Void> deactivateLink = new BookmarkablePageLink<>("deactivate", PublishPage.class,
                new PageParameters()
                        .set("template", "https://w3id.org/np/RAZ47_4JquvEXk30HYnVeSgFRcQqHtpdibcfBOeqHI2j4")
                        .set("template-version", "latest")
                        .set("param_resource", pageResourceId)
                        .set("param_view", viewDisplay.getViewIri() != null ? viewDisplay.getViewIri().stringValue() : viewDisplay.getView().getId())
                        .set("context", pageResourceId)
                        .set("refresh-upon-publish", pageResourceId));
        deactivateLink.setVisible(showAdjust && isViewDisplay);
        addEntry("deactivate", deactivateLink);

        boolean showAddToOwn = session.getUserIri() != null
                && viewDisplay.getViewIri() != null
                && pageResource instanceof IndividualAgent ia && !ia.isCurrentUser();
        String addToOwnUrl = "";
        if (showAddToOwn) {
            String userIri = session.getUserIri().stringValue();
            String viewIri = viewDisplay.getViewIri().stringValue();
            if (viewDisplay.getView() != null && viewDisplay.getView().getLabel() != null) {
                GuidedChoiceItem.setLabel(viewIri, viewDisplay.getView().getLabel());
            }
            addToOwnUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode("https://w3id.org/np/RAQhTCHtfzGCj1YiE1LualWcZjg3thlRiquFWUE14UF-g")
                    + "&template-version=latest"
                    + "&param_resource=" + Utils.urlEncode(userIri)
                    + "&param_view=" + Utils.urlEncode(viewIri)
                    + "&context=" + Utils.urlEncode(userIri)
                    + "&refresh-upon-publish=" + Utils.urlEncode(userIri)
                    + "&param_appliesToResource=" + Utils.urlEncode(userIri);
        }
        // Label (with its leading icon) comes from the markup body, so no label arg here.
        ExternalLink addToOwnLink = new ExternalLink("addToOwn", addToOwnUrl);
        addToOwnLink.setVisible(showAddToOwn);
        addEntry("addToOwn", addToOwnLink);

        // The version of the view definition this display is showing. A newer one can have
        // been published since the page was built, which "refresh now" checks for below.
        final String shownViewId = viewDisplay.getView() == null ? null : viewDisplay.getView().getId();

        // Refreshes this one view where it stands. Re-rendering the whole page would work too,
        // but it takes the reader back to the top of it, away from the view they were looking
        // at — and re-runs everything else on the page for a refresh they asked of one view.
        AjaxFallbackLink<Void> refreshLink = new AjaxFallbackLink<>("refreshNow") {
            @Override
            public void onClick(Optional<AjaxRequestTarget> target) {
                // A header view has no query, and so nothing to mark outdated here — its
                // whole content comes from the view definition re-checked just below.
                if (queryRef != null) ApiCache.clearCache(queryRef, 0);
                // Bringing a view up to date is not only a matter of re-running its query:
                // the view definition itself can have been superseded since this page was
                // built, and neither the memoized resolution nor the version the page's
                // structure resolved to would notice on their own (issue #654).
                View latestView = shownViewId == null ? null : View.refreshLatestVersion(shownViewId);
                if (latestView != null && !shownViewId.equals(latestView.getId())) {
                    // A new version can change everything the display is made of — its query,
                    // its columns, its actions, its width — which is more than the piece on
                    // screen can be patched into. The version in use comes from the page's
                    // structure (the get-view-displays query resolves it server-side), so the
                    // structure is what has to be asked again: the same route the page-level
                    // "refresh now" takes, with the current structure kept on screen under a
                    // spinner until the refreshed one lands.
                    AbstractResourceWithProfile r = pageResourceId.isEmpty()
                            ? null : AbstractResourceWithProfile.get(pageResourceId);
                    if (r != null) r.forceRefresh(0);
                    setResponsePage(getPage().getClass(), getPage().getPageParameters());
                    return;
                }
                QueryResult view = findParent(QueryResult.class);
                if (view == null && target.isPresent()) {
                    // Not every view display puts results in the page. A query-form view
                    // shows a form, and the results it leads to live on the page it submits
                    // to; a header view has no query at all. Either way there is nothing
                    // here to bring up to date once the view definition has been re-checked
                    // above (and, for the form, its query marked outdated for the next
                    // submit). Re-rendering the page on top of that would repaint everything
                    // for no visible change — which is what made query-form views, alone
                    // among the display types then offering a refresh, flicker on every one.
                    return;
                }
                // A view is not always what stands in the page: while it waits for its first
                // results it is inside Wicket's lazy-loading panel, and while it is being
                // brought up to date inside a RefreshingResultPanel. Either way the wrapper
                // is the piece that has to be replaced — replacing what is inside it would
                // leave the wrapper around it and address an element the browser does not
                // have, so nothing would change on screen and the markup left behind would
                // keep linking to components that are no longer there.
                Component replaceable = view;
                while (replaceable != null && "content".equals(replaceable.getId())
                        && (replaceable.getParent() instanceof RefreshingResultPanel
                            || replaceable.getParent() instanceof AjaxLazyLoadPanel)) {
                    replaceable = replaceable.getParent();
                }
                Component rebuilt = (view == null ? null : view.rebuild(replaceable.getId()));
                if (target.isEmpty() || rebuilt == null || !replaceable.getOutputMarkupId()) {
                    // No Ajax, nothing to rebuild, or nothing in the page to replace: fall
                    // back to re-rendering the page.
                    setResponsePage(getPage().getClass(), getPage().getPageParameters());
                    return;
                }
                // The replacement has to answer to the id the browser already has, or the
                // Ajax response would update an element that is not there and leave the old
                // markup — and its links, by then removed from the page — on screen.
                rebuilt.setMarkupId(replaceable.getMarkupId());
                replaceable.replaceWith(rebuilt);
                target.get().add(rebuilt);
            }

        };
        // Offered wherever there is something to bring up to date. That used to mean a query,
        // but since issue #654 the refresh re-resolves the view definition as well, which is
        // all a query-less header view consists of — so it is offered there too.
        refreshLink.setVisible(session.getUserIri() != null && (queryRef != null || shownViewId != null));
        addEntry("refreshNow", refreshLink);

        BookmarkablePageLink<Void> viewDeclarationLink = new BookmarkablePageLink<>("viewDeclaration", ExplorePage.class,
                new PageParameters().set("id", nanopubId));
        viewDeclarationLink.add(NavigationContext.pageContextFallback());
        viewDeclarationLink.setVisible(viewDisplay.getId() != null);
        addEntry("viewDeclaration", viewDeclarationLink);
    }

}
