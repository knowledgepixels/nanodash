package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;

/**
 * The template chooser shown on the Publish page: the highlighted "popular" and
 * "get started" views, followed by a single filterable list of all templates.
 */
public class TemplateList extends Panel {

    /**
     * Constructs the template list.
     *
     * @param id the wicket id of this component
     */
    public TemplateList(String id) {
        super(id);
        setOutputMarkupId(true);

        View popularTemplatesView = View.get("https://w3id.org/np/RAYMZEdmvjIS5QGFASa8L5hygapUlvK3feZBpG6quMYqc/popular-templates");
        QueryRef ptQueryRef = new QueryRef(popularTemplatesView.getQuery().getQueryId());
        add(QueryResultItemListBuilder.create("popular-templates", ptQueryRef, new ViewDisplay(popularTemplatesView).withDisplayWidth(6)).build());

        View getStartedView = View.get("https://w3id.org/np/RAeFTjDGTQ-bdulJy4tUlWzRlK8EucXFCxqLrb7Qj35SM/suggested-templates-get-started");
        QueryRef gsQueryRef = new QueryRef(getStartedView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("getstarted-templates", gsQueryRef, new ViewDisplay(getStartedView).withDisplayWidth(6)).build());

        View allTemplatesView = View.get("https://w3id.org/np/RA33Eah2-MF_vAZfMf9BCSKrHJRHxLPm7DNxJ14RPxdN8/all-templates-view");
        QueryRef atQueryRef = new QueryRef(allTemplatesView.getQuery().getQueryId());
        add(QueryResultTableBuilder.create("all-templates", atQueryRef, new ViewDisplay(allTemplatesView)).build());
    }

}
