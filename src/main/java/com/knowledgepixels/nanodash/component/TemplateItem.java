package com.knowledgepixels.nanodash.component;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;

public class TemplateItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TemplateItem(String id, Template template) {
		super(id);

		PageParameters params = new PageParameters();
		params.add("template", template.getId());
		params.add("template-version", "latest");
		BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("link", PublishPage.class, params);
		l.add(new Label("name", template.getLabel()));
		add(l);
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
		add(new Label("user", userString));
		String timeString = "unknown date";
		Calendar c = SimpleTimestampPattern.getCreationTime(template.getNanopub());
		if (c != null) {
			timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
		}
		add(new Label("timestamp", timeString));

	}

}
