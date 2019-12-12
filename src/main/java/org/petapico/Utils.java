package org.petapico;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.security.IntroNanopub;

public class Utils {

	private Utils() {}  // no instances allowed

	public static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

	public static String getShortNameFromURI(IRI uri) {
		String u = uri.stringValue();
		u = u.replaceFirst("[/#]$", "");
		u = u.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
		u = u.replaceFirst("^.*[/#]([^/#]*[^0-9][^/#]*)$", "$1");
		u = u.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
		u = u.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
		return u;
	}

	private static Map<String,IntroNanopub> introNanopubs = new HashMap<>();

	public static IntroNanopub getIntroNanopub(String userId) {
		IntroNanopub introNanopub = introNanopubs.get(userId);
		if (introNanopub == null) {
			try {
				introNanopub = IntroNanopub.get(userId);
				introNanopubs.put(userId, introNanopub);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return introNanopub;
	}

	private static Map<String,Set<String>> pubkeysForUser;

	private static Map<String,Set<String>> usersForPubkey;

	private static void collectUserData() {
		pubkeysForUser = new HashMap<>();
		usersForPubkey = new HashMap<>();
		List<Map<String,String>> userResults;
		try {
			userResults = ApiAccess.getAll("get_all_users", null);
		} catch (IOException ex) {
			ex.printStackTrace();
			// TODO
			return;
		}
		for (Map<String,String> entry : userResults) {
			String userId = entry.get("user");
			String pubkey = entry.get("pubkey");
			Set<String> pubkeys = pubkeysForUser.get(userId);
			if (pubkeys == null) {
				pubkeys = new HashSet<>();
				pubkeysForUser.put(userId, pubkeys);
			}
			pubkeys.add(pubkey);
			Set<String> users = usersForPubkey.get(pubkey);
			if (users == null) {
				users = new HashSet<>();
				usersForPubkey.put(pubkey, users);
			}
			users.add(userId);
		}
	}

	public static Set<String> getPubkeys(String userId) {
		if (pubkeysForUser == null) collectUserData();
		return pubkeysForUser.get(userId);
	}

	public static Set<String> getUsers(String pubkey) {
		if (usersForPubkey == null) collectUserData();
		return usersForPubkey.get(pubkey);
	}
}
