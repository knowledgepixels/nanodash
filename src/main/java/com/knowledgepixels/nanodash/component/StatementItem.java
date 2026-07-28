package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
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
            viewElements.addAll(r.getShownStatementParts());
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
            Set<IRI> modelsBefore = new HashSet<>(context.getComponentModels().keySet());
            List<Statement> statementsBefore = new ArrayList<>(statements);
            RepetitionGroup newGroup = new RepetitionGroup();
            boolean filled;
            if (newGroup.matches(statements)) {
                try {
                    newGroup.fill(statements);
                    filled = true;
                } catch (UnificationException ex) {
                    // matches() validates unifiability statelessly, but fill() mutates shared
                    // placeholder models via unifyWith as it goes, so binding an earlier part can
                    // make a later one non-unifiable ("seemed to work but then didn't"). Treat this
                    // like end-of-repetitions rather than letting it abort the whole template fill:
                    // roll back the partial statement consumption and discard the trial group.
                    logger.warn("Repetition fill failed after matches() succeeded for {}; stopping repetitions of this statement", statementId, ex);
                    statements.clear();
                    statements.addAll(statementsBefore);
                    filled = false;
                }
            } else {
                filled = false;
            }
            if (!filled) {
                newGroup.disconnect();
                // The trial group's constructor registered fresh (empty) component
                // models for its narrow-scope placeholders (e.g. public-key__N). If
                // left behind, a later real repetition group at the same index reuses
                // the stale empty model and skips param seeding — the "derive new
                // introduction" empty-fields bug. Drop exactly the models this trial
                // group added; shared (wide-scope) models existed before and stay.
                context.getComponentModels().keySet().retainAll(modelsBefore);
                return;
            }
            addRepetitionGroup(newGroup);
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
        // Optional group members that were left unassigned by a successful fill();
        // read-only rendering hides these rows:
        private Set<IRI> unmatchedParts = new HashSet<>();

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

                boolean isMemberOptional = isGrouped() && getTemplate().isOptionalStatement(s);

                if (!context.isReadOnly() && isOptional && isLastLine) {
                    optionalMark = new Label("label", "(optional)");
                } else {
                    optionalMark = new Label("label", "");
                    optionalMark.setVisible(false);
                }
                statement.add(optionalMark);
                // Member-level mark, rendered inline on the member's own line; unlike the
                // group-level mark it holds per repetition, so it is never toggled off:
                Label partOptionalMark = new Label("part-label", "(optional)");
                partOptionalMark.setVisible(!context.isReadOnly() && isMemberOptional);
                statement.add(partOptionalMark);
                if (!context.isReadOnly() && isMemberOptional) {
                    statement.add(new AttributeAppender("class", " nanopub-optional-part"));
                }
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
         * Returns the statement parts to render: in read-only contexts, optional group
         * members that were left unassigned by fill() are hidden; in editable contexts
         * (fresh publish, derive, update) all parts are shown, with skipped members
         * rendering as empty fields.
         *
         * @return a list of StatementPartItem objects to render
         */
        private List<StatementPartItem> getShownStatementParts() {
            if (!context.isReadOnly() || unmatchedParts.isEmpty()) return statementParts;
            List<StatementPartItem> shown = new ArrayList<>();
            for (int i = 0; i < statementParts.size(); i++) {
                if (!unmatchedParts.contains(statementPartIds.get(i))) {
                    shown.add(statementParts.get(i));
                }
            }
            return shown;
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
         * Returns the total number of repetition groups for this statement.
         *
         * @return the number of repetition groups
         */
        public int getRepetitionCount() {
            return StatementItem.this.getRepetitionCount();
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
                // Shift the value model and any derived model (e.g. the language-tag
                // model of a language-tag-selectable literal placeholder) alike.
                for (String derived : new String[]{"", TemplateContext.LANGUAGE_SUFFIX}) {
                    IRI thisIri = vf.createIRI(iriBase + thisSuffix + derived);
                    if (context.getComponentModels().containsKey(thisIri)) {
                        IModel swapModel1 = (IModel) context.getComponentModels().get(thisIri);
                        for (int i = getRepeatIndex() + 1; i < repetitionGroups.size(); i++) {
                            IModel swapModel2 = (IModel) context.getComponentModels().get(vf.createIRI(iriBase + getRepeatSuffix(i) + derived));
                            if (swapModel1 != null && swapModel2 != null) {
                                swapModel1.setObject(swapModel2.getObject());
                            }
                            // Drop any retained rawInput so the shifted model value is rendered
                            // instead of the user's previous (post-validation-error) entry.
                            clearInputForModel(swapModel1);
                            swapModel1 = swapModel2;
                        }
                        if (swapModel1 != null) {
                            swapModel1.setObject(null);
                            clearInputForModel(swapModel1);
                        }
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

        private void clearInputForModel(IModel<?> model) {
            if (model == null) return;
            for (Component c : context.getComponents()) {
                if (c instanceof FormComponent && c.getDefaultModel() == model) {
                    ((FormComponent<?>) c).clearInput();
                }
            }
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

        /**
         * Checks if the given member statement is effectively optional in this repetition group:
         * either the whole group is optional or the member itself carries the optional flag.
         * Unlike group-level optionality, the member-level flag holds per repetition, so it is
         * not affected by the number of repetition groups.
         *
         * @param partId the IRI of the member statement to check
         * @return true if the member statement is effectively optional, false otherwise
         */
        public boolean isOptionalPart(IRI partId) {
            return isOptional() || getTemplate().isOptionalStatement(partId);
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
                if (isGrouped() && t.isOptionalStatement(s) && (subj == null || pred == null || obj == null)) {
                    // Optional group member without all elements resolved: drop just this
                    // triple; the rest of the group is still emitted.
                    continue;
                }
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
                // Optional group members may stay empty; they are dropped at triple-creation
                // time and must not block or drop the rest of the group:
                if (isGrouped() && getTemplate().isOptionalStatement(s)) continue;
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
            // matches() must agree with fill(): because fill() binds shared placeholder models as it
            // goes, a valid assignment can only be confirmed by actually simulating those bindings.
            // We do exactly that (with backtracking) but on a copy and then restore the models, so
            // matches() stays side-effect free.
            Map<IRI, Object> snapshot = snapshotModels();
            Set<IRI> unmatchedBefore = new HashSet<>(unmatchedParts);
            try {
                List<Statement> copy = new ArrayList<>(statements);
                // A match must consume at least one statement: with optional members, an
                // assignment that skips every part would otherwise "match" without evidence,
                // which would also keep the repetition loop in fill() spinning forever.
                return assignParts(0, copy) && copy.size() < statements.size();
            } finally {
                restoreModels(snapshot);
                unmatchedParts.clear();
                unmatchedParts.addAll(unmatchedBefore);
            }
        }

        /**
         * Fills this repetition group with the provided list of statements, unifying them with the statement parts.
         *
         * @param statements the list of statements to match against
         * @throws UnificationException if the statements cannot be unified with this repetition group
         */
        public void fill(List<Statement> statements) throws UnificationException {
            if (filled) throw new UnificationException("Already filled");
            // Backtracking assignment: a greedy first-match can bind a shared placeholder in a way
            // that blocks a later part even though a consistent assignment exists. assignParts tries
            // alternatives and rolls back the model bindings between attempts. On success the matched
            // statements are removed from the list and the winning bindings are kept.
            unmatchedParts.clear();
            int sizeBefore = statements.size();
            if (!assignParts(0, statements) || statements.size() == sizeBefore) {
                throw new UnificationException("Unification seemed to work but then didn't");
            }
            filled = true;
        }

        /**
         * Tries to assign each remaining statement part (from index {@code partIndex} on) to a distinct
         * statement in {@code available} such that all bindings of shared placeholder models are mutually
         * consistent. Uses backtracking: on a failed branch the model bindings are restored and the next
         * candidate is tried. On success the chosen statements are removed from {@code available} and the
         * winning bindings remain applied to the component models.
         *
         * @param partIndex the index of the next statement part to assign
         * @param available the statements still available for assignment (mutated in place)
         * @return true if all remaining parts could be assigned consistently
         */
        private boolean assignParts(int partIndex, List<Statement> available) {
            if (partIndex == statementParts.size()) return true;
            StatementPartItem p = statementParts.get(partIndex);
            for (int i = 0; i < available.size(); i++) {
                Statement s = available.get(i);
                Map<IRI, Object> snapshot = snapshotModels();
                if (unifyPart(p.getPredicate(), s.getPredicate())  // checking predicate first optimizes performance
                        && unifyPart(p.getSubject(), s.getSubject())
                        && unifyPart(p.getObject(), s.getObject())) {
                    available.remove(i);
                    if (assignParts(partIndex + 1, available)) return true;
                    available.add(i, s);
                }
                restoreModels(snapshot);
            }
            IRI partId = statementPartIds.get(partIndex);
            if (isGrouped() && getTemplate().isOptionalStatement(partId)) {
                // Optional group member with no consistent candidate: leave it unassigned and
                // move on. Trying all candidates first (above) keeps fills maximal — a member
                // is only skipped when no consistent assignment exists.
                unmatchedParts.add(partId);
                if (assignParts(partIndex + 1, available)) return true;
                unmatchedParts.remove(partId);
            }
            return false;
        }

        /**
         * Checks unifiability and, if unifiable, applies the binding. Returns false instead of throwing,
         * so the caller can backtrack. (A partial binding left behind here is rolled back by the caller's
         * model snapshot.)
         */
        private boolean unifyPart(ValueItem item, Value v) {
            if (!item.isUnifiableWith(v)) return false;
            try {
                item.unifyWith(v);
                return true;
            } catch (UnificationException ex) {
                return false;
            }
        }

        /**
         * Snapshots the current values of all shared component models, so a trial binding can be rolled back.
         */
        private Map<IRI, Object> snapshotModels() {
            Map<IRI, Object> snapshot = new HashMap<>();
            for (Map.Entry<IRI, IModel<?>> e : context.getComponentModels().entrySet()) {
                snapshot.put(e.getKey(), e.getValue().getObject());
            }
            return snapshot;
        }

        /**
         * Restores component model values captured by {@link #snapshotModels()}.
         */
        @SuppressWarnings("unchecked")
        private void restoreModels(Map<IRI, Object> snapshot) {
            Map<IRI, IModel<?>> models = context.getComponentModels();
            for (Map.Entry<IRI, Object> e : snapshot.entrySet()) {
                IModel<?> m = models.get(e.getKey());
                if (m != null) ((IModel<Object>) m).setObject(e.getValue());
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
