package com.knowledgepixels.nanodash.page;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.nanopub.Nanopub;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.TitleBar;

public class QueryPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/query";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public QueryPage(final PageParameters parameters) {
		super(parameters);
		add(new TitleBar("titlebar", this, null));
		add(new Label("pagetitle", "Query Info | nanodash"));

		String npId = parameters.get("id").toString();
		String queryString = getQueryString(npId);
		List<String> placeholders = getQueryPlaceholders(queryString);
		String placeholdersString = "";
		for (String p : placeholders) {
			placeholdersString += p + " ";
		}
		add(new Label("placeholders", placeholdersString));
		String endpoint = getQueryEndpoint(npId);
		// TODO Replace hard-coded Nanopub Query URL with dynamic solution:
		String editLink = endpoint.replaceFirst("^.*/repo/", "https://query.petapico.org/tools/") + "/yasgui.html#query=" + URLEncoder.encode(queryString, Charsets.UTF_8);
		add(new ExternalLink("editlink", editLink));
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

	public static String getQueryString(String npId) {
		Nanopub np = Utils.getNanopub(npId);
		for (Statement st : np.getAssertion()) {
			if (st.getPredicate().stringValue().equals("https://w3id.org/kpxl/grlc/sparql")) {
				return st.getObject().stringValue();
			}
		}
		return null;
	}

	public static String getQueryEndpoint(String npId) {
		Nanopub np = Utils.getNanopub(npId);
		for (Statement st : np.getAssertion()) {
			if (st.getPredicate().stringValue().equals("https://w3id.org/kpxl/grlc/endpoint")) {
				return st.getObject().stringValue();
			}
		}
		return null;
	}

	public static List<String> getQueryPlaceholders(String queryString) {
		final Set<String> placeholders = new HashSet<>();
		ParsedQuery query = new SPARQLParser().parseQuery(queryString, null);
		query.getTupleExpr().visitChildren(new AbstractSimpleQueryModelVisitor<>() {

			@Override
			public void meet(Var node) throws RuntimeException {
				super.meet(node);
				if (!node.isConstant() && node.getName().startsWith("_")) {
					placeholders.add(node.getName());
				}
			}

		});
		final List<String> placeholdersList = new ArrayList<>(placeholders);
		Collections.sort(placeholdersList);
		return placeholdersList;
	}

}
