package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.UserList;

/**
 * Page that lists all users and groups.
 */
public class UserListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/userlist";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the user list page.
     *
     * @param parameters the page parameters
     */
    public UserListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "users"));

//		final List<Group> groupList = new ArrayList<Group>(Group.getGroups());
//		add(new DataView<Group>("groups", new ListDataProvider<Group>(groupList)) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void populateItem(Item<Group> item) {
//				Group g = item.getModelObject();
//				PageParameters params = new PageParameters();
//				params.add("id", g.getIri());
//				BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("grouplink", GroupPage.class, params);
//				l.add(new Label("linktext", g.getName()));
//				item.add(l);
//			}
//
//		});

        final HashMap<String, String> noParams = new HashMap<>();

        final String uQueryName = "get-top-creators-last30d";
        ApiResponse rResponse = ApiCache.retrieveResponse(uQueryName, noParams);
        if (rResponse != null) {
            add(new UserList("topcreators", rResponse, "userid", false));
        } else {
            add(new ApiResultComponent("topcreators", uQueryName, noParams) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new UserList(markupId, response, "userid", false);
                }
            });
        }

        final String aQueryName = "get-top-authors";
        ApiResponse aResponse = ApiCache.retrieveResponse(aQueryName, noParams);
        if (aResponse != null) {
            add(new UserList("topauthors", aResponse, "author", false));
        } else {
            add(new ApiResultComponent("topauthors", aQueryName, noParams) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new UserList(markupId, response, "author", false);
                }
            });
        }

        final List<IRI> userList = User.getUsers(true);

        add(new UserList("approved-users", userList, true));
        add(new UserList("other-users", User.getUsers(false), true));

        add(new ExternalLink("approve", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RA6TVVSnZChEwyxjvFDNAujk1i8sSPnQx60ZQjldtiDkw&template-version=latest", "approve somebody else"));
        //add(new ExternalLink("newgroup", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RAJz6w5cvlsFGkCDtWOUXt2VwEQ3tVGtPdy3atPj_DUhk&template-version=latest", "new group"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
