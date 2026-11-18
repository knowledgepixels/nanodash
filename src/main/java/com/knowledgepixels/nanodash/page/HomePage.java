package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.LazyContentPanel;
import com.knowledgepixels.nanodash.component.ResultComponent;
import com.knowledgepixels.nanodash.component.RefreshingStructurePanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;

/**
 * The home page of Nanodash, showing the views defined for the configured home resource.
 */
public class HomePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/";

    private static final String PAGE_TITLE = "Nanodash — browse and publish nanopublications";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * The home page shows the configured home resource, so it is that resource's page
     * and acts as its navigation context: links from here (including the post-publish
     * message link) carry the home resource along, even though no {@code context}
     * parameter is in the URL.
     */
    @Override
    public String getContextId() {
        return NanodashPreferences.get().getHomeResource();
    }

    /**
     * Constructor for the home page.
     *
     * @param parameters the page parameters
     */
    public HomePage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this));
        add(new Label("pagetitle", PAGE_TITLE));
        add(hiddenWhenEmpty(new Label("warning", versionWarningHtml()).setEscapeModelStrings(false)));
        add(hiddenWhenEmpty(new Label("notice", gettingStartedNoticeHtml()).setEscapeModelStrings(false)));

        setOutputMarkupId(true);

        final String homeResourceId = NanodashPreferences.get().getHomeResource();
        // The last resource this id ever resolved to, not merely the one the newest
        // answer carries: the home page is the one page that must survive a query answer
        // coming back short of it (issue #623).
        final MaintainedResource homeResource = MaintainedResourceRepository.get().findLastKnownById(homeResourceId);

        // Rendered only for a genuine misconfiguration (see below): the resource
        // repository knows maintained resources, and the configured id is none of them.
        final String notFoundHtml = "<div class=\"row-section\"><div class=\"col-12\"><p class=\"negative\">" +
                "Configured home resource <code>" + Strings.escapeMarkup(homeResourceId) + "</code> could not be found. " +
                "Set the <code>NANODASH_HOME_RESOURCE</code> environment variable to a valid maintained-resource IRI." +
                "</p></div></div>";

        if (homeResource == null && MaintainedResourceRepository.get().isAbsent(homeResourceId)) {
            // The repository holds resources and this id is none of them, nor was it ever:
            // a real misconfiguration, not a cold cache or an answer that came back short.
            add(new Label("views", notFoundHtml).setEscapeModelStrings(false));
            return;
        }

        // Resolve the repository singleton inside the anonymous classes rather than
        // capturing it: MaintainedResourceRepository is not Serializable, and a
        // captured reference makes the whole page fail to serialize to the page store.
        final IModel<MaintainedResource> homeResourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                return MaintainedResourceRepository.get().findLastKnownById(homeResourceId);
            }
        };

        if (homeResource != null) {
            homeResource.triggerDataUpdate();
            if (homeResource.isDataInitialized()) {
                add(RefreshingStructurePanel.of("views", homeResource, markupId -> {
                    ViewList viewList = new ViewList(markupId, homeResourceModel);
                    return viewList;
                }));
                return;
            }
        }

        // Either the resource exists but its data isn't initialized yet, or the
        // repository has nothing for the id yet (cache refresh in flight / racing spaces
        // load / an answer carrying no resources at all). Lazy-load and poll until the
        // data resolves, rather than declaring a hard "not found" on a transient null. If
        // the repository ends up holding resources without the configured id among them,
        // the misconfig notice is shown then.
        add(new LazyContentPanel("views", markupId -> {
            MaintainedResource r = homeResourceModel.getObject();
            if (r == null) {
                return new Label(markupId, notFoundHtml).setEscapeModelStrings(false);
            }
            ViewList viewList = new ViewList(markupId, homeResourceModel);
            return viewList;
        }) {

            @Override
            protected boolean isContentReady() {
                MaintainedResource r = homeResourceModel.getObject();
                // isDataInitialized() also kicks off the (idempotent) data load.
                if (r != null) return r.isDataInitialized();
                // Resource still unresolved: keep polling, and stop (to show the
                // misconfig notice) only once the id is known to be no resource of this
                // instance rather than one the answers keep leaving out.
                return MaintainedResourceRepository.get().isAbsent(homeResourceId);
            }

            @Override
            public Component getLoadingComponent(String id) {
                return new Label(id, ResultComponent.getSectionWaitHtml()).setEscapeModelStrings(false);
            }

            @Override
            protected void onDetach() {
                homeResourceModel.detach();
                super.onDetach();
            }

        });
    }

    /**
     * Hides a label that has nothing to say, so an empty notice leaves no blank space behind.
     *
     * @param label the label to hide when its model is empty
     * @return the same label
     */
    private static Component hiddenWhenEmpty(Component label) {
        return label.setVisible(!label.getDefaultModelObjectAsString().isEmpty());
    }

    /**
     * The warning about the running version: a snapshot build, or a newer public release.
     *
     * @return the warning as HTML, or an empty string when there is nothing to warn about
     */
    private static String versionWarningHtml() {
        if (NanodashPreferences.get().isOrcidLoginMode()) return "";
        String currentVersion = WicketApplication.getThisVersion();
        String latestVersion = WicketApplication.getLatestVersion();
        if (currentVersion.endsWith("-SNAPSHOT")) {
            String warning = "You are running a temporary snapshot version of Nanodash (" + Strings.escapeMarkup(currentVersion) + ").";
            if (latestVersion != null) warning += " The latest public version is " + Strings.escapeMarkup(latestVersion) + ".";
            return warning;
        }
        if (latestVersion != null && !currentVersion.equals(latestVersion)) {
            return "There is a new version available: " + Strings.escapeMarkup(latestVersion) + ". You are currently using " + Strings.escapeMarkup(currentVersion) + ". " +
                   "Run 'update' (Unix/Mac) or 'update-under-windows.bat' (Windows) to update to the latest version, or manually download it " +
                   "<a href=\"" + WicketApplication.LATEST_RELEASE_URL + "\">here</a>.";
        }
        return "";
    }

    /**
     * The next step a visitor has to take before they can publish: log in, or complete their profile.
     *
     * @return the notice as HTML, or an empty string when the visitor is ready to publish
     */
    private static String gettingStartedNoticeHtml() {
        NanodashSession session = NanodashSession.get();
        if (NanodashPreferences.get().isReadOnlyMode()) {
            return "This is a read-only instance, so you cannot publish new nanopublications here.";
        }
        if (session.isProfileComplete()) return "";
        if (NanodashPreferences.get().isOrcidLoginMode() && session.getUserIri() == null) {
            return callToActionHtml(OrcidLoginPage.getOrcidLoginUrl("."), "Log in with ORCID",
                    "to see your own nanopublications and publish new ones.");
        }
        return callToActionHtml(profileUrl(session.getUserIri()), "Complete your profile",
                "before you publish your first nanopublication.");
    }

    /**
     * The page where a visitor completes their profile: their own About tab once they have a
     * user IRI, the profile page otherwise.
     *
     * @param userIri the visitor's user IRI, or {@code null} when it is not resolved yet
     * @return the URL of the page to complete the profile on
     */
    private static String profileUrl(IRI userIri) {
        if (userIri == null) return ProfilePage.MOUNT_PATH;
        return UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(userIri) + "&tab=about";
    }

    /**
     * A button for the visitor's next step, followed by why they should take it.
     *
     * @param url         where the button leads
     * @param buttonLabel the text on the button
     * @param reason      what the step lets the visitor do
     * @return the call to action as HTML
     */
    private static String callToActionHtml(String url, String buttonLabel, String reason) {
        return "<a class=\"button\" href=\"" + Strings.escapeMarkup(url) + "\">" + buttonLabel + "</a> " +
               "<span class=\"home-hero-notice-reason\">" + reason + "</span>";
    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
