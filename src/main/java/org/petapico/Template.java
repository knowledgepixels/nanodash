package org.petapico;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

public class Template implements Serializable {

	private static final long serialVersionUID = 1L;

	private static List<Template> templates;

	public static List<Template> getTemplates() {
		if (templates == null) {
			templates = new ArrayList<>();
			Map<String,String> params = new HashMap<>();
			params.put("pred", RDF.TYPE.toString());
			params.put("obj", PublishForm.ASSERTION_TEMPLATE_CLASS.toString());
			params.put("graphpred", Nanopub.HAS_ASSERTION_URI.toString());
			List<Map<String,String>> templateEntries;
			try {
				templateEntries = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params);
				for (Map<String,String> entry : templateEntries) {
					if (entry.get("superseded").equals("1") || entry.get("superseded").equals("true")) continue;
					if (entry.get("retracted").equals("1") || entry.get("retracted").equals("true")) continue;
					templates.add(new Template(entry.get("np")));
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return templates;
	}

	private String id;

	private Template(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
