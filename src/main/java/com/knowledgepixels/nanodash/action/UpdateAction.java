package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Represents an action to update a nanopublication.
 */
public class UpdateAction extends NanopubAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "update";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplateUri(Nanopub np) {
        if (TemplateData.get().getTemplateId(np) != null) {
            return TemplateData.get().getTemplateId(np).stringValue();
        } else {
            return "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParamString(Nanopub np) {
        return "supersede=" + getEncodedUri(np);
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
