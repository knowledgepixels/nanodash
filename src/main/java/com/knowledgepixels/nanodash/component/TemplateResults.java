package com.knowledgepixels.nanodash.component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

public class TemplateResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public TemplateResults(String id, List<ApiResponseEntry> response) {
		super(id);

		add(new DataView<ApiResponseEntry>("template", new ListDataProvider<ApiResponseEntry>(response)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				// TODO Move this to separate class together with code in TemplateList
				Template template = TemplateData.get().getTemplate(item.getModelObject().get("template_np"));
				PageParameters params = new PageParameters();
				params.add("template", template.getId());
				BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("link", PublishPage.class, params);
				l.add(new Label("name", template.getLabel()));
				item.add(l);
				String userString = "somebody";
				try {
					NanopubSignatureElement se = SignatureUtils.getSignatureElement(template.getNanopub());
					if (se != null) {
						IRI signer = (se.getSigners().isEmpty() ? null : se.getSigners().iterator().next());
						userString = User.getShortDisplayName(signer, se.getPublicKeyString());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				item.add(new Label("user", userString));
				String timeString = "unknown date";
				Calendar c = SimpleTimestampPattern.getCreationTime(template.getNanopub());
				if (c != null) {
					timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
				}
				item.add(new Label("timestamp", timeString));
			}

		});
	}

}
