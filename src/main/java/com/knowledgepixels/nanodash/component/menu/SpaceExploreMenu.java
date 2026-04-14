package com.knowledgepixels.nanodash.component.menu;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.MaintainedResourcePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

public class SpaceExploreMenu extends BaseDisplayMenu {

    public SpaceExploreMenu(String id, String exploreUri, String exploreLabel, IRI sourceUri, Space space) {
        super(id);

        addEntry("explore", new BookmarkablePageLink<Void>("explore", ExplorePage.class,
                new PageParameters().set("id", exploreUri).set("label", exploreLabel)));
        addEntry("viewDeclaration", new BookmarkablePageLink<Void>("viewDeclaration", ExplorePage.class,
                new PageParameters().set("id", sourceUri)));
        addEntry("showViewDisplayQuery", new BookmarkablePageLink<Void>("showViewDisplayQuery", QueryPage.class,
                new PageParameters()
                        .set("id", QueryApiAccess.GET_VIEW_DISPLAYS)
                        .add("queryparam_resource", exploreUri)));

        boolean isAdmin = SpaceMemberRole.isCurrentUserAdmin(space);

        String spacePrefix = space.getId().replaceFirst("https://w3id.org/spaces/", "") + "/<SET-SUFFIX>";

        BookmarkablePageLink<Void> addResource = new BookmarkablePageLink<>("addMaintainedResource", PublishPage.class,
                new PageParameters()
                        .set("template", "https://w3id.org/np/RA25VaVFxSOgKEuZ70gFINn-N3QV4Pf62-IMK_SWkg-c8")
                        .set("param_space", space.getId())
                        .set("context", space.getId())
                        .set("refresh-upon-publish", space.getId())
                        .set("postpub-redirect-url", MaintainedResourcePage.MOUNT_PATH)
                        .set("template-version", "latest"));
        addResource.setVisible(isAdmin);
        addEntry("addMaintainedResource", addResource);

        BookmarkablePageLink<Void> addTimeLimited = new BookmarkablePageLink<>("addTimeLimitedSpace", PublishPage.class,
                new PageParameters()
                        .set("template", "https://w3id.org/np/RAaE7NP9RNIx03AHZxanFMdtUuaTfe50ns5tHhpEVloQ4")
                        .set("param_space", spacePrefix)
                        .set("context", space.getId())
                        .set("refresh-upon-publish", space.getId())
                        .set("postpub-redirect-url", SpacePage.MOUNT_PATH)
                        .set("template-version", "latest"));
        addTimeLimited.setVisible(isAdmin);
        addEntry("addTimeLimitedSpace", addTimeLimited);

        BookmarkablePageLink<Void> addOpenEnded = new BookmarkablePageLink<>("addOpenEndedSpace", PublishPage.class,
                new PageParameters()
                        .set("template", "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ")
                        .set("param_space", spacePrefix)
                        .set("context", space.getId())
                        .set("refresh-upon-publish", space.getId())
                        .set("postpub-redirect-url", SpacePage.MOUNT_PATH)
                        .set("template-version", "latest"));
        addOpenEnded.setVisible(isAdmin);
        addEntry("addOpenEndedSpace", addOpenEnded);
    }

}
