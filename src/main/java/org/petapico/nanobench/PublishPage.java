package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public PublishPage(final PageParameters parameters) {
		super();
		add(new TitleBar("titlebar"));
		if (!ProfilePage.isComplete()) {
			throw new RedirectToUrlException("./profile");
		}
		String templateId = parameters.get("template").toString();
		if (templateId != null) {
			add(new PublishForm("form", templateId));
		} else {
			add(new TemplateList("form"));
		}
	}

}
