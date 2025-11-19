package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.List;

public class QueryResultList extends Panel {

    private static final String SEPARATOR = ", ";
    private final List<AbstractLink> buttons = new ArrayList<>();
    private String contextId;
    private boolean finalized = false;
    private Space space;

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

    public void addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        if (contextId != null) {
            parameters.set("context", contextId);
        }
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            if (!buttons.isEmpty()) {
                add(new ButtonList("buttons", space, buttons, null, null));
            } else {
                add(new Label("buttons").setVisible(false));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    private String buildInlineLabel(ApiResponseEntry entry, ApiResponse response) {
        StringBuilder labelBuilder = new StringBuilder();
        for (String key : response.getHeader()) {
            if (!key.endsWith("_label")) {
                String entryValue = entry.get(key);
                if (entryValue != null && !entryValue.isBlank()) {
                    if (Utils.looksLikeHtml(entryValue)) {
                        entryValue = Utils.sanitizeHtml(entryValue);
                    } else if (entryValue.matches("https?://.+")) {
                        String label = entry.get(key + "_label");
                        String anchorElement = "<a href=\"%s\">%s</a>";
                        if (label != null && !label.isBlank()) {
                            entryValue = String.format(anchorElement, entryValue, label);
                        } else {
                            entryValue = String.format(anchorElement, entryValue, entryValue);
                        }
                    }
                    labelBuilder.append(entryValue).append(SEPARATOR);
                }
            }
        }
        if (labelBuilder.toString().endsWith(SEPARATOR)) {
            labelBuilder.setLength(labelBuilder.length() - 2);
        }
        return labelBuilder.toString();
    }

}
