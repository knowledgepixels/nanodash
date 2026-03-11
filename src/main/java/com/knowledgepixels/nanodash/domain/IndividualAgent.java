package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TODO Merge this class with User or otherwise make them aligned.
public class IndividualAgent extends AbstractResourceWithProfile {

    private static final Map<String, IndividualAgent> instanceMap = new HashMap<>();

    public static IndividualAgent get(String id) {
        if (!instanceMap.containsKey(id)) {
            instanceMap.put(id, new IndividualAgent(id));
        }
        return instanceMap.get(id);
    }

    private IndividualAgent(String id) {
        super(id);
    }

    /**
     * Checks if a given string represents a user ID.
     *
     * @param userId The string to check.
     * @return True if the string represents a user ID, false otherwise.
     */
    public static boolean isUser(String userId) {
        return User.getUserData().isUser(userId);
    }

    /**
     * Checks if a given IRI represents a software agent.
     *
     * @param userIri The IRI to check.
     * @return True if the IRI represents a software agent, false otherwise.
     */
    public static boolean isSoftware(IRI userIri) {
        return User.getUserData().isSoftware(userIri);
    }

    @Override
    public String getNanopubId() {
        // FIXME this will be removed in the future
        return null;
    }

    @Override
    public Nanopub getNanopub() {
        // FIXME this will be removed in the future
        return null;
    }

    @Override
    public String getNamespace() {
        // FIXME this will be removed in the future
        return null;
    }

    /**
     * Checks if this user is the currently logged-in user.
     *
     * @return True if this user is the currently logged-in user, false otherwise.
     */
    public boolean isCurrentUser() {
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) {
            return false;
        }
        return getId().equals(userIri.stringValue());
    }

    @Override
    public String getLabel() {
        try {
            return User.getUserData().getShortDisplayName(Utils.vf.createIRI(getId()));
        } catch (Exception ex) {
        }
        return getId();
    }

}
