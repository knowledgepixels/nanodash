package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.Utils;
import org.nanopub.Nanopub;
import org.nanopub.vocabulary.NTEMPLATE;

/**
 * Action to use a template.
 */
public class UseTemplateAction extends NanopubAction {

    private static final long serialVersionUID = 4348436856820074305L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLinkLabel(Nanopub np) {
        return "use template";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplateUri(Nanopub np) {
        return np.getUri().stringValue();
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
        return Utils.isNanopubOfClass(np, NTEMPLATE.ASSERTION_TEMPLATE);
    }

}
