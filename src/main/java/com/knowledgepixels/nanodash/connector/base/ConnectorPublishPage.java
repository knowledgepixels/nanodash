package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.TitleBar;

public abstract class ConnectorPublishPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPublishPage(PageParameters parameters) {
		this(parameters, true);
	}

	public ConnectorPublishPage(PageParameters parameters, boolean doInit) {
		super(parameters);
		if (parameters == null) return;
		if (!doInit) return;
		init(parameters);
	}

	protected void init(PageParameters parameters) {
		final ConnectorNanopubType type = ConnectorNanopubType.get(parameters.get("type").toString());

		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef(getConfig().getSelectPage().getClass(), "Create Nanopublication"),
				new NanodashPageRef("Publish")
			));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		if (parameters.get("template").toString() != null) {
			parameters.add("template-version", "latest");
			add(new PublishForm("form", parameters, getConfig().getPublishPage().getClass(), getConfig().getConnectPage().getClass()));
		} else {
			throw new RuntimeException("no template parameter");
		}

		PageParameters pageParams = new PageParameters();
		pageParams.add("id", type.getExampleId());
		pageParams.add("mode", "final");
		add(new BookmarkablePageLink<Void>("show-example", getConfig().getNanopubPage().getClass(), pageParams));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		// TODO: There is probably a better place to define this function:
		response.render(JavaScriptHeaderItem.forScript(
				"function disableTooltips() { $('.select2-selection__rendered').prop('title', ''); }\n" +
				//"$(document).ready(function() { $('.select2-static').select2(); });",  // for static select2 textfields
				"",
				"custom-functions"));
	}

}
