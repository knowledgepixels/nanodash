package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ThingListPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import java.util.Collections;

public class ThingListPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private ThingListPanel(String markupId, final Mode mode, final String thingRef, ApiResponse response, int limit) {
        super(markupId);
        // TODO Not copying the table here, which can lead to problems if at some point the same result list is sorted differently at different places:
        Collections.sort(response.getData(), new Utils.ApiResponseEntrySorter("date", true));
        if (response.getData().isEmpty()) {
            setVisible(false);
        } else if (response.getData().size() == 1) {
            add(new Label("message", mode.messageStart + " 1 " + mode.wordSg));
        } else if (response.getData().size() <= limit) {
            add(new Label("message", mode.messageStart + " " + response.getData().size() + " " + mode.wordPl));
        } else if (response.getData().size() == 1000) {
            add(new Label("message", mode.messageStart + " more " + mode.wordPl + " (>999) than what can be shown here"));
        } else {
            add(new Label("message", mode.messageStart + " " + response.getData().size() + " " + mode.wordPl));
        }
        if (mode == Mode.TEMPLATES) {
            add(TemplateResults.fromApiResponse("things", response, limit));
        } else {
            add(ThingResults.fromApiResponse("things", mode.returnField, response, limit));
        }

        BookmarkablePageLink<Void> showAllLink = new BookmarkablePageLink<Void>("show-all", ThingListPage.class, new PageParameters().add("ref", thingRef).add("mode", mode.modeId));
        showAllLink.setVisible(limit > 0 && response.getData().size() > limit);
        add(showAllLink);
    }

    public static Component createComponent(final String markupId, final Mode mode, final String thingRef, final String waitMessage, final int limit) {
        ApiResponse response = ApiCache.retrieveResponse(mode.queryName, mode.queryParam, thingRef);
        if (response != null) {
            return new ThingListPanel(markupId, mode, thingRef, response, limit);
        } else {
            ApiResultComponent c = new ApiResultComponent(markupId, mode.queryName, mode.queryParam, thingRef) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ThingListPanel(markupId, mode, thingRef, response, limit);
                }

            };
            c.setWaitMessage(waitMessage);
            return c;
        }
    }

    public enum Mode {
        CLASSES("get-classes-for-thing", "thing", "class", "class", "classes", "Assigned to"),
        INSTANCES("get-instances", "class", "instance", "instance", "instances", "Has"),
        PARTS("get-parts", "thing", "part", "part", "parts", "Has"),
        TEMPLATES("get-templates-with-uri", "thing", "np", "template", "templates", "Used in");

        public final String queryName;
        public final String queryParam;
        public final String returnField;
        public final String wordSg, wordPl;
        public final String messageStart;
        public final String modeId;

        private Mode(String queryName, String queryParam, String returnField, String wordSg, String wordPl, String messageStart) {
            this.queryName = queryName;
            this.queryParam = queryParam;
            this.returnField = returnField;
            this.wordSg = wordSg;
            this.wordPl = wordPl;
            this.messageStart = messageStart;
            this.modeId = name().toLowerCase();
        }

    }

}
