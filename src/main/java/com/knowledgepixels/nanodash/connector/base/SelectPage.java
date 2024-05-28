package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.TitleBar;


public abstract class SelectPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	private Form<?> form;
	private RadioGroup<String> radioGroup;

	public SelectPage(PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef("Create Nanopublication")
			));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));


		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				ConnectorNanopubType type = ConnectorNanopubType.get(radioGroup.getModelObject());
				PageParameters params = new PageParameters();
				params.add("type", type.getId());
				params.add("template", type.getTemplate().getId());
				params.add("prtemplate", type.getPrTemplateId());
				System.err.println(getConfig().getPublishPage().getMountPath());
				throw new RestartResponseException(getConfig().getPublishPage().getClass(), params);
			}

		};
		form.setOutputMarkupId(true);

		radioGroup = new RadioGroup<>("radio-group", new Model<String>(getOptions()[0]));
		radioGroup.setOutputMarkupId(true);

		form.add(radioGroup);
		for (String option : getOptions()) {
			Radio<String> radio = new Radio<String>(option + "-select", new Model<String>(option), radioGroup);
			radio.setOutputMarkupId(true);
			radioGroup.add(radio);
		}
		add(form);
	}

	protected abstract String[] getOptions();

}
