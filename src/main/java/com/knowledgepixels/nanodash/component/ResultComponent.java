package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;

public abstract class ResultComponent extends AjaxLazyLoadPanel<Component> {

    private boolean waitIconEnabled = true;
    private String waitMessage = null;
    private String waitComponentHtml = null;

    public ResultComponent(String id) {
        super(id);
    }

    /**
     * Sets whether to show a loading icon while waiting for the API response.
     *
     * @param waitIconEnabled true to enable the wait icon, false to disable it
     */
    public final void setWaitIconEnabled(boolean waitIconEnabled) {
        this.waitIconEnabled = waitIconEnabled;
    }

    /**
     * Sets a custom message to be displayed while waiting for the API response.
     *
     * @param waitMessage the message to display
     */
    public final void setWaitMessage(String waitMessage) {
        this.waitMessage = waitMessage;
    }

    /**
     * Sets a custom HTML component to be displayed while waiting for the API response.
     *
     * @param waitComponentHtml the HTML string to display
     */
    public final void setWaitComponentHtml(String waitComponentHtml) {
        this.waitComponentHtml = waitComponentHtml;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Component getLoadingComponent(String id) {
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

    /**
     * Returns the HTML for a loading icon.
     *
     * @return a string containing the HTML for the loading icon
     */
    public final static String getWaitIconHtml() {
        IRequestHandler handler = new ResourceReferenceRequestHandler(AbstractDefaultAjaxBehavior.INDICATOR);
        return "<img alt=\"Loading...\" src=\"" + RequestCycle.get().urlFor(handler) + "\"/>";
    }

    /**
     * Returns the HTML for a waiting message with an icon.
     *
     * @param waitMessage the message to display while waiting
     * @return a string containing the HTML for the waiting message
     */
    public final static String getWaitComponentHtml(String waitMessage) {
        return "<p class=\"waiting\">" + waitMessage + " " + getWaitIconHtml() + "</p>";
    }

}
