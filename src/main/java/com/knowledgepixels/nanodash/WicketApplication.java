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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.settings.ExceptionSettings;

import com.knowledgepixels.nanodash.connector.ios.DsConnectPage;
import com.knowledgepixels.nanodash.connector.ios.DsNanopubPage;
import com.knowledgepixels.nanodash.connector.ios.DsOverviewPage;
import com.knowledgepixels.nanodash.connector.ios.DsPublishPage;
import com.knowledgepixels.nanodash.connector.ios.DsSelectPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConnectPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjOverviewPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjPublishPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjSelectPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioConnectPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioOverviewPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioPublishPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioSelectPage;
import com.knowledgepixels.nanodash.page.ChannelPage;
import com.knowledgepixels.nanodash.page.ConnectorListPage;
import com.knowledgepixels.nanodash.page.ErrorPage;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.FdoForwarder;
import com.knowledgepixels.nanodash.page.GetNamePage;
import com.knowledgepixels.nanodash.page.GetViewPage;
import com.knowledgepixels.nanodash.page.GroupDemoPage;
import com.knowledgepixels.nanodash.page.GroupDemoPageSoc;
import com.knowledgepixels.nanodash.page.HomePage;
import com.knowledgepixels.nanodash.page.ThingListPage;
import com.knowledgepixels.nanodash.page.MyChannelPage;
import com.knowledgepixels.nanodash.page.OrcidLinkingPage;
import com.knowledgepixels.nanodash.page.OrcidLoginPage;
import com.knowledgepixels.nanodash.page.ProfilePage;
import com.knowledgepixels.nanodash.page.PublishConfirmPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.ReferenceTablePage;
import com.knowledgepixels.nanodash.page.ResultTablePage;
import com.knowledgepixels.nanodash.page.SearchPage;
import com.knowledgepixels.nanodash.page.TermForwarder;
import com.knowledgepixels.nanodash.page.TestPage;
import com.knowledgepixels.nanodash.page.TypePage;
import com.knowledgepixels.nanodash.page.UserListPage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.page.ViewPage;

public class WicketApplication extends WebApplication {

	public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/knowledgepixels/nanodash/releases";

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
		mountPage(ChannelPage.MOUNT_PATH, ChannelPage.class);
		mountPage(SearchPage.MOUNT_PATH, SearchPage.class);
		mountPage(ExplorePage.MOUNT_PATH, ExplorePage.class);
		mountPage(ReferenceTablePage.MOUNT_PATH, ReferenceTablePage.class);
		mountPage(PublishPage.MOUNT_PATH, PublishPage.class);
		mountPage(PublishConfirmPage.MOUNT_PATH, PublishConfirmPage.class);
		mountPage(ProfilePage.MOUNT_PATH, ProfilePage.class);
		mountPage(UserListPage.MOUNT_PATH, UserListPage.class);
		mountPage(GroupDemoPage.MOUNT_PATH, GroupDemoPage.class);
		mountPage(GroupDemoPageSoc.MOUNT_PATH, GroupDemoPageSoc.class);
		mountPage(OrcidLinkingPage.MOUNT_PATH, OrcidLinkingPage.class);
		mountPage(OrcidLoginPage.MOUNT_PATH, OrcidLoginPage.class);
		mountPage(ConnectorListPage.MOUNT_PATH, ConnectorListPage.class);
		mountPage(MyChannelPage.MOUNT_PATH, MyChannelPage.class);
		mountPage(TermForwarder.MOUNT_PATH, TermForwarder.class);
		mountPage(ViewPage.MOUNT_PATH, ViewPage.class);
		mountPage(GetViewPage.MOUNT_PATH, GetViewPage.class);
		mountPage(DsOverviewPage.MOUNT_PATH, DsOverviewPage.class);
		mountPage(DsSelectPage.MOUNT_PATH, DsSelectPage.class);
		mountPage(DsPublishPage.MOUNT_PATH, DsPublishPage.class);
		mountPage(DsConnectPage.MOUNT_PATH, DsConnectPage.class);
		mountPage(DsNanopubPage.MOUNT_PATH, DsNanopubPage.class);
		mountPage(RioOverviewPage.MOUNT_PATH, RioOverviewPage.class);
		mountPage(RioSelectPage.MOUNT_PATH, RioSelectPage.class);
		mountPage(RioPublishPage.MOUNT_PATH, RioPublishPage.class);
		mountPage(RioConnectPage.MOUNT_PATH, RioConnectPage.class);
		mountPage(RioNanopubPage.MOUNT_PATH, RioNanopubPage.class);
		mountPage(BdjOverviewPage.MOUNT_PATH, BdjOverviewPage.class);
		mountPage(BdjSelectPage.MOUNT_PATH, BdjSelectPage.class);
		mountPage(BdjPublishPage.MOUNT_PATH, BdjPublishPage.class);
		mountPage(BdjConnectPage.MOUNT_PATH, BdjConnectPage.class);
		mountPage(BdjNanopubPage.MOUNT_PATH, BdjNanopubPage.class);
		mountPage(FdoForwarder.MOUNT_PATH, FdoForwarder.class);
		mountPage(GetNamePage.MOUNT_PATH, GetNamePage.class);
		mountPage(TypePage.MOUNT_PATH, TypePage.class);
		mountPage(TestPage.MOUNT_PATH, TestPage.class);
		mountPage(ThingListPage.MOUNT_PATH, ThingListPage.class);
		mountPage(ResultTablePage.MOUNT_PATH, ResultTablePage.class);

		getCspSettings().blocking().disabled();
	}

	@Override
	public RuntimeConfigurationType getConfigurationType() {
		return RuntimeConfigurationType.DEPLOYMENT;
	}


	private static String latestVersion = null;

	public static String getLatestVersion() {
		if (latestVersion != null) return latestVersion;
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpResponse resp = client.execute(new HttpGet(LATEST_RELEASE_URL));
			int c = resp.getStatusLine().getStatusCode();
			if (c < 200 || c >= 300) throw new RuntimeException("HTTP error: " + c);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()))) {;
				while(reader.ready()) {
					String line = reader.readLine();
					// TODO: Do proper JSON parsing
					if (line.matches(".*\"tag_name\":\\s*\"nanodash-[0-9]+\\.[0-9]+\".*")) {
						latestVersion = line.replaceFirst(".*?\"tag_name\":\\s*\"nanodash-([0-9]+\\.[0-9]+)\".*", "$1");
						break;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return latestVersion;
	}

	public final static Properties properties = new Properties();

	static {
		try {
			properties.load(WicketApplication.class.getClassLoader().getResourceAsStream("nanodash.properties"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static String getThisVersion() {
		return properties.getProperty("nanodash.version");
	}

	@Override
	public Session newSession(Request request, Response response) {
		return new NanodashSession(request);
	}

}
