package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashThreadPool;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.*;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Abstract base class for Nanodash pages.
 * Provides functionality for auto-refreshing data and rendering JavaScript resources.
 */
public abstract class NanodashPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(NanodashPage.class);

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
        ensureRefreshed();
    }

    private void ensureRefreshed() {
        synchronized (getClass()) {
            state = lastRefresh;
            if (!refreshRunning && System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
                lastRefresh = System.currentTimeMillis();
                refreshRunning = true;
                // Mark resource profiles as needing update synchronously, so page
                // constructors see dataInitialized=false before the redirect:
                AbstractResourceWithProfile.refresh();
                NanodashThreadPool.submit(() -> {
                    try {
                        logger.info("Refreshing data...");
                        User.refreshUsers();
                        TemplateData.refreshTemplates();
                        Space.refresh();
                        MaintainedResource.refresh();
                        Project.refresh();
                        logger.info("Refreshing data... done");
                        lastRefresh = System.currentTimeMillis();
                    } catch (Exception ex) {
                        logger.error("Error during refresh", ex);
                    } finally {
                        refreshRunning = false;
                    }
                });
            }
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
        String version = ResourceBundle.getBundle("nanodash").getString("nanodash.version");
        response.render(CssHeaderItem.forUrl("style.css?v=" + version));
        response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
        response.render(JavaScriptReferenceHeaderItem.forReference(nanodashJs));
        String umamiScriptUrl = NanodashPreferences.get().getUmamiScriptUrl();
        String umamiWebsiteId = NanodashPreferences.get().getUmamiWebsiteId();
        if (umamiScriptUrl != null && !umamiScriptUrl.isBlank()
            && umamiWebsiteId != null && !umamiWebsiteId.isBlank()) {
            String umamiJs = "(function(){" +
                             "var s=document.createElement('script');" +
                             "s.src='" + umamiScriptUrl + "';" +
                             "s.defer=true;" +
                             "s.setAttribute('data-website-id','" + umamiWebsiteId + "');" +
                             "document.head.appendChild(s);" +
                             "})();";
            response.render(JavaScriptHeaderItem.forScript(umamiJs, "umami-loader"));
        }
    }

}
