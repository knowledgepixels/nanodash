package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.Utils;

import jakarta.servlet.http.HttpServletRequest;

public class FdoForwarder extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/fdo";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public FdoForwarder(final PageParameters parameters) {
		super(parameters);

		String id = parameters.get("id").toString();

		if ("object".equals(parameters.get("get").toString())) {
			// TODO properly parse the nanopub content
			Nanopub np = Utils.getNanopub(id);
			Map<String,String> formatMaterializationMap = new HashMap<>();
			for (Statement st : np.getAssertion()) {
				String subj = st.getSubject().stringValue();
				String pred = st.getPredicate().stringValue();
				String obj = st.getObject().stringValue();
				if (pred.equals("https://w3id.org/fdof/ontology#hasMetadata") && obj.equals(id)) {
					// TODO
				}
				if (pred.equals("https://w3id.org/fdof/ontology#hasEncodingFormat")) {
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
