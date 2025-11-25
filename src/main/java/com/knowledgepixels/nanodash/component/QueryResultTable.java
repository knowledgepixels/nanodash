package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
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

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.ResourceView;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;

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
    private Space space;
    private final QueryRef queryRef;
    private final ViewDisplay viewDisplay;

    QueryResultTable(String id, QueryRef queryRef, ApiResponse response, boolean plain, ViewDisplay viewDisplay, String contextId) {
        super(id);
        this.contextId = contextId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;

        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));

        if (plain) {
            add(new Label("label").setVisible(false));
            add(new Label("morelink").setVisible(false));
        } else {
            String label = grlcQuery.getLabel();
            if (viewDisplay.getTitle() != null) label = viewDisplay.getTitle();
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
        DataProvider dp;
        try {
            for (String h : response.getHeader()) {
                if (h.endsWith("_label")) continue;
                columns.add(new Column(h.replaceAll("_", " "), h));
            }
            if (!viewDisplay.getView().getViewEntryActionList().isEmpty()) {
                columns.add(new Column("", Column.ACTIONS));
            }
            dp = new DataProvider(response.getData());
            table = new DataTable<>("table", columns, dp, viewDisplay.getPageSize() < 1 ? Integer.MAX_VALUE : viewDisplay.getPageSize());
            table.setOutputMarkupId(true);
            table.addBottomToolbar(new AjaxNavigationToolbar(table));
            table.addBottomToolbar(new NoRecordsToolbar(table));
            table.addTopToolbar(new HeadersToolbar<String>(table, dp));
            add(table);
        } catch (Exception ex) {
            logger.error("Error creating table for query {}", grlcQuery.getQueryId(), ex);
            add(new Label("table", "").setVisible(false));
            addErrorMessage(ex.getMessage());
        }
    }

    // TODO button adding method copied and adjusted from ItemListPanel
    // TODO Improve this (member/admin) button handling:
    public QueryResultTable addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (contextId != null) parameters.set("context", contextId);
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
        return this;
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            if (!buttons.isEmpty()) {
                add(new ButtonList("buttons", space, buttons, null, null));
            } else {
                add(new Label("buttons").setVisible(false));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    /**
     * Set the space for this component.
     *
     * @param space The space to set.
     */
    public void setSpace(Space space) {
        this.space = space;
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
                if (key.equals(ACTIONS)) {
                    List<AbstractLink> links = new ArrayList<>();
                    ResourceView view = viewDisplay.getView();
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
                    cellItem.add(new ButtonList(componentId, space, links, null, null));
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


    private class DataProvider implements ISortableDataProvider<ApiResponseEntry, String> {

        private List<ApiResponseEntry> data = new ArrayList<>();
        private SingleSortState<String> sortState = new SingleSortState<>();

        public DataProvider() {
//			sortState.setSort(new SortParam<String>("date", false));
        }

        public DataProvider(List<ApiResponseEntry> data) {
            this();
            this.data = data;
        }

        @Override
        public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
//			List<ApiResponseEntry> copy = new ArrayList<>(data);
//			ApiResponseComparator comparator = new ApiResponseComparator(sortState.getSort());
//			Collections.sort(copy, comparator);
//			return Utils.subList(copy, first, first + count).iterator();
            return Utils.subList(data, first, first + count).iterator();
        }

        @Override
        public IModel<ApiResponseEntry> model(ApiResponseEntry object) {
            return new Model<ApiResponseEntry>(object);
        }

        @Override
        public long size() {
            return data.size();
        }

        @Override
        public ISortState<String> getSortState() {
            return sortState;
        }

        @Override
        public void detach() {
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
