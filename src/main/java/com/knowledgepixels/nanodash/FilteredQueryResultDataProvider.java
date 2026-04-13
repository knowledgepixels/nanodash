package com.knowledgepixels.nanodash;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Filtered data provider that wraps QueryResultDataProvider and filters results based on a filter string.
 */
public class FilteredQueryResultDataProvider implements ISortableDataProvider<ApiResponseEntry, String> {

    private final QueryResultDataProvider baseProvider;
    private final ApiResponse response;
    private String filterText = "";
    private List<ApiResponseEntry> filteredData = null;

    public FilteredQueryResultDataProvider(QueryResultDataProvider baseProvider, ApiResponse response) {
        this.baseProvider = baseProvider;
        this.response = response;
    }

    public void setFilterText(String filterText) {
        if (filterText == null) {
            filterText = "";
        }
        if (!this.filterText.equals(filterText)) {
            this.filterText = filterText;
            this.filteredData = null; // Invalidate cache
        }
    }

    public List<ApiResponseEntry> getFilteredData() {
        if (filteredData != null) {
            return filteredData;
        }

        List<ApiResponseEntry> allData = response.getData();
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredData = allData;
        } else {
            String lowerFilter = filterText.toLowerCase();
            filteredData = new ArrayList<>();
            for (ApiResponseEntry entry : allData) {
                boolean matches = false;
                for (String key : response.getHeader()) {
                    String value = entry.get(key);
                    if (value != null && value.toLowerCase().contains(lowerFilter)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    filteredData.add(entry);
                }
            }
        }
        return filteredData;
    }

    @Override
    public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
        List<ApiResponseEntry> data = new ArrayList<>(getFilteredData());
        SortParam<String> sortParam = baseProvider.getSortParam();
        if (sortParam != null) {
            String prop = sortParam.getProperty();
            data.sort((o1, o2) -> {
                String v1 = o1.get(prop);
                String v2 = o2.get(prop);
                int result;
                if (v1 == null && v2 == null) result = 0;
                else if (v1 == null) result = 1;
                else if (v2 == null) result = -1;
                else result = v1.compareTo(v2);
                if (!sortParam.isAscending()) result = -result;
                return result;
            });
        }
        return Utils.subList(data, first, first + count).iterator();
    }

    @Override
    public IModel<ApiResponseEntry> model(ApiResponseEntry object) {
        return baseProvider.model(object);
    }

    @Override
    public long size() {
        return getFilteredData().size();
    }

    @Override
    public ISortState<String> getSortState() {
        return baseProvider.getSortState();
    }

    @Override
    public void detach() {
        baseProvider.detach();
    }

}
