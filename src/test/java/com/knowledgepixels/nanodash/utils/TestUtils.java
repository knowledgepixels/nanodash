package com.knowledgepixels.nanodash.utils;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.core.request.handler.IPageClassRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestHandlerDelegate;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;

import java.util.Random;

import static org.eclipse.rdf4j.model.util.Values.iri;

/**
 * Utility class for creating test nanopublications and providing common constants.
 * This class is used in unit tests to generate valid nanopublications for testing purposes.
 */
public class TestUtils {

    public final static String NANOPUB_URI = "https://w3id.org/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_test";
    public final static ValueFactory vf = SimpleValueFactory.getInstance();
    public final static IRI anyIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");

    public static Nanopub createNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        return createNanopub(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator() throws NanopubAlreadyFinalizedException {
        return new NanopubCreator(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator(String nanopubUri) throws NanopubAlreadyFinalizedException {
        return new NanopubCreator(nanopubUri);
    }

    public static Nanopub createNanopub(String nanopubUri) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = getNanopubCreator(nanopubUri);

        // Create valid nanopub
        Statement assertionStatement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatements(assertionStatement);

        Statement provenanceStatement = vf.createStatement(creator.getAssertionUri(), anyIri, anyIri);
        creator.addProvenanceStatements(provenanceStatement);

        Statement pubinfoStatement = vf.createStatement(creator.getNanopubUri(), anyIri, anyIri);
        creator.addPubinfoStatements(pubinfoStatement);

        return creator.finalizeNanopub();
    }

    public static IRI randomIri() {
        return iri(anyIri + String.valueOf(new Random().nextInt()));
    }

    public static void fillProvenanceGraph(NanopubCreator creator) throws NanopubAlreadyFinalizedException {
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
    }

    public static void fillPubInfoGraph(NanopubCreator creator) throws NanopubAlreadyFinalizedException {
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));
    }

    /**
     * Returns the request handler a page forward was made to, unwrapping the decorators
     * Wicket puts around it.
     */
    private static IPageClassRequestHandler forwardHandler(RestartResponseException ex) {
        IRequestHandler handler = ex.getReplacementRequestHandler();
        while (handler instanceof IRequestHandlerDelegate delegate) {
            handler = delegate.getDelegateHandler();
        }
        return (IPageClassRequestHandler) handler;
    }

    /**
     * Returns the page a forward was made to.
     */
    public static Class<? extends IRequestablePage> forwardedPageClass(RestartResponseException ex) {
        return forwardHandler(ex).getPageClass();
    }

    /**
     * Returns the parameters a forward was made with.
     */
    public static PageParameters forwardedPageParameters(RestartResponseException ex) {
        return forwardHandler(ex).getPageParameters();
    }

}
