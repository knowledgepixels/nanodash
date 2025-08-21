package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ReferenceTablePage;
import org.apache.wicket.Component;
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
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;
import java.util.*;

public class ExploreDataTable extends Panel {

    private static final long serialVersionUID = 1L;

    private static final String refQueryName = "find-uri-references";

    private ExploreDataTable(String id, String ref, ApiResponse response, int limit) {
        super(id);
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
            dp = new DataProvider(filterData(response.getData(), ref, limit));
            DataTable<ApiResponseEntry, String> table = new DataTable<>("datatable", columns, dp, 100);
            table.addBottomToolbar(new NavigationToolbar(table));
            table.addBottomToolbar(new NoRecordsToolbar(table));
            table.addTopToolbar(new HeadersToolbar<String>(table, dp));
            add(table);
            add(new Label("message", "").setVisible(false));
            BookmarkablePageLink<Void> showAllLink = new BookmarkablePageLink<Void>("show-all", ReferenceTablePage.class, new PageParameters().add("id", ref));
            showAllLink.setVisible(limit > 0 && response.getData().size() > limit);
            add(showAllLink);
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new Label("datatable", "").setVisible(false));
            add(new Label("message", "Could not load data table."));
            add(new Label("show-all").setVisible(false));
        }
    }


    private List<ApiResponseEntry> filterData(List<ApiResponseEntry> data, String nanopubUri, int limit) {
        List<ApiResponseEntry> filteredList = new ArrayList<>();
        Nanopub np = Utils.getAsNanopub(nanopubUri);
        if (np == null && limit == 0) return data;
        for (ApiResponseEntry e : data) {
            if (np == null || !nanopubUri.equals(e.get("np"))) {
                filteredList.add(e);
            }
            if (limit > 0 && limit == filteredList.size()) break;
        }
        return filteredList;
    }


    private class Column extends AbstractColumn<ApiResponseEntry, String> {

        private static final long serialVersionUID = 1L;

        private String key, current;

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
                cellItem.add(new Label(componentId, value));
            }
        }

    }


    private class DataProvider implements ISortableDataProvider<ApiResponseEntry, String> {

        private static final long serialVersionUID = 1L;

        private List<ApiResponseEntry> data = new ArrayList<>();
        private SingleSortState<String> sortState = new SingleSortState<>();

        public DataProvider() {
            sortState.setSort(new SortParam<String>("date", false));
        }

        public DataProvider(List<ApiResponseEntry> data) {
            this();
            this.data = data;
        }

        @Override
        public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
            List<ApiResponseEntry> copy = new ArrayList<>(data);
            ApiResponseComparator comparator = new ApiResponseComparator(sortState.getSort());
            Collections.sort(copy, comparator);
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

    public static Component createComponent(final String markupId, final String ref, int limit) {
        ApiResponse response = ApiCache.retrieveResponse(refQueryName, getParams(ref));
        if (response != null) {
            return new ExploreDataTable(markupId, ref, response, limit);
        } else {
            return new ApiResultComponent(markupId, refQueryName, getParams(ref)) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ExploreDataTable(markupId, ref, response, limit);
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
