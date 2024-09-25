package com.knowledgepixels.nanodash.component;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

public class TemplateResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public static TemplateResults fromList(String id, List<Template> templateList) {
		TemplateResults r = new TemplateResults(id);

		r.add(new DataView<Template>("template", new ListDataProvider<Template>(templateList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Template> item) {
				item.add(new TemplateItem("template", item.getModelObject()));
			}

		});

		return r;
	}

	public static TemplateResults fromApiResponse(String id, List<ApiResponseEntry> apiResponse) {
		TemplateResults r = new TemplateResults(id);

		r.add(new DataView<ApiResponseEntry>("template", new ListDataProvider<ApiResponseEntry>(apiResponse)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				Template template = TemplateData.get().getTemplate(item.getModelObject().get("template_np"));
				item.add(new TemplateItem("template", template));
			}

		});

		return r;
	}

	private TemplateResults(String id) {
		super(id);
	}

}
