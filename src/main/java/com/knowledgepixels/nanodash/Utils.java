package com.knowledgepixels.nanodash;

import com.google.common.hash.Hashing;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.codec.Charsets;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.lang.StringUtils;
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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.setting.IntroNanopub;
import org.nanopub.vocabulary.FIP;
import org.nanopub.vocabulary.NPX;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Select2Choice;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class providing various helper methods for handling nanopublications, URIs, and other related functionalities.
 */
public class Utils {

    private Utils() {
    }  // no instances allowed

    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    // TODO Merge with IriItem.getShortNameFromURI
    public static String getShortNameFromURI(IRI uri) {
        return getShortNameFromURI(uri.stringValue());
    }

    public static String getShortNameFromURI(String u) {
        u = u.replaceFirst("\\?.*$", "");
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

    private static Map<String, Nanopub> nanopubs = new HashMap<>();

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
            logger.error("Could not build URL with parameters: {} {}", base, parameters, ex);
            return "/";
        }
    }

    /**
     * Generates a short name for a public key or public key hash.
     *
     * @param pubkeyOrPubkeyhash the public key (64 characters) or public key hash (40 characters)
     * @return a short representation of the public key or public key hash
     */
    public static String getShortPubkeyName(String pubkeyOrPubkeyhash) {
        if (pubkeyOrPubkeyhash.length() == 64) {
            return pubkeyOrPubkeyhash.replaceFirst("^(.{8}).*$", "$1");
        } else {
            return pubkeyOrPubkeyhash.replaceFirst("^(.).{39}(.{5}).*$", "$1..$2..");
        }
    }

    public static String getShortPubkeyhashLabel(String pubkeyOrPubkeyhash, IRI user) {
        String s = getShortPubkeyName(pubkeyOrPubkeyhash);
        NanodashSession session = NanodashSession.get();
        List<String> l = new ArrayList<>();
        if (pubkeyOrPubkeyhash.equals(session.getPubkeyString()) || pubkeyOrPubkeyhash.equals(session.getPubkeyhash()))
            l.add("local");
        // TODO: Make this more efficient:
        String hashed = Utils.createSha256HexHash(pubkeyOrPubkeyhash);
        if (User.getPubkeyhashes(user, true).contains(pubkeyOrPubkeyhash) || User.getPubkeyhashes(user, true).contains(hashed))
            l.add("approved");
        if (!l.isEmpty()) s += " (" + String.join("/", l) + ")";
        return s;
    }

    /**
     * Retrieves the name of the public key location based on the public key.
     *
     * @param pubkeyhash the public key string
     * @return the name of the public key location
     */
    public static String getPubkeyLocationName(String pubkeyhash) {
        return getPubkeyLocationName(pubkeyhash, getShortPubkeyName(pubkeyhash));
    }

    /**
     * Retrieves the name of the public key location, or returns a fallback name if not found.
     * If the key location is localhost, it returns "localhost".
     *
     * @param pubkeyhash the public key string
     * @param fallback   the fallback name to return if the key location is not found
     * @return the name of the public key location or the fallback name
     */
    public static String getPubkeyLocationName(String pubkeyhash, String fallback) {
        IRI keyLocation = User.getUserData().getKeyLocationForPubkeyhash(pubkeyhash);
        if (keyLocation == null) return fallback;
        if (keyLocation.stringValue().equals("http://localhost:37373/")) return "localhost";
        return keyLocation.stringValue().replaceFirst("https?://(nanobench\\.)?(nanodash\\.)?(.*[^/])/?$", "$3");
    }

    /**
     * Generates a short label for a public key location, including its status (local or approved).
     *
     * @param pubkeyhash the public key string
     * @param user       the IRI of the user associated with the public key
     * @return a short label indicating the public key location and its status
     */
    public static String getShortPubkeyLocationLabel(String pubkeyhash, IRI user) {
        String s = getPubkeyLocationName(pubkeyhash);
        NanodashSession session = NanodashSession.get();
        List<String> l = new ArrayList<>();
        if (pubkeyhash.equals(session.getPubkeyhash())) l.add("local");
        // TODO: Make this more efficient:
        if (User.getPubkeyhashes(user, true).contains(pubkeyhash)) l.add("approved");
        if (!l.isEmpty()) s += " (" + String.join("/", l) + ")";
        return s;
    }

    /**
     * Checks if a given public key has a Nanodash location.
     * A Nanodash location is identified by specific keywords in the key location.
     *
     * @param pubkeyhash the public key to check
     * @return true if the public key has a Nanodash location, false otherwise
     */
    public static boolean hasNanodashLocation(String pubkeyhash) {
        IRI keyLocation = User.getUserData().getKeyLocationForPubkeyhash(pubkeyhash);
        if (keyLocation == null) return true; // potentially a Nanodash location
        if (keyLocation.stringValue().contains("nanodash")) return true;
        if (keyLocation.stringValue().contains("nanobench")) return true;
        if (keyLocation.stringValue().contains(":37373")) return true;
        return false;
    }

    /**
     * Retrieves the short ORCID ID from an IRI object.
     *
     * @param orcidIri the IRI object representing the ORCID ID
     * @return the short ORCID ID as a string
     */
    public static String getShortOrcidId(IRI orcidIri) {
        return orcidIri.stringValue().replaceFirst("^https://orcid.org/", "");
    }

    /**
     * Retrieves the URI postfix from a given URI object.
     *
     * @param uri the URI object from which to extract the postfix
     * @return the URI postfix as a string
     */
    public static String getUriPostfix(Object uri) {
        String s = uri.toString();
        if (s.contains("#")) return s.replaceFirst("^.*#(.*)$", "$1");
        return s.replaceFirst("^.*/(.*)$", "$1");
    }

    /**
     * Retrieves the URI prefix from a given URI object.
     *
     * @param uri the URI object from which to extract the prefix
     * @return the URI prefix as a string
     */
    public static String getUriPrefix(Object uri) {
        String s = uri.toString();
        if (s.contains("#")) return s.replaceFirst("^(.*#).*$", "$1");
        return s.replaceFirst("^(.*/).*$", "$1");
    }

    /**
     * Checks if a given string is a valid URI postfix.
     * A valid URI postfix does not contain a colon (":").
     *
     * @param s the string to check
     * @return true if the string is a valid URI postfix, false otherwise
     */
    public static boolean isUriPostfix(String s) {
        return !s.contains(":");
    }

    /**
     * Retrieves the location of a given IntroNanopub.
     *
     * @param inp the IntroNanopub from which to extract the location
     * @return the IRI location of the nanopublication, or null if not found
     */
    public static IRI getLocation(IntroNanopub inp) {
        NanopubSignatureElement el = getNanopubSignatureElement(inp);
        for (KeyDeclaration kd : inp.getKeyDeclarations()) {
            if (el.getPublicKeyString().equals(kd.getPublicKeyString())) {
                return kd.getKeyLocation();
            }
        }
        return null;
    }

    /**
     * Retrieves the NanopubSignatureElement from a given IntroNanopub.
     *
     * @param inp the IntroNanopub from which to extract the signature element
     * @return the NanopubSignatureElement associated with the nanopublication
     */
    public static NanopubSignatureElement getNanopubSignatureElement(IntroNanopub inp) {
        try {
            return SignatureUtils.getSignatureElement(inp.getNanopub());
        } catch (MalformedCryptoElementException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Retrieves a Nanopub object from a given URI if it is a potential Trusty URI.
     *
     * @param uri the URI to check and retrieve the Nanopub from
     * @return the Nanopub object if found, or null if not a known nanopublication
     */
    public static Nanopub getAsNanopub(String uri) {
        if (TrustyUriUtils.isPotentialTrustyUri(uri)) {
            try {
                return Utils.getNanopub(uri);
            } catch (Exception ex) {
                logger.error("The given URI is not a known nanopublication: {}", uri, ex);
            }
        }
        return null;
    }

    private static final PolicyFactory htmlSanitizePolicy = new HtmlPolicyBuilder()
            .allowCommonBlockElements()
            .allowCommonInlineFormattingElements()
            .allowUrlProtocols("https", "http", "mailto")
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .allowElements("img")
            .allowAttributes("src").onElements("img")
            .requireRelNofollowOnLinks()
            .toFactory();

    /**
     * Sanitizes raw HTML input to ensure safe rendering.
     *
     * @param rawHtml the raw HTML input to sanitize
     * @return sanitized HTML string
     */
    public static String sanitizeHtml(String rawHtml) {
        return htmlSanitizePolicy.sanitize(rawHtml);
    }

    /**
     * Converts PageParameters to a URL-encoded string representation.
     *
     * @param params the PageParameters to convert
     * @return a string representation of the parameters in URL-encoded format
     */
    public static String getPageParametersAsString(PageParameters params) {
        String s = "";
        for (String n : params.getNamedKeys()) {
            if (!s.isEmpty()) s += "&";
            s += n + "=" + URLEncoder.encode(params.get(n).toString(), Charsets.UTF_8);
        }
        return s;
    }

    /**
     * Sets a minimal escape markup function for a Select2Choice component.
     * This function replaces certain characters and formats the display of choices.
     *
     * @param selectItem the Select2Choice component to set the escape markup for
     */
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

    /**
     * Checks if a nanopublication is of a specific class.
     *
     * @param np       the nanopublication to check
     * @param classIri the IRI of the class to check against
     * @return true if the nanopublication is of the specified class, false otherwise
     */
    public static boolean isNanopubOfClass(Nanopub np, IRI classIri) {
        return NanopubUtils.getTypes(np).contains(classIri);
    }

    /**
     * Checks if a nanopublication uses a specific predicate in its assertion.
     *
     * @param np           the nanopublication to check
     * @param predicateIri the IRI of the predicate to look for
     * @return true if the predicate is used in the assertion, false otherwise
     */
    public static boolean usesPredicateInAssertion(Nanopub np, IRI predicateIri) {
        for (Statement st : np.getAssertion()) {
            if (predicateIri.equals(st.getPredicate())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a map of FOAF names from the nanopublication's pubinfo.
     *
     * @param np the nanopublication from which to extract FOAF names
     * @return a map where keys are subjects and values are FOAF names
     */
    public static Map<String, String> getFoafNameMap(Nanopub np) {
        Map<String, String> foafNameMap = new HashMap<>();
        for (Statement st : np.getPubinfo()) {
            if (st.getPredicate().equals(FOAF.NAME) && st.getObject() instanceof Literal objL) {
                foafNameMap.put(st.getSubject().stringValue(), objL.stringValue());
            }
        }
        return foafNameMap;
    }

    /**
     * Creates an SHA-256 hash of the string representation of an object and returns it as a hexadecimal string.
     *
     * @param obj the object to hash
     * @return the SHA-256 hash of the object's string representation in hexadecimal format
     */
    public static String createSha256HexHash(Object obj) {
        return Hashing.sha256().hashString(obj.toString(), StandardCharsets.UTF_8).toString();
    }

    /**
     * Gets the types of a nanopublication.
     *
     * @param np the nanopublication from which to extract types
     * @return a list of IRI types associated with the nanopublication
     */
    public static List<IRI> getTypes(Nanopub np) {
        List<IRI> l = new ArrayList<>();
        for (IRI t : NanopubUtils.getTypes(np)) {
            if (t.equals(FIP.AVAILABLE_FAIR_ENABLING_RESOURCE)) continue;
            if (t.equals(FIP.FAIR_ENABLING_RESOURCE_TO_BE_DEVELOPED))
                continue;
            if (t.equals(FIP.AVAILABLE_FAIR_SUPPORTING_RESOURCE)) continue;
            if (t.equals(FIP.FAIR_SUPPORTING_RESOURCE_TO_BE_DEVELOPED))
                continue;
            l.add(t);
        }
        return l;
    }

    /**
     * Gets a label for a type IRI.
     *
     * @param typeIri the IRI of the type
     * @return a label for the type, potentially truncated
     */
    public static String getTypeLabel(IRI typeIri) {
        if (typeIri.equals(FIP.FAIR_ENABLING_RESOURCE)) return "FER";
        if (typeIri.equals(FIP.FAIR_SUPPORTING_RESOURCE)) return "FSR";
        if (typeIri.equals(FIP.FAIR_IMPLEMENTATION_PROFILE)) return "FIP";
        if (typeIri.equals(NPX.DECLARED_BY)) return "user intro";
        String l = typeIri.stringValue();
        l = l.replaceFirst("^.*[/#]([^/#]+)[/#]?$", "$1");
        l = l.replaceFirst("^(.+)Nanopub$", "$1");
        if (l.length() > 25) l = l.substring(0, 20) + "...";
        return l;
    }

    /**
     * Gets a label for a URI.
     *
     * @param uri the URI to get the label from
     * @return a label for the URI, potentially truncated
     */
    public static String getUriLabel(String uri) {
        if (uri == null) return "";
        String uriLabel = uri;
        if (uriLabel.matches(".*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}([^A-Za-z0-9-_].*)?")) {
            String newUriLabel = uriLabel.replaceFirst("(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{8})[A-Za-z0-9-_]{35}([^A-Za-z0-9-_].*)?", "$1...$2");
            if (newUriLabel.length() <= 70) return newUriLabel;
        }
        if (uriLabel.length() > 70) return uri.substring(0, 30) + "..." + uri.substring(uri.length() - 30);
        return uriLabel;
    }

    /**
     * Gets an ExternalLink with a URI label.
     *
     * @param markupId the markup ID for the link
     * @param uri      the URI to link to
     * @return an ExternalLink with the URI label
     */
    public static ExternalLink getUriLink(String markupId, String uri) {
        return new ExternalLink(markupId, (Utils.isLocalURI(uri) ? "" : uri), getUriLabel(uri));
    }

    /**
     * Gets an ExternalLink with a model for the URI label.
     *
     * @param markupId the markup ID for the link
     * @param model    the model containing the URI
     * @return an ExternalLink with the URI label
     */
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

    /**
     * Creates a sublist from a list based on the specified indices.
     *
     * @param list      the list from which to create the sublist
     * @param fromIndex the starting index (inclusive) for the sublist
     * @param toIndex   the ending index (exclusive) for the sublist
     * @param <E>       the type of elements in the list
     * @return an ArrayList containing the elements from the specified range
     */
    public static <E> ArrayList<E> subList(List<E> list, long fromIndex, long toIndex) {
        // So the resulting list is serializable:
        return new ArrayList<E>(list.subList((int) fromIndex, (int) toIndex));
    }

    /**
     * Creates a sublist from an array based on the specified indices.
     *
     * @param array     the array from which to create the sublist
     * @param fromIndex the starting index (inclusive) for the sublist
     * @param toIndex   the ending index (exclusive) for the sublist
     * @param <E>       the type of elements in the array
     * @return an ArrayList containing the elements from the specified range
     */
    public static <E> ArrayList<E> subList(E[] array, long fromIndex, long toIndex) {
        return subList(Arrays.asList(array), fromIndex, toIndex);
    }

    /**
     * Comparator for sorting ApiResponseEntry objects based on a specified field.
     */
    // TODO Move this to ApiResponseEntry class?
    public static class ApiResponseEntrySorter implements Comparator<ApiResponseEntry>, Serializable {

        private static final long serialVersionUID = 1L;

        private String field;
        private boolean descending;

        /**
         * Constructor for ApiResponseEntrySorter.
         *
         * @param field      the field to sort by
         * @param descending if true, sorts in descending order; if false, sorts in ascending order
         */
        public ApiResponseEntrySorter(String field, boolean descending) {
            this.field = field;
            this.descending = descending;
        }

        /**
         * Compares two ApiResponseEntry objects based on the specified field.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            if (descending) {
                return o2.get(field).compareTo(o1.get(field));
            } else {
                return o1.get(field).compareTo(o2.get(field));
            }
        }

    }

    /**
     * MIME type for TriG RDF format.
     */
    public static final String TYPE_TRIG = "application/trig";

    /**
     * MIME type for Jelly RDF format.
     */
    public static final String TYPE_JELLY = "application/x-jelly-rdf";

    /**
     * MIME type for JSON-LD format.
     */
    public static final String TYPE_JSONLD = "application/ld+json";

    /**
     * MIME type for N-Quads format.
     */
    public static final String TYPE_NQUADS = "application/n-quads";

    /**
     * MIME type for Trix format.
     */
    public static final String TYPE_TRIX = "application/trix";

    /**
     * MIME type for HTML format.
     */
    public static final String TYPE_HTML = "text/html";

    public static final String SUPPORTED_TYPES =
            TYPE_TRIG + "," +
            TYPE_JELLY + "," +
            TYPE_JSONLD + "," +
            TYPE_NQUADS + "," +
            TYPE_TRIX + "," +
            TYPE_HTML;

    /**
     * List of supported MIME types for nanopublications.
     */
    public static final List<String> SUPPORTED_TYPES_LIST = Arrays.asList(StringUtils.split(SUPPORTED_TYPES, ','));

    // TODO Move these to nanopub-java library:

    /**
     * Retrieves a set of introduced IRI IDs from the nanopublication.
     *
     * @param np the nanopublication from which to extract introduced IRI IDs
     * @return a set of introduced IRI IDs
     */
    public static Set<String> getIntroducedIriIds(Nanopub np) {
        Set<String> introducedIriIds = new HashSet<>();
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(NPX.INTRODUCES)) continue;
            if (st.getObject() instanceof IRI obj) introducedIriIds.add(obj.stringValue());
        }
        return introducedIriIds;
    }

    /**
     * Retrieves a set of embedded IRI IDs from the nanopublication.
     *
     * @param np the nanopublication from which to extract embedded IRI IDs
     * @return a set of embedded IRI IDs
     */
    public static Set<String> getEmbeddedIriIds(Nanopub np) {
        Set<String> embeddedIriIds = new HashSet<>();
        for (Statement st : np.getPubinfo()) {
            if (!st.getSubject().equals(np.getUri())) continue;
            if (!st.getPredicate().equals(NPX.EMBEDS)) continue;
            if (st.getObject() instanceof IRI obj) embeddedIriIds.add(obj.stringValue());
        }
        return embeddedIriIds;
    }

    /**
     * Returns the URL of the default Nanopub Registry as configured by the given instance.
     *
     * @return Nanopub Registry URL
     */
    public static String getMainRegistryUrl() {
        try {
            return EnvironmentUtils.getProcEnvironment().getOrDefault("NANODASH_MAIN_REGISTRY", "https://registry.knowledgepixels.com/");
        } catch (IOException ex) {
            logger.error("Could not get NANODASH_MAIN_REGISTRY environment variable, using default.", ex);
            return "https://registry.knowledgepixels.com/";
        }
    }

    private static final String PLAIN_LITERAL_PATTERN = "^\"(([^\\\\\\\"]|\\\\\\\\|\\\\\")*)\"";
    private static final String LANGTAG_LITERAL_PATTERN = "^\"(([^\\\\\\\"]|\\\\\\\\|\\\\\")*)\"@([0-9a-zA-Z-]{2,})$";
    private static final String DATATYPE_LITERAL_PATTERN = "^\"(([^\\\\\\\"]|\\\\\\\\|\\\\\")*)\"\\^\\^<([^ ><\"^]+)>";

    /**
     * Checks whether string is valid literal serialization.
     *
     * @param literalString the literal string
     * @return true if valid
     */
    public static boolean isValidLiteralSerialization(String literalString) {
        if (literalString.matches(PLAIN_LITERAL_PATTERN)) {
            return true;
        } else if (literalString.matches(LANGTAG_LITERAL_PATTERN)) {
            return true;
        } else if (literalString.matches(DATATYPE_LITERAL_PATTERN)) {
            return true;
        }
        return false;
    }

    /**
     * Returns a serialized version of the literal.
     *
     * @param literal the literal
     * @return the String serialization of the literal
     */
    public static String getSerializedLiteral(Literal literal) {
        if (literal.getLanguage().isPresent()) {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"@" + Literals.normalizeLanguageTag(literal.getLanguage().get());
        } else if (literal.getDatatype().equals(XSD.STRING)) {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"";
        } else {
            return "\"" + getEscapedLiteralString(literal.stringValue()) + "\"^^<" + literal.getDatatype() + ">";
        }
    }

    /**
     * Parses a serialized literal into a Literal object.
     *
     * @param serializedLiteral The serialized String of the literal
     * @return The parse Literal object
     */
    public static Literal getParsedLiteral(String serializedLiteral) {
        if (serializedLiteral.matches(PLAIN_LITERAL_PATTERN)) {
            return vf.createLiteral(getUnescapedLiteralString(serializedLiteral.replaceFirst(PLAIN_LITERAL_PATTERN, "$1")));
        } else if (serializedLiteral.matches(LANGTAG_LITERAL_PATTERN)) {
            String langtag = serializedLiteral.replaceFirst(LANGTAG_LITERAL_PATTERN, "$3");
            return vf.createLiteral(getUnescapedLiteralString(serializedLiteral.replaceFirst(LANGTAG_LITERAL_PATTERN, "$1")), langtag);
        } else if (serializedLiteral.matches(DATATYPE_LITERAL_PATTERN)) {
            IRI datatype = vf.createIRI(serializedLiteral.replaceFirst(DATATYPE_LITERAL_PATTERN, "$3"));
            return vf.createLiteral(getUnescapedLiteralString(serializedLiteral.replaceFirst(DATATYPE_LITERAL_PATTERN, "$1")), datatype);
        }
        throw new IllegalArgumentException("Not a valid literal serialization: " + serializedLiteral);
    }

    /**
     * Escapes quotes (") and slashes (/) of a literal string.
     *
     * @param unescapedString un-escaped string
     * @return escaped string
     */
    public static String getEscapedLiteralString(String unescapedString) {
        return unescapedString.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"");
    }

    /**
     * Un-escapes quotes (") and slashes (/) of a literal string.
     *
     * @param escapedString escaped string
     * @return un-escaped string
     */
    public static String getUnescapedLiteralString(String escapedString) {
        return escapedString.replaceAll("\\\\(\\\\|\\\")", "$1");
    }

    /**
     * Checks if a given IRI is a local URI.
     *
     * @param uri the IRI to check
     * @return true if the IRI is a local URI, false otherwise
     */
    public static boolean isLocalURI(IRI uri) {
        return uri != null && isLocalURI(uri.stringValue());
    }

    /**
     * Checks if a given string is a local URI.
     *
     * @param uriAsString the string to check
     * @return true if the string is a local URI, false otherwise
     */
    public static boolean isLocalURI(String uriAsString) {
        return !uriAsString.isBlank() && uriAsString.startsWith(LocalUri.PREFIX);
    }

}
