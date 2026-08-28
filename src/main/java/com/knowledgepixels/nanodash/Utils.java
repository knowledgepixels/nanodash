package com.knowledgepixels.nanodash;

import com.google.common.hash.Hashing;
import com.knowledgepixels.nanodash.domain.User;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.UriSchemes;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.NotEnoughAPIInstancesException;
import org.nanopub.extra.services.QueryCall;
import org.nanopub.extra.setting.IntroNanopub;
import org.nanopub.vocabulary.FIP;
import org.nanopub.vocabulary.NPX;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Select2Choice;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class providing various helper methods for handling nanopublications, URIs, and other related functionalities.
 */
public class Utils {

    private Utils() {
    }  // no instances allowed

    /**
     * ValueFactory instance for creating RDF model objects.
     */
    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final Pattern LEADING_TAG = Pattern.compile("^\\s*<(p|div|span|img|pre|svg)(\\s|>|/).*", Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_MAIN_QUERY_URL = "https://query.knowledgepixels.com/";
    private static final String DEFAULT_MAIN_REGISTRY_URL = "https://registry.knowledgepixels.com/";

    /**
     * Generates a short name from a given IRI object.
     *
     * @param uri the IRI object
     * @return a short representation of the URI
     */
    public static String getShortNameFromURI(IRI uri) {
        return getShortNameFromURI(uri.stringValue());
    }

    /**
     * Generates a short name from a given URI string.
     *
     * @param uri the URI string
     * @return a short representation of the URI
     */
    public static String getShortNameFromURI(String uri) {
        if (uri.startsWith("https://doi.org/") || uri.startsWith("http://dx.doi.org/")) {
            return uri.replaceFirst("^https?://(dx\\.)?doi.org/", "doi:");
        }
        String nonHierarchicalName = getNonHierarchicalShortName(uri);
        if (nonHierarchicalName != null) return nonHierarchicalName;
        uri = uri.replaceFirst("\\?.*$", "");
        uri = uri.replaceFirst("[/#]$", "");
        uri = uri.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
        if (uri.contains("#")) {
            uri = uri.replaceFirst("^.*#(.*[^0-9].*)$", "$1");
        } else {
            uri = uri.replaceFirst("^.*/([^/]*[^0-9/][^/]*)$", "$1");
        }
        uri = uri.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
        uri = uri.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
        uri = URLDecoder.decode(uri, UTF_8);
        return uri;
    }

    // Length below which an identifier is shown whole rather than elided.
    private static final int SHORT_NAME_ELISION_THRESHOLD = 14;

    /**
     * Short label for the URI schemes whose identifiers are opaque rather than hierarchical
     * (issue #655). The general logic in {@link #getShortNameFromURI(String)} splits on "/" and
     * "#", which a DID has neither of, and which leaves a bare CID as a 59-character "short"
     * name. The scheme is kept — it is the part that says what kind of thing this is — and the
     * identifier is elided in the middle, keeping its start (for a CID, the multibase and codec
     * prefix) and its end (enough to tell two of them apart).
     *
     * @param uri the URI to shorten
     * @return the short label, or null if the URI is hierarchical and handled by the caller
     */
    private static String getNonHierarchicalShortName(String uri) {
        String scheme = UriSchemes.getScheme(uri);
        if (scheme == null) return null;
        if (scheme.equals("ipfs") || scheme.equals("ipns")) {
            String id = uri.substring(scheme.length() + 1).replaceFirst("^//", "");
            // A path under the CID is hierarchical after all, so the general rules apply.
            if (id.contains("/")) return null;
            return scheme + ":" + elideMiddle(id);
        }
        if (scheme.equals("did")) {
            // did:<method>:<method-specific-id> -- the method is short and meaningful, so only
            // the identifier after it is elided.
            String rest = uri.substring(4);
            int colon = rest.indexOf(':');
            if (colon < 1) return "did:" + elideMiddle(rest);
            return "did:" + rest.substring(0, colon + 1) + elideMiddle(rest.substring(colon + 1));
        }
        if (scheme.equals("at")) {
            // at://<did>/<collection>/<rkey> -- the record key identifies the record, and is
            // what the general rules would pick out too, but they choke on a URI without a path.
            String rest = uri.substring(3).replaceFirst("^//", "");
            int lastSlash = rest.lastIndexOf('/');
            if (lastSlash >= 0) return "at:" + rest.substring(lastSlash + 1);
            // No record key: what is left is the repository, which is a DID, so it shortens by
            // the rule above rather than being elided as one opaque blob.
            String repository = getNonHierarchicalShortName(rest);
            return "at:" + (repository != null ? repository : elideMiddle(rest));
        }
        return null;
    }

    private static String elideMiddle(String id) {
        if (id.length() <= SHORT_NAME_ELISION_THRESHOLD) return id;
        return id.substring(0, 4) + "…" + id.substring(id.length() - 4);
    }

    /**
     * Generates a short nanopublication ID from a given nanopublication ID or URI.
     *
     * @param npId the nanopublication ID or URI
     * @return the first 10 characters of the artifact code
     */
    public static String getShortNanopubId(Object npId) {
        return TrustyUriUtils.getArtifactCode(npId.toString()).substring(0, 10);
    }

    // Nanopublications are immutable, so a cached one never goes out of date and the only
    // reason to drop one is to keep the cache from growing without end — which it used to,
    // being a plain map that never evicted. Bounded like the query caches in ApiCache, and
    // concurrent, as retrieval also happens on background threads (see NanopubLookup).
    private static final int MAX_CACHED_NANOPUBS = 10_000;

    private static final Cache<String, Nanopub> nanopubs = CacheBuilder.newBuilder()
            .maximumSize(MAX_CACHED_NANOPUBS)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build();

    // Registry fetches go through our own client rather than the library's shared one
    // (NanopubUtils.getHttpClient), which allows 10 seconds for each of connecting, reading
    // and waiting for a pooled connection — up to 30 seconds for a single fetch, on a thread
    // that may be rendering a page. These bounds are tighter, and in particular a saturated
    // pool fails fast here instead of queueing threads up behind it.
    private static final int REGISTRY_CONNECT_TIMEOUT_MS = 5_000;
    private static final int REGISTRY_SOCKET_TIMEOUT_MS = 10_000;
    private static final int REGISTRY_CONNECTION_REQUEST_TIMEOUT_MS = 2_000;

    private static final CloseableHttpClient registryHttpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(REGISTRY_CONNECT_TIMEOUT_MS)
                    .setSocketTimeout(REGISTRY_SOCKET_TIMEOUT_MS)
                    .setConnectionRequestTimeout(REGISTRY_CONNECTION_REQUEST_TIMEOUT_MS)
                    .build())
            .setMaxConnTotal(64)
            .setMaxConnPerRoute(16)
            .build();

    /**
     * The HTTP client to use for registry requests: same behaviour as the library's shared
     * one, but with tighter timeouts (see {@link #getNanopub(String)}).
     *
     * @return the shared registry HTTP client
     */
    public static CloseableHttpClient getRegistryHttpClient() {
        return registryHttpClient;
    }

    /**
     * The current nanopub cache content, for persisting across restarts (issue #570; see
     * {@link ApiCachePersistence}). Nanopubs are immutable, so unlike the query responses
     * they carry no staleness concerns at all.
     *
     * @return a copy of the cached nanopubs, keyed by artifact code
     */
    static Map<String, Nanopub> exportCachedNanopubs() {
        return new HashMap<>(nanopubs.asMap());
    }

    /**
     * Restores previously exported nanopubs into the cache, skipping any that are already
     * cached. Meant to run once at startup, before the instance serves requests.
     *
     * @param map the nanopubs to restore, keyed by artifact code
     * @return the number of restored nanopubs
     */
    static int importCachedNanopubs(Map<String, Nanopub> map) {
        int count = 0;
        for (Map.Entry<String, Nanopub> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            if (nanopubs.getIfPresent(e.getKey()) != null) continue;
            nanopubs.put(e.getKey(), e.getValue());
            count++;
        }
        return count;
    }

    /**
     * Adds a nanopublication to the local cache so it can be retrieved immediately
     * without needing to fetch it from the registry.
     *
     * @param np the nanopublication to cache
     */
    public static void cacheNanopub(Nanopub np) {
        String artifactCode = GetNanopub.getArtifactCode(np.getUri().stringValue()).toString();
        nanopubs.put(artifactCode, np);
    }

    /**
     * Retrieves a Nanopub object based on the given URI or artifact code.
     *
     * @param uriOrArtifactCode the URI or artifact code of the nanopublication
     * @return the Nanopub object, or null if not found
     */
    public static Nanopub getNanopub(String uriOrArtifactCode) {
        String artifactCode = GetNanopub.getArtifactCode(uriOrArtifactCode).toString();
        Nanopub cached = nanopubs.getIfPresent(artifactCode);
        if (cached != null) return cached;
        // A request thread gets one attempt: the retries are there to ride out a flaky
        // registry, and doing that while a user waits only multiplies the time they spend
        // looking at nothing. Background threads keep the full three.
        int attempts = RequestCycle.get() != null ? 1 : 3;
        for (int i = 0; i < attempts; i++) {
            Nanopub np = GetNanopub.get(artifactCode, registryHttpClient);
            if (np != null) {
                nanopubs.put(artifactCode, np);
                return np;
            }
        }
        return null;
    }

    /**
     * Strips a sub-IRI of a nanopublication (e.g. an embedded resource IRI like
     * {@code <np-uri>/template}) down to the nanopublication ID itself, i.e. up to
     * and including the trusty artifact code. An ID without a sub-path is returned
     * unchanged.
     *
     * @param id the ID to strip
     * @return the nanopublication ID
     */
    public static String stripToNanopubId(String id) {
        return id.replaceFirst("^(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
    }

    // Wicket URL-encodes the session into a path parameter when the client has not (yet)
    // returned a cookie. Harmless for a link the browser follows, poisonous for a URL that
    // outlives the request — see absolutePageUrl.
    private static final Pattern JSESSIONID_PATTERN = Pattern.compile(";jsessionid=[^?/;]*", Pattern.CASE_INSENSITIVE);

    // Splits an absolute URL into its origin (scheme and authority) and everything after it.
    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.\\-]*://[^/?#]*)(.*)$");

    /**
     * The absolute URL of a mounted page, for handing to something outside this request:
     * embedding in a downloaded file, giving to a third-party service to fetch, or showing
     * the user to copy.
     *
     * <p>Any {@code ;jsessionid=} Wicket added is stripped. Such a URL is not merely ugly —
     * it is bound to one visitor's session, so a calendar feed URL carrying one would break
     * once the session expired, and would hand out a live session identifier to anyone the
     * user shared it with.</p>
     *
     * <p>The host comes from the configured website URL where there is one, not from the
     * request. Behind a reverse proxy the request reveals only how the proxy reached this
     * container — {@code http://127.0.1.1:37373/} — which is useless to a calendar client or
     * to Google, both of which have to fetch the URL themselves from outside.</p>
     *
     * @param pageClass the mounted page
     * @param params    the page parameters
     * @return the full URL, including scheme and host
     */
    public static String absolutePageUrl(Class<? extends org.apache.wicket.Page> pageClass, PageParameters params) {
        RequestCycle cycle = RequestCycle.get();
        String url = cycle.urlFor(pageClass, params).toString();
        String requestUrl = stripSessionId(cycle.getUrlRenderer().renderFullUrl(Url.parse(url)));
        return rebaseOnWebsiteUrl(requestUrl, NanodashPreferences.get().getConfiguredWebsiteUrl());
    }

    /**
     * Moves a request-derived absolute URL onto the address this instance is published at.
     *
     * <p>Only the origin is taken from the website URL; the path and query stay as Wicket
     * rendered them, so the mount paths remain the single source of truth for where a page
     * lives. A website URL that carries a path of its own (an instance published under
     * {@code /nanodash/}, say) has it prefixed, unless the request path already includes it
     * because the servlet container is mounted there too.</p>
     *
     * @param requestUrl the absolute URL derived from the current request
     * @param websiteUrl the configured website URL, or null when this instance has none
     * @return the URL rebased on the website URL, or {@code requestUrl} unchanged if there is
     *         no usable website URL
     */
    static String rebaseOnWebsiteUrl(String requestUrl, String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) return requestUrl;
        Matcher website = ORIGIN_PATTERN.matcher(websiteUrl.trim());
        Matcher request = ORIGIN_PATTERN.matcher(requestUrl);
        if (!website.matches() || !request.matches()) {
            logger.warn("Cannot rebase '{}' on website URL '{}'; leaving it as it is", requestUrl, websiteUrl);
            return requestUrl;
        }
        String prefix = website.group(2).replaceFirst("[?#].*$", "").replaceFirst("/+$", "");
        String rest = request.group(2);
        boolean alreadyPrefixed = rest.equals(prefix) || rest.startsWith(prefix + "/")
                || rest.startsWith(prefix + "?") || rest.startsWith(prefix + "#");
        return website.group(1) + (alreadyPrefixed ? "" : prefix) + rest;
    }

    /**
     * Removes a {@code ;jsessionid=...} path parameter from a URL. See
     * {@link #absolutePageUrl} for why this must happen before a URL leaves the request.
     *
     * @param url the URL, possibly session-decorated
     * @return the URL without its session id
     */
    static String stripSessionId(String url) {
        return JSESSIONID_PATTERN.matcher(url).replaceAll("");
    }

    /**
     * URL-encodes the string representation of the given object using UTF-8 encoding.
     *
     * @param o the object to be URL-encoded
     * @return the URL-encoded string
     */
    public static String urlEncode(Object o) {
        return URLEncoder.encode((o == null ? "" : o.toString()), Charsets.UTF_8);
    }

    public static String truncateLabel(String label) {
        if (label != null && label.length() > 120) {
            return label.substring(0, 100) + "...";
        }
        return label;
    }

    /**
     * Truncates an over-long entity label for display in a link or breadcrumb:
     * labels longer than 60 characters are cut to 47 characters with an ellipsis
     * ("...") appended, so a single long label (e.g. a full IRI tail) never blows
     * up the surrounding UI. Shorter labels are returned unchanged. The full label
     * stays available on the entity's own page, which shows it as the title.
     *
     * @param label the label to truncate, or null
     * @return the truncated label, or the original if 60 characters or shorter
     */
    public static String truncateLinkLabel(String label) {
        if (label == null || label.length() <= 60) {
            return label;
        }
        return label.substring(0, 47).stripTrailing() + "...";
    }

    /**
     * Builds the HTML body for a menu entry whose label may begin with a leading
     * symbol/emoji used as the entry's icon. If {@code label} starts with a token
     * of symbol/emoji characters (no letters or digits) followed by whitespace,
     * that token is wrapped in the {@code .actionmenu-icon} slot and the remaining
     * text follows it (both escaped). Returns {@code null} when there is no such
     * leading icon, so callers can fall back to the plain (escaped) label.
     *
     * @param label the menu entry label
     * @return the icon+text HTML body to render with escaping disabled, or null
     */
    public static String menuEntryIconBodyHtml(String label) {
        if (label == null) {
            return null;
        }
        int sp = -1;
        for (int i = 0; i < label.length(); i++) {
            if (Character.isWhitespace(label.charAt(i))) {
                sp = i;
                break;
            }
        }
        if (sp <= 0) {
            return null;
        }
        String icon = label.substring(0, sp);
        String rest = label.substring(sp).replaceFirst("^\\s+", "");
        if (rest.isEmpty()) {
            return null;
        }
        // Only a pure symbol/emoji token (no letters or digits) counts as an icon.
        if (icon.codePoints().anyMatch(Character::isLetterOrDigit)) {
            return null;
        }
        return "<span class=\"actionmenu-icon\">" + Strings.escapeMarkup(icon) + "</span>" + Strings.escapeMarkup(rest);
    }

    /**
     * URL-decodes the string representation of the given object using UTF-8 encoding.
     *
     * @param o the object to be URL-decoded
     * @return the URL-decoded string
     */
    public static String urlDecode(Object o) {
        return URLDecoder.decode((o == null ? "" : o.toString()), Charsets.UTF_8);
    }

    /**
     * Generates a URL with the given base and appends the provided PageParameters as query parameters.
     *
     * @param base       the base URL
     * @param parameters the PageParameters to append
     * @return the complete URL with parameters
     */
    public static String getUrlWithParameters(String base, PageParameters parameters) {
        try {
            URIBuilder u = new URIBuilder(base);
            for (String key : parameters.getNamedKeys()) {
                for (StringValue value : parameters.getValues(key)) {
                    if (!value.isNull()) {
                        u.addParameter(key, value.toString());
                    }
                }
            }
            return u.build().toString();
        } catch (URISyntaxException ex) {
            logger.error("Could not build URL with parameters: {} {}", base, parameters, ex);
            return "/";
        }
    }

    /**
     * Generates a short name for a public key or public key hash.
     *
     * @param pubkeyOrPubkeyhash the public key (64 characters) or public key hash (40 characters)
     * @return a short representation of the public key or public key hash
     */
    public static String getShortPubkeyName(String pubkeyOrPubkeyhash) {
        if (pubkeyOrPubkeyhash.length() == 64) {
            return pubkeyOrPubkeyhash.replaceFirst("^(.{8}).*$", "$1");
        } else {
            return pubkeyOrPubkeyhash.replaceFirst("^(.).{39}(.{5}).*$", "$1..$2..");
        }
    }

    /**
     * Generates a short label for a public key or public key hash, including its status (local or approved).
     *
     * @param pubkeyOrPubkeyhash the public key (64 characters) or public key hash (40 characters)
     * @param user               the IRI of the user associated with the public key
     * @return a short label indicating the public key and its status
     */
    public static String getShortPubkeyhashLabel(String pubkeyOrPubkeyhash, IRI user) {
        String s = getShortPubkeyName(pubkeyOrPubkeyhash);
        NanodashSession session = NanodashSession.get();
        List<String> l = new ArrayList<>();
        if (pubkeyOrPubkeyhash.equals(session.getPubkeyString()) || pubkeyOrPubkeyhash.equals(session.getPubkeyhash())) {
            l.add("local");
        }
        // TODO: Make this more efficient:
        String hashed = Utils.createSha256HexHash(pubkeyOrPubkeyhash);
        if (User.getPubkeyhashes(user, true).contains(pubkeyOrPubkeyhash) || User.getPubkeyhashes(user, true).contains(hashed)) {
            l.add("approved");
        }
        if (!l.isEmpty()) {
            s += " (" + String.join("/", l) + ")";
        }
        return s;
    }

    /**
     * Retrieves the name of the public key location based on the public key.
     *
     * @param pubkeyhash the public key string
     * @return the name of the public key location
     */
    public static String getPubkeyLocationName(String pubkeyhash) {
        return getPubkeyLocationName(pubkeyhash, getShortPubkeyName(pubkeyhash));
    }

    /**
     * Retrieves the name of the public key location, or returns a fallback name if not found.
     * If the key location is localhost, it returns "localhost".
     *
     * @param pubkeyhash the public key string
     * @param fallback   the fallback name to return if the key location is not found
     * @return the name of the public key location or the fallback name
     */
    public static String getPubkeyLocationName(String pubkeyhash, String fallback) {
        IRI keyLocation = User.getUserData().getKeyLocationForPubkeyHash(pubkeyhash);
        if (keyLocation == null) {
            return fallback;
        }
        if (keyLocation.stringValue().equals("http://localhost:37373/")) {
            return "localhost";
        }
        return keyLocation.stringValue().replaceFirst("https?://(nanobench\\.)?(nanodash\\.(?=.*\\..))?(.*[^/])/?$", "$3");
    }

    /**
     * Generates a short label for a public key location, including its status (local or approved).
     *
     * @param pubkeyhash the public key string
     * @param user       the IRI of the user associated with the public key
     * @return a short label indicating the public key location and its status
     */
    public static String getShortPubkeyLocationLabel(String pubkeyhash, IRI user) {
        String s = getPubkeyLocationName(pubkeyhash);
        NanodashSession session = NanodashSession.get();
        List<String> l = new ArrayList<>();
        if (pubkeyhash.equals(session.getPubkeyhash())) {
            l.add("local");
        }
        // TODO: Make this more efficient:
        if (User.getPubkeyhashes(user, true).contains(pubkeyhash)) {
            l.add("approved");
        }
        if (!l.isEmpty()) {
            s += " (" + String.join("/", l) + ")";
        }
        return s;
    }

    /**
     * Checks if a given public key has a Nanodash location.
     * A Nanodash location is identified by specific keywords in the key location.
     *
     * @param pubkeyhash the public key to check
     * @return true if the public key has a Nanodash location, false otherwise
     */
    public static boolean hasNanodashLocation(String pubkeyhash) {
        IRI keyLocation = User.getUserData().getKeyLocationForPubkeyHash(pubkeyhash);
        if (keyLocation == null) {
            return true; // potentially a Nanodash location
        }
        if (keyLocation.stringValue().contains("nanodash")) {
            return true;
        }
        if (keyLocation.stringValue().contains("nanobench")) {
            return true;
        }
        if (keyLocation.stringValue().contains(":37373")) {
            return true;
        }
        return false;
    }

    /**
     * Retrieves the short ORCID ID from an IRI object.
     *
     * @param orcidIri the IRI object representing the ORCID ID
     * @return the short ORCID ID as a string
     */
    public static String getShortOrcidId(IRI orcidIri) {
        return orcidIri.stringValue().replaceFirst("^https://orcid.org/", "");
    }

    /**
     * Retrieves the URI postfix from a given URI object.
     *
     * @param uri the URI object from which to extract the postfix
     * @return the URI postfix as a string
     */
    public static String getUriPostfix(Object uri) {
        String s = uri.toString();
        if (s.contains("#")) {
            return s.replaceFirst("^.*#(.*)$", "$1");
        }
        return s.replaceFirst("^.*/(.*)$", "$1");
    }

    /**
     * Retrieves the URI prefix from a given URI object.
     *
     * @param uri the URI object from which to extract the prefix
     * @return the URI prefix as a string
     */
    public static String getUriPrefix(Object uri) {
        String s = uri.toString();
        if (s.contains("#")) {
            return s.replaceFirst("^(.*#).*$", "$1");
        }
        return s.replaceFirst("^(.*/).*$", "$1");
    }

    /**
     * Checks if a given string is a valid URI postfix.
     * A valid URI postfix does not contain a colon (":").
     *
     * @param s the string to check
     * @return true if the string is a valid URI postfix, false otherwise
     */
    public static boolean isUriPostfix(String s) {
        return !s.contains(":");
    }

    /**
     * Retrieves the location of a given IntroNanopub.
     *
     * @param inp the IntroNanopub from which to extract the location
     * @return the IRI location of the nanopublication, or null if not found
     */
    public static IRI getLocation(IntroNanopub inp) {
        NanopubSignatureElement el = getNanopubSignatureElement(inp);
        for (KeyDeclaration kd : inp.getKeyDeclarations()) {
            if (el.getPublicKeyString().equals(kd.getPublicKeyString())) {
                return kd.getKeyLocation();
            }
        }
        return null;
    }

    /**
     * Retrieves the NanopubSignatureElement from a given IntroNanopub.
     *
     * @param inp the IntroNanopub from which to extract the signature element
     * @return the NanopubSignatureElement associated with the nanopublication
     */
    public static NanopubSignatureElement getNanopubSignatureElement(IntroNanopub inp) {
        try {
            return SignatureUtils.getSignatureElement(inp.getNanopub());
        } catch (MalformedCryptoElementException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Retrieves a Nanopub object from a given URI if it is a potential Trusty URI.
     *
     * @param uri the URI to check and retrieve the Nanopub from
     * @return the Nanopub object if found, or null if not a known nanopublication
     */
    public static Nanopub getAsNanopub(String uri) {
        if (uri == null) {
            return null;
        }
        if (TrustyUriUtils.isPotentialTrustyUri(uri)) {
            try {
                return Utils.getNanopub(uri);
            } catch (Exception ex) {
                logger.error("The given URI is not a known nanopublication: {}", uri, ex);
            }
        }
        return null;
    }

    // A conservative static-SVG subset: basic shapes, text, grouping, and links.
    // Everything not allowed is dropped — in particular script/foreignObject/style
    // and all event handlers, plus use/image, whose href would reach outside the
    // sanitized document. Used by SVG views (QueryResultSvg) and, folded into the
    // HTML policy below, by inline SVG in HTML snippets coming from queries.
    // Attribute-name matching is case-sensitive and the matched spelling is emitted
    // verbatim, so the camelCase SVG attributes are listed in both spellings
    // (browsers also map the lowercase form back via the SVG attribute-adjustment
    // table, but the camelCase form works everywhere, including XML contexts).
    private static final String[] SVG_ELEMENTS = {"svg", "g", "defs", "marker", "title", "desc",
            "rect", "circle", "ellipse", "line", "polyline", "polygon", "path", "text", "tspan"};

    // The SVG elements plus the link element they can be wrapped in, which is
    // shared with HTML and therefore allowed separately by each policy.
    private static final String[] SVG_ELEMENTS_WITH_LINK = concat(SVG_ELEMENTS, "a");

    private static final String[] SVG_ATTRIBUTES = {"viewbox", "viewBox", "width", "height",
            "preserveaspectratio", "preserveAspectRatio", "xmlns",
            "x", "y", "x1", "y1", "x2", "y2", "cx", "cy", "r", "rx", "ry",
            "d", "points", "dx", "dy", "transform",
            "fill", "fill-opacity", "fill-rule",
            "stroke", "stroke-width", "stroke-opacity", "stroke-linecap",
            "stroke-linejoin", "stroke-dasharray", "stroke-dashoffset",
            "stroke-miterlimit", "clip-rule", "opacity",
            "font-family", "font-size", "font-weight", "font-style",
            "text-anchor", "dominant-baseline", "text-decoration",
            "marker-start", "marker-mid", "marker-end",
            "markerwidth", "markerWidth", "markerheight", "markerHeight",
            "refx", "refX", "refy", "refY", "orient", "markerunits", "markerUnits",
            "id"};

    // Drawing tools export SVG with the paint in a style attribute
    // (style="fill:rgb(120,184,134);") rather than in presentation attributes. The
    // sanitizer allows no style attribute -- it is the one attribute whose value can pull
    // in external resources -- so those declarations used to be dropped, and every shape
    // fell back to the SVG default fill: an all-black figure. They are therefore rewritten
    // into the equivalent presentation attributes first, which the existing allow-list
    // then validates like any other: nothing new is allowed through, and a declaration
    // whose property is not on the list (or whose value could reference something, i.e.
    // contains a function call other than a colour) is dropped as before.
    private static final Set<String> STYLE_PROPERTIES_AS_ATTRIBUTES = Set.of(
            "fill", "fill-opacity", "fill-rule",
            "stroke", "stroke-width", "stroke-opacity", "stroke-linecap",
            "stroke-linejoin", "stroke-dasharray", "stroke-dashoffset",
            "stroke-miterlimit", "clip-rule", "opacity",
            "font-family", "font-size", "font-weight", "font-style",
            "text-anchor", "dominant-baseline", "text-decoration");

    // A style attribute on any element, with either quoting style.
    private static final Pattern STYLE_ATTRIBUTE = Pattern.compile(
            "\\sstyle\\s*=\\s*(\"[^\"]*\"|'[^']*')", Pattern.CASE_INSENSITIVE);

    // A value safe to move into a presentation attribute: no function call other than the
    // colour functions, so url(...) and expression(...) can never survive.
    private static final Pattern SAFE_STYLE_VALUE = Pattern.compile(
            "(?:[-#%.,0-9a-zA-Z_ ]|(?:rgb|rgba|hsl|hsla)\\([-0-9%.,\\s]*\\))+");

    private static String styleToPresentationAttributes(String raw) {
        if (raw == null || !raw.contains("style")) return raw;
        Matcher m = STYLE_ATTRIBUTE.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String quoted = m.group(1);
            String declarations = quoted.substring(1, quoted.length() - 1);
            StringBuilder attributes = new StringBuilder();
            for (String declaration : declarations.split(";")) {
                int colon = declaration.indexOf(':');
                if (colon < 0) continue;
                String property = declaration.substring(0, colon).trim().toLowerCase();
                String value = declaration.substring(colon + 1).trim();
                if (!STYLE_PROPERTIES_AS_ATTRIBUTES.contains(property)) continue;
                if (value.isEmpty() || !SAFE_STYLE_VALUE.matcher(value).matches()) continue;
                attributes.append(' ').append(property).append("=\"").append(value).append('"');
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(attributes.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String[] concat(String[] values, String... more) {
        String[] result = Arrays.copyOf(values, values.length + more.length);
        System.arraycopy(more, 0, result, values.length, more.length);
        return result;
    }

    /**
     * Adds the static-SVG subset (elements and presentation attributes, but not the
     * {@code a} element itself, whose link handling differs per policy) to a policy
     * builder.
     *
     * @param builder the policy builder to extend
     * @return the given builder
     */
    private static HtmlPolicyBuilder allowSvgSubset(HtmlPolicyBuilder builder) {
        return builder
                .allowElements(SVG_ELEMENTS)
                .allowWithoutAttributes("svg", "g", "defs", "title", "desc", "text", "tspan")
                .allowAttributes(SVG_ATTRIBUTES).onElements(SVG_ELEMENTS_WITH_LINK);
    }

    // Links a view emits may point at any scheme a nanopublication is allowed to reference
    // (issue #655); without them here the sanitizer silently strips the href. All of these are
    // inert reference schemes -- nothing script-executing is added.
    private static final String[] SANITIZER_URL_PROTOCOLS =
            UriSchemes.ALLOWED_SCHEMES.stream().sorted().toArray(String[]::new);

    private static final PolicyFactory htmlSanitizePolicy = allowSvgSubset(new HtmlPolicyBuilder()
            .allowCommonBlockElements().allowCommonInlineFormattingElements()
            .allowUrlProtocols(SANITIZER_URL_PROTOCOLS).allowUrlProtocols("mailto")
            .allowElements("a").allowAttributes("href").onElements("a")
            .allowElements("img").allowAttributes("src").onElements("img")
            .allowElements("pre")
            .requireRelNofollowOnLinks()).toFactory();

    /**
     * Sanitizes raw HTML input to ensure safe rendering. Inline SVG is kept, reduced
     * to the same static subset as in {@link #sanitizeSvg(String)}.
     *
     * @param rawHtml the raw HTML input to sanitize
     * @return sanitized HTML string
     */
    public static String sanitizeHtml(String rawHtml) {
        return htmlSanitizePolicy.sanitize(styleToPresentationAttributes(normalizeSelfClosedSvgTags(rawHtml)));
    }

    private static final PolicyFactory svgSanitizePolicy = allowSvgSubset(new HtmlPolicyBuilder()
            .allowUrlProtocols(SANITIZER_URL_PROTOCOLS)
            .allowElements("a")
            .allowWithoutAttributes("a")
            .allowAttributes("href").onElements("a")).toFactory();

    // XML-style self-closed SVG tags (<rect .../>): the HTML-parsing sanitizer
    // ignores the slash on non-void elements and would re-parent all following
    // siblings as children of the "unclosed" element — which in SVG makes them
    // invisible (shape elements don't render children). Expanded to explicit end
    // tags before sanitizing. Quoted attribute values (which may contain ">") are
    // matched as units so the tag end is found correctly, and the attribute part
    // must start with whitespace so that a longer element name is not matched as
    // one of the listed ones plus attributes. Only SVG element names are expanded,
    // so that HTML void elements (<br/>, <img/>) and self-closed links keep their
    // HTML parsing.
    private static final Pattern SELF_CLOSED_SVG_TAG = Pattern.compile(
            "<(" + String.join("|", SVG_ELEMENTS) + ")((?:\\s(?:[^<>\"']|\"[^\"]*\"|'[^']*')*)?)/>",
            Pattern.CASE_INSENSITIVE);

    private static String normalizeSelfClosedSvgTags(String raw) {
        if (raw == null) return null;
        return SELF_CLOSED_SVG_TAG.matcher(raw).replaceAll("<$1$2></$1>");
    }

    /**
     * Sanitizes SVG markup (as produced by an SVG view's query) down to a static
     * subset that is safe to embed inline: shapes, text, grouping, and http(s)
     * links, with no scripting, styling, or external-reference capability.
     *
     * @param rawSvg the raw SVG markup
     * @return sanitized SVG markup
     */
    public static String sanitizeSvg(String rawSvg) {
        return svgSanitizePolicy.sanitize(styleToPresentationAttributes(normalizeSelfClosedSvgTags(rawSvg)));
    }

    // A whole inline SVG figure, dropped wherever HTML is reduced to text: its
    // labels would come out as a run of disconnected words.
    private static final Pattern SVG_FRAGMENT = Pattern.compile("<svg\\b.*?</svg\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(?:([0-9]{1,7})|[xX]([0-9a-fA-F]{1,6}));");

    /**
     * Reduces an HTML fragment to plain text: SVG figures are dropped entirely, the
     * remaining tags are stripped, the named and numeric entities are unescaped (the
     * sanitizer emits quotes etc. as numeric entities like {@code &#34;}), and
     * whitespace is collapsed.
     *
     * @param html the HTML fragment
     * @return the plain text
     */
    public static String htmlToPlainText(String html) {
        String text = SVG_FRAGMENT.matcher(html).replaceAll(" ");
        text = text.replaceAll("<[^>]*>", " ");
        text = text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&nbsp;", " ");
        text = NUMERIC_ENTITY.matcher(text).replaceAll(m -> {
            int codePoint = m.group(1) != null
                    ? Integer.parseInt(m.group(1))
                    : Integer.parseInt(m.group(2), 16);
            return Matcher.quoteReplacement(new String(Character.toChars(codePoint)));
        });
        text = text.replace("&amp;", "&");
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Reduces a value to the text to use for it in a label. HTML is a rendering
     * detail that has no place in a label, so an SVG figure is dropped wherever it
     * occurs, and a value that is HTML is further reduced to its text.
     *
     * @param value the raw value
     * @return the value as label text
     */
    public static String toLabelText(String value) {
        if (value == null) return null;
        if (looksLikeHtml(value)) {
            return htmlToPlainText(value);
        }
        return SVG_FRAGMENT.matcher(value).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Checks if a given string is likely to be HTML content.
     *
     * @param value the string to check
     * @return true if the given string is HTML content, false otherwise
     */
    public static boolean looksLikeHtml(String value) {
        return LEADING_TAG.matcher(value).find();
    }

    public static boolean isDate(String value) {
        return isDateLiteral(value) || isDateTimeLiteral(value);
    }

    public static boolean isDateLiteral(String value) {
        if (value == null || value.isBlank() || value.contains("T")) {
            return false;
        }
        try {
            DateTimeFormatter.ISO_DATE.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    /**
     * Checks whether a (raw query-result) string looks like an ISO-8601 date-time literal.
     *
     * @param value the string to check
     * @return true if the string parses as a xsd:dateTime-style value
     */
    public static boolean isDateTimeLiteral(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return true;
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    /**
     * Renders a {@code <time>} element for an ISO-8601 date-time value. The machine-readable
     * value goes in the {@code datetime} attribute; client-side script (nanodash.js) rewrites
     * the visible text to a relative form ("10 minutes ago") in the viewer's local timezone and
     * puts the absolute date-time in the tooltip. If script does not run, {@code fallbackText}
     * remains visible.
     *
     * @param isoValue     the ISO-8601 date-time string (machine-readable)
     * @param fallbackText the human-readable text shown when script is unavailable
     * @return an HTML {@code <time>} element string (caller must render with escaping disabled)
     */
    public static String friendlyDateHtml(String isoValue, String fallbackText) {
        return "<time class=\"friendly-date\" datetime=\"" + Strings.escapeMarkup(isoValue) + "\">" + Strings.escapeMarkup(fallbackText) + "</time>";
    }

    /**
     * Converts PageParameters to a URL-encoded string representation.
     *
     * @param params the PageParameters to convert
     * @return a string representation of the parameters in URL-encoded format
     */
    public static String getPageParametersAsString(PageParameters params) {
        String s = "";
        for (String n : params.getNamedKeys()) {
            if (!s.isEmpty()) {
                s += "&";
            }
            s += n + "=" + URLEncoder.encode(params.get(n).toString(), Charsets.UTF_8);
        }
        return s;
    }

    /**
     * Sets a minimal escape markup function for a Select2Choice component.
     * This function replaces certain characters and formats the display of choices.
     *
     * @param selectItem the Select2Choice component to set the escape markup for
     */
    public static void setSelect2ChoiceMinimalEscapeMarkup(Select2Choice<?> selectItem) {
        // The note of a to-be-minted value is not part of the value, so it is set in its own
        // span and styled as an aside rather than as the term itself (issue #652).
        String noteRegex = TO_BE_MINTED_NOTE.replace("(", "\\(").replace(")", "\\)");
        selectItem.getSettings().setEscapeMarkup("function(markup) {" + "return markup" + ".replaceAll('<','&lt;').replaceAll('>', '&gt;')" + ".replace(/^(.*?) - /, '<span class=\"term\">$1</span><br>')" + ".replace(/\\((https?:[\\S]+)\\)$/, '<br><code>$1</code>')" + ".replace(/^(.*) " + noteRegex + "$/, '<span class=\"term\">$1</span> <span class=\"mint-note\">" + TO_BE_MINTED_NOTE + "</span>')" + ".replace(/^([^<].*)$/, '<span class=\"term\">$1</span>')" + ";}");
    }

    /**
     * Checks if a nanopublication is of a specific class.
     *
     * @param np       the nanopublication to check
     * @param classIri the IRI of the class to check against
     * @return true if the nanopublication is of the specified class, false otherwise
     */
    public static boolean isNanopubOfClass(Nanopub np, IRI classIri) {
        return NanopubUtils.getTypes(np).contains(classIri);
    }

    /**
     * Checks if a nanopublication uses a specific predicate in its assertion.
     *
     * @param np           the nanopublication to check
     * @param predicateIri the IRI of the predicate to look for
     * @return true if the predicate is used in the assertion, false otherwise
     */
    public static boolean usesPredicateInAssertion(Nanopub np, IRI predicateIri) {
        for (Statement st : np.getAssertion()) {
            if (predicateIri.equals(st.getPredicate())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a map of FOAF names from the nanopublication's pubinfo.
     *
     * @param np the nanopublication from which to extract FOAF names
     * @return a map where keys are subjects and values are FOAF names
     */
    public static Map<String, String> getFoafNameMap(Nanopub np) {
        Map<String, String> foafNameMap = new HashMap<>();
        for (Statement st : np.getPubinfo()) {
            if (st.getPredicate().equals(FOAF.NAME) && st.getObject() instanceof Literal objL) {
                foafNameMap.put(st.getSubject().stringValue(), objL.stringValue());
            }
        }
        return foafNameMap;
    }

    /**
     * Creates an SHA-256 hash of the string representation of an object and returns it as a hexadecimal string.
     *
     * @param obj the object to hash
     * @return the SHA-256 hash of the object's string representation in hexadecimal format
     */
    public static String createSha256HexHash(Object obj) {
        return Hashing.sha256().hashString(obj.toString(), StandardCharsets.UTF_8).toString();
    }

    /**
     * Gets the types of a nanopublication.
     *
     * @param np the nanopublication from which to extract types
     * @return a list of IRI types associated with the nanopublication
     */
    public static List<IRI> getTypes(Nanopub np) {
        List<IRI> l = new ArrayList<>();
        for (IRI t : NanopubUtils.getTypes(np)) {
            if (t.equals(FIP.AVAILABLE_FAIR_ENABLING_RESOURCE)) {
                continue;
            }
            if (t.equals(FIP.FAIR_ENABLING_RESOURCE_TO_BE_DEVELOPED)) {
                continue;
            }
            if (t.equals(FIP.AVAILABLE_FAIR_SUPPORTING_RESOURCE)) {
                continue;
            }
            if (t.equals(FIP.FAIR_SUPPORTING_RESOURCE_TO_BE_DEVELOPED)) {
                continue;
            }
            l.add(t);
        }
        return l;
    }

    /**
     * Gets a label for a type IRI.
     *
     * @param typeIri the IRI of the type
     * @return a label for the type, potentially truncated
     */
    public static String getTypeLabel(IRI typeIri) {
        if (typeIri.equals(FIP.FAIR_ENABLING_RESOURCE)) {
            return "FER";
        }
        if (typeIri.equals(FIP.FAIR_SUPPORTING_RESOURCE)) {
            return "FSR";
        }
        if (typeIri.equals(FIP.FAIR_IMPLEMENTATION_PROFILE)) {
            return "FIP";
        }
        if (typeIri.equals(NPX.DECLARED_BY)) {
            return "user intro";
        }
        String l = typeIri.stringValue();
        l = l.replaceFirst("^.*[/#]([^/#]+)[/#]?$", "$1");
        l = l.replaceFirst("^(.+)Nanopub$", "$1");
        if (l.length() > 25) {
            l = l.substring(0, 20) + "...";
        }
        return l;
    }

    /**
     * Gets a label for a URI.
     *
     * @param uri the URI to get the label from
     * @return a label for the URI, potentially truncated
     */
    public static String getUriLabel(String uri) {
        if (uri == null) {
            return "";
        }
        String uriLabel = uri;
        if (uriLabel.matches(".*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}([^A-Za-z0-9-_].*)?")) {
            String newUriLabel = uriLabel.replaceFirst("(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{8})[A-Za-z0-9-_]{35}([^A-Za-z0-9-_].*)?", "$1...$2");
            if (newUriLabel.length() <= 70) {
                return newUriLabel;
            }
        }
        if (uriLabel.length() > 70) {
            return uri.substring(0, 30) + "..." + uri.substring(uri.length() - 30);
        }
        return uriLabel;
    }

    /**
     * Whether a term typed into a choice field can be entered as a plain name for a resource that
     * has no identifier yet (issue #652): it is not a URI already, and the IRI validator accepts
     * it once a prefix is put in front of it -- the field's own, or the local one when it has
     * none, in which case the nanopublication mints it under its own namespace.
     *
     * @param term the term typed into the field
     * @return true if the term can be offered as a plain name
     */
    public static boolean isPlainName(String term) {
        if (term == null || term.isBlank()) return false;
        if (isUriValue(term)) return false;
        // Same rule as the validator: no colon, hash or whitespace, and well-formed as a URI once
        // prefixed.
        if (!term.matches("[^:#\\s]+")) return false;
        return isWellFormedUri(LocalUri.PREFIX + term);
    }

    /**
     * How a value that a nanopublication will mint under its own namespace is shown in a choice
     * field: with the local prefix in front of it and marked as not being an identifier yet, e.g.
     * "local:john (to be minted)" (issue #652). See
     * {@link com.knowledgepixels.nanodash.template.TemplateContext#isToBeMinted(IRI, String)} for
     * which values these are.
     *
     * @param value the plain name held for the placeholder
     * @return the label to show for it
     */
    public static String getToBeMintedLabel(String value) {
        return LocalUri.PREFIX + value + " " + TO_BE_MINTED_NOTE;
    }

    /**
     * The note appended to a to-be-minted value, set apart from the value itself by
     * {@link #setSelect2ChoiceMinimalEscapeMarkup(Select2Choice)}.
     */
    private static final String TO_BE_MINTED_NOTE = "(mint locally)";

    /**
     * Gets an ExternalLink with a URI label.
     *
     * @param markupId the markup ID for the link
     * @param uri      the URI to link to
     * @return an ExternalLink with the URI label
     */
    public static ExternalLink getUriLink(String markupId, String uri) {
        return new ExternalLink(markupId, (Utils.isLocalURI(uri) ? "" : uri), getUriLabel(uri));
    }

    /**
     * Gets an ExternalLink with a model for the URI label.
     *
     * @param markupId the markup ID for the link
     * @param model    the model containing the URI
     * @return an ExternalLink with the URI label
     */
    public static ExternalLink getUriLink(String markupId, IModel<String> model) {
        return new ExternalLink(markupId, new UriHrefModel(model), new UriLabelModel(model));
    }

    /**
     * The href of a URI link: empty for anything that isn't a URI to link to, so that a local
     * URI or a locally minted name (issue #652) isn't turned into a relative link. This mirrors
     * what {@link #getUriLink(String, String)} does with local URIs.
     */
    private static class UriHrefModel implements IModel<String> {

        private IModel<String> uriModel;

        public UriHrefModel(IModel<String> uriModel) {
            this.uriModel = uriModel;
        }

        @Override
        public String getObject() {
            String uri = uriModel.getObject();
            if (uri == null || isLocalURI(uri) || !isUriValue(uri)) return "";
            return uri;
        }

    }

    private static class UriLabelModel implements IModel<String> {

        private IModel<String> uriModel;

        public UriLabelModel(IModel<String> uriModel) {
            this.uriModel = uriModel;
        }

        @Override
        public String getObject() {
            return getUriLabel(uriModel.getObject());
        }

    }

    /**
     * Creates a sublist from a list based on the specified indices.
     *
     * @param list      the list from which to create the sublist
     * @param fromIndex the starting index (inclusive) for the sublist
     * @param toIndex   the ending index (exclusive) for the sublist
     * @param <E>       the type of elements in the list
     * @return an ArrayList containing the elements from the specified range
     */
    public static <E> ArrayList<E> subList(List<E> list, long fromIndex, long toIndex) {
        // So the resulting list is serializable:
        return new ArrayList<E>(list.subList((int) fromIndex, (int) toIndex));
    }

    /**
     * Creates a sublist from an array based on the specified indices.
     *
     * @param array     the array from which to create the sublist
     * @param fromIndex the starting index (inclusive) for the sublist
     * @param toIndex   the ending index (exclusive) for the sublist
     * @param <E>       the type of elements in the array
     * @return an ArrayList containing the elements from the specified range
     */
    public static <E> ArrayList<E> subList(E[] array, long fromIndex, long toIndex) {
        return subList(Arrays.asList(array), fromIndex, toIndex);
    }

    /**
     * Comparator for sorting ApiResponseEntry objects based on a specified field.
     */
    // TODO Move this to ApiResponseEntry class?
    public static class ApiResponseEntrySorter implements Comparator<ApiResponseEntry>, Serializable {

        private String field;
        private boolean descending;

        /**
         * Constructor for ApiResponseEntrySorter.
         *
         * @param field      the field to sort by
         * @param descending if true, sorts in descending order; if false, sorts in ascending order
         */
        public ApiResponseEntrySorter(String field, boolean descending) {
            this.field = field;
            this.descending = descending;
        }

        /**
         * Compares two ApiResponseEntry objects based on the specified field.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            if (descending) {
                return o2.get(field).compareTo(o1.get(field));
            } else {
                return o1.get(field).compareTo(o2.get(field));
            }
        }

    }

    /**
     * MIME type for TriG RDF format.
     */
    public static final String TYPE_TRIG = "application/trig";

    /**
     * MIME type for Jelly RDF format.
     */
    public static final String TYPE_JELLY = "application/x-jelly-rdf";

    /**
     * MIME type for JSON-LD format.
     */
    public static final String TYPE_JSONLD = "application/ld+json";

    /**
     * MIME type for N-Quads format.
     */
    public static final String TYPE_NQUADS = "application/n-quads";

    /**
     * MIME type for Trix format.
     */
    public static final String TYPE_TRIX = "application/trix";

    /**
     * MIME type for HTML format.
     */
    public static final String TYPE_HTML = "text/html";

    /**
     * Comma-separated list of supported MIME types for nanopublications.
     */
    public static final String SUPPORTED_TYPES = TYPE_TRIG + "," + TYPE_JELLY + "," + TYPE_JSONLD + "," + TYPE_NQUADS + "," + TYPE_TRIX + "," + TYPE_HTML;

    /**
     * List of supported MIME types for nanopublications.
     */
    public static final List<String> SUPPORTED_TYPES_LIST = Arrays.asList(StringUtils.split(SUPPORTED_TYPES, ','));

    private static volatile String resolvedMainRegistryUrl;
    private static volatile String resolvedMainQueryUrl;

    /**
     * Eagerly resolves the main registry and query URLs. Call at application startup
     * so the (potentially slow) first-time discovery does not happen during a user request.
     */
    public static void initMainUrls() {
        getMainRegistryUrl();
        getMainQueryUrl();
    }

    /**
     * Returns the URL of the main Nanopub Registry for this nanodash instance.
     * <p>
     * If {@code NANODASH_MAIN_REGISTRY} is set and matches an entry in the library's
     * discovered registry instance list, that URL is used. Otherwise the first entry
     * of the library list is used. If the library list is empty, the env var value
     * (or built-in default) is used unvalidated. The result is cached for the JVM lifetime.
     *
     * @return Nanopub Registry URL (with trailing slash)
     */
    public static String getMainRegistryUrl() {
        if (resolvedMainRegistryUrl == null) {
            synchronized (Utils.class) {
                if (resolvedMainRegistryUrl == null) {
                    resolvedMainRegistryUrl = resolveMainRegistryUrl();
                }
            }
        }
        return resolvedMainRegistryUrl;
    }

    /**
     * Returns the URL of the main Nanopub Query API for this nanodash instance.
     * <p>
     * If {@code NANODASH_MAIN_QUERY} is set and matches an entry in the library's
     * discovered query instance list, that URL is used. Otherwise the first entry
     * of the library list is used. If the library list is empty, the env var value
     * (or built-in default) is used unvalidated. The result is cached for the JVM lifetime.
     *
     * @return Nanopub Query URL (with trailing slash)
     */
    public static String getMainQueryUrl() {
        if (resolvedMainQueryUrl == null) {
            synchronized (Utils.class) {
                if (resolvedMainQueryUrl == null) {
                    resolvedMainQueryUrl = resolveMainQueryUrl();
                }
            }
        }
        return resolvedMainQueryUrl;
    }

    private static String resolveMainRegistryUrl() {
        String envValue = trimToNull(System.getenv("NANODASH_MAIN_REGISTRY"));
        List<String> instances;
        try {
            instances = NanopubServerUtils.getRegistryServerList();
        } catch (Exception ex) {
            logger.warn("Could not retrieve registry instance list from nanopub library: {}", ex.toString());
            instances = Collections.emptyList();
        }
        return resolveMainUrl("NANODASH_MAIN_REGISTRY", envValue, instances, DEFAULT_MAIN_REGISTRY_URL);
    }

    private static String resolveMainQueryUrl() {
        String envValue = trimToNull(System.getenv("NANODASH_MAIN_QUERY"));
        List<String> instances;
        try {
            instances = QueryCall.getApiInstances();
        } catch (NotEnoughAPIInstancesException ex) {
            logger.warn("Nanopub library reports not enough query API instances available: {}", ex.toString());
            instances = Collections.emptyList();
        } catch (Exception ex) {
            logger.warn("Could not retrieve query instance list from nanopub library: {}", ex.toString());
            instances = Collections.emptyList();
        }
        return resolveMainUrl("NANODASH_MAIN_QUERY", envValue, instances, DEFAULT_MAIN_QUERY_URL);
    }

    private static String resolveMainUrl(String envVarName, String envValue, List<String> instances, String builtInDefault) {
        if (envValue != null) {
            if (containsNormalized(instances, envValue)) {
                logger.info("Using main URL from {} (validated against library instance list): {}", envVarName, envValue);
                return ensureTrailingSlash(envValue);
            }
            if (instances.isEmpty()) {
                logger.warn("Library instance list is empty; using {} unvalidated: {}", envVarName, envValue);
                return ensureTrailingSlash(envValue);
            }
            logger.warn("{}={} is not in the library instance list {}; falling back to first library instance", envVarName, envValue, instances);
            return ensureTrailingSlash(instances.get(0));
        }
        if (!instances.isEmpty()) {
            String first = instances.get(0);
            logger.info("{} not set; using first library instance: {}", envVarName, first);
            return ensureTrailingSlash(first);
        }
        logger.warn("{} not set and library instance list is empty; using built-in default: {}", envVarName, builtInDefault);
        return builtInDefault;
    }

    private static boolean containsNormalized(List<String> urls, String target) {
        String normTarget = normalizeUrl(target);
        for (String url : urls) {
            if (normalizeUrl(url).equals(normTarget)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.trim().replaceFirst("/+$", "").toLowerCase(Locale.ROOT);
    }

    private static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    // The part after the quoted string: a language tag, or a datatype IRI. The quoted
    // string itself is scanned rather than matched (see scanQuotedString): expressed as a
    // regex it costs one stack frame per character, so a literal of a few thousand
    // characters -- e.g. a profile picture given as SVG markup, issue #634 -- overflowed
    // the stack and turned publish-form validation into a 500.
    private static final Pattern LANGTAG_SUFFIX = Pattern.compile("^@([0-9a-zA-Z-]{2,})$");
    private static final Pattern DATATYPE_SUFFIX = Pattern.compile("^\\^\\^<([^ ><\"^]+)>$");

    /**
     * Scans a leading quoted string, in which a backslash may escape a backslash or a
     * quote, and returns the index just past its closing quote.
     *
     * @param s the string to scan
     * @return the index just past the closing quote, or -1 if the string does not start
     * with a well-formed quoted string
     */
    private static int scanQuotedString(String s) {
        if (s.isEmpty() || s.charAt(0) != '"') return -1;
        int i = 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i + 1 >= s.length()) return -1;
                char next = s.charAt(i + 1);
                if (next != '\\' && next != '"') return -1;
                i += 2;
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        return -1;
    }

    /**
     * Checks whether string is valid literal serialization.
     *
     * @param literalString the literal string
     * @return true if valid
     */
    public static boolean isValidLiteralSerialization(String literalString) {
        int end = scanQuotedString(literalString);
        if (end < 0) return false;
        String suffix = literalString.substring(end);
        return suffix.isEmpty()
                || LANGTAG_SUFFIX.matcher(suffix).matches()
                || DATATYPE_SUFFIX.matcher(suffix).matches();
    }

    /**
     * Returns a serialized version of the literal.
     *
     * @param literal the literal
     * @return the String serialization of the literal
     */
    public static String getSerializedLiteral(Literal literal) {
        if (literal.getLanguage().isPresent()) {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"@" + Literals.normalizeLanguageTag(literal.getLanguage().get());
        } else if (literal.getDatatype().equals(XSD.STRING)) {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"";
        } else {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"^^<" + literal.getDatatype() + ">";
        }
    }

    /**
     * Parses a serialized literal into a Literal object.
     *
     * @param serializedLiteral The serialized String of the literal
     * @return The parse Literal object
     */
    public static Literal getParsedLiteral(String serializedLiteral) {
        int end = scanQuotedString(serializedLiteral);
        if (end >= 0) {
            String value = getUnescapedLiteralString(serializedLiteral.substring(1, end - 1));
            String suffix = serializedLiteral.substring(end);
            if (suffix.isEmpty()) return vf.createLiteral(value);
            Matcher langtag = LANGTAG_SUFFIX.matcher(suffix);
            if (langtag.matches()) return vf.createLiteral(value, langtag.group(1));
            Matcher datatype = DATATYPE_SUFFIX.matcher(suffix);
            if (datatype.matches()) return vf.createLiteral(value, vf.createIRI(datatype.group(1)));
        }
        throw new IllegalArgumentException("Not a valid literal serialization: " + serializedLiteral);
    }

    /**
     * Escapes quotes (") and slashes (/) of a literal string.
     *
     * @param unescapedString un-escaped string
     * @return escaped string
     */
    public static String getEscapedLiteralString(String unescapedString) {
        return unescapedString.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Un-escapes quotes (") and slashes (/) of a literal string.
     *
     * @param escapedString escaped string
     * @return un-escaped string
     */
    public static String getUnescapedLiteralString(String escapedString) {
        return escapedString.replaceAll("\\\\(\\\\|\\\")", "$1");
    }

    /**
     * Checks if a given IRI is a local URI.
     *
     * @param uri the IRI to check
     * @return true if the IRI is a local URI, false otherwise
     */
    public static boolean isLocalURI(IRI uri) {
        return uri != null && isLocalURI(uri.stringValue());
    }

    /**
     * Checks if a given string is a local URI.
     *
     * @param uriAsString the string to check
     * @return true if the string is a local URI, false otherwise
     */
    public static boolean isLocalURI(String uriAsString) {
        return !uriAsString.isBlank() && uriAsString.startsWith(LocalUri.PREFIX);
    }

    /**
     * Checks whether a string should be treated as a URI reference rather than as plain text.
     * This is the single discriminator used across the code base, replacing the ad-hoc
     * {@code matches("https?://.+")} tests that only recognized http(s) (issue #655).
     * <p>
     * The set of accepted schemes comes from {@link UriSchemes} in nanopub-java, so that Nanodash
     * and the nanopublication verifier agree on what counts as a URI.
     * <p>
     * {@link UriSchemes#isAllowedUriScheme(String)} on its own only inspects the scheme, so it
     * accepts strings such as {@code "at: home"} that happen to start with an allowed scheme name
     * and a colon. Since many call sites use this method to decide between a literal and an IRI,
     * that would silently turn ordinary prose into a link. The extra conditions below reject such
     * values: a URI contains no whitespace, and must have something after the scheme -- which,
     * for the {@code scheme://} form, means something after the slashes, so that a bare
     * {@code "http://"} is no more a URI than it was under the {@code "https?://.+"} test.
     *
     * @param value the string to check
     * @return true if the string is a URI in one of the allowed schemes
     */
    public static boolean isUriValue(String value) {
        if (value == null || value.isBlank()) return false;
        if (!UriSchemes.isAllowedUriScheme(value)) return false;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) return false;
        }
        String rest = value.substring(value.indexOf(':') + 1);
        if (rest.startsWith("//")) rest = rest.substring(2);
        return !rest.isEmpty();
    }

    /**
     * The external web URL where a URI Nanodash cannot display itself can be looked up, from the
     * scheme-to-template map in {@link NanodashPreferences#getUriResolvers()} (issue #655).
     * http(s) URIs are never resolved this way: they are their own web address.
     *
     * @param uri the URI to resolve
     * @return the resolver URL, or null if the scheme has no configured resolver
     */
    public static String getExternalResolverUrl(String uri) {
        String scheme = UriSchemes.getScheme(uri);
        if (scheme == null || scheme.equals("http") || scheme.equals("https")) return null;
        String template = NanodashPreferences.get().getUriResolvers().get(scheme);
        if (template == null || template.isBlank()) return null;
        String rest = uri.substring(scheme.length() + 1);
        if (rest.startsWith("//")) rest = rest.substring(2);
        return template.replace("$rest", encodeForPath(rest)).replace("$uri", encodeForPath(uri));
    }

    // The resolvers substitute into the path of a URL, where the colons of a DID and the slashes
    // of an AT-URI or IPFS path are legal and load-bearing -- form encoding them (as urlEncode
    // does) would produce a URL the resolver cannot read. So only what is unsafe in a path is
    // escaped, which importantly includes "?" and "#": left as they are, a crafted URI could
    // append a query or fragment to the resolver URL rather than being resolved by it.
    private static final String PATH_SAFE_PUNCTUATION = "-._~!$&'()*+,;=:@/";

    private static String encodeForPath(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int codePoint = s.codePointAt(i);
            int width = Character.charCount(codePoint);
            char c = s.charAt(i);
            boolean alnum = width == 1
                    && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'));
            // An existing percent-escape is passed through, so that an already-encoded URI does
            // not come out double-encoded.
            boolean keptEscape = width == 1 && c == '%' && i + 2 < s.length()
                    && isHexDigit(s.charAt(i + 1)) && isHexDigit(s.charAt(i + 2));
            if (alnum || keptEscape || (width == 1 && PATH_SAFE_PUNCTUATION.indexOf(c) >= 0)) {
                sb.append(c);
            } else {
                for (byte b : new String(Character.toChars(codePoint)).getBytes(UTF_8)) {
                    sb.append(String.format("%%%02X", b));
                }
            }
            i += width;
        }
        return sb.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * The allowed URI schemes as a sorted, comma-separated list, for use in validation messages.
     *
     * @return the allowed schemes, e.g. "at, did, http, https, ipfs, ipns"
     */
    public static String getAllowedUriSchemesLabel() {
        return UriSchemes.ALLOWED_SCHEMES.stream().sorted().collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * Checks whether a string is a well-formed absolute URI.
     * <p>
     * {@link ParsedIRI} is tried first, so that http(s) input is judged exactly as it was before
     * issue #655. It cannot parse AT-URIs: in {@code at://did:plc:abc/app.bsky.feed.post/3k} the
     * authority is {@code did:plc:abc}, and it reads the colon as introducing a port, which then
     * fails to be a number. {@link URI} allows a registry-based authority and accepts these, while
     * still rejecting genuinely malformed input such as {@code at://}, {@code did:} or embedded
     * spaces, so it serves as the fallback rather than as a blanket exemption.
     *
     * @param uri the string to check
     * @return true if the string is a well-formed absolute URI
     */
    public static boolean isWellFormedUri(String uri) {
        if (uri == null || uri.isBlank()) return false;
        try {
            if (new ParsedIRI(uri).isAbsolute()) return true;
        } catch (URISyntaxException ex) {
            // fall through to the more permissive parser below
        }
        try {
            return new URI(uri).isAbsolute();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    public static String unescapeMultiValue(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                } else if (next == '\\') {
                    sb.append('\\');
                } else {
                    sb.append(next);
                }
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

}
