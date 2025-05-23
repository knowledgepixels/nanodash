package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.Utils;

public class TermForwarder extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/term";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public TermForwarder(final PageParameters parameters) {
		super(parameters);
		// Getting HTTP Accept header:
		//
		//System.err.println("P: " + ((HttpServletRequest) getRequest().getContainerRequest()).getHeader("Accept"));

		// Earlier code to redirect to nanopub if authority matches:

//		IRI id = vf.createIRI(parameters.get("id").toString());
//		IRI authority = null;
//		if (!parameters.get("authority").isEmpty()) {
//			authority = vf.createIRI(parameters.get("authority").toString());
//		}
//		Map<String,String> params = new HashMap<>();
//		params.put("thing", id.stringValue());
//		ApiResponse dataResponse = QueryApiAccess.get("get-introducing-nanopub", params);
//		List<ApiResponseEntry> responses;
//		if (authority == null) {
//			responses = dataResponse.getData();
//		} else {
//			responses = new ArrayList<>();
//			for (ApiResponseEntry r : dataResponse.getData()) {
//				if (authority.equals(User.getUserIri(r.get("pubkey")))) responses.add(r);
//			}
//		}
//		if (responses.size() != 1) {
//			throw new RedirectToUrlException(Utils.getUrlWithParameters(SearchPage.MOUNT_PATH, new PageParameters().add("query", id.stringValue())));
//		}
//		String npUri = responses.get(0).get("np");
//		throw new RedirectToUrlException(Utils.getUrlWithParameters(ExplorePage.MOUNT_PATH, new PageParameters().add("id", npUri)));

		// Currently just redirecting to explore page, where "authority" param is ignored:

		throw new RedirectToUrlException(Utils.getUrlWithParameters(ExplorePage.MOUNT_PATH, parameters));

		// TODO Consider authority again, and show matching (or non-matching) authority accordingly
	}

//	private static final ValueFactory vf = SimpleValueFactory.getInstance();

}
