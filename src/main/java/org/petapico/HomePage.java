package org.petapico;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
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

			private static final long serialVersionUID = -7900012913964111340L;

			@Override
        	protected void populateItem(Item<String> item) {
        		item.add(new Label("userid", item.getModelObject()));
        	}

        });
    }
}
