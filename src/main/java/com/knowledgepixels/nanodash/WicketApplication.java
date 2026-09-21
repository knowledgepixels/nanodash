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
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.events.NanopubPublishedListener;
import com.knowledgepixels.nanodash.events.NanopubPublishedPublisher;
import com.knowledgepixels.nanodash.page.*;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import de.agilecoders.wicket.webjars.WicketWebjars;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
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
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * WicketApplication is the main application class for the Nanodash web application.
 * It initializes the application, mounts pages, and provides version information.
 */
public class WicketApplication extends WebApplication implements NanopubPublishedPublisher {

    /**
     * URL to fetch the latest release information from GitHub.
     * This URL points to the releases of the Nanodash repository.
     */
    public static final String LATEST_RELEASE_URL = "https://api.github.com/repos/knowledgepixels/nanodash/releases";
    private static final Logger logger = LoggerFactory.getLogger(WicketApplication.class);

    private final List<NanopubPublishedListener> publishListeners = Collections.synchronizedList(new ArrayList<>());

    private static volatile String latestVersion = null;

    /**
     * When the latest version may be asked for again. GitHub allows 60 unauthenticated requests
     * an hour per address, and a developer restarting the app, or several instances behind one
     * address, run through those: a failed lookup has to be remembered as such, or the home page
     * asks again on every render and keeps the limit spent (issue #686). An hour is GitHub's own
     * window; a rate-limited answer says when it resets, and that is used when it does.
     */
    private static volatile long nextVersionLookup = 0L;

    private static final long VERSION_LOOKUP_RETRY_MS = 60 * 60 * 1000; // 1 hour

    @Override
    public void registerListener(NanopubPublishedListener listener) {
        logger.info("Registering listener {} for nanopub published events", listener.getClass().getName());
        publishListeners.add(listener);
    }

    @Override
    public void notifyNanopubPublished(Nanopub nanopub, String target, long waitMs) {
        for (NanopubPublishedListener listener : publishListeners) {
            listener.onNanopubPublished(nanopub, target, waitMs);
            logger.info("Notifying listener {} with toRefresh target <{}>", listener.getClass().getName(), target);
        }
    }

    /**
     * Static method to get the current instance of the WicketApplication.
     *
     * @return The current instance of WicketApplication.
     */
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
        // Looked up here and now, so the banner below can say it. This is startup, before
        // anything is served, which is the one place where waiting for it costs nobody
        // anything; everywhere else reads whatever this left behind (issue #686).
        if (claimVersionLookup()) lookUpLatestVersion();
        String lv = latestVersion == null ? "unknown" : latestVersion;
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

        Utils.initMainUrls();
        ServiceMode.init();
        ServiceHealth.init();

        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

        getExceptionSettings().setUnexpectedExceptionDisplay(ExceptionSettings.SHOW_NO_EXCEPTION_PAGE);

        mountPage(ErrorPage.MOUNT_PATH, ErrorPage.class);
        mountPage("/error/404", ErrorPage.class);
        mountPage("/error/500", ErrorPage.class);
        mountPage(NanopubNotFoundPage.MOUNT_PATH, NanopubNotFoundPage.class);

        mountPage(UserPage.MOUNT_PATH, UserPage.class);
        mountPage(ChannelPage.MOUNT_PATH, ChannelPage.class);
        mountPage(ExplorePage.MOUNT_PATH, ExplorePage.class);
        mountPage(ReferencesPage.MOUNT_PATH, ReferencesPage.class);
        mountPage(PublishPage.MOUNT_PATH, PublishPage.class);
        mountPage(PreviewPage.MOUNT_PATH, PreviewPage.class);
        mountPage(ProfilePage.MOUNT_PATH, ProfilePage.class);
        mountPage(GroupDemoPage.MOUNT_PATH, GroupDemoPage.class);
        mountPage(GroupDemoPageSoc.MOUNT_PATH, GroupDemoPageSoc.class);
        mountPage(OrcidLinkingPage.MOUNT_PATH, OrcidLinkingPage.class);
        mountPage(OrcidLoginPage.MOUNT_PATH, OrcidLoginPage.class);
        mountPage(MyChannelPage.MOUNT_PATH, MyChannelPage.class);
        mountPage(TermForwarder.MOUNT_PATH, TermForwarder.class);
        // The URLs of the retired list pages forward to the home page, which
        // shows their content as view sections now.
        mountPage(HomeForwarder.UserList.MOUNT_PATH, HomeForwarder.UserList.class);
        mountPage(HomeForwarder.SpaceList.MOUNT_PATH, HomeForwarder.SpaceList.class);
        mountPage(HomeForwarder.QueryList.MOUNT_PATH, HomeForwarder.QueryList.class);
        mountPage(HomeForwarder.Search.MOUNT_PATH, HomeForwarder.Search.class);
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
        mountPage(ClaudeChatPage.MOUNT_PATH, ClaudeChatPage.class);
        mountPage(ResultTablePage.MOUNT_PATH, ResultTablePage.class);
        mountPage(GenOverviewPage.MOUNT_PATH, GenOverviewPage.class);
        mountPage(GenSelectPage.MOUNT_PATH, GenSelectPage.class);
        mountPage(GenPublishPage.MOUNT_PATH, GenPublishPage.class);
        mountPage(GenConnectPage.MOUNT_PATH, GenConnectPage.class);
        mountPage(GenNanopubPage.MOUNT_PATH, GenNanopubPage.class);
        mountPage(ProjectPage.MOUNT_PATH, ProjectPage.class);
        mountPage(SpacePage.MOUNT_PATH, SpacePage.class);
        mountPage(QueryPage.MOUNT_PATH, QueryPage.class);
        mountPage(ViewResultsPage.MOUNT_PATH, ViewResultsPage.class);
        mountPage(ListPage.MOUNT_PATH, ListPage.class);
        mountPage(MaintainedResourcePage.MOUNT_PATH, MaintainedResourcePage.class);
        mountPage(ResourcePartPage.MOUNT_PATH, ResourcePartPage.class);
        mountPage(DownloadRdfPage.MOUNT_PATH, DownloadRdfPage.class);
        mountPage(DownloadDocPage.MOUNT_PATH, DownloadDocPage.class);
        mountPage(CalendarFeedPage.MOUNT_PATH, CalendarFeedPage.class);

        getCspSettings().blocking().disabled();
        getStoreSettings().setMaxSizePerSession(Bytes.megabytes(100));

        getRequestCycleListeners().add(new IRequestCycleListener() {
            @Override
            public void onBeginRequest(RequestCycle cycle) {
                Response response = cycle.getResponse();
                if (response instanceof WebResponse) {
                    ((WebResponse) response).setHeader("Nanodash-Version", getThisVersion());
                }
            }

            @Override
            public IRequestHandler onException(RequestCycle cycle, Exception ex) {
                logger.error("Unhandled exception during request [{}]", cycle.getRequest().getUrl(), ex);
                Throwable cause = ex.getCause();
                int depth = 1;
                while (cause != null) {
                    logger.error("  Caused by (depth {}): {}", depth++, cause.getMessage(), cause);
                    cause = cause.getCause();
                }
                return null;
            }
        });

        registerListeners();

        ApiCachePersistence.init();

        // Warm up the shared state in the background while the server finishes starting, so
        // the first request doesn't pay for it: the user data, the space and resource
        // repositories, and the home resource's view displays. Each of these builds from the
        // restored cache snapshot when one was loaded (issue #570) and from live queries
        // otherwise; either way the work happens now instead of on the first page render.
        // Two tasks rather than one: the home resource's data is sub-second when built from
        // the snapshot, and queueing it behind the user data (which always makes a few live
        // registry calls) would leave the home page showing its loading spinner for exactly
        // that long to anyone reloading the moment the server is up.
        NanodashThreadPool.submit(() -> {
            try {
                MaintainedResource homeResource = MaintainedResourceRepository.get()
                        .findById(NanodashPreferences.get().getHomeResource());
                if (homeResource != null) homeResource.triggerDataUpdate();
            } catch (Exception ex) {
                logger.error("Startup home-resource warm-up failed", ex);
            }
        });
        NanodashThreadPool.submit(() -> {
            try {
                User.ensureLoaded();
            } catch (Exception ex) {
                logger.error("Startup user-data warm-up failed", ex);
            }
        });

        String umamiScriptUrl = NanodashPreferences.get().getUmamiScriptUrl();
        if (umamiScriptUrl != null && !umamiScriptUrl.isBlank()) {
            logger.info("Umami analytics configured: {}", umamiScriptUrl);
        } else {
            logger.info("Umami analytics not configured (set NANODASH_UMAMI_SCRIPT_URL and NANODASH_UMAMI_WEBSITE_ID)");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Writes a final API cache snapshot, so a restart or upgrade comes back up with the
     * content it went down with.
     */
    @Override
    protected void onDestroy() {
        ApiCachePersistence.shutdown();
        super.onDestroy();
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

    /**
     * The latest publicly released version of Nanodash, as far as it is known here, and null
     * while it is not.
     * <p>
     * Answers with what the last lookup left behind rather than making one: this is read while
     * a page is being built (see {@link com.knowledgepixels.nanodash.page.HomePage}), and an
     * unreachable or slow api.github.com must not be a slow home page. A lookup that is due is
     * handed to the background, so what it finds is there for the next render. Not knowing the
     * latest version is a perfectly ordinary state — it only means one line of the home page is
     * left unsaid.
     *
     * @return the latest released version, or null if it is not known
     */
    public static String getLatestVersion() {
        if (latestVersion == null && claimVersionLookup()) {
            NanodashThreadPool.submit(WicketApplication::lookUpLatestVersion);
        }
        return latestVersion;
    }

    /**
     * Claims the right to make the next lookup, at most once per {@link #VERSION_LOOKUP_RETRY_MS}
     * and only while the version is unknown. Claiming it also postpones the one after, so a
     * lookup that fails is not immediately tried again (issue #686). Package-private for testing.
     *
     * @return true if the caller should make the lookup
     */
    static synchronized boolean claimVersionLookup() {
        long now = System.currentTimeMillis();
        if (latestVersion != null || now < nextVersionLookup) return false;
        nextVersionLookup = now + VERSION_LOOKUP_RETRY_MS;
        return true;
    }

    /**
     * Asks GitHub for the releases and remembers the latest one. Failing is not an error worth a
     * stack trace: the version check is there to tell the user that a newer version exists, and
     * not reaching GitHub — a rate limit spent, no network — leaves that unsaid and nothing else
     * undone (issue #686).
     */
    private static void lookUpLatestVersion() {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse resp = client.execute(new HttpGet(LATEST_RELEASE_URL));
            int c = resp.getStatusLine().getStatusCode();
            if (c < 200 || c >= 300) {
                postponeUntilRateLimitReset(resp);
                throw new HttpStatusException(c);
            }

            Gson gson = new Gson();
            Type nanopubReleasesType = new TypeToken<List<NanodashRelease>>() {
            }.getType();

            try (InputStreamReader reader = new InputStreamReader(resp.getEntity().getContent())) {
                List<NanodashRelease> releases = gson.fromJson(reader, nanopubReleasesType);
                if (!releases.isEmpty()) {
                    latestVersion = releases.getFirst().getVersionNumber();
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not fetch the latest version from {}: {}", LATEST_RELEASE_URL, ex.toString());
        }
    }

    /**
     * Waits out a spent rate limit for as long as the answer says it lasts, when it says so.
     * {@code x-ratelimit-reset} is a Unix timestamp in seconds; anything else in that header, or
     * a reset already in the past, leaves the standard interval in place. Package-private for
     * testing.
     *
     * @param resp the answer that was refused
     */
    static synchronized void postponeUntilRateLimitReset(HttpResponse resp) {
        Header header = resp.getFirstHeader("x-ratelimit-reset");
        if (header == null || header.getValue() == null) return;
        try {
            long resetAt = Long.parseLong(header.getValue().trim()) * 1000L;
            if (resetAt > nextVersionLookup) {
                logger.info("Rate limit for {} is spent until {}", LATEST_RELEASE_URL, new Date(resetAt));
                nextVersionLookup = resetAt;
            }
        } catch (NumberFormatException ex) {
            // Not a timestamp: the standard interval stands.
        }
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
                SpaceRepository.get().forceRootRefresh(waitMs);
            } else if (target.equals("maintainedResources")) {
                MaintainedResourceRepository.get().forceRootRefresh(waitMs);
            } else if (AbstractResourceWithProfile.isResourceWithProfile(target)) {
                AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(target);
                // What was just published can be a new version of a view this resource
                // shows, whose resolution the rebuilt structure would otherwise take from
                // the memo and so keep showing the previous definition (issue #654). The
                // views' results are left alone: only the view that was acted on is
                // refreshed (issue #622).
                resource.requestViewDefinitionRefresh();
                resource.forceRefresh(waitMs);
                if (resource instanceof Space) {
                    SpaceRepository.get().forceRootRefresh(waitMs);
                    MaintainedResourceRepository.get().forceRootRefresh(waitMs);
                } else if (resource instanceof MaintainedResource) {
                    MaintainedResourceRepository.get().forceRootRefresh(waitMs);
                }
            } else {
                QueryRef queryRef = QueryRef.parseString(target);
                ApiCache.clearCache(queryRef, waitMs, nanopub.getUri().stringValue());
            }
        });
    }

}
