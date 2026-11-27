package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.nanopub.Nanopub;
import org.nanopub.templates.FillRequest;
import org.nanopub.templates.NanopubResolver;
import org.nanopub.templates.TemplateFillException;
import org.nanopub.templates.TemplateFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Makes each nanopublication the publishing form makes a second time, with the filler of
 * nanopub-java and from what is filled in on the form, and logs where the two differ.
 * <p>
 * This is the step before the form makes its nanopublications with the library (#936): the log
 * shows where the two would come out differently, on what people actually fill in. It decides
 * nothing: the form's own nanopublication is the one that is signed and published, and a
 * failure here is only logged. Look for {@code Template fill:} in the log.
 * <p>
 * The two are compared triple by triple in each graph, with the URI of each nanopublication and
 * what lies within it written relative to it. The head, the creation time and the signature are
 * left out, as they differ anyway.
 */
public class TemplateFillGuard {

    private static final Logger logger = LoggerFactory.getLogger(TemplateFillGuard.class);

    // So that one odd nanopublication cannot fill the log.
    private static final int MAX_TRIPLES_LOGGED = 20;

    private TemplateFillGuard() {
    }

    /**
     * The outcome of a comparison.
     *
     * @param onlyFromForm    the triples only the form's nanopublication has
     * @param onlyFromLibrary the triples only the library's nanopublication has
     * @param problems        why the library could not fill the request, if it could not;
     *                        empty otherwise
     */
    public record Comparison(Set<String> onlyFromForm, Set<String> onlyFromLibrary, List<String> problems) {

        /**
         * @return true if the library made the same nanopublication
         */
        public boolean isSame() {
            return onlyFromForm.isEmpty() && onlyFromLibrary.isEmpty() && problems.isEmpty();
        }

    }

    /**
     * Fills the request the form gives with the library, compares the result with the form's
     * nanopublication, and logs what differs.
     *
     * @param formNanopub the nanopublication the form made, before it is signed
     * @param request     gives the request for what is filled in on the form
     * @return the comparison, or null if it could not be made
     */
    public static Comparison check(Nanopub formNanopub, Supplier<FillRequest> request) {
        return check(formNanopub, request, TemplateData.RESOLVER);
    }

    static Comparison check(Nanopub formNanopub, Supplier<FillRequest> requestSupplier, NanopubResolver resolver) {
        String template = null;
        try {
            long start = System.currentTimeMillis();
            FillRequest request = requestSupplier.get();
            template = request.getTemplate();
            Comparison comparison;
            try {
                Nanopub libraryNanopub = new TemplateFiller(resolver).fill(request);
                comparison = new Comparison(subtract(triples(formNanopub), triples(libraryNanopub)),
                        subtract(triples(libraryNanopub), triples(formNanopub)), List.of());
            } catch (TemplateFillException ex) {
                comparison = new Comparison(Set.of(), Set.of(), ex.getProblems());
            }
            long time = System.currentTimeMillis() - start;
            if (comparison.isSame()) {
                logger.info("Template fill: the library makes the same nanopublication from template {} ({} ms)", template, time);
            } else if (!comparison.problems().isEmpty()) {
                logger.warn("Template fill: the library cannot fill template {} with what the form has: {}", template, String.join("; ", comparison.problems()));
                logger.info("Template fill: the request was {}", request.toParams());
            } else {
                logger.warn("Template fill: the library makes another nanopublication from template {}: {} triple(s) only from the form, {} only from the library ({} ms)",
                        template, comparison.onlyFromForm().size(), comparison.onlyFromLibrary().size(), time);
                log("only from the form", comparison.onlyFromForm());
                log("only from the library", comparison.onlyFromLibrary());
                logger.info("Template fill: the request was {}", request.toParams());
            }
            return comparison;
        } catch (Exception | LinkageError ex) {
            logger.warn("Template fill: could not compare the nanopublication made from template {}: {}", template, ex.toString());
            return null;
        }
    }

    private static void log(String side, Set<String> triples) {
        int n = 0;
        for (String triple : triples) {
            if (++n > MAX_TRIPLES_LOGGED) {
                logger.warn("Template fill:   ... and {} more {}", triples.size() - MAX_TRIPLES_LOGGED, side);
                return;
            }
            logger.warn("Template fill:   {}: {}", side, triple);
        }
    }

    private static Set<String> subtract(Set<String> a, Set<String> b) {
        Set<String> result = new TreeSet<>(a);
        result.removeAll(b);
        return result;
    }

    /**
     * Returns the triples of a nanopublication's assertion, provenance and publication info,
     * written relative to the nanopublication, without its creation time.
     *
     * @param nanopub the nanopublication
     * @return the triples, each as "graph: subject predicate object"
     */
    static Set<String> triples(Nanopub nanopub) {
        String base = nanopub.getUri().stringValue().replaceFirst("[/#]$", "");
        Set<String> triples = new TreeSet<>();
        add(triples, "assertion", nanopub.getAssertion(), base);
        add(triples, "provenance", nanopub.getProvenance(), base);
        for (Statement st : nanopub.getPubinfo()) {
            if (st.getPredicate().equals(DCTERMS.CREATED) && st.getSubject().equals(nanopub.getUri())) continue;
            triples.add("pubinfo: " + str(st.getSubject(), base) + " " + str(st.getPredicate(), base) + " " + str(st.getObject(), base));
        }
        return triples;
    }

    private static void add(Set<String> triples, String graph, Collection<Statement> statements, String base) {
        for (Statement st : statements) {
            triples.add(graph + ": " + str(st.getSubject(), base) + " " + str(st.getPredicate(), base) + " " + str(st.getObject(), base));
        }
    }

    private static String str(Value value, String base) {
        if (!(value instanceof IRI)) return value.toString();
        String s = value.stringValue();
        if (s.equals(base) || s.equals(base + "/") || s.equals(base + "#")) return "this:";
        if (s.startsWith(base + "/") || s.startsWith(base + "#")) return "sub:" + s.substring(base.length() + 1);
        return "<" + s + ">";
    }

}
