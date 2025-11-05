package com.knowledgepixels.nanodash;

import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;

/**
 * A class representing the display of a resource view associated with a Space.
 */
public class ViewDisplay implements Serializable {

    private ResourceView view;
    private String nanopubId;

    /**
     * Constructor for ViewDisplay.
     *
     * @param entry an ApiResponseEntry containing the view and nanopub ID.
     */
    public ViewDisplay(ApiResponseEntry entry) {
        this.view = ResourceView.get(entry.get("view"));
        if (view == null) throw new IllegalArgumentException("View not found: " + entry.get("view"));
        this.nanopubId = entry.get("np");
    }

    /**
     * Gets the ResourceView associated with this ViewDisplay.
     *
     * @return the ResourceView
     */
    public ResourceView getView() {
        return view;
    }

    /**
     * Gets the nanopub ID associated with this ViewDisplay.
     *
     * @return the nanopub ID
     */
    public String getNanopubId() {
        return nanopubId;
    }

}
