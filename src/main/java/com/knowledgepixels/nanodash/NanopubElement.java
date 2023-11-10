package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;

public class NanopubElement implements Serializable {

	private static final long serialVersionUID = 1L;

	private Nanopub nanopub;
	private String uriString; // Keeping URI separately, as nanopub might be null when it cannot be fetched
	private String label;
	private Calendar creationTime;
	private Boolean seemsToHaveSignature;
	private Boolean hasValidSignature;
	private IRI signerId;
	private List<IRI> types;
	private Boolean isRetraction;
	private Boolean hasRetractionTarget;
	private IRI retractionTarget;
	private boolean retracted;

	public NanopubElement(String uri) {
		this(uri, false);
	}

	public NanopubElement(String uri, boolean retracted) {
		this.uriString = uri;
		this.nanopub = Utils.getNanopub(uri);
		if (nanopub == null) throw new IllegalArgumentException("No nanopublication found for URI: " + uri);
		this.retracted = retracted;
	}

	public NanopubElement(Nanopub nanopub) {
		this.uriString = nanopub.getUri().stringValue();
		this.nanopub = nanopub;
	}

	public Nanopub getNanopub() {
		return nanopub;
	}

	public boolean isRetracted() {
		return retracted;
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

	public String getPubkey() throws GeneralSecurityException, MalformedCryptoElementException {
		if (!hasValidSignature()) return null;
		return SignatureUtils.getSignatureElement(nanopub).getPublicKeyString();
	}

	public boolean hasValidSignature() throws GeneralSecurityException, MalformedCryptoElementException {
		if (nanopub == null) return false;
		if (hasValidSignature == null) {
			NanopubSignatureElement se = SignatureUtils.getSignatureElement(nanopub);
			if (se != null) {
				hasValidSignature = SignatureUtils.hasValidSignature(se);
				if (se.getSigners().size() == 1) signerId = se.getSigners().iterator().next();
			} else {
				hasValidSignature = false;
			}
		}
		return hasValidSignature;
	}

	public IRI getSignerId() {
		if (nanopub == null) return null;
		try {
			if (hasValidSignature == null) hasValidSignature();
		} catch (GeneralSecurityException | MalformedCryptoElementException ex) {
			ex.printStackTrace();
		}
		return signerId;
	}

	public List<IRI> getTypes() {
		isRetraction = false;
		if (types == null) {
			types = new ArrayList<>();
			if (nanopub == null) return types;
			for (Statement st : nanopub.getPubinfo()) {
				if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
					types.add((IRI) st.getObject());
					if (st.getObject().stringValue().equals("http://purl.org/nanopub/x/RetractionNanopub")) {
						isRetraction = true;
					}
				}
			}
		}
		return types;
	}

	public IRI getRetractionTarget() {
		if (isRetraction == null) {
			getTypes();
		}
		if (hasRetractionTarget == null) {
			hasRetractionTarget = false;
			if (nanopub == null) return null;
			if (isRetraction && nanopub.getAssertion().size() == 1) {
				Statement aSt = nanopub.getAssertion().iterator().next();
				String p = aSt.getPredicate().stringValue();
				if (p.equals("http://purl.org/nanopub/x/retracts") && aSt.getObject() instanceof IRI) {
					retractionTarget = (IRI) aSt.getObject();
					hasRetractionTarget = true;
				}
			}
		}
		return retractionTarget;
	}

}
