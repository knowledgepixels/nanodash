package org.petapico;

import org.apache.wicket.RuntimeConfigurationType;
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
		mountPage("/user", UserPage.class);
		mountPage("/type", TypePage.class);
		mountPage("/search", FreeTextSearchPage.class);
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}

}
