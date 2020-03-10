package org.petapico.nanobench;

import org.apache.wicket.markup.html.panel.Panel;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id) {
		super(id);
		add(new ProfileItem("profile"));
	}

}
