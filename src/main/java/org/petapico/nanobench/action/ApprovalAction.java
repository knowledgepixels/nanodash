package org.petapico.nanobench.action;

import org.nanopub.Nanopub;

public class ApprovalAction extends NanopubAction {

	public static final String TEMPLATE_URI = "http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg";

	@Override
	public String getLinkLabel(Nanopub np) {
		return "approve/disapprove";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return TEMPLATE_URI;
	}

	@Override
	public String getParamString(Nanopub np) {
		return "param_nanopub=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return false;
	}

	@Override
	public boolean isApplicableToOthersNanopubs() {
		return true;
	}

	@Override
	public boolean isApplicableTo(Nanopub np) {
		return true;
	}

}
