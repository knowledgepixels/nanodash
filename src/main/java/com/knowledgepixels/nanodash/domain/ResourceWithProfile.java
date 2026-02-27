package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ViewDisplay;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;

import java.util.List;
import java.util.Set;

/**
 * Interface representing a resource that has an associated profile.
 */
public interface ResourceWithProfile {

    String getId();

    String getLabel();

    String getNanopubId();

    Nanopub getNanopub();

    List<ViewDisplay> getViewDisplays();

    List<ViewDisplay> getTopLevelViewDisplays();

    List<ViewDisplay> getPartLevelViewDisplays(String resourceId, Set<IRI> classes);

    Thread triggerDataUpdate();

    void setDataNeedsUpdate();

    boolean isDataInitialized();

    Space getSpace();

    List<AbstractResourceWithProfile> getAllSuperSpacesUntilRoot();

    Long getRunUpdateAfter();

}
