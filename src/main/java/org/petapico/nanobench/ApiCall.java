package org.petapico.nanobench;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class ApiCall {

	public static HttpResponse run(String operation, Map<String,String> params) {
		ApiCall apiCall = new ApiCall(operation, params);
		apiCall.run();
		while (!apiCall.calls.isEmpty() && apiCall.resp == null) {
			try {
			    Thread.sleep(50);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
		return apiCall.resp;
	}

	private static RequestConfig requestConfig;

	static {
		requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
	}

	public static String[] apiInstances = new String[] {
		"http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/",
		"http://130.60.24.146:7881/api/local/local/",
		"https://openphacts.cs.man.ac.uk/nanopub/grlc/api/local/local/",
		"https://grlc.nanopubs.knows.idlab.ugent.be/api/local/local/",
		"http://grlc.np.scify.org/api/local/local/",
		"http://grlc.np.dumontierlab.com/api/local/local/"
	};

	private static String experimentalApi1 = "http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/";
	private static String experimentalApi2 = "http://130.60.24.146:7881/api/local/local/";

	private boolean isExperimentalOperation(String op) {
		return false;
	}

	private String operation;
	private String paramString;
	private int parallelCallCount = 2;
	private List<String> apisToCall = new ArrayList<>();
	private List<Call> calls = new ArrayList<>();

	private HttpResponse resp;

	private ApiCall(String operation, Map<String,String> params) {
		this.operation = operation;
		paramString = "";
		if (params != null) {
			paramString = "?";
			for (String k : params.keySet()) {
				if (paramString.length() > 1) paramString += "&";
				try {
					paramString += k + "=";
					paramString += URLEncoder.encode(params.get(k), Charsets.UTF_8.toString());
				} catch (java.io.UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void run() {
		List<String> apiInstancesToTry = new LinkedList<>(Arrays.asList(apiInstances));
		if (!isExperimentalOperation(operation)) {
			while (!apiInstancesToTry.isEmpty() && apisToCall.size() < parallelCallCount) {
				int randomIndex = (int) ((Math.random() * apiInstancesToTry.size()));
				String apiUrl = apiInstancesToTry.get(randomIndex);
				apisToCall.add(apiUrl);
				System.err.println("Trying API (" + apisToCall.size() + ") " + apiUrl);
				apiInstancesToTry.remove(randomIndex);
			}
		} else {
			apisToCall.add(experimentalApi1);
			apisToCall.add(experimentalApi2);
		}
		for (String api : apisToCall) {
			Call call = new Call(api);
			calls.add(call);
			new Thread(call).run();
		}
	}

	private void finished(HttpResponse resp, String apiUrl) {
		if (this.resp != null) return; // result already in
		System.err.println("Result in from " + apiUrl);
		this.resp = resp;
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}


	private class Call implements Runnable {

		private String apiUrl;

		public Call(String apiUrl) {
			this.apiUrl = apiUrl;
		}

		public void run() {
			HttpGet get = new HttpGet(apiUrl + operation + paramString);
			get.setHeader("Accept", "text/csv");
			try {
				HttpResponse resp = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build().execute(get);
				if (!wasSuccessful(resp)) {
					EntityUtils.consumeQuietly(resp.getEntity());
					throw new IOException(resp.getStatusLine().toString());
				}
				finished(resp, apiUrl);
			} catch (Exception ex) {
//				ex.printStackTrace();
				System.err.println("Request to " + apiUrl + " was not successful");
			}
			calls.remove(this);
		}

	}

}
