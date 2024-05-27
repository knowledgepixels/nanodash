package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.template.TemplateData;

public class ImproveAction extends NanopubAction {

	private static final long serialVersionUID = 7125118789143870705L;

	@Override
	public String getLinkLabel(Nanopub np) {
		return "improve";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return TemplateData.get().getTemplateId(np).stringValue();
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
		return TemplateData.get().getTemplateId(np) != null;
	}

}
