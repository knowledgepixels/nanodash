package com.knowledgepixels.nanodash;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turning the rows of a network view's query — one per edge — into the nodes and edges
 * its renderer draws (issue #792).
 */
class NetworkGraphTest {

    private static final UnaryOperator<String> PAGE_URL = iri -> "/explore?id=" + iri;

    /**
     * Builds a response with the given header and rows, as the query API returns one.
     */
    private static ApiResponse response(String[] header, String[]... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(header);
        for (String[] row : rows) response.add(row);
        return response;
    }

    private static JsonObject graphOf(NetworkGraph.Graph graph) {
        return JsonParser.parseString(graph.json()).getAsJsonObject();
    }

    private static JsonArray nodes(NetworkGraph.Graph graph) {
        return graphOf(graph).getAsJsonArray("nodes");
    }

    private static JsonArray edges(NetworkGraph.Graph graph) {
        return graphOf(graph).getAsJsonArray("edges");
    }

    private static JsonObject nodeById(NetworkGraph.Graph graph, String id) {
        for (var element : nodes(graph)) {
            JsonObject node = element.getAsJsonObject();
            if (id.equals(node.get("id").getAsString())) return node;
        }
        throw new AssertionError("no node " + id + " in " + graph.json());
    }

    private static String stringOrNull(JsonObject object, String member) {
        return object.has(member) ? object.get(member).getAsString() : null;
    }

    @Test
    void edgeRowsBecomeNodesAndEdges() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", "https://example.org/b"},
                new String[]{"https://example.org/b", "https://example.org/c"}), PAGE_URL);

        // Three distinct nodes out of two edges: the shared one is not drawn twice.
        assertEquals(3, graph.nodeCount());
        assertEquals(2, graph.edgeCount());
        assertEquals(3, nodes(graph).size());
        JsonObject firstEdge = edges(graph).get(0).getAsJsonObject();
        assertEquals("https://example.org/a", firstEdge.get("from").getAsString());
        assertEquals("https://example.org/b", firstEdge.get("to").getAsString());
    }

    @Test
    void nodeTakesItsLabelFromTheFirstRowThatKnowsOne() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_label", "target", "target_label"},
                new String[]{"https://example.org/a", "", "https://example.org/b", "Bee"},
                new String[]{"https://example.org/b", "Bee again", "https://example.org/a", "Ay"}), PAGE_URL);

        // "Bee" comes from the row where b was the target; the later row cannot rename it.
        assertEquals("Bee", nodeById(graph, "https://example.org/b").get("label").getAsString());
        // a had a blank label on the first row, so the later one is still allowed to name it.
        assertEquals("Ay", nodeById(graph, "https://example.org/a").get("label").getAsString());
    }

    @Test
    void nodeWithoutALabelInTheRowTakesTheOneNanodashKnows() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                        new String[]{"source", "source_label", "target"},
                        new String[]{"https://example.org/space", "", "https://example.org/other"}),
                PAGE_URL, iri -> "https://example.org/space".equals(iri) ? "The Space" : null);

        assertEquals("The Space", nodeById(graph, "https://example.org/space").get("label").getAsString());
        // Nothing known about the other one, so its short name stands.
        assertEquals("other", nodeById(graph, "https://example.org/other").get("label").getAsString());
    }

    @Test
    void aLabelFromTheQueryWinsOverTheOneNanodashKnows() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                        new String[]{"source", "source_label", "target"},
                        new String[]{"https://example.org/space", "As the query calls it", "https://example.org/b"}),
                PAGE_URL, iri -> "As Nanodash calls it");

        assertEquals("As the query calls it",
                nodeById(graph, "https://example.org/space").get("label").getAsString());
    }

    /**
     * The hover box says what the query gives it to say: a description where there is one,
     * the label otherwise. The caption under the node is the label either way — which is
     * the point of having two columns.
     */
    @Test
    void nodeHoverTextComesFromItsDescriptionColumn() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_label", "source_desc", "target"},
                new String[]{"https://example.org/a", "Ay",
                        "Ay, the first of its kind, described at length", "https://example.org/b"}), PAGE_URL);

        JsonObject node = nodeById(graph, "https://example.org/a");
        assertEquals("Ay", node.get("label").getAsString());
        // The hover box is the description and nothing else: no IRI, no type. The IRI is
        // what the status line in the corner is for.
        assertEquals("Ay, the first of its kind, described at length",
                node.get("title").getAsString());
    }

    @Test
    void nodeHoverTextFallsBackToTheLabel() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_label", "source_desc", "target"},
                new String[]{"https://example.org/a", "Ay", "", "https://example.org/b"}), PAGE_URL);

        assertEquals("Ay", nodeById(graph, "https://example.org/a").get("title").getAsString());
    }

    @Test
    void nodeDescriptionIsTakenFromTheFirstRowThatHasOne() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_desc", "target", "target_desc"},
                new String[]{"https://example.org/a", "", "https://example.org/b", "What b is"},
                new String[]{"https://example.org/b", "What b is, said again", "https://example.org/a", "What a is"}),
                PAGE_URL);

        assertEquals("What b is", nodeById(graph, "https://example.org/b").get("title").getAsString());
        // a was described by no row until the second one, which is still allowed to.
        assertEquals("What a is", nodeById(graph, "https://example.org/a").get("title").getAsString());
    }

    /**
     * A node the query neither names nor describes falls back to the name Nanodash knows
     * for it, in the hover box as well as in the caption.
     */
    @Test
    void hoverTextFallsBackToTheLabelNanodashKnows() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                        new String[]{"source", "target"},
                        new String[]{"https://example.org/space", "https://example.org/b"}),
                PAGE_URL, iri -> "https://example.org/space".equals(iri) ? "The Space" : null);

        assertEquals("The Space", nodeById(graph, "https://example.org/space").get("title").getAsString());
    }

    /**
     * A node the query neither describes nor names, and that Nanodash cannot name either,
     * has nothing to say on hover — so it says nothing, rather than opening a box with the
     * caption or the IRI in it.
     */
    @Test
    void nodeWithNothingToSayHasNoHoverBox() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/thing/a", "https://example.org/thing/b"}), PAGE_URL);

        JsonObject node = nodeById(graph, "https://example.org/thing/a");
        assertEquals("a", node.get("label").getAsString());
        assertNull(stringOrNull(node, "title"));
    }

    @Test
    void edgeHoverTextComesFromItsDescriptionColumn() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "edge_label", "edge_desc", "target", "np"},
                new String[]{"https://example.org/a", "https://example.org/p", "points at",
                        "points at it, since the third of March", "https://example.org/b",
                        "https://w3id.org/np/RA1"}), PAGE_URL);

        JsonObject edge = edges(graph).get(0).getAsJsonObject();
        // The caption on the line stays the label; only the hover box takes the description.
        assertEquals("points at", edge.get("label").getAsString());
        assertEquals("points at it, since the third of March", edge.get("title").getAsString());
    }

    @Test
    void edgeHoverTextFallsBackToItsLabel() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "edge_label", "target"},
                new String[]{"https://example.org/a", "https://example.org/p", "points at",
                        "https://example.org/b"}), PAGE_URL);

        assertEquals("points at", edges(graph).get(0).getAsJsonObject().get("title").getAsString());
    }

    /**
     * The status line names the relation an edge instantiates, so the IRI travels with the
     * edge even though no hover box shows it. An edge with no type names nothing.
     */
    @Test
    void edgeCarriesItsRelationIriForTheStatusLine() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "edge_label", "target"},
                new String[]{"https://example.org/a", "https://example.org/p", "points at",
                        "https://example.org/b"},
                new String[]{"https://example.org/a", "", "related to", "https://example.org/c"}), PAGE_URL);

        assertEquals("https://example.org/p", edges(graph).get(0).getAsJsonObject().get("iri").getAsString());
        assertNull(stringOrNull(edges(graph).get(1).getAsJsonObject(), "iri"));
    }

    @Test
    void edgeWithNothingToSayHasNoHoverBox() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "target", "np"},
                new String[]{"https://example.org/a", "https://w3id.org/kpxl/gen/terms/isMaintainedBy",
                        "https://example.org/b", "https://w3id.org/np/RA1"}), PAGE_URL);

        JsonObject edge = edges(graph).get(0).getAsJsonObject();
        // It is captioned from its type and leads to its nanopublication, but the query
        // gave it no words of its own, so there is no box to open.
        assertEquals("isMaintainedBy", edge.get("label").getAsString());
        assertNull(stringOrNull(edge, "title"));
    }

    @Test
    void nodeWithoutLabelFallsBackToItsShortName() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/thing/a", "https://example.org/thing/b"}), PAGE_URL);

        assertEquals("a", nodeById(graph, "https://example.org/thing/a").get("label").getAsString());
    }

    @Test
    void rowWithOnlyOneEndIsSkipped() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", ""},
                new String[]{"", "https://example.org/b"},
                new String[]{"https://example.org/a", "https://example.org/b"}), PAGE_URL);

        assertEquals(1, graph.edgeCount());
        assertEquals(2, graph.nodeCount());
    }

    @Test
    void repeatedEdgeIsDrawnOnce() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target", "edge_type", "np"},
                new String[]{"https://example.org/a", "https://example.org/b", "https://example.org/p", "https://w3id.org/np/RA1"},
                new String[]{"https://example.org/a", "https://example.org/b", "https://example.org/p", "https://w3id.org/np/RA2"}), PAGE_URL);

        assertEquals(1, graph.edgeCount());
    }

    @Test
    void differentRelationsBetweenTheSameNodesStayDistinct() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target", "edge_type"},
                new String[]{"https://example.org/a", "https://example.org/b", "https://example.org/p"},
                new String[]{"https://example.org/a", "https://example.org/b", "https://example.org/q"}), PAGE_URL);

        assertEquals(2, graph.edgeCount());
    }

    @Test
    void typesAreColouredApartAndListedInTheLegend() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_type", "target", "target_type"},
                new String[]{"https://example.org/s", "https://w3id.org/kpxl/gen/terms/Space",
                        "https://example.org/a", "https://w3id.org/kpxl/gen/terms/IndividualAgent"},
                new String[]{"https://example.org/s", "https://w3id.org/kpxl/gen/terms/Space",
                        "https://example.org/b", "https://w3id.org/kpxl/gen/terms/IndividualAgent"}), PAGE_URL);

        String spaceColor = nodeById(graph, "https://example.org/s").get("color").getAsString();
        String agentColor = nodeById(graph, "https://example.org/a").get("color").getAsString();
        assertNotEquals(spaceColor, agentColor);
        // Same type, same colour — however many rows it took to get there.
        assertEquals(agentColor, nodeById(graph, "https://example.org/b").get("color").getAsString());

        List<String> legendTypes = new ArrayList<>();
        for (NetworkGraph.LegendEntry entry : graph.legend()) legendTypes.add(entry.type());
        assertEquals(List.of("https://w3id.org/kpxl/gen/terms/Space",
                "https://w3id.org/kpxl/gen/terms/IndividualAgent"), legendTypes);
        assertEquals(spaceColor, graph.legend().get(0).color());
    }

    /**
     * A type is called whatever the query's {@code _type_label} calls it, so a legend can
     * read "space" and "person" rather than the tail of two IRIs.
     */
    @Test
    void legendNamesATypeByItsTypeLabel() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_type", "source_type_label",
                        "target", "target_type", "target_type_label"},
                new String[]{"https://example.org/s", "https://w3id.org/kpxl/gen/terms/Space", "space",
                        "https://example.org/a", "https://w3id.org/kpxl/gen/terms/IndividualAgent", "person"}),
                PAGE_URL);

        assertEquals(List.of("space", "person"), graph.legend().stream().map(NetworkGraph.LegendEntry::label).toList());
        // The IRI is still what the entry stands for, and what the status line shows.
        assertEquals("https://w3id.org/kpxl/gen/terms/Space", graph.legend().get(0).type());
    }

    /**
     * The first name a type is given wins. A query that names it differently two rows later
     * is not reconciled and not reported — a legend is a key to the colours, and the colour
     * is keyed on the IRI.
     */
    @Test
    void firstTypeLabelWinsOverLaterOnes() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_type", "source_type_label", "target"},
                new String[]{"https://example.org/a", "https://w3id.org/kpxl/gen/terms/Space", "",
                        "https://example.org/b"},
                new String[]{"https://example.org/c", "https://w3id.org/kpxl/gen/terms/Space", "space",
                        "https://example.org/d"},
                new String[]{"https://example.org/e", "https://w3id.org/kpxl/gen/terms/Space", "SPACE!!",
                        "https://example.org/f"}), PAGE_URL);

        assertEquals(1, graph.legend().size());
        assertEquals("space", graph.legend().get(0).label());
    }

    @Test
    void legendFallsBackToTheShortNameOfTheType() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_type", "target"},
                new String[]{"https://example.org/a", "https://w3id.org/kpxl/gen/terms/Space",
                        "https://example.org/b"}), PAGE_URL);

        assertEquals("Space", graph.legend().get(0).label());
        assertEquals("https://w3id.org/kpxl/gen/terms/Space", graph.legend().get(0).type());
    }

    @Test
    void untypedQueryHasNoLegend() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", "https://example.org/b"}), PAGE_URL);

        assertTrue(graph.legend().isEmpty());
        // Both nodes are drawn, in the one colour that means nothing in particular.
        assertEquals(nodeById(graph, "https://example.org/a").get("color").getAsString(),
                nodeById(graph, "https://example.org/b").get("color").getAsString());
    }

    /**
     * An edge is captioned by its label and identified by its type, and leads to the
     * nanopublication it was read from rather than to the relation it is an instance of.
     */
    @Test
    void edgeLeadsToTheNanopublicationThatAssertsIt() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "edge_label", "target", "np"},
                new String[]{"https://example.org/a", "https://example.org/p", "points at",
                        "https://example.org/b", "https://w3id.org/np/RA1"}), PAGE_URL);

        JsonObject edge = edges(graph).get(0).getAsJsonObject();
        assertEquals("points at", edge.get("label").getAsString());
        assertEquals("/explore?id=https://w3id.org/np/RA1", edge.get("url").getAsString());
        // The hover box says what the edge is; the relation's IRI goes to the status line.
        assertEquals("points at", edge.get("title").getAsString());
        assertEquals("https://example.org/p", edge.get("iri").getAsString());
    }

    @Test
    void untypedEdgeStillLeadsToItsNanopublication() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target", "np"},
                new String[]{"https://example.org/a", "https://example.org/b", "https://w3id.org/np/RA1"}), PAGE_URL);

        assertEquals("/explore?id=https://w3id.org/np/RA1",
                edges(graph).get(0).getAsJsonObject().get("url").getAsString());
    }

    @Test
    void edgeWithNothingToOpenHasNoUrl() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target", "edge_label"},
                new String[]{"https://example.org/a", "https://example.org/b", "related to"}), PAGE_URL);

        JsonObject edge = edges(graph).get(0).getAsJsonObject();
        assertEquals("related to", edge.get("label").getAsString());
        assertNull(stringOrNull(edge, "url"));
    }

    /**
     * A typed edge is not a link to its type: without a nanopublication there is nothing
     * for a click on it to open.
     */
    @Test
    void typedEdgeWithoutANanopublicationHasNoUrl() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "edge_type", "target"},
                new String[]{"https://example.org/a", "https://example.org/p", "https://example.org/b"}), PAGE_URL);

        assertNull(stringOrNull(edges(graph).get(0).getAsJsonObject(), "url"));
    }

    @Test
    void edgeCaptionFallsBackToTheRelationsShortName() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target", "edge_type"},
                new String[]{"https://example.org/a", "https://example.org/b",
                        "https://w3id.org/kpxl/gen/terms/isMaintainedBy"}), PAGE_URL);

        assertEquals("isMaintainedBy", edges(graph).get(0).getAsJsonObject().get("label").getAsString());
    }

    /**
     * A crowded diagram cuts its captions shorter than a link label would be, because the
     * labels are what collide; the hover box keeps the whole thing either way.
     */
    @Test
    void captionsAreCutShorterOnACrowdedDiagram() {
        String label = "A label of a length that a sparse diagram shows in full";
        String[][] rows = new String[40][];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new String[]{"https://example.org/n" + i, label, "https://example.org/m" + i, label};
        }
        NetworkGraph.Graph dense = NetworkGraph.build(
                response(new String[]{"source", "source_label", "target", "target_label"}, rows), PAGE_URL);
        NetworkGraph.Graph sparse = NetworkGraph.build(response(
                new String[]{"source", "source_label", "target", "target_label"},
                new String[]{"https://example.org/a", label, "https://example.org/b", label}), PAGE_URL);

        String denseCaption = nodeById(dense, "https://example.org/n0").get("label").getAsString();
        String sparseCaption = nodeById(sparse, "https://example.org/a").get("label").getAsString();
        assertEquals(label, sparseCaption);
        assertTrue(denseCaption.length() < sparseCaption.length(), denseCaption);
        assertTrue(denseCaption.endsWith("..."), denseCaption);
        // Whatever the caption, the hover box still carries the full label.
        assertEquals(label, nodeById(dense, "https://example.org/n0").get("title").getAsString());
    }

    @Test
    void resultIsCutShortAtTheEdgeLimit() {
        String[][] rows = new String[NetworkGraph.MAX_EDGES + 10][];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new String[]{"https://example.org/n" + i, "https://example.org/n" + (i + 1)};
        }
        NetworkGraph.Graph graph = NetworkGraph.build(
                response(new String[]{"source", "target"}, rows), PAGE_URL);

        assertEquals(NetworkGraph.MAX_EDGES, graph.edgeCount());
        assertTrue(graph.truncated());
    }

    @Test
    void aResultWithinTheLimitIsNotReportedAsCutShort() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", "https://example.org/b"}), PAGE_URL);

        assertFalse(graph.truncated());
    }

    @Test
    void emptyResultIsAnEmptyGraph() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(new String[]{"source", "target"}), PAGE_URL);

        assertTrue(graph.isEmpty());
        assertEquals(0, graph.edgeCount());
        assertEquals(0, nodes(graph).size());
    }

    /**
     * The JSON is embedded in a script block, so a label that looks like markup must not
     * be able to close it. Gson's HTML-safe escaping is what prevents that, and this is the
     * assertion that would notice if it were ever turned off.
     */
    @Test
    void labelCannotCloseTheScriptBlockItIsEmbeddedIn() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_label", "target"},
                new String[]{"https://example.org/a", "</script><script>alert(1)</script>",
                        "https://example.org/b"}), PAGE_URL);

        assertFalse(graph.json().contains("</script>"), "raw </script> in " + graph.json());
        assertTrue(graph.json().contains("\\u003c"));
        // Still carried as data: it is the page that must not execute it, not the label
        // that must disappear.
        assertTrue(nodeById(graph, "https://example.org/a").get("label").getAsString().contains("alert(1)"));
    }

    @Test
    void worksWithoutAUrlResolver() {
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", "https://example.org/b"}), null);

        assertEquals(2, graph.nodeCount());
        assertNull(stringOrNull(nodeById(graph, "https://example.org/a"), "url"));
    }

    @Test
    void nodeTooltipKeepsTheFullLabelTheCaptionHadToCut() {
        String longLabel = "A label that goes on well past the sixty characters a caption is given before it is cut";
        NetworkGraph.Graph graph = NetworkGraph.build(response(
                new String[]{"source", "source_label", "target"},
                new String[]{"https://example.org/a", longLabel, "https://example.org/b"}), PAGE_URL);

        JsonObject node = nodeById(graph, "https://example.org/a");
        assertTrue(node.get("label").getAsString().endsWith("..."));
        assertEquals(longLabel, node.get("title").getAsString());
    }

}
