package com.knowledgepixels.nanodash.page;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.QueryList;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * Page that lists queries and allows searching through them.
 */
public class QueryListPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/queries";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private TextField<String> searchField;

    /**
     * Constructor for the QueryListPage.
     *
     * @param parameters Page parameters containing the search query.
     */
    public QueryListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "query"));

        final String searchText = parameters.get("query").toString();

        Form<?> form = new Form<Void>("form") {

            protected void onSubmit() {
                String searchText = searchField.getModelObject().trim();
                PageParameters params = new PageParameters();
                params.set("query", searchText);
                setResponsePage(SearchPage.class, params);
            }
        };
        add(form);

        form.add(searchField = new TextField<String>("search", Model.of(searchText)));

        final String queryName = "get-queries";
        final QueryRef queryRef = new QueryRef(queryName);
        ApiResponse qResponse = ApiCache.retrieveResponse(queryRef);
        if (qResponse != null) {
            add(new QueryList("queries", qResponse));
        } else {
            add(new ApiResultComponent("queries", queryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new QueryList(markupId, response);
                }
            });

        }
    }

}
