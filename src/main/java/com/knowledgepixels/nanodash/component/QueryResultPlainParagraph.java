package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.FilteredQueryResultDataProvider;
import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.QueryResultDataProvider;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.EntryActionMenu;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultPlainParagraph extends QueryResult {

    private FilteredQueryResultDataProvider filteredDataProvider;
    private Model<String> filterModel = Model.of("");
    private WebMarkupContainer paragraphsContainer;

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultPlainParagraph(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label).setVisible(label != null && !label.isEmpty()));
        setOutputMarkupId(true);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new FilterUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && paragraphsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    paragraphsContainer.addOrReplace(buildParagraphsView());
                    target.add(paragraphsContainer);
                }
            }
        });
        filterField.setVisible(!fitsOnFirstPage());
        add(filterField);

        populateComponent();
    }

    @Override
    protected void populateComponent() {
        filteredDataProvider = new FilteredQueryResultDataProvider(new QueryResultDataProvider(response.getData()), response);

        paragraphsContainer = new WebMarkupContainer("paragraphs-container");
        paragraphsContainer.setOutputMarkupId(true);
        paragraphsContainer.add(buildParagraphsView());
        paragraphsContainer.add(new Label("no-records", "(nothing found)") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(filteredDataProvider.getFilteredData().isEmpty());
            }
        });
        add(paragraphsContainer);
    }

    private ListView<ApiResponseEntry> buildParagraphsView() {
        return new ListView<>("paragraphs", filteredDataProvider.getFilteredData()) {
            @Override
            protected void populateItem(ListItem<ApiResponseEntry> item) {
                String title = item.getModelObject().get("title");
                boolean hasTitle = title != null && !title.isBlank();
                // A row whose query names the paragraph's own IRI (a "paragraph" column)
                // gets a way to its part page (issue #701): the heading becomes a link and
                // the row menu gains an "open" entry. The body stays prose, never a link.
                // Without a navigation context there is no part page to link to, so the
                // heading stays plain.
                String paragraphId = item.getModelObject().get("paragraph");
                NanodashPageRef partRef = paragraphId != null && Utils.isUriValue(paragraphId)
                        ? partPageRef(paragraphId, hasTitle ? title : null) : null;
                // For a title-less paragraph (e.g. a space description) hide the
                // empty heading and float the source link into the corner (via
                // the "no-title" class) so the header takes no vertical line.
                WebMarkupContainer header = new WebMarkupContainer("header");
                if (!hasTitle) header.add(new AttributeAppender("class", " no-title"));
                WebMarkupContainer heading = new WebMarkupContainer("heading");
                heading.setVisible(hasTitle);
                heading.add(new Label("title", title).setVisible(partRef == null));
                if (partRef == null) {
                    heading.add(new WebMarkupContainer("title-link").setVisible(false));
                } else {
                    heading.add(partRef.createComponent("title-link", title));
                }
                header.add(heading);
                // The view's entry actions, followed by the "open" entry to the row's own
                // part page where there is one, and the former "^" source link as a
                // "source" entry, bundled into a per-paragraph dropdown as in the other
                // view types.
                List<AbstractLink> links = ViewActionMappings.buildEntryActionLinks(viewDisplay.getView(),
                        item.getModelObject(), queryRef,
                        resourceWithProfile != null ? resourceWithProfile : pageResource,
                        contextId, partId, refRoot, postPublishTab);
                if (partRef != null) {
                    BookmarkablePageLink<Void> openLink = new BookmarkablePageLink<>("link", partRef.getPageClass(), partRef.getParameters());
                    openLink.setBody(Model.of("<span class=\"actionmenu-icon\">📄</span>open")).setEscapeModelStrings(false);
                    links.add(openLink);
                }
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
                String content = item.getModelObject().get("content");
                item.add(new Label("content", content == null ? null : cellHtml(content)).setEscapeModelStrings(false));
            }
        };
    }

}
