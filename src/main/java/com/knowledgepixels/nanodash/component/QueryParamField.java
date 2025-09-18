package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;

import java.net.URISyntaxException;

/**
 * A field for entering query parameters, with validation for required fields and IRIs.
 */
public class QueryParamField extends Panel {

    private static final long serialVersionUID = 1L;

    private final TextField<String> textfield;
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
        textfield = new TextField<>("textfield", Model.of(""));
        textfield.add(new Validator());
        add(textfield);
        add(new Label("marker", isOptional() ? "" : "*"));
    }

    /**
     * Returns the text field for entering the parameter value.
     *
     * @return the text field component
     */
    public TextField<String> getTextField() {
        return textfield;
    }

    /**
     * Returns the value entered in the text field.
     *
     * @return the value of the text field
     */
    public String getValue() {
        return textfield.getModelObject();
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
     * Returns the model of the text field.
     *
     * @return the model of the text field
     */
    public IModel<String> getModel() {
        return textfield.getModel();
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

    /**
     * Checks if the parameter is a multi parameter (ends with "_multi" or "_multi_iri").
     *
     * @return true if the parameter is a multi parameter, false otherwise
     */
    public static boolean isMultiPlaceholder(String placeholder) {
        return placeholder.endsWith("_multi") || placeholder.endsWith("_multi_iri");
    }

    private class Validator extends InvalidityHighlighting implements INullAcceptingValidator<String> {

        private static final long serialVersionUID = 1L;

        @Override
        public void validate(IValidatable<String> s) {
            String value = s.getValue();
            if (isOptional() && !isSet(value)) {
                // all good
                return;
            }
            if (!isSet(value)) {
                s.error(new ValidationError("Missing value for " + paramId));
                return;
            }
            if (isIri()) {
                if (!value.matches("https?://.+")) {
                    s.error(new ValidationError("Invalid IRI protocol: " + value));
                    return;
                }
                try {
                    ParsedIRI piri = new ParsedIRI(value);
                    if (!piri.isAbsolute()) {
                        s.error(new ValidationError("IRI not well-formed: " + value));
                    }
                } catch (URISyntaxException ex) {
                    s.error(new ValidationError("IRI not well-formed: " + value));
                }
            }
        }

        private static boolean isSet(String s) {
            return s != null && !s.isBlank();
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
