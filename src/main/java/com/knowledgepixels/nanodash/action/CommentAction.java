package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

/**
 * Represents an action that can be performed on a Nanopub to add a comment.
 */
public class CommentAction extends NanopubAction {

    /**
     * The URI of the template for the comment action.
     */
    public static final String TEMPLATE_URI = "https://w3id.org/np/RAaqrSEatLCNYWZb6VigZk_ttPrtNSyvhhFSgunSybPEs";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "comment";
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
        return "param_thing=" + getEncodedUri(np);
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
