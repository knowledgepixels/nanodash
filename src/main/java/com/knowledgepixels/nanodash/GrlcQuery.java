package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryParamField;
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
 * an instance cache with {@link #get} factory methods, and integration with the
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
     * Returns a singleton instance of GrlcQuery for the given QueryRef.
     *
     * @param ref the QueryRef object containing the query name
     * @return a GrlcQuery instance
     */
    public static GrlcQuery get(QueryRef ref) {
        return get(ref.getQueryId());
    }

    /**
     * Returns a singleton instance of GrlcQuery for the given query ID.
     *
     * @param id the unique identifier or URI of the query
     * @return a GrlcQuery instance
     */
    public static GrlcQuery get(String id) {
        if (id == null) return null;
        GrlcQuery cached = instanceMap.getIfPresent(id);
        if (cached == null) {
            try {
                GrlcQuery q = new GrlcQuery(id);
                id = q.getQueryId();
                cached = instanceMap.getIfPresent(id);
                if (cached != null) return cached;
                instanceMap.put(id, q);
                cached = q;
            } catch (Exception ex) {
                logger.error("Could not load query: {}", id, ex);
            }
        }
        return cached;
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
