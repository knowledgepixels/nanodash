package org.petapico.nanobench;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.WebApplication;

public class WicketApplication extends WebApplication {

	public WicketApplication() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI("http://localhost:37373"));
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
		System.err.println("");
		System.err.println("----------------------------------------");
		System.err.println("               nanobench");
		System.err.println("----------------------------------------");
		System.err.println(" Your browser should show the nanobench");
		System.err.println(" interface in a few seconds.");
		System.err.println("");
		System.err.println(" If not, point your browser to:");
		System.err.println(" http://localhost:37373");
		System.err.println("----------------------------------------");
		System.err.println(""); 
	}

	public Class<HomePage> getHomePage() {
		return HomePage.class;
	}
	
	@Override
	protected void init() {
		super.init();
		mountPage("/user", UserPage.class);
//		mountPage("/type", TypePage.class);
		mountPage("/search", SearchPage.class);
		mountPage("/publish", PublishPage.class);
		mountPage("/publishconfirm", PublishConfirmPage.class);
		mountPage("/profile", ProfilePage.class);
		mountPage("/userlist", UserListPage.class);
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}

}
