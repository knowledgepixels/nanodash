package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ExploreDataTable;
import com.knowledgepixels.nanodash.component.ExternalLinkWithActionsPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ReferencesPage extends NanodashPage {

    public static final String MOUNT_PATH = "/references";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    public ReferencesPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        String ref = parameters.get("id").toString();
        ref = ref.replaceFirst(";jsessionid.*$", "");

        String shortName;
        if (parameters.get("label").isEmpty()) {
            shortName = Utils.getShortNameFromURI(ref);
        } else {
            shortName = parameters.get("label").toString();
        }

        add(new Label("pagetitle", shortName + " (references) | nanodash"));
        add(new Label("termname", shortName));
        add(new ExternalLinkWithActionsPanel("urilink", Model.of(ref)));
        add(new BookmarkablePageLink<Void>("back-link", ExplorePage.class, new PageParameters().set("id", ref)));
        add(ExploreDataTable.createComponent("reftable", ref));
    }

}
