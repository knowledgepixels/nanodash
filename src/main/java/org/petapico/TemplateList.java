package org.petapico;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TemplateList extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TemplateList(String id) {
		super(id);
		List<String> templateIds = new ArrayList<>();
		templateIds.add("RAqCjmtXsi8vJ8j7cFLiC6FMkEVZ-8gFbjHwmwRQ-sdbo");

		add(new DataView<String>("list", new ListDataProvider<String>(templateIds)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				PageParameters params = new PageParameters();
				params.add("template", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("link", PublishPage.class, params);
				l.add(new Label("text", item.getModelObject()));
				item.add(l);
			}

		});
	}

}
