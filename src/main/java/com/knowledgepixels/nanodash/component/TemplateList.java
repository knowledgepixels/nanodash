package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

import jakarta.xml.bind.DatatypeConverter;

/**
 * A list of templates, grouped by topic.
 */
public class TemplateList extends Panel {

    /**
     * A list of templates, grouped by topic.
     *
     * @param id the wicket id of this component
     */
    public TemplateList(String id) {
        super(id);
        setOutputMarkupId(true);

        add(new ItemListPanel<Template>(
                "popular-templates",
                "Popular Templates",
                new QueryRef(QueryApiAccess.GET_MOST_USED_TEMPLATES_LAST30D),
                TemplateData::getTemplateList,
                (template) -> new TemplateItem("item", template)
        ));

        add(new ItemListPanel<Template>(
                "getstarted-templates",
                "Suggested Templates to Get Started",
                new QueryRef(QueryApiAccess.GET_SUGGESTED_TEMPLATES_TO_GET_STARTED),
                TemplateData::getTemplateList,
                (template) -> new TemplateItem("item", template)
        ));

        ArrayList<ApiResponseEntry> templateList = new ArrayList<>(TemplateData.get().getAssertionTemplates());
        templateList.sort((t1, t2) -> {
            Calendar c1 = getTime(t1);
            Calendar c2 = getTime(t2);
            if (c1 == null && c2 == null) return 0;
            if (c1 == null) return 1;
            if (c2 == null) return -1;
            return c2.compareTo(c1);
        });

        Map<String, Topic> topics = new HashMap<>();
        for (ApiResponseEntry entry : templateList) {
            String tag = entry.get("tag");
            if (tag.isEmpty()) tag = null;
            if (!topics.containsKey(tag)) {
                topics.put(tag, new Topic(tag));
            }
            topics.get(tag).templates.add(entry);

        }
        ArrayList<Topic> topicList = new ArrayList<Topic>(topics.values());
        topicList.sort((t0, t1) -> {
            if (t0.tag == null) return 1;
            if (t1.tag == null) return -1;
            return t1.templates.size() - t0.templates.size();
        });
        DataView<Topic> topicDataView = new DataView<Topic>("topics", new ListDataProvider<Topic>(topicList)) {

            @Override
            protected void populateItem(Item<Topic> item) {
                String tag = item.getModelObject().tag;
                if (tag == null) tag = "Other";
                item.add(new ItemListPanel<ApiResponseEntry>(
                        "templates",
                        tag,
                        item.getModelObject().templates,
                        (respEntry) -> new TemplateItem("item", respEntry)
                ));
            }
        };
        topicDataView.setOutputMarkupId(true);
        add(topicDataView);
    }

    private static Calendar getTime(ApiResponseEntry entry) {
        return DatatypeConverter.parseDateTime(entry.get("date"));
    }


    private static class Topic implements Serializable {

        String tag;
        ArrayList<ApiResponseEntry> templates = new ArrayList<>();

        Topic(String tag) {
            this.tag = tag;
        }

    }

}
