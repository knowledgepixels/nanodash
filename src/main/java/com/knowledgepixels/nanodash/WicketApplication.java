package com.knowledgepixels.nanodash;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.eclipse.rdf4j.common.io.ZipUtil;

import com.knowledgepixels.nanodash.connector.test.ConnectorNanopubTestPage;
import com.knowledgepixels.nanodash.connector.test.ConnectorTestPage;

public class WicketApplication extends WebApplication {

	protected static final String LATEST_RELEASE_URL = "https://api.github.com/repos/knowledgepixels/nanodash/releases";

	public WicketApplication() {
		String runParam = System.getProperty("nanodash.run");
		if (runParam != null) {
			if (runParam.equals("update")) {
				System.err.println("");
				System.err.println("----------------------------------------");
				System.err.println("         Updating Nanodash...");
				System.err.println("----------------------------------------");
				System.err.println(" Nanodash is being updated. This might");
				System.err.println(" take a minute.");
				System.err.println("----------------------------------------");
				System.err.println("");
				try {
					String version = getLatestVersion();
					System.err.println("Found latest version: nanodash-" + version);
					String url = "https://github.com/knowledgepixels/nanodash/releases/download/nanodash-" + version + "/nanodash-" + version + ".zip";
					System.err.println("Downloading " + url);
					HttpResponse resp2 = HttpClientBuilder.create().build().execute(new HttpGet(url));
					int c2 = resp2.getStatusLine().getStatusCode();
					if (c2 < 200 || c2 >= 300) throw new RuntimeException("HTTP error: " + c2);
					File zipFile = new File(System.getProperty("user.dir") + "/nanodash.zip");
					FileOutputStream fileOut = new FileOutputStream(zipFile);
					resp2.getEntity().writeTo(fileOut);
					fileOut.close();
					System.err.println("Unzipping " + zipFile);
					ZipUtil.extract(zipFile, new File(System.getProperty("user.dir")));
					zipFile.delete();
					System.err.println("");
					System.err.println("----------------------------------------");
					System.err.println(" Nanodash updated successfully");
					System.err.println("----------------------------------------");
					System.err.println("");
					System.exit(0);
				} catch (Exception ex) {
					ex.printStackTrace();
					System.err.println("Failed to update Nanodash");
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
		String v = getThisVersion();
		String lv = getLatestVersion();
		System.err.println("");
		System.err.println("----------------------------------------");
		System.err.println("               Nanodash");
		System.err.println("----------------------------------------");
		System.err.println(" You are using version: " + v);
		System.err.println(" Latest public version: " + lv);
		System.err.println("----------------------------------------");
		System.err.println(" Your browser should show the Nanodash");
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

		getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

		mountPage(UserPage.MOUNT_PATH, UserPage.class);
		mountPage(SearchPage.MOUNT_PATH, SearchPage.class);
		mountPage(ExplorePage.MOUNT_PATH, ExplorePage.class);
		mountPage(PublishPage.MOUNT_PATH, PublishPage.class);
		mountPage(PublishConfirmPage.MOUNT_PATH, PublishConfirmPage.class);
		mountPage(ProfilePage.MOUNT_PATH, ProfilePage.class);
		mountPage(UserListPage.MOUNT_PATH, UserListPage.class);
		mountPage(OrcidLinkingPage.MOUNT_PATH, OrcidLinkingPage.class);
		mountPage(OrcidLoginPage.MOUNT_PATH, OrcidLoginPage.class);
		mountPage(ConnectorTestPage.MOUNT_PATH, ConnectorTestPage.class);
		mountPage(ConnectorNanopubTestPage.MOUNT_PATH, ConnectorNanopubTestPage.class);
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsTypePage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.FcOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.FcNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.RioOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.BdjOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage");

		getCspSettings().blocking().disabled();
	}

	private void tryToMountPage(String pageClassName) {
		try {
			@SuppressWarnings("unchecked")
			Class<WebPage> pageClass = (Class<WebPage>) Class.forName(pageClassName);
			String mountPath = pageClass.getField("MOUNT_PATH").get(null).toString();
			System.err.println("Mounting extra page: " + mountPath);
			mountPage(mountPath, pageClass);
		} catch (ClassNotFoundException ex) {
			// ignore
		} catch (ClassCastException | NoSuchFieldException | IllegalAccessException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}


	private static String latestVersion = null;

	protected static String getLatestVersion() {
		if (latestVersion != null) return latestVersion;
		BufferedReader reader = null;
		try {
			HttpResponse resp = HttpClientBuilder.create().build().execute(new HttpGet(LATEST_RELEASE_URL));
			int c = resp.getStatusLine().getStatusCode();
			if (c < 200 || c >= 300) throw new RuntimeException("HTTP error: " + c);
			reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
			while(reader.ready()) {
				String line = reader.readLine();
				// TODO: Do proper JSON parsing
				if (line.matches(".*\"tag_name\":\\s*\"nanodash-[0-9]+\\.[0-9]+\".*")) {
					latestVersion = line.replaceFirst(".*?\"tag_name\":\\s*\"nanodash-([0-9]+\\.[0-9]+)\".*", "$1");
					break;
				}
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return latestVersion;
	}

	protected final static Properties properties = new Properties();

	static {
		try {
			properties.load(WicketApplication.class.getClassLoader().getResourceAsStream("nanodash.properties"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected static String getThisVersion() {
		return properties.getProperty("nanodash.version");
	}

	@Override
	public Session newSession(Request request, Response response) {
		return new NanodashSession(request);
	}

}