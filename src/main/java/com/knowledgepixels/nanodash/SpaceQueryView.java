package com.knowledgepixels.nanodash;

import java.io.Serializable;

public class SpaceQueryView implements Serializable {

    private Space space;
    private GrlcQuery query;
    private String title;

    public SpaceQueryView(Space space, GrlcQuery query, String title) {
        this.space = space;
        this.query = query;
        this.title = title;
    }

    public Space getSpace() {
        return space;
    }

    public GrlcQuery getQuery() {
        return query;
    }

    public String getTitle() {
        return title;
    }

}
