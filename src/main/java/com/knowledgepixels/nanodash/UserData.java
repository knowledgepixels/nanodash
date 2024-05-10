package com.knowledgepixels.nanodash;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.FetchIndex;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.setting.IntroNanopub;
import org.nanopub.extra.setting.NanopubSetting;

import com.opencsv.exceptions.CsvValidationException;

import net.trustyuri.TrustyUriUtils;

public class UserData implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<IRI,Set<String>> approvedIdPubkeyMap = new HashMap<>();
	private Map<String,Set<IRI>> approvedPubkeyIdMap = new HashMap<>();
	private Map<String,Set<IRI>> approvedPubkeyLocationMap = new HashMap<>();
	private Map<IRI,Set<String>> unapprovedIdPubkeyMap = new HashMap<>();
	private Map<String,Set<IRI>> unapprovedPubkeyIdMap = new HashMap<>();
	private Map<String,Set<IRI>> unapprovedPubkeyLocationMap = new HashMap<>();
	private Map<String,Set<IRI>> pubkeyIntroMap = new HashMap<>();
	private Map<IRI,IntroNanopub> introMap = new HashMap<>();
	private Map<IRI,IntroNanopub> approvedIntroMap = new HashMap<>();
	private Map<IRI,String> idNameMap = new HashMap<>();
	private Map<IRI,List<IntroNanopub>> introNanopubLists = new HashMap<>();

	UserData() {
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

	private IntroNanopub toIntroNanopub(String i) {
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

	private void register(String npId, boolean approved) {
		if (!TrustyUriUtils.isPotentialTrustyUri(npId)) return;
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
			IRI keyLocation = kd.getKeyLocation();
			if (approved) {
				addValue(approvedIdPubkeyMap, userIri, pubkey);
				addValue(approvedPubkeyIdMap, pubkey, userIri);
				if (keyLocation != null) addValue(approvedPubkeyLocationMap, pubkey, keyLocation);
			} else {
				if (!hasValue(approvedIdPubkeyMap, userIri, pubkey)) {
					addValue(unapprovedIdPubkeyMap, userIri, pubkey);
					addValue(unapprovedPubkeyIdMap, pubkey, userIri);
					if (keyLocation != null) addValue(unapprovedPubkeyLocationMap, pubkey, keyLocation);
				}
				addValue(pubkeyIntroMap, pubkey, introNp.getNanopub().getUri());
			}
		}
		if (!idNameMap.containsKey(userIri)) {
			idNameMap.put(userIri, introNp.getName());
		}
	}

	private void addValue(Map<IRI,Set<String>> map, IRI key, String value) {
		Set<String> values = map.get(key);
		if (values == null) {
			values = new HashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	private void addValue(Map<String,Set<IRI>> map, String key, IRI value) {
		Set<IRI> values = map.get(key);
		if (values == null) {
			values = new HashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	private boolean hasValue(Map<IRI,Set<String>> map, IRI key, String value) {
		Set<String> values = map.get(key);
		if (values == null) return false;
		return values.contains(value);
	}

	private static boolean hasValue(Map<String,Set<IRI>> map, String key, IRI value) {
		Set<IRI> values = map.get(key);
		if (values == null) return false;
		return values.contains(value);
	}

	public boolean isApprovedKeyForUser(String key, IRI user) {
		return hasValue(approvedIdPubkeyMap, user, key);
	}

	private String getShortId(IRI userIri) {
		if (userIri == null) return "(unknown)";
		return userIri.stringValue().replaceFirst("^https://orcid.org/", "");
	}

	public IRI getUserIri(String pubkey, boolean approvedOnly) {
		Set<IRI> userIris = approvedPubkeyIdMap.get(pubkey);
		if (userIris != null && userIris.size() == 1) return userIris.iterator().next();
		if (!approvedOnly) {
			userIris = unapprovedPubkeyIdMap.get(pubkey);
			if (userIris != null && userIris.size() == 1) return userIris.iterator().next();
		}
		return null;
	}

	public IRI getUserIri(String pubkey) {
		return getUserIri(pubkey, true);
	}

	public IRI getSignatureOwnerIri(Nanopub np) {
		try {
			if (np != null) {
				NanopubSignatureElement se = SignatureUtils.getSignatureElement(np);
				if (se != null) {
					return getUserIri(se.getPublicKeyString());
				}
			}
		} catch (MalformedCryptoElementException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public String getName(IRI userIri) {
		return idNameMap.get(userIri);
	}

	public String getDisplayName(IRI userIri) {
		String name = getName(userIri);
		if (name != null && !name.isEmpty()) {
			return name + " (" + getShortId(userIri) + ")";
		}
		return getShortId(userIri);
	}

	public String getShortDisplayName(IRI userIri) {
		String name = getName(userIri);
		if (name != null && !name.isEmpty()) {
			return name;
		}
		return getShortId(userIri);
	}

	public String getShortDisplayName(IRI userIri, String pubkey) {
		Set<IRI> ids = approvedPubkeyIdMap.get(pubkey);
		if (ids == null || ids.isEmpty()) {
			ids = unapprovedPubkeyIdMap.get(pubkey);
			if (ids == null || ids.isEmpty()) {
				return getShortId(userIri);
			} else if (ids.size() == 1) {
				return getShortDisplayName(ids.iterator().next());
			} else {
				return getShortId(userIri) + " (contested identity)";
			}
		} else if (ids.size() == 1) {
			return getShortDisplayName(ids.iterator().next());
		} else {
			return "(contested identity)";
		}
	}

	public IRI findSingleIdForPubkey(String pubkey) {
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

	private transient Comparator<IRI> comparator = new Comparator<IRI>() {

		@Override
		public int compare(IRI iri1, IRI iri2) {
			return getDisplayName(iri1).toLowerCase().compareTo(getDisplayName(iri2).toLowerCase());
		}

	};

	public synchronized List<IRI> getUsers(boolean approved) {
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

	public synchronized List<String> getPubkeys(IRI user, Boolean approved) {
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

	public List<IntroNanopub> getIntroNanopubs(IRI user) {
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

	public Map<IRI,IntroNanopub> getIntroNanopubs(String pubkey) {
		Map<IRI,IntroNanopub> introNps = new HashMap<>();
		getIntroNanopubs(pubkey, introNps);
		return introNps;
	}

	private void getIntroNanopubs(String pubkey, Map<IRI,IntroNanopub> introNps) {
		if (pubkeyIntroMap.containsKey(pubkey)) {
			for (IRI iri : pubkeyIntroMap.get(pubkey)) {
				introNps.put(iri, introMap.get(iri));
			}
		}
	}

	public boolean isApproved(IntroNanopub in) {
		return approvedIntroMap.containsKey(in.getNanopub().getUri());
	}

	public IRI getKeyLocation(String pubkey) {
		if (approvedPubkeyLocationMap.containsKey(pubkey) && !approvedPubkeyLocationMap.get(pubkey).isEmpty()) {
			if (approvedPubkeyLocationMap.get(pubkey).size() == 1) return approvedPubkeyLocationMap.get(pubkey).iterator().next();
			return null;
		}
		if (unapprovedPubkeyLocationMap.containsKey(pubkey) && unapprovedPubkeyLocationMap.get(pubkey).size() == 1) {
			return unapprovedPubkeyLocationMap.get(pubkey).iterator().next();
		}
		return null;
	}

}
