package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TemplateItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

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

        final QueryRef rQueryRef = new QueryRef("get-most-recent-nanopubs");
        ApiResponse rResponse = ApiCache.retrieveResponse(rQueryRef);
        if (rResponse != null) {
            add(NanopubResults.fromApiResponse("mostrecent", rResponse));
        } else {
            add(new ApiResultComponent("mostrecent", rQueryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return NanopubResults.fromApiResponse(markupId, response);
                }
            });

        }

        add(new ItemListPanel<IRI>(
                "topcreators",
                "Most Active Nanopublishers Last Month",
                new QueryRef("get-top-creators-last30d"),
                (apiResponse) -> {
                    List<IRI> users = new ArrayList<>();
                    for (ApiResponseEntry e : apiResponse.getData()) {
                        users.add(Utils.vf.createIRI(e.get("userid")));
                    }
                    return users;
                },
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new ItemListPanel<Template>(
                "getstarted-templates",
                "Suggested Templates to Start Publishing",
                new QueryRef("get-suggested-templates-to-get-started"),
                TemplateData::getTemplateList,
                (template) -> new TemplateItem("item", template)
        ));

    }

}
