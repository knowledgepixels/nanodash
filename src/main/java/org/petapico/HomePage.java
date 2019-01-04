package org.petapico;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

/**
 * Homepage
 */
public class HomePage extends WebPage {

	private static final long serialVersionUID = 1L;

	// TODO Add any page properties or variables here

    /**
	 * Constructor that is invoked when page is invoked without a session.
	 * 
	 * @param parameters
	 *            Page parameters
	 */
    public HomePage(final PageParameters parameters) {
        add(new DataView<String>("users", new ListDataProvider<String>(Utils.getUsers())) {

			private static final long serialVersionUID = 1L;

			@Override
        	protected void populateItem(Item<String> item) {
				PageParameters params = new PageParameters();
				params.put("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", item.getModelObject()));
				item.add(l);
        	}

        });
    }
}
