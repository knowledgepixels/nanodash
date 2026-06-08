package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.page.ProfilePage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;

/**
 * Account/identity controls for the current user's own About tab: a logout
 * button, and — only in local mode (running without ORCID authentication) — a
 * form to set or change the ORCID identifier. There is deliberately no login
 * button here (that belongs to the logged-out state); in ORCID-login mode the
 * identifier comes from authentication and the form is hidden.
 */
public class ProfileAccountPanel extends Panel {

    /**
     * @param id            the Wicket markup id
     * @param userIriString the IRI of the user whose (own) About page this is
     */
    public ProfileAccountPanel(String id, String userIriString) {
        super(id);
        final NanodashSession session = NanodashSession.get();
        session.loadProfileInfo();
        final boolean loginMode = NanodashPreferences.get().isOrcidLoginMode();

        // Logout only makes sense with an ORCID-login session; in local mode
        // there is no login to end.
        Link<Void> logout = new Link<Void>("logout") {
            @Override
            public void onClick() {
                session.logout();
                throw new RestartResponseException(UserPage.class, new PageParameters().set("id", userIriString));
            }
        };
        logout.setVisible(loginMode);
        add(logout);

        Model<String> model = Model.of("");
        if (session.getUserIri() != null) {
            model.setObject(session.getUserIri().stringValue().replaceFirst("^https://orcid.org/", ""));
        }
        final TextField<String> orcidField = new TextField<>("orcidfield", model);
        orcidField.add(new PatternValidator(ProfilePage.ORCID_PATTERN));
        Form<Void> form = new Form<Void>("form") {
            @Override
            protected void onSubmit() {
                if (loginMode) return;
                session.setOrcid(orcidField.getModelObject());
                String newUserIri = "https://orcid.org/" + orcidField.getModelObject();
                session.invalidateNow();
                throw new RestartResponseException(UserPage.class,
                        new PageParameters().set("id", newUserIri).set("tab", "about"));
            }
        };
        form.add(orcidField);
        // Setting/changing the ORCID is only available in local mode (no ORCID
        // authentication); in ORCID-login mode the identifier comes from auth.
        form.setVisible(!loginMode);
        add(form);

        add(new FeedbackPanel("feedback"));

        // Signing key: public key + (local mode) the local key-file path.
        add(new ProfileSigItem("sigpart"));
    }

}
