package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publish";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public PublishPage(final PageParameters parameters) {
		super(parameters);

		final NanodashSession session = NanodashSession.get();
		add(new TitleBar("titlebar", this));
		if (parameters.get("template").toString() != null) {
			if (!parameters.get("sigkey").isNull() && !parameters.get("sigkey").toString().equals(session.getPubkeyString())) {
				add(new DifferentKeyErrorItem("form", parameters));
			} else {
				session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);
				add(new PublishForm("form", parameters, this));
			}
		} else {
			add(new TemplateList("form"));
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		// TODO: There is probably a better place to define this function:
		response.render(JavaScriptHeaderItem.forScript(
				"function disableTooltips() { $('.select2-selection__rendered').prop('title', ''); }\n" +
				//"$(document).ready(function() { $('.select2-static').select2(); });",  // for static select2 textfields
				"",
				"custom-functions"));
	}

}
