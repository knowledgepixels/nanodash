package com.knowledgepixels.nanodash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponseEntry;

public class ExplorePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/explore";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private static final int maxDetailTableCount = 1000000;

	public ExplorePage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this));

		final String ref = parameters.get("id").toString();
		final String shortName = IriItem.getShortNameFromURI(ref);
		add(new Label("pagetitle", shortName + " (explore) | nanodash"));
		add(new ExternalLink("urilink", ref, shortName));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("ref", ref);
		try {
			Nanopub np = Utils.getAsNanopub(ref);
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
				add(new NanopubItem("nanopub", new NanopubElement(np)).expand());
				add(new Label("counts", ""));
			}

//			if (subjCount + relCount + objCount < maxDetailTableCount) {
				add(new AjaxLazyLoadPanel<ExploreDataTable>("tablepart") {

					private static final long serialVersionUID = 1L;
	
					@Override
					public ExploreDataTable getLazyLoadComponent(String id) {
						ExploreDataTable t = new ExploreDataTable(id, ref);
						setResponsePage(getPage());
						return t;
					}

				});
//			} else {
//				add(new Label("tablepart", "(This term is too frequent to show a detailed table.)"));
//			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
