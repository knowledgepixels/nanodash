package org.petapico;

import org.apache.wicket.protocol.http.WebApplication;

public class WicketApplication extends WebApplication {

	public WicketApplication() {
	}

	public Class<HomePage> getHomePage() {
		return HomePage.class;
	}
	
	@Override
	protected void init() {
		super.init();
		mountBookmarkablePage("/user", UserPage.class);
	}

}
