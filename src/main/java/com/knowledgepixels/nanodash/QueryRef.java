package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.HashMap;

public class QueryRef implements Serializable {

    private final String name;
    private final HashMap<String, String> params;

    public QueryRef(String name, HashMap<String, String> params) {
        this.name = name;
        this.params = params;
    }

    public QueryRef(String name) {
        this(name, new HashMap<String, String>());
    }

    public QueryRef(String name, String paramKey, String paramValue) {
        this(name);
        params.put(paramKey, paramValue);
    }

    public String getName() {
        return name;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public String getParam(String key) {
        return params.get(key);
    }

}
