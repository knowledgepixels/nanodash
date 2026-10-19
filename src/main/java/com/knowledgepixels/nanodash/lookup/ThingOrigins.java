package com.knowledgepixels.nanodash.lookup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.eclipse.rdf4j.model.IRI;

import java.util.concurrent.TimeUnit;

/**
 * Tells who a thing found in the nanopub network comes from (issue #765): the Space that
 * governs it, if it is a maintained resource of a Space or lies in the namespace of one, or
 * otherwise the user who signed the nanopublication that introduced it. The signers are
 * recorded by {@link NanopubQueryLookup} as it reads its results.
 */
public final class ThingOrigins {

    private static final Cache<String, String> signerPubkeyHashes = CacheBuilder.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build();

    private ThingOrigins() {
    }

    /**
     * Records the key that signed the nanopublication a thing was found in.
     *
     * @param thing  the thing
     * @param pubkey the public key the nanopublication was signed with
     */
    static void recordSigner(String thing, String pubkey) {
        if (thing == null || pubkey == null || pubkey.isEmpty()) return;
        signerPubkeyHashes.put(thing, Utils.createSha256HexHash(pubkey));
    }

    /**
     * Forgets all recorded signers.
     */
    static void clear() {
        signerPubkeyHashes.invalidateAll();
    }

    /**
     * Returns a note that says who a thing comes from, e.g. "governed by Knowledge Pixels" or
     * "by Jane Doe".
     *
     * @param thing the thing
     * @return the note, or null if it is not known who the thing comes from
     */
    public static String getOriginNote(String thing) {
        if (thing == null) return null;
        Space space = getGoverningSpace(thing);
        if (space != null && space.getLabel() != null) return "governed by " + space.getLabel();
        String signer = getSignerName(thing);
        if (signer != null) return "by " + signer;
        return null;
    }

    /**
     * Returns the Space that governs a thing: the Space of the maintained resource it is, or
     * of the maintained resource whose namespace it lies in.
     *
     * @param thing the thing
     * @return the governing Space, or null if no Space governs the thing
     */
    static Space getGoverningSpace(String thing) {
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(thing);
        if (resource instanceof MaintainedResource) return resource.getSpace();
        if (resource instanceof Space) return null;
        MaintainedResource containing = MaintainedResourceRepository.get().findByNamespace(MaintainedResource.getNamespace(thing));
        if (containing == null) return null;
        return containing.getSpace();
    }

    private static String getSignerName(String thing) {
        String pubkeyHash = signerPubkeyHashes.getIfPresent(thing);
        if (pubkeyHash == null) return null;
        IRI user = User.findSingleIdForPubkeyhash(pubkeyHash);
        if (user == null) return null;
        return User.getShortDisplayNameForPubkeyhash(user, pubkeyHash);
    }

}
