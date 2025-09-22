package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Represents an action that can be performed on a Nanopub to improve it.
 */
public class ImproveAction extends NanopubAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "improve";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplateUri(Nanopub np) {
        return TemplateData.get().getTemplateId(np).stringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParamString(Nanopub np) {
        return "improve=" + getEncodedUri(np);
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
        return TemplateData.get().getTemplateId(np) != null;
    }

}
