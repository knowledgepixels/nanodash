package org.petapico.nanobench;

import static org.petapico.nanobench.PublishFormContext.ContextType.ASSERTION;
import static org.petapico.nanobench.PublishFormContext.ContextType.PROVENANCE;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;

public class ValueFiller {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private Nanopub fillNp;
	private PublishFormContext context;
	private String warningMessage;

	public ValueFiller(Nanopub fillNp, PublishFormContext context) {
		this.fillNp = fillNp;
		this.context = context;
	}

	public void fill() {
		Set<Statement> statements = new HashSet<>();
		Set<Statement> originalStatements;
		if (context.getType() == ASSERTION) {
			originalStatements = fillNp.getAssertion();
		} else if (context.getType() == PROVENANCE) {
			originalStatements = fillNp.getProvenance();
		} else {
			originalStatements = fillNp.getPubinfo();
		}
		for (Statement st : originalStatements) {
			statements.add(transform(st));
		}
		try {
			context.fill(statements);
		} catch (UnificationException ex) {
			ex.printStackTrace();
		}
		if (originalStatements.size() == statements.size()) {
			warningMessage = "Could not fill in form with content from given existing nanopublication.";
		} else if (!statements.isEmpty()) {
			warningMessage = "Content from given existing nanopublication could only partially be filled in.";
		}
	}

	public String getWarningMessage() {
		return warningMessage;
	}

	private Statement transform(Statement st) {
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
			return vf.createIRI(v.stringValue().replaceFirst("^" + fillNp.getUri().stringValue(), context.getTemplateId()));
		}
		return v;
	}

}
