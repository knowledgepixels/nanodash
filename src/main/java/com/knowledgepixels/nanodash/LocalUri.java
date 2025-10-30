package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.base.AbstractIRI;

import java.util.Objects;

/**
 * A simple implementation of IRI for local URIs. Local URIs have a fixed prefix "local:" and a local name.
 */
public class LocalUri extends AbstractIRI {
    /**
     * The prefix for local URIs.
     */
    public static final String PREFIX = "local:";
    private final String localName;

    private LocalUri(String localName) {
        this.localName = localName;
    }

    /**
     * Factory method to create a LocalUri instance.
     *
     * @param localName the local name to be appended to the "local:" prefix.
     * @return a new LocalUri instance.
     */
    public static LocalUri of(String localName) {
        if (localName == null || localName.isBlank()) {
            throw new IllegalArgumentException("Local name cannot be null or blank");
        }
        if (localName.contains("/") || localName.contains("#")) {
            throw new IllegalArgumentException("Local name cannot contain '/' or '#'");
        }
        return new LocalUri(localName);
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalUri localUri = (LocalUri) o;
        return Objects.equals(localName, localUri.localName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localName);
    }

    @Override
    public String stringValue() {
        return PREFIX + localName;
    }

}
