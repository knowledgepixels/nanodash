package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

/**
 * The home page of Nanodash, which shows the most recent nanopublications
 * and the latest accepted nanopublications.
 */
public class HomePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the home page.
     *
     * @param parameters the page parameters
     */
    public HomePage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));
        final NanodashSession session = NanodashSession.get();
        String v = WicketApplication.getThisVersion();
        String lv = WicketApplication.getLatestVersion();
        if (NanodashPreferences.get().isOrcidLoginMode()) {
            add(new Label("warning", ""));
        } else if (v.endsWith("-SNAPSHOT")) {
            add(new Label("warning", "You are running a temporary snapshot version of Nanodash (" + v + "). The latest public version is " + lv + "."));
        } else if (lv != null && !v.equals(lv)) {
            add(new Label("warning", "There is a new version available: " + lv + ". You are currently using " + v + ". " +
                                     "Run 'update' (Unix/Mac) or 'update-under-windows.bat' (Windows) to update to the latest version, or manually download it " +
                                     "<a href=\"" + WicketApplication.LATEST_RELEASE_URL + "\">here</a>.").setEscapeModelStrings(false));
        } else {
            add(new Label("warning", ""));
        }
        if (NanodashPreferences.get().isReadOnlyMode()) {
            add(new Label("text", "This is a read-only instance, so you cannot publish new nanopublications here."));
        } else if (NanodashSession.get().isProfileComplete()) {
            add(new Label("text", ""));
        } else if (NanodashPreferences.get().isOrcidLoginMode() && session.getUserIri() == null) {
            String loginUrl = OrcidLoginPage.getOrcidLoginUrl(".");
            add(new Label("text", "In order to see your own nanopublications and publish new ones, <a href=\"" + loginUrl + "\">login to ORCID</a> first.").setEscapeModelStrings(false));
        } else {
            add(new Label("text", "Before you can start, you first need to <a href=\"" + ProfilePage.MOUNT_PATH + "\">complete your profile</a>.").setEscapeModelStrings(false));
        }

        setOutputMarkupId(true);

        View mostRecentNanopubsView = View.get("https://w3id.org/np/RA85WirEeiXnxKdoL5IJMgnz9J5KcQLivapXLzTrupT6k/most-recent-nanopubs");
        QueryRef rQueryRef = new QueryRef(mostRecentNanopubsView.getQuery().getQueryId());
        add(QueryResultNanopubSetBuilder.create("mostrecent", rQueryRef, new ViewDisplay(mostRecentNanopubsView))
                .setItemsPerPage(5)
                .build()
                .add(AttributeModifier.remove("class"))
        );

        View upcomingEventsView = View.get("https://w3id.org/np/RAq5EwXCcCUsBEc7bMUgrT5oeLvX7khfqhA4hKzCjjBwk/upcoming-events-view");
        QueryRef eQueryRef = new QueryRef(upcomingEventsView.getQuery().getQueryId());
        add(QueryResultTableBuilder.create("upcomingevents", eQueryRef, new ViewDisplay(upcomingEventsView))
                .build()
                .add(AttributeModifier.remove("class"))
        );

        View topCreatorsView = View.get("https://w3id.org/np/RACcywnbkn6OAd_6E25qZL9-vdO-UwmpO1vXVWzNWJYLo/top-creators-last-30days");
        QueryRef cQueryRef = new QueryRef(topCreatorsView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("topCreators", cQueryRef, new ViewDisplay(topCreatorsView))
                .build()
                .add(AttributeModifier.remove("class"))
        );

        View getStartedView = View.get("https://w3id.org/np/RAeFTjDGTQ-bdulJy4tUlWzRlK8EucXFCxqLrb7Qj35SM/suggested-templates-get-started");
        QueryRef gQueryRef = new QueryRef(getStartedView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("getStartedTemplates", gQueryRef, new ViewDisplay(getStartedView))
                .build()
                .add(AttributeModifier.remove("class"))
        );

    }

}
