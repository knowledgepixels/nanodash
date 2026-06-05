package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "About" tab body for a user: their introduction nanopublications, a public
 * read-only view of their profile (default license, profile picture), assigned
 * presets, and the listing of their configured view displays (issue #302).
 */
public class AboutUserPanel extends Panel {

    /**
     * Read-only view that lists a user's introduction nanopublications (shown to
     * other users; the current user gets the editable {@link ProfileIntroItem}
     * companion instead).
     */
    public static final String INTRODUCTIONS_VIEW = "https://w3id.org/np/RAElH_0Za_T9H_GeyixS35lGwOAL_OD3r4XYs__BF6tl4/introductions-view";

    /**
     * View showing a user's basic profile properties (default license and
     * profile picture), one per row.
     */
    public static final String PROFILE_VIEW = "https://w3id.org/np/RAtTP_qhEqsz2V8YoR6MfZ_j7gwcJ9SE2WvzjXLiagb9Q/profile-view";

    /**
     * @param id            the Wicket markup id
     * @param userIriString the user IRI
     */
    public AboutUserPanel(String id, String userIriString) {
        super(id);

        // Account/identity controls (logout, local-mode ORCID form) only on the
        // current user's own About page.
        NanodashSession session = NanodashSession.get();
        boolean ownPage = session.getUserIri() != null && session.getUserIri().stringValue().equals(userIriString);
        if (ownPage) {
            add(new ProfileAccountPanel("account", userIriString));
        } else {
            add(new EmptyPanel("account").setVisible(false));
        }

        // Introductions: the current user (with a local key) gets the editable
        // companion with the full intro workflow; everyone else gets the
        // read-only view. The companion is styled to match the view tables.
        if (ownPage && session.getKeyPair() != null) {
            ProfileIntroItem introItem = new ProfileIntroItem("introductions");
            introItem.add(new AttributeAppender("class", " col-12"));
            add(introItem);
        } else {
            View introView = View.get(INTRODUCTIONS_VIEW);
            add(QueryResultTableBuilder.create("introductions", new QueryRef(introView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(introView))
                    .resourceWithProfile(IndividualAgent.get(userIriString))
                    .id(userIriString)
                    .contextId(userIriString)
                    .build());
        }

        // Profile view with result actions to update the profile image/license;
        // needs the resourceWithProfile/id/contextId for the action links.
        View profileView = View.get(PROFILE_VIEW);
        add(QueryResultTableBuilder.create("profile", new QueryRef(profileView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(profileView))
                .resourceWithProfile(IndividualAgent.get(userIriString))
                .id(userIriString)
                .contextId(userIriString)
                .build());

        View presetsView = View.get(AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", userIriString), new ViewDisplay(presetsView)).id(userIriString).contextId(userIriString).build());

        View vdView = View.get(AboutSpacePanel.VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), "resource", userIriString), new ViewDisplay(vdView)).id(userIriString).contextId(userIriString).build());
    }

}
