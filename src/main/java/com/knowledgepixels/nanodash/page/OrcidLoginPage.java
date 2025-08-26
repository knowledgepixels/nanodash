package com.knowledgepixels.nanodash.page;

import com.google.gson.Gson;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This page handles the login process with ORCID.
 */
public class OrcidLoginPage extends WebPage {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(OrcidLoginPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/orcidlogin";

    /**
     * Returns the ORCID login URL with the given base URL and parameters.
     *
     * @param base   the base URL to which the ORCID login parameters will be appended
     * @param params the parameters to be included in the ORCID login URL
     * @return the complete ORCID login URL
     */
    public static String getOrcidLoginUrl(String base, PageParameters params) {
        return getOrcidLoginUrl(Utils.getUrlWithParameters(base, params));
    }

    /**
     * A map to store redirect URLs temporarily.
     */
    // TODO Make sure the entries of this map get removed again at some point:
    public static Map<String, String> redirectHashMap = new HashMap<>();

    /**
     * Generates the ORCID login URL with a final redirect URL.
     * The final redirect URL is hashed and stored in a map to avoid long URLs.
     *
     * @param finalRedirectUrl the URL to redirect to after successful ORCID login
     * @return the complete ORCID login URL
     */
    public static String getOrcidLoginUrl(String finalRedirectUrl) {
        NanodashPreferences prefs = NanodashPreferences.get();
        String finalRedirectUrlHash = Utils.createSha256HexHash(finalRedirectUrl);
        redirectHashMap.put(finalRedirectUrlHash, finalRedirectUrl);
        // orcid.org gives errors if redirect URL is too long, so we need to store
        String redirectUrl = prefs.getWebsiteUrl() + "/orcidlogin?redirect-hash=" + finalRedirectUrlHash;
        return "https://orcid.org/oauth/authorize?" +
               "client_id=" + prefs.getOrcidClientId() + "&" +
               "response_type=code&" +
               "scope=/authenticate&" +
               "redirect_uri=" + Utils.urlEncode(redirectUrl);
    }

    /**
     * Constructor that handles the ORCID login process.
     * It exchanges the authorization code for an access token and redirects to the final URL.
     *
     * @param parameters The page parameters, which should include the authorization code and redirect hash.
     */
    public OrcidLoginPage(PageParameters parameters) {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try (client) {
            NanodashPreferences prefs = NanodashPreferences.get();
            String authCode = parameters.get("code").toString();
            HttpPost post = new HttpPost("https://orcid.org/oauth/token");
            post.setHeader("Accept", "application/json");
            List<NameValuePair> urlParams = new ArrayList<NameValuePair>();
            urlParams.add(new BasicNameValuePair("client_id", prefs.getOrcidClientId()));
            urlParams.add(new BasicNameValuePair("client_secret", prefs.getOrcidClientSecret()));
            urlParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
            // We need to report here the exact same redirect URI:
            urlParams.add(new BasicNameValuePair("redirect_uri", prefs.getWebsiteUrl() + "/orcidlogin?redirect-hash=" + Utils.urlEncode(parameters.get("redirect-hash").toString(""))));
            urlParams.add(new BasicNameValuePair("code", authCode));
            post.setEntity(new UrlEncodedFormEntity(urlParams));
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                // Success
                String respString = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
                OrcidLoginResponse r = OrcidLoginResponse.fromJson(respString);
//				rs.cookie("orcid", r.getOrcid());
//				rs.cookie("orcid-access-token", r.getAccessToken());
                logger.info("User logged in: {}", r.getOrcid());
                NanodashSession.get().setOrcid(r.getOrcid());
            } else {
                // Something went wrong
                logger.error("{} {}", statusCode, response.getStatusLine().getReasonPhrase());
                logger.error(IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8));
            }
        } catch (UnsupportedOperationException | IOException ex) {
            ex.printStackTrace();
        }
        throw new RedirectToUrlException(redirectHashMap.get(parameters.get("redirect-hash").toString()));
    }


    static class OrcidLoginResponse {

        private static Gson g = new Gson();

        public static OrcidLoginResponse fromJson(String jsonString) {
            return g.fromJson(jsonString, OrcidLoginResponse.class);
        }

        private String access_token;
        private String token_type;
        private String refresh_token;
        private long expires_in;
        private String scope;
        private String name;
        private String orcid;

        /**
         * Returns the access token.
         *
         * @return the access token string
         */
        public String getAccessToken() {
            return access_token;
        }

        /**
         * Returns the type of the token.
         *
         * @return the token type string
         */
        public String getTokenType() {
            return token_type;
        }

        /**
         * Returns the refresh token.
         *
         * @return the refresh token string
         */
        public String getRefreshToken() {
            return refresh_token;
        }

        /**
         * Returns the expiration time of the token in seconds.
         *
         * @return the expiration time in seconds
         */
        public long getExpiresIn() {
            return expires_in;
        }

        /**
         * Returns the scope of the access token.
         *
         * @return the scope string
         */
        public String getScope() {
            return scope;
        }

        /**
         * Returns the name associated with the ORCID account.
         *
         * @return the name string
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the ORCID identifier.
         *
         * @return the ORCID string
         */
        public String getOrcid() {
            return orcid;
        }

    }

}
