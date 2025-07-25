package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.codec.Charsets;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.setting.IntroNanopub;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.knowledgepixels.nanodash.Utils.urlEncode;

/**
 * A panel that shows the user's introduction nanopubs.
 */
public class ProfileIntroItem extends Panel {

    private static final long serialVersionUID = 1L;

    private NanodashSession session = NanodashSession.get();
    private NanodashPreferences prefs = NanodashPreferences.get();
    private int recommendedActionsCount = 0;
    private Map<IntroNanopub, String> includeKeysParamMap = new HashMap<>();
    private int approvedIntrosCount = 0;

    /**
     * Constructor.
     *
     * @param id the component id
     */
    public ProfileIntroItem(String id) {
        super(id);

        String publishIntroLinkString = PublishPage.MOUNT_PATH +
                "?template=http://purl.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA&" +
                "param_user=" + urlEncode(session.getUserIri()) + "&" +
//				"param_name=" + urlEncode(session.getOrcidName()) + "&" +
                "param_public-key=" + urlEncode(session.getPubkeyString()) + "&" +
                "param_key-declaration=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
                "param_key-declaration-ref=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
                "param_key-location=" + urlEncode(prefs.getWebsiteUrl()) + "&" +
                "link-message=" + urlEncode("Check the checkbox at the end of this page and press 'Publish' to publish this " +
                "introduction linking your ORCID identifier to the local key used on this site.");

        // Do this here, so we know whether to show the recommended action before rendering stage:
        if (session.getLocalIntro() != null) {
            for (IntroNanopub inp : session.getUserIntroNanopubs()) {
                if (User.isApproved(inp)) approvedIntrosCount++;
                String params = "";
                int paramIndex = 0;
                for (KeyDeclaration kd : inp.getKeyDeclarations()) {
                    if (!hasKey(session.getLocalIntro(), kd)) {
                        paramIndex++;
                        params += "&" +
                                "param_public-key__." + paramIndex + "=" + urlEncode(kd.getPublicKeyString()) + "&" +
                                "param_key-declaration__." + paramIndex + "=" + urlEncode(Utils.getShortPubkeyName(kd.getPublicKeyString())) + "&" +
                                "param_key-declaration-ref__." + paramIndex + "=" + urlEncode(Utils.getShortPubkeyName(kd.getPublicKeyString())) + "&" +
                                "param_key-location__." + paramIndex + "=" + urlEncode(kd.getKeyLocation() == null ? "" : kd.getKeyLocation());
                    }
                }
                if (paramIndex > 0) {
                    includeKeysParamMap.put(inp, params);
                }
            }
        }

        WebMarkupContainer publishIntroItem = new WebMarkupContainer("publish-intro-item");
        publishIntroItem.add(new ExternalLink("publish-intro-link", publishIntroLinkString, "new introduction..."));
        add(publishIntroItem);
        publishIntroItem.setVisible(session.getLocalIntroCount() == 0);
        if (publishIntroItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer includeKeysItem = new WebMarkupContainer("include-keys-item");
        add(includeKeysItem);
        includeKeysItem.setVisible(!includeKeysParamMap.isEmpty());
        if (includeKeysItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer retractIntroItem = new WebMarkupContainer("retract-intro-item");
        add(retractIntroItem);
        retractIntroItem.setVisible(session.getLocalIntroCount() > 1);
        if (retractIntroItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer deriveIntroItem = new WebMarkupContainer("derive-intro-item");
        add(deriveIntroItem);
        deriveIntroItem.setVisible(session.getLocalIntroCount() == 0 && !session.getUserIntroNanopubs().isEmpty());
        if (deriveIntroItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer updateApprovedItem = new WebMarkupContainer("update-approved-item");
        add(updateApprovedItem);
        updateApprovedItem.setVisible(!session.isPubkeyApproved() && approvedIntrosCount > 0 && session.getLocalIntroCount() > 0);
        if (updateApprovedItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer getApprovalItem = new WebMarkupContainer("get-approval-item");
        String introUrl = (session.getLocalIntro() == null ? "" : session.getLocalIntro().getNanopub().getUri().stringValue());
        getApprovalItem.add(new ExternalLink("introduction-to-be-approved", introUrl, introUrl));
        add(getApprovalItem);
        getApprovalItem.setVisible(!session.isPubkeyApproved() && session.getLocalIntroCount() == 1);
        if (getApprovalItem.isVisible()) recommendedActionsCount++;

        WebMarkupContainer orcidLinkingItem = new WebMarkupContainer("orcid-linking-item");
        add(orcidLinkingItem);
        orcidLinkingItem.setVisible(false);
//		orcidLinkingItem.setVisible(!session.isOrcidLinked() && session.getLocalIntroCount() == 1);
        if (orcidLinkingItem.isVisible()) recommendedActionsCount++;

        if (session.getUserIntroNanopubs().isEmpty()) {
            add(new Label("intro-note", "<em>There are no introductions yet.</em>").setEscapeModelStrings(false));
        } else if (session.getLocalIntroCount() == 0) {
            // TODO: Check whether it's part of an introduction for a different ORCID, and show a warning if so
            add(new Label("intro-note", "The local key from this site is <strong class=\"negative\">not part of an introduction</strong> yet.").setEscapeModelStrings(false));
        } else if (session.getLocalIntroCount() == 1) {
            add(new Label("intro-note", ""));
        } else {
            add(new Label("intro-note", "You have <strong class=\"negative\">multiple introductions from this site</strong>.").setEscapeModelStrings(false));
        }
        if (recommendedActionsCount == 0) {
            add(new Label("action-note", "<em>There are no recommended actions.</em>").setEscapeModelStrings(false));
        } else if (recommendedActionsCount == 1) {
            add(new Label("action-note", "It is recommended that you <strong>execute this action</strong>:").setEscapeModelStrings(false));
        } else {
            add(new Label("action-note", "It is recommended that you <strong>execute one of these actions</strong>:").setEscapeModelStrings(false));
        }

        add(new DataView<IntroNanopub>("intro-nps", new ListDataProvider<IntroNanopub>(session.getUserIntroNanopubs())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<IntroNanopub> item) {
                final IntroNanopub inp = item.getModelObject();
                IRI location = Utils.getLocation(inp);
                String uri = inp.getNanopub().getUri().stringValue();
                ExternalLink link = new ExternalLink("intro-uri", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8));
                link.add(new Label("intro-uri-label", TrustyUriUtils.getArtifactCode(uri).substring(0, 10)));
                item.add(link);
                if (User.isApproved(inp)) {
                    item.add(new Label("intro-note", " <strong class=\"positive\">(approved)</strong>").setEscapeModelStrings(false));
                } else {
                    item.add(new Label("intro-note", ""));
                }
                if (session.isIntroWithLocalKey(inp)) {
                    item.add(new Label("location", "<strong>this site</strong>").setEscapeModelStrings(false));
                } else if (location == null) {
                    item.add(new Label("location", "unknown site"));
                } else {
                    item.add(new Label("location", "<a href=\"" + location + "\">" + location + "</a>").setEscapeModelStrings(false));
                }
                Calendar creationDate = SimpleTimestampPattern.getCreationTime(inp.getNanopub());
                item.add(new Label("date", (creationDate == null ? "unknown date" : NanopubItem.simpleDateFormat.format(creationDate.getTime()))));

                ExternalLink retractLink = new ExternalLink("retract-link", PublishPage.MOUNT_PATH + "?" +
                        "template=http://purl.org/np/RA0QOsYNphQCityVcDIJEuldhhuJOX3GlBLw6QylRBhEI&" +
                        "param_nanopubToBeRetracted=" + urlEncode(inp.getNanopub().getUri()) + "&" +
                        "link-message=" + urlEncode("Check the checkbox at the end of this page and press 'Publish' to retract the " +
                        "given introduction."),
                        "retract...");
                item.add(retractLink);
                retractLink.setVisible(session.getLocalIntroCount() > 1 && session.isIntroWithLocalKey(inp));

                ExternalLink deriveLink = new ExternalLink("derive-link", PublishPage.MOUNT_PATH + "?" +
                        "template=http://purl.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA&" +
                        "derive-a=" + urlEncode(inp.getNanopub().getUri()) + "&" +
                        "param_public-key__.1=" + urlEncode(session.getPubkeyString()) + "&" +
                        "param_key-declaration__.1=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
                        "param_key-declaration-ref__.1=" + urlEncode(Utils.getShortPubkeyName(session.getPubkeyString())) + "&" +
                        "param_key-location__.1=" + urlEncode(prefs.getWebsiteUrl()) + "&" +
                        "link-message=" + urlEncode("Enter you name below, check the checkbox at the end of the page, and press 'Publish' to publish this " +
                        "introduction linking your ORCID identifier to the given keys."),
                        "derive new introduction...");
                item.add(deriveLink);
                deriveLink.setVisible(!session.isIntroWithLocalKey(inp) && session.getLocalIntroCount() == 0);

                if (includeKeysParamMap.containsKey(inp)) {
                    item.add(new ExternalLink("include-keys-link", PublishPage.MOUNT_PATH + "?" +
                            "template=http://purl.org/np/RAT8ayO62s4SFqDY1qjv24Iw0xarpbpc6zH68n7hRsAsA&" +
                            "supersede=" + urlEncode(session.getLocalIntro().getNanopub().getUri()) +
                            includeKeysParamMap.get(inp) + "&" +
                            "link-message=" + urlEncode("Check the checkbox at the end of this page and press 'Publish' to publish this " +
                            "introduction that includes the additional keys."),
                            "include keys..."));
                } else {
                    item.add(new ExternalLink("include-keys-link", ".", "").setVisible(false));
                }

                item.add(new DataView<KeyDeclaration>("intro-keys", new ListDataProvider<KeyDeclaration>(inp.getKeyDeclarations())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<KeyDeclaration> kdi) {
                        kdi.add(new Label("intro-key", Utils.getShortPubkeyName(kdi.getModelObject().getPublicKeyString())));
                    }

                });
            }

        });

    }

    private boolean hasKey(IntroNanopub inp, KeyDeclaration kd) {
        // TODO: Do this more efficiently:
        for (KeyDeclaration k : inp.getKeyDeclarations()) {
            if (k.getPublicKeyString().equals(kd.getPublicKeyString())) return true;
        }
        return false;
    }

}
