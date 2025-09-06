package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.UserPage;

/**
 * A panel that displays a list of users with links to their user pages.
 * Optionally, it can also display notes associated with each user.
 */
public class UserList extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for UserList.
     *
     * @param id    the component id
     * @param users a list of user IRI objects
     * @param notes a map of user IRI to notes (optional)
     */
    public UserList(String id, List<IRI> users, Map<IRI, String> notes) {
        super(id);
        setOutputMarkupId(true);
        init(users, notes);
    }

    /**
     * Constructor for UserList with a list of user IRI objects.
     *
     * @param id    the component id
     * @param users a list of user IRI objects
     */
    public UserList(String id, List<IRI> users) {
        super(id);
        setOutputMarkupId(true);
        init(users, null);
    }

    /**
     * Constructor for UserList that initializes from an ApiResponse.
     *
     * @param id        the component id
     * @param resp      the ApiResponse containing user data
     * @param userIdKey the key in the ApiResponse entries that contains the user IRI
     */
    public UserList(String id, ApiResponse resp, String userIdKey) {
        super(id);
        setOutputMarkupId(true);
        List<IRI> users = new ArrayList<>();
        for (ApiResponseEntry e : resp.getData()) {
            users.add(Utils.vf.createIRI(e.get(userIdKey)));
        }
        init(users, null);
    }

    private void init(List<IRI> users, Map<IRI, String> notes) {
        DataView<IRI> dataView = new DataView<>("userlist", new ListDataProvider<IRI>(users)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<IRI> item) {
                IRI userIri = item.getModelObject();
                PageParameters params = new PageParameters();
                params.add("id", userIri);
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("userlink", UserPage.class, params);
                l.add(new Label("linktext", User.getShortDisplayName(userIri)));
                item.add(l);
                if (notes != null && notes.containsKey(userIri)) {
                    item.add(new Label("notes", notes.get(userIri)));
                } else {
                    item.add(new Label("notes"));
                }
            }

        };
        dataView.setItemsPerPage(10);
        dataView.setOutputMarkupId(true);
        add(dataView);
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        pagingNavigator.setVisible(dataView.getPageCount() > 1);
        pagingNavigator.setOutputMarkupId(true);
        add(pagingNavigator);
    }

}
