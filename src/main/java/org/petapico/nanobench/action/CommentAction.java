package org.petapico.nanobench.action;

import org.nanopub.Nanopub;

public class CommentAction extends NanopubAction {

	@Override
	public String getLinkLabel() {
		return "comment";
	}

	@Override
	public String getLinkTarget(Nanopub np) {
		return "./publish?" +
			"template=http://purl.org/np/RAqfUmjV05ruLK3Efq2kCODsHfY16LJGO3nAwDi5rmtv0" +
			"&param_thing=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return true;
	}

	@Override
	public boolean isApplicableToOthersNanopubs() {
		return true;
	}

}
