package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.opencsv.exceptions.CsvValidationException;

import net.trustyuri.TrustyUriUtils;

public class TemplateData implements Serializable {

	private static final long serialVersionUID = 1L;

	private static TemplateData instance;

	public static synchronized void refreshTemplates() {
		instance = new TemplateData();
	}

	public static synchronized void ensureLoaded() {
		if (instance == null) refreshTemplates();
	}

	public static TemplateData get() {
		ensureLoaded();
		return instance;
	}

	private List<Template> assertionTemplates, provenanceTemplates, pubInfoTemplates;
	private Map<String,Template> templateMap;

	public TemplateData() {
		assertionTemplates = new ArrayList<>();
		provenanceTemplates = new ArrayList<>();
		pubInfoTemplates = new ArrayList<>();
		templateMap = new HashMap<>();
		refreshTemplates(assertionTemplates, Template.ASSERTION_TEMPLATE_CLASS);
		refreshTemplates(provenanceTemplates, Template.PROVENANCE_TEMPLATE_CLASS);
		refreshTemplates(pubInfoTemplates, Template.PUBINFO_TEMPLATE_CLASS);
	}

	private void refreshTemplates(List<Template> templates, IRI type) {
		Map<String,String> params = new HashMap<>();
		params.put("pred", RDF.TYPE.toString());
		params.put("obj", type.toString());
		params.put("graphpred", Nanopub.HAS_ASSERTION_URI.toString());
		ApiResponse templateEntries;
		try {
			templateEntries = ApiAccess.getAll("find_valid_signed_nanopubs_with_pattern", params);
			for (ApiResponseEntry entry : templateEntries.getData()) {
				try {
					Template t = new Template(entry.get("np"));
					if (!t.isUnlisted()) templates.add(t);
					templateMap.put(t.getId(), t);
				} catch (Exception ex) {
					System.err.println("Exception: " + ex.getMessage());
				}
			}
		} catch (IOException|CsvValidationException ex) {
			// TODO Better handle this (re-try to get templates)
			ex.printStackTrace();
		}
	}

	public List<Template> getAssertionTemplates() {
		return assertionTemplates;
	}

	public List<Template> getProvenanceTemplates() {
		return provenanceTemplates;
	}

	public List<Template> getPubInfoTemplates() {
		return pubInfoTemplates;
	}

	public Template getTemplate(String id) {
		Template template = templateMap.get(id);
		if (template != null) return template;
		if (TrustyUriUtils.isPotentialTrustyUri(id)) {
			try {
				Template t = new Template(id);
				templateMap.put(id, t);
				return t;
			} catch (Exception ex) {
				System.err.println("Exception: " + ex.getMessage());
				return null;
			}
		}
		return null;
	}

	public Template getTemplate(Nanopub np) {
		IRI templateId = getTemplateId(np);
		if (templateId == null) return null;
		return getTemplate(templateId.stringValue());
	}

	public Template getProvenanceTemplate(Nanopub np) {
		IRI templateId = getProvenanceTemplateId(np);
		if (templateId == null) return null;
		return getTemplate(templateId.stringValue());
	}

	public Set<Template> getPubinfoTemplates(Nanopub np) {
		Set<Template> templates = new HashSet<>();
		for (IRI id : getPubinfoTemplateIds(np)) {
			templates.add(getTemplate(id.stringValue()));
		}
		return templates;
	}

	public IRI getTemplateId(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			return (IRI) st.getObject();
		}
		return null;
	}

	public IRI getProvenanceTemplateId(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			return (IRI) st.getObject();
		}
		return null;
	}

	public Set<IRI> getPubinfoTemplateIds(Nanopub nanopub) {
		Set<IRI> iriSet = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			iriSet.add((IRI) st.getObject());
		}
		return iriSet;
	}

}
