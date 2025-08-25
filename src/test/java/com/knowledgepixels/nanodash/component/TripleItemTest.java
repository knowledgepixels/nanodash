package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static com.knowledgepixels.nanodash.utils.TestUtils.vf;

class TripleItemTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void rendersTripleWithIriObject() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        Statement statement = vf.createStatement(anyIri, anyIri, anyIri);
        creator.addAssertionStatement(statement);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();

        TripleItem tripleItem = new TripleItem("tripleItem", statement, nanopub, null);

        tester.startComponentInPage(tripleItem);
        tester.assertComponent("tripleItem:triple", WebMarkupContainer.class);
        tester.assertComponent("tripleItem:triple:subj", NanodashLink.class);
        tester.assertComponent("tripleItem:triple:pred", NanodashLink.class);
        tester.assertComponent("tripleItem:triple:obj", NanodashLink.class);
    }

    @Test
    void rendersTripleWithLiteralObject() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        Statement statement = vf.createStatement(anyIri, anyIri, vf.createLiteral("literal"));
        creator.addAssertionStatement(statement);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), anyIri, anyIri));
        creator.addPubinfoStatements(vf.createStatement(creator.getNanopubUri(), anyIri, anyIri));

        Nanopub nanopub = creator.finalizeNanopub();

        TripleItem tripleItem = new TripleItem("tripleItem", statement, nanopub, null);

        tester.startComponentInPage(tripleItem);
        tester.assertComponent("tripleItem:triple", WebMarkupContainer.class);
        tester.assertComponent("tripleItem:triple:subj", NanodashLink.class);
        tester.assertComponent("tripleItem:triple:pred", NanodashLink.class);
        tester.assertComponent("tripleItem:triple:obj", Label.class);
        String sanitizedLabel = Utils.sanitizeHtml("\"literal\"");
        String expectedLabel = StringEscapeUtils.escapeHtml4(StringEscapeUtils.unescapeHtml4(sanitizedLabel));
        tester.assertLabel("tripleItem:triple:obj", expectedLabel);
    }

}