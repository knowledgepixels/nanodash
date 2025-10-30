package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * An abstract base class that extends Wicket's Panel and implements ContextComponent, providing a TemplateContext for derived components.
 */
public abstract class AbstractContextComponent extends Panel implements ContextComponent {

    protected TemplateContext context;
    protected static ValueFactory vf = SimpleValueFactory.getInstance();

    /**
     * Constructor for AbstractContextComponent.
     *
     * @param id      the Wicket component ID
     * @param context the TemplateContext to be used by this component
     */
    public AbstractContextComponent(String id, TemplateContext context) {
        super(id);
        this.context = context;
    }

}
