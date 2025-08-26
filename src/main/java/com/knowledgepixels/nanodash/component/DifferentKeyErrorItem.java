package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Panel to display an error message when the public key in the URL does not match the local session's public key.
 */
public class DifferentKeyErrorItem extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for DifferentKeyErrorItem.
     *
     * @param id         the component id
     * @param parameters the page parameters containing the public key to compare against the session's public key
     */
    public DifferentKeyErrorItem(String id, final PageParameters parameters) {
        super(id);
        final NanodashSession session = NanodashSession.get();
        add(new Label("linkkey", Utils.getShortPubkeyName(Utils.createSha256HexHash(parameters.get("sigkey").toString()))));
        add(new Label("localkey", Utils.getShortPubkeyName(session.getPubkeyhash())));
    }

}
