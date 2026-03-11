package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

import static com.knowledgepixels.nanodash.Utils.urlEncode;

/**
 * A panel that shows the user's profile license.
 */
public class ProfileLicenseItem extends Panel {

    private final NanodashSession session = NanodashSession.get();
    private static final String DEFAULT_LICENSE_TEMPLATE = "https://w3id.org/np/RAFmIDts27rRMUzK1HVdrVQTig4bJyr8mqzgzR5ulB2T0";

    public ProfileLicenseItem(String id) {
        super(id);

        String publishLicenseLinkString = PublishPage.MOUNT_PATH +
                                          "?template=" + DEFAULT_LICENSE_TEMPLATE + "&" +
                                          "param_user=" + urlEncode(session.getUserIri()) + "&" +
                                          "link-message=" + urlEncode("Enter the URL of your preferred license below, check the checkbox at the end of this page and press 'Publish' to publish it.");

        IRI licenseUrl = User.getDefaultLicense(session.getUserIri());
        boolean hasLicense = licenseUrl != null;

        WebMarkupContainer licenseContainer = new WebMarkupContainer("license-container");
        licenseContainer.add(new ExternalLink("license-url-link", hasLicense ? licenseUrl.toString() : "#", hasLicense ? licenseUrl.toString() : ""));
        licenseContainer.setVisible(hasLicense);
        add(licenseContainer);

        add(new Label("no-license-note", "<em>No profile license has been set yet.</em>").setEscapeModelStrings(false).setVisible(!hasLicense));

        WebMarkupContainer publishLicenseItem = new WebMarkupContainer("publish-license-item");
        publishLicenseItem.add(new ExternalLink("publish-license-link", publishLicenseLinkString, hasLicense ? "update profile license..." : "set profile license..."));
        add(publishLicenseItem);
    }

}
