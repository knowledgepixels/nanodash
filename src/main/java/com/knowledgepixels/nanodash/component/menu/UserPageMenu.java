package com.knowledgepixels.nanodash.component.menu;

import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.ListPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class UserPageMenu extends BaseDisplayMenu {

    public UserPageMenu(String id, String userIri, String displayName) {
        super(id);

        addEntry("explore", new BookmarkablePageLink<Void>("explore", ExplorePage.class,
                new PageParameters().set("id", userIri).set("label", displayName)));
        addEntry("showchannel", new BookmarkablePageLink<Void>("showchannel", ListPage.class,
                new PageParameters().add("userid", userIri)));
    }

}
