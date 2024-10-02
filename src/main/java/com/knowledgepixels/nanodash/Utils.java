package com.knowledgepixels.nanodash;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.Charsets;
import org.apache.http.client.utils.URIBuilder;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.setting.IntroNanopub;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.wicketstuff.select2.Select2Choice;

import com.google.common.hash.Hashing;

import net.trustyuri.TrustyUriUtils;

public class Utils {

	private Utils() {}  // no instances allowed

	public static final ValueFactory vf = SimpleValueFactory.getInstance();

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

	public static Nanopub getNanopub(String uriOrArtifactCode) {
		String artifactCode = getArtifactCode(uriOrArtifactCode);
		if (!nanopubs.containsKey(artifactCode)) {
			for (int i = 0; i < 3; i++) {  // Try 3 times to get nanopub
				Nanopub np = GetNanopub.get(artifactCode);
				if (np != null) {
					nanopubs.put(artifactCode, np);
					break;
				}
			}
		}
		return nanopubs.get(artifactCode);
	}

	public static String getArtifactCode(String uriOrArtifactCode) {
		return uriOrArtifactCode.replaceFirst("^.*(RA[0-9a-zA-Z\\-_]{43})(\\?.*)?$", "$1");
	}

	public static String urlEncode(Object o) {
		return URLEncoder.encode((o == null ? "" : o.toString()), Charsets.UTF_8);
	}

	public static String urlDecode(Object o) {
		return URLDecoder.decode((o == null ? "" : o.toString()), Charsets.UTF_8);
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
		NanodashSession session = NanodashSession.get();
		List<String> l = new ArrayList<>();
		if (pubkey.equals(session.getPubkeyString())) l.add("local");
		// TODO: Make this more efficient:
		if (User.getPubkeys(user, true).contains(pubkey)) l.add("approved");
		if (!l.isEmpty()) s += " (" + String.join("/", l) + ")";
		return s;
	}

	public static String getPubkeyLocationName(String pubkey) {
		return getPubkeyLocationName(pubkey, pubkey.replaceFirst("^(.).{39}(.{5}).*$", "$1..$2.."));
	}

	public static String getPubkeyLocationName(String pubkey, String fallback) {
		IRI keyLocation = User.getUserData().getKeyLocation(pubkey);
		if (keyLocation == null) return fallback;
		if (keyLocation.stringValue().equals("http://localhost:37373/")) return "localhost";
		return keyLocation.stringValue().replaceFirst("https?://(nanobench\\.)?(nanodash\\.)?(.*[^/])/?$", "$3");
	}

	public static String getShortPubkeyLocationLabel(String pubkey, IRI user) {
		String s = getPubkeyLocationName(pubkey);
		NanodashSession session = NanodashSession.get();
		List<String> l = new ArrayList<>();
		if (pubkey.equals(session.getPubkeyString())) l.add("local");
		// TODO: Make this more efficient:
		if (User.getPubkeys(user, true).contains(pubkey)) l.add("approved");
		if (!l.isEmpty()) s += " (" + String.join("/", l) + ")";
		return s;
	}

	public static boolean hasNanodashLocation(String pubkey) {
		IRI keyLocation = User.getUserData().getKeyLocation(pubkey);
		if (keyLocation == null) return true; // potentially a Nanodash location
		if (keyLocation.stringValue().contains("nanodash")) return true;
		if (keyLocation.stringValue().contains("nanobench")) return true;
		if (keyLocation.stringValue().contains(":37373")) return true;
		return false;
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
		    .allowUrlProtocols("https", "http", "mailto")
		    .allowElements("a")
		    .allowAttributes("href").onElements("a")
		    .allowElements("img")
		    .allowAttributes("src").onElements("img")
		    .requireRelNofollowOnLinks()
		    .toFactory();

	public static String sanitizeHtml(String rawHtml) {
		return htmlSanitizePolicy.sanitize(rawHtml);
	}

	public static String getPageParametersAsString(PageParameters params) {
		String s = "";
		for (String n : params.getNamedKeys()) {
			if (!s.isEmpty()) s += "&";
			s += n + "=" + URLEncoder.encode(params.get(n).toString(), Charsets.UTF_8);
		}
		return s;
	}

	public static void setSelect2ChoiceMinimalEscapeMarkup(Select2Choice<?> selectItem) {
		selectItem.getSettings().setEscapeMarkup("function(markup) {" +
			"return markup" +
				".replaceAll('<','&lt;').replaceAll('>', '&gt;')" + 
				".replace(/^(.*?) - /, '<span class=\"term\">$1</span><br>')" +
				".replace(/\\((https?:[\\S]+)\\)$/, '<br><code>$1</code>')" +
				".replace(/^([^<].*)$/, '<span class=\"term\">$1</span>')" +
			";}"
		);
	}

	public static boolean isNanopubOfClass(Nanopub np, IRI classIri) {
		for (Statement st : np.getAssertion()) {
			if (np.getAssertionUri().equals(st.getSubject()) && RDF.TYPE.equals(st.getPredicate()) && classIri.equals(st.getObject())) {
				return true;
			}
		}
		return false;
	}

	public static boolean usesPredicateInAssertion(Nanopub np, IRI predicateIri) {
		for (Statement st : np.getAssertion()) {
			if (predicateIri.equals(st.getPredicate())) {
				return true;
			}
		}
		return false;
	}

	public static Map<String,String> getFoafNameMap(Nanopub np) {
		Map<String,String> foafNameMap = new HashMap<>();
		for (Statement st : np.getPubinfo()) {
			if (st.getPredicate().equals(FOAF.NAME) && st.getObject() instanceof Literal objL) {
				foafNameMap.put(st.getSubject().stringValue(), objL.stringValue());
			}
		}
		return foafNameMap;
	}

	public static String createSha256HexHash(Object obj) {
		return Hashing.sha256().hashString(obj.toString(), StandardCharsets.UTF_8).toString();
	}

	public static List<IRI> getTypes(Nanopub np) {
		List<IRI> l = new ArrayList<IRI>();
		for (IRI t : NanopubUtils.getTypes(np)) {
			if (t.stringValue().equals("https://w3id.org/fair/fip/terms/Available-FAIR-Enabling-Resource")) continue;
			if (t.stringValue().equals("https://w3id.org/fair/fip/terms/FAIR-Enabling-Resource-to-be-Developed")) continue;
			if (t.stringValue().equals("https://w3id.org/fair/fip/terms/Available-FAIR-Supporting-Resource")) continue;
			if (t.stringValue().equals("https://w3id.org/fair/fip/terms/FAIR-Supporting-Resource-to-be-Developed")) continue;
			l.add(t);
		}
		return l;
	}

	public static String getTypeLabel(IRI typeIri) {
		String l = typeIri.stringValue();
		if (l.equals("https://w3id.org/fair/fip/terms/FAIR-Enabling-Resource")) return "FER";
		if (l.equals("https://w3id.org/fair/fip/terms/FAIR-Supporting-Resource")) return "FSR";
		if (l.equals("https://w3id.org/fair/fip/terms/FAIR-Implementation-Profile")) return "FIP";
		if (l.equals("http://purl.org/nanopub/x/declaredBy")) return "user intro";
		if (l.equals("https://w3id.org/kpxl/grlc/grlc-query")) return "API";
		l = l.replaceFirst("^.*[/#]([^/#]+)[/#]?$", "$1");
		l = l.replaceFirst("Nanopub$", "");
		if (l.length() > 25) l = l.substring(0, 20) + "...";
		return l;
	}

	public static String getUriLabel(String uri) {
		String uriLabel = uri;
		if (uriLabel.matches(".*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}([^A-Za-z0-9-_].*)?")) {
			String newUriLabel = uriLabel.replaceFirst("(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{8})[A-Za-z0-9-_]{35}([^A-Za-z0-9-_].*)?", "$1...$2");
			if (newUriLabel.length() <= 70) return newUriLabel;
		}
		if (uriLabel.length() > 70) return uri.substring(0,30) + "..." + uri.substring(uri.length()-30);
		return uriLabel;
	}

	public static ExternalLink getUriLink(String markupId, String uri) {
		return new ExternalLink(markupId, (uri.startsWith("local:") ? "" : uri), getUriLabel(uri));
	}

	public static ExternalLink getUriLink(String markupId, IModel<String> model) {
		return new ExternalLink(markupId, model, new UriLabelModel(model));
	}

	private static class UriLabelModel implements IModel<String> {

		private static final long serialVersionUID = 1L;

		private IModel<String> uriModel;

		public UriLabelModel(IModel<String> uriModel) {
			this.uriModel = uriModel;
		}

		@Override
		public String getObject() {
			return getUriLabel(uriModel.getObject());
		}
		
	}

	public static <E> ArrayList<E> subList(List<E> list, long fromIndex, long toIndex) {
		// So the resulting list is serializable:
		return new ArrayList<E>(list.subList((int) fromIndex, (int) toIndex));
	}

	public static <E> ArrayList<E> subList(E[] array, long fromIndex, long toIndex) {
		return subList(Arrays.asList(array), fromIndex, toIndex);
	}
}
