package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * Base class for all Nanodash pages.
 * This class handles auto-refresh functionality and provides a method to get the mount path.
 */
public abstract class NanodashPage extends WebPage {

    private static final long serialVersionUID = 1L;

    private static long lastRefresh = 0L;
    private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
    private static boolean refreshRunning = false;

    private long state = 0L;

    private static JavaScriptResourceReference nanodashJs = new JavaScriptResourceReference(WicketApplication.class, "script/nanodash.js");

    /**
     * Returns the mount path for this page.
     *
     * @return the mount path as a String
     */
    public abstract String getMountPath();

    /**
     * Constructor for NanodashPage.
     *
     * @param parameters the page parameters
     */
    protected NanodashPage(PageParameters parameters) {
        super(parameters);
        state = lastRefresh;
        if (!refreshRunning && System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
            lastRefresh = System.currentTimeMillis();
            refreshRunning = true;
            new Thread() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    try {
                        System.err.println("Refreshing...");
                        User.refreshUsers();
                        TemplateData.refreshTemplates();
                        System.err.println("Refreshing done.");
                        lastRefresh = System.currentTimeMillis();
                    } finally {
                        refreshRunning = false;
                    }
                }

            }.start();
        }
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     * Override this method in subclasses to enable auto-refresh.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRender() {
        if (hasAutoRefreshEnabled() && state < lastRefresh) {
            throw new RedirectToUrlException(getMountPath() + "?" + Utils.getPageParametersAsString(getPageParameters()));
        }
        super.onRender();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Renders the head section of the page, including JavaScript references.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
        response.render(JavaScriptReferenceHeaderItem.forReference(nanodashJs));
    }

}
