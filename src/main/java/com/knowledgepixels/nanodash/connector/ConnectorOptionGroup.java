package com.knowledgepixels.nanodash.connector;

import java.io.Serializable;

/**
 * Represents a group of connector options, each with a title and an array of options.
 */
public class ConnectorOptionGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String title;
    private final ConnectorOption[] options;

    /**
     * Creates a new ConnectorOptionGroup with the specified title and options.
     *
     * @param title   the title of the option group
     * @param options the array of ConnectorOption objects that belong to this group
     */
    public ConnectorOptionGroup(String title, ConnectorOption... options) {
        this.title = title;
        this.options = options;
    }

    /**
     * Returns the title of this option group.
     *
     * @return the title of the option group
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the array of ConnectorOption objects that belong to this group.
     *
     * @return the array of ConnectorOption objects
     */
    public ConnectorOption[] getOptions() {
        return options;
    }

}
