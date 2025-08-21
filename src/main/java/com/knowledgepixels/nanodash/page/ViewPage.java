package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

public class ViewPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    public static final String MOUNT_PATH = "/view";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    public ViewPage(final PageParameters parameters) {
        super(parameters);
        addNanopubItem(this, parameters);
    }

    static void addNanopubItem(NanodashPage page, final PageParameters parameters) {
        String ref = parameters.get("id").toString();
        Nanopub np = Utils.getAsNanopub(ref);
        boolean showHeader = "on".equals(parameters.get("show-header").toOptionalString());
        boolean showFooter = "on".equals(parameters.get("show-footer").toOptionalString());
        boolean showProv = !"off".equals(parameters.get("show-prov").toOptionalString());
        boolean showPubinfo = !"off".equals(parameters.get("show-pubinfo").toOptionalString());
        String templateId = parameters.get("template").toString(null);
        page.add(new NanopubItem("nanopub", NanopubElement.get(np), templateId).setProvenanceHidden(!showProv).setPubinfoHidden(!showPubinfo).setHeaderHidden(!showHeader).setFooterHidden(!showFooter));
    }

}
