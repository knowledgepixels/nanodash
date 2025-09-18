package com.knowledgepixels.nanodash.component;

import java.net.URISyntaxException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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
     * Returns the value entered in the text field.
     *
     * @return the value of the text field
     */
    public String getValue() {
        return formComponent.getModelObject();
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
        return formComponent.getModel();
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
    public boolean isMultiPlaceholder() {
        return paramId.endsWith("_multi") || paramId.endsWith("_multi_iri");
    }

    private class Validator extends InvalidityHighlighting implements INullAcceptingValidator<String> {

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
