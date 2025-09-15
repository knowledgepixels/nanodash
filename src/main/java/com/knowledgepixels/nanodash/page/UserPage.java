package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * Page that shows a user profile, including their nanopubs and stats.
 */
public class UserPage extends NanodashPage {

    private static final long serialVersionUID = 1L;
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

    public UserPage(final PageParameters parameters) {
        super(parameters);

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

        final String displayName = User.getShortDisplayName(userIri);
        add(new Label("pagetitle", displayName + " (user) | nanodash"));
        add(new Label("username", displayName));

        add(new ExternalLink("fullid", userIriString, userIriString));

        add(new BookmarkablePageLink<Void>("showprofile", ProfilePage.class).setVisible(userIri.equals(NanodashSession.get().getUserIri())));

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
//				private static final long serialVersionUID = 1L;
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

        add(new BookmarkablePageLink<Void>("showchannel", ChannelPage.class, new PageParameters().add("id", userIriString)));

        final Map<String, String> params = new HashMap<>();
        final String queryName;
        if (pubkeyHashes.isEmpty()) {
            queryName = "get-latest-nanopubs-from-userid";
            params.put("userid", userIri.stringValue());
        } else {
            queryName = "get-latest-nanopubs-from-pubkeys";
            params.put("pubkeyhashes", pubkeyHashes);
            params.put("userid", userIri.stringValue());
        }
        ApiResponse response = ApiCache.retrieveResponse(queryName, params);
        if (response != null) {
            add(makeNanopubResultComponent("latestnanopubs", response));
        } else {
            add(new AjaxLazyLoadPanel<Component>("latestnanopubs") {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted while waiting for API response", ex);
                        }
                        if (!ApiCache.isRunning(queryName, params)) {
                            r = ApiCache.retrieveResponse(queryName, params);
                            if (r != null) break;
                        }
                    }
                    return makeNanopubResultComponent(markupId, r);
                }

//				@Override
//				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
//					super.onContentLoaded(content, target);
//					if (target.get() != null) target.get().appendJavaScript("updateElements();");
//				}

            });
        }

        ApiResponse acceptedNanopubList = ApiCache.retrieveResponse("get-accepted-nanopubs-by-author", "author", userIriString);
        if (acceptedNanopubList != null) {
            add(makeNanopubResultComponent("latestaccepted", acceptedNanopubList));
        } else {
            add(new AjaxLazyLoadPanel<Component>("latestaccepted") {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted while waiting for API response", ex);
                        }
                        if (!ApiCache.isRunning("get-accepted-nanopubs-by-author", "author", userIriString)) {
                            r = ApiCache.retrieveResponse("get-accepted-nanopubs-by-author", "author", userIriString);
                            if (r != null) break;
                        }
                    }
                    return makeNanopubResultComponent(markupId, r);
                }

//				@Override
//				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
//					super.onContentLoaded(content, target);
//					if (target.get() != null) target.get().appendJavaScript("updateElements();");
//				}

            });
        }
    }

    private static Component makeNanopubResultComponent(String markupId, ApiResponse response) {
        if (response.getData().isEmpty()) {
            return new Label(markupId, "(none)");
        } else {
            return NanopubResults.fromApiResponse(markupId, response);
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
