package org.petapico.nanobench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.server.FetchIndex;

import com.opencsv.exceptions.CsvValidationException;

public class UserNew {

	private UserNew() {}  // no instances allowed

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	// TODO Make this configurable:
	private static String authorityIndex = "http://purl.org/np/RAs2tE1BHvwEM2OmUftb1T0JZ6oK2J7Nnr9tGbrE_s4KQ";

	private static Map<IRI,Set<String>> approvedIdPubkeyMap;
	private static Map<String,Set<IRI>> approvedPubkeyIdMap;
	private static Map<IRI,Set<String>> unapprovedIdPubkeyMap;
	private static Map<String,Set<IRI>> unapprovedPubkeyIdMap;
	private static Map<IRI,String> idNameMap;

	public static synchronized void refreshUsers() {
		approvedIdPubkeyMap = new HashMap<>();
		approvedPubkeyIdMap = new HashMap<>();
		unapprovedIdPubkeyMap = new HashMap<>();
		unapprovedPubkeyIdMap = new HashMap<>();
		idNameMap = new HashMap<>();

		// TODO Make update strategy configurable:
		String latestAuthorityIndex = ApiAccess.getLatestVersionId(authorityIndex);
		System.err.println("Using authority index: " + latestAuthorityIndex);

		// Get users that are listed directly in the authority index, and consider them approved:
		ByteArrayOutputStream out = new ByteArrayOutputStream(); // TODO use piped out-in stream here
		new FetchIndex(latestAuthorityIndex, out, RDFFormat.TRIG, false, true, null).run();
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		try {
			MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new MultiNanopubRdfHandler.NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					registerUser(np, true);
				}
			});
		} catch (RDFParseException | RDFHandlerException | IOException | MalformedNanopubException ex) {
			ex.printStackTrace();
		}

		// Get users that are approved by somebody who is already approved, and consider them approved too:
		try {
			Map<String,String> params = new HashMap<>();
			params.put("pred", "http://purl.org/nanopub/x/approves-of");
			List<ApiResponseEntry> results = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params).getData();
			while (true) {
				boolean keepLooping = false;
				for (ApiResponseEntry entry : new ArrayList<>(results)) {
					if (!entry.get("superseded").equals("0") || !entry.get("retracted").equals("0")) continue;
					if (hasValue(approvedPubkeyIdMap, entry.get("pubkey"), vf.createIRI(entry.get("subj")))) {
						registerUser(Utils.getNanopub(entry.get("obj")), true);
						results.remove(entry);
						keepLooping = true;
					}
				}
				if (!keepLooping) break;
			}
		} catch (IOException|CsvValidationException ex) {
			ex.printStackTrace();
		}

		// Get other users, considered unapproved:
		try {
			for (ApiResponseEntry entry : ApiAccess.getAll("get_all_users", null).getData()) {
				registerUser(Utils.getNanopub(entry.get("intronp")), false);
			}
		} catch (IOException|CsvValidationException ex) {
			ex.printStackTrace();
		}
	}

	private static void registerUser(Nanopub np, boolean approved) {
		if (np == null) {
			//System.err.println("Nanopublication not received");
			return;
		}
		IntroNanopub introNp = new IntroNanopub(np);
		if (introNp.getUser() == null) {
			//System.err.println("No identifier found in introduction");
			return;
		} else if (introNp.getKeyDeclarations().isEmpty()) {
			//System.err.println("No key declarations found in introduction");
			return;
		}
		String userId = introNp.getUser().stringValue();
		IRI userIri = vf.createIRI(userId);
		if (userId.startsWith("https://orcid.org/")) {
			// Some simple ORCID ID wellformedness check:
			if (!userId.matches("https://orcid.org/[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]")) return;
		}
		for (KeyDeclaration kd : introNp.getKeyDeclarations()) {
			String pubkey = kd.getPublicKeyString();
			if (approved) {
				addValue(approvedIdPubkeyMap, userIri, pubkey);
				addValue(approvedPubkeyIdMap, pubkey, userIri);
			} else if (!hasValue(approvedIdPubkeyMap, userIri, pubkey)) {
				addValue(unapprovedIdPubkeyMap, userIri, pubkey);
				addValue(unapprovedPubkeyIdMap, pubkey, userIri);
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

	private static String getShortId(IRI userIri) {
		return userIri.stringValue().replaceFirst("^https://orcid.org/", "");
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
			return getDisplayName(iri1).compareTo(getDisplayName(iri2));
		}

	};

	public static synchronized List<IRI> getUsers(boolean approved) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		List<IRI> list;
		if (approved) {
			list = new ArrayList<IRI>(approvedIdPubkeyMap.keySet());
		} else {
			list = new ArrayList<IRI>(unapprovedIdPubkeyMap.keySet());
		}
		// TODO Cache the sorted list to not sort from scratch each time:
		list.sort(comparator);
		return list;
	}

	public static synchronized Set<String> getPubkeys(IRI user, boolean approved) {
		if (approvedPubkeyIdMap == null) refreshUsers();
		Set<String> pubkeys = null;
		if (user != null) {
			if (approved) {
				pubkeys = approvedIdPubkeyMap.get(user);
			} else {
				pubkeys = unapprovedIdPubkeyMap.get(user);
			}
		}
		if (pubkeys == null) pubkeys = new HashSet<>();
		return pubkeys;
	}

}
