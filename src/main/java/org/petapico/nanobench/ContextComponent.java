package org.petapico.nanobench;

import org.eclipse.rdf4j.model.Value;

public interface ContextComponent {

	public void removeFromContext();

	public boolean isUnifiableWith(Value v);

	public void unifyWith(Value v) throws UnificationException;

}
