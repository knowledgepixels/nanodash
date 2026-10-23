package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * The icon standing for a person in a page title: the profile picture they have published,
 * else the generic symbol for a person or for a software agent. The markup it binds to
 * carries the {@code user-profile-pic} class, which gives it the tilted-square mask.
 */
public class UserIcon {

    private UserIcon() {
    }

    /**
     * The icon for a person.
     *
     * @param markupId the Wicket component id
     * @param userIri  the person's IRI
     * @return the image component
     */
    public static Component of(String markupId, IRI userIri) {
        ProfilePicture profilePicture = User.getProfilePicture(userIri);
        if (profilePicture != null) {
            return new ExternalImage(markupId, profilePicture.getSrc());
        }
        String icon = IndividualAgent.isSoftware(userIri) ? "images/bot-icon.svg" : "images/user-icon.svg";
        return new Image(markupId, new ContextRelativeResourceReference(icon, false));
    }

    /**
     * The same, for a page whose resource is a person only sometimes — a part page, which
     * shows whatever the context maintains. Invisible where the id names no person, so the
     * title of a sub-space or a plain term is not given a face.
     *
     * @param markupId the Wicket component id
     * @param id       the page's resource id
     * @return the image component, or an invisible placeholder
     */
    public static Component ofPerson(String markupId, String id) {
        if (!IndividualAgent.isAgentId(id)) {
            return new WebMarkupContainer(markupId).setVisible(false);
        }
        return of(markupId, Values.iri(id));
    }

}
