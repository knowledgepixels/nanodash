package com.knowledgepixels.nanodash;

import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;

/**
 * A class representing the display of a resource view associated with a Space.
 */
public class ViewDisplay implements Serializable {

    private ResourceView view;
    private String nanopubId;
    private String title;
    private Integer pageSize;

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
     * Creates a plain minimal view display without attached view object.
     *
     * @param pageSize the page size of the view display
     */
    public ViewDisplay(Integer pageSize) {
        this.pageSize = pageSize;
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

    public Integer getPageSize() {
        if (pageSize != null) return pageSize;
        if (view.getPageSize() != null) return view.getPageSize();
        return 10;
    }

    public String getTitle() {
        if (title != null) return title;
        if (view != null) return view.getTitle();
        return null;
    }

}
