package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.nanopub.extra.services.ApiResponse;

import java.util.HashMap;

/**
 * A component that retrieves and displays the result of an API call.
 * It uses AjaxLazyLoadPanel to load the content lazily and shows a loading indicator while waiting for the response.
 */
public abstract class ApiResultComponent extends AjaxLazyLoadPanel<Component> {

    private static final long serialVersionUID = 1L;

    private final String queryName;
    private final HashMap<String, String> params;
    private boolean waitIconEnabled = true;
    private ApiResponse response = null;
    private String waitMessage = null;
    private String waitComponentHtml = null;

    /**
     * Constructor for ApiResultComponent.
     *
     * @param id        the component id
     * @param queryName the name of the API query to be executed
     * @param params    a map of parameters to be passed to the API query
     */
    public ApiResultComponent(String id, String queryName, HashMap<String, String> params) {
        super(id);
        this.queryName = queryName;
        this.params = params;
    }

    /**
     * Constructor for ApiResultComponent with a single parameter.
     *
     * @param id         the component id
     * @param queryName  the name of the API query to be executed
     * @param paramKey   the key of the parameter to be passed to the API query
     * @param paramValue the value of the parameter to be passed to the API query
     */
    public ApiResultComponent(String id, String queryName, String paramKey, String paramValue) {
        this(id, queryName, getParams(paramKey, paramValue));
    }

    private static HashMap<String, String> getParams(String paramKey, String paramValue) {
        final HashMap<String, String> params = new HashMap<>();
        params.put(paramKey, paramValue);
        return params;
    }

    /**
     * Sets whether to show a loading icon while waiting for the API response.
     *
     * @param waitIconEnabled true to enable the wait icon, false to disable it
     */
    public void setWaitIconEnabled(boolean waitIconEnabled) {
        this.waitIconEnabled = waitIconEnabled;
    }

    /**
     * Sets a custom message to be displayed while waiting for the API response.
     *
     * @param waitMessage the message to display
     */
    public void setWaitMessage(String waitMessage) {
        this.waitMessage = waitMessage;
    }

    /**
     * Sets a custom HTML component to be displayed while waiting for the API response.
     *
     * @param waitComponentHtml the HTML string to display
     */
    public void setWaitComponentHtml(String waitComponentHtml) {
        this.waitComponentHtml = waitComponentHtml;
    }

    @Override
    public Component getLazyLoadComponent(String markupId) {
        while (true) {
            if (!ApiCache.isRunning(queryName, params)) {
                try {
                    response = ApiCache.retrieveResponse(queryName, params);
                    if (response != null) break;
                } catch (Exception ex) {
                    return new Label(markupId, "<span class=\"negative\">API call failed.</span>").setEscapeModelStrings(false);
                }
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
        } else if (waitComponentHtml != null) {
            return new Label(id, waitComponentHtml).setEscapeModelStrings(false);
        } else if (waitMessage != null) {
            return new Label(id, getWaitComponentHtml(waitMessage)).setEscapeModelStrings(false);
        } else {
            return super.getLoadingComponent(id);
        }
    }

    @Override
    protected boolean isContentReady() {
        return response != null || !ApiCache.isRunning(queryName, params);
    }

    /**
     * Abstract method to be implemented by subclasses to provide the component that displays the API result.
     *
     * @param markupId the markup ID for the component
     * @param response the API response to display
     * @return a Component that displays the API result
     */
    // TODO Use lambda instead of abstract method?
    public abstract Component getApiResultComponent(String markupId, ApiResponse response);

    /**
     * Returns the HTML for a loading icon.
     *
     * @return a string containing the HTML for the loading icon
     */
    public static String getWaitIconHtml() {
        IRequestHandler handler = new ResourceReferenceRequestHandler(AbstractDefaultAjaxBehavior.INDICATOR);
        return "<img alt=\"Loading...\" src=\"" + RequestCycle.get().urlFor(handler) + "\"/>";
    }

    /**
     * Returns the HTML for a waiting message with an icon.
     *
     * @param waitMessage the message to display while waiting
     * @return a string containing the HTML for the waiting message
     */
    public static String getWaitComponentHtml(String waitMessage) {
        return "<p class=\"waiting\">" + waitMessage + " " + getWaitIconHtml() + "</p>";
    }

}
