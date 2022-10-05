package org.petapico.nanobench;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;

public class ProfilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/profile";

	public static final String ORCID_PATTERN = "[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]";

	public ProfilePage(final PageParameters parameters) {
		super();
		final NanobenchSession session = NanobenchSession.get();
		session.loadProfileInfo();
		User.refreshUsers();
		final boolean loginMode = NanobenchPreferences.get().isOrcidLoginMode();

		add(new TitleBar("titlebar"));

		if (session.isProfileComplete()) {
			add(new Label("message", "Congratulations, your profile is sufficiently complete to publish your own nanopublications. " +
					"You might see additional recommended actions below."));
		} else {
			if (loginMode) {
				add(new Label("message", "Before you can publish your own nanopublications, you need to login via ORCID."));
			} else {
				add(new Label("message", "Before you can publish your own nanopublications, you need to set your ORCID identifier."));
			}
		}

		if (session.getUserIri() == null) {
			if (loginMode) {
				String loginUrl = OrcidLoginPage.getOrcidLoginUrl("/profile");
				add(new Label("orcidmessage", "<a href=\"" + loginUrl + "\">Login with ORCID.</a>").setEscapeModelStrings(false));
			} else {
				add(new Label("orcidmessage", "Set your ORCID identifier below. " +
						"If you don't yet have an ORCID account, you can make one via the " +
						"<a href=\"https://orcid.org/\">ORCID website</a>.").setEscapeModelStrings(false));
			}
		} else {
			add(new Label("orcidmessage", ""));
		}

		Model<String> model = Model.of("");
		if (session.getUserIri() != null) {
			model.setObject(session.getUserIri().stringValue().replaceFirst("^https://orcid.org/", ""));
		}
		final TextField<String> orcidField = new TextField<>("orcidfield", model);
		orcidField.add(new PatternValidator(ORCID_PATTERN));
		Form<Void> form = new Form<Void>("form") {

			private static final long serialVersionUID = 6733510753912762551L;

			@Override
			protected void onSubmit() {
				if (loginMode) return;
				session.setOrcid(orcidField.getModelObject());
				session.resetOrcidLinked();
				session.invalidateNow();
				throw new RestartResponseException(ProfilePage.class);
			}

		};
		WebMarkupContainer submitButton = new WebMarkupContainer("submit");
		if (loginMode || session.getUserIri() != null) {
			orcidField.setEnabled(false);
			submitButton.setVisible(false);
		}
		form.add(orcidField);
		form.add(submitButton);
		String orcidName = session.getOrcidName();
		if (orcidName == null) {
			form.add(new Label("orcidname", ""));
		} else {
			form.add(new Label("orcidname", orcidName));
		}
		add(form);
		add(new FeedbackPanel("feedback"));

		if (session.getUserIri() != null) {
			add(new ProfileSigItem("sigpart"));
		} else {
			add(new Label("sigpart"));
		}

		if (session.getUserIri() != null && session.getKeyPair() != null) {
			add(new ProfileIntroItem("intropart"));
		} else {
			add(new Label("intropart"));
		}

//		if (session.isProfileComplete()) {
//			add(new ProfileOrcidLinkItem("orcidlinkpart"));
//		} else {
//			add(new Label("orcidlinkpart"));
//		}
	}

}
