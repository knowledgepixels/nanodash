package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.SiteMode;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The sitemap, so that search engines find the pages that are not reachable by following a
 * handful of links from the home page (issue #804).
 *
 * <p>Everything listed comes from the snapshots this instance already keeps in memory —
 * {@link SpaceRepository}, {@link MaintainedResourceRepository} and {@link User} — which the
 * background refresh maintains and which are restored from disk at startup. So building the
 * sitemap runs no query, and a crawler asking for it repeatedly cannot turn it into load on
 * the query fleet. The serialised document is held for {@link #REBUILD_INTERVAL} on top of
 * that, which also keeps the answer identical across the fetches a crawler makes in a row.</p>
 *
 * <p>Nanopublication landing pages are deliberately not listed. There are tens of millions of
 * them, each costing a fetch from the registry to render; listing them would mean hundreds of
 * sitemap files whose only effect would be to invite exactly the crawl this application cannot
 * afford. They are indexed the way they should be — by being reachable, canonical and
 * described (issues #168, #704, #710) — from the pages that <em>are</em> listed here.</p>
 *
 * <p>No {@code lastmod}, {@code changefreq} or {@code priority} is emitted: there is no
 * trustworthy modification time to hand for these resources, inventing one is worse than
 * leaving it out, and search engines ignore the other two.</p>
 */
public class SitemapPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(SitemapPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/sitemap.xml";

    /**
     * How long a built sitemap is reused before the snapshots are read again. The set of
     * spaces, users and resources changes by the day, not by the second.
     */
    private static final Duration REBUILD_INTERVAL = Duration.ofHours(1);

    /**
     * How long a fetched sitemap may be reused by a cache in between.
     */
    private static final Duration CACHE_DURATION = Duration.ofHours(6);

    /**
     * The most URLs one sitemap file may hold, per the sitemaps protocol. Nanodash is far
     * under it; the cap is here so that passing it shows up in the log rather than as an
     * invalid document a search engine silently drops.
     */
    static final int MAX_URLS = 50_000;

    private static volatile String document;
    private static volatile long builtAt;

    public SitemapPage(final PageParameters parameters) {
        super(parameters);

        byte[] bytes = currentDocument().getBytes(StandardCharsets.UTF_8);

        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(bytes);
            }

            @Override
            public String getContentType() {
                return "application/xml; charset=utf-8";
            }
        };

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, "sitemap.xml");
        handler.setContentDisposition(ContentDisposition.INLINE);
        handler.setCacheDuration(CACHE_DURATION);
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    private String currentDocument() {
        String current = document;
        if (current == null || System.currentTimeMillis() - builtAt > REBUILD_INTERVAL.toMillis()) {
            current = build();
            document = current;
            builtAt = System.currentTimeMillis();
        }
        return current;
    }

    /**
     * Serialises the sitemap. A {@link LinkedHashSet} holds the URLs, because the same
     * resource can be reached as more than one kind — a space that is also a user's, say —
     * and a sitemap listing a URL twice is invalid.
     *
     * @return the sitemap document
     */
    private static String build() {
        Set<String> urls = new LinkedHashSet<>();
        urls.add(Utils.absolutePageUrl(WicketApplication.get().getHomePage(), new PageParameters()));
        urls.add(Utils.absolutePageUrl(PrivacyPage.class, new PageParameters()));

        for (Space space : spaces()) {
            if (belongsHere(space.getId())) {
                urls.add(Utils.absolutePageUrl(SpacePage.class, new PageParameters().set("id", space.getId())));
            }
        }
        for (MaintainedResource resource : resources()) {
            if (belongsHere(resource.getId())) {
                urls.add(Utils.absolutePageUrl(MaintainedResourcePage.class, new PageParameters().set("id", resource.getId())));
            }
        }
        for (IRI user : users()) {
            if (belongsHere(user.stringValue())) {
                urls.add(Utils.absolutePageUrl(UserPage.class, new PageParameters().set("id", user.stringValue())));
            }
        }

        if (urls.size() > MAX_URLS) {
            logger.warn("Sitemap has {} URLs, which is past the {} a single sitemap may hold; "
                    + "the rest are left out. Split it into a sitemap index.", urls.size(), MAX_URLS);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        int count = 0;
        for (String url : urls) {
            if (++count > MAX_URLS) break;
            xml.append("  <url><loc>").append(Strings.escapeMarkup(url)).append("</loc></url>\n");
        }
        xml.append("</urlset>\n");
        logger.info("Built sitemap with {} URLs", count);
        return xml.toString();
    }

    /**
     * In site mode the instance is one space's website, so it must not advertise resources
     * belonging to other spaces as pages of its own.
     *
     * @param id the resource IRI
     * @return true if the resource belongs on this instance's sitemap
     */
    private static boolean belongsHere(String id) {
        return !SiteMode.isEnabled() || SiteMode.belongsToSite(id);
    }

    private static List<Space> spaces() {
        try {
            return SpaceRepository.get().findAll();
        } catch (Exception ex) {
            logger.warn("Could not list spaces for the sitemap: {}", ex.getMessage());
            return List.of();
        }
    }

    private static List<MaintainedResource> resources() {
        try {
            return MaintainedResourceRepository.get().findAll();
        } catch (Exception ex) {
            logger.warn("Could not list maintained resources for the sitemap: {}", ex.getMessage());
            return List.of();
        }
    }

    private static List<IRI> users() {
        try {
            // Approved users only: an unapproved account is not yet part of what this
            // instance vouches for, and the listings on the site treat it the same way.
            return new ArrayList<>(User.getUsers(true));
        } catch (Exception ex) {
            logger.warn("Could not list users for the sitemap: {}", ex.getMessage());
            return List.of();
        }
    }

}
