package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publish";

	public PublishPage(final PageParameters parameters) {
		super();
		final NanodashSession session = NanodashSession.get();
		add(new TitleBar("titlebar"));
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
				"function disableTooltips() { $('.select2-selection__rendered').prop('title', ''); }",
				"custom-functions"));
	}

}
