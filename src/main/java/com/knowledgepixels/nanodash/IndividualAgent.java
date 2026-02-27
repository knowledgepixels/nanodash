package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// TODO Merge this class with User or otherwise make them aligned.
public class IndividualAgent extends AbstractResourceWithProfile {

    private static Map<String, IndividualAgent> instanceMap = new HashMap<>();

    public static IndividualAgent get(String id) {
        if (!instanceMap.containsKey(id)) {
            instanceMap.put(id, new IndividualAgent(id));
        }
        return instanceMap.get(id);
    }

    private IndividualAgent(String id) {
        super(id);
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

    public boolean isCurrentUser() {
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) return false;
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

    /**
     * Returns whether any view display of this user applies to the given element.
     */
    public boolean appliesTo(String elementId, Set<IRI> classes) {
        triggerDataUpdate();
        for (ViewDisplay v : getViewDisplays()) {
            if (v.appliesTo(elementId, classes)) return true;
        }
        return false;
    }

}
