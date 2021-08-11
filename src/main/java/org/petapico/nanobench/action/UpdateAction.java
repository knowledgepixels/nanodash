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
		if (Template.getTemplateId(np) != null) {
			return Template.getTemplateId(np).stringValue();
		} else {
			return "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
		}
	}

	@Override
	public String getParamString(Nanopub np) {
		return "supersede=" + getEncodedUri(np);
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
		return true;
	}

}
