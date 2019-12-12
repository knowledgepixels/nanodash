package org.petapico;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.security.IntroNanopub;

public class Utils {

	private Utils() {}  // no instances allowed

	public static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

	public static String getShortNameFromURI(IRI uri) {
		String u = uri.stringValue();
		u = u.replaceFirst("[/#]$", "");
		u = u.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
		u = u.replaceFirst("^.*[/#]([^/#]*[^0-9][^/#]*)$", "$1");
		u = u.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
		u = u.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
		return u;
	}

	private static Map<String,IntroNanopub> introNanopubs = new HashMap<>();

	public static IntroNanopub getIntroNanopub(String userId) {
		IntroNanopub introNanopub = introNanopubs.get(userId);
		if (introNanopub == null) {
			try {
				introNanopub = IntroNanopub.get(userId);
				introNanopubs.put(userId, introNanopub);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return introNanopub;
	}

}
