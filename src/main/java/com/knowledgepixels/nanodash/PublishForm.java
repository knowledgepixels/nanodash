package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

public class PublishForm extends Panel {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static String creatorPubinfoTemplateId = "http://purl.org/np/RAA2MfqdBCzmz9yVWjKLXNbyfBNcwsMmOqcNUxkk1maIM";
	private static String defaultProvTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";
	private static String supersedesPubinfoTemplateId = "http://purl.org/np/RAjpBMlw3owYhJUBo3DtsuDlXsNAJ8cnGeWAutDVjuAuI";
	private static String derivesFromPubinfoTemplateId = "http://purl.org/np/RABngHbKpoJ3U9Nebc8mX_KUdv_vXw28EejqAyQya5zVA";

	private static List<PublishFormContext> fixedPubInfoContexts = new ArrayList<>();
	static {
		fixedPubInfoContexts.add(new PublishFormContext(ContextType.PUBINFO, creatorPubinfoTemplateId, "pi-statement"));
	}

	private enum FillMode { USE, SUPERSEDE, DERIVE }

	protected Form<?> form;
	protected FeedbackPanel feedbackPanel;
	private final PublishFormContext assertionContext;
	private PublishFormContext provenanceContext;
	private List<PublishFormContext> pubInfoContexts = new ArrayList<>();
	private Map<String,PublishFormContext> pubInfoContextMap = new HashMap<>();
	private List<PublishFormContext> requiredPubInfoContexts = new ArrayList<>();

	public PublishForm(String id, final PageParameters pageParams, final PublishPage page) {
		super(id);
		setOutputMarkupId(true);

		WebMarkupContainer linkMessageItem = new WebMarkupContainer("link-message-item");
		if (pageParams.get("link-message").isNull()) {
			linkMessageItem.add(new Label("link-message", ""));
			linkMessageItem.setVisible(false);
		} else {
			linkMessageItem.add(new Label("link-message", Utils.sanitizeHtml(pageParams.get("link-message").toString())).setEscapeModelStrings(false));
		}
		add(linkMessageItem);

		Nanopub fillNp = null;
		FillMode fillMode = null;
		boolean fillOnlyAssertion = false;
		if (!pageParams.get("use").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("use").toString());
			fillMode = FillMode.USE;
		} else if (!pageParams.get("use-a").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("use-a").toString());
			fillMode = FillMode.USE;
			fillOnlyAssertion = true;
		} else if (!pageParams.get("supersede").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("supersede").toString());
			fillMode = FillMode.SUPERSEDE;
		} else if (!pageParams.get("supersede-a").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("supersede-a").toString());
			fillMode = FillMode.SUPERSEDE;
			fillOnlyAssertion = true;
		} else if (!pageParams.get("derive").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("derive").toString());
			fillMode = FillMode.DERIVE;
		} else if (!pageParams.get("derive-a").isNull()) {
			fillNp = Utils.getNanopub(pageParams.get("derive-a").toString());
			fillMode = FillMode.DERIVE;
			fillOnlyAssertion = true;
		} else if (!pageParams.get("fill-all").isNull()) {
			// TODO: This is deprecated and should be removed at some point
			fillNp = Utils.getNanopub(pageParams.get("fill-all").toString());
			fillMode = FillMode.USE;
		} else if (!pageParams.get("fill").isNull()) {
			// TODO: This is deprecated and should be removed at some point
			fillNp = Utils.getNanopub(pageParams.get("fill").toString());
			fillMode = FillMode.SUPERSEDE;
		}

		assertionContext = new PublishFormContext(ContextType.ASSERTION, pageParams.get("template").toString(), "statement");
		String prTemplateId = pageParams.get("prtemplate").toString();
		if (prTemplateId == null) {
			if (fillNp != null && !fillOnlyAssertion) {
				if (Template.getProvenanceTemplateId(fillNp) != null) {
					prTemplateId = Template.getProvenanceTemplateId(fillNp).stringValue();
				} else {
					prTemplateId = "http://purl.org/np/RAcm8OurwUk15WOgBM9wySo-T3a5h6as4K8YR5MBrrxUc";
				}
			} else if (assertionContext.getTemplate().getDefaultProvenance() != null) {
				prTemplateId = assertionContext.getTemplate().getDefaultProvenance().stringValue();
			} else {
				prTemplateId = defaultProvTemplateId;
			}
		}
		provenanceContext = new PublishFormContext(ContextType.PROVENANCE, prTemplateId, "pr-statement");
		for (PublishFormContext c : fixedPubInfoContexts) {
			pubInfoContexts.add(c);
			pubInfoContextMap.put(c.getTemplate().getId(), c);
			requiredPubInfoContexts.add(c);
		}
		if (fillMode == FillMode.SUPERSEDE) {
			PublishFormContext c = new PublishFormContext(ContextType.PUBINFO, supersedesPubinfoTemplateId, "pi-statement");
			pubInfoContexts.add(c);
			pubInfoContextMap.put(supersedesPubinfoTemplateId, c);
			//requiredPubInfoContexts.add(c);
			c.setParam("np", fillNp.getUri().stringValue());
		} else if (fillMode == FillMode.DERIVE) {
			PublishFormContext c = new PublishFormContext(ContextType.PUBINFO, derivesFromPubinfoTemplateId, "pi-statement");
			pubInfoContexts.add(c);
			pubInfoContextMap.put(derivesFromPubinfoTemplateId, c);
			c.setParam("np", fillNp.getUri().stringValue());
		}
		for (IRI r : assertionContext.getTemplate().getRequiredPubinfoElements()) {
			PublishFormContext c = new PublishFormContext(ContextType.PUBINFO, r.stringValue(), "pi-statement");
			if (pubInfoContextMap.containsKey(c.getTemplate().getId())) continue;
			pubInfoContexts.add(c);
			pubInfoContextMap.put(c.getTemplate().getId(), c);
			requiredPubInfoContexts.add(c);
		}
		Map<Integer,PublishFormContext> piParamIdMap = new HashMap<>();
		for (String k : pageParams.getNamedKeys()) {
			if (!k.matches("pitemplate[1-9][0-9]*")) continue;
			Integer i = Integer.parseInt(k.replaceFirst("^pitemplate([1-9][0-9]*)$", "$1"));
			PublishFormContext c = getPubinfoContext(pageParams.get(k).toString());
			if (piParamIdMap.containsKey(i)) {
				// TODO: handle this error better
				System.err.println("ERROR: pitemplate param identifier assigned multiple times: " + i);
			}
			piParamIdMap.put(i, c);
			if (!pubInfoContexts.contains(c)) pubInfoContexts.add(c);
		}
		for (String k : pageParams.getNamedKeys()) {
			if (k.startsWith("param_")) assertionContext.setParam(k.substring(6), pageParams.get(k).toString());
			if (k.startsWith("prparam_")) provenanceContext.setParam(k.substring(8), pageParams.get(k).toString());
			if (k.matches("piparam[1-9][0-9]*_.*")) {
				Integer i = Integer.parseInt(k.replaceFirst("^piparam([1-9][0-9]*)_.*$", "$1"));
				if (!piParamIdMap.containsKey(i)) {
					// TODO: handle this error better
					System.err.println("ERROR: pitemplate param identifier not found: " + i);
					continue;
				}
				String n = k.replaceFirst("^piparam[1-9][0-9]*_(.*)$", "$1");
				System.err.println(n);
				piParamIdMap.get(i).setParam(n, pageParams.get(k).toString());
			}
		}

		// Init statements only now, in order to pick up parameter values:
		assertionContext.initStatements();
		provenanceContext.initStatements();
		for (PublishFormContext c : pubInfoContexts) {
			c.initStatements();
		}

		String latestAssertionId = ApiAccess.getLatestVersionId(assertionContext.getTemplateId());
		if (!assertionContext.getTemplateId().equals(latestAssertionId)) {
			add(new Label("newversion", "There is a new version of this assertion template:"));
			PageParameters params = new PageParameters(pageParams);
			params.set("template", latestAssertionId);
			add(new BookmarkablePageLink<PublishPage>("newversionlink", PublishPage.class, params));
			if ("latest".equals(pageParams.get("template-version").toString())) {
				throw new RestartResponseException(PublishPage.class, params);
			}
		} else {
			add(new Label("newversion", "").setVisible(false));
			add(new Label("newversionlink", "").setVisible(false));
		}

		String warningMessage = "";
		if (fillNp != null) {
			ValueFiller filler = new ValueFiller(fillNp, ContextType.ASSERTION);
			filler.fill(assertionContext);
			warningMessage += (filler.getWarningMessage() == null ? "" : "Assertion: " + filler.getWarningMessage() + " ");

			if (!fillOnlyAssertion) {
				ValueFiller prFiller = new ValueFiller(fillNp, ContextType.PROVENANCE);
				prFiller.fill(provenanceContext);
				warningMessage += (prFiller.getWarningMessage() == null ? "" : "Provenance: " + prFiller.getWarningMessage() + " ");
	
				ValueFiller piFiller = new ValueFiller(fillNp, ContextType.PUBINFO);
				for (PublishFormContext c : pubInfoContexts) {
					piFiller.fill(c);
				}
				if (piFiller.hasUnusedStatements()) {
					PublishFormContext c = getPubinfoContext("http://purl.org/np/RA2vCBXZf-icEcVRGhulJXugTGxpsV5yVr9yqCI1bQh4A");
					if (!pubInfoContexts.contains(c)) {
						pubInfoContexts.add(c);
						c.initStatements();
						piFiller.fill(c);
					}
				}
				warningMessage += (piFiller.getWarningMessage() == null ? "" : "Publication info: " + piFiller.getWarningMessage() + " ");
				// TODO: Also use pubinfo templates stated in nanopub to be filled in?
//				Set<IRI> pubinfoTemplateIds = Template.getPubinfoTemplateIds(fillNp);
//				if (!pubinfoTemplateIds.isEmpty()) {
//					ValueFiller piFiller = new ValueFiller(fillNp, ContextType.PUBINFO);
//					for (IRI pubinfoTemplateId : pubinfoTemplateIds) {
//						// TODO: Make smart choice on the ordering in trying to fill in all pubinfo elements
//						piFiller.fill(pubInfoContextMap.get(pubinfoTemplateId.stringValue()));
//					}
//					warningMessage += (piFiller.getWarningMessage() == null ? "" : "Publication info: " + piFiller.getWarningMessage() + " ");
//				}
			}
		} else if (!pageParams.get("improve").isNull()) {
			Nanopub improveNp = Utils.getNanopub(pageParams.get("improve").toString());
			ValueFiller filler = new ValueFiller(improveNp, ContextType.ASSERTION);
			filler.fill(assertionContext);
			warningMessage += (filler.getWarningMessage() == null ? "" : "Assertion: " + filler.getWarningMessage() + " ");
		}
		if (!warningMessage.isEmpty()) {
			add(new Label("warnings", warningMessage));
		} else {
			add(new Label("warnings", "").setVisible(false));
		}

		// Finalize statements, which picks up parameter values in repetitions:
		assertionContext.finalizeStatements();
		provenanceContext.finalizeStatements();
		for (PublishFormContext c : pubInfoContexts) {
			c.finalizeStatements();
		}

		final CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(false));
		consentCheck.setRequired(true);
		consentCheck.add(new InvalidityHighlighting());
		consentCheck.add(new IValidator<Boolean>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<Boolean> validatable) {
				if (!Boolean.TRUE.equals(validatable.getValue())) {
					validatable.error(new ValidationError("You need to check the checkbox that you understand the consequences."));
				}
			}
			
		});

		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				form.getFeedbackMessages().clear();
//				formComponents.clear();
			}

			protected void onSubmit() {
				try {
					Nanopub np = createNanopub();
					TransformContext tc = new TransformContext(SignatureAlgorithm.RSA, NanodashSession.get().getKeyPair(), null, false, false);
					Nanopub signedNp = SignNanopub.signAndTransform(np, tc);
					if (isLocal()) {
						// Testing mode
						System.err.println("This nanopublication would have been published (if we were not in testing mode):");
						System.err.println("----------");
						System.err.println(org.nanopub.NanopubUtils.writeToString(signedNp, org.eclipse.rdf4j.rio.RDFFormat.TRIG));
						System.err.println("----------");
					} else {
						PublishNanopub.publish(signedNp);
						System.err.println("Published " + signedNp.getUri());
					}
					PageParameters params = new PageParameters();
					params.add("id", NanodashSession.get().getUserIri().stringValue());
					throw new RestartResponseException(new PublishConfirmPage(signedNp));
				} catch (RestartResponseException ex) {
					throw ex;
				} catch (Exception ex) {
					ex.printStackTrace();
					String message = ex.getClass().getName();
					if (ex.getMessage() != null) message = ex.getMessage();
					feedbackPanel.error(message);
				}
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				for (FormComponent<String> fc : assertionContext.getFormComponents()) {
					processFeedback(fc);
				}
				for (FormComponent<String> fc : provenanceContext.getFormComponents()) {
					processFeedback(fc);
				}
				for (PublishFormContext c : pubInfoContexts) {
					for (FormComponent<String> fc : c.getFormComponents()) {
						processFeedback(fc);
					}
				}
			}

			private void processFeedback(FormComponent<String> fc) {
				fc.processInput();
				for (FeedbackMessage fm : fc.getFeedbackMessages()) {
					form.getFeedbackMessages().add(fm);
				}
			}

		};
		form.setOutputMarkupId(true);

		form.add(new BookmarkablePageLink<UserPage>("templatelink", ExplorePage.class, new PageParameters().add("id", assertionContext.getTemplate().getId())));
		form.add(new Label("templatename", assertionContext.getTemplate().getLabel()));
		String description = assertionContext.getTemplate().getLabel();
		if (description == null) description = "";
		form.add(new Label("templatedesc", assertionContext.getTemplate().getDescription()).setEscapeModelStrings(false));

		form.add(new ListView<StatementItem>("statements", assertionContext.getStatementItems()) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});

		ChoiceProvider<String> prTemplateChoiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(String object) {
				if (object == null || object.isEmpty()) return "";
				Template t = Template.getTemplate(object);
				if (t != null) return t.getLabel();
				return "";
			}

			@Override
			public String getIdValue(String object) {
				return object;
			}

			// Getting strange errors with Tomcat if this method is not overridden:
			@Override
			public void detach() {
			}

			@Override
			public void query(String term, int page, Response<String> response) {
				if (term == null) term = "";
				term = term.toLowerCase();
				for (Template t : Template.getProvenanceTemplates()) {
					String s = t.getLabel();
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
						response.add(t.getId());
					}
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		final IModel<String> prTemplateModel = Model.of(provenanceContext.getTemplate().getId());
		Select2Choice<String> prTemplateChoice = new Select2Choice<String>("prtemplate", prTemplateModel, prTemplateChoiceProvider);
		prTemplateChoice.setRequired(true);
		prTemplateChoice.getSettings().setCloseOnSelect(true);
		prTemplateChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				provenanceContext = new PublishFormContext(ContextType.PROVENANCE, prTemplateModel.getObject(), "pr-statement");
				provenanceContext.initStatements();
				refreshProvenance(target);
			}

		});
		form.add(prTemplateChoice);
		refreshProvenance(null);

		ChoiceProvider<String> piTemplateChoiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(String object) {
				if (object == null || object.isEmpty()) return "";
				Template t = Template.getTemplate(object);
				if (t != null) return t.getLabel();
				return "";
			}

			@Override
			public String getIdValue(String object) {
				return object;
			}

			// Getting strange errors with Tomcat if this method is not overridden:
			@Override
			public void detach() {
			}

			@Override
			public void query(String term, int page, Response<String> response) {
				if (term == null) term = "";
				term = term.toLowerCase();
				for (Template t : Template.getPubInfoTemplates()) {
					String s = t.getLabel();
					boolean isAlreadyUsed = false;
					for (PublishFormContext c : pubInfoContexts) {
						// TODO: make this more efficient/nicer
						if (c.getTemplate().getId().equals(t.getId())) {
							isAlreadyUsed = true;
							break;
						}
					}
					if (isAlreadyUsed) continue;
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
						response.add(t.getId());
					}
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		final IModel<String> newPiTemplateModel = Model.of();
		Select2Choice<String> piTemplateChoice = new Select2Choice<String>("pitemplate", newPiTemplateModel, piTemplateChoiceProvider);
		piTemplateChoice.getSettings().setCloseOnSelect(true);
		piTemplateChoice.getSettings().setPlaceholder("add element...");
		piTemplateChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				PublishFormContext c = new PublishFormContext(ContextType.PUBINFO, newPiTemplateModel.getObject(), "pi-statement");
				c.initStatements();
				pubInfoContexts.add(c);
				newPiTemplateModel.setObject(null);
				refreshPubInfo(target);
			}

		});
		form.add(piTemplateChoice);
		refreshPubInfo(null);

		form.add(consentCheck);
		add(form);

		if (isLocal()) {
			add(new Link<Object>("local-reload-link") {
				private static final long serialVersionUID = 1L;
				public void onClick() {
					setResponsePage(page.getPageClass(), page.getPageParameters());
				};
			});
			form.add(new Label("local-file-text", "TEST MODE. Nanopublication will not actually be published."));
		} else {
			Label l = new Label("local-reload-link", "");
			l.setVisible(false);
			add(l);
			form.add(new Label("local-file-text", ""));
		}

		feedbackPanel = new FeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);
	}

	private void refreshProvenance(AjaxRequestTarget target) {
		if (target != null) {
			form.remove("prtemplatelink");
			form.remove("pr-statements");
			target.add(form);
		}
		form.add(new BookmarkablePageLink<UserPage>("prtemplatelink", ExplorePage.class, new PageParameters().add("id", provenanceContext.getTemplate().getId())));
		ListView<StatementItem> list = new ListView<StatementItem>("pr-statements", provenanceContext.getStatementItems()) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<StatementItem> item) {
				item.add(item.getModelObject());
			}

		};
		list.setOutputMarkupId(true);
		form.add(list);
	}

	private void refreshPubInfo(AjaxRequestTarget target) {
		ListView<PublishFormContext> list = new ListView<PublishFormContext>("pis", pubInfoContexts) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<PublishFormContext> item) {
				final PublishFormContext pic = item.getModelObject();
				item.add(new Label("pitemplatename", pic.getTemplate().getLabel()));
				item.add(new BookmarkablePageLink<UserPage>("pitemplatelink", ExplorePage.class, new PageParameters().add("id", pic.getTemplate().getId())));
				Label remove = new Label("piremove", "×");
				item.add(remove);
				remove.add(new AjaxEventBehavior("click") {
					private static final long serialVersionUID = 1L;
					@Override
					protected void onEvent(AjaxRequestTarget target) {
						pubInfoContexts.remove(pic);
						target.add(PublishForm.this);
					}
				});
				if (requiredPubInfoContexts.contains(pic)) remove.setVisible(false);
				item.add(new ListView<StatementItem>("pi-statements", pic.getStatementItems()) {

					private static final long serialVersionUID = 1L;

					protected void populateItem(ListItem<StatementItem> item) {
						item.add(item.getModelObject());
					}

				});
			}

		};
		list.setOutputMarkupId(true);
		if (target == null) {
			form.add(list);
		} else {
			form.remove("pis");
			form.add(list);
			target.add(form);
		}
	}

	private PublishFormContext getPubinfoContext(String piTemplateId) {
		PublishFormContext c;
		if (pubInfoContextMap.containsKey(piTemplateId)) {
			c = pubInfoContextMap.get(piTemplateId);
		} else {
			c = new PublishFormContext(ContextType.PUBINFO, piTemplateId, "pi-statement");
			pubInfoContextMap.put(piTemplateId, c);
		}
		return c;
	}

	public static final IRI INTRODUCES_PREDICATE = vf.createIRI("http://purl.org/nanopub/x/introduces");

	private synchronized Nanopub createNanopub() throws MalformedNanopubException {
		assertionContext.getIntroducedIris().clear();
		NanopubCreator npCreator = new NanopubCreator(PublishFormContext.NP_TEMP_IRI);
		npCreator.setAssertionUri(PublishFormContext.ASSERTION_TEMP_IRI);
		assertionContext.propagateStatements(npCreator);
		provenanceContext.propagateStatements(npCreator);
		for (PublishFormContext c : pubInfoContexts) {
			c.propagateStatements(npCreator);
		}
		for (IRI introducedIri : assertionContext.getIntroducedIris()) {
			npCreator.addPubinfoStatement(INTRODUCES_PREDICATE, introducedIri);
		}
		npCreator.addNamespace("this", "http://purl.org/nanopub/temp/nanodash-new-nanopub/");
		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/nanodash-new-nanopub/#");
		npCreator.addTimestampNow();
//		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, ProfilePage.getUserIri());
		IRI templateUri = assertionContext.getTemplate().getNanopub().getUri();
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, templateUri);
		IRI prTemplateUri = provenanceContext.getTemplate().getNanopub().getUri();
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE, prTemplateUri);
		for (PublishFormContext c : pubInfoContexts) {
			IRI piTemplateUri = c.getTemplate().getNanopub().getUri();
			npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE, piTemplateUri);
		}
		return npCreator.finalizeNanopub();
	}

	private boolean isLocal() {
		if (assertionContext.isLocal()) return true;
		if (provenanceContext.isLocal()) return true;
		for (PublishFormContext c : pubInfoContexts) {
			if (c.isLocal()) return true;
		}
		return false;
	}

}
