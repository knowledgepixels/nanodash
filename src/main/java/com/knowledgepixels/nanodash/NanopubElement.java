package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Represents a wrapper for a Nanopub object, providing additional metadata and utility methods.
 * This class includes caching mechanisms to avoid redundant Nanopub retrievals.
 */
public class NanopubElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Map<String, NanopubElement> nanopubCache = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NanopubElement.class);

    /**
     * Retrieves a NanopubElement instance for the given URI.
     * If the instance is not already cached, it creates a new one and caches it.
     *
     * @param uri The URI of the Nanopub.
     * @return The NanopubElement instance.
     */
    public static NanopubElement get(String uri) {
        if (!nanopubCache.containsKey(uri)) {
            nanopubCache.put(uri, new NanopubElement(uri));
        }
        return nanopubCache.get(uri);
    }

    /**
     * Retrieves a NanopubElement instance for the given Nanopub object.
     * If the instance is not already cached, it creates a new one and caches it.
     *
     * @param nanopub The Nanopub object.
     * @return The NanopubElement instance.
     */
    public static NanopubElement get(Nanopub nanopub) {
        if (nanopub == null) {
            throw new IllegalArgumentException("Nanopub cannot be null");
        }
        String uri = nanopub.getUri().stringValue();
        if (!nanopubCache.containsKey(uri)) {
            nanopubCache.put(uri, new NanopubElement(nanopub));
        }
        return nanopubCache.get(uri);
    }

    private Nanopub nanopub;
    private String uriString; // Keeping URI separately, as nanopub might be null when it cannot be fetched
    private String label;
    private Calendar creationTime;
    private Boolean seemsToHaveSignature;
    private Boolean hasValidSignature;
    private IRI signerId;
    private List<IRI> types;
    private Map<String, String> foafNameMap;

    /**
     * Constructs a NanopubElement for the given URI.
     * Fetches the Nanopub object using the URI.
     *
     * @param uri The URI of the Nanopub.
     * @throws IllegalArgumentException If no Nanopub is found for the given URI.
     */
    private NanopubElement(String uri) {
        this.uriString = uri;
        this.nanopub = Utils.getNanopub(uri);
        if (nanopub == null) {
            throw new IllegalArgumentException("No nanopublication found for URI: " + uri);
        }
    }

    /**
     * Constructs a NanopubElement for the given Nanopub object.
     *
     * @param nanopub The Nanopub object.
     */
    private NanopubElement(Nanopub nanopub) {
        this.uriString = nanopub.getUri().stringValue();
        this.nanopub = nanopub;
    }

    /**
     * Returns the Nanopub object.
     *
     * @return The Nanopub object.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the URI of the Nanopub.
     *
     * @return The URI as a string.
     */
    public String getUri() {
        return uriString;
    }

    /**
     * Returns the label of the Nanopub.
     * If the label is not already set, it retrieves it from the Nanopub.
     *
     * @return The label of the Nanopub.
     */
    public String getLabel() {
        if (label != null) return label;
        if (nanopub == null) return null;
        label = NanopubUtils.getLabel(nanopub);
        if (label == null) label = "";
        return label;
    }

    /**
     * Returns the creation time of the Nanopub.
     * If the creation time is not already set, it retrieves it using a timestamp pattern.
     *
     * @return The creation time as a Calendar object.
     */
    public Calendar getCreationTime() {
        if (nanopub == null) return null;
        if (creationTime == null) {
            creationTime = SimpleTimestampPattern.getCreationTime(nanopub);
        }
        return creationTime;
    }

    /**
     * Checks if the Nanopub seems to have a signature.
     *
     * @return True if the Nanopub seems to have a signature, false otherwise.
     */
    public boolean seemsToHaveSignature() {
        if (nanopub == null) return false;
        if (seemsToHaveSignature == null) {
            seemsToHaveSignature = SignatureUtils.seemsToHaveSignature(nanopub);
        }
        return seemsToHaveSignature;
    }

    /**
     * Returns the public key of the Nanopub's signature.
     * If the signature is not valid, returns null.
     *
     * @return The public key as a string, or null if invalid.
     */
    public String getPubkey() {
        if (!hasValidSignature()) return null;
        try {
            return SignatureUtils.getSignatureElement(nanopub).getPublicKeyString();
        } catch (MalformedCryptoElementException ex) {
            logger.error("Error in getting the signature element of the nanopub {}", uriString, ex);
            return null;
        }
    }

    /**
     * Returns the SHA-256 hash of the public key of the Nanopub's signature
     *
     * @return The SHA-256 hash of the public key as a hexadecimal string, or null if the public key is not available.
     */
    public String getPubkeyhash() {
        String pubkey = getPubkey();
        if (pubkey == null) return null;
        return Utils.createSha256HexHash(pubkey);
    }

    /**
     * Checks if the Nanopub has a valid signature.
     *
     * @return True if the Nanopub has a valid signature, false otherwise.
     */
    public boolean hasValidSignature() {
        if (nanopub == null) return false;
        if (hasValidSignature == null) {
            try {
                NanopubSignatureElement se;
                se = SignatureUtils.getSignatureElement(nanopub);
                if (se != null) {
                    hasValidSignature = SignatureUtils.hasValidSignature(se);
                    if (se.getSigners().size() == 1) signerId = se.getSigners().iterator().next();
                } else {
                    hasValidSignature = false;
                }
            } catch (MalformedCryptoElementException | GeneralSecurityException ex) {
                logger.error("Error in checking the signature of the nanopub {}", uriString, ex);
                return false;
            }
        }
        return hasValidSignature;
    }

    /**
     * Returns the ID of the signer of the Nanopub.
     * If the signature is not valid, returns null.
     *
     * @return The signer ID as an IRI, or null if invalid.
     */
    public IRI getSignerId() {
        if (nanopub == null) return null;
        if (!hasValidSignature()) return null;
        return signerId;
    }

    /**
     * Returns the list of RDF types associated with the Nanopub.
     * If the types are not already set, it retrieves them from the Nanopub's pubinfo.
     *
     * @return A list of RDF types as IRIs.
     */
    public List<IRI> getTypes() {
        if (types == null) {
            types = new ArrayList<>();
            if (nanopub == null) return types;
            for (Statement st : nanopub.getPubinfo()) {
                if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
                    types.add((IRI) st.getObject());
                }
            }
        }
        return types;
    }

    /**
     * Returns a map of FOAF names associated with the Nanopub.
     * If the map is not already set, it retrieves it using a utility method.
     *
     * @return A map of FOAF names.
     */
    public Map<String, String> getFoafNameMap() {
        if (foafNameMap == null) foafNameMap = Utils.getFoafNameMap(nanopub);
        return foafNameMap;
    }

}
