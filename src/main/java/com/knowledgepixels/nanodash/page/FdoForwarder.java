package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.vocabulary.FDOF;

import java.util.HashMap;
import java.util.Map;

/**
 * This page is used to forward requests for FDO.
 */
public class FdoForwarder extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for the FdoForwarder page.
     */
    public static final String MOUNT_PATH = "/fdo";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the FdoForwarder page.
     *
     * @param parameters Page parameters containing the request details.
     */
    public FdoForwarder(final PageParameters parameters) {
        super(parameters);

        String id = parameters.get("id").toString();

        if ("object".equals(parameters.get("get").toString())) {
            // TODO properly parse the nanopub content
            Nanopub np = Utils.getNanopub(id);
            Map<String, String> formatMaterializationMap = new HashMap<>();
            for (Statement st : np.getAssertion()) {
                String subj = st.getSubject().stringValue();
                String pred = st.getPredicate().stringValue();
                String obj = st.getObject().stringValue();
                if (pred.equals(FDOF.HAS_METADATA.stringValue()) && obj.equals(id)) {
                    // TODO
                }
                if (pred.equals(FDOF.HAS_ENCODING_FORMAT.stringValue())) {
                    String format = obj.replace("https://iana.org/assignments/media-types/", "");
                    formatMaterializationMap.put(format, subj);
                }
            }
            String acceptHeader = ((HttpServletRequest) getRequest().getContainerRequest()).getHeader("Accept");
            if (formatMaterializationMap.containsKey(acceptHeader)) {
                throw new RedirectToUrlException(formatMaterializationMap.get(acceptHeader), 302);
            }
            if (!formatMaterializationMap.isEmpty()) {
                throw new RedirectToUrlException(formatMaterializationMap.values().iterator().next(), 302);
            }
        }
        throw new RedirectToUrlException(Utils.getUrlWithParameters(ExplorePage.MOUNT_PATH, new PageParameters().add("id", id)));
    }

}
