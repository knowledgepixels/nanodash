package com.knowledgepixels.nanodash.template;

import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.templates.NanopubResolver;
import org.nanopub.templates.PlaceholderMatcher;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.TemplateFillException;
import org.nanopub.templates.TemplateFiller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the nanopublication that a form makes from what its template context holds, with the
 * filler of nanopub-java as the publishing form does (#936). For tests that look at what a field
 * publishes. The standard provenance and publication info templates the filler adds come from
 * the test resources, so that nothing is retrieved from the network.
 */
public final class TestFill {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * The creator the nanopublications are made for.
     */
    public static final IRI CREATOR = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    // By artifact code.
    private static final Map<String, Nanopub> STANDARD_TEMPLATES = new HashMap<>();

    static {
        for (File file : new File("src/test/resources/templates").listFiles((dir, name) -> name.endsWith(".trig"))) {
            try {
                STANDARD_TEMPLATES.put(file.getName().replace(".trig", ""), new NanopubImpl(file, RDFFormat.TRIG));
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot read " + file, ex);
            }
        }
    }

    private TestFill() {
    }

    /**
     * Returns a resolver that gives the given nanopublications, by their URI or an IRI within
     * them such as a template node, the standard templates, and anything else as Nanodash
     * retrieves it.
     *
     * @param nanopubs the nanopublications to give, null ones are left out
     * @return the resolver
     */
    public static NanopubResolver resolver(Nanopub... nanopubs) {
        return id -> {
            for (Nanopub np : nanopubs) {
                if (np == null) continue;
                String uri = np.getUri().stringValue();
                if (id.equals(uri) || id.startsWith(uri.replaceFirst("[/#]$", "") + "/") || id.startsWith(uri + "#")) return np;
            }
            for (Map.Entry<String, Nanopub> e : STANDARD_TEMPLATES.entrySet()) {
                if (id.contains(e.getKey())) return e.getValue();
            }
            // Anything else as Nanodash retrieves it, such as a template the one under test
            // requires in its publication info.
            return TemplateData.RESOLVER.getNanopub(id);
        };
    }

    /**
     * Makes the nanopublication from what the given assertion context holds, with the default
     * provenance and publication info, as the publishing form does.
     *
     * @param assertion the assertion context
     * @param others    further nanopublications the filler may ask for, besides the template of
     *                  the context and the nanopublication it is filled from
     * @return the nanopublication, not signed
     * @throws TemplateFillException if the filler refuses
     */
    public static Nanopub fill(TemplateContext assertion, Nanopub... others) throws TemplateFillException {
        List<Nanopub> nanopubs = new ArrayList<>(List.of(others));
        nanopubs.add(assertion.getTemplate().getNanopub());
        nanopubs.add(assertion.getFillSource());
        NanopubResolver resolver = resolver(nanopubs.toArray(new Nanopub[0]));
        return new TemplateFiller(resolver).fill(FormRequest.of(assertion, null, List.of(), null, CREATOR, null, null));
    }

    /**
     * Returns the RDF value that the filler of nanopub-java makes of the text a field holds, for
     * a test of one field that leaves the others empty.
     *
     * @param context the context of the field
     * @param key     the placeholder of the field, with a {@code __N} suffix in a repetition
     * @return the value, or null if the field is empty
     * @throws IllegalArgumentException if the text cannot be a value of the placeholder
     */
    public static Value formed(TemplateContext context, IRI key) {
        IModel<?> model = context.getComponentModels().get(key);
        Object object = model == null ? null : model.getObject();
        String text = object == null ? "" : object.toString();
        return PlaceholderMatcher.of(context.getTemplate(), key).toValue(new RawValue(text), context.newBindingContext());
    }

    /**
     * Returns the triples of the assertion that the form makes.
     *
     * @param assertion the assertion context
     * @return the triples
     * @throws TemplateFillException if the filler refuses
     */
    public static List<Statement> assertion(TemplateContext assertion) throws TemplateFillException {
        return new ArrayList<>(fill(assertion).getAssertion());
    }

    /**
     * Returns the object of the one triple with the given predicate in the assertion that the
     * form makes.
     *
     * @param assertion the assertion context
     * @param predicate the predicate
     * @return the object, or null if the assertion has no such triple
     * @throws TemplateFillException if the filler refuses
     * @throws IllegalStateException if it has more than one
     */
    public static Value object(TemplateContext assertion, IRI predicate) throws TemplateFillException {
        Value object = null;
        for (Statement st : fill(assertion).getAssertion()) {
            if (!st.getPredicate().equals(predicate)) continue;
            if (object != null) throw new IllegalStateException("More than one triple with " + predicate);
            object = st.getObject();
        }
        return object;
    }

}
