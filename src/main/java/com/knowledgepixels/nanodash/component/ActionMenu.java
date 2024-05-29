package com.knowledgepixels.nanodash.component;

import java.net.URLEncoder;
import java.util.List;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;

import com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.UserData;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.action.NanopubAction;
import com.knowledgepixels.nanodash.page.PublishPage;

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
				String sigkeyParam = "";
				if (action.isApplicableToOwnNanopubs() && !action.isApplicableToOthersNanopubs()) {
					if (userIri != null && pubkey != null && !session.getPubkeyString().equals(pubkey)) {
						IRI keyLocation = userData.getKeyLocation(pubkey);
						if (keyLocation == null) {
							location = "http://localhost:37373";
							extraLabel = " at localhost";
						} else {
							location = keyLocation.stringValue().replaceFirst("/$", "");
							extraLabel = " at " + Utils.getPubkeyLocationName(pubkey, "localhost");
						}
						sigkeyParam = "&sigkey=" + URLEncoder.encode(pubkey, Charsets.UTF_8);
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
