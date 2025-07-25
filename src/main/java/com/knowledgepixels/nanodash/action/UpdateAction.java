package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Represents an action to update a nanopublication.
 */
public class UpdateAction extends NanopubAction {

    private static final long serialVersionUID = -7703679333911796987L;

    @Override
    public String getLinkLabel(Nanopub np) {
        return "update";
    }

    @Override
    public String getTemplateUri(Nanopub np) {
        if (TemplateData.get().getTemplateId(np) != null) {
            return TemplateData.get().getTemplateId(np).stringValue();
        } else {
            return "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
        }
    }

    @Override
    public String getParamString(Nanopub np) {
        return "supersede=" + getEncodedUri(np);
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
