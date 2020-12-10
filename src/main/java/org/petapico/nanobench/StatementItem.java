package org.petapico.nanobench;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.petapico.nanobench.PublishFormContext.ContextType;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	private boolean optional = false;
	private PublishFormContext context;
	private IRI subj, pred, obj;

	public StatementItem(String id, IRI statementId, PublishFormContext context, boolean optional) {
		super(id);
		this.optional = optional;
		this.context = context;

		WebMarkupContainer statement = new WebMarkupContainer("statement");
		if (optional) {
			statement.add(new AttributeModifier("class", "nanopub-optional"));
		}
		Template template = context.getTemplate();
		subj = template.getSubject(statementId);
		pred = template.getPredicate(statementId);
		obj = (IRI) template.getObject(statementId);
		statement.add(new ValueItem("subj", subj, true, context));
		statement.add(new ValueItem("pred", pred, true, context));
		statement.add(new ValueItem("obj", obj, true, context));
		if (optional) {
			statement.add(new Label("label", "(optional)"));
		} else {
			statement.add(new Label("label", ""));
		}
		add(statement);
	}

	public StatementItem(String id, IRI subj, IRI pred, Value obj, Nanopub np) {
		super(id);

		WebMarkupContainer statement = new WebMarkupContainer("statement");
		statement.add(new NanobenchLink("subj", subj.stringValue(), np));
		statement.add(new NanobenchLink("pred", pred.stringValue(), np));
		if (obj instanceof IRI) {
			statement.add(new NanobenchLink("obj", obj.stringValue(), np));
		} else {
			statement.add(new Label("obj", "\"" + obj.stringValue() + "\""));
		}
		statement.add(new Label("label", ""));
		add(statement);
	}

	public boolean isOptional() {
		return optional;
	}

	public IRI getSubject() {
		return subj;
	}

	public IRI getPredicate() {
		return pred;
	}

	public IRI getObject() {
		return obj;
	}

	public void addStatementTo(NanopubCreator npCreator) throws MalformedNanopubException {
		IRI pSubj = context.processIri(getSubject());
		IRI pPred = context.processIri(getPredicate());
		Value pObj = context.processValue(getObject());
		if (pSubj == null || pPred == null || pObj == null) {
			if (optional) {
				return;
			} else {
				throw new MalformedNanopubException("Field of statement not set.");
			}
		}
		if (context.getType() == ContextType.ASSERTION) {
			npCreator.addAssertionStatement(pSubj, pPred, pObj);
		} else if (context.getType() == ContextType.PROVENANCE) {
			npCreator.addProvenanceStatement(pSubj, pPred, pObj);
		} else if (context.getType() == ContextType.PUBINFO) {
			npCreator.addPubinfoStatement(pSubj, pPred, pObj);
		}
	}

}
