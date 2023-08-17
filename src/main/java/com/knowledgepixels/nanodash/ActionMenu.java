package com.knowledgepixels.nanodash;

import java.util.List;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.action.NanopubAction;

public class ActionMenu extends Panel {

	private static final long serialVersionUID = 1L;

	public ActionMenu(String id, final List<NanopubAction> menuItems, final Nanopub np) {
		super(id);
	
		add(new DataView<NanopubAction>("menulist", new ListDataProvider<NanopubAction>(menuItems)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubAction> item) {
				NanopubAction action = item.getModel().getObject();
				String url = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(action.getTemplateUri(np)) +
						"&" + action.getParamString(np) +
						"&template-version=latest";
				item.add(new ExternalLink("menuitem", url, action.getLinkLabel(np)));
			}

		});
	}

}
