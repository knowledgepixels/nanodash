package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.PublishForm;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConnectPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjPublishPage;

public abstract class ConnectorPublishPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPublishPage(PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		final ConnectorNanopubType type = ConnectorNanopubType.get(parameters.get("type").toString());

		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef(getConfig().getSelectPage().getClass(), "Create Nanopublication"),
				new NanodashPageRef("Publish")
			));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		if (parameters.get("template").toString() != null) {
			parameters.add("template-version", "latest");
			add(new PublishForm("form", parameters, BdjPublishPage.class, BdjConnectPage.class));
		} else {
			throw new RuntimeException("no template parameter");
		}

		PageParameters pageParams = new PageParameters();
		pageParams.add("id", type.getExampleId());
		pageParams.add("mode", "final");
		add(new BookmarkablePageLink<WebPage>("show-example", getConfig().getNanopubPage().getClass(), pageParams));
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
