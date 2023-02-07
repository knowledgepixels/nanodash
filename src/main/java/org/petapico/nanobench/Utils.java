package org.petapico.nanobench;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.client.utils.URIBuilder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.IntroNanopub;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import net.trustyuri.TrustyUriUtils;

public class Utils {

	private Utils() {}  // no instances allowed

	static ValueFactory vf = SimpleValueFactory.getInstance();

	public static final IRI DECLARED_BY = vf.createIRI("http://purl.org/nanopub/x/declaredBy");
	public static final IRI HAS_PUBLIC_KEY = vf.createIRI("http://purl.org/nanopub/x/hasPublicKey");

	public static String getShortNameFromURI(IRI uri) {
		String u = uri.stringValue();
		u = u.replaceFirst("[/#]$", "");
		u = u.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
		u = u.replaceFirst("^.*[/#]([^/#]*[^0-9][^/#]*)$", "$1");
		u = u.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
		u = u.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
		return u;
	}

	public static String getShortNanopubId(Object npId) {
		return TrustyUriUtils.getArtifactCode(npId.toString()).substring(0, 10);
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

	public static String urlEncode(Object o) {
		return URLEncoder.encode((o == null ? "" : o.toString()), Charsets.UTF_8);
	}

	public static String getUrlWithParameters(String base, PageParameters parameters) {
		try {
			URIBuilder u = new URIBuilder(base);
			for (String key : parameters.getNamedKeys()) {
				for (StringValue value : parameters.getValues(key)) {
					if (!value.isNull()) u.addParameter(key, value.toString());
				}
			}
			return u.build().toString();
		} catch (URISyntaxException ex) {
			ex.printStackTrace();
			return "/";
		}
	}

	public static String getShortPubkeyName(String pubkey) {
		return pubkey.replaceFirst("^(.).{39}(.{5}).*$", "$1..$2..");
	}

	public static String getShortPubkeyLabel(String pubkey, IRI user) {
		String s = getShortPubkeyName(pubkey);
		NanobenchSession session = NanobenchSession.get();
		List<String> l = new ArrayList<>();
		if (pubkey.equals(session.getPubkeyString())) l.add("local");
		// TODO: Make this more efficient:
		if (User.getPubkeys(user, true).contains(pubkey)) l.add("approved");
		if (!l.isEmpty()) s += " (" + String.join("/", l) + ")";
		return s;
	}

	public static String getShortOrcidId(IRI orcidIri) {
		return orcidIri.stringValue().replaceFirst("^https://orcid.org/", "");
	}

	public static String getUriPostfix(Object uri) {
		String s = uri.toString();
		if (s.contains("#")) return s.replaceFirst("^.*#(.*)$", "$1");
		return s.replaceFirst("^.*/(.*)$", "$1");
	}

	public static String getUriPrefix(Object uri) {
		String s = uri.toString();
		if (s.contains("#")) return s.replaceFirst("^(.*#).*$", "$1");
		return s.replaceFirst("^(.*/).*$", "$1");
	}

	public static boolean isUriPostfix(String s) {
		return !s.contains(":");
	}

	public static IRI getLocation(IntroNanopub inp) {
		NanopubSignatureElement el = getNanopubSignatureElement(inp);
		for (KeyDeclaration kd : inp.getKeyDeclarations()) {
			if (el.getPublicKeyString().equals(kd.getPublicKeyString())) {
				return kd.getKeyLocation();
			}
		}
		return null;
	}

	public static NanopubSignatureElement getNanopubSignatureElement(IntroNanopub inp) {
		try {
			return SignatureUtils.getSignatureElement(inp.getNanopub());
		} catch (MalformedCryptoElementException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String hashString(String s) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Nanopub getAsNanopub(String uri) {
		if (TrustyUriUtils.isPotentialTrustyUri(uri)) {
			try {
				return Utils.getNanopub(uri);
			} catch (Exception ex) {
				// wasn't a known nanopublication
			}	
		}
		return null;
	}

	private static PolicyFactory htmlSanitizePolicy = new HtmlPolicyBuilder()
			.allowCommonBlockElements()
			.allowCommonInlineFormattingElements()
		    .allowElements("a")
		    .allowUrlProtocols("https", "http", "mailto")
		    .allowAttributes("href").onElements("a")
		    .requireRelNofollowOnLinks()
		    .toFactory();

	public static String sanitizeHtml(String rawHtml) {
		return htmlSanitizePolicy.sanitize(rawHtml);
	}

}
