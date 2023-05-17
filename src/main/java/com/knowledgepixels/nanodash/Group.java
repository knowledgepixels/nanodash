package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.opencsv.exceptions.CsvValidationException;

public class Group implements Serializable {

	private static final long serialVersionUID = 1L;

	private static List<Group> groups;

	public static List<Group> getGroups() {
		if (groups == null) {
			try {
				groups = new ArrayList<>();
				Map<String,String> params = new HashMap<>();
				params.put("type", "http://xmlns.com/foaf/0.1/Group");
				params.put("searchterm", " ");
				ApiResponse result = ApiAccess.getAll("find_signed_things", params);
				for (ApiResponseEntry e : result.getData()) {
					groups.add(new Group(e));
				}
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
				groups = null;
			}
		}
		return groups;
	}

	public static void refreshGroups() {
		groups = null;
		getGroups();
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private final IRI iri;
	private final String name;
	private final Nanopub np;

	private Group(ApiResponseEntry e) {
		iri = vf.createIRI(e.get("thing"));
		name = e.get("label");
		np = GetNanopub.get(e.get("np"));
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