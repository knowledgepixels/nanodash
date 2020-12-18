package org.petapico.nanobench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
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
	private List<WebMarkupContainer> allStatements = new ArrayList<>();
	private List<RepetitionGroup> repetitionGroups = new ArrayList<>();
	private Set<IRI> iriSet = new HashSet<>();
	private List<ValueItem> items = new ArrayList<>();

	public StatementItem(String id, IRI statementId, PublishFormContext context) {
		super(id);
		this.statementId = statementId;
		this.context = context;

		if (isGrouped()) {
			statementPartIds.addAll(getTemplate().getStatementIris(statementId));
		} else {
			statementPartIds.add(statementId);
		}

		repeat();

		ListView<WebMarkupContainer> v = new ListView<WebMarkupContainer>("statement-group", allStatements) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<WebMarkupContainer> item) {
				item.add(item.getModelObject());
			}

		};
		v.setOutputMarkupId(true);
		add(v);
	}

	private void repeat() {
		RepetitionGroup rg = new RepetitionGroup();
		repetitionGroups.add(rg);
		refreshStatements();
	}

	private void refreshStatements() {
		allStatements.clear();
		for (ValueItem vi : items) {
			vi.removeFromContext();
		}
		items.clear();
		for (RepetitionGroup r : repetitionGroups) {
			r.refresh();
			allStatements.addAll(r.getStatements());
		}
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
		return repetitionGroups.size() == 1 && getTemplate().isOptionalStatement(statementId);
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

	public Set<IRI> getIriSet() {
		return iriSet;
	}


	public class RepetitionGroup implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<WebMarkupContainer> statements;

		public RepetitionGroup() {
		}

		public void refresh() {
			statements = new ArrayList<>();
			for (IRI s : statementPartIds) {
				WebMarkupContainer statement = new WebMarkupContainer("statement");
				IRI subj = getTemplate().getSubject(s);
				IRI pred = getTemplate().getPredicate(s);
				IRI obj = (IRI) getTemplate().getObject(s);
				if (isFirst()) {
					iriSet.add(subj);
					iriSet.add(pred);
					iriSet.add(obj);
				}
				makeValueItem("subj", subj, statement);
				makeValueItem("pred", pred, statement);
				makeValueItem("obj", obj, statement);
				statements.add(statement);
				if (statements.size() == 1 && !isFirst()) {
					statement.add(new AttributeAppender("class", " separate-statement"));
				}
				if (isFirst() && isOptional() && statements.size() == statementPartIds.size()) {
					statement.add(new Label("label", "(optional)"));
				} else {
					statement.add(new Label("label", "").setVisible(false));
				}
				if (isRepeatable() && statements.size() == 1 && isFirst()) {
					statement.add(new Link<Object>("add-repetition") {
						private static final long serialVersionUID = 1L;
						public void onClick() {
							repeat();
						};
					});
				} else {
					Label l = new Label("add-repetition", "");
					l.setVisible(false);
					statement.add(l);
				}
				if (isRepeatable() && statements.size() == 1 && isLast() && !isFirst()) {
					statement.add(new Link<Object>("remove-repetition") {
						private static final long serialVersionUID = 1L;
						public void onClick() {
							repetitionGroups.remove(RepetitionGroup.this);
							refreshStatements();
						};
					});
				} else {
					Label l = new Label("remove-repetition", "");
					l.setVisible(false);
					statement.add(l);
				}
			}
		}

		private void makeValueItem(String id, IRI iri, WebMarkupContainer statement) {
			ValueItem vi = new ValueItem(id, iri, this);
			items.add(vi);
			statement.add(vi);
		}

		public List<WebMarkupContainer> getStatements() {
			return statements;
		}

		public int getRepeatIndex() {
			return repetitionGroups.indexOf(this);
		}

		public boolean isFirst() {
			return getRepeatIndex() == 0;
		}

		public boolean isLast() {
			return getRepeatIndex() == repetitionGroups.size() - 1;
		}

		public PublishFormContext getContext() {
			return context;
		}

		public boolean isOptional() {
			return StatementItem.this.isOptional();
		}

	}

}
