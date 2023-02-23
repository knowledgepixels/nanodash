package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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
		setOutputMarkupId(true);

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

	public void repeat() {
		RepetitionGroup rg = new RepetitionGroup();
		repetitionGroups.add(rg);
		refreshStatements();
	}

	public void refreshStatements() {
		allStatements.clear();
		for (ValueItem vi : items) {
			vi.removeFromContext();
		}
		items.clear();
		boolean first = true;
		for (RepetitionGroup r : repetitionGroups) {
			if (isGrouped() && !first) {
				allStatements.add(new HorizontalLine("statement"));
			}
			r.refresh();
			allStatements.addAll(r.getStatements());
			first = false;
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

	public int getRepetitionCount() {
		return repetitionGroups.size();
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
		if (!hasMatch || !isRepeatable()) return;
		while (true) {
			RepetitionGroup newGroup = new RepetitionGroup();
			newGroup.refresh();
			if (newGroup.canMatch(statements)) {
				newGroup.fill(statements);
				repetitionGroups.add(newGroup);
				refreshStatements();
			} else {
				newGroup.disconnect();
				return;
			}
		}
	}

	public boolean isEmpty() {
		return repetitionGroups.size() == 1 && repetitionGroups.get(0).isEmpty();
	}


	public class RepetitionGroup implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<StatementPartItem> statements;
		private List<ValueItem> localItems = new ArrayList<>();

		public RepetitionGroup() {
		}

		public void refresh() {
			statements = new ArrayList<>();
			for (IRI s : statementPartIds) {
				StatementPartItem statement = new StatementPartItem("statement",
						makeValueItem("subj", getTemplate().getSubject(s), s),
						makeValueItem("pred", getTemplate().getPredicate(s), s),
						makeValueItem("obj", getTemplate().getObject(s), s)
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
				if (isRepeatable() && statements.size() == statementPartIds.size() && isLast()) {
					Label b = new Label("add-repetition", "+");
					statement.add(b);
					b.add(new AjaxEventBehavior("click") {
						private static final long serialVersionUID = 1L;
						@Override
						protected void onEvent(AjaxRequestTarget target) {
							repeat();
							target.add(StatementItem.this);
						}
					});
				} else {
					Label l = new Label("add-repetition", "");
					l.setVisible(false);
					statement.add(l);
				}
				if (isRepeatable() && statements.size() == 1 && !isOnly()) {
					Label b = new Label("remove-repetition", "-");
					statement.add(b);
					b.add(new AjaxEventBehavior("click") {
						private static final long serialVersionUID = 1L;
						@Override
						protected void onEvent(AjaxRequestTarget target) {
							RepetitionGroup.this.remove();
							refreshStatements();
							target.add(StatementItem.this);
						}
					});
				} else {
					Label l = new Label("remove-repetition", "");
					l.setVisible(false);
					statement.add(l);
				}
			}
		}

		private ValueItem makeValueItem(String id, Value value, IRI statementPartId) {
			if (isFirst() && value instanceof IRI) {
				iriSet.add((IRI) value);
			}
			ValueItem vi = new ValueItem(id, transform(value), statementPartId, this);
			localItems.add(vi);
			items.add(vi);
			return vi;
		}

		private void disconnect() {
			for (ValueItem vi : new ArrayList<>(localItems)) {
				// TODO These remove operations on list are slow. Improve:
				localItems.remove(vi);
				items.remove(vi);
				vi.removeFromContext();
			}
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

		public boolean isOnly() {
			return repetitionGroups.size() == 1;
		}

		private void remove() {
			String thisSuffix = getRepeatSuffix();
			for (IRI iriBase : iriSet) {
				IRI thisIri = vf.createIRI(iriBase + thisSuffix);
				if (context.getFormComponentModels().containsKey(thisIri)) {
					IModel<String> swapModel1 = context.getFormComponentModels().get(thisIri);
					for (int i = getRepeatIndex() + 1 ; i < repetitionGroups.size(); i++) {
						IModel<String> swapModel2 = context.getFormComponentModels().get(vf.createIRI(iriBase + getRepeatSuffix(i)));
						if (swapModel1 != null && swapModel2 != null) {
							swapModel1.setObject(swapModel2.getObject());
						}
						swapModel1 = swapModel2;
					}
					// Clear last object:
					if (swapModel1 != null) swapModel1.setObject("");
				}
			}
			repetitionGroups.remove(this);
		}

		private String getRepeatSuffix() {
			return getRepeatSuffix(getRepeatIndex());
		}

		private String getRepeatSuffix(int i) {
			if (i == 0) return "";
			// TODO: Check that this double-underscore pattern isn't used otherwise:
			return "__" + i;
		}

		public PublishFormContext getContext() {
			return context;
		}

		public boolean isOptional() {
			return StatementItem.this.isOptional();
		}

		private Value transform(Value value) {
			if (!(value instanceof IRI)) {
				return value;
			}
			IRI iri = (IRI) value;
			// Only add "__N" to URI from second repetition group on; for the first group, information about
			// narrow scopes is not yet complete.
			if (getRepeatIndex() > 0 && context.hasNarrowScope(iri)) {
				if (context.getTemplate().isPlaceholder(iri) || context.getTemplate().isLocalResource(iri)) {
					return vf.createIRI(iri.stringValue() + getRepeatSuffix());
				}
			}
			return iri;
		}

		public void addTriplesTo(NanopubCreator npCreator) {
			Template t = getTemplate();
			for (IRI s : statementPartIds) {
				IRI subj = context.processIri((IRI) transform(t.getSubject(s)));
				IRI pred = context.processIri((IRI) transform(t.getPredicate(s)));
				Value obj = context.processValue(transform(t.getObject(s)));
				if (context.getType() == ContextType.ASSERTION) {
					npCreator.addAssertionStatement(subj, pred, obj);
				} else if (context.getType() == ContextType.PROVENANCE) {
					npCreator.addProvenanceStatement(subj, pred, obj);
				} else if (context.getType() == ContextType.PUBINFO) {
					npCreator.addPubinfoStatement(subj, pred, obj);
				}
			}
			for (ValueItem vi : items) {
				if (vi.getComponent() instanceof GuidedChoiceItem) {
					GuidedChoiceItem gci = (GuidedChoiceItem) vi.getComponent();
					String value = gci.getModel().getObject();
					if (value != null && gci.getLabel(value) != null) {
						String label = gci.getLabel(value);
						if (label.length() > 1000) label = label.substring(0, 997) + "...";
						try {
							npCreator.addPubinfoStatement(vf.createIRI(value), Template.HAS_LABEL_FROM_API, vf.createLiteral(label));
						} catch (IllegalArgumentException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}

		private boolean hasEmptyElements() {
			for (IRI s : statementPartIds) {
				if (context.processIri((IRI) transform(getTemplate().getSubject(s))) == null) return true;
				if (context.processIri((IRI) transform(getTemplate().getPredicate(s))) == null) return true;
				if (context.processValue(transform(getTemplate().getObject(s))) == null) return true;
			}
			return false;
		}

		public boolean isEmpty() {
			for (IRI s : statementPartIds) {
				Template t = getTemplate();
				IRI subj = t.getSubject(s);
				if (t.isPlaceholder(subj) && context.hasNarrowScope(subj) && context.processIri((IRI) transform(subj)) != null) return false;
				IRI pred = t.getPredicate(s);
				if (t.isPlaceholder(pred) && context.hasNarrowScope(pred) && context.processIri((IRI) transform(pred)) != null) return false;
				Value obj = t.getObject(s);
				if (obj instanceof IRI && t.isPlaceholder((IRI) obj) && context.hasNarrowScope((IRI) obj) && context.processValue(transform(obj)) != null) return false;
			}
			return true;
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
