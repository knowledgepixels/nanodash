package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;


public abstract class ApiResultComponent extends AjaxLazyLoadPanel<Component> {

	private static final long serialVersionUID = 1L;

	private final String queryName;
	private final HashMap<String,String> params;

	public ApiResultComponent(String id, String queryName, HashMap<String,String> params) {
		super(id);
		this.queryName = queryName;
		this.params = params;
	}

	@Override
	public Component getLazyLoadComponent(String markupId) {
		ApiResponse r = null;
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			if (!ApiCache.isRunning(queryName, params)) {
				r = ApiCache.retrieveResponse(queryName, params);
				if (r != null) break;
			}
		}
		return getApiResultComponent(markupId, r);
	}

	// TODO Use lambda instead of abstract method?
	public abstract Component getApiResultComponent(String markupId, ApiResponse response);

}
