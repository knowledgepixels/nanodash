package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;

/**
 * A panel that displays a single RDF triple as a subject-predicate-object format.
 */
public class TripleItem extends Panel {

    /**
     * Constructor for TripleItem.
     *
     * @param id            the component id
     * @param st            the RDF statement to display
     * @param np            the Nanopub containing the statement
     * @param templateClass the IRI of the template class for links
     */
    public TripleItem(String id, Statement st, Nanopub np, IRI templateClass) {
        super(id);

        WebMarkupContainer statement = new WebMarkupContainer("triple");
        statement.add(new NanodashLink("subj", st.getSubject().stringValue(), np, templateClass, false));
        statement.add(new NanodashLink("pred", st.getPredicate().stringValue(), np, templateClass, false));
        if (st.getObject() instanceof IRI) {
            statement.add(new NanodashLink("obj", st.getObject().stringValue(), np, templateClass, true));
        } else {
            statement.add(new Label("obj", "\"" + st.getObject().stringValue() + "\""));
        }
        add(statement);
    }

}
