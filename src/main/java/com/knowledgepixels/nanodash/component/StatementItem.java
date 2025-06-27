package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	private TemplateContext context;
	private IRI statementId;
	private List<IRI> statementPartIds = new ArrayList<>();
	private List<WebMarkupContainer> viewElements = new ArrayList<>();
	private List<RepetitionGroup> repetitionGroups = new ArrayList<>();
	private boolean repetitionGroupsChanged = true;
	private Set<IRI> iriSet = new HashSet<>();
	private boolean isMatched = false;

	public StatementItem(String id, IRI statementId, TemplateContext context) {
		super(id);

		this.statementId = statementId;
		this.context = context;
		setOutputMarkupId(true);

		if (isGrouped()) {
			statementPartIds.addAll(getTemplate().getStatementIris(statementId));
		} else {
			statementPartIds.add(statementId);
		}

		addRepetitionGroup();

		ListView<WebMarkupContainer> v = new ListView<WebMarkupContainer>("statement-group", viewElements) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<WebMarkupContainer> item) {
				item.add(item.getModelObject());
			}

		};
		v.setOutputMarkupId(true);
		add(v);
	}

	public void addRepetitionGroup() {
		addRepetitionGroup(new RepetitionGroup());
	}

	public void addRepetitionGroup(RepetitionGroup rg) {
		repetitionGroups.add(rg);
		repetitionGroupsChanged = true;
	}

	@Override
	protected void onBeforeRender() {
		if (repetitionGroupsChanged) {
			updateViewElements();
			finalizeValues();
		}
		repetitionGroupsChanged = false;
		super.onBeforeRender();
	}

	private void updateViewElements() {
		viewElements.clear();
		boolean first = true;
		for (RepetitionGroup r : repetitionGroups) {
			if (isGrouped() && !first) {
				viewElements.add(new HorizontalLine("statement"));
			}
			viewElements.addAll(r.getStatementParts());
			boolean isOnly = repetitionGroups.size() == 1;
			boolean isLast = repetitionGroups.get(repetitionGroups.size()-1) == r;
			r.addRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && isLast);
			r.removeRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && !isOnly);
			r.optionalMark.setVisible(isOnly);
			first = false;
		}
		String htmlClassString = "";
		if (!context.isReadOnly() && isOptional()) {
			htmlClassString += "nanopub-optional ";
		}
		boolean singleItem = context.getStatementItems().size() == 1;
		boolean repeatableOrRepeated = (!context.isReadOnly() && isRepeatable()) || (context.isReadOnly() && getRepetitionCount() > 1);
		if ((isGrouped() || repeatableOrRepeated) && !singleItem) {
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

	public boolean isOptional() {
		return repetitionGroups.size() == 1 && getTemplate().isOptionalStatement(statementId);
	}

	public boolean isGrouped() {
		return getTemplate().isGroupedStatement(statementId);
	}

	public boolean isRepeatable() {
		return getTemplate().isRepeatableStatement(statementId);
	}

	public boolean hasEmptyElements() {
		return repetitionGroups.get(0).hasEmptyElements();
	}

	public Set<IRI> getIriSet() {
		return iriSet;
	}

	public boolean willMatchAnyTriple() {
		return repetitionGroups.get(0).matches(dummyStatementList);
	}

	public void fill(List<Statement> statements) throws UnificationException {
		if (isMatched) return;
		if (repetitionGroups.size() == 1) {
			RepetitionGroup rg = repetitionGroups.get(0);
			if (rg.matches(statements)) {
				rg.fill(statements);
			} else {
				return;
			}
		} else {
			return;
		}
		isMatched = true;
		if (!isRepeatable()) return;
		while (true) {
			RepetitionGroup newGroup = new RepetitionGroup();
			if (newGroup.matches(statements)) {
				newGroup.fill(statements);
				addRepetitionGroup(newGroup);
			} else {
				newGroup.disconnect();
				return;
			}
		}
	}

	public void fillFinished() {
		for (RepetitionGroup rg : repetitionGroups) {
			rg.fillFinished();
		}
	}

	public void finalizeValues() {
		for (RepetitionGroup rg : repetitionGroups) {
			rg.finalizeValues();
		}
	}

	public boolean isMatched() {
		return isMatched;
	}

	public boolean isEmpty() {
		return repetitionGroups.size() == 1 && repetitionGroups.get(0).isEmpty();
	}


	public class RepetitionGroup implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<StatementPartItem> statementParts;
		private List<ValueItem> localItems = new ArrayList<>();
		private boolean filled = false;

		private List<ValueItem> items = new ArrayList<>();

		Label addRepetitionButton, removeRepetitionButton, optionalMark;

		public RepetitionGroup() {
			statementParts = new ArrayList<>();
			for (IRI s : statementPartIds) {
				StatementPartItem statement = new StatementPartItem("statement",
						makeValueItem("subj", getTemplate().getSubject(s), s),
						makeValueItem("pred", getTemplate().getPredicate(s), s),
						makeValueItem("obj", getTemplate().getObject(s), s)
					);
				statementParts.add(statement);

				// Some of the methods of StatementItem and RepetitionGroup don't work properly before this
				// object is fully instantiated:
				boolean isFirstGroup = repetitionGroups.size() == 0;
				boolean isFirstLine = statementParts.size() == 1;
				boolean isLastLine = statementParts.size() == statementPartIds.size();
				boolean isOptional = getTemplate().isOptionalStatement(statementId);

				if (statementParts.size() == 1 && !isFirstGroup) {
					statement.add(new AttributeAppender("class", " separate-statement"));
				}
				if (!context.isReadOnly() && isOptional && isLastLine) {
					optionalMark = new Label("label", "(optional)");
				} else {
					optionalMark = new Label("label", "");
					optionalMark.setVisible(false);
				}
				statement.add(optionalMark);
				if (isLastLine) {
					addRepetitionButton = new Label("add-repetition", "+");
					statement.add(addRepetitionButton);
					addRepetitionButton.add(new AjaxEventBehavior("click") {
						private static final long serialVersionUID = 1L;
						@Override
						protected void onEvent(AjaxRequestTarget target) {
							addRepetitionGroup(new RepetitionGroup());
							target.add(StatementItem.this);
							target.appendJavaScript("updateElements();");
						}
					});
				} else {
					statement.add(new Label("add-repetition", "").setVisible(false));
				}
				if (isFirstLine) {
					removeRepetitionButton = new Label("remove-repetition", "-");
					statement.add(removeRepetitionButton);
					removeRepetitionButton.add(new AjaxEventBehavior("click") {
						private static final long serialVersionUID = 1L;
						@Override
						protected void onEvent(AjaxRequestTarget target) {
							RepetitionGroup.this.remove();
							target.appendJavaScript("updateElements();");
							target.add(StatementItem.this);
						}
					});
				} else {
					statement.add(new Label("remove-repetition", "").setVisible(false));
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

		public List<StatementPartItem> getStatementParts() {
			return statementParts;
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

		private void remove() {
			String thisSuffix = getRepeatSuffix();
			for (IRI iriBase : iriSet) {
				IRI thisIri = vf.createIRI(iriBase + thisSuffix);
				if (context.getComponentModels().containsKey(thisIri)) {
					IModel<String> swapModel1 = context.getComponentModels().get(thisIri);
					for (int i = getRepeatIndex() + 1 ; i < repetitionGroups.size(); i++) {
						IModel<String> swapModel2 = context.getComponentModels().get(vf.createIRI(iriBase + getRepeatSuffix(i)));
						if (swapModel1 != null && swapModel2 != null) {
							swapModel1.setObject(swapModel2.getObject());
						}
						swapModel1 = swapModel2;
					}
					// Clear last object:
					if (swapModel1 != null) swapModel1.setObject("");
				}
			}
			RepetitionGroup lastGroup = repetitionGroups.get(repetitionGroups.size()-1);
			repetitionGroups.remove(lastGroup);
			for (ValueItem vi : lastGroup.items) {
				vi.removeFromContext();
			}
			repetitionGroupsChanged = true;
		}

		private String getRepeatSuffix() {
			return getRepeatSuffix(getRepeatIndex());
		}

		private String getRepeatSuffix(int i) {
			if (i == 0) return "";
			// TODO: Check that this double-underscore pattern isn't used otherwise:
			return "__" + i;
		}

		public TemplateContext getContext() {
			return context;
		}

		public boolean isOptional() {
			if (!getTemplate().isOptionalStatement(statementId)) return false;
			if (repetitionGroups.size() == 0) return true;
			if (repetitionGroups.size() == 1 && repetitionGroups.get(0) == this) return true;
			return false;
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
					String value = ((GuidedChoiceItem) vi.getComponent()).getModel().getObject();
					if (value != null && GuidedChoiceItem.getLabel(value) != null) {
						String label = GuidedChoiceItem.getLabel(value);
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

		public boolean matches(List<Statement> statements) {
			if (filled) return false;
			List<Statement> st = new ArrayList<>(statements);
			for (StatementPartItem p : statementParts) {
				Statement matchedStatement = null;
				for (Statement s : st) {
					if (
							p.getPredicate().isUnifiableWith(s.getPredicate()) &&  // checking predicate first optimizes performance
							p.getSubject().isUnifiableWith(s.getSubject()) &&
							p.getObject().isUnifiableWith(s.getObject())) {
						matchedStatement = s;
						break;
					}
				}
				if (matchedStatement == null) {
					return false;
				} else {
					st.remove(matchedStatement);
				}
			}
			return true;
		}

		public void fill(List<Statement> statements) throws UnificationException {
			if (filled) throw new UnificationException("Already filled");
			for (StatementPartItem p : statementParts) {
				Statement matchedStatement = null;
				for (Statement s : statements) {
					if (
							p.getPredicate().isUnifiableWith(s.getPredicate()) &&  // checking predicate first optimizes performance
							p.getSubject().isUnifiableWith(s.getSubject()) &&
							p.getObject().isUnifiableWith(s.getObject())) {
						p.getPredicate().unifyWith(s.getPredicate());
						p.getSubject().unifyWith(s.getSubject());
						p.getObject().unifyWith(s.getObject());
						matchedStatement = s;
						break;
					}
				}
				if (matchedStatement == null) {
					throw new UnificationException("Unification seemed to work but then didn't");
				} else {
					statements.remove(matchedStatement);
				}
				filled = true;
			}
		}

		public void fillFinished() {
			for (ValueItem vi : items) {
				vi.fillFinished();
			}
		}

		public void finalizeValues() {
			for (ValueItem vi : items) {
				vi.finalizeValues();
			}
		}

	}

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	private static final List<Statement> dummyStatementList = new ArrayList<Statement>(Arrays.asList(vf.createStatement(vf.createIRI("http://dummy.com/"), vf.createIRI("http://dummy.com/"), vf.createIRI("http://dummy.com/"))));

}
