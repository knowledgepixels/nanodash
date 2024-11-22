package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public class ClassPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private static final String instanceNpQueryName = "get-latest-instance-nps";

	private Integer instanceCount = null;
	private String classRef;

	public ClassPanel(String markupId, final String classRef) {
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
				add(InstanceResults.fromApiResponse("instance-nanopubs", response));
			} else {
				add(new ApiResultComponent("instance-nanopubs", instanceNpQueryName, params) {

					private static final long serialVersionUID = 1L;

					@Override
					public Component getApiResultComponent(String markupId, ApiResponse response) {
						return InstanceResults.fromApiResponse(markupId, response);
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

	public static boolean isReady(String classRef) {
		return ApiCache.retrieveResponse(instanceNpQueryName, getParams(classRef)) != null;
	}

	private static HashMap<String,String> getParams(String classRef) {
		final HashMap<String,String> params = new HashMap<>();
		params.put("class", classRef);
		return params;
	}

}
