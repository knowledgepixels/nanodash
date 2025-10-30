package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.UnificationException;
import org.eclipse.rdf4j.model.Value;

/**
 * Interface for components that can be part of a context in the Nanodash application.
 * These components can be unified with RDF values and can be removed from the context.
 */
public interface ContextComponent {

    /**
     * Removes this component from the context.
     * This method should be called when the component is no longer needed in the context.
     */
    void removeFromContext();

    /**
     * Checks if this component can be unified with the given RDF value.
     *
     * @param v the RDF value to check against
     * @return true if the component can be unified with the value, false otherwise
     */
    boolean isUnifiableWith(Value v);

    /**
     * Unifies this component with the given RDF value.
     * If the component cannot be unified with the value, an UnificationException is thrown.
     *
     * @param v the RDF value to unify with
     * @throws UnificationException if the component cannot be unified with the value
     */
    void unifyWith(Value v) throws UnificationException;

    /**
     * Called when the filling of values is finished.
     * This method can be used to perform any final actions after all values have been filled.
     */
    void fillFinished();

    /**
     * Finalizes the values of this component.
     * This method should be called to ensure all values are set correctly and ready for use.
     */
    void finalizeValues();

}
