package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.FilteredQueryResultDataProvider;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.QueryResultDataProvider;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

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
        add(new Label("label", label));
        setOutputMarkupId(true);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && paragraphsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    paragraphsContainer.addOrReplace(buildParagraphsView());
                    target.add(paragraphsContainer);
                }
            }
        });
        add(filterField);

        populateComponent();
    }

    @Override
    protected void populateComponent() {
        filteredDataProvider = new FilteredQueryResultDataProvider(new QueryResultDataProvider(response.getData()), response);

        paragraphsContainer = new WebMarkupContainer("paragraphs-container");
        paragraphsContainer.setOutputMarkupId(true);
        paragraphsContainer.add(buildParagraphsView());
        add(paragraphsContainer);
    }

    private ListView<ApiResponseEntry> buildParagraphsView() {
        return new ListView<>("paragraphs", filteredDataProvider.getFilteredData()) {
            @Override
            protected void populateItem(ListItem<ApiResponseEntry> item) {
                item.add(new Label("title", item.getModelObject().get("title")));
                String npId = item.getModelObject().get("np");
                if (npId != null && !npId.isBlank()) {
                    item.add(new BookmarkablePageLink<Void>("pnp", ExplorePage.class, new PageParameters().set("id", npId)));
                } else {
                    item.add(new Label("pnp").setVisible(false));
                }
                item.add(new Label("content", item.getModelObject().get("content")).setEscapeModelStrings(false));
            }
        };
    }

}
