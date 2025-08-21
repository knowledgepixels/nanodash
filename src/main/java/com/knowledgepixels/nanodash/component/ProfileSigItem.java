package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class ProfileSigItem extends Panel {

    private static final long serialVersionUID = 1L;

    public ProfileSigItem(String id) {
        super(id);
        boolean loginMode = NanodashPreferences.get().isOrcidLoginMode();

        final NanodashSession session = NanodashSession.get();

        WebMarkupContainer localFilePanel = new WebMarkupContainer("localfile");
        if (loginMode) {
            localFilePanel.add(new Label("keyfile", ""));
            localFilePanel.setVisible(false);
        } else {
            localFilePanel.add(new Label("keyfile", session.getKeyFile().getPath()));
        }
        add(localFilePanel);
        if (session.getKeyFile().exists()) {
            if (session.getKeyPair() == null) {
                add(new Label("pubkey", "Error loading key file"));
            } else {
                add(new PubkeyItem("pubkey", session.getPubkeyString()));
            }
        } else {
            add(new Label("pubkey", ""));
        }
    }

}
