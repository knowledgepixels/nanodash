package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.TemplateDescriptions;
import org.nanopub.templates.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.nanopub.templates.TemplateMatch;
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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
        add(newDescriptionLabel());

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
     * Creates the label that tells the user how to fill in this statement, shown only while the
     * form is being filled in and only if the template describes the statement.
     *
     * @return the description label
     */
    private Label newDescriptionLabel() {
        String description = TemplateDescriptions.getStatementDescription(getTemplate(), statementId);
        Label label = new Label("description", description);
        label.setEscapeModelStrings(false);
        label.setVisible(description != null && !context.isReadOnly());
        return label;
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
            // A locked statement has the set of repetitions the form was opened with (issue #678):
            // no adding, no removing. Hiding the buttons is enough to enforce it, as Wicket does
            // not invoke a listener of a component that is not visible.
            boolean statementLocked = context.isStatementLocked(statementId);
            r.addRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && isLast && !statementLocked);
            r.removeRepetitionButton.setVisible(!context.isReadOnly() && isRepeatable() && !isOnly && !statementLocked);
            boolean clearable = !context.isReadOnly() && isOptional() && !statementLocked && r.canBeCleared();
            r.clearStatementButton.setVisible(clearable);
            if (clearable) r.revealOwnFields();
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
     * Returns one of the repetition groups of this statement item.
     *
     * @param index the index of the repetition group
     * @return the repetition group
     */
    RepetitionGroup getRepetitionGroup(int index) {
        return repetitionGroups.get(index);
    }

    /**
     * Returns the IRI of the statement node this item was built from.
     *
     * @return the statement IRI
     */
    public IRI getStatementId() {
        return statementId;
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
     * Returns the set of IRIs associated with this statement item.
     *
     * @return a set of IRIs
     */
    public Set<IRI> getIriSet() {
        return iriSet;
    }

    /**
     * Shows this statement in a read-only view as the nanopublication has it: once for each
     * repetition the matcher found triples for, and without the optional members of a group it
     * found none for. The values of the fields come from the context
     * ({@link TemplateContext#setShownValues}). A statement the nanopublication has no triples for
     * is not shown at all.
     *
     * @param matches the repetitions of this statement that triples were assigned to, in order
     */
    public void show(List<TemplateMatch.StatementMatch> matches) {
        isMatched = !matches.isEmpty();
        for (int i = 0; i < matches.size(); i++) {
            if (i == repetitionGroups.size()) addRepetitionGroup();
            RepetitionGroup rg = repetitionGroups.get(i);
            rg.unmatchedParts.clear();
            // The triples are keyed by member; that of a statement that is not grouped is keyed
            // by the statement itself.
            for (IRI part : statementPartIds) {
                if (!matches.get(i).triples().containsKey(part)) rg.unmatchedParts.add(part);
            }
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
     * Returns true if the nanopublication shown in a read-only view has triples for this
     * statement ({@link #show(List)}).
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
        // The members of a group the nanopublication shown in a read-only view has no triples
        // for, which are optional ones; read-only rendering hides these rows:
        private Set<IRI> unmatchedParts = new HashSet<>();

        private List<ValueItem> items = new ArrayList<>();

        Label addRepetitionButton, removeRepetitionButton, clearStatementButton, optionalMark;

        /**
         * Constructor for creating a RepetitionGroup.
         */
        public RepetitionGroup() {
            statementParts = new ArrayList<>();
            for (IRI s : statementPartIds) {
                // Declared subject/predicate: a template can (invalidly) put a literal there,
                // and rendering it shows the user what is wrong; Template.getStatementErrors()
                // spells it out.
                StatementPartItem statement = new StatementPartItem("statement",
                        makeValueItem("subj", getTemplate().getDeclaredSubject(s), s),
                        makeValueItem("pred", getTemplate().getDeclaredPredicate(s), s),
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
                if (isFirstLine) {
                    clearStatementButton = new Label("clear-statement", "×");
                    clearStatementButton.add(new AttributeModifier("data-fields", this::getOwnFieldMarkupIds));
                    clearStatementButton.add(new AttributeAppender("class", () -> hasFilledOwnPlaceholder() ? "" : "empty", " "));
                    statement.add(clearStatementButton);
                    clearStatementButton.add(new AjaxEventBehavior("click") {

                        @Override
                        protected void onEvent(AjaxRequestTarget target) {
                            RepetitionGroup.this.clear();
                            target.add(StatementItem.this);
                            target.appendJavaScript("updateElements();");
                        }

                    });
                } else {
                    statement.add(new Label("clear-statement", "").setVisible(false));
                }
            }
        }

        private ValueItem makeValueItem(String id, Value value, IRI statementPartId) {
            if (isFirst() && value instanceof IRI) {
                iriSet.add((IRI) value);
            }
            ValueItem vi = new ValueItem(id, transform(value), statementPartId, this);
            items.add(vi);
            return vi;
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
         * members the nanopublication has no triples for are hidden; in editable contexts
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
            // The lock of a pre-filled value belongs to the value, not to the slot it sits in, so
            // it has to travel with the value that shifts up (issue #678). Without this, removing
            // a locked repetition would leave the value that slides into its place uneditable.
            // Only the IRIs that actually get a repetition suffix are shifted, under the same
            // condition transform() applies: iriSet also holds the constants of the statement,
            // and one of those can share the postfix of a placeholder (rdfs:comment and a
            // "comment" placeholder, say), which would shift the same lock twice and undo it.
            Set<String> shiftedLockBases = new HashSet<>();
            for (IRI iriBase : iriSet) {
                if (!context.hasNarrowScope(iriBase)) continue;
                if (!getTemplate().isPlaceholder(iriBase) && !getTemplate().isLocalResource(iriBase)) continue;
                String lockBase = Utils.getUriPostfix(iriBase);
                if (!shiftedLockBases.add(lockBase)) continue;
                for (int i = getRepeatIndex(); i < repetitionGroups.size() - 1; i++) {
                    context.moveLock(lockBase + getRepeatSuffix(i + 1), lockBase + getRepeatSuffix(i));
                }
                context.clearLock(lockBase + getRepeatSuffix(repetitionGroups.size() - 1));
            }
            RepetitionGroup lastGroup = repetitionGroups.get(repetitionGroups.size() - 1);
            repetitionGroups.remove(lastGroup);
            for (ValueItem vi : lastGroup.items) {
                vi.removeFromContext();
            }
            repetitionGroupsChanged = true;
        }

        /**
         * The placeholders this repetition fills on its own, with its repetition suffix: those
         * no other statement of the template uses. Clearing a placeholder another statement
         * shares would empty that statement too, so those are left alone.
         *
         * @return the IRIs of the placeholders only this repetition fills
         */
        List<IRI> getOwnPlaceholderIris() {
            List<IRI> own = new ArrayList<>();
            for (IRI iriBase : iriSet) {
                if (!context.hasNarrowScope(iriBase)) continue;
                if (!getTemplate().isPlaceholder(iriBase)) continue;
                own.add(vf.createIRI(iriBase + getRepeatSuffix()));
            }
            return own;
        }

        /**
         * Whether this repetition has placeholders of its own and none of them is locked, so
         * that clearing it empties the statement without touching a value the user may not change.
         *
         * @return true if this repetition can be cleared
         */
        boolean canBeCleared() {
            List<IRI> own = getOwnPlaceholderIris();
            return !own.isEmpty() && own.stream().noneMatch(context::isLocked);
        }

        /**
         * The models of the placeholders this repetition fills on its own, including the
         * language-tag model a literal placeholder may have next to its value.
         *
         * @return the models
         */
        private List<IModel<?>> getOwnModels() {
            List<IModel<?>> models = new ArrayList<>();
            for (IRI own : getOwnPlaceholderIris()) {
                for (String derived : new String[]{"", TemplateContext.LANGUAGE_SUFFIX}) {
                    IModel<?> model = (IModel<?>) context.getComponentModels().get(vf.createIRI(own + derived));
                    if (model != null) models.add(model);
                }
            }
            return models;
        }

        /**
         * The form fields of the placeholders this repetition fills on its own.
         *
         * @return the form fields
         */
        private List<FormComponent<?>> getOwnFields() {
            List<IModel<?>> ownModels = getOwnModels();
            List<FormComponent<?>> fields = new ArrayList<>();
            for (Component c : context.getComponents()) {
                if (c instanceof FormComponent<?> field && isOneOf(field.getDefaultModel(), ownModels)) {
                    fields.add(field);
                }
            }
            return fields;
        }

        /**
         * Whether a model is the very same object as one of the given models. Models compare
         * equal by their values, so every empty model would count as any other empty one.
         *
         * @param model  the model to look for
         * @param models the models to look among
         * @return true if the model is one of them
         */
        private boolean isOneOf(IModel<?> model, List<IModel<?>> models) {
            return models.stream().anyMatch(candidate -> candidate == model);
        }

        /**
         * Gives the fields of this repetition's own placeholders an id in the page, so that the
         * clear button can tell from them whether there is anything to clear. Called before the
         * fields are rendered, as an id given afterwards would not make it into the page.
         */
        void revealOwnFields() {
            for (FormComponent<?> field : getOwnFields()) {
                field.setOutputMarkupId(true);
            }
        }

        /**
         * The page ids of the fields of this repetition's own placeholders, for the clear button
         * to watch.
         *
         * @return the ids, separated by spaces
         */
        String getOwnFieldMarkupIds() {
            List<String> ids = new ArrayList<>();
            for (FormComponent<?> field : getOwnFields()) {
                ids.add(field.getMarkupId());
            }
            return String.join(" ", ids);
        }

        /**
         * Whether any placeholder this repetition fills on its own has a value, which is when the
         * clear button has something to clear and is shown.
         *
         * @return true if one of its own placeholders is filled
         */
        boolean hasFilledOwnPlaceholder() {
            for (IModel<?> model : getOwnModels()) {
                Object value = model.getObject();
                if (value != null && !value.toString().isBlank()) return true;
            }
            return false;
        }

        /**
         * Empties the placeholders this repetition fills on its own (issue #145). An optional
         * statement with an empty placeholder is left out of the nanopublication, so this
         * removes the statement's content in one step, and leaves nothing behind that looks
         * filled in but would not be published.
         */
        @SuppressWarnings("unchecked")
        void clear() {
            for (IModel<?> model : getOwnModels()) {
                ((IModel<Object>) model).setObject(null);
                clearInputForModel(model);
            }
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
         * Checks if this repetition group is empty, meaning it has no filled items.
         *
         * @return true if the repetition group is empty, false otherwise
         */
        public boolean isEmpty() {
            Template t = getTemplate();
            for (IRI s : statementPartIds) {
                for (Value v : new Value[]{t.getSubject(s), t.getPredicate(s), t.getObject(s)}) {
                    if (v instanceof IRI iri && t.isPlaceholder(iri) && context.hasNarrowScope(iri)
                            && context.hasFieldValue((IRI) transform(iri))) return false;
                }
            }
            return true;
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

}
