package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.QueryResultTableBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

/**
 * ResultTablePage displays the result of a query in a table format.
 */
public class ResultTablePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/resulttable";

    private final String query;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for ResultTablePage.
     *
     * @param parameters Page parameters containing the query to be executed.
     */
    public ResultTablePage(final PageParameters parameters) {
        super(parameters);

        query = parameters.get("query").toString();
        add(new TitleBar("titlebar", this, null));
        final String shortName = query.replaceFirst("^.*/", "");
        add(new Label("pagetitle", shortName + " (result table) | nanodash"));

        add(QueryResultTableBuilder.create("table", new QueryRef(query), 20).build());

    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
