package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Data provider that filters a list by a text string using a function to get searchable text from each item.
 * Uses {@link SerializableFunction} so the provider can be serialized with the Wicket page.
 */
public class FilteredListDataProvider<T extends Serializable> implements IDataProvider<T> {

    /**
     * Serializable function interface for use in Wicket components that get serialized with the page.
     */
    @FunctionalInterface
    public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    }

    private static final long serialVersionUID = 1L;

    private final List<T> fullList;
    private final SerializableFunction<T, String> textExtractor;
    private final IModel<String> filterModel;
    private String filterText = "";
    private List<T> filteredData = null;

    public FilteredListDataProvider(List<T> fullList, SerializableFunction<T, String> textExtractor) {
        this.fullList = fullList;
        this.textExtractor = textExtractor;
        this.filterModel = null;
    }

    /**
     * Constructor with filter model; filter text is read from the model on each access.
     */
    public FilteredListDataProvider(List<T> fullList, SerializableFunction<T, String> textExtractor, IModel<String> filterModel) {
        this.fullList = fullList;
        this.textExtractor = textExtractor;
        this.filterModel = filterModel;
    }

    public void setFilterText(String filterText) {
        if (filterText == null) {
            filterText = "";
        }
        if (!this.filterText.equals(filterText)) {
            this.filterText = filterText;
            this.filteredData = null;
        }
    }

    private String getFilterText() {
        if (filterModel != null) {
            String t = filterModel.getObject();
            return t != null ? t.trim() : "";
        }
        return filterText != null ? filterText : "";
    }

    private List<T> getFilteredData() {
        if (filteredData != null && filterModel == null) {
            return filteredData;
        }
        if (fullList == null) {
            filteredData = new ArrayList<>();
            return filteredData;
        }
        String currentFilter = getFilterText();
        if (currentFilter.isEmpty()) {
            filteredData = new ArrayList<>(fullList);
        } else {
            String lowerFilter = currentFilter.toLowerCase();
            filteredData = new ArrayList<>();
            for (T item : fullList) {
                if (item == null) continue;
                String text = textExtractor != null ? textExtractor.apply(item) : null;
                if (text != null && text.toLowerCase().contains(lowerFilter)) {
                    filteredData.add(item);
                }
            }
        }
        return filteredData;
    }

    @Override
    public Iterator<? extends T> iterator(long first, long count) {
        List<T> data = getFilteredData();
        return Utils.subList(data, first, first + count).iterator();
    }

    @Override
    public long size() {
        return getFilteredData().size();
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }

    @Override
    public void detach() {
        filteredData = null;
        if (filterModel != null) {
            filterModel.detach();
        }
    }

}
