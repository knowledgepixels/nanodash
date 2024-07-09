package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;

public class GetNamePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/get-name";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GetNamePage(final PageParameters parameters) {
		super(parameters);
		if (parameters.contains("id")) {
			String name = User.getShortDisplayName(Utils.vf.createIRI(parameters.get("id").toString()));
			add(new Label("name", name));
		} else {
			throw new IllegalArgumentException("argument 'id' not found");
		}
	}

}
