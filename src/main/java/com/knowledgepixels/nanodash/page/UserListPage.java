package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.QueryResultListBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.util.List;
import java.util.stream.Collectors;

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

        View topCreatorsView = View.get("https://w3id.org/np/RACcywnbkn6OAd_6E25qZL9-vdO-UwmpO1vXVWzNWJYLo/top-creators-last-30days");
        QueryRef tcQueryRef = new QueryRef(topCreatorsView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("topcreators", tcQueryRef, new ViewDisplay(topCreatorsView)).build());

        View latestUsersView = View.get("https://w3id.org/np/RAtwNLvsJbz3pk_UxdKSydsghbX6D_60ivTZpDQhK-9zA/latest-users");
        QueryRef luQueryRef = new QueryRef(latestUsersView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("latestusers", luQueryRef, new ViewDisplay(latestUsersView)).build());

        add(new ItemListPanel<IRI>(
                "approved-human-users",
                "👤 Human Users",
                User.getUsers(true).stream().filter(iri -> !IndividualAgent.isSoftware(iri)).collect(Collectors.toList()),
                (userIri) -> new ItemListElement("item", UserPage.class, new PageParameters().set("id", userIri), User.getShortDisplayName(userIri)),
                User::getShortDisplayName
        ));

        add(new ItemListPanel<IRI>(
                "approved-software-agents",
                "🤖 Software Agents",
                User.getUsers(true).stream().filter(IndividualAgent::isSoftware).collect(Collectors.toList()),
                (userIri) -> new ItemListElement("item", UserPage.class, new PageParameters().set("id", userIri), User.getShortDisplayName(userIri)),
                User::getShortDisplayName
        ));

        add(new ItemListPanel<IRI>(
                "other-users",
                "❓ Non-Approved Users",
                User.getUsers(false),
                (userIri) -> new ItemListElement("item", UserPage.class, new PageParameters().set("id", userIri), User.getShortDisplayName(userIri)),
                User::getShortDisplayName
        ));

        add(new ExternalLink("approve", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RA6TVVSnZChEwyxjvFDNAujk1i8sSPnQx60ZQjldtiDkw&template-version=latest", "approve somebody else..."));
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
