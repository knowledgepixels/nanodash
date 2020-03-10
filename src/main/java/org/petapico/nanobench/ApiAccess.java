package org.petapico.nanobench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.wicket.model.Model;

import com.opencsv.CSVReader;

public abstract class ApiAccess {

	private static RequestConfig requestConfig;

	static {
		requestConfig = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(100).setSocketTimeout(10000).build();
	}

	public static String[] apiInstances = new String[] {
		"http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/",
//		"http://130.60.24.146:7881/api/local/local/",
		"https://openphacts.cs.man.ac.uk/nanopub/grlc/api/local/local/"
//		"https://grlc.nanopubs.knows.idlab.ugent.be/api/local/local/"
	};

	static {
		String env = System.getenv("NANOBENCH_API_INSTANCES");
		if (env != null && !env.isEmpty()) {
			apiInstances = env.trim().split(" ");
			System.err.println("API Instances:");
			for (String s : apiInstances) System.err.println("- " + s);
		}
	}

	protected abstract void processHeader(String[] line);

	protected abstract void processLine(String[] line);

	public void call(String operation, Map<String,String> params) throws IOException {
		String paramString = "";
		if (params != null) {
			paramString = "?";
			for (String k : params.keySet()) {
				if (paramString.length() > 1) paramString += "&";
				paramString += k + "=";
				paramString += urlEncode(params.get(k));
			}
		}

		List<String> apiInstancesToTry = new LinkedList<>(Arrays.asList(apiInstances));
		CSVReader csvReader = null;
		boolean success = false;
		while (success == false && !apiInstancesToTry.isEmpty()) {
			int randomIndex = (int) ((Math.random() * apiInstancesToTry.size()));
			String apiUrl = apiInstancesToTry.get(randomIndex);
			System.err.println("Trying API " + apiUrl);
			apiInstancesToTry.remove(randomIndex);
			HttpGet get = new HttpGet(apiUrl + operation + paramString);
			get.setHeader("Accept", "text/csv");
			try {
				HttpResponse resp = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build().execute(get);
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
				success = true;
			} finally {
				if (csvReader != null) csvReader.close();
			}
		}
	}

	public static List<String> getAll(String operation, Map<String,String> params, final int column) throws IOException {
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

	public static List<Map<String,String>> getAll(String operation, Map<String,String> params) throws IOException {
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

	public static List<Map<String,String>> getRecent(String operation, Map<String,String> params, Model<String> progress) {
		Map<String,Map<String,String>> resultEntries = new HashMap<>();
		Map<String,Map<String,String>> overflowEntries = new HashMap<>();
		int moveLeftCount = 0;
		Calendar day = Calendar.getInstance();
		day.setTimeZone(timeZone);
		int level = 3;
		while (true) {
			Map<String,String> paramsx = new HashMap<>(params);
			if (level == 0) {
				progress.setObject("Searching for results in " + getDayString(day) + "...");
				paramsx.put("day", "http://purl.org/nanopub/admin/date/" + getDayString(day));
			} else if (level == 1) {
				progress.setObject("Searching for results in " + getMonthString(day) + "...");
				paramsx.put("month", "http://purl.org/nanopub/admin/date/" + getMonthString(day));
			} else if (level == 2) {
				progress.setObject("Searching for results in " + getYearString(day) + "...");
				paramsx.put("year", "http://purl.org/nanopub/admin/date/" + getYearString(day));
			} else {
				progress.setObject("Searching for results...");
			}
			List<Map<String,String>> tempResult;
			try {
				tempResult = getAll(operation, paramsx);
			} catch (IOException ex) {
				// TODO distinguish between different types of exceptions
				ex.printStackTrace();
				if (level > 0) {
					level--;
					System.err.println("MOVE DOWN");
					continue;
				}
				break;
			}
			System.err.println("LIST SIZE:" + tempResult.size());
			if (tempResult.size() == 1000 && level > 0) {
				level--;
				System.err.println("MOVE DOWN");
				for (Map<String,String> r : tempResult) {
					overflowEntries.put(r.get("np"), r);
				}
				continue;
			}
			for (Map<String,String> r : tempResult) {
				resultEntries.put(r.get("np"), r);
			}
			System.err.println("RESULT SIZE:" + resultEntries.size());
			if (resultEntries.size() < 10) {
				if (level == 0) {
					if (day.get(Calendar.DAY_OF_MONTH) > 1) {
						if (moveLeftCount > 90) break;
						day.add(Calendar.DATE, -1);
						System.err.println("MOVE LEFT");
						moveLeftCount += 1;
						continue;
					}
				} else if (level == 1) {
					if (day.get(Calendar.MONTH) > 1) {
						if (moveLeftCount > 730) break;
						day.add(Calendar.MONTH, -1);
						day.set(Calendar.DAY_OF_MONTH, day.getActualMaximum(Calendar.DAY_OF_MONTH));
						System.err.println("MOVE LEFT");
						moveLeftCount += 30;
						continue;
					}
				} else if (level == 2) {
					if (day.get(Calendar.YEAR) > 2013) {
						day.set(Calendar.DAY_OF_MONTH, 31);
						day.set(Calendar.MONTH, 11);
						day.add(Calendar.YEAR, -1);
						System.err.println("MOVE LEFT");
						moveLeftCount += 365;
						continue;
					}
				}
			}
			break;
		}
		resultEntries.putAll(overflowEntries);
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
