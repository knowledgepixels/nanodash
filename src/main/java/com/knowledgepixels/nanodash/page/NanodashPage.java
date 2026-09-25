package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.SiteMode;
import com.knowledgepixels.nanodash.NanodashThreadPool;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.chat.ClaudeChatService;
import com.knowledgepixels.nanodash.chat.RemoteAgentService;
import com.knowledgepixels.nanodash.component.ClaudeChatPanel;
import com.knowledgepixels.nanodash.domain.*;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;
import org.owasp.html.Encoding;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Abstract base class for Nanodash pages.
 * Provides functionality for auto-refreshing data and rendering JavaScript resources.
 */
public abstract class NanodashPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(NanodashPage.class);

    private static long lastRefresh = 0L;
    private static final long REFRESH_INTERVAL = 60 * 1000; // 1 minute
    private static boolean refreshRunning = false;

    private long state = 0L;

    private static JavaScriptResourceReference nanodashJs = new JavaScriptResourceReference(WicketApplication.class, "script/nanodash.js");

    private static final String SITE_NAME = "Nanodash";

    private static final String SITE_META_DESCRIPTION =
            "Nanodash is a web client to browse and publish nanopublications: small, "
            + "self-contained and citable units of scientific knowledge.";

    private static final String PAGE_TITLE_ID = "pagetitle";

    private static final int MAX_META_DESCRIPTION_LENGTH = 300;

    private String metaDescription = defaultMetaDescription();

    private static final PolicyFactory TEXT_ONLY_POLICY = new HtmlPolicyBuilder().toFactory();

    private static final Pattern BLOCK_BOUNDARY = Pattern.compile(
            "</?(?:p|div|br|li|ul|ol|h[1-6]|tr|td|th|table|blockquote|pre|section|article)\\b[^>]*>",
            Pattern.CASE_INSENSITIVE);

    /**
     * Returns the mount path for this page.
     *
     * @return the mount path as a String
     */
    public abstract String getMountPath();

    /**
     * What this instance calls itself: Nanodash, or the site's name when it is a site
     * (issue #692).
     *
     * @return the site name
     */
    protected static String siteName() {
        return SiteMode.isEnabled() ? SiteMode.getName() : SITE_NAME;
    }

    /**
     * The ending of a page title, naming the site the page is on: {@code " | nanodash"}, or
     * the site's name in site mode.
     *
     * @return the title suffix, including its separator
     */
    protected static String titleSuffix() {
        return " | " + (SiteMode.isEnabled() ? SiteMode.getName() : "nanodash");
    }

    /**
     * The description of a page that has none of its own: what Nanodash is, or in site mode
     * what the site's space says about itself.
     */
    private static String defaultMetaDescription() {
        if (!SiteMode.isEnabled()) return SITE_META_DESCRIPTION;
        Space space = SiteMode.getSpace();
        if (space != null && space.getDescription() != null && !space.getDescription().isBlank()) {
            return space.getDescription();
        }
        return SiteMode.getName() + ": browse and publish nanopublications.";
    }

    /**
     * Constructor for NanodashPage.
     *
     * @param parameters the page parameters
     */
    protected NanodashPage(PageParameters parameters) {
        super(parameters);
        markIfBrowserReload();
        ensureRefreshed();
        // A session built while a service was unavailable holds no profile information
        // (issue #684); this picks it up once the service answers.
        NanodashSession.get().refreshProfileInfoIfIncomplete();
        // In site mode the body says so, for the stylesheet and script to tell links that
        // leave the site from those that stay (issue #692). Transparent, so that the page's
        // own components resolve through it as before.
        TransparentWebMarkupContainer body = new TransparentWebMarkupContainer("body");
        if (SiteMode.isEnabled()) body.add(new AttributeAppender("class", "site"));
        add(body);
        add(new ClaudeChatPanel("claudechat", true) {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(ClaudeChatService.get().isEnabled() && hasClaudeChatDock());
            }

        });
        addRemoteNavigationPollIfNeeded();
    }

    /**
     * Lets a remote AI agent acting for the logged-in user steer this browser
     * tab via the open_page tool (see docs/remote-mcp.md). The poll is only
     * attached when, at render time, the user's agent has been active within
     * the last 30 minutes — so pages of uninvolved users never poll — and it
     * stops itself once that window lapses. A page rendered before the agent's
     * first call doesn't poll until the user next navigates or reloads.
     */
    private void addRemoteNavigationPollIfNeeded() {
        if (!NanodashPreferences.get().isMcpRemoteEnabled()) return;
        var userIriObj = NanodashSession.get().getUserIri();
        if (userIriObj == null) return;
        final String userIri = userIriObj.stringValue();
        if (!RemoteAgentService.get().isRecentlyActive(userIri)) return;
        add(new AbstractAjaxTimerBehavior(Duration.ofSeconds(3)) {

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                String path = RemoteAgentService.get().pollNavigation(userIri);
                if (path != null) {
                    // Path is validated by the open_page tool: in-app, no quotes or backslashes.
                    target.appendJavaScript("window.location = '" + path + "';");
                } else if (!RemoteAgentService.get().isRecentlyActive(userIri)) {
                    stop(target);
                }
            }

        });
    }

    /**
     * Whether this page shows the docked Claude chat panel (when the feature
     * is enabled). Pages that embed the chat themselves can switch it off.
     *
     * @return true to show the docked panel
     */
    protected boolean hasClaudeChatDock() {
        return true;
    }

    /**
     * Flags the request for a cache force-refresh when it is a genuine browser
     * reload. Browsers send {@code Cache-Control: max-age=0} on a normal reload and
     * {@code no-cache} (often with {@code Pragma: no-cache}) on a hard reload, but
     * not on link navigation, Ajax requests, or a followed server redirect — so
     * this targets only the reload case. The flag is read by {@link ApiCache},
     * which then re-queries this page's views instead of serving the cache. Set
     * here (in the base constructor) so it is in effect before subclasses build
     * their query components.
     */
    private void markIfBrowserReload() {
        RequestCycle rc = RequestCycle.get();
        if (rc == null || !(rc.getRequest() instanceof WebRequest req)) return;
        String cacheControl = req.getHeader("Cache-Control");
        String pragma = req.getHeader("Pragma");
        boolean reload = (cacheControl != null && (cacheControl.contains("no-cache") || cacheControl.contains("max-age=0")))
                || (pragma != null && pragma.contains("no-cache"));
        if (reload) {
            rc.setMetaData(ApiCache.FORCE_REFRESH_ON_RELOAD, Boolean.TRUE);
        }
    }

    private void ensureRefreshed() {
        synchronized (getClass()) {
            state = lastRefresh;
            if (!refreshRunning && System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL) {
                refreshRunning = true;
                NanodashThreadPool.submit(() -> {
                    try {
                        logger.info("Refreshing data...");
                        User.refreshUsers();
                        TemplateData.refreshTemplates();
                        Space.refresh();
                        MaintainedResource.refresh();
                        AbstractResourceWithProfile.refresh();
                        logger.info("Refreshing data... done");
                        lastRefresh = System.currentTimeMillis();
                    } catch (Exception ex) {
                        logger.error("Error during refresh", ex);
                    } finally {
                        refreshRunning = false;
                    }
                });
            }
        }
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     * Override this method in subclasses to enable auto-refresh.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return false;
    }

    /**
     * The navigation context id (space/user/maintained resource) this page was reached
     * under. Pages showing a context resource override this to return their own resource
     * id, so links from them carry the context even without a {@code context} parameter.
     *
     * @return the context resource id, or null if none
     */
    public String getContextId() {
        return NavigationContext.getContextId(getPageParameters());
    }

    /**
     * The navigation context this page was reached under, as opposed to the one it
     * hands on: the plain {@code context} parameter, without the override pages showing
     * a context resource apply to {@link #getContextId()}. Tells a context page where
     * the user came from, so the trail there is not lost (issue #697).
     *
     * @return the incoming context resource id, or null if none
     */
    public String getIncomingContextId() {
        return NavigationContext.getContextId(getPageParameters());
    }

    /**
     * The resource part this page was reached under (or is itself), carried along as the
     * {@code part} parameter. A part is not a context resource of its own, so it travels
     * next to {@link #getIncomingContextId()}, which names the maintaining resource it
     * belongs to (issue #697).
     *
     * @return the part resource id, or null if the page was not reached from a part
     */
    public String getPartId() {
        // Stepping up to the resource that maintains the part lands on that resource's
        // own page; from there on the part is behind the user, not where they came from,
        // so it stops travelling here.
        if (isContextPage() && getContextId() != null && getContextId().equals(getIncomingContextId())) return null;
        return NavigationContext.getPartId(getPageParameters());
    }

    /**
     * The label of {@link #getPartId()}, carried along so a back-link can name the part
     * without resolving it over the network.
     *
     * @return the part label, or null if none is known
     */
    public String getPartLabel() {
        return NavigationContext.getPartLabel(getPageParameters());
    }

    /**
     * Whether this page shows a context resource itself (space, user, maintained
     * resource, or resource part). Such pages have their own breadcrumb or tab strip and
     * don't get the title bar's back-to-context link.
     *
     * @return true if this is a context resource's own page
     */
    public boolean isContextPage() {
        return false;
    }

    /**
     * Whether this page's content pane runs the full viewport width (class
     * {@code full}), so the title bar's breadcrumb strip should too instead of
     * centering at the standard content width.
     *
     * @return true if this page has full-width content
     */
    public boolean hasFullWidthContent() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRender() {
        if (hasAutoRefreshEnabled() && state < lastRefresh) {
            String query = Utils.getPageParametersAsString(getPageParameters());
            throw new RedirectToUrlException(getMountPath() + (query.isEmpty() ? "" : "?" + query));
        }
        super.onRender();
    }

    // How long a stylesheet URL is reused before the file is checked again. Keeps the
    // stat off every single render without making an edit wait noticeably to show up.
    private static final long STYLESHEET_STAMP_TTL_MS = 5000;

    private static volatile String stylesheetUrl;
    private static volatile long stylesheetUrlStampedAt;

    /**
     * The URL to load style.css from, stamped so that a changed file is a changed URL.
     * <p>
     * The app version alone does not do that: it stays the same across every edit of a
     * snapshot build (and across redeploys of the same release), while the file is served
     * straight from the webapp directory with only a {@code Last-Modified} header — no
     * {@code ETag}, no {@code Cache-Control}. Browsers are then free to keep serving their
     * copy without revalidating, which is why CSS changes used to need a hard refresh.
     * Stamping the file's own modification time into the query string makes each edit a new
     * URL that no cache can answer from an old copy.
     *
     * @return the stylesheet URL including its cache-busting query string
     */
    private static String getStyleSheetUrl() {
        long now = System.currentTimeMillis();
        String url = stylesheetUrl;
        if (url == null || now - stylesheetUrlStampedAt > STYLESHEET_STAMP_TTL_MS) {
            String version = ResourceBundle.getBundle("nanodash").getString("nanodash.version");
            long lastModified = getStyleSheetLastModified();
            url = "style.css?v=" + version + (lastModified > 0 ? "-" + lastModified : "");
            stylesheetUrl = url;
            stylesheetUrlStampedAt = now;
        }
        return url;
    }

    /**
     * @return style.css's last-modified time, or 0 when it cannot be determined (a packed
     * war, where the file cannot change without a redeploy anyway)
     */
    private static long getStyleSheetLastModified() {
        try {
            String path = WebApplication.get().getServletContext().getRealPath("/style.css");
            if (path == null) return 0;
            return new File(path).lastModified();
        } catch (Exception ex) {
            logger.warn("Could not determine the stylesheet's modification time: {}", ex.getMessage());
            return 0;
        }
    }

    /**
     * The description search engines and link previews show for this page, which is the
     * one describing Nanodash itself until a page sets its own.
     *
     * @return the description
     */
    protected String getMetaDescription() {
        return metaDescription;
    }

    /**
     * Gives this page a description of its own subject, replacing the one describing
     * Nanodash itself. Pages call this from their constructor, so that the description is
     * in place before the head is rendered.
     *
     * @param description the description; blank leaves the site description in place, and
     *                    one longer than a search result snippet can show is cut short
     */
    protected void setMetaDescription(String description) {
        String text = toMetaDescription(description);
        if (text != null) metaDescription = text;
    }

    /**
     * Turns a description into the text a meta description holds: descriptions are often
     * HTML, which search results and link previews show verbatim, so its markup is dropped
     * and its entities decoded; the rest is put on one line and cut short where it is
     * longer than a search result snippet can show.
     *
     * @param description the description, as plain text or HTML
     * @return the meta description, or null if nothing is left of the description
     */
    static String toMetaDescription(String description) {
        if (description == null) return null;
        String oneLine = toPlainText(description).strip().replaceAll("\\s+", " ");
        if (oneLine.isEmpty()) return null;
        return oneLine.length() <= MAX_META_DESCRIPTION_LENGTH
                ? oneLine
                : oneLine.substring(0, MAX_META_DESCRIPTION_LENGTH).stripTrailing() + "\u2026";
    }

    /**
     * The text of an HTML fragment, with block-level boundaries kept as spaces so that
     * paragraphs and list items do not run into each other.
     *
     * @param html the HTML fragment, or plain text
     * @return the text, with markup removed and entities decoded
     */
    static String toPlainText(String html) {
        String spaced = BLOCK_BOUNDARY.matcher(html).replaceAll(" ");
        return Encoding.decodeHtml(TEXT_ONLY_POLICY.sanitize(spaced));
    }

    /**
     * The title link previews show for this page, taken from the page's own title label
     * where it has one and falling back to the site name where the title is fixed markup.
     *
     * @return the title
     */
    protected String getMetaTitle() {
        Component pageTitle = get(PAGE_TITLE_ID);
        if (pageTitle == null) return siteName();
        Object title = pageTitle.getDefaultModelObject();
        return title == null ? siteName() : title.toString();
    }

    /**
     * What this page offers as RDF (issue #710). Pages about a resource that the download
     * page can serve override this; the default is nothing, which leaves the page HTML-only.
     *
     * @return the RDF source, or null for a page without one
     */
    protected RdfSource getRdfSource() {
        return null;
    }

    /**
     * Answers a client that asked for RDF in its {@code Accept} header with a 303 to the
     * download page in the matching format, and lets everyone else have the HTML (issue
     * #710). Pages call this from their constructor as soon as they know their resource,
     * before building anything, so that a machine client gets its redirect without the
     * page's own work being done first. Either way the response is marked as varying on
     * the {@code Accept} header, so that caches keep the two apart.
     *
     * @param source what to serve; its declarations are not needed here and may be empty
     * @throws RedirectToUrlException when the client asked for RDF
     */
    protected void redirectIfRdfRequested(RdfSource source) {
        if (getResponse() instanceof WebResponse webResponse) {
            webResponse.setHeader("Vary", "Accept");
        }
        String accept = getRequest() instanceof WebRequest webRequest ? webRequest.getHeader("Accept") : null;
        RdfNegotiation.Variant variant = RdfNegotiation.negotiate(accept);
        if (variant == null) return;
        String url = source.downloadUrl(variant);
        logger.info("RDF requested as {} for {} {}; redirecting to {}", variant.mediaType(), source.type(), source.id(), url);
        throw new RedirectToUrlException(url, 303);
    }

    /**
     * Renders what lets HTML-reading tools find this page's RDF (issue #710): one
     * alternate link per download format, and the declaring assertions as an embedded
     * JSON-LD block. Nothing is rendered for a page without an RDF source.
     *
     * @param response the header response to render into
     */
    private void renderRdfLinks(IHeaderResponse response) {
        RdfSource source = getRdfSource();
        if (source == null) return;
        for (RdfNegotiation.Variant variant : RdfNegotiation.VARIANTS) {
            response.render(headTag("link", "rel", "alternate", "href", source.downloadUrl(variant), "type", variant.mediaType()));
        }
        String jsonLd;
        try {
            jsonLd = source.toEmbeddedJsonLd(source.downloadUrl(RdfNegotiation.VARIANTS.get(0)));
        } catch (Exception ex) {
            logger.warn("Could not embed the JSON-LD for {} {}: {}", source.type(), source.id(), ex.getMessage());
            return;
        }
        if (jsonLd == null) return;
        response.render(StringHeaderItem.forString("<script type=\"application/ld+json\">\n" + jsonLd + "\n</script>\n"));
    }

    /**
     * Renders the description, canonical URL, Open Graph and Twitter card tags that
     * search engines and link previews read (issue #704).
     * <p>
     * The URL comes from {@link Utils#absolutePageUrl}, so it carries the configured
     * website address rather than how a reverse proxy reached this container, and never
     * the visitor's {@code ;jsessionid}.
     *
     * @param response the header response to render into
     */
    private void renderPageMetadata(IHeaderResponse response) {
        String title = getMetaTitle();
        String description = getMetaDescription();
        String url = Utils.absolutePageUrl(getClass(), getPageParameters());
        response.render(headTag("meta", "name", "description", "content", description));
        response.render(headTag("link", "rel", "canonical", "href", url));
        response.render(headTag("meta", "property", "og:type", "content", "website"));
        response.render(headTag("meta", "property", "og:site_name", "content", siteName()));
        response.render(headTag("meta", "property", "og:title", "content", title));
        response.render(headTag("meta", "property", "og:description", "content", description));
        response.render(headTag("meta", "property", "og:url", "content", url));
        response.render(headTag("meta", "name", "twitter:card", "content", "summary"));
        response.render(headTag("meta", "name", "twitter:title", "content", title));
        response.render(headTag("meta", "name", "twitter:description", "content", description));
    }

    /**
     * A {@code meta} or {@code link} tag for the head, with every attribute value escaped
     * for HTML. Wicket's {@code MetaDataHeaderItem} only backslash-escapes double quotes,
     * which HTML does not honour, so a value taken from a nanopublication (a space's
     * description, say) could end the attribute and put markup of its own into the page.
     *
     * @param tagName    the tag, {@code meta} or {@code link}
     * @param attributes attribute names and values, alternating
     * @return the header item
     * @throws IllegalArgumentException if a name is given without a value
     */
    static StringHeaderItem headTag(String tagName, String... attributes) {
        if (attributes.length % 2 != 0) {
            throw new IllegalArgumentException("Attribute names and values must come in pairs");
        }
        StringBuilder tag = new StringBuilder("<").append(tagName);
        for (int i = 0; i < attributes.length; i += 2) {
            tag.append(' ').append(attributes[i]).append("=\"").append(escapeAttribute(attributes[i + 1])).append('"');
        }
        return StringHeaderItem.forString(tag.append(" />\n").toString());
    }

    /**
     * Escapes a value for a double- or single-quoted HTML attribute.
     *
     * @param value the value, or null for an empty one
     * @return the escaped value
     */
    static String escapeAttribute(String value) {
        return value == null ? "" : Strings.escapeMarkup(value).toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Renders the head section of the page, including JavaScript references.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        renderPageMetadata(response);
        renderRdfLinks(response);
        // A site's logo is its icon too; otherwise it is Nanodash's (issue #692).
        String siteLogo = SiteMode.getLogoSrc();
        response.render(siteLogo != null
                ? headTag("link", "rel", "icon", "href", siteLogo)
                : headTag("link", "rel", "icon", "type", "image/svg+xml", "href", "images/favicon.svg"));
        response.render(CssHeaderItem.forUrl(getStyleSheetUrl()));
        // A site's own stylesheet comes after Nanodash's, so that it wins where they meet.
        String siteCss = SiteMode.isEnabled() ? NanodashPreferences.get().getSiteCss() : null;
        if (siteCss != null) response.render(CssHeaderItem.forUrl(siteCss));
        response.render(JavaScriptHeaderItem.forReference(getApplication().getJavaScriptLibrarySettings().getJQueryReference()));
        response.render(JavaScriptReferenceHeaderItem.forReference(nanodashJs));
        String umamiScriptUrl = NanodashPreferences.get().getUmamiScriptUrl();
        String umamiWebsiteId = NanodashPreferences.get().getUmamiWebsiteId();
        if (umamiScriptUrl != null && !umamiScriptUrl.isBlank()
            && umamiWebsiteId != null && !umamiWebsiteId.isBlank()) {
            String umamiJs = "(function(){" +
                             "var s=document.createElement('script');" +
                             "s.src='" + umamiScriptUrl + "';" +
                             "s.defer=true;" +
                             "s.setAttribute('data-website-id','" + umamiWebsiteId + "');" +
                             "document.head.appendChild(s);" +
                             "})();";
            response.render(JavaScriptHeaderItem.forScript(umamiJs, "umami-loader"));
        }
    }

}
