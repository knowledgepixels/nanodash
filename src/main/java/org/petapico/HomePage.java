package org.petapico;

import java.util.List;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class HomePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private TextField<String> searchField;

	public HomePage(final PageParameters parameters) {
		Form<?> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				String searchText = searchField.getModelObject().trim();
				PageParameters params = new PageParameters();
				params.add("query", searchText);
				setResponsePage(FreeTextSearchPage.class, params);
			}
		};
		add(form);
		form.add(searchField = new TextField<String>("search", Model.of("")));

		List<String> users = ApiAccess.getAll("get_all_users", null, 0);
		add(new DataView<String>("users", new ListDataProvider<String>(users)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				PageParameters params = new PageParameters();
				params.add("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", item.getModelObject()));
				item.add(l);
			}

		});
	}

}
