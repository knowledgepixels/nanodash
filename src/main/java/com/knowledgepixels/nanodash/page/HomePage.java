package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
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
            add(new Label("text", "Before you can start, you first need to <a href=\"" + ProfilePage.MOUNT_PATH + "\">complete your profile</a>.").setEscapeModelStrings(false));
        }

        setOutputMarkupId(true);

        String homeResourceId = NanodashPreferences.get().getHomeResource();
        MaintainedResource homeResource = MaintainedResourceRepository.get().findById(homeResourceId);
        if (homeResource == null) {
            String msg = "Configured home resource <code>" + Strings.escapeMarkup(homeResourceId) + "</code> could not be found. " +
                    "Set the <code>NANODASH_HOME_RESOURCE</code> environment variable to a valid maintained-resource IRI.";
            add(new Label("views", "<div class=\"row-section\"><div class=\"col-12\"><p class=\"negative\">" + msg + "</p></div></div>").setEscapeModelStrings(false));
            return;
        }
        homeResource.triggerDataUpdate();

        if (homeResource.isDataInitialized()) {
            ViewList viewList = new ViewList("views", homeResource);
            viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", this));
            add(viewList);
        } else {
            add(new AjaxLazyLoadPanel<Component>("views") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    ViewList viewList = new ViewList(markupId, homeResource);
                    viewList.setPageFooter(new Fragment("page-footer", "homeFooterFragment", HomePage.this));
                    return viewList;
                }

                @Override
                protected boolean isContentReady() {
                    return homeResource.isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                }

            });
        }
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
