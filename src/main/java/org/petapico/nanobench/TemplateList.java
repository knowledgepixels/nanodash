package org.petapico.nanobench;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;

public class TemplateList extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TemplateList(String id) {
		super(id);

		add(new Link<String>("refresh") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				Template.refreshTemplates();
				throw new RestartResponseException(PublishPage.class);
			}

		});

		ArrayList<Template> templateList = new ArrayList<>(Template.getAssertionTemplates());
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
						PageParameters params = new PageParameters();
						params.add("template", item.getModelObject().getId());
						BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("link", PublishPage.class, params);
						l.add(new Label("name", item.getModelObject().getLabel()));
						item.add(l);
						String userString = "somebody";
						try {
							NanopubSignatureElement se = SignatureUtils.getSignatureElement(item.getModelObject().getNanopub());
							if (se != null) {
								User user = User.getUserForPubkey(se.getPublicKeyString());
								if (user != null) userString = user.getShortDisplayName();
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						item.add(new Label("user", userString));
						String timeString = "unknown date";
						Calendar c = getTime(item.getModelObject());
						if (c != null) {
							timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
						}
						item.add(new Label("timestamp", timeString));
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
