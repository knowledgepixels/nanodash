package org.petapico;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.IntegerLiteral;
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

	protected IRI userIri = vf.createIRI("https://orcid.org/0000-0002-1267-0234");  // TODO: Use actual user ID here

	protected Nanopub templateNanopub;
	protected String templateLabel;
	protected Map<IRI,List<IRI>> typeMap = new HashMap<>();
	protected Map<IRI,String> labelMap = new HashMap<>();
	protected Map<IRI,Boolean> templateStatementIris = new HashMap<>();
	protected Map<IRI,IRI> templateStatementSubjects = new HashMap<>();
	protected Map<IRI,IRI> templateStatementPredicates = new HashMap<>();
	protected Map<IRI,Value> templateStatementObjects = new HashMap<>();
	protected Map<IRI,Integer> templateStatementOrder = new HashMap<>();
	protected Map<IRI,IModel<String>> textFieldModels = new HashMap<>();
	protected List<TextField<String>> textFields = new ArrayList<>();
	protected Form<?> form;

	public PublishPage(final PageParameters parameters) {
		super();
		final String templateId = parameters.get("template").toString();
		templateNanopub = GetNanopub.get(templateId);
		processTemplate(templateNanopub);
		add(new Label("templatename", templateLabel));
		List<List<IRI>> statements = new ArrayList<>();
		List<IRI> statementIris = new ArrayList<>(templateStatementIris.keySet());
		statementIris.sort(new Comparator<IRI>() {
			@Override
			public int compare(IRI arg0, IRI arg1) {
				Integer i0 = templateStatementOrder.get(arg0);
				Integer i1 = templateStatementOrder.get(arg1);
				if (i0 == null && i1 == null) return arg0.stringValue().compareTo(arg1.stringValue());
				if (i0 == null) return 1;
				if (i1 == null) return -1;
				return i0-i1;
			}
		});
		for (IRI st : statementIris) {
			List<IRI> triple = new ArrayList<>();
			triple.add(processIri(templateStatementSubjects.get(st), false));
			triple.add(processIri(templateStatementPredicates.get(st), false));
			triple.add(processIri((IRI) templateStatementObjects.get(st), false));
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
				item.add(new StatementItem("statement", item.getModelObject(), PublishPage.this));
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
					processValue(templateStatementObjects.get(st), true));
		}
		npCreator.addProvenanceStatement(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO, userIri);
		npCreator.addTimestampNow();
		npCreator.addPubinfoStatement(SimpleCreatorPattern.DCT_CREATOR, userIri);
		npCreator.addPubinfoStatement(WAS_CREATED_FROM_TEMPLATE_PREDICATE, templateNanopub.getUri());
		return npCreator.finalizeNanopub();
	}

	private IRI processIri(IRI iri, boolean fillPlaceholders) {
		Value v = processValue(iri, fillPlaceholders);
		if (v instanceof IRI) return (IRI) v;
		return iri;
	}

	private Value processValue(Value value, boolean fillPlaceholders) {
		if (!(value instanceof IRI)) return value;
		IRI iri = (IRI) value;
		if (fillPlaceholders && typeMap.containsKey(iri)) {
			if (typeMap.get(iri).contains(URI_PLACEHOLDER_CLASS)) {
				IModel<String> tf = textFieldModels.get(iri);
				if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
					return vf.createIRI(tf.getObject());
				}
			} else if (typeMap.get(iri).contains(LITERAL_PLACEHOLDER_CLASS)) {
				IModel<String> tf = textFieldModels.get(iri);
				if (tf != null && tf.getObject() != null && !tf.getObject().isBlank()) {
					return vf.createLiteral(tf.getObject());
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
			} else if (st.getPredicate().equals(RDFS.LABEL) && st.getObject() instanceof Literal) {
				labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
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
				} else if (st.getPredicate().equals(STATEMENT_ORDER_PREDICATE)) {
					if (st.getObject() instanceof IntegerLiteral) {
						templateStatementOrder.put((IRI) st.getSubject(), ((IntegerLiteral) st.getObject()).intValue());
					}
				}
			}
		}
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");
	public static final IRI CREATOR_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/CREATOR");
	public static final IRI WAS_CREATED_FROM_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");
	public static final IRI STATEMENT_ORDER_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/statementOrder");

}
