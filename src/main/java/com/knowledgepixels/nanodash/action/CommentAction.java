package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

public class CommentAction extends NanopubAction {

	private static final long serialVersionUID = 7995012295240119652L;

	public static final String TEMPLATE_URI = "http://purl.org/np/RAqfUmjV05ruLK3Efq2kCODsHfY16LJGO3nAwDi5rmtv0";

	@Override
	public String getLinkLabel(Nanopub np) {
		return "comment";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return TEMPLATE_URI;
	}

	@Override
	public String getParamString(Nanopub np) {
		return "param_thing=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return true;
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
