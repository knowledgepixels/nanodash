package org.petapico;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.commonjava.mimeparse.MIMEParse;

import com.opencsv.CSVReader;

public class Utils {

	private Utils() {}  // no instances allowed

	private static HttpClient httpClient;

	static {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}

	public static String[] apiInstances = new String[] {
		"http://grlc.io/api/peta-pico/nanopub-api/",
		"http://grlc.nanopubs.d2s.labs.vu.nl/api/local/local/"
	};

	public static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

	public static List<String> getUsers() {
		Set<String> users = new HashSet<>();
		for (String apiUrl : apiInstances) {
			CSVReader csvReader = null;
			try {
				HttpGet get = new HttpGet(apiUrl + "get_all_users");
				get.setHeader("Accept", "text/csv");
				try {
					HttpResponse resp = httpClient.execute(get);
					if (!wasSuccessful(resp)) {
						EntityUtils.consumeQuietly(resp.getEntity());
						throw new IOException(resp.getStatusLine().toString());
					}
					csvReader = new CSVReader(new BufferedReader(new InputStreamReader(resp.getEntity().getContent())));
					String[] line = null;
					int n = 0;
					while ((line = csvReader.readNext()) != null) {
						n++;
						if (n == 1) continue; // skip header
						users.add(line[0]);
					}
				} finally {
					if (csvReader != null) csvReader.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return new ArrayList<String>(users);
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

}
