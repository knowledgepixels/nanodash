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
import java.util.HashSet;
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
 * before the space maintains the kind no longer freezes the pin.
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

        /**
         * Whether this declares the same {@code (kind, space)} pair as the given one.
         *
         * @param other another pair, may be null
         * @return true if both kind and space are the same
         */
        public boolean isSamePairAs(GovernedRef other) {
            return other != null && kind.equals(other.kind) && space.equals(other.space);
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

    /**
     * The {@code mode} the {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} query reports
     * when the space doesn't maintain the kind and it followed the pin's supersedes chain
     * instead.
     */
    static final String MODE_SUPERSEDES = "supersedes";

    /**
     * How many hand-overs a single resolution follows at most. Real chains have one or two;
     * the bound only keeps a malformed one from looping.
     */
    static final int MAX_HANDOVERS = 8;

    /**
     * Where a definition reference lands: the nanopublication holding the version it stands
     * for, and that version's own IRI where the governed-version query named it. The IRI is
     * null where only a supersedes lookup was involved; the caller then takes the definition
     * the nanopublication embeds, as for any supersedes head.
     *
     * @param nanopubId  the nanopublication of the version
     * @param versionIri the version's IRI, or null if not known from the query
     */
    public record Resolution(String nanopubId, String versionIri) {

        /**
         * Whether this resolution stays at the given nanopublication.
         *
         * @param npId the nanopublication of the pinned version
         * @return true if there is nothing newer to move to
         */
        public boolean staysAt(String npId) {
            return nanopubId == null || nanopubId.equals(npId);
        }

    }

    /**
     * Resolves a reference to a definition (a view, a template, a preset) to the version it
     * currently stands for, also where the definition was handed over from one authority to
     * another along the way.
     * <p>
     * A pin that declares {@code gen:governedBy} resolves through the governed-version query;
     * one that doesn't follows its own supersedes chain. Whenever that lands on a version by
     * way of a supersedes chain — an ungoverned pin's head, or the head the query falls back to
     * while the space doesn't maintain the kind — and that version declares a governance pair
     * other than the one just resolved, the key that published it has handed the definition
     * over: from no governance to a space, or from a space that let go of it (no longer
     * maintains the kind) to another one. Resolution then carries on from that version within
     * its own pair, which takes effect only once the receiving space maintains the kind (the
     * query checks this, and otherwise just returns that version). A space that maintains the
     * kind keeps it: a governed result is final, whatever later versions by its members
     * declare, so a member cannot move a definition out of its space. See
     * docs/views-and-presets-as-maintained-resources.md.
     * <p>
     * This blocks on whatever lookups aren't cached yet.
     *
     * @param pinned the nanopublication of the referenced version
     * @return where the reference lands, which is the pin itself if nothing newer applies
     */
    public static Resolution resolve(Nanopub pinned) {
        return walk(pinned, new Lookups() {
            @Override
            public String supersedesHead(String npId) {
                return QueryApiAccess.getLatestVersionId(npId);
            }

            @Override
            public ApiResponse governedVersion(QueryRef ref) {
                return ApiCache.retrieveResponseSync(ref, false);
            }
        });
    }

    /**
     * Marks every lookup a resolution of the given pin goes through as outdated, so that the
     * next {@link #resolve(Nanopub)} asks the query API again at each step instead of
     * answering from a memo or cache that can be a minute old. For the places where the user
     * explicitly asks for current data, such as {@link View#refreshLatestVersion(String)}. The
     * steps are retraced from what the caches hold, without querying anything.
     *
     * @param pinned the nanopublication of the referenced version
     */
    public static void forgetResolution(Nanopub pinned) {
        walk(pinned, new Lookups() {
            @Override
            public String supersedesHead(String npId) {
                QueryRef ref = new QueryRef(QueryApiAccess.GET_LATEST_VERSION_OF_NP, "np", npId);
                ApiResponse cached = ApiCache.retrieveStaleResponse(ref);
                QueryApiAccess.forgetLatestVersion(npId);
                ApiCache.clearCache(ref, 0);
                if (cached == null || cached.getData().size() != 1) return npId;
                String head = cached.getData().getFirst().get("latest");
                return head == null || head.isEmpty() ? npId : head;
            }

            @Override
            public ApiResponse governedVersion(QueryRef ref) {
                ApiResponse cached = ApiCache.retrieveStaleResponse(ref);
                ApiCache.clearCache(ref, 0);
                return cached;
            }
        });
    }

    /**
     * The two lookups a resolution is made of, so that resolving and forgetting retrace the
     * same steps.
     */
    private interface Lookups {

        /**
         * @param npId a nanopublication
         * @return the head of its same-key supersedes chain, or the nanopublication itself
         */
        String supersedesHead(String npId);

        /**
         * @param ref the governed-version query for a pin
         * @return its response, possibly null
         */
        ApiResponse governedVersion(QueryRef ref);

    }

    private static Resolution walk(Nanopub pinned, Lookups lookups) {
        String pinId = pinned.getUri().stringValue();
        Resolution current = new Resolution(pinId, null);
        GovernedRef ref = findGovernedRef(pinned);
        Set<String> seen = new HashSet<>();
        seen.add(pinId);
        if (ref == null) {
            String headId = lookups.supersedesHead(pinId);
            if (headId == null || headId.equals(pinId)) return current;
            current = new Resolution(headId, null);
        } else {
            ApiResponse response = lookups.governedVersion(getQueryRef(ref));
            String npId = getVersionNanopubIri(response);
            // No candidate: the pin is the floor.
            if (npId == null) return current;
            current = new Resolution(npId, getVersionIri(response));
            if (!MODE_SUPERSEDES.equals(getValue(response, "mode"))) return current;
        }
        // The current version was reached along a supersedes chain. If it declares a pair of
        // its own, the definition was handed over, and resolution carries on within that pair.
        for (int i = 0; i < MAX_HANDOVERS; i++) {
            if (!seen.add(current.nanopubId())) break;
            GovernedRef next = findGovernedRef(Utils.getAsNanopub(current.nanopubId()));
            if (next == null || next.isSamePairAs(ref)) break;
            ApiResponse response = lookups.governedVersion(getQueryRef(next));
            String npId = getVersionNanopubIri(response);
            // The receiving space maintains the kind but has no version of it: the version
            // handing it over is the floor.
            if (npId == null) break;
            current = new Resolution(npId, getVersionIri(response));
            if (!MODE_SUPERSEDES.equals(getValue(response, "mode"))) break;
            ref = next;
        }
        return current;
    }

}
