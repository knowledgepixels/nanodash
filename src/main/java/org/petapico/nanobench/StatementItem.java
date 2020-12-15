package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
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

	private final boolean optional;
	private PublishFormContext context;
	private IRI subj, pred, obj;

	public StatementItem(String id, IRI statementId, PublishFormContext context) {
		super(id);
		this.context = context;

		Template template = context.getTemplate();
		optional = template.isOptionalStatement(statementId);

		List<IRI> statementPartIds = new ArrayList<>();
		statementPartIds.add(statementId);
		// TODO: more statement parts of a grouped statement can be added here later

		List<WebMarkupContainer> statements = new ArrayList<>();
		for (IRI s : statementPartIds) {
			WebMarkupContainer statement = new WebMarkupContainer("statement");
			subj = template.getSubject(s);
			pred = template.getPredicate(s);
			obj = (IRI) template.getObject(s);
			statement.add(new ValueItem("subj", subj, optional, context));
			statement.add(new ValueItem("pred", pred, optional, context));
			statement.add(new ValueItem("obj", obj, optional, context));
			statements.add(statement);
			if (optional && statements.size() == statementPartIds.size()) {
				statement.add(new Label("label", "(optional)"));
			} else {
				statement.add(new Label("label", "").setVisible(false));
			}
		}

		ListView<WebMarkupContainer> v = new ListView<WebMarkupContainer>("statement-group", statements) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<WebMarkupContainer> item) {
				item.add(item.getModelObject());
			}

		};
		v.setOutputMarkupId(true);
		add(v);

		if (optional) {
			add(new AttributeModifier("class", "nanopub-optional"));
		}

	}

	public void addTriplesTo(NanopubCreator npCreator) throws MalformedNanopubException {
		IRI pSubj = context.processIri(subj);
		IRI pPred = context.processIri(pred);
		Value pObj = context.processValue(obj);
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
