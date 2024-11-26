package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public class ClassesPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String classesQueryName = "get-classes-for-thing";

	public ClassesPanel(String markupId, final String thingRef) {
		super(markupId);

		final HashMap<String,String> params = getParams(thingRef);
		ApiResponse response = ApiCache.retrieveResponse(classesQueryName, params);
		if (response != null) {
			if (response.getData().isEmpty()) setVisible(false);
			add(ThingResults.fromApiResponse("classes", "class", response));
		} else {
			add(new ApiResultComponent("classes", classesQueryName, params) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					if (response.getData().isEmpty()) ClassesPanel.this.setVisible(false);
					return ThingResults.fromApiResponse(markupId, "class", response);
				}
			});

		}
	}

	public static Component createComponent(final String markupId, final String thingRef, final boolean showWaitIcon) {
		if (ApiCache.retrieveResponse(classesQueryName, getParams(thingRef)) != null) {
			return new ClassesPanel(markupId, thingRef);
		} else {
			return new AjaxLazyLoadPanel<Component>(markupId) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getLazyLoadComponent(String markupId) {
					return new ClassesPanel(markupId, thingRef);
				}

				@Override
				public Component getLoadingComponent(final String id) {
					if (showWaitIcon) return super.getLoadingComponent(id);
					return new Label(id);
				}

			};
		}
	}

	private static HashMap<String,String> getParams(String thingRef) {
		final HashMap<String,String> params = new HashMap<>();
		params.put("thing", thingRef);
		return params;
	}

}
