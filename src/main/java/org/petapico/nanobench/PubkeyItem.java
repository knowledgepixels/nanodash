package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class PubkeyItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public PubkeyItem(String id, String pubkey) {
		super(id);

		if (pubkey == null || pubkey.isEmpty()) {
			add(new Label("label", ".."));
			add(new Label("fullkey", "(key does not exist)"));
		} else {
			add(new Label("label", Utils.getShortPubkeyLabel(pubkey)));
			add(new Label("fullkey", pubkey));
		}
	}

}
