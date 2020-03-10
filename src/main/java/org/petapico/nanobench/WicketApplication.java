package org.petapico.nanobench;

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
		mountPage("/search", SearchPage.class);
		mountPage("/publish", PublishPage.class);
		mountPage("/publishconfirm", PublishConfirmPage.class);
		mountPage("/profile", ProfilePage.class);
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}

}
