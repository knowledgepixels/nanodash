package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.opencsv.exceptions.CsvValidationException;

public class Group implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Map<String,Group> groups;

	public static Collection<Group> getGroups() {
		ensureLoaded();
		return groups.values();
	}

	public synchronized static void ensureLoaded() {
		if (groups == null) {
			refreshGroups();
		}
	}

	public static void refreshGroups() {
		Map<String,Group> newGroups = new HashMap<>();
		try {
			Map<String,String> params = new HashMap<>();
			params.put("type", "http://xmlns.com/foaf/0.1/Group");
			params.put("searchterm", " ");
			ApiResponse result = ApiAccess.getAll("find_valid_signed_things", params);
			for (ApiResponseEntry e : result.getData()) {
				Group g = new Group(e);
				newGroups.put(g.getId(), g);
			}
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
			newGroups = null;
		}
		groups = newGroups;
	}

	public static Group get(String groupId) {
		ensureLoaded();
		return groups.get(groupId);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private final IRI iri;
	private final String name;
	private final Nanopub np;
	private final IRI owner;
	private final List<IRI> members = new ArrayList<>();

	private Group(ApiResponseEntry e) {
		iri = vf.createIRI(e.get("thing"));
		name = e.get("label");
		np = GetNanopub.get(e.get("np"));
		owner = User.getSignatureOwnerIri(np);
		for (Statement st : np.getAssertion()) {
			if (!st.getSubject().equals(iri)) continue;
			if (!st.getPredicate().equals(FOAF.MEMBER)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			members.add((IRI) st.getObject());
		}
	}

	public String getId() {
		return iri.stringValue();
	}

	public IRI getIri() {
		return iri;
	}

	public String getName() {
		return name;
	}

	public Nanopub getNanopub() {
		return np;
	}

	public List<IRI> getMembers() {
		return members;
	}

	public IRI getOwner() {
		return owner;
	}

	public String getOwnerPubkey() {
		NanopubSignatureElement e = null;
		try {
			e = SignatureUtils.getSignatureElement(np);
		} catch (MalformedCryptoElementException ex) {
			ex.printStackTrace();
		}
		if (e == null) {
			return null;
		} else {
			return e.getPublicKeyString();
		}
	}

	@Override
	public int hashCode() {
		return iri.stringValue().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			return iri.equals(((Group) obj).iri);
		}
		return false;
	}

	@Override
	public String toString() {
		return iri.stringValue();
	}

}