package org.petapico;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nanopub.Nanopub;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;

public class NanopubElement implements Serializable {

	private static final long serialVersionUID = 1L;

	private Nanopub nanopub;
	private Calendar creationTime;
	private Boolean seemsToHaveSignature;
	private Boolean hasValidSignature;
	private List<URI> types;

	public NanopubElement(Nanopub nanopub) {
		this.nanopub = nanopub;
	}

	public NanopubElement(String uri) {
		this.nanopub = GetNanopub.get(uri);
	}

	public Nanopub getNanopub() {
		return nanopub;
	}

	public String getUri() {
		return nanopub.getUri().stringValue();
	}

	public Calendar getCreationTime() {
		if (creationTime == null) {
			creationTime = SimpleTimestampPattern.getCreationTime(nanopub);
		}
		return creationTime;
	}

	public boolean seemsToHaveSignature() {
		if (seemsToHaveSignature == null) {
			seemsToHaveSignature = SignatureUtils.seemsToHaveSignature(nanopub);
		}
		return seemsToHaveSignature;
	}

	public boolean hasValidSignature() throws GeneralSecurityException, MalformedCryptoElementException {
		if (hasValidSignature == null) {
			hasValidSignature = SignatureUtils.hasValidSignature(SignatureUtils.getSignatureElement(nanopub));
		}
		return hasValidSignature;
	}

	public List<URI> getTypes() {
		if (types == null) {
			types = new ArrayList<>();
			for (Statement st : nanopub.getPubinfo()) {
				if (st.getSubject().equals(nanopub.getUri()) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof URI) {
					types.add((URI) st.getObject());
				}
			}
		}
		return types;
	}
}
