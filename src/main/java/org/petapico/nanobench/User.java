package org.petapico.nanobench;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;

public class User implements Serializable, Comparable<User> {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static List<User> users;
	private static Map<String,User> userIdMap;
	private static Map<String,User> userPubkeyMap;
	private static Map<String,String> nameFromOrcidMap = new HashMap<>();

	private static synchronized void loadUsers() {
		users = new ArrayList<>();
		userIdMap = new HashMap<String,User>();
		userPubkeyMap = new HashMap<String,User>();
		try {
			for (Map<String,String> entry : ApiAccess.getAll("get_all_users", null)) {
				User user = new User(entry);
				users.add(user);
				userIdMap.put(user.getId().stringValue(), user);
				userPubkeyMap.put(user.getPubkeyString(), user);
			}
			Collections.sort(users);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static synchronized List<User> getUsers(boolean refresh) {
		if (users == null || refresh) {
			loadUsers();
		}
		return users;
	}

	public static User getUser(String id) {
		if (userIdMap == null) {
			loadUsers();
		}
		return userIdMap.get(id);
	}

	public static User getUserForPubkey(String pubkey) {
		if (userPubkeyMap == null) {
			loadUsers();
		}
		return userPubkeyMap.get(pubkey);
	}

	private IRI id;
	private String name;
	private IRI introNpIri;
	private String pubkeyString;

	private User(Map<String,String> entry) {
		id = vf.createIRI(entry.get("user"));
		name = entry.get("name");
		introNpIri = vf.createIRI(entry.get("intronp"));
		pubkeyString = entry.get("pubkey");
	}

	public IRI getId() {
		return id;
	}

	public String getShortId() {
		return id.stringValue().replaceFirst("^https://orcid.org/", "");
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		if (name != null && !name.isEmpty()) {
			return name + " (" + getShortId() + ")";
		}
		String nameFromOrcid = getNameFromOrcid(id.stringValue());
		if (nameFromOrcid != null && !nameFromOrcid.isEmpty()) {
			return nameFromOrcid + " (" + getShortId() + ")";
		}
		return getShortId();
	}

	public String getShortDisplayName() {
		if (name != null && !name.isEmpty()) {
			return name;
		}
		String nameFromOrcid = getNameFromOrcid(id.stringValue());
		if (nameFromOrcid != null && !nameFromOrcid.isEmpty()) {
			return nameFromOrcid;
		}
		return getShortId();
	}

	public IRI getIntropubIri() {
		return introNpIri;
	}

	public String getPubkeyString() {
		return pubkeyString;
	}

	@Override
	public int compareTo(User other) {
		return getDisplayName().compareTo(other.getDisplayName());
	}

	public String getNameFromOrcid(String userId) {
		if (!nameFromOrcidMap.containsKey(userId)) {
			try {
				IntroExtractor ie = IntroNanopub.extract(userId, null);
				if (ie != null) {
					nameFromOrcidMap.put(userId, ie.getName());
				} else {
					nameFromOrcidMap.put(userId, null);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return nameFromOrcidMap.get(userId);
	}

}
