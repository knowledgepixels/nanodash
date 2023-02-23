package com.knowledgepixels.nanodash;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;

public class NanopubResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubResults(String id, List<NanopubElement> nanopubs) {
		super(id);


		add(new AjaxCheckBox("showp", Model.of(NanodashSession.get().isShowProvenanceEnabled())) {

			private static final long serialVersionUID = -6951066705477126322L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				NanodashSession.get().setShowProvenanceEnabled(getModelObject());
				setResponsePage(target.getPage());
			}

		});
		add(new AjaxCheckBox("showi", Model.of(NanodashSession.get().isShowPubinfoEnabled())) {

			private static final long serialVersionUID = 5451216630648827493L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				NanodashSession.get().setShowPubinfoEnabled(getModelObject());
				setResponsePage(target.getPage());
			}

		});

		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				NanodashSession session = NanodashSession.get();
				item.add(new NanopubItem("nanopub", item.getModelObject(), !session.isShowProvenanceEnabled(), !session.isShowPubinfoEnabled()));
			}

		});
	}

}
