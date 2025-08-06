package com.knowledgepixels.nanodash;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.IntroNanopub;

public class User {

	private User() {}  // no instances allowed

	private static transient UserData userData;

	private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
	private static transient long lastRefresh = 0l;

	public static void refreshUsers() {
		if (System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
			lastRefresh = System.currentTimeMillis();
			userData = new UserData();
		}
	}

	public synchronized static void ensureLoaded() {
		if (userData == null) refreshUsers();
	}

	public static UserData getUserData() {
		ensureLoaded();
		return userData;
	}

	public static boolean isApprovedPubkeyhashForUser(String pubkeyhash, IRI user) {
		return getUserData().isApprovedPubkeyhashForUser(pubkeyhash, user);
	}

	public static IRI getSignatureOwnerIri(Nanopub np) {
		return getUserData().getSignatureOwnerIri(np);
	}

	public static String getName(IRI userIri) {
		return getUserData().getName(userIri);
	}

	public static String getDisplayName(IRI userIri) {
		return getUserData().getDisplayName(userIri);
	}

	public static String getShortDisplayName(IRI userIri) {
		return getUserData().getShortDisplayName(userIri);
	}

	public static String getShortDisplayNameForPubkeyhash(IRI userIri, String pubkeyhash) {
		return getUserData().getShortDisplayNameForPubkeyhash(userIri, pubkeyhash);
	}

	public static IRI findSingleIdForPubkeyhash(String pubkeyhash) {
		return getUserData().findSingleIdForPubkeyhash(pubkeyhash);
	}

	public static List<IRI> getUsers(boolean approved) {
		return getUserData().getUsers(approved);
	}

	public static List<String> getPubkeyhashes(IRI user, Boolean approved) {
		return getUserData().getPubkeyhashes(user, approved);
	}

	public static List<IntroNanopub> getIntroNanopubs(IRI user) {
		return getUserData().getIntroNanopubs( user);
	}

	public static Map<IRI,IntroNanopub> getIntroNanopubs(String pubkey) {
		return getUserData().getIntroNanopubs(pubkey);
	}

	public static boolean isApproved(IntroNanopub in) {
		return getUserData().isApproved(in);
	}

	public static boolean isUser(IRI userIri) {
		return getUserData().isUser(userIri);
	}

	public static boolean isUser(String userId) {
		return getUserData().isUser(userId);
	}

}
