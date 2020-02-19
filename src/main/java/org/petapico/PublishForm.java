package org.petapico;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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
	protected List<FormComponent<String>> formComponents = new ArrayList<>();
	protected Form<?> form;

	public PublishForm(String id, final String templateId) {
		super(id);
		template = Template.getTemplate(templateId);
		add(new Label("templatename", template.getLabel()));
		List<List<IRI>> statements = new ArrayList<>();
		for (IRI st : template.getStatementIris()) {
			List<IRI> triple = new ArrayList<>();
			triple.add(processIri(template.getSubject(st), false));
			triple.add(processIri(template.getPredicate(st), false));
			triple.add(processIri((IRI) template.getObject(st), false));
			statements.add(triple);
		}

		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

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
			
		};

		form.add(new ListView<List<IRI>>("statements", statements) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<List<IRI>> item) {
				item.add(new StatementItem("statement", item.getModelObject(), PublishForm.this));
			}

		});
		add(form);
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
			npCreator.addAssertionStatement(processIri(template.getSubject(st), true),
					processIri(template.getPredicate(st), true),
					processValue(template.getObject(st), true));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, userIri);
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, userIri);
		npCreator.addPubinfoStatement(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, template.getNanopub().getUri());
		return npCreator.finalizeNanopub();
	}

	private IRI processIri(IRI iri, boolean fillPlaceholders) {
		Value v = processValue(iri, fillPlaceholders);
		if (v instanceof IRI) return (IRI) v;
		return iri;
	}

	private Value processValue(Value value, boolean fillPlaceholders) {
		if (!fillPlaceholders) return value;
		if (!(value instanceof IRI)) return value;
		IRI iri = (IRI) value;
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
