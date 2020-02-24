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

public class User implements Serializable, Comparable<User> {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static List<User> users;
	private static Map<String,User> userMap;

	private static void loadUsers() {
		users = new ArrayList<>();
		userMap = new HashMap<String,User>();
		try {
			for (Map<String,String> entry : ApiAccess.getAll("get_all_users", null)) {
				User user = new User(entry);
				users.add(user);
				userMap.put(user.getId().stringValue(), user);
			}
			Collections.sort(users);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static List<User> getUsers(boolean refresh) {
		if (users == null || refresh) {
			loadUsers();
		}
		return users;
	}

	public static User getUser(String id) {
		if (userMap == null) {
			loadUsers();
		}
		return userMap.get(id);
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
		if (name == null || name.isBlank()) return getShortId();
		return name + " (" + getShortId() + ")";
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

}
