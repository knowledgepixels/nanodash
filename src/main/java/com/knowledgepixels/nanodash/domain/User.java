package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.NanodashThreadPool;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.IntroNanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * The User class provides utility methods for managing and retrieving user-related data.
 * This class is designed to be used statically and does not allow instantiation.
 */
public class User {

    private User() {
    }  // no instances allowed

    private static final Logger logger = LoggerFactory.getLogger(User.class);
    private static volatile UserData userData;
    private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
    // How long incomplete data is kept before another load is attempted. Short, because such
    // data is a stand-in for what a service could not tell us and the instance should come
    // right as soon as it can answer; not immediate, so an outage is not hammered.
    private static final long INCOMPLETE_RETRY_INTERVAL = 30 * 1000; // 30 seconds
    private static transient long lastRefresh = 0L;
    private static transient long lastIncompleteRetry = 0L;

    /**
     * Refreshes the user data by creating a new UserData instance.
     */
    public static void refreshUsers() {
        logger.info("Refreshing user data...");
        synchronized (User.class) {
            if (userData == null || System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
                lastRefresh = System.currentTimeMillis();
                install(new UserData(true));
            }
        }
    }

    /**
     * Ensures that the user data is loaded. If not, it loads it from the cached query
     * responses where available — in particular from the persisted snapshot right after a
     * restart (issue #570) — instead of forcing the fetches like the periodic
     * {@link #refreshUsers()} cycle does, which would stall the first request (session
     * creation runs through here) on the network.
     * <p>
     * Data that could not be built in full — a service that could not answer (issue #684) —
     * is kept and used, and another load is attempted in the background.
     */
    public static void ensureLoaded() {
        if (userData == null) {
            synchronized (User.class) {
                if (userData == null) {
                    lastRefresh = System.currentTimeMillis();
                    install(new UserData(false));
                }
            }
        }
        retryIfIncomplete();
    }

    /**
     * Takes on the given user data, unless it would replace what we have with less. A load
     * that a service could not answer for yields {@link UserData#isComplete() incomplete}
     * data (issue #684): that is worth having when there is nothing at all — it is what lets
     * a cold instance serve pages while its query service is unavailable — but never worth
     * overwriting a full load with, as a passing outage would then empty out user data that
     * was perfectly good.
     */
    private static void install(UserData newData) {
        if (!shouldReplace(userData, newData)) {
            logger.warn("Keeping the user data we have: the new load could not be completed");
            return;
        }
        if (!newData.isComplete()) logger.warn("User data is incomplete; will retry");
        userData = newData;
    }

    /**
     * Whether a freshly built set of user data should take the place of what is held now:
     * anything is better than nothing, and complete data is better than incomplete, but
     * incomplete data never replaces complete data. Package-private for testing.
     *
     * @param current the data held now, or null if none
     * @param loaded  the data just built
     * @return true if the new data should be installed
     */
    static boolean shouldReplace(UserData current, UserData loaded) {
        return current == null || !current.isComplete() || loaded.isComplete();
    }

    /**
     * Attempts another load when what we hold is incomplete, at most every
     * {@link #INCOMPLETE_RETRY_INTERVAL}. The attempt runs in the background: the callers of
     * this are request threads serving pages from the partial data, and they must not wait on
     * the very service that is failing to answer.
     */
    private static void retryIfIncomplete() {
        UserData data = userData;
        if (data == null || data.isComplete()) return;
        synchronized (User.class) {
            if (userData == null || userData.isComplete()) return;
            long now = System.currentTimeMillis();
            if (now - lastIncompleteRetry < INCOMPLETE_RETRY_INTERVAL) return;
            lastIncompleteRetry = now;
        }
        NanodashThreadPool.submit(() -> {
            logger.info("Retrying the incomplete user-data load...");
            try {
                UserData reloaded = new UserData(false);
                synchronized (User.class) {
                    lastRefresh = System.currentTimeMillis();
                    install(reloaded);
                }
            } catch (Exception ex) {
                logger.error("Retry of the incomplete user-data load failed", ex);
            }
        });
    }

    /**
     * Retrieves the current user data instance.
     *
     * @return The UserData instance.
     */
    public static UserData getUserData() {
        ensureLoaded();
        return userData;
    }

    /**
     * Checks if a given key is approved for a specific user.
     *
     * @param pubkeyhash The key to check.
     * @param user       The IRI of the user.
     * @return True if the key is approved, false otherwise.
     */
    public static boolean isApprovedPubkeyhashForUser(String pubkeyhash, IRI user) {
        return getUserData().isApprovedPubkeyHashForUser(pubkeyhash, user);
    }

    /**
     * Retrieves the signature owner's IRI from a given nanopublication.
     *
     * @param np The nanopublication.
     * @return The IRI of the signature owner.
     */
    public static IRI getSignatureOwnerIri(Nanopub np) {
        return getUserData().getSignatureOwnerIri(np);
    }

    /**
     * Retrieves the name of a user based on their IRI.
     *
     * @param userIri The IRI of the user.
     * @return The name of the user.
     */
    public static String getName(IRI userIri) {
        return getUserData().getName(userIri);
    }

    /**
     * Retrieves the display name of a user based on their IRI.
     *
     * @param userIri The IRI of the user.
     * @return The display name of the user.
     */
    public static String getDisplayName(IRI userIri) {
        return getUserData().getDisplayName(userIri);
    }

    /**
     * Retrieves the short display name of a user based on their IRI.
     *
     * @param userIri The IRI of the user.
     * @return The short display name of the user.
     */
    public static String getShortDisplayName(IRI userIri) {
        return getUserData().getShortDisplayName(userIri);
    }

    /**
     * Retrieves the short display name of a user based on their IRI and public key.
     *
     * @param userIri    The IRI of the user.
     * @param pubkeyhash The public key of the user.
     * @return The short display name of the user.
     */
    public static String getShortDisplayNameForPubkeyhash(IRI userIri, String pubkeyhash) {
        return getUserData().getShortDisplayNameForPubkeyHash(userIri, pubkeyhash);
    }

    /**
     * Finds a single user ID for a given public key hash.
     *
     * @param pubkeyhash The public key.
     * @return The IRI of the user.
     */
    public static IRI findSingleIdForPubkeyhash(String pubkeyhash) {
        return getUserData().findSingleIdForPubkeyHash(pubkeyhash);
    }

    /**
     * Retrieves a list of users, optionally filtering by approval status.
     *
     * @param approved True to retrieve only approved users, false otherwise.
     * @return A list of user IRIs.
     */
    public static List<IRI> getUsers(boolean approved) {
        return getUserData().getUsers(approved);
    }

    /**
     * Retrieves a list of public keys for a user, optionally filtering by approval status.
     *
     * @param user     The IRI of the user.
     * @param approved True to retrieve only approved keys, false otherwise.
     * @return A list of public keys.
     */
    public static List<String> getPubkeyhashes(IRI user, Boolean approved) {
        return getUserData().getPubkeyHashes(user, approved);
    }

    /**
     * Retrieves a list of introduction nanopublications for a user.
     *
     * @param user The IRI of the user.
     * @return A list of introduction nanopublications.
     */
    public static List<IntroNanopub> getIntroNanopubs(IRI user) {
        return getUserData().getIntroNanopubs(user);
    }

    /**
     * Retrieves a map of introduction nanopublications for a public key.
     *
     * @param pubkey The public key.
     * @return A map of user IRIs to introduction nanopublications.
     */
    public static Map<IRI, IntroNanopub> getIntroNanopubs(String pubkey) {
        return getUserData().getIntroNanopubs(pubkey);
    }

    /**
     * Checks if an introduction nanopublication is approved.
     *
     * @param in The introduction nanopublication.
     * @return True if approved, false otherwise.
     */
    public static boolean isApproved(IntroNanopub in) {
        return getUserData().isApproved(in);
    }

    /**
     * Checks if a given IRI represents a user.
     *
     * @param userIri The IRI to check.
     * @return True if the IRI represents a user, false otherwise.
     */
    public static boolean isUser(IRI userIri) {
        return getUserData().isUser(userIri);
    }

    /**
     * Retrieves the profile picture IRI for a user based on their IRI.
     *
     * @param userIri The IRI of the user.
     * @return The user's profile picture, or null if not set.
     */
    public static ProfilePicture getProfilePicture(IRI userIri) {
        return getUserData().getProfilePicture(userIri);
    }

    /**
     * Retrieves the default license for a user based on their IRI.
     *
     * @param userIri The IRI of the user.
     * @return The IRI of the default license for the user.
     */
    public static IRI getDefaultLicense(IRI userIri) {
        return getUserData().getDefaultLicense(userIri);
    }

}
