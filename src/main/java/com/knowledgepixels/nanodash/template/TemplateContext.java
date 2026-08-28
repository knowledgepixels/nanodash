package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.LiteralDateItem;
import com.knowledgepixels.nanodash.component.LiteralDateTimeItem;
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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.*;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.time.ZonedDateTime;
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
    private List<Component> components = new ArrayList<>();
    private final Map<IRI, IModel<?>> componentModels = new HashMap<>();
    private Set<IRI> introducedIris = new HashSet<>();
    private Set<IRI> embeddedIris = new HashSet<>();
    private Map<IRI, IRI> rolePropertyPins = new LinkedHashMap<>();
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
     * Finalizes the statements by processing all parameters and setting the repetition counts.
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
                if (hasParam(p)) {
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

    private String resolvePrefixBase(String token) {
        String base = DynamicPrefix.resolveFromContext(token, navigationContextId);
        if (base != null && !base.isEmpty()) return base;
        IModel<?> model = componentModels.get(getPrefixModelKey(token));
        Object chosen = (model == null) ? null : model.getObject();
        if (chosen == null || chosen.toString().isEmpty()) return null;
        return chosen.toString();
    }

    /**
     * Returns the introduced IRIs in this context.
     *
     * @return a set of introduced IRIs
     */
    public Set<IRI> getIntroducedIris() {
        return introducedIris;
    }

    /**
     * Returns the embedded IRIs in this context.
     *
     * @return a set of embedded IRIs
     */
    public Set<IRI> getEmbeddedIris() {
        return embeddedIris;
    }

    /**
     * Returns the role-instantiation direction pins collected in this context, mapping
     * each filled/constant role predicate to its pin class
     * ({@link com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS#INVERSE_ROLE_PROPERTY}
     * or {@code REGULAR_ROLE_PROPERTY}). Emitted into pubinfo at publish time; see #525.
     *
     * @return a map of predicate IRI to direction-pin class
     */
    public Map<IRI, IRI> getRolePropertyPins() {
        return rolePropertyPins;
    }

    /**
     * Processes an IRI by applying the template's processing rules.
     *
     * @param iri the IRI to process
     * @return the processed IRI, or null if the processing results in no value
     */
    public IRI processIri(IRI iri) {
        Value v = processValue(iri);
        if (v == null) return null;
        if (v instanceof IRI) return (IRI) v;
        return iri;
    }

    /**
     * Processes a Value according to the template's rules.
     *
     * @param value the Value to process
     * @return the processed Value, or the original Value if no processing is applicable
     */
    public Value processValue(Value value) {
        if (!(value instanceof IRI)) {
            return value;
        }
        IRI iri = (IRI) value;
        if (iri.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) {
            iri = NanodashSession.get().getUserIri();
        }
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            iri = vf.createIRI(targetNamespace + "assertion");
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            iri = vf.createIRI(targetNamespace);
        } else if (template.isRootNanopubPlaceholder(iri)) {
            IModel<?> rootModel = componentModels.get(iri);
            String rootValue = (rootModel == null || rootModel.getObject() == null) ? "" : rootModel.getObject().toString();
            if (fillMode == FillMode.DERIVE) {
                // Deriving creates a new resource, so the new nanopub becomes its own root
                // definition rather than keeping the source's root (issue #527).
                iri = vf.createIRI(targetNamespace);
            } else if (rootValue.equals(LocalUri.of("nanopub").stringValue())) {
                // Sentinel: in supersede/override mode this means the existing nanopub was the root
                Nanopub ref = getReferenceNanopub();
                if (ref != null && (fillMode == FillMode.SUPERSEDE || fillMode == FillMode.OVERRIDE)) {
                    iri = vf.createIRI(ref.getUri().stringValue());
                } else {
                    iri = vf.createIRI(targetNamespace);
                }
            } else if (rootValue.isEmpty()) {
                iri = vf.createIRI(targetNamespace);
            } else {
                iri = vf.createIRI(rootValue);
            }
        }
        // TODO: Move this code below to the respective placeholder classes:
        IModel<?> tf = componentModels.get(iri);
        Value processedValue = null;
        Object tfObjectGeneric = null;
        if (tf != null) {
            tfObjectGeneric = tf.getObject();
        }
        if (template.isRestrictedChoicePlaceholder(iri)) {
            String tfObject = (String) tfObjectGeneric;
            if (tf != null && tfObject != null && !tfObject.isEmpty()) {
                String prefix = getPrefix(iri);
                boolean unresolvedPrefix = prefix == null && hasUnresolvedPrefix(iri);
                if (prefix == null) prefix = "";
                // A dynamic prefix is how a template asks for its new resource to be minted
                // under the space / maintained resource rather than under the nanopublication,
                // so it wins over the local-resource default (issue #571).
                if (template.isLocalResource(iri) && !hasDynamicPrefix(iri)) {
                    prefix = targetNamespace;
                    unresolvedPrefix = false;
                }
                if (Utils.isUriValue(tfObject)) {
                    prefix = "";
                    unresolvedPrefix = false;
                }
                // An unresolved space-/namespace-dependent prefix would mint the resource
                // under the wrong namespace; leave the value unset instead. Form validation
                // blocks publishing in this state.
                if (!unresolvedPrefix) {
                    String v = prefix + tf.getObject();
                    if (v.matches("[^:# ]+")) v = targetNamespace + v;
                    if (Utils.isUriValue(v)) {
                        processedValue = vf.createIRI(v);
                    } else {
                        processedValue = vf.createLiteral(tfObject);
                    }
                }
            }
        } else if (template.isUriPlaceholder(iri)) {
            String tfObject = (String) tfObjectGeneric;
            if (tf != null && tfObject != null && !tfObject.isEmpty()) {
                String prefix = getPrefix(iri);
                boolean unresolvedPrefix = prefix == null && hasUnresolvedPrefix(iri);
                if (prefix == null) prefix = "";
                // A dynamic prefix is how a template asks for its new resource to be minted
                // under the space / maintained resource rather than under the nanopublication,
                // so it wins over the local-resource default (issue #571).
                if (template.isLocalResource(iri) && !hasDynamicPrefix(iri)) {
                    prefix = targetNamespace;
                    unresolvedPrefix = false;
                }
                String v;
                if (template.isAutoEscapePlaceholder(iri)) {
                    v = prefix + Utils.urlEncode(tf.getObject());
                } else {
                    if (Utils.isUriValue(tfObject)) {
                        prefix = "";
                        unresolvedPrefix = false;
                    }
                    v = prefix + tf.getObject();
                }
                // See the restricted-choice branch above: an unresolved dynamic prefix
                // leaves the value unset rather than minting a wrongly-namespaced IRI.
                if (!unresolvedPrefix) {
                    if (v.matches("[^:# ]+")) v = targetNamespace + v;
                    processedValue = vf.createIRI(v);
                }
            }
        } else if (template.isIntroducedResource(iri)
                && (fillMode == FillMode.SUPERSEDE || fillMode == FillMode.OVERRIDE)
                && tfObjectGeneric instanceof String pinnedIri && !pinnedIri.isEmpty()) {
            // An introduced resource keeps its IRI across versions (docs/fill-modes.md), so
            // supersede/override pin the source's IRI that ValueFiller left in the (read-only)
            // model. This must not be gated on isLocalResource: LOCAL_RESOURCE is only ever
            // attached to sub-IRIs of the template nanopub (Template#tagIfUntypedLocal), while a
            // template may introduce an absolute IRI in a foreign namespace via the
            // "~~ARTIFACTCODE~~" marker (e.g. "Defining a biochementity"). Those used to fall
            // through to the catch-all branch, keep the marker, and get a fresh artifact code
            // substituted at signing time -- minting a new resource instead of a new version.
            processedValue = vf.createIRI(pinnedIri);
        } else if (template.isLocalResource(iri)) {
            if (template.isIntroducedResource(iri) && (fillMode == FillMode.SUPERSEDE || fillMode == FillMode.OVERRIDE)) {
                // Pinned by the branch above whenever a source IRI is known. With no source
                // value the slot stays unresolved rather than minting a fresh identity, so an
                // introduced identifier the old version didn't declare yet stays fillable
                // (issue #549).
            } else {
                String prefix = Utils.getUriPrefix(iri);
                processedValue = vf.createIRI(iri.stringValue().replace(prefix, targetNamespace));
            }
        } else if (template.isLiteralPlaceholder(iri)) {
            IRI datatype = template.getDatatype(iri);
            String languageTag = template.getLanguageTag(iri);
            if (XSD.DATETIME.equals(datatype)) {
                ZonedDateTime tfObject = (ZonedDateTime) tfObjectGeneric;
                if (tfObject != null) {
                    processedValue = vf.createLiteral(LiteralDateTimeItem.format.format(tfObject), datatype);
                }
            } else if (XSD.DATE.equals(datatype)) {
                Date tfObject = (Date) tfObjectGeneric;
                if (tfObject != null) {
                    processedValue = vf.createLiteral(LiteralDateItem.format.format(tfObject), datatype);
                }
            } else {
                String tfObject = (String) tfObjectGeneric;
                if (tf != null && tfObject != null && !tfObject.isEmpty()) {
                    if (template.isLanguageTagSelectable(iri)) {
                        IModel<?> langModel = componentModels.get(getLanguageModelKey(iri));
                        Object chosenTag = (langModel == null) ? null : langModel.getObject();
                        if (chosenTag != null && !chosenTag.toString().isEmpty()) {
                            processedValue = vf.createLiteral(tfObject, Literals.normalizeLanguageTag(chosenTag.toString()));
                        }
                        // No tag chosen: leave the value unresolved rather than emitting an
                        // untagged literal; form validation blocks this case on submit.
                    } else if (languageTag != null) {
                        processedValue = vf.createLiteral(tfObject, languageTag);
                    } else if (datatype != null) {
                        processedValue = vf.createLiteral(tfObject, datatype);
                    } else {
                        processedValue = vf.createLiteral(tfObject);
                    }
                }
            }
        } else if (template.isValuePlaceholder(iri)) {
            String tfObject = (String) tfObjectGeneric;
            if (tf != null && tfObject != null && !tfObject.isEmpty()) {
                if (Utils.isValidLiteralSerialization(tfObject)) {
                    processedValue = Utils.getParsedLiteral(tfObject);
                } else {
                    String v = tfObject;
                    if (v.matches("[^:# ]+")) v = targetNamespace + v;
                    processedValue = vf.createIRI(v);
                }
            }
        } else if (template.isSequenceElementPlaceholder(iri)) {
            String tfObject = (String) tfObjectGeneric;
            if (tf != null && tfObject != null && !tfObject.isEmpty()) {
                processedValue = vf.createIRI(tfObject);
            }
        } else {
            processedValue = iri;
        }
        if (processedValue instanceof IRI pvIri && template.isIntroducedResource(iri)) {
            introducedIris.add(pvIri);
        }
        if (processedValue instanceof IRI pvIri && template.isEmbeddedResource(iri)) {
            embeddedIris.add(pvIri);
        }
        if (processedValue instanceof IRI pvIri) {
            IRI directionPin = template.getRoleDirectionPin(iri);
            if (directionPin != null) rolePropertyPins.put(pvIri, directionPin);
        }
        return processedValue;
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
     * Propagates the statements from this context to a NanopubCreator.
     *
     * @param npCreator the NanopubCreator to which the statements will be added
     * @throws org.nanopub.MalformedNanopubException        if there is an error in the nanopub structure
     * @throws org.nanopub.NanopubAlreadyFinalizedException if the nanopub has already been finalized
     */
    public void propagateStatements(NanopubCreator npCreator) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        if (template.getNanopub() instanceof NanopubWithNs) {
            NanopubWithNs np = (NanopubWithNs) template.getNanopub();
            for (String p : np.getNsPrefixes()) {
                npCreator.addNamespace(p, np.getNamespace(p));
            }
        }
        for (StatementItem si : statementItems) {
            si.addTriplesTo(npCreator);
        }
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
     * Checks if any of the statement items in this context will match any triple.
     *
     * @return true if any statement item will match any triple, false otherwise
     */
    public boolean willMatchAnyTriple() {
        initStatements();
        for (StatementItem si : statementItems) {
            if (si.willMatchAnyTriple()) return true;
        }
        return false;
    }

    /**
     * Fills the context with statements, processing each StatementItem.
     *
     * @param statements the list of statements to fill
     * @throws UnificationException if there is an error during unification of statements
     */
    public void fill(List<Statement> statements) throws UnificationException {
        for (StatementItem si : statementItems) {
            // Isolate each statement: a unification failure on one must not abort the rest,
            // otherwise every later template statement is left unmatched. Roll back any partial
            // statement consumption so the next statement sees an intact pool.
            List<Statement> statementsBefore = new ArrayList<>(statements);
            try {
                si.fill(statements);
            } catch (UnificationException ex) {
                logger.warn("Could not fill statement; continuing with the remaining statements", ex);
                statements.clear();
                statements.addAll(statementsBefore);
            }
        }
        for (StatementItem si : statementItems) {
            si.fillFinished();
        }
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
