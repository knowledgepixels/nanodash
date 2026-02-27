package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a restricted choice for a placeholder in a template context.
 * This class manages possible values for a placeholder, including fixed values
 * and reference values that depend on other placeholders.
 */
public class RestrictedChoice implements Serializable {

    private IRI placeholderIri;

    private TemplateContext context;

    private final Map<String, Boolean> fixedPossibleValues = new HashMap<>();

    private final List<IRI> possibleRefValues = new ArrayList<>();

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Constructs a RestrictedChoice object for a given placeholder and template context.
     *
     * @param placeholderIri The IRI of the placeholder.
     * @param context        The template context.
     */
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

    /**
     * Retrieves a list of possible values for the placeholder.
     * This includes both fixed values and dynamically resolved reference values.
     *
     * @return A sorted list of possible values.
     */
    public List<String> getPossibleValues() {
        Set<String> possibleValues = new HashSet<>(fixedPossibleValues.keySet());
        for (IRI r : possibleRefValues) {
            for (int i = 0; true; i++) {
                String suffix = "__" + i;
                if (i == 0) suffix = "";
                IRI refIri = vf.createIRI(r.stringValue() + suffix);
                IModel<String> m = (IModel<String>) context.getComponentModels().get(refIri);
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

    /**
     * Checks if there are any reference values for the placeholder.
     *
     * @return True if there are reference values, false otherwise.
     */
    public boolean hasPossibleRefValues() {
        return !possibleRefValues.isEmpty();
    }

    /**
     * Checks if a given value is a fixed possible value for the placeholder.
     *
     * @param value The value to check.
     * @return True if the value is a fixed possible value, false otherwise.
     */
    public boolean hasFixedPossibleValue(String value) {
        return fixedPossibleValues.containsKey(value);
    }

}
