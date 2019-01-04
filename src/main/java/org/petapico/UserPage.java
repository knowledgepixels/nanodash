package org.petapico;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;


public class UserPage extends WebPage {

    public UserPage(final PageParameters parameters) {
    	add(new Label("userid", parameters.getString("id")));

    	Map<String,String> p = new HashMap<>();
    	p.put("user", parameters.getString("id"));
    	List<String> pubkeys = ApiAccess.getAll("get_publickeys_for_user", p, 0);
        add(new DataView<String>("pubkeys", new ListDataProvider<String>(pubkeys)) {

			private static final long serialVersionUID = 1L;

			@Override
        	protected void populateItem(Item<String> item) {
				String s = item.getModelObject();
				item.add(new Label("pubkey", s.substring(0, 10) + "..." + s.substring(s.length() - 20)));
        	}

        });
    }

}
