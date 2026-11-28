package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.templates.BindingContext;
import org.nanopub.templates.PlaceholderMatcher;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.Template;
import org.nanopub.templates.Violation;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.util.*;

/**
 * Context for a template, containing all necessary information to fill.
 */
public class TemplateContext implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TemplateContext.class);
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private final ContextType contextType;
    private final Template template;
    private final String componentId;
    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> sourceValues = new HashMap<>();
    private final Map<IRI, Value> shownValues = new HashMap<>();
    private final Set<String> lockedParams = new HashSet<>();
    private final Set<String> lockedStatements = new HashSet<>();
    private List<Component> components = new ArrayList<>();
    private final Map<IRI, IModel<?>> componentModels = new HashMap<>();
    private List<StatementItem> statementItems;
    private Set<IRI> iriSet = new HashSet<>();
    private Map<IRI, StatementItem> narrowScopeMap = new HashMap<>();
    private String targetNamespace = Template.DEFAULT_TARGET_NAMESPACE;
    private Nanopub existingNanopub;
    private Nanopub fillSource;
    private Map<IRI, String> labels;
    private FillMode fillMode = null;
    private String navigationContextId;

    /**
     * Constructor for creating a new template context for filling a template.
     *
     * @param contextType     the type of context
     * @param templateId      the ID of the template to fill
     * @param componentId     the ID of the component that will use this context
     * @param targetNamespace the target namespace for the template, can be null to use the default namespace
     */
    public TemplateContext(ContextType contextType, String templateId, String componentId, String targetNamespace) {
        this(contextType, templateId, componentId, targetNamespace, null);
    }

    /**
     * Constructor for creating a new template context for filling a template.
     *
     * @param contextType     the type of context
     * @param templateId      the ID of the template to fill
     * @param componentId     the ID of the component that will use this context
     * @param existingNanopub an existing nanopub to fill, can be null if creating a new nanopub
     */
    public TemplateContext(ContextType contextType, String templateId, String componentId, Nanopub existingNanopub) {
        this(contextType, templateId, componentId, null, existingNanopub);
    }

    private TemplateContext(ContextType contextType, String templateId, String componentId, String targetNamespace, Nanopub existingNanopub) {
        this.contextType = contextType;
        // TODO: check whether template is of correct type:
        this.template = TemplateData.get().getTemplate(templateId);
        this.componentId = componentId;
        if (targetNamespace != null) {
            this.targetNamespace = targetNamespace;
        }
        this.existingNanopub = existingNanopub;
        if (existingNanopub == null && NanodashSession.get().getUserIri() != null) {
            componentModels.put(NTEMPLATE.CREATOR_PLACEHOLDER, Model.of(NanodashSession.get().getUserIri().stringValue()));
        }
    }

    /**
     * Initializes the statements for this context.
     */
    public void initStatements() {
        if (statementItems != null) return;
        statementItems = new ArrayList<>();
        for (IRI st : template.getStatementIris()) {
            StatementItem si = new StatementItem(componentId, st, this);
            statementItems.add(si);
            for (IRI i : si.getIriSet()) {
                if (iriSet.contains(i)) {
                    narrowScopeMap.remove(i);
                } else {
                    iriSet.add(i);
                    narrowScopeMap.put(i, si);
                }
            }
        }
    }

    /**
     * Finalizes the statements by processing all parameters and values from the source, and
     * setting the repetition counts.
     */
    public void finalizeStatements() {
        Map<StatementItem, Integer> finalRepetitionCount = new HashMap<>();
        for (IRI ni : narrowScopeMap.keySet()) {
            // TODO: Move all occurrences of this to utility function:
            String postfix = Utils.getUriPostfix(ni);
            StatementItem si = narrowScopeMap.get(ni);
            int i = si.getRepetitionCount();
            while (true) {
                String p = postfix + "__" + i;
                if (hasValue(p)) {
                    si.addRepetitionGroup();
                } else {
                    break;
                }
                i++;
            }
            i = 1;
            int corr = 0;
            if (si.isEmpty()) corr = 1;
            while (true) {
                String p = postfix + "__." + i;
                if (hasParam(p)) {
                    int absPos = si.getRepetitionCount() + i - 1 - corr;
                    String param = postfix + "__" + absPos;
                    if (i - corr == 0) param = postfix;
                    setParam(param, getParam(p));
                    // A lock stated on the relative name has to follow the value to the
                    // absolute name it ends up under, or it would never match a placeholder.
                    if (isLocked(p)) setLocked(param);
                    finalRepetitionCount.put(si, i - corr);
                } else {
                    break;
                }
                i++;
            }
        }
        for (StatementItem si : finalRepetitionCount.keySet()) {
            for (int i = 0; i < finalRepetitionCount.get(si); i++) {
                si.addRepetitionGroup();
            }
        }
        for (StatementItem si : statementItems) {
            si.finalizeValues();
        }
    }

    /**
     * Sets the fill mode for this context.
     *
     * @param fillMode the fill mode to set
     */
    public void setFillMode(FillMode fillMode) {
        this.fillMode = fillMode;
    }

    /**
     * Gets the fill mode for this context.
     *
     * @return the fill mode, or null if not set
     */
    public FillMode getFillMode() {
        return fillMode;
    }

    /**
     * Returns the type of context.
     *
     * @return the context type
     */
    public ContextType getType() {
        return contextType;
    }

    /**
     * Returns the template associated with this context.
     *
     * @return the template
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Returns the ID of the template associated with this context.
     *
     * @return the template ID
     */
    public String getTemplateId() {
        return template.getId();
    }

    /**
     * Returns the URI of the nanopublication containing the template of this
     * context. Placeholder and other local IRIs of a template are minted under
     * this URI, so use it (not {@link #getTemplateId()}, which may be an embedded
     * sub-IRI) wherever a local-IRI prefix is stripped or constructed.
     *
     * @return the template's nanopublication URI
     */
    public String getTemplateNanopubUri() {
        return template.getNanopub().getUri().stringValue();
    }

    /**
     * Sets a parameter for this context.
     *
     * @param name  the name of the parameter
     * @param value the value of the parameter
     */
    public void setParam(String name, String value) {
        params.put(name, value);
    }

    /**
     * Gets a parameter value by its name.
     *
     * @param name the name of the parameter
     * @return the value of the parameter, or null if not set
     */
    public String getParam(String name) {
        return params.get(name);
    }

    /**
     * Checks if a parameter with the given name exists.
     *
     * @param name the name of the parameter
     * @return true if the parameter exists, false otherwise
     */
    public boolean hasParam(String name) {
        return params.containsKey(name);
    }

    /**
     * Sets a value read from the nanopublication the form is filled from, under the name of
     * its field, as a parameter would give it ({@link SourceFill}). The field starts with it
     * unless a parameter gives the field a value: that is what the user asked for, so it wins
     * over the source (issue #73).
     *
     * @param name  the name of the field, with any repetition or language suffix
     * @param value the value, as it would be typed into the field
     */
    public void setSourceValue(String name, String value) {
        sourceValues.put(name, value);
    }

    /**
     * Checks whether the nanopublication the form is filled from gives a value for the field of
     * the given name.
     *
     * @param name the name of the field
     * @return true if the source gives a value
     */
    public boolean hasSourceValue(String name) {
        return sourceValues.containsKey(name);
    }

    /**
     * Returns the value the nanopublication the form is filled from gives for the field of the
     * given name.
     *
     * @param name the name of the field
     * @return the value, or null if the source gives none
     */
    public String getSourceValue(String name) {
        return sourceValues.get(name);
    }

    /**
     * Checks whether a parameter or the nanopublication the form is filled from gives a value
     * for the field of the given name.
     *
     * @param name the name of the field
     * @return true if there is a value to start the field with
     */
    public boolean hasValue(String name) {
        return params.containsKey(name) || sourceValues.containsKey(name);
    }

    /**
     * Returns the value to start the field of the given name with: the one a parameter gives,
     * or else the one from the nanopublication the form is filled from.
     *
     * @param name the name of the field
     * @return the value, or null if there is none
     */
    public String getValue(String name) {
        return params.containsKey(name) ? params.get(name) : sourceValues.get(name);
    }

    /**
     * Sets the values a read-only view shows, as the matcher of nanopub-java has bound them to
     * the positions of the template ({@link ViewFill}). A field takes its value when it is made,
     * so these have to be set before the statements are built.
     *
     * @param values the values, by the value the template has in each position, with the
     *               repetition suffix {@code __N} where the position has a value of its own in each
     *               repetition
     */
    public void setShownValues(Map<IRI, Value> values) {
        shownValues.clear();
        shownValues.putAll(values);
    }

    /**
     * Returns the value a read-only view shows in a position.
     *
     * @param iri the value of the template in that position, as the form names it: with any
     *            repetition suffix, and with the artifact code marker it renders
     * @return the value, or null if the view shows none there
     */
    public Value getShownValue(IRI iri) {
        if (shownValues.isEmpty()) return null;
        // The form renders the artifact code marker of a template with an extra tilde on each
        // side (StatementItem.RepetitionGroup#transform); the matcher keys it as the template has it.
        String key = iri.stringValue().replace("~~~ARTIFACTCODE~~~", "~~ARTIFACTCODE~~");
        return shownValues.get(vf.createIRI(key));
    }

    /**
     * Marks a parameter as locked: the form field it pre-fills is shown but cannot be edited
     * (issue #678). Locking is per repetition, as the name carries the repetition suffix of the
     * field it refers to ("key" locks the first repetition, "key__1" the second, and so on).
     * Locking a value the user cannot see or reach would leave the form unfillable, so only
     * parameters that actually carry a value can be locked.
     *
     * @param name the name of the parameter to lock
     */
    public void setLocked(String name) {
        if (!hasParam(name)) {
            logger.warn("Ignoring lock on parameter {}, which has no value in this context", name);
            return;
        }
        lockedParams.add(name);
    }

    /**
     * Checks whether the given parameter name is locked.
     *
     * @param name the name of the parameter
     * @return true if the parameter is locked, false otherwise
     */
    public boolean isLocked(String name) {
        return lockedParams.contains(name);
    }

    /**
     * Checks whether the given placeholder is locked, i.e. pre-filled with a value the user is
     * not allowed to change. The placeholder IRI carries the repetition suffix of its repetition
     * group (see {@link StatementItem.RepetitionGroup}), so this holds per repetition: the second
     * repetition of a locked placeholder is editable unless it was locked in its own right.
     *
     * @param iri the placeholder IRI, as handed to the form component
     * @return true if the placeholder is locked, false otherwise
     */
    public boolean isLocked(IRI iri) {
        if (iri == null) return false;
        return isLocked(Utils.getUriPostfix(iri));
    }

    /**
     * Moves the lock of one parameter to another, used when removing a repetition group shifts
     * the values of the following groups up into its slot ({@link StatementItem.RepetitionGroup}
     * shifts values through fixed placeholder slots rather than deleting one). The lock belongs
     * to the pre-filled value, not to the slot, so it has to travel with the value: otherwise
     * removing a locked repetition would leave the value that slides up into its place
     * uneditable.
     *
     * @param fromName the parameter name the value is moving away from
     * @param toName   the parameter name the value is moving to
     */
    public void moveLock(String fromName, String toName) {
        if (lockedParams.remove(fromName)) {
            lockedParams.add(toName);
        } else {
            lockedParams.remove(toName);
        }
    }

    /**
     * Removes the lock of the given parameter, if any.
     *
     * @param name the name of the parameter to unlock
     */
    public void clearLock(String name) {
        lockedParams.remove(name);
    }

    /**
     * Marks a statement as locked: the user cannot add or remove repetitions of it (issue #678).
     * Unlike a locked value, which is a guardrail against accidental edits, this one holds:
     * Wicket does not invoke a listener of a component that is not visible, so the hidden
     * repetition buttons cannot be triggered from the browser either.
     * <p>
     * The name is resolved when the lock is queried rather than here, because the statements are
     * only built later ({@link #initStatements()}): it matches either the name of the statement
     * node itself ("st2") or the name of a placeholder used in that statement and no other
     * ("public-key"), which is the name a link author is more likely to have at hand.
     *
     * @param name the name of the statement, or of a placeholder that identifies it
     */
    public void setStatementLocked(String name) {
        lockedStatements.add(name);
    }

    /**
     * Checks whether the given statement is locked, i.e. whether its repetitions are fixed.
     *
     * @param statementId the IRI of the statement node
     * @return true if the statement is locked, false otherwise
     */
    public boolean isStatementLocked(IRI statementId) {
        if (statementId == null || lockedStatements.isEmpty()) return false;
        if (lockedStatements.contains(Utils.getUriPostfix(statementId))) return true;
        // A placeholder used in a single statement names that statement unambiguously; one used
        // in several is wide-scope and names none of them.
        for (Map.Entry<IRI, StatementItem> e : narrowScopeMap.entrySet()) {
            if (e.getValue() == null) continue;
            if (!lockedStatements.contains(Utils.getUriPostfix(e.getKey()))) continue;
            if (statementId.equals(e.getValue().getStatementId())) return true;
        }
        return false;
    }

    /**
     * Returns the components associated with this context.
     *
     * @return a list of components
     */
    public List<Component> getComponents() {
        return components;
    }

    /**
     * Returns the component models associated with this context.
     *
     * @return a map of IRI to model of strings
     */
    public Map<IRI, IModel<?>> getComponentModels() {
        return componentModels;
    }

    /**
     * Suffix under which the language-tag model of a language-tag-selectable literal
     * placeholder is keyed in the component models (appended after any repetition suffix).
     */
    public static final String LANGUAGE_SUFFIX = "__lang";

    /**
     * Returns the component-model key for the language tag of the given literal
     * placeholder IRI, which must already carry its repetition suffix if any.
     *
     * @param iri the (repetition-suffixed) literal placeholder IRI
     * @return the derived key for the language-tag model
     */
    public static IRI getLanguageModelKey(IRI iri) {
        return vf.createIRI(iri.stringValue() + LANGUAGE_SUFFIX);
    }

    /**
     * Suffix under which the escaped spelling that an auto-escape placeholder's value was
     * filled from is keyed in the component models (appended after any repetition suffix).
     */
    public static final String AUTO_ESCAPE_SOURCE_SUFFIX = "__autoescape-source";

    /**
     * Returns the component-model key for the source spelling of the given auto-escape
     * placeholder IRI, which must already carry its repetition suffix if any.
     *
     * @param iri the (repetition-suffixed) auto-escape placeholder IRI
     * @return the derived key for the source-spelling model
     */
    public static IRI getAutoEscapeSourceModelKey(IRI iri) {
        return vf.createIRI(iri.stringValue() + AUTO_ESCAPE_SOURCE_SUFFIX);
    }

    /**
     * Remembers the escaped postfix an auto-escape placeholder's value was filled from, so
     * that {@link #autoEscape(IRI, String)} can give back the IRI the source nanopublication
     * actually used rather than a re-escaped equivalent of it.
     *
     * @param iri     the (repetition-suffixed) auto-escape placeholder IRI
     * @param escaped the escaped postfix, as it appeared in the value filled in
     */
    public void setAutoEscapeSource(IRI iri, String escaped) {
        componentModels.put(getAutoEscapeSourceModelKey(iri), Model.of(escaped));
    }

    /**
     * The escaped postfix to build an auto-escape placeholder's IRI from.
     * {@link Utils#urlEncode} is not the inverse of {@link Utils#urlDecode}: both "+" and
     * "%20" decode to a space, and encoding only ever gives back "+". Re-escaping an
     * untouched value would therefore silently respell an IRI that its source nanopublication
     * wrote with "%20" -- a different IRI, and one nothing else refers to, which both the
     * links on the filled form and a superseding version would then point at. So where the
     * text still decodes from the spelling it was filled with, that spelling is kept
     * verbatim; only text the user actually changed is escaped afresh.
     *
     * @param iri  the (repetition-suffixed) auto-escape placeholder IRI
     * @param text the placeholder's current text, not escaped
     * @return the escaped postfix
     */
    public String autoEscape(IRI iri, String text) {
        IModel<?> sourceModel = componentModels.get(getAutoEscapeSourceModelKey(iri));
        Object source = (sourceModel == null) ? null : sourceModel.getObject();
        if (source != null && Utils.urlDecode(source).equals(text)) return source.toString();
        return Utils.urlEncode(text);
    }

    /**
     * Suffix of the URL parameter that pre-fills the chosen base of a
     * space-/namespace-dependent prefix, appended to a placeholder's postfix
     * (e.g. {@code param_resource__prefix}).
     */
    public static final String PREFIX_SUFFIX = "__prefix";

    /**
     * Returns the component-model key for the base chosen for the given
     * space-/namespace-dependent prefix placeholder. The key is derived from the
     * <em>token</em>, not from the placeholder, so every field whose prefix depends on the
     * same thing shares one model: picking a space in one picker sets it for all of them
     * (and for every repetition), which is what the shared-model AJAX refresh in the form
     * items keys on.
     *
     * @param token the token, as returned by {@link DynamicPrefix#getToken(String)}
     * @return the derived key for the prefix-base model
     */
    public static IRI getPrefixModelKey(String token) {
        return vf.createIRI(LocalUri.PREFIX + "prefix-base/" + token.replace("~", ""));
    }

    /**
     * Sets the navigation context (the {@code context} URL parameter of the page this
     * context is filled on), which determines the space or maintained resource that
     * space-/namespace-dependent prefixes resolve against.
     *
     * @param navigationContextId the context resource id, or null if the page has none
     */
    public void setNavigationContextId(String navigationContextId) {
        this.navigationContextId = navigationContextId;
    }

    /**
     * Returns the navigation context id this context resolves space-/namespace-dependent
     * prefixes against.
     *
     * @return the context resource id, or null if none is set
     */
    public String getNavigationContextId() {
        return navigationContextId;
    }

    /**
     * Returns the prefix of the given placeholder, with a space-/namespace-dependent
     * placeholder ({@link DynamicPrefix#SPACE_TOKEN}, {@link DynamicPrefix#NAMESPACE_TOKEN})
     * substituted by the base the navigation context — or the user's pick — determines.
     * Use this instead of {@link Template#getPrefix(IRI)} wherever the prefix is shown to
     * the user or used to build a value.
     *
     * @param iri the placeholder IRI
     * @return the resolved prefix; null if the template declares none, and also null if it
     * declares a dynamic one that isn't resolved yet (tell the two apart with
     * {@link #hasUnresolvedPrefix(IRI)})
     */
    public String getPrefix(IRI iri) {
        String rawPrefix = template.getPrefix(iri);
        String token = DynamicPrefix.getToken(rawPrefix);
        if (token == null) return rawPrefix;
        String base = resolvePrefixBase(token);
        if (base == null) return null;
        return rawPrefix.replace(token, base);
    }

    /**
     * Whether the given placeholder declares a space-/namespace-dependent prefix (resolved
     * or not). Such a prefix takes precedence over the target namespace a
     * {@code nt:LocalResource} would otherwise be minted under: declaring one is exactly
     * how a template says "mint this new resource under the space / maintained resource
     * instead of under the nanopublication".
     *
     * @param iri the placeholder IRI
     * @return true if the prefix is space-/namespace-dependent
     */
    public boolean hasDynamicPrefix(IRI iri) {
        return DynamicPrefix.getToken(template.getPrefix(iri)) != null;
    }

    /**
     * Whether the given placeholder's prefix is space-/namespace-dependent and its base is
     * not (yet) determined, i.e. the page carries no usable navigation context and the user
     * hasn't picked one.
     *
     * @param iri the placeholder IRI
     * @return true if the prefix is dynamic and unresolved
     */
    public boolean hasUnresolvedPrefix(IRI iri) {
        String token = DynamicPrefix.getToken(template.getPrefix(iri));
        return token != null && resolvePrefixBase(token) == null;
    }

    /**
     * Whether the given value, held for the given placeholder, will be minted as a new IRI under
     * the namespace of the nanopublication being published, rather than referring to an existing
     * one (issue #652). The IRI is formed by the library, as when the nanopublication is made
     * ({@link #newBindingContext()}). This lets the form show such a value as what it is rather
     * than as a bare word.
     *
     * @param iri   the placeholder IRI
     * @param value the value currently held for that placeholder
     * @return true if publishing would mint the value under the target namespace
     */
    public boolean isToBeMinted(IRI iri, String value) {
        // Only a plain name (no colon, hash or space) is minted. An IRI typed out in full
        // refers to what it names, even where it lies under the target namespace.
        if (value == null || !value.matches("[^:# ]+")) return false;
        // An external URI placeholder points at something that exists outside this
        // nanopublication, so there is nothing to mint for it (issue #676).
        if (template.isExternalUriPlaceholder(iri)) return false;
        BindingContext context = newBindingContext();
        IRI formed = formIri(iri, value, context);
        return formed != null && context.getLocalName(formed) != null;
    }

    /**
     * Returns the base that a space-/namespace-dependent prefix token stands for here: the one
     * the navigation context determines, or else the one the user picked.
     *
     * @param token the token, {@link DynamicPrefix#SPACE_TOKEN} or {@link DynamicPrefix#NAMESPACE_TOKEN}
     * @return the base, or null if it is not determined
     */
    public String getPrefixBase(String token) {
        return resolvePrefixBase(token);
    }

    private String resolvePrefixBase(String token) {
        String base = DynamicPrefix.resolveFromContext(token, navigationContextId);
        if (base != null && !base.isEmpty()) return base;
        IModel<?> model = componentModels.get(getPrefixModelKey(token));
        Object chosen = (model == null) ? null : model.getObject();
        if (chosen == null || chosen.toString().isEmpty()) return null;
        return chosen.toString();
    }

    /**
     * Returns a binding context of nanopub-java for the nanopublication this form makes, with the
     * bases of space-/namespace-dependent prefixes as they are resolved here. With it, the
     * library forms the IRI for a value in the same way as when it makes the nanopublication
     * ({@link PlaceholderMatcher#toValue}).
     *
     * @return a new binding context, without bindings
     */
    public BindingContext newBindingContext() {
        return newBindingContext(null);
    }

    private BindingContext newBindingContext(IRI creator) {
        BindingContext context = BindingContext.forNewNanopub(template, targetNamespace, creator);
        context.setPrefixBase(BindingContext.SPACE_TOKEN, resolvePrefixBase(DynamicPrefix.SPACE_TOKEN));
        context.setPrefixBase(BindingContext.NAMESPACE_TOKEN, resolvePrefixBase(DynamicPrefix.NAMESPACE_TOKEN));
        return context;
    }

    /**
     * Returns the default value the template gives a placeholder, as it stands in the
     * nanopublication this form makes ({@link PlaceholderMatcher#getDefault}): {@code nt:CREATOR}
     * stands for the user, and an IRI within the template for the one of the same name minted
     * within the nanopublication. A default that cannot stand there is none.
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @return the default value, or null if there is none
     */
    public Value getDefaultValue(IRI placeholder) {
        return PlaceholderMatcher.of(template, placeholder).getDefault(newBindingContext(getCreator()));
    }

    /**
     * Returns the default value of a placeholder as a person enters it into its field, which is
     * how a field starts out that has no other value: the short form where there is one, such as
     * the part after the prefix, as for a value filled in from an existing nanopublication.
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @return the default as it is entered, or null if there is none
     */
    public RawValue getDefaultRaw(IRI placeholder) {
        PlaceholderMatcher matcher = PlaceholderMatcher.of(template, placeholder);
        BindingContext context = newBindingContext(getCreator());
        Value value = matcher.getDefault(context);
        return value == null ? null : matcher.toRaw(value, context);
    }

    // The user the form makes the nanopublication for, which the form keeps as the value of
    // nt:CREATOR; null when nobody is logged in.
    private IRI getCreator() {
        IModel<?> model = componentModels.get(NTEMPLATE.CREATOR_PLACEHOLDER);
        Object creator = model == null ? null : model.getObject();
        return creator == null || creator.toString().isEmpty() ? null : vf.createIRI(creator.toString());
    }

    /**
     * Returns the raw value the form gives the library for what a field holds, as when the
     * nanopublication is made ({@link FormRequest}). An auto-escape value that is left as it was
     * filled in keeps the spelling it was filled from, which escaping the text afresh does not
     * give back where the source wrote "%20" ({@link #autoEscape}); the library takes an IRI after
     * the prefix of the placeholder as it is.
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @param text        the text of the field
     * @param languageTag the language chosen for it, or null
     * @return the raw value
     */
    public RawValue toRawValue(IRI placeholder, String text, String languageTag) {
        if (!text.isEmpty() && template.isAutoEscapePlaceholder(placeholder)) {
            String escaped = autoEscape(placeholder, text);
            String prefix = getPrefix(placeholder);
            if (!escaped.equals(Utils.urlEncode(text)) && prefix != null && !prefix.isEmpty()) {
                text = prefix + escaped;
            }
        }
        return new RawValue(text, languageTag);
    }

    /**
     * Checks a value as it is entered into the field of a placeholder, by the rules the library
     * makes the nanopublication by ({@link PlaceholderMatcher#match(RawValue, BindingContext)}).
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @param raw         the value as it is entered ({@link #toRawValue})
     * @return the violations, with the binding context they were found in
     */
    public FieldCheck check(IRI placeholder, RawValue raw) {
        BindingContext context = newBindingContext();
        return new FieldCheck(PlaceholderMatcher.of(template, placeholder).match(raw, context).violations(), context);
    }

    /**
     * What the check of a value finds ({@link #check}).
     *
     * @param violations the problems of the value, empty if there are none
     * @param context    the binding context it was checked in, which tells the names minted within
     *                   the nanopublication
     */
    public record FieldCheck(List<Violation> violations, BindingContext context) {
    }

    /**
     * Returns the IRI that the library forms for a value of a placeholder: the value itself when
     * it is a full IRI, and otherwise the value after the prefix of the placeholder, or minted
     * within the nanopublication.
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @param value       the value as it is entered
     * @param context     the binding context to form it in, from {@link #newBindingContext()}
     * @return the IRI, or null if the value is empty or forms no IRI, which includes a
     * space-/namespace-dependent prefix that is not resolved yet
     */
    private IRI formIri(IRI placeholder, String value, BindingContext context) {
        if (value == null || value.isEmpty()) return null;
        try {
            Value formed = PlaceholderMatcher.of(template, placeholder).toValue(new RawValue(value), context);
            return formed instanceof IRI iri ? iri : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Whether the field of a placeholder holds a value.
     *
     * @param iri the placeholder, with a {@code __N} suffix in a repetition
     * @return true if its model holds something other than nothing or an empty text
     */
    public boolean hasFieldValue(IRI iri) {
        IModel<?> model = componentModels.get(iri);
        Object value = model == null ? null : model.getObject();
        return value != null && !value.toString().isEmpty();
    }

    /**
     * The identifier a value entered for a placeholder gives a new resource, if it is one that is
     * checked against the identifiers already in use: the template marks the placeholder with
     * {@link org.nanopub.vocabulary.NTEMPLATE#NEW_URI_PLACEHOLDER}, and the value forms an IRI
     * that is not minted within the nanopublication. Such an identifier carries no artifact code,
     * so nothing makes it unique: two people filling the same form with the same name arrive at
     * the same IRI, and the second nanopublication silently extends the first one's resource
     * (#646). One minted within the nanopublication gets its artifact code when it is signed.
     * <p>
     * Only the tag makes an identifier one to check. How the value was formed makes no
     * difference -- typed out in full, or a name placed under a prefix -- because whether a value
     * names something new is the template author's call, not something to infer from the shape
     * of the form. The IRI is formed by the library, as when the nanopublication is made, so that
     * a check made while the field is filled in looks up exactly what publishing would.
     *
     * @param placeholder the placeholder, with a {@code __N} suffix in a repetition
     * @param value       the value entered into it
     * @return the identifier, or null if there is nothing to check: an untagged placeholder, an
     * empty value, a space-/namespace-dependent prefix that is not resolved yet, a value that
     * forms no valid IRI, or an IRI minted within the nanopublication
     */
    public IRI getNewUriIri(IRI placeholder, String value) {
        if (!template.isNewUriPlaceholder(placeholder)) return null;
        BindingContext context = newBindingContext();
        IRI formed = formIri(placeholder, value, context);
        if (formed == null || !Utils.isUriValue(formed.stringValue())) return null;
        return context.getLocalName(formed) == null ? formed : null;
    }

    /**
     * Returns the statement items associated with this context.
     *
     * @return a list of StatementItem objects
     */
    public List<StatementItem> getStatementItems() {
        return statementItems;
    }

    /**
     * Checks if the context has a narrow scope for the given IRI.
     *
     * @param iri the IRI to check
     * @return true if there is a narrow scope for the IRI, false otherwise
     */
    public boolean hasNarrowScope(IRI iri) {
        return narrowScopeMap.containsKey(iri);
    }

    /**
     * Returns the existing Nanopub associated with this context, if any.
     *
     * @return the existing Nanopub, or null if this context is for a new Nanopub
     */
    public Nanopub getExistingNanopub() {
        return existingNanopub;
    }

    /**
     * Returns the nanopub being used to fill this context (e.g. in supersede/derive
     * mode), if any. Unlike {@link #getExistingNanopub()} this does not imply the
     * context is read-only.
     *
     * @return the fill source nanopub, or null if none
     */
    public Nanopub getFillSource() {
        return fillSource;
    }

    /**
     * Sets the nanopub used to fill this context (e.g. in supersede/derive mode).
     *
     * @param fillSource the nanopub providing the values, or null to clear
     */
    public void setFillSource(Nanopub fillSource) {
        this.fillSource = fillSource;
    }

    /**
     * Returns the existing or fill-source nanopub for this context, preferring the
     * existing nanopub if set.
     *
     * @return the reference nanopub, or null if none
     */
    public Nanopub getReferenceNanopub() {
        return existingNanopub != null ? existingNanopub : fillSource;
    }

    /**
     * Checks if this context is read-only.
     *
     * @return true if the context is read-only, false otherwise
     */
    public boolean isReadOnly() {
        return existingNanopub != null;
    }

    /**
     * Returns the label for a given IRI, if available.
     *
     * @param iri the IRI for which to get the label
     * @return the label as a String, or null if no label is found
     */
    public String getLabel(IRI iri) {
        if (existingNanopub == null) return null;
        if (labels == null) {
            labels = new HashMap<>();
            for (Statement st : existingNanopub.getPubinfo()) {
                if (st.getPredicate().equals(NTEMPLATE.HAS_LABEL_FROM_API) || st.getPredicate().equals(RDFS.LABEL)) {
                    String label = st.getObject().stringValue();
                    labels.put((IRI) st.getSubject(), label);
                }
            }
        }
        return labels.get(iri);
    }

}
