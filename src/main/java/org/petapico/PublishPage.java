package org.petapico;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public PublishPage(final PageParameters parameters) {
		super();
		String templateId = parameters.get("template").toString();
		if (templateId != null) {
			System.err.println("PARA: " + templateId);
			add(new PublishForm("form", templateId));
		} else {
			System.err.println("FOO");
			add(new Label("form", "foobar"));
		}
	}

}
