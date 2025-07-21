package com.knowledgepixels.nanodash.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

public class TestUtils {

    public final static String NANOPUB_URI = "https://w3id.org/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_test";
    public final static ValueFactory vf = SimpleValueFactory.getInstance();
    public final static IRI anyIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");

    public static Nanopub createNanopub() throws MalformedNanopubException {
        return createNanopub(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator() {
        return new NanopubCreator(NANOPUB_URI);
    }

    public static NanopubCreator getNanopubCreator(String nanopubUri) {
        return new NanopubCreator(nanopubUri);
    }

    public static Nanopub createNanopub(String nanopubUri) throws MalformedNanopubException {
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

}
