package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Action to create a new nanopub using the same template as the given nanopub.
 */
public class UseSameTemplateAction extends NanopubAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "create new with same template";
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
        return "";
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
