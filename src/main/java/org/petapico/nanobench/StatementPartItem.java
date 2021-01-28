package org.petapico.nanobench;

import org.apache.wicket.markup.html.panel.Panel;

public class StatementPartItem extends Panel {

	private static final long serialVersionUID = 1L;

	private final ValueItem subjItem, predItem, objItem;

	public StatementPartItem(String id, ValueItem subjItem, ValueItem predItem, ValueItem objItem) {
		super(id);
		this.subjItem = subjItem;
		this.predItem = predItem;
		this.objItem = objItem;
		add(subjItem);
		add(predItem);
		add(objItem);
	}

	public ValueItem getSubject() {
		return subjItem;
	}

	public ValueItem getPredicate() {
		return predItem;
	}

	public ValueItem getObject() {
		return objItem;
	}

}
