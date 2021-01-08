package org.petapico.nanobench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.nanopub.extra.security.IntroNanopub.IntroExtractor;
import org.nanopub.extra.server.FetchIndex;

public class User implements Serializable, Comparable<User> {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static String authorityIndex = "http://purl.org/np/RAZ4KCC6EceKwJP602FI4WG0UgsjS0OaGDuKvadd6z-jI";

	private static List<User> users;
	private static Map<String,User> userIdMap;
	private static Map<String,User> userPubkeyMap;
	private static Map<String,String> nameFromOrcidMap = new HashMap<>();

	private static synchronized void loadUsers() {
		users = new ArrayList<>();
		userIdMap = new HashMap<String,User>();
		userPubkeyMap = new HashMap<String,User>();
		try {
			for (ApiResponseEntry entry : ApiAccess.getAll("get_all_users", null).getData()) {
				User user = new User(entry);
				users.add(user);
				userIdMap.put(user.getId().stringValue(), user);
				userPubkeyMap.put(user.getPubkeyString(), user);
			}
			Collections.sort(users);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// TODO: use piped out-in stream here:
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new FetchIndex(authorityIndex, out, RDFFormat.TRIG, false, true, null).run();
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		try {
			MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new MultiNanopubRdfHandler.NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					System.err.println("Authority index element: " + np.getUri());
					// TODO: process these authority index nanopublications
				}
			});
		} catch (RDFParseException | RDFHandlerException | IOException | MalformedNanopubException ex) {
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

	private User(ApiResponseEntry entry) {
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
				System.err.println("Could not get name from ORCID account: " + userId);
			}
		}
		return nameFromOrcidMap.get(userId);
	}

}
