package org.petapico.nanobench;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class ProfileOrcidLinkItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileOrcidLinkItem(String id) {
		super(id);

		Link<String> retryLink = new Link<String>("retry") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		retryLink.setVisible(false);
		if (ProfilePage.isOrcidLinked() == null) {
			add(new Label("orcidlinkmessage", ""));
			retryLink.setVisible(true);
		} else if (ProfilePage.isOrcidLinked()) {
			add(new Label("orcidlinkmessage", "ORCID is correctly linked."));
		} else {
			add(new Label("orcidlinkmessage", "ORCID is not yet linked."));
		}
		add(new Label("orcidlinkerror", ProfilePage.getOrcidLinkError()));
		add(retryLink);
	}

}
