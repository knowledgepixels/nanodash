package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class PublishPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publish";

	public PublishPage(final PageParameters parameters) {
		super();
		final NanobenchSession session = NanobenchSession.get();
		add(new TitleBar("titlebar"));
		if (!NanobenchSession.get().isProfileComplete()) {
			throw new RedirectToUrlException(OrcidLoginPage.getOrcidLoginUrl("." + MOUNT_PATH, parameters));
		}
		if (parameters.get("template").toString() != null) {
			if (!parameters.get("sigkey").isNull() && !parameters.get("sigkey").toString().equals(session.getPubkeyString())) {
				add(new DifferentKeyErrorItem("form", parameters));
			} else {
				add(new PublishForm("form", parameters, this));
			}
		} else {
			add(new TemplateList("form"));
		}
	}

}
