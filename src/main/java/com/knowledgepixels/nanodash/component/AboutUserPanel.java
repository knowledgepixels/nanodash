package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "About" tab body for a user: their introduction nanopublications, a public
 * read-only view of their profile (default license, profile picture), assigned
 * presets, and the listing of their configured view displays (issue #302).
 */
public class AboutUserPanel extends Panel {

    /**
     * View that lists a user's introduction nanopublications.
     */
    public static final String INTRODUCTIONS_VIEW = "https://w3id.org/np/RABbGzAaESdtU2ZG4Ar5fnwcUiV4kpFMp_k5p-6wOnc_s/introductions-view";

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

        View introView = View.get(INTRODUCTIONS_VIEW);
        add(QueryResultTableBuilder.create("introductions", new QueryRef(introView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(introView)).build());

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
