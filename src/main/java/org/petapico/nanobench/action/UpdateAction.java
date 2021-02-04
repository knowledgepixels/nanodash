package org.petapico.nanobench.action;

import org.nanopub.Nanopub;
import org.petapico.nanobench.Template;

public class UpdateAction extends NanopubAction {

	@Override
	public String getLinkLabel(Nanopub np) {
		return "update";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return Template.getTemplateId(np).stringValue();
	}

	@Override
	public String getParamString(Nanopub np) {
		return "fill=" + getEncodedUri(np);
	}

	@Override
	public boolean isApplicableToOwnNanopubs() {
		return true;
	}

	@Override
	public boolean isApplicableToOthersNanopubs() {
		return false;
	}

	@Override
	public boolean isApplicableTo(Nanopub np) {
		return Template.getTemplateId(np) != null;
	}

}
