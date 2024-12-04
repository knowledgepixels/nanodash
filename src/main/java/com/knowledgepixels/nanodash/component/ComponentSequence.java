package com.knowledgepixels.nanodash.component;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

public class ComponentSequence extends Panel {

	private static final long serialVersionUID = 1L;

	public ComponentSequence(String id, final String separator, final List<Component> components) {
		super(id);
		add(new ListView<Component>("components", components) {
			
			private static final long serialVersionUID = 1L;
			private boolean isFirst = true;

			@Override
			protected void populateItem(ListItem<Component> item) {
				if (isFirst) {
					item.add(new Label("separator").setVisible(false));
					isFirst = false;
				} else {
					item.add(new Label("separator", separator));
				}
				item.add(item.getModelObject());
			}

		});
	}

}
