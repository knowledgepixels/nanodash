package com.knowledgepixels.nanodash.component;

import java.util.Collections;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.InstancesPage;

public class InstancesPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String instancesQueryName = "get-instances";

	public InstancesPanel(String markupId, final String classRef, ApiResponse response, int limit) {
		super(markupId);
		// TODO Not copying the table here, which can lead to problems if at some point the same result list is sorted differently at different places:
		Collections.sort(response.getData(), new Utils.ApiResponseEntrySorter("date", true));
		if (response.getData().isEmpty()) {
			setVisible(false);
		} else if (response.getData().size() == 1) {
			add(new Label("instance-count", "1 instance:"));
		} else if (response.getData().size() <= limit) {
			add(new Label("instance-count", response.getData().size() + " instances:"));
		} else if (response.getData().size() == 1000) {
			add(new Label("instance-count", "has more instances (>999) than what can be shown here:"));
		} else {
			add(new Label("instance-count", response.getData().size() + " instances:"));
		}
		add(ThingResults.fromApiResponse("instances", "instance", response, limit));

		BookmarkablePageLink<Void> showAllLink = new BookmarkablePageLink<Void>("show-all", InstancesPage.class, new PageParameters().add("class", classRef));
		showAllLink.setVisible(limit > 0 && response.getData().size() > limit);
		add(showAllLink);
	}

	public static Component createComponent(final String markupId, final String classRef, final String waitMessage, final int limit) {
		ApiResponse response = ApiCache.retrieveResponse(instancesQueryName, "class", classRef);
		if (response != null) {
			return new InstancesPanel(markupId, classRef, response, limit);
		} else {
			ApiResultComponent c = new ApiResultComponent(markupId, instancesQueryName, "class", classRef) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new InstancesPanel(markupId, classRef, response, limit);
				}

			};
			c.setWaitMessage(waitMessage);
			return c;
		}
	}

}
