package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A reference to a query with optional parameters.
 * This class is used to encapsulate the name of the query and any parameters
 * that need to be passed to it.
 */
public class QueryRef implements Serializable {

    private final String name;
    private final Map<String, String> params;

    /**
     * Constructor for QueryRef.
     *
     * @param name   the name of the query
     * @param params a map of parameters for the query
     */
    public QueryRef(String name, Map<String, String> params) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Query name cannot be null or empty");
        }
        this.name = name;
        this.params = params;
    }

    /**
     * Constructor for QueryRef with no parameters.
     *
     * @param name the name of the query
     */
    public QueryRef(String name) {
        this(name, new HashMap<>());
    }

    /**
     * Constructor for QueryRef with a single parameter.
     *
     * @param name       the name of the query
     * @param paramKey   the key of the parameter
     * @param paramValue the value of the parameter
     */
    public QueryRef(String name, String paramKey, String paramValue) {
        this(name);
        if (paramKey == null || paramKey.isBlank()) {
            throw new IllegalArgumentException("Parameter key cannot be null or empty");
        }
        params.put(paramKey, paramValue);
    }

    /**
     * Get the name of the query.
     *
     * @return the name of the query
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parameters of the query.
     *
     * @return a map of parameters
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * Get a specific parameter by key.
     *
     * @param key the key of the parameter
     * @return the value of the parameter, or null if not found
     */
    public String getParam(String key) {
        return params.get(key);
    }

}
