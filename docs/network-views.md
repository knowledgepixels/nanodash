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
sub:my-network-view a gen:ResourceView, gen:NetworkView ;
  dct:isVersionOf sub:my-network-view-kind ;
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
SELECT ?source ?source_label ?source_type
       ?target ?target_label ?target_type
       ?edge   ?edge_label
       ?np ?np_label
WHERE { ... }
```

| column | required | meaning |
| --- | --- | --- |
| `source`, `target` | yes | the two node IRIs. Nodes are deduplicated across rows, so a node reached by five edges is drawn once |
| `source_label`, `target_label` | no | the node's caption, and the first line of its hover box. The first non-empty value for a node wins, so a later row cannot rename it. A node the query leaves unnamed falls back to the label Nanodash already knows for that IRI — a user's display name, a space's or a maintained resource's label — and then to the short name of the IRI |
| `source_type`, `target_type` | no | groups nodes by colour, with a legend under the diagram. Colours are assigned in the order the types are first seen |
| `edge` | no | the relation's IRI, which makes the edge clickable |
| `edge_label` | no | the edge's caption. Without `?edge` it is a caption with nothing to open; without `?edge_label` an `?edge` is captioned with its short name |
| `np` | no | the nanopublication asserting the edge: named in the edge's hover box, and its click target where `?edge` is absent |

A row that names only one of `source`/`target` describes no edge and is passed over. A
relation asserted by several nanopublications is drawn as one edge, not as a bundle.

This is the house `_label` convention, so the companion columns behave as they do in a
table: `?x_label` always sits beside its `?x`. No change to the query API is needed —
the graph travels as ordinary result cells.

## Interaction

- **Hovering** a node shows its full label (which the caption may have had to cut), its
  IRI, and its type where the query gives one; hovering an edge shows its caption, the
  relation's IRI and the asserting nanopublication.
- **Clicking** a node opens its page, routed exactly as a link in a table cell would be
  (part page, space, user, maintained resource, or Explore), carrying this view's
  navigation context so the destination's back-link leads back here. Clicking an edge
  opens the relation's page, or the nanopublication that asserts it.
- **Dragging** a node rearranges the layout. A drag reports no click, so moving nodes
  around never navigates away.
- The force simulation stops once the layout has settled, so an open page with several
  network views is not left spinning.

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
  travel into an RTF or PDF, so the export lists the edges as a table instead.

## Authoring notes

- **Keep the result small.** A force layout over a few hundred edges is already dense;
  past that it is a hairball, whatever the library does. Aggregate or scope in SPARQL
  rather than leaning on the 1000-edge cap, which is a backstop, not a setting.
- **Label every node the query can.** Nanodash names users, spaces and maintained
  resources by itself, but anything else falls back to the short name of its IRI, which for
  many vocabularies is a code. Hand the `*_label` columns the same labels a table would show.
- **Type the nodes when the graph mixes kinds of thing.** A space, its sub-spaces, its
  maintained resources and its members read very differently in four colours than in one.
  The legend shows each type's short name, so a type IRI is as good as a word here.
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
with the component, so a page without a network view loads neither the library nor the
script.

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
select distinct ?source ?source_label ?source_type ?target ?target_label ?target_type
       ?edge ?edge_label ?np ('^' as ?np_label)
where {
  graph npa:graph { <http://purl.org/nanopub/admin/thisRepo> npa:hasCurrentSpaceState ?g . }
  {
    graph ?g { ?_resource_iri npa:hasSubSpace* ?space . ?space npa:hasSubSpace ?child . }
    bind(?space as ?source) bind(?child as ?target)
    bind(gen:Space as ?source_type) bind(gen:Space as ?target_type)
    bind("has sub-space" as ?edge_label)
  } union {
    graph ?g {
      ?_resource_iri npa:hasSubSpace* ?space .
      ?ri a gen:RoleInstantiation ; npa:forSpace ?space ;
        npa:forAgent ?agent ; npa:hasRoleType ?tier ; npa:viaNanopub ?grantNp .
    }
    optional { graph ?g { ?agent foaf:name ?agentName . } }
    bind(?space as ?source) bind(?agent as ?target)
    bind(gen:Space as ?source_type) bind(gen:IndividualAgent as ?target_type)
    bind(?agentName as ?target_label) bind(?tier as ?edge)
    bind(lcase(replace(replace(str(?tier), "^.*/", ""), "Role$", "")) as ?edge_label)
    bind(?grantNp as ?np)
  }
}
```

Two things it illustrates. The `optional` sits **outside** the `graph ?g { … }` wrapper,
because an optional pattern inside one silently drops the whole row in RDF4J. And the
space nodes carry no label: the space state has none, so they are left to the fallback,
which names them from what Nanodash knows — the reason that fallback is worth having.
