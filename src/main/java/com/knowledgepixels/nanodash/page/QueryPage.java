package com.knowledgepixels.nanodash.page;

import java.net.URLEncoder;
import java.util.List;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.nanopub.extra.services.QueryRef;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * Page for displaying a query and its parameters, allowing users to run the query with specified parameters.
 */
public class QueryPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/query";

    private final Form<Void> form;
    private final List<QueryParamField> paramFields;
    private final FeedbackPanel feedbackPanel;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the QueryPage.
     *
     * @param parameters The page parameters, which should include the query ID and any query parameters.
     */
    public QueryPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this, null));
        add(new Label("pagetitle", "Query Info | nanodash"));

        String id = parameters.get("id").toString();
        final String queryId = parameters.get("runquery").toString();
        if (id == null) id = queryId;

        final Multimap<String, String> queryParams = ArrayListMultimap.create();
        for (NamedPair param : parameters.getAllNamed()) {
            if (!param.getKey().startsWith("queryparam_")) continue;
            queryParams.put(param.getKey().replaceFirst("queryparam_", ""), param.getValue());
        }

        GrlcQuery q = GrlcQuery.get(id);

        add(new Label("querylabel", q.getLabel()));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", q.getNanopub().getUri().stringValue())));
        // TODO Replace hard-coded domain with dynamic solution:
        add(new ExternalLink("openapi-this", "https://query.knowledgepixels.com/openapi/?url=spec/" + id));
        add(new ExternalLink("openapi-latest", "https://query.knowledgepixels.com/openapi/?url=spec/" + id + "%3Fapi-version=latest"));
        add(new Label("querydesc", q.getDescription()));

        form = new Form<Void>("form") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                form.getFeedbackMessages().clear();
            }

            @Override
            protected void onSubmit() {
                try {
                    PageParameters params = new PageParameters();
                    params.add("runquery", q.getQueryId());
                    for (QueryParamField f : paramFields) {
                        for (String v : f.getValues()) {
                            params.add("queryparam_" + f.getParamName(), v);
                        }
                    }
                    setResponsePage(QueryPage.class, params);
                } catch (Exception ex) {
                    String message = ex.getClass().getName();
                    if (ex.getMessage() != null) message = ex.getMessage();
                    feedbackPanel.error(message);
                }
            }

            @Override
            protected void onValidate() {
                super.onValidate();
                for (QueryParamField f : paramFields) {
                    f.getFormComponent().processInput();
                    for (FeedbackMessage fm : f.getFormComponent().getFeedbackMessages()) {
                        form.getFeedbackMessages().add(fm);
                    }
                }
            }

        };
        form.setOutputMarkupId(true);

        WebMarkupContainer paramContainer = new WebMarkupContainer("params");

        paramFields = q.createParamFields("paramfield");
        paramContainer.add(new ListView<QueryParamField>("paramfields", paramFields) {

            protected void populateItem(ListItem<QueryParamField> item) {
                QueryParamField f = item.getModelObject();
                for (StringValue parameter : parameters.getValues("queryparam_" + f.getParamName())) {
                    f.putValue(parameter.toString().replaceFirst("\\s*$", ""));
                }
                item.add(item.getModelObject());
            }

        });
        paramContainer.setVisible(!paramFields.isEmpty());
        form.add(paramContainer);

        // TODO Replace hard-coded Nanopub Query URL with dynamic solution:
        String editLink = q.getEndpoint().stringValue().replaceFirst("^.*/repo/", "https://query.petapico.org/tools/") + "/yasgui.html#query=" + URLEncoder.encode(q.getSparql(), Charsets.UTF_8);
        // TODO We also need to replace the nanopub-query placeholder service URLs in the query above.
        // Deactivated for now:
        form.add(new ExternalLink("yasgui", editLink).setVisible(false));

        add(form);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        if (queryId == null) {
            add(new Label("resulttable").setVisible(false));
        } else {
            add(QueryResultTable.createPlainComponent("resulttable", new QueryRef(queryId, queryParams), 20, null));
        }
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
