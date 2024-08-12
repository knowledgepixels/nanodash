package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

public class UserPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	private String pubkeyHashes = "";

	public static final String MOUNT_PATH = "/user";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private IRI userIri;
	
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

		add(new BookmarkablePageLink<Void>("showchannel", ChannelPage.class, new PageParameters().add("id", userIriString)));

		List<NanopubElement> nanopubList = ApiCache.retrieveNanopubList("get-latest-nanopubs-from-pubkeys", pubkeyHashes);
		if (nanopubList != null) {
			add(makeNanopubResultComponent("latestnanopubs", nanopubList));
		} else {
			add(new AjaxLazyLoadPanel<Component>("latestnanopubs") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<NanopubElement> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning(pubkeyHashes)) {
							l = ApiCache.retrieveNanopubList("get-latest-nanopubs-from-pubkeys", pubkeyHashes);
							if (l != null) break;
						}
					}
					return makeNanopubResultComponent(markupId, l);
				}
	
				@Override
				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
					super.onContentLoaded(content, target);
					if (target.get() != null) target.get().appendJavaScript("updateElements();");
				}
	
			});
		}

		List<NanopubElement> acceptedNanopubList = ApiCache.retrieveNanopubList("get-accepted-nanopubs-by-author", userIriString);
		if (acceptedNanopubList != null) {
			add(makeNanopubResultComponent("latestaccepted", acceptedNanopubList));
		} else {
			add(new AjaxLazyLoadPanel<Component>("latestaccepted") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<NanopubElement> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning(pubkeyHashes)) {
							l = ApiCache.retrieveNanopubList("get-accepted-nanopubs-by-author", userIriString);
							if (l != null) break;
						}
					}
					return makeNanopubResultComponent(markupId, l);
				}
	
				@Override
				protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
					super.onContentLoaded(content, target);
					if (target.get() != null) target.get().appendJavaScript("updateElements();");
				}
	
			});
		}
	}

	private static Component makeNanopubResultComponent(String markupId, List<NanopubElement> nanopubs) {
		if (nanopubs.isEmpty()) {
			return new Label(markupId, "(none)");
		} else {
			return new NanopubResults(markupId, makeShortList(nanopubs));
		}
	}

	private static List<NanopubElement> makeShortList(List<NanopubElement> list) {
		List<NanopubElement> shortList = new ArrayList<>();
		for (NanopubElement e : list) {
			shortList.add(e);
			if (shortList.size() == 5) break;
		}
		return shortList;
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
