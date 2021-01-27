package org.petapico.nanobench.action;

import org.nanopub.Nanopub;

public class ApprovalAction extends NanopubAction {

	@Override
	public String getLinkLabel() {
		return "approve/disapprove";
	}

	@Override
	public String getLinkTarget(Nanopub np) {
		return "./publish?" +
			"template=http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg" +
			"&param_nanopub=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return false;
	}

	@Override
	public boolean isApplicableToOthersNanopubs() {
		return true;
	}

}
