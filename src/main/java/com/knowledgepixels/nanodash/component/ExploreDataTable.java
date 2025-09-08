package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
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
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;

/**
 * A component that displays a data table of nanopublication references.
 */
public class ExploreDataTable extends Panel {

    private static final long serialVersionUID = 1L;

    private static final String refQueryName = "find-uri-references";
    private static final Logger logger = LoggerFactory.getLogger(ExploreDataTable.class);

    private ExploreDataTable(String id, String ref, ApiResponse response) {
        super(id);
        setOutputMarkupId(true);

        List<IColumn<ApiResponseEntry, String>> columns = new ArrayList<>();
        DataProvider dp;
        try {
            columns.add(new Column("Nanopublication", "np", ref));
            columns.add(new Column("Part", "graphpred", ref));
            columns.add(new Column("Subject", "subj", ref));
            columns.add(new Column("Predicate", "pred", ref));
            columns.add(new Column("Object", "obj", ref));
            columns.add(new Column("Published By", "pubkey", ref));
            columns.add(new Column("Published On", "date", ref));
            dp = new DataProvider(filterData(response.getData(), ref));
            DataTable<ApiResponseEntry, String> table = new DataTable<>("datatable", columns, dp, 10);
            table.addBottomToolbar(new AjaxNavigationToolbar(table));
            table.addBottomToolbar(new NoRecordsToolbar(table));
            table.addTopToolbar(new HeadersToolbar<String>(table, dp));
            table.setOutputMarkupId(true);
            add(table);
            add(new Label("message", "").setVisible(false));
        } catch (Exception ex) {
            logger.error("Could not create data table for reference: {}", ref, ex);
            add(new Label("datatable", "").setVisible(false));
            add(new Label("message", "Could not load data table."));
            add(new Label("show-all").setVisible(false));
        }
    }


    private List<ApiResponseEntry> filterData(List<ApiResponseEntry> data, String nanopubUri) {
        List<ApiResponseEntry> filteredList = new ArrayList<>();
        Nanopub np = Utils.getAsNanopub(nanopubUri);
        if (np == null) return data;
        for (ApiResponseEntry e : data) {
            if (np == null || !nanopubUri.equals(e.get("np"))) {
                filteredList.add(e);
            }
        }
        return filteredList;
    }


    private class Column extends AbstractColumn<ApiResponseEntry, String> {

        private static final long serialVersionUID = 1L;

        private String key, current;

        /**
         * Constructor for a column in the data table.
         *
         * @param title   The title of the column.
         * @param key     The key used to retrieve data from ApiResponseEntry.
         * @param current The current value to highlight in the column.
         */
        public Column(String title, String key, String current) {
            super(new Model<String>(title), key);
            this.key = key;
            this.current = current;
        }

        @Override
        public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
            String value = rowModel.getObject().get(key);
            if (value.equals(current)) {
                cellItem.add(new Label(componentId, "<strong>" + IriItem.getShortNameFromURI(value) + "</strong>").setEscapeModelStrings(false));
            } else if (value.matches("https?://.+")) {
                cellItem.add(new NanodashLink(componentId, value));
            } else {
                if (key.equals("pubkey")) {
                    cellItem.add(new Label(componentId, User.getShortDisplayNameForPubkeyhash(null, Utils.createSha256HexHash(value))));
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

        /**
         * Default constructor that initializes the sort state.
         */
        public DataProvider() {
            sortState.setSort(new SortParam<String>("date", false));
        }

        /**
         * Constructor that initializes the data provider with a list of ApiResponseEntry.
         *
         * @param data The list of ApiResponseEntry to be used in the data provider.
         */
        public DataProvider(List<ApiResponseEntry> data) {
            this();
            this.data = data;
        }

        @Override
        public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
            List<ApiResponseEntry> copy = new ArrayList<>(data);
            ApiResponseComparator comparator = new ApiResponseComparator(sortState.getSort());
            copy.sort(comparator);
            return Utils.subList(copy, first, first + count).iterator();
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

        /**
         * Constructor that initializes the comparator with a sort parameter.
         *
         * @param sortParam The sort parameter defining the property and order for comparison.
         */
        public ApiResponseComparator(SortParam<String> sortParam) {
            this.sortParam = sortParam;
        }

        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            String p = sortParam.getProperty();
            int result = o1.get(p).compareTo(o2.get(p));
            if (!sortParam.isAscending()) result = -result;
            return result;
        }

    }

    /**
     * Creates a new ExploreDataTable component.
     *
     * @param markupId the Wicket markup ID for the component
     * @param ref      the reference URI to be displayed in the table
     * @return a new ExploreDataTable component or an ApiResultComponent if the data is not cached
     */
    public static Component createComponent(final String markupId, final String ref) {
        ApiResponse response = ApiCache.retrieveResponse(refQueryName, getParams(ref));
        if (response != null) {
            return new ExploreDataTable(markupId, ref, response);
        } else {
            return new ApiResultComponent(markupId, refQueryName, getParams(ref)) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ExploreDataTable(markupId, ref, response);
                }

            };
        }
    }

    private static HashMap<String, String> getParams(String ref) {
        final HashMap<String, String> params = new HashMap<>();
        params.put("ref", ref);
        return params;
    }

}
