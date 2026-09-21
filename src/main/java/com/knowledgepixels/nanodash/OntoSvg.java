package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Map.entry;

/**
 * Serializes an RDF description of an SVG image, as produced by a SPARQL CONSTRUCT
 * view query, into SVG markup (issue #592).
 *
 * <p>The RDF follows the <a href="https://github.com/floresbakker/OntoSVG">OntoSVG</a>
 * model: an element is a resource whose {@code rdf:type} carries an {@code xml:tag}
 * name in the SVG vocabulary, its attributes are the vocabulary properties holding
 * literals, its children are the numbered container-membership properties
 * ({@code rdf:_1}, {@code rdf:_2}, ...), and character data is a node whose class has
 * no tag, carrying its string in {@code xml:fragment}.
 *
 * <p>The markup produced here is not trusted: it is handed to
 * {@link Utils#sanitizeSvg(String)} before rendering, exactly like the markup a
 * non-CONSTRUCT SVG view returns in its {@code svg} column.
 */
public class OntoSvg {

    /** Namespace of the OntoSVG element and attribute vocabulary. */
    public static final String SVG_NAMESPACE = "http://www.w3.org/SVG/model/def/";

    /** Namespace of the OntoSVG XML vocabulary, which carries {@code xmlns} and text fragments. */
    public static final String XML_NAMESPACE = "http://www.w3.org/XML/model/def/";

    /** Namespace of the OntoSVG XLink vocabulary, which carries SVG 1.1 links. */
    public static final String XLINK_NAMESPACE = "https://www.w3.org/1999/xlink/model/def/";

    private static final String FRAGMENT_PROPERTY = XML_NAMESPACE + "fragment";
    private static final String XMLNS_PROPERTY = XML_NAMESPACE + "xmlns";
    private static final String XLINK_HREF_PROPERTY = XLINK_NAMESPACE + "href";
    private static final String MEMBERSHIP_PREFIX = RDF.NAMESPACE + "_";
    private static final String ROOT_CLASS = SVG_NAMESPACE + "Svg";

    // Guards against a graph whose membership properties form a cycle, which would
    // otherwise recurse until the stack gives out. A figure nested this deeply is
    // past anything the sanitized subset can express.
    private static final int MAX_DEPTH = 64;

    // The element name for each OntoSVG class, taken from the xml:tag values in the
    // vocabulary's "svg - core" specification. Not derivable from the class name:
    // svg:TextElement is <text> (svg:Text is character data, not an element), and
    // the font and colour-profile classes hyphenate.
    private static final Map<String, String> TAG_NAMES = Map.ofEntries(
            entry("A", "a"),
            entry("AltGlyph", "altGlyph"),
            entry("AltGlyphDef", "altGlyphDef"),
            entry("AltGlyphItem", "altGlyphItem"),
            entry("Animate", "animate"),
            entry("AnimateColor", "animateColor"),
            entry("AnimateMotion", "animateMotion"),
            entry("AnimateTransform", "animateTransform"),
            entry("Circle", "circle"),
            entry("ClipPath", "clipPath"),
            entry("ColorProfile", "color-profile"),
            entry("Cursor", "cursor"),
            entry("Defs", "defs"),
            entry("Desc", "desc"),
            entry("Ellipse", "ellipse"),
            entry("FeBlend", "feBlend"),
            entry("FeColorMatrix", "feColorMatrix"),
            entry("FeComponentTransfer", "feComponentTransfer"),
            entry("FeComposite", "feComposite"),
            entry("FeConvolveMatrix", "feConvolveMatrix"),
            entry("FeDiffuseLighting", "feDiffuseLighting"),
            entry("FeDisplacementMap", "feDisplacementMap"),
            entry("FeDistantLight", "feDistantLight"),
            entry("FeFlood", "feFlood"),
            entry("FeFuncA", "feFuncA"),
            entry("FeFuncB", "feFuncB"),
            entry("FeFuncG", "feFuncG"),
            entry("FeFuncR", "feFuncR"),
            entry("FeGaussianBlur", "feGaussianBlur"),
            entry("FeImage", "feImage"),
            entry("FeMerge", "feMerge"),
            entry("FeMergeNode", "feMergeNode"),
            entry("FeMorphology", "feMorphology"),
            entry("FeOffset", "feOffset"),
            entry("FePointLight", "fePointLight"),
            entry("FeSpecularLighting", "feSpecularLighting"),
            entry("FeSpotLight", "feSpotLight"),
            entry("FeTile", "feTile"),
            entry("FeTurbulence", "feTurbulence"),
            entry("Filter", "filter"),
            entry("Font", "font"),
            entry("FontFace", "font-face"),
            entry("FontFaceFormat", "font-face-format"),
            entry("FontFaceName", "font-face-name"),
            entry("FontFaceSrc", "font-face-src"),
            entry("FontFaceUri", "font-face-uri"),
            entry("ForeignObject", "foreignObject"),
            entry("G", "g"),
            entry("Glyph", "glyph"),
            entry("GlyphRef", "glyphRef"),
            entry("Hkern", "hkern"),
            entry("Image", "image"),
            entry("Line", "line"),
            entry("LinearGradient", "linearGradient"),
            entry("Marker", "marker"),
            entry("Mask", "mask"),
            entry("Metadata", "metadata"),
            entry("MissingGlyph", "missing-glyph"),
            entry("Mpath", "mpath"),
            entry("Path", "path"),
            entry("Pattern", "pattern"),
            entry("Polygon", "polygon"),
            entry("Polyline", "polyline"),
            entry("RadialGradient", "radialGradient"),
            entry("Rect", "rect"),
            entry("Script", "script"),
            entry("Set", "set"),
            entry("Stop", "stop"),
            entry("Style", "style"),
            entry("Svg", "svg"),
            entry("Switch", "switch"),
            entry("Symbol", "symbol"),
            entry("TextElement", "text"),
            entry("TextPath", "textPath"),
            entry("Title", "title"),
            entry("Tref", "tref"),
            entry("Tspan", "tspan"),
            entry("Use", "use"),
            entry("View", "view"),
            entry("Vkern", "vkern")
    );

    private OntoSvg() {
    }

    /**
     * Serializes every SVG image described in the given model, outermost images only,
     * in a stable order.
     *
     * @param model the RDF description of zero or more SVG images
     * @return the SVG markup of each image, one string per image
     */
    public static List<String> toSvgMarkup(Model model) {
        List<String> figures = new ArrayList<>();
        if (model == null) return figures;
        for (Resource root : findRoots(model)) {
            StringBuilder markup = new StringBuilder();
            appendNode(model, root, markup, 0, new HashSet<>());
            if (!markup.isEmpty()) figures.add(markup.toString());
        }
        return figures;
    }

    // The outermost svg:Svg nodes: those not contained in another one, so that an <svg>
    // nested inside a figure is rendered once, in its place, rather than a second time as
    // a figure of its own. Being someone's child is not the test -- the vocabulary wraps a
    // whole document in an svg:Document holding the doctype and the svg:Svg, and that
    // wrapper's child is still the figure's root.
    private static List<Resource> findRoots(Model model) {
        List<Resource> candidates = new ArrayList<>();
        for (Statement st : model.filter(null, RDF.TYPE, null)) {
            if (!ROOT_CLASS.equals(st.getObject().stringValue())) continue;
            if (!candidates.contains(st.getSubject())) candidates.add(st.getSubject());
        }
        Set<Resource> nested = new HashSet<>();
        for (Resource candidate : candidates) {
            collectNestedImages(model, candidate, nested, new HashSet<>());
        }
        List<Resource> roots = new ArrayList<>(candidates);
        roots.removeAll(nested);
        // A model is an unordered set of statements, so without this a query returning
        // several figures would render them in a different order on each fetch.
        roots.sort(Comparator.comparing(Value::stringValue));
        return roots;
    }

    private static void collectNestedImages(Model model, Resource node, Set<Resource> nested, Set<Resource> visited) {
        if (!visited.add(node)) return;
        for (Resource child : childrenOf(model, node)) {
            if (isImage(model, child)) nested.add(child);
            collectNestedImages(model, child, nested, visited);
        }
    }

    private static boolean isImage(Model model, Resource node) {
        for (Statement st : model.filter(node, RDF.TYPE, null)) {
            if (ROOT_CLASS.equals(st.getObject().stringValue())) return true;
        }
        return false;
    }

    private static void appendNode(Model model, Resource node, StringBuilder out, int depth, Set<Resource> ancestors) {
        if (depth > MAX_DEPTH || !ancestors.add(node)) return;
        try {
            String tag = tagOf(model, node);
            if (tag == null) {
                out.append(escape(literalOf(model, node, FRAGMENT_PROPERTY)));
                return;
            }
            out.append('<').append(tag);
            for (Map.Entry<String, String> attribute : attributesOf(model, node).entrySet()) {
                out.append(' ').append(attribute.getKey()).append("=\"").append(escape(attribute.getValue())).append('"');
            }
            out.append('>');
            for (Resource child : childrenOf(model, node)) {
                appendNode(model, child, out, depth + 1, ancestors);
            }
            out.append("</").append(tag).append('>');
        } finally {
            ancestors.remove(node);
        }
    }

    // Null for anything that is not an element -- character data, and any class the
    // vocabulary gives no tag name.
    private static String tagOf(Model model, Resource node) {
        for (Statement st : model.filter(node, RDF.TYPE, null)) {
            String type = st.getObject().stringValue();
            if (!type.startsWith(SVG_NAMESPACE)) continue;
            String tag = TAG_NAMES.get(type.substring(SVG_NAMESPACE.length()));
            if (tag != null) return tag;
        }
        return null;
    }

    private static Map<String, String> attributesOf(Model model, Resource node) {
        // Sorted, because the statements arrive unordered and the same figure should
        // produce the same markup every time it is rendered.
        Map<String, String> attributes = new TreeMap<>();
        for (Statement st : model.filter(node, null, null)) {
            if (!(st.getObject() instanceof org.eclipse.rdf4j.model.Literal)) continue;
            String name = attributeNameOf(st.getPredicate());
            if (name != null) attributes.put(name, st.getObject().stringValue());
        }
        return reorderXmlnsFirst(attributes);
    }

    // The attribute's name is the local name of its property, per the vocabulary, where
    // every attribute's xml:key matches it. Qualified with a prefix only when it comes
    // from another namespace than the element, which for the sanitized subset leaves
    // only the link: OntoSVG models SVG 1.1, whose xlink:href is written href in SVG 2
    // -- and href is the spelling that survives sanitization and that browsers follow.
    private static String attributeNameOf(IRI predicate) {
        String iri = predicate.stringValue();
        if (XLINK_HREF_PROPERTY.equals(iri)) return "href";
        if (XMLNS_PROPERTY.equals(iri)) return "xmlns";
        if (FRAGMENT_PROPERTY.equals(iri)) return null;
        if (iri.startsWith(SVG_NAMESPACE)) return iri.substring(SVG_NAMESPACE.length());
        return null;
    }

    private static Map<String, String> reorderXmlnsFirst(Map<String, String> attributes) {
        String xmlns = attributes.remove("xmlns");
        if (xmlns == null) return attributes;
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("xmlns", xmlns);
        ordered.putAll(attributes);
        return ordered;
    }

    private static List<Resource> childrenOf(Model model, Resource node) {
        Map<Integer, Resource> byPosition = new TreeMap<>();
        for (Statement st : model.filter(node, null, null)) {
            if (!isMembershipProperty(st.getPredicate())) continue;
            if (!(st.getObject() instanceof Resource child)) continue;
            Integer position = positionOf(st.getPredicate());
            // Numerically, so that rdf:_10 follows rdf:_9 instead of rdf:_1.
            if (position != null) byPosition.put(position, child);
        }
        return new ArrayList<>(byPosition.values());
    }

    private static boolean isMembershipProperty(IRI predicate) {
        return predicate.stringValue().startsWith(MEMBERSHIP_PREFIX);
    }

    private static Integer positionOf(IRI predicate) {
        try {
            return Integer.valueOf(predicate.stringValue().substring(MEMBERSHIP_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String literalOf(Model model, Resource node, String property) {
        for (Statement st : model.filter(node, null, null)) {
            if (property.equals(st.getPredicate().stringValue())) return st.getObject().stringValue();
        }
        return "";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

}
