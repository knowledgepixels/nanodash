package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ItemListElement extends Panel {

    public ItemListElement(String markupId, Class<? extends WebPage> pageClass, final PageParameters parameters, String linkText, String note) {
        super(markupId);

        add(new BookmarkablePageLink<>("link", pageClass, parameters).setBody(Model.of(linkText)));
        if (note == null) {
            add(new Label("note").setVisible(false));
        } else {
            add(new Label("note", note));
        }
    }

    public ItemListElement(String markupId, Class<? extends WebPage> pageClass, final PageParameters parameters, String linkText) {
        this(markupId, pageClass, parameters, linkText, null);
    }

}
