package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

public class TemplateResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public static TemplateResults fromList(String id, List<Template> templateList) {
		TemplateResults r = new TemplateResults(id);

		r.add(new DataView<Template>("templates", new ListDataProvider<Template>(templateList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Template> item) {
				item.add(new TemplateItem("template", item.getModelObject()));
			}

		});

		return r;
	}

	public static TemplateResults fromApiResponse(String id, ApiResponse apiResponse) {
		return fromApiResponse(id, apiResponse, 0);
	}

	public static TemplateResults fromApiResponse(final String id, ApiResponse apiResponse, int limit) {
		List<ApiResponseEntry> list = apiResponse.getData();
		if (limit > 0 && list.size() > limit) {
			List<ApiResponseEntry> shortList = new ArrayList<>();
			for (ApiResponseEntry e : list) {
				shortList.add(e);
				if (shortList.size() == limit) break;
			}
			list = shortList;
		}
		TemplateResults r = new TemplateResults(id);

		r.add(new DataView<ApiResponseEntry>("templates", new ListDataProvider<ApiResponseEntry>(list)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				String templateNpField = item.getModelObject().get("template_np");
				if (templateNpField == null) templateNpField = item.getModelObject().get("np");
				Template template = TemplateData.get().getTemplate(templateNpField);
				item.add(new TemplateItem("template", template));
			}

		});

		return r;
	}

	private TemplateResults(String id) {
		super(id);
	}

}
