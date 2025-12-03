package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.ApiResponseEntryFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataProvider extends SortableDataProvider<ApiResponseEntry, String> implements IFilterStateLocator<ApiResponseEntryFilter> {

    private List<ApiResponseEntry> data = new ArrayList<>();
    private ApiResponseEntryFilter filter = new ApiResponseEntryFilter();

    public DataProvider() {
        setSort("date", SortOrder.ASCENDING);
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
        return filterEntries(data)
                .subList((int) first, (int) (first + count))
                .iterator();
        //return Utils.subList(data, first, first + count).iterator();
    }

    @Override
    public IModel<ApiResponseEntry> model(ApiResponseEntry object) {
        return new Model<ApiResponseEntry>(object);
    }

    @Override
    public long size() {
        return filterEntries(data).size();
        //return data.size();
    }

    @Override
    public ApiResponseEntryFilter getFilterState() {
        return filter;
    }

    @Override
    public void setFilterState(ApiResponseEntryFilter apiResponseEntryFilter) {
        this.filter = apiResponseEntryFilter;
    }

    private List<ApiResponseEntry> filterEntries(List<ApiResponseEntry> responseEntries) {
        ArrayList<ApiResponseEntry> result = new ArrayList<>();
        String textSearch = filter.getTextSearch();

        for (ApiResponseEntry entry : responseEntries) {
            if (textSearch != null) {
                for (String key : entry.getKeys()) {
                    String entryValue = entry.get(key);
                    if (entryValue != null && entryValue.toLowerCase().contains(textSearch.toLowerCase())) {
                        result.add(entry);
                        break;
                    }
                }
            } else {
                result.add(entry);
            }
        }

        return result;
    }

}
