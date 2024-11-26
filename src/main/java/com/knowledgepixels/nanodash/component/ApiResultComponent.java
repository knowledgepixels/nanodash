package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public abstract class ApiResultComponent extends AjaxLazyLoadPanel<Component> {

	private static final long serialVersionUID = 1L;

	private final String queryName;
	private final HashMap<String,String> params;
	private boolean waitIconEnabled = true;
	private ApiResponse response = null;
	private String waitMessage = null;

	public ApiResultComponent(String id, String queryName, HashMap<String,String> params) {
		super(id);
		this.queryName = queryName;
		this.params = params;
	}

	public void setWaitIconEnabled(boolean waitIconEnabled) {
		this.waitIconEnabled = waitIconEnabled;
	}

	public void setWaitMessage(String waitMessage) {
		this.waitMessage = waitMessage;
	}

	@Override
	public Component getLazyLoadComponent(String markupId) {
		while (true) {
			if (!ApiCache.isRunning(queryName, params)) {
				response = ApiCache.retrieveResponse(queryName, params);
				if (response != null) break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		return getApiResultComponent(markupId, response);
	}

	@Override
	public Component getLoadingComponent(String id) {
		if (!waitIconEnabled) {
			return new Label(id);
		} else if (waitMessage != null) {
			IRequestHandler handler = new ResourceReferenceRequestHandler(AbstractDefaultAjaxBehavior.INDICATOR);
			return new Label(id, "<p class=\"waiting\">" + waitMessage + " <img alt=\"Loading...\" src=\"" + RequestCycle.get().urlFor(handler) + "\"/></p>").setEscapeModelStrings(false);
		} else {
			return super.getLoadingComponent(id);
		}
	}

	@Override
	protected boolean isContentReady() {
		return response != null || !ApiCache.isRunning(queryName, params);
	}

	// TODO Use lambda instead of abstract method?
	public abstract Component getApiResultComponent(String markupId, ApiResponse response);

}
