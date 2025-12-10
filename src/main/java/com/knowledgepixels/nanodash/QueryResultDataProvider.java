package com.knowledgepixels.nanodash;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Data provider for query results.
 */
public class QueryResultDataProvider implements ISortableDataProvider<ApiResponseEntry, String> {

    private List<ApiResponseEntry> data = new ArrayList<>();
    private SingleSortState<String> sortState = new SingleSortState<>();

    /**
     * Default constructor.
     */
    public QueryResultDataProvider() {
    }

    /**
     * Constructor with initial data.
     *
     * @param data List of ApiResponseEntry
     */
    public QueryResultDataProvider(List<ApiResponseEntry> data) {
        this();
        this.data = data;
    }

    @Override
    public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
        return Utils.subList(data, first, first + count).iterator();
    }

    @Override
    public IModel<ApiResponseEntry> model(ApiResponseEntry object) {
        return new Model<>(object);
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
