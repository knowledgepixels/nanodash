package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
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
     * View that lists a user's introduction nanopublications. Its per-row
     * retract/derive actions are driven by the viewer's session-bound magic query
     * params (LOCALPUBKEY/SITEURL), so the owner gets the editable actions and
     * everyone else gets the read-only table — no custom companion needed.
     */
    public static final String INTRODUCTIONS_VIEW = "https://w3id.org/np/RAmdDJAKs1gKPPdo23t6lMUXC6mlzLjRZyjDCcNEMx7Tw/introductions-view";

    /**
     * Owner-gated view (via the CURRENTUSER magic param) listing recommended
     * introduction actions — currently "Create Introduction" (when the local key is
     * in no introduction) and "Get approved" (when the single local introduction is
     * not yet approved). Returns nothing for non-owners or when nothing applies.
     */
    public static final String RECOMMENDATIONS_VIEW = "https://w3id.org/np/RAhcXD0pxuru6l3PR9n_zcEleJMH-M-H953RLKrgu-nbc/intro-recommendations-view";

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

        // The owner's account/identity controls (logout, local-mode ORCID form,
        // signing key) live in the page header stripe (see UserPage), not here.
        NanodashSession session = NanodashSession.get();
        boolean ownPage = session.getUserIri() != null && session.getUserIri().stringValue().equals(userIriString);

        // Recommended actions: shown only on the owner's own About page when they
        // have a local key. The query is owner-gated (CURRENTUSER) and may return
        // nothing; the <wicket:enclosure> in the markup drops the whole section for
        // viewers who don't qualify (non-owners / no local key).
        if (ownPage && session.getKeyPair() != null) {
            View recView = View.get(RECOMMENDATIONS_VIEW);
            add(QueryResultListBuilder.create("recommendations", new QueryRef(recView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(recView))
                    .resourceWithProfile(IndividualAgent.get(userIriString))
                    .id(userIriString)
                    .contextId(userIriString)
                    .build());
        } else {
            add(new EmptyPanel("recommendations").setVisible(false));
        }

        // Introductions: a proper view for everyone. The owner's session-bound magic
        // params drive the per-row retract/derive action buttons; non-owners get the
        // read-only table. (Formerly the ProfileIntroItem companion for the owner.)
        View introView = View.get(INTRODUCTIONS_VIEW);
        add(QueryResultTableBuilder.create("introductions", new QueryRef(introView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(introView))
                .resourceWithProfile(IndividualAgent.get(userIriString))
                .id(userIriString)
                .contextId(userIriString)
                .build());

        // Profile view with result actions to update the profile image/license;
        // needs the resourceWithProfile/id/contextId for the action links.
        View profileView = View.get(PROFILE_VIEW);
        add(QueryResultTableBuilder.create("profile", new QueryRef(profileView.getQuery().getQueryId(), "user", userIriString), new ViewDisplay(profileView))
                .resourceWithProfile(IndividualAgent.get(userIriString))
                .id(userIriString)
                .contextId(userIriString)
                .build());

        View presetsView = View.get(AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", userIriString), new ViewDisplay(presetsView)).resourceWithProfile(IndividualAgent.get(userIriString)).id(userIriString).contextId(userIriString).build());

        View vdView = View.get(AboutSpacePanel.VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), "resource", userIriString), new ViewDisplay(vdView)).resourceWithProfile(IndividualAgent.get(userIriString)).id(userIriString).contextId(userIriString).build());
    }

}
