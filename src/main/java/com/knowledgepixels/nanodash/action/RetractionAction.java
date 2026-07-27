package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

/**
 * Represents an action to retract a nanopublication.
 */
public class RetractionAction extends NanopubAction {

    /**
     * The URI of the template used for retraction actions.
     */
    public static final String TEMPLATE_URI = "https://w3id.org/np/RAZl7bwTcLA5M5uLy5WCPuz8QEejBYEQnVNkL_d1lbj4Y";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "retract";
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
        return "param_nanopubToBeRetracted=" + getEncodedUri(np);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableToOwnNanopubs() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableToOthersNanopubs() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableTo(Nanopub np) {
        return true;
    }

}
