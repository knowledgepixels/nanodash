package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A stand-in for a page that is a resource's own page, used to render the {@code cite-as}
 * link without reaching for a nanopublication over the network.
 */
public class CiteAsSubjectPage extends NanodashPage {

    /** The IRI this page claims to be cited as. */
    public static final String SUBJECT_IRI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";

    /**
     * @param parameters the page parameters, unused
     */
    public CiteAsSubjectPage(final PageParameters parameters) {
        super(parameters);
        try {
            add(new TitleBar("titlebar", this));
        } catch (Exception ex) {
            add(new EmptyPanel("titlebar"));
        }
        add(new Label("pagetitle", "Cite-as | nanodash"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return "/cite-as-subject";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCiteAsIri() {
        return SUBJECT_IRI;
    }

}
