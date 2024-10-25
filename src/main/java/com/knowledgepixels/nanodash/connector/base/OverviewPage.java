package com.knowledgepixels.nanodash.connector.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.page.OrcidLoginPage;
import com.knowledgepixels.nanodash.page.ProfilePage;

public abstract class OverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public OverviewPage(PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this, "connectors"));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		try {

			final WebMarkupContainer c = new WebMarkupContainer("owncandidates-component");
			c.setOutputMarkupId(true);
			add(c);

			if (NanodashSession.get().getUserIri() != null) {

				HashMap<String,String> apiParam = new HashMap<>();
				apiParam.put("creator", NanodashSession.get().getUserIri().stringValue());
				ApiResponse resp = callApi(getConfig().getCandidateNanopubsApiCall(), apiParam);
				while (resp == null) {
					// we only get here in case of second-generation API calls
					// TODO Do this in an AJAX way:
					try {
						Thread.sleep(200);
					} catch (InterruptedException ex) {}
					resp = callApi(getConfig().getCandidateNanopubsApiCall(), apiParam);
				}

				final List<ApiResponseEntry> listData = new ArrayList<ApiResponseEntry>();
				final ArrayList<ApiResponseEntry> fullList = new ArrayList<>();
				for (ApiResponseEntry a : resp.getData()) {
					if (listData.size() < 10) listData.add(a);
					// TODO This will become inefficient at some point:
					fullList.add(a);
				}

				c.add(new DataView<ApiResponseEntry>("own", new ListDataProvider<ApiResponseEntry>(listData)) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "author");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("ownlink", getConfig().getNanopubPage().getClass(), params);
						l.add(new Label("ownlinktext", "\"" +  e.get("label") + "\""));
						item.add(l);
						String username = User.getShortDisplayName(null, e.get("pubkey"));
						item.add(new Label("ownnote", "by " + username + " on " + e.get("date").substring(0, 10)));
					}

				});

				c.add(new AjaxLink<>("allowncandidates") {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						try {
							listData.clear();
							listData.addAll(fullList);
							target.add(c);
							setVisible(false);
							target.add(this);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}.setVisible(fullList.size() > 10));

				add(new ExternalLink("create-new", getConfig().getSelectPage().getMountPath(), "Create Nanopublication"));
			} else {
				c.add(new Label("allowncandidates", "").setVisible(false));
				c.add(new Label("own", "").setVisible(false));
				if (NanodashPreferences.get().isOrcidLoginMode()) {
					String loginUrl = OrcidLoginPage.getOrcidLoginUrl(getMountPath());
					add(new ExternalLink("create-new", loginUrl, "Login to See More"));
				} else {
					add(new ExternalLink("create-new", ProfilePage.MOUNT_PATH, "Complete Your Profile to See More"));
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			final WebMarkupContainer c = new WebMarkupContainer("candidates-component");
			c.setOutputMarkupId(true);
			add(c);

			ApiResponse resp = callApi(getConfig().getCandidateNanopubsApiCall(), new HashMap<>());
			while (resp == null) {
				// TODO Do this in an AJAX way:
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {}
				resp = callApi(getConfig().getCandidateNanopubsApiCall(), new HashMap<>());
			}

			final List<ApiResponseEntry> listData = new ArrayList<ApiResponseEntry>();
			final ArrayList<ApiResponseEntry> fullList = new ArrayList<>();
			for (ApiResponseEntry a : resp.getData()) {
				if (listData.size() < 10) listData.add(a);
				// TODO This will become inefficient at some point:
				fullList.add(a);
			}

			c.add(new DataView<ApiResponseEntry>("candidates", new ListDataProvider<ApiResponseEntry>(listData)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(Item<ApiResponseEntry> item) {
					ApiResponseEntry e = item.getModelObject();
					PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "candidate");
					BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("candidatelink", getConfig().getNanopubPage().getClass(), params);
					l.add(new Label("candidatelinktext", "\"" +  e.get("label") + "\""));
					item.add(l);
					String username = User.getShortDisplayName(null, e.get("pubkey"));
					item.add(new Label("candidatenote", "by " + username + " on " + e.get("date").substring(0, 10)));
				}

			});

			c.add(new AjaxLink<>("allcandidates") {

				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					try {
						listData.clear();
						listData.addAll(fullList);
						target.add(c);
						setVisible(false);
						target.add(this);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			}.setVisible(fullList.size() > 10));

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (getConfig().getAcceptedNanopubsApiCall() != null ) {
			try {
				final WebMarkupContainer c = new WebMarkupContainer("accepted-component");
				c.setOutputMarkupId(true);
				add(c);

				ApiResponse resp = callApi(getConfig().getAcceptedNanopubsApiCall(), new HashMap<>());
				while (resp == null) {
					// TODO Do this in an AJAX way:
					try {
						Thread.sleep(200);
					} catch (InterruptedException ex) {}
					resp = callApi(getConfig().getAcceptedNanopubsApiCall(), new HashMap<>());
				}

				final List<ApiResponseEntry> listData = new ArrayList<ApiResponseEntry>();
				final ArrayList<ApiResponseEntry> fullList = new ArrayList<>();
				for (ApiResponseEntry a : resp.getData()) {
					if (listData.size() < 10) listData.add(a);
					// TODO This will become inefficient at some point:
					fullList.add(a);
				}

				c.add(new DataView<ApiResponseEntry>("accepted", new ListDataProvider<ApiResponseEntry>(listData)) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "final");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("acceptedlink", getConfig().getNanopubPage().getClass(), params);
						l.add(new Label("acceptedlinktext", "\"" +  e.get("label") + "\""));
						item.add(l);
						String username = User.getShortDisplayName(Utils.vf.createIRI(e.get("firstAuthor")));
						item.add(new Label("acceptednote", "by " + username + " on " + e.get("date").substring(0, 10)));
					}

				});

				c.add(new AjaxLink<>("allaccepted") {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						try {
							listData.clear();
							listData.addAll(fullList);
							target.add(c);
							setVisible(false);
							target.add(this);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}.setVisible(fullList.size() > 10));

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		if (getConfig().getGeneralReactionsApiCall() != null ) {
			try {
				final WebMarkupContainer c = new WebMarkupContainer("reactions-component");
				c.setOutputMarkupId(true);
				add(c);

				ApiResponse resp = callApi(getConfig().getGeneralReactionsApiCall(), new HashMap<>());
				while (resp == null) {
					// TODO Do this in an AJAX way:
					try {
						Thread.sleep(200);
					} catch (InterruptedException ex) {}
					resp = callApi(getConfig().getGeneralReactionsApiCall(), new HashMap<>());
				}

				final List<ApiResponseEntry> listData = new ArrayList<ApiResponseEntry>();
				final ArrayList<ApiResponseEntry> fullList = new ArrayList<>();
				for (ApiResponseEntry a : resp.getData()) {
					if (listData.size() < 10) listData.add(a);
					// TODO This will become inefficient at some point:
					fullList.add(a);
				}

				c.add(new DataView<ApiResponseEntry>("reactions", new ListDataProvider<ApiResponseEntry>(listData)) {
					
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters().add("id", e.get("ref_np")).add("mode", "candidate");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("reactionlink", getConfig().getNanopubPage().getClass(), params);
						l.add(new Label("reactionlinktext", "\"" +  e.get("comment") + "\""));
						item.add(l);
						String username = User.getShortDisplayName(null, e.get("pubkey"));
						item.add(new Label("reactionnote", "by " + username + " on " + e.get("date").substring(0, 10)));
					}

				});

				c.add(new AjaxLink<>("allreactions") {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						try {
							listData.clear();
							listData.addAll(fullList);
							target.add(c);
							setVisible(false);
							target.add(this);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}.setVisible(fullList.size() > 10));

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
