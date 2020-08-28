package org.petapico.nanobench;

import java.io.Serializable;
import java.net.URL;
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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import net.trustyuri.TrustyUriUtils;

public class ExplorePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static final int maxDetailTableCount = 1000000;

	public ExplorePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final String id = parameters.get("id").toString();
		add(new ExternalLink("urilink", id, IriItem.getShortNameFromURI(id)));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", id);
		try {
			Nanopub np = null;
			if (TrustyUriUtils.isPotentialTrustyUri(id)) {
				try {
					np = new NanopubImpl(new URL(id));
				} catch (Exception ex) {
					// wasn't a known nanopublication
				}	
			}
			List<ApiResponseEntry> usageResponse = ApiAccess.getAll("get_uri_usage", nanopubParams).getData();
			int subjCount = Integer.valueOf(usageResponse.get(0).get("subj"));
			int relCount = Integer.valueOf(usageResponse.get(0).get("pred"));
			int objCount = Integer.valueOf(usageResponse.get(0).get("obj"));
			int classCount = Integer.valueOf(usageResponse.get(0).get("class"));
			int indCount = subjCount + objCount - classCount;
			if (np == null) {
				add(new Label("name", "Term"));
				add(new Label("nanopub", ""));
				add(new Label("counts", "This term is used <strong>" + indCount + "</strong> times as individual,\n" + 
						"<strong>" + classCount + "</strong> times as class, and\n" + 
						"<strong>" + relCount + "</strong> times as relation.").setEscapeModelStrings(false));
			} else {
				add(new Label("name", "Nanopublication"));
				add(new NanopubItem("nanopub", new NanopubElement(np), true));
				add(new Label("counts", ""));
			}

			Map<String,String> params = new HashMap<>();
			params.put("graphpred", Nanopub.HAS_ASSERTION_URI.stringValue());
			params.put("ref", id);
			List<IColumn<ApiResponseEntry,String>> columns = new ArrayList<>();
			DataProvider dp;
			if (subjCount + relCount + objCount < maxDetailTableCount) {
				ApiResponse dataResponse = ApiAccess.getAll("find_signed_nanopubs_with_uri", params);
				columns.add(new Column("Nanopublication", "np", id));
				columns.add(new Column("Subject", "subj", id));
				columns.add(new Column("Predicate", "pred", id));
				columns.add(new Column("Object", "obj", id));
				columns.add(new Column("Published By", "pubkey", id));
				columns.add(new Column("Published On", "date", id));
				dp = new DataProvider(dataResponse.getData());
				add(new Label("message", ""));
			} else {
				dp = new DataProvider();
				add(new Label("message", "This term is too frequent to show a detailed table."));
			}
			DataTable<ApiResponseEntry,String> table = new DataTable<>("datatable", columns, dp, 100);
			table.addBottomToolbar(new NavigationToolbar(table));
			table.addBottomToolbar(new NoRecordsToolbar(table));
			table.addTopToolbar(new HeadersToolbar<String>(table, dp));
			table.setVisible(subjCount + relCount + objCount < maxDetailTableCount);
			add(table);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
				String s = "(unknown)";
				User user = User.getUserForPubkey(value);
				if (user != null) s = user.getShortDisplayName();
				cellItem.add(new Label(componentId, s));
			} else if (value.matches("(https?|file)://.+")) {
				cellItem.add(new Link(componentId, value));
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
			for (ApiResponseEntry r : data) {
				if (r.getAsBoolean("retracted") || r.getAsBoolean("superseded")) continue;
				this.data.add(r);
			}
		}

		@Override
		public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
			List<ApiResponseEntry> copy = new ArrayList<>(data);
			ApiResponseComparator comparator = new ApiResponseComparator(sortState.getSort());
			Collections.sort(copy, comparator);
			return copy.subList((int)first, (int)(first + count)).iterator();
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
