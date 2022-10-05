package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

public class PublishConfirmPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publishconfirm";

	public PublishConfirmPage(Nanopub np) {
		super();
		add(new TitleBar("titlebar"));
		PageParameters params = new PageParameters();
		params.add("id", NanobenchSession.get().getUserIri());
		add(new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params));
		add(new NanopubItem("nanopub", new NanopubElement(np), false, false));
	}

}
