package org.petapico.nanobench;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

public class NanopubResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubResults(String id, List<NanopubElement> nanopubs) {
		super(id);
		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject(), true));
			}

		});
	}

}
