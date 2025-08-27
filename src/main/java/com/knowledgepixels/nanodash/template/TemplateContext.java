package com.knowledgepixels.nanodash.template;

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
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubWithNs;

import java.io.Serializable;
import java.util.*;

/**
 * Context for a template, containing all necessary information to fill.
 */
public class TemplateContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    private final ContextType contextType;
    private final Template template;
    private final String componentId;
    private final Map<String, String> params = new HashMap<>();
    private List<Component> components = new ArrayList<>();
    private Map<IRI, IModel<String>> componentModels = new HashMap<>();
    private Set<IRI> introducedIris = new HashSet<>();
    private Set<IRI> embeddedIris = new HashSet<>();
    private List<StatementItem> statementItems;
    private Set<IRI> iriSet = new HashSet<>();
    private Map<IRI, StatementItem> narrowScopeMap = new HashMap<>();
    private String targetNamespace = Template.DEFAULT_TARGET_NAMESPACE;
    private Nanopub existingNanopub;
    private Map<IRI, String> labels;
    private FillMode fillMode = null;

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
            componentModels.put(Template.CREATOR_PLACEHOLDER, Model.of(NanodashSession.get().getUserIri().stringValue()));
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
    public Map<IRI, IModel<String>> getComponentModels() {
        return componentModels;
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
        if (!(value instanceof IRI)) return value;
        IRI iri = (IRI) value;
        if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
            iri = NanodashSession.get().getUserIri();
        }
        if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
            iri = vf.createIRI(targetNamespace + "assertion");
        } else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
            iri = vf.createIRI(targetNamespace);
        }
        // TODO: Move this code below to the respective placeholder classes:
        IModel<String> tf = componentModels.get(iri);
        Value processedValue = null;
        if (template.isRestrictedChoicePlaceholder(iri)) {
            if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
                String prefix = template.getPrefix(iri);
                if (prefix == null) prefix = "";
                if (template.isLocalResource(iri)) prefix = targetNamespace;
                if (tf.getObject().matches("https?://.+")) prefix = "";
                String v = prefix + tf.getObject();
                if (v.matches("[^:# ]+")) v = targetNamespace + v;
                if (v.matches("https?://.*")) {
                    processedValue = vf.createIRI(v);
                } else {
                    processedValue = vf.createLiteral(tf.getObject());
                }
            }
        } else if (template.isUriPlaceholder(iri)) {
            if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
                String prefix = template.getPrefix(iri);
                if (prefix == null) prefix = "";
                if (template.isLocalResource(iri)) prefix = targetNamespace;
                String v;
                if (template.isAutoEscapePlaceholder(iri)) {
                    v = prefix + Utils.urlEncode(tf.getObject());
                } else {
                    if (tf.getObject().matches("https?://.+")) prefix = "";
                    v = prefix + tf.getObject();
                }
                if (v.matches("[^:# ]+")) v = targetNamespace + v;
                processedValue = vf.createIRI(v);
            }
        } else if (template.isLocalResource(iri)) {
            String prefix = Utils.getUriPrefix(iri);
            processedValue = vf.createIRI(iri.stringValue().replace(prefix, targetNamespace));
        } else if (template.isLiteralPlaceholder(iri)) {
            IRI datatype = template.getDatatype(iri);
            String languagetag = template.getLanguageTag(iri);
            if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
                if (datatype != null) {
                    processedValue = vf.createLiteral(tf.getObject(), datatype);
                } else if (languagetag != null) {
                    processedValue = vf.createLiteral(tf.getObject(), languagetag);
                } else {
                    processedValue = vf.createLiteral(tf.getObject());
                }
            }
        
        } else if (template.isValuePlaceholder(iri)) {
            if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
                if (tf.getObject().startsWith("\"") && tf.getObject().endsWith("\"")) {
                    processedValue = vf.createLiteral(tf.getObject().substring(1, tf.getObject().length() - 1).replaceAll("\\\\(\\\\|\\\")", "$1"));
                } else {
                    String v = tf.getObject();
                    if (v.matches("[^:# ]+")) v = targetNamespace + v;
                    processedValue = vf.createIRI(v);
                }
            }
        } else if (template.isSequenceElementPlaceholder(iri)) {
            if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
                processedValue = vf.createIRI(tf.getObject());
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
     * @throws org.nanopub.MalformedNanopubException if there is an error in the nanopub structure
     */
    public void propagateStatements(NanopubCreator npCreator) throws MalformedNanopubException {
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
     * @throws com.knowledgepixels.nanodash.template.UnificationException if there is an error during unification of statements
     */
    public void fill(List<Statement> statements) throws UnificationException {
        for (StatementItem si : statementItems) {
            si.fill(statements);
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
                if (st.getPredicate().equals(Template.HAS_LABEL_FROM_API) || st.getPredicate().equals(RDFS.LABEL)) {
                    String label = st.getObject().stringValue();
                    labels.put((IRI) st.getSubject(), label);
                }
            }
        }
        return labels.get(iri);
    }

}
