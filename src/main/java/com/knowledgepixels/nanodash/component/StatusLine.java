package com.knowledgepixels.nanodash.component;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.net.URLEncoder;
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
        ApiResultComponent c = new ApiResultComponent("statusline", "get-newer-versions-of-np", "np", npId) {

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
    public StatusLine(String markupId, String npId, ApiResponse response) {
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
        String text = null;
        // TODO Improve HTML/link generation below (do it with Wicket Java code):
        if (latest.isEmpty() && retractions.isEmpty()) {
            text = "<em>This nanopublication doesn't seem to be properly published (yet). This can take a minute or two for new nanopublications.</em>";
        } else if (latest.size() == 1) {
            String l = latest.getFirst();
            if (l.equals(npId)) {
                text = "This is the latest version.";
            } else {
                text = "This nanopublication has a <strong>newer version</strong>: " + getLink(l);
            }
        } else if (latest.size() > 1) {
            text = "This nanopublication has <strong>newer versions</strong>:";
            for (String l : latest) {
                text += " " + getLink(l);
            }
        } else {
            text = "This nanopublication has been <strong>retracted</strong>:";
            for (String r : retractions) {
                text += " " + getLink(r);
            }
        }
        add(new Label("statusline", text).setEscapeModelStrings(false));
    }

    private static String getLink(String npId) {
        String shortLabel = TrustyUriUtils.getArtifactCode(npId).substring(0, 10);
        String encodedLink = URLEncoder.encode(npId, Charsets.UTF_8);
        return "<a href=\"/explore?id=" + encodedLink + "\">" + shortLabel + "</a>";
    }

}
