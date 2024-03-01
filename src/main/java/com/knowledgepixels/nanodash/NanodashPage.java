package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class NanodashPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static long lastRefresh = 0l;
	private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute

	public abstract String getMountPath();

	protected NanodashPage(PageParameters parameters) {
		super(parameters);
		if (System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
			lastRefresh = System.currentTimeMillis();
			new Thread() {

				@Override
				public void run() {
					System.err.println("Refreshing...");
					User.refreshUsers();
					Group.refreshGroups();
					Template.refreshTemplates();
					System.err.println("Refreshing done.");
					lastRefresh = System.currentTimeMillis();
				}

			}.start();
		}
	}

}
