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
import com.knowledgepixels.nanodash.page.ThingListPage;

public class ClassesPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String classesQueryName = "get-classes-for-thing";

	public ClassesPanel(String markupId, final String thingRef, ApiResponse response, int limit) {
		super(markupId);
		// TODO Not copying the table here, which can lead to problems if at some point the same result list is sorted differently at different places:
		Collections.sort(response.getData(), new Utils.ApiResponseEntrySorter("date", true));
		if (response.getData().isEmpty()) {
			setVisible(false);
		} else if (response.getData().size() == 1) {
			add(new Label("class-count", "1 class"));
		} else if (response.getData().size() <= limit) {
			add(new Label("class-count", response.getData().size() + " classes"));
		} else if (response.getData().size() == 1000) {
			add(new Label("class-count", "has more classes (>999) than what can be shown here"));
		} else {
			add(new Label("class-count", response.getData().size() + " classes"));
		}
		add(ThingResults.fromApiResponse("classes", "class", response, limit));

		BookmarkablePageLink<Void> showAllLink = new BookmarkablePageLink<Void>("show-all", ThingListPage.class, new PageParameters().add("ref", thingRef).add("mode", "classes"));
		showAllLink.setVisible(limit > 0 && response.getData().size() > limit);
		add(showAllLink);
	}

	public static Component createComponent(final String markupId, final String thingRef, final String waitMessage, final int limit) {
		ApiResponse response = ApiCache.retrieveResponse(classesQueryName, "thing", thingRef);
		if (response != null) {
			return new ClassesPanel(markupId, thingRef, response, limit);
		} else {
			ApiResultComponent c = new ApiResultComponent(markupId, classesQueryName, "thing", thingRef) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new ClassesPanel(markupId, thingRef, response, limit);
				}

			};
			c.setWaitMessage(waitMessage);
			return c;
		}
	}

}
