package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

/**
 * Represents an action that can be performed on a Nanopub to approve or disapprove it.
 */
public class ApprovalAction extends NanopubAction {

    private static final long serialVersionUID = 1789820405326599889L;

    /**
     * The URI of the template for the approval action.
     */
    public static final String TEMPLATE_URI = "http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "approve/disapprove";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplateUri(Nanopub np) {
        return TEMPLATE_URI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParamString(Nanopub np) {
        return "param_nanopub=" + getEncodedUri(np);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableToOwnNanopubs() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableToOthersNanopubs() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableTo(Nanopub np) {
        return true;
    }

}
