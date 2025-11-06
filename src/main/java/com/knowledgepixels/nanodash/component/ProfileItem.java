package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

/**
 * A panel that displays a link to the user's profile.
 */
public class ProfileItem extends Panel {

    /**
     * Constructor for the ProfileItem panel.
     *
     * @param id   the component id
     * @param page a {@link com.knowledgepixels.nanodash.page.NanodashPage} object
     */
    public ProfileItem(String id, NanodashPage page) {
        super(id);
        NanodashSession session = NanodashSession.get();
        NanodashPreferences prefs = NanodashPreferences.get();
        IRI userId = session.getUserIri();
        if (prefs.isOrcidLoginMode() && userId == null) {
            String redirectMountPath = ProfilePage.MOUNT_PATH;
            PageParameters redirectPageParams = new PageParameters();
            if (page != null) {
                redirectMountPath = ((NanodashPage) page).getMountPath();
                redirectPageParams = ((NanodashPage) page).getPageParameters();
            }
            ExternalLink l = new ExternalLink("profilelink", OrcidLoginPage.getOrcidLoginUrl(redirectMountPath, redirectPageParams));
            l.add(new Label("profiletext", "Login with ORCID"));
            add(l);
        } else if (prefs.isReadOnlyMode()) {
            BookmarkablePageLink<HomePage> l = new BookmarkablePageLink<>("profilelink", HomePage.class);
            l.add(new Label("profiletext", ""));
            add(l);
        } else {
            if (userId != null) {
                BookmarkablePageLink<ProfilePage> l = new BookmarkablePageLink<ProfilePage>("profilelink", UserPage.class, new PageParameters().set("id", userId.stringValue()));
                l.add(new Label("profiletext", User.getShortDisplayName(userId)));
                add(l);
            } else {
                BookmarkablePageLink<ProfilePage> l = new BookmarkablePageLink<ProfilePage>("profilelink", ProfilePage.class);
                l.add(new Label("profiletext", "incomplete profile"));
                add(l);
            }
        }
    }

}
