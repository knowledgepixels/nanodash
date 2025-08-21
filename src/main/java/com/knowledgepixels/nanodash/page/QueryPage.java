package com.knowledgepixels.nanodash.page;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

public class QueryPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    public static final String MOUNT_PATH = "/query";

    private final Form<Void> form;
    private final List<QueryParamField> paramFields;
    private final FeedbackPanel feedbackPanel;

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    public QueryPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this, null));
        add(new Label("pagetitle", "Query Info | nanodash"));

        String id = parameters.get("id").toString();
        final String queryId = parameters.get("runquery").toString();
        if (id == null) id = queryId;

        final HashMap<String, String> queryParams = new HashMap<>();
        for (String paramKey : parameters.getNamedKeys()) {
            if (!paramKey.startsWith("queryparam_")) continue;
            queryParams.put(paramKey.replaceFirst("queryparam_", ""), parameters.get(paramKey).toString());
        }

        GrlcQuery q = GrlcQuery.get(id);

        add(new Label("querylabel", q.getLabel()));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", q.getNanopub().getUri().stringValue())));
        // TODO Replace hard-coded domain with dynamic solution:
        add(new ExternalLink("openapi-this", "https://query.knowledgepixels.com/openapi/?url=spec/" + q.getQueryId()));
        add(new ExternalLink("openapi-latest", "https://query.knowledgepixels.com/openapi/?url=spec/" + q.getQueryId() + "%3Fapi-version=latest"));
        add(new Label("querydesc", q.getDescription()));

        form = new Form<Void>("form") {

            private static final long serialVersionUID = 1L;

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
                        if (f.getValue() == null) continue;
                        params.add("queryparam_" + f.getParamName(), f.getValue());
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
                    f.getTextField().processInput();
                    for (FeedbackMessage fm : f.getTextField().getFeedbackMessages()) {
                        form.getFeedbackMessages().add(fm);
                    }
                }
            }

        };
        form.setOutputMarkupId(true);

        WebMarkupContainer paramContainer = new WebMarkupContainer("params");

        paramFields = q.createParamFields("paramfield");
        paramContainer.add(new ListView<QueryParamField>("paramfields", paramFields) {

            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem<QueryParamField> item) {
                QueryParamField f = item.getModelObject();
                f.getModel().setObject(parameters.get("queryparam_" + f.getParamName()).toString());
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
            add(QueryResultTable.createComponent("resulttable", queryId, queryParams, true));
        }
    }

    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
