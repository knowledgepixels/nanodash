package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for issue #505: {@code RepetitionGroup.matches()} and
 * {@code RepetitionGroup.fill()} used to disagree for grouped statements whose
 * sub-statements share a placeholder/local resource. {@code fill()} binds those
 * shared models greedily as it walks the parts, so an early binding could block a
 * later part even though a consistent assignment existed — leaving the grouped
 * statement's reification triples in the "unused" section.
 *
 * <p>Fixture: the published "Defining a biochementity" template nanopub, viewed via
 * its meta-template "Defining an assertion template". Its {@code st9a}–{@code st9e}
 * grouped sub-statements share the {@code contextalias} local resource and the
 * introduced {@code ~~ARTIFACTCODE~~} resource. Before the fix those reification
 * triples remained unused; after it, they are matched into the form.
 */
class GroupedSharedPlaceholderFillTest {

    // "Defining an assertion template" — the meta-template the fixture was created from.
    private static final String META_TEMPLATE = "https://w3id.org/np/RAM3Mma65yu2b7JmKhw6GYcMVc2LWdgcqkZD3FGJbCIo4";
    // The "Defining a biochementity" template nanopub (a template definition, viewed via the meta-template).
    private static final String FIXTURE = "https://w3id.org/np/RAhSlIuuw5YqmMoyyvmy5GL3qIhs7sp14i6x2y3DCOhXM";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    @Test
    void groupedSubStatementsWithSharedPlaceholderGetMatched() {
        Nanopub fixture = Utils.getNanopub(FIXTURE);
        assertNotNull(fixture, "fixture nanopub should be fetchable from the registry");

        // Mirror the viewer flow (NanopubItem.populateStatementItemList): read-only context built
        // from the meta-template, fed the fixture's assertion triples.
        ValueFiller filler = new ValueFiller(fixture, ContextType.ASSERTION, false);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, META_TEMPLATE, "assertion-statement", fixture);
        context.initStatements();
        filler.fill(context);

        List<Statement> unused = filler.getUnusedStatements();
        // The previously-failing triples are the contextalias grouped sub-statement reifications.
        List<Statement> unusedContextAlias = unused.stream()
                .filter(st -> {
                    String s = st.toString();
                    return s.contains("ContextAlias") || s.contains("contextalias");
                })
                .collect(Collectors.toList());

        assertTrue(unusedContextAlias.isEmpty(),
                "grouped contextalias sub-statements should be matched, but these stayed unused:\n"
                        + unusedContextAlias.stream().map(Object::toString).collect(Collectors.joining("\n")));
    }
}
