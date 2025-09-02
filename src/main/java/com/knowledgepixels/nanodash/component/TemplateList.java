package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.template.TemplateData;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * A list of templates, grouped by topic.
 */
public class TemplateList extends Panel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TemplateList.class);

    /**
     * A list of templates, grouped by topic.
     *
     * @param id the wicket id of this component
     */
    public TemplateList(String id) {
        super(id);

        final Map<String, String> ptParams = new HashMap<>();
        final String ptQueryName = "get-most-used-templates-last30d";
        ApiResponse ptResponse = ApiCache.retrieveResponse(ptQueryName, ptParams);
        if (ptResponse != null) {
            add(TemplateResults.fromApiResponse("populartemplates", ptResponse));
        } else {
            add(new AjaxLazyLoadPanel<Component>("populartemplates") {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted", ex);
                        }
                        if (!ApiCache.isRunning(ptQueryName, ptParams)) {
                            r = ApiCache.retrieveResponse(ptQueryName, ptParams);
                            if (r != null) break;
                        }
                    }
                    return TemplateResults.fromApiResponse(markupId, r);
                }

            });
        }

        final Map<String, String> stParams = new HashMap<>();
        final String stQueryName = "get-suggested-templates-to-get-started";
        ApiResponse stResponse = ApiCache.retrieveResponse(stQueryName, stParams);
        if (stResponse != null) {
            add(TemplateResults.fromApiResponse("getstartedtemplates", stResponse));
        } else {
            add(new AjaxLazyLoadPanel<Component>("getstartedtemplates") {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted", ex);
                        }
                        if (!ApiCache.isRunning(stQueryName, stParams)) {
                            r = ApiCache.retrieveResponse(stQueryName, stParams);
                            if (r != null) break;
                        }
                    }
                    return TemplateResults.fromApiResponse(markupId, r);
                }

            });
        }

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
            if ("".equals(tag)) tag = null;
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
        add(new DataView<Topic>("topics", new ListDataProvider<Topic>(topicList)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<Topic> item) {
                String tag = item.getModelObject().tag;
                if (tag == null) tag = "Other";
                item.add(new Label("title", tag));
                item.add(new DataView<ApiResponseEntry>("template-list", new ListDataProvider<ApiResponseEntry>(item.getModelObject().templates)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<ApiResponseEntry> item) {
                        item.add(new TemplateItem("template", item.getModelObject()));
                    }

                });
            }
        });

    }

    private static Calendar getTime(ApiResponseEntry entry) {
        return DatatypeConverter.parseDateTime(entry.get("date"));
    }


    private static class Topic implements Serializable {

        private static final long serialVersionUID = 5919614141679468774L;

        String tag;
        ArrayList<ApiResponseEntry> templates = new ArrayList<>();

        Topic(String tag) {
            this.tag = tag;
        }

    }

}
