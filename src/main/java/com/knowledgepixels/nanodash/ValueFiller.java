package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.CryptoElement;
import org.nanopub.extra.security.NanopubSignatureElement;

public class ValueFiller {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private Nanopub fillNp;
	private List<Statement> unusedStatements = new ArrayList<>();
	private int initialSize;
	private boolean formMode;

	public ValueFiller(Nanopub fillNp, ContextType contextType, boolean formMode) {
		this.fillNp = fillNp;
		this.formMode = formMode;
		Set<Statement> statements;
		if (contextType == ContextType.ASSERTION) {
			statements = fillNp.getAssertion();
		} else if (contextType == ContextType.PROVENANCE) {
			statements = fillNp.getProvenance();
		} else {
			statements = fillNp.getPubinfo();
		}
		for (Statement st : statements) {
			Statement stT = transform(st);
			if (stT != null) unusedStatements.add(stT);
		}
		Collections.sort(unusedStatements, new Comparator<Statement>() {
			@Override
			public int compare(Statement st1, Statement st2) {
				String st1s = st1.getSubject() + " " + st1.getPredicate() + " " + st1.getObject();
				String st2s = st2.getSubject() + " " + st2.getPredicate() + " " + st2.getObject();
				return st1s.compareTo(st2s);
			}
		});
		initialSize = unusedStatements.size();
	}

	public void fill(PublishFormContext context) {
		try {
			context.fill(unusedStatements);
		} catch (UnificationException ex) {
			ex.printStackTrace();
		}
//		if (unusedStatements.size() == initialSize) {
//			warningMessage = "Could not fill in form with content from given existing nanopublication.";
//		} else if (!statements.isEmpty()) {
//			warningMessage = "Content from given existing nanopublication could only partially be filled in.";
//		}
	}

	public boolean hasStatements() {
		return initialSize > 0;
	}

	public boolean hasUsedStatements() {
		return unusedStatements.size() < initialSize;
	}

	public boolean hasUnusedStatements() {
		return unusedStatements.size() > 0;
	}

	public List<Statement> getUnusedStatements() {
		return unusedStatements;
	}

	private Statement transform(Statement st) {
		if (formMode && st.getContext().equals(fillNp.getPubinfoUri())) {
			IRI pred = st.getPredicate();
			if (st.getSubject().equals(fillNp.getUri())) {
				if (pred.equals(DCTERMS.CREATED)) return null;
				if (pred.equals(Nanopub.SUPERSEDES)) return null;
				if (pred.equals(RDFS.LABEL)) return null;
				if (pred.equals(PublishForm.NANOPUB_TYPE_PREDICATE)) return null;
				if (pred.equals(PublishForm.INTRODUCES_PREDICATE)) return null;
				if (pred.equals(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE)) return null;
				if (pred.equals(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE)) return null;
				if (pred.equals(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE)) return null;
			}
			if (pred.equals(CryptoElement.HAS_ALGORITHM)) return null;
			if (pred.equals(CryptoElement.HAS_PUBLIC_KEY)) return null;
			if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE)) return null;
			if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) return null;
			if (pred.equals(Template.HAS_LABEL_FROM_API)) {
				GuidedChoiceItem.setLabel(st.getSubject().stringValue(), st.getObject().stringValue());
				return null;
			}
		}
		return vf.createStatement(
				(Resource) transform(st.getSubject()),
				(IRI) transform(st.getPredicate()),
				transform(st.getObject()),
				(Resource) transform(st.getContext()));
	}

	private Value transform(Value v) {
		if (fillNp.getUri().equals(v)) {
			return Template.NANOPUB_PLACEHOLDER;
		} else if (fillNp.getAssertionUri().equals(v)) {
			return Template.ASSERTION_PLACEHOLDER;
		} else if (v instanceof IRI) {
			// TODO: Check that there are no regex characters in nanopub URI:
			return vf.createIRI(v.stringValue().replaceFirst("^" + fillNp.getUri().stringValue() + "[#/]?", "local:"));
		}
		return v;
	}

}
