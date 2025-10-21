package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

/**
 * Panel to show a list of things.
 */
public class ThingListPanel extends Panel {

    /**
     * Constructor for the ThingListPanel.
     *
     * @param markupId the Wicket markup ID for this panel
     * @param mode     the mode of the panel, determining what kind of things to show
     * @param thingRef the reference of the thing to show the list for
     * @param response the API response containing the data to display
     */
    private ThingListPanel(String markupId, final Mode mode, final String thingRef, ApiResponse response) {
        super(markupId);
        // TODO Not copying the table here, which can lead to problems if at some point the same result list is sorted differently at different places:
        response.getData().sort(new Utils.ApiResponseEntrySorter("date", true));
        if (response.getData().isEmpty()) {
            setVisible(false);
        } else if (mode == Mode.TEMPLATES) {
            add(new Label("message").setVisible(false));
        } else if (response.getData().size() == 1) {
            add(new Label("message", mode.messageStart + " 1 " + mode.wordSg + ":"));
        } else if (response.getData().size() == 1000) {
            add(new Label("message", mode.messageStart + " more " + mode.wordPl + " (>999) than what can be shown here:"));
        } else {
            add(new Label("message", mode.messageStart + " " + response.getData().size() + " " + mode.wordPl + ":"));
        }
        if (mode == Mode.TEMPLATES) {
            add(new ItemListPanel<Template>(
                    "templates",
                    "Related Templates",
                    TemplateData.getTemplateList(response),
                    (template) -> new TemplateItem("item", template)
                ));
            add(new Label("things").setVisible(false));
        } else {
            add(ThingResults.fromApiResponse("things", mode.returnField, response));
            add(new Label("templates").setVisible(false));
        }
    }

    /**
     * Factory method to create a ThingListPanel component.
     *
     * @param markupId    the Wicket markup ID for the panel
     * @param mode        the mode of the panel, determining what kind of things to show
     * @param thingRef    the reference of the thing to show the list for
     * @param waitMessage the message to display while waiting for the API response
     * @return a new ThingListPanel component
     */
    public static Component createComponent(final String markupId, final Mode mode, final String thingRef, final String waitMessage) {
        QueryRef queryRef = new QueryRef(mode.queryName, mode.queryParamKey, thingRef);
        ApiResponse response = ApiCache.retrieveResponse(queryRef);
        if (response != null) {
            return new ThingListPanel(markupId, mode, thingRef, response);
        } else {
            ApiResultComponent c = new ApiResultComponent(markupId, queryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ThingListPanel(markupId, mode, thingRef, response);
                }

            };
            c.setWaitMessage(waitMessage);
            return c;
        }
    }

    /**
     * Enum representing the different modes of the ThingListPanel.
     * Each mode corresponds to a specific type of thing and defines how to query and display them.
     */
    public enum Mode {
        CLASSES("get-classes-for-thing", "thing", "class", "class", "classes", "Assigned to"),
        INSTANCES("get-instances", "class", "instance", "instance", "instances", "Has"),
        PARTS("get-parts", "thing", "part", "part", "parts", "Has"),
        TEMPLATES("get-templates-with-uri", "thing", "np", "template", "templates", "Used in"),
        DESCRIPTIONS("get-term-definitions", "term", "np", "nanopublication", "nanopublications", "Described in");

        /**
         * The name of the query to be used for this mode.
         */
        public final String queryName;
        /**
         * The parameter to be used in the query for this mode.
         */
        public final String queryParamKey;
        /**
         * The field in the API response that contains the relevant data for this mode.
         */
        public final String returnField;
        /**
         * The singular word used in messages for this mode.
         */
        public final String wordSg;
        /**
         * The plural word used in messages for this mode.
         */
        public final String wordPl;
        /**
         * The start of the message displayed in the panel for this mode.
         */
        public final String messageStart;
        /**
         * The identifier for this mode, used in URLs and other references.
         */
        public final String modeId;

        private Mode(String queryName, String queryParamKey, String returnField, String wordSg, String wordPl, String messageStart) {
            this.queryName = queryName;
            this.queryParamKey = queryParamKey;
            this.returnField = returnField;
            this.wordSg = wordSg;
            this.wordPl = wordPl;
            this.messageStart = messageStart;
            this.modeId = name().toLowerCase();
        }

    }

}
