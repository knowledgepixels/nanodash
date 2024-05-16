package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class NanodashPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static long lastRefresh = 0l;
	private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute

	private long state = 0l;

	public abstract String getMountPath();

	protected NanodashPage(PageParameters parameters) {
		super(parameters);
		state = lastRefresh;
		if (System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
			lastRefresh = System.currentTimeMillis();
			new Thread() {

				@Override
				public void run() {
					System.err.println("Refreshing...");
					User.refreshUsers();
					Group.refreshGroups();
					TemplateData.refreshTemplates();
					System.err.println("Refreshing done.");
					lastRefresh = System.currentTimeMillis();
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

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
	}

}
