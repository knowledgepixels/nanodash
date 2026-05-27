package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.QueryResultItemListBuilder;
import com.knowledgepixels.nanodash.component.QueryResultListBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

/**
 * Page that lists all users and groups.
 */
public class UserListPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/userlist";

    private static final String HUMAN_USERS_VIEW = "https://w3id.org/np/RAeDwLoelA43CfcetS7LVQOAgQuDCw-Yf5naRYdmCmCXs/human-users-view";
    private static final String SOFTWARE_AGENTS_VIEW = "https://w3id.org/np/RAr4qrDh77rNoRcodAoGDOJLEECu3sBvrUJPAhuK73e1c/software-agents-view";
    private static final String NON_APPROVED_USERS_VIEW = "https://w3id.org/np/RA8Xkr-SnsRqu0RBGExZ3Ms8J1TviL_1bRQVRnymaWafw/non-approved-users-view";

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
        add(QueryResultListBuilder.create("topcreators", tcQueryRef, new ViewDisplay(topCreatorsView).withDisplayWidth(6)).build());

        View latestUsersView = View.get("https://w3id.org/np/RAtwNLvsJbz3pk_UxdKSydsghbX6D_60ivTZpDQhK-9zA/latest-users");
        QueryRef luQueryRef = new QueryRef(latestUsersView.getQuery().getQueryId());
        add(QueryResultListBuilder.create("latestusers", luQueryRef, new ViewDisplay(latestUsersView).withDisplayWidth(6)).build());

        View humanUsersView = View.get(HUMAN_USERS_VIEW);
        add(QueryResultItemListBuilder.create("approved-human-users",
                new QueryRef(humanUsersView.getQuery().getQueryId()), new ViewDisplay(humanUsersView)).build());

        View softwareAgentsView = View.get(SOFTWARE_AGENTS_VIEW);
        add(QueryResultItemListBuilder.create("approved-software-agents",
                new QueryRef(softwareAgentsView.getQuery().getQueryId()), new ViewDisplay(softwareAgentsView)).build());

        View nonApprovedUsersView = View.get(NON_APPROVED_USERS_VIEW);
        add(QueryResultItemListBuilder.create("other-users",
                new QueryRef(nonApprovedUsersView.getQuery().getQueryId()), new ViewDisplay(nonApprovedUsersView)).build());

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
