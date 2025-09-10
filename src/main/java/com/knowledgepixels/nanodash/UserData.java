package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.setting.IntroNanopub;
import org.nanopub.extra.setting.NanopubSetting;
import org.nanopub.vocabulary.NPX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * UserData class manages user-related data.
 */
public class UserData implements Serializable {

    private static final long serialVersionUID = 1L;

    private static ValueFactory vf = SimpleValueFactory.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(UserData.class);

    private HashMap<IRI, Set<String>> approvedIdPubkeyhashMap = new HashMap<>();
    private HashMap<String, Set<IRI>> approvedPubkeyhashIdMap = new HashMap<>();
    private HashMap<String, Set<IRI>> approvedPubkeyhashLocationMap = new HashMap<>();
    private HashMap<IRI, Set<String>> unapprovedIdPubkeyhashMap = new HashMap<>();
    private HashMap<String, Set<IRI>> unapprovedPubkeyhashIdMap = new HashMap<>();
    private HashMap<String, Set<IRI>> unapprovedPubkeyhashLocationMap = new HashMap<>();
    private HashMap<String, Set<IRI>> pubkeyhashIntroMap = new HashMap<>();
    private HashMap<IRI, IntroNanopub> introMap = new HashMap<>();
    private Set<IRI> approvedIntros = new HashSet<>();
    private HashMap<IRI, String> idNameMap = new HashMap<>();
    private HashMap<IRI, List<IntroNanopub>> introNanopubLists = new HashMap<>();

    /**
     * Default constructor for UserData.
     * Initializes the user data by fetching nanopublications settings.
     */
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
        if (setting.getUpdateStrategy().equals(NPX.UPDATES_BY_CREATOR)) {
            settingId = QueryApiAccess.getLatestVersionId(settingId);
            setting = new NanopubSetting(GetNanopub.get(settingId));
        }
        logger.info("Using nanopublication setting: {}", settingId);

//		// Get users that are listed directly in the authority index, and consider them approved:
//		ByteArrayOutputStream out = new ByteArrayOutputStream(); // TODO use piped out-in stream here
//		new FetchIndex(setting.getAgentIntroCollection().stringValue(), out, RDFFormat.TRIG, false, true, null).run();
//		InputStream in = new ByteArrayInputStream(out.toByteArray());
//		try {
//			MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new MultiNanopubRdfHandler.NanopubHandler() {
//				@Override
//				public void handleNanopub(Nanopub np) {
//					// TODO: Check that latest version talks about same user
//					register(QueryApiAccess.getLatestVersionId(np.getUri().stringValue()), true);
//				}
//			});
//		} catch (RDFParseException | RDFHandlerException | IOException | MalformedNanopubException ex) {
//			logger.error();
//		}
///
//		if (setting.getTrustRangeAlgorithm().equals(NPX.TRANSITIVE_TRUST)) {
//			ApiResponse resp = QueryApiAccess.forcedGet("get-approved-nanopubs");
//			List<ApiResponseEntry> results = new ArrayList<>(resp.getData());
//			while (true) {
//				boolean keepLooping = false;
//				for (ApiResponseEntry entry : new ArrayList<>(results)) {
//					if (hasValue(approvedPubkeyIdMap, entry.get("pubkey"), Utils.vf.createIRI(entry.get("approver")))) {
//						register(entry.get("approved_np"), true);
//						results.remove(entry);
//						keepLooping = true;
//					}
//				}
//				if (!keepLooping) break;
//			}
//		}

        logger.info("Loading approved users...");
        try {
            for (RegistryAccountInfo rai : RegistryAccountInfo.fromUrl(Utils.getMainRegistryUrl() + "list.json")) {
                registerApproved(rai);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        logger.info("Loading user details...");
        // Get latest introductions for all users, including unapproved ones:
        for (ApiResponseEntry entry : QueryApiAccess.forcedGet("get-all-user-intros").getData()) {
            register(entry);
        }
    }

    private IntroNanopub toIntroNanopub(IRI iri) {
        if (iri == null) return null;
        if (introMap.containsKey(iri)) return introMap.get(iri);
        Nanopub np = Utils.getNanopub(iri.stringValue());
        if (np == null) return null;
        IntroNanopub introNp = new IntroNanopub(np);
        introMap.put(np.getUri(), introNp);
        return introNp;
    }

    private void registerApproved(RegistryAccountInfo rai) {
        if (rai.getAgent().equals("$")) return;
        addValue(approvedIdPubkeyhashMap, rai.getAgentIri(), rai.getPubkey());
        addValue(approvedPubkeyhashIdMap, rai.getPubkey(), rai.getAgentIri());
    }

    private void register(ApiResponseEntry entry) {
        IRI userIri;
        try {
            userIri = vf.createIRI(entry.get("user"));
        } catch (IllegalArgumentException ex) {
            logger.error("Error creating IRI from user string: {}", entry.get("user"), ex);
            return;
        }
        String pubkeyhash = entry.get("pubkeyHash");
        boolean approved = approvedIdPubkeyhashMap.containsKey(userIri) && approvedIdPubkeyhashMap.get(userIri).contains(pubkeyhash);
        boolean authoritative = "true".equals(entry.get("authoritative"));
        IRI introNpIri = null;
        try {
            introNpIri = vf.createIRI(entry.get("intronp"));
        } catch (IllegalArgumentException ex) {
            logger.error("Error creating IRI from intronp string: {}", entry.get("intronp"), ex);
        }
        IRI keyLocation = null;
        try {
            if (!entry.get("keyLocation").isEmpty()) {
                keyLocation = vf.createIRI(entry.get("keyLocation"));
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Error creating IRI from keyLocation string: {}", entry.get("keyLocation"), ex);
        }
        if (approved) {
            if (authoritative) {
                if (introNpIri != null) approvedIntros.add(introNpIri);
                if (keyLocation != null) addValue(approvedPubkeyhashLocationMap, pubkeyhash, keyLocation);
            }
        } else {
            addValue(unapprovedIdPubkeyhashMap, userIri, entry.get("pubkeyHash"));
            addValue(unapprovedPubkeyhashIdMap, entry.get("pubkeyHash"), userIri);
            if (keyLocation != null) addValue(unapprovedPubkeyhashLocationMap, pubkeyhash, keyLocation);
        }
        if (introNpIri != null) {
            addValue(pubkeyhashIntroMap, entry.get("pubkeyHash"), introNpIri);
        }
        String name = entry.get("name");
        if (!name.isEmpty() && !idNameMap.containsKey(userIri)) {
            idNameMap.put(userIri, name);
        }
    }

/*
    private void register(String npId, boolean approved) {
        if (!TrustyUriUtils.isPotentialTrustyUri(npId)) return;
        IntroNanopub introNp = toIntroNanopub(npId);
        if (introNp == null) {
            //logger.error("No latest version of introduction found");
            return;
        }
        if (introNp.getUser() == null) {
            //logger.error("No identifier found in introduction");
            return;
        }
        if (introNp.getKeyDeclarations().isEmpty()) {
            //logger.error("No key declarations found in introduction");
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
    */

    private void addValue(Map<IRI, Set<String>> map, IRI key, String value) {
        Set<String> values = map.get(key);
        if (values == null) {
            values = new HashSet<>();
            map.put(key, values);
        }
        values.add(value);
    }

    private void addValue(Map<String, Set<IRI>> map, String key, IRI value) {
        Set<IRI> values = map.get(key);
        if (values == null) {
            values = new HashSet<>();
            map.put(key, values);
        }
        values.add(value);
    }

    private boolean hasValue(Map<IRI, Set<String>> map, IRI key, String value) {
        Set<String> values = map.get(key);
        if (values == null) return false;
        return values.contains(value);
    }

//	private static boolean hasValue(Map<String,Set<IRI>> map, String key, IRI value) {
//		Set<IRI> values = map.get(key);
//		if (values == null) return false;
//		return values.contains(value);
//	}

    /**
     * Checks if the given IRI is a user identifier.
     *
     * @param userIri the IRI to check
     * @return true if the IRI is a user identifier, false otherwise
     */
    public boolean isUser(IRI userIri) {
        return approvedIdPubkeyhashMap.containsKey(userIri) || unapprovedIdPubkeyhashMap.containsKey(userIri);
    }

    /**
     * Checks if the given userId is a valid user identifier.
     *
     * @param userId the user identifier to check, must start with "https://" or "http://"
     * @return true if the userId is a valid user identifier, false otherwise
     */
    public boolean isUser(String userId) {
        if (!userId.startsWith("https://") && !userId.startsWith("http://")) return false;
        try {
            IRI userIri = Utils.vf.createIRI(userId);
            return approvedIdPubkeyhashMap.containsKey(userIri) || unapprovedIdPubkeyhashMap.containsKey(userIri);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Checks if the given public key is approved for the specified user.
     *
     * @param pubkeyhash the public key to check
     * @param user       the IRI of the user to check against
     * @return true if the key is approved for the user, false otherwise
     */
    public boolean isApprovedPubkeyhashForUser(String pubkeyhash, IRI user) {
        return hasValue(approvedIdPubkeyhashMap, user, pubkeyhash);
    }

    private String getShortName(IRI userIri) {
        if (userIri == null) return "(unknown)";
        String n = userIri.stringValue();
        n = n.replaceFirst("^https://orcid.org/", "");
        if (n.length() > 40) return n.substring(0, 30) + "...";
        return n;
    }

    /**
     * Retrieves the IRI of a user based on their public key.
     *
     * @param pubkeyHash   the public key of the user
     * @param approvedOnly if true, only approved users are considered; if false, unapproved users are also considered
     * @return the IRI of the user if found, or null if not found
     */
    public IRI getUserIriForPubkeyhash(String pubkeyHash, boolean approvedOnly) {
        Set<IRI> userIris = approvedPubkeyhashIdMap.get(pubkeyHash);
        if (userIris != null && userIris.size() == 1) return userIris.iterator().next();
        if (!approvedOnly) {
            userIris = unapprovedPubkeyhashIdMap.get(pubkeyHash);
            if (userIris != null && userIris.size() == 1) return userIris.iterator().next();
        }
        return null;
    }

    public IRI getSignatureOwnerIri(Nanopub np) {
        try {
            if (np != null) {
                NanopubSignatureElement se = SignatureUtils.getSignatureElement(np);
                if (se != null) {
                    String pubkeyhash = Utils.createSha256HexHash(se.getPublicKeyString());
                    return getUserIriForPubkeyhash(pubkeyhash, true);
                }
            }
        } catch (MalformedCryptoElementException ex) {
            logger.error("Error getting signature element", ex);
        }
        return null;
    }

    /**
     * Retrieves the name of a user based on their IRI.
     *
     * @param userIri the IRI of the user
     * @return the name of the user if found, or null if not found
     */
    public String getName(IRI userIri) {
        return idNameMap.get(userIri);
    }

    /**
     * Retrieves the display name of a user based on their IRI.
     *
     * @param userIri the IRI of the user
     * @return the display name of the user, which includes their name and short name
     */
    public String getDisplayName(IRI userIri) {
        String name = getName(userIri);
        if (name != null && !name.isEmpty()) {
            return name + " (" + getShortName(userIri) + ")";
        }
        return getShortName(userIri);
    }

    /**
     * Retrieves a short display name for a user based on their IRI.
     *
     * @param userIri the IRI of the user
     * @return the short display name of the user, which is either their name or their short name
     */
    public String getShortDisplayName(IRI userIri) {
        String name = getName(userIri);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return getShortName(userIri);
    }

    /**
     * Retrieves a short display name for a user based on their IRI and public key.
     *
     * @param userIri    the IRI of the user
     * @param pubkeyhash the public key of the user
     * @return the short display name of the user, which may include a contested identity note if multiple identities are associated with the public key
     */
    public String getShortDisplayNameForPubkeyhash(IRI userIri, String pubkeyhash) {
        Set<IRI> ids = approvedPubkeyhashIdMap.get(pubkeyhash);
        if (ids == null || ids.isEmpty()) {
            ids = unapprovedPubkeyhashIdMap.get(pubkeyhash);
            if (ids == null || ids.isEmpty()) {
                return getShortName(userIri);
            } else if (ids.size() == 1) {
                return getShortDisplayName(ids.iterator().next());
            } else {
                return getShortName(userIri) + " (contested identity)";
            }
        } else if (ids.size() == 1) {
            return getShortDisplayName(ids.iterator().next());
        } else {
            return "(contested identity)";
        }
    }

    /**
     * Finds a single user ID for a given public key.
     *
     * @param pubkeyhash the public key to search for
     * @return the IRI of the user if exactly one ID is found for the public key, or null if no ID or multiple IDs are found
     */
    public IRI findSingleIdForPubkeyhash(String pubkeyhash) {
        if (approvedPubkeyhashIdMap.containsKey(pubkeyhash) && !approvedPubkeyhashIdMap.get(pubkeyhash).isEmpty()) {
            if (approvedPubkeyhashIdMap.get(pubkeyhash).size() == 1) {
                return approvedPubkeyhashIdMap.get(pubkeyhash).iterator().next();
            } else {
                return null;
            }
        }
        if (unapprovedPubkeyhashIdMap.containsKey(pubkeyhash) && !unapprovedPubkeyhashIdMap.get(pubkeyhash).isEmpty()) {
            if (unapprovedPubkeyhashIdMap.get(pubkeyhash).size() == 1) {
                return unapprovedPubkeyhashIdMap.get(pubkeyhash).iterator().next();
            } else {
                return null;
            }
        }
        return null;
    }

    public final transient Comparator<IRI> userComparator = (iri1, iri2) -> getDisplayName(iri1).toLowerCase().compareTo(getDisplayName(iri2).toLowerCase());

    /**
     * Retrieves a list of users, either approved or unapproved.
     *
     * @param approved if true, retrieves approved users; if false, retrieves unapproved users
     * @return a sorted list of user IRIs
     */
    public List<IRI> getUsers(boolean approved) {
        List<IRI> list;
        if (approved) {
            list = new ArrayList<IRI>(approvedIdPubkeyhashMap.keySet());
        } else {
            list = new ArrayList<IRI>();
            for (IRI u : unapprovedIdPubkeyhashMap.keySet()) {
                if (!approvedIdPubkeyhashMap.containsKey(u)) list.add(u);
            }
        }
        // TODO Cache the sorted list to not sort from scratch each time:
        list.sort(userComparator);
        return list;
    }

    /**
     * Retrieves a list of public keys for a given user.
     *
     * @param user     the IRI of the user whose public keys are to be retrieved
     * @param approved if true, retrieves approved public keys; if false, retrieves unapproved public keys
     * @return a list of public keys associated with the user, either approved or unapproved
     */
    public List<String> getPubkeyhashes(IRI user, Boolean approved) {
        List<String> pubkeys = new ArrayList<>();
        if (user != null) {
            if (approved == null || approved) {
                if (approvedIdPubkeyhashMap.containsKey(user)) pubkeys.addAll(approvedIdPubkeyhashMap.get(user));
            }
            if (approved == null || !approved) {
                if (unapprovedIdPubkeyhashMap.containsKey(user)) pubkeys.addAll(unapprovedIdPubkeyhashMap.get(user));
            }
        }
        return pubkeys;
    }

    /**
     * Retrieves a list of introduction nanopublications for a given user.
     *
     * @param user the IRI of the user whose introduction nanopublications are to be retrieved
     * @return a list of introduction nanopublications associated with the user, sorted by creation time
     */
    public List<IntroNanopub> getIntroNanopubs(IRI user) {
        if (introNanopubLists.containsKey(user)) return introNanopubLists.get(user);

        Map<IRI, IntroNanopub> introNps = new HashMap<>();
        if (approvedIdPubkeyhashMap.containsKey(user)) {
            for (String pk : approvedIdPubkeyhashMap.get(user)) {
                getIntroNanopubs(pk, introNps);
            }
        }
        if (unapprovedIdPubkeyhashMap.containsKey(user)) {
            for (String pk : unapprovedIdPubkeyhashMap.get(user)) {
                getIntroNanopubs(pk, introNps);
            }
        }
        List<IntroNanopub> list = new ArrayList<>(introNps.values());
        list.sort((i0, i1) -> {
            Calendar c0 = SimpleTimestampPattern.getCreationTime(i0.getNanopub());
            Calendar c1 = SimpleTimestampPattern.getCreationTime(i1.getNanopub());
            if (c0 == null && c1 == null) return 0;
            if (c0 == null) return 1;
            if (c1 == null) return -1;
            return -c0.compareTo(c1);
        });
        introNanopubLists.put(user, list);
        return list;
    }

    /**
     * Retrieves a map of introduction nanopublications for a given public key.
     *
     * @param pubkey the public key for which introduction nanopublications are to be retrieved
     * @return a map where the keys are IRI identifiers of introduction nanopublications and the values are the corresponding IntroNanopub objects
     */
    public Map<IRI, IntroNanopub> getIntroNanopubs(String pubkey) {
        Map<IRI, IntroNanopub> introNps = new HashMap<>();
        getIntroNanopubs(pubkey, introNps);
        return introNps;
    }

    private void getIntroNanopubs(String pubkeyhash, Map<IRI, IntroNanopub> introNps) {
        if (pubkeyhashIntroMap.containsKey(pubkeyhash)) {
            for (IRI iri : pubkeyhashIntroMap.get(pubkeyhash)) {
                IntroNanopub introNp = toIntroNanopub(iri);
                if (introNp != null) {
                    introNps.put(iri, introNp);
                }
            }
        }
    }

    /**
     * Checks if the given introduction nanopublication is approved.
     *
     * @param in the introduction nanopublication to check
     * @return true if the introduction nanopublication is approved, false otherwise
     */
    public boolean isApproved(IntroNanopub in) {
        return approvedIntros.contains(in.getNanopub().getUri());
    }

    /**
     * Retrieves the location of a public key.
     *
     * @param pubkeyhash the public key for which the location is to be retrieved
     * @return the IRI of the key location if found, or null if not found or if multiple locations are associated with the key
     */
    public IRI getKeyLocationForPubkeyhash(String pubkeyhash) {
        if (approvedPubkeyhashLocationMap.containsKey(pubkeyhash) && !approvedPubkeyhashLocationMap.get(pubkeyhash).isEmpty()) {
            if (approvedPubkeyhashLocationMap.get(pubkeyhash).size() == 1)
                return approvedPubkeyhashLocationMap.get(pubkeyhash).iterator().next();
            return null;
        }
        if (unapprovedPubkeyhashLocationMap.containsKey(pubkeyhash) && unapprovedPubkeyhashLocationMap.get(pubkeyhash).size() == 1) {
            return unapprovedPubkeyhashLocationMap.get(pubkeyhash).iterator().next();
        }
        return null;
    }

}
