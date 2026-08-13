package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.EntryActionMenu;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

/**
 * Component for displaying query results as inline SVG (gen:SvgView). The query is
 * expected to return ready-to-embed SVG markup in an "svg" column — one rendered
 * figure per result row, with an optional "title" column as the figure's heading.
 * The markup is sanitized to a static-SVG subset before rendering, so the query
 * fully controls the visual but cannot inject scripting or styling.
 */
public class QueryResultSvg extends QueryResult {

    /**
     * Constructor for QueryResultSvg.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultSvg(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label).setVisible(label != null && !label.isEmpty()));
        setOutputMarkupId(true);

        populateComponent();
    }

    @Override
    protected void populateComponent() {
        WebMarkupContainer container = new WebMarkupContainer("svg-container");
        container.setOutputMarkupId(true);
        container.add(new ListView<ApiResponseEntry>("figures", response.getData()) {
            @Override
            protected void populateItem(ListItem<ApiResponseEntry> item) {
                String title = item.getModelObject().get("title");
                boolean hasTitle = title != null && !title.isBlank();
                // As in the plain-paragraph view: hide the empty heading for a
                // title-less figure and float the source link into the corner.
                WebMarkupContainer header = new WebMarkupContainer("header");
                if (!hasTitle) header.add(new AttributeAppender("class", " no-title"));
                header.add(new Label("title", title).setVisible(hasTitle));
                List<AbstractLink> links = ViewActionMappings.buildEntryActionLinks(viewDisplay.getView(),
                        item.getModelObject(), queryRef,
                        resourceWithProfile != null ? resourceWithProfile : pageResource,
                        contextId, partId, refRoot, postPublishTab);
                String npId = item.getModelObject().get("np");
                if (npId != null && !npId.isBlank()) {
                    BookmarkablePageLink<Void> sourceLink = new BookmarkablePageLink<>("link", ExplorePage.class,
                            new PageParameters().set("id", npId));
                    sourceLink.add(NavigationContext.pageContextFallback());
                    sourceLink.setBody(Model.of("<span class=\"actionmenu-icon\">↗︎</span>source")).setEscapeModelStrings(false);
                    links.add(sourceLink);
                }
                if (links.isEmpty()) {
                    header.add(new Label("pnp").setVisible(false));
                } else {
                    header.add(new EntryActionMenu("pnp", links));
                }
                item.add(header);
                String svg = item.getModelObject().get("svg");
                item.add(new Label("content", svg == null ? null : withContextInHtmlLinks(Utils.sanitizeSvg(svg))).setEscapeModelStrings(false));
            }
        });
        container.add(new Label("no-records", "(nothing found)").setVisible(response.getData().isEmpty()));
        add(container);
    }

}
