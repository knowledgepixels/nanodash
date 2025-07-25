package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.GuidedChoiceItem;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.CryptoElement;
import org.nanopub.extra.security.NanopubSignatureElement;

import java.util.*;

/**
 * ValueFiller is a utility class that processes a Nanopub and fills a TemplateContext.
 */
public class ValueFiller {

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    private Nanopub fillNp;
    private FillMode fillMode;
    private List<Statement> unusedStatements = new ArrayList<>();
    private int initialSize;
    private boolean formMode;

    /**
     * Constructor for ValueFiller.
     *
     * @param fillNp      the Nanopub to fill
     * @param contextType the type of context to fill
     * @param formMode    if true, the filler will adapt to form mode, filtering out certain statements
     */
    public ValueFiller(Nanopub fillNp, ContextType contextType, boolean formMode) {
        this(fillNp, contextType, formMode, null);
    }

    /**
     * Constructor for ValueFiller with specified fill mode.
     *
     * @param fillNp      the Nanopub to fill
     * @param contextType the type of context to fill
     * @param formMode    if true, the filler will adapt to form mode, filtering out certain statements
     * @param fillMode    the fill mode to use, can be null for default behavior
     */
    public ValueFiller(Nanopub fillNp, ContextType contextType, boolean formMode, FillMode fillMode) {
        this.fillNp = fillNp;
        this.formMode = formMode;
        this.fillMode = fillMode;
        Set<Statement> statements;
        if (contextType == ContextType.ASSERTION) {
            statements = fillNp.getAssertion();
        } else if (contextType == ContextType.PROVENANCE) {
            statements = fillNp.getProvenance();
        } else {
            statements = fillNp.getPubinfo();
        }
        for (Statement st : statements) {
            Statement stT = transform(st);
            if (stT != null) unusedStatements.add(stT);
        }
        Collections.sort(unusedStatements, new Comparator<Statement>() {
            @Override
            public int compare(Statement st1, Statement st2) {
                String st1s = st1.getSubject() + " " + st1.getPredicate() + " " + st1.getObject();
                String st2s = st2.getSubject() + " " + st2.getPredicate() + " " + st2.getObject();
                return st1s.compareTo(st2s);
            }
        });
        initialSize = unusedStatements.size();
    }

    /**
     * Fills the TemplateContext with the unused statements.
     *
     * @param context the TemplateContext to fill
     */
    public void fill(TemplateContext context) {
        try {
            context.fill(unusedStatements);
        } catch (UnificationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Checks if the Nanopub has any statements.
     *
     * @return true if there are statements, false otherwise
     */
    public boolean hasStatements() {
        return initialSize > 0;
    }

    /**
     * Checks if there are any used statements in the Nanopub.
     *
     * @return true if there are used statements, false otherwise
     */
    public boolean hasUsedStatements() {
        return unusedStatements.size() < initialSize;
    }

    /**
     * Checks if there are any unused statements in the Nanopub.
     *
     * @return true if there are unused statements, false otherwise
     */
    public boolean hasUnusedStatements() {
        return unusedStatements.size() > 0;
    }

    /**
     * Returns the list of unused statements in the Nanopub.
     *
     * @return the list of unused statements
     */
    public List<Statement> getUnusedStatements() {
        return unusedStatements;
    }

    /**
     * Removes a specific unused statement from the list.
     *
     * @param st the statement to remove
     */
    public void removeUnusedStatement(Statement st) {
        unusedStatements.remove(st);
    }

    /**
     * Removes unused statements based on the specified subject, predicate, and object.
     *
     * @param subj the subject to match, can be null
     * @param pred the predicate to match, can be null
     * @param obj  the object to match, can be null
     */
    public void removeUnusedStatements(IRI subj, IRI pred, Value obj) {
        for (Statement st : new ArrayList<>(unusedStatements)) {
            if (subj != null && !st.getSubject().equals(subj)) continue;
            if (pred != null && !st.getPredicate().equals(pred)) continue;
            if (obj != null && !st.getObject().equals(obj)) continue;
            unusedStatements.remove(st);
        }
    }

    private Statement transform(Statement st) {
        if (formMode && st.getContext().equals(fillNp.getPubinfoUri())) {
            IRI pred = st.getPredicate();
            // TODO We might want to filter some of these out afterwards in PublishForm, to be more precise:
            if (st.getSubject().equals(fillNp.getUri())) {
                if (pred.equals(DCTERMS.CREATED)) return null;
                if (pred.equals(Nanopub.SUPERSEDES)) return null;
                if (pred.equals(RDFS.LABEL)) return null;
                if (pred.equals(NanopubUtils.INTRODUCES)) return null;
                if (pred.equals(NanopubUtils.EMBEDS)) return null;
                if (pred.equals(PublishForm.WAS_CREATED_AT_PREDICATE)) return null;
                if (pred.equals(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE)) return null;
                if (pred.equals(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE)) return null;
                if (pred.equals(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE)) return null;
            }
            if (pred.equals(CryptoElement.HAS_ALGORITHM)) return null;
            if (pred.equals(CryptoElement.HAS_PUBLIC_KEY)) return null;
            if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE)) return null;
            if (pred.equals(NanopubSignatureElement.HAS_SIGNATURE_TARGET)) return null;
            if (pred.equals(NanopubSignatureElement.SIGNED_BY)) return null;
            if (pred.equals(Template.HAS_LABEL_FROM_API)) {
                GuidedChoiceItem.setLabel(st.getSubject().stringValue(), st.getObject().stringValue());
                return null;
            }
            if (pred.equals(RDFS.LABEL)) {
                GuidedChoiceItem.setLabel(st.getSubject().stringValue(), st.getObject().stringValue());
            }
        }
        return vf.createStatement(
                (Resource) transform(st.getSubject()),
                (IRI) transform(st.getPredicate()),
                transform(st.getObject()),
                (Resource) transform(st.getContext()));
    }

    private Value transform(Value v) {
        if (fillNp.getUri().equals(v)) {
            return vf.createIRI("local:nanopub");
//			return Template.NANOPUB_PLACEHOLDER;
        } else if (fillNp.getAssertionUri().equals(v)) {
            return vf.createIRI("local:assertion");
//			return Template.ASSERTION_PLACEHOLDER;
        } else if (v instanceof IRI iri && formMode) {
            if (!Utils.getIntroducedIriIds(fillNp).contains(iri.stringValue()) || fillMode != FillMode.SUPERSEDE) {
                if (v.stringValue().startsWith(fillNp.getUri().stringValue())) {
                    return vf.createIRI("local:" + Utils.getUriPostfix(v.stringValue()));
                }
            }
        }
        return v;
    }

}
