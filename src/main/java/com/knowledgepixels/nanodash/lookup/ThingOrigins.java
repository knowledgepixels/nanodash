package com.knowledgepixels.nanodash.lookup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knowledgepixels.nanodash.NanodashThreadPool;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tells who a thing found in the nanopub network comes from (issue #765) and when it was
 * published (issue #817). It comes from the Space that governs it, if it is a maintained
 * resource of a Space, lies in the namespace of one, or declares one with
 * {@code gen:governedBy} as a governed view, template, query or preset version does, and
 * otherwise from the user who signed the nanopublication that introduced it. The signers and
 * dates are recorded by {@link NanopubQueryLookup} as it reads its results.
 */
public final class ThingOrigins {

    private static final Logger logger = LoggerFactory.getLogger(ThingOrigins.class);

    /**
     * How long the governing spaces of a set of things are waited for. The things found are
     * shown as soon as this is up, with what is known about them by then; a resolution still
     * running goes on filling the cache for the next search.
     */
    private static final Duration RESOLUTION_TIMEOUT = Duration.ofSeconds(2);

    /**
     * What is cached for a thing that governs itself, i.e. declares no governing Space, since
     * the cache cannot hold nulls.
     */
    private static final String NOT_GOVERNED = "";

    private static final Cache<String, String> signerPubkeyHashes = newCache();

    private static final Cache<String, String> publicationDates = newCache();

    private static final Cache<String, String> declaredGoverningSpaces = newCache();

    private static <T> Cache<String, T> newCache() {
        return CacheBuilder.newBuilder().maximumSize(50_000).expireAfterAccess(24, TimeUnit.HOURS).build();
    }

    private ThingOrigins() {
    }

    /**
     * Records what a query result says about a thing.
     *
     * @param thing  the thing
     * @param pubkey the public key the nanopublication it was found in was signed with
     * @param date   the date that nanopublication was published, in ISO-8601
     */
    static void record(String thing, String pubkey, String date) {
        if (thing == null) return;
        if (pubkey != null && !pubkey.isEmpty()) signerPubkeyHashes.put(thing, Utils.createSha256HexHash(pubkey));
        if (date != null && !date.isEmpty()) publicationDates.put(thing, date);
    }

    /**
     * Forgets all recorded origins.
     */
    static void clear() {
        signerPubkeyHashes.invalidateAll();
        publicationDates.invalidateAll();
        declaredGoverningSpaces.invalidateAll();
    }

    /**
     * Finds out which of the given things declare a governing Space with
     * {@code gen:governedBy}, by reading the nanopublications they are defined in, several of
     * them at a time and at most for {@link #RESOLUTION_TIMEOUT}. The outcome is cached, so a
     * thing is read once; a nanopublication that could not be read is left unknown and tried
     * again on the next search. Only a thing that is part of a nanopublication, i.e. an IRI
     * under that nanopublication's own, can declare this, so only those are read.
     *
     * @param things the things found
     */
    static void resolveGoverningSpaces(Collection<String> things) {
        List<String> toResolve = things.stream().distinct()
                .filter(thing -> declaredGoverningSpaces.getIfPresent(thing) == null)
                .filter(thing -> !Utils.stripToNanopubId(thing).equals(thing))
                .toList();
        if (toResolve.isEmpty()) return;
        if (toResolve.size() == 1) {
            String thing = toResolve.get(0);
            cacheGoverningSpace(thing, readGoverningSpace(thing));
            return;
        }
        Map<String, Future<String>> pending = new LinkedHashMap<>();
        for (String thing : toResolve) {
            pending.put(thing, NanodashThreadPool.submit(() -> readGoverningSpace(thing)));
        }
        long deadline = System.nanoTime() + RESOLUTION_TIMEOUT.toNanos();
        pending.forEach((thing, resolution) -> cacheGoverningSpace(thing, await(resolution, thing, deadline)));
    }

    private static void cacheGoverningSpace(String thing, String governingSpace) {
        if (governingSpace != null) declaredGoverningSpaces.put(thing, governingSpace);
    }

    private static String await(Future<String> resolution, String thing, long deadline) {
        try {
            return resolution.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException ex) {
            logger.info("Still looking up who governs {} after {}, left out for now", thing, RESOLUTION_TIMEOUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            logger.error("Could not look up who governs {}", thing, ex.getCause());
        }
        return null;
    }

    /**
     * Reads the Space a thing declares as its governing one from the nanopublication it is part
     * of.
     *
     * @param thing the thing
     * @return the Space IRI, {@link #NOT_GOVERNED} if the thing declares none, or null if its
     * nanopublication could not be read
     */
    private static String readGoverningSpace(String thing) {
        Nanopub np = Utils.getNanopub(Utils.stripToNanopubId(thing));
        if (np == null) return null;
        for (Statement st : np.getAssertion()) {
            if (!st.getSubject().stringValue().equals(thing)) continue;
            if (st.getPredicate().equals(KPXL_TERMS.GOVERNED_BY) && st.getObject() instanceof IRI space) {
                return space.stringValue();
            }
        }
        return NOT_GOVERNED;
    }

    /**
     * Returns a note that says who a thing comes from and when it was published, e.g.
     * "by Knowledge Pixels · 2026-10-17T09:12:00Z". The date is left as it is: the
     * dropdown renders it in the short relative form ("3 days ago") used elsewhere, in the
     * viewer's own timezone.
     *
     * @param thing the thing
     * @return the note, or null if neither is known
     */
    public static String getOriginNote(String thing) {
        if (thing == null) return null;
        String origin = getOrigin(thing);
        String date = publicationDates.getIfPresent(thing);
        if (origin == null) return date;
        if (date == null) return origin;
        return origin + Utils.ORIGIN_NOTE_SEPARATOR + date;
    }

    private static String getOrigin(String thing) {
        Space space = getGoverningSpace(thing);
        if (space != null && space.getLabel() != null) return "by " + space.getLabel();
        String signer = getSignerName(thing);
        if (signer != null) return "by " + signer;
        return null;
    }

    /**
     * Returns the Space that governs a thing: the Space of the maintained resource it is, or
     * of the maintained resource whose namespace it lies in, or the one it declares with
     * {@code gen:governedBy}.
     *
     * @param thing the thing
     * @return the governing Space, or null if no Space governs the thing
     */
    static Space getGoverningSpace(String thing) {
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(thing);
        if (resource instanceof MaintainedResource) return resource.getSpace();
        if (resource instanceof Space) return null;
        MaintainedResource containing = MaintainedResourceRepository.get().findByNamespace(MaintainedResource.getNamespace(thing));
        if (containing != null) return containing.getSpace();
        return getDeclaredGoverningSpace(thing);
    }

    /**
     * Returns the Space a governed definition declares with {@code gen:governedBy}. Only a
     * resolution already made is used, so that showing a thing never waits for a
     * nanopublication; {@link #resolveGoverningSpaces(Collection)} makes them.
     *
     * @param thing the thing
     * @return the declared Space, or null if the thing declares none or is not resolved yet
     */
    private static Space getDeclaredGoverningSpace(String thing) {
        String spaceIri = declaredGoverningSpaces.getIfPresent(thing);
        if (spaceIri == null || spaceIri.isEmpty()) return null;
        return AbstractResourceWithProfile.get(spaceIri) instanceof Space space ? space : null;
    }

    private static String getSignerName(String thing) {
        String pubkeyHash = signerPubkeyHashes.getIfPresent(thing);
        if (pubkeyHash == null) return null;
        IRI user = User.findSingleIdForPubkeyhash(pubkeyHash);
        if (user == null) return null;
        return User.getShortDisplayNameForPubkeyhash(user, pubkeyHash);
    }

}
