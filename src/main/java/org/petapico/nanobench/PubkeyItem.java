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
			add(new Label("fullkey", "(key does not exist)"));
		} else {
			add(new Label("label", Utils.getShortPubkeyLabel(pubkey)));
			String notes = "";
			if (pubkey.equals(session.getPubkeyString())) {
				notes += "<strong>Local key:</strong> This is the key that is used to sign your nanopublications on this site.<br>";
			}
			if (User.getPubkeys(session.getUserIri(), true).contains(pubkey)) {
				notes += "<strong class=\"positive\">Approved key:</strong> This key is approved by the community.<br>";
			} else {
				notes += "<strong class=\"negative\">Unapproved key:</strong> This key is so far not approved by the community.<br>";
			}
			add(new Label("notes", notes).setEscapeModelStrings(false));
			add(new Label("fullkey", pubkey));
		}
	}

}
