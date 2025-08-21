package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.UnificationException;
import org.eclipse.rdf4j.model.Value;

public interface ContextComponent {

    public void removeFromContext();

    public boolean isUnifiableWith(Value v);

    public void unifyWith(Value v) throws UnificationException;

    public void fillFinished();

    public void finalizeValues();

}
