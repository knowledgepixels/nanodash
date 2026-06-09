package com.knowledgepixels.nanodash;

import com.google.common.collect.LinkedHashMultimap;
import org.apache.wicket.Session;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Session-bound ("magic") query parameters: view-query placeholders that Nanodash
 * fills automatically from the current browser session, rather than from a value
 * the caller passes or the user types into a form. They let a data-driven view
 * branch on session/site-local state (the local signing key, the site URL) that a
 * SPARQL query keyed only on a resource IRI cannot otherwise see. See
 * docs/magic-query-params.md.
 *
 * <p>A placeholder is "magic" iff its parameter name — what
 * {@link QueryTemplate#getParamName(String)} produces after stripping the leading
 * underscore(s) and the type suffix — is a registered magic name. Names are
 * written in {@code SCREAMING_CASE} (e.g. {@code ?__LOCALPUBKEY_multi}) by
 * convention, but detection is pure registry membership, not case.</p>
 *
 * <p>Magic placeholders are wired into the SPARQL as <b>optional multi</b>
 * placeholders (double-underscore prefix, {@code _multi} suffix) with an explicit
 * empty {@code values ?__NAME_multi {}} block. The live grlc service fills that
 * block from the bound value(s) when present, and degrades gracefully to "no
 * binding" when absent — so a query reads the value via a normal SPARQL variable
 * and need not special-case the logged-out viewer. Comparisons against a possibly
 * unbound magic variable should be {@code coalesce}-guarded.</p>
 */
public class MagicQueryParams {

    private static final Logger logger = LoggerFactory.getLogger(MagicQueryParams.class);

    private MagicQueryParams() {
    }

    /**
     * Registry: magic parameter name (the {@code getParamName} stem) to a resolver
     * of its value(s) from the current session. A resolver returns an empty list
     * when the value is unavailable (logged out, no key pair), so the binding is
     * simply omitted.
     */
    private static final Map<String, Supplier<List<String>>> REGISTRY = Map.of(
            "LOCALPUBKEY", MagicQueryParams::localPubkey,
            "SITEURL", MagicQueryParams::siteUrl,
            "CURRENTUSER", MagicQueryParams::currentUser
    );

    /**
     * Whether the given raw placeholder (as returned by
     * {@link QueryTemplate#getPlaceholdersList()}) is a magic parameter.
     *
     * @param rawPlaceholder the raw placeholder name, or null
     * @return true if it resolves to a registered magic name
     */
    public static boolean isMagic(String rawPlaceholder) {
        if (rawPlaceholder == null) return false;
        return REGISTRY.containsKey(QueryTemplate.getParamName(rawPlaceholder));
    }

    /**
     * Returns the query reference augmented with session-bound values for any magic
     * placeholders its query declares, resolving the query by id. The reference is
     * returned unchanged if the query can't be loaded, declares no magic
     * placeholders, or no values are available.
     *
     * <p>Call this on the <b>request thread</b> (it reads the Wicket session),
     * before the reference is handed to {@link ApiCache} (whose fetch runs on
     * background threads without a session).</p>
     *
     * @param queryRef the query reference, or null
     * @return the (possibly augmented) query reference
     */
    public static QueryRef augment(QueryRef queryRef) {
        if (queryRef == null) return queryRef;
        GrlcQuery query;
        try {
            query = GrlcQuery.get(queryRef.getQueryId());
        } catch (Exception ex) {
            logger.error("Could not resolve query for magic-param binding: {}", queryRef.getQueryId(), ex);
            return queryRef;
        }
        return augment(queryRef, query);
    }

    /**
     * Core augmentation against an already-resolved query. Magic bindings are added
     * in a deterministic (name-sorted) order so the resulting
     * {@link QueryRef#getAsUrlString()} — the {@link ApiCache} key — is stable
     * across requests; the original parameters keep their order.
     *
     * @param queryRef the query reference, or null
     * @param query    the query whose placeholders to inspect, or null
     * @return the (possibly augmented) query reference
     */
    static QueryRef augment(QueryRef queryRef, GrlcQuery query) {
        if (queryRef == null || query == null) return queryRef;
        TreeMap<String, List<String>> magic = new TreeMap<>();
        for (String raw : query.getPlaceholdersList()) {
            String name = QueryTemplate.getParamName(raw);
            Supplier<List<String>> resolver = REGISTRY.get(name);
            if (resolver != null) magic.put(name, resolver.get());
        }
        if (magic.isEmpty()) return queryRef;
        LinkedHashMultimap<String, String> params = LinkedHashMultimap.create();
        params.putAll(queryRef.getParams());
        boolean added = false;
        for (Map.Entry<String, List<String>> e : magic.entrySet()) {
            for (String value : e.getValue()) {
                if (value != null && !value.isEmpty()) {
                    params.put(e.getKey(), value);
                    added = true;
                }
            }
        }
        if (!added) return queryRef;
        return new QueryRef(queryRef.getQueryId(), params);
    }

    private static List<String> localPubkey() {
        if (!Session.exists()) return List.of();
        String pubkey = NanodashSession.get().getPubkeyString();
        return (pubkey == null || pubkey.isEmpty()) ? List.of() : List.of(pubkey);
    }

    private static List<String> siteUrl() {
        String url = NanodashPreferences.get().getWebsiteUrl();
        return (url == null || url.isEmpty()) ? List.of() : List.of(url);
    }

    private static List<String> currentUser() {
        if (!Session.exists()) return List.of();
        IRI iri = NanodashSession.get().getUserIri();
        return iri == null ? List.of() : List.of(iri.stringValue());
    }

}
