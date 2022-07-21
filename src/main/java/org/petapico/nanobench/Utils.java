package org.petapico.nanobench;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;

public class Utils {

	private Utils() {}  // no instances allowed

	static ValueFactory vf = SimpleValueFactory.getInstance();

	public static final IRI DECLARED_BY = vf.createIRI("http://purl.org/nanopub/x/declaredBy");
	public static final IRI HAS_PUBLIC_KEY = vf.createIRI("http://purl.org/nanopub/x/hasPublicKey");

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

	private static Map<String,Nanopub> nanopubs = new HashMap<>();

	public static Nanopub getNanopub(String uri) {
		if (!nanopubs.containsKey(uri)) {
			for (int i = 0; i < 3; i++) {  // Try 3 times to get nanopub
				Nanopub np = GetNanopub.get(uri);
				if (np != null) {
					nanopubs.put(uri, np);
					break;
				}
			}
		}
		return nanopubs.get(uri);
	}

	public static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		return "";
	}

	public static String getShortPubkeyLabel(String pubkey) {
		return pubkey.replaceFirst("^(.).{39}(.{5}).*$", "$1..$2..");
	}

}
