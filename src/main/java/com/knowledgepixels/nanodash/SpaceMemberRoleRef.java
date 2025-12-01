package com.knowledgepixels.nanodash;

import java.io.Serializable;

/**
 * Reference to a Space Member Role along with its associated Nanopublication URI.
 */
public class SpaceMemberRoleRef implements Serializable {

    private SpaceMemberRole role;
    private String nanopubUri;

    /**
     * Constructor for SpaceMemberRoleRef.
     *
     * @param role       the SpaceMemberRole
     * @param nanopubUri the associated Nanopublication URI
     */
    public SpaceMemberRoleRef(SpaceMemberRole role, String nanopubUri) {
        this.role = role;
        this.nanopubUri = nanopubUri;
    }

    /**
     * Gets the SpaceMemberRole.
     *
     * @return the SpaceMemberRole
     */
    public SpaceMemberRole getRole() {
        return role;
    }

    /**
     * Gets the Nanopublication URI.
     *
     * @return the Nanopublication URI
     */
    public String getNanopubUri() {
        return nanopubUri;
    }

}
