package org.petapico;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.server.GetNanopub;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private IRI userIri = vf.createIRI("https://orcid.org/0000-0002-1267-0234");  // TODO: Use actual user ID here

	private Nanopub templateNanopub;
	private String templateLabel;
	private Map<IRI,List<IRI>> typeMap = new HashMap<>();
	private Map<IRI,Boolean> templateStatementIris = new HashMap<>();
	private Map<IRI,IRI> templateStatementSubjects = new HashMap<>();
	private Map<IRI,IRI> templateStatementPredicates = new HashMap<>();
	private Map<IRI,Value> templateStatementObjects = new HashMap<>();
	private Map<IRI,IModel<String>> textFieldModels = new HashMap<>();

	public PublishPage(final PageParameters parameters) {
		super();
		final String templateId = parameters.get("template").toString();
		templateNanopub = GetNanopub.get(templateId);
		processTemplate(templateNanopub);
		add(new Label("templatename", templateLabel));
		List<List<IRI>> statements = new ArrayList<>();
		for (IRI st : templateStatementIris.keySet()) {
			List<IRI> triple = new ArrayList<>();
			triple.add(processIri(templateStatementSubjects.get(st), false));
			triple.add(processIri(templateStatementPredicates.get(st), false));
			triple.add(processIri((IRI) templateStatementObjects.get(st), false));
			statements.add(triple);
		}

		Form<?> form = new Form<Void>("form") {

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
				item.add(new StatementItem("statement", item.getModelObject(), typeMap, textFieldModels));
			}
			
		});
		add(form);
	}

	private Nanopub createNanopub() throws MalformedNanopubException {
		NanopubCreator npCreator = new NanopubCreator(vf.createIRI("http://purl.org/np/"));
		npCreator.addNamespace("prov", vf.createIRI("http://www.w3.org/ns/prov#"));
		for (IRI st : templateStatementIris.keySet()) {
			npCreator.addAssertionStatement(processIri(templateStatementSubjects.get(st), true),
					processIri(templateStatementPredicates.get(st), true),
					processIri((IRI) templateStatementObjects.get(st), true));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, userIri);
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, userIri);
		npCreator.addPubinfoStatement(WAS_CREATED_FROM_TEMPLATE_PREDICATE, templateNanopub.getUri());
		return npCreator.finalizeNanopub();
	}

	private IRI processIri(IRI iri, boolean fillPlaceholders) {
		if (iri.equals(CREATOR_PLACEHOLDER)) {
			return userIri;
		}
		if (fillPlaceholders) {
			if (typeMap.containsKey(iri) && typeMap.get(iri).contains(URI_PLACEHOLDER_CLASS)) {
				IModel<String> tf = textFieldModels.get(iri);
				if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
					return vf.createIRI(tf.getObject());
				}
			}
		}
		return iri;
	}

	private void processTemplate(Nanopub templateNp) {
		for (Statement st : templateNp.getAssertion()) {
			if (st.getSubject().equals(templateNp.getAssertionUri())) {
				if (st.getPredicate().equals(RDFS.LABEL)) {
					templateLabel = st.getObject().stringValue();
				} else if (st.getPredicate().equals(HAS_STATEMENT_PREDICATE) && st.getObject() instanceof IRI) {
					templateStatementIris.put((IRI) st.getObject(), true);
				}
			}
			if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
				List<IRI> l = typeMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					typeMap.put((IRI) st.getSubject(), l);
				}
				l.add((IRI) st.getObject());
			}
		}
		for (Statement st : templateNp.getAssertion()) {
			if (templateStatementIris.containsKey(st.getSubject())) {
				if (st.getPredicate().equals(RDF.SUBJECT) && st.getObject() instanceof IRI) {
					templateStatementSubjects.put((IRI) st.getSubject(), (IRI) st.getObject());
				} else if (st.getPredicate().equals(RDF.PREDICATE) && st.getObject() instanceof IRI) {
					templateStatementPredicates.put((IRI) st.getSubject(), (IRI) st.getObject());
				} else if (st.getPredicate().equals(RDF.OBJECT)) {
					templateStatementObjects.put((IRI) st.getSubject(), st.getObject());
				}
			}
		}
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI CREATOR_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/CREATOR");
	public static final IRI WAS_CREATED_FROM_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");

}
