package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.ExternalLinkWithActionsPanel;
import com.knowledgepixels.nanodash.component.QueryResultTableBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

public class ReferencesPage extends NanodashPage {

    public static final String MOUNT_PATH = "/references";

    /**
     * The view used to render references to a given URI. Shared with the About pages.
     */
    public static final String REFERENCES_VIEW = "https://w3id.org/np/RAZ0EGsBlca8unLqQzGl5kVapGgllKvDbGFlTA_FFD7oM/references-view";

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

        View view = View.get(REFERENCES_VIEW);
        QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), "ref", ref);
        add(QueryResultTableBuilder.create("reftable", queryRef, new ViewDisplay(view)).build());
    }

}
