package com.knowledgepixels.nanodash;

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
import org.nanopub.NanopubUtils;

public class GrlcDefPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/grlc-def";

	public static final ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI HAS_SPARQL = vf.createIRI("https://w3id.org/kpxl/grlc/sparql");

	private Nanopub np;
	private String requestUrl;
	private String artifactCode, queryPart;
	private String queryName;
	private String label;
	private String desc;
	private String queryContent;

	public GrlcDefPage(PageParameters parameters) {
		super(parameters);
		requestUrl = RequestCycle.get().getRequest().getUrl().toString();
		if (!requestUrl.matches(".*/RA[A-Za-z0-9\\-_]{43}/(.*)?")) return;
		artifactCode = requestUrl.replaceFirst(".*/(RA[A-Za-z0-9\\-_]{43})/(.*)?", "$1");
		queryPart = requestUrl.replaceFirst(".*/(RA[A-Za-z0-9\\-_]{43}/)(.*)?", "$2");
		np = Utils.getAsNanopub(artifactCode);
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
			} else if (st.getPredicate().equals(HAS_SPARQL)) {
				queryContent = st.getObject().stringValue();
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
			// TODO: proper escaping:
			if (label == null) {
				response.write("title: \"untitled query\"\n");
			} else {
				response.write("title: \"" + escape(label) + "\"\n");
			}
			response.write("description: \"" + escape(desc) + "\"\n");
			response.write("queries:\n");
			String baseUrl = NanodashPreferences.get().getWebsiteUrl();
			response.write("  - " + baseUrl + requestUrl + queryName);
		} else if (queryPart.equals(queryName)) {
			if (label != null) {
				response.write("#+ summary: \"" + escape(label) + "\"\n");
			}
			if (desc != null) {
				response.write("#+ description: \"" + escape(desc) + "\"\n");
			}
			response.write(queryContent);
		} else {
			response.sendError(404, "query definition not found / not valid");
		}
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
	}

}
