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
				getSession().invalidateNow();
				ProfilePage.resetOrcidLinked();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		if (ProfilePage.isOrcidLinked() == null) {
			add(new Label("orcidlinkmessage", ""));
		} else if (!ProfilePage.isOrcidLinked()) {
			add(new Label("orcidlinkmessage", "Follow <a href=\"./orcidlinking\">these instructions</a> to link it, and then press 'retry'.").setEscapeModelStrings(false));
		} else if (ProfilePage.isOrcidLinked()) {
			add(new Label("orcidlinkmessage", "ORCID is correctly linked."));
			retryLink.setVisible(false);
		}
		add(new Label("orcidlinkerror", ProfilePage.getOrcidLinkError()));
		add(retryLink);
	}

}
