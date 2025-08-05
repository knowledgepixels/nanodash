package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

/**
 * Represents an action that can be performed on a Nanopub to add a comment.
 */
public class CommentAction extends NanopubAction {

    private static final long serialVersionUID = 7995012295240119652L;

    /**
     * The URI of the template for the comment action.
     */
    public static final String TEMPLATE_URI = "http://purl.org/np/RAqfUmjV05ruLK3Efq2kCODsHfY16LJGO3nAwDi5rmtv0";

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
