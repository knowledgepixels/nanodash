package com.knowledgepixels.nanodash;

import java.io.Serializable;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.page.NanodashPage;

public class NanodashPageRef implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Class<? extends NanodashPage> pageClass;
	private final PageParameters parameters;
	private final String label;

	public NanodashPageRef(Class<? extends NanodashPage> pageClass, PageParameters parameters, String label) {
		this.pageClass = pageClass;
		this.parameters = parameters;
		this.label = label;
	}

	public NanodashPageRef(Class<? extends NanodashPage> pageClass, String label) {
		this(pageClass, null, label);
	}

	public NanodashPageRef(String label) {
		this(null, null, label);
	}

	public Class<? extends NanodashPage> getPageClass() {
		return pageClass;
	}

	public PageParameters getParameters() {
		return parameters;
	}

	public String getLabel() {
		return label;
	}

	public WebMarkupContainer createComponent(String id) {
		if (pageClass == null) {
			ExternalLink l = new ExternalLink(id, "#");
			l.add(new Label(id + "-label", label));
			return l;
		} else {
			BookmarkablePageLink<Void> l = new BookmarkablePageLink<>(id, pageClass, parameters);
			l.add(new Label(id + "-label", label));
			return l;
		}
	}

}
