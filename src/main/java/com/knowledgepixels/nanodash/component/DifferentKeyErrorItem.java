package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class DifferentKeyErrorItem extends Panel {

    private static final long serialVersionUID = 1L;

    public DifferentKeyErrorItem(String id, final PageParameters parameters) {
        super(id);
        final NanodashSession session = NanodashSession.get();
        add(new Label("linkkey", Utils.getShortPubkeyName(parameters.get("sigkey").toString())));
        add(new Label("localkey", Utils.getShortPubkeyName(session.getPubkeyString())));
    }

}
