package com.knowledgepixels.nanodash;

import java.io.Serializable;

import org.nanopub.extra.services.ApiResponseEntry;

public class ViewDisplay implements Serializable {

    private ResourceView view;
    private String nanopubId;

    public ViewDisplay(ApiResponseEntry entry) {
        this.view= ResourceView.get(entry.get("view"));
        if (view == null) throw new IllegalArgumentException("View not found: " + entry.get("view"));
        this.nanopubId = entry.get("np");
    }

    public ResourceView getView() {
        return view;
    }

    public String getNanopubId() {
        return nanopubId;
    }

}
