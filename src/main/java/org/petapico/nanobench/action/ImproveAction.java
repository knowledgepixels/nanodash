package org.petapico.nanobench.action;

import org.nanopub.Nanopub;
import org.petapico.nanobench.Template;

public class ImproveAction extends NanopubAction {

	@Override
	public String getLinkLabel(Nanopub np) {
		return "improve";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return Template.getTemplateId(np).stringValue();
	}

	@Override
	public String getParamString(Nanopub np) {
		return "improve=" + getEncodedUri(np);
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
		return Template.getTemplateId(np) != null;
	}

}
