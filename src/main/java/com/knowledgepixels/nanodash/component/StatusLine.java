package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.page.ExplorePage;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * A component that displays the status of a nanopublication.
 */
public class StatusLine extends Panel {

    /**
     * Creates a new StatusLine component.
     *
     * @param markupId the Wicket markup ID for this component
     * @param npId     the nanopublication ID to check for newer versions
     * @return a new StatusLine component
     */
    public static Component createComponent(String markupId, String npId) {
        // TODO Use the query cache here but with quicker refresh interval?
        ApiResultComponent c = new ApiResultComponent(markupId, new QueryRef(QueryApiAccess.GET_NEWER_VERSIONS_OF_NP, "np", npId)) {

            @Override
            public Component getApiResultComponent(String markupId, ApiResponse response) {
                return new StatusLine(markupId, npId, response);
            }

        };
        c.setWaitComponentHtml("<h4>Status</h4><div>" + ApiResultComponent.getWaitIconHtml() + "</div>");
        return c;
    }

    /**
     * Constructs a StatusLine component with the given markup ID, nanopublication ID, and API response.
     *
     * @param markupId the Wicket markup ID for this component
     * @param npId     the nanopublication ID to check for newer versions
     * @param response the API response containing data about newer versions or retractions
     */
    StatusLine(String markupId, String npId, ApiResponse response) {
        super(markupId);
        List<String> latest = new ArrayList<>();
        List<String> retractions = new ArrayList<>();
        for (ApiResponseEntry e : response.getData()) {
            String newerVersion = e.get("newerVersion");
            String retractedBy = e.get("retractedBy");
            String supersededBy = e.get("supersededBy");
            if (retractedBy.isEmpty() && supersededBy.isEmpty()) {
                latest.add(newerVersion);
            } else if (!retractedBy.isEmpty() && supersededBy.isEmpty()) {
                retractions.add(retractedBy);
            }
        }

        String statusText;
        List<String> links = List.of();

        if (latest.isEmpty() && retractions.isEmpty()) {
            statusText = "<em>This nanopublication doesn't seem to be properly published (yet). This can take a minute or two for new nanopublications.</em>";
        } else if (!latest.isEmpty()) {
            if (latest.size() == 1 && latest.getFirst().equals(npId)) {
                statusText = "This is the latest version.";
            } else if (latest.size() == 1) {
                statusText = "This nanopublication has a <strong>newer version</strong>:";
                links = latest;
            } else {
                statusText = "This nanopublication has <strong>newer versions</strong>:";
                links = latest;
            }
        } else {
            statusText = "This nanopublication has been <strong>retracted</strong>:";
            links = retractions;
        }

        final List<String> finalLinks = links;
        //WebMarkupContainer statusContainer = new WebMarkupContainer("statusLine");
        add(new Label("statusText", statusText).setEscapeModelStrings(false));
        add(new ListView<>("linkList", finalLinks) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String id = item.getModelObject();
                String shortLabel = TrustyUriUtils.getArtifactCode(id).substring(0, 10);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("npLink",
                        ExplorePage.class,
                        new PageParameters().add("id", id));
                link.add(new Label("npLabel", shortLabel));
                item.add(link);
            }
        });
    }

}
