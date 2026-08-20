package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryParamField;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a GRLC query extracted from a nanopublication.
 * <p>
 * Query parsing (SPARQL, endpoint, label, description, placeholders) is inherited from
 * {@link QueryTemplate} in nanopub-java. This subclass adds Nanodash-specific concerns:
 * an instance cache with the {@link #get} and {@link #load} factory methods, and integration with the
 * {@link QueryParamField} form components used by the query UI.
 */
public class GrlcQuery extends QueryTemplate {

    private static final Logger logger = LoggerFactory.getLogger(GrlcQuery.class);

    private static final Cache<String, GrlcQuery> instanceMap = CacheBuilder.newBuilder()
        .maximumSize(5_000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    private static final Pattern ARTIFACT_CODE_PATTERN = Pattern.compile("RA[A-Za-z0-9\\-_]{43}");

    /**
     * Picks the numeric character code out of the lexical errors the RDF4J SPARQL parser
     * reports, which read like {@code Lexical error at line 20, column 56.  Encountered: "..."
     * (160), after : ""} — where the number in brackets is the code of the character it
     * tripped over.
     */
    private static final Pattern LEXICAL_ERROR_CHARACTER_PATTERN = Pattern.compile("Encountered:[^(]*\\((\\d+)\\)");

    /**
     * Returns a singleton instance of GrlcQuery for the given QueryRef.
     *
     * @param ref the QueryRef object containing the query name
     * @return a GrlcQuery instance
     */
    public static GrlcQuery get(QueryRef ref) {
        return get(ref.getQueryId());
    }

    /**
     * Returns a singleton instance of GrlcQuery for the given query ID, or null if there is
     * no query to be had for it. Use {@link #load(String)} instead where the reason matters,
     * e.g. to tell the user what is wrong with the query.
     *
     * @param id the unique identifier or URI of the query
     * @return a GrlcQuery instance, or null if the query couldn't be loaded
     */
    public static GrlcQuery get(String id) {
        try {
            return load(id);
        } catch (QueryLoadException ex) {
            return null;
        }
    }

    /**
     * Returns a singleton instance of GrlcQuery for the given query ID, reporting why it
     * can't be had when it can't. Queries come from nanopublications that anybody can
     * publish, so failing to load one says something about that nanopublication rather than
     * about Nanodash, and the user is better served by being told what is wrong with it than
     * by a generic error.
     *
     * @param id the unique identifier or URI of the query
     * @return a GrlcQuery instance, never null
     * @throws QueryLoadException if the query cannot be loaded, with a message explaining why
     */
    public static GrlcQuery load(String id) {
        if (id == null || id.isBlank()) {
            throw new QueryLoadException("No query was given to show or run.");
        }
        GrlcQuery cached = instanceMap.getIfPresent(id);
        if (cached != null) return cached;
        GrlcQuery q;
        try {
            q = new GrlcQuery(id);
        } catch (Exception ex) {
            // Logged in full here, because what is shown to the user is only the gist of it.
            logger.warn("Could not load query: {}", id, ex);
            throw new QueryLoadException(explainLoadFailure(id, ex), ex);
        }
        // Cached under the normalized ID, so that the different ways of referring to the same
        // query (URI, ID, containing nanopublication) share one instance.
        cached = instanceMap.getIfPresent(q.getQueryId());
        if (cached != null) return cached;
        instanceMap.put(q.getQueryId(), q);
        return q;
    }

    /**
     * Puts into plain words why the query with the given ID couldn't be loaded, for showing
     * to the user.
     *
     * @param id the query ID or URI that was asked for
     * @param ex the failure that loading it ran into
     * @return the explanation
     */
    private static String explainLoadFailure(String id, Exception ex) {
        MalformedQueryException sparqlFailure = null;
        if (ex instanceof MalformedQueryException direct) {
            sparqlFailure = direct;
        } else if (ex.getCause() instanceof MalformedQueryException wrapped) {
            sparqlFailure = wrapped;
        }
        if (sparqlFailure != null) {
            String characterHint = explainOffendingCharacter(sparqlFailure.getMessage());
            String detail = summarize(sparqlFailure.getMessage());
            return "The SPARQL code of the query '" + id + "' is not valid, so the query can't be shown or run."
                    + (detail == null ? "" : " The SPARQL parser reports: " + detail)
                    + (characterHint == null ? "" : " " + characterHint)
                    + " This is a problem with the published query itself, which only a corrected version of it can fix.";
        }
        String detail = ex.getMessage();
        if (detail == null || detail.isBlank()) detail = ex.getClass().getSimpleName();
        return "The query '" + id + "' couldn't be loaded: " + detail;
    }

    /**
     * Reduces a SPARQL parser failure to the one line that says what it tripped over and
     * where, leaving out the list of what it was expecting instead. That list runs to dozens
     * of grammar rules, which is more than an error page can carry; it is kept in the log.
     *
     * @param parserMessage the message of the SPARQL parser failure
     * @return the gist of it as a single sentence, or null if there is nothing to say
     */
    private static String summarize(String parserMessage) {
        if (parserMessage == null) return null;
        // Lexical errors trail off into a dangling comma, which would run into what follows.
        String firstLine = parserMessage.split("\\R", 2)[0].trim().replaceAll("[,;:\\s]+$", "");
        if (firstLine.isEmpty()) return null;
        return firstLine.endsWith(".") ? firstLine : firstLine + ".";
    }

    /**
     * Names the character a SPARQL lexical error tripped over, as long as it is one that
     * doesn't belong in SPARQL in the first place. Such characters — a non-breaking space, a
     * typographic quote — look like their plain ASCII counterparts or like nothing at all, so
     * the parser's report of a numeric character code leaves the author of the query none the
     * wiser about what to correct.
     *
     * @param parserMessage the message of the SPARQL parser failure
     * @return the explanation, or null if the message doesn't point at such a character
     */
    private static String explainOffendingCharacter(String parserMessage) {
        if (parserMessage == null) return null;
        Matcher m = LEXICAL_ERROR_CHARACTER_PATTERN.matcher(parserMessage);
        if (!m.find()) return null;
        int codePoint;
        try {
            codePoint = Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
        // Only non-ASCII characters are worth naming: for the ASCII ones the parser's own
        // report already shows what is there.
        if (codePoint < 128 || !Character.isValidCodePoint(codePoint)) return null;
        String name = Character.getName(codePoint);
        return "The character in that position is " + String.format("U+%04X", codePoint)
                + (name == null ? "" : " (" + name + ")")
                + ", which SPARQL doesn't allow there. Characters like this one tend to slip in when a query is"
                + " copied from a word processor or a web page, and replacing them with their plain equivalents"
                + " makes the query valid again.";
    }

    /**
     * Constructs a GrlcQuery by parsing the given query ID or URI, fetching the underlying
     * nanopublication through Nanodash's {@link Utils#getNanopub(String)} (which uses the
     * configured registries and a local cache).
     *
     * @param id the query ID or URI
     * @throws IllegalArgumentException if the ID is invalid or the nanopublication does not
     *                                  contain exactly one query
     */
    private GrlcQuery(String id) {
        super(Utils.getNanopub(extractArtifactCode(id)), id);
    }

    private static String extractArtifactCode(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Null value for query ID");
        }
        Matcher m = ARTIFACT_CODE_PATTERN.matcher(id);
        if (m.find()) {
            return m.group();
        }
        throw new IllegalArgumentException("Not a valid query ID or URI: " + id);
    }

    /**
     * Creates a list of query parameter fields for the placeholders in the query.
     *
     * @param markupId The markup ID for the fields.
     * @return A list of query parameter fields.
     */
    public List<QueryParamField> createParamFields(String markupId) {
        List<QueryParamField> l = new ArrayList<>();
        for (String s : getPlaceholdersList()) {
            // Magic placeholders are bound from the session, not entered by the user.
            if (MagicQueryParams.isMagic(s)) continue;
            l.add(new QueryParamField(markupId, s));
        }
        return l;
    }

    /**
     * Expands the SPARQL query by substituting the user-entered param-field values, plus the
     * session-bound {@linkplain MagicQueryParams magic} placeholders (which are not part of the
     * form fields). This adapts the UI {@link QueryParamField}s into the parameter map of
     * {@link QueryTemplate#expandQuery(Map, boolean)} and expands non-strictly: missing/unset
     * params are not errors but left partially expanded (the placeholder kept for single values,
     * the empty {@code VALUES} block dropped for multi values), as needed for the Yasgui link.
     *
     * <p>Resolves magic values from the current session, so call on the request thread.</p>
     *
     * @param paramFields the list of query parameter fields with user-entered values
     * @return the expanded SPARQL query string
     */
    public String expandQuery(List<QueryParamField> paramFields) {
        Map<String, List<String>> params = new HashMap<>();
        for (QueryParamField f : paramFields) {
            if (f.isSet()) params.put(f.getParamName(), List.of(f.getValues()));
        }
        // Magic placeholders are excluded from the form fields, so fill them from the session here.
        params.putAll(MagicQueryParams.resolve(this));
        return expandQuery(params, false);
    }

    /**
     * Returns true if all mandatory (non-optional) param fields have values set.
     *
     * @param paramFields the list of query parameter fields
     * @return true if all mandatory fields are set
     */
    public static boolean allMandatoryFieldsSet(List<QueryParamField> paramFields) {
        for (QueryParamField f : paramFields) {
            if (!f.isOptional() && !f.isSet()) return false;
        }
        return true;
    }

}
