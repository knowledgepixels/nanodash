package org.petapico.nanobench.action;

import org.nanopub.Nanopub;

public class RetractionAction extends NanopubAction {

	@Override
	public String getLinkLabel() {
		return "retract";
	}

	@Override
	public String getLinkTarget(Nanopub np) {
		return "./publish?" +
			"template=http://purl.org/np/RAvySE8-JDPqaPnm_XShAa-aVuDZ2iW2z7Oc1Q9cfvxZE" +
			"&param_nanopubToBeRetracted=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return true;
	}

	@Override
	public boolean isApplicableToOthersNanopubs() {
		return false;
	}

}
