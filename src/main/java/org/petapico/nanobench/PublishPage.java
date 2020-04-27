package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private boolean localFileMode = false;

	public PublishPage(final PageParameters parameters) {
		super();
		add(new TitleBar("titlebar"));
		if (!ProfilePage.isComplete()) {
			throw new RedirectToUrlException("./profile");
		}
		String templateId = parameters.get("template").toString();
		if (templateId != null) {
			add(new PublishForm("form", templateId));
			if (templateId.startsWith("file://")) localFileMode = true;
		} else {
			add(new TemplateList("form"));
		}
		if (localFileMode) {
			add(new Link<Object>("local-reload-link") {
				private static final long serialVersionUID = 1L;
				public void onClick() {
					setResponsePage(getPageClass(), getPageParameters());
				};
			});
		} else {
			Label l = new Label("local-reload-link", "");
			l.setVisible(false);
			add(l);
		}
	}

}
