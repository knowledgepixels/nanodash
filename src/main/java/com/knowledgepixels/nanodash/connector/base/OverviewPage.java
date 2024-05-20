package com.knowledgepixels.nanodash.connector.base;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.OrcidLoginPage;
import com.knowledgepixels.nanodash.ProfilePage;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.User;

public abstract class OverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public OverviewPage(PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this, "connectors"));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		if (getConfig().getGeneralApiCall() != null) {

			if (NanodashSession.get().getUserIri() != null) {

				try {
					HashMap<String,String> apiParam = new HashMap<>();
					apiParam.put("creator", NanodashSession.get().getUserIri().stringValue());
					ApiResponse resp = callApi(getConfig().getGeneralApiCall(), apiParam);

					ArrayList<ApiResponseEntry> respList = new ArrayList<>();
					for (ApiResponseEntry a : resp.getData()) {
						if (respList.size() == 10) break;
						respList.add(a);
					}

					add(new DataView<ApiResponseEntry>("own", new ListDataProvider<ApiResponseEntry>(respList)) {

						private static final long serialVersionUID = 1L;

						@Override
						protected void populateItem(Item<ApiResponseEntry> item) {
							ApiResponseEntry e = item.getModelObject();
							PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "author");
							BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("ownlink", getConfig().getNanopubPage().getClass(), params);
							l.add(new Label("ownlinktext", "\"" +  e.get("label") + "\""));
							item.add(l);
							String username = User.getShortDisplayName(null, e.get("pubkey"));
							item.add(new Label("ownnote", "by " + username + " on " + e.get("date").substring(0, 10)));
						}

					});
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				add(new ExternalLink("create-new", getConfig().getSelectPage().getMountPath(), "Create Nanopublication"));
			} else {
				add(new Label("own", "").setVisible(false));
				if (NanodashPreferences.get().isOrcidLoginMode()) {
					String loginUrl = OrcidLoginPage.getOrcidLoginUrl(getMountPath());
					add(new ExternalLink("create-new", loginUrl, "Login to See More"));
				} else {
					add(new ExternalLink("create-new", ProfilePage.MOUNT_PATH, "Complete Your Profile to See More"));
				}
			}

			try {
				ApiResponse resp = callApi(getConfig().getGeneralApiCall(), new HashMap<>());

				ArrayList<ApiResponseEntry> respList = new ArrayList<>();
				for (ApiResponseEntry a : resp.getData()) {
					if (respList.size() == 10) break;
					respList.add(a);
				}

				add(new DataView<ApiResponseEntry>("candidates", new ListDataProvider<ApiResponseEntry>(respList)) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "candidate");
						BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("candidatelink", getConfig().getNanopubPage().getClass(), params);
						l.add(new Label("candidatelinktext", "\"" +  e.get("label") + "\""));
						item.add(l);
						String username = User.getShortDisplayName(null, e.get("pubkey"));
						item.add(new Label("candidatenote", "by " + username + " on " + e.get("date").substring(0, 10)));
					}

				});
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
