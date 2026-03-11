package com.knowledgepixels.nanodash.component;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Component for displaying CONSTRUCT query results as a subject/predicate/object table.
 */
public class QueryResultRdf extends Panel {

    public QueryResultRdf(String id, org.eclipse.rdf4j.model.Model rdfModel) {
        super(id);

        List<String[]> rows = new ArrayList<>();
        for (org.eclipse.rdf4j.model.Statement st : rdfModel) {
            rows.add(new String[]{
                st.getSubject().stringValue(),
                st.getPredicate().stringValue(),
                st.getObject().stringValue()
            });
        }

        List<AbstractColumn<String[], String>> columns = new ArrayList<>();
        columns.add(new TripleColumn("Subject", 0));
        columns.add(new TripleColumn("Predicate", 1));
        columns.add(new TripleColumn("Object", 2));

        DataTable<String[], String> table = new DataTable<>("table", columns, new TripleDataProvider(rows), 20);
        table.addBottomToolbar(new AjaxNavigationToolbar(table));
        table.addBottomToolbar(new NoRecordsToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, null));
        add(table);
    }

    private static class TripleDataProvider implements ISortableDataProvider<String[], String> {

        private final List<String[]> rows;
        private final SingleSortState<String> sortState = new SingleSortState<>();

        TripleDataProvider(List<String[]> rows) {
            this.rows = rows;
        }

        @Override
        public Iterator<String[]> iterator(long first, long count) {
            int f = (int) first;
            int t = (int) Math.min(first + count, rows.size());
            return rows.subList(f, t).iterator();
        }

        @Override
        public long size() {
            return rows.size();
        }

        @Override
        public IModel<String[]> model(String[] object) {
            return Model.of(object);
        }

        @Override
        public ISortState<String> getSortState() {
            return sortState;
        }

        @Override
        public void detach() {
        }

    }

    private static class TripleColumn extends AbstractColumn<String[], String> {

        private final int index;

        TripleColumn(String title, int index) {
            super(new Model<>(title));
            this.index = index;
        }

        @Override
        public void populateItem(Item<ICellPopulator<String[]>> cellItem, String componentId, IModel<String[]> rowModel) {
            String value = rowModel.getObject()[index];
            if (value.matches("https?://.+")) {
                cellItem.add(new NanodashLink(componentId, value));
            } else {
                cellItem.add(new Label(componentId, value));
            }
        }

    }

}
