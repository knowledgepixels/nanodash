package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.ApiAccess;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

public class PublishForm extends Panel {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static String creatorPubinfoTemplateId = "http://purl.org/np/RAA2MfqdBCzmz9yVWjKLXNbyfBNcwsMmOqcNUxkk1maIM";
	private static String licensePubinfoTempalteId = "http://purl.org/np/RAh1gm83JiG5M6kDxXhaYT1l49nCzyrckMvTzcPn-iv90";
	private static String defaultProvTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";
	private static String supersedesPubinfoTemplateId = "http://purl.org/np/RAjpBMlw3owYhJUBo3DtsuDlXsNAJ8cnGeWAutDVjuAuI";
	private static String derivesFromPubinfoTemplateId = "http://purl.org/np/RABngHbKpoJ3U9Nebc8mX_KUdv_vXw28EejqAyQya5zVA";

	private static String[] fixedPubInfoTemplates = new String[] {creatorPubinfoTemplateId, licensePubinfoTempalteId};

	private enum FillMode { USE, SUPERSEDE, DERIVE }

	protected Form<?> form;
	protected FeedbackPanel feedbackPanel;
	private final TemplateContext assertionContext;
	private TemplateContext provenanceContext;
	private List<TemplateContext> pubInfoContexts = new ArrayList<>();
	private Map<String,TemplateContext> pubInfoContextMap = new HashMap<>();
	private List<TemplateContext> requiredPubInfoContexts = new ArrayList<>();
	private String targetNamespace;

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

		final Nanopub fillNp;
		final FillMode fillMode;
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
		} else {
			fillNp = null;
			fillMode = null;
		}

		final TemplateData td = TemplateData.get();

		String templateId = pageParams.get("template").toString();
		targetNamespace = td.getTemplate(templateId).getTargetNamespace();
		if (!pageParams.get("target-namespace").isNull()) {
			// TODO: properly integrate this feature:
			targetNamespace = pageParams.get("target-namespace").toString();
		}

		assertionContext = new TemplateContext(ContextType.ASSERTION, templateId, "statement", targetNamespace);
		String prTemplateId = pageParams.get("prtemplate").toString();
		if (prTemplateId == null) {
			if (fillNp != null && !fillOnlyAssertion) {
				if (td.getProvenanceTemplateId(fillNp) != null) {
					prTemplateId = td.getProvenanceTemplateId(fillNp).stringValue();
				} else {
					prTemplateId = "http://purl.org/np/RAcm8OurwUk15WOgBM9wySo-T3a5h6as4K8YR5MBrrxUc";
				}
			} else if (assertionContext.getTemplate().getDefaultProvenance() != null) {
				prTemplateId = assertionContext.getTemplate().getDefaultProvenance().stringValue();
			} else {
				prTemplateId = defaultProvTemplateId;
			}
		}
		provenanceContext = new TemplateContext(ContextType.PROVENANCE, prTemplateId, "pr-statement", targetNamespace);
		for (String t : fixedPubInfoTemplates) {
			TemplateContext c = new TemplateContext(ContextType.PUBINFO, t, "pi-statement", targetNamespace);
			pubInfoContexts.add(c);
			pubInfoContextMap.put(c.getTemplate().getId(), c);
			requiredPubInfoContexts.add(c);
		}
		if (fillMode == FillMode.SUPERSEDE) {
			TemplateContext c = new TemplateContext(ContextType.PUBINFO, supersedesPubinfoTemplateId, "pi-statement", targetNamespace);
			pubInfoContexts.add(c);
			pubInfoContextMap.put(supersedesPubinfoTemplateId, c);
			//requiredPubInfoContexts.add(c);
			c.setParam("np", fillNp.getUri().stringValue());
		} else if (fillMode == FillMode.DERIVE) {
			TemplateContext c = new TemplateContext(ContextType.PUBINFO, derivesFromPubinfoTemplateId, "pi-statement", targetNamespace);
			pubInfoContexts.add(c);
			pubInfoContextMap.put(derivesFromPubinfoTemplateId, c);
			c.setParam("np", fillNp.getUri().stringValue());
		}
		for (IRI r : assertionContext.getTemplate().getRequiredPubinfoElements()) {
			String latestId = ApiAccess.getLatestVersionId(r.stringValue());
			if (pubInfoContextMap.containsKey(r.stringValue()) || pubInfoContextMap.containsKey(latestId)) continue;
			TemplateContext c = new TemplateContext(ContextType.PUBINFO, r.stringValue(), "pi-statement", targetNamespace);
			pubInfoContexts.add(c);
			pubInfoContextMap.put(c.getTemplate().getId(), c);
			requiredPubInfoContexts.add(c);
		}
		Map<Integer,TemplateContext> piParamIdMap = new HashMap<>();
		for (String k : pageParams.getNamedKeys()) {
			if (!k.matches("pitemplate[1-9][0-9]*")) continue;
			Integer i = Integer.parseInt(k.replaceFirst("^pitemplate([1-9][0-9]*)$", "$1"));
			TemplateContext c = getPubinfoContext(pageParams.get(k).toString());
			if (piParamIdMap.containsKey(i)) {
				// TODO: handle this error better
				System.err.println("ERROR: pitemplate param identifier assigned multiple times: " + i);
			}
			piParamIdMap.put(i, c);
			if (!pubInfoContexts.contains(c)) pubInfoContexts.add(c);
		}
		if (fillNp != null && !fillOnlyAssertion) {
			for (IRI piTemplateId : td.getPubinfoTemplateIds(fillNp)) {
				if (piTemplateId.stringValue().equals(supersedesPubinfoTemplateId)) continue;
				TemplateContext c = getPubinfoContext(piTemplateId.stringValue());
				if (!pubInfoContexts.contains(c)) pubInfoContexts.add(c);
			}
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
		for (TemplateContext c : pubInfoContexts) {
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

		final Nanopub improveNp;
		if (!pageParams.get("improve").isNull()) {
			improveNp = Utils.getNanopub(pageParams.get("improve").toString());
		} else {
			improveNp = null;
		}
		
		final List<Statement> unusedStatementList = new ArrayList<>();
		final List<Statement> unusedPrStatementList = new ArrayList<>();
		final List<Statement> unusedPiStatementList = new ArrayList<>();
		if (fillNp != null) {
			ValueFiller filler = new ValueFiller(fillNp, ContextType.ASSERTION, true);
			filler.fill(assertionContext);
			unusedStatementList.addAll(filler.getUnusedStatements());

			if (!fillOnlyAssertion) {
				ValueFiller prFiller = new ValueFiller(fillNp, ContextType.PROVENANCE, true);
				prFiller.fill(provenanceContext);
				unusedPrStatementList.addAll(prFiller.getUnusedStatements());
	
				ValueFiller piFiller = new ValueFiller(fillNp, ContextType.PUBINFO, true);
				if (!assertionContext.getTemplate().getTargetNanopubTypes().isEmpty()) {
					for (Statement st : new ArrayList<>(piFiller.getUnusedStatements())) {
						if (st.getSubject().stringValue().equals("local:nanopub") && st.getPredicate().equals(PublishForm.NANOPUB_TYPE_PREDICATE)) {
							if (assertionContext.getTemplate().getTargetNanopubTypes().contains(st.getObject())) {
								piFiller.removeUnusedStatement(st);
							}
						}
					}
				}
				for (TemplateContext c : pubInfoContexts) {
					piFiller.fill(c);
				}
				if (piFiller.hasUnusedStatements()) {
					TemplateContext c = getPubinfoContext("http://purl.org/np/RA2vCBXZf-icEcVRGhulJXugTGxpsV5yVr9yqCI1bQh4A");
					if (!pubInfoContexts.contains(c)) {
						pubInfoContexts.add(c);
						c.initStatements();
						piFiller.fill(c);
					}
				}
				unusedPiStatementList.addAll(piFiller.getUnusedStatements());
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
		} else if (improveNp != null) {
			ValueFiller filler = new ValueFiller(improveNp, ContextType.ASSERTION, true);
			filler.fill(assertionContext);
			unusedStatementList.addAll(filler.getUnusedStatements());
		}
		if (!unusedStatementList.isEmpty()) {
			add(new Label("warnings", "Some content from the existing nanopublication could not be filled in:"));
		} else {
			add(new Label("warnings", "").setVisible(false));
		}
		add(new DataView<Statement>("unused-statements", new ListDataProvider<Statement>(unusedStatementList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				item.add(new TripleItem("unused-statement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
			}

		});
		add(new DataView<Statement>("unused-prstatements", new ListDataProvider<Statement>(unusedPrStatementList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				item.add(new TripleItem("unused-prstatement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
			}

		});
		add(new DataView<Statement>("unused-pistatements", new ListDataProvider<Statement>(unusedPiStatementList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				item.add(new TripleItem("unused-pistatement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
			}

		});

		// Finalize statements, which picks up parameter values in repetitions:
		assertionContext.finalizeStatements();
		provenanceContext.finalizeStatements();
		for (TemplateContext c : pubInfoContexts) {
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
				System.err.println("Publish form submitted");
				Nanopub signedNp = null;
				try {
					Nanopub np = createNanopub();
					System.err.println("Nanopublication created: " + np.getUri());
					TransformContext tc = new TransformContext(SignatureAlgorithm.RSA, NanodashSession.get().getKeyPair(), NanodashSession.get().getUserIri(), false, false);
					signedNp = SignNanopub.signAndTransform(np, tc);
					System.err.println("Nanopublication signed: " + signedNp.getUri());
					String npUrl = PublishNanopub.publish(signedNp);
					System.err.println("Nanopublication published: " + npUrl);
				} catch (Exception ex) {
					signedNp = null;
					ex.printStackTrace();
					String message = ex.getClass().getName();
					if (ex.getMessage() != null) message = ex.getMessage();
					feedbackPanel.error(message);
				}
				if (signedNp != null) {
					throw new RestartResponseException(new PublishConfirmPage(signedNp, pageParams));
				} else {
					System.err.println("Nanopublication publishing failed");
				}
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				for (Component fc : assertionContext.getComponents()) {
					processFeedback(fc);
				}
				for (Component fc : provenanceContext.getComponents()) {
					processFeedback(fc);
				}
				for (TemplateContext c : pubInfoContexts) {
					for (Component fc : c.getComponents()) {
						processFeedback(fc);
					}
				}
			}

			private void processFeedback(Component c) {
				if (c instanceof FormComponent) {
					((FormComponent<?>) c).processInput();
				}
				for (FeedbackMessage fm : c.getFeedbackMessages()) {
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

		final List<Template> provTemplateOptions;
		if (pageParams.get("prtemplate-options").isNull()) {
			provTemplateOptions = td.getProvenanceTemplates();
		} else {
			provTemplateOptions = new ArrayList<>();
			for (String tid : pageParams.get("prtemplate-options").toString().split(" ")) {
				provTemplateOptions.add(td.getTemplate(tid));
			}
		}

		ChoiceProvider<String> prTemplateChoiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(String object) {
				if (object == null || object.isEmpty()) return "";
				Template t = td.getTemplate(object);
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
				for (Template t : provTemplateOptions) {
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
				provenanceContext = new TemplateContext(ContextType.PROVENANCE, prTemplateModel.getObject(), "pr-statement", targetNamespace);
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
				Template t = td.getTemplate(object);
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
				for (Template t : td.getPubInfoTemplates()) {
					String s = t.getLabel();
					boolean isAlreadyUsed = false;
					for (TemplateContext c : pubInfoContexts) {
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
				TemplateContext c = new TemplateContext(ContextType.PUBINFO, newPiTemplateModel.getObject(), "pi-statement", targetNamespace);
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
		ListView<TemplateContext> list = new ListView<TemplateContext>("pis", pubInfoContexts) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<TemplateContext> item) {
				final TemplateContext pic = item.getModelObject();
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

	private TemplateContext getPubinfoContext(String piTemplateId) {
		TemplateContext c;
		if (pubInfoContextMap.containsKey(piTemplateId)) {
			c = pubInfoContextMap.get(piTemplateId);
		} else {
			c = new TemplateContext(ContextType.PUBINFO, piTemplateId, "pi-statement", targetNamespace);
			pubInfoContextMap.put(piTemplateId, c);
		}
		return c;
	}

	public static final IRI INTRODUCES_PREDICATE = vf.createIRI("http://purl.org/nanopub/x/introduces");
	public static final IRI NANOPUB_TYPE_PREDICATE = vf.createIRI("http://purl.org/nanopub/x/hasNanopubType");
	public static final IRI WAS_CREATED_AT_PREDICATE = vf.createIRI("http://purl.org/nanopub/x/wasCreatedAt");

	private synchronized Nanopub createNanopub() throws MalformedNanopubException {
		assertionContext.getIntroducedIris().clear();
		NanopubCreator npCreator = new NanopubCreator(targetNamespace);
		npCreator.setAssertionUri(vf.createIRI(targetNamespace + "assertion"));
		assertionContext.propagateStatements(npCreator);
		provenanceContext.propagateStatements(npCreator);
		for (TemplateContext c : pubInfoContexts) {
			c.propagateStatements(npCreator);
		}
		for (IRI introducedIri : assertionContext.getIntroducedIris()) {
			npCreator.addPubinfoStatement(INTRODUCES_PREDICATE, introducedIri);
		}
		npCreator.addNamespace("this", targetNamespace);
		npCreator.addNamespace("sub", targetNamespace + "#");
		npCreator.addTimestampNow();
		IRI templateUri = assertionContext.getTemplate().getNanopub().getUri();
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, templateUri);
		IRI prTemplateUri = provenanceContext.getTemplate().getNanopub().getUri();
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE, prTemplateUri);
		for (TemplateContext c : pubInfoContexts) {
			IRI piTemplateUri = c.getTemplate().getNanopub().getUri();
			npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE, piTemplateUri);
		}
		String nanopubLabel = getNanopubLabel();
		if (nanopubLabel != null) {
			npCreator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral(nanopubLabel));
		}
		for (IRI type : assertionContext.getTemplate().getTargetNanopubTypes()) {
			npCreator.addPubinfoStatement(NANOPUB_TYPE_PREDICATE, type);
		}
		IRI userIri = NanodashSession.get().getUserIri();
		if (User.getName(userIri) != null) {
			npCreator.addPubinfoStatement(userIri, FOAF.NAME, vf.createLiteral(User.getName(userIri)));
			npCreator.addNamespace("foaf", FOAF.NAMESPACE);
		}
		String websiteUrl = NanodashPreferences.get().getWebsiteUrl();
		if (websiteUrl != null) {
			npCreator.addPubinfoStatement(WAS_CREATED_AT_PREDICATE, vf.createIRI(websiteUrl));
		}
		return npCreator.finalizeNanopub();
	}

	private String getNanopubLabel() {
		if (assertionContext.getTemplate().getNanopubLabelPattern() == null) return null;
		String nanopubLabel = assertionContext.getTemplate().getNanopubLabelPattern();
		while (nanopubLabel.matches(".*\\$\\{[_a-zA-Z0-9-]+\\}.*")) {
			String placeholderPostfix = nanopubLabel.replaceFirst("^.*\\$\\{([_a-zA-Z0-9-]+)\\}.*$", "$1");
			IRI placeholderIri = vf.createIRI(assertionContext.getTemplateId() + "#" + placeholderPostfix);
			String placeholderValue = "";
			IModel<String> m = assertionContext.getComponentModels().get(placeholderIri);
			if (m != null) placeholderValue = m.orElse("").getObject();
			if (placeholderValue == null) placeholderValue = "";
			String placeholderLabel = placeholderValue;
			if (assertionContext.getTemplate().isUriPlaceholder(placeholderIri)) {
				try {
					// TODO Fix this. It doesn't work for placeholders with auto-encode placeholders, etc.
					//      Not sure we need labels for these, but this code should be improved anyway.
					String prefix = assertionContext.getTemplate().getPrefix(placeholderIri);
					if (prefix != null) placeholderValue = prefix + placeholderValue;
					IRI placeholderValueIri = vf.createIRI(placeholderValue);
					String l = assertionContext.getTemplate().getLabel(placeholderValueIri);
					if (l == null) l = GuidedChoiceItem.getLabel(placeholderValue);
					if (l != null && !l.isEmpty()) {
						placeholderLabel = l.replaceFirst(" - .*$", "");
					} else {
						placeholderLabel = Utils.getShortNameFromURI(placeholderValueIri);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			placeholderLabel = placeholderLabel.replaceAll("\\s+", " ");
			if (placeholderLabel.length() > 100) placeholderLabel = placeholderLabel.substring(0, 97) + "...";
			nanopubLabel = StringUtils.replace(nanopubLabel, "${" + placeholderPostfix + "}", placeholderLabel);
		}
		return nanopubLabel;
	}

}
