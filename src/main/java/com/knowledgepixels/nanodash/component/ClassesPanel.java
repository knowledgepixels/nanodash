package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public class ClassesPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String classesQueryName = "get-classes-for-thing";

	public ClassesPanel(String markupId, final String thingRef, ApiResponse response) {
		super(markupId);

		if (response.getData().isEmpty()) setVisible(false);
		add(ThingResults.fromApiResponse("classes", "class", response));
	}

	public static Component createComponent(final String markupId, final String thingRef, final String waitMessage) {
		ApiResponse response = ApiCache.retrieveResponse(classesQueryName, getParams(thingRef));
		if (response != null) {
			return new ClassesPanel(markupId, thingRef, response);
		} else {
			ApiResultComponent c = new ApiResultComponent(markupId, classesQueryName, getParams(thingRef)) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new ClassesPanel(markupId, thingRef, response);
				}

			};
			c.setWaitMessage(waitMessage);
			return c;
		}
	}

	private static HashMap<String,String> getParams(String thingRef) {
		final HashMap<String,String> params = new HashMap<>();
		params.put("thing", thingRef);
		return params;
	}

}
