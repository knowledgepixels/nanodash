package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.QueryPage;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * A table component that displays the results of a query.
 */
public class QueryResultTable extends Panel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(QueryResultTable.class);

    private QueryResultTable(String id, GrlcQuery q, ApiResponse response, boolean plain) {
        super(id);

        if (plain) {
            add(new Label("label").setVisible(false));
            add(new Label("morelink").setVisible(false));
        } else {
            add(new Label("label", q.getLabel()));
            add(new BookmarkablePageLink<Void>("morelink", QueryPage.class, new PageParameters().add("id", q.getNanopub().getUri())));
        }

        List<IColumn<ApiResponseEntry, String>> columns = new ArrayList<>();
        DataProvider dp;
        try {
            for (String h : response.getHeader()) {
                if (h.endsWith("_label")) continue;
                columns.add(new Column(h.replaceAll("_", " "), h));
            }
            dp = new DataProvider(response.getData());
            DataTable<ApiResponseEntry, String> table = new DataTable<>("table", columns, dp, 100);
            table.addBottomToolbar(new NavigationToolbar(table));
            table.addBottomToolbar(new NoRecordsToolbar(table));
            table.addTopToolbar(new HeadersToolbar<String>(table, dp));
            add(table);
        } catch (Exception ex) {
            logger.error("Error creating table for query {}", q.getQueryId(), ex);
            add(new Label("table", "").setVisible(false));
        }
    }


    private class Column extends AbstractColumn<ApiResponseEntry, String> {

        private static final long serialVersionUID = 1L;

        private String key;

        public Column(String title, String key) {
            super(new Model<String>(title), key);
            this.key = key;
        }

        @Override
        public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
            String value = rowModel.getObject().get(key);
            if (value.matches("https?://.+ .+")) {
                List<Component> links = new ArrayList<>();
                for (String v : value.split(" ")) {
                    links.add(new NanodashLink("component", v));
                }
                cellItem.add(new ComponentSequence(componentId, ", ", links));
            } else if (value.matches("https?://.+")) {
                String label = rowModel.getObject().get(key + "_label");
                cellItem.add(new NanodashLink(componentId, value, null, null, false, label));
            } else {
                if (key.startsWith("pubkey")) {
                    cellItem.add(new Label(componentId, value).add(new AttributeAppender("style", "overflow-wrap: anywhere;")));
                } else {
                    cellItem.add(new Label(componentId, value));
                }
            }
        }

    }


    private class DataProvider implements ISortableDataProvider<ApiResponseEntry, String> {

        private static final long serialVersionUID = 1L;

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

    private class ApiResponseComparator implements Comparator<ApiResponseEntry>, Serializable {

        private static final long serialVersionUID = 1L;
        private SortParam<String> sortParam;

        public ApiResponseComparator(SortParam<String> sortParam) {
            this.sortParam = sortParam;
        }

        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            String p = sortParam.getProperty();
            int result;
            if (o1.get(p) == null && o2.get(p) == null) {
                result = 0;
            } else if (o1.get(p) == null) {
                result = 1;
            } else if (o2.get(p) == null) {
                result = -1;
            } else {
                result = o1.get(p).compareTo(o2.get(p));
            }
            if (!sortParam.isAscending()) result = -result;
            return result;
        }

    }

    /**
     * <p>createComponent.</p>
     *
     * @param markupId a {@link java.lang.String} object
     * @param queryId  a {@link java.lang.String} object
     * @param plain    a boolean
     * @return a {@link org.apache.wicket.Component} object
     */
    public static Component createComponent(final String markupId, final String queryId, boolean plain) {
        return createComponent(markupId, queryId, getParams(), plain);
    }

    /**
     * <p>createComponent.</p>
     *
     * @param markupId a {@link java.lang.String} object
     * @param queryId  a {@link java.lang.String} object
     * @param params   a {@link java.util.HashMap} object
     * @param plain    a boolean
     * @return a {@link org.apache.wicket.Component} object
     */
    public static Component createComponent(final String markupId, final String queryId, HashMap<String, String> params, boolean plain) {
        final GrlcQuery q = GrlcQuery.get(queryId);
        ApiResponse response = ApiCache.retrieveResponse(queryId, params);
        if (response != null) {
            return new QueryResultTable(markupId, q, response, plain);
        } else {
            return new ApiResultComponent(markupId, queryId, params) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new QueryResultTable(markupId, q, response, plain);
                }

            };
        }
    }

    private static HashMap<String, String> getParams() {
        final HashMap<String, String> params = new HashMap<>();
        return params;
    }

}
