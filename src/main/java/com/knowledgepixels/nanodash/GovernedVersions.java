package com.knowledgepixels.nanodash;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helpers around space-governed versioning: definitions that float within a
 * {@code (kind, space)} pair via {@code dct:isVersionOf} + {@code gen:governedBy}
 * instead of via an {@code npx:supersedes} chain.
 * <p>
 * See docs/views-and-presets-as-maintained-resources.md and
 * docs/template-identity-and-governance.md. The authority checks (signer is a
 * current member+ of the space, kind is a maintained resource of that space,
 * retracted versions skipped) all happen server-side in the
 * {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} query; this class only
 * assembles its inputs and reads its single row.
 * <p>
 * The declaration only takes effect once the kind is a maintained resource of the
 * space. Until then the same query resolves the pinned version like an ungoverned
 * one, along its own same-key supersedes chain, so declaring {@code gen:governedBy}
 * ahead of the registration no longer freezes the pin.
 */
public class GovernedVersions {

    private GovernedVersions() {
    } // no instances allowed

    /**
     * The {@code (kind, space)} pair a governed definition version declares, along with
     * the nanopublication declaring it, as plain strings so it can be held by Wicket
     * components across serialization.
     */
    public static class GovernedRef implements Serializable {

        private final String kind;
        private final String space;
        private final String nanopubId;

        private GovernedRef(String kind, String space, String nanopubId) {
            this.kind = kind;
            this.space = space;
            this.nanopubId = nanopubId;
        }

        /**
         * @return the definition kind, i.e. the {@code dct:isVersionOf} target
         */
        public String getKind() {
            return kind;
        }

        /**
         * @return the governing space, i.e. the {@code gen:governedBy} target
         */
        public String getSpace() {
            return space;
        }

        /**
         * @return the nanopublication declaring the pair, which is the pin the
         * governed-version query resolves from
         */
        public String getNanopubId() {
            return nanopubId;
        }

    }

    /**
     * Finds the space-governed definition embedded in the given nanopublication:
     * an {@code npx:embeds} target that declares both {@code dct:isVersionOf} and
     * {@code gen:governedBy} in the assertion. This is the same shape the
     * {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} query matches on, and it
     * is type-agnostic — it covers governed views, templates, presets and queries
     * alike.
     *
     * @param np the nanopublication to inspect
     * @return the declared kind/space pair, or null if this nanopublication does
     * not embed a governed definition
     */
    public static GovernedRef findGovernedRef(Nanopub np) {
        if (np == null) return null;
        Set<String> embedded = NanopubUtils.getEmbeddedIriIds(np);
        if (embedded.isEmpty()) return null;
        Map<String, String> kinds = new HashMap<>();
        Map<String, String> spaces = new HashMap<>();
        for (Statement st : np.getAssertion()) {
            String subj = st.getSubject().stringValue();
            if (!embedded.contains(subj)) continue;
            if (!(st.getObject() instanceof IRI obj)) continue;
            if (st.getPredicate().equals(DCTERMS.IS_VERSION_OF)) {
                kinds.put(subj, obj.stringValue());
            } else if (st.getPredicate().equals(KPXL_TERMS.GOVERNED_BY)) {
                spaces.put(subj, obj.stringValue());
            }
        }
        // Nanopublications embedding more than one governed definition aren't a
        // thing we produce; taking the first match is enough.
        for (Map.Entry<String, String> kind : kinds.entrySet()) {
            String space = spaces.get(kind.getKey());
            if (space != null) return new GovernedRef(kind.getValue(), space, np.getUri().stringValue());
        }
        return null;
    }

    /**
     * Builds the query reference resolving the current version of a governed pin: the
     * newest version of its {@code (kind, space)} pair if the kind is a maintained
     * resource of the space, otherwise the head of the pin's own supersedes chain.
     *
     * @param kindIri  the definition kind (the {@code dct:isVersionOf} target)
     * @param spaceIri the governing space
     * @param pinId    the nanopublication of the pinned version
     * @return the query reference
     */
    public static QueryRef getQueryRef(String kindIri, String spaceIri, String pinId) {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("kind", kindIri);
        params.put("space", spaceIri);
        params.put("pin", pinId);
        return new QueryRef(QueryApiAccess.GET_LATEST_GOVERNED_VERSION, params);
    }

    /**
     * Builds the query reference resolving the current version for the given pair,
     * pinned at the nanopublication it was read off.
     *
     * @param ref the kind/space pair
     * @return the query reference
     */
    public static QueryRef getQueryRef(GovernedRef ref) {
        return getQueryRef(ref.getKind(), ref.getSpace(), ref.getNanopubId());
    }

    /**
     * Resolves the current version of a governed pin, blocking on the query if it
     * isn't cached yet.
     *
     * @param kindIri  the definition kind (the {@code dct:isVersionOf} target)
     * @param spaceIri the governing space
     * @param pinId    the nanopublication of the pinned version
     * @return the definition IRI of the current version, or null if there is no valid
     * candidate (the caller then keeps its pin)
     */
    public static String getLatestVersionIriSync(String kindIri, String spaceIri, String pinId) {
        return getVersionIri(ApiCache.retrieveResponseSync(getQueryRef(kindIri, spaceIri, pinId), false));
    }

    /**
     * Reads the definition IRI off a {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION}
     * response.
     *
     * @param response the query response, may be null
     * @return the {@code version} value of the single row, or null if the response
     * is null or empty
     */
    public static String getVersionIri(ApiResponse response) {
        return getValue(response, "version");
    }

    /**
     * Reads the IRI of the nanopublication containing the current governed version
     * off a {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} response.
     *
     * @param response the query response, may be null
     * @return the {@code np} value of the single row, or null if the response is
     * null or empty
     */
    public static String getVersionNanopubIri(ApiResponse response) {
        return getValue(response, "np");
    }

    private static String getValue(ApiResponse response, String key) {
        if (response == null || response.getData().isEmpty()) return null;
        String value = response.getData().get(0).get(key);
        if (value == null || value.isEmpty()) return null;
        return value;
    }

}
