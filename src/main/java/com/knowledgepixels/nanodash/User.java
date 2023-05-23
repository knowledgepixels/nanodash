package com.knowledgepixels.nanodash;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.server.FetchIndex;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.setting.IntroNanopub;
import org.nanopub.extra.setting.NanopubSetting;

import com.opencsv.exceptions.CsvValidationException;

public class User {

	private User() {}  // no instances allowed

	private static Map<IRI,Set<String>> approvedIdPubkeyMap;
	private static Map<String,Set<IRI>> approvedPubkeyIdMap;
	private static Map<IRI,Set<String>> unapprovedIdPubkeyMap;
	private static Map<String,Set<IRI>> unapprovedPubkeyIdMap;
	private static Map<String,Set<IRI>> pubkeyIntroMap;
	private static Map<IRI,IntroNanopub> introMap;
	private static Map<IRI,IntroNanopub> approvedIntroMap;
	private static Map<IRI,String> idNameMap;
	private static Map<IRI,List<IntroNanopub>> introNanopubLists;

	public static synchronized void refreshUsers() {
		approvedIdPubkeyMap = new HashMap<>();
		approvedPubkeyIdMap = new HashMap<>();
		unapprovedIdPubkeyMap = new HashMap<>();
		unapprovedPubkeyIdMap = new HashMap<>();
		pubkeyIntroMap = new HashMap<>();
		introMap = new HashMap<>();
		approvedIntroMap = new HashMap<>();
		idNameMap = new HashMap<>();
		introNanopubLists = new HashMap<>();

		final NanodashPreferences pref = NanodashPreferences.get();

		// TODO Make nanopublication setting configurable:
		NanopubSetting setting;
		if (pref.getSettingUri() != null) {
			setting = new NanopubSetting(GetNanopub.get(pref.getSettingUri()));
		} else {
			try {
				setting = NanopubSetting.getLocalSetting();
			} catch (RDF4JException | MalformedNanopubException | IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		String settingId = setting.getNanopub().getUri().stringValue();
		if (setting.getUpdateStrategy().stringValue().equals("http://purl.org/nanopub/x/UpdatesByCreator")) {
			settingId = ApiAccess.getLatestVersionId(settingId);
			setting = new NanopubSetting(GetNanopub.get(settingId));
		}
		System.err.println("Using nanopublication setting: " + settingId);

		// Get users that are listed directly in the authority index, and consider them approved:
		ByteArrayOutputStream out = new ByteArrayOutputStream(); // TODO use piped out-in stream here
		new FetchIndex(setting.getAgentIntroCollection().stringValue(), out, RDFFormat.TRIG, false, true, null).run();
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		try {
			MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new MultiNanopubRdfHandler.NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					// TODO: Check that latest version talks about same user
					register(ApiAccess.getLatestVersionId(np.getUri().stringValue()), true);
				}
			});
		} catch (RDFParseException | RDFHandlerException | IOException | MalformedNanopubException ex) {
			ex.printStackTrace();
		}

		if (setting.getTrustRangeAlgorithm().stringValue().equals("http://purl.org/nanopub/x/TransitiveTrust")) {
			// Get users that are approved by somebody who is already approved, and consider them approved too:
			try {
				Map<String,String> params = new HashMap<>();
				params.put("pred", "http://purl.org/nanopub/x/approvesOf");
				List<ApiResponseEntry> results = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params).getData();
				while (true) {
					boolean keepLooping = false;
					for (ApiResponseEntry entry : new ArrayList<>(results)) {
						if (!entry.get("superseded").equals("0") || !entry.get("retracted").equals("0")) continue;
						if (hasValue(approvedPubkeyIdMap, entry.get("pubkey"), Utils.vf.createIRI(entry.get("subj")))) {
							register(entry.get("obj"), true);
							results.remove(entry);
							keepLooping = true;
						}
					}
					if (!keepLooping) break;
				}
			} catch (IOException|CsvValidationException ex) {
				ex.printStackTrace();
			}
		}

		// Get latest introductions for all users, including unapproved ones:
		try {
			for (ApiResponseEntry entry : ApiAccess.getAll("get_all_users", null).getData()) {
				register(entry.get("intronp"), false);
			}
		} catch (IOException|CsvValidationException ex) {
			ex.printStackTrace();
		}
	}

	private static IntroNanopub toIntroNanopub(String i) {
		if (i == null) return null;
		IRI iri = Utils.vf.createIRI(i);
		if (introMap.containsKey(iri)) return introMap.get(iri);
		Nanopub np = Utils.getNanopub(i);
		if (np == null) return null;
		if (introMap.containsKey(np.getUri())) return introMap.get(np.getUri());
		IntroNanopub introNp = new IntroNanopub(np);
		introMap.put(np.getUri(), introNp);
		return introNp;
	}

	private static void register(String npId, boolean approved) {
		IntroNanopub introNp = toIntroNanopub(npId);
		if (introNp == null) {
			//System.err.println("No latest version of introduction found");
			return;
		}
		if (introNp.getUser() == null) {
			//System.err.println("No identifier found in introduction");
			return;
		}
		if (introNp.getKeyDeclarations().isEmpty()) {
			//System.err.println("No key declarations found in introduction");
			return;
		}
		if (approved) {
			approvedIntroMap.put(introNp.getNanopub().getUri(), introNp);
		}
		String userId = introNp.getUser().stringValue();
		IRI userIri = Utils.vf.createIRI(userId);
		if (userId.startsWith("https://orcid.org/")) {
			// Some simple ORCID ID wellformedness check:
			if (!userId.matches("https://orcid.org/[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]")) return;
		}
		for (KeyDeclaration kd : introNp.getKeyDeclarations()) {
			String pubkey = kd.getPublicKeyString();
			if (approved) {
				addValue(approvedIdPubkeyMap, userIri, pubkey);
				addValue(approvedPubkeyIdMap, pubkey, userIri);
			} else {
				if (!hasValue(approvedIdPubkeyMap, userIri, pubkey)) {
					addValue(unapprovedIdPubkeyMap, userIri, pubkey);
					addValue(unapprovedPubkeyIdMap, pubkey, userIri);
				}
				addValue(pubkeyIntroMap, pubkey, introNp.getNanopub().getUri());
			}
		}
		if (!idNameMap.containsKey(userIri)) {
			idNameMap.put(userIri, introNp.getName());
		}
	}

	private static void addValue(Map<IRI,Set<String>> map, IRI key, String value) {
		Set<String> values = map.get(key);
		if (values == null) {
			values = new HashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	private static void addValue(Map<String,Set<IRI>> map, String key, IRI value) {
		Set<IRI> values = map.get(key);
		if (values == null) {
			values = new HashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	private static boolean hasValue(Map<IRI,Set<String>> map, IRI key, String value) {
		Set<String> values = map.get(key);
		if (values == null) return false;
		return values.contains(value);
	}

	private static boolean hasValue(Map<String,Set<IRI>> map, String key, IRI value) {
		Set<IRI> values = map.get(key);
		if (values == null) return false;
		return values.contains(value);
	}

	public static boolean isApprovedKeyForUser(String key, IRI user) {
		return hasValue(approvedIdPubkeyMap, user, key);
	}

	private static String getShortId(IRI userIri) {
		return userIri.stringValue().replaceFirst("^https://orcid.org/", "");
	}

	public static IRI getUserIri(String pubkey) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		Set<IRI> userIris = approvedPubkeyIdMap.get(pubkey);
		if (userIris != null && userIris.size() == 1) return userIris.iterator().next();
		return null;
	}

	public static String getName(IRI userIri) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		return idNameMap.get(userIri);
	}

	public static String getDisplayName(IRI userIri) {
		String name = getName(userIri);
		if (name != null && !name.isEmpty()) {
			return name + " (" + getShortId(userIri) + ")";
		}
		return getShortId(userIri);
	}

	public static String getShortDisplayName(IRI userIri) {
		String name = getName(userIri);
		if (name != null && !name.isEmpty()) {
			return name;
		}
		return getShortId(userIri);
	}

	public static String getShortDisplayNameForPubkey(String pubkey) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		Set<IRI> ids = approvedPubkeyIdMap.get(pubkey);
		if (ids == null || ids.isEmpty()) {
			ids = unapprovedPubkeyIdMap.get(pubkey);
			if (ids == null || ids.isEmpty()) {
				return "(unknown)";
			} else if (ids.size() == 1) {
				return getShortDisplayName(ids.iterator().next());
			} else {
				return "(contested identity)";
			}
		} else if (ids.size() == 1) {
			return getShortDisplayName(ids.iterator().next());
		} else {
			return "(contested identity)";
		}
	}

	public static IRI findSingleIdForPubkey(String pubkey) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		if (approvedPubkeyIdMap.containsKey(pubkey) && !approvedPubkeyIdMap.get(pubkey).isEmpty()) {
			if (approvedPubkeyIdMap.get(pubkey).size() == 1) {
				return approvedPubkeyIdMap.get(pubkey).iterator().next();
			} else {
				return null;
			}
		}
		if (unapprovedPubkeyIdMap.containsKey(pubkey) && !unapprovedPubkeyIdMap.get(pubkey).isEmpty()) {
			if (unapprovedPubkeyIdMap.get(pubkey).size() == 1) {
				return unapprovedPubkeyIdMap.get(pubkey).iterator().next();
			} else {
				return null;
			}
		}
		return null;
	}

	private static Comparator<IRI> comparator = new Comparator<IRI>() {

		@Override
		public int compare(IRI iri1, IRI iri2) {
			return getDisplayName(iri1).toLowerCase().compareTo(getDisplayName(iri2).toLowerCase());
		}

	};

	public static synchronized List<IRI> getUsers(boolean approved) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		List<IRI> list;
		if (approved) {
			list = new ArrayList<IRI>(approvedIdPubkeyMap.keySet());
		} else {
			list = new ArrayList<IRI>();
			for (IRI u : unapprovedIdPubkeyMap.keySet()) {
				if (!approvedIdPubkeyMap.containsKey(u)) list.add(u);
			}
		}
		// TODO Cache the sorted list to not sort from scratch each time:
		list.sort(comparator);
		return list;
	}

	public static synchronized List<String> getPubkeys(IRI user, Boolean approved) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		List<String> pubkeys = new ArrayList<>();
		if (user != null) {
			if (approved == null || approved) {
				if (approvedIdPubkeyMap.containsKey(user)) pubkeys.addAll(approvedIdPubkeyMap.get(user));
			}
			if (approved == null || !approved) {
				if (unapprovedIdPubkeyMap.containsKey(user)) pubkeys.addAll(unapprovedIdPubkeyMap.get(user));
			}
		}
		return pubkeys;
	}

	public static List<IntroNanopub> getIntroNanopubs(IRI user) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		if (introNanopubLists.containsKey(user)) return introNanopubLists.get(user);

		Map<IRI,IntroNanopub> introNps = new HashMap<>();
		if (approvedIdPubkeyMap.containsKey(user)) {
			for (String pk : approvedIdPubkeyMap.get(user)) {
				getIntroNanopubs(pk, introNps);
			}
		}
		if (unapprovedIdPubkeyMap.containsKey(user)) {
			for (String pk : unapprovedIdPubkeyMap.get(user)) {
				getIntroNanopubs(pk, introNps);
			}
		}
		List<IntroNanopub> list = new ArrayList<>(introNps.values());
		Collections.sort(list, new Comparator<IntroNanopub>() {
			@Override
			public int compare(IntroNanopub i0, IntroNanopub i1) {
				Calendar c0 = SimpleTimestampPattern.getCreationTime(i0.getNanopub());
				Calendar c1 = SimpleTimestampPattern.getCreationTime(i1.getNanopub());
				if (c0 == null && c1 == null) return 0;
				if (c0 == null) return 1;
				if (c1 == null) return -1;
				return -c0.compareTo(c1);
			}
		});
		introNanopubLists.put(user, list);
		return list;
	}

	public static Map<IRI,IntroNanopub> getIntroNanopubs(String pubkey) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		Map<IRI,IntroNanopub> introNps = new HashMap<>();
		getIntroNanopubs(pubkey, introNps);
		return introNps;
	}

	private static void getIntroNanopubs(String pubkey, Map<IRI,IntroNanopub> introNps) {
		if (pubkeyIntroMap.containsKey(pubkey)) {
			for (IRI iri : pubkeyIntroMap.get(pubkey)) {
				introNps.put(iri, introMap.get(iri));
			}
		}
	}

	public static boolean isApproved(IntroNanopub in) {
		return approvedIntroMap.containsKey(in.getNanopub().getUri());
	}

}
