package org.petapico;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class HList extends Panel {

	private static final long serialVersionUID = -8506288478189670570L;

	public HList(String id, List<IRI> items) {
		super(id);

		add(new ListView<IRI>("items", items) {

			private static final long serialVersionUID = -6222434246491371652L;

			protected void populateItem(ListItem<IRI> item) {
				item.add(new ThingItem("item", item.getModelObject()));
			}
			
		});
	}

}
