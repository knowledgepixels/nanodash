package org.petapico.nanobench;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
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
import org.nanopub.extra.server.PublishNanopub;
import org.petapico.nanobench.PublishFormContext.ContextType;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import net.trustyuri.TrustyUriException;

public class PublishForm extends Panel {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	protected Form<?> form;
	protected FeedbackPanel feedbackPanel;
	private final PublishFormContext assertionContext;
	private PublishFormContext provenanceContext;
	private List<PublishFormContext> pubInfoContexts = new ArrayList<>();

	public PublishForm(String id, final PageParameters pageParams, final PublishPage page) {
		super(id);

		assertionContext = new PublishFormContext(ContextType.ASSERTION, pageParams.get("template").toString());
		String prTemplateId = pageParams.get("prtemplate").toString();;
		if (prTemplateId == null) {
			prTemplateId = "http://purl.org/np/RANwQa4ICWS5SOjw7gp99nBpXBasapwtZF1fIM3H2gYTM";
		}
		provenanceContext = new PublishFormContext(ContextType.PROVENANCE, prTemplateId);
		String piTemplateId = "http://purl.org/np/RAA2MfqdBCzmz9yVWjKLXNbyfBNcwsMmOqcNUxkk1maIM";
		pubInfoContexts.add(new PublishFormContext(ContextType.PUBINFO, piTemplateId));
		for (String k : pageParams.getNamedKeys()) {
			if (k.startsWith("param_")) assertionContext.setParam(k.substring(6), pageParams.get(k).toString());
			if (k.startsWith("prparam_")) provenanceContext.setParam(k.substring(8), pageParams.get(k).toString());
//			if (k.startsWith("piparam_")) pubInfoContext.setParam(k.substring(8), pageParams.get(k).toString());
		}

		List<Panel> statementItems = assertionContext.makeStatementItems("statement");

		final CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(false));
		consentCheck.setRequired(true);
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
					Nanopub signedNp = SignNanopub.signAndTransform(np, SignatureAlgorithm.RSA, ProfilePage.getKeyPair());
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
					params.add("id", ProfilePage.getUserIri().stringValue());
					throw new RestartResponseException(new PublishConfirmPage(signedNp));
				} catch (IOException | MalformedNanopubException | GeneralSecurityException | TrustyUriException ex) {
					ex.printStackTrace();
				}
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				for (FormComponent<String> fc : assertionContext.getFormComponents()) {
					fc.processInput();
					for (FeedbackMessage fm : fc.getFeedbackMessages()) {
						form.getFeedbackMessages().add(fm);
					}
				}
			}

		};
		form.setOutputMarkupId(true);

		form.add(new ExternalLink("templatelink", assertionContext.getTemplate().getId()));
		form.add(new Label("templatename", assertionContext.getTemplate().getLabel()));

		form.add(new ListView<Panel>("statements", statementItems) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<Panel> item) {
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
				provenanceContext = new PublishFormContext(ContextType.PROVENANCE, prTemplateModel.getObject());
				refreshProvenance(target);
			}

		});
		form.add(prTemplateChoice);
		refreshProvenance(null);

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
		ExternalLink link = new ExternalLink("prtemplatelink", provenanceContext.getTemplate().getId());
		List<Panel> provStatementItems = provenanceContext.makeStatementItems("pr-statement");
		ListView<Panel> list = new ListView<Panel>("pr-statements", provStatementItems) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<Panel> item) {
				item.add(item.getModelObject());
			}

		};
		list.setOutputMarkupId(true);
		if (target == null) {
			form.add(link);
			form.add(list);
		} else {
			form.remove("prtemplatelink");
			form.add(link);
			form.remove("pr-statements");
			form.add(list);
			target.add(form);
		}
	}

	private void refreshPubInfo(AjaxRequestTarget target) {
		ListView<PublishFormContext> list = new ListView<PublishFormContext>("pis", pubInfoContexts) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<PublishFormContext> item) {
				PublishFormContext pic = item.getModelObject();
				item.add(new ExternalLink("pitemplatelink", pic.getTemplate().getId()));
				List<Panel> pubinfoStatementItems = pic.makeStatementItems("pi-statement");
				item.add(new ListView<Panel>("pi-statements", pubinfoStatementItems) {

					private static final long serialVersionUID = 1L;

					protected void populateItem(ListItem<Panel> item) {
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
		npCreator.addNamespace("this", "http://purl.org/nanopub/temp/nanobench-new-nanopub/");
		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/nanobench-new-nanopub/#");
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
