package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;

/**
 * Represents an action that can be performed on a Nanopub to derive a new nanopublication from it.
 */
public class DeriveAction extends NanopubAction {

    private static final long serialVersionUID = 4348436856820074305L;

    @Override
    public String getLinkLabel(Nanopub np) {
        return "edit as derived nanopublication";
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
        return "derive=" + getEncodedUri(np);
    }

    @Override
    public boolean isApplicableToOwnNanopubs() {
        return true;
    }

    @Override
    public boolean isApplicableToOthersNanopubs() {
        return true;
    }

    @Override
    public boolean isApplicableTo(Nanopub np) {
        return true;
    }

}
