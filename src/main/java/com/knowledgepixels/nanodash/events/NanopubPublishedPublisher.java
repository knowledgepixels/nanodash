package com.knowledgepixels.nanodash.events;

import org.nanopub.Nanopub;

/**
 * An interface for publishers that want to notify listeners when a nanopublication is published.
 */
public interface NanopubPublishedPublisher {

    /**
     * Register listeners for nanopublication published events.
     *
     * @param listener the listener to be registered for nanopublication published events.
     */
    void registerListener(NanopubPublishedListener listener);

    /**
     * Notify all registered listeners that a nanopublication has been published.
     *
     * @param nanopub the nanopublication that was published
     * @param target  the target or context associated with the published nanopublication
     * @param waitMs  the wait time in milliseconds before any action is taken after the notification
     */
    void notifyNanopubPublished(Nanopub nanopub, String target, long waitMs);

}
