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

public class QueryParamField extends Panel {

    private static final long serialVersionUID = 1L;

    private final TextField<String> textfield;
    private final String paramId;

    public QueryParamField(String id, String paramId) {
        super(id);
        this.paramId = paramId;
        add(new Label("paramname", getParamName()));
        textfield = new TextField<>("textfield", Model.of(""));
        textfield.add(new Validator());
        add(textfield);
        add(new Label("marker", isOptional() ? "" : "*"));
    }

    public TextField<String> getTextField() {
        return textfield;
    }

    public String getValue() {
        return textfield.getModelObject();
    }

    public String getParamId() {
        return paramId;
    }

    public String getParamName() {
        return getParamName(paramId);
    }

    public IModel<String> getModel() {
        return textfield.getModel();
    }

    public boolean isOptional() {
        return paramId.startsWith("__");
    }

    public boolean isIri() {
        return paramId.endsWith("_iri");
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

    public static String getParamName(String placeholderId) {
        return placeholderId.replaceFirst("^_+", "").replaceFirst("_iri$", "");
    }

}
