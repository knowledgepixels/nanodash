package org.petapico;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.wicket.model.Model;

import com.opencsv.CSVReader;

public abstract class ApiAccess {

	private static HttpClient httpClient;

	static {
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
	}

	public static String[] apiInstances = new String[] {
		"http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/",
		"http://130.60.24.146:7881/api/local/local/"
	};

	protected abstract void processHeader(String[] line);

	protected abstract void processLine(String[] line);

	public void call(String operation, Map<String,String> params) {
		String paramString = "";
		if (params != null) {
			paramString = "?";
			for (String k : params.keySet()) {
				if (paramString.length() > 1) paramString += "&";
				paramString += k + "=";
				paramString += urlEncode(params.get(k));
			}
		}

		String apiUrl = apiInstances[0];  // TODO: check several APIs
		CSVReader csvReader = null;
		try {
			HttpGet get = new HttpGet(apiUrl + operation + paramString);
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
					if (n == 1) {
						processHeader(line);
					} else {
						processLine(line);
					}
				}
			} finally {
				if (csvReader != null) csvReader.close();
			}
		} catch (IOException ex) {
			// TODO: proper logging
			ex.printStackTrace();
		}
	}

	public static List<String> getAll(String operation, Map<String,String> params, final int column) {
		final List<String> result = new ArrayList<>();
		ApiAccess a = new ApiAccess() {
			
			@Override
			protected void processLine(String[] line) {
				result.add(line[column]);
			}
			
			@Override
			protected void processHeader(String[] line) {
				// ignore
			}

		};
		a.call(operation, params);
		return result;
	}

	public static List<Map<String,String>> getAll(String operation, Map<String,String> params) {
		final List<Map<String,String>> result = new ArrayList<>();
		ApiAccess a = new ApiAccess() {

			String[] header;
			
			@Override
			protected void processHeader(String[] line) {
				header = line;
			}

			@Override
			protected void processLine(String[] line) {
				Map<String,String> entry = new HashMap<String,String>();
				for (int i = 0 ; i < line.length ; i++) {
					entry.put(header[i], line[i]);
				}
				result.add(entry);
			}

		};
		a.call(operation, params);
		return result;
	}

	public static List<Map<String,String>> getRecent(String operation, Map<String,String> params, Model<String> progressModel) {
		Map<String,Map<String,String>> resultEntries = new HashMap<>();
		Calendar day = Calendar.getInstance();
		day.setTimeZone(timeZone);
		int level = 3;
		while (true) {
			Map<String,String> paramsx = new HashMap<>(params);
			if (level == 0) {
				progressModel.setObject("Searching for results in " + getDayString(day) + "...");
				paramsx.put("day", "http://purl.org/nanopub/admin/date/" + getDayString(day));
			} else if (level == 1) {
				progressModel.setObject("Searching for results in " + getMonthString(day) + "...");
				paramsx.put("month", "http://purl.org/nanopub/admin/date/" + getMonthString(day));
			} else if (level == 2) {
				progressModel.setObject("Searching for results in " + getYearString(day) + "...");
				paramsx.put("year", "http://purl.org/nanopub/admin/date/" + getYearString(day));
			} else {
				progressModel.setObject("Searching for results...");
			}
			List<Map<String,String>> tempResult = getAll(operation, paramsx);
			if (tempResult.size() == 1000 && level > 0) {
				level--;
				System.err.println("MOVE DOWN");
				continue;
			}
			for (Map<String,String> r : tempResult) {
				resultEntries.put(r.get("np"), r);
			}
			System.err.println("RESULT SIZE:" + resultEntries.size());
			if (resultEntries.size() < 10) {
				if (level == 0) {
					if (day.get(Calendar.DAY_OF_MONTH) > 1) {
						day.add(Calendar.DATE, -1);
						System.err.println("MOVE LEFT");
						continue;
					}
				} else if (level == 1) {
					if (day.get(Calendar.MONTH) > 1) {
						day.add(Calendar.MONTH, -1);
						day.set(Calendar.DAY_OF_MONTH, day.getActualMaximum(Calendar.DAY_OF_MONTH));
						System.err.println("MOVE LEFT");
						continue;
					}
				} else if (level == 2) {
					if (day.get(Calendar.YEAR) > 2013) {
						day.set(Calendar.DAY_OF_MONTH, 31);
						day.set(Calendar.MONTH, 11);
						day.add(Calendar.YEAR, -1);
						System.err.println("MOVE LEFT");
						continue;
					}
				}
			}
			break;
		}
		List<Map<String,String>> result = new ArrayList<>(resultEntries.values());
		Collections.sort(result, nanopubResultComparator);
		return result;
	}

	private static boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static TimeZone timeZone = TimeZone.getTimeZone("UTC");

	private static String getDayString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static String getMonthString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static String getYearString(Calendar c) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy");
		df.setTimeZone(timeZone);
		return df.format(c.getTime());
	}

	private static Comparator<Map<String,String>> nanopubResultComparator = new Comparator<Map<String,String>>() {
		@Override
		public int compare(Map<String,String> e1, Map<String,String> e2) {
			return e2.get("date").compareTo(e1.get("date"));
		}
	};

}
