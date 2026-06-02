package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

/**
 * Standalone "About" page for a user, showing a public read-only view of their
 * profile (introductions, keys, license), their configured views, and
 * references. Reachable by direct URL only; not yet linked from the main
 * {@link UserPage}.
 * <p>
 * Space memberships ("assigned roles") are not listed yet: there is no ready
 * API to enumerate the spaces a user belongs to. See issue #478 for the
 * planned follow-up.
 */
public class AboutUserPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/userabout";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the AboutUserPage.
     *
     * @param parameters the page parameters, must include "id" with the user IRI
     */
    public AboutUserPage(final PageParameters parameters) {
        super(parameters);

        if (parameters.get("id").isEmpty()) {
            throw new IllegalArgumentException("No user id given");
        }
        final String userIriString = parameters.get("id").toString();
        final IRI userIri = Utils.vf.createIRI(userIriString);

        add(new TitleBar("titlebar", this, "users"));

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        IRI profilePictureIri = User.getProfilePicture(userIri);
        if (profilePictureIri != null) {
            add(new ExternalImage("userIcon", profilePictureIri));
        } else if (IndividualAgent.isSoftware(userIri)) {
            add(new Image("userIcon", new ContextRelativeResourceReference("images/bot-icon.svg", false)));
        } else {
            add(new Image("userIcon", new ContextRelativeResourceReference("images/user-icon.svg", false)));
        }

        final String displayName = User.getShortDisplayName(userIri);
        add(new Label("pagetitle", displayName + " (about) | nanodash"));
        add(new Label("username", displayName));
        add(new ExternalLinkWithActionsPanel("fullid", Model.of(userIriString), Model.of(displayName)));
        add(new DownloadRdfLinks("download-rdf", "user", userIriString));

        // Public read-only profile
        add(new PublicProfilePanel("profile", userIri));

        // Assigned view displays (a listing of the configured view displays,
        // not the rendered views themselves).
        View vdView = View.get(AboutSpacePage.VIEW_DISPLAYS_VIEW);
        QueryRef vdQueryRef = new QueryRef(vdView.getQuery().getQueryId(), "resource", userIriString);
        add(QueryResultTableBuilder.create("viewdisplays", vdQueryRef, new ViewDisplay(vdView)).build());

        // References
        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        QueryRef refQueryRef = new QueryRef(refView.getQuery().getQueryId(), "ref", userIriString);
        add(QueryResultTableBuilder.create("references", refQueryRef, new ViewDisplay(refView)).build());
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
