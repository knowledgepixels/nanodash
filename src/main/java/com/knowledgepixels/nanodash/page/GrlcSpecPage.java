package com.knowledgepixels.nanodash.page;

import java.util.Set;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.SimpleCreatorPattern;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;

import net.trustyuri.TrustyUriUtils;

public class GrlcSpecPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/grlc-spec";

	public static final ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI HAS_SPARQL = vf.createIRI("https://w3id.org/kpxl/grlc/sparql");
	public static final IRI HAS_ENDPOINT = vf.createIRI("https://w3id.org/kpxl/grlc/endpoint");

	private Nanopub np;
	private String requestUrlBase;
	private String artifactCode, queryPart;
	private String queryName;
	private String label;
	private String desc;
	private String license;
	private String queryContent;
	private String endpoint;

	public GrlcSpecPage(PageParameters parameters) {
		super(parameters);
		String requestUrl = RequestCycle.get().getRequest().getUrl().toString();
		requestUrl = requestUrl.replaceFirst("\\?.*$", "");
		if (!requestUrl.matches(".*/RA[A-Za-z0-9\\-_]{43}/(.*)?")) return;
		artifactCode = requestUrl.replaceFirst("^(.*/)(RA[A-Za-z0-9\\-_]{43})/(.*)?$", "$2");
		queryPart = requestUrl.replaceFirst("^(.*/)(RA[A-Za-z0-9\\-_]{43}/)(.*)?$", "$3");
		requestUrlBase = requestUrl.replaceFirst("^(.*/)(RA[A-Za-z0-9\\-_]{43})/(.*)?$", "$1");

		np = Utils.getAsNanopub(artifactCode);
		if ("latest".equals(getRequest().getQueryParameters().getParameterValue("api-version").toString())) {
			np = Utils.getAsNanopub(QueryApiAccess.getLatestVersionId(np.getUri().stringValue()));
			artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
		}
		for (Statement st : np.getAssertion()) {
			if (!st.getSubject().stringValue().startsWith(np.getUri().stringValue())) continue;
			String qn = st.getSubject().stringValue().replaceFirst("^.*[#/](.*)$", "$1");
			if (queryName != null && !qn.equals(queryName)) {
				np = null;
				break;
			}
			queryName = qn;
			if (st.getPredicate().equals(RDFS.LABEL)) {
				label = st.getObject().stringValue();
			} else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
				desc = st.getObject().stringValue();
			} else if (st.getPredicate().equals(DCTERMS.LICENSE) && st.getObject() instanceof IRI) {
				license = st.getObject().stringValue();
			} else if (st.getPredicate().equals(HAS_SPARQL)) {
				queryContent = st.getObject().stringValue().replace("https://w3id.org/np/l/nanopub-query-1.1/", NanodashPreferences.get().getQueryUrl());
			} else if (st.getPredicate().equals(HAS_ENDPOINT) && st.getObject() instanceof IRI) {
				endpoint = st.getObject().stringValue();
				if (NanodashPreferences.get().getQueryUrl() != null && endpoint.startsWith("https://w3id.org/np/l/nanopub-query-1.1/")) {
					endpoint = endpoint.replace("https://w3id.org/np/l/nanopub-query-1.1/", NanodashPreferences.get().getQueryUrl());
				}
			}
		}
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	public final void renderPage() {
		WebResponse response = (WebResponse) getResponse();
		if (np == null) {
			response.sendError(404, "query/API definition not found / not valid");
			return;
		}
		response.setContentType("text/plain");
		if (queryPart.isEmpty()) {
			if (label == null) {
				response.write("title: \"untitled query\"\n");
			} else {
				response.write("title: \"" + escape(label) + "\"\n");
			}
			response.write("description: \"" + escape(desc) + "\"\n");
			String userName = "";
			Set<IRI> creators = SimpleCreatorPattern.getCreators(np);
			for (IRI userIri : creators) {
				userName += ", " + User.getShortDisplayName(userIri);
			}
			if (!userName.isEmpty()) userName = userName.substring(2);
			String url = "";
			if (!creators.isEmpty()) url = creators.iterator().next().stringValue();
			response.write("contact:\n");
			response.write("  name: \"" + escape(userName) + "\"\n");
			response.write("  url: " + url + "\n");
			if (license != null) {
				response.write("licence: " + license + "\n");
			}
			response.write("queries:\n");
			String baseUrl = NanodashPreferences.get().getWebsiteUrl();
			response.write("  - " + baseUrl + requestUrlBase + artifactCode + "/" + queryName + ".rq");
		} else if (queryPart.equals(queryName + ".rq")) {
			if (label != null) {
				response.write("#+ summary: \"" + escape(label) + "\"\n");
			}
			if (desc != null) {
				response.write("#+ description: \"" + escape(desc) + "\"\n");
			}
			if (license != null) {
				response.write("#+ licence: " + license + "\n");
			}
			if (endpoint != null) {
				response.write("#+ endpoint: " + endpoint + "\n");
			}
			response.write("\n");
			response.write(queryContent);
		} else {
			response.sendError(404, "query definition not found / not valid");
		}
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
	}

}
