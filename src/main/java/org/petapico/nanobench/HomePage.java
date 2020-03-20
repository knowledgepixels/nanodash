package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class HomePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		if (ProfilePage.isComplete()) {
			add(new Label("text", "Click on the menu items above to explore or publish nanopublications."));
		} else {
			add(new Label("text", "Before you can start, you first need to <a href=\"./profile\">complete your profile</a>.").setEscapeModelStrings(false));
		}
	}

}
