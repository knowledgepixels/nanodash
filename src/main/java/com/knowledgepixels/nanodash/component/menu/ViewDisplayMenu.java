package com.knowledgepixels.nanodash.component.menu;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

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
     */
    public ViewDisplayMenu(String id, ViewDisplay viewDisplay, QueryRef queryRef, AbstractResourceWithProfile pageResource) {
        super(id);

        PageParameters showQueryParams = new PageParameters().set("id", queryRef.getQueryId());
        for (var entry : queryRef.getParams().entries()) {
            showQueryParams.add("queryparam_" + entry.getKey(), entry.getValue());
        }
        addEntry("showQuery", new BookmarkablePageLink<Void>("showQuery", QueryPage.class, showQueryParams));

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

        // Determine supersede vs derive based on whether this user's pubkey matches the nanopub's
        String nanopubPubkey = NanopubElement.get(viewDisplay.getNanopub()).getPubkey();
        String sessionPubkey = session.getPubkeyString();
        String adjustParam = (nanopubPubkey != null && nanopubPubkey.equals(sessionPubkey))
                ? "supersede" : "derive";

        IRI templateId = TemplateData.get().getTemplateId(viewDisplay.getNanopub());
        String templateUri = templateId != null ? templateId.stringValue()
                : "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
        String adjustUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(templateUri)
                + "&" + adjustParam + "=" + Utils.urlEncode(nanopubId.stringValue())
                + "&template-version=latest"
                + "&context=" + Utils.urlEncode(pageResource.getId());
        ExternalLink adjustLink = new ExternalLink("adjust", adjustUrl, "edit view display");
        adjustLink.setVisible(showAdjust);
        addEntry("adjust", adjustLink);

        BookmarkablePageLink<Void> deactivateLink = new BookmarkablePageLink<>("deactivate", PublishPage.class,
                new PageParameters()
                        .set("template", "https://w3id.org/np/RAZ47_4JquvEXk30HYnVeSgFRcQqHtpdibcfBOeqHI2j4")
                        .set("template-version", "latest")
                        .set("param_resource", pageResource.getId())
                        .set("param_view", viewDisplay.getView().getId())
                        .set("context", pageResource.getId()));
        deactivateLink.setVisible(showAdjust);
        addEntry("deactivate", deactivateLink);

        addEntry("viewDeclaration", new BookmarkablePageLink<Void>("viewDeclaration", ExplorePage.class,
                new PageParameters().set("id", nanopubId)));
    }

}
