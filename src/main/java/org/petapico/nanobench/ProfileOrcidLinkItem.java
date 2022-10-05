package org.petapico.nanobench;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

// This is currently disabled

public class ProfileOrcidLinkItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileOrcidLinkItem(String id) {
		super(id);

		final NanobenchSession session = NanobenchSession.get();

		Link<String> retryLink = new Link<String>("retry") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				getSession().invalidateNow();
				session.resetOrcidLinked();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		if (session.isOrcidLinked() == null) {
			add(new Label("orcidlinkmessage", ""));
		} else if (!session.isOrcidLinked()) {
			add(new Label("orcidlinkmessage", "Follow <a href=\"." + OrcidLinkingPage.MOUNT_PATH + "\">these instructions</a> to link it, and then press 'retry'.").setEscapeModelStrings(false));
		} else if (session.isOrcidLinked()) {
			add(new Label("orcidlinkmessage", "ORCID is linked."));
			retryLink.setVisible(false);
		}
		add(new Label("orcidlinkerror", session.getOrcidLinkError()));
		add(retryLink);
	}

}
