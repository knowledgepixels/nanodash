package com.knowledgepixels.nanodash.component;

/**
 * Interface for components that can be part of a context in the Nanodash application.
 * These components can be removed from the context.
 */
public interface ContextComponent {

    /**
     * Removes this component from the context.
     * This method should be called when the component is no longer needed in the context.
     */
    void removeFromContext();

    /**
     * Finalizes the values of this component.
     * This method should be called to ensure all values are set correctly and ready for use.
     */
    void finalizeValues();

}
