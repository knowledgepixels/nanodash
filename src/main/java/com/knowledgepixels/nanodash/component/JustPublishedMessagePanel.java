package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NPX;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.ProfilePage;
import com.knowledgepixels.nanodash.page.UserPage;

/**
 * Title-bar message panel rendering up to three stacked boxes:
 * <ul>
 *   <li>a blue "successfully published" confirmation box, shown only right after
 *       publishing (i.e. when the page carries a "just-published" parameter) and
 *       dismissable via a close button;</li>
 *   <li>a red "you haven't published an introduction yet" warning box, shown on
 *       <em>every</em> page (no close button) whenever a logged-in user with a
 *       local key has not yet published an introduction linking that key to their
 *       ORCID. It disappears only once such an introduction is published; and</li>
 *   <li>an amber "your account is pending approval" notice box, shown on
 *       <em>every</em> page whenever the user has an introduction but no approved
 *       key yet (issue #625): their self-signed contributions are marked as
 *       pending (⏳) until an approved user approves their introduction.</li>
 * </ul>
 */
public class JustPublishedMessagePanel extends Panel {

    public JustPublishedMessagePanel(String id, PageParameters parameters) {
        super(id);
        // Placeholder tag so the centered title-bar slot collapses cleanly when
        // there is nothing to show.
        setOutputMarkupPlaceholderTag(true);

        NanodashSession session = NanodashSession.get();
        String justPublishedId = parameters.get("just-published").toString("");
        boolean justPublished = !justPublishedId.isEmpty();

        // --- Confirmation box: only right after publishing; dismissable. ---
        final WebMarkupContainer confirmBox = new WebMarkupContainer("confirmBox");
        confirmBox.setOutputMarkupPlaceholderTag(true);
        confirmBox.setVisible(justPublished);
        add(confirmBox);
        if (justPublished) {
            confirmBox.add(new AjaxLink<Void>("confirmClose") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    confirmBox.setVisible(false);
                    target.add(confirmBox);
                }
            }.setBody(Model.of("×")));
            Nanopub np = Utils.getAsNanopub(justPublishedId);
            String label = (np != null ? NanopubUtils.getLabel(np) : null);
            if (label == null || label.isEmpty()) label = Utils.getShortNameFromURI(justPublishedId);
            confirmBox.add(new BookmarkablePageLink<Void>("link", ExplorePage.class, new PageParameters().set("id", justPublishedId))
                    .setBody(Model.of(Utils.truncateLinkLabel(label)))
                    .add(NavigationContext.pageContextFallback()));
            // Remember a freshly-published introduction so the warning below clears.
            if (np != null && Utils.usesPredicateInAssertion(np, NPX.DECLARED_BY)) session.setIntroPublishedNow();
        } else {
            confirmBox.add(new WebMarkupContainer("confirmClose").setVisible(false));
            confirmBox.add(new WebMarkupContainer("link").setVisible(false));
        }

        // --- Missing-introduction warning: shown on every page (no close button).
        // The user can only get rid of it by publishing an introduction. Gated on
        // having a local key + ORCID, so users who cannot yet publish one (no
        // profile set up) are not nagged. A just-published introduction takes a
        // moment to register, so it is suppressed for 5 minutes after publishing. ---
        boolean canPublishIntro = session.getUserIri() != null && session.getKeyPair() != null;
        boolean hasLocalIntro = session.getLocalIntroCount() > 0;
        boolean recentlyPublishedIntro = session.getTimeSinceLastIntroPublished() <= 5 * 60 * 1000;
        if (canPublishIntro && !hasLocalIntro && session.hasIntroPublished()) User.refreshUsers();
        boolean showIntroWarning = canPublishIntro && !hasLocalIntro && !recentlyPublishedIntro;
        WebMarkupContainer introBox = new WebMarkupContainer("introBox");
        introBox.setVisible(showIntroWarning);
        String introProfileUrl = session.getUserIri() != null
                ? UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(session.getUserIri()) + "&tab=about"
                : ProfilePage.MOUNT_PATH + "?message=publish-intro";
        introBox.add(new ExternalLink("profile-link", introProfileUrl));
        add(introBox);

        // --- Pending-approval notice: the user has published an introduction, but no
        // approved user has approved it yet, and they hold no approved key elsewhere
        // either (issue #625). Until approval, their self-signed contributions count
        // as pending: they show up marked ⏳ in role listings (e.g. the observers
        // table) and on their own page, but stay hidden where approval is required.
        // Shown on every page, like the intro warning; it disappears once the
        // introduction is approved (via the periodic user-data refresh). Users with
        // an approved key elsewhere are not in this pending state — their local-key
        // situation is covered by the About-page recommendations instead. ---
        boolean anyApprovedKey = session.getUserIri() != null
                && !User.getPubkeyhashes(session.getUserIri(), true).isEmpty();
        boolean showPendingNotice = canPublishIntro && hasLocalIntro && !anyApprovedKey;
        WebMarkupContainer pendingBox = new WebMarkupContainer("pendingBox");
        pendingBox.setVisible(showPendingNotice);
        pendingBox.add(new ExternalLink("about-link", introProfileUrl));
        add(pendingBox);

        setVisible(justPublished || showIntroWarning || showPendingNotice);
    }
}
