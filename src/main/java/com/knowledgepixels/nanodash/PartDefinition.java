package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The nanopublication that defines a part of a resource, as the part page, its About tab,
 * the Explore page's forwarding and the document export all resolve it, together with the
 * rank of whoever signed it.
 * <p>
 * <b>The owning resource's rule comes first</b>, unchanged: the newest definition signed by a
 * role-holder of the owning space at its {@code gen:hasPartDefinitionTier} or above (every
 * role-holder when it declares none), or by the owning user on a user page
 * ({@link ViewDataFetcher#partDefinitionPubkeys}).
 * <p>
 * <b>A view display can lower the bar for itself</b> with a {@code gen:hasPartDefinitionTier}
 * of its own; a tier at or above the owning resource's has no effect, so a display can only
 * open itself up, never close itself off. Only where the owning resource's rule finds no
 * definition are those lower tiers tried, most restrictive first, and a definition found that
 * way is taken only if a display accepting its signer's rank actually applies to the part —
 * so a space that opens one view to everybody's parts does not thereby present everybody's
 * nanopublications as its parts elsewhere. {@code gen:EveryoneRole} is the lowest tier,
 * anyone at all: for an IRI minted inside a nanopublication (one carrying its artifact code),
 * that nanopublication defines it, or its latest same-key version — never a third party's
 * nanopublication, so nobody can redefine someone else's part. It has to introduce or
 * describe the IRI ({@code npx:introduces}, {@code npx:describes}); merely embedding it does
 * not count there, unlike for role-holders. A space declaring {@code gen:EveryoneRole} itself
 * opens every one of its part views in the same way.
 * <p>
 * The signer rank is what each display is matched against
 * ({@link ViewDisplay#acceptsPartDefinition}): a display declaring no tier requires the owning
 * resource's own rank, so existing displays behave exactly as before.
 * <p>
 * Which displays a resource has is part of the question, and a resource loads them
 * asynchronously: {@link #resolve(String, String, AbstractResourceWithProfile)} asks for them
 * and waits briefly, and a caller that needs the answer within one request whatever the
 * state of that load hands in displays it fetched itself
 * ({@link #resolve(String, String, AbstractResourceWithProfile, List)}).
 *
 * @param nanopubId  the defining nanopublication's id, or null if there is none
 * @param nanopub    the defining nanopublication, or null
 * @param signerRank the rank of its signer in the owning resource (see
 *                   {@link TierRanks#partDefinitionRank}), or {@link Integer#MAX_VALUE}
 *                   when there is no definition, which filters no display
 * @param pubkeys    the public key hashes whose definitions were looked up to find it, so a
 *                   query resolving the definition itself (the part Info view) finds the same
 *                   one; pass them on through {@link ViewDataFetcher#restrictingPubkeys}, as
 *                   no keys at all would mean no restriction to such a query
 */
public record PartDefinition(String nanopubId, Nanopub nanopub, int signerRank, List<String> pubkeys) {

    private static final Logger logger = LoggerFactory.getLogger(PartDefinition.class);

    /**
     * What a view's {@code …Np} parameter is given for a part without a defining
     * nanopublication: a reference that matches none.
     */
    public static final String NO_NANOPUB_REF = "x:";

    /**
     * No definition: the part is known by its IRI alone (and an agent by being one), and
     * every display may apply to it by namespace, class or explicit target as before. Keeps
     * the keys the owning resource's rule looked among, so a query resolving the definition
     * itself keeps that rule too.
     */
    private static PartDefinition none(List<String> keys) {
        return new PartDefinition(null, null, Integer.MAX_VALUE, keys);
    }

    // An IRI minted inside a nanopublication: its artifact code followed by a local part.
    // The same test the Explore page applies before resolving such an IRI to its nanopub.
    private static final Pattern MINTED_IRI = Pattern.compile("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$");

    /**
     * Whether a definition was found.
     *
     * @return true if there is a defining nanopublication
     */
    public boolean exists() {
        return nanopubId != null;
    }

    /**
     * The definition as a view's {@code …Np} parameter takes it.
     *
     * @return the defining nanopublication's id, or {@link #NO_NANOPUB_REF} if there is none
     */
    public String nanopubRef() {
        return nanopubId == null ? NO_NANOPUB_REF : nanopubId;
    }

    /**
     * Resolves the definition of a part of the given resource, among the view displays the
     * resource has loaded. Asking starts that load where it has not happened yet and waits
     * briefly for the displays to be seeded from the cache; on a resource whose displays are
     * still not there after that, no display can open the part to a lower tier yet, and a
     * caller keeping the answer should resolve again once
     * {@link AbstractResourceWithProfile#isDataInitialized()} says they are.
     *
     * @param partId    the part IRI
     * @param contextId the owning resource's IRI
     * @param resource  the owning resource
     * @return the definition; without a nanopublication if there is none
     */
    public static PartDefinition resolve(String partId, String contextId, AbstractResourceWithProfile resource) {
        resource.isDataInitialized();
        return resolve(partId, contextId, resource, resource.getViewDisplays());
    }

    /**
     * Resolves the definition of a part of the given resource, among the given view displays
     * of that resource: all of them, as fetched, not only those applying to the part.
     *
     * @param partId    the part IRI
     * @param contextId the owning resource's IRI
     * @param resource  the owning resource
     * @param displays  the resource's view displays
     * @return the definition; without a nanopublication if there is none
     */
    public static PartDefinition resolve(String partId, String contextId, AbstractResourceWithProfile resource, List<ViewDisplay> displays) {
        Space space = resource.getSpace();
        int baseRank = baseRank(resource);

        // The owning resource's own rule, exactly as the part page has always applied it.
        List<String> baseKeys = ViewDataFetcher.partDefinitionPubkeys(contextId, resource);
        int baseKeyRank = keyRank(baseRank, space);
        // Never asked with no keys, which the definitions query takes as no restriction at
        // all: where the role-holders or their keys cannot be read right now, nobody's
        // nanopublication counts rather than everybody's.
        PartDefinition definition = baseKeys.isEmpty() ? null
                : lookup(ViewDataFetcher.partDefinitionQueryRef(partId, baseKeys), baseKeys, baseKeyRank, space, contextId);
        if (definition != null) {
            return definition;
        }

        // The lower tiers some display of the resource opens itself to, most restrictive first.
        for (int rank : declaredRanksBelow(displays, baseRank)) {
            List<String> keys = keysBelow(rank, space, baseKeyRank);
            if (!keys.isEmpty()) {
                definition = lookup(ViewDataFetcher.partDefinitionQueryRef(partId, keys), keys, keyRank(rank, space), space, contextId);
                if (definition != null && isAcceptedByADisplay(definition, partId, resource, displays, baseRank)) {
                    return definition;
                }
            }
            if (rank == TierRanks.ANYONE) {
                definition = minted(partId, space, contextId);
                if (definition != null && isAcceptedByADisplay(definition, partId, resource, displays, baseRank)) {
                    return definition;
                }
            }
        }

        // A space open to anyone has no display to ask: every one of its views follows its rule.
        if (baseRank == TierRanks.ANYONE) {
            definition = minted(partId, space, contextId);
            if (definition != null) return definition;
        }
        return none(baseKeys);
    }

    /**
     * The lookups the definition of a part is resolved through, whichever of them ends up
     * answering: what a publication that redefines the part has to invalidate for the part
     * page to show it.
     *
     * @param partId    the part IRI
     * @param contextId the owning resource's IRI
     * @param resource  the owning resource
     * @return the lookups, possibly none
     */
    public static List<QueryRef> lookupRefs(String partId, String contextId, AbstractResourceWithProfile resource) {
        List<QueryRef> refs = new ArrayList<>();
        Space space = resource.getSpace();
        int baseRank = baseRank(resource);
        List<String> baseKeys = ViewDataFetcher.partDefinitionPubkeys(contextId, resource);
        if (!baseKeys.isEmpty()) {
            refs.add(ViewDataFetcher.partDefinitionQueryRef(partId, baseKeys));
        }
        boolean openToAnyone = baseRank == TierRanks.ANYONE;
        for (int rank : declaredRanksBelow(resource.getViewDisplays(), baseRank)) {
            List<String> keys = keysBelow(rank, space, keyRank(baseRank, space));
            if (!keys.isEmpty()) {
                refs.add(ViewDataFetcher.partDefinitionQueryRef(partId, keys));
            }
            if (rank == TierRanks.ANYONE) openToAnyone = true;
        }
        if (openToAnyone) {
            Nanopub minting = mintingNanopub(partId);
            if (minting != null) refs.add(latestVersionRef(partId, minting));
        }
        return refs;
    }

    /**
     * The rank whoever holds a key of the list for the given tier has at least: among a
     * space's keys every signer holds a role, so ranks no lower than any role-holder, while a
     * user page has the one key list, the owner's.
     */
    private static int keyRank(int rank, Space space) {
        return space != null ? Math.max(rank, TierRanks.EVERYONE) : rank;
    }

    /**
     * The keys of the role-holders at the given rank or above, where those are more than the
     * owning resource's rule already looked among; none otherwise. A user page has no tiers
     * below its owner, and below the role-holders of a space there are no further keys:
     * anyone beyond them is reached through the nanopublication minting the part instead.
     */
    private static List<String> keysBelow(int rank, Space space, int baseKeyRank) {
        if (space == null || keyRank(rank, space) >= baseKeyRank) return List.of();
        return ViewDataFetcher.partDefinitionPubkeys(space, keyRank(rank, space));
    }

    /**
     * The rank the owning resource requires of a part's definition when a display declares
     * none: the space's {@code gen:hasPartDefinitionTier}, or, on a user page, the user
     * themselves (who counts as the page's sole admin, as for {@code gen:isVisibleTo}).
     *
     * @param resource the owning resource
     * @return the required rank
     */
    public static int baseRank(AbstractResourceWithProfile resource) {
        Space space = resource.getSpace();
        if (space != null) {
            return space.getPartDefinitionTierRank();
        }
        return TierRanks.ADMIN;
    }

    /**
     * The classes a definition gives a part: what it says with {@code rdf:type} about it,
     * plus {@code gen:IndividualAgent} when the part is an agent. With no definition, what
     * is left is what the part is whoever defines it.
     *
     * @param nanopub the defining nanopublication, or null
     * @param partId  the part IRI
     * @return the classes, possibly empty
     */
    public static Set<IRI> classesOf(Nanopub nanopub, String partId) {
        Set<IRI> classes = declaredClassesOf(nanopub, partId);
        if (IndividualAgent.isAgentId(partId)) {
            classes.add(KPXL_TERMS.INDIVIDUAL_AGENT);
        }
        return classes;
    }

    /**
     * What a definition says about a part with {@code rdf:type}, and nothing else.
     *
     * @param nanopub the defining nanopublication, or null
     * @param partId  the part IRI
     * @return the declared classes, possibly empty
     */
    public static Set<IRI> declaredClassesOf(Nanopub nanopub, String partId) {
        Set<IRI> classes = new HashSet<>();
        if (nanopub != null) {
            for (Statement st : nanopub.getAssertion()) {
                if (st.getSubject().stringValue().equals(partId) && st.getPredicate().equals(RDF.TYPE)
                        && st.getObject() instanceof IRI objIri) {
                    classes.add(objIri);
                }
            }
        }
        return classes;
    }

    /**
     * The distinct part-definition ranks the given view displays declare below the given
     * one, most restrictive first. Deactivated and outdated displays are included: this only
     * decides which lookups are worth trying, and {@link #isAcceptedByADisplay} asks the
     * displays the page would actually render.
     */
    private static List<Integer> declaredRanksBelow(List<ViewDisplay> displays, int baseRank) {
        TreeSet<Integer> ranks = new TreeSet<>();
        for (ViewDisplay vd : displays) {
            IRI tier = vd.getPartDefinitionTier();
            if (tier == null) continue;
            int rank = TierRanks.partDefinitionRank(tier);
            if (rank < baseRank) ranks.add(rank);
        }
        return new ArrayList<>(ranks.descendingSet());
    }

    private static boolean isAcceptedByADisplay(PartDefinition definition, String partId, AbstractResourceWithProfile resource,
                                                List<ViewDisplay> displays, int baseRank) {
        Set<IRI> classes = classesOf(definition.nanopub(), partId);
        for (ViewDisplay vd : resource.getPartLevelViewDisplays(displays, partId, classes)) {
            if (vd.acceptsPartDefinition(definition.signerRank(), baseRank)) return true;
        }
        return false;
    }

    /**
     * The newest definition among the given keys. Whoever signed it holds one of them, so its
     * signer ranks at least {@code keyRank} even where the nanopub or the key mapping cannot be
     * read right now: a passing failure must not hide the displays that rank qualifies for.
     * The keys must not be empty, which the query would take as no restriction.
     */
    private static PartDefinition lookup(QueryRef queryRef, List<String> keys, int keyRank, Space space, String contextId) {
        ApiResponse response = ApiCache.retrieveResponseSync(queryRef, false);
        if (response == null || response.getData().isEmpty()) {
            return null;
        }
        String nanopubId = response.getData().iterator().next().get("np");
        Nanopub nanopub = Utils.getAsNanopub(nanopubId);
        int rank = Math.max(keyRank, signerRank(nanopub, space, contextId));
        return new PartDefinition(nanopubId, nanopub, rank, keys);
    }

    /**
     * The nanopublication whose artifact code the given IRI carries, or null if the IRI
     * carries none or that nanopublication cannot be found.
     */
    private static Nanopub mintingNanopub(String partId) {
        Matcher m = MINTED_IRI.matcher(partId);
        if (!m.matches()) return null;
        return Utils.getAsNanopub(m.group(2));
    }

    /**
     * The lookup of the latest same-key versions of the nanopublication that minted a part
     * in which the part is still introduced or described.
     */
    private static QueryRef latestVersionRef(String partId, Nanopub minting) {
        QueryRef latestRef = new QueryRef(QueryApiAccess.GET_LATEST_THING_NANOPUB, "thing", partId);
        latestRef.getParams().put("np", minting.getUri().stringValue());
        return latestRef;
    }

    /**
     * The definition by the nanopublication that minted the part's IRI, in its latest
     * same-key version, or null if the IRI was not minted in one or that nanopublication was
     * retracted or neither introduces nor describes it. Where its versions have forked into
     * several latest ones, the most recently created counts.
     */
    private static PartDefinition minted(String partId, Space space, String contextId) {
        Nanopub minting = mintingNanopub(partId);
        if (minting == null) return null;
        ApiResponse response = ApiCache.retrieveResponseSync(latestVersionRef(partId, minting), false);
        if (response == null || response.getData().isEmpty()) {
            // Retracted, or the IRI is merely under its namespace without being introduced.
            return null;
        }
        String nanopubId = null;
        Instant newest = null;
        for (ApiResponseEntry row : response.getData()) {
            Instant created = parseInstant(row.get("date"));
            if (nanopubId == null || (created != null && (newest == null || created.isAfter(newest)))) {
                nanopubId = row.get("latestVersion");
                newest = created;
            }
        }
        Nanopub nanopub = nanopubId.equals(minting.getUri().stringValue()) ? minting : Utils.getAsNanopub(nanopubId);
        if (nanopub == null) return null;
        String pubkeyHash = pubkeyHash(nanopub);
        // Without a readable key there is nothing to hand a query resolving the definition
        // itself that would keep it from reading anybody's.
        if (pubkeyHash == null) return null;
        List<String> keys = new ArrayList<>();
        keys.add(pubkeyHash);
        return new PartDefinition(nanopubId, nanopub, signerRank(nanopub, space, contextId), keys);
    }

    private static Instant parseInstant(String dateTime) {
        if (dateTime == null) return null;
        try {
            return OffsetDateTime.parse(dateTime).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * The rank of a nanopublication's signer in the owning resource: their tier among the
     * space's role-holders ({@link TierRanks#EVERYONE} at least), the owner's
     * admin rank on a user page, and {@link TierRanks#ANYONE} for anyone else.
     * Matched on the approved keys, as {@link ViewDataFetcher#partDefinitionPubkeys} collects
     * them, rather than on the nanopub's self-declared signer.
     */
    static int signerRank(Nanopub nanopub, Space space, String contextId) {
        String hash = pubkeyHash(nanopub);
        if (hash == null) return TierRanks.ANYONE;
        if (space != null) {
            int rank = TierRanks.ANYONE;
            for (IRI userIri : space.getUsers()) {
                if (User.getUserData().getPubkeyHashes(userIri, true).contains(hash)) {
                    rank = Math.max(rank, Math.max(space.userTier(userIri), TierRanks.EVERYONE));
                }
            }
            return rank;
        }
        if (User.getUserData().getPubkeyHashes(Utils.vf.createIRI(contextId), true).contains(hash)) {
            return TierRanks.ADMIN;
        }
        return TierRanks.ANYONE;
    }

    static String pubkeyHash(Nanopub nanopub) {
        if (nanopub == null) return null;
        try {
            NanopubSignatureElement se = SignatureUtils.getSignatureElement(nanopub);
            if (se == null || se.getPublicKeyString() == null) return null;
            return Utils.createSha256HexHash(se.getPublicKeyString());
        } catch (MalformedCryptoElementException ex) {
            logger.warn("Cannot read the signature of {}", nanopub.getUri(), ex);
            return null;
        }
    }

}
