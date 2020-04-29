package org.petapico.nanobench;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

		List<Template> templateList = new ArrayList<>(Template.getTemplates());
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

		add(new DataView<Template>("list", new ListDataProvider<Template>(templateList)) {

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

	private static Calendar getTime(Template template) {
		return SimpleTimestampPattern.getCreationTime(template.getNanopub());
	}

}
