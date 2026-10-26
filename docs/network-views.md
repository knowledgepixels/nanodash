# Network views

A **network view** (`gen:NetworkView`) is a resource-view display type that draws its
query result as an interactive node-link diagram: nodes and edges are laid out in the
browser, can be hovered for detail, dragged around, and lead to their resource pages on
click ([#792](https://github.com/knowledgepixels/nanodash/issues/792)).

It is the counterpart to an [SVG view](svg-views.md). There, the query computes the
figure — coordinates and all — and Nanodash only embeds it. Here the query reports
*relations*, and the layout is computed where it can be interactive. A graph whose exact
arrangement matters is an SVG view; a graph whose structure is the point is a network
view.

## View declaration

Exactly like any other view, with `gen:NetworkView` as the display type:

```turtle
sub:viewVersion a gen:ResourceView, gen:NetworkView ;
  dct:isVersionOf sub:my-network-view ;
  rdfs:label "My space structure network view" ;
  dct:title "🕸 Structure" ;
  gen:hasViewQuery <query-np-uri> ;
  gen:hasViewQueryTargetField "resource" ;
  gen:appliesToInstancesOf gen:Space ;
  gen:hasStructuralPosition "5.5.network" .
```

Display/preset attachment, structural position, display width, governed versions and
view-level actions all work as for the other display types. Older Nanodash instances
without network-view support skip the view silently (unknown display type).

## Query contract

The query returns **one row per edge**:

```sparql
SELECT ?source ?source_label ?source_desc ?source_type ?source_type_label
       ?edge_type ?edge_label ?edge_desc
       ?target ?target_label ?target_desc ?target_type ?target_type_label
       ?np ?np_label
WHERE { ... }
```

The edge columns sit between the source and the target columns, so that a row reads the
way the edge does — source, relation, target — wherever the rows are shown as rows, as in
the document export.

| column | required | meaning |
| --- | --- | --- |
| `source`, `target` | yes | the two node IRIs. Nodes are deduplicated across rows, so a node reached by five edges is drawn once |
| `source_label`, `target_label` | no | the node's caption, and its hover box where no `_desc` gives one. The first non-empty value for a node wins, so a later row cannot rename it. A node the query leaves unnamed falls back to the label Nanodash already knows for that IRI — a user's display name, a space's or a maintained resource's label — and then, for the caption only, to the short name of the IRI |
| `source_desc`, `target_desc` | no | what the node's hover box says, for what a caption has no room to say. Defaults to the node's label, and like the label it is taken from the first row that has one. A node with neither has no hover box |
| `source_type`, `target_type` | no | groups nodes by colour, with a legend under the diagram. Colours are assigned in the order the types are first seen |
| `source_type_label`, `target_type_label` | no | what the legend calls the type; without one it uses the short name of the type's IRI. The first row that names a type decides, and a row naming it differently later is passed over rather than reconciled |
| `edge_type` | no | the relation's IRI: named in the status line while the pointer is on the edge, and the edge's caption where the query gives no `?edge_label`. Edges are all drawn alike — only nodes are grouped by colour |
| `edge_label` | no | the edge's caption, and its hover box where no `?edge_desc` gives one; without a label, an `?edge_type` is captioned with its short name |
| `edge_desc` | no | what the edge's hover box says, defaulting to `?edge_label`. An edge with neither has no hover box |
| `np` | no | the nanopublication asserting the edge: where a click on the edge leads |

A row that names only one of `source`/`target` describes no edge and is passed over. A
relation asserted by several nanopublications is drawn as one edge, not as a bundle, and a
click on it opens the first nanopublication the result named for it.

This is the house `_label` convention, so the companion columns behave as they do in a
table: `?x_label` always sits beside its `?x`. No change to the query API is needed —
the graph travels as ordinary result cells.

## Interaction

- **Hovering** a node shows what the query says about it, and only that: its
  `?source_desc`/`?target_desc` where there is one, otherwise its full label, which the
  caption may have had to cut. An edge behaves the same way, with `?edge_desc` and
  `?edge_label`. Where the query says neither, nothing opens — a box repeating the caption
  is worth less than no box at all.
- **The status line** in the bottom-left corner of the diagram names what the pointer is
  on, the way a browser names the target of the link under the cursor: a node's own IRI, an
  edge's `?edge_type`, and — pointing at the legend below the diagram — the IRI of the type
  that entry stands for. It is how an IRI is read off the diagram now that the hover box
  carries words rather than identifiers, and it is where to look when a caption is
  ambiguous — two spaces of the same name, say. An edge the query gives no type leaves it
  empty.
- **Clicking** a node opens its page, routed exactly as a link in a table cell would be
  (part page, space, user, maintained resource, or Explore), carrying this view's
  navigation context so the destination's back-link leads back here. Clicking an edge
  opens the nanopublication the edge was read from — the answer to the question an edge
  raises, which is who says so.
- **Dragging** a node rearranges the layout around it: the simulation wakes up and the
  node's neighbours follow it, so a graph can be pulled apart to read a crowded corner.
  Moving nodes around never navigates away: a click is acted on when the button comes up,
  and a gesture that dragged something is not one.
- **The layout happens on screen.** Only a short pre-settle is computed before the first
  frame — enough to pull the worst crossings out — and the rest of it runs in view, with the
  diagram zooming out to keep up as the nodes spread. That the layout is being computed is
  then something the reader can see, rather than something a blank box hides. The view stops
  following the graph as soon as the reader touches it, and the simulation stops itself once
  the layout has come to rest, so an open page with several network views is not left
  spinning; a graph too dense ever to come to rest is stopped after a few seconds, its nodes
  left where they are and still draggable. A reader whose system asks for reduced motion gets
  the whole layout computed before the first frame instead, as before.

## Limits

- **No per-row entry actions.** A `gen:ViewEntryAction` needs a row of its own to hang a
  menu on, and a canvas has none. The view's `gen:ViewResultAction`s work as everywhere
  else, in the view's dropdown menu.
- **No CONSTRUCT queries.** The query returns rows, not a graph. (An RDF-graph input
  would be a natural second source — the edges are triples, after all — and would fit
  behind the same fork SVG views use for OntoSVG; it is simply not built.)
- **`gen:hasPageSize` has no effect**: there are no pages to turn. At most 1000 edges are
  drawn, and the view says so when a result was cut short.
- **Not in the document export as a diagram.** A layout computed in the browser cannot
  travel into an RTF or PDF, so the export lists the edges as a table instead — every
  column of them, the `_desc` ones included, since a table has nothing to hover.

## Authoring notes

- **Keep the result small.** A force layout over a few hundred edges is already dense;
  past that it is a hairball, whatever the library does. Aggregate or scope in SPARQL
  rather than leaning on the 1000-edge cap, which is a backstop, not a setting. A graph of
  several hundred nodes also never comes to rest at all: it is still drifting when the
  layout is stopped after a few seconds, so what such a view settles on is wherever it had
  got to, not a finished arrangement.
- **Label every node the query can.** Nanodash names users, spaces and maintained
  resources by itself, but anything else falls back to the short name of its IRI, which for
  many vocabularies is a code. Hand the `*_label` columns the same labels a table would show.
- **Type the nodes when the graph mixes kinds of thing.** A space, its sub-spaces, its
  maintained resources and its members read very differently in four colours than in one.
  The legend falls back to each type's short name, so a type IRI is as good as a word here —
  and `?source_type_label`/`?target_type_label` give it a better one where the short name
  reads like a class name rather than like a word.
- **Type the edges when the relation matters.** `?edge_type` is what the status line names
  while the pointer is on an edge, and what captions an edge the query does not label. Captions on the lines are
  dropped above a few dozen edges, where they overlap each other and the nodes, so in a
  dense graph the relation is a hover away rather than on the page.
- **Say the rest on hover.** The `_desc` columns are read only there, so a node can be
  captioned with a short name and still explain itself in full when pointed at: a person's
  affiliation, a space's subtitle, what a relation meant when it was asserted. The hover box
  is text, not markup, and keeps the line breaks a description brings with it.
- **Time the query.** Like any view query it runs on every page load; the same rules apply
  as elsewhere (start from the most selective pattern, avoid `optional { { select … } }`,
  which is re-evaluated per outer binding).
- A node's IRI need not be anything Nanodash knows: an external vocabulary term is drawn
  like any other node and links to its Explore page.

## How it reaches the page

The server turns the rows into a node set and an edge set
(`com.knowledgepixels.nanodash.NetworkGraph`) and embeds them as JSON in a
`<script type="application/json">` block inside the view's container;
`script/network.js` reads it and draws the diagram with
[vis-network](https://visjs.github.io/vis-network/docs/network/), loaded from a webjar
with the view — or, while a cold query is still running, with the placeholder waiting in its
place, so that the engine and the query are not waited for one after the other. Either way a
page without a network view loads neither the library nor the script. Only the library's
script is loaded; its stylesheet is 220 KB of mostly base64 icons for a navigation bar, an
editing toolbar and a configuration panel that a network view does not turn on, so the one
rule its popup needs lives in `style.css` instead.

Nothing in the graph is markup. Labels and tooltips are put on the page as text —
the JSON is escaped HTML-safe, so a label containing `</script>` cannot close the block
it sits in, and tooltips are built as elements with their text set as text rather than
handed to the library as strings, which it would write with `innerHTML`. This is also why
a network cannot be delivered through a query's HTML cell: the sanitizer drops `script`
elements and `data-*` attributes alike, as it should.

## Example

The first network-view query is "Get the network of a space, its sub-spaces and their
members": given a space, it returns the space's parent, its sub-spaces at any depth, and
everyone holding a validated role in any of them, with the role tier as the relation. It
runs against the `spaces` repo's current validated space state, and is the kind of result
a network shows better than a table: an agent who holds roles in several sub-spaces ties
those spaces together, which a list of rows leaves for the reader to work out.

```sparql
select distinct ?source ?source_label ?source_type ?edge_type ?edge_label
       ?target ?target_label ?target_type ?np ('^' as ?np_label)
where {
  graph npa:graph { <http://purl.org/nanopub/admin/thisRepo> npa:hasCurrentSpaceState ?g . }
  {
    graph ?g { ?_resource_iri npa:hasSubSpace* ?space . ?space npa:hasSubSpace ?child . }
    bind(?space as ?source) bind(?child as ?target)
    bind(gen:Space as ?source_type) bind(gen:Space as ?target_type)
    bind(npa:hasSubSpace as ?edge_type) bind("has sub-space" as ?edge_label)
  } union {
    graph ?g {
      ?_resource_iri npa:hasSubSpace* ?space .
      ?ri a gen:RoleInstantiation ; npa:forSpace ?space ;
        npa:forAgent ?agent ; npa:hasRoleType ?tier ; npa:viaNanopub ?grantNp .
    }
    optional { graph ?g { ?agent foaf:name ?agentName . } }
    bind(?space as ?source) bind(?agent as ?target)
    bind(gen:Space as ?source_type) bind(gen:IndividualAgent as ?target_type)
    bind(?agentName as ?target_label) bind(?tier as ?edge_type)
    bind(lcase(replace(replace(str(?tier), "^.*/", ""), "Role$", "")) as ?edge_label)
    bind(?grantNp as ?np)
  }
}
```

Three things it illustrates. The `optional` sits **outside** the `graph ?g { … }` wrapper,
because an optional pattern inside one silently drops the whole row in RDF4J. The space
nodes carry no label: the space state has none, so they are left to the fallback, which
names them from what Nanodash knows — the reason that fallback is worth having. And each
role tier is its own `?edge_type`, which is what the status line names when a grant is
pointed at; the sub-space edges are the only ones with no asserting nanopublication to
point at, since the space state does not record one.
