# SVG views

An **SVG view** (`gen:SvgView`) is a resource-view display type whose query returns
ready-to-embed SVG markup, rendered inline on the page. Where the other display
types lay out the query's rows as a table, list, or paragraphs, an SVG view's query
computes the *visual itself* — e.g. a diagram laid out in SPARQL from the underlying
data — and Nanodash only sanitizes and embeds it. The query API needs no changes for
this: the SVG travels as an ordinary result-cell string.

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

## Query contract

- **`svg`** — the complete SVG markup (`<svg ...>...</svg>`), one rendered figure
  per result row. Typically such a query returns a single row.
- **`title`** (optional) — a heading rendered above the figure, as in plain-paragraph
  views.
- **`np`** (optional) — the source nanopub, linked from the figure's dropdown menu.

Entry actions and their query mappings work per row as usual.

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
down to fit (`max-width: 100%`).

## Example

The first SVG-view query is "Get class typology diagram as SVG": given an
ontology-like maintained resource, it renders the subclass typology of the
resource's classes (root banner, branch group boxes in a two-column layout,
per-branch item lists with a second indented level), with all boxes and positions
computed in SPARQL and every label linked to its class IRI.
