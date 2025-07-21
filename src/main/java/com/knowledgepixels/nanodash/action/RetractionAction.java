package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

public class RetractionAction extends NanopubAction {

    private static final long serialVersionUID = 7802391790148929067L;

    /**
     * The URI of the template used for retraction actions.
     */
    public static final String TEMPLATE_URI = "http://purl.org/np/RAvySE8-JDPqaPnm_XShAa-aVuDZ2iW2z7Oc1Q9cfvxZE";

    @Override
    public String getLinkLabel(Nanopub np) {
        return "retract";
    }

    @Override
    public String getTemplateUri(Nanopub np) {
        return TEMPLATE_URI;
    }

    @Override
    public String getParamString(Nanopub np) {
        return "param_nanopubToBeRetracted=" + getEncodedUri(np);
    }

    @Override
    public boolean isApplicableToOwnNanopubs() {
        return true;
    }

    @Override
    public boolean isApplicableToOthersNanopubs() {
        return false;
    }

    @Override
    public boolean isApplicableTo(Nanopub np) {
        return true;
    }

}
