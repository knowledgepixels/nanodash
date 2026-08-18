package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.ProfilePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.PatternValidator;
import org.nanopub.extra.setting.IntroNanopub;

import java.util.ArrayList;
import java.util.List;

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
        final NanodashPreferences prefs = NanodashPreferences.get();
        final boolean loginMode = prefs.isOrcidLoginMode();

        // Prominent onboarding CTA: a "Create Introduction" button shown only when the
        // user has a local key but no introduction from this site yet. Mirrors the
        // "Create Introduction" action of the 👉 Recommended-actions view (same intro
        // template + params), but surfaced as a primary button so it can't be missed.
        boolean canCreateIntro = session.getKeyPair() != null && session.getLocalIntroCount() == 0;
        String createIntroUrl = "";
        if (canCreateIntro) {
            String shortKey = Utils.getShortPubkeyName(session.getPubkeyString());
            String aboutUrl = UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(userIriString) + "&tab=about";
            createIntroUrl = PublishPage.MOUNT_PATH
                    + "?template=" + Utils.urlEncode("https://w3id.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA")
                    + "&template-version=latest"
                    + "&param_user=" + Utils.urlEncode(userIriString)
                    + "&param_public-key=" + Utils.urlEncode(session.getPubkeyString())
                    + "&param_key-declaration=" + Utils.urlEncode(shortKey)
                    + "&param_key-declaration-ref=" + Utils.urlEncode(shortKey)
                    + "&param_key-location=" + Utils.urlEncode(prefs.getWebsiteUrl())
                    + "&context=" + Utils.urlEncode(userIriString)
                    + "&postpub-redirect-url=" + Utils.urlEncode(aboutUrl)
                    + "&link-message=" + Utils.urlEncode("Check the checkbox at the end of this page and press 'Publish' to "
                            + "publish this introduction linking your ORCID identifier to the local key used on this site.");
        }
        ExternalLink createIntro = new ExternalLink("createIntro", createIntroUrl, "Create Introduction");
        createIntro.setVisible(canCreateIntro);
        add(createIntro);

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

        // Personal API tokens for remote MCP access, when that is enabled.
        ApiTokenPanel apiTokens = new ApiTokenPanel("apitokens", userIriString);
        apiTokens.setVisible(prefs.isMcpRemoteEnabled());
        add(apiTokens);

        // Recommended actions — hard-coded here (formerly the 👉 Recommended-actions
        // view/query). One bullet per applicable case, computed from the session's
        // introduction/approval state. The "create" case is the Create Introduction
        // button above, so it has no bullet of its own.
        // All recommendations concern the site's local key, so only apply when there is one.
        List<String> recs = new ArrayList<>();
        if (session.getKeyPair() != null) {
            int localCount = session.getLocalIntroCount();
            boolean approved = session.isPubkeyApproved();
            // create: no introduction with the local key yet (the action is the Create
            // Introduction button above, but it gets a bullet too for context).
            if (localCount == 0) {
                recs.add("The local key from this site is not part of an introduction yet. Use the "
                        + "<em>'Create Introduction'</em> button above to link it to your identity.");
            }
            // get-approval: exactly one local introduction, not approved yet
            if (localCount == 1 && !approved) {
                String t = "Your introduction with the local key is not approved yet. Share it so an already "
                        + "approved user can approve it";
                IntroNanopub localIntro = session.getLocalIntro();
                if (localIntro != null) {
                    String introUri = localIntro.getNanopub().getUri().stringValue();
                    t += ": <a href=\"" + ExplorePage.MOUNT_PATH + "?id=" + Utils.urlEncode(introUri) + "\">"
                            + Strings.escapeMarkup(introUri) + "</a>.";
                } else {
                    t += ".";
                }
                recs.add(t);
            }
            // derive: no local introduction, but the user has introductions elsewhere.
            // This always co-occurs with the create bullet above (same localCount==0),
            // so it is phrased as the alternative to creating a fresh introduction.
            if (localCount == 0 && !session.getUserIntroNanopubs().isEmpty()) {
                recs.add("As you have introductions elsewhere, you can alternatively use "
                        + "<em>'derive new introduction'</em> from the row menu in the Introductions table below to "
                        + "declare those keys alongside the local key.");
            }
            // retract: more than one local introduction
            if (localCount > 1) {
                recs.add("You have multiple introductions from this site. Use <em>'retract'</em> from the row menu in "
                        + "the Introductions table below to remove the redundant ones.");
            }
            // update-approved: local key not approved, but the user has another approved key
            String localHash = session.getPubkeyhash();
            boolean hasAnotherApprovedKey = localHash != null && session.getUserIri() != null
                    && User.getPubkeyhashes(session.getUserIri(), true).stream().anyMatch(h -> !h.equals(localHash));
            if (localCount > 0 && !approved && hasAnotherApprovedKey) {
                recs.add("Your local key is not approved, but you have an approved introduction elsewhere. Add this "
                        + "site's local key to that approved introduction, at the site where you created it.");
            }
        }
        WebMarkupContainer recommendations = new WebMarkupContainer("recommendations");
        recommendations.setVisible(!recs.isEmpty());
        recommendations.add(new ListView<String>("recItems", recs) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label("recText", item.getModelObject()).setEscapeModelStrings(false));
            }
        });
        add(recommendations);
    }

}
