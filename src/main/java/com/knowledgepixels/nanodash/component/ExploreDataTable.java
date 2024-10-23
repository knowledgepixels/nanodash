package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
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

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;

public class ExploreDataTable extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ExploreDataTable(String id, String ref) {
		super(id);
		Map<String,String> params = new HashMap<>();
//		params.put("graphpred", Nanopub.HAS_ASSERTION_URI.stringValue());
		params.put("ref", ref);
		List<IColumn<ApiResponseEntry,String>> columns = new ArrayList<>();
		DataProvider dp;
		ApiResponse dataResponse = null;
		try {
			dataResponse = QueryApiAccess.get("find-uri-references", params);
			columns.add(new Column("Nanopublication", "np", ref));
			columns.add(new Column("Part", "graphpred", ref));
			columns.add(new Column("Subject", "subj", ref));
			columns.add(new Column("Predicate", "pred", ref));
			columns.add(new Column("Object", "obj", ref));
			columns.add(new Column("Published By", "pubkey", ref));
			columns.add(new Column("Published On", "date", ref));
			dp = new DataProvider(filterData(dataResponse.getData(), ref));
			DataTable<ApiResponseEntry,String> table = new DataTable<>("datatable", columns, dp, 100);
			table.addBottomToolbar(new NavigationToolbar(table));
			table.addBottomToolbar(new NoRecordsToolbar(table));
			table.addTopToolbar(new HeadersToolbar<String>(table, dp));
			add(table);
			add(new Label("message", "").setVisible(false));
		} catch (Exception ex) {
			ex.printStackTrace();
			add(new Label("datatable", "").setVisible(false));
			add(new Label("message", "Could not load data table."));
		}
	}

	
	private List<ApiResponseEntry> filterData(List<ApiResponseEntry> data, String nanopubUri) {
		Nanopub np = Utils.getAsNanopub(nanopubUri);
		if (np == null) return data;
		List<ApiResponseEntry> filteredList = new ArrayList<>(data);
		for (ApiResponseEntry e : data) {
			if (nanopubUri.equals(e.get("np"))) filteredList.remove(e);
		}
		return filteredList;
	}


	private class Column extends AbstractColumn<ApiResponseEntry,String> {

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
			} else if (key.equals("pubkey")) {
				cellItem.add(new Label(componentId, User.getShortDisplayName(null, value)));
			} else if (value.matches("https?://.+")) {
				cellItem.add(new NanodashLink(componentId, value));
			} else {
				cellItem.add(new Label(componentId, value));
			}
		}

	}

	
	private class DataProvider implements ISortableDataProvider<ApiResponseEntry,String> {

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

}
