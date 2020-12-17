package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubCreator;
import org.petapico.nanobench.PublishFormContext.ContextType;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	private PublishFormContext context;
	private IRI statementId;
	private List<IRI> statementPartIds = new ArrayList<>();
	private List<WebMarkupContainer> statements = new ArrayList<>();

	public StatementItem(String id, IRI statementId, PublishFormContext context) {
		super(id);
		this.statementId = statementId;
		this.context = context;

		if (isGrouped()) {
			statementPartIds.addAll(getTemplate().getStatementIris(statementId));
		} else {
			statementPartIds.add(statementId);
		}

		createStatements();

		ListView<WebMarkupContainer> v = new ListView<WebMarkupContainer>("statement-group", statements) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<WebMarkupContainer> item) {
				item.add(item.getModelObject());
			}

		};
		v.setOutputMarkupId(true);
		add(v);

		String htmlClassString = "";
		if (isOptional()) {
			htmlClassString += "nanopub-optional ";
		}
		if (isGrouped()) {
			htmlClassString += "nanopub-group ";
		}
		if (!htmlClassString.isEmpty()) {
			add(new AttributeModifier("class", htmlClassString));
		}

	}

	private void createStatements() {
		for (IRI s : statementPartIds) {
			WebMarkupContainer statement = new WebMarkupContainer("statement");
			IRI subj = getTemplate().getSubject(s);
			IRI pred = getTemplate().getPredicate(s);
			IRI obj = (IRI) getTemplate().getObject(s);
			statement.add(new ValueItem("subj", subj, isOptional(), context));
			statement.add(new ValueItem("pred", pred, isOptional(), context));
			statement.add(new ValueItem("obj", obj, isOptional(), context));
			statements.add(statement);
			if (isOptional() && statements.size() == statementPartIds.size()) {
				statement.add(new Label("label", "(optional)"));
			} else {
				statement.add(new Label("label", "").setVisible(false));
			}
			if (isRepeatable() && statements.size() == 1) {
				statement.add(new Link<Object>("add-repetition") {
					private static final long serialVersionUID = 1L;
					public void onClick() {
						// TODO: This doesn't really work yet
						createStatements();
					};
				});
			} else {
				Label l = new Label("add-repetition", "");
				l.setVisible(false);
				statement.add(l);
			}
		}
	}

	public void addTriplesTo(NanopubCreator npCreator) throws MalformedNanopubException {
		if (hasEmptyElements()) {
			if (isOptional()) {
				return;
			} else {
				throw new MalformedNanopubException("Field of statement not set.");
			}
		}
		for (IRI s : statementPartIds) {
			IRI pSubj = context.processIri(getTemplate().getSubject(s));
			IRI pPred = context.processIri(getTemplate().getPredicate(s));
			Value pObj = context.processValue(getTemplate().getObject(s));
			if (context.getType() == ContextType.ASSERTION) {
				npCreator.addAssertionStatement(pSubj, pPred, pObj);
			} else if (context.getType() == ContextType.PROVENANCE) {
				npCreator.addProvenanceStatement(pSubj, pPred, pObj);
			} else if (context.getType() == ContextType.PUBINFO) {
				npCreator.addPubinfoStatement(pSubj, pPred, pObj);
			}
		}
	}

	private Template getTemplate() {
		return context.getTemplate();
	}

	private boolean isOptional() {
		return getTemplate().isOptionalStatement(statementId);
	}

	private boolean isGrouped() {
		return getTemplate().isGroupedStatement(statementId);
	}

	private boolean isRepeatable() {
		return getTemplate().isRepeatableStatement(statementId);
	}

	private boolean hasEmptyElements() {
		for (IRI s : statementPartIds) {
			if (context.processIri(getTemplate().getSubject(s)) == null) return true;
			if (context.processIri(getTemplate().getPredicate(s)) == null) return true;
			if (context.processValue(getTemplate().getObject(s)) == null) return true;
		}
		return false;
	}

}
