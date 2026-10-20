package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Serves {@code robots.txt} (issue #804).
 *
 * <p>Generated rather than shipped as a static file, because the {@code Sitemap:} line has
 * to be an absolute URL and this application is served from several hostnames, from
 * single-space sites and from instances anyone can run. A file in the webapp directory
 * cannot know which of those it is being fetched from; {@link Utils#absolutePageUrl} can.</p>
 *
 * <p>Two mechanisms keep pages out of search results and they do different jobs, so what
 * goes where is a decision rather than a preference:</p>
 * <ul>
 *   <li>A path listed here is never fetched at all. That is what the expensive paths want —
 *       the ones that fan out into the query fleet, and the forms and downloads that only
 *       make sense inside a session.</li>
 *   <li>A page that is cheap to serve but should not appear in an index is left crawlable
 *       and says so itself, through the {@code robots} meta tag
 *       {@link NanodashPage#getRobotsDirective()} renders. A crawler has to be allowed to
 *       fetch a page to read that tag, so disallowing such a page here would have the
 *       opposite of the intended effect: it would stay in the index, listed without a
 *       description.</li>
 * </ul>
 */
public class RobotsPage extends WebPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/robots.txt";

    /**
     * How long a fetched robots.txt may be reused.
     */
    private static final Duration CACHE_DURATION = Duration.ofHours(24);

    /**
     * The paths crawlers are asked to stay away from altogether: every one of them either
     * runs queries across the fleet for each page, serves a file rather than a page, or only
     * means anything to the user whose session it belongs to.
     */
    static final List<String> DISALLOWED = List.of(
            "/publish",
            "/preview",
            "/profile",
            "/mychannel",
            "/query",
            "/resulttable",
            "/view",
            "/view-results",
            "/list",
            "/references",
            "/claudechat",
            "/download-rdf",
            "/download-doc",
            "/calendar.ics",
            "/site-loading",
            "/connector/",
            "/search",
            "/test",
            "/groupdemo",
            "/wicket/");

    /**
     * How long a crawler is asked to wait between requests. Only Bing honours it — the real
     * cap on crawl rate belongs in the reverse proxy — but it costs nothing to ask.
     */
    private static final int CRAWL_DELAY_SECONDS = 5;

    public RobotsPage(final PageParameters parameters) {
        super(parameters);

        byte[] bytes = build().getBytes(StandardCharsets.UTF_8);

        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(bytes);
            }

            @Override
            public String getContentType() {
                return "text/plain; charset=utf-8";
            }
        };

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, "robots.txt");
        handler.setContentDisposition(ContentDisposition.INLINE);
        handler.setCacheDuration(CACHE_DURATION);
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Builds the robots.txt body.
     *
     * @return the body, ending in a newline
     */
    static String build() {
        return build(sitemapUrl());
    }

    /**
     * Builds the robots.txt body naming the given sitemap.
     *
     * @param sitemapUrl the absolute sitemap URL, or null to name none
     * @return the body, ending in a newline
     */
    static String build(String sitemapUrl) {
        StringBuilder text = new StringBuilder();
        // These two are tokens read from this file, not user agents anything ever sends, so
        // they have to be addressed here or opting out of AI training is impossible.
        text.append("User-agent: Google-Extended\nDisallow: /\n\n");
        text.append("User-agent: Applebot-Extended\nDisallow: /\n\n");
        text.append("User-agent: *\n");
        text.append("Crawl-delay: ").append(CRAWL_DELAY_SECONDS).append('\n');
        for (String path : DISALLOWED) text.append("Disallow: ").append(path).append('\n');
        text.append("Allow: /\n");
        if (sitemapUrl != null) text.append('\n').append("Sitemap: ").append(sitemapUrl).append('\n');
        return text.toString();
    }

    /**
     * @return the absolute URL of this instance's sitemap, or null where the instance has no
     * configured address to build one from
     */
    private static String sitemapUrl() {
        String url = Utils.absolutePageUrl(SitemapPage.class, new PageParameters());
        // Without a configured website URL the address comes from the request, which behind a
        // reverse proxy is how the proxy reached this container -- useless to a crawler.
        if (NanodashPreferences.get().getConfiguredWebsiteUrl() == null) return null;
        return url;
    }

}
