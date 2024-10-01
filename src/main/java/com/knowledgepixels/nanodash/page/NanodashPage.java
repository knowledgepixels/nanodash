package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.TemplateData;

public abstract class NanodashPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static long lastRefresh = 0l;
	private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
	private static boolean refreshRunning = false;

	private long state = 0l;

	public abstract String getMountPath();


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

	protected boolean hasAutoRefreshEnabled() {
		return false;
	}

	@Override
	protected void onRender() {
		if (hasAutoRefreshEnabled() && state < lastRefresh) {
			throw new RedirectToUrlException(getMountPath() + "?" + Utils.getPageParametersAsString(getPageParameters()));
		}
		super.onRender();
	}

	private static JavaScriptResourceReference nanodashJs = new JavaScriptResourceReference(WicketApplication.class, "script/nanodash.js");

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
		response.render(JavaScriptReferenceHeaderItem.forReference(nanodashJs));
		response.render(JavaScriptHeaderItem.forUrl("/scripts/nanopub.js"));
	}

}
