package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.List;

public class TemplateResults extends Panel {

    private static final long serialVersionUID = -5109507637942030910L;

    public static TemplateResults fromList(String id, List<Template> templateList) {
        return fromList(id, templateList, null);
    }

    public static TemplateResults fromList(String id, List<Template> templateList, final PageParameters additionalParams) {
        TemplateResults r = new TemplateResults(id);

        r.add(new DataView<Template>("templates", new ListDataProvider<Template>(templateList)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<Template> item) {
                item.add(new TemplateItem("template", item.getModelObject(), additionalParams));
            }

        });

        return r;
    }

    public static TemplateResults fromApiResponse(String id, ApiResponse apiResponse) {
        return fromApiResponse(id, apiResponse, 0, null);
    }

    public static TemplateResults fromApiResponse(String id, ApiResponse apiResponse, int limit) {
        return fromApiResponse(id, apiResponse, limit, null);
    }

    public static TemplateResults fromApiResponse(final String id, ApiResponse apiResponse, int limit, final PageParameters additionalParams) {
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
