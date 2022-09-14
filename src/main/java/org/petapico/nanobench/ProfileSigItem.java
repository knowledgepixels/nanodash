package org.petapico.nanobench;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class ProfileSigItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileSigItem(String id) {
		super(id);
		boolean loginMode = NanobenchPreferences.get().isOrcidLoginMode();

		final NanobenchSession session = NanobenchSession.get();

		Label keymessage = new Label("keymessage", "Next, you need to create the key files necessary for digital signatures:");
		Link<String> createKeyLink = new Link<String>("createkey") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				session.makeKeys();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		WebMarkupContainer localFilePanel = new WebMarkupContainer("localfile");
		if (loginMode) {
			localFilePanel.add(new Label("keyfile", ""));
			localFilePanel.setVisible(false);
		} else {
			localFilePanel.add(new Label("keyfile", session.getKeyFile().getPath()));
		}
		add(localFilePanel);
		if (session.getKeyFile().exists()) {
			if (session.getKeyPair() == null) {
				add(new Label("pubkey", "Error loading key file"));
			} else {
				add(new PubkeyItem("pubkey", session.getPubkeyString()));
			}
			keymessage.setVisible(false);
			createKeyLink.setVisible(false);
		} else {
			add(new Label("pubkey", ""));
		}
		add(keymessage);
		add(createKeyLink);
	}

}
