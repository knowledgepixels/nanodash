package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.action.NanopubAction;
import com.knowledgepixels.nanodash.page.PublishPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;

import java.net.URLEncoder;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ActionMenu extends Panel {

    private static final long serialVersionUID = 1L;

    public ActionMenu(String id, final List<NanopubAction> menuItems, final NanopubElement n) {
        super(id);

        final NanodashSession session = NanodashSession.get();
        final UserData userData = User.getUserData();
        final IRI userIri = session.getUserIri();

        add(new DataView<NanopubAction>("menulist", new ListDataProvider<NanopubAction>(menuItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<NanopubAction> item) {
                NanopubAction action = item.getModel().getObject();
                String location = "";
                String extraLabel = "";
                final String pubkey = n.getPubkey();
                final String pubkeyhash = n.getPubkeyhash();
                String sigkeyParam = "";
                if (action.isApplicableToOwnNanopubs() && !action.isApplicableToOthersNanopubs()) {
                    if (userIri != null && pubkey != null && !session.getPubkeyString().equals(pubkey)) {
                        IRI keyLocation = userData.getKeyLocationForPubkeyhash(pubkeyhash);
                        if (keyLocation == null) {
                            location = "http://localhost:37373";
                            extraLabel = " at localhost";
                        } else {
                            location = keyLocation.stringValue().replaceFirst("/$", "");
                            extraLabel = " at " + Utils.getPubkeyLocationName(pubkeyhash, "localhost");
                        }
                        sigkeyParam = "&sigkey=" + URLEncoder.encode(pubkey, UTF_8);
                    }
                }
                String url = location + PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(action.getTemplateUri(n.getNanopub())) +
                             "&" + action.getParamString(n.getNanopub()) +
                             "&template-version=latest" + sigkeyParam;
                item.add(new ExternalLink("menuitem", url, action.getLinkLabel(n.getNanopub()) + extraLabel));
            }

        });

        setVisible(!menuItems.isEmpty());
    }

}
