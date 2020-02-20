package org.petapico;

import java.io.File;
import java.security.KeyPair;

import javax.xml.bind.DatatypeConverter;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;

public class ProfilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private static KeyPair keyPair;
	private static IRI userIri;

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

	static IRI getUserIri() {
		return userIri;
	}

	public ProfilePage(final PageParameters parameters) {
		super();
		Model<String> model = Model.of("");
		if (userIri != null) {
			model.setObject(userIri.stringValue().replaceFirst("^https://orcid.org/", ""));
		}
		final TextField<String> orcidField = new TextField<>("orcidfield", model);
		orcidField.add(new PatternValidator("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]"));
		Form<Void> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				setOrcid(orcidField.getModelObject());
			}

		};
		form.add(orcidField);
		add(form);
		add(new FeedbackPanel("feedback"));

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

	private static void setOrcid(String orcid) {
		userIri = vf.createIRI("https://orcid.org/" + orcid);
	}

}
