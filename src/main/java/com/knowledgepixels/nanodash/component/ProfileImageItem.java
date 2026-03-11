package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

import static com.knowledgepixels.nanodash.Utils.urlEncode;

public class ProfileImageItem extends Panel {

    private final NanodashSession session = NanodashSession.get();

    public ProfileImageItem(String id) {
        super(id);

        String publishImageLinkString = PublishPage.MOUNT_PATH +
                                        "?template=https://w3id.org/np/RAV6PgoKtIc5pkI0AXRbmwRlTFpA-DHytaZfEFGq3OTjo&" +
                                        "param_user=" + urlEncode(session.getUserIri()) + "&" +
                                        "link-message=" + urlEncode("Enter the URL of your profile image below, check the checkbox at the end of this page and press 'Publish' to publish it.");

        IRI imageUrl = User.getProfilePicture(session.getUserIri());
        boolean hasImage = imageUrl != null;

        WebMarkupContainer imageContainer = new WebMarkupContainer("image-container");
        imageContainer.add(new ExternalImage("image", hasImage ? imageUrl.toString() : ""));
        imageContainer.add(new ExternalLink("image-url-link", hasImage ? imageUrl.toString() : "#", hasImage ? imageUrl.toString() : ""));
        imageContainer.setVisible(hasImage);
        add(imageContainer);

        add(new Label("no-image-note", "<em>No profile image has been set yet.</em>").setEscapeModelStrings(false).setVisible(!hasImage));

        WebMarkupContainer publishImageItem = new WebMarkupContainer("publish-image-item");
        publishImageItem.add(new ExternalLink("publish-image-link", publishImageLinkString, hasImage ? "update profile image..." : "set profile image..."));
        add(publishImageItem);
    }

}
