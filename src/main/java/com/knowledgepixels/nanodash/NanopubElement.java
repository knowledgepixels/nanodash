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

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.*;

public class NanopubElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Map<String, NanopubElement> nanopubCache = new HashMap<>();

    public static NanopubElement get(String uri) {
        if (!nanopubCache.containsKey(uri)) {
            nanopubCache.put(uri, new NanopubElement(uri));
        }
        return nanopubCache.get(uri);
    }

    public static NanopubElement get(Nanopub nanopub) {
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

    private NanopubElement(String uri) {
        this.uriString = uri;
        this.nanopub = Utils.getNanopub(uri);
        if (nanopub == null) throw new IllegalArgumentException("No nanopublication found for URI: " + uri);
    }

    private NanopubElement(Nanopub nanopub) {
        this.uriString = nanopub.getUri().stringValue();
        this.nanopub = nanopub;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public String getUri() {
        return uriString;
    }

    public String getLabel() {
        if (label != null) return label;
        if (nanopub == null) return null;
        label = NanopubUtils.getLabel(nanopub);
        if (label == null) label = "";
        return label;
    }

    public Calendar getCreationTime() {
        if (nanopub == null) return null;
        if (creationTime == null) {
            creationTime = SimpleTimestampPattern.getCreationTime(nanopub);
        }
        return creationTime;
    }

    public boolean seemsToHaveSignature() {
        if (nanopub == null) return false;
        if (seemsToHaveSignature == null) {
            seemsToHaveSignature = SignatureUtils.seemsToHaveSignature(nanopub);
        }
        return seemsToHaveSignature;
    }

    public String getPubkey() {
        if (!hasValidSignature()) return null;
        try {
            return SignatureUtils.getSignatureElement(nanopub).getPublicKeyString();
        } catch (MalformedCryptoElementException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public String getPubkeyhash() {
        String pubkey = getPubkey();
        if (pubkey == null) return null;
        return Utils.createSha256HexHash(pubkey);
    }

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
                ex.printStackTrace();
                return false;
            }
        }
        return hasValidSignature;
    }

    public IRI getSignerId() {
        if (nanopub == null) return null;
        if (!hasValidSignature()) return null;
        return signerId;
    }

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

    private Map<String, String> foafNameMap;

    public Map<String, String> getFoafNameMap() {
        if (foafNameMap == null) foafNameMap = Utils.getFoafNameMap(nanopub);
        return foafNameMap;
    }

}
