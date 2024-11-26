package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public class InstancesPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String instanceNpQueryName = "get-latest-instance-nps";

	private Integer instanceCount = null;
	private String classRef;

	public InstancesPanel(String markupId, final String classRef) {
		super(markupId);
		this.classRef = classRef;

		retrieveInstanceCount();
		if (instanceCount == null || instanceCount == 0) {
			setVisible(false);
		} else {
			add(new Label("instance-count", instanceCount.toString()));


			final HashMap<String,String> params = getParams(classRef);
			ApiResponse response = ApiCache.retrieveResponse(instanceNpQueryName, params);
			if (response != null) {
				add(ThingResults.fromApiResponse("instance-nanopubs", "instance", response));
			} else {
				add(new ApiResultComponent("instance-nanopubs", instanceNpQueryName, params) {

					private static final long serialVersionUID = 1L;

					@Override
					public Component getApiResultComponent(String markupId, ApiResponse response) {
						return ThingResults.fromApiResponse(markupId, "instance", response);
					}
				});

			}
		}
	}

	private void retrieveInstanceCount() {
		ApiResponse response = null;
		while (response == null) {
			response = ApiCache.retrieveResponse("get-instance-count", "class", classRef);
			if (response != null) {
				instanceCount = Integer.parseInt(response.getData().iterator().next().get("count"));
				return;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static Component createComponent(final String markupId, final String classRef, final String waitMessage) {
		if (ApiCache.retrieveResponse(instanceNpQueryName, getParams(classRef)) != null) {
			return new InstancesPanel(markupId, classRef);
		} else {
			return new AjaxLazyLoadPanel<Component>(markupId) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getLazyLoadComponent(String markupId) {
					return new InstancesPanel(markupId, classRef);
				}

				@Override
				public Component getLoadingComponent(final String id) {
					IRequestHandler handler = new ResourceReferenceRequestHandler(AbstractDefaultAjaxBehavior.INDICATOR);
					return new Label(id, "<p class=\"waiting\">" + waitMessage + " <img alt=\"Loading...\" src=\"" + RequestCycle.get().urlFor(handler) + "\"/></p>").setEscapeModelStrings(false);
				}

			};
		}
	}

	private static HashMap<String,String> getParams(String classRef) {
		final HashMap<String,String> params = new HashMap<>();
		params.put("class", classRef);
		return params;
	}

}
