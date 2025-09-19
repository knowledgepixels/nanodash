package com.knowledgepixels.nanodash.component;

import java.net.URISyntaxException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;

/**
 * A field for entering query parameters, with validation for required fields and IRIs.
 */
public class QueryParamField extends Panel {

    private final FormComponent<String> formComponent;
    private final String paramId;

    /**
     * Constructs a QueryParamField with the given ID and parameter ID.
     *
     * @param id      the Wicket component ID
     * @param paramId the parameter ID, which may start with underscores and end with "_iri"
     */
    public QueryParamField(String id, String paramId) {
        super(id);
        this.paramId = paramId;
        add(new Label("paramname", getParamName()));
        if (isMultiPlaceholder()) {
            add(new Label("textfield").setVisible(false));
            formComponent = new TextArea<>("textarea", Model.of(""));
        } else {
            formComponent = new TextField<>("textfield", Model.of(""));
            add(new Label("textarea").setVisible(false));
        }
        formComponent.add(new Validator());
        add(formComponent);
        add(new Label("marker", isOptional() ? "" : "*"));
    }

    /**
     * Returns the text field for entering the parameter value.
     *
     * @return the text field component
     */
    public FormComponent<String> getFormComponent() {
        return formComponent;
    }

    /**
     * Returns the values (multi) or single value (non-multi) entered in the text field.
     *
     * @return the value of the text field
     */
    public String[] getValues() {
        return expandValues(formComponent.getModelObject(), paramId);
    }

    /**
     * Returns the parameter ID.
     *
     * @return the parameter ID
     */
    public String getParamId() {
        return paramId;
    }

    /**
     * Returns the parameter name derived from the parameter ID.
     *
     * @return the parameter name
     */
    public String getParamName() {
        return getParamName(paramId);
    }

    /**
     * Sets the value of the field (non-multi) or adds the value to the list of values (multi).
     *
     * @param value the value to be set/added
     */
    public void putValue(String value) {
        if (value == null) return;
        if (isMultiPlaceholder()) {
            formComponent.getModel().setObject(formComponent.getModel().getObject() + value + "\n");
        } else {
            formComponent.getModel().setObject(value);
        }
    }

    /**
     * Checks if the parameter is optional (starts with "__").
     *
     * @return true if the parameter is optional, false otherwise
     */
    public boolean isOptional() {
        return paramId.startsWith("__");
    }

    /**
     * Checks if the parameter is an IRI parameter (ends with "_iri").
     *
     * @return true if the parameter is an IRI parameter, false otherwise
     */
    public boolean isIri() {
        return paramId.endsWith("_iri");
    }

    public boolean isSet() {
        return isSet(formComponent.getModelObject());
    }

    /**
     * Checks if the parameter is a multi parameter (ends with "_multi" or "_multi_iri").
     *
     * @return true if the parameter is a multi parameter, false otherwise
     */
    public boolean isMultiPlaceholder() {
        return isMultiPlaceholder(paramId);
    }

    private class Validator extends InvalidityHighlighting implements INullAcceptingValidator<String> {

        @Override
        public void validate(IValidatable<String> i) {
            if (isOptional() && !isSet()) {
                // all good
                return;
            }
            if (!isSet(i.getValue())) {
                i.error(new ValidationError("Missing value for " + paramId));
                return;
            }
            if (isIri()) {
                for (String value : expandValues(i.getValue(), paramId)) {
                    if (!value.matches("https?://.+")) {
                        i.error(new ValidationError("Invalid IRI protocol: " + value));
                        return;
                    }
                    try {
                        ParsedIRI piri = new ParsedIRI(value);
                        if (!piri.isAbsolute()) {
                            i.error(new ValidationError("IRI not well-formed: " + value));
                        }
                    } catch (URISyntaxException ex) {
                        i.error(new ValidationError("IRI not well-formed: " + value));
                    }
                }
            }
        }

    }

    public static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    public static boolean isMultiPlaceholder(String p) {
        return p.endsWith("_multi") || p.endsWith("_multi_iri");
    }

    public static String[] expandValues(String s, String paramId) {
        if (!isSet(s)) {
            return new String[] {};
        } else if (isMultiPlaceholder(paramId)) {
            return s.replaceFirst("\r?\n$", "").split("\r?\n");
        } else {
            return new String[] { s };
        }
    }

    /**
     * Extracts the parameter name from the placeholder ID.
     *
     * @param placeholderId the placeholder ID, which may start with underscores and end with "_iri" and/or "_multi"
     * @return the parameter name, stripped of leading underscores and "_iri"/"_multi" suffixes
     */
    public static String getParamName(String placeholderId) {
        return placeholderId.replaceFirst("^_+", "").replaceFirst("_iri$", "").replaceFirst("_multi$", "");
    }

}
