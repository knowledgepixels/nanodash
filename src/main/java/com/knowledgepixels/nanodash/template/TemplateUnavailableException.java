package com.knowledgepixels.nanodash.template;

/**
 * Thrown when a form cannot be built because one of the templates it is made of could not be
 * loaded: it is not a template, or it could not be fetched right now. Unchecked, since it
 * comes out of component constructors; the places that build a form catch it and say so on
 * the page instead of failing the request.
 */
public class TemplateUnavailableException extends RuntimeException {

    private final ContextType contextType;
    private final String templateId;

    /**
     * @param contextType which part of the nanopublication the template is for
     * @param templateId  the ID the template was asked for with
     */
    public TemplateUnavailableException(ContextType contextType, String templateId) {
        super("Could not load " + kindOf(contextType) + " template: " + templateId);
        this.contextType = contextType;
        this.templateId = templateId;
    }

    /**
     * @return which part of the nanopublication the template is for
     */
    public ContextType getContextType() {
        return contextType;
    }

    /**
     * @return the ID the template was asked for with
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * @return the kind of template in words, such as "provenance"
     */
    public String getKind() {
        return kindOf(contextType);
    }

    private static String kindOf(ContextType contextType) {
        return switch (contextType) {
            case ASSERTION -> "assertion";
            case PROVENANCE -> "provenance";
            case PUBINFO -> "publication info";
        };
    }

}
