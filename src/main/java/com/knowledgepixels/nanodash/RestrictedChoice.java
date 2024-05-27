package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.knowledgepixels.nanodash.template.TemplateContext;

public class RestrictedChoice implements Serializable {

	private static final long serialVersionUID = 1L;

	private IRI placeholderIri;
	private TemplateContext context;
	private final Map<String,Boolean> fixedPossibleValues = new HashMap<>();
	private final List<IRI> possibleRefValues = new ArrayList<>();

	public RestrictedChoice(IRI placeholderIri, TemplateContext context) {
		this.placeholderIri = placeholderIri;
		this.context = context;
		for (Value v : context.getTemplate().getPossibleValues(placeholderIri)) {
			if (v instanceof IRI && context.getTemplate().isPlaceholder((IRI) v)) {
				possibleRefValues.add((IRI) v);
			} else {
				fixedPossibleValues.put(v.toString(), true);
			}
		}
	}

	public List<String> getPossibleValues() {
		Set<String> possibleValues = new HashSet<>();
		possibleValues.addAll(fixedPossibleValues.keySet());
		for (IRI r : possibleRefValues) {
			for (int i = 0 ; true ; i++) {
				String suffix = "__" + i;
				if (i == 0) suffix = "";
				IRI refIri = vf.createIRI(r.stringValue() + suffix);
				IModel<String> m = context.getComponentModels().get(refIri);
				if (m == null) break;
				if (m.getObject() != null && !m.getObject().startsWith("\"")) {
					possibleValues.add(m.getObject());
				}
			}
		}
		List<String> possibleValuesList = new ArrayList<>();
		for (String s : possibleValues) {
			if (context.getTemplate().isLocalResource(placeholderIri)) {
				if (s.matches("https?://.+")) continue;
			}
			possibleValuesList.add(s);
		}
		Collections.sort(possibleValuesList);
		return possibleValuesList;
	}

	public boolean hasPossibleRefValues() {
		return !possibleRefValues.isEmpty();
	}

	public boolean hasFixedPossibleValue(String value) {
		return fixedPossibleValues.containsKey(value);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
