package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class PubkeyItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public PubkeyItem(String id, String pubkey) {
		super(id);

		NanobenchSession session = NanobenchSession.get();
		if (pubkey == null || pubkey.isEmpty()) {
			add(new Label("label", ".."));
			add(new Label("notes", ""));
			//add(new Label("fullkey", "(key does not exist)"));
		} else {
			add(new Label("label", Utils.getShortPubkeyName(pubkey)));
			String notes = "";
			if (pubkey.equals(session.getPubkeyString())) {
				notes += "This is the <strong>local key</strong> that is used to sign your nanopublications on this site. ";
			} else {
				notes += "This key is used on another site. ";
			}
			if (User.getPubkeys(session.getUserIri(), true).contains(pubkey)) {
				notes += "It is <strong class=\"positive\">approved</strong> by the community.";
			} else {
				notes += "It is so far <strong class=\"negative\">not approved</strong> by the community.";
			}
			add(new Label("notes", notes).setEscapeModelStrings(false));
			//add(new Label("fullkey", pubkey));
		}
	}

}
