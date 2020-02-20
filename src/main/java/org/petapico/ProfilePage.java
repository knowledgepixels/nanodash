package org.petapico;

import java.io.File;
import java.security.KeyPair;

import javax.xml.bind.DatatypeConverter;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;

public class ProfilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static KeyPair keyPair;

	static KeyPair getKeyPair() {
		if (keyPair == null) {
			try {
				keyPair = SignNanopub.loadKey("~/.nanopub/id_rsa", SignatureAlgorithm.RSA);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return keyPair;
	}

	public ProfilePage(final PageParameters parameters) {
		super();
		String keyFilename = System.getProperty("user.home") + "/.nanopub/id_rsa";
		File keyFile = new File(keyFilename);
		add(new Label("keyfile", keyFilename));
		if (keyFile.exists()) {
			if (getKeyPair() == null) {
				add(new Label("pubkey", "Error loading key file"));
			} else {
				String pubkeyString = DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
				add(new Label("pubkey", pubkeyString));
			}
		} else {
			add(new Label("pubkey", "Key file not found"));
		}
	}

}
