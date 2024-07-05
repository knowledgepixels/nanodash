package com.knowledgepixels.nanodash.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.Group;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.opencsv.exceptions.CsvValidationException;

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

		IRI id = vf.createIRI(parameters.get("id").toString());
		IRI authority = null;
		if (!parameters.get("authority").isEmpty()) {
			authority = vf.createIRI(parameters.get("authority").toString());
		}
		if (Group.get(id.stringValue()) != null) {
			throw new RedirectToUrlException(Utils.getUrlWithParameters(GroupPage.MOUNT_PATH, new PageParameters().add("id", id)));
		}
		Map<String,String> params = new HashMap<>();
		params.put("graphpred", "http://www.nanopub.org/nschema#hasPublicationInfo");
		params.put("pred", "http://purl.org/nanopub/x/introduces");
		params.put("obj", id.stringValue());
		ApiResponse dataResponse = null;
		try {
			dataResponse = ApiAccess.getAll("find_valid_signed_nanopubs_with_pattern", params);
		} catch (IOException|CsvValidationException ex) {
			throw new RuntimeException(ex);
		}
		List<ApiResponseEntry> responses;
		if (authority == null) {
			responses = dataResponse.getData();
		} else {
			responses = new ArrayList<>();
			for (ApiResponseEntry r : dataResponse.getData()) {
				if (authority.equals(User.getUserIri(r.get("pubkey")))) responses.add(r);
			}
		}
		if (responses.size() != 1) {
			throw new RedirectToUrlException(Utils.getUrlWithParameters(SearchPage.MOUNT_PATH, new PageParameters().add("query", id.stringValue())));
		}
		String npUri = responses.get(0).get("np");
		throw new RedirectToUrlException(Utils.getUrlWithParameters(ExplorePage.MOUNT_PATH, new PageParameters().add("id", npUri)));
	}

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

}
