package org.petapico.nanobench;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

import net.trustyuri.TrustyUriUtils;

public class ExplorePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static final int maxDetailTableCount = 1000000;

	public ExplorePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final String ref = parameters.get("id").toString();
		add(new ExternalLink("urilink", ref, IriItem.getShortNameFromURI(ref)));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", ref);
		try {
			Nanopub np = getAsNanopub(ref);
			List<ApiResponseEntry> usageResponse = ApiAccess.getAll("get_uri_usage", nanopubParams).getData();
			int subjCount = Integer.valueOf(usageResponse.get(0).get("subj"));
			int relCount = Integer.valueOf(usageResponse.get(0).get("pred"));
			int objCount = Integer.valueOf(usageResponse.get(0).get("obj"));
			int classCount = Integer.valueOf(usageResponse.get(0).get("class"));
			int indCount = subjCount + objCount - classCount;
			if (np == null) {
				add(new Label("name", "Term"));
				add(new Label("nanopub", ""));
				add(new Label("counts", "This term is used <strong>" + indCount + "</strong> times as individual,\n" + 
						"<strong>" + classCount + "</strong> times as class, and\n" + 
						"<strong>" + relCount + "</strong> times as relation.").setEscapeModelStrings(false));
			} else {
				add(new Label("name", "Nanopublication"));
				add(new NanopubItem("nanopub", new NanopubElement(np), true));
				add(new Label("counts", ""));
			}

			if (subjCount + relCount + objCount < maxDetailTableCount) {
				add(new AjaxLazyLoadPanel<ExploreDataTable>("tablepart") {

					private static final long serialVersionUID = 1L;
	
					@Override
					public ExploreDataTable getLazyLoadComponent(String id) {
						ExploreDataTable t = new ExploreDataTable(id, ref);
						setResponsePage(getPage());
						return t;
					}

				});
			} else {
				add(new Label("tablepart", "(This term is too frequent to show a detailed table.)"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected static Nanopub getAsNanopub(String uri) {
		if (TrustyUriUtils.isPotentialTrustyUri(uri)) {
			try {
				return Utils.getNanopub(uri);
			} catch (Exception ex) {
				// wasn't a known nanopublication
			}	
		}
		return null;
	}
}
