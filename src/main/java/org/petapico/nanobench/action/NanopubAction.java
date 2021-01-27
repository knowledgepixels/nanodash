package org.petapico.nanobench.action;

import org.nanopub.Nanopub;
import org.petapico.nanobench.Utils;

public abstract class NanopubAction {

	public abstract String getLinkLabel();

	public abstract String getLinkTarget(Nanopub np);

	public abstract boolean isApplicableToOwnNanopubs();

	public abstract boolean isApplicableToOthersNanopubs();

	protected static String getEncodedUri(Nanopub np) {
		return Utils.urlEncode(np.getUri().stringValue());
	}

}
