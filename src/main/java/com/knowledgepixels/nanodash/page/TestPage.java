package com.knowledgepixels.nanodash.page;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.component.ActivityPanel;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.TitleBar;

public class TestPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/test";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public TestPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));

		final String queryName = "get-type-overview-last-12-months";
		final HashMap<String,String> params = new HashMap<>();
		params.put("creator", "https://orcid.org/0000-0002-1267-0234");
		ApiResponse response = ApiCache.retrieveResponse(queryName, params);
		if (response != null) {
			add(new ActivityPanel("activity", response));
		} else {
			add(new ApiResultComponent("activity", queryName, params) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new ActivityPanel(markupId, response);
				}
			});

		}
	}

}
