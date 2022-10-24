package org.petapico.nanobench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class OrcidLoginPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/orcidlogin";

	public static String getOrcidLoginUrl(String base, PageParameters params) {
		return getOrcidLoginUrl(Utils.getUrlWithParameters(base, params));		
	}

	// TODO Make sure the entries of this map get removed again at some point:
	public static Map<String,String> redirectHashMap = new HashMap<>();

	public static String getOrcidLoginUrl(String finalRedirectUrl) {
		NanobenchPreferences prefs = NanobenchPreferences.get();
		String finalRedirectUrlHash = Utils.hashString(finalRedirectUrl);
		redirectHashMap.put(finalRedirectUrlHash, finalRedirectUrl);
		// orcid.org gives errors if redirect URL is too long, so we need to store 
		String redirectUrl = prefs.getWebsiteUrl() + "/orcidlogin?redirect-hash=" + finalRedirectUrlHash;
		return "https://orcid.org/oauth/authorize?" +
			"client_id=" + prefs.getOrcidClientId() + "&" +
			"response_type=code&" +
			"scope=/authenticate&" +
			"redirect_uri=" + Utils.urlEncode(redirectUrl);
	}

	public OrcidLoginPage(PageParameters parameters) {
		try {
			NanobenchPreferences prefs = NanobenchPreferences.get();
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
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 300) {
				// Success
				String respString = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
				OrcidLoginResponse r = OrcidLoginResponse.fromJson(respString);
//				rs.cookie("orcid", r.getOrcid());
//				rs.cookie("orcid-access-token", r.getAccessToken());
				System.err.println("User logged in: " + r.getOrcid());
				NanobenchSession.get().setOrcid(r.getOrcid());
			} else {
				// Something went wrong
				System.err.println(statusCode + " " + response.getStatusLine().getReasonPhrase());
				System.err.println(IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8));
			}
		} catch (UnsupportedOperationException | IOException ex) {
			ex.printStackTrace();
		}
		throw new RedirectToUrlException(redirectHashMap.get(parameters.get("redirect-hash").toString()));
	}

}
