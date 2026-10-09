package com.knowledgepixels.nanodash;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    /**
     * The name of the "_group" column of this result, if any: the column whose values
     * group the rows into sections (rendered as header rows by the result table).
     *
     * @return the column name, or null when the result has no such column
     */
    public String getGroupColumn() {
        for (String h : response.getHeader()) {
            if (h.endsWith("_group")) return h;
        }
        return null;
    }

    /**
     * The rank of each group value: its position among the distinct values of the
     * "_group" column in the order the query returned them. Column sorting keeps the
     * sections in that order and only reorders the rows inside each section.
     */
    private Map<String, Integer> groupRanks() {
        String groupColumn = getGroupColumn();
        Map<String, Integer> ranks = new HashMap<>();
        if (groupColumn == null) return ranks;
        for (ApiResponseEntry entry : response.getData()) {
            String g = entry.get(groupColumn);
            if (g == null) g = "";
            ranks.putIfAbsent(g, ranks.size());
        }
        return ranks;
    }

    @Override
    public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
        List<ApiResponseEntry> data = new ArrayList<>(getFilteredData());
        SortParam<String> sortParam = baseProvider.getSortParam();
        if (sortParam != null) {
            String prop = sortParam.getProperty();
            String labelProp = prop + "_label";
            String sortProp = Arrays.asList(response.getHeader()).contains(labelProp) ? labelProp : prop;
            // With a "_group" column, the sections keep their query order whichever
            // column is sorted and in whichever direction; the sort applies within them.
            String groupColumn = getGroupColumn();
            Map<String, Integer> groupRanks = groupRanks();
            // Values are compared with Utils.compareValues rather than as plain text, so
            // that a column of numbers does not come out with "10" above "9" (issue #673).
            data.sort((o1, o2) -> {
                if (groupColumn != null) {
                    String g1 = o1.get(groupColumn);
                    String g2 = o2.get(groupColumn);
                    int byGroup = Integer.compare(groupRanks.getOrDefault(g1 == null ? "" : g1, Integer.MAX_VALUE),
                            groupRanks.getOrDefault(g2 == null ? "" : g2, Integer.MAX_VALUE));
                    if (byGroup != 0) return byGroup;
                }
                String v1 = o1.get(sortProp);
                String v2 = o2.get(sortProp);
                int result;
                if (v1 == null && v2 == null) result = 0;
                else if (v1 == null) result = 1;
                else if (v2 == null) result = -1;
                else result = Utils.compareValues(v1, v2);
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
