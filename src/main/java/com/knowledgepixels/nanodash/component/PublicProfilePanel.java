package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.KeyDeclaration;
import org.nanopub.extra.setting.IntroNanopub;

import java.util.Calendar;
import java.util.List;

/**
 * A read-only, public-facing view of a user's profile information: their
 * introduction nanopublications, declared public keys, and default license.
 * <p>
 * Unlike the session-bound profile items used on the editable
 * {@code ProfilePage} (e.g. {@code ProfileIntroItem}, {@code ProfileSigItem}),
 * this panel takes an arbitrary user IRI and shows no publish/edit actions, so
 * it can be rendered for any user.
 */
public class PublicProfilePanel extends Panel {

    /**
     * Constructs a PublicProfilePanel for the given user.
     *
     * @param id      the Wicket component id
     * @param userIri the IRI of the user whose profile is shown
     */
    public PublicProfilePanel(String id, IRI userIri) {
        super(id);

        // Introductions
        List<IntroNanopub> intros = User.getIntroNanopubs(userIri);
        add(new Label("intro-note", intros.isEmpty()
                ? "<em>There are no introductions yet.</em>" : "").setEscapeModelStrings(false));
        add(new DataView<IntroNanopub>("intro-nps", new ListDataProvider<>(intros)) {
            @Override
            protected void populateItem(Item<IntroNanopub> item) {
                final IntroNanopub inp = item.getModelObject();
                String uri = inp.getNanopub().getUri().stringValue();
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("intro-uri", ExplorePage.class, new PageParameters().set("id", uri));
                link.add(new Label("intro-uri-label", TrustyUriUtils.getArtifactCode(uri).substring(0, 10)));
                item.add(link);
                if (User.isApproved(inp)) {
                    item.add(new Label("intro-note", " <strong class=\"positive\">(approved)</strong>").setEscapeModelStrings(false));
                } else {
                    item.add(new Label("intro-note", ""));
                }
                IRI location = Utils.getLocation(inp);
                if (location == null) {
                    item.add(new Label("location", "unknown site"));
                } else {
                    item.add(new Label("location", "<a href=\"" + location + "\">" + location + "</a>").setEscapeModelStrings(false));
                }
                Calendar creationDate = SimpleTimestampPattern.getCreationTime(inp.getNanopub());
                item.add(new Label("date", creationDate == null ? "unknown date" : NanopubItem.simpleDateFormat.format(creationDate.getTime())));

                item.add(new DataView<KeyDeclaration>("intro-keys", new ListDataProvider<>(inp.getKeyDeclarations())) {
                    @Override
                    protected void populateItem(Item<KeyDeclaration> kdi) {
                        kdi.add(new Label("intro-key", Utils.getShortPubkeyName(Utils.createSha256HexHash(kdi.getModelObject().getPublicKeyString()))));
                    }
                });
            }
        });

        // Public keys (with per-user approval status)
        List<String> pubkeyhashes = User.getPubkeyhashes(userIri, null);
        add(new Label("keys-note", pubkeyhashes.isEmpty()
                ? "<em>There are no public keys yet.</em>" : "").setEscapeModelStrings(false));
        add(new DataView<String>("keys", new ListDataProvider<>(pubkeyhashes)) {
            @Override
            protected void populateItem(Item<String> item) {
                String hash = item.getModelObject();
                item.add(new Label("key-label", Utils.getShortPubkeyName(hash)));
                if (User.isApprovedPubkeyhashForUser(hash, userIri)) {
                    item.add(new Label("key-note", " <strong class=\"positive\">(approved)</strong>").setEscapeModelStrings(false));
                } else {
                    item.add(new Label("key-note", " <strong class=\"negative\">(not approved)</strong>").setEscapeModelStrings(false));
                }
            }
        });

        // Default license
        IRI license = User.getDefaultLicense(userIri);
        WebMarkupContainer licenseContainer = new WebMarkupContainer("license-container");
        if (license == null) {
            licenseContainer.setVisible(false);
            licenseContainer.add(new ExternalLink("license", "."));
        } else {
            licenseContainer.add(new ExternalLink("license", license.stringValue(), license.stringValue()));
        }
        add(licenseContainer);
    }

}
