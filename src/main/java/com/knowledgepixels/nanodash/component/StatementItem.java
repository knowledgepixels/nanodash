package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
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
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a single item in a statement, which can be a subject, predicate, or object.
 */
public class StatementItem extends Panel {

    private TemplateContext context;
    private IRI statementId;
    private List<IRI> statementPartIds = new ArrayList<>();
    private List<WebMarkupContainer> viewElements = new ArrayList<>();
    private List<RepetitionGroup> repetitionGroups = new ArrayList<>();
    private boolean repetitionGroupsChanged = true;
    private Set<IRI> iriSet = new HashSet<>();
    private boolean isMatched = false;
    private static final Logger logger = LoggerFactory.getLogger(StatementItem.class);

    /**
     * Constructor for creating a StatementItem with a specific ID and statement ID.
     *
     * @param id          the Wicket component ID
     * @param statementId the IRI of the statement this item represents
     * @param context     the template context containing information about the template and its items
     */
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

            @Override
            protected void populateItem(ListItem<WebMarkupContainer> item) {
                item.add(item.getModelObject());
            }

        };
        v.setOutputMarkupId(true);
        add(v);
    }

    /**
     * Adds a new repetition group to this StatementItem with a default RepetitionGroup.
     */
    public void addRepetitionGroup() {
        addRepetitionGroup(new RepetitionGroup());
    }

    /**
     * Adds a new repetition group to this StatementItem.
     *
     * @param rg the RepetitionGroup to add
     */
    public void addRepetitionGroup(RepetitionGroup rg) {
        repetitionGroups.add(rg);
        repetitionGroupsChanged = true;
    }

    /**
     * {@inheritDoc}
     */
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
            boolean isLast = repetitionGroups.get(repetitionGroups.size() - 1) == r;
            r.addRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && isLast);
            r.removeRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && !isOnly);
            r.optionalMark.setVisible(isOnly);
            first = false;
        }
        String htmlClassString = "";
        if (!context.isReadOnly()) {
            if (isOptional()) {
                htmlClassString += "nanopub-optional ";
            }
            if (isAdvanced()) {
                htmlClassString += "advanced ";
            }
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

    /**
     * Adds the triples of this statement item to the given NanopubCreator.
     *
     * @param npCreator the NanopubCreator to which the triples will be added
     * @throws org.nanopub.MalformedNanopubException        if the statement item is not properly set up
     * @throws org.nanopub.NanopubAlreadyFinalizedException if the NanopubCreator has already been finalized
     */
    public void addTriplesTo(NanopubCreator npCreator) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
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

    /**
     * Returns the number of the repetition groups for this statement item.
     *
     * @return the number of repetition groups
     */
    public int getRepetitionCount() {
        return repetitionGroups.size();
    }

    /**
     * Returns whether the statement is optional.
     */
    public boolean isOptional() {
        return repetitionGroups.size() == 1 && getTemplate().isOptionalStatement(statementId);
    }

    /**
     * Returns whether the statement is advanced.
     */
    public boolean isAdvanced() {
        return getTemplate().isAdvancedStatement(statementId);
    }

    /**
     * Checks if this statement item is grouped.
     *
     * @return true if the statement item is grouped, false otherwise
     */
    public boolean isGrouped() {
        return getTemplate().isGroupedStatement(statementId);
    }

    /**
     * Checks if this statement item is repeatable.
     *
     * @return true if the statement item is repeatable, false otherwise
     */
    public boolean isRepeatable() {
        return getTemplate().isRepeatableStatement(statementId);
    }

    /**
     * Checks if this statement item has empty elements.
     *
     * @return true if any of the repetition groups has empty elements, false otherwise
     */
    public boolean hasEmptyElements() {
        return repetitionGroups.get(0).hasEmptyElements();
    }

    /**
     * Returns the set of IRIs associated with this statement item.
     *
     * @return a set of IRIs
     */
    public Set<IRI> getIriSet() {
        return iriSet;
    }

    /**
     * Checks if this statement item will match any triple.
     *
     * @return true if it will match any triple, false otherwise
     */
    public boolean willMatchAnyTriple() {
        return repetitionGroups.get(0).matches(dummyStatementList);
    }

    /**
     * Fills this statement item with the provided list of statements, matching them against the repetition groups.
     *
     * @param statements the list of statements to match against
     * @throws com.knowledgepixels.nanodash.template.UnificationException if the statements cannot be unified with this statement item
     */
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

    /**
     * Marks the filling of this statement item as finished, indicating that all values have been filled.
     */
    public void fillFinished() {
        for (RepetitionGroup rg : repetitionGroups) {
            rg.fillFinished();
        }
    }

    /**
     * Finalizes the values of all ValueItems in this statement item.
     */
    public void finalizeValues() {
        for (RepetitionGroup rg : repetitionGroups) {
            rg.finalizeValues();
        }
    }

    /**
     * Returns true if the statement item has been matched with a set of statements.
     *
     * @return true if matched, false otherwise
     */
    public boolean isMatched() {
        return isMatched;
    }

    /**
     * Checks if this statement item is empty, meaning it has no filled repetition groups.
     *
     * @return true if the statement item is empty, false otherwise
     */
    public boolean isEmpty() {
        return repetitionGroups.size() == 1 && repetitionGroups.get(0).isEmpty();
    }

    /**
     * Represents a group of repetitions for a statement item, containing multiple statement parts.
     */
    public class RepetitionGroup implements Serializable {

        private List<StatementPartItem> statementParts;
        private List<ValueItem> localItems = new ArrayList<>();
        private boolean filled = false;

        private List<ValueItem> items = new ArrayList<>();

        Label addRepetitionButton, removeRepetitionButton, optionalMark;

        /**
         * Constructor for creating a RepetitionGroup.
         */
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
                boolean isFirstGroup = repetitionGroups.isEmpty();
                boolean isFirstLine = statementParts.size() == 1;
                boolean isLastLine = statementParts.size() == statementPartIds.size();
                boolean isOptional = getTemplate().isOptionalStatement(statementId);

                if (statementParts.size() == 1 && !isFirstGroup) {
                    statement.add(new AttributeAppender("class", " separate-statement"));
                }

                // This code adds "advanced" marks similar to "optional":
//                if (!context.isReadOnly()) {
//                    if (isOptional && isLastLine) {
//                        if (isAdvanced()) {
//                            optionalMark = new Label("label", "(optional, advanced)");
//                        } else {
//                            optionalMark = new Label("label", "(optional)");
//                        }
//                    } else if (isAdvanced()) {
//                        optionalMark = new Label("label", "(advanced)");
//                    } else {
//                        optionalMark = new Label("label", "");
//                        optionalMark.setVisible(false);
//                    }
//                } else {
//                    optionalMark = new Label("label", "");
//                    optionalMark.setVisible(false);
//                }

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

        /**
         * Returns the statement parts.
         *
         * @return a list of StatementPartItem objects representing the statement parts
         */
        public List<StatementPartItem> getStatementParts() {
            return statementParts;
        }

        /**
         * Returns the index of this repetition group in the list of repetition groups.
         *
         * @return the index of this repetition group
         */
        public int getRepeatIndex() {
            if (!repetitionGroups.contains(this)) return repetitionGroups.size();
            return repetitionGroups.indexOf(this);
        }

        /**
         * Returns true if the repeat index if the first one.
         *
         * @return true if the repeat index is 0, false otherwise
         */
        public boolean isFirst() {
            return getRepeatIndex() == 0;
        }

        /**
         * Returns true if the repeat index is the last one.
         *
         * @return true if the repeat index is the last one, false otherwise
         */
        public boolean isLast() {
            return getRepeatIndex() == repetitionGroups.size() - 1;
        }

        private void remove() {
            String thisSuffix = getRepeatSuffix();
            for (IRI iriBase : iriSet) {
                IRI thisIri = vf.createIRI(iriBase + thisSuffix);
                if (context.getComponentModels().containsKey(thisIri)) {
                    IModel swapModel1 = (IModel) context.getComponentModels().get(thisIri);
                    for (int i = getRepeatIndex() + 1; i < repetitionGroups.size(); i++) {
                        IModel swapModel2 = (IModel) context.getComponentModels().get(vf.createIRI(iriBase + getRepeatSuffix(i)));
                        if (swapModel1 != null && swapModel2 != null) {
                            // TODO check how to fix this -- maybe a function that does the swap?
                            swapModel1.setObject(swapModel2.getObject());
                        }
                        swapModel1 = swapModel2;
                    }
                    // Clear last object:
                    if (swapModel1 != null) {
                        // FIXME check if this is fine now
                        swapModel1.setObject(null);
                    }
                }
            }
            RepetitionGroup lastGroup = repetitionGroups.get(repetitionGroups.size() - 1);
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

        /**
         * Returns the template context associated.
         *
         * @return the TemplateContext
         */
        public TemplateContext getContext() {
            return context;
        }

        /**
         * Checks if this repetition group is optional.
         *
         * @return true if the repetition group is optional, false otherwise
         */
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
            String iriString = iri.stringValue();
            iriString = iriString.replaceAll("~~ARTIFACTCODE~~", "~~~ARTIFACTCODE~~~");
            // Only add "__N" to URI from second repetition group on; for the first group, information about
            // narrow scopes is not yet complete.
            if (getRepeatIndex() > 0 && context.hasNarrowScope(iri)) {
                if (context.getTemplate().isPlaceholder(iri) || context.getTemplate().isLocalResource(iri)) {
                    iriString += getRepeatSuffix();
                }
            }
            return vf.createIRI(iriString);
        }

        /**
         * Adds the triples of this repetition group to the given NanopubCreator.
         *
         * @param npCreator the NanopubCreator to which the triples will be added
         * @throws org.nanopub.NanopubAlreadyFinalizedException if the NanopubCreator has already been finalized
         */
        public void addTriplesTo(NanopubCreator npCreator) throws NanopubAlreadyFinalizedException {
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
                            npCreator.addPubinfoStatement(vf.createIRI(value), NTEMPLATE.HAS_LABEL_FROM_API, vf.createLiteral(label));
                        } catch (IllegalArgumentException ex) {
                            logger.error("Could not create IRI from value: {}", value, ex);
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

        /**
         * Checks if this repetition group is empty, meaning it has no filled items.
         *
         * @return true if the repetition group is empty, false otherwise
         */
        public boolean isEmpty() {
            for (IRI s : statementPartIds) {
                Template t = getTemplate();
                IRI subj = t.getSubject(s);
                if (t.isPlaceholder(subj) && context.hasNarrowScope(subj) && context.processIri((IRI) transform(subj)) != null)
                    return false;
                IRI pred = t.getPredicate(s);
                if (t.isPlaceholder(pred) && context.hasNarrowScope(pred) && context.processIri((IRI) transform(pred)) != null)
                    return false;
                Value obj = t.getObject(s);
                if (obj instanceof IRI && t.isPlaceholder((IRI) obj) && context.hasNarrowScope((IRI) obj) && context.processValue(transform(obj)) != null)
                    return false;
            }
            return true;
        }

        /**
         * Checks if this repetition group matches the provided list of statements.
         *
         * @param statements the list of statements to match against
         * @return true if the repetition group matches, false otherwise
         */
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

        /**
         * Fills this repetition group with the provided list of statements, unifying them with the statement parts.
         *
         * @param statements the list of statements to match against
         * @throws UnificationException if the statements cannot be unified with this repetition group
         */
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

        /**
         * Marks the filling of this repetition group as finished, indicating that all values have been filled.
         */
        public void fillFinished() {
            for (ValueItem vi : items) {
                vi.fillFinished();
            }
        }

        /**
         * Finalizes the values of all ValueItems in this repetition group.
         */
        public void finalizeValues() {
            for (ValueItem vi : items) {
                vi.finalizeValues();
            }
        }

    }

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final List<Statement> dummyStatementList = new ArrayList<Statement>(Collections.singletonList(vf.createStatement(vf.createIRI("http://dummy.com/"), vf.createIRI("http://dummy.com/"), vf.createIRI("http://dummy.com/"))));

}
