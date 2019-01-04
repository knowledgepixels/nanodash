package org.petapico;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;


public class UserPage extends WebPage {

    public UserPage(final PageParameters parameters) {
    	add(new Label("userid", parameters.getString("id")));
    }

}
