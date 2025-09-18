package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * A component that displays a public key item with its label and notes.
 * If the public key is null or empty, it shows a placeholder label.
 * If the public key exists, it shows a short name and notes about its approval status.
 */
public class PubkeyItem extends Panel {

    /**
     * Constructor for PubkeyItem.
     *
     * @param id     the Wicket component ID
     * @param pubkey the public key string, can be null or empty
     */
    public PubkeyItem(String id, String pubkey) {
        super(id);

        NanodashSession session = NanodashSession.get();
        if (pubkey == null || pubkey.isEmpty()) {
            add(new Label("label", ".."));
            add(new Label("notes", ""));
            //add(new Label("fullkey", "(key does not exist)"));
        } else {
            add(new Label("label", Utils.getShortPubkeyName(pubkey)));
            String notes = "";
            if (session.isPubkeyApproved()) {
                notes += "It is <strong class=\"positive\">approved</strong> by the community.";
            } else {
                notes += "It is so far <strong class=\"negative\">not approved</strong> by the community.";
            }
            add(new Label("notes", notes).setEscapeModelStrings(false));
            //add(new Label("fullkey", pubkey));
        }
    }

}
