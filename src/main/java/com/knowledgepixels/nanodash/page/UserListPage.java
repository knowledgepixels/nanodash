package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * Page that lists all users and groups.
 */
public class UserListPage extends NanodashPage {

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

        add(new ItemListPanel<IRI>(
                "topcreators",
                "Most Active Nanopublishers Last Month",
                new QueryRef("get-top-creators-last30d"),
                (apiResponse) -> {
                    List<IRI> users = new ArrayList<>();
                    for (ApiResponseEntry e : apiResponse.getData()) {
                        users.add(Utils.vf.createIRI(e.get("userid")));
                    }
                    return users;
                },
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new ItemListPanel<IRI>(
                "latestusers",
                "Latest New Users",
                new QueryRef("get-latest-users"),
                (apiResponse) -> {
                    List<IRI> users = new ArrayList<>();
                    for (ApiResponseEntry e : apiResponse.getData()) {
                        users.add(Utils.vf.createIRI(e.get("user")));
                    }
                    return users;
                },
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new ItemListPanel<IRI>(
                "approved-users",
                "Approved Users",
                User.getUsers(true),
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new ItemListPanel<IRI>(
                "other-users",
                "Non-Approved Users",
                User.getUsers(false),
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

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
