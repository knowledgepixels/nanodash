package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Page that shows a user profile, including their nanopubs and stats.
 */
public class UserPage extends NanodashPage {

    private static final Logger logger = LoggerFactory.getLogger(UserPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/user";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private IRI userIri;
    private String pubkeyHashes = "";

    /**
     * Constructor for UserPage.
     *
     * @param parameters Page parameters, must include "id" with the user IRI.
     */
    public UserPage(final PageParameters parameters) {
        super(parameters);
        setOutputMarkupId(true);

        if (parameters.get("id") == null) throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
        final String userIriString = parameters.get("id").toString();
        userIri = Utils.vf.createIRI(userIriString);
        //NanodashSession session = NanodashSession.get();

        for (String pk : User.getPubkeyhashes(userIri, null)) {
            pubkeyHashes += " " + pk;
        }
        if (!pubkeyHashes.isEmpty()) pubkeyHashes = pubkeyHashes.substring(1);

        String pageType = "users";
        add(new TitleBar("titlebar", this, pageType));

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        final String displayName = User.getShortDisplayName(userIri);
        add(new Label("pagetitle", displayName + " (user) | nanodash"));
        EmptyPanel userIcon = new EmptyPanel("userIcon");
        userIcon.add(AttributeModifier.replace("class", IndividualAgent.isSoftware(userIri) ? "bot-icon" : "user-icon"));
        add(userIcon);
        add(new Label("username", displayName));

        add(new BookmarkablePageLink<Void>("fullid", ExplorePage.class, parameters.set("label", displayName)).setBody(Model.of(userIriString)));

        add(new BookmarkablePageLink<Void>("showprofile", ProfilePage.class).setVisible(userIri.equals(NanodashSession.get().getUserIri())));
        add(new BookmarkablePageLink<Void>("showchannel", ListPage.class, new PageParameters().add("userid", userIriString)));


//		final Map<String,String> statsParams = new HashMap<>();
//		final String statsQueryName;
//		if (pubkeyHashes.isEmpty()) {
//			statsQueryName = "get-user-stats-from-userid";
//			statsParams.put("userid", userIriString);
//		} else {
//			statsQueryName = "get-user-stats-from-pubkeys";
//			statsParams.put("userid", userIriString);
//			statsParams.put("pubkeyhashes", pubkeyHashes);
//		} 
//		Map<String,String> statsMap = ApiCache.retrieveMap(statsQueryName, statsParams);
//		if (statsMap != null) {
//			add(new StatsPanel("stats", userIriString, pubkeyHashes, statsMap));
//		} else {
//			add(new AjaxLazyLoadPanel<Component>("stats") {
//	
//				@Override
//				public Component getLazyLoadComponent(String markupId) {
//					Map<String,String> m = null;
//					while (true) {
//						try {
//							Thread.sleep(500);
//						} catch (InterruptedException ex) {
//							logger.error();
//						}
//						if (!ApiCache.isRunning(statsQueryName, statsParams)) {
//							m = ApiCache.retrieveMap(statsQueryName, statsParams);
//							if (m != null) break;
//						}
//					}
//					return new StatsPanel(markupId, userIriString, pubkeyHashes, m);
//				}
//
//				@Override
//				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
//					super.onContentLoaded(content, target);
//					if (target.get() != null) target.get().appendJavaScript("updateElements();");
//				}
//	
//			});
//		}

        final List<AbstractLink> viewButtons = new ArrayList<>();
        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .set("template", "https://w3id.org/np/RAQhTCHtfzGCj1YiE1LualWcZjg3thlRiquFWUE14UF-g")
                .set("template-version", "latest")
                .set("param_resource", userIriString)
                .set("param_appliesToResource", userIriString)
                .set("context", userIriString)
                .set("refresh-upon-publish", userIriString)
        );
        addViewButton.setBody(Model.of("+ view display"));
        viewButtons.add(addViewButton);

        IndividualAgent individualAgent = IndividualAgent.get(userIriString);
        if (individualAgent.isDataInitialized()) {
            boolean empty = individualAgent.getTopLevelViewDisplays().isEmpty();
            if (empty) {
                add(new WebMarkupContainer("views").setVisible(false));
            } else {
                add(new ViewList("views", individualAgent, null, null, null, individualAgent, viewButtons, false));
            }
            add(new WebMarkupContainer("unconfigured-notice").setVisible(empty));
            if (empty) {
                ViewDisplay defaultViewDisplay = new ViewDisplay(View.get("https://w3id.org/np/RAjYa33Z3H1whRl486AW3LMnV11WQqkTqvuHROhKbmtlE/latest-nanopubs-example"));
                add(new ViewList("latestnanopubsview", individualAgent, List.of(defaultViewDisplay), individualAgent, viewButtons));
            } else {
                add(new EmptyPanel("latestnanopubsview").setVisible(false));
            }
        } else {
            final WebMarkupContainer unconfiguredNotice = new WebMarkupContainer("unconfigured-notice");
            unconfiguredNotice.setVisible(false);
            unconfiguredNotice.setOutputMarkupPlaceholderTag(true);
            add(unconfiguredNotice);

            ViewDisplay defaultViewDisplay = new ViewDisplay(View.get("https://w3id.org/np/RAjYa33Z3H1whRl486AW3LMnV11WQqkTqvuHROhKbmtlE/latest-nanopubs-example"));
            final ViewList latestNanopubsView = new ViewList("latestnanopubsview", individualAgent, List.of(defaultViewDisplay), individualAgent, viewButtons);
            latestNanopubsView.setVisible(false);
            latestNanopubsView.setOutputMarkupPlaceholderTag(true);
            add(latestNanopubsView);

            add(new AjaxLazyLoadPanel<Component>("views") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    if (individualAgent.getTopLevelViewDisplays().isEmpty()) {
                        return new ViewList(markupId, individualAgent, null, null, null, null, null, false);
                    }
                    return new ViewList(markupId, individualAgent, null, null, null, individualAgent, viewButtons, false);
                }

                @Override
                protected boolean isContentReady() {
                    return individualAgent.isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                }

                @Override
                protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
                    super.onContentLoaded(content, target);
                    target.ifPresent(t -> {
                        boolean isEmpty = individualAgent.getTopLevelViewDisplays().isEmpty();
                        if (isEmpty) {
                            t.appendJavaScript("document.getElementById('" + getMarkupId() + "').remove();");
                        }
                        unconfiguredNotice.setVisible(isEmpty);
                        t.add(unconfiguredNotice);
                        latestNanopubsView.setVisible(isEmpty);
                        t.add(latestNanopubsView);
                    });
                }

            });
        }
    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
