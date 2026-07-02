package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Represents an action that creates a new nanopublication overriding an existing one:
 * like deriving (it records a {@code prov:wasDerivedFrom} link) but keeping the source
 * nanopub's introduced resource IRIs and its root definition nanopub.
 */
public class OverrideAction extends NanopubAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "edit as overriding nanopublication";
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
        return "override=" + getEncodedUri(np);
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
