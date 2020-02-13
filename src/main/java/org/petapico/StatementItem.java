package org.petapico;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public StatementItem(String id, List<IRI> items, final PublishPage page) {
		super(id);

		add(new ListView<IRI>("items", items) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<IRI> item) {
				item.add(new ValueItem("item", item.getModelObject(), page));
			}

		});
	}

}
