package org.petapico.nanobench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.wicket.model.Model;

import com.opencsv.CSVReader;

public abstract class ApiAccess {

	protected abstract void processHeader(String[] line);

	protected abstract void processLine(String[] line);

	public void call(String operation, Map<String,String> params) throws IOException {
		CSVReader csvReader = null;
		try {
			HttpResponse resp = ApiCall.run(operation, params);
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
	}

	public static ApiResponse getAll(String operation, Map<String,String> params) throws IOException {
		final ApiResponse response = new ApiResponse();
		ApiAccess a = new ApiAccess() {

			@Override
			protected void processHeader(String[] line) {
				response.setHeader(line);
			}

			@Override
			protected void processLine(String[] line) {
				response.add(line);
			}

		};
		a.call(operation, params);
		return response;
	}

	public static ApiResponse getRecent(String operation, Map<String,String> params, Model<String> progress) {
		Map<String,ApiResponseEntry> resultEntries = new HashMap<>();
		Map<String,ApiResponseEntry> overflowEntries = new HashMap<>();
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
			ApiResponse tempResult;
			try {
				tempResult = getAll(operation, paramsx);
			} catch (Exception ex) {
				// TODO distinguish between different types of exceptions
				//ex.printStackTrace();
				System.err.println("Request not successful");
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
				for (ApiResponseEntry r : tempResult.getData()) {
					overflowEntries.put(r.get("np"), r);
				}
				continue;
			}
			for (ApiResponseEntry r : tempResult.getData()) {
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
		ApiResponse response = new ApiResponse(resultEntries.values());
		Collections.sort(response.getData(), nanopubResultComparator);
		return response;
	}

	private static Map<String,String> latestVersionMap = new HashMap<>();

	public static String getLatestVersionId(String nanopubId) {
		if (!latestVersionMap.containsKey(nanopubId)) {
			Map<String,String> params = new HashMap<>();
			params.put("np", nanopubId);
			try {
				ApiResponse r = ApiAccess.getAll("get_latest_version", params);
				if (r.getData().size() != 1) return nanopubId;
				String l = r.getData().get(0).get("latest");
				latestVersionMap.put(nanopubId, l);
			} catch (Exception ex) {
				ex.printStackTrace();
				return nanopubId;
			}
		}
		return latestVersionMap.get(nanopubId);
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

	private static Comparator<ApiResponseEntry> nanopubResultComparator = new Comparator<ApiResponseEntry>() {
		@Override
		public int compare(ApiResponseEntry e1, ApiResponseEntry e2) {
			return e2.get("date").compareTo(e1.get("date"));
		}
	};

}
