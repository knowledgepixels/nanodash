package org.petapico.nanobench;

import java.util.HashMap;
import java.util.Map;

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
			Map<String,String> params = new HashMap<String,String>();
			for (String k : parameters.getNamedKeys()) {
				if (k.startsWith("param_")) params.put(k.substring(6), parameters.get(k).toString());
			}
			add(new PublishForm("form", templateId, params, this));
		} else {
			add(new TemplateList("form"));
		}
	}

}
