package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.page.ExplorePage;

/**
 * Panel to display an error message when the nanopublication that is to be
 * superseded or overridden is not the latest version anymore. It is shown
 * instead of the publish form, so the user doesn't fill in a form that cannot
 * be published anyway.
 */
public class OutdatedSourceErrorItem extends Panel {

    /**
     * Constructor for OutdatedSourceErrorItem.
     *
     * @param id the component id
     * @param pageParams the page parameters that ask to supersede or override
     * an outdated nanopublication
     * @param publishPageClass the class of the publish page, used to link to
     * the same action on the latest version
     */
    public OutdatedSourceErrorItem(String id, final PageParameters pageParams, Class<? extends WebPage> publishPageClass) {
        super(id);
        String sourceNpId = PublishForm.getSupersededOrOverriddenNanopubId(pageParams);
        String latestNpId = QueryApiAccess.getLatestVersionId(sourceNpId);
        add(new BookmarkablePageLink<Void>("latest-link", ExplorePage.class, new PageParameters().set("id", latestNpId)));
        add(new BookmarkablePageLink<Void>("latest-action-link", publishPageClass, PublishForm.withSourceNanopub(pageParams, latestNpId)));
    }

}
