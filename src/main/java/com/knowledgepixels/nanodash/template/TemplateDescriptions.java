package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.templates.Template;

/**
 * The descriptions of a template as HTML that is safe to show.
 * <p>
 * A {@link Template} gives its descriptions as published, which may contain any HTML: whoever
 * publishes a template decides what is in them. They have to go through here before they are
 * shown without escaping.
 */
public class TemplateDescriptions {

    private TemplateDescriptions() {
    }

    /**
     * Returns the description of a template.
     *
     * @param template the template
     * @return the sanitized description, or null if the template has none
     */
    public static String getDescription(Template template) {
        String description = template.getDescription();
        return description == null ? null : Utils.sanitizeHtml(description);
    }

    /**
     * Returns the description that tells how to fill in a statement or statement group.
     *
     * @param template    the template
     * @param statementId the IRI of the statement or statement group
     * @return the sanitized description, or null if the template gives none
     */
    public static String getStatementDescription(Template template, IRI statementId) {
        String description = template.getStatementDescription(statementId);
        return description == null ? null : Utils.sanitizeHtml(description);
    }

}
