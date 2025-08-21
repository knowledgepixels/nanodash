package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;

/**
 * <p>GroupDemoPage class.</p>
 */
public class GroupDemoPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constant <code>MOUNT_PATH="/groupdemo"</code>
     */
    public static final String MOUNT_PATH = "/groupdemo";

    private static final String GROUP_DEMO_TEMPLATE = "http://purl.org/np/RAJz6w5cvlsFGkCDtWOUXt2VwEQ3tVGtPdy3atPj_DUhk";

    private static final String GROUP_DEMO_EXAMPLE = "http://purl.org/np/RAtnL4ZLql_L5KMSrGNx3y393qqWYaFfxurBySRam8dac";

    private static final String HAS_MEMBER = "http://xmlns.com/foaf/0.1/member";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * <p>Constructor for GroupDemoPage.</p>
     *
     * @param parameters a {@link org.apache.wicket.request.mapper.parameter.PageParameters} object
     * @throws org.nanopub.extra.services.FailedApiCallException if any.
     */
    public GroupDemoPage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        String id = parameters.get("id").toString();
        ApiResponse resp = QueryApiAccess.get("get-introducing-np", "thing", GROUP_DEMO_EXAMPLE);
        String npId = resp.getData().getFirst().get("np");
        Nanopub np = Utils.getAsNanopub(npId);

        String groupName = id.replaceFirst("^.*/", "");

        add(new Label("pagetitle", groupName + " (project) | nanodash"));
        add(new Label("groupname", groupName));
    }

}
