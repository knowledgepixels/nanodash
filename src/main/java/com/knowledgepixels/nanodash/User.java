package com.knowledgepixels.nanodash;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.setting.IntroNanopub;

public class User {

	private User() {}  // no instances allowed

	private static transient UserData userData;

	public static void refreshUsers() {
		userData = new UserData();
	}

	public synchronized static void ensureLoaded() {
		if (userData == null) refreshUsers();
	}

	public static UserData getUserData() {
		ensureLoaded();
		return userData;
	}

	public static boolean isApprovedKeyForUser(String key, IRI user) {
		return getUserData().isApprovedKeyForUser(key, user);
	}

	public static IRI getUserIri(String pubkey) {
		return getUserData().getUserIri(pubkey);
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

	public static String getShortDisplayName(IRI userIri, String pubkey) {
		return getUserData().getShortDisplayName(userIri, pubkey);
	}

	public static IRI findSingleIdForPubkey(String pubkey) {
		return getUserData().findSingleIdForPubkey(pubkey);
	}

	public static List<IRI> getUsers(boolean approved) {
		return getUserData().getUsers(approved);
	}

	public static List<String> getPubkeys(IRI user, Boolean approved) {
		return getUserData().getPubkeys(user, approved);
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
