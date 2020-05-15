package org.petapico.nanobench;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.WebApplication;
import org.eclipse.rdf4j.common.io.ZipUtil;

public class WicketApplication extends WebApplication {

	private static final String LATEST_RELEASE_URL = "https://github.com/peta-pico/nanobench/releases/latest";

	public WicketApplication() {
		String runParam = System.getProperty("nanobench.run");
		if (runParam != null) {
			if (runParam.equals("update")) {
				System.err.println("");
				System.err.println("----------------------------------------");
				System.err.println("         Updating Nanobench...");
				System.err.println("----------------------------------------");
				System.err.println(" Nanobench is being updated. This might");
				System.err.println(" take a minute.");
				System.err.println("----------------------------------------");
				System.err.println("");
				try {
					HttpResponse resp = HttpClientBuilder.create().build().execute(new HttpGet(LATEST_RELEASE_URL));
					int c = resp.getStatusLine().getStatusCode();
					if (c < 200 || c >= 300) throw new RuntimeException("HTTP error: " + c);
					BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
					while(reader.ready()) {
						String line = reader.readLine();
						if (line.matches(".*/download/nanobench-[0-9]+.[0-9+]/nanobench-[0-9]+.[0-9+].zip.*")) {
							String version = line.replaceFirst(".*/download/nanobench-[0-9]+.[0-9+]/nanobench-([0-9]+.[0-9+]).zip.*", "$1");
							System.err.println("Found latest version: nanobench-" + version);
							String url = "https://github.com/peta-pico/nanobench/releases/download/nanobench-" + version + "/nanobench-" + version + ".zip";
							System.err.println("Downloading " + url);
							HttpResponse resp2 = HttpClientBuilder.create().build().execute(new HttpGet(url));
							int c2 = resp2.getStatusLine().getStatusCode();
							if (c2 < 200 || c2 >= 300) throw new RuntimeException("HTTP error: " + c2);
							File zipFile = new File(System.getProperty("user.dir") + "/nanobench.zip");
							FileOutputStream fileOut = new FileOutputStream(zipFile);
							resp2.getEntity().writeTo(fileOut);
							fileOut.close();
							System.err.println("Unzipping " + zipFile);
							ZipUtil.extract(zipFile, new File(System.getProperty("user.dir")));
							zipFile.delete();
							reader.close();
							System.err.println("");
							System.err.println("----------------------------------------");
							System.err.println(" Nanobench updated successfully");
							System.err.println("----------------------------------------");
							System.err.println("");
							System.exit(0);
						}
					}
					reader.close();
					System.err.println("Failed to update Nanobench");
					System.exit(1);
				} catch (Exception ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
			System.err.println("Unknown command: " + runParam);
			System.exit(1);
		}

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI("http://localhost:37373"));
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
		System.err.println("");
		System.err.println("----------------------------------------");
		System.err.println("               Nanobench");
		System.err.println("----------------------------------------");
		System.err.println(" Your browser should show the Nanobench");
		System.err.println(" interface in a few seconds.");
		System.err.println("");
		System.err.println(" If not, point your browser to:");
		System.err.println(" http://localhost:37373");
		System.err.println("----------------------------------------");
		System.err.println("");

		ProfilePage.loadProfileInfo();
	}

	public Class<HomePage> getHomePage() {
		return HomePage.class;
	}
	
	@Override
	protected void init() {
		super.init();

		getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

		mountPage("/user", UserPage.class);
//		mountPage("/type", TypePage.class);
		mountPage("/search", SearchPage.class);
		mountPage("/publish", PublishPage.class);
		mountPage("/publishconfirm", PublishConfirmPage.class);
		mountPage("/profile", ProfilePage.class);
		mountPage("/userlist", UserListPage.class);
		mountPage("/orcidlinking", OrcidLinkingPage.class);
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}

}
