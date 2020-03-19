package org.petapico.nanobench;

import javax.xml.bind.DatatypeConverter;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class ProfileSigItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileSigItem(String id) {
		super(id);

		Label keymessage = new Label("keymessage", "Next, you need to create the key files necessary for digital signatures:");
		Link<String> createKeyLink = new Link<String>("createkey") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				ProfilePage.makeKeys();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		add(new Label("keyfile", ProfilePage.getKeyfile().getPath()));
		if (ProfilePage.getKeyfile().exists()) {
			if (ProfilePage.getKeyPair() == null) {
				add(new Label("pubkey", "Error loading key file"));
			} else {
				String pubkeyString = DatatypeConverter.printBase64Binary(ProfilePage.getKeyPair().getPublic().getEncoded()).replaceAll("\\s", "");
				add(new Label("pubkey", pubkeyString));
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
