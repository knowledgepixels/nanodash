package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultList extends QueryResult {

    private static final String SEPARATOR = " · ";

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultList(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        if (viewDisplay.getNanopubId() != null) {
            add(new SourceNanopub("np", viewDisplay.getNanopubId(), "smallbutton"));
        } else {
            add(new Label("np").setVisible(false));
        }

        setOutputMarkupId(true);
        populateComponent();
    }

    @Override
    protected void populateComponent() {
        QueryResultDataProvider dataProvider = new QueryResultDataProvider(response.getData());
        DataView<ApiResponseEntry> dataView = new DataView<>("items", dataProvider) {

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                ApiResponseEntry entry = item.getModelObject();
                RepeatingView listItem = new RepeatingView("listItem");

                List<Component> components = new ArrayList<>();
                for (String key : response.getHeader()) {
                    if (!key.endsWith("_label")) {
                        String entryValue = entry.get(key);
                        if (entryValue != null && !entryValue.isBlank()) {
                            if (entryValue.matches("https?://.+")) {
                                String entryLabel = entry.get(key + "_label");
                                components.add(new NanodashLink("component", entryValue, null, null, entryLabel, contextId));
                            } else {
                                if (Utils.looksLikeHtml(entryValue)) {
                                    entryValue = Utils.sanitizeHtml(entryValue);
                                }
                                components.add(new Label("component", entryValue).setEscapeModelStrings(false));
                            }
                        }
                    }
                }
                View view = viewDisplay.getView();
                if (view != null && !view.getViewEntryActionList().isEmpty()) {
                    List<AbstractLink> links = new ArrayList<>();
                    for (IRI actionIri : view.getViewEntryActionList()) {
                        // TODO Copied code and adjusted from QueryResultTableBuilder:
                        Template t = view.getTemplateForAction(actionIri);
                        if (t == null) continue;
                        String targetField = view.getTemplateTargetFieldForAction(actionIri);
                        if (targetField == null) targetField = "resource";
                        String labelForAction = view.getLabelForAction(actionIri);
                        if (labelForAction == null) labelForAction = "action...";
                        PageParameters params = new PageParameters().set("template", t.getId())
                                .set("param_" + targetField, contextId)
                                .set("context", contextId)
                                .set("template-version", "latest");
                        String partField = view.getTemplatePartFieldForAction(actionIri);
                        if (partField != null) {
                            // TODO Find a better way to pass the MaintainedResource object to this method:
                            MaintainedResource r = MaintainedResource.get(contextId);
                            if (r != null && r.getNamespace() != null) {
                                params.set("param_" + partField, r.getNamespace() + "<SET-SUFFIX>");
                            }
                        }
                        String queryMapping = view.getTemplateQueryMapping(actionIri);
                        if (queryMapping != null && queryMapping.contains(":")) {
                            // This part is different from the code in QueryResultTableBuilder:
                            String queryParam = queryMapping.split(":")[0];
                            String templateParam = queryMapping.split(":")[1];
                            params.set("param_" + templateParam, entry.get(queryParam));
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, params);
                        button.setBody(Model.of(labelForAction));
                        links.add(button);
                    }
                    components.add(new ButtonList("component", resourceWithProfile, links, null, null));
                }
                ComponentSequence componentSequence = new ComponentSequence(listItem.newChildId(), SEPARATOR, components);
                listItem.add(componentSequence);
                item.add(listItem);
            }
        };
        dataView.setItemsPerPage(10);
        dataView.setOutputMarkupId(true);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);

        add(navigation);
        add(dataView);
    }

}
