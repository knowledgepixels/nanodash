package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.Arrays;
import java.util.stream.Collectors;

public class QueryResultList extends Panel {

    QueryResultList(String markupId, GrlcQuery grlcQuery, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId);

        add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));

        String label = grlcQuery.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        if (viewDisplay.getNanopubId() != null) {
            add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", viewDisplay.getNanopubId())));
        } else {
            add(new Label("np").setVisible(false));
        }
        RepeatingView listItems = new RepeatingView("listItems");
        for (ApiResponseEntry entry : response.getData()) {
            String labelText = buildInlineLabel(entry, response);
            listItems.add(new Label(listItems.newChildId(), labelText).setEscapeModelStrings(false));
        }
        add(listItems);
    }

    private String buildInlineLabel(ApiResponseEntry entry, ApiResponse response) {
        return Arrays.stream(response.getHeader())
                .map(entry::get)
                .filter(entryValue -> entryValue != null && !entryValue.isBlank())
                .map(entryValue -> Utils.looksLikeHtml(entryValue) ? Utils.sanitizeHtml(entryValue) : entryValue)
                .collect(Collectors.joining(", "));
    }

}
