package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.ResultComponent;
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
            // A logged-in user completes their profile on their own About page; only fall
            // back to the profile page when there is no resolved user IRI yet.
            String profileUrl = session.getUserIri() != null
                    ? UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(session.getUserIri()) + "&tab=about"
                    : ProfilePage.MOUNT_PATH;
            add(new Label("text", "Before you can start, you first need to <a href=\"" + profileUrl + "\">complete your profile</a>.").setEscapeModelStrings(false));
        }

        setOutputMarkupId(true);

        final String homeResourceId = NanodashPreferences.get().getHomeResource();
        final MaintainedResource homeResource = MaintainedResourceRepository.get().findById(homeResourceId);

        // Rendered only for a genuine misconfiguration (see below): the resource
        // repository is warm but the configured id is not a known maintained resource.
        final String notFoundHtml = "<div class=\"row-section\"><div class=\"col-12\"><p class=\"negative\">" +
                "Configured home resource <code>" + Strings.escapeMarkup(homeResourceId) + "</code> could not be found. " +
                "Set the <code>NANODASH_HOME_RESOURCE</code> environment variable to a valid maintained-resource IRI." +
                "</p></div></div>";

        if (homeResource == null && MaintainedResourceRepository.get().isReady()) {
            // The repository has a full snapshot, yet the configured id isn't in it:
            // a real misconfiguration, not a cold-cache race.
            add(new Label("views", notFoundHtml).setEscapeModelStrings(false));
            return;
        }

        if (homeResource != null) {
            homeResource.triggerDataUpdate();
            if (homeResource.isDataInitialized()) {
                ViewList viewList = new ViewList("views", homeResource);
                viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", this));
                add(viewList);
                return;
            }
        }

        // Either the resource exists but its data isn't initialized yet, or the
        // repository itself is still cold so findById is transiently null (cache
        // refresh in flight / racing spaces load). Lazy-load and poll until the data
        // resolves, rather than declaring a hard "not found" on a transient null. If
        // the repository warms up without the configured id, the misconfig notice is
        // shown then.
        // Resolve the repository singleton inside the anonymous classes rather than
        // capturing it: MaintainedResourceRepository is not Serializable, and a
        // captured reference makes the whole page fail to serialize to the page store.
        final IModel<MaintainedResource> homeResourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                return MaintainedResourceRepository.get().findById(homeResourceId);
            }
        };

        add(new AjaxLazyLoadPanel<Component>("views") {

            @Override
            public Component getLazyLoadComponent(String markupId) {
                MaintainedResource r = homeResourceModel.getObject();
                if (r == null) {
                    return new Label(markupId, notFoundHtml).setEscapeModelStrings(false);
                }
                ViewList viewList = new ViewList(markupId, r);
                viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", HomePage.this));
                return viewList;
            }

            @Override
            protected boolean isContentReady() {
                MaintainedResource r = homeResourceModel.getObject();
                // isDataInitialized() also kicks off the (idempotent) data load.
                if (r != null) return r.isDataInitialized();
                // Resource still unresolved: keep polling while the repository is cold,
                // and stop (to show the misconfig notice) only once it is warm.
                return MaintainedResourceRepository.get().isReady();
            }

            @Override
            public Component getLoadingComponent(String id) {
                return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
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
