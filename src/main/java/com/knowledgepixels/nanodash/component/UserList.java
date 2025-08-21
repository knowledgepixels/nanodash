package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.UserPage;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserList extends Panel {

    private static final long serialVersionUID = 1L;

    public UserList(String id, List<IRI> users, Map<IRI, String> notes) {
        super(id);
        init(users, notes);
    }

    public UserList(String id, List<IRI> users) {
        super(id);
        init(users, null);
    }

    public UserList(String id, ApiResponse resp, String userIdKey) {
        super(id);
        List<IRI> users = new ArrayList<>();
        for (ApiResponseEntry e : resp.getData()) {
            users.add(Utils.vf.createIRI(e.get(userIdKey)));
        }
        init(users, null);
    }

    private void init(List<IRI> users, Map<IRI, String> notes) {
        add(new DataView<IRI>("userlist", new ListDataProvider<IRI>(users)) {

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

        });
    }

}
