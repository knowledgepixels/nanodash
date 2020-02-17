package org.petapico;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.SimpleTimestampPattern;

public class TemplateList extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TemplateList(String id) {
		super(id);

		add(new DataView<Template>("list", new ListDataProvider<Template>(Template.getTemplates())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Template> item) {
				PageParameters params = new PageParameters();
				params.add("template", item.getModelObject().getId());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("link", PublishPage.class, params);
				l.add(new Label("name", item.getModelObject().getLabel()));
				String timeString = "undated";
				Calendar c = SimpleTimestampPattern.getCreationTime(item.getModelObject().getNanopub());
				if (c != null) {
					timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
				}
				l.add(new Label("timestamp", timeString));
				item.add(l);
			}

		});
	}

}
