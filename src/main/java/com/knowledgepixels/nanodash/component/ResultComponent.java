package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;

/**
 * A base class for components that display results from an API call with a loading indicator.
 * This class extends AjaxLazyLoadPanel to provide lazy loading functionality.
 */
public abstract class ResultComponent extends AjaxLazyLoadPanel<Component> {

    private boolean waitIconEnabled = true;
    private String waitMessage = null;
    private String waitComponentHtml = null;

    /**
     * Constructor.
     *
     * @param id the component id
     */
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
            return getDefaultLoadingComponent(id);
        }
    }

    /**
     * The loading state shown when neither a wait message nor custom markup was set: the
     * bare spinner. Subclasses override this to show something more specific — see
     * {@link ApiResultComponent}, which puts the view's title beside it.
     *
     * @param id the component id
     * @return the component to show while waiting
     */
    protected Component getDefaultLoadingComponent(String id) {
        return new Label(id, getStandaloneWaitIconHtml()).setEscapeModelStrings(false);
    }

    /**
     * Returns the HTML for the loading icon at the size it takes when it sits next to
     * something — a wait message, a panel title — where the neighbouring text carries the
     * eye and the spinner only has to confirm that something is happening.
     *
     * @return a string containing the HTML for the loading icon
     */
    public static String getWaitIconHtml() {
        return "<span class=\"refresh-spinner\" title=\"Updating...\"></span>";
    }

    /**
     * Returns the HTML for the loading icon when it stands alone — a whole page section or
     * panel body with nothing in it yet. Same spinner, drawn larger, because here it is the
     * only thing on that stretch of the page and a small one is easy to miss.
     *
     * @return a string containing the HTML for the loading icon
     */
    public static String getStandaloneWaitIconHtml() {
        return "<span class=\"refresh-spinner standalone\" title=\"Loading...\"></span>";
    }

    /**
     * Returns the HTML for a whole page section that is still loading, as the pages' own
     * lazy-loading panels show it.
     *
     * @return a string containing the HTML for the loading section
     */
    public static String getSectionWaitHtml() {
        return "<div class=\"row-section\"><div class=\"col-12\">" + getStandaloneWaitIconHtml() + "</div></div>";
    }

    /**
     * Returns the HTML for a waiting message with an icon.
     *
     * @param waitMessage the message to display while waiting
     * @return a string containing the HTML for the waiting message
     */
    public static String getWaitComponentHtml(String waitMessage) {
        if (waitMessage == null || waitMessage.isBlank()) {
            return "<p class=\"waiting nomessage\">" + getStandaloneWaitIconHtml() + "</p>";
        }
        return "<p class=\"waiting\">" + waitMessage + " " + getWaitIconHtml() + "</p>";
    }

}
