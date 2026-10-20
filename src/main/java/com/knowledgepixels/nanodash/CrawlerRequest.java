package com.knowledgepixels.nanodash;

import java.util.regex.Pattern;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;

/**
 * Whether the request being served comes from a crawler rather than from a person
 * (issue #804).
 *
 * <p>Crawlers are told apart by their user agent, which is a declaration rather than
 * proof. That is fine for what this is used for: the answer only ever makes a page
 * <em>cheaper</em> to serve — no queued queries, no bound session, no page buffered into
 * one — and never shows a crawler anything a visitor would not see. A visitor
 * mis-identified as a crawler sees a page without its not-yet-loaded sections; a crawler
 * that hides its identity simply gets the ordinary treatment.</p>
 *
 * <p>The matched agents are the indexing ones nginx already names in front of this
 * application, plus the link-preview and AI agents. The generic {@code bot|crawler|spider}
 * tail is deliberate: a preview bot wants the head tags, not the lazily loaded tables.</p>
 */
public class CrawlerRequest {

    private CrawlerRequest() {
    }

    // Matched case-insensitively against the whole user agent. The \b around "bot" keeps
    // "Cubot" and other device names in browser agents from being read as crawlers.
    private static final Pattern CRAWLER_AGENT = Pattern.compile(
            "googlebot|bingbot|duckduckbot|baiduspider|yandex|slurp|applebot|petalbot"
            + "|facebookexternalhit|twitterbot|linkedinbot|slackbot|discordbot|whatsapp"
            + "|telegrambot|mastodon|gptbot|oai-searchbot|chatgpt-user|claudebot"
            + "|perplexitybot|amazonbot|bytespider|semrushbot|ahrefsbot|mj12bot|dotbot"
            + "|crawler|spider|\\bbot\\b",
            Pattern.CASE_INSENSITIVE);

    // Worked out once per request: the answer is needed by the page renderer, the cache,
    // and every lazily loaded component on the page.
    private static final MetaDataKey<Boolean> IS_CRAWLER = new MetaDataKey<>() {};

    /**
     * Whether a user agent is a crawler's.
     *
     * @param userAgent the {@code User-Agent} header, or null where there is none
     * @return true if it names a crawler
     */
    public static boolean isCrawler(String userAgent) {
        return userAgent != null && CRAWLER_AGENT.matcher(userAgent).find();
    }

    /**
     * Whether the request being served comes from a crawler. Answers false outside a
     * request, so that background work is never treated as a crawl.
     *
     * @return true if this request's user agent names a crawler
     */
    public static boolean isCrawler() {
        RequestCycle cycle = RequestCycle.get();
        if (cycle == null) return false;
        Boolean known = cycle.getMetaData(IS_CRAWLER);
        if (known != null) return known;
        boolean crawler = cycle.getRequest() instanceof WebRequest request
                && isCrawler(request.getHeader("User-Agent"));
        cycle.setMetaData(IS_CRAWLER, crawler);
        return crawler;
    }

}
