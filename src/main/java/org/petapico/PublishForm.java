package org.petapico;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;

public class PublishForm extends Panel {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();
	protected IRI userIri = vf.createIRI("https://orcid.org/0000-0002-1267-0234");  // TODO: Use actual user ID here

	protected Template template;
	protected Map<IRI,IModel<String>> formComponentModels = new HashMap<>();
	protected Set<FormComponent<String>> formComponents = new HashSet<>();
	protected Form<?> form;
	protected FeedbackPanel feedbackPanel;

	public PublishForm(String id, final String templateId) {
		super(id);
		template = Template.getTemplate(templateId);
		add(new ExternalLink("templatelink", templateId));
		add(new Label("templatename", template.getLabel()));
		List<List<IRI>> statements = new ArrayList<>();
		for (IRI st : template.getStatementIris()) {
			List<IRI> triple = new ArrayList<>();
			triple.add(template.getSubject(st));
			triple.add(template.getPredicate(st));
			triple.add((IRI) template.getObject(st));
			statements.add(triple);
		}

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
				formComponents.clear();
			}

			protected void onSubmit() {
				try {
					Nanopub np = createNanopub();
					KeyPair keyPair = SignNanopub.loadKey("~/.nanopub/id_rsa", SignatureAlgorithm.RSA);
					Nanopub signedNp = SignNanopub.signAndTransform(np, SignatureAlgorithm.RSA, keyPair);
					System.err.println(NanopubUtils.writeToString(signedNp, RDFFormat.TRIG));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				for (FormComponent<String> fc : formComponents) {
					fc.processInput();
					for (FeedbackMessage fm : fc.getFeedbackMessages()) {
						form.getFeedbackMessages().add(fm);
					}
				}
			}

		};

		form.add(new ListView<List<IRI>>("statements", statements) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<List<IRI>> item) {
				item.add(new StatementItem("statement", item.getModelObject(), PublishForm.this));
			}

		});

		form.add(consentCheck);
		add(form);

		feedbackPanel = new FeedbackPanel("feedback");
		feedbackPanel.setOutputMarkupId(true);
		add(feedbackPanel);
	}

	private Nanopub createNanopub() throws MalformedNanopubException {
		NanopubCreator npCreator = new NanopubCreator(vf.createIRI("http://purl.org/nanopub/temp/"));
		if (template.getNanopub() instanceof NanopubWithNs) {
			NanopubWithNs np = (NanopubWithNs) template.getNanopub();
			for (String p : np.getNsPrefixes()) {
				npCreator.addNamespace(p, np.getNamespace(p));
			}
		}
		npCreator.addNamespace("this", "http://purl.org/nanopub/temp/");
		npCreator.addNamespace("sub", "http://purl.org/nanopub/temp/#");
		for (IRI st : template.getStatementIris()) {
			npCreator.addAssertionStatement(processIri(template.getSubject(st)),
					processIri(template.getPredicate(st)),
					processValue(template.getObject(st)));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, userIri);
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, userIri);
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, template.getNanopub().getUri());
		return npCreator.finalizeNanopub();
	}

	private IRI processIri(IRI iri) {
		Value v = processValue(iri);
		if (v instanceof IRI) return (IRI) v;
		return iri;
	}

	private Value processValue(Value value) {
		if (!(value instanceof IRI)) return value;
		IRI iri = (IRI) value;
		if (iri.stringValue().startsWith("https://w3id.org/np/o/ntemplate/local/")) {
			return vf.createIRI(iri.stringValue().replaceFirst("^https://w3id.org/np/o/ntemplate/local/", "http://purl.org/nanopub/temp/"));
		}
		if (template.isUriPlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
				return vf.createIRI(tf.getObject());
			}
		} else if (template.isLiteralPlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
				return vf.createLiteral(tf.getObject());
			}
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
				if (tf.getObject().matches("https?://.*")) {
					return vf.createIRI(tf.getObject());
				}
				return vf.createLiteral(tf.getObject());
			}
		}
		return iri;
	}

}
