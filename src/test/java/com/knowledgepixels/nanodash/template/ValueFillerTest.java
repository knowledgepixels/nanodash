package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class ValueFillerTest {

    @Test
    void transformValueNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);

        Value value = valueFiller.transform(np.getUri());
        assertEquals(value, iri("local:nanopub"));
    }

    @Test
    void transformValueAssertion() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);

        Value value = valueFiller.transform(np.getAssertionUri());
        assertEquals(value, iri("local:assertion"));
    }

    @Test
    void constructValueFiller() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);

        valueFiller.getUnusedStatements().forEach(System.out::println);
    }

    @Test
    void hasStatements() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);
        assertTrue(valueFiller.hasStatements());
    }

    @Test
    void hasUsedStatements() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);
        assertFalse(valueFiller.hasUsedStatements());
    }

    @Test
    void hasUnusedStatements() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);
        assertTrue(valueFiller.hasUnusedStatements());
    }

    @Test
    void getUnusedStatements() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub();
        ValueFiller valueFiller = new ValueFiller(np, ContextType.ASSERTION, true);
        List<Statement> unusedStatements = valueFiller.getUnusedStatements();
        List<Statement> expectedStatements = np.getAssertion().stream().map(valueFiller::transform).collect(Collectors.toList());
        assertEquals(unusedStatements, expectedStatements);
    }

}