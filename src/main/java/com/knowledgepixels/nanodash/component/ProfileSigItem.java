package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Shows the user's local signing key as a single line: its short name and, in
 * local mode, the path of the key file ("Local key (...) is in file: ..."). The
 * approval status is no longer shown here — it is surfaced per key in the
 * introductions table.
 */
public class ProfileSigItem extends Panel {

    /**
     * Constructs a ProfileSigItem panel.
     *
     * @param id the Wicket component ID
     */
    public ProfileSigItem(String id) {
        super(id);
        boolean loginMode = NanodashPreferences.get().isOrcidLoginMode();

        final NanodashSession session = NanodashSession.get();
        boolean keyFileExists = session.getKeyFile().exists();
        boolean keyLoaded = keyFileExists && session.getKeyPair() != null;

        WebMarkupContainer keyLine = new WebMarkupContainer("keyline");
        keyLine.add(new Label("keylabel", keyLoaded ? Utils.getShortPubkeyName(session.getPubkeyhash()) : ".."));
        WebMarkupContainer filePart = new WebMarkupContainer("filepart");
        // The key file path is only meaningful in local mode (no ORCID login).
        filePart.add(new Label("keyfile", keyLoaded ? session.getKeyFile().getPath() : ""));
        filePart.setVisible(keyLoaded && !loginMode);
        keyLine.add(filePart);
        keyLine.setVisible(keyLoaded);
        add(keyLine);

        // Only relevant when the key file is present but could not be loaded.
        Label keyError = new Label("keyerror", "Error loading key file");
        keyError.setVisible(keyFileExists && session.getKeyPair() == null);
        add(keyError);
    }

}
