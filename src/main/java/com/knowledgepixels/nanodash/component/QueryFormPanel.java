package com.knowledgepixels.nanodash.component;

import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.MagicQueryParams;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.ViewResultsPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel rendering a query-form view ({@code gen:QueryFormView}): a form with a field
 * for each of the view query's placeholders that is neither magic (session-bound) nor
 * fixed by the page context. Submitting redirects to {@link ViewResultsPage}, where
 * the results render according to the view's display type. The fixed parameters are
 * carried along in the redirect.
 */
public class QueryFormPanel extends Panel {

    private final Form<?> form;
    private final List<QueryParamField> paramFields;
    private final FeedbackPanel feedbackPanel;

    /**
     * Constructs a QueryFormPanel.
     *
     * @param id          the Wicket component ID
     * @param viewDisplay the view display of the query-form view
     * @param fixedParams parameter values fixed by the page context (auto-filled, not
     *                    shown as form fields), keyed by parameter name
     */
    public QueryFormPanel(String id, ViewDisplay viewDisplay, Multimap<String, String> fixedParams) {
        this(id, viewDisplay, fixedParams, null, null);
    }

    /**
     * Constructs a QueryFormPanel with initial field values.
     *
     * @param id            the Wicket component ID
     * @param viewDisplay   the view display of the query-form view
     * @param fixedParams   parameter values fixed by the page context (auto-filled, not
     *                      shown as form fields), keyed by parameter name
     * @param initialValues values to pre-fill the form fields with (e.g. the previously
     *                      submitted ones), keyed by parameter name; may be null
     * @param pageResource  the resource whose page the panel is on; non-null makes the
     *                      panel carry the standard view-display dropdown menu (null on
     *                      the view-results page, where the results part carries it)
     */
    public QueryFormPanel(String id, ViewDisplay viewDisplay, Multimap<String, String> fixedParams, Multimap<String, String> initialValues, AbstractResourceWithProfile pageResource) {
        super(id);
        final View view = viewDisplay.getView();
        GrlcQuery query = view.getQuery();

        add(new Label("label", viewDisplay.getTitle()));

        if (pageResource != null && viewDisplay.getNanopubId() != null) {
            add(new ViewDisplayMenu("np", viewDisplay, new QueryRef(query.getQueryId(), fixedParams), pageResource, List.of()));
        } else {
            add(new Label("np").setVisible(false));
        }

        form = new Form<Void>("form") {

            @Override
            protected void onSubmit() {
                PageParameters params = new PageParameters();
                params.set("view", view.getId());
                for (Map.Entry<String, String> e : fixedParams.entries()) {
                    params.add("queryparam_" + e.getKey(), e.getValue());
                }
                for (QueryParamField f : paramFields) {
                    for (String v : f.getValues()) {
                        params.add("queryparam_" + f.getParamName(), v);
                    }
                }
                // Carry the navigation context along, so the results page shows the
                // "< [resource]" back-crumb of the page the search started from.
                if (getPage() instanceof NanodashPage page) {
                    NavigationContext.withContext(params, page.getContextId());
                }
                setResponsePage(ViewResultsPage.class, params);
            }

            @Override
            protected void onError() {
                // Move validation messages to the session: on a resource page this
                // panel sits in ViewList's ListView, which recreates it on the error
                // re-render, discarding component-scoped messages (they also wouldn't
                // survive the auto-refresh redirect). Session-scoped ones render in
                // the recreated panel's feedback panel and are cleared after showing.
                for (QueryParamField f : paramFields) {
                    for (FeedbackMessage fm : f.getFormComponent().getFeedbackMessages()) {
                        getSession().error(String.valueOf(fm.getMessage()));
                    }
                    f.getFormComponent().getFeedbackMessages().clear();
                }
            }

        };
        form.setOutputMarkupId(true);

        paramFields = new ArrayList<>();
        for (String p : query.getPlaceholdersList()) {
            // Magic placeholders are bound from the session, fixed ones from the page context.
            if (MagicQueryParams.isMagic(p)) continue;
            String paramName = QueryTemplate.getParamName(p);
            if (fixedParams.containsKey(paramName)) continue;
            QueryParamField field = new QueryParamField("paramfield", p);
            if (initialValues != null) {
                for (String v : initialValues.get(paramName)) {
                    field.putValue(v);
                }
            }
            if (!field.isOptional()) {
                // Let the browser catch the empty-mandatory case before it reaches
                // the server (same pattern as the basic search forms).
                field.getFormComponent().add(AttributeModifier.replace("required", "required"));
            }
            paramFields.add(field);
        }
        WebMarkupContainer paramContainer = new WebMarkupContainer("params");
        paramContainer.add(new ListView<QueryParamField>("paramfields", paramFields) {

            protected void populateItem(ListItem<QueryParamField> item) {
                item.add(item.getModelObject());
            }

        });
        paramContainer.setVisible(!paramFields.isEmpty());
        form.add(paramContainer);
        add(form);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

}
