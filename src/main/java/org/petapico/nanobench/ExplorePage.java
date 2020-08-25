package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

public class ExplorePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public ExplorePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final String id = parameters.get("id").toString();
		ExternalLink link = new ExternalLink("urilink", id);
		link.add(new Label("urilinktext", id));
		add(link);

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", id);
		try {
			List<ApiResponseEntry> nanopubResults = ApiAccess.getAll("get_uri_usage", nanopubParams).getData();
			int subjCount = Integer.valueOf(nanopubResults.get(0).get("subj"));
			int relCount = Integer.valueOf(nanopubResults.get(0).get("pred"));
			int objCount = Integer.valueOf(nanopubResults.get(0).get("obj"));
			int classCount = Integer.valueOf(nanopubResults.get(0).get("class"));
			int indCount = subjCount + objCount - classCount;
			add(new Label("indcount", indCount));
			add(new Label("classcount", classCount));
			add(new Label("relcount", relCount));

			Map<String,String> params = new HashMap<>();
			params.put("graphpred", Nanopub.HAS_ASSERTION_URI.stringValue());
			params.put("pred", id);
			ApiResponse dataResponse = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params);
			final List<IColumn<ApiResponseEntry,String>> columns = new ArrayList<>();
			for (final String s : new String[] {"subj", "obj", "date"}) {
				columns.add(new AbstractColumn<ApiResponseEntry,String>(new Model<String>(s)) {

					private static final long serialVersionUID = 1L;

					@Override
					public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
						cellItem.add(new Label(componentId, rowModel.getObject().get(s)));
					}
					
				});
			}
			columns.add(new AbstractColumn<ApiResponseEntry,String>(new Model<String>("user")) {

				private static final long serialVersionUID = 1L;

				@Override
				public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
					String s = "(unknown)";
					String pubkey = rowModel.getObject().get("pubkey");
					User user = User.getUserForPubkey(pubkey);
					if (user != null) s = user.getShortDisplayName();
					cellItem.add(new Label(componentId, s));
				}
				
			});

			DataProvider dp = new DataProvider(dataResponse.getData());
			DefaultDataTable<ApiResponseEntry,String> table = new DefaultDataTable<>("datatable", columns, dp, 100);
			add(table);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private class DataProvider implements ISortableDataProvider<ApiResponseEntry,String> {

		private static final long serialVersionUID = 1L;

		private List<ApiResponseEntry> data = new ArrayList<>();
		private SingleSortState<String> sortState = new SingleSortState<>();

		public DataProvider(List<ApiResponseEntry> data) {
			for (ApiResponseEntry r : data) {
				if (r.getAsBoolean("retracted") || r.getAsBoolean("superseded")) continue;
				this.data.add(r);
			}
		}

		@Override
		public Iterator<? extends ApiResponseEntry> iterator(long first, long count) {
			return data.subList((int)first, (int)(first + count)).iterator();
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

}
