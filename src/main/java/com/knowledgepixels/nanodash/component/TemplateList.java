package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

public class TemplateList extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TemplateList(String id) {
		super(id);

		final Map<String,String> ptParams = new HashMap<>();
		final String ptQueryName = "get-most-used-templates-last30d";
		List<ApiResponseEntry> ptResponse = ApiCache.retrieveNanopubList(ptQueryName, ptParams);
		if (ptResponse != null) {
			add(TemplateResults.fromApiResponse("populartemplates", ptResponse));
		} else {
			add(new AjaxLazyLoadPanel<Component>("populartemplates") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<ApiResponseEntry> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning(ptQueryName, ptParams)) {
							l = ApiCache.retrieveNanopubList(ptQueryName, ptParams);
							if (l != null) break;
						}
					}
					return TemplateResults.fromApiResponse(markupId, l);
				}
	
			});
		}

		final Map<String,String> stParams = new HashMap<>();
		final String stQueryName = "get-suggested-templates-to-get-started";
		List<ApiResponseEntry> stResponse = ApiCache.retrieveNanopubList(stQueryName, stParams);
		if (stResponse != null) {
			add(TemplateResults.fromApiResponse("getstartedtemplates", stResponse));
		} else {
			add(new AjaxLazyLoadPanel<Component>("getstartedtemplates") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					List<ApiResponseEntry> l = null;
					while (true) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						if (!ApiCache.isRunning(stQueryName, stParams)) {
							l = ApiCache.retrieveNanopubList(stQueryName, stParams);
							if (l != null) break;
						}
					}
					return TemplateResults.fromApiResponse(markupId, l);
				}
	
			});
		}

		ArrayList<Template> templateList = new ArrayList<>(TemplateData.get().getAssertionTemplates());
		Collections.sort(templateList, new Comparator<Template>() {
			@Override
			public int compare(Template t1, Template t2) {
				Calendar c1 = getTime(t1);
				Calendar c2 = getTime(t2);
				if (c1 == null && c2 == null) return 0;
				if (c1 == null) return 1;
				if (c2 == null) return -1;
				return c2.compareTo(c1);
			}
		});

		Map<String,Topic> topics = new HashMap<>();
		for (Template t : templateList) {
			String tag = t.getTag();
			if (!topics.containsKey(tag)) {
				topics.put(tag, new Topic(tag));
			}
			topics.get(tag).templates.add(t);

		}
		ArrayList<Topic> topicList = new ArrayList<Topic>(topics.values());
		topicList.sort(new Comparator<Topic>() {

			@Override
			public int compare(Topic t0, Topic t1) {
				if (t0.tag == null) return 1;
				if (t1.tag == null) return -1;
				return t1.templates.size() - t0.templates.size();
			}

		});
		add(new DataView<Topic>("topics", new ListDataProvider<Topic>(topicList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Topic> item) {
				String tag = item.getModelObject().tag;
				if (tag == null) tag = "Other";
				item.add(new Label("title", tag));
				item.add(new DataView<Template>("template-list", new ListDataProvider<Template>(item.getModelObject().templates)) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(Item<Template> item) {
						item.add(new TemplateItem("template", item.getModelObject()));
					}

				});
			}
		});
		
	}

	private static Calendar getTime(Template template) {
		return SimpleTimestampPattern.getCreationTime(template.getNanopub());
	}


	private static class Topic implements Serializable {

		private static final long serialVersionUID = 5919614141679468774L;

		String tag;
		ArrayList<Template> templates = new ArrayList<>();

		Topic(String tag) {
			this.tag = tag;
		}

	}

}
