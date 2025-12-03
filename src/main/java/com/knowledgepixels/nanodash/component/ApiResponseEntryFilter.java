package com.knowledgepixels.nanodash.component;

import java.io.Serializable;

// FIXME the textSearch value is always null and the input text is never passed
public class ApiResponseEntryFilter implements Serializable {

    private String textSearch;

    public String getTextSearch() {
        return textSearch;
    }

    public void setTextSearch(String textSearch) {
        this.textSearch = textSearch;
    }

}
