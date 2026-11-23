package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import java.util.Set;

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
     * Applicable only to a nanopublication that introduces or describes a resource, other than
     * one it embeds: that is what an override keeps and deriving would not (see
     * docs/fill-modes.md). A nanopublication with a root definition introduces the resource it
     * defines, so that is covered too. Overriding anything else would make the same
     * nanopublication as deriving does (issue #920).
     *
     * @param np the nanopublication to check
     * @return true if the nanopublication has an identity an override would keep
     */
    @Override
    public boolean isApplicableTo(Nanopub np) {
        // Embedded resources are minted afresh by an override too, as by deriving.
        Set<String> keptIriIds = NanopubUtils.getIntroducedIriIds(np);
        keptIriIds.removeAll(NanopubUtils.getEmbeddedIriIds(np));
        return !keptIriIds.isEmpty();
    }

}
