# SVG views

An **SVG view** (`gen:SvgView`) is a resource-view display type whose query returns
ready-to-embed SVG markup, rendered inline on the page. Where the other display
types lay out the query's rows as a table, list, or paragraphs, an SVG view's query
computes the *visual itself* — e.g. a diagram laid out in SPARQL from the underlying
data — and Nanodash only sanitizes and embeds it. The query API needs no changes for
this: the SVG travels as an ordinary result-cell string.

The query can supply that figure in either of two ways: as **markup**, in an `svg`
result column, or as **RDF**, from a CONSTRUCT query describing the image in the
OntoSVG vocabulary. Both end up in the same sanitizer and the same container.

## View declaration

Exactly like any other view, with `gen:SvgView` as the display type:

```turtle
sub:my-svg-view a gen:ResourceView, gen:SvgView ;
  dct:isVersionOf sub:my-svg-view-kind ;
  rdfs:label "My typology diagram view" ;
  dct:title "🌳 Typology" ;
  gen:hasViewQuery <query-np-uri> ;
  gen:hasViewQueryTargetField "resource" ;
  gen:appliesToInstancesOf gen:MaintainedResource .
```

Display/preset attachment, structural position, display width, governed versions,
and view actions all work as for the other view types. Older Nanodash instances
without SVG-view support skip the view silently (unknown display type), and the
document export renders a "(view type not supported in document export)" note.

## Query contract (markup)

- **`svg`** — the complete SVG markup (`<svg ...>...</svg>`), one rendered figure
  per result row. Typically such a query returns a single row.
- **`title`** (optional) — a heading rendered above the figure, as in plain-paragraph
  views.
- **`np`** (optional) — the source nanopub, linked from the figure's dropdown menu.

Entry actions and their query mappings work per row as usual.

## RDF figures (CONSTRUCT queries)

A view query may instead be a SPARQL **CONSTRUCT** that describes the figure as RDF, in
the [OntoSVG](https://github.com/floresbakker/OntoSVG) vocabulary. Nanodash serializes
that graph to SVG markup and then sanitizes and renders it exactly as above, so nothing
else about the view declaration changes — the display type stays `gen:SvgView`.

This is the better option whenever the figure is assembled from many parts. Building
markup as a string means `group_concat`, whose ordering is not dependable (see the
authoring notes below); describing the elements as RDF lets the numbered child
properties carry the order instead, and leaves the escaping to Nanodash.

```sparql
CONSTRUCT {
  ?figure a svg:Svg ;
    xml:xmlns "http://www.w3.org/2000/svg" ;
    svg:width "200" ; svg:height "200" ;
    rdf:_1 ?box .
  ?box a svg:Rect ;
    svg:x "10" ; svg:y "10" ; svg:width "180" ; svg:height "40" ;
    svg:fill "#eef" ;
    rdf:_1 ?caption .
  ?caption a svg:TextElement ;
    svg:x "20" ; svg:y "35" ;
    rdf:_1 ?captionText .
  ?captionText a svg:Text ; xml:fragment ?label .
} WHERE { ... }
```

The vocabulary namespaces are `svg: <http://www.w3.org/SVG/model/def/>`,
`xml: <http://www.w3.org/XML/model/def/>` and
`xlink: <https://www.w3.org/1999/xlink/model/def/>`.

### How the graph is read

- **Elements** are resources whose `rdf:type` is an OntoSVG class — `svg:Rect`,
  `svg:Circle`, `svg:G`. The element name comes from the vocabulary, not from the class
  name: most are simply lower-cased, but `svg:ColorProfile` is `color-profile`, the
  `svg:FontFace*` classes hyphenate, and see the warning about `svg:Text` below.
- **Attributes** are the `svg:` properties holding literals: `svg:cx "100"`,
  `svg:stroke-width "3"`. The attribute's name is the property's local name.
- **Children** are the numbered properties `rdf:_1`, `rdf:_2`, … , read in *numeric*
  order, so `rdf:_10` follows `rdf:_9`. Their order in the graph is irrelevant.
- **Text** is a node with no element class carrying its string in `xml:fragment`, as
  `svg:Text` does. It is XML-escaped on the way out, so query-derived labels need no
  escaping of their own.
- `xml:xmlns` becomes the `xmlns` attribute.
- `xlink:href` is written as `href`. OntoSVG models SVG 1.1, where links are
  `xlink:href`; `href` is the SVG 2 spelling, and the one that both survives
  sanitization and is followed by browsers.

> **`svg:Text` is not `<text>`.** In OntoSVG, `svg:Text` is an XML *text node* — the
> characters themselves — and the `<text>` element is **`svg:TextElement`**. A label is
> therefore an `svg:TextElement` with an `svg:Text` child holding the string. Typing the
> label node `svg:Text` directly renders nothing.

Anything the vocabulary does not define — an unknown class, a property from another
namespace — is left out rather than guessed at.

### What a CONSTRUCT view gives up

The tabular form carries one figure per row and reads `title` and `np` from the row's
other columns. A CONSTRUCT result has no columns, so a figure rendered this way has no
per-figure heading and no source link; the view's own title still shows. Each outermost
`svg:Svg` in the graph becomes one figure, in a stable order, and an `<svg>` nested
inside another is rendered in place rather than a second time on its own.

## Sanitization

The markup is sanitized server-side (`Utils.sanitizeSvg`) to a **static SVG
subset** before rendering; the query controls the visual but cannot inject
scripting or styling:

- **Elements**: `svg`, `g`, `defs`, `marker`, `title`, `desc`, `rect`, `circle`,
  `ellipse`, `line`, `polyline`, `polygon`, `path`, `text`, `tspan`, `a`.
- **Attributes**: geometry (`x`, `y`, `d`, `points`, `viewBox`, `transform`, …) and
  presentation (`fill`, `stroke`, `font-size`, `text-anchor`, `opacity`, …); `href`
  only on `a` and only with `http(s)` URLs.
- **Dropped**: `script`, `style` (element and attribute), event handlers,
  `foreignObject`, and external-reference elements (`use`, `image`).

Authoring notes:

- **Write explicit end tags** (`<rect ...></rect>`), not XML self-closing syntax
  (`<rect .../>`); inline SVG is parsed under HTML rules where the self-closing
  slash is ignored on these elements. The sanitizer normalizes self-closed tags as
  a safety net, but emitting end tags keeps the markup valid in both worlds.
- Use the **camelCase** spellings of SVG attributes (`viewBox`,
  `preserveAspectRatio`); they are matched case-sensitively and passed through
  verbatim.
- XML-escape all data-derived text (`&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`) in
  both text content and attribute values — the values come from published nanopub
  data, which is untrusted.
- Prefer **absolute coordinates** computed in the query (e.g. via rank subqueries)
  over document-order-dependent constructs: `group_concat` does not reliably
  preserve a subquery's ORDER BY, so output that depends on concatenation order
  renders nondeterministically.
- `<title>` children provide hover tooltips — useful when labels are truncated to
  fit fixed-width boxes.

The figure container (`.svg-view-content`) scales diagrams wider than their panel
down to fit (`max-width: 100%`), and text inside `a` links takes the page's link
color and hover color via CSS (overriding the SVG's own `fill` attributes, which
thus serve as the fallback for standalone rendering of the query output).

## Example

The first SVG-view query is "Get class typology diagram as SVG": given an
ontology-like maintained resource, it renders the subclass typology of the
resource's classes (root banner, branch group boxes in a two-column layout,
per-branch item lists with a second indented level), with all boxes and positions
computed in SPARQL and every label linked to its class IRI.
