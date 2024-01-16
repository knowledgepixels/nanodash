package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ReferenceTablePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/reftable";

	
	private final String ref;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ReferenceTablePage(final PageParameters parameters) {
		super(parameters);

		ref = parameters.get("id").toString();
		add(new TitleBar("titlebar", this));
		final String shortName = IriItem.getShortNameFromURI(ref);
		add(new Label("termname", shortName));
		add(new ExternalLink("urilink", ref, ref));
		add(new Label("pagetitle", shortName + " (references) | nanodash"));

		add(new ExploreDataTable("table", ref));
	}

}
