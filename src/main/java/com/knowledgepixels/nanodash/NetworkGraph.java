package com.knowledgepixels.nanodash;

import com.google.gson.Gson;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Turns a network view's query result into the graph its browser-side renderer draws
 * (issue #792). The query returns one row per edge; this is where those rows become a
 * node set and an edge set.
 *
 * <p>Kept free of Wicket so that the whole translation can be tested without a page,
 * the same separation {@link OntoSvg} has. What the class cannot know on its own — which
 * page an IRI leads to, which depends on the navigation context — is injected as a
 * function.
 *
 * <p>The result is serialized with Gson, whose HTML-safe escaping is what makes it safe
 * to embed in a {@code <script>} block: a label carrying {@code </script>} comes out as
 * {@code </script>} and cannot close it. Nothing here produces markup — the
 * renderer puts every label and tooltip on the page as text.
 */
public class NetworkGraph {

    /**
     * The most edges a single network view draws. A force layout over more than this is
     * unreadable anyway, and the browser would spend seconds on it; a query that exceeds
     * it is better fixed than rendered, so the view says it was cut short.
     */
    public static final int MAX_EDGES = 1000;

    /**
     * The colours nodes are grouped by, assigned in the order the types are first seen,
     * so a given query keeps its colouring from one page load to the next. Chosen to stay
     * apart for the common forms of colour blindness and to sit calmly on the page.
     */
    private static final String[] PALETTE = {
            "#4878a8", "#d08a3e", "#5a9e6f", "#b4556b",
            "#7a6aa8", "#4f9aa8", "#8a6a4f", "#9a9a3e",
    };

    /**
     * The colour of a node whose row declares no type, and of every node when the query
     * has no type columns at all.
     */
    private static final String UNTYPED_COLOR = "#8c8c8c";

    /**
     * Past this many nodes, full captions crowd each other out in the middle of the
     * diagram, so they are cut shorter than a link label would be. Nothing is lost: the
     * hover box carries the whole label either way.
     */
    private static final int DENSE_NODE_COUNT = 25;

    /**
     * How much of a caption survives in a dense diagram.
     */
    private static final int DENSE_CAPTION_CHARS = 30;

    private NetworkGraph() {
    }

    /**
     * A built graph: the JSON its renderer reads, plus what the view needs to say about it.
     *
     * @param json      the graph as JSON, ready to embed in a script block
     * @param nodeCount the number of distinct nodes
     * @param edgeCount the number of edges drawn
     * @param truncated whether the result was cut short at {@link #MAX_EDGES}
     * @param legend    the node types and their colours, in assignment order; empty when
     *                  the query declares no types
     */
    public record Graph(String json, int nodeCount, int edgeCount, boolean truncated,
                        List<LegendEntry> legend) {

        /**
         * Whether there is anything to draw.
         *
         * @return true if the graph has no nodes
         */
        public boolean isEmpty() {
            return nodeCount == 0;
        }
    }

    /**
     * One entry of the colour legend shown under a network.
     *
     * @param type  the node type as the query reported it
     * @param color the colour its nodes are drawn in
     */
    public record LegendEntry(String type, String color) {
    }

    // The shapes below are serialized straight to JSON, so their field names are the
    // renderer's contract; vis-network reads "from"/"to" on an edge and "group" on a node.
    private record Node(String id, String label, String title, String color, String url) {
    }

    private record Edge(String from, String to, String label, String title, String url) {
    }

    private record GraphData(List<Node> nodes, List<Edge> edges) {
    }

    /**
     * Builds the graph of a network view's query result, naming nodes from the rows alone.
     *
     * @param response the query result, one row per edge
     * @param pageUrl  the page an IRI leads to, or null for an IRI that leads nowhere
     * @return the built graph
     */
    public static Graph build(ApiResponse response, UnaryOperator<String> pageUrl) {
        return build(response, pageUrl, null);
    }

    /**
     * Builds the graph of a network view's query result.
     *
     * @param response   the query result, one row per edge
     * @param pageUrl    the page an IRI leads to, or null for an IRI that leads nowhere
     * @param knownLabel a label Nanodash already knows for an IRI, consulted where the
     *                   query supplies none, or null to go straight to the short name
     * @return the built graph
     */
    public static Graph build(ApiResponse response, UnaryOperator<String> pageUrl,
                              UnaryOperator<String> knownLabel) {
        Map<String, NodeBuilder> nodes = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>();
        Set<String> seenEdges = new LinkedHashSet<>();
        Map<String, String> colorByType = new LinkedHashMap<>();
        boolean truncated = false;

        for (ApiResponseEntry row : response.getData()) {
            String source = trimmed(row.get("source"));
            String target = trimmed(row.get("target"));
            // An edge needs both ends. A row that has only one of them describes nothing
            // this view can draw, so it is passed over rather than drawn half.
            if (source == null || target == null) continue;
            if (edges.size() >= MAX_EDGES) {
                truncated = true;
                break;
            }

            String edgeIri = trimmed(row.get("edge"));
            String edgeLabel = trimmed(row.get("edge_label"));
            String npId = trimmed(row.get("np"));
            // One line per edge, so repeated rows (a relation asserted twice, or reported
            // once per nanopublication) do not stack into a thicker-looking one.
            String key = source + "\u0000" + target + "\u0000" + (edgeIri == null ? edgeLabel : edgeIri);
            if (!seenEdges.add(key)) continue;

            node(nodes, source, trimmed(row.get("source_label")), trimmed(row.get("source_type")));
            node(nodes, target, trimmed(row.get("target_label")), trimmed(row.get("target_type")));

            String caption = edgeLabel != null ? edgeLabel
                    : (edgeIri != null ? Utils.getShortNameFromURI(edgeIri) : null);
            // The relation's own page when the query names it, else the nanopublication
            // that asserts the edge — so a "where does this come from?" click lands
            // somewhere useful either way.
            String clickTarget = edgeIri != null ? edgeIri : npId;
            edges.add(new Edge(source, target, Utils.truncateLinkLabel(caption),
                    edgeTitle(caption, edgeIri, npId),
                    clickTarget == null || pageUrl == null ? null : pageUrl.apply(clickTarget)));
        }

        List<Node> nodeList = new ArrayList<>(nodes.size());
        boolean dense = nodes.size() > DENSE_NODE_COUNT;
        for (NodeBuilder nb : nodes.values()) {
            // A query that names its nodes decides what they are called. Where it does not,
            // the IRI may still be one Nanodash can name — a space, a user, a maintained
            // resource — and a node reads better as that than as the tail of its IRI.
            if (nb.label == null && knownLabel != null) nb.label = trimmed(knownLabel.apply(nb.id));
            String color = UNTYPED_COLOR;
            if (nb.type != null) {
                color = colorByType.get(nb.type);
                if (color == null) {
                    color = PALETTE[colorByType.size() % PALETTE.length];
                    colorByType.put(nb.type, color);
                }
            }
            nodeList.add(new Node(nb.id,
                    caption(nb.label != null ? nb.label : Utils.getShortNameFromURI(nb.id), dense),
                    nodeTitle(nb),
                    color,
                    pageUrl == null ? null : pageUrl.apply(nb.id)));
        }

        List<LegendEntry> legend = new ArrayList<>(colorByType.size());
        colorByType.forEach((type, color) -> legend.add(new LegendEntry(type, color)));

        String json = new Gson().toJson(new GraphData(nodeList, edges));
        return new Graph(json, nodeList.size(), edges.size(), truncated, legend);
    }

    // The caption under a node: as much of the label as the diagram has room for.
    private static String caption(String label, boolean dense) {
        if (dense && label != null && label.length() > DENSE_CAPTION_CHARS) {
            return label.substring(0, DENSE_CAPTION_CHARS - 3).stripTrailing() + "...";
        }
        return Utils.truncateLinkLabel(label);
    }

    private static void node(Map<String, NodeBuilder> nodes, String iri, String label, String type) {
        NodeBuilder nb = nodes.computeIfAbsent(iri, NodeBuilder::new);
        // First non-empty value wins: a node reached by several edges is named and typed
        // by the first row that knows, and later rows cannot blank it out.
        if (nb.label == null) nb.label = label;
        if (nb.type == null) nb.type = type;
    }

    // The tooltip is plain text, shown over several lines. It carries the full label, which
    // the caption may have had to cut, and always the IRI, so a node can be identified
    // without clicking through.
    private static String nodeTitle(NodeBuilder nb) {
        StringBuilder sb = new StringBuilder();
        if (nb.label != null) sb.append(nb.label).append("\n");
        sb.append(nb.id);
        if (nb.type != null) sb.append("\n").append(nb.type);
        return sb.toString();
    }

    private static String edgeTitle(String caption, String edgeIri, String npId) {
        StringBuilder sb = new StringBuilder();
        if (caption != null) sb.append(caption);
        if (edgeIri != null) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(edgeIri);
        }
        if (npId != null) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("source: ").append(npId);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String trimmed(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class NodeBuilder {
        private final String id;
        private String label;
        private String type;

        private NodeBuilder(String id) {
            this.id = id;
        }
    }

}
