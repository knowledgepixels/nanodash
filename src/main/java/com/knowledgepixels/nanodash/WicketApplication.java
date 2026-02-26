package com.knowledgepixels.nanodash;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.knowledgepixels.nanodash.connector.*;
import com.knowledgepixels.nanodash.connector.ios.DsNanopubPage;
import com.knowledgepixels.nanodash.connector.ios.DsOverviewPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjOverviewPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioOverviewPage;
import com.knowledgepixels.nanodash.events.NanopubPublishedListener;
import com.knowledgepixels.nanodash.page.*;
import de.agilecoders.wicket.webjars.WicketWebjars;
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
import org.apache.wicket.util.lang.Bytes;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * WicketApplication is the main application class for the Nanodash web application.
 * It initializes the application, mounts pages, and provides version information.
 */
public class WicketApplication extends WebApplication {

    /**
     * URL to fetch the latest release information from GitHub.
     * This URL points to the releases of the Nanodash repository.
     */
    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/knowledgepixels/nanodash/releases";
    private static final Logger logger = LoggerFactory.getLogger(WicketApplication.class);

    private final List<NanopubPublishedListener> publishListeners = Collections.synchronizedList(new ArrayList<>());

    public void registerListener(NanopubPublishedListener listener) {
        logger.info("Registering listener {} for nanopub published events", listener.getClass().getName());
        publishListeners.add(listener);
    }

    public void notifyNanopubPublished(Nanopub nanopub, String target, long waitMs) {
        for (NanopubPublishedListener entry : publishListeners) {
            entry.onNanopubPublished(nanopub, target, waitMs);
            logger.info("Notifying listener {} with toRefresh target <{}>", entry.getClass().getName(), target);
        }
    }

    public static WicketApplication get() {
        return (WicketApplication) WebApplication.get();
    }

    /**
     * Constructor for the WicketApplication.
     * Displays version information and provides instructions for accessing the application.
     */
    public WicketApplication() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:37373"));
            } catch (IOException | URISyntaxException ex) {
                logger.error("Error in opening browser", ex);
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

    /**
     * Returns the home page class for the application.
     *
     * @return The HomePage class.
     */
    public Class<HomePage> getHomePage() {
        return HomePage.class;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Initializes the application settings and mounts pages.
     */
    @Override
    protected void init() {
        super.init();
        WicketWebjars.install(this);

        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

        getExceptionSettings().setUnexpectedExceptionDisplay(ExceptionSettings.SHOW_NO_EXCEPTION_PAGE);

        mountPage(ErrorPage.MOUNT_PATH, ErrorPage.class);
        mountPage("/error/404", ErrorPage.class);
        mountPage("/error/500", ErrorPage.class);

        mountPage(UserPage.MOUNT_PATH, UserPage.class);
        mountPage(ChannelPage.MOUNT_PATH, ChannelPage.class);
        mountPage(SearchPage.MOUNT_PATH, SearchPage.class);
        mountPage(ExplorePage.MOUNT_PATH, ExplorePage.class);
        mountPage(PublishPage.MOUNT_PATH, PublishPage.class);
        mountPage(PreviewPage.MOUNT_PATH, PreviewPage.class);
        mountPage(ProfilePage.MOUNT_PATH, ProfilePage.class);
        mountPage(UserListPage.MOUNT_PATH, UserListPage.class);
        mountPage(GroupDemoPage.MOUNT_PATH, GroupDemoPage.class);
        mountPage(GroupDemoPageSoc.MOUNT_PATH, GroupDemoPageSoc.class);
        mountPage(OrcidLinkingPage.MOUNT_PATH, OrcidLinkingPage.class);
        mountPage(OrcidLoginPage.MOUNT_PATH, OrcidLoginPage.class);
        mountPage(SpaceListPage.MOUNT_PATH, SpaceListPage.class);
        mountPage(MyChannelPage.MOUNT_PATH, MyChannelPage.class);
        mountPage(TermForwarder.MOUNT_PATH, TermForwarder.class);
        mountPage(ViewPage.MOUNT_PATH, ViewPage.class);
        mountPage(GetViewPage.MOUNT_PATH, GetViewPage.class);
        mountPage(DsOverviewPage.MOUNT_PATH, DsOverviewPage.class);
        mountPage(DsNanopubPage.MOUNT_PATH, DsNanopubPage.class);
        mountPage(RioOverviewPage.MOUNT_PATH, RioOverviewPage.class);
        mountPage(RioNanopubPage.MOUNT_PATH, RioNanopubPage.class);
        mountPage(BdjOverviewPage.MOUNT_PATH, BdjOverviewPage.class);
        mountPage(BdjNanopubPage.MOUNT_PATH, BdjNanopubPage.class);
        mountPage(FdoForwarder.MOUNT_PATH, FdoForwarder.class);
        mountPage(GetNamePage.MOUNT_PATH, GetNamePage.class);
        mountPage(TestPage.MOUNT_PATH, TestPage.class);
        mountPage(ResultTablePage.MOUNT_PATH, ResultTablePage.class);
        mountPage(GenOverviewPage.MOUNT_PATH, GenOverviewPage.class);
        mountPage(GenSelectPage.MOUNT_PATH, GenSelectPage.class);
        mountPage(GenPublishPage.MOUNT_PATH, GenPublishPage.class);
        mountPage(GenConnectPage.MOUNT_PATH, GenConnectPage.class);
        mountPage(GenNanopubPage.MOUNT_PATH, GenNanopubPage.class);
        mountPage(ProjectPage.MOUNT_PATH, ProjectPage.class);
        mountPage(SpacePage.MOUNT_PATH, SpacePage.class);
        mountPage(QueryPage.MOUNT_PATH, QueryPage.class);
        mountPage(QueryListPage.MOUNT_PATH, QueryListPage.class);
        mountPage(ListPage.MOUNT_PATH, ListPage.class);
        mountPage(MaintainedResourcePage.MOUNT_PATH, MaintainedResourcePage.class);
        mountPage(ResourcePartPage.MOUNT_PATH, ResourcePartPage.class);

        getCspSettings().blocking().disabled();
        getStoreSettings().setMaxSizePerSession(Bytes.MAX);

        registerListeners();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the runtime configuration type for the application.
     */
    @Override
    public RuntimeConfigurationType getConfigurationType() {
        return RuntimeConfigurationType.DEPLOYMENT;
    }


    private static String latestVersion = null;

    /**
     * Retrieves the latest version of the application from the GitHub API.
     *
     * @return The latest version as a string.
     */
    public static String getLatestVersion() {
        if (latestVersion != null) return latestVersion;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse resp = client.execute(new HttpGet(LATEST_RELEASE_URL));
            int c = resp.getStatusLine().getStatusCode();
            if (c < 200 || c >= 300) {
                throw new HttpStatusException(c);
            }

            Gson gson = new Gson();
            Type nanopubReleasesType = new TypeToken<List<NanodashRelease>>() {
            }.getType();

            List<NanodashRelease> releases = gson.fromJson(new InputStreamReader(resp.getEntity().getContent()), nanopubReleasesType);
            if (!releases.isEmpty()) {
                latestVersion = releases.getFirst().getVersionNumber();
            }
        } catch (Exception ex) {
            logger.error("Error in fetching latest version", ex);
        }
        return latestVersion;
    }

    /**
     * Properties object to hold application properties.
     */
    public final static Properties properties = new Properties();

    static {
        try {
            properties.load(WicketApplication.class.getClassLoader().getResourceAsStream("nanodash.properties"));
        } catch (IOException ex) {
            logger.error("Error in loading properties", ex);
        }
    }

    /**
     * Retrieves the current version of the application.
     *
     * @return The current version as a string.
     */
    public static String getThisVersion() {
        return properties.getProperty("nanodash.version");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session newSession(Request request, Response response) {
        return new NanodashSession(request);
    }

    private void registerListeners() {
        logger.info("Registering nanopub published event listeners for spaces, maintained resources, resource with profile and query ref refresh");
        registerListener((nanopub, target, waitMs) -> {
            logger.info("Received nanopub published event with target <{}> and waitMs {}", target, waitMs);
            if (target.equals("spaces")) {
                Space.forceRootRefresh(waitMs);
            } else if (target.equals("maintainedResources")) {
                MaintainedResource.forceRootRefresh(waitMs);
            } else if (ResourceWithProfile.isResourceWithProfile(target)) {
                ResourceWithProfile.forceRefresh(target, waitMs);
            } else {
                QueryRef queryRef = QueryRef.parseString(target);
                ApiCache.clearCache(queryRef, waitMs);
            }
        });
    }

}
