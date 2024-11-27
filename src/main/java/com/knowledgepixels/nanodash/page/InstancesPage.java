package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.component.InstancesPanel;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.TitleBar;

public class InstancesPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/instances";

	private final String classRef;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public InstancesPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));

		classRef = parameters.get("class").toString();
		final String shortName = IriItem.getShortNameFromURI(classRef);

		add(new Label("pagetitle", shortName + " (instances) | nanodash"));
		add(new Label("heading", shortName));

		add(InstancesPanel.createComponent("list", classRef, null, 0));
	}

}
