package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A table component that displays the results of a query.
 */
public class QueryResultTable extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultTable.class);

    private Model<String> errorMessages = Model.of("");
    private DataTable<ApiResponseEntry, String> table;
    private Label errorLabel;
    private boolean finalized = false;
    private List<AbstractLink> buttons = new ArrayList<>();
    private String contextId;
    private ProfiledResource profiledResource;
    private final QueryRef queryRef;
    private final ViewDisplay viewDisplay;

    QueryResultTable(String id, QueryRef queryRef, ApiResponse response, boolean plain, ViewDisplay viewDisplay) {
        super(id);
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;

        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));

        if (plain) {
            add(new Label("label").setVisible(false));
            add(new Label("morelink").setVisible(false));
        } else {
            String label = grlcQuery.getLabel();
            if (viewDisplay.getTitle() != null) {
                label = viewDisplay.getTitle();
            }
            add(new Label("label", label));
            if (viewDisplay.getNanopubId() != null) {
                add(new BookmarkablePageLink<Void>("morelink", ExplorePage.class, new PageParameters().set("id", viewDisplay.getNanopubId())));
            } else {
                add(new Label("morelink").setVisible(false));
            }
        }

        errorLabel = new Label("error-messages", errorMessages);
        errorLabel.setVisible(false);
        add(errorLabel);

        List<IColumn<ApiResponseEntry, String>> columns = new ArrayList<>();
        QueryResultDataProvider dataProvider;
        try {
            for (String h : response.getHeader()) {
                if (h.endsWith("_label")) {
                    continue;
                }
                columns.add(new Column(h.replaceAll("_", " "), h));
            }
            if (viewDisplay.getView() != null && !viewDisplay.getView().getViewEntryActionList().isEmpty()) {
                columns.add(new Column("", Column.ACTIONS));
            }
            dataProvider = new QueryResultDataProvider(response.getData());
            table = new DataTable<>("table", columns, dataProvider, viewDisplay.getPageSize() < 1 ? Integer.MAX_VALUE : viewDisplay.getPageSize());
            table.setOutputMarkupId(true);
            table.addBottomToolbar(new AjaxNavigationToolbar(table));
            table.addBottomToolbar(new NoRecordsToolbar(table));
            table.addTopToolbar(new HeadersToolbar<String>(table, dataProvider));
            add(table);
        } catch (Exception ex) {
            logger.error("Error creating table for query {}", grlcQuery.getQueryId(), ex);
            add(new Label("table", "").setVisible(false));
            addErrorMessage(ex.getMessage());
        }
    }

    // TODO button adding method copied and adjusted from ItemListPanel
    // TODO Improve this (member/admin) button handling:
    public void addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        if (contextId != null) {
            parameters.set("context", contextId);
        }
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            if (!buttons.isEmpty()) {
                add(new ButtonList("buttons", profiledResource, buttons, null, null));
            } else {
                add(new Label("buttons").setVisible(false));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    /**
     * Set the profiled resource for this component.
     *
     * @param profiledResource The profiled resource to set.
     */
    public void setProfiledResource(ProfiledResource profiledResource) {
        this.profiledResource = profiledResource;
    }

    /**
     * Set the context ID for this component.
     *
     * @param contextId The context ID to set.
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    private void addErrorMessage(String errorMessage) {
        String s = errorMessages.getObject();
        if (s.isEmpty()) {
            s = "Error: " + errorMessage;
        } else {
            s += ", " + errorMessage;
        }
        errorMessages.setObject(s);
        errorLabel.setVisible(true);
        if (table != null) table.setVisible(false);
    }

    private class Column extends AbstractColumn<ApiResponseEntry, String> {

        private String key;
        public static final String ACTIONS = "*actions*";

        public Column(String title, String key) {
            super(new Model<String>(title), key);
            this.key = key;
        }

        @Override
        public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
            try {
                ResourceView view = viewDisplay.getView();
                if (key.equals(ACTIONS) && view != null) {
                    List<AbstractLink> links = new ArrayList<>();
                    for (IRI actionIri : view.getViewEntryActionList()) {
                        // TODO Copied code and adjusted from QueryResultTableBuilder:
                        Template t = view.getTemplateForAction(actionIri);
                        if (t == null) continue;
                        String targetField = view.getTemplateTargetFieldForAction(actionIri);
                        if (targetField == null) targetField = "resource";
                        String label = view.getLabelForAction(actionIri);
                        if (label == null) label = "action...";
                        PageParameters params = new PageParameters().set("template", t.getId()).set("param_" + targetField, contextId).set("context", contextId);
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
                            params.set("param_" + templateParam, rowModel.getObject().get(queryParam));
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, params);
                        button.setBody(Model.of(label));
                        links.add(button);
                    }
                    cellItem.add(new ButtonList(componentId, profiledResource, links, null, null));
                } else {
                    String value = rowModel.getObject().get(key);
                    if (value.matches("https?://.+ .+")) {
                        List<Component> links = new ArrayList<>();
                        for (String v : value.split(" ")) {
                            links.add(new NanodashLink("component", v));
                        }
                        cellItem.add(new ComponentSequence(componentId, ", ", links));
                    } else if (value.matches("https?://.+")) {
                        String label = rowModel.getObject().get(key + "_label");
                        cellItem.add(new NanodashLink(componentId, value, null, null, label, contextId));
                    } else {
                        if (key.startsWith("pubkey")) {
                            cellItem.add(new Label(componentId, value).add(new AttributeAppender("style", "overflow-wrap: anywhere;")));
                        } else {
                            Label cellLabel;
                            if (Utils.looksLikeHtml(value)) {
                                cellLabel = (Label) new Label(componentId, Utils.sanitizeHtml(value))
                                        .setEscapeModelStrings(false)
                                        .add(new AttributeAppender("class", "cell-data-html"));
                            } else {
                                cellLabel = new Label(componentId, value);
                            }
                            cellItem.add(cellLabel);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("Failed to populate table column: ", ex);
                cellItem.add(new Label(componentId).setVisible(false));
                addErrorMessage(ex.getMessage());
            }
        }

    }

//    private class ApiResponseComparator implements Comparator<ApiResponseEntry>, Serializable {
//
//        private SortParam<String> sortParam;
//
//        public ApiResponseComparator(SortParam<String> sortParam) {
//            this.sortParam = sortParam;
//        }
//
//        @Override
//        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
//            String p = sortParam.getProperty();
//            int result;
//            if (o1.get(p) == null && o2.get(p) == null) {
//                result = 0;
//            } else if (o1.get(p) == null) {
//                result = 1;
//            } else if (o2.get(p) == null) {
//                result = -1;
//            } else {
//                result = o1.get(p).compareTo(o2.get(p));
//            }
//            if (!sortParam.isAscending()) result = -result;
//            return result;
//        }
//
//    }

}
