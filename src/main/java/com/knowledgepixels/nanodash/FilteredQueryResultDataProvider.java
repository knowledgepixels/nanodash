package com.knowledgepixels.nanodash;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
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

    private List<ApiResponseEntry> getFilteredData() {
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
        List<ApiResponseEntry> data = getFilteredData();
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
