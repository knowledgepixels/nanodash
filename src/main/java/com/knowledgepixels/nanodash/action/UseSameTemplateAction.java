package com.knowledgepixels.nanodash.action;

import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.template.TemplateData;

public class UseSameTemplateAction extends NanopubAction {

	private static final long serialVersionUID = 4348436856820074305L;

	@Override
	public String getLinkLabel(Nanopub np) {
		return "create new with same template";
	}

	@Override
	public String getTemplateUri(Nanopub np) {
		return TemplateData.get().getTemplateId(np).stringValue();
	}

	@Override
	public String getParamString(Nanopub np) {
		return "";
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
