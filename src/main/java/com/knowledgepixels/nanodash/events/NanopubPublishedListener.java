package com.knowledgepixels.nanodash.events;

import org.nanopub.Nanopub;

/**
 * An interface for listeners that want to be notified when a nanopublication is published. Implementing classes should define the behavior to execute when a nanopublication is published, including any optional wait time before processing the event.
 */
public interface NanopubPublishedListener {

    /**
     * Method to be called when a nanopublication is published. Implementing classes should define the behavior to execute when this event occurs, including handling the nanopublication and any optional wait time before processing.
     *
     * @param nanopub the nanopublication that was published
     * @param target  the target or context associated with the published nanopublication (e.g., a specific view, space, or query that should be run)
     * @param waitMs  the optional wait time in milliseconds before processing the event (0 for immediate processing)
     */
    void onNanopubPublished(Nanopub nanopub, String target, long waitMs);

}
