package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import org.nanopub.Nanopub;

public class UseTemplateAction extends NanopubAction {

    private static final long serialVersionUID = 4348436856820074305L;

    @Override
    public String getLinkLabel(Nanopub np) {
        return "use template";
    }

    @Override
    public String getTemplateUri(Nanopub np) {
        return np.getUri().stringValue();
    }

    @Override
    public String getParamString(Nanopub np) {
        return "";
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
        return Utils.isNanopubOfClass(np, Template.ASSERTION_TEMPLATE_CLASS);
    }

}
