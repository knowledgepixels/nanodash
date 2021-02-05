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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubCreator;

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
		if (isGrouped() || isRepeatable()) {
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
		for (RepetitionGroup rg : repetitionGroups) {
			rg.addTriplesTo(npCreator);
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
		return repetitionGroups.get(0).hasEmptyElements();
	}

	public Set<IRI> getIriSet() {
		return iriSet;
	}

	public void fill(List<Statement> statements) throws UnificationException {
		boolean hasMatch = false;
		for (RepetitionGroup rg : repetitionGroups) {
			hasMatch = rg.canMatch(statements);
			if (hasMatch) {
				rg.fill(statements);
			}
		}
		if (!hasMatch) return;
		while (true) {
			RepetitionGroup newGroup = new RepetitionGroup();
			newGroup.refresh();
			if (newGroup.canMatch(statements)) {
				newGroup.fill(statements);
				repetitionGroups.add(newGroup);
				refreshStatements();
			} else {
				return;
			}
		}
	}


	public class RepetitionGroup implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<StatementPartItem> statements;

		public RepetitionGroup() {
		}

		public void refresh() {
			statements = new ArrayList<>();
			for (IRI s : statementPartIds) {
				StatementPartItem statement = new StatementPartItem("statement",
						makeValueItem("subj", getTemplate().getSubject(s)),
						makeValueItem("pred", getTemplate().getPredicate(s)),
						makeValueItem("obj", (IRI) getTemplate().getObject(s))
					);
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

		private ValueItem makeValueItem(String id, IRI iri) {
			if (isFirst()) {
				iriSet.add(iri);
			}
			ValueItem vi = new ValueItem(id, transform(iri), this);
			items.add(vi);
			return vi;
		}

		public List<StatementPartItem> getStatements() {
			return statements;
		}

		public int getRepeatIndex() {
			if (!repetitionGroups.contains(this)) return repetitionGroups.size();
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

		private IRI transform(IRI iri) {
			// Only add "__N" to URI from second repetition group on; for the first group, information about
			// narrow scopes is not yet complete.
			if (getRepeatIndex() > 0 && context.hasNarrowScope(iri)) {
				if (context.getTemplate().isPlaceholder(iri) || context.getTemplate().isLocalResource(iri)) {
					// TODO: Check that this double-underscore pattern isn't used otherwise:
					return vf.createIRI(iri.stringValue() + "__" + getRepeatIndex());
				}
			}
			return iri;
		}

		public void addTriplesTo(NanopubCreator npCreator) {
			for (IRI s : statementPartIds) {
				IRI pSubj = context.processIri(transform(getTemplate().getSubject(s)));
				IRI pPred = context.processIri(transform(getTemplate().getPredicate(s)));
				Value pObj = context.processValue(transform((IRI) getTemplate().getObject(s)));
				if (context.getType() == ContextType.ASSERTION) {
					npCreator.addAssertionStatement(pSubj, pPred, pObj);
				} else if (context.getType() == ContextType.PROVENANCE) {
					npCreator.addProvenanceStatement(pSubj, pPred, pObj);
				} else if (context.getType() == ContextType.PUBINFO) {
					npCreator.addPubinfoStatement(pSubj, pPred, pObj);
				}
			}
		}

		private boolean hasEmptyElements() {
			for (IRI s : statementPartIds) {
				if (context.processIri(transform(getTemplate().getSubject(s))) == null) return true;
				if (context.processIri(transform(getTemplate().getPredicate(s))) == null) return true;
				if (context.processValue(transform((IRI) getTemplate().getObject(s))) == null) return true;
			}
			return false;
		}

		public boolean canMatch(List<Statement> st) {
			//System.err.println("Try to match repetition group...");
			for (StatementPartItem p : statements) {
				//System.err.println("Try to match: " + p);
				boolean matchFound = false;
				for (Statement s : st) {
					//System.err.println("Checking statement: " + s.getSubject() + " " + s.getPredicate() + " " + s.getObject());
					if (
							p.getSubject().isUnifiableWith(s.getSubject()) &&
							p.getPredicate().isUnifiableWith(s.getPredicate()) &&
							p.getObject().isUnifiableWith(s.getObject())) {
						matchFound = true;
						//System.err.println("Statement matched.");
						break;
					}
				}
				if (!matchFound) {
					//System.err.println("Not matched.");
					return false;
				}
			}
			//System.err.println("Repetition group matched.");
			return true;
		}

		public void fill(List<Statement> st) throws UnificationException {
			for (StatementPartItem p : statements) {
				boolean matchFound = false;
				for (Statement s : st) {
					if (
							p.getSubject().isUnifiableWith(s.getSubject()) &&
							p.getPredicate().isUnifiableWith(s.getPredicate()) &&
							p.getObject().isUnifiableWith(s.getObject())) {
						p.getSubject().unifyWith(s.getSubject());
						p.getPredicate().unifyWith(s.getPredicate());
						p.getObject().unifyWith(s.getObject());
						st.remove(s);
						matchFound = true;
						break;
					}
				}
				if (!matchFound) throw new UnificationException("Unification seemed to work but then didn't");
			}
		}

	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
