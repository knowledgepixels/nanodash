package com.knowledgepixels.nanodash.component.menu;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.GuidedChoiceItem;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.template.TemplateData;
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
     * @param queryRef     the query reference used by this view display
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

        PageParameters showQueryParams = new PageParameters().set("id", queryRef.getQueryId());
        for (var entry : queryRef.getParams().entries()) {
            showQueryParams.add("queryparam_" + entry.getKey(), entry.getValue());
        }
        addEntry("showQuery", new BookmarkablePageLink<Void>("showQuery", QueryPage.class, showQueryParams));

        addEntry("showView", new BookmarkablePageLink<Void>("showView", ExplorePage.class,
                new PageParameters().set("id", viewDisplay.getView().getNanopub().getUri())));

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

        Link<Void> refreshLink = new Link<>("refreshNow") {
            @Override
            public void onClick() {
                ApiCache.clearCache(queryRef, 0);
                setResponsePage(getPage().getClass(), getPage().getPageParameters());
            }
        };
        refreshLink.setVisible(session.getUserIri() != null);
        addEntry("refreshNow", refreshLink);

        BookmarkablePageLink<Void> viewDeclarationLink = new BookmarkablePageLink<>("viewDeclaration", ExplorePage.class,
                new PageParameters().set("id", nanopubId));
        viewDeclarationLink.setVisible(viewDisplay.getId() != null);
        addEntry("viewDeclaration", viewDeclarationLink);
    }

}
