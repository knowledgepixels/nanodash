package com.knowledgepixels.nanodash.page;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.MagicQueryParams;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.QueryFormPanel;
import com.knowledgepixels.nanodash.component.QueryResultComponentFactory;
import com.knowledgepixels.nanodash.component.QueryResultTableBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;

/**
 * Standalone page showing the full results of a view's query, rendered according to
 * the view's display type. This is where the form of a query-form view
 * ({@code gen:QueryFormView}) leads on submit; the form is repeated at the top with
 * the submitted values, so the search can be refined. Parameters: {@code view} (the
 * view id) and {@code queryparam_<name>} for the query parameter values.
 */
public class ViewResultsPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/view-results";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the ViewResultsPage.
     *
     * @param parameters Page parameters containing the view id and the query parameter values.
     */
    public ViewResultsPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        String viewId = parameters.get("view").toString();
        View view = (viewId == null ? null : View.get(viewId));
        if (view == null) {
            add(new Label("queryform").setVisible(false));
            add(new Label("results", "<span class=\"negative\">View not found: " + Strings.escapeMarkup(String.valueOf(viewId)) + "</span>").setEscapeModelStrings(false));
            return;
        }

        Multimap<String, String> queryParams = ArrayListMultimap.create();
        for (NamedPair param : parameters.getAllNamed()) {
            if (!param.getKey().startsWith("queryparam_")) continue;
            queryParams.put(param.getKey().replaceFirst("queryparam_", ""), param.getValue());
        }

        add(new QueryFormPanel("queryform", new ViewDisplay(view).withDisplayWidth(12), ArrayListMultimap.create(), queryParams, null));

        for (String p : view.getQuery().getPlaceholdersList()) {
            if (QueryTemplate.isOptionalPlaceholder(p)) continue;
            if (MagicQueryParams.isMagic(p)) continue;
            if (!queryParams.containsKey(QueryTemplate.getParamName(p))) {
                add(new Label("results", "Fill in the form above to see the results."));
                return;
            }
        }

        // The form above already shows the view's icon+title, so the results render
        // without their own title row. The navigation context (the resource the search
        // started from) doubles as the id, so view-level actions pre-fill their target
        // field with it, as they do on the resource's own page.
        ViewDisplay resultsDisplay = new ViewDisplay(view).withDisplayWidth(12).withTitle("");
        QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryParams);
        String contextId = getContextId();
        Component results = QueryResultComponentFactory.build("results", queryRef, resultsDisplay, null, contextId, contextId, null);
        if (results == null) {
            // No (or unrecognized) display type on the view: fall back to the plain result table.
            results = QueryResultTableBuilder.create("results", queryRef, resultsDisplay).plain(true).build();
        }
        add(results);
    }

}
