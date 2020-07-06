package org.petapico.nanobench;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

public class LiteralTextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public LiteralTextfieldItem(String id, IRI iri, boolean optional, PublishFormContext context) {
		super(id);
		IModel<String> model = context.getFormComponentModels().get(iri);
		if (model == null) {
			String value = "";
			String postfix = iri.stringValue().replaceFirst("^.*[/#](.*)$", "$1");
			if (context.hasParam(postfix)) {
				value = context.getParam(postfix);
			}
			model = Model.of(value);
			context.getFormComponentModels().put(iri, model);
		}
		TextField<String> textfield = new TextField<>("textfield", model);
		if (!optional) textfield.setRequired(true);
		if (context.getTemplate().getLabel(iri) != null) {
			textfield.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
		}
		context.getFormComponentModels().put(iri, textfield.getModel());
		context.getFormComponents().add(textfield);
		add(textfield);
	}

}
