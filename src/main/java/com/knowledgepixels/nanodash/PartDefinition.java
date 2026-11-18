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
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * of its own. Only where the owning resource's rule finds no definition are those lower tiers
 * tried, most restrictive first, and a definition found that way is taken only if a display
 * accepting its signer's rank actually applies to the part — so a space that opens one view
 * to everybody's parts does not thereby present everybody's nanopublications as its parts
 * elsewhere. {@code gen:EveryoneRole} is the lowest tier, anyone at all: for an IRI minted
 * inside a nanopublication (one carrying its artifact code), that nanopublication defines
 * it, or its latest same-key version — never a third party's nanopublication, so nobody can
 * redefine someone else's part. A space declaring {@code gen:EveryoneRole} itself opens
 * every one of its part views in the same way.
 * <p>
 * The signer rank is what each display is matched against
 * ({@link ViewDisplay#acceptsPartDefinition}): a display declaring no tier requires the owning
 * resource's own rank, so existing displays behave exactly as before.
 *
 * @param nanopubId  the defining nanopublication's id, or null if there is none
 * @param nanopub    the defining nanopublication, or null
 * @param signerRank the rank of its signer in the owning resource (see
 *                   {@link TierRanks#partDefinitionRank}), or {@link Integer#MAX_VALUE}
 *                   when there is no definition, which filters no display
 * @param pubkeys    the public key hashes whose definitions were looked up to find it, so a
 *                   query resolving the definition itself (the part Info view) finds the same
 *                   one
 */
public record PartDefinition(String nanopubId, Nanopub nanopub, int signerRank, List<String> pubkeys) {

    private static final Logger logger = LoggerFactory.getLogger(PartDefinition.class);

    /**
     * No definition: the part is known by its IRI alone (and an agent by being one), and
     * every display may apply to it by namespace, class or explicit target as before. Keeps
     * the keys the owning resource's rule looked among, so a query resolving the definition
     * itself keeps that rule too: no keys at all would mean no restriction to it.
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
     * Resolves the definition of a part of the given resource.
     *
     * @param partId    the part IRI
     * @param contextId the owning resource's IRI
     * @param resource  the owning resource
     * @return the definition; without a nanopublication if there is none
     */
    public static PartDefinition resolve(String partId, String contextId, AbstractResourceWithProfile resource) {
        Space space = resource.getSpace();
        int baseRank = baseRank(resource);

        // The owning resource's own rule, exactly as the part page has always applied it.
        List<String> baseKeys = ViewDataFetcher.partDefinitionPubkeys(contextId, resource);
        // Among a space's keys every signer holds a role, so ranks no lower than any role-holder.
        int baseKeyRank = space != null ? Math.max(baseRank, TierRanks.EVERYONE) : baseRank;
        PartDefinition definition = lookup(ViewDataFetcher.partDefinitionQueryRef(partId, contextId, resource), baseKeys, baseKeyRank, space, contextId);
        if (definition != null) {
            return definition;
        }

        // The lower tiers some display of the resource opens itself to, most restrictive first.
        for (int rank : declaredRanksBelow(resource, baseRank)) {
            definition = atRank(rank, partId, space, contextId);
            if (definition != null && isAcceptedByADisplay(definition, partId, resource, baseRank)) {
                return definition;
            }
            if (rank == TierRanks.ANYONE) {
                definition = minted(partId, space, contextId);
                if (definition != null && isAcceptedByADisplay(definition, partId, resource, baseRank)) {
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
     * The newest definition among the role-holders at the given rank or above, or null. On a
     * user page there are no tiers below the owner to look among. Never asked with no keys:
     * the definitions query takes an empty key list as no restriction at all.
     */
    private static PartDefinition atRank(int rank, String partId, Space space, String contextId) {
        if (space == null) return null;
        int keyRank = Math.max(rank, TierRanks.EVERYONE);
        List<String> keys = ViewDataFetcher.partDefinitionPubkeys(space, keyRank);
        if (keys.isEmpty()) return null;
        return lookup(ViewDataFetcher.partDefinitionQueryRef(partId, keys), keys, keyRank, space, contextId);
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
     * plus {@code gen:IndividualAgent} when the part is an agent. The same reading the part
     * page applies ({@code ResourcePartPage.getPartClasses}).
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
     * The distinct part-definition ranks the resource's view displays declare below the given
     * one, most restrictive first. Deactivated and outdated displays are included: this only
     * decides which lookups are worth trying, and {@link #isAcceptedByADisplay} asks the
     * displays the page would actually render.
     */
    private static List<Integer> declaredRanksBelow(AbstractResourceWithProfile resource, int baseRank) {
        TreeSet<Integer> ranks = new TreeSet<>();
        for (ViewDisplay vd : resource.getViewDisplays()) {
            IRI tier = vd.getPartDefinitionTier();
            if (tier == null) continue;
            int rank = TierRanks.partDefinitionRank(tier);
            if (rank < baseRank) ranks.add(rank);
        }
        return new ArrayList<>(ranks.descendingSet());
    }

    private static boolean isAcceptedByADisplay(PartDefinition definition, String partId, AbstractResourceWithProfile resource, int baseRank) {
        Set<IRI> classes = classesOf(definition.nanopub(), partId);
        for (ViewDisplay vd : resource.getPartLevelViewDisplays(partId, classes)) {
            if (vd.acceptsPartDefinition(definition.signerRank(), baseRank)) return true;
        }
        return false;
    }

    /**
     * The newest definition among the given keys. Whoever signed it holds one of them, so its
     * signer ranks at least {@code keyRank} even where the nanopub or the key mapping cannot be
     * read right now: a passing failure must not hide the displays that rank qualifies for.
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
     * The definition by the nanopublication that minted the part's IRI, in its latest
     * same-key version, or null if the IRI was not minted in one or that nanopublication was
     * retracted or does not introduce it.
     */
    private static PartDefinition minted(String partId, Space space, String contextId) {
        Matcher m = MINTED_IRI.matcher(partId);
        if (!m.matches()) return null;
        Nanopub minting = Utils.getAsNanopub(m.group(2));
        if (minting == null) return null;
        QueryRef latestRef = new QueryRef(QueryApiAccess.GET_LATEST_THING_NANOPUB, "thing", partId);
        latestRef.getParams().put("np", minting.getUri().stringValue());
        ApiResponse response = ApiCache.retrieveResponseSync(latestRef, false);
        if (response == null || response.getData().isEmpty()) {
            // Retracted, or the IRI is merely under its namespace without being introduced.
            return null;
        }
        String nanopubId = response.getData().getFirst().get("latestVersion");
        Nanopub nanopub = nanopubId.equals(minting.getUri().stringValue()) ? minting : Utils.getAsNanopub(nanopubId);
        if (nanopub == null) return null;
        String pubkeyHash = pubkeyHash(nanopub);
        return new PartDefinition(nanopubId, nanopub, signerRank(nanopub, space, contextId),
                pubkeyHash == null ? List.of() : List.of(pubkeyHash));
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
