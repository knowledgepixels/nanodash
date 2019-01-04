package org.petapico;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;

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
//        add(new DataView<String>("users", new ListDataProvider<String>(Utils.getUsers())) {
//			@Override
//        	protected void populateItem(Item<String> item) {
//        		item.add(new Label("userid", item.getModelObject()));
//        	}
//        });
        RepeatingView userIdItems = new RepeatingView("userid");
        for (String u : Utils.getUsers()) {
        	userIdItems.add(new Label(userIdItems.newChildId(), u));
        }
        add(userIdItems);
    }
}
