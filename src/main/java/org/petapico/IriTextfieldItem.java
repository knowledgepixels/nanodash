package org.petapico;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

public class IriTextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public IriTextfieldItem(String id, IRI iri, final PublishPage page) {
		super(id);
		IModel<String> model = page.textFieldModels.get(iri);
		if (model == null) {
			model = Model.of("");
			page.textFieldModels.put(iri, model);
		}
		final TextField<String> textfield = new TextField<>("textfield", model);
		page.textFields.add(textfield);
		textfield.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (TextField<String> t : page.textFields) {
					if (t == textfield) continue;
					if (t.getModel() == textfield.getModel()) {
						t.modelChanged();
						target.add(t);
					}
				}
			}

		});
		add(textfield);
	}

}
