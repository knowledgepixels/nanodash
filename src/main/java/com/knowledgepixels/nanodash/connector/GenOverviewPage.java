package com.knowledgepixels.nanodash.connector;

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
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.page.OrcidLoginPage;
import com.knowledgepixels.nanodash.page.PublishPage;

public class GenOverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector";

	private ConnectorConfig config;

	public GenOverviewPage(PageParameters params) {
		super(params);
		final String journalId = params.get("journal").toString();
		config = ConnectorConfig.get(journalId);

		add(new TitleBar("titlebar", this, "connectors"));
		add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));

		if (getConfig().getTechnicalEditorIds().contains(NanodashSession.get().getUserIri())) {
			WebMarkupContainer technicalEditorActions = new WebMarkupContainer("technical-editor-actions");

			technicalEditorActions.add(new BookmarkablePageLink<Void>("publish-article-metadata", PublishPage.class,
					new PageParameters().add("template", "https://w3id.org/np/RA48p4Ct8tWL--rIc1Dcr2BcYpW_7X1pfuv_2LK3anolY")
						.add("template-version", "latest")
						.add("param_journal", getConfig().getJournalIssn())
						.add("param_journal-title", getConfig().getJournalName())
						.add("prtemplate", "https://w3id.org/np/RAekcN47h13fk6ZK4XiObgGgk-qB01sLOjyGyhMCq_jT4")
						.add("pitemplate1", "https://w3id.org/np/RA5R_qv3VsZIrDKd8Mr37x3HoKCsKkwN5tJVqgQsKhjTE")
						.add("piparam1_type", getConfig().getNanopubType() == null ? "" : getConfig().getNanopubType().stringValue())
						.add("pitemplate2", "https://w3id.org/np/RA16U9Wo30ObhrK1NzH7EsmVRiRtvEuEA_Dfc-u8WkUCA")
						.add("target-namespace", getConfig().getTargetNamespace() == null ? "https://w3id.org/np/" : getConfig().getTargetNamespace())
				));
			add(technicalEditorActions);
		} else {
			add(new WebMarkupContainer("technical-editor-actions").setVisible(false));
		}

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
						PageParameters params = new PageParameters().add("journal", journalId).add("id", e.get("np")).add("mode", "author");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("ownlink", GenNanopubPage.class, params);
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

				add(new BookmarkablePageLink<Void>("create-new", GenSelectPage.class, params));
			} else {
				c.add(new Label("allowncandidates", "").setVisible(false));
				c.add(new Label("own", "").setVisible(false));
				if (NanodashPreferences.get().isOrcidLoginMode()) {
					String loginUrl = OrcidLoginPage.getOrcidLoginUrl(getMountPath(), getPageParameters());
					add(new ExternalLink("create-new", loginUrl, "Login to See More"));
				} else {
					add(new ExternalLink("create-new", Utils.getUrlWithParameters(getMountPath(), getPageParameters()), "Complete Your Profile to See More"));
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
					PageParameters params = new PageParameters().add("journal", journalId).add("id", e.get("np")).add("mode", "candidate");
					BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("candidatelink", GenNanopubPage.class, params);
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
						PageParameters params = new PageParameters().add("journal", journalId).add("id", e.get("np")).add("mode", "final");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("acceptedlink", GenNanopubPage.class, params);
						l.add(new Label("acceptedlinktext", "\"" +  e.get("label") + "\""));
						item.add(l);
						IRI firstAuthorIri = Utils.vf.createIRI(e.get("firstAuthor"));

						// TODO Move this user name extraction to a helper method:
						String username;
						if (User.getName(firstAuthorIri) != null) {
							username = User.getShortDisplayName(firstAuthorIri);
						} else {
							try {
								Nanopub np = Utils.getAsNanopub(e.get("np"));
								username = Utils.getFoafNameMap(np).get(e.get("firstAuthor"));
							} catch (Exception ex) {
								ex.printStackTrace();
								username = User.getShortDisplayName(firstAuthorIri);
							}
						}

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

		if (getConfig().getGeneralReactionsApiCall() != null) {
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
						PageParameters params = new PageParameters().add("journal", journalId).add("id", e.get("ref_np")).add("mode", "candidate");
						BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("reactionlink", GenNanopubPage.class, params);
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

		add(new Label("pagetitle", config.getJournalName() + " | nanodash"));
		add(new Label("journal-name-title", config.getJournalName()));
		add(new ExternalLink("journal-link", config.getJournalUrl(), config.getJournalName()));
		add(new Label("extra-instructions", config.getExtraInstructions()).setEscapeModelStrings(false));

		if (getConfig().getGeneralReactionsApiCall() == null) {
			// TODO Fix this in OverviewPage code once refactoring is finished:
			add(new Label("reactions-component").setVisible(false));
		}
		add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + config.getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

}
