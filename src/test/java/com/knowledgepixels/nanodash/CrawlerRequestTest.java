package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tells crawler user agents from people's (issue #804).
 */
class CrawlerRequestTest {

    /**
     * The indexing agents, which is what the whole feature is for.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "DuckDuckBot/1.1; (+http://duckduckgo.com/duckduckbot.html)",
            "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Safari/605.1.15 (Applebot/0.1; +http://www.apple.com/go/applebot)",
            "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)",
    })
    void recognisesSearchEngines(String userAgent) {
        assertTrue(CrawlerRequest.isCrawler(userAgent), userAgent);
    }

    /**
     * Link-preview agents get the same treatment: they are after the head tags, and the
     * lazily loaded sections are of no use to them.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
            "Twitterbot/1.0",
            "LinkedInBot/1.0 (compatible; Mozilla/5.0; Jakarta Commons-HttpClient/3.1)",
            "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)",
            "Mozilla/5.0 (compatible; Discordbot/2.0; +https://discordapp.com)",
            "WhatsApp/2.19.81 A",
            "Mastodon/4.2.1 (http://localhost/)",
    })
    void recognisesLinkPreviewBots(String userAgent) {
        assertTrue(CrawlerRequest.isCrawler(userAgent), userAgent);
    }

    /**
     * The AI agents, so that a page they fetch costs no more than one a search engine
     * fetches.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; GPTBot/1.1; +https://openai.com/gptbot",
            "Mozilla/5.0 (compatible; ClaudeBot/1.0; +claudebot@anthropic.com)",
            "Mozilla/5.0 (compatible; PerplexityBot/1.0; +https://perplexity.ai/perplexitybot)",
            "Mozilla/5.0 (compatible; Bytespider; spider-feedback@bytedance.com)",
    })
    void recognisesAiAgents(String userAgent) {
        assertTrue(CrawlerRequest.isCrawler(userAgent), userAgent);
    }

    /**
     * A browser must never be read as a crawler: it would be shown a page without its
     * not-yet-loaded sections and never see them arrive.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:133.0) Gecko/20100101 Firefox/133.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            "Mozilla/5.0 (Linux; Android 14; Cubot Note 21) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Mobile Safari/537.36",
            "curl/8.5.0",
            "",
    })
    void doesNotRecogniseBrowsers(String userAgent) {
        assertFalse(CrawlerRequest.isCrawler(userAgent), userAgent);
    }

    /**
     * A request without a user agent is treated as a person's, so that the cheaper
     * rendering is never reached by leaving the header out.
     */
    @Test
    void treatsAMissingUserAgentAsAPerson() {
        assertFalse(CrawlerRequest.isCrawler((String) null));
    }

    /**
     * Outside a request there is nothing to be a crawler: background refreshes must not
     * take the crawler path.
     */
    @Test
    void isNotACrawlerOutsideARequest() {
        assertFalse(CrawlerRequest.isCrawler());
    }

}
