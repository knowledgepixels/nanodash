package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.LazyContentPanel;
import com.knowledgepixels.nanodash.component.ResultComponent;
import com.knowledgepixels.nanodash.component.PageTitleMenu;
import com.knowledgepixels.nanodash.component.RefreshingStructurePanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;
import org.apache.wicket.markup.html.panel.Fragment;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;

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
        final NanodashSession session = NanodashSession.get();
        String v = WicketApplication.getThisVersion();
        String lv = WicketApplication.getLatestVersion();
        if (NanodashPreferences.get().isOrcidLoginMode()) {
            add(new Label("warning", ""));
        } else if (v.endsWith("-SNAPSHOT")) {
            // The latest public version is not always known — GitHub may not have answered yet,
            // or at all (issue #686) — and "the latest public version is null" says nothing.
            String warning = "You are running a temporary snapshot version of Nanodash (" + v + ").";
            if (lv != null) warning += " The latest public version is " + lv + ".";
            add(new Label("warning", warning));
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
            // A logged-in user completes their profile on their own About page; only fall
            // back to the profile page when there is no resolved user IRI yet.
            String profileUrl = session.getUserIri() != null
                    ? UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(session.getUserIri()) + "&tab=about"
                    : ProfilePage.MOUNT_PATH;
            add(new Label("text", "Before you can start, you first need to <a href=\"" + profileUrl + "\">complete your profile</a>.").setEscapeModelStrings(false));
        }

        setOutputMarkupId(true);

        final String homeResourceId = NanodashPreferences.get().getHomeResource();
        // The last resource this id ever resolved to, not merely the one the newest
        // answer carries: the home page is the one page that must survive a query answer
        // coming back short of it (issue #623).
        final MaintainedResource homeResource = MaintainedResourceRepository.get().findLastKnownById(homeResourceId);

        // Added before the branching below, which returns early on several paths.
        add(PageTitleMenu.forResource("titlemenu", homeResource));

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

        if (homeResource != null) {
            homeResource.triggerDataUpdate();
            if (homeResource.isDataInitialized()) {
                add(RefreshingStructurePanel.of("views", homeResource, markupId -> {
                    ViewList viewList = new ViewList(markupId, homeResource);
                    viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", HomePage.this));
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
        // Resolve the repository singleton inside the anonymous classes rather than
        // capturing it: MaintainedResourceRepository is not Serializable, and a
        // captured reference makes the whole page fail to serialize to the page store.
        final IModel<MaintainedResource> homeResourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                return MaintainedResourceRepository.get().findLastKnownById(homeResourceId);
            }
        };

        add(new LazyContentPanel("views", markupId -> {
            MaintainedResource r = homeResourceModel.getObject();
            if (r == null) {
                return new Label(markupId, notFoundHtml).setEscapeModelStrings(false);
            }
            ViewList viewList = new ViewList(markupId, r);
            viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", HomePage.this));
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
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
