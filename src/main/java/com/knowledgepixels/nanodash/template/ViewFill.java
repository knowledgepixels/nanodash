package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.templates.BindingContext;
import org.nanopub.templates.Template;
import org.nanopub.templates.TemplateMatch;
import org.nanopub.templates.TemplateMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the contexts of the read-only view of a nanopublication. Each graph is matched against
 * the templates of the view by the matcher of nanopub-java ({@link TemplateMatcher}). A statement
 * of a template is shown once for each repetition the matcher assigned triples to, and its fields
 * show the values the matcher bound ({@link TemplateContext#setShownValues}). The publication info
 * is shown with several templates, each of which gets what those before it have left.
 * <p>
 * A fill is for one nanopublication and one view: what is left of the publication info shrinks as
 * it is shown.
 */
public class ViewFill {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private final Nanopub nanopub;
    private final List<Statement> pubinfo;

    /**
     * Creates a fill for the view of a nanopublication.
     *
     * @param nanopub the nanopublication shown
     */
    public ViewFill(Nanopub nanopub) {
        this.nanopub = nanopub;
        this.pubinfo = new ArrayList<>(nanopub.getPubinfo());
    }

    /**
     * Fills the context of the assertion template.
     *
     * @param context the context, whose statements are not built yet
     * @return the triples of the assertion the template has no statement for, as the view shows
     * them
     */
    public List<Statement> fillAssertion(TemplateContext context) {
        return toShown(fill(context, nanopub.getAssertion()).getUnusedTriples());
    }

    /**
     * Fills the context of the provenance template.
     *
     * @param context the context, whose statements are not built yet
     * @return the triples of the provenance the template has no statement for, as the view shows
     * them
     */
    public List<Statement> fillProvenance(TemplateContext context) {
        return toShown(fill(context, nanopub.getProvenance()).getUnusedTriples());
    }

    /**
     * Fills the context of a publication info template with what the templates filled before it
     * have left of the publication info, and takes away what it shows.
     *
     * @param context the context, whose statements are not built yet
     */
    public void fillPubinfo(TemplateContext context) {
        TemplateMatch match = fill(context, pubinfo);
        for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) {
            pubinfo.removeAll(statementMatch.triples().values());
        }
    }

    /**
     * Checks whether a template has a statement that takes any triple, as the template for
     * statements written by hand has. Such a template is to be filled after the others, so that
     * it shows only what they leave.
     *
     * @param template the template
     * @param nanopub  the nanopublication shown
     * @return true if the template takes any triple
     */
    public static boolean isCatchAll(Template template, Nanopub nanopub) {
        IRI predicate = vf.createIRI("https://example.org/any-predicate");
        for (IRI subject : List.of(nanopub.getUri(), vf.createIRI("https://example.org/any-subject"))) {
            for (Value object : List.of(vf.createIRI("https://example.org/any-object"), vf.createLiteral("any"))) {
                List<Statement> triple = List.of(vf.createStatement(subject, predicate, object));
                TemplateMatch match = new TemplateMatcher(template).match(triple, BindingContext.forNanopub(template, nanopub));
                if (match.getUnusedTriples().isEmpty()) return true;
            }
        }
        return false;
    }

    // Matches the triples against the template of the context, gives the context the values
    // found, and builds its statements, with a repetition for each one that triples were
    // assigned to.
    private TemplateMatch fill(TemplateContext context, Collection<Statement> triples) {
        Template template = context.getTemplate();
        TemplateMatch match = new TemplateMatcher(template).match(triples, BindingContext.forNanopub(template, nanopub));
        context.setShownValues(match.getContext().getBindings());
        context.initStatements();
        Map<IRI, List<TemplateMatch.StatementMatch>> byStatement = new HashMap<>();
        for (TemplateMatch.StatementMatch statementMatch : match.getMatches()) {
            byStatement.computeIfAbsent(statementMatch.statement(), k -> new ArrayList<>()).add(statementMatch);
        }
        for (StatementItem si : context.getStatementItems()) {
            si.show(byStatement.getOrDefault(si.getStatementId(), List.of()));
        }
        return match;
    }

    // The triples nothing took, with the nanopublication and its assertion by the names the
    // view gives them, in a fixed order.
    private List<Statement> toShown(List<Statement> triples) {
        List<Statement> shown = new ArrayList<>();
        for (Statement st : triples) {
            shown.add(vf.createStatement((Resource) toShown(st.getSubject()), (IRI) toShown(st.getPredicate()),
                    toShown(st.getObject()), (Resource) toShown(st.getContext())));
        }
        shown.sort(Comparator.comparing(st -> st.getSubject() + " " + st.getPredicate() + " " + st.getObject()));
        return shown;
    }

    private Value toShown(Value value) {
        if (nanopub.getUri().equals(value)) return LocalUri.of("nanopub");
        if (nanopub.getAssertionUri().equals(value)) return LocalUri.of("assertion");
        return value;
    }

}
