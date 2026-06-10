package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.PatternValidator;

/**
 * The ProfilePage class represents a user profile page in the Nanodash application.
 */
public class ProfilePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/profile";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * The pattern for ORCID identifiers.
     */
    public static final String ORCID_PATTERN = "[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]";

    /**
     * Constructor for the ProfilePage.
     *
     * @param parameters the page parameters
     */
    public ProfilePage(final PageParameters parameters) {
        super(parameters);

        final NanodashSession session = NanodashSession.get();
        session.loadProfileInfo();
//		User.refreshUsers();

        // Once the user's ORCID iD is known, their own About page is the canonical,
        // richer profile surface (the "This is you" account box plus the profile /
        // introduction / recommendation views), so forward there. This page then only
        // serves the no-iD-yet case: the ORCID setup form in local mode.
        if (session.getUserIri() != null) {
            throw new RedirectToUrlException(UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(session.getUserIri()) + "&tab=about");
        }

        final boolean loginMode = NanodashPreferences.get().isOrcidLoginMode();

        add(new TitleBar("titlebar", this, null));

        // Past the guard above the user has no ORCID iD yet, so this page only helps
        // them obtain one: a login link in ORCID-login mode, or a manual entry form in
        // local mode. (Everything else — account box, signing key, introductions,
        // profile picture, license — lives on the user's About page once they have an iD.)
        if (loginMode) {
            add(new Label("message", "Before you can publish your own nanopublications, you need to login via ORCID."));
            add(new Label("orcidmessage", ""));
            add(new ExternalLink("loginout", OrcidLoginPage.getOrcidLoginUrl("/profile"), "login with ORCID"));
        } else {
            add(new Label("message", "Before you can publish your own nanopublications, you need to set your ORCID identifier."));
            add(new Label("loginout", "").setVisible(false));
            add(new Label("orcidmessage", "Set your ORCID identifier below. " +
                                          "If you don't yet have an ORCID account, you can make one via the " +
                                          "<a href=\"https://orcid.org/\">ORCID website</a>.").setEscapeModelStrings(false));
        }

        final TextField<String> orcidField = new TextField<>("orcidfield", Model.of(""));
        orcidField.add(new PatternValidator(ORCID_PATTERN));
        Form<Void> form = new Form<Void>("form") {

            @Override
            protected void onSubmit() {
                if (loginMode) return;
                session.setOrcid(orcidField.getModelObject());
                session.invalidateNow();
                throw new RestartResponseException(ProfilePage.class);
            }

        };
        WebMarkupContainer submitButton = new WebMarkupContainer("submit");
        if (loginMode) {
            // In ORCID-login mode the identifier comes from auth, not manual entry.
            orcidField.setEnabled(false);
            submitButton.setVisible(false);
        }
        form.add(orcidField);
        form.add(submitButton);
        form.add(new Label("orcidname", ""));
        add(form);

        add(new FeedbackPanel("feedback"));
    }

}
