package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.component.ClassesPanel;
import com.knowledgepixels.nanodash.component.InstancesPanel;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.TitleBar;

public class ThingListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/thinglist";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ThingListPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));

		final String ref = parameters.get("ref").toString();
		final String shortName = IriItem.getShortNameFromURI(ref);
		final String mode = parameters.get("mode").toString();

		add(new Label("pagetitle", shortName + " (instances) | nanodash"));
		add(new Label("heading", shortName));

		add(new ExternalLink("urilink", ref, ref));

		if (mode.equals("instances")) {
			add(InstancesPanel.createComponent("list", ref, null, 0));
		} else if (mode.equals("classes")) {
			add(ClassesPanel.createComponent("list", ref, null, 0));
		}
	}

}
