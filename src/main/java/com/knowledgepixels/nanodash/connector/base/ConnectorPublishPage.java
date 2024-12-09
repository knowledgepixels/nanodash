package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class ConnectorPublishPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPublishPage(PageParameters parameters) {
		super(parameters);
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
