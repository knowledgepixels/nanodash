package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.IntroNanopub;

import java.util.List;
import java.util.Map;

/**
 * The User class provides utility methods for managing and retrieving user-related data.
 * This class is designed to be used statically and does not allow instantiation.
 */
public class User {

    private User() {
    }  // no instances allowed

    private static transient UserData userData;
    private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
    private static transient long lastRefresh = 0L;

    /**
     * Refreshes the user data by creating a new UserData instance.
     */
    public static void refreshUsers() {
        boolean refreshNeeded = (userData == null);
        synchronized (User.class) {
            if (System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
                lastRefresh = System.currentTimeMillis();
                refreshNeeded = true;
            }
        }
        if (refreshNeeded) {
            userData = new UserData();
        }
    }

    /**
     * Ensures that the user data is loaded. If not, it refreshes the user data.
     */
    public static void ensureLoaded() {
        if (userData == null) refreshUsers();
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
        return getUserData().isApprovedPubkeyhashForUser(pubkeyhash, user);
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
        return getUserData().getShortDisplayNameForPubkeyhash(userIri, pubkeyhash);
    }

    /**
     * Finds a single user ID for a given public key hash.
     *
     * @param pubkeyhash The public key.
     * @return The IRI of the user.
     */
    public static IRI findSingleIdForPubkeyhash(String pubkeyhash) {
        return getUserData().findSingleIdForPubkeyhash(pubkeyhash);
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
        return getUserData().getPubkeyhashes(user, approved);
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
     * Checks if a given IRI represents a software agent.
     *
     * @param userIri The IRI to check.
     * @return True if the IRI represents a software agent, false otherwise.
     */
    public static boolean isSoftware(IRI userIri) {
        return getUserData().isSoftware(userIri);
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
     * Checks if a given string represents a user ID.
     *
     * @param userId The string to check.
     * @return True if the string represents a user ID, false otherwise.
     */
    public static boolean isUser(String userId) {
        return getUserData().isUser(userId);
    }

}
