package com.knowledgepixels.nanodash;

import java.awt.Desktop;
import java.io.BufferedReader;
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
import org.apache.wicket.settings.ExceptionSettings;

public class WicketApplication extends WebApplication {

	protected static final String LATEST_RELEASE_URL = "https://api.github.com/repos/knowledgepixels/nanodash/releases";

	public WicketApplication() {
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

		getExceptionSettings().setUnexpectedExceptionDisplay(ExceptionSettings.SHOW_NO_EXCEPTION_PAGE);

		mountPage(ErrorPage.MOUNT_PATH, ErrorPage.class);
		mountPage("/error/404", ErrorPage.class);
		mountPage("/error/500", ErrorPage.class);

		mountPage(UserPage.MOUNT_PATH, UserPage.class);
		mountPage(SearchPage.MOUNT_PATH, SearchPage.class);
		mountPage(ExplorePage.MOUNT_PATH, ExplorePage.class);
		mountPage(ReferenceTablePage.MOUNT_PATH, ReferenceTablePage.class);
		mountPage(PublishPage.MOUNT_PATH, PublishPage.class);
		mountPage(PublishConfirmPage.MOUNT_PATH, PublishConfirmPage.class);
		mountPage(ProfilePage.MOUNT_PATH, ProfilePage.class);
		mountPage(UserListPage.MOUNT_PATH, UserListPage.class);
		mountPage(GroupListPage.MOUNT_PATH, GroupListPage.class);
		mountPage(GroupPage.MOUNT_PATH, GroupPage.class);
		mountPage(GroupDemoPage.MOUNT_PATH, GroupDemoPage.class);
		mountPage(GroupDemoPageSoc.MOUNT_PATH, GroupDemoPageSoc.class);
		mountPage(OrcidLinkingPage.MOUNT_PATH, OrcidLinkingPage.class);
		mountPage(OrcidLoginPage.MOUNT_PATH, OrcidLoginPage.class);
		mountPage(ConnectorListPage.MOUNT_PATH, ConnectorListPage.class);
		mountPage(MyChannelPage.MOUNT_PATH, MyChannelPage.class);
		mountPage(TermForwarder.MOUNT_PATH, TermForwarder.class);
		mountPage(ViewPage.MOUNT_PATH, ViewPage.class);
		mountPage(GetViewPage.MOUNT_PATH, GetViewPage.class);
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsTypePage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.DsNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.FcOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.FcTypePage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.ios.FcNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.RioOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.RioTypePage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.BdjOverviewPage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.BdjTypePage");
		tryToMountPage("com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage");

		mountPage(GrlcDefPage.MOUNT_PATH, GrlcDefPage.class);

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
