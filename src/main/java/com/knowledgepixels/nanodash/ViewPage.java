package com.knowledgepixels.nanodash;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

public class ViewPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/view";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ViewPage(final PageParameters parameters) {
		super(parameters);
		addNanopubItem(this, parameters);
	}

	static void addNanopubItem(NanodashPage page, final PageParameters parameters) {
		final String ref = parameters.get("id").toString();
		Nanopub np = Utils.getAsNanopub(ref);
		page.add(new NanopubItem("nanopub", new NanopubElement(np), false, false).expanded());
	}

}
