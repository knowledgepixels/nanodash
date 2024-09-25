package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

public class UserPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/user";

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

		for (String pk : User.getPubkeys(userIri, null)) {
			pubkeyHashes += " " + Utils.createSha256HexHash(pk);
		}
		if (!pubkeyHashes.isEmpty()) pubkeyHashes = pubkeyHashes.substring(1);

		String pageType = "users";
		add(new TitleBar("titlebar", this, pageType));

		final String displayName = User.getShortDisplayName(userIri);
		add(new Label("pagetitle", displayName + " (user) | nanodash"));
		add(new Label("username", displayName));

		add(new ExternalLink("fullid", userIriString, userIriString));

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
//							ex.printStackTrace();
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

		final Map<String,String> params = new HashMap<>();
		final String queryName;
		if (pubkeyHashes.isEmpty()) {
			queryName = "get-latest-nanopubs-from-userid";
			params.put("userid", userIri.stringValue());
		} else {
			queryName = "get-latest-nanopubs-from-pubkeys";
			params.put("pubkeyhashes", pubkeyHashes);
			params.put("userid", userIri.stringValue());
		} 
		List<ApiResponseEntry> response = ApiCache.retrieveNanopubList(queryName, params);
		if (response != null) {
			add(makeNanopubResultComponent("latestnanopubs", response));
		} else {
			add(new AjaxLazyLoadPanel<Component>("latestnanopubs") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<ApiResponseEntry> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning(queryName, params)) {
							l = ApiCache.retrieveNanopubList(queryName, params);
							if (l != null) break;
						}
					}
					return makeNanopubResultComponent(markupId, l);
				}
	
//				@Override
//				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
//					super.onContentLoaded(content, target);
//					if (target.get() != null) target.get().appendJavaScript("updateElements();");
//				}
	
			});
		}

		List<ApiResponseEntry> acceptedNanopubList = ApiCache.retrieveNanopubList("get-accepted-nanopubs-by-author", "author", userIriString);
		if (acceptedNanopubList != null) {
			add(makeNanopubResultComponent("latestaccepted", acceptedNanopubList));
		} else {
			add(new AjaxLazyLoadPanel<Component>("latestaccepted") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<ApiResponseEntry> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning("get-accepted-nanopubs-by-author", "author", userIriString)) {
							l = ApiCache.retrieveNanopubList("get-accepted-nanopubs-by-author", "author", userIriString);
							if (l != null) break;
						}
					}
					return makeNanopubResultComponent(markupId, l);
				}
	
//				@Override
//				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
//					super.onContentLoaded(content, target);
//					if (target.get() != null) target.get().appendJavaScript("updateElements();");
//				}
	
			});
		}
	}

	private static Component makeNanopubResultComponent(String markupId, List<ApiResponseEntry> response) {
		if (response.isEmpty()) {
			return new Label(markupId, "(none)");
		} else {
			return NanopubResults.fromApiResponse(markupId, makeShortList(response));
		}
	}

	private static List<ApiResponseEntry> makeShortList(List<ApiResponseEntry> list) {
		List<ApiResponseEntry> shortList = new ArrayList<>();
		for (ApiResponseEntry e : list) {
			shortList.add(e);
			if (shortList.size() == 5) break;
		}
		return shortList;
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
